package br.com.jylabtech.zoop_payment_tech.licence

class LicenceResult {
    var method: String? = null
    var errorMessage: String? = null
    /** 0 = sucesso. Começa como erro: só vira sucesso quando a licença é validada. */
    var result: Int = LicenceRules.RESULT_ERROR
    var message: String? = null
    var licenceEntity: CheckLicenceEntity? = null

    val isSuccess: Boolean
        get() = result == LicenceRules.RESULT_SUCCESS
}
