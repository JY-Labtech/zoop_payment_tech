import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:zoop_payment_tech/zoop_payment_tech.dart';

class FakeHandler implements IZoopHandler {
  final List<String> errors = [];
  final List<String> successes = [];
  final List<String> messages = [];
  final List<bool> loadings = [];
  final List<String> qrCodes = [];
  final List<String> tokens = [];
  final List<List<Map<String, dynamic>>> voidLists = [];

  void clear() {
    errors.clear();
    successes.clear();
    messages.clear();
    loadings.clear();
    qrCodes.clear();
    tokens.clear();
    voidLists.clear();
  }

  @override
  Future<void> onError(String message) async => errors.add(message);

  @override
  Future<void> onSuccess(String message) async => successes.add(message);

  @override
  Future<void> onLoading(bool show) async => loadings.add(show);

  @override
  Future<void> onMessage(String message) async => messages.add(message);

  @override
  Future<void> onQRCode(String qrCode) async => qrCodes.add(qrCode);

  @override
  Future<void> onToken(String token) async => tokens.add(token);

  @override
  Future<void> onVoidTransactionList(List<Map<String, dynamic>> transactions) async =>
      voidLists.add(transactions);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(CHANNEL_NAME);
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  final calls = <MethodCall>[];
  late FakeHandler handler;

  /// Simula o nativo respondendo `true` a todas as chamadas.
  void nativeAccepts() {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return true;
    });
  }

  /// Simula o nativo respondendo erro (ex.: SDK falhou, transação não encontrada).
  void nativeFails() {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      throw PlatformException(code: 'ERROR', message: 'falha no SDK');
    });
  }

  /// Envia um callback do nativo para o Dart, como o plugin faz com `channel.invokeMethod`.
  Future<void> nativeSends(String method, Map<String, dynamic> args) async {
    await messenger.handlePlatformMessage(
      CHANNEL_NAME,
      const StandardMethodCodec().encodeMethodCall(MethodCall(method, args)),
      (_) {},
    );
  }

  test('payment antes de initPayment lança erro', () {
    expect(() => ZoopPaymentTech.I.payment, throwsA(anything));
  });

  test('ZoopController.payment antes de initPayment lança erro', () {
    expect(() => ZoopController().payment, throwsA(isA<Exception>()));
  });

  test('nome do canal', () {
    expect(CHANNEL_NAME, 'zoop_payment_tech');
    expect(ZoopPaymentTechCore.channelName, CHANNEL_NAME);
  });

  group('com payment inicializado', () {
    late Payment payment;

    setUpAll(() {
      handler = FakeHandler();
      payment = ZoopPaymentTech.I.initPayment(handler: handler);
    });

    setUp(() {
      calls.clear();
      handler.clear();
    });

    tearDown(() => messenger.setMockMethodCallHandler(channel, null));

    test('initPayment devolve sempre a mesma instância', () {
      expect(ZoopPaymentTech.I.initPayment(handler: FakeHandler()), same(payment));
      expect(ZoopPaymentTech.I.payment, same(payment));
      expect(ZoopController().initPayment(handler: handler), same(payment));
    });

    test('initialize envia as credenciais e nenhum outro parâmetro', () async {
      nativeAccepts();
      expect(
        await payment.initialize(marketplace: 'm', seller: 's', accessKey: 'a', downloadTheme: true),
        isTrue,
      );
      expect(calls.single.method, 'initialize');
      expect(calls.single.arguments, {
        'marketplace': 'm',
        'seller': 's',
        'accessKey': 'a',
        'downloadTheme': true,
      });
    });

    test('initialize sem credenciais envia nulos', () async {
      nativeAccepts();
      expect(await payment.initialize(), isTrue);
      expect(calls.single.arguments, {
        'marketplace': null,
        'seller': null,
        'accessKey': null,
        'downloadTheme': false,
      });
    });

    test('todas as operações chamam o canal direto e retornam true', () async {
      nativeAccepts();
      expect(await payment.login(downloadTheme: true), isTrue);
      expect(await payment.checkZoopKey(), isTrue);
      expect(await payment.creditPayment(amount: 1000, installments: 2), isTrue);
      expect(await payment.creditPayment(amount: 500), isTrue);
      expect(await payment.pixPayment(amount: 1500), isTrue);
      expect(await payment.voidTransaction(), isTrue);
      expect(await payment.selectVoidTransaction('abc'), isTrue);
      expect(await payment.cancelOperation(), isTrue);

      expect(calls.map((c) => c.method), [
        'login',
        'checkZoopKey',
        'creditPayment',
        'creditPayment',
        'pixPayment',
        'voidTransaction',
        'selectVoidTransaction',
        'cancelOperation',
      ]);
      expect(calls[0].arguments, {'downloadTheme': true});
      expect(calls[1].arguments, isNull);
      expect(calls[2].arguments, {'amount': 1000, 'installments': 2});
      expect(calls[3].arguments, {'amount': 500, 'installments': 1});
      expect(calls[4].arguments, {'amount': 1500});
      expect(calls[5].arguments, isNull);
      expect(calls[6].arguments, {'transactionKey': 'abc'});
      expect(calls[7].arguments, isNull);
    });

    test('erro do nativo: as operações retornam false sem lançar exceção', () async {
      nativeFails();
      final operations = <String, Future<bool> Function()>{
        'initialize': () => payment.initialize(),
        'login': () => payment.login(),
        'checkZoopKey': () => payment.checkZoopKey(),
        'creditPayment': () => payment.creditPayment(amount: 1000),
        'pixPayment': () => payment.pixPayment(amount: 1000),
        'voidTransaction': () => payment.voidTransaction(),
        'selectVoidTransaction': () => payment.selectVoidTransaction('abc'),
        'cancelOperation': () => payment.cancelOperation(),
      };

      for (final entry in operations.entries) {
        expect(await entry.value(), isFalse, reason: entry.key);
      }
      expect(calls.map((c) => c.method), operations.keys);
      expect(handler.successes, isEmpty);
    });

    test('callbacks do nativo chegam ao handler', () async {
      await nativeSends('onSuccess', {'message': 'Pagamento realizado com sucesso'});
      await nativeSends('onError', {'message': 'Falha no pagamento'});
      await nativeSends('onMessage', {'message': 'Insira o PIN'});
      await nativeSends('onLoading', {'show': true});
      await nativeSends('onQRCode', {'qrCode': '000201...'});
      await nativeSends('onToken', {'token': '123456'});
      await nativeSends('onVoidTransactionList', {
        'transactions': [
          {'amount': '10,00', 'date': '01/01', 'time': '10:00', 'transactionKey': 'k1'},
        ],
      });

      expect(handler.successes, ['Pagamento realizado com sucesso']);
      expect(handler.errors, ['Falha no pagamento']);
      expect(handler.messages, ['Insira o PIN']);
      expect(handler.loadings, [true]);
      expect(handler.qrCodes, ['000201...']);
      expect(handler.tokens, ['123456']);
      expect(handler.voidLists.single.single['transactionKey'], 'k1');
      expect(
        ZoopVoidTransaction.fromJson(handler.voidLists.single.single).toJson(),
        {'amount': '10,00', 'date': '01/01', 'time': '10:00', 'transactionKey': 'k1'},
      );
    });

    test('callback sem argumentos usa valores padrão', () async {
      await nativeSends('onError', {});
      await nativeSends('onLoading', {});
      expect(handler.errors, ['Erro desconhecido']);
      expect(handler.loadings, [false]);
    });

    test('setHandler troca o handler que recebe os callbacks', () async {
      final other = FakeHandler();
      payment.setHandler(other);
      addTearDown(() => payment.setHandler(handler));

      await nativeSends('onSuccess', {'message': 'ok'});
      expect(other.successes, ['ok']);
      expect(handler.successes, isEmpty);
    });
  });

  group('modelos', () {
    test('ZoopCredentials fromJson/toJson', () {
      final json = {
        'marketplace': 'm',
        'seller': 's',
        'terminal': 't',
        'accessKey': 'a',
        'sellerName': 'Loja',
      };
      expect(ZoopCredentials.fromJson(json).toJson(), json);
      expect(ZoopCredentials.fromJson({}).marketplace, '');
    });

    test('ZoopResponse fromJson/toJson', () {
      final json = {'success': true, 'message': 'ok', 'data': {'a': 1}};
      expect(ZoopResponse.fromJson(json).toJson(), json);
      expect(ZoopResponse.fromJson({}).success, isFalse);
    });
  });
}
