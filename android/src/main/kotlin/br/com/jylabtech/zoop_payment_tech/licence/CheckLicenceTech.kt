package br.com.jylabtech.zoop_payment_tech.licence

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.concurrent.TimeUnit

class CheckLicenceTech(private val context: Context) {

    private val licenceProvider = LicenceProvider(context)

    companion object {
        private const val PREFS_NAME = "licence_prefs_encrypted"
        private const val KEY_JSON = "licence_entity"
    }

    /**
     * Valida a licença chamando a API e salva localmente se sucesso.
     */
    fun validateLicence(licenceKey: String, licenceInternalKey: String): LicenceResult {
        val licenceResult = LicenceResult()
        licenceResult.method = "licence"

        return try {
            val licenceDatabase = validateLicenceDatabase(licenceKey, licenceInternalKey)
            if (licenceDatabase) {
                licenceResult.result = 0
                return licenceResult
            }

            val apiUrl = sanitizeUrl(licenceInternalKey)
            val licence = runBlocking {
                licenceProvider.checkLicence(apiUrl, licenceKey)
            }

            if (licence != null && licence.result == 0) {
                val isValidLicence = validateLicenceServer(licence.licenceEntity)
                if (isValidLicence && licence.licenceEntity != null) {
                    val entity = licence.licenceEntity!!
                    entity.licenceInternalKey = licenceInternalKey
                    saveLicenceEncrypted(entity)
                    licenceResult.result = 0
                    licenceResult.licenceEntity = entity
                } else {
                    licenceResult.result = 999999
                    licenceResult.errorMessage = "Licença inválida ou expirada"
                }
            } else {
                licenceResult.result = licence?.result ?: 999999
                licenceResult.errorMessage = licence?.errorMessage ?: "Erro ao validar licença"
            }

            licenceResult
        } catch (e: Exception) {
            licenceResult.result = 999999
            licenceResult.errorMessage = e.message
            e.printStackTrace()
            licenceResult
        }
    }

    fun validateExistsLicence(): Boolean {
        val now = Date()
        return try {
            val licenceDatabase = getLicenceEncrypted()
            if (licenceDatabase != null) {
                var aux = licenceDatabase.expiredAt?.after(now) == true

                if (licenceDatabase.isExpired || licenceDatabase.isInactive) {
                    aux = false
                }

                if (licenceDatabase.savedLocalDatabaseAt != null) {
                    val diffInMillis = now.time - licenceDatabase.savedLocalDatabaseAt!!.time
                    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                    if (diffInDays > 7) {
                        deleteLicenceDatabase()
                        aux = false
                    }
                }

                aux
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun validateLicenceServer(entity: CheckLicenceEntity?): Boolean {
        if (entity == null) return false

        val now = Date()
        var aux = entity.expiredAt?.after(now) == true

        if (entity.isExpired || entity.isInactive) {
            aux = false
        }

        return aux
    }

    private fun validateLicenceDatabase(licenceKey: String, licenceInternalKey: String): Boolean {
        val now = Date()
        return try {
            val licenceDatabase = getLicenceEncrypted()
            if (licenceDatabase != null) {
                var aux = licenceDatabase.expiredAt?.after(now) == true

                if (licenceDatabase.isExpired || licenceDatabase.isInactive) {
                    aux = false
                }

                if (licenceDatabase.savedLocalDatabaseAt != null) {
                    val diffInMillis = now.time - licenceDatabase.savedLocalDatabaseAt!!.time
                    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                    if (diffInDays > 7) {
                        deleteLicenceDatabase()
                        aux = false
                    }
                }

                if (licenceDatabase.licenceEncrypt != licenceKey) {
                    aux = false
                }

                if (licenceDatabase.licenceInternalKey != licenceInternalKey) {
                    deleteLicenceDatabase()
                    aux = false
                }

                aux
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Salva a entidade de licença localmente de forma criptografada.
     */
    private fun saveLicenceEncrypted(entity: CheckLicenceEntity): Boolean {
        return try {
            entity.savedLocalDatabaseAt = Date()

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs: SharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            prefs.edit().putString(KEY_JSON, entity.toJson()).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Recupera a licença salva localmente de forma criptografada.
     */
    private fun getLicenceEncrypted(): CheckLicenceEntity? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs: SharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val savedJson = prefs.getString(KEY_JSON, null)
            if (savedJson != null) {
                CheckLicenceEntity.fromJson(savedJson)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deleteLicenceDatabase() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs: SharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            prefs.edit().remove(KEY_JSON).apply()
        } catch (e: Exception) {
            Log.e("CheckLicenceTech", "Erro ao deletar licença", e)
        }
    }

    private fun sanitizeUrl(data: String): String {
        return if (data.contains("http")) {
            data
        } else {
            val decodedBytes = Base64.decode(data, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8)
        }
    }
}

