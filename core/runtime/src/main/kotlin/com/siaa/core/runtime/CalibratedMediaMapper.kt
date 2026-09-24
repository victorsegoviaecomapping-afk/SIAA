package com.siaa.core.runtime

import com.siaa.core.model.DeviceProfile

class CalibratedMediaMapper {
    fun map(keyCode: Int, profile: DeviceProfile?): MediaControlEvent? {
        if (profile != null) {
            when (keyCode) {
                profile.primaryKeyCode -> return MediaControlEvent.PLAY_PAUSE
                profile.secondaryKeyCode -> return MediaControlEvent.NEXT
                profile.backKeyCode -> return MediaControlEvent.PREVIOUS
                profile.stopKeyCode -> return MediaControlEvent.STOP
            }
        }
        return null
    }
}
