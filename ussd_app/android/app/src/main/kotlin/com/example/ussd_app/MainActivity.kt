import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : FlutterActivity() {

    private val USSD_CHANNEL = "groceries.ussd"
    private val CALL_PHONE_REQUEST_CODE = 1
    private val READ_PHONE_STATE_REQUEST_CODE = 2
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

    // Add this method to handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CALL_PHONE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Rest of your existing code...
}
