package com.siaa.app

import android.app.Application
import com.siaa.app.media.AudioPlayerController
import com.siaa.app.media.EarbudCommandRouter
import com.siaa.core.data.CurriculumRepository
import com.siaa.core.data.UserPreferencesRepository
import com.siaa.core.runtime.SessionStateMachine

class SiaaApplication : Application() {

    lateinit var curriculumRepository: CurriculumRepository
        private set
    lateinit var audioPlayerController: AudioPlayerController
        private set
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set
    lateinit var sessionStateMachine: SessionStateMachine
        private set
    lateinit var earbudCommandRouter: EarbudCommandRouter
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        curriculumRepository = CurriculumRepository(this)
        audioPlayerController = AudioPlayerController(this)
        userPreferencesRepository = UserPreferencesRepository(this)
        sessionStateMachine = SessionStateMachine()
        earbudCommandRouter = EarbudCommandRouter()
    }

    override fun onTerminate() {
        super.onTerminate()
        audioPlayerController.release()
    }

    companion object {
        lateinit var instance: SiaaApplication
            private set
    }
}
