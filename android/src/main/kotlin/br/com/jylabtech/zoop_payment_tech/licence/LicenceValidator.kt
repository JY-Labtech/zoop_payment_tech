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
 * Fluxo de validação de licença conforme o contrato de licenças do jylabtech_core:
 *
 * - A primeira validação de cada instância (abertura do app) sempre consulta o servidor.
 * - Depois disso, com cache válido, usa o cache sem rede e consulta o servidor de novo a cada
 *   [LicenceConfig.REVALIDATE_INTERVAL_MS] (4h).
 * - Recusa definitiva do servidor (4xx exceto 408/429, ou 200 com licença inutilizável) apaga o
 *   cache e bloqueia na hora.
 * - Falha temporária (rede, timeout, 408, 429, 5xx, corpo ilegível) usa o cache válido (salvo há
 *   no máximo 72h); sem cache, bloqueia. Após uma falha, espera
 *   [LicenceConfig.RETRY_AFTER_FAILURE_MS] para tentar o servidor de novo, para não travar cada
 *   operação offline no timeout.
 *
 * O estado online fica em memória nesta instância (não é persistido): reiniciar o app força nova
 * consulta ao servidor. Classe pura (sem Android), testável em JVM com fakes.
 */
class LicenceValidator(
    private val store: LicenceStore,
    private val httpClient: LicenceHttpClient,
    private val base64Decoder: (String) -> ByteArray,
    private val clock: () -> Date = { Date() }
) {

    private var checkedOnlineInThisProcess = false
    private var lastOnlineFailureAt: Date? = null

    @Synchronized
    fun validate(licenceKey: String?, licenceInternalKey: String?): LicenceResult {
        val key = licenceKey?.trim().orEmpty()
        if (key.isEmpty()) {
            return error("Licença não configurada (licenceKey vazio)")
        }

        val acceptedUrl = LicenceConfig.acceptedLicenceUrl(licenceInternalKey, base64Decoder)
        val resolvedUrl = acceptedUrl ?: LicenceConfig.LICENCE_CHECK_URL
        val now = clock()

        val cache = validCache(key, licenceInternalKey, resolvedUrl, acceptedUrl != null, now)
        if (cache != null && !LicenceRules.shouldRevalidate(cache, checkedOnlineInThisProcess, now)) {
            return success(cache)
        }
        if (cache != null && recentlyFailedOnline(now)) {
            return success(cache)
        }

        val response = try {
            httpClient.post(resolvedUrl, key)
        } catch (e: Exception) {
            return onlineFailure(
                cache,
                "Falha de comunicação com o servidor de licenças: ${e.message ?: e.javaClass.simpleName}"
            )
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
            checkedOnlineInThisProcess = true
            lastOnlineFailureAt = null
            return result
        }

        if (result.rejectedByServer) {
            // Recusa definitiva: o cache não pode mais liberar o uso.
            deleteCache()
            checkedOnlineInThisProcess = true
            lastOnlineFailureAt = null
            return result
        }
        return onlineFailure(cache, result.errorMessage ?: "Erro ao validar licença")
    }

    /** Cache utilizável pelas regras de [LicenceRules.isCacheUsable]; cache inválido é apagado. */
    private fun validCache(
        key: String,
        licenceInternalKey: String?,
        resolvedUrl: String,
        internalKeyIsOfficial: Boolean,
        now: Date
    ): CheckLicenceEntity? {
        val cache = try {
            store.load()
        } catch (e: Exception) {
            null
        } ?: return null
        if (LicenceRules.isCacheUsable(cache, key, licenceInternalKey, resolvedUrl, internalKeyIsOfficial, now)) {
            return cache
        }
        deleteCache()
        return null
    }

    private fun recentlyFailedOnline(now: Date): Boolean {
        val failedAt = lastOnlineFailureAt ?: return false
        val elapsed = now.time - failedAt.time
        return elapsed >= 0 && elapsed < LicenceConfig.RETRY_AFTER_FAILURE_MS
    }

    /** Servidor inacessível: usa o cache válido (até 72h) ou bloqueia se não houver. */
    private fun onlineFailure(cache: CheckLicenceEntity?, message: String): LicenceResult {
        lastOnlineFailureAt = clock()
        return if (cache != null) success(cache) else error(message)
    }

    private fun deleteCache() {
        try {
            store.delete()
        } catch (_: Exception) {
        }
    }

    private fun success(entity: CheckLicenceEntity): LicenceResult {
        val result = LicenceResult()
        result.method = "licence"
        result.result = LicenceRules.RESULT_SUCCESS
        result.licenceEntity = entity
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
