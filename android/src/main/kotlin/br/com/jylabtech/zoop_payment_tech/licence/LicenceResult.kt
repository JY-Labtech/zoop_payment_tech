package br.com.jylabtech.zoop_payment_tech.licence

class LicenceResult {
    var method: String? = null
    var errorMessage: String? = null
    /** 0 = sucesso. Começa como erro: só vira sucesso quando a licença é validada. */
    var result: Int = LicenceRules.RESULT_ERROR
    var message: String? = null
    var licenceEntity: CheckLicenceEntity? = null
    /**
     * true quando o servidor respondeu e recusou a licença de forma definitiva (4xx exceto 408/429,
     * ou 200 com licença inutilizável). Nesse caso o cache é apagado e o uso é bloqueado na hora;
     * falhas de rede, timeout, 408, 429, 5xx e corpo ilegível ficam false e podem usar o cache
     * offline (até 72h).
     */
    var rejectedByServer: Boolean = false

    val isSuccess: Boolean
        get() = result == LicenceRules.RESULT_SUCCESS
}
