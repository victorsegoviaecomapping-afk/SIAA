import json,re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
k=[x for x in json.load(open(root/'app/src/main/assets/content/kcs.json', encoding='utf-8')) if x['id'].startswith('G_CEFRJ')]

def mode(k):
 kid=k.get('id',''); f=k.get('form',''); meaning=k.get('meaning','')
 if '_INT_NEG' in kid: return 'nq'
 if '_INT_AFF' in kid or meaning=='pregunta' or f.rstrip().endswith('?') or f.startswith(('WH- QUESTION:','FUNCTIONAL QUESTION:')): return 'q'
 if kid.endswith('_NEG') or meaning=='forma negativa' or re.search(r"\b(?:not|isn't|aren't|wasn't|weren't|don't|doesn't|didn't|cannot|can't|couldn't|won't|wouldn't|shouldn't|mustn't|hasn't|haven't|hadn't)\b", f, re.I): return 'neg'
 if meaning=='instrucción o imperativo' or 'IMPERATIVE' in f: return 'imp'
 return 'aff'

def apply_mode(variants,k):
 return variants.get(mode(k)) or variants.get('aff') or next(iter(variants.values()))

def suffix(k):
 return {'q':' en preguntas','nq':' en preguntas negativas','neg':' en negativas','imp':' en instrucciones','aff':''}[mode(k)]

def tense_family(kid: str, form: str) -> str:
    ordered=['FUTPFPRG','PASTPFPRG','PRPFPRG','FUTPF','PASTPF','PRPF','FUTPRG','PASTPRG','PRPRG','PAST_BE','PAST_DO','PRESENT_BE','PRESENT_DOES','PRESENT_DO','FUT']
    for key in ordered:
        if key in kid: return key
    by_form={
      'TENSE/ASPECT: PRESENT (BE)':'PRESENT_BE','TENSE/ASPECT: PRESENT (lexical verbs)':'PRESENT_DO',
      'TENSE/ASPECT: PRESENT (lexical verbs; third person & singular)':'PRESENT_DOES','TENSE/ASPECT: PRESENT PROGRESSIVE':'PRPRG',
      'TENSE/ASPECT: PRESENT PERFECT':'PRPF','TENSE/ASPECT: PRESENT PERFECT PROGRESSIVE':'PRPFPRG',
      'TENSE/ASPECT: PAST (BE)':'PAST_BE','TENSE/ASPECT: PAST PROGRESSIVE':'PASTPRG','TENSE/ASPECT: PAST PERFECT':'PASTPF',
      'TENSE/ASPECT: PAST PERFECT PROGRESSIVE':'PASTPFPRG','TENSE/ASPECT: FUTURE':'FUT','TENSE/ASPECT: FUTURE PROGRESSIVE':'FUTPRG',
      'TENSE/ASPECT: FUTURE PERFECT':'FUTPF','TENSE/ASPECT: FUTURE PERFECT PROGRESSIVE':'FUTPFPRG'}
    return by_form.get(form,'')

def label(k):
 f=k['form']; s=suffix(k)
 special={
  'Do+IMPERATIVE':'Imperativo enfático con «do»', 'IMPERATIVE: AFFIRMATIVE (lexical verbs)':'Imperativo afirmativo',
  'WH- QUESTION: How ADJ/ADV ...?':'Preguntas con «How + adjetivo/adverbio»',
  'VERB not to DO':'Verbo + «not to» + infinitivo', 'VERB OBJECT to DO':'Verbo + objeto + infinitivo con «to»', 'VERB to DO':'Verbo + infinitivo con «to»',
  'VERB V-ING':'Verbo + forma «-ing»', 'VERB OBJECT V-ING':'Verbo + objeto + forma «-ing»', 'VERB not V-ING':'Verbo + «not» + forma «-ing»', 'VERB OBJECT not V-ING':'Verbo + objeto + «not» + forma «-ing»',
  'COMPOUND RELATIVE PRONOUN:':'Relativos compuestos', 'PRONOUNS: the other/others':'Uso de «the other/others»', "PRONOUN: others (excluding 'the":'Uso de «others»',
  'SENTENCE PATTERN: SUBJECT+MAKE+OBJECT+COMPLEMENT (ADJ)':'Causativo «make + objeto + adjetivo»',
  'ASK/TELL+NP+to+INFINITIVE':'«ask/tell + persona + to + verbo»', "know/wonder+WH-(CLAUSE) (except for 'whether')":'Preguntas indirectas con «know/wonder»',
  'there+AUX+be':'«there + modal + be»', 'AUX+PERFECT':'Modal + «have + participio»', 'AUX+PROGRESSIVE':'Modal + «be + -ing»',
  'it+BE(+ADV)+ADJ(+for+NP)+to+INFINITIVE':'Estructura impersonal «it is + adjetivo + to…»', 'it+BE(+ADV)+ADJ+that+CLAUSE':'Estructura impersonal «it is + adjetivo + that…»',
  'BE to DO':'«be to + verbo»', 'BE about to DO':'«be about to + verbo»', 'WH-+to+INFINITIVE':'Palabra interrogativa + infinitivo con «to»',
  'such (a/an) ADJ NOUN':'«such + adjetivo + sustantivo»', 'AS IF/THOUGH+SECOND CONDITIONAL':'«as if/as though» con hipótesis presente', 'AS IF/THOUGH+THIRD CONDITIONAL':'«as if/as though» con hipótesis pasada',
  'WISH+SECOND CONDITIONAL':'«wish» para deseos irreales presentes', 'WISH+THIRD CONDITIONAL':'«wish» para arrepentimientos pasados', 'IF ONLY+SECOND CONDITIONAL':'«if only» para deseos irreales presentes', 'IF ONLY+THIRD CONDITIONAL':'«if only» para arrepentimientos pasados',
  'IF+SHOULD (e.g. if it should rain tomorrow)':'Condicional formal con «if + should»', 'WH-EVER':'Formas con «-ever» (whoever/whatever/whichever)',
 }
 if f=='IMPERATIVE: AFFIRMATIVE (lexical verbs)':
  if mode(k)=='neg': return 'Imperativo negativo'
  return 'Imperativo afirmativo'
 if f in special: return special[f]+s
 if f.startswith('MODAL/AUX: '): return f'Uso de «{f.split(": ",1)[1]}»{s}'
 if f.startswith('TENSE/ASPECT: '):
  fam=tense_family(k.get('id',''),f)
  mp={'PRESENT_BE':'presente de «be»','PRESENT_DO':'presente simple','PRESENT_DOES':'presente simple en tercera persona','PRPRG':'presente continuo','PRPF':'presente perfecto','PRPFPRG':'presente perfecto continuo','PAST_BE':'pasado de «be»','PAST_DO':'pasado simple','PASTPRG':'pasado continuo','PASTPF':'pasado perfecto','PASTPFPRG':'pasado perfecto continuo','FUT':'futuro con «will»','FUTPRG':'futuro continuo','FUTPF':'futuro perfecto','FUTPFPRG':'futuro perfecto continuo'}
  return mp.get(fam,'Tiempo verbal')+s
 if f.startswith('PASSIVE: '):
  t=f.split(': ',1)[1]
  mp={'PRESENT':'presente','PAST':'pasado','PRESENT PROGRESSIVE':'presente continuo','PAST PROGRESSIVE':'pasado continuo','PRESENT PERFECT':'presente perfecto','PAST PERFECT':'pasado perfecto','FUTURE':'futuro','FUTURE PROGRESSIVE':'futuro continuo','FUTURE PERFECT':'futuro perfecto','AUX':'con verbos modales','AUX+PERFECT':'perfecta con verbos modales','DIRECT OBJECTS OF GIVE/PASS/SEND/SHOW/TEACH/TELL AS SUBJECTS':'con el objeto directo como sujeto','INDIRECT OBJECTS OF GIVE/PASS/SEND/SHOW/TEACH/TELL AS SUBJECTS':'con el objeto indirecto como sujeto'}
  return 'Voz pasiva: '+mp.get(t,t.lower())+s
 if f.startswith('WH- QUESTION: '):
  w=f.split(': ',1)[1].replace('...?', '').strip()
  return f'Preguntas con «{w}»'
 if f.startswith('FUNCTIONAL QUESTION: '): return 'Pregunta funcional «'+f.split(': ',1)[1]+'»'
 if f.startswith('ADVERBIAL CLAUSE: '): return 'Oraciones con «'+f.split(': ',1)[1]+'»'
 if f.startswith('SENTENCE PATTERN: '):
  x=f.split(': ',1)[1]
  repl=[('SUBJECT','sujeto'),('DIRECT OBJECT','objeto directo'),('INDIRECT OBJECT','objeto indirecto'),('OBJECT','objeto'),('COMPLEMENT','complemento'),('BECOME/FEEL/GO/LOOK/SEEM/SOUND','become/feel/go/look/seem/sound'),('GIVE/PASS/SEND/SHOW/TEACH/TELL','give/pass/send/show/teach/tell'),('(ADJ)','(adjetivo)'),('+V',' + verbo'),('V+','verbo + ')]
  for a,b in repl:x=x.replace(a,b)
  x=x.replace('+',' + ')
  return 'Patrón '+re.sub(r'\s+',' ',x).strip()+s
 # named families
 prefix_map={
 'COMPARATIVE OF SUPERIORITY: -er':'Comparativos con «-er»',
 'COMPARATIVE OF SUPERIORITY: more+ADJ/ADV':'Comparativos con «more»',
 'SUPERLATIVE OF SUPERIORITY: -est':'Superlativos con «-est»',
 'SUPERLATIVE OF SUPERIORITY: most+ADJ/ADV':'Superlativos con «most»',
 'COMPARISON OF EQUALITY: as ... as':'Comparación con «as … as»',
 'COMPARISON OF EQUALITY: not as/so ... as':'Comparación con «not as/so … as»',
 'COMPARATIVE/SUPERLATIVE OF INFERIORITY':'Comparativos y superlativos con «less/least»',
 'COMPARATIVE and COMPARATIVE (the same adjective)':'Cambio gradual: «colder and colder»',
 'INTENSIFIED COMPARATIVES: a lot/by far/even/far/much/still':'Comparativos intensificados',
 'INTENSIFIED SUPERLATIVES: by far/far/far and away/much':'Superlativos intensificados',
 'the COMPARATIVE (...), the COMPARATIVE':'Correlación «the more…, the more…»',
 'COORDINATING CONJUNCTIONS':'Conjunciones «and, but, or»',
 'SUBORDINATE CLAUSE: after/albeit/although/because/before/despite/except/for/lest/like/once/since/so/than/though/till/unless/until/where/whereas/wheresoever/whether/while/whilst tagged as IN':'Conectores de subordinación',
 'INDEFINITE ARTICLES':'Artículos «a/an»','DEFINITE ARTICLES':'Artículo «the»','PREPOSITIONS':'Preposiciones frecuentes',
 'DETERMINERS: some/any':'Determinantes «some/any»','DETERMINER: no':'Determinante «no»','DETERMINER: another':'Determinante «another»',
 'INDEFINITE PRONOUNS':'Pronombres indefinidos','INDEFINITE PRONOUN: none':'Pronombre «none»','INDEFINITE PRONOUN/PROP-WORDS: ones (except for \'one\')':'Sustitución con «ones»',
 "POSSESSIVE PRONOUNS (except for 'his' and 'its')":'Pronombres posesivos','REFLEXIVE PRONOUNS':'Pronombres reflexivos',
 'RECIPROCAL PRONOUN: each other':'Recíproco «each other»','RECIPROCAL PRONOUN: one another':'Recíproco «one another»',
 'ADVERBS OF FREQUENCY: always/usually/often/frequently/occasionally/sometimes/rarely':'Adverbios de frecuencia',
 'ADVERBS: INTENSIFIERS: extremely/greatly/really/so/terribly/too/unbelievably/very':'Intensificadores como «really/very»',
 'ADVERBS OF NEGATION: never':'Negación con «never»','ADVERBS OF QUASI-NEGATION: hardly/little/scarcely/seldom':'Adverbios de sentido casi negativo',
 'ADVERBS OF ATTITUDES: apparently/clearly/fortunately/frankly/unfortunately':'Adverbios de actitud',
 'PHRASAL VERBS (V+PARTICLE)':'Phrasal verbs: verbo + partícula','PHRASAL VERBS (V+NP+PARTICLE)':'Phrasal verbs separables','PHRASAL VERBS (V+PARTICLE+PREP+NP)':'Phrasal verbs con preposición',
 'PREPOSITION STRANDING':'Preposición al final de una relativa o pregunta',
 'TAG QUESTION: FOLLOWING AFFIRMATIVE SENTENCE':'Question tags tras afirmación','TAG QUESTION: FOLLOWING NEGATIVE SENTENCE':'Question tags tras negación',
 'CONDITIONAL: SECOND':'Segundo condicional','CONDITIONAL: THIRD':'Tercer condicional',
 'EXCLAMATION: How ADJ/ADV ...!':'Exclamaciones con «How…!»','EXCLAMATION: What ...!':'Exclamaciones con «What…!»',
 'INDIRECT SPEECH: SAY/EXPLAIN/REPORT':'Estilo indirecto con «say/report»','INDIRECT SPEECH: TELL':'Estilo indirecto con «tell»',
 'INDIRECT QUESTION: ASK/REMIND/SHOW/TEACH/TELL':'Preguntas indirectas tras «ask/tell…»','INDIRECT QUESTION: DECIDE/EXPLAIN/KNOW/LEARN/SEE/UNDERSTAND/WONDER':'Preguntas indirectas tras «wonder/know…»',
 'INVERSION: Hardly/Little/Never/No sooner/Scarcely/Seldom ....':'Inversión tras expresiones negativas','INVERSION: neither/nor+BE/HAVE/DO/AUX+PERSONAL PRON':'Acuerdo negativo con «neither/nor»','INVERSION: so+BE/HAVE/DO/AUX+PERSONAL PRON':'Acuerdo afirmativo con «so»','INVERSION: CONDITIONAL WITH SHOULD (e.g. Should it rain tomorrow.....)':'Condicional invertido con «should»',
 'HAVE/LET/MAKE+NP+INFINITIVE':'Causativos con «have/let/make»','HAVE/GET+NP+PAST PARTICIPLE':'Causativo «have/get something done»','GET+NP+PRESENT PARTICIPLE':'Causativo «get someone doing»','GET+PAST PARTICIPLE':'Construcción «get + participio»',
 'TO-INFINITIVE: to DO (not preceded by \'not\')':'Infinitivo con «to»','TO-INFINITIVE: not to DO':'Infinitivo negativo «not to…»','TO-INFINITIVE: to be DONE':'Infinitivo pasivo «to be + participio»','TO-INFINITIVE: to have DONE':'Infinitivo perfecto «to have + participio»','TO-INFINITIVE: WITH NOTIONAL SUBJECT':'Infinitivo con sujeto explícito',
 'V-ING (not preceded by \'not\')':'Forma «-ing»','V-ING: WITH NOTIONAL SUBJECT (expressed as possessive pronouns)':'Forma «-ing» con sujeto posesivo','PREP+V-ING':'Preposición + forma «-ing»',
 'PARTICIPIAL CONSTRUCTION: PRESENT PARTICIPLE':'Construcción con participio presente','PARTICIPIAL CONSTRUCTION: PAST PARTICIPLE':'Construcción con participio pasado',
 'PREMODIFYING PRESENT PARTICIPLE':'Participio presente antes del sustantivo','POSTMODIFYING PRESENT':'Participio presente después del sustantivo','PREMODIFYING PAST PARTICIPLE':'Participio pasado antes del sustantivo','POSTMODIFYING PAST PARTICIPLE':'Participio pasado después del sustantivo',
 'GENITIVE RELATIVE PRONOUN':'Relativas con «whose»','ELLIPTICAL ACCUSATIVE RELATIVE PRONOUN':'Relativa sin pronombre objeto','NOMINATIVE RELATIVE PRONOUN: who':'Relativas de sujeto con «who»','NOMINATIVE RELATIVE PRONOUN: which':'Relativas de sujeto con «which»','NOMINATIVE RELATIVE PRONOUN:':'Relativas de sujeto','ACCUSATIVE RELATIVE PRONOUN: who':'Relativas de objeto con «who»','ACCUSATIVE RELATIVE PRONOUN: whom':'Relativas de objeto con «whom»','ACCUSATIVE RELATIVE PRONOUN: which':'Relativas de objeto con «which»','ACCUSATIVE RELATIVE PRONOUN: that':'Relativas de objeto con «that»','PREP+RELATIVE PRONOUN':'Preposición + pronombre relativo','RELATIVE PRONOUN: NONRESTRICTIVE':'Relativas no restrictivas','RELATIVE ADVERB: NONRESTRICTIVE':'Relativas no restrictivas con adverbio','RELATIVE ADVERB: WITH':'Relativas con adverbio relativo','RELATIVE ADVERB: WITHOUT ANTECEDENT':'Relativas libres con adverbio',
 }
 if f in prefix_map:return prefix_map[f]+s
 # ordinary exact forms: turn into readable usage label after replacing notation
 x=f
 reps=[('PAST PARTICIPLE','participio pasado'),('PRESENT PARTICIPLE','participio presente'),('INFINITIVE','infinitivo'),('CLAUSE','oración'),('PLURAL NOUN','sustantivo plural'),('UNCOUNTABLE NOUN','sustantivo incontable'),('PERSONAL PRON','pronombre personal'),('NP','persona/cosa'),('ADJ/ADV','adjetivo/adverbio'),('ADJ','adjetivo'),('ADV','adverbio'),('V-ING','forma -ing'),('VVG','forma -ing'),('VN','participio pasado'),('DO','verbo'),(' V ',' verbo '),('+V','+verbo')]
 for a,b in reps:x=x.replace(a,b)
 x=x.replace('+',' + ')
 x=re.sub(r'\s+',' ',x).strip()
 return 'Uso de «'+x+'»'+s

def negative_question(q: str) -> str:
 contractions={
  'Is':'Isn’t','Are':'Aren’t','Was':'Wasn’t','Were':'Weren’t','Do':'Don’t','Does':'Doesn’t','Did':'Didn’t',
  'Has':'Hasn’t','Have':'Haven’t','Had':'Hadn’t','Will':'Won’t','Can':'Can’t','Could':'Couldn’t',
  'Would':'Wouldn’t','Should':'Shouldn’t','Must':'Mustn’t','Ought':'Oughtn’t'
 }
 first=q.split(' ',1)[0]
 if first in contractions and ' ' in q:
  return contractions[first]+' '+q.split(' ',1)[1]
 for prefix in ('May she ','Might she ','May the report ','Might the report '):
  if q.startswith(prefix):
   return prefix + 'not ' + q[len(prefix):]
 return q

def example(k):
 f=k['form']; kid=k['id']; m=mode(k)
 if f=='AUX+PERFECT':
  vals={'aff':'She might have forgotten.','neg':'She might not have forgotten.','q':'Might she have forgotten?','nq':'Might she not have forgotten?'}
  return vals.get(m,vals['aff'])
 if f=='AUX+PROGRESSIVE':
  vals={'aff':'She may be working now.','neg':'She may not be working now.','q':'May she be working now?','nq':'May she not be working now?'}
  return vals.get(m,vals['aff'])
 if f=='there+be':
  vals={'aff':'There is a bus at six.','neg':'There is not a bus at six.','q':'Is there a bus at six?','nq':'Isn’t there a bus at six?'}
  return vals.get(m,vals['aff'])
 if f=='there+AUX+be':
  vals={'aff':'There may be a delay.','neg':'There may not be a delay.','q':'Could there be a delay?','nq':'Couldn’t there be a delay?'}
  return vals.get(m,vals['aff'])
 if f=='IMPERATIVE: AFFIRMATIVE (lexical verbs)':
  return 'Do not open the door.' if m=='neg' else 'Open the door.'
 # helper variants
 def v(aff,neg=None,q=None,nq=None):
  return {'aff':aff,'neg':neg or aff,'q':q or aff,'imp':aff}.get(m) or aff
 # Modal
 if f.startswith('MODAL/AUX: '):
  x=f.split(': ',1)[1]
  maps={
   'can':('She can swim.','She cannot swim.','Can she swim?','Can’t she swim?'),
   'could':('She could help us.','She could not help us.','Could she help us?','Couldn’t she help us?'),
   'will':('She will call tomorrow.','She will not call tomorrow.','Will she call tomorrow?','Won’t she call tomorrow?'),
   'would':('She would agree.','She would not agree.','Would she agree?','Wouldn’t she agree?'),
   'should':('She should rest.','She should not rest yet.','Should she rest?','Shouldn’t she rest?'),
   'may':('She may arrive later.','She may not arrive today.','May she come in?','May she not come in?'),
   'might':('She might be late.','She might not be late.','Might she be late?','Might she not be late?'),
   'must':('She must leave now.','She must not enter.','Must she leave now?','Mustn’t she leave now?'),
   'have to':('She has to leave early.','She does not have to leave early.','Does she have to leave early?','Doesn’t she have to leave early?'),
   'have got to':("She has got to leave now.","She hasn’t got to leave yet.","Has she got to leave now?","Hasn’t she got to leave now?"),
   '(have) got to':("She’s got to leave now.","She hasn’t got to leave yet.","Has she got to leave now?","Hasn’t she got to leave now?"),
   'be going to':('She is going to travel next week.','She is not going to travel next week.','Is she going to travel next week?','Isn’t she going to travel next week?'),
   'ought to':('She ought to call them.','She ought not to call them yet.','Ought she to call them?','Oughtn’t she to call them?'),
   'used to':('She used to live here.','She did not use to live here.','Did she use to live here?','Didn’t she use to live here?'),
   'dare (to)':('She dares to ask difficult questions.','She does not dare to ask.','Does she dare to ask?','Doesn’t she dare to ask?'),
   'need (to)':('She needs to leave.','She does not need to leave.','Does she need to leave?','Doesn’t she need to leave?'),
   'be able to':('She is able to solve it.','She is not able to solve it.','Is she able to solve it?','Isn’t she able to solve it?'),
   'may well':('She may well be right.','She may well not agree.','May she well be right?','May she not be right?'),
   'may as well':('We may as well leave now.','We may as well not wait.','May we as well leave now?','May we not wait?'),
   'might as well':('We might as well start now.','We might as well not wait.','Might we as well start now?','Might we not wait?'),
   'would rather':('I would rather stay home.','I would rather not stay home.','Would you rather stay home?','Wouldn’t you rather stay home?'),
  }
  a,n,q,nq=maps.get(x,('She can help.','She cannot help.','Can she help?','Can’t she help?'))
  return {'aff':a,'neg':n,'q':q,'nq':nq}.get(m,a)
 # tense/aspect derive from ID when useful
 if f.startswith('TENSE/ASPECT: '):
  fam=tense_family(kid,f)
  mp={
   'PRESENT_BE':('She is ready.','She is not ready.','Is she ready?'),
   'PRESENT_DOES':('She works here.','She does not work here.','Does she work here?'),
   'PRESENT_DO':('They work here.','They do not work here.','Do they work here?'),
   'PAST_DO':('She worked yesterday.','She did not work yesterday.','Did she work yesterday?'),
   'PAST_BE':('She was tired.','She was not tired.','Was she tired?'),
   'PRPRG':('She is working now.','She is not working now.','Is she working now?'),
   'PASTPRG':('She was working at six.','She was not working at six.','Was she working at six?'),
   'PRPF':('She has finished the report.','She has not finished the report.','Has she finished the report?'),
   'PASTPF':('She had finished before noon.','She had not finished before noon.','Had she finished before noon?'),
   'PRPFPRG':('She has been working for two hours.','She has not been working for two hours.','Has she been working for two hours?'),
   'PASTPFPRG':('She had been working for two hours.','She had not been working for two hours.','Had she been working for two hours?'),
   'FUT':('She will call tomorrow.','She will not call tomorrow.','Will she call tomorrow?'),
   'FUTPRG':('She will be working at six.','She will not be working at six.','Will she be working at six?'),
   'FUTPF':('She will have finished by noon.','She will not have finished by noon.','Will she have finished by noon?'),
   'FUTPFPRG':('She will have been working for two hours by noon.','She will not have been working for two hours by noon.','Will she have been working for two hours by noon?'),
  }
  a,n,q=mp.get(fam,('She works here.','She does not work here.','Does she work here?'))
  return {'aff':a,'neg':n,'q':q,'nq':negative_question(q)}.get(m,a)
 if f.startswith('PASSIVE: '):
  t=f.split(': ',1)[1]
  mp={
   'PRESENT':('The report is checked every day.','The report is not checked every day.','Is the report checked every day?'),
   'PAST':('The report was checked yesterday.','The report was not checked yesterday.','Was the report checked yesterday?'),
   'PRESENT PROGRESSIVE':('The report is being checked now.','The report is not being checked now.','Is the report being checked now?'),
   'PAST PROGRESSIVE':('The report was being checked at noon.','The report was not being checked at noon.','Was the report being checked at noon?'),
   'PRESENT PERFECT':('The report has been checked.','The report has not been checked.','Has the report been checked?'),
   'PAST PERFECT':('The report had been checked before noon.','The report had not been checked before noon.','Had the report been checked before noon?'),
   'FUTURE':('The report will be checked tomorrow.','The report will not be checked tomorrow.','Will the report be checked tomorrow?'),
   'FUTURE PROGRESSIVE':('The road will be being repaired at noon.','The road will not be being repaired at noon.','Will the road be being repaired at noon?'),
   'FUTURE PERFECT':('The report will have been checked by noon.','The report will not have been checked by noon.','Will the report have been checked by noon?'),
   'AUX':('The report can be checked online.','The report cannot be checked online.','Can the report be checked online?'),
   'AUX+PERFECT':('The report may have been checked already.','The report may not have been checked yet.','May the report have been checked already?'),
   'DIRECT OBJECTS OF GIVE/PASS/SEND/SHOW/TEACH/TELL AS SUBJECTS':('The book was given to Maria.','The book was not given to Maria.','Was the book given to Maria?'),
   'INDIRECT OBJECTS OF GIVE/PASS/SEND/SHOW/TEACH/TELL AS SUBJECTS':('Maria was given the book.','Maria was not given the book.','Was Maria given the book?'),
  }
  a,n,q=mp[t];return {'aff':a,'neg':n,'q':q,'nq':negative_question(q)}.get(m,a)
 # exact question/fixed forms and groups
 fixed={
  'there+be':'There is a bus at six.', 'WH-EVER':'Take whichever seat you prefer.',
  'COORDINATING CONJUNCTIONS':'I was tired, but I finished the work.',
  'SUBORDINATE CLAUSE: after/albeit/although/because/before/despite/except/for/lest/like/once/since/so/than/though/till/unless/until/where/whereas/wheresoever/whether/while/whilst tagged as IN':'Although it was late, we continued working.',
  "hope/know/think+CLAUSE (without 'that')":"I think she is right.",
  'ADVERBIAL CLAUSE: when':'When I get home, I will call you.','ADVERBIAL CLAUSE: if':'If it rains, we will stay inside.','ADVERBIAL CLAUSE: as':'As I was leaving, the phone rang.','ADVERBIAL CLAUSE: as soon as':'Call me as soon as you arrive.','ADVERBIAL CLAUSE: by the time':'By the time we arrived, the meeting had started.','ADVERBIAL CLAUSE: so that':'I wrote it down so that I would not forget.',
  'COMPARATIVE OF SUPERIORITY: -er':'This route is shorter than the other one.','COMPARATIVE OF SUPERIORITY: more+ADJ/ADV':'This option is more reliable than the first.','SUPERLATIVE OF SUPERIORITY: -est':'This is the fastest route.','SUPERLATIVE OF SUPERIORITY: most+ADJ/ADV':'This is the most reliable option.','COMPARISON OF EQUALITY: as ... as':'This bag is as heavy as that one.','COMPARISON OF EQUALITY: not as/so ... as':'This route is not as long as the other one.','COMPARATIVE/SUPERLATIVE OF INFERIORITY':'This option is less expensive than the first.','COMPARATIVE and COMPARATIVE (the same adjective)':'It is getting colder and colder.','INTENSIFIED COMPARATIVES: a lot/by far/even/far/much/still':'This route is much faster.','INTENSIFIED SUPERLATIVES: by far/far/far and away/much':'This is by far the best option.','the COMPARATIVE (...), the COMPARATIVE':'The more you practice, the easier it gets.',
  'INDEFINITE ARTICLES':'I bought a book and an umbrella.','DEFINITE ARTICLES':'The book on the table is mine.','DETERMINER: no':'There is no milk left.','DETERMINERS: some/any':'We have some coffee, but we do not have any tea.','DETERMINER: another':'Can I have another cup?',
  'PREPOSITIONS':'The keys are on the table near the door.',
  'my/our/your/her/their (except for \'his\' and \'its\')':'This is my book.','me/us/him/her/them (except for \'you\' and \'it\')':'She called me yesterday.','POSSESSIVE PRONOUNS (except for \'his\' and \'its\')':'This seat is mine.','REFLEXIVE PRONOUNS':'She taught herself English.','RECIPROCAL PRONOUN: each other':'They help each other.','RECIPROCAL PRONOUN: one another':'The team members support one another.','INDEFINITE PRONOUNS':'Someone is waiting outside.','INDEFINITE PRONOUN: none':'None of the answers is correct.',"INDEFINITE PRONOUN/PROP-WORDS: ones (except for 'one')":'I prefer the blue ones.','PRONOUNS: the other/others':'One option is cheap; the other is faster.',"PRONOUN: others (excluding 'the":'Some people agreed; others did not.',
  'ADVERBS OF FREQUENCY: always/usually/often/frequently/occasionally/sometimes/rarely':'She usually takes the bus.','ADVERBS: INTENSIFIERS: extremely/greatly/really/so/terribly/too/unbelievably/very':'The test was really difficult.','ADVERBS OF NEGATION: never':'I never drink coffee at night.','ADVERBS OF QUASI-NEGATION: hardly/little/scarcely/seldom':'She rarely complains and seldom arrives late.','ADVERBS OF ATTITUDES: apparently/clearly/fortunately/frankly/unfortunately':'Fortunately, nobody was hurt.',
  'PHRASAL VERBS (V+PARTICLE)':'Please turn off the light.','PHRASAL VERBS (V+NP+PARTICLE)':'She turned the radio off.','PHRASAL VERBS (V+PARTICLE+PREP+NP)':'She looks forward to the holiday.',
  'PREPOSITION STRANDING':'The person I spoke to was helpful.',
  'NOMINATIVE RELATIVE PRONOUN: who':'The woman who called is my manager.','NOMINATIVE RELATIVE PRONOUN: which':'The train which leaves at six is usually full.','NOMINATIVE RELATIVE PRONOUN:':'The person that called left a message.','ACCUSATIVE RELATIVE PRONOUN: who':'The person who I met was friendly.','ACCUSATIVE RELATIVE PRONOUN: whom':'The person whom I met was friendly.','ACCUSATIVE RELATIVE PRONOUN: which':'The book which I bought is useful.','ACCUSATIVE RELATIVE PRONOUN: that':'The book that I bought is useful.','ELLIPTICAL ACCUSATIVE RELATIVE PRONOUN':'The book I bought is useful.','GENITIVE RELATIVE PRONOUN':'The student whose phone rang apologized.','PREP+RELATIVE PRONOUN':'The person to whom I spoke was helpful.','RELATIVE PRONOUN: NONRESTRICTIVE':'My brother, who lives in Cusco, is visiting.','RELATIVE ADVERB: NONRESTRICTIVE':'Lima, where I grew up, is very busy.','RELATIVE ADVERB: WITH':'The city where I grew up is small.','RELATIVE ADVERB: WITHOUT ANTECEDENT':'Where you sit does not matter.','COMPOUND RELATIVE PRONOUN:':'Whoever arrives first can start.',
  'TAG QUESTION: FOLLOWING AFFIRMATIVE SENTENCE':'You are ready, are you not?','TAG QUESTION: FOLLOWING NEGATIVE SENTENCE':'You are not busy, are you?',
  'CONDITIONAL: SECOND':'If I had more time, I would travel.','CONDITIONAL: THIRD':'If I had known, I would have called.','WISH+SECOND CONDITIONAL':'I wish I had more time.','WISH+THIRD CONDITIONAL':'I wish I had called earlier.','IF ONLY+SECOND CONDITIONAL':'If only I had more time.','IF ONLY+THIRD CONDITIONAL':'If only I had called earlier.','AS IF/THOUGH+SECOND CONDITIONAL':'He talks as if he knew everything.','AS IF/THOUGH+THIRD CONDITIONAL':'She looked as if she had seen a ghost.','IF+SHOULD (e.g. if it should rain tomorrow)':'If you should need help, call me.','INVERSION: CONDITIONAL WITH SHOULD (e.g. Should it rain tomorrow.....)':'Should you need help, call me.','had it not been for ...':'Had it not been for your help, we would have failed.',"if it hadn't been for ...":'If it had not been for your help, we would have failed.','if it were not for ...':'If it were not for the rain, we could go out.','were it not for ...':'Were it not for the rain, we could go out.',
  'INDIRECT SPEECH: SAY/EXPLAIN/REPORT':'She said that she was tired.','INDIRECT SPEECH: TELL':'She told me that she was tired.','INDIRECT QUESTION: ASK/REMIND/SHOW/TEACH/TELL':'I asked where the station was.','INDIRECT QUESTION: DECIDE/EXPLAIN/KNOW/LEARN/SEE/UNDERSTAND/WONDER':'I wonder where he went.','know/wonder+WH-(CLAUSE) (except for \'whether\')':'I wonder where she lives.','whether':'I do not know whether he will come.','the fact(s) that+CLAUSE':'The fact that she called surprised me.','V+that+CLAUSE':'I believe that the plan will work.',
  'HAVE/LET/MAKE+NP+INFINITIVE':'They made us wait.','HAVE/GET+NP+PAST PARTICIPLE':'I had my phone repaired.','GET+NP+PRESENT PARTICIPLE':'The joke got everyone laughing.','GET+PAST PARTICIPLE':'He got hurt during the game.',
  'TO-INFINITIVE: to DO (not preceded by \'not\')':'I want to learn.','TO-INFINITIVE: not to DO':'She decided not to go.','VERB not to DO':'She promised not to tell anyone.','VERB OBJECT to DO':'They asked me to wait.','VERB to DO':'She hopes to travel.','TO-INFINITIVE: WITH NOTIONAL SUBJECT':'It is important for students to practice.','TO-INFINITIVE: to be DONE':'The work needs to be completed.','TO-INFINITIVE: to have DONE':'She seems to have forgotten.','PREP+V-ING':'She is interested in learning English.','V-ING (not preceded by \'not\')':'Swimming is good exercise.','VERB V-ING':'She enjoys reading.','VERB OBJECT V-ING':'I saw him crossing the street.','not+V-ING':'Not knowing the answer, she stayed quiet.','VERB not V-ING':'She regretted not calling earlier.','VERB OBJECT not V-ING':'I remember him not wanting to leave.','V-ING: WITH NOTIONAL SUBJECT (expressed as possessive pronouns)':'I appreciate your helping me.','too ADJ/ADV to+INFINITIVE':'The box is too heavy to carry.','ADJ/ADV enough (except for \'not enough\')':'The room is quiet enough to work in.','so as to DO':'He whispered so as not to wake the baby.','in order to DO':'She left early in order to catch the bus.','BE to DO':'The president is to visit tomorrow.','BE about to DO':'The train is about to leave.','WH-+to+INFINITIVE':'I do not know what to say.','ASK/TELL+NP+to+INFINITIVE':'She told me to wait.',
  'PARTICIPIAL CONSTRUCTION: PRESENT PARTICIPLE':'Walking home, I saw Ana.','PARTICIPIAL CONSTRUCTION: PAST PARTICIPLE':'Built in 1900, the house needs repairs.','PREMODIFYING PRESENT PARTICIPLE':'A smiling child opened the door.','POSTMODIFYING PRESENT':'The people waiting outside are students.','PREMODIFYING PAST PARTICIPLE':'The broken window was replaced.','POSTMODIFYING PAST PARTICIPLE':'The documents attached to the email are complete.','having+PAST PARTICIPLE':'Having finished the report, she went home.','having been+PAST PARTICIPLE':'Having been warned, he was careful.','being+PAST PARTICIPLE':'Being invited was a surprise.',
  'INVERSION: neither/nor+BE/HAVE/DO/AUX+PERSONAL PRON':'I do not like it, and neither does she.','INVERSION: so+BE/HAVE/DO/AUX+PERSONAL PRON':'I like it, and so does she.','INVERSION: Hardly/Little/Never/No sooner/Scarcely/Seldom ....':'Never have I seen such a view.',
  'EXCLAMATION: How ADJ/ADV ...!':'How quickly she runs!','EXCLAMATION: What ...!':'What a beautiful view!',
  'Do+IMPERATIVE':'Do be careful.','IMPERATIVE: AFFIRMATIVE (lexical verbs)':'Open the door.',"let's (not followed by 'not')":"Let’s go.","let's not":"Let’s not rush.",'Please+INFINITIVE':'Please sit down.',"Please+don't/never+INFINITIVE":'Please do not touch that.',
  'few PLURAL NOUN':'Few students missed the class.','little UNCOUNTABLE NOUN':'There is little time left.','much UNCOUNTABLE NOUN':'We do not have much time.','-thing ADJ':'I need something useful.','such (a/an) ADJ NOUN':'It was such a difficult test.','so ADJ/ADV (that) CLAUSE':'It was so cold that we stayed inside.','here is/are':'Here is your ticket.','These/Those N':'Those books are new.','This/That N':'This book is mine.','These/Those are':'Those are my keys.','These/Those are not':'Those are not my keys.','This/That is':'This is my seat.','This/That is not':'This is not my seat.','It is':'It is cold today.','It is not':'It is not cold today.','I am':'I am ready.','I am not':'I am not ready.','You are':'You are early.','You are not':'You are not late.','he/she is':'She is at home.','he/she is not':'She is not at home.','they are':'They are ready.','they are not':'They are not ready.','we are':'We are ready.','we are not':'We are not ready.',
  'it+BE(+ADV)+ADJ(+for+NP)+to+INFINITIVE':'It is important for you to rest.','it+BE(+ADV)+ADJ+that+CLAUSE':'It is clear that the plan needs work.','there+AUX+be':'There may be a delay.',
  'AUX+PERFECT':'She might have forgotten.','AUX+PROGRESSIVE':'She may be working now.','did DO':'I did call you.','do/does DO':'She does understand the problem.',
 }
 if f in fixed:
  ex=fixed[f]
  # apply obvious question/negative variants for simple fixed form duplicates separately below
  if f=='there+be': return {'aff':'There is a bus at six.','neg':'There is not a bus at six.','q':'Is there a bus at six?','nq':'Isn’t there a bus at six?'}.get(m,'There is a bus at six.')
  if f=='there+AUX+be': return {'aff':'There may be a delay.','neg':'There may not be a delay.','q':'Could there be a delay?','nq':'Couldn’t there be a delay?'}.get(m,'There may be a delay.')
  return ex
 # WH questions
 if f.startswith('WH- QUESTION: '):
  w=f.split(': ',1)[1]
  mp={'How ...?':'How do you get to work?','How ADJ/ADV ...?':'How far is the station?','What ...?':'What do you need?','What N ...?':'What time does it start?','When ...?':'When does it start?','Where ...?':'Where do you live?','Which ...?':'Which do you prefer?','Which N ...?':'Which bus goes downtown?','Who ...?':'Who called you?','Whom ...?':'Whom did you invite?','Whose ...?':'Whose is this?','Whose N ...?':'Whose bag is this?','Why ...?':'Why did you leave?'}
  return mp[w]
 if f.startswith('FUNCTIONAL QUESTION: '):
  pat=f.split(': ',1)[1]
  mp={'Can I ...?':'Can I sit here?','Can you ...?':'Can you help me?','Can\'t you ...?':'Can’t you stay a little longer?','Could I ...?':'Could I borrow your pen?','Could you ...?':'Could you open the window?','Couldn\'t you ...?':'Couldn’t you call them tomorrow?','How about ...?':'How about meeting at six?','May I ...?':'May I come in?','Shall I ...?':'Shall I carry that for you?','Shall we ...?':'Shall we start now?','Should I ...?':'Should I call her?','Why don\'t we ...?':'Why don’t we take the bus?','Why don\'t you ...?':'Why don’t you ask Ana?','Why not ...?':'Why not try again?','Will you ...?':'Will you close the door?','Won\'t you ...?':'Won’t you join us?','Would you ...?':'Would you help me?','Wouldn\'t you ...?':'Wouldn’t you prefer to sit down?'}
  return mp[pat]
 # exact BE questions
 if '...?' in f or f.endswith('?'):
  return f.replace('...','ready').replace('this/that','this').replace('these/those','these').replace('he/she','she')
 # sentence patterns
 if f.startswith('SENTENCE PATTERN: '):
  x=f.split(': ',1)[1]
  if x=='SUBJECT+V': base=('The baby sleeps.','The baby does not sleep.','Does the baby sleep?')
  elif x=='SUBJECT+V+OBJECT': base=('She reads books.','She does not read books.','Does she read books?')
  elif 'BECOME/FEEL/GO/LOOK/SEEM/SOUND' in x: base=('She feels tired.','She does not feel tired.','Does she feel tired?')
  elif '+INDIRECT OBJECT+DIRECT OBJECT' in x: base=('She gave me the book.','She did not give me the book.','Did she give me the book?')
  elif '+DIRECT OBJECT+to+INDIRECT OBJECT' in x: base=('She gave the book to me.','She did not give the book to me.','Did she give the book to me?')
  elif 'MAKE+OBJECT+COMPLEMENT' in x: base=('The news made me happy.','The news did not make me happy.','Did the news make you happy?')
  else: base=('She works here.','She does not work here.','Does she work here?')
  return {'aff':base[0],'neg':base[1],'q':base[2],'nq':negative_question(base[2])}.get(m,base[0])
 # some exact forms
 exact={
  'Am I ...?':'Am I late?','Am I not ...?':'Am I not invited?','Are they ...?':'Are they ready?','Are we ...?':'Are we late?','Are you ...?':'Are you ready?',"Aren't they ...?":'Aren’t they ready?',"Aren't we ...?":'Aren’t we late?',"Aren't you ...?":'Aren’t you ready?','Is he/she ...?':'Is she ready?','Is it ...?':'Is it open?',"Isn't he/she ...?":'Isn’t she ready?',"Isn't it ...?":'Isn’t it open?','Are these/those ...?':'Are these yours?',"Aren't these/those ...?":'Aren’t these yours?','Is this/that ...?':'Is this yours?',"Isn't this/that ...?":'Isn’t this yours?',
 }
 if f in exact:return exact[f]
 # fallback on ordinary expressions
 return None


def main():
    content=root/'app/src/main/assets/content'
    kcs=json.loads((content/'kcs.json').read_text(encoding='utf-8'))
    exercises=json.loads((content/'exercises.json').read_text(encoding='utf-8'))
    lexemes=json.loads((content/'lexemes.json').read_text(encoding='utf-8'))
    targets={x['id']:x for x in kcs if x['id'].startswith('G_CEFRJ')}
    labels={kid:label(item) for kid,item in targets.items()}
    examples={kid:example(item) for kid,item in targets.items()}
    unresolved=[kid for kid,x in examples.items() if not x]
    if unresolved:
        raise SystemExit(f'Unresolved CEFR-J examples: {unresolved}')

    # Learner-facing metadata; original technical notation remains in form for provenance/auditing.
    for item in targets.values():
        learner=labels[item['id']]
        item['name']=learner
        item['meaning']=learner.lower()
        item['use']=f'identificar y producir {learner.lower()} en contexto natural'
        tags=list(dict.fromkeys([*item.get('tags',[]),'learner-facing-v23']))
        item['tags']=tags

    # Deterministic same-level distractor labels.
    level_ids={}
    for kid,item in targets.items(): level_ids.setdefault(item['cefr'],[]).append(kid)
    for ids in level_ids.values(): ids.sort()
    def distractor_for(kid):
        item=targets[kid]; ids=level_ids[item['cefr']]; pos=ids.index(kid)
        for step in range(1,len(ids)):
            other=ids[(pos+step*17)%len(ids)]
            if labels[other].casefold()!=labels[kid].casefold(): return labels[other]
        raise RuntimeError('No distractor')

    changed=0
    for q in exercises:
        kid=next((x for x in q.get('kcIds',[]) if x in targets),None)
        if not kid: continue
        learner=labels[kid]; model=examples[kid]; other=distractor_for(kid)
        q['cefr']=targets[kid]['cefr']
        if q['type']=='TEACH':
            q['promptEs']=f'Escucha el ejemplo y presta atención a este recurso: {learner}.'
            q['stimulusEn']=model
            q['optionA']=q['optionB']=q['correctOption']=q['spellTarget']=''
            q['explanationEs']=f'Recurso: {learner}. Modelo: {model}'
            q['tags']=list(dict.fromkeys([*q.get('tags',[]),'learner-facing-v23']))
        elif q['type']=='AB':
            q['promptEs']='Escucha la frase. ¿Qué recurso gramatical estás oyendo?'
            q['stimulusEn']=model
            flip=int(__import__('hashlib').sha256(kid.encode()).hexdigest()[-1],16)%2==1
            if flip:
                q['optionA'],q['optionB'],q['correctOption']=other,learner,'B'
            else:
                q['optionA'],q['optionB'],q['correctOption']=learner,other,'A'
            q['explanationEs']=f'Recurso correcto: {learner}. Modelo: {model}'
            q['tags']=list(dict.fromkeys([*q.get('tags',[]),'learner-facing-v23','audio-grammar']))
        elif q['type']=='SELF_ASSESS':
            q['promptEs']=f'Produce mentalmente una frase nueva con este recurso: {learner}. No copies el modelo.'
            q['stimulusEn']=model
            q['optionA']=q['optionB']=q['correctOption']=q['spellTarget']=''
            q['explanationEs']='Comprueba que tu frase mantiene el patrón objetivo y expresa una idea completa.'
            q['tags']=list(dict.fromkeys([*q.get('tags',[]),'learner-facing-v23']))
        changed+=1

    # Replace the last generic lexical distractors with plausible same-level alternatives.
    fixes={'V_WOMAN':'hombre','V_MAN':'mujer','V_VALUE':'precio','V_TABLET':'teléfono'}
    lex_by={x['id']:x for x in lexemes}
    for kid,d in fixes.items():
        if kid in lex_by: lex_by[kid]['distractorEs']=d

    for name,obj in [('kcs.json',kcs),('exercises.json',exercises),('lexemes.json',lexemes)]:
        (content/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'Humanized {len(targets)} CEFR-J grammar KCs and {changed} exercises; lexical distractors fixed: {sum(k in lex_by for k in fixes)}')

if __name__=='__main__':
    main()
