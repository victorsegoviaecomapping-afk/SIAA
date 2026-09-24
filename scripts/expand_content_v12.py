#!/usr/bin/env python3
from __future__ import annotations
import json, hashlib, re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CONTENT=ROOT/'app/src/main/assets/content'
LEVEL_ORDER={'Pre-A1':0,'A1':1,'A2':2,'B1':3,'B2':4,'C1':5,'C2':6}

def load(name): return json.loads((CONTENT/name).read_text(encoding='utf-8'))
def save(name,obj): (CONTENT/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def safe(s): return re.sub(r'[^A-Z0-9]+','_',s.upper()).strip('_') or 'X'
def ex(id,type,kcs,cefr,diff,prompt,stim='',a='',b='',correct='',exp='',spell='',seconds=20,tags=(),mis=()):
    return {'id':id,'type':type,'kcIds':kcs,'cefr':cefr,'difficulty':diff,'promptEs':prompt,'stimulusEn':stim,
            'optionA':a,'optionB':b,'correctOption':correct,'explanationEs':exp,'spellTarget':spell,
            'estimatedSeconds':seconds,'tags':list(tags),'misconceptionIds':list(mis)}

kcs=load('kcs.json'); edges=load('edges.json'); exercises=load('exercises.json'); lexemes=load('lexemes.json')
eid={x['id'] for x in exercises}; kid={x['id'] for x in kcs}

def add_ex(x):
    if x['id'] not in eid:
        exercises.append(x); eid.add(x['id']); return True
    return False

def balanced_options(key, correct, wrong):
    # Stable balancing prevents a hidden A-only strategy.
    if int(hashlib.sha1(key.encode()).hexdigest()[-1],16) % 2 == 0:
        return correct, wrong, 'A'
    return wrong, correct, 'B'

# ---------------------------------------------------------------------------
# 1. Listening bank: original SIAA scripts, source-informed but not copied.
#    Twelve new scripts per CEFR level. Together with v1.1 this gives a
#    substantially deeper listening progression and exact pre-rendered audio.
# ---------------------------------------------------------------------------
LISTENING = {
'A1': [
('train_platform','The train to Bristol leaves from platform four at six fifteen.','¿Desde qué andén sale el tren?','platform four','platform fourteen','L11_KEY_DETAILS'),
('cafe_order','Can I have a cheese sandwich and a glass of water, please?','¿Qué bebida pide?','a glass of water','a cup of coffee','L11_KEY_DETAILS'),
('office_instruction','Write your name at the top of the page, then put the paper on my desk.','¿Qué debe hacer después de escribir su nombre?','put the paper on the desk','leave the office','L11_INSTRUCTIONS'),
('weather_clothes','It is cold and windy today, so take your coat.','¿Qué recomienda llevar?','a coat','swimming clothes','L11_INSTRUCTIONS'),
('family_visit','My sister is coming on Saturday, but my brother will arrive on Sunday.','¿Quién llega primero?','the sister','the brother','L11_KEY_DETAILS'),
('shop_close','The shop closes at five, but the supermarket stays open until nine.','¿A qué hora cierra la tienda?','five','nine','L11_KEY_DETAILS'),
('phone_number','My new number is zero seven nine, four two, six one, eight three.','¿Qué tipo de información escucha?','a phone number','a street address','L11_WORD_BOUND'),
('room_location','The bathroom is next to the bedroom, across from the kitchen.','¿Dónde está el baño?','next to the bedroom','behind the garden','L11_KEY_DETAILS'),
('simple_plan','We are meeting at the cinema at seven. The film starts at seven thirty.','¿A qué hora se encuentran?','seven','seven thirty','L11_KEY_DETAILS'),
('doctor_step','Take one tablet after breakfast and drink plenty of water.','¿Cuándo debe tomar la tableta?','after breakfast','before sleeping','L11_INSTRUCTIONS'),
('lost_item','Your blue bag is under the chair near the door.','¿Dónde está la bolsa?','under the chair','on the table','L11_KEY_DETAILS'),
('class_task','Listen to the dialogue first. Then answer questions one to five.','¿Qué debe hacer primero?','listen to the dialogue','answer question five','L11_INSTRUCTIONS'),
],
'A2': [
('late_bus','The first bus was full, so I waited for the next one and arrived twenty minutes late.','¿Por qué llegó tarde?','the first bus was full','the station was closed','L11_GIST'),
('weekend_change','We planned to go hiking, but the forecast says heavy rain, so we will visit the science museum instead.','¿Qué harán finalmente?','visit the science museum','go hiking in heavy rain','L11_GIST'),
('recipe_order','First heat the oil, then add the onions. When they are soft, add the tomatoes.','¿Qué se añade después del aceite?','the onions','the tomatoes','L11_SEQUENCE'),
('lost_wallet','I thought I left my wallet at the cafe, but Sara found it in my jacket pocket.','¿Dónde estaba la billetera?','in the jacket pocket','at the cafe','L11_REFERENCE'),
('course_choice','The morning class is cheaper, but I work until noon, so I chose the evening class.','¿Por qué eligió la clase nocturna?','because of the work schedule','because it was cheaper','L11_GIST'),
('hotel_problem','Our room was clean, but the air conditioner did not work. The receptionist moved us to another room.','¿Cómo resolvieron el problema?','they changed rooms','they left the hotel','L11_SEQUENCE'),
('birthday_plan','Marta will bring the cake, and I will buy the drinks. Luis said he can decorate the room.','¿Quién comprará las bebidas?','the speaker','Luis','L11_REFERENCE'),
('appointment','The dentist moved my appointment from Tuesday morning to Wednesday afternoon.','¿Cuándo es ahora la cita?','Wednesday afternoon','Tuesday morning','L11_KEY_DETAILS'),
('online_order','I ordered the shoes online on Monday. They arrived on Thursday, one day earlier than expected.','¿Cuándo llegaron los zapatos?','Thursday','Friday','L11_SEQUENCE'),
('museum_rules','You can take photos in the main hall, but you must not use a flash.','¿Qué está prohibido?','using a flash','taking any photos','L11_GIST'),
('study_routine','I usually study vocabulary on the bus and grammar at home because I need a quiet place for longer exercises.','¿Dónde estudia gramática?','at home','on the bus','L11_GIST'),
('meeting_reference','Daniel sent the report to Elena after she asked for the updated figures. She reviewed it that evening.','¿Quién revisó el informe?','Elena','Daniel','L11_REFERENCE'),
],
'B1': [
('remote_balance','Working remotely saves me commuting time, but I miss informal conversations with colleagues. I now go to the office twice a week.','¿Qué solución encontró?','a mixed home-and-office routine','working only from home','L11_MAIN_SUPPORT'),
('course_feedback','The course was demanding at first, especially the weekly presentations. By the end, however, I felt much more confident speaking in public.','¿Cómo cambió su reacción?','from difficulty to greater confidence','from confidence to disappointment','L11_ATTITUDE'),
('repair_infer','The technician said the battery was fine but the charging port was damaged. After replacing that part, the phone worked normally.','¿Qué causaba el problema?','the charging port','the battery','L11_CONTEXT_INFER'),
('travel_delay','Our flight was delayed for three hours. Instead of waiting at the gate, we used the time to finish some work at a quiet cafe.','¿Cómo reaccionaron al retraso?','they used the waiting time productively','they cancelled the trip','L11_MAIN_SUPPORT'),
('exercise_habit','I used to drive to work every day. Since I started cycling, I have more energy in the morning and I spend less on fuel.','¿Qué beneficio menciona además de ahorrar dinero?','having more energy','arriving later','L11_MAIN_SUPPORT'),
('neighbour_noise','At first I thought the music came from the apartment upstairs, but it stopped as soon as the cafe across the street closed.','¿Qué se puede inferir?','the cafe was probably the source','the upstairs neighbour admitted it','L11_CONTEXT_INFER'),
('job_choice','The new job pays slightly less, yet it offers flexible hours and better training. For me, those advantages matter more right now.','¿Qué valora más el hablante?','flexibility and training','the highest possible salary','L11_ATTITUDE'),
('project_risk','We can finish by Friday if the supplier delivers the parts tomorrow. Otherwise, we will need to move the deadline.','¿De qué depende terminar el viernes?','the supplier delivering tomorrow','hiring a new manager','L11_MAIN_SUPPORT'),
('book_reaction','I expected the novel to be slow, but the second half was so engaging that I finished it in one evening.','¿Cómo cambió su opinión?','it became much more positive','it became more negative','L11_ATTITUDE'),
('health_context','The doctor did not prescribe antibiotics. She explained that the infection was viral and recommended rest and fluids.','¿Qué se puede inferir?','antibiotics were not considered useful','the patient needed surgery','L11_CONTEXT_INFER'),
('presentation_plan','I will explain the problem first, then show the data, and finally discuss two possible solutions.','¿Cómo está organizada la presentación?','problem, evidence, then solutions','solutions before the problem','L11_MAIN_SUPPORT'),
('housing_tradeoff','The flat is smaller than the other one, but it is close to work and the rent includes electricity.','¿Qué ventaja compensa el menor tamaño?','location and included electricity','a larger kitchen','L11_MAIN_SUPPORT'),
],
'B2': [
('policy_tradeoff','The proposal would reduce traffic in the city centre, but businesses worry that fewer drivers could mean fewer customers. The council therefore plans a six-month trial before making the change permanent.','¿Por qué proponen una prueba temporal?','to evaluate benefits and business concerns','to avoid collecting any evidence','L11_ARGUMENT'),
('research_caution','The two groups differed after the intervention, yet they were not randomly assigned. We should therefore be careful about claiming that the intervention caused the difference.','¿Por qué pide cautela?','the design does not establish causation securely','the groups had identical results','L11_ARGUMENT'),
('manager_hint','I am not saying the report is wrong, but the figures in section three deserve another look before we send it to the client.','¿Qué intenta lograr el gerente?','prompt a careful recheck','praise the report without reservation','L11_IMPLICIT_INTENT'),
('subscription_cost','The basic plan appears cheaper, whereas the professional plan includes support and storage that we would otherwise purchase separately.','¿Cuál es el punto central?','headline price may not equal total cost','support and storage have no value','L11_ARGUMENT'),
('meeting_softening','Perhaps we could revisit that assumption before we commit to the final model.','¿Qué función cumple perhaps?','soften a suggestion or challenge','express absolute certainty','L11_IMPLICIT_INTENT'),
('urban_tree','Planting trees can reduce summer heat and improve shade. However, species choice and long-term maintenance determine whether those benefits last.','¿Qué matiz añade el hablante?','implementation affects long-term benefits','any tree always gives the same result','L11_ARGUMENT'),
('data_privacy','The app collects location only while a trip is active. Even so, users should be told clearly how long those records are kept.','¿Qué preocupación mantiene el hablante?','data-retention transparency','whether phones have GPS','L11_ARGUMENT'),
('training_request','If you have a moment, it might be worth showing the new staff how the backup process works.','¿Qué intención expresa?','a polite recommendation','a refusal to help','L11_IMPLICIT_INTENT'),
('repair_decision','Replacing the motor would extend the machine’s life, but the repair costs almost as much as a newer, more efficient unit.','¿Qué decisión presenta como compleja?','repair versus replacement','whether to clean the machine','L11_ARGUMENT'),
('conference_comment','The talk covered a great deal of material, perhaps more than could be absorbed in forty minutes.','¿Qué crítica implícita hace?','the talk may have been too dense','the talk had too little content','L11_IMPLICIT_INTENT'),
('connected_request','Could you have sent it to me by the end of the day?','¿Qué debe identificar el oyente?','a request about sending something by a deadline','a question about yesterday’s weather','L11_CONNECTED_SPEECH'),
('housing_policy','Rent controls can protect existing tenants, while critics argue they may reduce incentives to build new housing.','¿Qué estructura argumentativa presenta?','a benefit contrasted with a possible cost','two identical claims','L11_ARGUMENT'),
],
'C1': [
('correlation_limit','The correlation is remarkably stable across samples. That strengthens the descriptive finding, but it still tells us little about the direction of causality.','¿Cuál es la postura?','the pattern is robust but causality remains unresolved','the correlation proves the causal direction','L11_STANCE'),
('budget_reframe','At first the savings look impressive. Once deferred maintenance is included, however, much of the apparent efficiency disappears.','¿Cómo cambia la interpretación?','some savings reflect postponed costs','the later information confirms all savings','L11_DISTRIBUTED'),
('institutional_focus','Technical capacity is not the binding constraint. Several agencies already have the necessary systems; the unresolved issue is whether they can agree on shared standards.','¿Dónde sitúa el cuello de botella?','institutional coordination','absence of technical systems','L11_DISCOURSE_ORG'),
('qualified_support','The evidence is broadly consistent with the proposed mechanism, though two alternative explanations remain difficult to exclude.','¿Qué postura adopta?','qualified support with residual uncertainty','complete rejection','L11_STANCE'),
('speaker_revision','Earlier I described the effect as small. That was too simple: it is small on average, but substantial for participants who began with the lowest scores.','¿Qué revisa el hablante?','an aggregate description using subgroup evidence','the existence of any effect','L11_DISTRIBUTED'),
('causal_language','If anything, the result should make us less confident in the causal story, not more.','¿Qué indica if anything aquí?','a correction that strengthens the opposite conclusion','enthusiastic agreement','L11_STANCE'),
('policy_sequence','The speaker first treats cost as the main objection, then concedes that funding is available, and finally shifts to concerns about implementation capacity.','¿Cómo evoluciona el argumento?','the central objection shifts from cost to implementation','the same objection is repeated unchanged','L11_DISCOURSE_ORG'),
('evaluation_distance','The intervention appears promising, but the follow-up period is too short to know whether the gains will persist.','¿Qué limita la conclusión?','insufficient long-term evidence','lack of any initial improvement','L11_STANCE'),
('distributed_exception','The opening claim suggests universal improvement. Near the end, though, the speaker notes that two regions actually declined.','¿Qué debe integrar el oyente?','a later exception qualifies the opening generalization','all regions improved equally','L11_DISTRIBUTED'),
('methodological_shift','What matters here is not whether the model predicts well in the original sample; it is whether that performance survives genuinely new data.','¿Qué criterio prioriza?','out-of-sample generalization','fit to the original sample only','L11_DISCOURSE_ORG'),
('epistemic_distance','It would be premature to rule out measurement error, especially given the unusually high variance in the final wave.','¿Qué comunica premature?','the conclusion should remain open','measurement error is impossible','L11_STANCE'),
('nested_argument','The author accepts that regulation imposes costs, disputes the estimate of their magnitude, and then argues that the comparison omits avoided damages.','¿Cuál es la estructura?','concession, challenge, then additional countervailing benefit','unqualified agreement with the cost estimate','L11_DISCOURSE_ORG'),
],
'C2': [
('strategic_evasion','Asked whether the target had been missed, the minister replied that the programme had “created an unprecedented platform for future progress.”','¿Qué hace discursivamente?','reframes the question to avoid a direct admission','directly confirms that the target was met','L11_SUBTEXT'),
('aggregate_paradox','The national average barely moved, yet that stability is deceptive: gains in urban districts almost exactly offset losses in rural ones.','¿Qué debe inferirse?','aggregate stability masks divergent subgroup changes','nothing changed in any subgroup','L11_FAST_DENSE'),
('qualified_mediation','The paper does not reject the theory outright. Rather, it narrows the circumstances under which the authors believe the theory can plausibly apply.','¿Cuál es la síntesis más precisa?','the theory is retained but its scope is restricted','the theory is accepted without qualification','L11_MEDIATION'),
('dry_irony','After the fourth revision request, he smiled and said, “At least the process is wonderfully efficient.”','¿Qué comunica efficient?','ironic criticism of inefficiency','literal praise of the process','L11_SUBTEXT'),
('dense_counterfactual','Had the baseline trend continued, the apparent improvement would have occurred even without the intervention; that possibility substantially weakens the headline claim.','¿Qué debilita la afirmación principal?','a counterfactual trend that could explain the improvement','proof that the intervention had no participants','L11_FAST_DENSE'),
('rhetorical_distance','Calling the arrangement “temporary” is doing considerable rhetorical work here, given that it has already been renewed five times.','¿Qué implica el comentario?','the label temporary is being questioned','the arrangement has clearly ended','L11_SUBTEXT'),
('multi_source','One source attributes the decline to demand, another to regulation, while the administrative data suggest both mechanisms may operate in different sectors.','¿Cuál es la mejor mediación?','the evidence supports a sector-dependent combination of explanations','all sources identify exactly the same cause','L11_MEDIATION'),
('scope_shift','The claim is defensible only if “success” means short-term uptake; it becomes much harder to sustain if success includes long-term retention.','¿Qué hace el hablante?','shows that the conclusion depends on how success is defined','argues that definitions never matter','L11_SUBTEXT'),
('compressed_reasoning','The absence of complaints is not evidence of satisfaction when the very groups most affected have no effective channel through which to complain.','¿Qué presuposición cuestiona?','that silence reliably indicates satisfaction','that complaints can be recorded','L11_FAST_DENSE'),
('diplomatic_rebuttal','There is something to that objection, but it assumes the baseline itself is neutral, which is precisely what the historical evidence calls into question.','¿Cómo responde?','concedes part of the objection then challenges its premise','dismisses the objection without engaging it','L11_MEDIATION'),
('implicature_meeting','When asked whether the deadline was realistic, the engineer said, “Well, the calendar certainly has a Friday on it.”','¿Qué implica?','skepticism about meeting the deadline','confidence that Friday is easy','L11_SUBTEXT'),
('layered_summary','The review praises the programme’s reach, questions the reliability of its outcome measures, and recommends expansion only if stronger evaluation is built into the next phase.','¿Cuál es la síntesis adecuada?','conditional support tied to better evaluation','unconditional recommendation for immediate expansion','L11_MEDIATION'),
]
}
DIFF={'A1':.30,'A2':.42,'B1':.56,'B2':.68,'C1':.80,'C2':.90}
added_listening=0
for level, rows in LISTENING.items():
    for slug,stim,prompt,correct,wrong,kcid in rows:
        a,b,ans=balanced_options('listen:'+slug,correct,wrong)
        added_listening += add_ex(ex('L12_'+safe(slug),'LISTENING_AB',[kcid],level,DIFF[level],prompt,stim,a,b,ans,
            f'La interpretación correcta es: {correct}.',seconds=max(24, min(55, 14+len(stim)//8)),tags=('listening','original-script','audio-asset','v12','transfer')))

# ---------------------------------------------------------------------------
# 2. Pragmatic recognition: every pragmatic KC gets an objective recognition
#    item in addition to teaching/mental production. This makes pragmatics
#    measurable without requiring speech on the bus.
# ---------------------------------------------------------------------------
wrong_by_level={
'A1':'I do not understand this conversation at all.',
'A2':'That is unrelated, so I will say something else.',
'B1':'I refuse to respond to that point.',
'B2':'Whatever. That issue is not worth discussing.',
'C1':'There is no need to engage with the other person’s position.',
'C2':'The best strategy is to ignore the implied meaning completely.'}
added_prag=0
for k in [x for x in kcs if x.get('domain')=='PRAGMATICS']:
    model=(k.get('form') or '').strip()
    use=(k.get('use') or k.get('name') or '').strip()
    if not model: continue
    lvl=k.get('cefr','B1')
    wrong=wrong_by_level.get(lvl,wrong_by_level['B1'])
    a,b,ans=balanced_options('prag:'+k['id'],model,wrong)
    added_prag += add_ex(ex('P12_'+safe(k['id']),'AB',[k['id']],lvl,min(.92,DIFF.get(lvl,.56)),
        f'Quieres {use.lower()}. ¿Qué opción cumple mejor esa función comunicativa?', '', a,b,ans,
        f'La respuesta adecuada conserva la función: {use}.',seconds=24,tags=('pragmatics','recognition','appropriacy','v12')))

# ---------------------------------------------------------------------------
# 3. Pronunciation discrimination: actual same/different trials. The stimulus
#    itself is pre-rendered as exact audio; no microphone is required.
# ---------------------------------------------------------------------------
PAIRS=[
('A1','P11_I_IY','ship','sheep'),('A1','P11_I_IY','sit','seat'),('A1','P11_I_IY','live','leave'),
('A1','P11_B_V','berry','very'),('A1','P11_B_V','ban','van'),('A1','P11_B_V','boat','vote'),
('A1','P11_FINAL_S','cats','cats'),('A1','P11_FINAL_S','cats','dogs'),('A1','P11_FINAL_S','buses','buses'),
('A2','P11_TH','think','sink'),('A2','P11_TH','thin','sin'),('A2','P11_TH','this','dis'),
('A2','P11_ED','worked','worked'),('A2','P11_ED','worked','wanted'),('A2','P11_ED','played','played'),
('A2','P11_SCHWA','about','about'),('A2','P11_SCHWA','support','sport'),('A2','P11_SCHWA','banana','bandana'),
('B1','P11_LINKING','pick it up','pick it up'),('B1','P11_LINKING','turn it on','turn it off'),('B1','P11_LINKING','take it away','take it away'),
('B1','P11_SENT_STRESS','I need the REPORT today','I need the report TODAY'),('B1','P11_SENT_STRESS','SHE sent the email','she sent the EMAIL'),('B1','P11_SENT_STRESS','I wanted TWO','I wanted TWO'),
('B2','P11_ELISION','next day','next day'),('B2','P11_ELISION','want to','wanted to'),('B2','P11_ELISION','most people','most people'),
('B2','P11_NUCLEAR','I wanted the RED one','I wanted the BLUE one'),('B2','P11_NUCLEAR','JOHN called me','John called ME'),('B2','P11_NUCLEAR','I said MONDAY','I said MONDAY'),
('C1','P11_STANCE_INTON','Really?','Really.'),('C1','P11_STANCE_INTON','Fine?','Fine.'),('C1','P11_STANCE_INTON','You did?','You did?'),
('C1','P11_CHUNKING','When the data arrived, we checked it again.','When the data arrived, we checked it again.'),('C1','P11_CHUNKING','If we wait, we lose time.','If we wait we lose, time.'),('C1','P11_CHUNKING','What matters is the result.','What matters is the result.'),
('C2','P11_FINE_VARIATION','I could have told you.','I could have told you.'),('C2','P11_FINE_VARIATION','It might have been easier.','It may have been easier.'),('C2','P11_FINE_VARIATION','That is not necessarily so.','That is not necessarily so.'),
]
added_phono=0
for n,(lvl,kcid,w1,w2) in enumerate(PAIRS,1):
    if kcid not in kid: continue
    same=w1.casefold()==w2.casefold()
    correct='igual' if same else 'diferente'
    wrong='diferente' if same else 'igual'
    a,b,ans=balanced_options(f'phono:{n}:{w1}:{w2}',correct,wrong)
    stim=f'{w1}. {w2}.'
    added_phono += add_ex(ex(f'PD12_{n:03d}','PRON_DISCRIMINATION',[kcid],lvl,DIFF.get(lvl,.6),
        'Escucha dos realizaciones. ¿Son iguales o diferentes?',stim,a,b,ans,
        f'La respuesta es {correct}. Compara cuidadosamente el rasgo objetivo.',seconds=18,tags=('pronunciation','discrimination','audio-asset','v12','transfer')))

# ---------------------------------------------------------------------------
# 4. Lexical transfer depth: add a second self-assessment task for every
#    active lexeme that lacks a transfer-tagged item. This exercises recall in
#    a novel mental context without requiring speech or screen use.
# ---------------------------------------------------------------------------
existing_by_kc={}
for e in exercises:
    for kcid in e.get('kcIds',[]): existing_by_kc.setdefault(kcid,[]).append(e)
added_lex_transfer=0
for lx in lexemes:
    kid0=lx['id']; lvl=lx.get('cefr','A1')
    if any('lexical-transfer-v12' in e.get('tags',[]) for e in existing_by_kc.get(kid0,[])): continue
    added_lex_transfer += add_ex(ex('VX12_'+safe(kid0),'SELF_ASSESS',[kid0],lvl,min(.90,DIFF.get(lvl,.55)),
        f'Piensa en una situación nueva donde usarías la palabra inglesa que significa “{lx.get("meaningEs","")}”. Forma una frase corta mentalmente.',
        lx['lemma'],exp=f'Palabra objetivo: {lx["lemma"]}. No memorices sólo la traducción: recupera significado, sonido y un uso posible.',
        seconds=22,tags=('vocabulary','lexical-transfer-v12','transfer')))

# Predictable ordering
level=lambda x: LEVEL_ORDER.get(x.get('cefr','A1'),99)
exercises.sort(key=lambda x:(level(x),x.get('type',''),x['id']))
save('exercises.json',exercises)
checks={name:hashlib.sha256((CONTENT/name).read_bytes()).hexdigest() for name in ('kcs.json','edges.json','exercises.json','lexemes.json')}
manifest=load('manifest.json')
manifest.update({
  'version':'1.2.0','contentVersion':'1.2.0',
  'description':'SIAA content pack v1.2: base A1–C2 densa con léxico bilingüe, gramática granular, pragmática medible, listening original ampliado, discriminación fonológica y audio offline.',
  'checksums':checks,
  'coverage':{
      'kcs':len(kcs),'edges':len(edges),'staticExercises':len(exercises),'lexemes':len(lexemes),
      'generatedVariantsEstimate':sum(3+min(3,len(x.get('chunks',[]))) for x in lexemes),
      'levels':['Pre-A1','A1','A2','B1','B2','C1','C2']
  }
})
src=list(manifest.get('sources',[]))
for item in [
    'British Council / EAQUALS Core Inventory for General English (functional and topic progression reference)',
    'U.S. Department of State American English Teaching Pragmatics (open teaching reference)'
]:
    if item not in src: src.append(item)
manifest['sources']=src
save('manifest.json',manifest)
print(json.dumps({'listeningAdded':added_listening,'pragmaticsRecognitionAdded':added_prag,'phonologyDiscriminationAdded':added_phono,'lexicalTransferAdded':added_lex_transfer,'staticExercises':len(exercises)},indent=2))
