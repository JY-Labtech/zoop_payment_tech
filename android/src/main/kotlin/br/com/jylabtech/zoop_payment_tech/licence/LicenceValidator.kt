package br.com.jylabtech.zoop_payment_tech.licence

import java.util.Date

/** Resposta HTTP crua do servidor de licenças. */
class LicenceHttpResponse(val statusCode: Int, val body: String?)

/** Transporte da validação online (implementado por [LicenceProvider]). */
interface LicenceHttpClient {
    /** Executa o POST de validação. Pode lançar exceção em falha de rede/timeout. */
    fun post(url: String, licenceKey: String): LicenceHttpResponse
}

/** Armazenamento do cache offline (implementado com EncryptedSharedPreferences). */
interface LicenceStore {
    fun load(): CheckLicenceEntity?
    fun save(entity: CheckLicenceEntity)
    fun delete()
}

/**
 * Fluxo de validação de licença: cache válido (sem rede) ou validação online no core.
 * Classe pura (sem Android), testável em JVM com fakes.
 */
class LicenceValidator(
    private val store: LicenceStore,
    private val httpClient: LicenceHttpClient,
    private val base64Decoder: (String) -> ByteArray,
    private val clock: () -> Date = { Date() }
) {

    fun validate(licenceKey: String?, licenceInternalKey: String?): LicenceResult {
        val key = licenceKey?.trim().orEmpty()
        if (key.isEmpty()) {
            return error("Licença não configurada (licenceKey vazio)")
        }

        val acceptedUrl = LicenceConfig.acceptedLicenceUrl(licenceInternalKey, base64Decoder)
        val resolvedUrl = acceptedUrl ?: LicenceConfig.LICENCE_CHECK_URL
        val now = clock()

        val cache = try {
            store.load()
        } catch (e: Exception) {
            null
        }
        if (cache != null) {
            if (LicenceRules.isCacheUsable(
                    cache, key, licenceInternalKey, resolvedUrl, acceptedUrl != null, now
                )
            ) {
                val ok = LicenceResult()
                ok.method = "licence"
                ok.result = LicenceRules.RESULT_SUCCESS
                ok.licenceEntity = cache
                return ok
            }
            try {
                store.delete()
            } catch (_: Exception) {
            }
        }

        val response = try {
            httpClient.post(resolvedUrl, key)
        } catch (e: Exception) {
            return error("Falha de comunicação com o servidor de licenças: ${e.message ?: e.javaClass.simpleName}")
        }

        val result = LicenceRules.evaluateServerResponse(response.statusCode, response.body, clock())
        if (result.result == LicenceRules.RESULT_SUCCESS) {
            val entity = result.licenceEntity!!
            entity.licenceInternalKey = resolvedUrl
            entity.savedLocalDatabaseAt = clock()
            try {
                store.save(entity)
            } catch (_: Exception) {
                // Falha ao salvar o cache não invalida a licença recém-validada.
            }
        }
        return result
    }

    private fun error(message: String): LicenceResult {
        val result = LicenceResult()
        result.method = "licence"
        result.result = LicenceRules.RESULT_ERROR
        result.errorMessage = message
        return result
    }
}
