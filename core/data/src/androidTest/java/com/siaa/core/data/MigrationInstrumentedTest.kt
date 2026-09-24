package com.siaa.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siaa.core.data.migration.MIGRATION_1_2
import com.siaa.core.data.migration.MIGRATION_2_3
import com.siaa.core.data.migration.MIGRATION_3_4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SiaaDatabase::class.java
    )

    @Test
    fun migrate1To4_preservesDataAndValidatesSchema() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO learner_kc_state (kcId, mastery, recognition, production, orthography, automaticity, halfLifeHours, uncertainty, lastReviewedAtEpochMs, consecutiveSuccess, consecutiveFailure, totalAttempts, totalCorrect) VALUES ('G',0.4,0.4,0.3,0.3,0.2,8,0.4,NULL,1,0,3,2)")
            execSQL("INSERT INTO interactions (id,sessionId,exerciseId,timestampEpochMs,response,correct,confidence,latencyMs,hintDepth,plannerScore,stateBeforeMastery,stateAfterMastery) VALUES (7,2,'E',11,'B',1,NULL,100,0,0.5,0.4,0.5)")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).apply {
            query("SELECT exposureCount, transferSuccesses, novelSuccesses FROM learner_kc_state WHERE kcId='G'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getInt(0) == 0)
                check(cursor.getInt(1) == 0)
                check(cursor.getInt(2) == 0)
            }
            query("SELECT turnId, graded, kind FROM interactions WHERE id=7").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getLong(0) == 7L)
                check(cursor.getInt(1) == 1)
                check(cursor.getString(2) == "GRADED_RESPONSE")
            }
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='skill_evidence'").use { cursor -> check(cursor.moveToFirst()) }
            close()
        }
    }
}
