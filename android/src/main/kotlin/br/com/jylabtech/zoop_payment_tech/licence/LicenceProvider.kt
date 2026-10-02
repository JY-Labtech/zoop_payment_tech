package br.com.jylabtech.zoop_payment_tech.licence

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Transporte HTTP da validação de licença. Bloqueante: chamar fora da main thread.
 */
class LicenceProvider(private val context: Context) : LicenceHttpClient {

    override fun post(url: String, licenceKey: String): LicenceHttpResponse {
        var connection: HttpURLConnection? = null
        try {
            val body = JSONObject().apply {
                put("deviceId", getAndroidId())
                put("deviceUid", getSerialNumber())
                put("deviceLocalId", getDeviceUUID())
                put("deviceName", Build.MODEL)
            }

            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = LicenceConfig.CONNECT_TIMEOUT_MS
            connection.readTimeout = LicenceConfig.READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/json")
            if (licenceKey.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer licence:$licenceKey")
            }
            connection.doOutput = true

            val os: OutputStream = connection.outputStream
            os.write(body.toString().toByteArray(Charsets.UTF_8))
            os.flush()
            os.close()

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            return LicenceHttpResponse(responseCode, readBody(stream))
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBody(stream: InputStream?): String? {
        if (stream == null) return null
        return try {
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        } catch (e: Exception) {
            null
        }
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
