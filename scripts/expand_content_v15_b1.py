#!/usr/bin/env python3
from __future__ import annotations
import csv,json,hashlib,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'; REF=ROOT/'app/src/main/assets/reference/cefrj/cefrj-vocabulary-profile-1.5.csv'
def load(n): return json.loads((CONTENT/n).read_text(encoding='utf-8'))
def save(n,o): (CONTENT/n).write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def safe(s): return re.sub(r'[^A-Z0-9]+','_',s.upper()).strip('_') or 'X'
def spell_diff(w):
    x=w.lower();d=.26+min(.20,max(0,len(x)-6)*.016)
    if any(p in x for p in ('ough','augh','eigh','igh','tion','sion','ph','wr','kn','gue')):d+=.18
    return min(.86,d)

# B1 items, reviewed individually. Glosses are original SIAA authoring.
B1=[
('abandon','abandonar','verb'),('able','capaz','adjective'),('abnormal','anormal','adjective'),('aboard','a bordo','adverb'),
('absent','ausente','adjective'),('absolute','absoluto','adjective'),('absolutely','absolutamente','adverb'),('absorb','absorber','verb'),
('abstract','abstracto','adjective'),('abundance','abundancia','noun'),('abundant','abundante','adjective'),('academic','académico','adjective'),
('academy','academia','noun'),('accent','acento','noun'),('access','acceso','noun'),('accessible','accesible','adjective'),
('accidental','accidental','adjective'),('accidentally','accidentalmente','adverb'),('accompany','acompañar','verb'),('accomplish','lograr','verb'),
('according to','según','preposition'),('accountant','contador','noun'),('accuracy','precisión','noun'),('accuse','acusar','verb'),
('ache','dolor','noun'),('achievement','logro','noun'),('acquaintance','conocido','noun'),('acquire','adquirir','verb'),
('active','activo','adjective'),('actress','actriz','noun'),('ad','anuncio','noun'),('adapt','adaptar','verb'),
('administration','administración','noun'),('admiration','admiración','noun'),('adopt','adoptar','verb'),('adorable','adorable','adjective'),
('advance','avance','noun'),('adverb','adverbio','noun'),('advert','anuncio','noun'),('advertise','anunciar','verb'),
('aerobics','aeróbicos','noun'),('affection','afecto','noun'),('afford','permitirse','verb'),('afterward','después','adverb'),
('afterwards','después','adverb'),('aggressive','agresivo','adjective'),('agricultural','agrícola','adjective'),('agriculture','agricultura','noun'),
('aid','ayuda','noun'),('air conditioning','aire acondicionado','noun'),('airline','aerolínea','noun'),('alcohol','alcohol','noun'),
('alcoholic','alcohólico','adjective'),('alike','parecido','adjective'),('allergic','alérgico','adjective'),('allowance','asignación','noun'),
('aloud','en voz alta','adverb'),('alphabet','alfabeto','noun'),('alternative','alternativa','noun'),('altogether','totalmente','adverb'),
('amazed','sorprendido','adjective'),('amazing','increíble','adjective'),('ambitious','ambicioso','adjective'),('ambulance','ambulancia','noun'),
('amount','cantidad','noun'),('amusing','divertido','adjective'),('analysis','análisis','noun'),('anger','ira','noun'),
('angle','ángulo','noun'),('animated','animado','adjective'),('animation','animación','noun'),('announce','anunciar','verb'),
('annoyance','molestia','noun'),('annoyed','molesto','adjective'),('annual','anual','adjective'),('ant','hormiga','noun'),
('antique','antigüedad','noun'),('antonym','antónimo','noun'),('anxiety','ansiedad','noun'),('anxiously','ansiosamente','adverb'),
('ape','simio','noun'),('apology','disculpa','noun'),('apparent','aparente','adjective'),('appetite','apetito','noun'),
('applaud','aplaudir','verb'),('applause','aplauso','noun'),('application','solicitud','noun'),('appoint','nombrar','verb'),
('appreciation','aprecio','noun'),('approach','enfoque','noun'),('approve','aprobar','verb'),('approximately','aproximadamente','adverb'),
('architect','arquitecto','noun'),('arise','surgir','verb'),('arithmetic','aritmética','noun'),('army','ejército','noun'),
('arrange','organizar','verb'),('arrangement','arreglo','noun'),('arrival','llegada','noun'),('arrow','flecha','noun'),
('artistic','artístico','adjective'),('ashamed','avergonzado','adjective'),('aside','aparte','adverb'),('aspect','aspecto','noun'),
('aspirin','aspirina','noun'),('assign','asignar','verb'),('assist','ayudar','verb'),('assistance','asistencia','noun'),
('associate','asociar','verb'),('astronomer','astrónomo','noun'),('athletic','atlético','adjective'),('atmosphere','atmósfera','noun'),
('atomic','atómico','adjective'),('attach','adjuntar','verb'),('attachment','adjunto','noun'),('attain','alcanzar','verb'),
('attend','asistir','verb'),('attract','atraer','verb'),('attraction','atracción','noun'),('aware','consciente','adjective'),
('awareness','conciencia','noun'),('awesome','increíble','adjective'),('awkward','incómodo','adjective'),('babysit','cuidar niños','verb'),
('babysitter','niñera','noun'),('backache','dolor de espalda','noun'),('backpack','mochila','noun'),('backpacker','mochilero','noun'),
('backpacking','viaje con mochila','noun'),('bacon','tocino','noun'),('baggage','equipaje','noun'),('baker','panadero','noun'),
('bakery','panadería','noun'),('balance','equilibrio','noun'),('bald','calvo','adjective'),('bang','golpe','noun'),
('bank account','cuenta bancaria','noun'),('bare','sin cubrir','adjective'),('barely','apenas','adverb'),('barman','camarero de bar','noun'),
('barrel','barril','noun'),('basement','sótano','noun'),('basin','lavabo','noun'),('basis','fundamento','noun'),
('bathe','bañarse','verb'),('battle','batalla','noun'),('beard','barba','noun'),('beast','bestia','noun'),
('beautifully','bellamente','adverb'),('beaver','castor','noun'),('because of','debido a','preposition'),('bedside','junto a la cama','noun'),
('behalf','nombre','noun'),('behave','comportarse','verb'),('belief','creencia','noun'),('beloved','amado','adjective'),
('beneath','debajo de','preposition'),('bent','doblado','adjective'),('bet','apostar','verb'),('bilingual','bilingüe','adjective'),
('bin','contenedor','noun'),('biology','biología','noun'),('bishop','obispo','noun'),('bitter','amargo','adjective'),
('bleed','sangrar','verb'),('blend','mezclar','verb'),('bless','bendecir','verb'),('blind','ciego','adjective'),
('blog','blog','noun'),('blogger','bloguero','noun'),('blush','sonrojarse','verb'),('boast','presumir','verb'),
('bold','audaz','adjective'),('bomb','bomba','noun'),('boom','auge','noun'),('boot','bota','noun'),
('border','frontera','noun'),('bore','aburrir','verb'),('boredom','aburrimiento','noun'),('bounce','rebotar','verb'),
('boxing','boxeo','noun'),('bracelet','pulsera','noun'),('brainstorming','lluvia de ideas','noun'),('brand-new','completamente nuevo','adjective'),
('brass','latón','noun'),('breakthrough','avance decisivo','noun'),('breast','pecho','noun'),('breath','aliento','noun'),
('breathless','sin aliento','adjective'),('brick','ladrillo','noun'),('brightly','brillantemente','adverb'),('broad','amplio','adjective'),
('broadcast','transmitir','verb'),('broccoli','brócoli','noun'),('bronze','bronce','noun'),('bubble','burbuja','noun'),
('buddy','amigo','noun'),('builder','constructor','noun'),('bull','toro','noun'),('bullet','bala','noun'),
('bulletin','boletín','noun'),('bump','golpe','noun'),('bunch','grupo','noun'),('burden','carga','noun'),
('burglar','ladrón','noun'),('burning','ardiente','adjective'),('burst','estallar','verb'),('butcher','carnicero','noun'),
('buyer','comprador','noun'),('cabbage','repollo','noun'),('cabin','cabaña','noun'),('cage','jaula','noun'),
('calculate','calcular','verb'),('calculation','cálculo','noun'),('calculator','calculadora','noun'),('caller','persona que llama','noun'),
('calm','tranquilo','adjective'),('camel','camello','noun'),('campsite','campamento','noun'),('canal','canal','noun'),
('cancel','cancelar','verb'),('candle','vela','noun'),('canned','enlatado','adjective'),('canteen','cantina','noun'),
('capable','capaz','adjective'),('capsule','cápsula','noun'),('capture','capturar','verb'),('career','carrera profesional','noun'),
('careless','descuidado','adjective'),('carelessly','descuidadamente','adverb'),('carpet','alfombra','noun'),('carriage','vagón','noun'),
('carton','cartón','noun'),('cashpoint','cajero automático','noun'),('casual','informal','adjective'),('cattle','ganado','noun'),
('caution','precaución','noun'),('cautious','cauteloso','adjective'),('cave','cueva','noun'),('celebrity','celebridad','noun'),
('cell','célula','noun'),('central','central','adjective'),('central heating','calefacción central','noun'),('ceremony','ceremonia','noun'),
('certainty','certeza','noun'),('challenging','desafiante','adjective'),('champion','campeón','noun'),('chaos','caos','noun'),
('characteristic','característica','noun'),('charity','caridad','noun'),('charm','encanto','noun'),('chase','perseguir','verb'),
('checkout','caja','noun'),('cheerful','alegre','adjective'),('chemistry','química','noun'),('cherish','apreciar','verb'),
('chief','jefe','noun'),('choir','coro','noun'),('choke','ahogarse','verb'),('chronological','cronológico','adjective'),
('circus','circo','noun'),('civil','civil','adjective'),('classical','clásico','adjective'),('classify','clasificar','verb'),
('cliff','acantilado','noun'),('climate','clima','noun'),('climber','escalador','noun'),('coal','carbón','noun'),
('coastal','costero','adjective'),('coffin','ataúd','noun'),('collapse','colapsar','verb'),('collar','cuello','noun'),
('combination','combinación','noun'),('combine','combinar','verb'),('comedian','comediante','noun'),('comedy','comedia','noun'),
('comet','cometa','noun'),('comfort','comodidad','noun'),('comma','coma','noun'),('command','orden','noun'),
('comment','comentario','noun'),('commercial','comercial','adjective'),('commit','comprometerse','verb'),('common sense','sentido común','noun'),
('companion','compañero','noun'),('comparison','comparación','noun'),('compete','competir','verb'),('competitive','competitivo','adjective'),
('competitor','competidor','noun'),('complement','complemento','noun'),('complicate','complicar','verb'),('complicated','complicado','adjective'),
('compliment','cumplido','noun'),('compose','componer','verb'),('composition','composición','noun'),('compound','compuesto','noun'),
('compromise','compromiso','noun'),('concept','concepto','noun'),('concerned','preocupado','adjective'),('conclude','concluir','verb'),
('confidence','confianza','noun'),('confirm','confirmar','verb'),('confirmation','confirmación','noun'),('conflict','conflicto','noun'),
('confusing','confuso','adjective'),('congratulate','felicitar','verb'),('connect','conectar','verb'),('conquer','conquistar','verb'),
('conscious','consciente','adjective'),('conservation','conservación','noun'),('conservative','conservador','adjective'),('consonant','consonante','noun'),
('constantly','constantemente','adverb'),('construct','construir','verb'),('construction','construcción','noun'),('consume','consumir','verb'),
('consumer','consumidor','noun'),('consumption','consumo','noun'),('contain','contener','verb'),('content','contenido','noun'),
('continual','continuo','adjective'),('continuously','continuamente','adverb'),('contrary','contrario','adjective'),('contribution','contribución','noun'),
('controversial','controvertido','adjective'),('convince','convencer','verb'),('correction','corrección','noun'),('cotton','algodón','noun'),
('counter','mostrador','noun'),('courage','valor','noun'),('courageous','valiente','adjective'),('craft','artesanía','noun'),
('crash','choque','noun'),('creator','creador','noun'),('criminal','criminal','noun'),('crisis','crisis','noun'),
('critic','crítico','noun'),('critical','crítico','adjective'),('crop','cultivo','noun'),('crossing','cruce','noun'),
('crossroads','cruce de caminos','noun'),('cruel','cruel','adjective'),('cucumber','pepino','noun'),('cultivate','cultivar','verb'),
('cultural','cultural','adjective'),('cure','cura','noun'),('curiosity','curiosidad','noun'),('curious','curioso','adjective'),
('curly','rizado','adjective'),('currency','moneda','noun'),('current','actual','adjective'),('curtain','cortina','noun'),
('curve','curva','noun'),('cushion','cojín','noun'),('customs','aduanas','noun'),('cycle','ciclo','noun'),
('damage','daño','noun'),('damaged','dañado','adjective'),('damp','húmedo','adjective'),('dangerously','peligrosamente','adverb'),
('dare','atreverse','verb'),('darkness','oscuridad','noun'),('dating','citas','noun'),('deadly','mortal','adjective'),
('deaf','sordo','adjective'),('dealer','vendedor','noun'),('debris','escombros','noun'),('debt','deuda','noun'),
('declaration','declaración','noun'),('declare','declarar','verb'),('decrease','disminución','noun'),('dedicate','dedicar','verb'),
('defeat','derrota','noun'),('defend','defender','verb'),('defender','defensor','noun'),('deficiency','deficiencia','noun'),
('define','definir','verb'),('definite','definitivo','adjective'),('definition','definición','noun'),('delete','eliminar','verb'),
('delicate','delicado','adjective'),('delight','deleite','noun'),('delighted','encantado','adjective'),('delightful','encantador','adjective'),
('deliver','entregar','verb'),('demand','demanda','noun'),('democratic','democrático','adjective'),('demonstration','demostración','noun'),
('deny','negar','verb'),('depart','partir','verb'),('departure','salida','noun'),('dependent','dependiente','adjective'),
('deposit','depósito','noun'),('depressed','deprimido','adjective'),('depressing','deprimente','adjective'),('deprive','privar','verb'),
('depth','profundidad','noun'),('deserve','merecer','verb'),('designer','diseñador','noun'),('desire','deseo','noun'),
('despair','desesperación','noun'),('desperate','desesperado','adjective'),('despite','a pesar de','preposition'),('destruction','destrucción','noun'),
('destructive','destructivo','adjective'),('detective','detective','noun'),('determination','determinación','noun'),('development','desarrollo','noun'),
('diagram','diagrama','noun'),('dial','marcar','verb'),('diameter','diámetro','noun'),('digital','digital','adjective'),
('dioxide','dióxido','noun'),('dirt','suciedad','noun'),('disability','discapacidad','noun'),('disabled','discapacitado','adjective'),
('disagreement','desacuerdo','noun'),('disappoint','decepcionar','verb'),('disappointment','decepción','noun'),('disaster','desastre','noun'),
('disastrous','desastroso','adjective'),('discomfort','incomodidad','noun'),('discourage','desanimar','verb'),('discrimination','discriminación','noun'),
('disease','enfermedad','noun'),('disgusting','asqueroso','adjective'),('dishwasher','lavavajillas','noun'),('dissolve','disolver','verb'),
('distance','distancia','noun'),('distant','distante','adjective')
]

profile={}
with REF.open(encoding='utf-8-sig',newline='') as f:
    for r in csv.DictReader(f):
        w=(r.get('headword') or '').strip().casefold(); l=(r.get('CEFR') or '').strip()
        if w and l: profile.setdefault(w,set()).add(l)
kcs=load('kcs.json');edges=load('edges.json');exs=load('exercises.json');lex=load('lexemes.json');man=load('manifest.json')
lem={x['lemma'].casefold():x for x in lex};kids={x['id'] for x in kcs};eids={x['id'] for x in exs}

def add_ex(e):
    if e['id'] not in eids: exs.append(e);eids.add(e['id'])
def chunk_for(lemma,pos):
    if pos=='verb': return 'to '+lemma
    if pos in ('preposition','adverb'): return lemma
    return ''
def add_one(i,lemma,meaning,pos):
    if 'B1' not in profile.get(lemma.casefold(),set()):return 'unsupported'
    if lemma.casefold() in lem:
        lx=lem[lemma.casefold()];tg=set(lx.get('tags',[]));tg.update(['cefrj','siaa-authored-gloss','v15-b1-review']);lx['tags']=sorted(tg);return 'enriched'
    lid='V_'+safe(lemma);distr=B1[(i+29)%len(B1)][1]
    if distr==meaning:distr=B1[(i+61)%len(B1)][1]
    ch=chunk_for(lemma,pos)
    lx={'id':lid,'lemma':lemma,'meaningEs':meaning,'cefr':'B1','frequencyRank':None,'spellingDifficulty':spell_diff(lemma),
        'distractorEs':distr,'chunks':[ch] if ch and ch.casefold()!=lemma.casefold() else [],'exampleFrames':[],
        'tags':['cefrj','siaa-authored-gloss','v15-b1','pos:'+pos]}
    lex.append(lx);lem[lemma.casefold()]=lx
    if lid not in kids:
        kcs.append({'id':lid,'name':lemma,'cefr':'B1','domain':'VOCABULARY','form':lemma,'meaning':meaning,'use':'reconocimiento auditivo, significado y uso léxico',
                    'importance':.64,'priorMastery':.04,'tags':['lexeme','cefrj','siaa-authored-gloss','v15-b1','pos:'+pos]});kids.add(lid)
    add_ex({'id':'T15_'+safe(lid),'type':'TEACH','kcIds':[lid],'cefr':'B1','difficulty':.34,'promptEs':f'Aprende una palabra B1: {lemma} significa {meaning}.',
            'stimulusEn':lemma,'optionA':'','optionB':'','correctOption':'','explanationEs':f'{lemma} significa {meaning}.','spellTarget':lemma,'estimatedSeconds':18,
            'tags':['vocabulary','teach','v15-b1'],'misconceptionIds':[]})
    add_ex({'id':'X15_'+safe(lid),'type':'SELF_ASSESS','kcIds':[lid],'cefr':'B1','difficulty':.52,
            'promptEs':f'Escucha {lemma}. Recupera su significado y crea mentalmente un uso nuevo, sin repetir el ejemplo aprendido.',
            'stimulusEn':lemma,'optionA':'','optionB':'','correctOption':'','explanationEs':f'Significado objetivo: {meaning}.','spellTarget':lemma,'estimatedSeconds':23,
            'tags':['vocabulary','transfer','lexical-transfer-v12','v15-b1'],'misconceptionIds':[]})
    return 'added'
st={'added':0,'enriched':0,'unsupported':0}
for i,r in enumerate(B1):st[add_one(i,*r)]+=1
order={'Pre-A1':0,'A1':1,'A2':2,'B1':3,'B2':4,'C1':5,'C2':6}
lex.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('frequencyRank') if isinstance(x.get('frequencyRank'),int) else 10**9,x['lemma'].casefold()))
kcs.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('domain',''),x['id']));exs.sort(key=lambda x:(order.get(x.get('cefr','A1'),99),x.get('type',''),x['id']))
save('lexemes.json',lex);save('kcs.json',kcs);save('edges.json',edges);save('exercises.json',exs)
man['version']='1.5.0';man['contentVersion']='1.5.0';man['description']='SIAA v1.5: A1, A2 y B1 ampliados entrada por entrada con glosas originales, audio offline y transferencia.'
man['coverage']={'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exs),'lexemes':len(lex),'generatedVariantsEstimate':sum(3+min(3,len(x.get('chunks',[]))) for x in lex),'levels':['Pre-A1','A1','A2','B1','B2','C1','C2']}
src=list(man.get('sources',[]));q='SIAA original Spanish glosses for B1 (v1.5 authoring pass)';
if q not in src:src.append(q)
man['sources']=src;save('manifest.json',man);man['checksums']={n:hashlib.sha256((CONTENT/n).read_bytes()).hexdigest() for n in ('kcs.json','edges.json','exercises.json','lexemes.json')};save('manifest.json',man)
print(json.dumps({**st,'lexemes':len(lex),'kcs':len(kcs),'exercises':len(exs),'generatedEstimate':man['coverage']['generatedVariantsEstimate']},ensure_ascii=False,indent=2))
