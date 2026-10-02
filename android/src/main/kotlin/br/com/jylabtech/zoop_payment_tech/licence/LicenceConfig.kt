package br.com.jylabtech.zoop_payment_tech.licence

import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Configuração fixa da validação de licença.
 *
 * Classe pura (sem dependências do Android), testável em JVM.
 */
object LicenceConfig {
    /** Endpoint oficial de validação de licença (jylabtech_core). */
    const val LICENCE_CHECK_URL = "https://licence-server.invalid/pos-payments/licence/check"

    const val LICENCE_HOST = "licence-server.invalid"
    const val LICENCE_PATH_PREFIX = "/pos-payments/licence/check"

    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 15_000

    /** Validade máxima do cache offline. */
    const val CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

    /** Tolerância para `savedLocalDatabaseAt` no futuro (relógio do aparelho). */
    const val CLOCK_SKEW_TOLERANCE_MS = 5L * 60 * 1000

    /**
     * Decodifica o `licenceInternalKey` (URL em texto ou base64) e devolve a URL somente se ela
     * apontar para o servidor oficial (https, host exato, path de licença). Caso contrário, null.
     */
    fun acceptedLicenceUrl(
        licenceInternalKey: String?,
        base64Decoder: (String) -> ByteArray
    ): String? {
        val raw = licenceInternalKey?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val decoded = try {
            if (raw.contains("http")) {
                raw
            } else {
                String(base64Decoder(raw), StandardCharsets.UTF_8).trim()
            }
        } catch (e: Exception) {
            return null
        }
        if (decoded.isEmpty()) return null

        return try {
            val uri = URI(decoded)
            val scheme = uri.scheme ?: return null
            val host = uri.host ?: return null
            val path = uri.rawPath ?: return null
            val validPort = uri.port == -1 || uri.port == 443
            if (scheme.equals("https", ignoreCase = true) &&
                host.equals(LICENCE_HOST, ignoreCase = true) &&
                uri.rawUserInfo == null &&
                validPort &&
                path.startsWith(LICENCE_PATH_PREFIX)
            ) {
                decoded
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * URL efetiva de validação: o valor do `licenceInternalKey` quando aponta para o servidor
     * oficial; em qualquer outro caso (vazio, base64 inválido, URL antiga, outro host, http...),
     * a URL padrão do core.
     */
    fun resolveLicenceUrl(
        licenceInternalKey: String?,
        base64Decoder: (String) -> ByteArray
    ): String {
        return acceptedLicenceUrl(licenceInternalKey, base64Decoder) ?: LICENCE_CHECK_URL
    }
}
