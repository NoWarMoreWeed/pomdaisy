package android.view

class KeyEvent(val action: Int = ACTION_DOWN, val keyCode: Int = 0, val repeatCount: Int = 0) {
    constructor(downTime: Long, eventTime: Long, action: Int, code: Int, repeat: Int) : this(action, code, repeat)
    constructor(downTime: Long, eventTime: Long, action: Int, code: Int, repeat: Int, meta: Int) : this(action, code, repeat)
    constructor(downTime: Long, eventTime: Long, action: Int, code: Int, repeat: Int, meta: Int, device: Int, scancode: Int) : this(action, code, repeat)
    constructor(downTime: Long, eventTime: Long, action: Int, code: Int, repeat: Int, meta: Int, device: Int, scancode: Int, flags: Int) : this(action, code, repeat)
    constructor(downTime: Long, eventTime: Long, action: Int, code: Int, repeat: Int, meta: Int, device: Int, scancode: Int, flags: Int, source: Int) : this(action, code, repeat) { this.source = source }
    var source: Int = InputDevice.SOURCE_KEYBOARD
    val isCanceled: Boolean get() = false
    val isLongPress: Boolean get() = false
    val isSystem: Boolean get() = keyCode == KEYCODE_HOME || keyCode == KEYCODE_BACK || keyCode == KEYCODE_VOLUME_UP || keyCode == KEYCODE_VOLUME_DOWN
    val deviceId: Int get() = 0
    val device: InputDevice? get() = null
    val metaState = 0
    val scanCode = 0
    val flags = 0
    val unicodeChar: Int get() = 0
    val eventTime: Long get() = android.os.SystemClock.uptimeMillis()
    val downTime: Long get() = eventTime
    fun isFromSource(s: Int) = (source and s) == s
    fun isShiftPressed() = false
    fun isCtrlPressed() = false
    fun isAltPressed() = false
    fun getUnicodeChar(meta: Int) = 0
    fun startTracking() {}
    val isTracking: Boolean get() = false
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
        const val KEYCODE_BUTTON_MODE = 110
        const val KEYCODE_BUTTON_THUMBL = 106
        const val KEYCODE_BUTTON_THUMBR = 107
        const val KEYCODE_BUTTON_C = 98
        const val KEYCODE_BUTTON_Z = 101
        const val KEYCODE_MENU = 82
        const val KEYCODE_SPACE = 62
        const val KEYCODE_DEL = 67
        const val KEYCODE_TAB = 61
        const val KEYCODE_UNKNOWN = 0
        const val KEYCODE_SEARCH = 84
        const val KEYCODE_F1 = 131
        const val KEYCODE_A = 29; const val KEYCODE_B = 30; const val KEYCODE_C = 31; const val KEYCODE_D = 32; const val KEYCODE_E = 33
        const val KEYCODE_F = 34; const val KEYCODE_G = 35; const val KEYCODE_H = 36; const val KEYCODE_I = 37; const val KEYCODE_J = 38
        const val KEYCODE_K = 39; const val KEYCODE_L = 40; const val KEYCODE_M = 41; const val KEYCODE_N = 42; const val KEYCODE_O = 43
        const val KEYCODE_P = 44; const val KEYCODE_Q = 45; const val KEYCODE_R = 46; const val KEYCODE_S = 47; const val KEYCODE_T = 48
        const val KEYCODE_U = 49; const val KEYCODE_V = 50; const val KEYCODE_W = 51; const val KEYCODE_X = 52; const val KEYCODE_Y = 53
        const val KEYCODE_Z = 54; const val KEYCODE_COMMA = 55; const val KEYCODE_PERIOD = 56; const val KEYCODE_ALT_LEFT = 57
        const val KEYCODE_ALT_RIGHT = 58; const val KEYCODE_SHIFT_LEFT = 59; const val KEYCODE_SHIFT_RIGHT = 60; const val KEYCODE_GRAVE = 68
        const val KEYCODE_MINUS = 69; const val KEYCODE_EQUALS = 70; const val KEYCODE_LEFT_BRACKET = 71; const val KEYCODE_RIGHT_BRACKET = 72
        const val KEYCODE_BACKSLASH = 73; const val KEYCODE_SEMICOLON = 74; const val KEYCODE_APOSTROPHE = 75; const val KEYCODE_SLASH = 76
        const val KEYCODE_CTRL_LEFT = 113; const val KEYCODE_CTRL_RIGHT = 114; const val KEYCODE_FORWARD_DEL = 112; const val KEYCODE_PAGE_UP = 92
        const val KEYCODE_PAGE_DOWN = 93; const val KEYCODE_MOVE_HOME = 122; const val KEYCODE_MOVE_END = 123; const val KEYCODE_INSERT = 124
        const val KEYCODE_1 = 8; const val KEYCODE_2 = 9; const val KEYCODE_3 = 10; const val KEYCODE_4 = 11; const val KEYCODE_5 = 12
        const val KEYCODE_6 = 13; const val KEYCODE_7 = 14; const val KEYCODE_8 = 15; const val KEYCODE_F2 = 132; const val KEYCODE_F3 = 133
        const val KEYCODE_F4 = 134; const val KEYCODE_F5 = 135; const val KEYCODE_F6 = 136; const val KEYCODE_F7 = 137; const val KEYCODE_F8 = 138
        const val KEYCODE_F9 = 139; const val KEYCODE_F10 = 140; const val KEYCODE_F11 = 141; const val KEYCODE_F12 = 142
        const val KEYCODE_NUMPAD_0 = 144; const val KEYCODE_NUMPAD_ENTER = 160; const val KEYCODE_CAPS_LOCK = 115
        const val KEYCODE_0 = 7
        const val KEYCODE_9 = 16
        const val KEYCODE_MEDIA_PLAY_PAUSE = 85
        const val KEYCODE_BUTTON_1 = 188
        const val KEYCODE_BUTTON_16 = 203
        const val FLAG_CANCELED = 32
        const val FLAG_LONG_PRESS = 128
        const val META_SHIFT_ON = 1
        const val META_CTRL_ON = 4096
        const val META_ALT_ON = 2
        const val ACTION_MULTIPLE = 2
        fun getMaxKeyCode() = 300
        fun isGamepadButton(c: Int) = c in 96..110 || c in 188..203
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

/** MotionEvent: touches (ViewHost turns Compose's pointers into these) and, never sent on iOS yet, joystick motion. */
class MotionEvent private constructor(
    val downTime: Long, val eventTime: Long, val action: Int,
    private val ids: IntArray, private val xs: FloatArray, private val ys: FloatArray,
    private val dx: Float = 0f, private val dy: Float = 0f,
) {
    var source: Int = InputDevice.SOURCE_TOUCHSCREEN
    val deviceId: Int get() = 0
    val device: InputDevice? get() = null
    val metaState = 0
    val buttonState = 0
    val flags = 0
    val edgeFlags = 0
    val actionMasked: Int get() = action and ACTION_MASK
    val actionIndex: Int get() = (action and ACTION_POINTER_INDEX_MASK) shr ACTION_POINTER_INDEX_SHIFT
    val pointerCount: Int get() = ids.size
    val x: Float get() = getX(0)
    val y: Float get() = getY(0)
    val rawX: Float get() = xs.getOrElse(0) { 0f } - dx
    val rawY: Float get() = ys.getOrElse(0) { 0f } - dy
    val historySize = 0
    val pressure = 1f
    val size = 1f
    fun getX(i: Int) = xs.getOrElse(i) { 0f }
    fun getY(i: Int) = ys.getOrElse(i) { 0f }
    fun getRawX(i: Int) = getX(i) - dx
    fun getRawY(i: Int) = getY(i) - dy
    fun getPointerId(i: Int) = ids.getOrElse(i) { 0 }
    fun findPointerIndex(id: Int) = ids.indexOf(id)
    fun getToolType(i: Int) = TOOL_TYPE_FINGER
    fun getPressure(i: Int = 0) = 1f
    fun getHistoricalX(i: Int, h: Int) = getX(i)
    fun getHistoricalY(i: Int, h: Int) = getY(i)
    fun isFromSource(s: Int): Boolean = (source and s) == s
    fun getAxisValue(axis: Int): Float = when (axis) { AXIS_X -> x; AXIS_Y -> y; else -> 0f }
    fun getAxisValue(axis: Int, pointer: Int): Float = when (axis) { AXIS_X -> getX(pointer); AXIS_Y -> getY(pointer); else -> 0f }
    fun recycle() {}
    fun setLocation(x: Float, y: Float) { if (xs.isNotEmpty()) { xs[0] = x; ys[0] = y } }
    fun offsetLocation(ox: Float, oy: Float) { for (i in xs.indices) { xs[i] += ox; ys[i] += oy } }
    fun setAction(a: Int): MotionEvent = MotionEvent(downTime, eventTime, a, ids, xs, ys, dx, dy).also { it.source = source }
    /** This event moved by (ox, oy), for a child's coordinates (ViewGroup's dispatch). */
    fun offset(ox: Float, oy: Float) = MotionEvent(downTime, eventTime, action, ids, FloatArray(xs.size) { xs[it] + ox }, FloatArray(ys.size) { ys[it] + oy }, dx + ox, dy + oy).also { it.source = source }
    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        const val ACTION_CANCEL = 3
        const val ACTION_OUTSIDE = 4
        const val ACTION_POINTER_DOWN = 5
        const val ACTION_POINTER_UP = 6
        const val ACTION_HOVER_MOVE = 7
        const val ACTION_SCROLL = 8
        const val ACTION_HOVER_ENTER = 9
        const val ACTION_HOVER_EXIT = 10
        const val ACTION_MASK = 0xFF
        const val ACTION_POINTER_INDEX_MASK = 0xFF00
        const val ACTION_POINTER_INDEX_SHIFT = 8
        const val AXIS_X = 0
        const val AXIS_Y = 1
        const val AXIS_Z = 11
        const val AXIS_RZ = 14
        const val AXIS_HAT_X = 15
        const val AXIS_HAT_Y = 16
        const val AXIS_LTRIGGER = 17
        const val AXIS_RTRIGGER = 18
        const val AXIS_THROTTLE = 19
        const val AXIS_RX = 12
        const val AXIS_RY = 13
        const val AXIS_GAS = 22
        const val AXIS_BRAKE = 23
        const val AXIS_VSCROLL = 9
        const val AXIS_HSCROLL = 10
        const val TOOL_TYPE_FINGER = 1
        const val TOOL_TYPE_MOUSE = 3
        const val TOOL_TYPE_STYLUS = 2
        const val BUTTON_PRIMARY = 1
        fun obtain(downTime: Long, eventTime: Long, action: Int, x: Float, y: Float, metaState: Int) =
            MotionEvent(downTime, eventTime, action, intArrayOf(0), floatArrayOf(x), floatArrayOf(y))
        fun obtain(downTime: Long, eventTime: Long, action: Int, ids: IntArray, xs: FloatArray, ys: FloatArray) =
            MotionEvent(downTime, eventTime, action, ids, xs, ys)
        fun obtain(e: MotionEvent) = e.offset(0f, 0f)
        fun actionToString(a: Int) = a.toString()
    }
}

class InputDevice {
    val name: String = ""
    val id: Int = 0
    val sources: Int = 0
    val isVirtual = true
    val isExternal = false
    val vendorId = 0
    val productId = 0
    val descriptor = ""
    val motionRanges: List<MotionRange> = emptyList()
    fun supportsSource(s: Int) = false
    fun getMotionRange(axis: Int): MotionRange? = null
    fun getMotionRange(axis: Int, source: Int): MotionRange? = null
    fun hasKeys(vararg keys: Int): BooleanArray = BooleanArray(keys.size)
    class MotionRange { val axis = 0; val min = -1f; val max = 1f; val flat = 0f; val fuzz = 0f; val source = 0 }
    val vibrator: android.os.Vibrator get() = android.os.Vibrator()
    companion object {
        const val SOURCE_JOYSTICK = 0x01000010
        const val SOURCE_GAMEPAD = 0x00000401
        const val SOURCE_DPAD = 0x00000201
        const val SOURCE_TOUCHSCREEN = 0x00001002
        const val SOURCE_MOUSE = 0x00002002
        const val SOURCE_KEYBOARD = 0x00000101
        const val SOURCE_CLASS_JOYSTICK = 0x10
        const val SOURCE_CLASS_POINTER = 0x2
        const val KEYBOARD_TYPE_ALPHABETIC = 2
        fun getDevice(id: Int): InputDevice? = null
        fun getDeviceIds(): IntArray = IntArray(0)
    }
}

class KeyCharacterMap private constructor() {
    fun get(code: Int, meta: Int) = 0
    fun getDisplayLabel(code: Int): Char = ' '
    companion object {
        const val VIRTUAL_KEYBOARD = -1
        const val BUILT_IN_KEYBOARD = 0
        fun load(id: Int) = KeyCharacterMap()
        fun deviceHasKey(code: Int) = false
    }
}
