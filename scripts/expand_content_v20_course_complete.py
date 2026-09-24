#!/usr/bin/env python3
from __future__ import annotations
import json, hashlib, re
from pathlib import Path
from collections import defaultdict

ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'
LEVELS=['Pre-A1','A1','A2','B1','B2','C1','C2']
ORDER={x:i for i,x in enumerate(LEVELS)}

def load(n): return json.loads((CONTENT/n).read_text(encoding='utf-8'))
def save(n,o): (CONTENT/n).write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def safe(s): return re.sub(r'[^A-Z0-9]+','_',s.upper()).strip('_') or 'X'
def kc(id,name,cefr,domain,form,meaning,use,importance=.65,prior=.04,tags=()):
    return {'id':id,'name':name,'cefr':cefr,'domain':domain,'form':form,'meaning':meaning,'use':use,
            'importance':importance,'priorMastery':prior,'tags':list(tags)}
def edge(a,b,w=.3,hard=False): return {'fromId':a,'toId':b,'weight':w,'hardPrerequisite':hard}
def ex(id,type,kcs,cefr,diff,prompt,stim='',a='',b='',correct='',exp='',spell='',seconds=22,tags=(),mis=()):
    return {'id':id,'type':type,'kcIds':kcs,'cefr':cefr,'difficulty':diff,'promptEs':prompt,'stimulusEn':stim,
            'optionA':a,'optionB':b,'correctOption':correct,'explanationEs':exp,'spellTarget':spell,
            'estimatedSeconds':seconds,'tags':list(tags),'misconceptionIds':list(mis)}
def correct_a(key): return int(hashlib.sha1(key.encode()).hexdigest()[-1],16)%2==0
def options(key,correct,wrong): return (correct,wrong,'A') if correct_a(key) else (wrong,correct,'B')

def parse_block(block):
    out=[]
    for raw in block.strip().splitlines():
        raw=raw.strip()
        if not raw or raw.startswith('#'): continue
        parts=[x.strip() for x in raw.split('|')]
        if len(parts)!=4: raise ValueError(f'bad row: {raw}')
        out.append(parts)
    return out

kcs=load('kcs.json'); edges=load('edges.json'); exercises=load('exercises.json'); lexemes=load('lexemes.json'); manifest=load('manifest.json')
kid={x['id'] for x in kcs}; eid={x['id'] for x in exercises}; edgekey={(x['fromId'],x['toId']) for x in edges}
lex_by_lemma={x['lemma'].casefold():x for x in lexemes}

def add_kc(x):
    if x['id'] not in kid: kcs.append(x);kid.add(x['id']);return True
    return False
def add_ex(x):
    if x['id'] not in eid: exercises.append(x);eid.add(x['id']);return True
    return False
def add_edge(a,b,w=.3,hard=False):
    if a in kid and b in kid and (a,b) not in edgekey:
        edges.append(edge(a,b,w,hard));edgekey.add((a,b));return True
    return False

# ---------------------------------------------------------------------------
# 1) Multiword vocabulary / collocations / discourse formulas.
# Original SIAA authoring. Common formulaic strings are not copied from a
# coursebook; examples and Spanish glosses are original.
# ---------------------------------------------------------------------------
MW={
'A1': parse_block(r'''
get up|levantarse|I get up at seven every day.|routine
have breakfast|desayunar|We have breakfast before work.|routine
go home|ir a casa|I go home after class.|routine
come back|regresar|Please come back at six.|phrasal-verb
go to bed|irse a dormir|I usually go to bed before eleven.|routine
go to work|ir al trabajo|She goes to work by bus.|routine
go to school|ir a la escuela|The children go to school together.|routine
take a bus|tomar un autobús|We take a bus to the city centre.|collocation
wait for|esperar a / esperar por|I am waiting for my friend.|verb-pattern
look at|mirar|Look at the picture and listen.|verb-pattern
listen to|escuchar|Listen to the question carefully.|verb-pattern
talk to|hablar con|I need to talk to my teacher.|verb-pattern
live with|vivir con|He lives with his parents.|verb-pattern
be from|ser de|I am from Peru.|formula
be good at|ser bueno en|She is good at maths.|formula
be afraid of|tener miedo de|He is afraid of dogs.|formula
a lot of|mucho / muchos|There are a lot of people here.|quantifier
every day|todos los días|I practise English every day.|time-expression
right now|ahora mismo|I am busy right now.|time-expression
how much|cuánto / cuánta cantidad|How much is this ticket?|question-formula
how many|cuántos / cuántas|How many people are coming?|question-formula
excuse me|disculpe / perdón|Excuse me, where is the station?|pragmatic-formula
thank you|gracias|Thank you for your help.|pragmatic-formula
you're welcome|de nada|You're welcome. Have a good day.|pragmatic-formula
see you later|hasta luego|See you later after class.|pragmatic-formula
'''),
'A2': parse_block(r'''
look for|buscar|I'm looking for my keys.|phrasal-verb
find out|averiguar / descubrir|We need to find out what happened.|phrasal-verb
turn on|encender|Please turn on the light.|phrasal-verb
turn off|apagar|Turn off your phone during the film.|phrasal-verb
put on|ponerse|Put on your coat; it is cold.|phrasal-verb
take off|quitarse|He took off his shoes at the door.|phrasal-verb
get on|subir a|We got on the bus near the market.|phrasal-verb
get off|bajar de|Get off at the next stop.|phrasal-verb
arrive at|llegar a|We arrived at the station early.|verb-pattern
arrive in|llegar a una ciudad o país|They arrived in Lima yesterday.|verb-pattern
depend on|depender de|The price depends on the date.|verb-pattern
ask for|pedir|She asked for a glass of water.|verb-pattern
pay for|pagar por|I paid for the tickets online.|verb-pattern
think about|pensar en / sobre|Think about the question before you answer.|verb-pattern
worry about|preocuparse por|Do not worry about the small mistake.|verb-pattern
interested in|interesado en|I'm interested in environmental science.|adjective-pattern
good for|bueno para|Walking is good for your health.|adjective-pattern
because of|debido a|The match was cancelled because of the rain.|linker
in front of|delante de|The taxi is in front of the hotel.|preposition
at the end of|al final de|There is a short test at the end of the lesson.|time-expression
once a week|una vez por semana|We meet once a week.|frequency-expression
make sure|asegurarse|Make sure the door is closed.|collocation
take care of|cuidar de|She takes care of her younger brother.|multiword-verb
on time|puntual / a tiempo|The train arrived on time.|time-expression
in time|con tiempo suficiente|We arrived in time to see the beginning.|time-expression
for example|por ejemplo|Some fruit, for example apples, is easy to carry.|discourse-marker
as soon as|tan pronto como|Call me as soon as you arrive.|linker
a few|unos pocos / unas pocas|I have a few questions.|quantifier
a little|un poco|Add a little water.|quantifier
have to|tener que|I have to finish this today.|modal-formula
'''),
'B1': parse_block(r'''
deal with|ocuparse de / afrontar|We need to deal with the problem today.|phrasal-verb
take part in|participar en|More than fifty students took part in the survey.|collocation
get along with|llevarse bien con|I get along with my new colleagues.|phrasal-verb
look forward to|esperar con ilusión|I'm looking forward to the weekend.|phrasal-verb
run out of|quedarse sin|We ran out of paper during the meeting.|phrasal-verb
set up|crear / configurar|They set up a small research team.|phrasal-verb
work out|resolver / resultar|We finally worked out how to fix it.|phrasal-verb
carry on|continuar|Please carry on with your presentation.|phrasal-verb
give up|dejar / rendirse|He gave up smoking last year.|phrasal-verb
pick up|recoger / aprender incidentalmente|I'll pick you up at the station.|phrasal-verb
drop off|dejar a alguien / algo|She dropped the package off on her way home.|phrasal-verb
grow up|crecer|I grew up near the coast.|phrasal-verb
end up|terminar / acabar|We ended up taking a later train.|phrasal-verb
point out|señalar|The reviewer pointed out two weaknesses.|phrasal-verb
turn out|resultar|The task turned out to be easier than expected.|phrasal-verb
according to|según|According to the report, emissions fell last year.|discourse-marker
in addition to|además de|In addition to the interview, we collected survey data.|linker
on the other hand|por otro lado|The job is demanding; on the other hand, it offers good training.|discourse-marker
as a result|como resultado|The road was closed; as a result, we arrived late.|discourse-marker
in case|por si / en caso de que|Take an umbrella in case it rains.|linker
even though|aunque|Even though he was tired, he finished the report.|linker
rather than|en lugar de|We decided to walk rather than take a taxi.|linker
at least|al menos|At least three people checked the figures.|discourse-marker
in general|en general|In general, the participants preferred the second option.|discourse-marker
in fact|de hecho|The task looked simple. In fact, it took two hours.|discourse-marker
make a decision|tomar una decisión|We need to make a decision by Friday.|collocation
take responsibility|asumir responsabilidad|Managers must take responsibility for the final decision.|collocation
have an effect on|tener un efecto en|Sleep has an effect on concentration.|collocation
raise awareness|crear conciencia / sensibilizar|The campaign aims to raise awareness of recycling.|collocation
solve a problem|resolver un problema|The team solved the problem without extra equipment.|collocation
meet a deadline|cumplir un plazo|We worked late to meet the deadline.|collocation
keep in touch|mantenerse en contacto|We still keep in touch after university.|formula
take into account|tener en cuenta|The plan must take local conditions into account.|collocation
be likely to|ser probable que|Prices are likely to rise next month.|formula
be responsible for|ser responsable de|She is responsible for checking the data.|formula
'''),
'B2': parse_block(r'''
bring about|provocar / causar|The new policy brought about several changes.|phrasal-verb
carry out|llevar a cabo|The laboratory carried out three independent tests.|phrasal-verb
come up with|idear / proponer|The team came up with a practical solution.|phrasal-verb
cut down on|reducir|We are trying to cut down on unnecessary travel.|phrasal-verb
figure out|averiguar / resolver|It took us a while to figure out why the sensor failed.|phrasal-verb
rule out|descartar|The evidence does not rule out an alternative explanation.|phrasal-verb
account for|explicar / representar|Transport accounts for a large share of the emissions.|phrasal-verb
result in|dar lugar a|Poor ventilation can result in higher indoor temperatures.|verb-pattern
lead to|conducir a|A small error can lead to a larger problem.|verb-pattern
contribute to|contribuir a|Several factors contributed to the final result.|verb-pattern
cope with|hacer frente a|The system must cope with sudden changes in demand.|verb-pattern
focus on|centrarse en|The analysis focuses on long-term trends.|verb-pattern
rely on|depender de / confiar en|The model relies on several assumptions.|verb-pattern
refer to|referirse a|The term refers to a specific type of waste.|verb-pattern
distinguish between|distinguir entre|It is important to distinguish between correlation and causation.|verb-pattern
in terms of|en términos de|The options differ mainly in terms of cost.|discourse-marker
with regard to|con respecto a|With regard to safety, both designs meet the standard.|discourse-marker
by contrast|en cambio|The first group improved; by contrast, the second remained stable.|discourse-marker
to some extent|hasta cierto punto|The result can be explained, to some extent, by seasonal variation.|hedge
in the long run|a largo plazo|The investment should reduce costs in the long run.|time-expression
on the whole|en general / en conjunto|On the whole, the programme achieved its main objectives.|discourse-marker
in spite of|a pesar de|In spite of the delay, the project stayed within budget.|linker
provided that|siempre que|The method is reliable provided that the equipment is calibrated.|linker
unless|a menos que|The deadline will remain unchanged unless new evidence appears.|linker
make an assumption|hacer una suposición|The model makes an assumption about future demand.|collocation
draw a conclusion|sacar una conclusión|We should not draw a conclusion from one observation.|collocation
reach a consensus|alcanzar un consenso|The committee eventually reached a consensus.|collocation
pose a risk|plantear / suponer un riesgo|The leak may pose a risk to nearby residents.|collocation
address an issue|abordar un problema|The revised plan addresses the main privacy issue.|collocation
meet a requirement|cumplir un requisito|The device must meet the legal requirement.|collocation
take measures|tomar medidas|The company took measures to reduce noise.|collocation
play a role in|desempeñar un papel en|Temperature plays a role in the reaction rate.|collocation
have access to|tener acceso a|Only authorised staff have access to the records.|formula
be subject to|estar sujeto a|The permit is subject to periodic review.|formula
in accordance with|de acuerdo con|The samples were handled in accordance with the protocol.|formal-linker
by means of|por medio de|The concentration was estimated by means of a standard method.|formal-linker
'''),
'C1': parse_block(r'''
shed light on|arrojar luz sobre|The new data shed light on the cause of the discrepancy.|idiomatic-collocation
pave the way for|allanar el camino para|The pilot project paved the way for a larger programme.|idiomatic-collocation
call into question|poner en duda|The inconsistency calls the original conclusion into question.|idiomatic-collocation
give rise to|dar lugar a|The change may give rise to unintended consequences.|formal-collocation
put forward|plantear / proponer|The authors put forward two competing explanations.|phrasal-verb
set out|exponer / establecer|The report sets out the criteria used in the assessment.|phrasal-verb
stem from|derivarse de|Most of the uncertainty stems from missing historical data.|phrasal-verb
hinge on|depender decisivamente de|The decision hinges on whether the assumptions are realistic.|idiomatic-collocation
fall short of|no alcanzar|The evidence falls short of proving a causal relationship.|phrasal-verb
bear in mind|tener presente|Bear in mind that the sample was relatively small.|formula
take for granted|dar por sentado|We should not take reliable internet access for granted.|idiomatic-collocation
make a distinction|hacer una distinción|The study makes a useful distinction between exposure and mastery.|collocation
exert influence on|ejercer influencia sobre|Institutional rules exert considerable influence on behaviour.|formal-collocation
be consistent with|ser coherente con|The observed pattern is consistent with the proposed mechanism.|academic-formula
be indicative of|ser indicativo de|A sudden increase may be indicative of a measurement problem.|academic-formula
be attributable to|ser atribuible a|Part of the improvement may be attributable to prior training.|academic-formula
in light of|a la luz de|In light of the new evidence, the policy should be reviewed.|formal-linker
in the context of|en el contexto de|The result is more meaningful in the context of long-term trends.|formal-linker
from this perspective|desde esta perspectiva|From this perspective, efficiency is not the only relevant criterion.|discourse-marker
by the same token|por la misma lógica|Costs may rise; by the same token, service quality may improve.|discourse-marker
to a large extent|en gran medida|The outcome depends, to a large extent, on local implementation.|hedge
for the most part|en su mayor parte|For the most part, the participants followed the protocol.|hedge
on balance|considerándolo todo|On balance, the benefits appear to outweigh the costs.|stance-marker
with the exception of|con la excepción de|All sites improved, with the exception of the smallest facility.|formal-linker
at odds with|en desacuerdo / contradicción con|The finding is at odds with the earlier survey.|academic-formula
in line with|en consonancia con|The result is broadly in line with previous research.|academic-formula
a matter of|una cuestión de|The disagreement is partly a matter of interpretation.|formula
a range of|una variedad de|The framework considers a range of environmental impacts.|formula
the extent to which|el grado en que|The study examines the extent to which the policy changed behaviour.|academic-formula
there is little doubt that|hay pocas dudas de que|There is little doubt that the system needs maintenance.|stance-marker
it is worth noting that|conviene señalar que|It is worth noting that the effect was not uniform across groups.|stance-marker
notwithstanding|a pesar de|Notwithstanding these limitations, the evidence remains informative.|formal-linker
albeit|aunque / si bien|The intervention produced a modest, albeit measurable, improvement.|formal-linker
insofar as|en la medida en que|The comparison is useful insofar as the groups are genuinely comparable.|formal-linker
in keeping with|de acuerdo con / en consonancia con|The revised procedure is in keeping with current guidance.|formal-linker
be prone to|ser propenso a|Small sensors can be prone to drift over time.|academic-formula
'''),
'C2': parse_block(r'''
be tantamount to|equivaler prácticamente a|Ignoring the uncertainty would be tantamount to assuming it does not exist.|advanced-formula
be predicated on|basarse en|The argument is predicated on a distinction that the data do not support.|academic-formula
be contingent on|depender de|The recommendation is contingent on the availability of reliable monitoring.|academic-formula
run counter to|contradecir|The proposal runs counter to the principle stated earlier.|idiomatic-collocation
beg the question|dar por supuesto lo que debe demostrarse|Claiming the method is superior because it is better simply begs the question.|rhetorical-formula
take issue with|discrepar de / objetar|Several reviewers took issue with the way the evidence was interpreted.|idiomatic-collocation
lend credence to|dar credibilidad a|The replication lends credence to the original explanation.|academic-collocation
cast doubt on|poner en duda|The missing records cast doubt on the precision of the estimate.|academic-collocation
be symptomatic of|ser sintomático de|Repeated delays may be symptomatic of a deeper coordination problem.|academic-formula
be emblematic of|ser emblemático de|The dispute is emblematic of a broader conflict over priorities.|academic-formula
be conducive to|ser favorable para|A stable environment is conducive to careful decision-making.|academic-formula
be detrimental to|ser perjudicial para|Frequent interruptions can be detrimental to sustained attention.|academic-formula
be commensurate with|ser proporcional / acorde con|The response should be commensurate with the level of risk.|formal-formula
in the absence of|a falta de|In the absence of direct measurements, the estimate remains provisional.|formal-linker
by virtue of|en virtud de|The site qualifies by virtue of its legal status.|formal-linker
for the sake of|por el bien de / en aras de|For the sake of clarity, the two mechanisms should be discussed separately.|formal-linker
in the wake of|a raíz de|The procedure was revised in the wake of the incident.|formal-linker
in the event of|en caso de|In the event of a power failure, the system switches to backup mode.|formal-linker
for all intents and purposes|a efectos prácticos|For all intents and purposes, the two versions behave identically.|idiomatic-formula
all things considered|considerándolo todo|All things considered, postponing the decision is the safer option.|stance-marker
be that as it may|sea como fuere|Be that as it may, the underlying uncertainty still needs to be addressed.|formal-formula
suffice it to say|baste decir|Suffice it to say that the negotiations were far from straightforward.|formal-formula
needless to say|no hace falta decir|Needless to say, confidential records should not be shared casually.|formula
far from|lejos de|Far from resolving the dispute, the announcement intensified it.|rhetorical-formula
let alone|y mucho menos|The dataset cannot establish causation, let alone explain the mechanism.|rhetorical-formula
much less|mucho menos|The report does not identify a clear trend, much less a definitive cause.|rhetorical-formula
to the extent that|en la medida en que|The policy is effective to the extent that local agencies can enforce it.|formal-linker
in stark contrast to|en marcado contraste con|The second period was stable, in stark contrast to the first.|discourse-marker
on the face of it|a primera vista|On the face of it, the two results appear incompatible.|stance-marker
on closer inspection|al examinarlo con más detalle|On closer inspection, the apparent contradiction disappears.|discourse-marker
not so much X as Y|no tanto X como Y|The problem is not so much technical as institutional.|rhetorical-frame
be hard-pressed to|tener grandes dificultades para|We would be hard-pressed to justify that conclusion from the available data.|advanced-formula
leave much to be desired|dejar mucho que desear|The documentation leaves much to be desired.|idiomatic-formula
by no means|de ningún modo|The absence of complaints is by no means proof that the system is fair.|stance-marker
for want of|por falta de|For want of better evidence, the estimate was retained provisionally.|formal-linker
''')
}

chunk_kcs_by_level=defaultdict(list)
for lvl,rows in MW.items():
    meanings=[r[1] for r in rows]
    for i,(phrase,meaning,example,kind) in enumerate(rows):
        cid='C20_'+safe(phrase)
        add_kc(kc(cid,phrase,lvl,'CHUNK',phrase,meaning,
                  f'usar la expresión como unidad formulaica ({kind})',.64 if lvl in ('A1','A2','B1') else .70,.035,
                  ('multiword','formulaic',kind,'v20','original-authoring')))
        chunk_kcs_by_level[lvl].append(cid)
        wrong=meanings[(i*7+3)%len(meanings)]
        if wrong==meaning: wrong=meanings[(i+1)%len(meanings)]
        a,b,c=options(cid,meaning,wrong)
        add_ex(ex('T20_'+safe(cid),'TEACH',[cid],lvl,.28 if lvl in ('A1','A2') else .48,
                  f'Aprende la expresión {phrase}.',phrase,exp=f'{phrase}: {meaning}. Ejemplo: {example}',seconds=20,
                  tags=('chunk','phrase-audio','v20','teach',kind)))
        add_ex(ex('Q20_'+safe(cid),'AB',[cid],lvl,.38 if lvl in ('A1','A2') else .62,
                  f'¿Qué significa la expresión {phrase}?',phrase,a,b,c,f'{phrase} significa {meaning}.',seconds=20,
                  tags=('chunk','phrase-audio','v20','meaning',kind)))
        add_ex(ex('X20_'+safe(cid),'SELF_ASSESS',[cid],lvl,.44 if lvl in ('A1','A2') else .70,
                  f'Escucha el ejemplo y reconstruye mentalmente el significado y la función de {phrase}.',example,
                  exp=f'Expresión objetivo: {phrase} = {meaning}.',seconds=24,
                  tags=('chunk','phrase-audio','v20','transfer',kind)))
        # Soft lexical prerequisites from component lemmas where available.
        for tok in re.findall(r"[A-Za-z']+",phrase.casefold()):
            lx=lex_by_lemma.get(tok)
            if lx: add_edge(lx['id'],cid,.04,False)

# ---------------------------------------------------------------------------
# 2) Grammar objective discrimination for every grammar KC.
# CEFR-J-derived KCs already encode form/meaning/use. Add an objective item
# where one is missing, paired with a same-level alternative form.
# ---------------------------------------------------------------------------
grammar=[x for x in kcs if x['domain']=='GRAMMAR']
g_by_level=defaultdict(list)
for g in grammar: g_by_level[g['cefr']].append(g)
ex_by_kc=defaultdict(set)
for q in exercises:
    for qk in q.get('kcIds',[]): ex_by_kc[qk].add(q['type'])
for lvl,gs in g_by_level.items():
    for i,g in enumerate(gs):
        if 'AB' in ex_by_kc[g['id']] or 'MEANING_AB' in ex_by_kc[g['id']]: continue
        alt=gs[(i+max(1,len(gs)//3))%len(gs)] if len(gs)>1 else g
        form=(g.get('form') or g['name']).strip(); other=(alt.get('form') or alt['name']).strip()
        if other==form: other=f"otra construcción de nivel {lvl}"
        a,b,c=options('gramobj:'+g['id'],form,other)
        target=(g.get('meaning') or g.get('use') or g['name']).strip()
        add_ex(ex('G20_OBJ_'+safe(g['id']),'AB',[g['id']],lvl,.40 if lvl in ('Pre-A1','A1','A2') else .64,
                  f'¿Qué forma corresponde mejor a esta función: {target}?','',a,b,c,
                  f'Forma objetivo: {form}. Uso: {g.get("use","")}',seconds=21,
                  tags=('grammar','form-meaning-use','objective','v20')))

# Ensure teaching + transfer around legacy grammar anchors.
for g in grammar:
    types=ex_by_kc[g['id']]
    if 'TEACH' not in types:
        add_ex(ex('G20_T_'+safe(g['id']),'TEACH',[g['id']],g['cefr'],.35,
                  f'Revisa la construcción {g["name"]}.',g.get('form',''),
                  exp=f'Forma: {g.get("form","")}. Significado: {g.get("meaning","")}. Uso: {g.get("use","")}.',
                  seconds=24,tags=('grammar','teach','v20')))
    if 'SELF_ASSESS' not in types:
        add_ex(ex('G20_X_'+safe(g['id']),'SELF_ASSESS',[g['id']],g['cefr'],.55,
                  f'Construye mentalmente una oración nueva que use {g["name"]} para esta función: {g.get("use") or g.get("meaning")}.',
                  g.get('form',''),exp=f'Comprueba que tu ejemplo respete la forma {g.get("form","")}.',seconds=26,
                  tags=('grammar','transfer','v20')))

# ---------------------------------------------------------------------------
# 3) Listening: original SIAA material. Adds short and extended passages plus
# metacognitive reflection tasks inspired by planning-monitoring-evaluation.
# ---------------------------------------------------------------------------
L_TARGETS={
'A1':['L11_KEY_DETAILS','L11_INSTRUCTIONS','L11_WORD_BOUND'],
'A2':['L11_GIST','L11_SEQUENCE','L11_REFERENCE'],
'B1':['L11_MAIN_SUPPORT','L11_ATTITUDE','L11_CONTEXT_INFER'],
'B2':['L11_ARGUMENT','L11_IMPLICIT_INTENT','L11_CONNECTED_SPEECH'],
'C1':['L11_DISCOURSE_ORG','L11_DISTRIBUTED','L11_STANCE'],
'C2':['L11_FAST_DENSE','L11_MEDIATION','L11_SUBTEXT'],
}
LISTENING={
'A1':[
('library','The library opens at nine and closes at six. On Saturday it closes at two.','¿A qué hora cierra el sábado?','at two','at six'),
('breakfast','For breakfast I usually have bread, fruit and tea. Today I only have coffee because I am late.','¿Qué toma hoy?','coffee','tea'),
('meeting_place','Meet me outside the bank, next to the pharmacy, at half past four.','¿Dónde deben encontrarse?','outside the bank','inside the pharmacy'),
('classroom','Open your book on page twelve. Read the first paragraph, but do not answer the questions yet.','¿Qué debe hacer primero?','read the first paragraph','answer all the questions'),
('week_plan','I work on Monday and Tuesday. Wednesday is my free day, and I study on Thursday evening.','¿Qué día tiene libre?','Wednesday','Thursday'),
('shopping','The red shirt is twenty dollars and the blue one is fifteen. I will take the blue one.','¿Cuál compra?','the blue shirt','the red shirt'),
],
'A2':[
('missed_train','We left home early, but traffic was very slow. We reached the station just after the train had left, so we took the next one.','¿Por qué tomaron el siguiente tren?','they arrived after the first train left','they forgot their tickets'),
('changed_recipe','The recipe says to use cream, but I did not have any, so I used plain yogurt instead. The sauce was lighter but still good.','¿Qué sustituyó la crema?','plain yogurt','milk'),
('course_schedule','I wanted the Tuesday class, but it was full. The school offered me Thursday evening instead, and that time actually suits me better.','¿Qué clase tomó finalmente?','Thursday evening','Tuesday morning'),
('hotel_location','The hotel is not in the city centre, but there is a metro station across the street and the journey downtown takes only ten minutes.','¿Qué compensa la ubicación?','easy metro access','a free taxi'),
('gift_choice','I first thought of buying Marta a book. Then I remembered she already has it, so I chose a small plant for her desk.','¿Qué regalo eligió?','a small plant','a book'),
('appointment_delay','The doctor is running about twenty minutes late. You can wait here, or you can get a coffee and come back at three fifteen.','¿Cuándo puede regresar?','at three fifteen','at two fifteen'),
],
'B1':[
('energy_bill','Our electricity bill went up in winter, so we checked which appliances used the most power. The old heater was the main problem. We replaced it and the next bill was noticeably lower.','¿Qué cambio redujo la factura?','replacing the old heater','using more appliances'),
('team_feedback','At first the new meeting format felt too strict because everyone had only two minutes to speak. After a few weeks, however, we noticed that meetings were shorter and quieter colleagues contributed more often.','¿Qué beneficio apareció con el tiempo?','more efficient and balanced participation','longer meetings'),
('study_method','I used to reread my notes again and again. Now I test myself without looking at them and return to the material a few days later. It feels harder, but I remember much more.','¿Qué cambió en su método?','from rereading to retrieval and spaced review','from testing to copying notes'),
('neighbourhood','The council planted trees along the main road last year. Traffic has not decreased, but people say the street feels cooler in the afternoon and more pleasant to walk along.','¿Qué efecto positivo menciona?','a cooler and more pleasant street','less traffic'),
('job_training','The salary was not the main reason I accepted the job. The company offered six months of structured training and a mentor, which seemed more valuable at the beginning of my career.','¿Qué factor fue decisivo?','training and mentoring','the highest salary'),
('travel_advice','If you arrive during rush hour, the airport bus can take more than ninety minutes. The train costs a little more but is usually faster and more predictable.','¿Qué ventaja tiene el tren?','it is usually faster and more predictable','it is always cheaper'),
],
'B2':[
('pilot_policy','The city plans to restrict private cars on two central streets for six months. Supporters expect cleaner air and safer walking conditions, while shop owners fear losing customers. Rather than make the change permanent immediately, the council will compare traffic, pollution and sales data before deciding.','¿Por qué se usa una prueba de seis meses?','to compare competing effects before a permanent decision','to avoid measuring outcomes'),
('study_design','Participants who chose to join the training programme improved more than those who did not. That difference is encouraging, but self-selection matters: people who volunteered may already have been more motivated. The result supports further research, not a strong causal claim.','¿Cuál es la principal cautela?','self-selection may explain part of the difference','the groups were randomly assigned'),
('maintenance','The factory reduced reported downtime by postponing some preventive maintenance. In the short term the figures looked better, but several engineers warned that failures could become more frequent later. The apparent efficiency gain therefore needs to be interpreted cautiously.','¿Por qué puede ser engañosa la mejora?','maintenance was postponed rather than eliminated','all machines were replaced'),
('privacy_tradeoff','The app can provide more personalised recommendations if it stores detailed usage history. The same data, however, increases privacy risk if access controls fail. The design team proposes keeping only the minimum history needed for adaptation.','¿Qué solución propone el equipo?','data minimisation','storing every possible detail'),
('forecast','The model performed well during ordinary weather but underestimated demand during two heat waves. Before using it operationally, the analysts want to retrain it with more extreme events and test whether the improvement generalises.','¿Qué debilidad identificaron?','poor performance during extreme heat','poor performance in all normal conditions'),
('budget','A cheaper component would reduce the purchase price by twelve percent. Yet it needs replacement twice as often and requires more maintenance hours. Once those costs are included, the supposedly cheaper option may not save money over the full life cycle.','¿Cuál es la idea central?','initial price can differ from life-cycle cost','the cheapest purchase price always wins'),
],
'C1':[
('causal_language','The intervention coincided with a decline in complaints, and the timing is certainly suggestive. Still, several other changes occurred during the same period, including new staffing rules and a seasonal fall in demand. The evidence is compatible with an effect, but it does not isolate one mechanism convincingly.','¿Qué postura adopta el hablante?','the intervention may matter but causation is not isolated','the intervention is proven to be the only cause'),
('distributed_argument','At the beginning of the talk, the speaker presents falling operating costs as a success. Much later she notes that investment in maintenance also fell sharply. In the final section she warns that equipment failures have started to rise. Taken together, the details complicate the initial success story.','¿Qué interpretación integra mejor la información?','lower costs may partly reflect underinvestment with later consequences','lower costs prove sustainable efficiency'),
('institutional_problem','The technology itself has already worked in three pilot sites. What remains uncertain is governance: agencies use different definitions, budgets are approved on different schedules, and no institution has authority to resolve conflicts. Scaling the system therefore depends less on engineering than on coordination.','¿Dónde está el principal obstáculo?','institutional coordination','basic technical feasibility'),
('hedged_review','The review identifies a recurring association between exposure and performance, but the studies vary widely in design and measurement. A pooled estimate is informative, provided it is not mistaken for evidence that every context behaves in the same way.','¿Qué reserva introduce?','the average pattern may not hold uniformly across contexts','all studies are identical'),
('policy_stance','The author accepts that the regulation improved transparency. She nevertheless questions whether the administrative burden is proportionate for very small organisations, and suggests a simplified route for low-risk cases.','¿Cuál es su postura?','support for the goal with a qualified criticism of implementation','total rejection of transparency'),
('model_limits','The simulation reproduces historical averages remarkably well. Its weakest point is precisely where decision-makers most need guidance: rare disruptions. Because the model has seen few such events, confidence intervals widen sharply under extreme scenarios.','¿Cuál es la principal limitación?','uncertainty under rare extreme events','inability to match historical averages'),
],
'C2':[
('strategic_answer','Asked whether the merger had delivered the promised savings, the chair replied that the organisation was now “better positioned for future efficiencies.” The phrase sounds positive, but it quietly shifts attention from realised savings to possible future gains.','¿Qué hace retóricamente la respuesta?','avoids confirming the original claim by reframing it','provides a direct numerical confirmation'),
('aggregation','At the national level the indicator barely changes, which might suggest stability. Disaggregate the same data, however, and two opposing movements emerge: urban values rise sharply while rural values fall. The aggregate average conceals a redistribution rather than an absence of change.','¿Qué muestra el análisis desagregado?','opposing subgroup trends hidden by the average','identical trends in every subgroup'),
('normative_tradeoff','The proposal is often presented as a purely technical optimisation, yet choosing what to optimise already embeds a value judgement. A system that minimises average waiting time may still impose very long waits on a small minority. Efficiency and fairness cannot simply be treated as interchangeable objectives.','¿Qué supuesto cuestiona?','that technical optimisation is value-neutral','that waiting time can be measured'),
('ironic_comment','After receiving the third contradictory instruction of the morning, the analyst smiled and said, “Wonderful. At least the process is perfectly clear now.” The literal wording is positive, but the context and exaggeration signal the opposite evaluation.','¿Cómo debe interpretarse wonderful?','as irony expressing frustration','as literal satisfaction'),
('methodological_reframe','One critic argues that the experiment failed because the expected effect was not statistically significant. Another responds that the study was designed to estimate the size and uncertainty of the effect, not to produce a binary verdict. The disagreement is partly about what question the analysis is supposed to answer.','¿Qué se está replanteando?','the purpose of statistical analysis','whether any data were collected'),
('mediation','The first report emphasises economic benefits, whereas the second focuses on ecological risks. A fair synthesis does not average those claims mechanically; it identifies that they evaluate the same project through different criteria and makes the trade-off explicit.','¿Qué caracteriza una mediación fiel?','making the differing criteria and trade-off explicit','pretending the two reports say the same thing'),
]
}
for lvl,items in LISTENING.items():
    targets=L_TARGETS[lvl]
    for i,(slug,stim,prompt,corr,wrong) in enumerate(items):
        lid=targets[i%len(targets)]
        a,b,c=options('L20:'+lvl+slug,corr,wrong)
        add_ex(ex('L20_'+safe(lvl+'_'+slug),'LISTENING_AB',[lid],lvl,
                  {'A1':.30,'A2':.42,'B1':.56,'B2':.69,'C1':.81,'C2':.90}[lvl],
                  prompt,stim,a,b,c,f'Interpretación objetivo: {corr}.',seconds=30 if lvl in ('A1','A2') else 42,
                  tags=('listening','original-script','audio-asset','v20','transfer','extended' if lvl in ('B1','B2','C1','C2') else 'short')))

# Metacognitive prompts: planning -> monitoring -> evaluation/relisten.
for lvl,targets in L_TARGETS.items():
    lid=targets[0]
    prompts=[
      ('PLAN','Antes de escuchar, predice mentalmente qué información será más importante a partir del tema y la pregunta.','planning'),
      ('MON','Después de una primera escucha, identifica qué parte entendiste con seguridad y qué parte necesita una segunda escucha.','monitoring'),
      ('EVAL','Tras volver a escuchar, evalúa qué pista resolvió la dificultad: léxico, segmentación, prosodia, contexto o estructura del discurso.','evaluation'),
    ]
    for tag,prompt,proc in prompts:
        add_ex(ex(f'L20_META_{safe(lvl)}_{tag}','SELF_ASSESS',[lid],lvl,.48,
                  prompt,'',exp=f'Proceso metacognitivo: {proc}. El objetivo es regular conscientemente la comprensión.',seconds=24,
                  tags=('listening','metacognitive',proc,'v20')))

# ---------------------------------------------------------------------------
# 4) Pronunciation/perception: broaden segmental + prosodic coverage.
# Synthetic audio is explicitly training-only, not a human gold standard.
# ---------------------------------------------------------------------------
PRON={
'A1':[
('P20_A1_E_AE','/e/ frente a /æ/','pen. pan.','diferente','igual'),
('P20_A1_FINAL_T_D','/t/ frente a /d/ final','bet. bed.','diferente','igual'),
('P20_A1_S_Z','/s/ frente a /z/','sip. zip.','diferente','igual'),
('P20_A1_WORD_STRESS','Acento en palabras bisílabas','TAble. hoTEL.','diferente','igual'),
],
'A2':[
('P20_A2_SH_CH','/ʃ/ frente a /tʃ/','ship. chip.','diferente','igual'),
('P20_A2_W_V','/w/ frente a /v/','west. vest.','diferente','igual'),
('P20_A2_WEAK_TO','Forma débil de to','I want to go.','reducción esperable','sin reducción posible'),
('P20_A2_CAN_CANT','can frente a can\'t','I can go. I can\'t go.','diferente','igual'),
],
'B1':[
('P20_B1_LINK_VV','Enlace entre vocales','go out. see it.','enlace perceptible','pausa obligatoria'),
('P20_B1_REDUCED_AND','Reducción de and','bread and butter.','reducción esperable','pronunciación aislada obligatoria'),
('P20_B1_FLAP_T','/t/ intervocálica en inglés americano','water. city.','variación contextual','error léxico'),
('P20_B1_FOCUS','Acento contrastivo','I wanted the BLUE one.','foco en blue','foco en wanted'),
],
'B2':[
('P20_B2_ASSIMILATION','Asimilación','green park. ten boys.','cambio contextual posible','cada sonido siempre idéntico'),
('P20_B2_ELISION_TD','Elisión de /t/ o /d/','next day. old man.','reducción posible','reducción imposible'),
('P20_B2_THOUGHT_GROUPS','Grupos entonativos','After the meeting | we checked the figures.','dos grupos','una sola palabra'),
('P20_B2_ATTITUDE','Entonación y actitud','Really? Really.','actitud diferente','significado prosódico idéntico'),
],
'C1':[
('P20_C1_NUCLEAR_SHIFT','Desplazamiento de foco nuclear','I said the FIRST report. I said the first REPORT.','foco diferente','foco idéntico'),
('P20_C1_PARENTHESES','Prosodia parentética','The result, as you know, was unexpected.','inserción prosódica','lista de palabras aisladas'),
('P20_C1_STANCE_FALLRISE','Fall-rise y matiz','It is possible...','reserva / continuidad posible','certeza absoluta obligatoria'),
('P20_C1_RATE_RESEG','Resegmentación a velocidad natural','an aim. a name.','límites pueden ser ambiguos','límites siempre acústicamente explícitos'),
],
'C2':[
('P20_C2_IRONY_PROSODY','Prosodia irónica','Oh, brilliant. Exactly what we needed.','lectura irónica posible','elogio literal obligatorio'),
('P20_C2_FINE_FOCUS','Foco correctivo fino','I meant THURSDAY, not Tuesday.','contraste correctivo','enumeración neutra'),
('P20_C2_REGISTER_PROSODY','Prosodia y registro','Would you mind taking a seat?','registro cortés','orden brusca obligatoria'),
('P20_C2_ACCENT_ADAPT','Adaptación a variación de acento','The schedule has changed again.','misma proposición con variación fonética','palabras distintas obligatoriamente'),
]
}
for lvl,items in PRON.items():
    for pid,name,stim,corr,wrong in items:
        add_kc(kc(pid,name,lvl,'PHONOLOGY',stim,name,name,.64,.035,('phonology','perception','v20')))
        add_ex(ex('T20_'+pid,'TEACH',[pid],lvl,.38,f'Escucha este objetivo fonológico: {name}.',stim,
                  exp=f'Rasgo objetivo: {name}.',seconds=18,tags=('pronunciation','audio-asset','v20','teach')))
        a,b,c=options('PRON:'+pid,corr,wrong)
        add_ex(ex('Q20_'+pid,'PRON_DISCRIMINATION',[pid],lvl,.48 if lvl in ('A1','A2') else .72,
                  'Escucha el estímulo y selecciona la interpretación acústica adecuada.',stim,a,b,c,
                  f'Objetivo perceptivo: {name}.',seconds=19,tags=('pronunciation','discrimination','audio-asset','v20','transfer')))

# ---------------------------------------------------------------------------
# 5) Pragmatics: extend appropriacy/register/mediation, especially C levels.
# ---------------------------------------------------------------------------
PRAG={
'A1':[
('PR20_A1_POLITE_ATTENTION','Llamar la atención cortésmente','Excuse me, could you help me?','pedir atención antes de una petición'),
('PR20_A1_SIMPLE_THANKS','Agradecer y responder','Thanks a lot. — You\'re welcome.','cerrar un intercambio breve cortésmente'),
],
'A2':[
('PR20_A2_SOFT_REQUEST','Suavizar una petición','Could you open the window, please?','hacer una petición cotidiana cortés'),
('PR20_A2_APOLOGY_REPAIR','Disculparse y reparar','I\'m sorry I\'m late. The bus broke down.','reconocer un problema y dar explicación'),
],
'B1':[
('PR20_B1_PARTIAL_AGREE','Acuerdo parcial','I agree with the main idea, but I\'m not sure about the cost.','mostrar acuerdo y reserva'),
('PR20_B1_CLARIFY','Pedir aclaración','When you say “efficient”, do you mean faster or cheaper?','resolver ambigüedad'),
],
'B2':[
('PR20_B2_DIPLOMATIC_CRITIQUE','Crítica diplomática','The proposal is promising, although the evidence section could be stronger.','criticar sin rechazo frontal'),
('PR20_B2_NEGOTIATE','Negociar una solución','If we move the deadline, could you provide the missing data by Monday?','intercambiar concesión y condición'),
('PR20_B2_REFRAME','Reformular para confirmar','So, if I understand correctly, your main concern is reliability rather than price.','verificar comprensión mediante reformulación'),
],
'C1':[
('PR20_C1_HEDGE_CLAIM','Matizar una afirmación','The pattern appears to be consistent, although the evidence is still limited.','expresar postura con cautela epistémica'),
('PR20_C1_CONCEDE_PIVOT','Conceder y redirigir','Granted, the method is faster; the more important question is whether it is accurate enough.','conceder un punto y cambiar el foco'),
('PR20_C1_FACE_SAVING','Discrepar preservando la relación','I can see why that interpretation is attractive. I read the evidence somewhat differently.','discrepar sin amenazar innecesariamente la imagen del interlocutor'),
('PR20_C1_TURN_MANAGEMENT','Gestionar turno complejo','Before we move on, could I pick up on one point from Maria\'s argument?','recuperar un punto y gestionar el turno'),
],
'C2':[
('PR20_C2_STRATEGIC_AMBIGUITY','Reconocer ambigüedad estratégica','The statement is carefully worded: it acknowledges progress without committing to a specific outcome.','interpretar formulación deliberadamente no categórica'),
('PR20_C2_IRONIC_ALIGNMENT','Interpretar alineamiento irónico','“Perfect timing,” he said as the system failed five minutes before the deadline.','inferir significado opuesto apoyado por contexto y prosodia'),
('PR20_C2_MEDIATE_VALUES','Mediar marcos de valor','One side prioritises efficiency; the other prioritises fairness. A useful synthesis must preserve that difference.','reformular posiciones incompatibles sin borrarlas'),
('PR20_C2_DIPLOMATIC_REFUSAL','Rechazo diplomático avanzado','I would hesitate to endorse the proposal in its present form, though the underlying objective is sound.','rechazar una propuesta manteniendo espacio para acuerdo'),
('PR20_C2_META_PRAGMATIC','Comentar el efecto pragmático','Calling the result “interesting” here functions less as praise than as a cautious signal of doubt.','explicar cómo una forma produce un efecto pragmático contextual'),
]
}
for lvl,items in PRAG.items():
    wrong_pool=[x[2] for x in items]
    for i,(pid,name,stim,use) in enumerate(items):
        add_kc(kc(pid,name,lvl,'PRAGMATICS',stim,use,use,.68,.035,('pragmatics','appropriacy','v20','original-authoring')))
        add_ex(ex('T20_'+pid,'TEACH',[pid],lvl,.40,f'Función pragmática: {name}.',stim,
                  exp=f'Uso: {use}.',seconds=22,tags=('pragmatics','teach','v20')))
        wrong=wrong_pool[(i+1)%len(wrong_pool)]
        corr=f'adecuado para {use}'
        wrong_label='adecuado para una función distinta o con un registro menos apropiado'
        a,b,c=options('PRAG:'+pid,corr,wrong_label)
        add_ex(ex('Q20_'+pid,'AB',[pid],lvl,.55,
                  f'En el contexto dado, ¿cómo debe interpretarse esta formulación: {stim}?',stim,a,b,c,
                  f'La formulación se usa para {use}.',seconds=23,tags=('pragmatics','appropriacy','objective','v20')))
        add_ex(ex('X20_'+pid,'SELF_ASSESS',[pid],lvl,.65,
                  f'Imagina un contexto nuevo y produce mentalmente una formulación equivalente para {use}.',stim,
                  exp=f'Conserva la función pragmática y ajusta el registro al interlocutor.',seconds=27,
                  tags=('pragmatics','transfer','v20')))

# ---------------------------------------------------------------------------
# 6) Level scaffolding for new chunks/pragmatics/phonology.
# ---------------------------------------------------------------------------
for lvl in ['A1','A2','B1','B2','C1','C2']:
    # Soft connection from listening/pragmatics to formulaic competence of same level.
    chunks=[x['id'] for x in kcs if x['domain']=='CHUNK' and x['cefr']==lvl]
    prags=[x['id'] for x in kcs if x['domain']=='PRAGMATICS' and x['cefr']==lvl]
    for p in prags:
        for c in chunks[:3]: add_edge(c,p,.035,False)

# Sort stable.
kcs.sort(key=lambda x:(ORDER.get(x.get('cefr','A1'),99),x.get('domain',''),x.get('name','').casefold(),x['id']))
edges.sort(key=lambda x:(x['fromId'],x['toId']))
exercises.sort(key=lambda x:(ORDER.get(x.get('cefr','A1'),99),x.get('type',''),x['id']))
lexemes.sort(key=lambda x:(ORDER.get(x.get('cefr','A1'),99),x.get('frequencyRank') if isinstance(x.get('frequencyRank'),int) else 10**9,x['lemma'].casefold()))

save('kcs.json',kcs);save('edges.json',edges);save('exercises.json',exercises);save('lexemes.json',lexemes)
manifest['version']='2.0.0';manifest['contentVersion']='2.0.0'
manifest['description']='SIAA v2.0 Content Complete Candidate: A1–C2 con profundidad gramatical, léxico, expresiones multiword, pragmática, listening metacognitivo y fonología perceptiva; audio offline pre-renderizado.'
manifest['coverage']={
    'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exercises),'lexemes':len(lexemes),
    'multiwordKcs':sum(1 for x in kcs if x['domain']=='CHUNK'),
    'listeningItems':sum(1 for x in exercises if x['type']=='LISTENING_AB'),
    'pronunciationItems':sum(1 for x in exercises if x['type']=='PRON_DISCRIMINATION'),
    'generatedVariantsEstimate':sum(3+min(3,len(x.get('chunks',[]))) for x in lexemes),
    'levels':['Pre-A1','A1','A2','B1','B2','C1','C2']
}
src=list(manifest.get('sources',[]))
for s in [
  'SIAA original multiword/collocation authoring v2.0 (levelled against CEFR-J/Core Inventory guidance)',
  'CEFR Companion Volume 2020 phonological and pragmatic descriptor guidance',
  'Vandergrift & Goh metacognitive listening sequence (planning, monitoring, problem-solving, evaluation)'
]:
    if s not in src: src.append(s)
manifest['sources']=src
manifest['checksums']={n:hashlib.sha256((CONTENT/n).read_bytes()).hexdigest() for n in ('kcs.json','edges.json','exercises.json','lexemes.json')}
save('manifest.json',manifest)

print(json.dumps({
 'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exercises),'lexemes':len(lexemes),
 'chunks':sum(1 for x in kcs if x['domain']=='CHUNK'),
 'listening':sum(1 for x in exercises if x['type']=='LISTENING_AB'),
 'pronunciation':sum(1 for x in exercises if x['type']=='PRON_DISCRIMINATION'),
 'pragmatics':sum(1 for x in kcs if x['domain']=='PRAGMATICS'),
 'grammarObjective':sum(1 for x in exercises if 'objective' in x.get('tags',[]) and 'grammar' in x.get('tags',[]))
},ensure_ascii=False,indent=2))
