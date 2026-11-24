package br.com.jylabtech.zoop_payment_tech.licence

import org.json.JSONObject
import java.util.Date

/**
 * Regras de validade da licença. Classe pura (sem Android), testável em JVM.
 */
object LicenceRules {
    const val RESULT_SUCCESS = 0
    const val RESULT_ERROR = 999999

    /** Motivo do core para cliente bloqueado/banido (bloqueio total imediato). */
    const val REASON_CUSTOMER_BLOCKED = "customer_blocked"
    const val CUSTOMER_BLOCKED_MESSAGE = "Cliente bloqueado. Entre em contato com a JY Labtech."

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
     * 72h (última validação online bem-sucedida), não está no futuro (tolerância de 5 min),
     * pertence à chave atual e foi validado na mesma URL.
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
     * Indica se uma resposta HTTP diferente de 200 é uma recusa definitiva do servidor (licença
     * não encontrada, vencida, cancelada, suspensa, cliente bloqueado, aparelho desativado, limite
     * de aparelhos, requisição inválida). 408, 429, 5xx e outros códigos são falha temporária
     * (servidor fora do ar), que pode usar o cache offline.
     */
    fun isDefinitiveRejection(statusCode: Int): Boolean {
        return statusCode in 400..499 && statusCode != 408 && statusCode != 429
    }

    /**
     * Indica se é hora de consultar o servidor: sem cache, na primeira validação desta instância
     * (abertura do app) ou quando a última consulta online tem [LicenceConfig.REVALIDATE_INTERVAL_MS]
     * ou mais.
     */
    fun shouldRevalidate(cache: CheckLicenceEntity?, checkedOnlineInThisProcess: Boolean, now: Date?): Boolean {
        if (cache == null || !checkedOnlineInThisProcess || now == null) return true
        val savedAt = cache.savedLocalDatabaseAt ?: return true
        return now.time - savedAt.time >= LicenceConfig.REVALIDATE_INTERVAL_MS
    }

    /**
     * Decide o resultado da chamada HTTP de validação. Somente 200 com licença utilizável é sucesso.
     * Marca [LicenceResult.rejectedByServer] nas recusas definitivas (ver [isDefinitiveRejection] e
     * 200 com licença lida mas inutilizável); corpo 200 ilegível (ex.: HTML de proxy) não é recusa.
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
                // Corpo ilegível (ex.: página de erro de proxy) não é recusa da licença.
                result.rejectedByServer = entity != null
            }
            return result
        }

        result.rejectedByServer = isDefinitiveRejection(statusCode)
        result.errorMessage = errorMessageFromBody(statusCode, body)
        return result
    }

    /**
     * Extrai `message` do corpo de erro; sem corpo JSON, "Erro ao validar licença (HTTP <code>)".
     * Com `"reason":"customer_blocked"` devolve [CUSTOMER_BLOCKED_MESSAGE].
     */
    fun errorMessageFromBody(statusCode: Int, body: String?): String {
        val fallback = "Erro ao validar licença (HTTP $statusCode)"
        if (body.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(body)
            if (json.optString("reason", "") == REASON_CUSTOMER_BLOCKED) {
                CUSTOMER_BLOCKED_MESSAGE
            } else if (json.isNull("message")) {
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
