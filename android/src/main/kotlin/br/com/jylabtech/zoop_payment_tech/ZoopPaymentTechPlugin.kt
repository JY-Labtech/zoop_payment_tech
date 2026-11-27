package br.com.jylabtech.zoop_payment_tech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.zoop.pos.Zoop
import com.zoop.pos.collection.UserInput
import com.zoop.pos.collection.UserSelection
import com.zoop.pos.collection.VoidTransaction
import com.zoop.pos.exception.ZoopRequestCanceledException
import com.zoop.pos.plugin.DashboardConfirmationResponse
import com.zoop.pos.plugin.DashboardThemeResponse
import com.zoop.pos.plugin.DashboardTokenResponse
import com.zoop.pos.plugin.ZoopFoundationPlugin
import com.zoop.pos.plugin.smartpos.SmartPOSPlugin
import com.zoop.pos.plugin.smartpos.requestBuilder.*
import com.zoop.pos.requestfield.MessageCallbackRequestField
import com.zoop.pos.requestfield.PinCallbackRequestField
import com.zoop.pos.requestfield.QRCodeCallbackRequestField
import com.zoop.pos.requestfield.TransactionIdCallbackRequestField
import com.zoop.pos.terminal.Terminal
import com.zoop.pos.type.Callback
import com.zoop.pos.type.Environment
import com.zoop.pos.type.LogLevel
import com.zoop.pos.type.Option
import com.zoop.pos.type.Request
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

class ZoopPaymentTechPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private var channel: MethodChannel? = null
    private var context: Context? = null
    private var paymentRequest: Request? = null
    private var voidRequest: Request? = null
    private var loginRequest: Request? = null
    private var pixRequest: Request? = null
    private var voidTransaction: UserSelection<VoidTransaction>? = null

    private val TAG = "ZoopPaymentTechPlugin"
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val dispatcher = ZoopMethodDispatcher(object : ZoopOperations {
        override fun platformVersion() = "Android ${android.os.Build.VERSION.RELEASE}"
        override fun initialize(call: MethodCall, result: MethodChannel.Result) = handleInitialize(call, result)
        override fun login(call: MethodCall, result: MethodChannel.Result) = handleLogin(call, result)
        override fun checkZoopKey(result: MethodChannel.Result) = handleCheckZoopKey(result)
        override fun creditPayment(call: MethodCall, result: MethodChannel.Result) = handleCreditPayment(call, result)
        override fun pixPayment(call: MethodCall, result: MethodChannel.Result) = handlePixPayment(call, result)
        override fun voidTransaction(result: MethodChannel.Result) = handleVoidTransaction(result)
        override fun selectVoidTransaction(call: MethodCall, result: MethodChannel.Result) =
            handleSelectVoidTransaction(call, result)
        override fun cancelOperation(result: MethodChannel.Result) = handleCancelOperation(result)
        override fun reportError(message: String, error: Exception) {
            Log.e(TAG, message, error)
            sendError(message)
        }
    })

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "zoop_payment_tech")
        context = flutterPluginBinding.applicationContext
        channel?.setMethodCallHandler(this)

        // Initialize Zoop SDK
        context?.let { initializeZoop(it) }
    }

    // As chamadas chegam na main thread e vão direto para o SDK da Zoop (ver ZoopMethodDispatcher).
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        dispatcher.dispatch(call, result)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        context = null
        terminateZoop()
    }

    private fun initializeZoop(context: Context) {
        Zoop.initialize(context) {
            // Configuração inicial sem credenciais
        }
        Zoop.setEnvironment(Environment.Production)
        Zoop.setLogLevel(LogLevel.Trace)
        Zoop.setStrict(false)
        Zoop.setTimeout(15 * 1000L)
        Zoop.findPlugin<SmartPOSPlugin>() ?: Zoop.plug(SmartPOSPlugin(Zoop.constructorParameters()))
    }

    private fun initializeZoopWithCredentials(
        context: Context,
        marketplace: String,
        seller: String,
        accessKey: String
    ) {
        Zoop.initialize(context) {
            credentials {
                this.marketplace = marketplace
                this.seller = seller
                this.accessKey = accessKey
            }
        }
        Zoop.setEnvironment(Environment.Production)
        Zoop.setLogLevel(LogLevel.Trace)
        Zoop.setStrict(false)
        Zoop.setTimeout(15 * 1000L)
        Zoop.findPlugin<SmartPOSPlugin>() ?: Zoop.plug(SmartPOSPlugin(Zoop.constructorParameters()))
    }

    private fun terminateZoop() {
        Zoop.findPlugin<SmartPOSPlugin>()?.run(Zoop::unplug)
    }

    private fun handleInitialize(call: MethodCall, result: MethodChannel.Result) {
        val marketplace = call.argument<String>("marketplace")
        val seller = call.argument<String>("seller")
        val accessKey = call.argument<String>("accessKey")

        context?.let { ctx ->
            if (marketplace != null && seller != null && accessKey != null) {
                initializeZoopWithCredentials(ctx, marketplace, seller, accessKey)
            } else {
                initializeZoop(ctx)
            }
            result.success(true)
        } ?: run {
            result.error("ERROR", "Context not available", null)
        }
    }

    private fun handleLogin(call: MethodCall, result: MethodChannel.Result) {
        val downloadTheme = call.argument<Boolean>("downloadTheme") ?: false

        loginRequest = ZoopFoundationPlugin.createDashboardActivationRequestBuilder()
            .downloadTheme(downloadTheme)
            .tokenCallback(object : Callback<DashboardTokenResponse>() {
                override fun onStart() {
                    sendMessage("Requisitando token")
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Falha ao requisitar token: ${error.message}")
                    sendError("Falha ao requisitar token")
                }

                override fun onSuccess(response: DashboardTokenResponse) {
                    Log.d(TAG, "Token recebido")
                    sendToken(response.token)
                }
            })
            .confirmCallback(object : Callback<DashboardConfirmationResponse>() {
                override fun onFail(error: Throwable) {
                    val message = when (error) {
                        is ZoopRequestCanceledException -> "Operação cancelada"
                        else -> error.message ?: "Erro desconhecido"
                    }
                    sendError(message)
                }

                override fun onSuccess(response: DashboardConfirmationResponse) {
                    Log.d(TAG, "Login realizado com sucesso")
                    Log.d(TAG, "MarketplaceId: ${response.credentials.marketplace}")
                    Log.d(TAG, "SellerId: ${response.credentials.seller}")
                    Log.d(TAG, "Terminal: ${response.credentials.terminal}")
                    Log.d(TAG, "SellerName: ${response.owner.name}")

                    sendSuccess("Login realizado: ${response.owner.name}")
                    
                    // Reinicializar com credenciais
                    context?.let { ctx ->
                        initializeZoopWithCredentials(
                            ctx,
                            response.credentials.marketplace,
                            response.credentials.seller,
                            response.credentials.accessKey
                        )
                    }
                }

                override fun onComplete() {
                    runBlocking { delay(2000) }
                }
            })
            .themeCallback(object : Callback<DashboardThemeResponse>() {
                override fun onStart() {
                    if (loginRequest?.isCancelRequested == true) return
                    sendMessage("Baixando temas")
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Erro ao baixar temas: ${error.message}")
                    sendError(error.message ?: "Erro ao baixar temas")
                }

                override fun onSuccess(response: DashboardThemeResponse) {
                    Log.d(TAG, "Temas baixados com sucesso")
                    sendMessage("Login realizado")
                }

                override fun onComplete() {
                    runBlocking { delay(2000) }
                }
            })
            .build()

        Zoop.post(loginRequest!!)
        result.success(true)
    }

    private fun handleCheckZoopKey(result: MethodChannel.Result) {
        SmartPOSPlugin.createZoopKeyValidationRequestBuilder()
            .callback(object : Callback<SmartPOSZoopKeyValidationResponse>() {
                override fun onFail(error: Throwable) {
                    sendError(error.message ?: "Erro ao verificar chave")
                }

                override fun onSuccess(response: SmartPOSZoopKeyValidationResponse) {
                    val message = if (response.hasKey) {
                        "Chave Zoop encontrada"
                    } else {
                        "Chave ausente, necessário envio para PAX para gravação de chave"
                    }
                    sendMessage(message)
                }

                override fun onComplete() {
                    runBlocking { delay(2000) }
                }
            })
            .build()
            .run { Zoop.post(this) }
        
        result.success(true)
    }

    private fun handleCreditPayment(call: MethodCall, result: MethodChannel.Result) {
        val amount = call.argument<Int>("amount") ?: 0
        val installments = call.argument<Int>("installments") ?: 1

        paymentRequest = SmartPOSPlugin.createPaymentRequestBuilder()
            .amount(amount)
            .option(Option.CREDIT)
            .installments(installments)
            .callback(object : Callback<SmartPOSPaymentResponse>() {
                override fun onStart() {
                    sendMessage("Iniciando pagamento")
                    sendLoading(true)
                }

                override fun onSuccess(response: SmartPOSPaymentResponse) {
                    sendSuccess("Pagamento realizado com sucesso")
                    sendLoading(false)
                }

                override fun onFail(error: Throwable) {
                    val message = if (error.message?.contains("invalid session") == true) {
                        "Não foi realizado um login"
                    } else {
                        error.message ?: "Falha no pagamento"
                    }
                    sendError(message)
                    sendLoading(false)
                }

                override fun onComplete() {
                    runBlocking { delay(2000) }
                    sendLoading(false)
                }
            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    sendMessage(response.message)
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Erro em messageCallback: ${error.message}")
                }
            })
            .pinCallback(object : Callback<PinCallbackRequestField.PinData>() {
                override fun onSuccess(response: PinCallbackRequestField.PinData) {
                    when (response.type) {
                        Terminal.PinEventHandler.EventType.Start -> 
                            sendMessage("Insira o PIN")
                        Terminal.PinEventHandler.EventType.Finish -> 
                            sendMessage("PIN processado")
                        Terminal.PinEventHandler.EventType.Inserted -> 
                            sendMessage("Cartão inserido")
                        Terminal.PinEventHandler.EventType.Removed -> 
                            sendMessage("Cartão removido")
                        else -> {}
                    }
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Erro em pinCallback: ${error.message}")
                }
            })
            .build()

        Zoop.post(paymentRequest!!)
        result.success(true)
    }

    private fun handlePixPayment(call: MethodCall, result: MethodChannel.Result) {
        val amount = call.argument<Int>("amount") ?: 0

        pixRequest = SmartPOSPlugin.createPixPaymentRequestBuilder()
            .amount(amount.toLong())
            .callback(object : Callback<SmartPOSPixPaymentResponse>() {
                override fun onStart() {
                    sendMessage("Iniciando pagamento PIX")
                    sendLoading(true)
                }

                override fun onSuccess(response: SmartPOSPixPaymentResponse) {
                    sendSuccess("Pagamento PIX realizado com sucesso")
                    sendLoading(false)
                }

                override fun onFail(error: Throwable) {
                    val message = if (error.message?.contains("invalid session") == true) {
                        "Não foi realizado um login"
                    } else {
                        error.message ?: "Falha no pagamento PIX"
                    }
                    sendError(message)
                    sendLoading(false)
                }

                override fun onComplete() {
                    runBlocking { delay(2000) }
                    sendLoading(false)
                }
            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    sendMessage(response.message)
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Erro em messageCallback PIX: ${error.message}")
                }
            })
            .qrCodeCallback(object : Callback<QRCodeCallbackRequestField.QRCodeData>() {
                override fun onSuccess(response: QRCodeCallbackRequestField.QRCodeData) {
                    sendQRCode(response.data)
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Erro em qrCodeCallback: ${error.message}")
                }
            })
            .transactionIdCallback(object : Callback<TransactionIdCallbackRequestField.transactionIdData>() {
                override fun onSuccess(response: TransactionIdCallbackRequestField.transactionIdData) {
                    Log.d(TAG, "Transaction ID: ${response.transactionId}")
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Erro em transactionIdCallback: ${error.message}")
                }
            })
            .build()

        Zoop.post(pixRequest!!)
        result.success(true)
    }

    private fun handleVoidTransaction(result: MethodChannel.Result) {
        voidRequest = SmartPOSPlugin.createVoidRequestBuilder()
            .callback(object : Callback<SmartPOSVoidResponse>() {
                override fun onStart() {
                    sendMessage("Processando cancelamento")
                    sendLoading(true)
                }

                override fun onSuccess(response: SmartPOSVoidResponse) {
                    sendSuccess("Cancelamento realizado com sucesso")
                    voidTransaction = null
                    sendLoading(false)
                }

                override fun onFail(error: Throwable) {
                    sendError(error.message ?: "Falha na operação de cancelamento")
                    voidTransaction = null
                    sendLoading(false)
                }

                override fun onComplete() {
                    runBlocking { delay(2000) }
                    sendLoading(false)
                }
            })
            .voidTransactionCallback(object : Callback<UserSelection<VoidTransaction>>() {
                override fun onSuccess(response: UserSelection<VoidTransaction>) {
                    voidTransaction = response
                    val transactionsList = response.items.map { transaction ->
                        mapOf(
                            "amount" to transaction.amount,
                            "date" to transaction.date,
                            "time" to transaction.time,
                            "transactionKey" to transaction.transactionKey
                        )
                    }
                    sendVoidTransactionList(transactionsList)
                }

                override fun onFail(error: Throwable) {
                    sendError(error.message ?: "Erro ao buscar transações")
                }
            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    sendMessage(response.message)
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Erro em messageCallback void: ${error.message}")
                }
            })
            .build()

        Zoop.post(voidRequest!!)
        result.success(true)
    }

    private fun handleSelectVoidTransaction(call: MethodCall, result: MethodChannel.Result) {
        val transactionKey = call.argument<String>("transactionKey")
        
        if (transactionKey != null && voidTransaction != null) {
            val transaction = voidTransaction!!.items.find { it.transactionKey == transactionKey }
            transaction?.let {
                voidTransaction!!.select(it)
                result.success(true)
            } ?: run {
                result.error("ERROR", "Transaction not found", null)
            }
        } else {
            result.error("ERROR", "Transaction key required or void transaction not initialized", null)
        }
    }

    private fun handleCancelOperation(result: MethodChannel.Result) {
        paymentRequest?.cancel()
        pixRequest?.cancel()
        voidTransaction?.cancel()
        voidRequest?.cancel()
        loginRequest?.cancel()
        result.success(true)
    }

    // Helper methods to send callbacks to Flutter

    /**
     * Os callbacks do SDK Zoop podem chegar em threads de fundo, mas o MethodChannel só pode
     * ser usado na main thread: todo envio ao Dart passa por aqui.
     */
    private fun invokeOnMain(method: String, arguments: Any?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            channel?.invokeMethod(method, arguments)
        } else {
            mainHandler.post { channel?.invokeMethod(method, arguments) }
        }
    }

    private fun sendSuccess(message: String) {
        invokeOnMain("onSuccess", mapOf("message" to message))
    }

    private fun sendError(message: String) {
        invokeOnMain("onError", mapOf("message" to message))
    }

    private fun sendMessage(message: String) {
        invokeOnMain("onMessage", mapOf("message" to message))
    }

    private fun sendLoading(show: Boolean) {
        invokeOnMain("onLoading", mapOf("show" to show))
    }

    private fun sendQRCode(qrCode: String) {
        invokeOnMain("onQRCode", mapOf("qrCode" to qrCode))
    }

    private fun sendToken(token: String) {
        invokeOnMain("onToken", mapOf("token" to token))
    }

    private fun sendVoidTransactionList(transactions: List<Map<String, Any>>) {
        invokeOnMain("onVoidTransactionList", mapOf("transactions" to transactions))
    }
}

