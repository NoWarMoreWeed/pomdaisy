@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.hardware.input

class InputManager {
    fun getInputDeviceIds(): IntArray = IntArray(0)
    fun getInputDevice(id: Int): android.view.InputDevice? = null
    fun registerInputDeviceListener(l: InputDeviceListener, h: android.os.Handler?) {}
    fun unregisterInputDeviceListener(l: InputDeviceListener) {}
    interface InputDeviceListener { fun onInputDeviceAdded(id: Int); fun onInputDeviceRemoved(id: Int); fun onInputDeviceChanged(id: Int) }
}
