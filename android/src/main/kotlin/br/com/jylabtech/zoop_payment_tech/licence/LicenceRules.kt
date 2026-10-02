package br.com.jylabtech.zoop_payment_tech.licence

import org.json.JSONObject
import java.util.Date

/**
 * Regras de validade da licença. Classe pura (sem Android), testável em JVM.
 */
object LicenceRules {
    const val RESULT_SUCCESS = 0
    const val RESULT_ERROR = 999999

    /** Licença utilizável: vigente pela data e sem flags de vencida, inativa ou cancelada. */
    fun isLicenceUsable(entity: CheckLicenceEntity?, now: Date): Boolean {
        if (entity == null) return false
        val expiredAt = entity.expiredAt ?: return false
        return expiredAt.after(now) &&
            !entity.isExpired &&
            !entity.isInactive &&
            !entity.isCanceled
    }

    /** Motivo legível para uma licença não utilizável. */
    fun unusableReason(entity: CheckLicenceEntity?, now: Date): String {
        return when {
            entity == null -> "Resposta inválida do servidor de licenças"
            entity.expiredAt == null -> "Resposta do servidor de licenças sem data de vencimento"
            entity.isCanceled -> "Licença cancelada"
            entity.isInactive -> "Licença inativa ou suspensa"
            entity.isExpired || !entity.expiredAt!!.after(now) -> "Licença vencida"
            else -> "Licença inválida"
        }
    }

    /**
     * O cache offline só vale se, ao mesmo tempo: a licença é utilizável, foi salvo há no máximo
     * 7 dias, não está no futuro (tolerância de 5 min), pertence à chave atual e foi validado na
     * mesma URL.
     *
     * @param resolvedUrl URL efetiva (já passada por [LicenceConfig.resolveLicenceUrl]).
     * @param internalKeyIsOfficial true quando o `licenceInternalKey` atual aponta para o servidor
     * oficial; permite aceitar caches antigos que guardavam o valor bruto do `licenceInternalKey`.
     */
    fun isCacheUsable(
        cache: CheckLicenceEntity?,
        licenceKey: String,
        licenceInternalKey: String?,
        resolvedUrl: String,
        internalKeyIsOfficial: Boolean,
        now: Date
    ): Boolean {
        if (cache == null) return false
        if (!isLicenceUsable(cache, now)) return false

        val savedAt = cache.savedLocalDatabaseAt ?: return false
        val age = now.time - savedAt.time
        if (age > LicenceConfig.CACHE_MAX_AGE_MS) return false
        if (age < -LicenceConfig.CLOCK_SKEW_TOLERANCE_MS) return false

        if (licenceKey.isEmpty() || cache.licenceEncrypt != licenceKey) return false

        val stored = cache.licenceInternalKey
        val sameUrl = stored == resolvedUrl ||
            (internalKeyIsOfficial && !licenceInternalKey.isNullOrEmpty() && stored == licenceInternalKey)
        return sameUrl
    }

    /**
     * Decide o resultado da chamada HTTP de validação. Somente 200 com licença utilizável é sucesso.
     */
    fun evaluateServerResponse(statusCode: Int, body: String?, now: Date): LicenceResult {
        val result = LicenceResult()
        result.method = "licence"
        result.result = RESULT_ERROR

        if (statusCode == 200) {
            val entity = if (body.isNullOrBlank()) null else CheckLicenceEntity.fromJson(body)
            if (isLicenceUsable(entity, now)) {
                result.result = RESULT_SUCCESS
                result.licenceEntity = entity
            } else {
                result.errorMessage = unusableReason(entity, now)
            }
            return result
        }

        result.errorMessage = errorMessageFromBody(statusCode, body)
        return result
    }

    /** Extrai `message` do corpo de erro; sem corpo JSON, "Erro ao validar licença (HTTP <code>)". */
    fun errorMessageFromBody(statusCode: Int, body: String?): String {
        val fallback = "Erro ao validar licença (HTTP $statusCode)"
        if (body.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(body)
            if (json.isNull("message")) {
                fallback
            } else {
                val message = json.optString("message").trim()
                if (message.isEmpty()) fallback else message
            }
        } catch (e: Exception) {
            fallback
        }
    }
}
