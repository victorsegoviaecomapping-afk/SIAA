package com.siaa.core.runtime

import com.siaa.core.model.DeviceProfile

class MediaCommandInterpreter(
    private val calibratedMapper: CalibratedMediaMapper = CalibratedMediaMapper()
) {
    fun interpretKeyCode(
        keyCode: Int,
        state: LessonState,
        profile: DeviceProfile? = null
    ): RuntimeCommand? {
        val event = calibratedMapper.map(keyCode, profile) ?: return null
        return interpret(event, state)
    }

    fun interpret(event: MediaControlEvent, state: LessonState): RuntimeCommand? {
        if (event == MediaControlEvent.STOP) return RuntimeCommand.STOP

        return when (state) {
            LessonState.WAITING_BINARY -> when (event) {
                MediaControlEvent.PLAY_PAUSE, MediaControlEvent.PLAY -> RuntimeCommand.PRIMARY
                MediaControlEvent.NEXT -> RuntimeCommand.SECONDARY
                MediaControlEvent.PREVIOUS -> RuntimeCommand.BACK
                MediaControlEvent.PAUSE -> RuntimeCommand.PAUSE
                MediaControlEvent.STOP -> RuntimeCommand.STOP
            }
            LessonState.WAITING_SELF_ASSESSMENT -> when (event) {
                MediaControlEvent.PLAY_PAUSE, MediaControlEvent.PLAY -> RuntimeCommand.PRIMARY
                MediaControlEvent.NEXT -> RuntimeCommand.SECONDARY
                MediaControlEvent.PREVIOUS -> RuntimeCommand.BACK
                MediaControlEvent.PAUSE -> RuntimeCommand.PAUSE
                MediaControlEvent.STOP -> RuntimeCommand.STOP
            }
            LessonState.SPEAKING, LessonState.HELPING, LessonState.FEEDBACK, LessonState.PREPARING -> when (event) {
                MediaControlEvent.PLAY_PAUSE, MediaControlEvent.PAUSE, MediaControlEvent.PLAY -> RuntimeCommand.PAUSE
                // Help/repeat is only accepted while explicitly waiting for a binary response.
                MediaControlEvent.NEXT, MediaControlEvent.PREVIOUS -> null
                MediaControlEvent.STOP -> RuntimeCommand.STOP
            }
            LessonState.PAUSED -> when (event) {
                MediaControlEvent.PLAY, MediaControlEvent.PLAY_PAUSE -> RuntimeCommand.PLAY
                MediaControlEvent.STOP -> RuntimeCommand.STOP
                else -> null
            }
            LessonState.EVALUATING, LessonState.PLANNING_NEXT, LessonState.FINISHING,
            LessonState.IDLE, LessonState.SESSION_END, LessonState.ERROR -> when (event) {
                MediaControlEvent.STOP -> RuntimeCommand.STOP
                else -> null
            }
        }
    }
}
