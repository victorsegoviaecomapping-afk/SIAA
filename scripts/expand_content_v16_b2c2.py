#!/usr/bin/env python3
from __future__ import annotations
import csv,json,hashlib,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'
CTX=ROOT/'app/src/main/assets/reference/contexto/contexto_cefr_v16_selected.json'
REF_A=ROOT/'app/src/main/assets/reference/cefrj/cefrj-vocabulary-profile-1.5.csv'
REF_C=ROOT/'app/src/main/assets/reference/cefrj/octanove-vocabulary-profile-c1c2-1.0.csv'

def load(n): return json.loads((CONTENT/n).read_text(encoding='utf-8'))
def save(n,o): (CONTENT/n).write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def safe(s): return re.sub(r'[^A-Z0-9]+','_',s.upper()).strip('_') or 'X'
def spell_diff(w):
    x=w.lower();d=.28+min(.24,max(0,len(x)-6)*.017)
    if any(p in x for p in ('ough','augh','eigh','igh','tion','sion','ph','wr','kn','gue','rh','ps')): d+=.16
    return min(.90,d)

def profile_map():
    p={}
    for ref in (REF_A,REF_C):
        with ref.open(encoding='utf-8-sig',newline='') as f:
            for r in csv.DictReader(f):
                w=(r.get('headword') or '').strip().casefold(); l=(r.get('CEFR') or '').strip()
                if w and l: p.setdefault(w,set()).add(l)
    return p

def ex(id,type,kcs,cefr,diff,prompt,stim='',exp='',spell='',seconds=22,tags=()):
    return {'id':id,'type':type,'kcIds':kcs,'cefr':cefr,'difficulty':diff,'promptEs':prompt,'stimulusEn':stim,
            'optionA':'','optionB':'','correctOption':'','explanationEs':exp,'spellTarget':spell,
            'estimatedSeconds':seconds,'tags':list(tags),'misconceptionIds':[]}

profile=profile_map(); rows=json.loads(CTX.read_text(encoding='utf-8'))
kcs=load('kcs.json'); edges=load('edges.json'); exs=load('exercises.json'); lex=load('lexemes.json'); man=load('manifest.json')
lem={x['lemma'].casefold():x for x in lex}; kids={x['id'] for x in kcs}; eids={x['id'] for x in exs}

def add_ex(e):
    if e['id'] not in eids: exs.append(e); eids.add(e['id'])

def add_one(i,r):
    lemma=r['lemma'].strip(); meaning=r['meaningEs'].strip(); pos=r.get('pos','').strip() or 'other'; level=r['cefr'].strip(); rank=r.get('frequencyRank')
    if level not in profile.get(lemma.casefold(),set()): return 'unsupported'
    if lemma.casefold() in lem:
        lx=lem[lemma.casefold()]; tg=set(lx.get('tags',[])); tg.update(['contexto-pack','cefrj','v16-reviewed']); lx['tags']=sorted(tg)
        if lx.get('frequencyRank') is None and isinstance(rank,int): lx['frequencyRank']=rank
        return 'enriched'
    lid='V_'+safe(lemma)
    pool=[x['meaningEs'] for x in rows if x['cefr']==level and x['meaningEs']!=meaning]
    distract=pool[(i*7+19)%len(pool)] if pool else 'otra opción'
    lx={'id':lid,'lemma':lemma,'meaningEs':meaning,'cefr':level,'frequencyRank':rank if isinstance(rank,int) else None,
        'spellingDifficulty':spell_diff(lemma),'distractorEs':distract,'chunks':[],'exampleFrames':[],
        'tags':['cefrj','contexto-pack','open-data','v16-b2c2','pos:'+pos]}
    lex.append(lx); lem[lemma.casefold()]=lx
    if lid not in kids:
        kcs.append({'id':lid,'name':lemma,'cefr':level,'domain':'VOCABULARY','form':lemma,'meaning':meaning,
                    'use':'reconocimiento auditivo, significado, ortografía y transferencia léxica',
                    'importance':.66 if level=='B2' else (.64 if level=='C1' else .62),'priorMastery':.035,
                    'tags':['lexeme','cefrj','contexto-pack','v16-b2c2','pos:'+pos]}); kids.add(lid)
    diff={'B2':.62,'C1':.75,'C2':.86}[level]
    add_ex(ex('T16_'+safe(lid),'TEACH',[lid],level,max(.30,diff-.18),
              f'Aprende una palabra {level}: {lemma} significa {meaning}.',lemma,
              f'{lemma} significa {meaning}.',lemma,19,('vocabulary','teach','contexto-pack','v16-b2c2')))
    add_ex(ex('X16_'+safe(lid),'SELF_ASSESS',[lid],level,diff,
              f'Escucha {lemma}. Recupera el significado y construye mentalmente un uso nuevo apropiado para nivel {level}.',lemma,
              f'Significado objetivo: {meaning}. Evita limitarte a traducir: úsala en un contexto nuevo.',lemma,26,
              ('vocabulary','transfer','lexical-transfer-v12','contexto-pack','v16-b2c2')))
    return 'added'

st={'added':0,'enriched':0,'unsupported':0}
for i,r in enumerate(rows): st[add_one(i,r)]+=1
order={'Pre-A1':0,'A1':1,'A2':2,'B1':3,'B2':4,'C1':5,'C2':6}
lex.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('frequencyRank') if isinstance(x.get('frequencyRank'),int) else 10**9,x['lemma'].casefold()))
kcs.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('domain',''),x['id']))
exs.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('type',''),x['id']))
save('lexemes.json',lex);save('kcs.json',kcs);save('edges.json',edges);save('exercises.json',exs)
man['version']='1.6.0'; man['contentVersion']='1.6.0'
man['description']='SIAA v1.6: B2–C2 ampliados con entradas Contexto pack de alta confianza validadas contra CEFR-J/Open Language Profiles.'
man['coverage']={'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exs),'lexemes':len(lex),
                 'generatedVariantsEstimate':sum(3+min(3,len(x.get('chunks',[]))) for x in lex),'levels':['Pre-A1','A1','A2','B1','B2','C1','C2']}
src=list(man.get('sources',[]))
for q in ['Contexto Spanish language pack (selected open-data pack) — selected high-confidence entries','CEFR-J / Open Language Profiles — CEFR validation']:
    if q not in src: src.append(q)
man['sources']=src
save('manifest.json',man)
man['checksums']={n:hashlib.sha256((CONTENT/n).read_bytes()).hexdigest() for n in ('kcs.json','edges.json','exercises.json','lexemes.json')}
save('manifest.json',man)
print(json.dumps({**st,'lexemes':len(lex),'kcs':len(kcs),'exercises':len(exs),'generatedEstimate':man['coverage']['generatedVariantsEstimate']},ensure_ascii=False,indent=2))
