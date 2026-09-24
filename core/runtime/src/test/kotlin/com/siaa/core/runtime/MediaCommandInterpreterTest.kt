package com.siaa.core.runtime

import com.siaa.core.model.DeviceProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaCommandInterpreterTest {
    private val interpreter = MediaCommandInterpreter()

    @Test
    fun waitingBinaryMapsEventsCorrectly() {
        val state = LessonState.WAITING_BINARY
        assertEquals(RuntimeCommand.PRIMARY, interpreter.interpret(MediaControlEvent.PLAY_PAUSE, state))
        assertEquals(RuntimeCommand.PRIMARY, interpreter.interpret(MediaControlEvent.PLAY, state))
        assertEquals(RuntimeCommand.SECONDARY, interpreter.interpret(MediaControlEvent.NEXT, state))
        assertEquals(RuntimeCommand.BACK, interpreter.interpret(MediaControlEvent.PREVIOUS, state))
        assertEquals(RuntimeCommand.PAUSE, interpreter.interpret(MediaControlEvent.PAUSE, state))
        assertEquals(RuntimeCommand.STOP, interpreter.interpret(MediaControlEvent.STOP, state))
    }

    @Test
    fun waitingSelfAssessmentMapsEventsCorrectly() {
        val state = LessonState.WAITING_SELF_ASSESSMENT
        assertEquals(RuntimeCommand.PRIMARY, interpreter.interpret(MediaControlEvent.PLAY_PAUSE, state))
        assertEquals(RuntimeCommand.PRIMARY, interpreter.interpret(MediaControlEvent.PLAY, state))
        assertEquals(RuntimeCommand.SECONDARY, interpreter.interpret(MediaControlEvent.NEXT, state))
        assertEquals(RuntimeCommand.BACK, interpreter.interpret(MediaControlEvent.PREVIOUS, state))
        assertEquals(RuntimeCommand.PAUSE, interpreter.interpret(MediaControlEvent.PAUSE, state))
        assertEquals(RuntimeCommand.STOP, interpreter.interpret(MediaControlEvent.STOP, state))
    }

    @Test
    fun speakingMapsEventsCorrectly() {
        val state = LessonState.SPEAKING
        assertEquals(RuntimeCommand.PAUSE, interpreter.interpret(MediaControlEvent.PLAY_PAUSE, state))
        assertEquals(RuntimeCommand.PAUSE, interpreter.interpret(MediaControlEvent.PAUSE, state))
        assertEquals(RuntimeCommand.PAUSE, interpreter.interpret(MediaControlEvent.PLAY, state))
        assertNull(interpreter.interpret(MediaControlEvent.PREVIOUS, state))
        assertEquals(RuntimeCommand.STOP, interpreter.interpret(MediaControlEvent.STOP, state))
        assertNull(interpreter.interpret(MediaControlEvent.NEXT, state))
    }

    @Test
    fun pausedMapsEventsCorrectly() {
        val state = LessonState.PAUSED
        assertEquals(RuntimeCommand.PLAY, interpreter.interpret(MediaControlEvent.PLAY, state))
        assertEquals(RuntimeCommand.PLAY, interpreter.interpret(MediaControlEvent.PLAY_PAUSE, state))
        assertEquals(RuntimeCommand.STOP, interpreter.interpret(MediaControlEvent.STOP, state))
        assertNull(interpreter.interpret(MediaControlEvent.NEXT, state))
        assertNull(interpreter.interpret(MediaControlEvent.PREVIOUS, state))
    }

    @Test
    fun evaluatingAndPlanningOnlyAllowStop() {
        for (state in listOf(LessonState.EVALUATING, LessonState.PLANNING_NEXT)) {
            assertNull(interpreter.interpret(MediaControlEvent.PLAY_PAUSE, state))
            assertNull(interpreter.interpret(MediaControlEvent.PAUSE, state))
            assertNull(interpreter.interpret(MediaControlEvent.PREVIOUS, state))
            assertEquals(RuntimeCommand.STOP, interpreter.interpret(MediaControlEvent.STOP, state))
        }
    }

    @Test
    fun calibratedProfileMapsCustomKeyCodes() {
        val customProfile = DeviceProfile(
            name = "Custom Earbuds",
            primaryKeyCode = 1001,
            secondaryKeyCode = 1002,
            backKeyCode = 1003,
            stopKeyCode = 1004
        )

        val state = LessonState.WAITING_BINARY
        assertEquals(
            RuntimeCommand.PRIMARY,
            interpreter.interpretKeyCode(1001, state, customProfile)
        )
        assertEquals(
            RuntimeCommand.SECONDARY,
            interpreter.interpretKeyCode(1002, state, customProfile)
        )
        assertEquals(
            RuntimeCommand.BACK,
            interpreter.interpretKeyCode(1003, state, customProfile)
        )
        assertEquals(
            RuntimeCommand.STOP,
            interpreter.interpretKeyCode(1004, state, customProfile)
        )
        assertNull(
            interpreter.interpretKeyCode(9999, state, customProfile)
        )
    }
}
