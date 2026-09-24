#!/usr/bin/env python3
from __future__ import annotations
import json, re, sys, collections
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'
kcs=json.loads((CONTENT/'kcs.json').read_text(encoding='utf-8'))
exercises=json.loads((CONTENT/'exercises.json').read_text(encoding='utf-8'))
lexemes=json.loads((CONTENT/'lexemes.json').read_text(encoding='utf-8'))
errors=[]; warnings=[]

kc_by={x['id']:x for x in kcs}
cefrj={x['id'] for x in kcs if x['id'].startswith('G_CEFRJ')}
technical=re.compile(r'\b(?:AUX|NP|CLAUSE|SUBJECT|OBJECT|INFINITIVE|PARTICIPLE|TENSE/ASPECT|PASSIVE:|ADJ/ADV|V-ING|VVG|VN|VERB|WH-EVER)\b')
cjk=re.compile(r'[\u3040-\u30ff\u3400-\u9fff]')
placeholder=re.compile(r'\b(?:TBD|TODO|lorem|dummy|placeholder|replace me)\b',re.I)

cefrj_counts=collections.Counter()
for q in exercises:
    target=next((i for i in q.get('kcIds',[]) if i in cefrj),None)
    if target:
        cefrj_counts[q['type']]+=1
        if not ({'learner-facing-v23','learner-facing-v24'} & set(q.get('tags',[]))): errors.append(f'{q["id"]}: missing learner-facing tag')
        for field in ('promptEs','stimulusEn','optionA','optionB','explanationEs'):
            text=(q.get(field) or '').strip()
            if technical.search(text): errors.append(f'{q["id"]}: technical grammar notation leaked into {field}: {text}')
            if cjk.search(text): errors.append(f'{q["id"]}: CJK/source-note text leaked into {field}')
            if placeholder.search(text): errors.append(f'{q["id"]}: authoring placeholder in {field}')
        if q['type'] in {'TEACH','AB','SELF_ASSESS'} and not (q.get('stimulusEn') or '').strip():
            errors.append(f'{q["id"]}: CEFR-J learner task has no natural English model')
        if q['type']=='AB':
            if q.get('correctOption') not in {'A','B'}: errors.append(f'{q["id"]}: objective task has no key')
            if (q.get('optionA') or '').casefold()==(q.get('optionB') or '').casefold(): errors.append(f'{q["id"]}: duplicate labels')

for kid in sorted(cefrj):
    item=kc_by[kid]
    if 'learner-facing-v23' not in item.get('tags',[]): errors.append(f'{kid}: KC not marked learner-facing-v23')
    if technical.search(item.get('name','')): errors.append(f'{kid}: technical notation remains in learner-facing name: {item.get("name")}')
    if len(item.get('name',''))>120: warnings.append(f'{kid}: long learner name ({len(item["name"])} chars)')

weak={'respuesta','opción','otra respuesta','otra opción','una opción diferente','opción incorrecta'}
for lx in lexemes:
    d=(lx.get('distractorEs') or '').strip().casefold()
    if d in weak: errors.append(f'{lx["id"]}: generic lexical distractor remains: {d}')
    if d==(lx.get('meaningEs') or '').strip().casefold(): errors.append(f'{lx["id"]}: distractor equals meaning')

# Duplicate target labels are allowed only when they represent distinct sentence polarities/questions.
labels=collections.defaultdict(list)
for kid in cefrj: labels[kc_by[kid]['name'].casefold()].append(kid)
for label,ids in labels.items():
    if len(ids)>6: warnings.append(f'learner label shared by {len(ids)} KCs: {kc_by[ids[0]]["name"]}')

print('CEFR-J learner tasks',dict(cefrj_counts),'KCs',len(cefrj))
print('lexemes',len(lexemes),'warnings',len(warnings))
for w in warnings[:30]: print('WARN',w)
if errors:
    for e in errors[:200]: print('ERROR',e)
    if len(errors)>200: print(f'ERROR ... {len(errors)-200} more')
    sys.exit(1)
print('linguistic QA audit OK')
