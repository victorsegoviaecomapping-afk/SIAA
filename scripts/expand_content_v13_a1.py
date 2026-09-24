#!/usr/bin/env python3
from __future__ import annotations
import csv, json, hashlib, re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'
REF=ROOT/'app/src/main/assets/reference/cefrj/cefrj-vocabulary-profile-1.5.csv'

def load(name): return json.loads((CONTENT/name).read_text(encoding='utf-8'))
def save(name,obj): (CONTENT/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def safe(s): return re.sub(r'[^A-Z0-9]+','_',s.upper()).strip('_') or 'X'

def spell_diff(word:str)->float:
    w=word.lower(); score=.22 + min(.16,max(0,len(w)-4)*.018)
    if any(x in w for x in ('ough','augh','eigh','igh','tion','sion','ph','wr','kn')): score += .18
    if any(x in w for x in ('ee','ea','ie','ei','oo')): score += .06
    return min(.82,score)

# A1 items are added deliberately one by one. The CEFR level comes from CEFR-J;
# Spanish glosses and short examples are original SIAA authoring (not copied dictionary text).
# Fields: lemma, Spanish gloss, POS, example/chunk.
A1 = [
('action','acción','noun','take action'),('actor','actor','noun','a famous actor'),('add','añadir','verb','add some water'),
('afraid','asustado','adjective','afraid of dogs'),('age','edad','noun','your age'),('album','álbum','noun','a photo album'),
('angry','enfadado','adjective','angry with me'),('apple','manzana','noun','an apple'),('april','abril','noun','in April'),
('apron','delantal','noun','wear an apron'),('arm','brazo','noun','my arm'),('art','arte','noun','modern art'),
('august','agosto','noun','in August'),('aunt','tía','noun','my aunt'),('autumn','otoño','noun','in autumn'),
('awake','despierto','adjective','still awake'),('baby','bebé','noun','a baby'),('ball','pelota','noun','a ball'),
('banana','plátano','noun','a banana'),('band','banda','noun','a music band'),('bank','banco','noun','at the bank'),
('bar','bar','noun','at the bar'),('baseball','béisbol','noun','play baseball'),('basketball','baloncesto','noun','play basketball'),
('bath','baño','noun','take a bath'),('bathroom','baño','noun','the bathroom'),('bean','frijol','noun','green beans'),
('bear','oso','noun','a brown bear'),('beautiful','hermoso','adjective','a beautiful place'),('bedroom','dormitorio','noun','my bedroom'),
('bee','abeja','noun','a bee'),('beef','carne de res','noun','beef and rice'),('bell','campana','noun','ring the bell'),
('bike','bicicleta','noun','ride a bike'),('bird','pájaro','noun','a small bird'),('birth','nacimiento','noun','date of birth'),
('birthday','cumpleaños','noun','happy birthday'),('biscuit','galleta','noun','a biscuit'),('black','negro','adjective','a black bag'),
('block','bloque','noun','one block away'),('blue','azul','adjective','a blue shirt'),('board','tablero','noun','write on the board'),
('boat','barco','noun','a small boat'),('bookstore','librería','noun','at the bookstore'),('boring','aburrido','adjective','a boring film'),
('bottle','botella','noun','a bottle of water'),('bottom','parte inferior','noun','at the bottom'),('bowl','tazón','noun','a bowl of soup'),
('box','caja','noun','a small box'),('boy','niño','noun','a young boy'),('boyfriend','novio','noun','her boyfriend'),
('break','descanso','noun','take a break'),('breathe','respirar','verb','breathe slowly'),('bright','brillante','adjective','a bright room'),
('broken','roto','adjective','a broken phone'),('brother','hermano','noun','my brother'),('brown','marrón','adjective','brown shoes'),
('brush','cepillo','noun','a toothbrush'),('bucket','balde','noun','a bucket'),('build','construir','verb','build a house'),
('burger','hamburguesa','noun','a burger'),('busy','ocupado','adjective','busy today'),('butter','mantequilla','noun','bread and butter'),
('butterfly','mariposa','noun','a butterfly'),('cake','pastel','noun','birthday cake'),('camp','campamento','noun','summer camp'),
('candy','caramelo','noun','some candy'),('cap','gorra','noun','wear a cap'),('care','cuidado','noun','take care'),
('carry','llevar','verb','carry a bag'),('cartoon','dibujo animado','noun','watch a cartoon'),('case','caso','noun','in this case'),
('cat','gato','noun','a cat'),('catch','atrapar','verb','catch the ball'),('celebrate','celebrar','verb','celebrate a birthday'),
('celebration','celebración','noun','a family celebration'),('character','personaje','noun','a main character'),('chocolate','chocolate','noun','dark chocolate'),
('church','iglesia','noun','near the church'),('cinema','cine','noun','go to the cinema'),('clean','limpio','adjective','a clean room'),
('clever','inteligente','adjective','a clever student'),('climb','escalar','verb','climb a hill'),('clock','reloj','noun','look at the clock'),
('closed','cerrado','adjective','the shop is closed'),('cloth','tela','noun','a piece of cloth'),('clothes','ropa','noun','clean clothes'),
('cloudy','nublado','adjective','a cloudy day'),('club','club','noun','join a club'),('coach','entrenador','noun','the team coach'),
('coat','abrigo','noun','wear a coat'),('coke','cola','noun','a glass of cola'),('cold','frío','adjective','a cold day'),
('collect','recoger','verb','collect the papers'),('collection','colección','noun','a book collection'),('concert','concierto','noun','go to a concert'),
('conversation','conversación','noun','a short conversation'),('cook','cocinar','verb','cook dinner'),('cookie','galleta','noun','a chocolate cookie'),
('cool','fresco','adjective','cool weather'),('cop','policía','noun','a police officer'),('copy','copia','noun','make a copy'),
('corn','maíz','noun','corn and beans'),('corner','esquina','noun','on the corner'),('correct','correcto','adjective','the correct answer'),
('couch','sofá','noun','sit on the couch'),('cousin','primo','noun','my cousin'),('cover','cubierta','noun','the book cover'),
('cow','vaca','noun','a cow'),('cream','crema','noun','ice cream'),('credit card','tarjeta de crédito','noun','pay by credit card'),
('cry','llorar','verb','the baby may cry'),('cup','taza','noun','a cup of tea'),('cut','cortar','verb','cut the paper'),
('cute','lindo','adjective','a cute dog'),('dad','papá','noun','my dad'),('daddy','papá','noun','her daddy'),
('dance','bailar','verb','dance together'),('dancing','baile','noun','go dancing'),('dark','oscuro','adjective','a dark room'),
('date','fecha','noun','today’s date'),('daughter','hija','noun','their daughter'),('dear','querido','adjective','a dear friend'),
('december','diciembre','noun','in December'),('delicious','delicioso','adjective','delicious food'),('design','diseño','noun','a simple design'),
('dig','cavar','verb','dig a hole'),('dining room','comedor','noun','in the dining room'),('dirty','sucio','adjective','dirty shoes'),
('discuss','discutir','verb','discuss the problem'),('dish','plato','noun','a hot dish'),('doll','muñeca','noun','a toy doll'),
('dollar','dólar','noun','ten dollars'),('drama','drama','noun','a television drama'),('draw','dibujar','verb','draw a picture'),
('dream','sueño','noun','a strange dream'),('dress','vestido','noun','a red dress'),('drink','bebida','noun','a cold drink'),
('driver','conductor','noun','the bus driver'),('drop','gota','noun','a drop of water'),('drum','tambor','noun','play the drums'),
('dry','seco','adjective','keep it dry'),('dvd','DVD','noun','a DVD'),('ear','oreja','noun','my ear'),
('early','temprano','adverb','arrive early'),('eight','ocho','number','eight people'),('eighteen','dieciocho','number','eighteen years old'),
('eighty','ochenta','number','eighty percent'),('elementary','elemental','adjective','elementary school'),('eleven','once','number','eleven o’clock'),
('end','final','noun','the end of the road'),('engineer','ingeniero','noun','an environmental engineer'),('enjoy','disfrutar','verb','enjoy the trip'),
('everyday','cotidiano','adjective','everyday English'),('excellent','excelente','adjective','an excellent idea'),('excited','emocionado','adjective','excited about the trip'),
('exciting','emocionante','adjective','an exciting game'),('excuse','excusa','noun','an excuse'),('face','cara','noun','wash your face'),
('fair','justo','adjective','a fair decision'),('fairy','hada','noun','a fairy tale'),('false','falso','adjective','a false statement'),
('famous','famoso','adjective','a famous actor'),('farm','granja','noun','on a farm'),('farmer','granjero','noun','a local farmer'),
('fast','rápido','adjective','a fast car'),('february','febrero','noun','in February'),('feed','alimentar','verb','feed the dog'),
('feeling','sentimiento','noun','a good feeling'),('field','campo','noun','a green field'),('fifteen','quince','number','fifteen minutes'),
('fifty','cincuenta','number','fifty dollars'),('fight','pelea','noun','a fight'),('fill','llenar','verb','fill the bottle'),
('finish','terminar','verb','finish the work'),('fire','fuego','noun','a small fire'),('first','primero','adjective','the first day'),
('fishing','pesca','noun','go fishing'),('five','cinco','number','five minutes'),('flower','flor','noun','a red flower'),
('focus','enfocarse','verb','focus on the question'),('foggy','neblinoso','adjective','a foggy morning'),('following','siguiente','adjective','the following day'),
('football','fútbol','noun','play football'),('foreign','extranjero','adjective','a foreign language'),('foreigner','extranjero','noun','a foreigner'),
('form','formulario','noun','fill in the form'),('forty','cuarenta','number','forty minutes'),('four','cuatro','number','four people'),
('fourteen','catorce','number','fourteen days'),('free','gratis','adjective','free entry'),('friday','viernes','noun','on Friday'),
('frog','rana','noun','a green frog'),('front','frente','noun','in front of the house'),('full','lleno','adjective','a full bottle'),
('fun','diversión','noun','have fun'),('funny','gracioso','adjective','a funny story'),('future','futuro','noun','in the future'),
('garbage','basura','noun','take out the garbage'),('garden','jardín','noun','in the garden'),('ghost','fantasma','noun','a ghost story'),
('girl','niña','noun','a young girl'),('girlfriend','novia','noun','his girlfriend'),('glad','contento','adjective','glad to help'),
('glasses','gafas','noun','wear glasses'),('gold','oro','noun','made of gold'),('good afternoon','buenas tardes','expression','Good afternoon!'),
('good morning','buenos días','expression','Good morning!'),('good night','buenas noches','expression','Good night!'),('grandfather','abuelo','noun','my grandfather'),
('grandma','abuela','noun','my grandma'),('grandmother','abuela','noun','my grandmother'),('grandpa','abuelo','noun','my grandpa'),
('grandparent','abuelo o abuela','noun','a grandparent'),('grape','uva','noun','green grapes'),('grass','césped','noun','on the grass'),
('great','genial','adjective','a great idea'),('green','verde','adjective','a green shirt'),('greet','saludar','verb','greet the guests'),
('ground','suelo','noun','on the ground'),('grow','crecer','verb','plants grow'),('guess','adivinar','verb','guess the answer'),
('guitar','guitarra','noun','play the guitar'),('guy','chico','noun','a nice guy'),('habit','hábito','noun','a good habit'),
('hair','cabello','noun','long hair'),('haircut','corte de pelo','noun','get a haircut'),('half','mitad','noun','half an hour'),
('hall','pasillo','noun','in the hall'),('hamburger','hamburguesa','noun','a hamburger'),('hand','mano','noun','raise your hand'),
('handsome','guapo','adjective','a handsome man'),('hard','difícil','adjective','a hard question'),('ice','hielo','noun','ice and water'),
('island','isla','noun','a small island'),('juice','jugo','noun','orange juice'),('knee','rodilla','noun','my knee'),
('knife','cuchillo','noun','a kitchen knife'),('leg','pierna','noun','my leg'),('letter','letra','noun','the letter A'),
('magazine','revista','noun','read a magazine'),('mail','correo','noun','send the mail'),('moon','luna','noun','the full moon'),
('movie','película','noun','watch a movie'),('music','música','noun','listen to music'),('neck','cuello','noun','my neck'),
('newspaper','periódico','noun','read the newspaper'),('noise','ruido','noun','a loud noise'),('number','número','noun','a phone number'),
('orange','naranja','noun','an orange'),('pair','par','noun','a pair of shoes'),('pocket','bolsillo','noun','in my pocket'),
('potato','papa','noun','a potato'),('rabbit','conejo','noun','a white rabbit'),('rain','lluvia','noun','heavy rain'),
('radio','radio','noun','listen to the radio'),('red','rojo','adjective','a red car'),('restaurant','restaurante','noun','at a restaurant'),
('shirt','camisa','noun','a white shirt'),('shoe','zapato','noun','a black shoe'),('sister','hermana','noun','my sister'),
('snow','nieve','noun','white snow'),('song','canción','noun','a popular song'),('summer','verano','noun','in summer'),
('supermarket','supermercado','noun','at the supermarket'),('telephone','teléfono','noun','answer the telephone'),('television','televisión','noun','watch television'),
('towel','toalla','noun','a clean towel'),('uncle','tío','noun','my uncle'),('winter','invierno','noun','in winter'),
('yellow','amarillo','adjective','a yellow flower'),('zoo','zoológico','noun','at the zoo')
]

# load CEFR-J A1 headwords to ensure every addition has source-level support
profile={}
with REF.open(encoding='utf-8-sig',newline='') as f:
    for r in csv.DictReader(f):
        word=(r.get('headword') or '').strip().lower()
        lvl=(r.get('CEFR') or '').strip()
        if word and lvl: profile.setdefault(word,set()).add(lvl)

kcs=load('kcs.json'); edges=load('edges.json'); exercises=load('exercises.json'); lex=load('lexemes.json'); manifest=load('manifest.json')
lemmas={x['lemma'].casefold():x for x in lex}; kids={x['id'] for x in kcs}; eids={x['id'] for x in exercises}

def add_ex(x):
    if x['id'] not in eids: exercises.append(x); eids.add(x['id'])

def add_one(index, lemma, meaning, pos, chunk):
    key=lemma.casefold()
    if 'A1' not in profile.get(key,set()):
        return 'unsupported'
    if key in lemmas:
        # Enrich an existing entry with the newly authored chunk if absent.
        lx=lemmas[key]
        chunks=list(lx.get('chunks',[]))
        if chunk and chunk.casefold()!=lemma.casefold() and chunk not in chunks:
            chunks.append(chunk); lx['chunks']=chunks[:3]
        tags=set(lx.get('tags',[])); tags.update(['cefrj','siaa-authored-gloss','v13-a1-review']); lx['tags']=sorted(tags)
        return 'enriched'
    lxid='V_'+safe(lemma)
    distractor=A1[(index+17)%len(A1)][1]
    if distractor==meaning: distractor=A1[(index+31)%len(A1)][1]
    lx={'id':lxid,'lemma':lemma,'meaningEs':meaning,'cefr':'A1','frequencyRank':None,
        'spellingDifficulty':spell_diff(lemma),'distractorEs':distractor,'chunks':[chunk] if chunk and chunk.casefold()!=lemma.casefold() else [],
        'exampleFrames':[],'tags':['cefrj','siaa-authored-gloss','v13-a1','pos:'+pos]}
    lex.append(lx); lemmas[key]=lx
    if lxid not in kids:
        kcs.append({'id':lxid,'name':lemma,'cefr':'A1','domain':'VOCABULARY','form':lemma,'meaning':meaning,
                    'use':'reconocimiento auditivo, significado y uso léxico','importance':0.68,'priorMastery':0.05,
                    'tags':['lexeme','cefrj','siaa-authored-gloss','v13-a1','pos:'+pos]}); kids.add(lxid)
    # Static teach + transfer. ControlledVariantGenerator contributes meaning/spelling variants at seed time.
    add_ex({'id':'T13_'+safe(lxid),'type':'TEACH','kcIds':[lxid],'cefr':'A1','difficulty':0.22,
            'promptEs':f'Aprende una palabra nueva. {lemma} significa {meaning}.','stimulusEn':lemma,'optionA':'','optionB':'','correctOption':'',
            'explanationEs':f'{lemma} significa {meaning}.','spellTarget':lemma,'estimatedSeconds':18,
            'tags':['vocabulary','teach','v13-a1'],'misconceptionIds':[]})
    add_ex({'id':'X13_'+safe(lxid),'type':'SELF_ASSESS','kcIds':[lxid],'cefr':'A1','difficulty':0.36,
            'promptEs':f'Escucha {lemma}. Recupera mentalmente su significado en español y una situación donde podrías usarla.',
            'stimulusEn':lemma,'optionA':'','optionB':'','correctOption':'','explanationEs':f'Significado objetivo: {meaning}.',
            'spellTarget':lemma,'estimatedSeconds':20,'tags':['vocabulary','transfer','lexical-transfer-v12','v13-a1'],'misconceptionIds':[]})
    return 'added'

stats={'added':0,'enriched':0,'unsupported':0}
for i,row in enumerate(A1):
    status=add_one(i,*row); stats[status]+=1

order={'Pre-A1':0,'A1':1,'A2':2,'B1':3,'B2':4,'C1':5,'C2':6}
lex.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('frequencyRank') if isinstance(x.get('frequencyRank'),int) else 10**9,x['lemma'].casefold()))
kcs.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('domain',''),x['id']))
exercises.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('type',''),x['id']))
save('lexemes.json',lex); save('kcs.json',kcs); save('edges.json',edges); save('exercises.json',exercises)

manifest['version']='1.3.0'; manifest['contentVersion']='1.3.0'
manifest['description']='SIAA v1.3: expansión A1 revisada elemento por elemento, sobre la base v1.2 A1–C2 con audio offline y motor adaptativo.'
manifest['coverage']={
    'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exercises),'lexemes':len(lex),
    'generatedVariantsEstimate':sum(3+min(3,len(x.get('chunks',[]))) for x in lex),
    'levels':['Pre-A1','A1','A2','B1','B2','C1','C2']
}
sources=list(manifest.get('sources',[]))
for src in ['CEFR-J / Open Language Profiles vocabulary profile','SIAA original Spanish glosses and A1 chunks (v1.3 authoring pass)']:
    if src not in sources: sources.append(src)
manifest['sources']=sources
# write once, then hash content files and rewrite manifest
save('manifest.json',manifest)
manifest['checksums']={name:hashlib.sha256((CONTENT/name).read_bytes()).hexdigest() for name in ('kcs.json','edges.json','exercises.json','lexemes.json')}
save('manifest.json',manifest)
print(json.dumps({**stats,'lexemes':len(lex),'kcs':len(kcs),'exercises':len(exercises),'generatedEstimate':manifest['coverage']['generatedVariantsEstimate']},ensure_ascii=False,indent=2))
