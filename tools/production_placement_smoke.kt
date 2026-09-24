import com.siaa.core.algorithm.*
import com.siaa.core.model.*

fun main(){
    val score=ProductionScoring.speakingSimilarity("I need the station", "I need station")
    check(score>0.65) { "speaking scorer too low $score" }
    val w=ProductionScoring.writingScore("I need the station now", listOf("need","station"), 4)
    check(w>0.85) { "writing scorer too low $w" }
    val comps=listOf(
        KnowledgeComponent("A1","basic","A1",KcDomain.GRAMMAR),
        KnowledgeComponent("B1","intermediate","B1",KcDomain.GRAMMAR),
        KnowledgeComponent("L1","listen","A2",KcDomain.LISTENING)
    )
    val ex=listOf(
        ExerciseDefinition("P1",ExerciseType.AB,listOf("A1"),"A1",.2,"",optionA="a",optionB="b",correctOption="A",tags=setOf("placement")),
        ExerciseDefinition("P2",ExerciseType.AB,listOf("B1"),"B1",.6,"",optionA="a",optionB="b",correctOption="A",tags=setOf("placement")),
        ExerciseDefinition("P3",ExerciseType.LISTENING_AB,listOf("L1"),"A2",.4,"",optionA="a",optionB="b",correctOption="A",tags=setOf("placement"))
    )
    val snap=LearningSnapshot(comps, emptyList(), emptyList(), ex)
    val ints=listOf(
        InteractionRecord(sessionId=1,turnId=1,exerciseId="P1",timestampEpochMs=1,response="A",correct=true),
        InteractionRecord(sessionId=1,turnId=2,exerciseId="P2",timestampEpochMs=2,response="B",correct=false),
        InteractionRecord(sessionId=1,turnId=3,exerciseId="P3",timestampEpochMs=3,response="A",correct=true)
    )
    val profile=AdaptivePlacementEngine().estimate(snap,ints)
    check(profile.answeredItems==3)
    check(profile.overallCefr.isNotBlank())
    println("SIAA placement/production smoke OK score=$score writing=$w cefr=${profile.overallCefr}")
}
