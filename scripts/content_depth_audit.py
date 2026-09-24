#!/usr/bin/env python3
import json,collections,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; C=ROOT/'app/src/main/assets/content'; P=ROOT/'app/src/main/assets/practice/practice_catalog.json'
k=json.loads((C/'kcs.json').read_text()); e=json.loads((C/'exercises.json').read_text()); l=json.loads((C/'lexemes.json').read_text()); practice=json.loads(P.read_text())
errors=[]; levels=['Pre-A1','A1','A2','B1','B2','C1','C2']
listening=collections.Counter(x['cefr'] for x in e if x['type']=='LISTENING_AB'); pron=collections.Counter(x['cefr'] for x in e if x['type']=='PRON_DISCRIMINATION'); chunks=collections.Counter(x['cefr'] for x in k if x['domain']=='CHUNK'); prag=collections.Counter(x['cefr'] for x in k if x['domain']=='PRAGMATICS')
for lvl in levels:
    if listening[lvl] < 90: errors.append(f'{lvl}: listening {listening[lvl]} < 90')
    if pron[lvl] < 35: errors.append(f'{lvl}: pronunciation {pron[lvl]} < 35')
    if chunks[lvl] < (20 if lvl=='Pre-A1' else 35): errors.append(f'{lvl}: chunks {chunks[lvl]} too low')
    if prag[lvl] < (8 if lvl=='Pre-A1' else 7): errors.append(f'{lvl}: pragmatics {prag[lvl]} too low')
pre=sum(1 for x in l if x.get('cefr')=='Pre-A1')
if pre<150: errors.append(f'Pre-A1 lexemes {pre} < 150')
example=sum(bool(x.get('exampleFrames')) for x in l); hard=sum(bool(x.get('hardDistractorsEs')) for x in l); sense=sum(bool(x.get('senseId')) and bool(x.get('pos')) and bool(x.get('semanticDomain')) for x in l)
for label,count,threshold in [('exampleFrames',example,int(.90*len(l))),('hardDistractors',hard,int(.90*len(l))),('sense-aware',sense,int(.95*len(l)))]:
    if count<threshold: errors.append(f'{label} {count}/{len(l)} below threshold {threshold}')
multi=sum(1 for x in e if len(set(x.get('kcIds',[])))>=2)
if multi<700: errors.append(f'multi-KC exercises {multi} < 700')
placement=sum(1 for x in e if 'placement' in x.get('tags',[]) and x['type'] not in {'TEACH','SELF_ASSESS','SPELL_FROM_AUDIO'})
if placement<250: errors.append(f'placement objective items {placement} < 250')
for name,minn in [('speaking',200),('reading',130),('writing',130)]:
    if len(practice.get(name,[]))<minn: errors.append(f'{name} catalog too small: {len(practice.get(name,[]))}')
lex_transfer={x['kcIds'][0] for x in e if 'lexical-transfer-v12' in x.get('tags',[]) and x.get('kcIds')}
if len(lex_transfer)<len(l): errors.append(f'{len(l)-len(lex_transfer)} lexemes lack transfer task')
print('listening',dict(listening)); print('pronunciation',dict(pron)); print('lexemes',len(l),'Pre-A1',pre,'examples',example,'hard',hard,'sense-aware',sense); print('multiKC',multi,'placement',placement,'practice',{x:len(practice[x]) for x in ('speaking','reading','writing')})
if errors:
    [print('ERROR',x) for x in errors]; sys.exit(1)
print('content depth audit OK')
