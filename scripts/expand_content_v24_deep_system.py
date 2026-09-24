#!/usr/bin/env python3
from __future__ import annotations
import collections, hashlib, json, math, re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'
PRACTICE=ROOT/'app/src/main/assets/practice'
PRACTICE.mkdir(parents=True,exist_ok=True)

kcs=json.loads((CONTENT/'kcs.json').read_text(encoding='utf-8'))
edges=json.loads((CONTENT/'edges.json').read_text(encoding='utf-8'))
ex=json.loads((CONTENT/'exercises.json').read_text(encoding='utf-8'))
lex=json.loads((CONTENT/'lexemes.json').read_text(encoding='utf-8'))

kid={x['id'] for x in kcs}; qid={x['id'] for x in ex}; lid={x['id'] for x in lex}
by_lemma={x['lemma'].casefold():x for x in lex}
levels=['Pre-A1','A1','A2','B1','B2','C1','C2']
level_order={x:i for i,x in enumerate(levels)}

def slug(s):
    s=re.sub(r'[^a-z0-9]+','_',s.casefold()).strip('_')
    return s[:64]

def add_kc(obj):
    if obj['id'] in kid:return False
    kcs.append(obj);kid.add(obj['id']);return True

def add_ex(obj):
    if obj['id'] in qid:return False
    ex.append(obj);qid.add(obj['id']);return True

def add_lex(obj):
    if obj['id'] in lid:return False
    lex.append(obj);lid.add(obj['id']);by_lemma.setdefault(obj['lemma'].casefold(),obj);return True

# --- semantic groups used for genuinely competitive lexical distractors ---
SEMANTIC_GROUPS={
'people': 'man woman boy girl child mother father parent brother sister friend teacher student doctor worker customer manager person people baby family husband wife'.split(),
'places': 'home house school office hospital hotel station airport restaurant shop store bank park street room bathroom kitchen bedroom city town country place'.split(),
'transport': 'bus train car taxi bicycle bike plane flight station airport ticket platform stop road street travel trip journey'.split(),
'food': 'food water coffee tea milk bread rice meat fish fruit apple banana breakfast lunch dinner meal restaurant'.split(),
'actions': 'go come stop start open close sit stand listen look read write speak say tell ask help want need like love work play walk run buy pay give take bring get find know learn understand remember forget'.split(),
'qualities': 'good bad big small hot cold happy sad tired hungry thirsty easy difficult important new old young fast slow early late right wrong'.split(),
'time': 'time day week month year today tomorrow yesterday morning afternoon evening night hour minute second'.split(),
'numbers': 'zero one two three four five six seven eight nine ten eleven twelve twenty hundred thousand number'.split(),
'directions': 'here there left right up down inside outside near far north south east west front back'.split(),
'communication': 'name phone email address question answer word sentence letter number hello goodbye please thanks sorry yes no'.split(),
'workstudy': 'work job office school class course lesson test exam book paper project meeting task report study research'.split(),
'body': 'head face eye ear mouth hand arm leg foot heart body hair'.split(),
'clothes': 'shirt shoe shoes coat jacket dress trousers pants hat clothes'.split(),
'home': 'door window table chair bed room kitchen bathroom house home key light'.split(),
'money': 'money price cost bill pay buy sell cheap expensive bank card cash'.split(),
}
word_domain={w:d for d,ws in SEMANTIC_GROUPS.items() for w in ws}

# POS recovery for previously unlabelled common entries.
verbs=set('ask begin bring buy call close come do drive eat feel find forget get give go help keep know learn like listen live look love make mean meet need open pay play put read remember run say see sell send show sit speak stand start stop take talk tell think try understand use wait walk want watch wear work write'.split())
adjs=set('bad big cold difficult easy good happy hot hungry important late left new old right sad small sorry thirsty tired wrong young'.split())
advs=set('always here there today tomorrow yesterday now never often sometimes usually again already still soon together away back'.split())
function_pos={
 'yes':'expression','no':'expression','please':'expression','hello':'expression','goodbye':'expression','thanks':'expression','sorry':'expression',
 'one':'number','two':'number','three':'number','six':'number','seven':'number','nine':'number','ten':'number','zero':'number','eleven':'number','twelve':'number'
}

def pos_of(x):
    if x.get('pos'): return x['pos']
    for t in x.get('tags',[]):
        if t.startswith('pos:'): return t.split(':',1)[1]
    w=x['lemma'].casefold()
    if w in function_pos:return function_pos[w]
    if w in verbs:return 'verb'
    if w in adjs:return 'adjective'
    if w in advs:return 'adverb'
    m=(x.get('meaningEs') or '').strip().casefold()
    first=m.split('/')[0].split(',')[0].strip()
    if re.search(r'(ar|er|ir|arse|erse|irse)$',first) and ' ' not in first:return 'verb'
    if re.search(r'(mente)$',first):return 'adverb'
    return 'noun'

# Pre-A1 foundation. Existing A1 words are deliberately moved down when they are
# survival/foundation concepts; CEFR-J provenance remains in tags.
pre_core='''name water food bus train car house home school work family mother father man woman child boy girl four five eight morning afternoon evening night today tomorrow here right open close stop go come listen look read write speak say tell help want need good bad big small cold happy tired bathroom hotel station airport ticket money price phone number time day week book door table chair bed street city teacher student friend doctor coffee tea milk bread apple fish cat dog red blue green white black new old fast slow early late walk run buy pay give take bring get find know learn understand remember question answer word letter class room shop restaurant hospital park key bag map'''.split()
for w in pre_core:
    x=by_lemma.get(w)
    if x and x.get('cefr')=='A1':
        x['cefr']='Pre-A1'
        x['tags']=list(dict.fromkeys([*x.get('tags',[]),'pre-a1-core-v24']))
        for k in kcs:
            if k['id']==x['id']:
                k['cefr']='Pre-A1';k['tags']=list(dict.fromkeys([*k.get('tags',[]),'pre-a1-core-v24']))
        for q in ex:
            if x['id'] in q.get('kcIds',[]): q['cefr']='Pre-A1'

# Add missing survival vocabulary, including function words/numbers/directions.
NEW_PRE=[
('yes','sí','expression','communication'),('no','no','expression','communication'),('hello','hola','expression','communication'),('goodbye','adiós','expression','communication'),('please','por favor','expression','communication'),('thanks','gracias','expression','communication'),('sorry','lo siento / disculpa','expression','communication'),
('one','uno','number','numbers'),('two','dos','number','numbers'),('three','tres','number','numbers'),('six','seis','number','numbers'),('seven','siete','number','numbers'),('nine','nueve','number','numbers'),('ten','diez','number','numbers'),('zero','cero','number','numbers'),('eleven','once','number','numbers'),('twelve','doce','number','numbers'),
('there','allí','adverb','directions'),('left','izquierda','noun','directions'),('up','arriba','adverb','directions'),('down','abajo','adverb','directions'),('inside','dentro','adverb','directions'),('outside','fuera','adverb','directions'),('near','cerca','adverb','directions'),('far','lejos','adverb','directions'),
('sit','sentarse','verb','actions'),('stand','ponerse de pie','verb','actions'),('like','gustar','verb','actions'),('love','encantar / amar','verb','actions'),('hot','caliente','adjective','qualities'),('sad','triste','adjective','qualities'),('hungry','hambriento','adjective','qualities'),('thirsty','sediento','adjective','qualities'),
('toilet','baño','noun','places'),('exit','salida','noun','places'),('entrance','entrada','noun','places'),('help','ayuda','noun','communication'),('police','policía','noun','people'),('emergency','emergencia','noun','communication'),('passport','pasaporte','noun','travel'),('address','dirección','noun','communication'),('email','correo electrónico','noun','communication'),
('Monday','lunes','noun','time'),('Tuesday','martes','noun','time'),('Wednesday','miércoles','noun','time'),('Thursday','jueves','noun','time'),('Friday','viernes','noun','time'),('Saturday','sábado','noun','time'),('Sunday','domingo','noun','time'),
('January','enero','noun','time'),('February','febrero','noun','time'),('March','marzo','noun','time'),('April','abril','noun','time'),('May','mayo','noun','time'),('June','junio','noun','time'),('July','julio','noun','time'),('August','agosto','noun','time'),('September','septiembre','noun','time'),('October','octubre','noun','time'),('November','noviembre','noun','time'),('December','diciembre','noun','time'),
('first','primero','adjective','numbers'),('second','segundo','adjective','numbers'),('next','siguiente','adjective','directions'),('last','último','adjective','directions'),('same','mismo','adjective','qualities'),('different','diferente','adjective','qualities'),
('again','otra vez','adverb','communication'),('slowly','despacio','adverb','communication'),('now','ahora','adverb','time'),('okay','de acuerdo','expression','communication'),('ready','listo','adjective','qualities'),
('where','dónde','expression','communication'),('what','qué','expression','communication'),('who','quién','expression','communication'),('when','cuándo','expression','communication'),('how','cómo','expression','communication'),('why','por qué','expression','communication'),
('cash','efectivo','noun','money'),('card','tarjeta','noun','money'),('cheap','barato','adjective','money'),('expensive','caro','adjective','money'),('menu','menú','noun','food'),('bill','cuenta','noun','money'),('seat','asiento','noun','transport'),('platform','andén','noun','transport'),('flight','vuelo','noun','transport'),('taxi','taxi','noun','transport'),('bike','bicicleta','noun','transport'),
('shirt','camisa','noun','clothes'),('shoe','zapato','noun','clothes'),('jacket','casaca / chaqueta','noun','clothes'),('coat','abrigo','noun','clothes'),('hat','sombrero','noun','clothes'),
('eye','ojo','noun','body'),('ear','oído / oreja','noun','body'),('mouth','boca','noun','body'),('hand','mano','noun','body'),('foot','pie','noun','body'),('head','cabeza','noun','body'),
]

def add_vocab(lemma,meaning,level,pos,domain,suffix=''):
    base=f"V24_{slug(lemma).upper()}{suffix}"
    n=2; ident=base
    while ident in kid or ident in lid:
        ident=f'{base}_{n}'; n+=1
    distract='otra opción'
    kc={'id':ident,'name':lemma,'cefr':level,'domain':'VOCABULARY','form':lemma,'meaning':meaning,'use':f'comprender y usar «{lemma}» en contexto','importance':0.78 if level=='Pre-A1' else 0.66,'priorMastery':0.05 if level=='Pre-A1' else 0.10,'tags':['v24','lexical-sense',f'pos:{pos}',f'sem:{domain}','original-authoring']}
    lx={'id':ident,'lemma':lemma,'meaningEs':meaning,'cefr':level,'frequencyRank':None,'spellingDifficulty':0.24 if len(lemma)<=6 else 0.36,'distractorEs':distract,'chunks':[],'exampleFrames':[],'tags':['v24','lexical-sense',f'pos:{pos}',f'sem:{domain}','original-authoring']}
    add_kc(kc); add_lex(lx)
    add_ex({'id':f'T24_{ident}','type':'TEACH','kcIds':[ident],'cefr':level,'difficulty':0.16,'promptEs':f'Escucha esta palabra: {meaning}.','stimulusEn':lemma,'optionA':'','optionB':'','correctOption':'','explanationEs':f'{lemma} significa {meaning}.','spellTarget':lemma,'estimatedSeconds':14,'tags':['v24','lexical-teach','audio-asset'],'misconceptionIds':[]})
    add_ex({'id':f'X24_{ident}','type':'SELF_ASSESS','kcIds':[ident],'cefr':level,'difficulty':0.34,'promptEs':f'Usa mentalmente «{lemma}» en una frase corta distinta de los ejemplos.','stimulusEn':lemma,'optionA':'','optionB':'','correctOption':'','explanationEs':f'Comprueba que «{lemma}» expresa {meaning}.','spellTarget':lemma,'estimatedSeconds':18,'tags':['v24','lexical-transfer-v12','transfer','novel-context'],'misconceptionIds':[]})
    return ident

for lemma,meaning,pos,dom in NEW_PRE:
    if lemma.casefold() not in by_lemma:
        add_vocab(lemma,meaning,'Pre-A1',pos,dom)

# Explicit sense-level modelling for polysemous high-value forms.
SENSES=[
('bank','orilla de un río','B1','noun','places'),('light','luz','A1','noun','home'),('light','ligero','A2','adjective','qualities'),('right','derecho / derecho legal','B1','noun','general'),('work','trabajo / empleo','A1','noun','workstudy'),('play','obra de teatro','B1','noun','workstudy'),('watch','reloj de pulsera','A1','noun','home'),('book','reservar','A2','verb','actions'),('room','espacio / margen','B1','noun','general'),('mean','significar','A2','verb','communication'),('mean','promedio','B2','noun','general'),('kind','tipo / clase','A2','noun','general'),('order','orden / secuencia','A2','noun','general'),('order','pedir / ordenar','A2','verb','actions'),('change','cambio','A2','noun','general'),('change','cambiar','A2','verb','actions'),('break','descanso','A2','noun','workstudy'),('break','romper','A2','verb','actions'),('set','conjunto','B1','noun','general'),('set','establecer / fijar','B1','verb','actions'),('run','gestionar / operar','B2','verb','actions'),('point','punto / aspecto','B1','noun','general'),('point','señalar','B1','verb','actions'),('case','caso / situación','B1','noun','general'),('issue','asunto / problema','B1','noun','general'),('matter','asunto','B1','noun','general'),('matter','importar','B1','verb','actions'),('charge','cargo / tarifa','B2','noun','money'),('charge','cobrar','B2','verb','money'),('current','corriente / flujo','B2','noun','general'),('current','actual','B1','adjective','qualities'),('file','archivo','A2','noun','workstudy'),('file','presentar / archivar','B2','verb','workstudy'),('key','clave / fundamental','B1','adjective','qualities'),('field','campo / área','B1','noun','workstudy'),('line','línea / fila','A2','noun','general'),('term','término','B1','noun','communication'),('subject','tema','B1','noun','workstudy'),('subject','sujeto gramatical','B1','noun','workstudy'),('object','objeto','A1','noun','general'),('object','oponerse','C1','verb','communication'),('capital','capital / ciudad principal','A2','noun','places'),('capital','capital financiero','B2','noun','money'),('interest','interés / curiosidad','B1','noun','general'),('interest','interés financiero','B2','noun','money'),('support','apoyo','B1','noun','general'),('support','apoyar','B1','verb','actions'),('address','dirección postal','A1','noun','communication'),('address','abordar un problema','B2','verb','communication'),('scale','escala','B2','noun','general'),('figure','cifra','B1','noun','numbers'),('figure','deducir / resolver','B2','verb','actions'),('record','registro','B1','noun','workstudy'),('record','grabar / registrar','B1','verb','actions'),('present','presente / actualidad','A2','noun','time'),('present','presentar','B1','verb','communication'),('close','cercano','B1','adjective','qualities'),('sound','sonido','A2','noun','general'),('sound','parecer / sonar','B1','verb','communication'),('spring','primavera','A2','noun','time'),('spring','resorte','B2','noun','general'),('match','partido','A2','noun','general'),('match','coincidir','B1','verb','actions'),('date','fecha','A1','noun','time'),('date','cita romántica','B1','noun','general'),('fair','justo','B1','adjective','qualities'),('fair','feria','B1','noun','places'),('face','cara','A1','noun','body'),('face','afrontar','B2','verb','actions'),('head','cabeza','A1','noun','body'),('head','dirigir / encabezar','B2','verb','actions'),('hand','mano','A1','noun','body'),('hand','entregar','B1','verb','actions'),('back','espalda','A1','noun','body'),('back','respaldar','B2','verb','actions'),('course','curso','A2','noun','workstudy'),('course','rumbo','B2','noun','general'),('state','estado / condición','B1','noun','general'),('state','declarar','B2','verb','communication'),('form','forma','A2','noun','general'),('form','formar','B1','verb','actions'),('account','cuenta','A2','noun','money'),('account','explicar / representar','C1','verb','communication'),('party','fiesta','A1','noun','general'),('party','parte / actor involucrado','C1','noun','general'),('report','informe','B1','noun','workstudy'),('report','informar','B1','verb','communication'),('position','posición / postura','B1','noun','general'),('level','nivel','A2','noun','general'),('level','nivelar','B2','verb','actions')]
existing_senses={(x['lemma'].casefold(),x.get('meaningEs','').casefold()) for x in lex}
for lemma,meaning,level,pos,dom in SENSES:
    if (lemma.casefold(),meaning.casefold()) not in existing_senses:
        ident=add_vocab(lemma,meaning,level,pos,dom,suffix='_SENSE')
        existing_senses.add((lemma.casefold(),meaning.casefold()))

# Enrich the entire lexicon with explicit sense/POS/context metadata.
for i,x in enumerate(lex):
    pos=pos_of(x)
    tags=[t for t in x.get('tags',[]) if not t.startswith('pos:')]
    tags=list(dict.fromkeys([*tags,f'pos:{pos}']))
    x['tags']=tags
    x['pos']=pos
    x['senseId']=x.get('senseId') or f"{x['lemma'].casefold()}#{slug(x.get('meaningEs','sense'))[:28]}"
    x['register']=x.get('register') or 'neutral'
    x['semanticDomain']=x.get('semanticDomain') or next((t.split(':',1)[1] for t in tags if t.startswith('sem:')),word_domain.get(x['lemma'].casefold(),'general'))
    x['audioForm']=x.get('audioForm') or x['lemma']
    if not x.get('exampleFrames'):
        lemma=x['lemma'];p=pos
        if p=='verb': frames=[f"They {lemma} every day.",f"I want to {lemma}."]
        elif p=='adjective': frames=[f"It is {lemma}.",f"That seems {lemma}."]
        elif p=='adverb': frames=[f"It happens {lemma}."]
        elif p=='number': frames=[f"I need {lemma} tickets."]
        elif p=='expression': frames=[f"{lemma.capitalize()}!"]
        else: frames=[f"This is the {lemma}.",f"I need the {lemma}."]
        x['exampleFrames']=frames
    if not x.get('chunks'):
        if pos=='verb': x['chunks']=[f'to {x["lemma"]}']
        elif pos=='adjective': x['chunks']=[f'very {x["lemma"]}']
        elif pos=='noun': x['chunks']=[f'the {x["lemma"]}']
    # mirror enriched lexical metadata in KC tags.
    kc=next((k for k in kcs if k['id']==x['id']),None)
    if kc:
        kc['tags']=list(dict.fromkeys([*kc.get('tags',[]),f'pos:{pos}',f'sem:{x["semanticDomain"]}','sense-aware-v24']))

# Hard distractors are chosen from same level + POS + semantic domain first, then same level+POS.
WEAK_DISTRACTORS={'respuesta','opción','otra respuesta','otra opción','una opción diferente','opción incorrecta'}
def candidates_for(x):
    same=[y for y in lex if y['id']!=x['id'] and y['cefr']==x['cefr'] and y.get('pos')==x.get('pos') and y.get('semanticDomain')==x.get('semanticDomain') and y.get('meaningEs','').casefold()!=x.get('meaningEs','').casefold() and y.get('meaningEs','').strip().casefold() not in WEAK_DISTRACTORS]
    if len(same)<3:
        same += [y for y in lex if y['id']!=x['id'] and y['cefr']==x['cefr'] and y.get('pos')==x.get('pos') and y.get('meaningEs','').casefold()!=x.get('meaningEs','').casefold() and y.get('meaningEs','').strip().casefold() not in WEAK_DISTRACTORS and y not in same]
    return same
for x in lex:
    cand=candidates_for(x)
    if cand:
        start=int(hashlib.sha1(x['id'].encode()).hexdigest()[:8],16)%len(cand)
        ds=[]
        for j in range(min(3,len(cand))):
            m=cand[(start+j)%len(cand)]['meaningEs']
            if m.casefold()!=x['meaningEs'].casefold() and m not in ds:ds.append(m)
        if ds:
            x['hardDistractorsEs']=ds
            x['distractorEs']=ds[0]

# Reference pools by level for integrated listening/transfer.
def ids_by(domain,level):
    return [x['id'] for x in kcs if x['domain']==domain and x['cefr']==level]
def vocab_by(level):
    return [x['id'] for x in kcs if x['domain']=='VOCABULARY' and x['cefr']==level]

# Original listening scenarios. 70/level => +490 items. These are deliberately
# varied in function, detail, inference and discourse load by CEFR level.
names=['Ana','Leo','Mia','Omar','Sara','Noah','Lina','Hugo','Eva','Tom']
places=['station','library','cafe','hotel','office','school','clinic','market','museum','airport']
reasons=['the bus is delayed','the room is closed','the meeting moved','it started raining','the payment failed','the train is full','the teacher is absent','the road is blocked','the system is offline','the order is late']
plans=['meet at the entrance','take the next bus','call after lunch','work from home','use the side door','leave ten minutes earlier','send the file tonight','move the meeting online','wait near reception','buy the ticket there']

def listen_case(level,i):
    n=names[i%len(names)];p=places[(i*3)%len(places)];r=reasons[(i*7)%len(reasons)];pl=plans[(i*5+2)%len(plans)]
    a=8+(i%11); b=(i*3)%60; t=f'{a}:{b:02d}'
    if level=='Pre-A1':
        text=f"{n}: Hello. The {p} is here. Go to number {1+i%9}."
        return text,'¿Qué número debe buscar?',str(1+i%9),str(1+(i+4)%9)
    if level=='A1':
        text=f"{n}: Meet me at the {p} at {t}. Please bring your ticket."
        return text,'¿Dónde deben encontrarse?',p,next(x for x in places if x != p and x != places[(i+1)%len(places)])
    if level=='A2':
        text=f"{n}: We planned to {plans[i%len(plans)]}, but {r}. So we will {pl}."
        return text,'¿Qué harán finalmente?',pl,plans[(i+3)%len(plans)]
    if level=='B1':
        text=f"{n}: I thought the {p} would be quiet, but {r}. I still need to finish the task, so I will {pl}."
        return text,'¿Por qué cambia su plan?',r,next(x for x in reasons if x != r)
    if level=='B2':
        text=f"{n}: The proposal would save time, although it may increase costs at first. Given that {r}, I would test it on a small scale before deciding whether to {pl}."
        return text,'¿Qué postura adopta?', 'probar primero antes de decidir','rechazar la propuesta inmediatamente'
    if level=='C1':
        text=f"{n}: While the initial figures appear encouraging, they do not isolate the effect of the policy from the fact that {r}. I would therefore treat the result as provisional and {pl}."
        return text,'¿Cómo interpreta la evidencia?', 'como prometedora pero provisional','como prueba definitiva de causalidad'
    text=f"{n}: The committee called the outcome ‘unexpectedly reassuring’, which is an elegant way of saying the forecast missed the problem until {r}. Even so, the report recommends that we {pl}, rather than pretending the uncertainty has disappeared."
    return text,'¿Qué implica el comentario?', 'critica con ironía la confianza del pronóstico','afirma que el pronóstico fue completamente exacto'

for level in levels:
    ls=ids_by('LISTENING',level); prag=ids_by('PRAGMATICS',level); chunks=ids_by('CHUNK',level); voc=vocab_by(level); grams=ids_by('GRAMMAR',level)
    support=(prag+chunks+voc+grams) or ls
    for i in range(70):
        text,prompt,correct,wrong=listen_case(level,i)
        k1=ls[i%len(ls)] if ls else support[i%len(support)]
        k2=support[(i*5+1)%len(support)]
        k3=support[(i*11+3)%len(support)] if len(support)>2 else k2
        kcids=list(dict.fromkeys([k1,k2,k3]))
        flip=(i%2)==1
        add_ex({'id':f'L24_{slug(level).upper()}_{i+1:03d}','type':'LISTENING_AB','kcIds':kcids,'cefr':level,'difficulty':min(0.94,0.20+0.10*level_order[level]+0.006*(i%10)),'promptEs':prompt,'stimulusEn':text,'optionA':wrong if flip else correct,'optionB':correct if flip else wrong,'correctOption':'B' if flip else 'A','explanationEs':f'La respuesta se infiere del mensaje: {correct}.','spellTarget':'','estimatedSeconds':18+level_order[level]*5,'tags':['v24','deep-listening','audio-asset','transfer','multi-kc','novel-context',f'level:{level}'],'misconceptionIds':[]})

# Pronunciation/perception expansion: 30 per level, including connected speech and prosody.
PAIR_BANK={
'Pre-A1':[('bat','pat'),('map','nap'),('sip','zip'),('sit','seat'),('big','pig')],
'A1':[('berry','very'),('ship','sheep'),('bed','bad'),('coat','code'),('rice','rise'),('cheap','chip')],
'A2':[('think','sink'),('then','den'),('ship','chip'),('west','vest'),("can","can't"),('to','two')],
'B1':[('writer','rider'),('ice cream','I scream'),('bread and butter','bread butter'),('desert','dessert'),('turn off','turn on')],
'B2':[('green park','green bark'),('next day','next stay'),('an aim','a name'),('could have','could of'),('used to','use to')],
'C1':[('I did say it.','I did not say it.'),('What I meant was...','What I said was...'),('could have been','could not have been'),('not entirely','not at all'),('apparently','clearly')],
'C2':[('That was helpful.','That was not helpful.'),('I suppose so.','Absolutely.'),('hardly surprising','very surprising'),('to put it mildly','literally'),('ostensibly','obviously')]
}
for level in levels:
    ph=ids_by('PHONOLOGY',level); pairs=PAIR_BANK[level]
    for i in range(30):
        a,b=pairs[i%len(pairs)]; heard=a if i%2==0 else b
        flip=(i//2)%2==1
        oa,ob=(b,a) if flip else (a,b)
        corr='B' if (flip and heard==a) or ((not flip) and heard==b) else 'A'
        prompt='Escucha con atención. ¿Qué forma o interpretación corresponde mejor al audio?'
        add_ex({'id':f'P24_{slug(level).upper()}_{i+1:03d}','type':'PRON_DISCRIMINATION','kcIds':[ph[i%len(ph)]] if ph else [],'cefr':level,'difficulty':min(.95,.18+.11*level_order[level]+.01*(i%5)),'promptEs':prompt,'stimulusEn':heard,'optionA':oa,'optionB':ob,'correctOption':corr,'explanationEs':f'La forma objetivo era «{heard}».','spellTarget':'','estimatedSeconds':14+level_order[level]*2,'tags':['v24','pronunciation','audio-asset','perception','transfer'],'misconceptionIds':[]})

# Integrated multi-KC transfer beyond single-skill rephrasing: +210.
for level in levels:
    pools={d:ids_by(d,level) for d in ['GRAMMAR','VOCABULARY','PRAGMATICS','CHUNK','LISTENING']}
    merged=[x for d in pools.values() for x in d]
    if len(merged)<3: continue
    for i in range(30):
        text,prompt,correct,wrong=listen_case(level,100+i)
        kcids=[]
        for d,offset in [('GRAMMAR',1),('VOCABULARY',3),('PRAGMATICS',5)]:
            arr=pools[d]
            if arr:kcids.append(arr[(i*offset)%len(arr)])
        if len(kcids)<2: kcids=list(dict.fromkeys([merged[i%len(merged)],merged[(i*7+2)%len(merged)],merged[(i*13+4)%len(merged)]]))
        flip=i%2==0
        add_ex({'id':f'XFER24_{slug(level).upper()}_{i+1:03d}','type':'AB','kcIds':list(dict.fromkeys(kcids)),'cefr':level,'difficulty':min(.96,.28+.09*level_order[level]+.008*(i%8)),'promptEs':'Integra forma, significado y contexto. '+prompt,'stimulusEn':text,'optionA':wrong if flip else correct,'optionB':correct if flip else wrong,'correctOption':'B' if flip else 'A','explanationEs':'La respuesta exige combinar varias pistas lingüísticas del contexto.','spellTarget':'','estimatedSeconds':22+level_order[level]*4,'tags':['v24','integrated-transfer','transfer','multi-kc','novel-context'],'misconceptionIds':[]})

# Placement bank: objective items spread across levels and domains. Existing items are tagged,
# plus explicit lexical meaning checks so vocabulary is directly observed.
for level in levels:
    eligible=[q for q in ex if q.get('cefr')==level and q.get('type') in {'AB','LISTENING_AB','PRON_DISCRIMINATION','SPELLING_AB','MEANING_AB','CHUNK_AB'}]
    # deterministic broad sample, max 32 existing items per level
    eligible=sorted(eligible,key=lambda q:hashlib.sha1(q['id'].encode()).hexdigest())[:32]
    for q in eligible:q['tags']=list(dict.fromkeys([*q.get('tags',[]),'placement']))
    level_lex=[x for x in lex if x['cefr']==level and x.get('hardDistractorsEs')]
    level_lex=sorted(level_lex,key=lambda x:hashlib.sha1(x['id'].encode()).hexdigest())[:12]
    for i,x in enumerate(level_lex):
        d=x['hardDistractorsEs'][0]; flip=i%2==1
        add_ex({'id':f'PLAC24_{slug(level).upper()}_{i+1:03d}','type':'MEANING_AB','kcIds':[x['id']],'cefr':level,'difficulty':min(.95,.15+.12*level_order[level]+.01*(i%4)),'promptEs':f'¿Qué significa «{x["lemma"]}» en este sentido?','stimulusEn':x['lemma'],'optionA':d if flip else x['meaningEs'],'optionB':x['meaningEs'] if flip else d,'correctOption':'B' if flip else 'A','explanationEs':f'En este sentido, {x["lemma"]} = {x["meaningEs"]}.','spellTarget':'','estimatedSeconds':13,'tags':['v24','placement','objective','lexical-sense'],'misconceptionIds':[]})

# --- Extended practice catalog: speaking / reading / writing ---
# Uses current KCs so objective production evidence can feed the same learner model.
practice={'version':'2.4.0','speaking':[],'reading':[],'writing':[]}
for level in levels:
    level_kcs=[x['id'] for x in kcs if x['cefr']==level]
    lvlex=[x for x in lex if x['cefr']==level]
    if not level_kcs: continue
    # 30 speaking/shadowing targets per level
    for i in range(30):
        lx=lvlex[i%len(lvlex)] if lvlex else None
        if lx:
            frame=(lx.get('exampleFrames') or [f'I use {lx["lemma"]}.'])[i%len(lx.get('exampleFrames') or [1])]
            target=frame
            kcids=[lx['id']]
        else:
            target=listen_case(level,i)[0].split(': ',1)[-1]
            kcids=[level_kcs[i%len(level_kcs)]]
        practice['speaking'].append({'id':f'SPK24_{slug(level).upper()}_{i+1:03d}','cefr':level,'promptEs':'Escucha/lee el modelo y repítelo con tus propias palabras o mediante shadowing.','targetEn':target,'kcIds':kcids,'minSimilarity':round(0.58+0.04*level_order[level],2),'tags':['v24','speaking','shadowing','objective-production']})
    # 20 reading comprehension items per level
    for i in range(20):
        text,prompt,correct,wrong=listen_case(level,200+i)
        flip=i%2==1
        practice['reading'].append({'id':f'READ24_{slug(level).upper()}_{i+1:03d}','cefr':level,'passage':text,'questionEs':prompt,'optionA':wrong if flip else correct,'optionB':correct if flip else wrong,'correctOption':'B' if flip else 'A','kcIds':[level_kcs[(i*3)%len(level_kcs)]],'tags':['v24','reading','objective']})
    # 20 writing prompts per level with explicit rubric anchors.
    for i in range(20):
        lx1=lvlex[(i*3)%len(lvlex)] if lvlex else None; lx2=lvlex[(i*7+1)%len(lvlex)] if lvlex else None
        words=[x['lemma'] for x in [lx1,lx2] if x]
        min_words=4+level_order[level]*4
        ref=f"Write about {listen_case(level,300+i)[2]}." if not words else f"Use {' and '.join(words)} in a clear message."
        practice['writing'].append({'id':f'WRITE24_{slug(level).upper()}_{i+1:03d}','cefr':level,'promptEs':f'Escribe al menos {min_words} palabras en inglés. {ref}','referenceEn':ref,'keywords':words,'minWords':min_words,'kcIds':[x['id'] for x in [lx1,lx2] if x] or [level_kcs[i%len(level_kcs)]],'tags':['v24','writing','productive']})

(PRACTICE/'practice_catalog.json').write_text(json.dumps(practice,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Manifest/checksums are finalized after all JSON writes.
for name,obj in [('kcs.json',kcs),('edges.json',edges),('exercises.json',ex),('lexemes.json',lex)]:
    (CONTENT/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
manifest=json.loads((CONTENT/'manifest.json').read_text(encoding='utf-8'))
manifest['version']=manifest['contentVersion']='2.4.0'
manifest['schemaVersion']='1.1'
manifest['description']='SIAA v2.4 Deep Learning System: placement adaptativo, léxico por sentidos, distractores competitivos, transferencia multi-KC, expansión intensiva de listening/pronunciación y catálogo productivo speaking/reading/writing.'
manifest.setdefault('sources',[]).extend([
 'SIAA original sense-aware lexical authoring v2.4',
 'SIAA original integrated listening/transfer authoring v2.4',
 'SIAA original extended-skills practice catalog v2.4'
])
manifest['sources']=list(dict.fromkeys(manifest['sources']))
manifest['checksums']={n:hashlib.sha256((CONTENT/n).read_bytes()).hexdigest() for n in ['kcs.json','edges.json','exercises.json','lexemes.json']}
counts=collections.Counter(x['cefr'] for x in ex if x['type']=='LISTENING_AB')
pron=collections.Counter(x['cefr'] for x in ex if x['type']=='PRON_DISCRIMINATION')
manifest['coverage']={
 'kcs':len(kcs),'edges':len(edges),'staticExercises':len(ex),'lexemes':len(lex),
 'preA1Lexemes':sum(x['cefr']=='Pre-A1' for x in lex),
 'senseAwareLexemes':sum(bool(x.get('senseId')) for x in lex),
 'hardDistractorLexemes':sum(bool(x.get('hardDistractorsEs')) for x in lex),
 'exampleEnrichedLexemes':sum(bool(x.get('exampleFrames')) for x in lex),
 'multiKcExercises':sum(len(x.get('kcIds',[]))>1 for x in ex),
 'placementItems':sum('placement' in x.get('tags',[]) for x in ex),
 'listeningItems':sum(x['type']=='LISTENING_AB' for x in ex),
 'pronunciationItems':sum(x['type']=='PRON_DISCRIMINATION' for x in ex),
 'practiceSpeaking':len(practice['speaking']),'practiceReading':len(practice['reading']),'practiceWriting':len(practice['writing']),
 'levels':levels
}
manifest['practiceCatalog']={'path':'practice/practice_catalog.json','sha256':hashlib.sha256((PRACTICE/'practice_catalog.json').read_bytes()).hexdigest()}
(CONTENT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'kcs':len(kcs),'exercises':len(ex),'lexemes':len(lex),'preA1Lexemes':sum(x['cefr']=='Pre-A1' for x in lex),'listening':dict(counts),'pronunciation':dict(pron),'multiKc':sum(len(x.get('kcIds',[]))>1 for x in ex),'placement':sum('placement' in x.get('tags',[]) for x in ex),'practice':{k:len(v) for k,v in practice.items() if isinstance(v,list)}},ensure_ascii=False,indent=2))
