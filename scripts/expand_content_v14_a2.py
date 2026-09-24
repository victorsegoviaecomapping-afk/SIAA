#!/usr/bin/env python3
from __future__ import annotations
import csv,json,hashlib,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'
REF=ROOT/'app/src/main/assets/reference/cefrj/cefrj-vocabulary-profile-1.5.csv'

def load(n): return json.loads((CONTENT/n).read_text(encoding='utf-8'))
def save(n,o): (CONTENT/n).write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def safe(s): return re.sub(r'[^A-Z0-9]+','_',s.upper()).strip('_') or 'X'
def spell_diff(w):
    x=w.lower(); d=.24+min(.18,max(0,len(x)-5)*.017)
    if any(p in x for p in ('ough','augh','eigh','igh','tion','sion','ph','wr','kn')): d+=.18
    if any(p in x for p in ('ee','ea','ie','ei','oo')): d+=.05
    return min(.84,d)

# A2: individual SIAA authoring. CEFR-J supplies the level; Spanish glosses/chunks are original.
A2=[
('ability','capacidad','noun','the ability to learn'),('abroad','en el extranjero','adverb','travel abroad'),('accept','aceptar','verb','accept an offer'),
('acceptable','aceptable','adjective','an acceptable answer'),('accident','accidente','noun','a road accident'),('across','al otro lado','preposition','across the street'),
('act','actuar','verb','act quickly'),('actually','en realidad','adverb','Actually, I agree.'),('addition','adición','noun','in addition'),
('additional','adicional','adjective','additional information'),('adjective','adjetivo','noun','an English adjective'),('adjust','ajustar','verb','adjust the volume'),
('admire','admirar','verb','admire her work'),('admit','admitir','verb','admit a mistake'),('advanced','avanzado','adjective','an advanced course'),
('advantage','ventaja','noun','a clear advantage'),('adventure','aventura','noun','an exciting adventure'),('advertisement','anuncio','noun','an online advertisement'),
('advertising','publicidad','noun','online advertising'),('advice','consejo','noun','give advice'),('advise','aconsejar','verb','advise a friend'),
('against','contra','preposition','against the wall'),('agent','agente','noun','a travel agent'),('ahead','adelante','adverb','go ahead'),
('aisle','pasillo','noun','an aisle seat'),('alarm','alarma','noun','set the alarm'),('alarm clock','despertador','noun','an alarm clock'),
('alive','vivo','adjective','still alive'),('ambition','ambición','noun','a personal ambition'),('among','entre','preposition','among friends'),
('amused','divertido','adjective','amused by the story'),('amusement','diversión','noun','for amusement'),('ancestor','antepasado','noun','a family ancestor'),
('ancient','antiguo','adjective','an ancient city'),('angel','ángel','noun','an angel'),('ankle','tobillo','noun','hurt my ankle'),
('anniversary','aniversario','noun','a wedding anniversary'),('annoy','molestar','verb','annoy the neighbours'),('annoying','molesto','adjective','an annoying noise'),
('anxious','ansioso','adjective','anxious about the exam'),('anymore','ya no','adverb','I do not live there anymore.'),('anyway','de todos modos','adverb','Anyway, let us continue.'),
('anywhere','en cualquier lugar','adverb','go anywhere'),('apart','separado','adverb','live far apart'),('apparently','aparentemente','adverb','Apparently, it is closed.'),
('appear','aparecer','verb','appear on screen'),('appearance','apariencia','noun','physical appearance'),('appreciate','apreciar','verb','appreciate your help'),
('architecture','arquitectura','noun','modern architecture'),('argue','discutir','verb','argue about money'),('armchair','sillón','noun','sit in an armchair'),
('artificial','artificial','adjective','artificial light'),('asleep','dormido','adjective','fall asleep'),('astronaut','astronauta','noun','an astronaut'),
('athlete','atleta','noun','a professional athlete'),('atom','átomo','noun','an atom'),('attack','ataque','noun','an attack'),
('attention','atención','noun','pay attention'),('attitude','actitud','noun','a positive attitude'),('attractive','atractivo','adjective','an attractive place'),
('audience','público','noun','a large audience'),('audio','audio','noun','an audio file'),('automatic','automático','adjective','an automatic door'),
('average','promedio','noun','the national average'),('award','premio','noun','win an award'),('awful','horrible','adjective','awful weather'),
('background','antecedentes','noun','educational background'),('badly','mal','adverb','work badly'),('badminton','bádminton','noun','play badminton'),
('bake','hornear','verb','bake a cake'),('balcony','balcón','noun','on the balcony'),('balloon','globo','noun','a red balloon'),
('banking','banca','noun','online banking'),('barbecue','barbacoa','noun','have a barbecue'),('barber','barbero','noun','go to the barber'),
('bargain','ganga','noun','a real bargain'),('basic','básico','adjective','basic information'),('basket','canasta','noun','a shopping basket'),
('beauty','belleza','noun','natural beauty'),('beer','cerveza','noun','a bottle of beer'),('beg','rogar','verb','beg for help'),
('beginner','principiante','noun','a beginner course'),('beginning','comienzo','noun','at the beginning'),('belong','pertenecer','verb','belong to a group'),
('belt','cinturón','noun','wear a belt'),('bend','doblar','verb','bend your knees'),('besides','además','adverb','Besides, it is cheaper.'),
('beyond','más allá','preposition','beyond the bridge'),('bit','poco','noun','a little bit'),('bite','morder','verb','a dog may bite'),
('blackboard','pizarra','noun','write on the blackboard'),('blame','culpar','verb','blame the weather'),('blanket','manta','noun','a warm blanket'),
('blonde','rubio','adjective','blonde hair'),('blood','sangre','noun','a blood test'),('bloom','florecer','verb','flowers bloom'),
('board game','juego de mesa','noun','play a board game'),('boil','hervir','verb','boil the water'),('boiled','hervido','adjective','a boiled egg'),
('bonus','bonificación','noun','an annual bonus'),('bookcase','librero','noun','a wooden bookcase'),('bookshelf','estante para libros','noun','on the bookshelf'),
('bookshop','librería','noun','visit a bookshop'),('bored','aburrido','adjective','feel bored'),('boss','jefe','noun','my boss'),
('bother','molestar','verb','Sorry to bother you.'),('brake','freno','noun','use the brake'),('brand','marca','noun','a popular brand'),
('brave','valiente','adjective','a brave person'),('breeze','brisa','noun','a light breeze'),('bride','novia','noun','the bride and groom'),
('brilliant','brillante','adjective','a brilliant idea'),('bug','insecto','noun','a small bug'),('bulb','bombilla','noun','a light bulb'),
('bury','enterrar','verb','bury the box'),('bus station','terminal de autobuses','noun','at the bus station'),('bus stop','parada de autobús','noun','wait at the bus stop'),
('bush','arbusto','noun','behind the bush'),('businessman','empresario','noun','a local businessman'),('businesswoman','empresaria','noun','a businesswoman'),
('cab','taxi','noun','take a cab'),('cafeteria','cafetería','noun','in the cafeteria'),('camping','campamento','noun','go camping'),
('capital letter','letra mayúscula','noun','use a capital letter'),('captain','capitán','noun','the team captain'),('car park','estacionamiento','noun','in the car park'),
('carrot','zanahoria','noun','a carrot'),('castle','castillo','noun','an old castle'),('cause','causa','noun','the main cause'),
('cent','centavo','noun','fifty cents'),('century','siglo','noun','in the twentieth century'),('cereal','cereal','noun','breakfast cereal'),
('certain','seguro','adjective','I am certain.'),('certainly','ciertamente','adverb','Certainly, I can help.'),('chain','cadena','noun','a chain of events'),
('chairman','presidente','noun','the committee chairman'),('champagne','champán','noun','a glass of champagne'),('championship','campeonato','noun','win the championship'),
('chance','oportunidad','noun','a good chance'),('chat','charlar','verb','chat with friends'),('cheek','mejilla','noun','on the cheek'),
('cheer','animar','verb','cheer the team'),('chef','chef','noun','a restaurant chef'),('chemical','químico','adjective','a chemical reaction'),
('cheque','cheque','noun','write a cheque'),('chess','ajedrez','noun','play chess'),('chest','pecho','noun','chest pain'),
('childhood','infancia','noun','during childhood'),('chimpanzee','chimpancé','noun','a chimpanzee'),('chin','barbilla','noun','touch your chin'),
('cigarette','cigarrillo','noun','smoke a cigarette'),('citizen','ciudadano','noun','a local citizen'),('classic','clásico','adjective','a classic film'),
('classical music','música clásica','noun','listen to classical music'),('cleaner','limpiador','noun','a household cleaner'),('clear','claro','adjective','a clear explanation'),
('click','hacer clic','verb','click the button'),('climbing','escalada','noun','go climbing'),('clone','clon','noun','a genetic clone'),
('clown','payaso','noun','a circus clown'),('clue','pista','noun','a useful clue'),('coast','costa','noun','along the coast'),
('coin','moneda','noun','a coin'),('cola','cola','noun','a bottle of cola'),('comb','peine','noun','use a comb'),
('comic','cómic','noun','read a comic'),('commitment','compromiso','noun','a long-term commitment'),('communicate','comunicarse','verb','communicate clearly'),
('communication','comunicación','noun','effective communication'),('comparative','comparativo','adjective','a comparative form'),('competition','competencia','noun','a sports competition'),
('complain','quejarse','verb','complain about the service'),('complete','completo','adjective','a complete answer'),('composer','compositor','noun','a famous composer'),
('concentrate','concentrarse','verb','concentrate on the task'),('concern','preocupación','noun','a serious concern'),('confident','seguro de sí mismo','adjective','feel confident'),
('confuse','confundir','verb','confuse two words'),('confused','confundido','adjective','feel confused'),('consequence','consecuencia','noun','a possible consequence'),
('consist','consistir','verb','consist of three parts'),('contact','contactar','verb','contact the office'),('container','recipiente','noun','a plastic container'),
('context','contexto','noun','understand the context'),('continent','continente','noun','a large continent'),('contrast','contraste','noun','a clear contrast'),
('convenience','comodidad','noun','for convenience'),('convenient','conveniente','adjective','a convenient time'),('cooker','cocina','noun','an electric cooker'),
('cooking','cocina','noun','enjoy cooking'),('correctly','correctamente','adverb','answer correctly'),('countryside','campo','noun','live in the countryside'),
('couple','pareja','noun','a young couple'),('crazy','loco','adjective','a crazy idea'),('creative','creativo','adjective','a creative solution'),
('creativity','creatividad','noun','show creativity'),('creature','criatura','noun','a strange creature'),('cricket','críquet','noun','play cricket'),
('cross','cruzar','verb','cross the road'),('crowd','multitud','noun','a large crowd'),('crowded','lleno de gente','adjective','a crowded bus'),
('crown','corona','noun','wear a crown'),('cruise','crucero','noun','go on a cruise'),('cupboard','armario','noun','in the cupboard'),
('curry','curry','noun','chicken curry'),('custom','costumbre','noun','a local custom'),('cycling','ciclismo','noun','go cycling'),
('dancer','bailarín','noun','a professional dancer'),('danger','peligro','noun','in danger'),('dangerous','peligroso','adjective','a dangerous road'),
('daylight','luz del día','noun','in daylight'),('dead','muerto','adjective','a dead battery'),('deal','acuerdo','noun','make a deal'),
('death','muerte','noun','cause of death'),('debate','debate','noun','a public debate'),('deep','profundo','adjective','deep water'),
('delay','retraso','noun','a flight delay'),('department store','tienda por departamentos','noun','at a department store'),('desert','desierto','noun','a dry desert'),
('dessert','postre','noun','have dessert'),('destroy','destruir','verb','destroy the evidence'),('diamond','diamante','noun','a diamond ring'),
('diary','diario','noun','write in a diary'),('diet','dieta','noun','a healthy diet'),('difficulty','dificultad','noun','have difficulty'),
('digital camera','cámara digital','noun','a digital camera'),('dinosaur','dinosaurio','noun','a dinosaur'),('direct','directo','adjective','a direct flight'),
('direction','dirección','noun','ask for directions'),('director','director','noun','the film director'),('disadvantage','desventaja','noun','a major disadvantage'),
('disappear','desaparecer','verb','disappear suddenly'),('disappointed','decepcionado','adjective','feel disappointed'),('disappointing','decepcionante','adjective','a disappointing result'),
('disco','discoteca','noun','go to a disco'),('discover','descubrir','verb','discover a new place'),('dishonest','deshonesto','adjective','a dishonest person'),
('dislike','no gustar','verb','dislike loud music'),('display','pantalla','noun','a digital display'),('disturb','molestar','verb','do not disturb'),
('divide','dividir','verb','divide into groups'),('dizzy','mareado','adjective','feel dizzy'),('double','doble','adjective','a double room'),
('doubt','duda','noun','have doubts'),('download','descargar','verb','download a file'),('downstairs','abajo','adverb','go downstairs'),
('downtown','centro de la ciudad','noun','go downtown'),('drawer','cajón','noun','in the drawer'),('drawing','dibujo','noun','a pencil drawing'),
('duck','pato','noun','a duck'),('dust','polvo','noun','clean the dust'),('earn','ganar','verb','earn money'),
('earring','arete','noun','wear an earring'),('earth','Tierra','noun','planet Earth'),('earthquake','terremoto','noun','a strong earthquake'),
('east','este','noun','in the east'),('education','educación','noun','higher education'),('educational','educativo','adjective','an educational programme'),
('effect','efecto','noun','a strong effect'),('effort','esfuerzo','noun','make an effort'),('elderly','anciano','adjective','elderly people'),
('electric','eléctrico','adjective','an electric car'),('elephant','elefante','noun','an elephant'),('elevator','ascensor','noun','take the elevator'),
('elsewhere','en otro lugar','adverb','look elsewhere'),('email','correo electrónico','noun','send an email'),('embarrassing','vergonzoso','adjective','an embarrassing mistake'),
('emergency','emergencia','noun','an emergency'),('empty','vacío','adjective','an empty room'),('encourage','animar','verb','encourage a student'),
('endangered','en peligro','adjective','an endangered species'),('ending','final','noun','a happy ending'),('energetic','enérgico','adjective','an energetic person'),
('enormous','enorme','adjective','an enormous building'),('enter','entrar','verb','enter the room'),('entertainment','entretenimiento','noun','evening entertainment'),
('envelope','sobre','noun','an envelope'),('envy','envidia','noun','feel envy'),('episode','episodio','noun','a television episode'),
('escalator','escalera mecánica','noun','take the escalator'),('escape','escapar','verb','escape from danger'),('especially','especialmente','adverb','especially useful'),
('essay','ensayo','noun','write an essay'),('euro','euro','noun','ten euros'),('exchange','intercambio','noun','a student exchange'),
('exhibition','exposición','noun','an art exhibition'),('exist','existir','verb','does it exist?'),('expert','experto','noun','an expert'),
('explanation','explicación','noun','a clear explanation'),('explore','explorar','verb','explore the city'),('express','expresar','verb','express an opinion'),
('expression','expresión','noun','a common expression'),('extra','extra','adjective','extra time'),('extremely','extremadamente','adverb','extremely important')
]

profile={}
with REF.open(encoding='utf-8-sig',newline='') as f:
    for r in csv.DictReader(f):
        w=(r.get('headword') or '').strip().casefold(); l=(r.get('CEFR') or '').strip()
        if w and l: profile.setdefault(w,set()).add(l)
kcs=load('kcs.json'); edges=load('edges.json'); exs=load('exercises.json'); lex=load('lexemes.json'); man=load('manifest.json')
lem={x['lemma'].casefold():x for x in lex}; kids={x['id'] for x in kcs}; eids={x['id'] for x in exs}

def add_ex(e):
    if e['id'] not in eids: exs.append(e);eids.add(e['id'])
def add_one(i,lemma,meaning,pos,chunk):
    if 'A2' not in profile.get(lemma.casefold(),set()): return 'unsupported'
    if lemma.casefold() in lem:
        lx=lem[lemma.casefold()]; ch=list(lx.get('chunks',[]));
        if chunk and chunk not in ch: ch.append(chunk); lx['chunks']=ch[:3]
        tg=set(lx.get('tags',[]));tg.update(['cefrj','siaa-authored-gloss','v14-a2-review']);lx['tags']=sorted(tg)
        return 'enriched'
    lid='V_'+safe(lemma); distract=A2[(i+23)%len(A2)][1]
    if distract==meaning:distract=A2[(i+47)%len(A2)][1]
    lx={'id':lid,'lemma':lemma,'meaningEs':meaning,'cefr':'A2','frequencyRank':None,'spellingDifficulty':spell_diff(lemma),
        'distractorEs':distract,'chunks':[chunk] if chunk and chunk.casefold()!=lemma.casefold() else [],'exampleFrames':[],
        'tags':['cefrj','siaa-authored-gloss','v14-a2','pos:'+pos]}
    lex.append(lx);lem[lemma.casefold()]=lx
    if lid not in kids:
        kcs.append({'id':lid,'name':lemma,'cefr':'A2','domain':'VOCABULARY','form':lemma,'meaning':meaning,
                    'use':'reconocimiento auditivo, significado y uso léxico','importance':.66,'priorMastery':.045,
                    'tags':['lexeme','cefrj','siaa-authored-gloss','v14-a2','pos:'+pos]});kids.add(lid)
    add_ex({'id':'T14_'+safe(lid),'type':'TEACH','kcIds':[lid],'cefr':'A2','difficulty':.27,'promptEs':f'Aprende una palabra: {lemma} significa {meaning}.',
            'stimulusEn':lemma,'optionA':'','optionB':'','correctOption':'','explanationEs':f'{lemma} significa {meaning}.','spellTarget':lemma,
            'estimatedSeconds':18,'tags':['vocabulary','teach','v14-a2'],'misconceptionIds':[]})
    add_ex({'id':'X14_'+safe(lid),'type':'SELF_ASSESS','kcIds':[lid],'cefr':'A2','difficulty':.43,
            'promptEs':f'Escucha {lemma}. Recupera su significado y crea mentalmente un ejemplo distinto al que aprendiste.',
            'stimulusEn':lemma,'optionA':'','optionB':'','correctOption':'','explanationEs':f'Significado objetivo: {meaning}.',
            'spellTarget':lemma,'estimatedSeconds':22,'tags':['vocabulary','transfer','lexical-transfer-v12','v14-a2'],'misconceptionIds':[]})
    return 'added'

st={'added':0,'enriched':0,'unsupported':0}
for i,r in enumerate(A2): st[add_one(i,*r)]+=1
order={'Pre-A1':0,'A1':1,'A2':2,'B1':3,'B2':4,'C1':5,'C2':6}
lex.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('frequencyRank') if isinstance(x.get('frequencyRank'),int) else 10**9,x['lemma'].casefold()))
kcs.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('domain',''),x['id']))
exs.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('type',''),x['id']))
save('lexemes.json',lex);save('kcs.json',kcs);save('edges.json',edges);save('exercises.json',exs)
man['version']='1.4.0';man['contentVersion']='1.4.0';man['description']='SIAA v1.4: expansión A1 y A2 revisada entrada por entrada sobre la base auditiva adaptativa A1–C2.'
man['coverage']={'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exs),'lexemes':len(lex),'generatedVariantsEstimate':sum(3+min(3,len(x.get('chunks',[]))) for x in lex),'levels':['Pre-A1','A1','A2','B1','B2','C1','C2']}
src=list(man.get('sources',[]))
for q in ['CEFR-J / Open Language Profiles vocabulary profile','SIAA original Spanish glosses and A2 chunks (v1.4 authoring pass)']:
    if q not in src:src.append(q)
man['sources']=src
save('manifest.json',man);man['checksums']={n:hashlib.sha256((CONTENT/n).read_bytes()).hexdigest() for n in ('kcs.json','edges.json','exercises.json','lexemes.json')};save('manifest.json',man)
print(json.dumps({**st,'lexemes':len(lex),'kcs':len(kcs),'exercises':len(exs),'generatedEstimate':man['coverage']['generatedVariantsEstimate']},ensure_ascii=False,indent=2))
