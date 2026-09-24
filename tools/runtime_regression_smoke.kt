import com.siaa.core.algorithm.*
import com.siaa.core.model.*
import com.siaa.core.runtime.*
import kotlinx.coroutines.*

class R(private val exercises: List<ExerciseDefinition>, initial: LearnerKcState? = null): LearningRepository {
    val comps = listOf(KnowledgeComponent("G","G","A1",KcDomain.GRAMMAR, priorMastery=0.2))
    val states = mutableMapOf<String,LearnerKcState>()
    val interactions = mutableListOf<InteractionRecord>()
    val events = mutableListOf<RuntimeEvent>()
    var ended = 0
    var active: SessionRecord? = null
    init { if(initial!=null) states[initial.kcId]=initial }
    override suspend fun loadSnapshot()=LearningSnapshot(comps, emptyList(), states.values.toList(), exercises)
    override suspend fun createSession(mode:SessionMode, nowEpochMs:Long):Long { active=SessionRecord(1L,mode,nowEpochMs); return 1L }
    override suspend fun finishSession(sessionId:Long, nowEpochMs:Long){ ended++; if(active?.id==sessionId) active=active?.copy(endedAtEpochMs=nowEpochMs) }
    override suspend fun activeSession():SessionRecord?=active?.takeIf{it.endedAtEpochMs==null}
    override suspend fun recordInteraction(record:InteractionRecord):Long { interactions+=record; return interactions.size.toLong() }
    override suspend fun updateLearnerState(state:LearnerKcState){states[state.kcId]=state}
    override suspend fun commitTurn(interaction:InteractionRecord,updatedStates:List<LearnerKcState>,misconceptionUpdates:List<Misconception>):Long {
        check(interactions.none{it.sessionId==interaction.sessionId&&it.turnId==interaction.turnId}){"duplicate"}
        updatedStates.forEach{states[it.kcId]=it}; interactions+=interaction; return interactions.size.toLong()
    }
    override suspend fun recordRuntimeEvent(event:RuntimeEvent):Long{events+=event; return events.size.toLong()}
    override suspend fun recentRuntimeEvents(limit:Int)=events.asReversed().take(limit)
    override suspend fun recentInteractions(limit:Int)=interactions.asReversed().take(limit)
    override suspend fun dashboardStats(nowEpochMs:Long)=DashboardStats(1,0,0,0.0,0.0,interactions.size,"A1")
    override suspend fun saveDeviceProfile(profile:DeviceProfile)=1L
    override suspend fun latestDeviceProfile():DeviceProfile?=null
    override suspend fun hasInteraction(sessionId:Long,turnId:Long)=interactions.any{it.sessionId==sessionId&&it.turnId==turnId}
}
class P: ExercisePlanner {
    override fun rank(mode:SessionMode,snapshot:LearningSnapshot,recentInteractions:List<InteractionRecord>,nowEpochMs:Long,limit:Int):List<PlannerCandidate> {
        val seen=recentInteractions.map{it.exerciseId}.toSet(); val e=snapshot.exercises.firstOrNull{it.id !in seen}?:snapshot.exercises.firstOrNull()?:return emptyList()
        return listOf(PlannerCandidate(e,.5,.6,.5,.5,0.0,0.0,"test"))
    }
}
class SlowSpeech(private val ms:Long): SpeechPort { val spoken=mutableListOf<String>(); override suspend fun speak(text:String,languageTag:String,rate:Float){spoken+=text; delay(ms)}; override fun stop(){}; override fun shutdown(){} }
class E: EarconPort { val xs=mutableListOf<EarconKind>(); override fun play(kind:EarconKind){xs+=kind}; override fun release(){} }
suspend fun waitState(rt:LessonRuntime, st:LessonState, max:Long=4000){ val start=System.currentTimeMillis(); while(System.currentTimeMillis()-start<max){ if(rt.snapshot.value.state==st)return; delay(5)}; error("wait $st got ${rt.snapshot.value}") }
fun bin()=ExerciseDefinition("B",ExerciseType.AB,listOf("G"),"A1",.4,"q",optionA="a",optionB="b",correctOption="B")
fun teach()=ExerciseDefinition("T",ExerciseType.TEACH,listOf("G"),"A1",.2,"teach",stimulusEn="hello")
fun main()=runBlocking {
    // STOP during TTS must remain terminal.
    run {
        val r=R(listOf(bin())); val s=SlowSpeech(300); val rt=LessonRuntime(r,P(),StateUpdater(),s,E(),dispatcher=Dispatchers.Default)
        rt.start(SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false)); waitState(rt,LessonState.SPEAKING); rt.stop(); delay(800)
        check(rt.snapshot.value.state==LessonState.SESSION_END){"STOP regression ${rt.snapshot.value}"}; check(r.ended==1)
        println("STOP_OK")
        rt.shutdown()
    }
    // Route loss during TTS must remain PAUSED; resume creates fresh execution.
    run {
        val r=R(listOf(bin())); val rt=LessonRuntime(r,P(),StateUpdater(),SlowSpeech(250),E(),dispatcher=Dispatchers.Default)
        rt.start(SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false)); waitState(rt,LessonState.SPEAKING); rt.pauseForRouteChange(); delay(700)
        check(rt.snapshot.value.state==LessonState.PAUSED){"PAUSE regression ${rt.snapshot.value}"}
        check(rt.onCommand(RuntimeCommand.PLAY)); waitState(rt,LessonState.WAITING_BINARY,5000)
        println("PAUSE_OK")
        rt.stop(); rt.shutdown()
    }
    // Timeout retry must not self-cancel and final timeout must close a turn without review evidence.
    run {
        val initial=LearnerKcState("G",mastery=.33,lastReviewedAtEpochMs=1234L,totalAttempts=2)
        val r=R(listOf(bin()),initial); val e=E(); val rt=LessonRuntime(r,P(),StateUpdater(),SlowSpeech(0),e,dispatcher=Dispatchers.Default)
        rt.start(SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false,policy=SessionPolicy(binaryResponseTimeoutMs=80L,selfAssessmentTimeoutMs=80L,maxTimeoutRetries=1)))
        waitState(rt,LessonState.SESSION_END,5000)
        check(r.interactions.size==1 && r.interactions[0].kind==InteractionKind.TIMEOUT){"timeout interaction ${r.interactions}"}
        val after=r.states["G"]!!; check(after.mastery==initial.mastery); check(after.lastReviewedAtEpochMs==1234L); check(after.totalAttempts==2)
        check(rt.snapshot.value.turnsCompleted==1); check(EarconKind.INCORRECT !in e.xs)
        println("TIMEOUT_OK")
        rt.shutdown()
    }
    // TEACH is exposure only and honors maxItems.
    run {
        val initial=LearnerKcState("G",mastery=.44,recognition=.51,halfLifeHours=20.0,lastReviewedAtEpochMs=999L,totalAttempts=4,exposureCount=3)
        val r=R(listOf(teach()),initial); val rt=LessonRuntime(r,P(),StateUpdater(),SlowSpeech(0),E(),dispatcher=Dispatchers.Default)
        rt.start(SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false)); waitState(rt,LessonState.SESSION_END,3000)
        val after=r.states["G"]!!; check(after.mastery==initial.mastery); check(after.recognition==initial.recognition); check(after.halfLifeHours==initial.halfLifeHours); check(after.lastReviewedAtEpochMs==initial.lastReviewedAtEpochMs); check(after.totalAttempts==initial.totalAttempts); check(after.exposureCount==4)
        check(r.interactions.single().kind==InteractionKind.TEACH_EXPOSURE && !r.interactions.single().correct)
        check(rt.snapshot.value.turnsCompleted==1)
        println("TEACH_OK")
        rt.shutdown()
    }
    // HELP and answer compete for the same turn gate.
    run {
        val r=R(listOf(bin())); val rt=LessonRuntime(r,P(),StateUpdater(),SlowSpeech(0),E(),dispatcher=Dispatchers.Default)
        rt.start(SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false)); waitState(rt,LessonState.WAITING_BINARY)
        check(rt.onCommand(RuntimeCommand.BACK)); check(rt.onCommand(RuntimeCommand.SECONDARY)); delay(20)
        check(r.interactions.isEmpty()) { "answer committed while help owned the turn" }
        println("TURN_GATE_OK")
        rt.stop(); rt.shutdown()
    }
    // A process recreation restores an unfinished selected turn instead of duplicating it.
    run {
        val r=R(listOf(bin())); val first=LessonRuntime(r,P(),StateUpdater(),SlowSpeech(0),E(),dispatcher=Dispatchers.Default)
        first.start(SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false)); waitState(first,LessonState.WAITING_BINARY)
        val turn=first.snapshot.value.turnId; first.shutdown(); delay(20)
        val second=LessonRuntime(r,P(),StateUpdater(),SlowSpeech(0),E(),dispatcher=Dispatchers.Default)
        second.start(SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false)); waitState(second,LessonState.WAITING_BINARY)
        check(second.snapshot.value.turnId==turn) { "restored turn changed: ${second.snapshot.value}" }
        check(r.interactions.isEmpty())
        println("RESTORE_OK")
        second.stop(); second.shutdown()
    }
    // Natural finish stays FINISHING while the final utterance is active; route loss stops it and ends safely.
    run {
        class FinalSpeech: SpeechPort {
            val spoken=mutableListOf<String>(); var stops=0
            override suspend fun speak(text:String, languageTag:String, rate:Float){ spoken+=text; if(text.contains("Objetivo")) delay(350) }
            override fun stop(){ stops++ }
            override fun shutdown(){}
        }
        val r=R(listOf(bin())); val sp=FinalSpeech(); val rt=LessonRuntime(r,P(),StateUpdater(),sp,E(),dispatcher=Dispatchers.Default)
        rt.start(SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false)); waitState(rt,LessonState.WAITING_BINARY)
        check(rt.onCommand(RuntimeCommand.SECONDARY)); waitState(rt,LessonState.FINISHING,3000)
        val before=sp.stops; rt.pauseForRouteChange(); delay(20)
        check(sp.stops>before){"route loss did not stop final speech"}
        waitState(rt,LessonState.SESSION_END,2000)
        println("FINISHING_ROUTE_OK")
        rt.shutdown()
    }

    // Process death exactly in PLANNING_NEXT must re-plan, not reuse the previous exercise id from a state event.
    run {
        val e1=ExerciseDefinition("E1",ExerciseType.AB,listOf("G"),"A1",.4,"q1",optionA="a",optionB="b",correctOption="B")
        val e2=ExerciseDefinition("E2",ExerciseType.AB,listOf("G"),"A1",.4,"q2",optionA="a",optionB="b",correctOption="B")
        val r=R(listOf(e1,e2)); r.active=SessionRecord(1L,SessionMode.GRAMMAR,System.currentTimeMillis())
        r.interactions += InteractionRecord(sessionId=1L,turnId=1L,exerciseId="E1",timestampEpochMs=1L,response="B",correct=true,graded=true)
        r.events += RuntimeEvent(sessionId=1L,turnId=2L,timestampEpochMs=2L,eventType=RuntimeEventType.STATE_TRANSITION,stateBefore="FEEDBACK",stateAfter="PLANNING_NEXT",exerciseId="E1")
        val rt=LessonRuntime(r,P(),StateUpdater(),SlowSpeech(0),E(),dispatcher=Dispatchers.Default)
        rt.start(SessionConfig(SessionMode.GRAMMAR,maxItems=3,announceControls=false)); waitState(rt,LessonState.WAITING_BINARY,3000)
        check(rt.snapshot.value.turnId==2L){"wrong restored turn ${rt.snapshot.value}"}
        check(rt.snapshot.value.currentExerciseId=="E2"){"stale exercise restored ${rt.snapshot.value}"}
        println("RESTORE_PLANNING_OK")
        rt.stop(); rt.shutdown()
    }

}
