package br.com.jylabtech.zoop_payment_tech.licence

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dados de teste fictícios (sem chaves reais). */
object LicenceTestData {
    const val KEY = "test-licence-key-0000"
    const val CORE_B64 = "***REMOVIDO***"
    const val DAY_MS = 24L * 60 * 60 * 1000

    fun fmt(date: Date): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(date)

    /** JSON no formato do LegacyLicence do jylabtech_core. */
    fun coreJson(
        expiredAt: String? = fmt(Date(System.currentTimeMillis() + 30 * DAY_MS)),
        isExpired: Boolean = false,
        isInactive: Boolean = false,
        isCanceled: Boolean = false,
        licenceEncrypt: String = KEY
    ): String {
        val expired = if (expiredAt == null) "null" else "\"$expiredAt\""
        return """
            {
              "id": "65f000000000000000000001",
              "uid": "65f000000000000000000001",
              "deviceUid": "",
              "deviceSerialNumber": "",
              "userUid": "65f000000000000000000002",
              "productId": "65f000000000000000000003",
              "productName": "Licença plugin de pagamento",
              "productDescription": "",
              "integrationLibId": "",
              "integrationLibName": "pos_payments",
              "checkoutSessionId": "",
              "licenceKey": "$licenceEncrypt",
              "licenceEncrypt": "$licenceEncrypt",
              "expiredAt": $expired,
              "isExpired": $isExpired,
              "isInactive": $isInactive,
              "isTrial": false,
              "isDebug": false,
              "isCanceled": $isCanceled,
              "canceledAt": null,
              "licencesQuantity": -1,
              "subscriptionId": "",
              "subscription": null,
              "checkoutType": "one_time",
              "createdAt": "2026-01-10T09:30:00",
              "updatedAt": "2026-09-30T18:00:00",
              "coreToken": "fake.core.token"
            }
        """.trimIndent()
    }

    fun entity(
        expiredAt: Date? = Date(System.currentTimeMillis() + 30 * DAY_MS),
        isExpired: Boolean = false,
        isInactive: Boolean = false,
        isCanceled: Boolean = false
    ): CheckLicenceEntity {
        val e = CheckLicenceEntity()
        e.expiredAt = expiredAt
        e.isExpired = isExpired
        e.isInactive = isInactive
        e.isCanceled = isCanceled
        e.licenceEncrypt = KEY
        e.licenceKey = KEY
        return e
    }
}
