package br.com.jylabtech.zoop_payment_tech.licence

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class LicenceProvider(private val context: Context) {

    suspend fun checkLicence(urlString: String, licenceKey: String): LicenceResult {
        return withContext(Dispatchers.IO) {
            val licenceResult = LicenceResult()
            licenceResult.method = "licence"

            var connection: HttpURLConnection? = null
            try {
                val deviceId = getAndroidId()
                val deviceUid = getSerialNumber()
                val deviceLocalId = getDeviceUUID()
                val deviceName = Build.MODEL

                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")

                if (licenceKey.isNotEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer licence:$licenceKey")
                }

                connection.doOutput = true

                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("deviceUid", deviceUid)
                    put("deviceLocalId", deviceLocalId)
                    put("deviceName", deviceName)
                }

                val os: OutputStream = connection.outputStream
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val responseStr = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        responseStr.append(line)
                    }
                    reader.close()

                    licenceResult.result = 0
                    licenceResult.licenceEntity = CheckLicenceEntity.fromJson(responseStr.toString())
                } else {
                    licenceResult.result = 999999
                    licenceResult.errorMessage = handleErrors(connection)
                }

                licenceResult
            } catch (e: Exception) {
                licenceResult.result = 999999
                licenceResult.errorMessage = e.message
                licenceResult
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun handleErrors(connection: HttpURLConnection): String {
        var errorMessage = "Erro interno"
        try {
            val reader = BufferedReader(InputStreamReader(connection.errorStream))
            val responseStr = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                responseStr.append(line)
            }
            reader.close()

            val json = JSONObject(responseStr.toString())
            errorMessage = json.optString("message", errorMessage)
        } catch (e: Exception) {
            Log.d("LicenceProvider", "ExceptionMessageError: ${e.message}")
            errorMessage = e.message ?: "Erro desconhecido"
        }
        return errorMessage
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }

    private fun getSerialNumber(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL ?: "UNKNOWN_SERIAL"
            }
        } catch (e: SecurityException) {
            "UNKNOWN_SERIAL"
        } catch (e: Exception) {
            "UNKNOWN_SERIAL"
        }
    }

    private fun getDeviceUUID(): String {
        return UUID.randomUUID().toString()
    }
}

