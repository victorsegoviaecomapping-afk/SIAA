#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app/src/main/assets/content"


def load(name):
    return json.loads((CONTENT / name).read_text(encoding="utf-8"))


def save(name, obj):
    (CONTENT / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def safe(value: str) -> str:
    return re.sub(r"[^A-Z0-9]+", "_", value.upper()).strip("_") or "X"


def choose(key: str, good: str, bad: str):
    correct_a = int(hashlib.sha1(key.encode("utf-8")).hexdigest()[-1], 16) % 2 == 0
    return (good, bad, "A") if correct_a else (bad, good, "B")


def kc(id_, name, cefr, domain, form, meaning, use, importance=.7, prior=.04, tags=()):
    return {
        "id": id_, "name": name, "cefr": cefr, "domain": domain, "form": form,
        "meaning": meaning, "use": use, "importance": importance,
        "priorMastery": prior, "tags": list(tags),
    }


def ex(id_, type_, kcs, cefr, diff, prompt, stim="", a="", b="", correct="", exp="",
       spell="", seconds=22, tags=(), mis=()):
    return {
        "id": id_, "type": type_, "kcIds": kcs, "cefr": cefr, "difficulty": diff,
        "promptEs": prompt, "stimulusEn": stim, "optionA": a, "optionB": b,
        "correctOption": correct, "explanationEs": exp, "spellTarget": spell,
        "estimatedSeconds": seconds, "tags": list(tags), "misconceptionIds": list(mis),
    }


def edge(a, b, w=.04, hard=False):
    return {"fromId": a, "toId": b, "weight": w, "hardPrerequisite": hard}


kcs = load("kcs.json")
edges = load("edges.json")
exercises = load("exercises.json")
lexemes = load("lexemes.json")
manifest = load("manifest.json")

kid = {x["id"] for x in kcs}
eid = {x["id"] for x in exercises}
edgekey = {(x["fromId"], x["toId"]) for x in edges}
kby = {x["id"]: x for x in kcs}

stats = Counter()


def add_kc(item):
    if item["id"] not in kid:
        kcs.append(item)
        kid.add(item["id"])
        kby[item["id"]] = item
        stats["kcs_added"] += 1
        return True
    return False


def add_ex(item):
    if item["id"] not in eid:
        exercises.append(item)
        eid.add(item["id"])
        stats["exercises_added"] += 1
        return True
    return False


def add_edge(a, b, w=.04, hard=False):
    if a in kid and b in kid and (a, b) not in edgekey:
        edges.append(edge(a, b, w, hard))
        edgekey.add((a, b))
        stats["edges_added"] += 1
        return True
    return False


# ---------------------------------------------------------------------------
# 1) Close the alphabet / spelling foundation.
# ---------------------------------------------------------------------------
letter_ids = [f"L_{chr(c)}" for c in range(ord("A"), ord("Z") + 1)]
letter_names = {x["id"]: x["meaning"] for x in kcs if x.get("domain") == "LETTER"}
for idx, lid in enumerate(letter_ids):
    letter = lid[-1]
    lname = letter_names[lid]
    distract_lid = letter_ids[(idx + 7) % len(letter_ids)]
    distract = letter_names[distract_lid]
    a, b, c = choose("v22-letter-" + lid, lname, distract)
    add_ex(ex(
        f"LQ22_{letter}", "AB", [lid], "Pre-A1", .20,
        f"¿Cuál es el nombre inglés de la letra {letter}?", "", a, b, c,
        f"La letra {letter} se nombra {lname}.", seconds=14,
        tags=("alphabet", "letter-name", "objective", "v22"),
    ))
    add_ex(ex(
        f"LT22_{letter}", "TEACH", [lid], "Pre-A1", .10,
        f"Aprende el nombre inglés de la letra {letter}.", lname,
        exp=f"{letter} se nombra {lname}. Escucha y asocia el sonido con el símbolo {letter}.",
        seconds=14, tags=("alphabet", "letter-name", "teach", "phrase-audio", "v22"),
    ))
    add_ex(ex(
        f"LX22_{letter}", "SELF_ASSESS", [lid], "Pre-A1", .25,
        "Escucha el nombre de la letra y recuerda mentalmente qué símbolo representa.", lname,
        exp=f"La respuesta objetivo es {letter}.", seconds=14,
        tags=("alphabet", "letter-name", "transfer", "phrase-audio", "v22"),
    ))
    add_edge(lid, "O_ALPHABET", .025, False)

# Explicit objective/transfer for the alphabet KC itself.
a, b, c = choose("O_ALPHABET", "deletrear una palabra letra por letra", "reconocer únicamente el significado de una palabra")
add_ex(ex("O22_ALPHABET_OBJ", "AB", ["O_ALPHABET"], "Pre-A1", .22,
          "¿Para qué sirve dominar los nombres ingleses de las letras?", "", a, b, c,
          "Permiten deletrear y reconstruir la forma escrita de una palabra.", seconds=16,
          tags=("orthography", "alphabet", "objective", "v22")))
add_ex(ex("O22_ALPHABET_X", "SELF_ASSESS", ["O_ALPHABET"], "Pre-A1", .30,
          "Deletrea mentalmente tu nombre usando los nombres ingleses de las letras.", "",
          exp="Comprueba que cada símbolo se corresponde con su nombre inglés.", seconds=20,
          tags=("orthography", "alphabet", "transfer", "v22")))

# ---------------------------------------------------------------------------
# 2) Close orphan orthography and stress KCs with teach/objective/transfer.
# ---------------------------------------------------------------------------
orthography_pack = {
    "O_WORD_BOUND": {
        "teach": ("En habla continua las palabras pueden sonar unidas, pero la escritura mantiene fronteras.", "turn it off",
                  "turn it off son tres palabras aunque al hablar se encadenen."),
        "obj": ("Escucha. ¿Cuántas palabras hay en la expresión?", "turn it off", "tres palabras", "una palabra",
                "La expresión escrita es turn | it | off: tres palabras."),
        "x": ("Escucha pick it up e identifica mentalmente sus fronteras antes de imaginar la escritura.", "pick it up",
              "La segmentación esperada es pick | it | up."),
    },
    "O_SILENT_E": {
        "teach": ("La e final puede ser muda y cambiar la vocal anterior.", "cap. cape. kit. kite.",
                  "Compara cap/cape y kit/kite: la e final no se pronuncia como una sílaba independiente."),
        "obj": ("Escucha la palabra. ¿Qué forma escrita corresponde?", "kite", "kite", "kit",
                "La palabra escuchada es kite, con e final muda."),
        "x": ("Escucha name e imagina su escritura completa antes de comprobarla.", "name",
              "La forma escrita es N - A - M - E."),
    },
    "O_ED": {
        "teach": ("La terminación escrita -ed puede sonar /t/, /d/ o /id/ según el verbo.", "worked. played. wanted.",
                  "worked termina con /t/, played con /d/ y wanted añade una sílaba /id/."),
        "obj": ("Escucha worked. ¿Cómo suena la terminación -ed?", "worked", "/t/", "/d/",
                "En worked, -ed se realiza como /t/."),
        "x": ("Escucha wanted y decide mentalmente cuántas sílabas tiene y cómo se realiza -ed.", "wanted",
              "wanted tiene dos sílabas y -ed se realiza aproximadamente /id/."),
    },
}
for oid, data in orthography_pack.items():
    level = kby[oid]["cefr"]
    tp, ts, te = data["teach"]
    op, os, good, bad, oe = data["obj"]
    xp, xs, xe = data["x"]
    a, b, c = choose("v22-orth-" + oid, good, bad)
    add_ex(ex(f"T22_{oid}", "TEACH", [oid], level, .30, tp, ts, exp=te, seconds=23,
              tags=("orthography", "teach", "phrase-audio", "v22")))
    add_ex(ex(f"Q22_{oid}", "AB", [oid], level, .44, op, os, a, b, c, oe, seconds=20,
              tags=("orthography", "objective", "phrase-audio", "v22")))
    add_ex(ex(f"X22_{oid}", "SELF_ASSESS", [oid], level, .52, xp, xs, exp=xe, seconds=22,
              tags=("orthography", "transfer", "phrase-audio", "v22")))

if "PH_STRESS" in kid:
    a, b, c = choose("v22-ph-stress", "la segunda sílaba", "la primera sílaba")
    add_ex(ex("T22_PH_STRESS", "TEACH", ["PH_STRESS"], "A2", .38,
              "El inglés tiene una sílaba tónica más prominente dentro de muchas palabras.",
              "teacher. hotel. about.", exp="Escucha dónde recae la prominencia: TEA-cher, ho-TEL, a-BOUT.",
              seconds=24, tags=("pronunciation", "stress", "teach", "audio-asset", "v22")))
    add_ex(ex("Q22_PH_STRESS", "AB", ["PH_STRESS"], "A2", .50,
              "Escucha hotel. ¿Dónde cae la sílaba tónica?", "hotel", a, b, c,
              "hotel lleva el acento principal en la segunda sílaba.", seconds=18,
              tags=("pronunciation", "stress", "objective", "audio-asset", "v22")))
    add_ex(ex("X22_PH_STRESS", "SELF_ASSESS", ["PH_STRESS"], "A2", .58,
              "Escucha computer e identifica mentalmente la sílaba más prominente.", "computer",
              exp="La prominencia principal está en la segunda sílaba: com-PU-ter.", seconds=20,
              tags=("pronunciation", "stress", "transfer", "audio-asset", "v22")))

# ---------------------------------------------------------------------------
# 3) Real Pre-A1 survival layer: formulaic language, pragmatics, listening, pronunciation.
# ---------------------------------------------------------------------------
PRE_CHUNKS = [
    ("hello", "hola", "Hello. My name is Leo.", "greeting"),
    ("goodbye", "adiós", "Goodbye. See you tomorrow.", "leave-taking"),
    ("thanks a lot", "muchas gracias", "Thanks a lot for your help.", "thanks"),
    ("please", "por favor", "Water, please.", "politeness"),
    ("sorry", "lo siento / perdón", "Sorry, I'm late.", "apology"),
    ("yes, please", "sí, por favor", "Yes, please. One coffee.", "service"),
    ("no, thank you", "no, gracias", "No, thank you. I'm fine.", "service"),
    ("my name is", "me llamo / mi nombre es", "My name is Ana.", "identity"),
    ("what's your name", "cómo te llamas", "What's your name?", "identity"),
    ("I'm from", "soy de / vengo de", "I'm from Peru.", "identity"),
    ("I live in", "vivo en", "I live in Lima.", "identity"),
    ("how are you", "cómo estás", "How are you today?", "greeting"),
    ("I'm fine, thanks", "estoy bien, gracias", "I'm fine, thanks. And you?", "greeting"),
    ("where are you from", "de dónde eres", "Where are you from?", "identity"),
    ("where is the bathroom", "dónde está el baño", "Excuse me. Where is the bathroom?", "survival"),
    ("I need help", "necesito ayuda", "I need help, please.", "survival"),
    ("I don't understand", "no entiendo", "Sorry, I don't understand.", "repair"),
    ("please repeat", "repita / repite, por favor", "Please repeat the number.", "repair"),
    ("more slowly, please", "más despacio, por favor", "More slowly, please.", "repair"),
    ("how much is it", "cuánto cuesta", "How much is it?", "shopping"),
    ("what does this mean", "qué significa esto", "What does this mean?", "repair"),
    ("can you help me", "puedes ayudarme", "Can you help me, please?", "survival"),
    ("see you tomorrow", "nos vemos mañana", "See you tomorrow. Bye!", "leave-taking"),
    ("good morning", "buenos días", "Good morning. How are you?", "greeting"),
    ("good afternoon", "buenas tardes", "Good afternoon. Can I help you?", "greeting"),
    ("good evening", "buenas tardes / noches al saludar", "Good evening. Welcome.", "greeting"),
    ("good night", "buenas noches al despedirse", "Good night. See you tomorrow.", "leave-taking"),
    ("one more time, please", "una vez más, por favor", "One more time, please.", "repair"),
]
existing_chunk_names = {x["name"].casefold() for x in kcs if x.get("domain") == "CHUNK"}
lex_by_lemma = {x["lemma"].casefold(): x for x in lexemes}
pre_chunk_ids = []
for i, (phrase, meaning, example, kind) in enumerate(PRE_CHUNKS):
    if phrase.casefold() in existing_chunk_names:
        continue
    cid = "C22_PRE_" + safe(phrase)
    add_kc(kc(cid, phrase, "Pre-A1", "CHUNK", phrase, meaning,
              f"usar una fórmula básica de {kind}", .82, .06,
              ("multiword", "formulaic", "pre-a1", kind, "v22", "original-authoring")))
    pre_chunk_ids.append(cid)
    existing_chunk_names.add(phrase.casefold())
    wrong = PRE_CHUNKS[(i + 9) % len(PRE_CHUNKS)][1]
    if wrong == meaning:
        wrong = PRE_CHUNKS[(i + 1) % len(PRE_CHUNKS)][1]
    a, b, c = choose("v22-pre-chunk-" + cid, meaning, wrong)
    add_ex(ex("T22_" + safe(cid), "TEACH", [cid], "Pre-A1", .15,
              f"Aprende esta expresión básica: {phrase}.", phrase,
              exp=f"{phrase}: {meaning}. Ejemplo: {example}", seconds=17,
              tags=("chunk", "pre-a1", "phrase-audio", "teach", "v22", kind)))
    add_ex(ex("Q22_" + safe(cid), "AB", [cid], "Pre-A1", .25,
              f"¿Qué significa {phrase}?", phrase, a, b, c,
              f"{phrase} significa {meaning}.", seconds=16,
              tags=("chunk", "pre-a1", "phrase-audio", "objective", "v22", kind)))
    add_ex(ex("X22_" + safe(cid), "SELF_ASSESS", [cid], "Pre-A1", .32,
              f"Escucha el ejemplo y recuerda cuándo usarías {phrase}.", example,
              exp=f"Fórmula objetivo: {phrase} = {meaning}.", seconds=18,
              tags=("chunk", "pre-a1", "phrase-audio", "transfer", "v22", kind)))
    for tok in re.findall(r"[A-Za-z]+", phrase.casefold()):
        lx = lex_by_lemma.get(tok)
        if lx:
            add_edge(lx["id"], cid, .03, False)
for a_id, b_id in zip(pre_chunk_ids, pre_chunk_ids[1:]):
    add_edge(a_id, b_id, .02, False)

PRE_PRAG = [
    ("P22_PRE_GREETING", "Saludar", "Hello. How are you?", "Good night. Go away.",
     "iniciar un contacto social básico con una fórmula apropiada",
     "A person you know arrives in the morning.", "Good morning!"),
    ("P22_PRE_THANKS", "Responder a agradecimiento", "You're welcome.", "What's your name?",
     "responder de forma básica a un agradecimiento",
     "Someone says: Thank you for your help.", "You're welcome."),
    ("P22_PRE_APOLOGY", "Disculparse", "Sorry.", "How much is it?",
     "reparar una molestia o error simple",
     "You arrive a little late.", "Sorry I'm late."),
    ("P22_PRE_ATTENTION", "Llamar la atención con cortesía", "Excuse me.", "Goodbye.",
     "iniciar una pregunta a un desconocido con cortesía",
     "You need to ask a stranger a question.", "Excuse me."),
    ("P22_PRE_HELP", "Pedir ayuda", "Can you help me, please?", "I'm from Peru.",
     "pedir ayuda de manera simple y cortés",
     "You cannot find the station.", "Can you help me, please?"),
    ("P22_PRE_REPAIR", "Indicar falta de comprensión", "I don't understand. Please repeat.", "I'm fine, thanks.",
     "pedir reparación cuando no se entiende",
     "The speaker talks too fast.", "More slowly, please."),
    ("P22_PRE_LEAVE", "Despedirse", "Goodbye. See you tomorrow.", "Where are you from?",
     "cerrar una interacción breve de forma apropiada",
     "You are leaving after class.", "See you tomorrow. Bye!"),
    ("P22_PRE_SERVICE", "Aceptar o rechazar cortésmente", "No, thank you.", "No.",
     "aceptar o rechazar una oferta con una fórmula cortés",
     "Someone offers you more coffee and you do not want any.", "No, thank you."),
]
for pid, name, good, bad, use, scenario, transfer_resp in PRE_PRAG:
    add_kc(kc(pid, name, "Pre-A1", "PRAGMATICS", good, name.lower(), use,
              .76, .05, ("pragmatics", "pre-a1", "v22", "original-authoring")))
    a, b, c = choose("v22-prag-" + pid, good, bad)
    add_ex(ex("T22_" + pid, "TEACH", [pid], "Pre-A1", .18,
              f"Aprende una forma básica para esta función: {name.lower()}.", good,
              exp=f"Función: {use}. Respuesta modelo: {good}", seconds=18,
              tags=("pragmatics", "pre-a1", "teach", "phrase-audio", "v22")))
    add_ex(ex("Q22_" + pid, "AB", [pid], "Pre-A1", .28,
              f"{scenario} ¿Qué respuesta es más adecuada?", "", a, b, c,
              f"La respuesta adecuada es: {good}", seconds=17,
              tags=("pragmatics", "pre-a1", "appropriacy", "objective", "v22")))
    add_ex(ex("X22_" + pid, "SELF_ASSESS", [pid], "Pre-A1", .34,
              f"Imagina una situación parecida y produce mentalmente una respuesta breve para {name.lower()}.",
              transfer_resp, exp=f"Modelo posible: {transfer_resp}", seconds=18,
              tags=("pragmatics", "pre-a1", "transfer", "phrase-audio", "v22")))

# Listening skills and 24 short original items.
PRE_LISTEN_SKILLS = [
    ("L22_PRE_GREET", "Reconocer saludos y despedidas", "identificar fórmulas sociales muy frecuentes"),
    ("L22_PRE_IDENTITY", "Reconocer identidad básica", "captar nombre, origen y lugar de residencia"),
    ("L22_PRE_NUMBERS", "Reconocer números básicos", "captar números aislados en contexto"),
    ("L22_PRE_SPELL", "Reconocer deletreo", "reconstruir una palabra a partir de nombres de letras"),
    ("L22_PRE_INSTR", "Reconocer instrucciones simples", "seguir una instrucción breve y directa"),
    ("L22_PRE_PLACE", "Reconocer lugares básicos", "identificar un lugar mencionado explícitamente"),
]
for lid, name, meaning in PRE_LISTEN_SKILLS:
    add_kc(kc(lid, name, "Pre-A1", "LISTENING", "", meaning,
              "comprensión auditiva básica de una frase corta", .79, .05,
              ("listening", "pre-a1", "v22", "original-authoring")))
    add_ex(ex("T22_" + lid, "TEACH", [lid], "Pre-A1", .16,
              f"Estrategia: {name.lower()}.", "",
              exp=f"Escucha primero la palabra o dato clave. Objetivo: {meaning}.", seconds=17,
              tags=("listening", "pre-a1", "teach", "metacognitive", "v22")))
    add_ex(ex("X22_" + lid, "SELF_ASSESS", [lid], "Pre-A1", .30,
              "Antes de la siguiente escucha, predice qué dato clave buscarás y comprueba después si lo identificaste.", "",
              exp="Usa una meta concreta de escucha: saludo, nombre, número, letras, instrucción o lugar.", seconds=18,
              tags=("listening", "pre-a1", "transfer", "metacognitive", "v22")))

LISTEN_ITEMS = [
    ("L22_PRE_GREET", "hello_morning", "Good morning, Mia.", "¿Qué tipo de saludo escuchas?", "un saludo de mañana", "una despedida"),
    ("L22_PRE_GREET", "bye_tomorrow", "Goodbye. See you tomorrow.", "¿Qué está haciendo el hablante?", "despidiéndose", "presentándose"),
    ("L22_PRE_GREET", "evening_welcome", "Good evening. Welcome.", "¿Qué ocurre?", "un saludo al llegar", "una disculpa"),
    ("L22_PRE_GREET", "night_leave", "Good night. See you soon.", "¿Qué función tiene la frase?", "despedirse", "pedir ayuda"),
    ("L22_PRE_IDENTITY", "name_ana", "My name is Ana.", "¿Cómo se llama?", "Ana", "Mia"),
    ("L22_PRE_IDENTITY", "from_peru", "I'm from Peru.", "¿De dónde es?", "Peru", "Canada"),
    ("L22_PRE_IDENTITY", "live_lima", "I live in Lima.", "¿Dónde vive?", "Lima", "Cusco"),
    ("L22_PRE_IDENTITY", "name_leo", "Hello. I'm Leo.", "¿Cuál es su nombre?", "Leo", "Luis"),
    ("L22_PRE_NUMBERS", "two_coffees", "Two coffees, please.", "¿Cuántos cafés pide?", "two", "three"),
    ("L22_PRE_NUMBERS", "room_six", "Room six.", "¿Qué número de habitación?", "six", "seven"),
    ("L22_PRE_NUMBERS", "bus_four", "Bus four.", "¿Qué número de bus?", "four", "five"),
    ("L22_PRE_NUMBERS", "price_ten", "Ten dollars, please.", "¿Qué número escuchas?", "ten", "twelve"),
    ("L22_PRE_SPELL", "mia_spell", "M. I. A.", "¿Qué secuencia de letras escuchas?", "M-I-A", "M-A-I"),
    ("L22_PRE_SPELL", "leo_spell", "L. E. O.", "¿Qué secuencia de letras escuchas?", "L-E-O", "L-O-E"),
    ("L22_PRE_SPELL", "bus_spell", "B. U. S.", "¿Qué palabra se puede formar?", "BUS", "SUB"),
    ("L22_PRE_SPELL", "map_spell", "M. A. P.", "¿Qué palabra se puede formar?", "MAP", "PAM"),
    ("L22_PRE_INSTR", "sit_down", "Please sit down.", "¿Qué debes hacer?", "sentarte", "ponerte de pie"),
    ("L22_PRE_INSTR", "stand_up", "Please stand up.", "¿Qué debes hacer?", "ponerte de pie", "sentarte"),
    ("L22_PRE_INSTR", "come_in", "Come in, please.", "¿Qué debes hacer?", "entrar", "salir"),
    ("L22_PRE_INSTR", "listen", "Listen, please.", "¿Qué acción pide?", "escuchar", "escribir"),
    ("L22_PRE_PLACE", "bathroom_left", "The bathroom is on the left.", "¿Qué lugar menciona?", "the bathroom", "the station"),
    ("L22_PRE_PLACE", "station_here", "The station is here.", "¿Qué lugar menciona?", "the station", "the hotel"),
    ("L22_PRE_PLACE", "hotel_there", "The hotel is there.", "¿Qué lugar menciona?", "the hotel", "the school"),
    ("L22_PRE_PLACE", "school_right", "The school is on the right.", "¿Qué lugar menciona?", "the school", "the airport"),
]
for lid, slug, stim, prompt, good, bad in LISTEN_ITEMS:
    a, b, c = choose("v22-listen-" + slug, good, bad)
    add_ex(ex("L22_" + safe(slug), "LISTENING_AB", [lid], "Pre-A1", .22,
              prompt, stim, a, b, c, f"Respuesta objetivo: {good}.", seconds=18,
              tags=("listening", "pre-a1", "original-script", "audio-asset", "transfer", "v22")))

PRE_PHONOLOGY = [
    ("P22_PRE_B_P", "Contraste inicial b/p", "distinguir /b/ y /p/ al inicio", [("bat. pat.", "diferente"), ("big. pig.", "diferente")]),
    ("P22_PRE_M_N", "Contraste inicial m/n", "distinguir /m/ y /n/ al inicio", [("map. nap.", "diferente"), ("mine. nine.", "diferente")]),
    ("P22_PRE_S_Z", "Contraste inicial s/z", "distinguir /s/ y /z/", [("sip. zip.", "diferente"), ("bus. buzz.", "diferente")]),
    ("P22_PRE_I_IY", "Contraste corto/largo i", "distinguir ship/sheep y pares similares", [("ship. sheep.", "diferente"), ("sit. seat.", "diferente")]),
]
for pid, name, meaning, pairs in PRE_PHONOLOGY:
    add_kc(kc(pid, name, "Pre-A1", "PHONOLOGY", "", meaning,
              "percepción auditiva inicial", .68, .04,
              ("phonology", "pre-a1", "perception", "v22")))
    add_ex(ex("T22_" + pid, "TEACH", [pid], "Pre-A1", .18,
              f"Escucha el contraste: {name.lower()}.", pairs[0][0],
              exp=f"Objetivo: {meaning}. No necesitas producirlo todavía; primero aprende a oír la diferencia.", seconds=18,
              tags=("pronunciation", "pre-a1", "teach", "audio-asset", "v22")))
    for j, (stim, answer) in enumerate(pairs, 1):
        good, bad = ("diferente", "igual") if answer == "diferente" else ("igual", "diferente")
        a, b, c = choose(f"v22-ph-{pid}-{j}", good, bad)
        add_ex(ex(f"PD22_{safe(pid)}_{j}", "PRON_DISCRIMINATION", [pid], "Pre-A1", .24,
                  "Escucha dos palabras. ¿Suenan iguales o diferentes?", stim, a, b, c,
                  f"La respuesta es {answer}.", seconds=16,
                  tags=("pronunciation", "pre-a1", "discrimination", "audio-asset", "transfer", "v22")))
    add_ex(ex("X22_" + pid, "SELF_ASSESS", [pid], "Pre-A1", .30,
              "Vuelve a escuchar el contraste y nombra mentalmente qué parte del sonido cambia.", pairs[-1][0],
              exp=f"Objetivo perceptivo: {meaning}.", seconds=18,
              tags=("pronunciation", "pre-a1", "transfer", "audio-asset", "v22")))

# ---------------------------------------------------------------------------
# 4) Close any legacy CHUNK / LISTENING KCs that still lack transfer evidence.
# ---------------------------------------------------------------------------
by_kc = defaultdict(list)
for q in exercises:
    for qkid in q.get("kcIds", []):
        by_kc[qkid].append(q)

chunk_examples = {
    "C_DONT_KNOW": "I don't know the answer yet.",
    "C_WANT_TO": "I want to learn English on the bus.",
    "C_DEPENDS": "It depends on the time and the price.",
    "C_NOT_SURE": "I'm not sure which option is better.",
    "C_AS_FAR": "As far as I know, the office opens at nine.",
}
for item in kcs:
    cid = item["id"]
    qs = by_kc.get(cid, [])
    has_transfer = any(q["type"] == "SELF_ASSESS" or "transfer" in q.get("tags", []) for q in qs)
    if item.get("domain") == "CHUNK" and not has_transfer:
        stimulus = chunk_examples.get(cid, item.get("form") or item.get("name", ""))
        add_ex(ex("X22_LEGACY_" + safe(cid), "SELF_ASSESS", [cid], item["cefr"], .48,
                  f"Escucha un contexto nuevo y recupera la función de {item['name']}.", stimulus,
                  exp=f"Unidad objetivo: {item.get('form') or item['name']} = {item.get('meaning','')}.", seconds=22,
                  tags=("chunk", "transfer", "phrase-audio", "legacy-closure", "v22")))

listen_transfer_examples = {
    "LS_KEYWORDS": "The class starts at nine in room four.",
    "LS_DETAIL": "The meeting is on Thursday at two thirty.",
}
# Refresh after chunk additions.
by_kc = defaultdict(list)
for q in exercises:
    for qkid in q.get("kcIds", []):
        by_kc[qkid].append(q)
for item in kcs:
    cid = item["id"]
    if item.get("domain") != "LISTENING":
        continue
    qs = by_kc.get(cid, [])
    has_transfer = any(q["type"] == "SELF_ASSESS" or "transfer" in q.get("tags", []) for q in qs)
    if not has_transfer:
        stim = listen_transfer_examples.get(cid, "Listen once for the main idea, then listen again for one precise detail.")
        add_ex(ex("X22_LEGACY_" + safe(cid), "SELF_ASSESS", [cid], item["cefr"], .46,
                  "Escucha y explica mentalmente qué pista usarías para resolver una pregunta nueva del mismo tipo.", stim,
                  exp=f"Habilidad objetivo: {item.get('meaning') or item.get('name')}.", seconds=24,
                  tags=("listening", "transfer", "metacognitive", "audio-asset", "legacy-closure", "v22")))

# Add teach items to listening KCs that had practice but no explicit strategy instruction.
by_kc = defaultdict(list)
for q in exercises:
    for qkid in q.get("kcIds", []):
        by_kc[qkid].append(q)
for item in kcs:
    cid = item["id"]
    if item.get("domain") != "LISTENING":
        continue
    if not any(q["type"] == "TEACH" for q in by_kc.get(cid, [])):
        add_ex(ex("T22_LEGACY_" + safe(cid), "TEACH", [cid], item["cefr"], .35,
                  f"Estrategia de listening: {item['name']}.", "",
                  exp=f"Objetivo: {item.get('meaning') or item['name']}. Escucha con una meta concreta y verifica la evidencia después.",
                  seconds=20, tags=("listening", "teach", "metacognitive", "legacy-closure", "v22")))

# ---------------------------------------------------------------------------
# 5) Improve lexical distractors and connect lexemes to the formulaic bank.
# ---------------------------------------------------------------------------

def pos_of(lx):
    for tag in lx.get("tags", []):
        if tag.startswith("pos:"):
            return tag[4:]
    return "other"

pool = defaultdict(list)
for lx in lexemes:
    pool[(lx.get("cefr", "A1"), pos_of(lx))].append(lx)
for group in pool.values():
    group.sort(key=lambda x: (x.get("frequencyRank") if isinstance(x.get("frequencyRank"), int) else 10**9, x["lemma"].casefold()))

for lx in lexemes:
    candidates = [c for c in pool[(lx.get("cefr", "A1"), pos_of(lx))]
                  if c["id"] != lx["id"] and c.get("meaningEs") and c.get("meaningEs") != lx.get("meaningEs")]
    if candidates:
        rank = lx.get("frequencyRank") if isinstance(lx.get("frequencyRank"), int) else 10**9
        candidates.sort(key=lambda c: (abs((c.get("frequencyRank") if isinstance(c.get("frequencyRank"), int) else 10**9) - rank), c["lemma"].casefold()))
        top = candidates[: min(12, len(candidates))]
        pick = int(hashlib.sha1(lx["id"].encode()).hexdigest()[:8], 16) % len(top)
        new_distractor = top[pick]["meaningEs"]
        if lx.get("distractorEs") != new_distractor:
            lx["distractorEs"] = new_distractor
            stats["distractors_improved"] += 1

# Collect existing formulaic forms and their best transfer examples.
by_kc = defaultdict(list)
for q in exercises:
    for qkid in q.get("kcIds", []):
        by_kc[qkid].append(q)
formulaic = [x for x in kcs if x.get("domain") == "CHUNK"]
for lx in lexemes:
    lemma = lx["lemma"].casefold().strip()
    if not lemma:
        continue
    chunks = list(dict.fromkeys(lx.get("chunks", [])))
    frames = list(dict.fromkeys(lx.get("exampleFrames", [])))
    for ch in formulaic:
        form = (ch.get("form") or ch.get("name") or "").strip()
        if not form:
            continue
        tokens = re.findall(r"[a-z]+", form.casefold())
        matches = lemma in tokens if " " not in lemma else lemma in form.casefold()
        if not matches:
            continue
        if form not in chunks and len(chunks) < 3:
            chunks.append(form)
        for q in by_kc.get(ch["id"], []):
            stim = (q.get("stimulusEn") or "").strip()
            if (q["type"] == "SELF_ASSESS" or "transfer" in q.get("tags", [])) and stim and stim.casefold() != form.casefold():
                if stim not in frames and len(frames) < 2:
                    frames.append(stim)
        if len(chunks) >= 3 and len(frames) >= 2:
            break
    if chunks != lx.get("chunks", []):
        lx["chunks"] = chunks
        stats["lexemes_chunk_enriched"] += 1
    if frames != lx.get("exampleFrames", []):
        lx["exampleFrames"] = frames
        stats["lexemes_examples_enriched"] += 1

# ---------------------------------------------------------------------------
# 6) Safe text hygiene for obvious imported profile typos (IDs remain stable).
# ---------------------------------------------------------------------------
REPLACEMENTS = {
    "COMPLEME NT": "COMPLEMENT",
    "lexicalverbs": "lexical verbs",
}
for arr in (kcs, exercises):
    for item in arr:
        for field, value in list(item.items()):
            if not isinstance(value, str):
                continue
            new = value
            for old, rep in REPLACEMENTS.items():
                new = new.replace(old, rep)
            if new != value:
                item[field] = new
                stats["text_fixes"] += 1

# ---------------------------------------------------------------------------
# 7) Deterministic order, manifest/version/checksums.
# ---------------------------------------------------------------------------
level_order = {"Pre-A1": 0, "A1": 1, "A2": 2, "B1": 3, "B2": 4, "C1": 5, "C2": 6}
kcs.sort(key=lambda x: (level_order.get(x.get("cefr", "A1"), 99), x.get("domain", ""), x["id"]))
edges.sort(key=lambda x: (x["fromId"], x["toId"], not x.get("hardPrerequisite", True)))
exercises.sort(key=lambda x: (level_order.get(x.get("cefr", "A1"), 99), x.get("type", ""), x["id"]))
lexemes.sort(key=lambda x: (level_order.get(x.get("cefr", "A1"), 99),
                            x.get("frequencyRank") if isinstance(x.get("frequencyRank"), int) else 10**9,
                            x["lemma"].casefold()))

save("kcs.json", kcs)
save("edges.json", edges)
save("exercises.json", exercises)
save("lexemes.json", lexemes)

manifest["version"] = "2.2.0"
manifest["contentVersion"] = "2.2.0"
manifest["description"] = (
    "SIAA v2.2: cierre de contenido curricular A1-C2 y fundación Pre-A1 ampliada; "
    "alfabeto totalmente evaluable, ortografía/fonología sin KCs huérfanos, supervivencia, "
    "pragmática, listening y pronunciación Pre-A1, y distractores léxicos más plausibles."
)
for source in [
    "SIAA original Pre-A1 survival/formulaic authoring v2.2",
    "SIAA original Pre-A1 listening/pragmatics/phonology authoring v2.2",
    "CEFR Companion Volume 2020 — Pre-A1/A1 reception and interaction guidance",
]:
    if source not in manifest.setdefault("sources", []):
        manifest["sources"].append(source)

generated_estimate = sum(3 + min(3, len(x.get("chunks", []))) for x in lexemes)
manifest["coverage"] = {
    "kcs": len(kcs),
    "edges": len(edges),
    "staticExercises": len(exercises),
    "lexemes": len(lexemes),
    "multiwordKcs": sum(1 for x in kcs if x.get("domain") == "CHUNK"),
    "listeningItems": sum(1 for x in exercises if x.get("type") == "LISTENING_AB"),
    "pronunciationItems": sum(1 for x in exercises if x.get("type") == "PRON_DISCRIMINATION"),
    "generatedVariantsEstimate": generated_estimate,
    "levels": ["Pre-A1", "A1", "A2", "B1", "B2", "C1", "C2"],
}
manifest["checksums"] = {
    n: hashlib.sha256((CONTENT / n).read_bytes()).hexdigest()
    for n in ("kcs.json", "edges.json", "exercises.json", "lexemes.json")
}
save("manifest.json", manifest)

print(json.dumps({
    **stats,
    "kcs": len(kcs),
    "edges": len(edges),
    "exercises": len(exercises),
    "lexemes": len(lexemes),
    "multiword": manifest["coverage"]["multiwordKcs"],
    "listening": manifest["coverage"]["listeningItems"],
    "pronunciation": manifest["coverage"]["pronunciationItems"],
    "generatedEstimate": generated_estimate,
}, ensure_ascii=False, indent=2))

# ---------------------------------------------------------------------------
# 8) Final closure pass for legacy orthography/phonology and weak distractors.
# This block is deliberately idempotent so the full script remains reproducible.
# ---------------------------------------------------------------------------
kcs = load("kcs.json")
edges = load("edges.json")
exercises = load("exercises.json")
lexemes = load("lexemes.json")
manifest = load("manifest.json")
kid = {x["id"] for x in kcs}
eid = {x["id"] for x in exercises}
kby = {x["id"]: x for x in kcs}

final_added = 0

def fadd(item):
    global final_added
    if item["id"] not in eid:
        exercises.append(item)
        eid.add(item["id"])
        final_added += 1

for oid, stim, explanation in [
    ("O_IGH", "night. light. right.", "Las tres palabras comparten la secuencia escrita igh aunque debes aprender cada palabra como una unidad sonido-escritura."),
    ("O_OUGH", "enough. through. thought. although.", "La secuencia ough cambia de pronunciación entre palabras; recupera la forma escrita exacta de cada palabra."),
]:
    if oid in kid:
        fadd(ex("X22_FINAL_" + oid, "SELF_ASSESS", [oid], kby[oid]["cefr"], .58,
                "Escucha las palabras y reconstruye mentalmente su patrón ortográfico antes de comprobarlo.", stim,
                exp=explanation, seconds=24,
                tags=("orthography", "transfer", "phrase-audio", "legacy-closure", "v22")))

if "PH_TH" in kid:
    fadd(ex("T22_FINAL_PH_TH", "TEACH", ["PH_TH"], "A2", .36,
            "Escucha el contraste de th sordo y sonoro en palabras frecuentes.", "think. this. three. they.",
            exp="think/three comienzan con /th/ sordo; this/they usan la variante sonora. Primero entrena la percepción.",
            seconds=23, tags=("pronunciation", "teach", "audio-asset", "v22")))
    fadd(ex("X22_FINAL_PH_TH", "SELF_ASSESS", ["PH_TH"], "A2", .58,
            "Escucha think y this e identifica mentalmente qué cambia entre los dos sonidos iniciales.", "think. this.",
            exp="Ambas se escriben th, pero el primer sonido es sordo y el segundo sonoro.",
            seconds=20, tags=("pronunciation", "transfer", "audio-asset", "v22")))
if "PH_SHORT_LONG_I" in kid:
    fadd(ex("X22_FINAL_PH_SHORT_LONG_I", "SELF_ASSESS", ["PH_SHORT_LONG_I"], "A1", .48,
            "Escucha sit y seat. Decide mentalmente cuál tiene la vocal más larga/tensa.", "sit. seat.",
            exp="seat contiene /i:/; sit contiene /i/ corta.", seconds=18,
            tags=("pronunciation", "transfer", "audio-asset", "v22")))

# Replace the five legacy placeholder distractors with real same-level chunk meanings.
chunk_by_level = defaultdict(list)
for item in kcs:
    if item.get("domain") == "CHUNK" and item.get("meaning"):
        chunk_by_level[item.get("cefr", "A1")].append(item)
for q in exercises:
    weak_fields = [f for f in ("optionA", "optionB") if (q.get(f) or "").strip().casefold() in {"una opción diferente", "otra opción"}]
    if not weak_fields or not q.get("kcIds"):
        continue
    target = kby.get(q["kcIds"][0])
    if not target or target.get("domain") != "CHUNK":
        continue
    candidates = [x for x in chunk_by_level[target.get("cefr", "A1")] if x["id"] != target["id"] and x.get("meaning") != target.get("meaning")]
    if not candidates:
        continue
    candidates.sort(key=lambda x: x["id"])
    pick = int(hashlib.sha1(q["id"].encode()).hexdigest()[:8], 16) % len(candidates)
    replacement = candidates[pick]["meaning"]
    for field in weak_fields:
        q[field] = replacement

level_order = {"Pre-A1": 0, "A1": 1, "A2": 2, "B1": 3, "B2": 4, "C1": 5, "C2": 6}
exercises.sort(key=lambda x: (level_order.get(x.get("cefr", "A1"), 99), x.get("type", ""), x["id"]))
save("exercises.json", exercises)
manifest["coverage"]["staticExercises"] = len(exercises)
manifest["coverage"]["listeningItems"] = sum(1 for x in exercises if x.get("type") == "LISTENING_AB")
manifest["coverage"]["pronunciationItems"] = sum(1 for x in exercises if x.get("type") == "PRON_DISCRIMINATION")
manifest["checksums"] = {
    n: hashlib.sha256((CONTENT / n).read_bytes()).hexdigest()
    for n in ("kcs.json", "edges.json", "exercises.json", "lexemes.json")
}
save("manifest.json", manifest)
print(json.dumps({"finalClosureExercisesAdded": final_added, "finalStaticExercises": len(exercises)}, ensure_ascii=False))
