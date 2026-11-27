package br.com.jylabtech.zoop_payment_tech

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZoopMethodDispatcherTest {

    /** Guarda cada resposta enviada ao Flutter. */
    private class RecordingResult : MethodChannel.Result {
        val responses = mutableListOf<String>()
        var successValue: Any? = null
        var errorCode: String? = null
        var errorMessage: String? = null

        override fun success(result: Any?) {
            responses += "success"
            successValue = result
        }

        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
            responses += "error"
            this.errorCode = errorCode
            this.errorMessage = errorMessage
        }

        override fun notImplemented() {
            responses += "notImplemented"
        }
    }

    /** Simula o SDK da Zoop: registra a operação chamada e responde `true`. */
    private class FakeOperations : ZoopOperations {
        val calls = mutableListOf<String>()
        val calledWith = mutableListOf<MethodCall>()
        val reportedErrors = mutableListOf<String>()
        var throwOn: String? = null

        private fun run(name: String, call: MethodCall?, result: MethodChannel.Result) {
            calls += name
            call?.let { calledWith += it }
            if (throwOn == name) throw IllegalStateException("falha no SDK")
            result.success(true)
        }

        override fun platformVersion(): String {
            calls += "getPlatformVersion"
            return "Android 13"
        }

        override fun initialize(call: MethodCall, result: MethodChannel.Result) = run("initialize", call, result)
        override fun login(call: MethodCall, result: MethodChannel.Result) = run("login", call, result)
        override fun checkZoopKey(result: MethodChannel.Result) = run("checkZoopKey", null, result)
        override fun creditPayment(call: MethodCall, result: MethodChannel.Result) = run("creditPayment", call, result)
        override fun pixPayment(call: MethodCall, result: MethodChannel.Result) = run("pixPayment", call, result)
        override fun voidTransaction(result: MethodChannel.Result) = run("voidTransaction", null, result)
        override fun selectVoidTransaction(call: MethodCall, result: MethodChannel.Result) =
            run("selectVoidTransaction", call, result)
        override fun cancelOperation(result: MethodChannel.Result) = run("cancelOperation", null, result)

        override fun reportError(message: String, error: Exception) {
            reportedErrors += message
        }
    }

    private lateinit var operations: FakeOperations
    private lateinit var dispatcher: ZoopMethodDispatcher

    @Before
    fun setUp() {
        operations = FakeOperations()
        dispatcher = ZoopMethodDispatcher(operations)
    }

    private fun dispatch(method: String, arguments: Any? = null): RecordingResult {
        val result = RecordingResult()
        dispatcher.dispatch(MethodCall(method, arguments), result)
        return result
    }

    @Test
    fun `cada metodo chama a operacao correspondente direto e responde uma vez`() {
        val operationMethods = ZoopMethodDispatcher.METHODS - "getPlatformVersion"
        for (method in operationMethods) {
            operations.calls.clear()
            val result = dispatch(method)
            assertEquals(method, listOf(method), operations.calls)
            assertEquals(method, listOf("success"), result.responses)
            assertEquals(method, true, result.successValue)
        }
        assertTrue(operations.reportedErrors.isEmpty())
    }

    @Test
    fun `lista de metodos cobre todas as operacoes do canal`() {
        assertEquals(
            setOf(
                "getPlatformVersion", "initialize", "login", "checkZoopKey", "creditPayment",
                "pixPayment", "voidTransaction", "selectVoidTransaction", "cancelOperation"
            ),
            ZoopMethodDispatcher.METHODS
        )
    }

    @Test
    fun `getPlatformVersion devolve a versao do Android`() {
        val result = dispatch("getPlatformVersion")
        assertEquals(listOf("success"), result.responses)
        assertEquals("Android 13", result.successValue)
    }

    @Test
    fun `argumentos chegam intactos na operacao`() {
        val args = mapOf("amount" to 1000, "installments" to 3)
        dispatch("creditPayment", args)
        dispatch("pixPayment", mapOf("amount" to 500))
        dispatch("selectVoidTransaction", mapOf("transactionKey" to "abc"))
        dispatch("initialize", mapOf("marketplace" to "m", "seller" to "s", "accessKey" to "k"))
        dispatch("login", mapOf("downloadTheme" to true))

        assertEquals(1000, operations.calledWith[0].argument<Int>("amount"))
        assertEquals(3, operations.calledWith[0].argument<Int>("installments"))
        assertEquals(500, operations.calledWith[1].argument<Int>("amount"))
        assertEquals("abc", operations.calledWith[2].argument<String>("transactionKey"))
        assertEquals("m", operations.calledWith[3].argument<String>("marketplace"))
        assertEquals(true, operations.calledWith[4].argument<Boolean>("downloadTheme"))
    }

    @Test
    fun `initialize nao envia nem exige chaves extras`() {
        val args = mapOf("marketplace" to "m", "seller" to "s", "accessKey" to "k", "downloadTheme" to false)
        dispatch("initialize", args)
        assertSame(args, operations.calledWith.single().arguments)
    }

    @Test
    fun `metodo desconhecido responde notImplemented sem chamar operacao`() {
        val result = dispatch("debitPayment")
        assertEquals(listOf("notImplemented"), result.responses)
        assertTrue(operations.calls.isEmpty())
    }

    @Test
    fun `excecao na operacao vira ERROR e onError`() {
        operations.throwOn = "creditPayment"
        val result = dispatch("creditPayment", mapOf("amount" to 100))
        assertEquals(listOf("error"), result.responses)
        assertEquals("ERROR", result.errorCode)
        assertEquals("falha no SDK", result.errorMessage)
        assertEquals(listOf("Erro: falha no SDK"), operations.reportedErrors)
        assertNull(result.successValue)
    }
}
