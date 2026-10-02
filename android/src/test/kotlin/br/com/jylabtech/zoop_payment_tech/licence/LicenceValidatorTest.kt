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

    private val now = Date()
    private val core = LicenceConfig.LICENCE_CHECK_URL

    private fun validator(store: FakeStore, http: FakeHttp) =
        LicenceValidator(store, http, { Base64.getDecoder().decode(it) }, { now })

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
    fun validCache_noNetwork() {
        val store = FakeStore(validCache())
        val http = FakeHttp { throw AssertionError("não deveria chamar a rede") }
        val r = validator(store, http).validate(KEY, CORE_B64)
        assertTrue(r.isSuccess)
        assertTrue(http.urls.isEmpty())
    }

    @Test
    fun staleCache_isDeletedAndRevalidatedOnline() {
        val store = FakeStore(validCache(savedAt = Date(now.time - 8 * DAY_MS)))
        val http = FakeHttp { ok() }
        val r = validator(store, http).validate(KEY, CORE_B64)
        assertTrue(r.isSuccess)
        assertEquals(1, store.deletes)
        assertEquals(1, http.urls.size)
        assertEquals(1, store.saves)
    }

    @Test
    fun staleCache_andServerRejects_isErrorAndCacheGone() {
        val store = FakeStore(validCache(savedAt = Date(now.time - 8 * DAY_MS)))
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
        assertEquals(1, store.deletes)
    }
}
