#!/usr/bin/env python3
from __future__ import annotations
import csv, hashlib, json, re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'
REF=ROOT/'app/src/main/assets/reference'
OVERLAY=REF/'open_lexicon/contexto_cefr_overlay.json'

LEVEL_ORDER={'Pre-A1':0,'A1':1,'A2':2,'B1':3,'B2':4,'C1':5,'C2':6}

def load(name): return json.loads((CONTENT/name).read_text(encoding='utf-8'))
def save(name,obj): (CONTENT/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def safe(s): return re.sub(r'[^A-Z0-9]+','_',s.upper()).strip('_') or 'X'
def kc(id,name,cefr,domain,form='',meaning='',use='',importance=.6,prior=.07,tags=()):
    return {'id':id,'name':name,'cefr':cefr,'domain':domain,'form':form,'meaning':meaning,'use':use,'importance':importance,'priorMastery':prior,'tags':list(tags)}
def edge(a,b,w=.5,hard=False): return {'fromId':a,'toId':b,'weight':w,'hardPrerequisite':hard}
def ex(id,type,kcs,cefr,diff,prompt,stim='',a='',b='',correct='',exp='',spell='',seconds=20,tags=(),mis=()):
    return {'id':id,'type':type,'kcIds':kcs,'cefr':cefr,'difficulty':diff,'promptEs':prompt,'stimulusEn':stim,'optionA':a,'optionB':b,'correctOption':correct,'explanationEs':exp,'spellTarget':spell,'estimatedSeconds':seconds,'tags':list(tags),'misconceptionIds':list(mis)}

kcs=load('kcs.json'); edges=load('edges.json'); exercises=load('exercises.json'); lexemes=load('lexemes.json')
kid={x['id'] for x in kcs}; eid={x['id'] for x in exercises}; lemmas={x['lemma'].lower():x for x in lexemes}
edgekeys={(x['fromId'],x['toId']) for x in edges}

def add_kc(x):
    if x['id'] not in kid: kcs.append(x); kid.add(x['id'])
def add_edge(a,b,w=.5,hard=False):
    if a in kid and b in kid and (a,b) not in edgekeys:
        edges.append(edge(a,b,w,hard)); edgekeys.add((a,b))
def add_ex(x):
    if x['id'] not in eid: exercises.append(x); eid.add(x['id'])
def add_lex(x):
    key=x['lemma'].lower()
    if key not in lemmas:
        lexemes.append(x); lemmas[key]=x; return True
    return False

def spelling_difficulty(word):
    w=word.lower()
    score=.24 + min(len(w),12)*.018
    for pat,val in [('ough',.28),('igh',.20),('tion',.12),('sion',.12),('ph',.08),('kn',.08),('wr',.08),('ee',.05),('ea',.05),('ie',.06)]:
        if pat in w: score+=val
    return min(.95,score)

def choose_distractor(level, meaning, idx):
    pools={
      'A1':['trabajo','casa','persona','tiempo','comida','ciudad','familia','dinero'],
      'A2':['experiencia','problema','viaje','salud','opción','cambio','reunión','resultado'],
      'B1':['proceso','evidencia','objetivo','riesgo','enfoque','efecto','apoyo','decisión'],
      'B2':['restricción','consecuencia','marco','evaluación','suposición','recurso','tendencia','alcance'],
      'C1':['matiz','postura','implicación','coherencia','premisa','sesgo','planteamiento','criterio'],
      'C2':['ambigüedad','connotación','sutileza','retórica','paradoja','presuposición','reformulación','mediación']}
    p=pools.get(level,pools['B1'])
    for off in range(len(p)):
        d=p[(idx+off)%len(p)]
        if d.lower() not in meaning.lower() and meaning.lower() not in d.lower(): return d
    return 'otra idea'

# ---------------------------------------------------------------------------
# 1. Large conservative bilingual lexical overlay (open-data derived)
# ---------------------------------------------------------------------------
overlay=json.loads(OVERLAY.read_text(encoding='utf-8'))
added_overlay=0
for i,row in enumerate(overlay):
    lemma,meaning,pos,level,rank=row
    if level not in LEVEL_ORDER: continue
    lx_id='V_'+safe(lemma)
    # Existing seed entries are more deliberately authored; keep them.
    if lemma.lower() in lemmas: continue
    spec={'id':lx_id,'lemma':lemma,'meaningEs':meaning,'cefr':level,
          'frequencyRank':rank if isinstance(rank,int) else None,
          'spellingDifficulty':spelling_difficulty(lemma),
          'distractorEs':choose_distractor(level,meaning,i),'chunks':[],
          'exampleFrames':[],
          'tags':['cefrj','contexto-consensus','open-data','pos:'+pos]}
    if add_lex(spec):
        add_kc(kc(lx_id,lemma,level,'VOCABULARY',lemma,meaning,'reconocimiento auditivo, significado y uso léxico',.64,.06,('lexeme','cefrj','open-data','pos:'+pos)))
        added_overlay+=1

# ---------------------------------------------------------------------------
# 2. Hand-curated high-yield verbs/adjectives + advanced academic/pragmatic lexis
#    Original SIAA authoring; fills POS/level gaps left by the conservative overlay.
# ---------------------------------------------------------------------------
CURATED={
'A1': '''go|ir\ncome|venir\nmake|hacer/crear\ndo|hacer\nget|obtener/llegar\ngive|dar\ntake|tomar/llevar\nknow|saber/conocer\nthink|pensar\nsee|ver\nlook|mirar\nsay|decir\ntell|decir/contar\nask|preguntar\nhelp|ayudar\nfind|encontrar\nuse|usar\ncall|llamar\ntry|intentar\nfeel|sentir\nleave|salir/dejar\nput|poner\nkeep|mantener\nlet|dejar/permitir\nbegin|empezar\nstart|comenzar\nstop|parar\nopen|abrir\nclose|cerrar\nbuy|comprar\npay|pagar\nbring|traer\nsend|enviar\nwait|esperar\nwalk|caminar\nrun|correr\ndrive|conducir\nlearn|aprender\nstudy|estudiar\nteach|enseñar\nread|leer\nwrite|escribir\nspeak|hablar\nlisten|escuchar\nunderstand|entender\nremember|recordar\nforget|olvidar\ngood|bueno\nbad|malo\nbig|grande\nsmall|pequeño\nnew|nuevo\nold|viejo/antiguo\neasy|fácil\ndifficult|difícil\nhappy|feliz\ntired|cansado\nready|listo\nimportant|importante''',
'A2': '''travel|viajar\nchoose|elegir\nchange|cambiar\nmove|mover/mudarse\nhappen|suceder\nbecome|volverse/convertirse\nmean|significar\nseem|parecer\nbelieve|creer\nhope|esperar/desear\ndecide|decidir\nexplain|explicar\nagree|estar de acuerdo\ndisagree|no estar de acuerdo\nplan|planear\narrive|llegar\nborrow|pedir prestado\nlend|prestar\ninvite|invitar\noffer|ofrecer\norder|pedir/ordenar\nbook|reservar\ncheck|comprobar\ncompare|comparar\ndescribe|describir\nrecommend|recomendar\nprefer|preferir\nneed|necesitar\nwant|querer\nshould|debería\npossible|posible\ndifferent|diferente\nsimilar|similar\ncomfortable|cómodo\navailable|disponible\ncareful|cuidadoso\nsafe|seguro\nhealthy|saludable\nexpensive|caro\ncheap|barato\nuseful|útil\nenough|suficiente''',
'B1': '''improve|mejorar\ndevelop|desarrollar\nincrease|aumentar\nreduce|reducir\nmanage|gestionar/lograr\nsupport|apoyar\nconsider|considerar\nexpect|esperar/prever\nsuggest|sugerir\navoid|evitar\nallow|permitir\nrequire|requerir\nachieve|lograr\nprovide|proporcionar\ninclude|incluir\nremain|permanecer\ncreate|crear\ncontinue|continuar\nsolve|resolver\nprevent|prevenir\nprotect|proteger\nmeasure|medir\nmonitor|monitorear\nreport|informar/reportar\ndepend|depender\naffect|afectar\napply|aplicar\nrecognize|reconocer\nrealize|darse cuenta\nmention|mencionar\nlikely|probable\nrecent|reciente\nreliable|confiable\neffective|eficaz\nefficient|eficiente\nenvironmental|ambiental\nresponsible|responsable\nsuitable|adecuado\nspecific|específico\ncommon|común''',
'B2': '''assess|evaluar\nevaluate|evaluar\nindicate|indicar\ninvolve|implicar/involucrar\nmaintain|mantener\nestablish|establecer\ndetermine|determinar\noccur|ocurrir\naddress|abordar\nenhance|mejorar/potenciar\nensure|garantizar\ncontribute|contribuir\nassume|suponer\ninterpret|interpretar\njustify|justificar\ndemonstrate|demostrar\nidentify|identificar\nemphasize|enfatizar\nacknowledge|reconocer/admitir\nchallenge|cuestionar/desafiar\nrelevant|pertinente\nsignificant|significativo\nsustainable|sostenible\nconsistent|coherente/consistente\nappropriate|apropiado\ncomplex|complejo\naccurate|preciso\nconsiderable|considerable\nunderlying|subyacente\noverall|general/global''',
'C1': '''derive|derivar\ninfer|inferir\nconvey|transmitir\naccount for|explicar/tener en cuenta\nunderpin|sustentar\narticulate|articular/expresar con precisión\nsubstantiate|fundamentar con evidencia\nreconcile|conciliar\nchallenge|cuestionar\ndelineate|delimitar/describir con precisión\ncharacterize|caracterizar\nconstrain|restringir\nmediate|mediar\nmitigate|mitigar\nnuance|matizar\nqualify|matizar/limitar una afirmación\nrefine|refinar\nsynthesize|sintetizar\nvalidate|validar\ncorroborate|corroborar\nrobust|robusto\nplausible|plausible\nsubtle|sutil\nimplicit|implícito\nexplicit|explícito\ncoherent|coherente\nambiguous|ambiguo\ntentative|tentativo/provisional\ncompelling|convincente\ncomprehensive|exhaustivo/amplio\nnotwithstanding|no obstante\nwhereas|mientras que\nnevertheless|sin embargo\narguably|podría sostenerse que\nconsequently|en consecuencia\nthereby|de ese modo\nhence|por lo tanto\nmoreover|además\nstance|postura\nimplication|implicación\npremise|premisa\nconstraint|restricción\ntrade-off|compensación entre objetivos\nframework|marco conceptual\nvalidity|validez\nreliability|fiabilidad\nbias|sesgo\nuncertainty|incertidumbre\nassumption|supuesto''',
'C2': '''connotation|connotación\nambiguity|ambigüedad\nrhetorical|retórico\nparaphrase|paráfrasis/reformular\nreframe|replantear\nhedge|matizar una afirmación\npresuppose|presuponer\nimplicate|implicar de forma no explícita\nallude|aludir\nelicit|provocar/obtener una respuesta\ndisambiguate|desambiguar\ncircumvent|sortear/eludir\nforeground|poner en primer plano\ndownplay|restar importancia\nunderscore|subrayar/enfatizar\nextrapolate|extrapolar\ntriangulate|triangular evidencia\nproblematize|problematizar\nrebut|refutar\nconcede|conceder un punto\ncontend|sostener/argumentar\npostulate|postular\nqualifier|matizador/elemento calificativo\nnuanced|matizado\nidiomatic|idiomático\nfigurative|figurado\ncolloquial|coloquial\nmarked|marcado/estilísticamente saliente\npragmatic|pragmático\nidiomaticity|idiomaticidad\nregister|registro\nsubtext|subtexto\nsubtlety|sutileza\nparadox|paradoja\nfallacy|falacia\nrhetoric|retórica\nmediation|mediación\nreformulation|reformulación\nimplicature|implicatura\npresupposition|presuposición\npolysemy|polisemia\ncollocation|colocación léxica\ncohesion|cohesión\ncoherence|coherencia\nintertextuality|intertextualidad\nmetadiscourse|metadiscurso'''
}
curated_added=0
for level,block in CURATED.items():
    for j,line in enumerate(block.strip().splitlines()):
        lemma,meaning=[x.strip() for x in line.split('|',1)]
        if lemma.lower() in lemmas: continue
        lx_id='V_'+safe(lemma)
        spec={'id':lx_id,'lemma':lemma,'meaningEs':meaning,'cefr':level,'frequencyRank':None,
              'spellingDifficulty':spelling_difficulty(lemma),'distractorEs':choose_distractor(level,meaning,j),
              'chunks':[],'exampleFrames':[], 'tags':['siaa-curated','high-yield']}
        if add_lex(spec):
            add_kc(kc(lx_id,lemma,level,'VOCABULARY',lemma,meaning,'comprensión auditiva, significado, forma y uso',.66,.06,('lexeme','siaa-curated')))
            curated_added+=1

# Auto-create lexical teaching/retrieval exercises for every lexeme that has none in static bank.
# Runtime generator adds additional meaning/spelling/chunk variants at install time.
for idx,lx in enumerate(lexemes):
    level=lx.get('cefr','A1'); kid0=lx['id']; lemma=lx['lemma']; meaning=lx['meaningEs']
    if f'TLEX_{safe(kid0)}' not in eid:
        add_ex(ex(f'TLEX_{safe(kid0)}','TEACH',[kid0],level,.20 if level=='A1' else .35,
                  f'Nueva unidad léxica. Escucha {lemma}. Significa: {meaning}.',lemma,
                  exp=f'{lemma}: {meaning}. Después entrenarás sonido, significado y escritura.',spell=lemma,seconds=20,
                  tags=('lexical','teach','v11')))
    if f'RLEX_{safe(kid0)}' not in eid:
        add_ex(ex(f'RLEX_{safe(kid0)}','SELF_ASSESS',[kid0],level,.38 if level in ('A1','A2') else .55,
                  f'Escucha {lemma}. Recupera mentalmente su significado y una frase posible.',lemma,
                  exp=f'Significado objetivo: {meaning}.',spell=lemma,seconds=18,tags=('lexical','recall','v11')))

# ---------------------------------------------------------------------------
# 3. CEFR-J grammar profile: activate every item that has a defensible CEFR level.
# ---------------------------------------------------------------------------
def norm_level(row):
    for col in ('CEFR-J Level','Core Inventory','GSELO','EGP'):
        v=(row.get(col) or '').upper()
        m=re.search(r'\b(A1|A2|B1|B2|C1|C2)\b',v)
        if m: return m.group(1)
    return None

def sentence_type_es(st):
    st=(st or '').upper()
    if 'INT' in st: return 'pregunta'
    if 'NEG' in st: return 'forma negativa'
    if 'IMP' in st: return 'instrucción o imperativo'
    if 'DEC' in st: return 'enunciado declarativo'
    return 'construcción gramatical'

grammar_rows=[]
with open(REF/'cefrj/cefrj-grammar-profile-20180315.csv',encoding='utf-8-sig',newline='') as f:
    grammar_rows=list(csv.DictReader(f))
level_anchor={'A1':'G_BE','A2':'G_PAST_SIMPLE','B1':'G_PERFECT','B2':'G_COND1','C1':'G_PERFECT','C2':'G_PERFECT'}
grammar_added=0
for row in grammar_rows:
    level=norm_level(row)
    item=(row.get('Grammatical Item') or '').strip()
    if not level or not item: continue
    code=(row.get('Shorthand Code') or row.get('ID') or item).strip()
    gid='G_CEFRJ_'+safe(code)[:80]
    if gid in kid: continue
    st=(row.get('Sentence Type') or '').strip()
    meaning=sentence_type_es(st)
    source_tags=['cefrj-grammar','open-language-profiles','grammar-profile']
    add_kc(kc(gid,item,level,'GRAMMAR',item,meaning,'reconocer y producir esta construcción en contexto',.58,.045,source_tags))
    anchor=level_anchor.get(level)
    if anchor in kid: add_edge(anchor,gid,.20,False)
    difficulty={'A1':.28,'A2':.40,'B1':.53,'B2':.64,'C1':.75,'C2':.84}[level]
    add_ex(ex(f'T_{gid}','TEACH',[gid],level,max(.18,difficulty-.15),
              f'Observa esta construcción: {item}. Identifica su forma antes de escucharla.',item,
              exp=f'Construcción objetivo: {item}. Función de esta entrada: {meaning}.',seconds=18,
              tags=('grammar','noticing','cefrj','v11')))
    add_ex(ex(f'R_{gid}','SELF_ASSESS',[gid],level,difficulty,
              f'Construye mentalmente un ejemplo nuevo que use la forma: {item}.',item,
              exp='Comprueba que mantuviste la estructura objetivo sin copiar literalmente el ejemplo.',seconds=22,
              tags=('grammar','productive-recall','transfer','cefrj','v11')))
    grammar_added+=1

# ---------------------------------------------------------------------------
# 4. Functional/pragmatic curriculum derived from CEFR/Core Inventory categories.
#    Phrases/examples below are original SIAA authoring, not copied exercises.
# ---------------------------------------------------------------------------
FUNCTIONS={
'A1':[
('directions','Pedir y dar direcciones','Excuse me, where is the station?','pedir y dar una dirección básica'),
('personal_info','Dar información personal','I live in Lima and I study engineering.','dar nombre, origen, residencia o actividad'),
('greetings','Saludar y despedirse','Hi. Nice to meet you. See you later.','abrir y cerrar una interacción breve'),
('time_numbers','Hora, números y precios','It is half past seven. It costs twelve dollars.','comprender y producir horas, cantidades y precios'),
('routine','Describir hábitos y rutinas','I usually take the bus at seven.','describir actividades habituales'),
],
'A2':[
('past_experience','Describir experiencias pasadas','I visited Cusco last year.','narrar una experiencia sencilla'),
('people_places_things','Describir personas, lugares y cosas','The room is small but very bright.','dar una descripción concreta'),
('obligation','Expresar obligación y necesidad','You have to show your ticket.','expresar obligación o necesidad'),
('requests','Hacer peticiones','Could you help me with this bag?','formular una petición adecuada'),
('suggestions','Hacer sugerencias','Why don’t we meet after work?','proponer una acción'),
('advice','Dar consejo','You should rest and drink water.','dar un consejo sencillo'),
],
'B1':[
('check_understanding','Comprobar comprensión','So, do you mean the meeting starts at nine?','comprobar e interpretar comprensión'),
('feelings','Describir sentimientos','I was relieved when I heard the news.','expresar emociones y reacciones'),
('opinions','Expresar y justificar opiniones','I think this option is better because it costs less.','opinar y justificar'),
('agree_disagree','Acordar y discrepar','I see your point, but I’m not completely convinced.','acordar o discrepar con cortesía'),
('interaction_management','Gestionar la interacción','Sorry to interrupt. Can I add something?','interrumpir, cambiar de tema o retomar'),
('conversation_open_close','Iniciar y cerrar conversación','It was great talking to you. I should get going.','gestionar apertura y cierre'),
],
'B2':[
('critique_review','Criticar y reseñar','The proposal is practical, although its cost is a serious limitation.','evaluar fortalezas y debilidades'),
('hopes_plans','Expresar esperanzas y planes','I’m hoping to finish the project by the end of the month.','expresar planes con matiz'),
('argument','Desarrollar un argumento','The main reason is cost; moreover, the alternative is easier to maintain.','organizar razones y evidencia'),
('abstract_ideas','Expresar ideas abstractas','Freedom involves responsibility as well as choice.','hablar de conceptos abstractos'),
('speculation','Especular','They may have missed the train, but I’m not sure.','expresar hipótesis y grados de certeza'),
('synthesis','Sintetizar y evaluar información','Overall, the two sources agree on the trend but differ on its cause.','integrar información de más de una fuente'),
('informal_reaction','Reaccionar informalmente','No way! That’s amazing.','mostrar interés, sorpresa, simpatía o indiferencia'),
],
'C1':[
('concede','Conceder un punto','Admittedly, the method is expensive; nevertheless, it offers better control.','conceder sin abandonar el argumento'),
('defend_view','Defender un punto de vista','The evidence strongly supports this interpretation, particularly when the two datasets are combined.','defender una posición persuasivamente'),
('systematic_argument','Desarrollar argumento sistemático','First, the assumption is questionable. More importantly, the data point in the opposite direction.','estructurar una argumentación compleja'),
('hedging','Matizar una opinión','This may indicate a relationship, although the evidence remains limited.','expresar cautela epistémica'),
('certainty_shades','Expresar grados finos de certeza','It is highly likely, but by no means certain, that the change was deliberate.','graduar certeza, probabilidad y duda'),
('counterargument','Responder a contraargumentos','That objection is valid in principle; however, it does not explain the later observations.','responder a una objeción'),
('hypothesise','Especular sobre causas y consecuencias','Had the policy been introduced earlier, the outcome might have been different.','hipotetizar con relaciones complejas'),
],
'C2':[
('reframe','Reformular perspectivas','Another way of framing the issue is to distinguish feasibility from desirability.','reformular una idea sin perder matices'),
('implicit_stance','Interpretar y expresar postura implícita','He stopped short of calling the proposal impossible.','manejar significado implícito y postura'),
('diplomacy','Discrepar con diplomacia avanzada','I would hesitate to draw that conclusion from the evidence available.','proteger la imagen del interlocutor al discrepar'),
('irony_subtext','Interpretar ironía y subtexto','That went brilliantly, he said after everything had failed.','interpretar significado no literal'),
('mediate_complex','Mediar contenido complejo','In simpler terms, the author accepts the goal but questions the proposed mechanism.','reexpresar para otro interlocutor o propósito'),
]
}
prev_by_level={}
for level,items in FUNCTIONS.items():
    for n,(slug,name,stim,use) in enumerate(items):
        pid='PR_'+safe(level+'_'+slug)
        add_kc(kc(pid,name,level,'PRAGMATICS',stim,'función comunicativa',use,.62,.05,('pragmatics','core-inventory-inspired','v11')))
        if n>0: add_edge(prev_by_level[level],pid,.10,False)
        prev_by_level[level]=pid
        diff={'A1':.25,'A2':.36,'B1':.50,'B2':.63,'C1':.76,'C2':.86}[level]
        add_ex(ex(f'T_{pid}','TEACH',[pid],level,diff-.10,f'Función comunicativa: {name}. Escucha un modelo.',stim,
                  exp=f'Objetivo: {use}.',seconds=22,tags=('pragmatics','teach','v11')))
        add_ex(ex(f'R_{pid}','SELF_ASSESS',[pid],level,diff,f'Imagina una situación distinta y construye mentalmente una respuesta que permita: {use}.',stim,
                  exp='No memorices sólo la frase modelo: conserva la función y adapta la forma al contexto.',seconds=26,tags=('pragmatics','transfer','v11')))

# ---------------------------------------------------------------------------
# 5. Listening-by-process + original graded scripts. Every script becomes an
#    exact audio asset later in generate_audio_bank.py.
# ---------------------------------------------------------------------------
LISTENING_KCS=[
('A1','L11_WORD_BOUND','Segmentación de palabras','detectar límites entre palabras en frases cortas'),
('A1','L11_KEY_DETAILS','Detalles básicos','extraer quién, dónde, cuándo, número o precio'),
('A1','L11_INSTRUCTIONS','Instrucciones breves','seguir una instrucción de uno o dos pasos'),
('A2','L11_GIST','Idea principal','captar el tema y propósito global de un mensaje breve'),
('A2','L11_SEQUENCE','Secuencia temporal','reconstruir orden con before, after, then y finally'),
('A2','L11_REFERENCE','Referencia pronominal','resolver a quién o qué refiere un pronombre'),
('B1','L11_MAIN_SUPPORT','Idea principal y apoyo','distinguir afirmación principal y detalle de apoyo'),
('B1','L11_ATTITUDE','Actitud básica','inferir aprobación, preocupación, duda o alivio'),
('B1','L11_CONTEXT_INFER','Inferencia por contexto','inferir significado aproximado de un elemento desconocido'),
('B2','L11_ARGUMENT','Seguimiento de argumento','seguir afirmación, razón, contraste y conclusión'),
('B2','L11_IMPLICIT_INTENT','Intención implícita','inferir lo que el hablante intenta lograr'),
('B2','L11_CONNECTED_SPEECH','Habla conectada','mantener segmentación con reducciones y enlaces'),
('C1','L11_STANCE','Stance','inferir grado de compromiso, distancia o evaluación'),
('C1','L11_DISTRIBUTED','Información distribuida','integrar información separada a lo largo del discurso'),
('C1','L11_DISCOURSE_ORG','Organización discursiva','seguir cambios de enfoque sin marcadores explícitos'),
('C2','L11_SUBTEXT','Subtexto e implicatura','interpretar significado no literal dependiente del contexto'),
('C2','L11_FAST_DENSE','Habla rápida y densa','mantener comprensión de discurso complejo con alta densidad informativa'),
('C2','L11_MEDIATION','Síntesis auditiva','extraer y reformular la tesis y reservas de un discurso complejo'),
]
for level,lid0,name,meaning in LISTENING_KCS:
    add_kc(kc(lid0,name,level,'LISTENING','procesamiento auditivo',meaning,meaning,.68,.05,('listening-process','v11')))

SCRIPTS={
'A1':[
('a1_bus_time','The bus leaves at seven thirty.','¿A qué hora sale el autobús?','seven thirty','eight thirty','A','El dato explícito es seven thirty.','L11_KEY_DETAILS'),
('a1_meeting_place','Meet me outside the library.','¿Dónde deben encontrarse?','outside the library','inside the station','A','La ubicación es outside the library.','L11_KEY_DETAILS'),
('a1_instruction','Open the door and wait here.','¿Qué debe hacer primero?','open the door','leave the building','A','La primera acción es open the door.','L11_INSTRUCTIONS'),
('a1_price','The ticket is twelve dollars.','¿Cuánto cuesta el boleto?','twelve dollars','twenty dollars','A','El precio escuchado es twelve dollars.','L11_KEY_DETAILS'),
('a1_person','Maria works at the hotel.','¿Dónde trabaja Maria?','at the hotel','at the hospital','A','La frase dice at the hotel.','L11_WORD_BOUND'),
],
'A2':[
('a2_sequence','First I called the office, then I sent an email, and finally I went there in person.','¿Qué hizo después de llamar?','sent an email','went home','A','Then I sent an email marca el segundo paso.','L11_SEQUENCE'),
('a2_gist','The weather will be rainy this afternoon, so the outdoor concert will move to the sports hall.','¿Cuál es la idea principal?','the concert changes location because of rain','the concert is cancelled forever','A','El mensaje anuncia un cambio de lugar por la lluvia.','L11_GIST'),
('a2_reference','Ana gave Rosa the key because she was leaving early. Rosa kept it until the evening.','¿Quién conservó la llave?','Rosa','Ana','A','La segunda oración identifica a Rosa.','L11_REFERENCE'),
('a2_trip','Our train was late, but we still arrived before the museum closed.','¿Qué ocurrió?','they arrived before closing time','they missed the museum completely','A','Still arrived before the museum closed expresa el resultado.','L11_GIST'),
],
'B1':[
('b1_support','I prefer working from home because I can concentrate better. However, I still go to the office for team meetings.','¿Por qué prefiere trabajar desde casa?','because concentration is easier','because meetings are impossible','A','La razón explícita es better concentration.','L11_MAIN_SUPPORT'),
('b1_attitude','I was worried when the results were delayed, but I felt much better once the laboratory confirmed everything was normal.','¿Cómo cambia su actitud?','from worried to relieved','from excited to angry','A','El hablante pasa de worried a sentirse better.','L11_ATTITUDE'),
('b1_infer','The road was flooded, so the driver took a different route. We reached the station ten minutes late.','¿Qué se puede inferir sobre flooded?','water covered the road','the road was empty and dry','A','El cambio de ruta y el contexto permiten inferir presencia de agua.','L11_CONTEXT_INFER'),
('b1_plan','The team will test the new system on Monday. If the test goes well, everyone will start using it next week.','¿De qué depende el inicio general?','the Monday test','a new office','A','If the test goes well establece la condición.','L11_MAIN_SUPPORT'),
],
'B2':[
('b2_argument','The cheaper sensor looks attractive at first. Nevertheless, it needs calibration twice as often, so its total operating cost may actually be higher.','¿Cuál es la conclusión del hablante?','the cheaper sensor may cost more over time','calibration has no effect on cost','A','El argumento distingue precio inicial de costo total.','L11_ARGUMENT'),
('b2_intent','You could submit the report today, but I would check the figures one more time if I were you.','¿Qué intenta comunicar principalmente?','a cautious recommendation to review the figures','an order to delete the report','A','La forma condicional suaviza una recomendación.','L11_IMPLICIT_INTENT'),
('b2_contrast','Most participants supported the proposal. Even so, several raised concerns about how the data would be stored.','¿Qué contraste aparece?','general support versus privacy concerns','no one supported the proposal','A','Even so introduce una reserva frente al apoyo general.','L11_ARGUMENT'),
('b2_connected','What did you want to do after the meeting?','¿Qué pregunta hace el hablante?','what action was planned after the meeting','where the meeting happened yesterday','A','La cadena debe segmentarse pese al habla conectada.','L11_CONNECTED_SPEECH'),
],
'C1':[
('c1_stance','The association is suggestive, but the evidence is hardly conclusive. At best, it justifies a more targeted study.','¿Cuál es la postura?','cautious and provisional','completely certain','A','Hardly conclusive y at best expresan cautela.','L11_STANCE'),
('c1_distributed','Early in the presentation, the speaker says costs fell. Much later, she explains that the reduction came mainly from postponing maintenance rather than improving efficiency.','¿Cómo debe interpretarse la caída de costos?','it may reflect postponed maintenance rather than efficiency','it proves permanent efficiency gains','A','La explicación relevante aparece separada de la afirmación inicial.','L11_DISTRIBUTED'),
('c1_structure','The proposal sounds straightforward. The difficulty is not the technology itself. What matters is whether institutions can coordinate their decisions over time.','¿Dónde sitúa el problema central?','institutional coordination','basic technical feasibility','A','El hablante desplaza el foco de tecnología a coordinación.','L11_DISCOURSE_ORG'),
('c1_hedge','The pattern may be consistent with a causal effect, although alternative explanations cannot yet be ruled out.','¿Qué grado de certeza expresa?','a plausible but unconfirmed interpretation','absolute proof','A','May y cannot yet be ruled out mantienen incertidumbre.','L11_STANCE'),
],
'C2':[
('c2_subtext','When asked whether the reform had succeeded, the director replied, “It has certainly generated a great deal of discussion.”','¿Qué sugiere la respuesta?','the director avoids directly claiming success','the director gives an unequivocal yes','A','La respuesta elude la afirmación solicitada y desplaza el criterio.','L11_SUBTEXT'),
('c2_dense','The apparent consensus dissolves once the categories are disaggregated: what looks stable at the aggregate level masks opposing trends across subgroups.','¿Qué problema describe?','aggregation hides opposing subgroup trends','all subgroups move identically','A','La afirmación contrasta nivel agregado y subgrupos.','L11_FAST_DENSE'),
('c2_mediation','The author accepts the ethical objective but rejects the proposed mechanism, arguing that it would shift costs onto the very population it is intended to protect.','¿Cuál es la síntesis más fiel?','the goal is accepted but the mechanism is criticized for its distributional effects','both the goal and mechanism are rejected','A','La síntesis preserva la concesión y la crítica.','L11_MEDIATION'),
('c2_irony','After the third system failure that morning, she looked at the error message and said, “Excellent. Exactly what we needed.”','¿Cómo debe interpretarse excellent?','ironically, as frustration','literally, as satisfaction','A','El contexto hace incompatible una lectura literal positiva.','L11_SUBTEXT'),
]
}
for level,items in SCRIPTS.items():
    for slug,stim,prompt,a,b,corr,expl,lid0 in items:
        xid='LQ_'+safe(slug)
        add_ex(ex(xid,'LISTENING_AB',[lid0],level,{'A1':.28,'A2':.40,'B1':.54,'B2':.66,'C1':.78,'C2':.88}[level],prompt,stim,a,b,corr,expl,seconds=26,tags=('listening','original-script','audio-asset','v11')))

# ---------------------------------------------------------------------------
# 6. Pronunciation / connected-speech targets; paired forms are original labels.
# ---------------------------------------------------------------------------
PHONO=[
('A1','P11_I_IY','/ɪ/ vs /iː/','ship / sheep','distinguir vocal corta y larga'),
('A1','P11_B_V','/b/ vs /v/','berry / very','distinguir /b/ y /v/'),
('A1','P11_FINAL_S','Terminaciones -s','cats / dogs / buses','reconocer /s/, /z/ y /ɪz/'),
('A2','P11_TH','/θ/ y /ð/','think / this','reconocer fricativas dentales'),
('A2','P11_ED','Terminaciones -ed','worked / played / wanted','reconocer /t/, /d/ y /ɪd/'),
('A2','P11_SCHWA','Schwa','about / support','reconocer vocal reducida en sílaba átona'),
('B1','P11_LINKING','Linking','pick it up','resegmentar palabras enlazadas'),
('B1','P11_SENT_STRESS','Sentence stress','I NEED the report TODAY','detectar prominencia informativa'),
('B2','P11_ELISION','Elisión y reducción','next day / want to','tolerar reducción en habla natural'),
('B2','P11_NUCLEAR','Nuclear stress','I wanted the RED one','identificar foco contrastivo'),
('C1','P11_STANCE_INTON','Entonación y stance','Really? / Really.','inferir postura por prosodia'),
('C1','P11_CHUNKING','Chunking prosódico','when the data arrived | we checked it again','segmentar unidades informativas'),
('C2','P11_FINE_VARIATION','Variación fonética fina','speaker and style variation','mantener comprensión con variación sutil'),
]
for level,pid,name,stim,meaning in PHONO:
    add_kc(kc(pid,name,level,'PHONOLOGY',stim,meaning,meaning,.64,.05,('phonology','perception','v11')))
    add_ex(ex('T_'+pid,'TEACH',[pid],level,.30 if level in ('A1','A2') else .58,
              f'Entrena este contraste perceptivo: {name}.',stim,exp=f'Objetivo: {meaning}.',seconds=18,tags=('pronunciation','teach','v11')))
    add_ex(ex('Q_'+pid,'SELF_ASSESS',[pid],level,.42 if level in ('A1','A2') else .68,
              f'Escucha el patrón y decide mentalmente qué rasgo acústico distingue las realizaciones: {name}.',stim,
              exp=f'Revisa el rasgo objetivo: {stim}.',seconds=20,tags=('pronunciation','perception','v11')))

# ---------------------------------------------------------------------------
# 7. Soft edges to letters for spelling, and level scaffold for lexical items.
# ---------------------------------------------------------------------------
letter_ids={x['id'] for x in kcs if x['domain']=='LETTER'}
for lx in lexemes:
    for ch in set(lx['lemma'].upper()):
        lid='L_'+ch
        if lid in letter_ids: add_edge(lid,lx['id'],.02,False)

# Sort predictably: preserve semantic hierarchy via level/domain/name, while keeping IDs stable.
# We intentionally do not rewrite old IDs or state keys.
level=lambda x: LEVEL_ORDER.get(x.get('cefr','A1'),99)
kcs.sort(key=lambda x:(level(x),x.get('domain',''),x.get('name','').lower(),x['id']))
edges.sort(key=lambda x:(x['fromId'],x['toId']))
exercises.sort(key=lambda x:(LEVEL_ORDER.get(x.get('cefr','A1'),99),x.get('type',''),x['id']))
lexemes.sort(key=lambda x:(LEVEL_ORDER.get(x.get('cefr','A1'),99),x.get('frequencyRank') if isinstance(x.get('frequencyRank'),int) else 10**9,x['lemma'].lower()))

save('kcs.json',kcs); save('edges.json',edges); save('exercises.json',exercises); save('lexemes.json',lexemes)
checks={name:hashlib.sha256((CONTENT/name).read_bytes()).hexdigest() for name in ('kcs.json','edges.json','exercises.json','lexemes.json')}
manifest=load('manifest.json')
manifest.update({
  'version':'1.1.0','contentVersion':'1.1.0','schemaVersion':'1.0',
  'description':'SIAA content pack v1.1: currículo auditivo adaptativo ampliado A1–C2 con léxico bilingüe, gramática CEFR-J, pragmática funcional, listening original, pronunciación y ortografía.',
  'checksums':checks,
  'coverage':{
      'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exercises),'lexemes':len(lexemes),
      'generatedVariantsEstimate':sum(3+min(3,len(x.get('chunks',[]))) for x in lexemes),
      'levels':['Pre-A1','A1','A2','B1','B2','C1','C2']
  },
  'sources':[
      'CEFR Companion Volume 2020',
      'CEFR-J / Open Language Profiles vocabulary and grammar profiles',
      'British Council / EAQUALS Core Inventory for General English (curriculum reference)',
      'Nation - Learning Vocabulary in Another Language and BNC/COCA frequency principles',
      'Lightbown & Spada - How Languages are Learned',
      'Vandergrift & Goh - Teaching and Learning Second Language Listening',
      'Celce-Murcia et al. - Teaching Pronunciation',
      'Larsen-Freeman & Celce-Murcia - The Grammar Book',
      'American English / U.S. Department of State Teaching Pragmatics resources',
      'Jason-Latz/contexto MIT Spanish language pack (derived lexical overlay)'
  ]
})
save('manifest.json',manifest)
print(json.dumps({'overlayAdded':added_overlay,'curatedAdded':curated_added,'grammarAdded':grammar_added,'kcs':len(kcs),'edges':len(edges),'exercises':len(exercises),'lexemes':len(lexemes),'generatedEstimate':manifest['coverage']['generatedVariantsEstimate']},ensure_ascii=False,indent=2))
