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
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Base64
import java.util.Date

class LicenceValidatorTest {

    private class FakeStore(var entity: CheckLicenceEntity? = null) : LicenceStore {
        var saves = 0
        var deletes = 0
        override fun load(): CheckLicenceEntity? = entity?.let { CheckLicenceEntity.fromJson(it.toJson()) }
        override fun save(entity: CheckLicenceEntity) {
            saves++
            this.entity = entity
        }
        override fun delete() {
            deletes++
            entity = null
        }
    }

    private class FakeHttp(var response: () -> LicenceHttpResponse) : LicenceHttpClient {
        val urls = mutableListOf<String>()
        val keys = mutableListOf<String>()
        override fun post(url: String, licenceKey: String): LicenceHttpResponse {
            urls.add(url)
            keys.add(licenceKey)
            return response()
        }
    }

    private var now = Date()
    private val core = LicenceConfig.LICENCE_CHECK_URL
    private val hour = 60L * 60 * 1000

    private fun validator(store: FakeStore, http: FakeHttp) =
        LicenceValidator(store, http, { Base64.getDecoder().decode(it) }, { now })

    private fun advance(millis: Long) {
        now = Date(now.time + millis)
    }

    private fun ok() = LicenceHttpResponse(200, LicenceTestData.coreJson())

    private fun validCache(savedAt: Date = Date(now.time - DAY_MS)): CheckLicenceEntity {
        val e = CheckLicenceEntity.fromJson(LicenceTestData.coreJson())!!
        e.savedLocalDatabaseAt = savedAt
        e.licenceInternalKey = core
        return e
    }

    @Test
    fun onlineSuccess_savesCacheWithResolvedUrlAndNow() {
        val store = FakeStore()
        val http = FakeHttp { ok() }
        val r = validator(store, http).validate(KEY, CORE_B64)
        assertTrue(r.isSuccess)
        assertEquals(listOf(core), http.urls)
        assertEquals(listOf(KEY), http.keys)
        assertEquals(1, store.saves)
        assertEquals(core, store.entity!!.licenceInternalKey)
        assertEquals(LicenceTestData.fmt(now), LicenceTestData.fmt(store.entity!!.savedLocalDatabaseAt!!))
    }

    @Test
    fun firstCheckOfProcessGoesOnlineEvenWithValidCache() {
        val store = FakeStore(validCache())
        val http = FakeHttp { ok() }
        val r = validator(store, http).validate(KEY, CORE_B64)
        assertTrue(r.isSuccess)
        assertEquals(1, http.urls.size)
        assertEquals(LicenceTestData.fmt(now), LicenceTestData.fmt(store.entity!!.savedLocalDatabaseAt!!))
    }

    @Test
    fun validCacheSkipsNetworkAfterOnlineCheckUntilRevalidationInterval() {
        val store = FakeStore()
        val http = FakeHttp { ok() }
        val v = validator(store, http)
        assertTrue(v.validate(KEY, CORE_B64).isSuccess)
        assertEquals(1, http.urls.size)

        advance(hour)
        assertTrue(v.validate(KEY, CORE_B64).isSuccess)
        assertEquals(1, http.urls.size)

        advance(LicenceConfig.REVALIDATE_INTERVAL_MS)
        assertTrue(v.validate(KEY, CORE_B64).isSuccess)
        assertEquals(2, http.urls.size)
    }

    @Test
    fun customerBlockedClearsCacheAndBlocksImmediately() {
        val store = FakeStore()
        var response = ok()
        val http = FakeHttp { response }
        val v = validator(store, http)
        v.validate(KEY, CORE_B64)
        advance(LicenceConfig.REVALIDATE_INTERVAL_MS)
        response = LicenceHttpResponse(
            428,
            """{"message":"A data de expiração da licença é inválida. [-]","reason":"customer_blocked"}"""
        )

        val r = v.validate(KEY, CORE_B64)
        assertFalse(r.isSuccess)
        assertTrue(r.rejectedByServer)
        assertEquals(LicenceRules.CUSTOMER_BLOCKED_MESSAGE, r.errorMessage)
        assertNull(store.entity)
        // sem cache: a próxima operação também consulta o servidor e continua bloqueada
        assertFalse(v.validate(KEY, CORE_B64).isSuccess)
        assertEquals(3, http.urls.size)
    }

    @Test
    fun everyDefinitiveRefusalClearsValidCache() {
        for (code in listOf(404, 412, 428, 403, 400)) {
            val store = FakeStore(validCache())
            val http = FakeHttp { LicenceHttpResponse(code, """{"message":"recusada"}""") }
            val r = validator(store, http).validate(KEY, CORE_B64)
            assertFalse("HTTP $code", r.isSuccess)
            assertEquals("HTTP $code", "recusada", r.errorMessage)
            assertNull("HTTP $code", store.entity)
        }
    }

    @Test
    fun online200CanceledClearsValidCache() {
        val store = FakeStore(validCache())
        val http = FakeHttp { LicenceHttpResponse(200, LicenceTestData.coreJson(isCanceled = true)) }
        assertFalse(validator(store, http).validate(KEY, CORE_B64).isSuccess)
        assertNull(store.entity)
    }

    @Test
    fun outageUsesCacheUpTo72Hours() {
        val store = FakeStore()
        var fail = false
        val http = FakeHttp { if (fail) throw IOException("sem internet") else ok() }
        val v = validator(store, http)
        v.validate(KEY, CORE_B64)
        fail = true

        advance(71 * hour)
        assertTrue(v.validate(KEY, CORE_B64).isSuccess)
        assertNotNull(store.entity)

        advance(hour + 1000)
        val r = v.validate(KEY, CORE_B64)
        assertFalse(r.isSuccess)
        assertTrue(r.errorMessage!!.contains("sem internet"))
        assertNull(store.entity)
    }

    @Test
    fun serverErrorsUseCache() {
        for (code in listOf(500, 502, 503, 408, 429)) {
            val store = FakeStore(validCache())
            val http = FakeHttp { LicenceHttpResponse(code, """{"message":"fora do ar"}""") }
            val r = validator(store, http).validate(KEY, CORE_B64)
            assertTrue("HTTP $code", r.isSuccess)
            assertNotNull("HTTP $code", store.entity)
        }
    }

    @Test
    fun unreadable200UsesCache() {
        val store = FakeStore(validCache())
        val http = FakeHttp { LicenceHttpResponse(200, "<html>captive portal</html>") }
        assertTrue(validator(store, http).validate(KEY, CORE_B64).isSuccess)
        assertNotNull(store.entity)
    }

    @Test
    fun afterOutageWaitsBeforeRetryingServer() {
        val store = FakeStore(validCache())
        var response: () -> LicenceHttpResponse = { throw IOException("sem internet") }
        val http = FakeHttp { response() }
        val v = validator(store, http)
        assertTrue(v.validate(KEY, CORE_B64).isSuccess)
        assertEquals(1, http.urls.size)

        advance(60 * 1000)
        assertTrue(v.validate(KEY, CORE_B64).isSuccess)
        assertEquals(1, http.urls.size)

        advance(LicenceConfig.RETRY_AFTER_FAILURE_MS)
        response = { LicenceHttpResponse(428, """{"message":"Licença vencida"}""") }
        assertFalse(v.validate(KEY, CORE_B64).isSuccess)
        assertEquals(2, http.urls.size)
        assertNull(store.entity)
    }

    @Test
    fun outageWithoutCacheBlocksAndKeepsTrying() {
        val http = FakeHttp { throw IOException("sem internet") }
        val v = validator(FakeStore(), http)
        assertFalse(v.validate(KEY, CORE_B64).isSuccess)
        assertFalse(v.validate(KEY, CORE_B64).isSuccess)
        assertEquals(2, http.urls.size)
    }

    @Test
    fun staleCache_isDeletedAndRevalidatedOnline() {
        val store = FakeStore(validCache(savedAt = Date(now.time - 4 * DAY_MS)))
        val http = FakeHttp { ok() }
        val r = validator(store, http).validate(KEY, CORE_B64)
        assertTrue(r.isSuccess)
        assertEquals(1, store.deletes)
        assertEquals(1, http.urls.size)
        assertEquals(1, store.saves)
    }

    @Test
    fun staleCache_andServerRejects_isErrorAndCacheGone() {
        val store = FakeStore(validCache(savedAt = Date(now.time - 4 * DAY_MS)))
        val http = FakeHttp { LicenceHttpResponse(428, """{"message":"Licença vencida"}""") }
        val r = validator(store, http).validate(KEY, CORE_B64)
        assertFalse(r.isSuccess)
        assertEquals("Licença vencida", r.errorMessage)
        assertNull(store.entity)
        assertEquals(0, store.saves)
    }

    @Test
    fun maliciousInternalKey_alwaysCallsCore() {
        val store = FakeStore()
        val http = FakeHttp { ok() }
        validator(store, http).validate(KEY, "https://evil.com/pos-payments/licence/check")
        validator(store.also { it.entity = null }, http)
            .validate(KEY, "***REMOVIDO***")
        validator(store.also { it.entity = null }, http).validate(KEY, "")
        validator(store.also { it.entity = null }, http).validate(KEY, null)
        assertEquals(listOf(core, core, core, core), http.urls)
    }

    @Test
    fun http200WithExpiredLicence_isErrorAndNotSaved() {
        val store = FakeStore()
        val http = FakeHttp { LicenceHttpResponse(200, LicenceTestData.coreJson(isExpired = true)) }
        val r = validator(store, http).validate(KEY, CORE_B64)
        assertFalse(r.isSuccess)
        assertEquals(0, store.saves)
        assertNull(store.entity)
    }

    @Test
    fun http200Canceled_isErrorAndNotSaved() {
        val store = FakeStore()
        val http = FakeHttp { LicenceHttpResponse(200, LicenceTestData.coreJson(isCanceled = true)) }
        val r = validator(store, http).validate(KEY, CORE_B64)
        assertFalse(r.isSuccess)
        assertEquals(0, store.saves)
    }

    @Test
    fun networkErrorOrTimeout_isError() {
        val store = FakeStore()
        val r1 = validator(store, FakeHttp { throw SocketTimeoutException("timeout") }).validate(KEY, CORE_B64)
        assertFalse(r1.isSuccess)
        assertNotNull(r1.errorMessage)
        assertTrue(r1.errorMessage!!.contains("timeout"))
        val r2 = validator(store, FakeHttp { throw IOException() }).validate(KEY, CORE_B64)
        assertFalse(r2.isSuccess)
        assertNotNull(r2.errorMessage)
        assertEquals(0, store.saves)
    }

    @Test
    fun emptyLicenceKey_isErrorWithoutNetwork() {
        val http = FakeHttp { throw AssertionError("não deveria chamar a rede") }
        val r = validator(FakeStore(), http).validate("", CORE_B64)
        assertFalse(r.isSuccess)
        assertNotNull(r.errorMessage)
        assertFalse(validator(FakeStore(), http).validate(null, CORE_B64).isSuccess)
    }

    @Test
    fun cacheFromAnotherKey_isNotUsed() {
        val store = FakeStore(validCache())
        val http = FakeHttp { LicenceHttpResponse(404, """{"message":"Licença não encontrada"}""") }
        val r = validator(store, http).validate("other-key", CORE_B64)
        assertFalse(r.isSuccess)
        assertEquals("Licença não encontrada", r.errorMessage)
        assertTrue(store.deletes >= 1)
        assertNull(store.entity)
    }
}
