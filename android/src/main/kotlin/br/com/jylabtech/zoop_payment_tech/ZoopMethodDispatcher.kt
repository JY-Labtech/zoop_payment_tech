package br.com.jylabtech.zoop_payment_tech

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Operações expostas pelo canal `zoop_payment_tech`. O [ZoopPaymentTechPlugin] implementa
 * cada uma chamando o SDK da Zoop; nos testes JVM elas são substituídas por um fake.
 */
internal interface ZoopOperations {
    fun platformVersion(): String
    fun initialize(call: MethodCall, result: MethodChannel.Result)
    fun login(call: MethodCall, result: MethodChannel.Result)
    fun checkZoopKey(result: MethodChannel.Result)
    fun creditPayment(call: MethodCall, result: MethodChannel.Result)
    fun pixPayment(call: MethodCall, result: MethodChannel.Result)
    fun voidTransaction(result: MethodChannel.Result)
    fun selectVoidTransaction(call: MethodCall, result: MethodChannel.Result)
    fun cancelOperation(result: MethodChannel.Result)

    /** Erro inesperado ao executar uma operação: o plugin registra no log e envia `onError`. */
    fun reportError(message: String, error: Exception)
}

/**
 * Roteia cada chamada do canal para a operação correspondente, na thread em que a chamada
 * chegou (main thread). Cada chamada recebe exatamente uma resposta no `result`: a da
 * operação, `notImplemented()` para métodos desconhecidos ou `error("ERROR", ...)` se a
 * operação lançar exceção.
 */
internal class ZoopMethodDispatcher(private val operations: ZoopOperations) {

    companion object {
        /** Métodos aceitos pelo canal. */
        val METHODS: Set<String> = setOf(
            "getPlatformVersion",
            "initialize",
            "login",
            "checkZoopKey",
            "creditPayment",
            "pixPayment",
            "voidTransaction",
            "selectVoidTransaction",
            "cancelOperation"
        )
    }

    fun dispatch(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "getPlatformVersion" -> result.success(operations.platformVersion())
                "initialize" -> operations.initialize(call, result)
                "login" -> operations.login(call, result)
                "checkZoopKey" -> operations.checkZoopKey(result)
                "creditPayment" -> operations.creditPayment(call, result)
                "pixPayment" -> operations.pixPayment(call, result)
                "voidTransaction" -> operations.voidTransaction(result)
                "selectVoidTransaction" -> operations.selectVoidTransaction(call, result)
                "cancelOperation" -> operations.cancelOperation(result)
                else -> result.notImplemented()
            }
        } catch (e: Exception) {
            operations.reportError("Erro: ${e.message}", e)
            result.error("ERROR", e.message, null)
        }
    }
}
