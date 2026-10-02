package br.com.jylabtech.zoop_payment_tech.licence

import br.com.jylabtech.zoop_payment_tech.licence.LicenceTestData.CORE_B64
import br.com.jylabtech.zoop_payment_tech.licence.LicenceTestData.DAY_MS
import br.com.jylabtech.zoop_payment_tech.licence.LicenceTestData.KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class LicenceRulesTest {

    private val now = Date()
    private val core = LicenceConfig.LICENCE_CHECK_URL

    // ---------- isLicenceUsable ----------

    @Test
    fun usable_validLicence() {
        assertTrue(LicenceRules.isLicenceUsable(LicenceTestData.entity(), now))
    }

    @Test
    fun usable_expiredByDate() {
        val e = LicenceTestData.entity(expiredAt = Date(now.time - 1000))
        assertFalse(LicenceRules.isLicenceUsable(e, now))
    }

    @Test
    fun usable_isExpiredFlag() {
        assertFalse(LicenceRules.isLicenceUsable(LicenceTestData.entity(isExpired = true), now))
    }

    @Test
    fun usable_isInactiveFlag() {
        assertFalse(LicenceRules.isLicenceUsable(LicenceTestData.entity(isInactive = true), now))
    }

    @Test
    fun usable_isCanceledFlag() {
        assertFalse(LicenceRules.isLicenceUsable(LicenceTestData.entity(isCanceled = true), now))
    }

    @Test
    fun usable_nullExpiredAt() {
        assertFalse(LicenceRules.isLicenceUsable(LicenceTestData.entity(expiredAt = null), now))
    }

    @Test
    fun usable_nullEntity() {
        assertFalse(LicenceRules.isLicenceUsable(null, now))
    }

    @Test
    fun usable_masterKey2099() {
        val e = CheckLicenceEntity.fromJson(LicenceTestData.coreJson(expiredAt = "2099-12-31T23:59:59"))
        assertTrue(LicenceRules.isLicenceUsable(e, now))
    }

    // ---------- parse da resposta real do core ----------

    @Test
    fun parse_coreLegacyLicenceJson() {
        val e = CheckLicenceEntity.fromJson(LicenceTestData.coreJson(expiredAt = "2099-12-31T23:59:59"))
        assertNotNull(e)
        e!!
        assertEquals(KEY, e.licenceEncrypt)
        assertEquals(KEY, e.licenceKey)
        assertEquals("2099-12-31T23:59:59", LicenceTestData.fmt(e.expiredAt!!))
        assertFalse(e.isExpired)
        assertFalse(e.isInactive)
        assertFalse(e.isCanceled)
        assertFalse(e.isTrial)
        assertEquals("one_time", e.checkoutType)
        assertEquals("", e.subscriptionId)
        assertNotNull(e.createdAt)
        assertNotNull(e.updatedAt)
        // licenceInternalKey não vem do servidor: não pode virar a string "null"
        assertEquals("", e.licenceInternalKey)
    }

    @Test
    fun parse_isCanceledTrue() {
        val e = CheckLicenceEntity.fromJson(LicenceTestData.coreJson(isCanceled = true))
        assertTrue(e!!.isCanceled)
    }

    @Test
    fun parse_nullExpiredAt() {
        val e = CheckLicenceEntity.fromJson(LicenceTestData.coreJson(expiredAt = null))
        assertNotNull(e)
        assertNull(e!!.expiredAt)
    }

    @Test
    fun parse_invalidJsonReturnsNull() {
        assertNull(CheckLicenceEntity.fromJson("<html>bad gateway</html>"))
    }

    @Test
    fun toJsonRoundTripKeepsCacheFormat() {
        val e = CheckLicenceEntity.fromJson(LicenceTestData.coreJson(isCanceled = true))!!
        e.savedLocalDatabaseAt = Date(now.time - 1000)
        e.licenceInternalKey = core
        val back = CheckLicenceEntity.fromJson(e.toJson())!!
        assertEquals(e.licenceEncrypt, back.licenceEncrypt)
        assertEquals(LicenceTestData.fmt(e.expiredAt!!), LicenceTestData.fmt(back.expiredAt!!))
        assertEquals(LicenceTestData.fmt(e.savedLocalDatabaseAt!!), LicenceTestData.fmt(back.savedLocalDatabaseAt!!))
        assertEquals(core, back.licenceInternalKey)
        assertTrue(back.isCanceled)
    }

    // ---------- decisão do resultado HTTP ----------

    @Test
    fun http200Valid_isSuccess() {
        val r = LicenceRules.evaluateServerResponse(200, LicenceTestData.coreJson(), now)
        assertEquals(LicenceRules.RESULT_SUCCESS, r.result)
        assertTrue(r.isSuccess)
        assertNotNull(r.licenceEntity)
    }

    @Test
    fun http200ExpiredByDate_isError() {
        val past = LicenceTestData.fmt(Date(now.time - DAY_MS))
        val r = LicenceRules.evaluateServerResponse(200, LicenceTestData.coreJson(expiredAt = past), now)
        assertFalse(r.isSuccess)
        assertEquals("Licença vencida", r.errorMessage)
    }

    @Test
    fun http200IsExpired_isError() {
        val r = LicenceRules.evaluateServerResponse(200, LicenceTestData.coreJson(isExpired = true), now)
        assertFalse(r.isSuccess)
        assertEquals("Licença vencida", r.errorMessage)
    }

    @Test
    fun http200IsInactive_isError() {
        val r = LicenceRules.evaluateServerResponse(200, LicenceTestData.coreJson(isInactive = true), now)
        assertFalse(r.isSuccess)
        assertNotNull(r.errorMessage)
    }

    @Test
    fun http200IsCanceled_isError() {
        val r = LicenceRules.evaluateServerResponse(200, LicenceTestData.coreJson(isCanceled = true), now)
        assertFalse(r.isSuccess)
        assertEquals("Licença cancelada", r.errorMessage)
    }

    @Test
    fun http200InvalidJson_isError() {
        val r = LicenceRules.evaluateServerResponse(200, "not json", now)
        assertFalse(r.isSuccess)
        assertNotNull(r.errorMessage)
        assertNull(r.licenceEntity)
    }

    @Test
    fun http200EmptyBodyOrMissingExpiredAt_isError() {
        assertFalse(LicenceRules.evaluateServerResponse(200, "", now).isSuccess)
        assertFalse(LicenceRules.evaluateServerResponse(200, null, now).isSuccess)
        assertFalse(LicenceRules.evaluateServerResponse(200, "{}", now).isSuccess)
        assertFalse(LicenceRules.evaluateServerResponse(200, LicenceTestData.coreJson(expiredAt = null), now).isSuccess)
    }

    @Test
    fun httpErrors_useMessageFromBody() {
        val cases = mapOf(
            404 to "Licença não encontrada",
            412 to "Limite de aparelhos atingido",
            428 to "Licença vencida",
            403 to "Token não enviado",
            400 to "Aparelho sem identificador",
            429 to "Muitas requisições",
            500 to "Erro interno"
        )
        for ((code, msg) in cases) {
            val r = LicenceRules.evaluateServerResponse(code, """{"message":"$msg"}""", now)
            assertEquals("HTTP $code", LicenceRules.RESULT_ERROR, r.result)
            assertEquals("HTTP $code", msg, r.errorMessage)
        }
    }

    @Test
    fun httpErrors_emptyOrNonJsonBodyUseFallbackMessage() {
        assertEquals("Erro ao validar licença (HTTP 502)", LicenceRules.evaluateServerResponse(502, null, now).errorMessage)
        assertEquals("Erro ao validar licença (HTTP 502)", LicenceRules.evaluateServerResponse(502, "", now).errorMessage)
        assertEquals("Erro ao validar licença (HTTP 503)", LicenceRules.evaluateServerResponse(503, "<html/>", now).errorMessage)
        assertEquals("Erro ao validar licença (HTTP 404)", LicenceRules.evaluateServerResponse(404, "{}", now).errorMessage)
        assertEquals("Erro ao validar licença (HTTP 404)", LicenceRules.evaluateServerResponse(404, """{"message":null}""", now).errorMessage)
    }

    @Test
    fun licenceResultStartsAsError() {
        assertEquals(LicenceRules.RESULT_ERROR, LicenceResult().result)
        assertFalse(LicenceResult().isSuccess)
    }

    // ---------- regras do cache ----------

    private fun cache(
        savedAt: Date? = Date(now.time - DAY_MS),
        storedUrl: String? = core,
        entity: CheckLicenceEntity = LicenceTestData.entity()
    ): CheckLicenceEntity {
        entity.savedLocalDatabaseAt = savedAt
        entity.licenceInternalKey = storedUrl
        return entity
    }

    private fun usable(c: CheckLicenceEntity?, key: String = KEY, internalKey: String? = CORE_B64, official: Boolean = true) =
        LicenceRules.isCacheUsable(c, key, internalKey, core, official, now)

    @Test
    fun cache_valid() {
        assertTrue(usable(cache()))
    }

    @Test
    fun cache_validWithLegacyRawInternalKeyStored() {
        // caches antigos guardavam o valor bruto do licenceInternalKey
        assertTrue(usable(cache(storedUrl = CORE_B64)))
    }

    @Test
    fun cache_olderThan7Days() {
        assertFalse(usable(cache(savedAt = Date(now.time - 8 * DAY_MS))))
    }

    @Test
    fun cache_savedAtMissing() {
        assertFalse(usable(cache(savedAt = null)))
    }

    @Test
    fun cache_savedAtInTheFuture() {
        assertFalse(usable(cache(savedAt = Date(now.time + 10 * 60 * 1000))))
        // dentro da tolerância de 5 min
        assertTrue(usable(cache(savedAt = Date(now.time + 60 * 1000))))
    }

    @Test
    fun cache_differentKey() {
        assertFalse(usable(cache(), key = "another-key"))
        assertFalse(usable(cache(), key = ""))
    }

    @Test
    fun cache_differentUrl() {
        val old = "***REMOVIDO***"
        assertFalse(usable(cache(storedUrl = "https://evil.com/pos-payments/licence/check")))
        assertFalse(usable(cache(storedUrl = null)))
        // cache salvo com um valor que não é oficial não vale, mesmo se o app ainda usa esse valor
        assertFalse(usable(cache(storedUrl = old), internalKey = old, official = false))
    }

    @Test
    fun cache_expiredLicence() {
        assertFalse(usable(cache(entity = LicenceTestData.entity(expiredAt = Date(now.time - 1000)))))
        assertFalse(usable(cache(entity = LicenceTestData.entity(isExpired = true))))
        assertFalse(usable(cache(entity = LicenceTestData.entity(isInactive = true))))
        assertFalse(usable(cache(entity = LicenceTestData.entity(isCanceled = true))))
    }

    @Test
    fun cache_null() {
        assertFalse(usable(null))
    }
}
