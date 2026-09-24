#!/usr/bin/env python3
from __future__ import annotations
import json, hashlib, re
from pathlib import Path
from collections import Counter

ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'

def load(n): return json.loads((CONTENT/n).read_text(encoding='utf-8'))
def save(n,o): (CONTENT/n).write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def safe(s): return re.sub(r'[^A-Z0-9]+','_',s.upper()).strip('_') or 'X'
def kc(id,name,cefr,domain,form,meaning,use,importance=.67,prior=.035,tags=()):
    return {'id':id,'name':name,'cefr':cefr,'domain':domain,'form':form,'meaning':meaning,'use':use,
            'importance':importance,'priorMastery':prior,'tags':list(tags)}
def ex(id,type,kcs,cefr,diff,prompt,stim='',a='',b='',correct='',exp='',spell='',seconds=22,tags=(),mis=()):
    return {'id':id,'type':type,'kcIds':kcs,'cefr':cefr,'difficulty':diff,'promptEs':prompt,'stimulusEn':stim,
            'optionA':a,'optionB':b,'correctOption':correct,'explanationEs':exp,'spellTarget':spell,
            'estimatedSeconds':seconds,'tags':list(tags),'misconceptionIds':list(mis)}
def edge(a,b,w=.04,hard=False): return {'fromId':a,'toId':b,'weight':w,'hardPrerequisite':hard}
def choose(key,good,bad):
    if int(hashlib.sha1(key.encode()).hexdigest()[-1],16)%2==0: return good,bad,'A'
    return bad,good,'B'
def parse(block):
    out=[]
    for raw in block.strip().splitlines():
        raw=raw.strip()
        if not raw or raw.startswith('#'): continue
        p=[x.strip() for x in raw.split('|')]
        if len(p)!=4: raise ValueError(raw)
        out.append(p)
    return out

BANK={
'A1':parse(r'''
wake up|despertarse|I wake up at six on weekdays.|routine
get dressed|vestirse|I get dressed after breakfast.|routine
have lunch|almorzar|We have lunch at one o'clock.|routine
have dinner|cenar|They have dinner together in the evening.|routine
go shopping|ir de compras|We go shopping on Saturday morning.|routine
go for a walk|salir a caminar|I go for a walk after dinner.|routine
sit down|sentarse|Please sit down and listen.|classroom-formula
stand up|ponerse de pie|Please stand up for a moment.|classroom-formula
come in|entrar|Come in and close the door, please.|classroom-formula
write down|anotar|Write down your name and phone number.|classroom-formula
what time|a qué hora|What time does the film start?|question-formula
how often|con qué frecuencia|How often do you exercise?|question-formula
I'd like|quisiera|I'd like a coffee, please.|service-formula
here you are|aquí tiene / aquí tienes|Here you are. That's five dollars.|service-formula
have a good day|que tenga un buen día|Thank you. Have a good day.|pragmatic-formula
nice to meet you|mucho gusto|Nice to meet you. I'm Ana.|pragmatic-formula
'''),
'A2':parse(r'''
call back|devolver la llamada|I'll call you back after the meeting.|phrasal-verb
fill in|rellenar|Please fill in this form before you leave.|phrasal-verb
check in|registrarse|We need to check in two hours before the flight.|phrasal-verb
check out|dejar el hotel / comprobar|We checked out of the hotel at noon.|phrasal-verb
hurry up|darse prisa|Hurry up or we'll miss the bus.|phrasal-verb
lie down|acostarse / tumbarse|You should lie down and rest for a while.|phrasal-verb
try on|probarse|Can I try on this jacket?|phrasal-verb
throw away|botar / tirar|Don't throw away the receipt.|phrasal-verb
look after|cuidar de|Can you look after the dog this weekend?|phrasal-verb
get back|regresar|We got back home after midnight.|phrasal-verb
be proud of|estar orgulloso de|She is proud of her final project.|adjective-pattern
be worried about|estar preocupado por|I'm worried about tomorrow's exam.|adjective-pattern
ask someone to|pedirle a alguien que|I asked him to send the document again.|verb-pattern
spend time|pasar tiempo|We spend a lot of time outdoors.|collocation
make a mistake|cometer un error|Everyone makes a mistake sometimes.|collocation
take a photo|tomar una foto|Could you take a photo of us?|collocation
'''),
'B1':parse(r'''
bring up|mencionar / sacar un tema|She brought up the budget during the meeting.|phrasal-verb
come across|encontrarse con por casualidad|I came across an interesting article online.|phrasal-verb
get over|superar / recuperarse de|It took him a week to get over the flu.|phrasal-verb
put off|posponer|They put off the meeting until Friday.|phrasal-verb
take up|empezar una actividad / ocupar|She took up running last year.|phrasal-verb
get rid of|deshacerse de|We need to get rid of unnecessary files.|multiword-verb
look into|investigar|The team will look into the complaint.|phrasal-verb
break down|averiarse / descomponerse|Our car broke down on the way home.|phrasal-verb
calm down|calmarse|Take a minute to calm down before you answer.|phrasal-verb
make up|inventar / reconciliarse|He made up an excuse for being late.|phrasal-verb
keep track of|llevar un registro de|The app helps you keep track of your progress.|collocation
reach an agreement|llegar a un acuerdo|Both sides finally reached an agreement.|collocation
take advantage of|aprovechar|Students should take advantage of the extra practice.|collocation
make progress|progresar|You can make progress with short daily sessions.|collocation
pay attention to|prestar atención a|Pay attention to the final consonant.|collocation
have difficulty with|tener dificultad con|Many learners have difficulty with fast speech.|formula
'''),
'B2':parse(r'''
account for|explicar / representar|Transport accounts for a large share of the emissions.|phrasal-verb
back up|respaldar|The conclusion is backed up by two independent studies.|phrasal-verb
brush up on|repasar / refrescar|I need to brush up on my statistics before the course.|phrasal-verb
come down to|reducirse a|The final choice comes down to cost and reliability.|phrasal-verb
draw on|recurrir a / aprovechar|The report draws on several years of monitoring data.|phrasal-verb
get across|transmitir una idea|The speaker got the main point across clearly.|phrasal-verb
go over|revisar|Let's go over the results one more time.|phrasal-verb
hold back|contener / frenar|A lack of funding is holding the project back.|phrasal-verb
lay out|exponer / disponer|The introduction lays out the main argument.|phrasal-verb
live up to|estar a la altura de|The product did not live up to expectations.|phrasal-verb
make up for|compensar|The lower price does not make up for the poor durability.|phrasal-verb
rule out|descartar|The tests ruled out contamination as the main cause.|phrasal-verb
sum up|resumir|To sum up, the intervention reduced average travel time.|discourse-formula
take on|asumir / adquirir|She took on more responsibility after the promotion.|phrasal-verb
weigh up|sopesar|We need to weigh up the benefits and risks first.|phrasal-verb
reach a conclusion|llegar a una conclusión|It is too early to reach a firm conclusion.|collocation
'''),
'C1':parse(r'''
bear out|confirmar / respaldar|The follow-up data bear out the initial interpretation.|phrasal-verb
boil down to|reducirse esencialmente a|The disagreement boils down to how risk is defined.|phrasal-verb
hinge on|depender fundamentalmente de|The decision hinges on whether the evidence is reliable.|phrasal-verb
iron out|resolver dificultades|The teams met to iron out the remaining technical problems.|phrasal-verb
map out|trazar / planificar|The review maps out three plausible pathways for reform.|phrasal-verb
phase out|eliminar gradualmente|The company plans to phase out the older equipment.|phrasal-verb
play down|minimizar|The spokesperson played down the significance of the delay.|phrasal-verb
spell out|explicar con claridad|The protocol spells out exactly how samples must be handled.|phrasal-verb
stem from|derivarse de|Most of the uncertainty stems from missing observations.|phrasal-verb
usher in|dar paso a / inaugurar|The reform ushered in a new regulatory framework.|phrasal-verb
fall short of|no alcanzar|The available evidence falls short of proving causation.|advanced-formula
run counter to|contradecir / ir en contra de|That interpretation runs counter to the observed pattern.|advanced-formula
shed light on|arrojar luz sobre|The interviews shed light on why the policy failed locally.|collocation
draw a distinction between|establecer una distinción entre|The paper draws a distinction between exposure and vulnerability.|academic-formula
lend support to|respaldar|The sensitivity analysis lends support to the main finding.|academic-formula
raise questions about|plantear dudas sobre|The discrepancy raises questions about the original assumption.|academic-formula
'''),
'C2':parse(r'''
be predicated on|basarse en / estar condicionado por|The recommendation is predicated on a level of compliance that has not been demonstrated.|formal-formula
call into question|poner en duda|The new evidence calls the earlier interpretation into question.|academic-formula
take issue with|discrepar de / cuestionar|Several reviewers took issue with the way the comparison was framed.|academic-formula
hold sway|predominar / ejercer influencia|For decades, a simpler explanation held sway in the field.|idiomatic-formula
bring to bear|aplicar / poner en juego|The analysis brings several independent sources of evidence to bear on the question.|formal-formula
be at odds with|estar en contradicción con|The survey results are at odds with the administrative records.|advanced-formula
be premised on|basarse en|The argument is premised on an assumption that may not hold in practice.|formal-formula
lend itself to|prestarse a|The framework lends itself to comparison across very different settings.|advanced-formula
stand to reason|ser lógico|It stands to reason that repeated exposure will affect recall.|idiomatic-formula
have a bearing on|tener incidencia en|The sampling decision may have a bearing on the final estimate.|formal-formula
strike a balance between|lograr un equilibrio entre|The design must strike a balance between precision and usability.|academic-formula
err on the side of|pecar por exceso de / inclinarse por prudencia|When evidence is sparse, the protocol errs on the side of caution.|idiomatic-formula
put paid to|poner fin a / frustrar|The unexpected closure put paid to the original fieldwork plan.|idiomatic-formula
set against|contrastar con|Set against the long-term trend, the short-term increase appears less unusual.|rhetorical-formula
give rise to|dar lugar a|A poorly specified threshold can give rise to systematic classification errors.|formal-formula
notwithstanding the fact that|a pesar de que|Notwithstanding the fact that the sample was small, the pattern was remarkably stable.|formal-linker
''')
}

kcs=load('kcs.json'); edges=load('edges.json'); exercises=load('exercises.json'); lexemes=load('lexemes.json'); manifest=load('manifest.json')
kid={x['id'] for x in kcs}; eid={x['id'] for x in exercises}; edgekey={(x['fromId'],x['toId']) for x in edges}
existing_phrases={x['name'].casefold() for x in kcs if x.get('domain')=='CHUNK'}
lex_by_lemma={x['lemma'].casefold():x for x in lexemes}
added=[]

def add_edge(a,b,w=.04):
    if a in kid and b in kid and (a,b) not in edgekey:
        edges.append(edge(a,b,w,False)); edgekey.add((a,b))

for lvl,rows in BANK.items():
    meanings=[r[1] for r in rows]
    for i,(phrase,meaning,example,kind) in enumerate(rows):
        if phrase.casefold() in existing_phrases: continue
        cid='C21_'+safe(phrase)
        if cid in kid: continue
        kcs.append(kc(cid,phrase,lvl,'CHUNK',phrase,meaning,f'usar la unidad formulaica en contexto ({kind})',
                      .66 if lvl in ('A1','A2') else .72,.035,('multiword','formulaic',kind,'v21','original-authoring')))
        kid.add(cid); existing_phrases.add(phrase.casefold()); added.append((lvl,cid,phrase))
        wrong=meanings[(i*5+3)%len(meanings)]
        if wrong==meaning: wrong=meanings[(i+1)%len(meanings)]
        a,b,c=choose(cid,meaning,wrong)
        teaches=[
            ex('T21_'+safe(cid),'TEACH',[cid],lvl,.27 if lvl in ('A1','A2') else .48,
               f'Aprende la expresión {phrase}.',phrase,exp=f'{phrase}: {meaning}. Ejemplo: {example}',seconds=20,
               tags=('chunk','phrase-audio','v21','teach',kind)),
            ex('Q21_'+safe(cid),'AB',[cid],lvl,.38 if lvl in ('A1','A2') else .62,
               f'¿Qué significa la expresión {phrase}?',phrase,a,b,c,f'{phrase} significa {meaning}.',seconds=20,
               tags=('chunk','phrase-audio','v21','meaning',kind)),
            ex('X21_'+safe(cid),'SELF_ASSESS',[cid],lvl,.43 if lvl in ('A1','A2') else .70,
               f'Escucha el ejemplo y reconstruye mentalmente el significado y la función de {phrase}.',example,
               exp=f'Expresión objetivo: {phrase} = {meaning}.',seconds=25,
               tags=('chunk','phrase-audio','v21','transfer',kind)),
        ]
        for q in teaches:
            if q['id'] not in eid: exercises.append(q);eid.add(q['id'])
        for tok in re.findall(r"[A-Za-z']+",phrase.casefold()):
            lx=lex_by_lemma.get(tok)
            if lx: add_edge(lx['id'],cid,.04)

# soft progression within each level so the planner can move from common to denser formulae
by=Counter(lvl for lvl,_,_ in added)
for lvl in BANK:
    ids=[cid for l,cid,_ in added if l==lvl]
    for a,b in zip(ids,ids[1:]): add_edge(a,b,.025)

save('kcs.json',kcs); save('edges.json',edges); save('exercises.json',exercises)
manifest['version']='2.1.0'; manifest['contentVersion']='2.1.0'
manifest['description']='SIAA v2.1: Content Complete Candidate + segunda capa formulaica A1–C2 con nuevas rutinas, collocations, phrasal verbs, discourse markers y fórmulas académicas avanzadas.'
for s in [
    'SIAA original multiword/collocation authoring v2.1 — second formulaic layer A1–C2',
    'CEFR Companion Volume 2020 — functional/pragmatic progression reference',
    'CEFR-J / Open Language Profiles — level consistency reference'
]:
    if s not in manifest.setdefault('sources',[]): manifest['sources'].append(s)
manifest['coverage']={
    'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exercises),'lexemes':len(lexemes),
    'multiwordKcs':sum(1 for x in kcs if x.get('domain')=='CHUNK'),
    'listeningItems':sum(1 for x in exercises if x.get('type')=='LISTENING_AB'),
    'pronunciationItems':sum(1 for x in exercises if x.get('type')=='PRON_DISCRIMINATION'),
    'generatedVariantsEstimate':manifest.get('coverage',{}).get('generatedVariantsEstimate',8944),
    'levels':['Pre-A1','A1','A2','B1','B2','C1','C2']
}
checks={}
for n in ('kcs.json','edges.json','exercises.json','lexemes.json'):
    checks[n]=hashlib.sha256((CONTENT/n).read_bytes()).hexdigest()
manifest['checksums']=checks
save('manifest.json',manifest)
print(json.dumps({'added':len(added),'byLevel':dict(by),'kcs':len(kcs),'edges':len(edges),'exercises':len(exercises),'multiword':manifest['coverage']['multiwordKcs']},ensure_ascii=False))
