package com.siaa.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.siaa.app.ui.screens.CurriculumScreen
import com.siaa.app.ui.screens.EarbudCalibrationScreen
import com.siaa.app.ui.screens.ProgressStatsScreen
import com.siaa.app.ui.screens.StudySessionScreen
import com.siaa.app.ui.theme.SiaaDarkBg
import com.siaa.app.ui.theme.SiaaPrimaryCyan
import com.siaa.app.ui.theme.SiaaSurface
import com.siaa.app.ui.theme.SiaaTextSecondary
import com.siaa.app.ui.theme.SiaaTheme
import com.siaa.core.runtime.MediaControlEvent

sealed class Screen(val title: String, val icon: ImageVector, val tag: String) {
    object Study : Screen("Estudio", Icons.Default.PlayArrow, "nav_study")
    object Curriculum : Screen("Currículo", Icons.Default.Info, "nav_curriculum")
    object Earbuds : Screen("Audífonos", Icons.Default.DateRange, "nav_earbuds")
    object Progress : Screen("Progreso", Icons.Default.Star, "nav_progress")
}

class MainActivity : ComponentActivity() {

    private val app by lazy { application as SiaaApplication }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SiaaTheme {
                MainAppContainer(app = app)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val profile = app.userPreferencesRepository.deviceProfile.value
        val mediaEvent = app.earbudCommandRouter.mapKeyCodeToEvent(keyCode, profile)
        if (mediaEvent != null) {
            val handled = app.sessionStateMachine.handleMediaControlEvent(mediaEvent)
            if (handled) {
                if (mediaEvent == MediaControlEvent.PLAY_PAUSE) {
                    val currentEx = app.sessionStateMachine.uiState.value.currentExercise
                    currentEx?.let {
                        app.audioPlayerController.playAssetOrTts(it.audioPath, it.promptText, it.title)
                    }
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun MainAppContainer(app: SiaaApplication) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    val screens = listOf(Screen.Study, Screen.Curriculum, Screen.Earbuds, Screen.Progress)

    // Preload exercises on launch
    LaunchedEffect(Unit) {
        val exercises = app.curriculumRepository.getExercises(level = "A1")
        if (app.sessionStateMachine.uiState.value.currentExercise == null) {
            app.sessionStateMachine.startSession(
                exercises = exercises,
                initialProgress = app.userPreferencesRepository.userProgress.value
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SiaaDarkBg),
        bottomBar = {
            NavigationBar(
                containerColor = SiaaSurface,
                contentColor = SiaaPrimaryCyan
            ) {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedScreenIndex == index,
                        onClick = { selectedScreenIndex = index },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(text = screen.title)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = SiaaPrimaryCyan,
                            indicatorColor = SiaaPrimaryCyan,
                            unselectedIconColor = SiaaTextSecondary,
                            unselectedTextColor = SiaaTextSecondary
                        ),
                        modifier = Modifier.testTag(screen.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreenIndex) {
                0 -> StudySessionScreen(
                    sessionStateMachine = app.sessionStateMachine,
                    audioPlayerController = app.audioPlayerController,
                    onNavigateToCalibration = { selectedScreenIndex = 2 }
                )
                1 -> CurriculumScreen(
                    curriculumRepository = app.curriculumRepository,
                    sessionStateMachine = app.sessionStateMachine,
                    audioPlayerController = app.audioPlayerController,
                    onStartPractice = { selectedScreenIndex = 0 }
                )
                2 -> EarbudCalibrationScreen(
                    userPreferencesRepository = app.userPreferencesRepository,
                    earbudCommandRouter = app.earbudCommandRouter
                )
                3 -> ProgressStatsScreen(
                    userPreferencesRepository = app.userPreferencesRepository
                )
            }
        }
    }
}
