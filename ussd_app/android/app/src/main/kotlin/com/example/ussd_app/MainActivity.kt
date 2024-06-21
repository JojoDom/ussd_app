package com.example.ussd_app

import io.flutter.embedding.android.FlutterActivity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UssdResponseCallback
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugins.GeneratedPluginRegistrant
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.CompletableFuture

class MainActivity : FlutterActivity() {

    private val USSD_CHANNEL = "groceries.ussd"
    private val CALL_PHONE_REQUEST_CODE = 1
    private val ussdApi: USSDApi = USSDController



    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, USSD_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "sendUssd" -> {
                    val ussdCode = call.argument<String>("ussdCode")
                    if (ussdCode != null) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                            singleSessionUssd(ussdCode, -1, result)
                        } else {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE), CALL_PHONE_REQUEST_CODE)
                            result.error("Permission Denied", "CALL_PHONE or READ_PHONE_STATE permission is denied", null)
                        }
                    } else {
                        result.error("Invalid Code", "USSD code is null", null)
                    }
                }
                "multisessionUssd" -> {
                    val ussdCode = call.argument<String>("ussdCode")
                    if (ussdCode != null) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                            multisessionUssd(ussdCode, -1, result)
                        } else {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE), CALL_PHONE_REQUEST_CODE)
                            result.error("Permission Denied", "CALL_PHONE or READ_PHONE_STATE permission is denied", null)
                        }
                    } else {
                        result.error("Invalid Code", "USSD code is null", null)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun sendUssd(ussdCode: String) {
        var number = ussdCode.replace("#", "%23")
        if (!number.startsWith("tel:")) {
            number = "tel:$number"
        }
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse(number)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun singleSessionUssd(ussdCode: String, subscriptionId: Int, result: MethodChannel.Result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), CALL_PHONE_REQUEST_CODE)
                return
            }

            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val simManager = telephonyManager.createForSubscriptionId(subscriptionId)

            val callback = object : UssdResponseCallback() {
                override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                    result.success(response)
                    Toast.makeText(this@MainActivity, "USSD Response: $response", Toast.LENGTH_LONG).show()
                }

                override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                    val errorMessage = when (failureCode) {
                        TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> "USSD Error: Service Unavailable"
                        TelephonyManager.USSD_RETURN_FAILURE -> "USSD Error: Return Failure"
                        else -> "USSD Error: Unknown"
                    }
                    result.error("USSD_FAILED", errorMessage, null)
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            if (subscriptionId == -1) {
                telephonyManager.sendUssdRequest(ussdCode, callback, Handler(Looper.getMainLooper()))
            } else {
                simManager.sendUssdRequest(ussdCode, callback, Handler(Looper.getMainLooper()))
            }
        } else {
            sendUssd(ussdCode)
        }
    }


    private var currentEvent: AccessibilityEvent? = null

    private fun multisessionUssd(ussdCode: String, subscriptionId: Int, @NonNull result: MethodChannel.Result) {
        var slot = subscriptionId
        if (subscriptionId == -1) {
            slot = 0
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE), CALL_PHONE_REQUEST_CODE)
                return
            }
            USSDController.callUSSDInvoke(this, ussdCode, slot, object : USSDController.CallbackInvoke {
    
                override fun responseInvoke(event: AccessibilityEvent) {
                    currentEvent = AccessibilityEvent.obtain(event)
                    result.success(event.text.joinToString("\n"))
                }
    
                override fun over(message: String) {
                    result.success(message)
                }
            })
        }
    }


   
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}