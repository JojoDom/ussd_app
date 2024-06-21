package com.example.ussd_app
import android.view.accessibility.AccessibilityEvent
interface USSDInterface {
    fun sendData(text: String)
    fun sendData2(text: String, event: AccessibilityEvent)
    fun stopRunning()
}