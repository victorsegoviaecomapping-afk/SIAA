package com.siaa.core.data

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import com.siaa.core.model.DeviceProfile
import com.siaa.core.model.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("siaa_user_prefs", Context.MODE_PRIVATE)

    private val _deviceProfile = MutableStateFlow(loadDeviceProfile())
    val deviceProfile: StateFlow<DeviceProfile> = _deviceProfile.asStateFlow()

    private val _userProgress = MutableStateFlow(loadUserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress.asStateFlow()

    private val _selectedLevel = MutableStateFlow(prefs.getString("selected_level", "A1") ?: "A1")
    val selectedLevel: StateFlow<String> = _selectedLevel.asStateFlow()

    fun saveDeviceProfile(profile: DeviceProfile) {
        _deviceProfile.value = profile
        prefs.edit()
            .putLong("profile_id", profile.id)
            .putString("profile_name", profile.name)
            .putInt("primary_key", profile.primaryKeyCode)
            .putInt("secondary_key", profile.secondaryKeyCode)
            .putInt("back_key", profile.backKeyCode)
            .putInt("stop_key", profile.stopKeyCode)
            .apply()
    }

    private fun loadDeviceProfile(): DeviceProfile {
        return DeviceProfile(
            id = prefs.getLong("profile_id", 1L),
            name = prefs.getString("profile_name", "Audífonos Predeterminados") ?: "Audífonos Predeterminados",
            primaryKeyCode = prefs.getInt("primary_key", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
            secondaryKeyCode = prefs.getInt("secondary_key", KeyEvent.KEYCODE_MEDIA_NEXT),
            backKeyCode = prefs.getInt("back_key", KeyEvent.KEYCODE_MEDIA_PREVIOUS),
            stopKeyCode = prefs.getInt("stop_key", KeyEvent.KEYCODE_MEDIA_STOP)
        )
    }

    fun setSelectedLevel(level: String) {
        _selectedLevel.value = level
        prefs.edit().putString("selected_level", level).apply()
    }

    fun updateUserProgress(progress: UserProgress) {
        _userProgress.value = progress
        prefs.edit()
            .putInt("total_attempts", progress.totalAttempts)
            .putInt("correct_attempts", progress.correctAttempts)
            .putInt("streak_days", progress.currentStreakDays)
            .apply()
    }

    private fun loadUserProgress(): UserProgress {
        return UserProgress(
            totalAttempts = prefs.getInt("total_attempts", 42),
            correctAttempts = prefs.getInt("correct_attempts", 35),
            currentStreakDays = prefs.getInt("streak_days", 4),
            minutesStudiedToday = 22,
            dailyGoalMinutes = 20,
            selectedLevel = prefs.getString("selected_level", "A1") ?: "A1"
        )
    }
}
