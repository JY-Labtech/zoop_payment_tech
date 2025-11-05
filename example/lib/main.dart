import 'package:flutter/material.dart';
import 'package:zoop_payment_tech/zoop_payment_tech.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Zoop Payment Tech Example',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const PaymentPage(),
    );
  }
}

class PaymentPage extends StatefulWidget {
  const PaymentPage({super.key});

  @override
  State<PaymentPage> createState() => _PaymentPageState();
}

class _PaymentPageState extends State<PaymentPage> implements IZoopHandler {
  late Payment _payment;
  String _statusMessage = '';
  bool _isLoading = false;
  String? _qrCode;
  String? _token;

  @override
  void initState() {
    super.initState();
    // Inicializar o plugin com o handler
    _payment = ZoopPaymentTech.I.initPayment(handler: this);
  }

  Future<void> _login() async {
    setState(() {
      _statusMessage = 'Iniciando login...';
      _isLoading = true;
    });
    await _payment.login();
  }

  Future<void> _checkZoopKey() async {
    setState(() {
      _statusMessage = 'Verificando chave...';
      _isLoading = true;
    });
    await _payment.checkZoopKey();
  }

  Future<void> _makePayment() async {
    setState(() {
      _statusMessage = 'Iniciando pagamento...';
      _isLoading = true;
    });
    await _payment.creditPayment(amount: 500); // R$ 5,00
  }

  Future<void> _makePixPayment() async {
    setState(() {
      _statusMessage = 'Iniciando pagamento PIX...';
      _isLoading = true;
    });
    await _payment.pixPayment(amount: 500); // R$ 5,00
  }

  Future<void> _voidTransaction() async {
    setState(() {
      _statusMessage = 'Carregando transações...';
      _isLoading = true;
    });
    await _payment.voidTransaction();
  }

  Future<void> _cancelOperation() async {
    await _payment.cancelOperation();
    setState(() {
      _statusMessage = 'Operação cancelada';
      _isLoading = false;
    });
  }

  @override
  Future<void> onSuccess(String message) async {
    setState(() {
      _statusMessage = '✅ $message';
      _isLoading = false;
    });
  }

  @override
  Future<void> onError(String message) async {
    setState(() {
      _statusMessage = '❌ Erro: $message';
      _isLoading = false;
    });
  }

  @override
  Future<void> onMessage(String message) async {
    setState(() {
      _statusMessage = message;
    });
  }

  @override
  Future<void> onLoading(bool show) async {
    setState(() {
      _isLoading = show;
    });
  }

  @override
  Future<void> onQRCode(String qrCode) async {
    setState(() {
      _qrCode = qrCode;
    });
  }

  @override
  Future<void> onToken(String token) async {
    setState(() {
      _token = token;
      _statusMessage = 'Token gerado: $token\nInsira no dashboard para continuar.';
    });
  }

  @override
  Future<void> onVoidTransactionList(List<Map<String, dynamic>> transactions) async {
    setState(() {
      _statusMessage = '${transactions.length} transação(ões) encontrada(s)';
      _isLoading = false;
    });
    // Aqui você pode mostrar uma lista de transações para o usuário selecionar
    // Exemplo: await _payment.selectVoidTransaction(transactionKey);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('Zoop Payment Tech'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (_isLoading)
              const Padding(
                padding: EdgeInsets.all(16.0),
                child: Center(child: CircularProgressIndicator()),
              ),
            if (_statusMessage.isNotEmpty)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Text(
                    _statusMessage,
                    style: const TextStyle(fontSize: 16),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            if (_token != null)
              Card(
                color: Colors.amber[50],
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      const Text(
                        'Token para ativação:',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _token!,
                        style: const TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 2,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            if (_qrCode != null)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      const Text('QR Code PIX:'),
                      const SizedBox(height: 8),
                      // Aqui você pode usar um pacote para exibir QR Code
                      // Por exemplo: qr_flutter
                      Container(
                        width: 200,
                        height: 200,
                        color: Colors.grey[300],
                        child: const Center(
                          child: Text('QR Code aqui\n(use pacote qr_flutter)'),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _isLoading ? null : _login,
              child: const Text('Login/Ativar Terminal'),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _isLoading ? null : _checkZoopKey,
              child: const Text('Verificar Chave Zoop'),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _isLoading ? null : _makePayment,
              child: const Text('Pagamento Crédito (R\$ 5,00)'),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _isLoading ? null : _makePixPayment,
              child: const Text('Pagamento PIX (R\$ 5,00)'),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: _isLoading ? null : _voidTransaction,
              child: const Text('Cancelar Transação'),
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: _isLoading ? _cancelOperation : null,
              child: const Text('Cancelar Operação Atual'),
            ),
          ],
        ),
      ),
    );
  }
}
