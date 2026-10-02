package br.com.jylabtech.zoop_payment_tech.licence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class LicenceConfigTest {

    private val decoder: (String) -> ByteArray = { Base64.getDecoder().decode(it) }
    private val core = LicenceConfig.LICENCE_CHECK_URL

    private fun b64(s: String) = Base64.getEncoder().encodeToString(s.toByteArray())
    private fun resolve(v: String?) = LicenceConfig.resolveLicenceUrl(v, decoder)

    @Test
    fun coreUrlIsTheOfficialEndpoint() {
        assertEquals("https://portal-api.jylabtech.com.br/pos-payments/licence/check", core)
    }

    @Test
    fun acceptsCorePlainText() {
        assertEquals(core, resolve(core))
    }

    @Test
    fun acceptsCoreBase64() {
        assertEquals(
            core,
            resolve("aHR0cHM6Ly9wb3J0YWwtYXBpLmp5bGFidGVjaC5jb20uYnIvcG9zLXBheW1lbnRzL2xpY2VuY2UvY2hlY2s=")
        )
    }

    @Test
    fun acceptsInvoiceAndSubscriptionVariants() {
        val invoice = "$core/invoice"
        val subscription = "$core/subscription"
        assertEquals(invoice, resolve(invoice))
        assertEquals(invoice, resolve(b64(invoice)))
        assertEquals(subscription, resolve(subscription))
        assertEquals(subscription, resolve(b64(subscription)))
    }

    @Test
    fun oldCloudRunUrlFallsBackToCore() {
        val old = "https://pos-payments-api-577464235408.southamerica-east1.run.app/pos-payments/licence/check"
        assertEquals(core, resolve(old))
        assertEquals(core, resolve(b64(old)))
        // valor antigo documentado no README
        assertEquals(
            core,
            resolve("aHR0cHM6Ly9wb3MtcGF5bWVudHMtYXBpLTU3NzQ2NDIzNTQwOC5zb3V0aGFtZXJpY2EtZWFzdDEucnVuLmFwcC9wb3MtcGF5bWVudHMvbGljZW5jZS9jaGVjaw==")
        )
    }

    @Test
    fun otherHostFallsBackToCore() {
        assertEquals(core, resolve("https://evil.com/pos-payments/licence/check"))
        assertEquals(core, resolve(b64("https://evil.com/pos-payments/licence/check")))
    }

    @Test
    fun maliciousHostSuffixFallsBackToCore() {
        assertEquals(core, resolve("https://portal-api.jylabtech.com.br.evil.com/pos-payments/licence/check"))
        assertEquals(core, resolve("https://evil.com/portal-api.jylabtech.com.br/pos-payments/licence/check"))
    }

    @Test
    fun userInfoFallsBackToCore() {
        assertEquals(core, resolve("https://portal-api.jylabtech.com.br@evil.com/pos-payments/licence/check"))
        assertEquals(core, resolve("https://user@portal-api.jylabtech.com.br/pos-payments/licence/check"))
    }

    @Test
    fun plainHttpFallsBackToCore() {
        assertEquals(core, resolve("http://portal-api.jylabtech.com.br/pos-payments/licence/check"))
    }

    @Test
    fun otherPathOrPortFallsBackToCore() {
        assertEquals(core, resolve("https://portal-api.jylabtech.com.br/other"))
        assertEquals(core, resolve("https://portal-api.jylabtech.com.br:8443/pos-payments/licence/check"))
    }

    @Test
    fun emptyAndNullFallBackToCore() {
        assertEquals(core, resolve(null))
        assertEquals(core, resolve(""))
        assertEquals(core, resolve("   "))
        assertNull(LicenceConfig.acceptedLicenceUrl(null, decoder))
    }

    @Test
    fun garbageBase64FallsBackToCore() {
        assertEquals(core, resolve("%%%not-base64%%%"))
        assertEquals(core, resolve(b64("not a url at all")))
        assertNull(LicenceConfig.acceptedLicenceUrl("%%%not-base64%%%", decoder))
    }
}
