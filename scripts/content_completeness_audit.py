#!/usr/bin/env python3
from __future__ import annotations

import collections
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app/src/main/assets/content"

kcs = json.loads((CONTENT / "kcs.json").read_text(encoding="utf-8"))
exercises = json.loads((CONTENT / "exercises.json").read_text(encoding="utf-8"))
lexemes = json.loads((CONTENT / "lexemes.json").read_text(encoding="utf-8"))

errors: list[str] = []
warnings: list[str] = []

kid = {x["id"] for x in kcs}
by_kc: dict[str, list[dict]] = collections.defaultdict(list)
for q in exercises:
    for qkid in q.get("kcIds", []):
        by_kc[qkid].append(q)

# 1) Every non-lexical KC must have explicit teaching, an objective graded task,
#    and transfer. Vocabulary may rely on the deterministic generator for graded
#    meaning/spelling tasks, but still needs static teach + transfer.
non_graded_types = {"TEACH", "SELF_ASSESS", "SPELL_FROM_AUDIO"}
for item in kcs:
    qs = by_kc.get(item["id"], [])
    if not qs:
        errors.append(f"{item['id']}: no exercises")
        continue
    has_teach = any(q["type"] == "TEACH" for q in qs)
    has_transfer = any(q["type"] == "SELF_ASSESS" or "transfer" in q.get("tags", []) for q in qs)
    has_graded = any(q["type"] not in non_graded_types for q in qs)
    if not has_teach:
        errors.append(f"{item['id']}: missing TEACH")
    if not has_transfer:
        errors.append(f"{item['id']}: missing transfer")
    if item.get("domain") != "VOCABULARY" and not has_graded:
        errors.append(f"{item['id']}: missing objective graded task")

# 2) Lexical completeness for the runtime generator.
lex_by_id = {x["id"]: x for x in lexemes}
for lx in lexemes:
    if lx["id"] not in kid:
        errors.append(f"{lx['id']}: lexeme has no KC")
    if not (lx.get("lemma") or "").strip():
        errors.append(f"{lx.get('id')}: empty lemma")
    if not (lx.get("meaningEs") or "").strip():
        errors.append(f"{lx.get('id')}: empty Spanish meaning")
    if not (lx.get("distractorEs") or "").strip():
        errors.append(f"{lx['id']}: empty lexical distractor")
    if (lx.get("distractorEs") or "").strip().casefold() == (lx.get("meaningEs") or "").strip().casefold():
        errors.append(f"{lx['id']}: distractor equals correct meaning")

vocab_kcs = {x["id"] for x in kcs if x.get("domain") == "VOCABULARY"}
if vocab_kcs != set(lex_by_id):
    only_kc = sorted(vocab_kcs - set(lex_by_id))[:20]
    only_lex = sorted(set(lex_by_id) - vocab_kcs)[:20]
    if only_kc:
        errors.append(f"vocabulary KCs without lexeme specs: {only_kc}")
    if only_lex:
        errors.append(f"lexeme specs without vocabulary KC: {only_lex}")

# 3) Binary objective sanity.
for q in exercises:
    if q["type"] in {"TEACH", "SELF_ASSESS", "SPELL_FROM_AUDIO"}:
        if q.get("correctOption") not in {"", None}:
            warnings.append(f"{q['id']}: non-graded task unexpectedly has a key")
        continue
    if q.get("correctOption") not in {"A", "B"}:
        errors.append(f"{q['id']}: graded task has no A/B key")
    if not (q.get("optionA") or "").strip() or not (q.get("optionB") or "").strip():
        errors.append(f"{q['id']}: graded task has an empty option")
    if (q.get("optionA") or "").strip().casefold() == (q.get("optionB") or "").strip().casefold():
        errors.append(f"{q['id']}: identical A/B options")
    if q["type"] in {"LISTENING_AB", "PRON_DISCRIMINATION", "SPELLING_AB", "MEANING_AB"} and not (q.get("stimulusEn") or "").strip():
        # A few legacy orthography tasks are intentionally visually cued rather than audio cued.
        if not (q["type"] == "MEANING_AB" and "chunk" in q.get("tags", [])) and q["type"] not in {"SPELLING_AB"}:
            warnings.append(f"{q['id']}: {q['type']} has empty English stimulus")

# 4) No known weak placeholder distractors / authoring markers.
weak_exact = {"una opción diferente", "otra opción", "otra respuesta", "opción incorrecta"}
marker_re = re.compile(r"\b(?:tbd|lorem ipsum|replace me|dummy text)\b", re.I)
todo_re = re.compile(r"\bTODO\b")
for q in exercises:
    for field in ("optionA", "optionB"):
        if (q.get(field) or "").strip().casefold() in weak_exact:
            errors.append(f"{q['id']}: weak placeholder distractor in {field}")
    blob = " ".join(str(q.get(f, "")) for f in ("promptEs", "stimulusEn", "optionA", "optionB", "explanationEs"))
    if marker_re.search(blob) or todo_re.search(blob):
        errors.append(f"{q['id']}: authoring marker remains")

# 5) Pre-A1 is now a real foundation rather than only pronouns + alphabet.
levels = ["Pre-A1", "A1", "A2", "B1", "B2", "C1", "C2"]
listening = collections.Counter(x["cefr"] for x in exercises if x["type"] == "LISTENING_AB")
pron = collections.Counter(x["cefr"] for x in exercises if x["type"] == "PRON_DISCRIMINATION")
chunks = collections.Counter(x["cefr"] for x in kcs if x["domain"] == "CHUNK")
prag = collections.Counter(x["cefr"] for x in kcs if x["domain"] == "PRAGMATICS")
for level in levels:
    if level == "Pre-A1":
        if listening[level] < 20: errors.append(f"Pre-A1: listening {listening[level]} < 20")
        if pron[level] < 8: errors.append(f"Pre-A1: pronunciation {pron[level]} < 8")
        if chunks[level] < 20: errors.append(f"Pre-A1: chunks {chunks[level]} < 20")
        if prag[level] < 8: errors.append(f"Pre-A1: pragmatics {prag[level]} < 8")
    else:
        if listening[level] < 20: errors.append(f"{level}: listening {listening[level]} < 20")
        if pron[level] < 7: errors.append(f"{level}: pronunciation {pron[level]} < 7")
        if chunks[level] < 20: errors.append(f"{level}: chunks {chunks[level]} < 20")
        if prag[level] < 7: errors.append(f"{level}: pragmatics {prag[level]} < 7")

letter_ids = {f"L_{chr(c)}" for c in range(ord("A"), ord("Z") + 1)}
for lid in sorted(letter_ids):
    qs = by_kc.get(lid, [])
    if not any(q["type"] == "TEACH" for q in qs): errors.append(f"{lid}: no letter teaching")
    if not any(q["type"] == "AB" for q in qs): errors.append(f"{lid}: no letter objective")
    if not any(q["type"] == "SELF_ASSESS" or "transfer" in q.get("tags", []) for q in qs): errors.append(f"{lid}: no letter transfer")

# 6) Report compact coverage diagnostics.
domains = collections.Counter(x["domain"] for x in kcs)
levels_kc = collections.Counter(x["cefr"] for x in kcs)
print("domains", dict(domains))
print("kc levels", dict(levels_kc))
print("Pre-A1/A1-C2 listening", dict(listening))
print("Pre-A1/A1-C2 pronunciation", dict(pron))
print("Pre-A1/A1-C2 chunks", dict(chunks))
print("Pre-A1/A1-C2 pragmatics", dict(prag))
print("lexemes", len(lexemes), "chunk-enriched", sum(1 for x in lexemes if x.get("chunks")), "example-enriched", sum(1 for x in lexemes if x.get("exampleFrames")))
print("warnings", len(warnings))
for w in warnings[:25]:
    print("WARN", w)
if errors:
    for err in errors[:200]:
        print("ERROR", err)
    if len(errors) > 200:
        print(f"ERROR ... {len(errors)-200} more")
    sys.exit(1)
print("content completeness audit OK")
