package android.view

class KeyEvent(val action: Int = ACTION_DOWN, val keyCode: Int = 0, val repeatCount: Int = 0) {
    companion object {
        fun keyCodeFromString(s: String): Int = s.hashCode() and 0xFFFF
        /** The real name for the constants below (the rebind shots show them), else the number. */
        fun keyCodeToString(c: Int): String = "KEYCODE_" + (NAMES[c] ?: c.toString())
        /** KeyEvent.KEYCODE_<name>'s value, for java.lang.Class.getField (the app looks key codes up by name). */
        fun fieldValue(field: String): Int? = NAMES.entries.firstOrNull { "KEYCODE_" + it.value == field }?.key
        private val NAMES: Map<Int, String> by lazy {
            mapOf<Int, String>(19 to "DPAD_UP", 20 to "DPAD_DOWN", 21 to "DPAD_LEFT", 22 to "DPAD_RIGHT", 23 to "DPAD_CENTER", 66 to "ENTER",
                111 to "ESCAPE", 96 to "BUTTON_A", 97 to "BUTTON_B", 99 to "BUTTON_X", 100 to "BUTTON_Y", 102 to "BUTTON_L1",
                103 to "BUTTON_R1", 108 to "BUTTON_START", 109 to "BUTTON_SELECT", 4 to "BACK", 104 to "BUTTON_L2", 105 to "BUTTON_R2",
                3 to "HOME", 24 to "VOLUME_UP", 25 to "VOLUME_DOWN", 26 to "POWER", 164 to "VOLUME_MUTE", 187 to "APP_SWITCH",
                106 to "BUTTON_THUMBL", 107 to "BUTTON_THUMBR", 110 to "BUTTON_MODE", 98 to "BUTTON_C", 101 to "BUTTON_Z",
                268 to "DPAD_UP_LEFT", 269 to "DPAD_DOWN_LEFT", 270 to "DPAD_UP_RIGHT", 271 to "DPAD_DOWN_RIGHT", 82 to "MENU",
                62 to "SPACE", 67 to "DEL", 61 to "TAB", 59 to "SHIFT_LEFT", 60 to "SHIFT_RIGHT", 113 to "CTRL_LEFT", 114 to "CTRL_RIGHT",
                57 to "ALT_LEFT", 58 to "ALT_RIGHT", 112 to "FORWARD_DEL", 92 to "PAGE_UP", 93 to "PAGE_DOWN", 122 to "MOVE_HOME", 123 to "MOVE_END") +
                ('A'..'Z').mapIndexed { i, c -> (29 + i) to c.toString() } + ('0'..'9').mapIndexed { i, c -> (7 + i) to c.toString() } +
                (1..12).map { (130 + it) to "F$it" } + (1..16).map { (187 + it) to "BUTTON_$it" }
        }
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val KEYCODE_DPAD_UP = 19
        const val KEYCODE_DPAD_DOWN = 20
        const val KEYCODE_DPAD_LEFT = 21
        const val KEYCODE_DPAD_RIGHT = 22
        const val KEYCODE_DPAD_CENTER = 23
        const val KEYCODE_ENTER = 66
        const val KEYCODE_ESCAPE = 111
        const val KEYCODE_BUTTON_A = 96
        const val KEYCODE_BUTTON_B = 97
        const val KEYCODE_BUTTON_X = 99
        const val KEYCODE_BUTTON_Y = 100
        const val KEYCODE_BUTTON_L1 = 102
        const val KEYCODE_BUTTON_R1 = 103
        const val KEYCODE_BUTTON_START = 108
        const val KEYCODE_BUTTON_SELECT = 109
        const val KEYCODE_BACK = 4
        const val KEYCODE_BUTTON_L2 = 104
        const val KEYCODE_BUTTON_R2 = 105
        const val KEYCODE_HOME = 3
        const val KEYCODE_VOLUME_UP = 24
        const val KEYCODE_VOLUME_DOWN = 25
        const val KEYCODE_POWER = 26
        const val KEYCODE_VOLUME_MUTE = 164
        const val KEYCODE_APP_SWITCH = 187
        const val KEYCODE_DPAD_UP_LEFT = 268
        const val KEYCODE_DPAD_DOWN_LEFT = 269
        const val KEYCODE_DPAD_UP_RIGHT = 270
        const val KEYCODE_DPAD_DOWN_RIGHT = 271
    }
}

/** Joystick motion (Settings' analog L2 / R2 capture): never sent in the preview. */
class MotionEvent {
    fun isFromSource(source: Int): Boolean = false
    fun getAxisValue(axis: Int): Float = 0f
    companion object {
        const val AXIS_LTRIGGER = 17
        const val AXIS_RTRIGGER = 18
        const val AXIS_THROTTLE = 19
        const val AXIS_GAS = 22
        const val AXIS_BRAKE = 23
    }
}

class InputDevice {
    companion object {
        const val SOURCE_JOYSTICK = 0x01000010
    }
}
