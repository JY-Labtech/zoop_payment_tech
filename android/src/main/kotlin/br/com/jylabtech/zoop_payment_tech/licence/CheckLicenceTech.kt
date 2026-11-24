package br.com.jylabtech.zoop_payment_tech.licence

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Validação de licença no Android: liga o [LicenceValidator] (regras puras) ao cache
 * criptografado (EncryptedSharedPreferences) e ao transporte HTTP ([LicenceProvider]).
 *
 * [validateLicence] é bloqueante quando precisa consultar o servidor: chamar fora da main thread.
 */
class CheckLicenceTech(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "licence_prefs_encrypted"
        private const val KEY_JSON = "licence_entity"
        private const val TAG = "CheckLicenceTech"
    }

    private val validator = LicenceValidator(
        store = EncryptedLicenceStore(),
        httpClient = LicenceProvider(context),
        base64Decoder = { Base64.decode(it, Base64.DEFAULT) }
    )

    /**
     * Valida a licença conforme o contrato do jylabtech_core (ver [LicenceValidator]): a primeira
     * validação desta instância consulta o servidor oficial (a URL passa sempre por
     * [LicenceConfig.resolveLicenceUrl]); depois o cache vale sem rede e é revalidado a cada 4h.
     * Recusa definitiva do servidor apaga o cache e bloqueia na hora; falha de rede/5xx usa o
     * cache salvo há no máximo 72h. O estado online é desta instância (uma por engine do plugin).
     */
    @Synchronized
    fun validateLicence(licenceKey: String?, licenceInternalKey: String?): LicenceResult {
        return try {
            validator.validate(licenceKey, licenceInternalKey)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar licença", e)
            val result = LicenceResult()
            result.method = "licence"
            result.result = LicenceRules.RESULT_ERROR
            result.errorMessage = e.message ?: "Erro ao validar licença"
            result
        }
    }

    private inner class EncryptedLicenceStore : LicenceStore {
        private fun prefs(): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        override fun load(): CheckLicenceEntity? {
            return try {
                val savedJson = prefs().getString(KEY_JSON, null)
                if (savedJson != null) CheckLicenceEntity.fromJson(savedJson) else null
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao ler licença salva", e)
                null
            }
        }

        override fun save(entity: CheckLicenceEntity) {
            try {
                prefs().edit().putString(KEY_JSON, entity.toJson()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar licença", e)
            }
        }

        override fun delete() {
            try {
                prefs().edit().remove(KEY_JSON).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao deletar licença", e)
            }
        }
    }
}
