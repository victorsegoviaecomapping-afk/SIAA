import com.siaa.core.algorithm.*
import com.siaa.core.model.*
import com.siaa.core.runtime.*
import kotlinx.coroutines.*

class SpellRepo : LearningRepository {
    val comps = listOf(KnowledgeComponent("V_ENOUGH", "enough", "A2", KcDomain.VOCABULARY, priorMastery = 0.2))
    val ex = ExerciseDefinition(
        id="S1", type=ExerciseType.SPELLING_AB, kcIds=listOf("V_ENOUGH"), cefr="A2", difficulty=.5,
        promptEs="¿Cuál deletreo corresponde a la palabra?", stimulusEn="enough",
        optionA="enough", optionB="enogh", correctOption="A", explanationEs="Forma correcta", spellTarget="enough"
    )
    val interactions=mutableListOf<InteractionRecord>(); val events=mutableListOf<RuntimeEvent>(); var state:LearnerKcState?=null
    override suspend fun loadSnapshot()=LearningSnapshot(comps, emptyList(), listOfNotNull(state), listOf(ex))
    override suspend fun createSession(mode:SessionMode, nowEpochMs:Long)=1L
    override suspend fun finishSession(sessionId:Long, nowEpochMs:Long){}
    override suspend fun recordInteraction(record:InteractionRecord):Long{interactions+=record;return interactions.size.toLong()}
    override suspend fun updateLearnerState(state:LearnerKcState){this.state=state}
    override suspend fun commitTurn(interaction:InteractionRecord,updatedStates:List<LearnerKcState>,misconceptionUpdates:List<Misconception>):Long{state=updatedStates.firstOrNull();interactions+=interaction;return interactions.size.toLong()}
    override suspend fun recordRuntimeEvent(event:RuntimeEvent):Long{events+=event;return events.size.toLong()}
    override suspend fun recentRuntimeEvents(limit:Int)=events.asReversed().take(limit)
    override suspend fun recentInteractions(limit:Int)=interactions.asReversed().take(limit)
    override suspend fun dashboardStats(nowEpochMs:Long)=DashboardStats(1,0,0,0.0,0.0,interactions.size,"A2")
    override suspend fun saveDeviceProfile(profile:DeviceProfile)=1L
    override suspend fun latestDeviceProfile():DeviceProfile?=null
}
class CaptureSpeech: SpeechPort {
    val spoken=mutableListOf<Pair<String,String>>()
    override suspend fun speak(text:String,languageTag:String,rate:Float){spoken += languageTag to text}
    override fun stop(){}
    override fun shutdown(){}
}
class QuietEar: EarconPort { override fun play(kind:EarconKind){}; override fun release(){} }
class SpellPlanner: ExercisePlanner {
    override fun rank(mode:SessionMode,snapshot:LearningSnapshot,recentInteractions:List<InteractionRecord>,nowEpochMs:Long,limit:Int):List<PlannerCandidate> =
        if(recentInteractions.isEmpty()) listOf(PlannerCandidate(snapshot.exercises.first(),.6,.7,.4,.4,0.0,0.0,"spell-smoke")) else emptyList()
}
suspend fun waitSpell(r:LessonRuntime,state:LessonState){repeat(350){if(r.snapshot.value.state==state)return;delay(10)};error("timeout $state ${r.snapshot.value}")}
fun main()=runBlocking{
    val repo=SpellRepo(); val speech=CaptureSpeech(); val runtime=LessonRuntime(repo,SpellPlanner(),StateUpdater(),speech,QuietEar(),dispatcher=Dispatchers.Default)
    runtime.start(SessionConfig(SessionMode.SPELLING,maxItems=1,announceControls=false))
    waitSpell(runtime,LessonState.WAITING_BINARY)
    val all=speech.spoken.joinToString(" | "){"${it.first}:${it.second}"}.lowercase()
    check("ee, en, oh, you, gee, aitch" in all) { all }
    check("ee, en, oh, gee, aitch" in all) { all }
    check(repo.events.any{it.eventType==RuntimeEventType.PREDICTION_RECORDED && it.payload?.contains("successProbability=")==true})
    check(runtime.onCommand(RuntimeCommand.PRIMARY))
    waitSpell(runtime,LessonState.SESSION_END)
    val finalSpeech=speech.spoken.joinToString(" | "){it.second}.lowercase()
    check("ee, en, oh, you, gee, aitch" in finalSpeech)
    println("SIAA spelling runtime smoke OK")
    runtime.shutdown()
}
