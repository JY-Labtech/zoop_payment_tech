import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:zoop_payment_tech/zoop_payment_tech.dart';

class FakeHandler implements IZoopHandler {
  final List<String> errors = [];
  final List<String> successes = [];

  @override
  Future<void> onError(String message) async => errors.add(message);

  @override
  Future<void> onSuccess(String message) async => successes.add(message);

  @override
  Future<void> onLoading(bool show) async {}

  @override
  Future<void> onMessage(String message) async {}

  @override
  Future<void> onQRCode(String qrCode) async {}

  @override
  Future<void> onToken(String token) async {}

  @override
  Future<void> onVoidTransactionList(List<Map<String, dynamic>> transactions) async {}
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(CHANNEL_NAME);
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  const licenceErrorMessage = 'Sua licença não foi ativada. Licença vencida';

  final calls = <MethodCall>[];
  late FakeHandler handler;

  /// Simula o nativo: o gate de licença reprova, envia onError e devolve LICENCE_ERROR.
  void nativeRejectsLicence() {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      if (call.method == 'getPlatformVersion' || call.method == 'cancelOperation') {
        return true;
      }
      await messenger.handlePlatformMessage(
        CHANNEL_NAME,
        const StandardMethodCodec().encodeMethodCall(
          const MethodCall('onError', {'message': licenceErrorMessage}),
        ),
        (_) {},
      );
      throw PlatformException(code: 'LICENCE_ERROR', message: licenceErrorMessage);
    });
  }

  void nativeAcceptsLicence() {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return true;
    });
  }

  test('payment antes de initPayment lança erro', () {
    expect(() => ZoopPaymentTech.I.payment, throwsA(anything));
  });

  group('com payment inicializado', () {
    late Payment payment;

    setUpAll(() {
      handler = FakeHandler();
      payment = ZoopPaymentTech.I.initPayment(handler: handler);
    });

    setUp(() {
      calls.clear();
      handler.errors.clear();
      handler.successes.clear();
    });

    tearDown(() => messenger.setMockMethodCallHandler(channel, null));

    test('licenceCheck aponta para o portal da JY Labtech (core)', () {
      final url = utf8.decode(base64.decode(licenceCheck));
      expect(url, 'https://portal-api.jylabtech.com.br/pos-payments/licence/check');
      final uri = Uri.parse(url);
      expect(uri.scheme, 'https');
      expect(uri.host, 'portal-api.jylabtech.com.br');
      expect(uri.path, startsWith('/pos-payments/'));
    });

    test('a licença é configurada no nativo: o Dart não envia chaves pelo canal', () async {
      nativeAcceptsLicence();
      await payment.initialize(marketplace: 'm', seller: 's', accessKey: 'a');
      expect(calls.single.method, 'initialize');
      final args = calls.single.arguments as Map;
      expect(args.containsKey('licenceKey'), isFalse);
      expect(args.containsKey('licenceInternalKey'), isFalse);
    });

    test('com licença válida as operações retornam true', () async {
      nativeAcceptsLicence();
      expect(await payment.initialize(), isTrue);
      expect(await payment.login(), isTrue);
      expect(await payment.checkZoopKey(), isTrue);
      expect(await payment.creditPayment(amount: 1000, installments: 2), isTrue);
      expect(await payment.pixPayment(amount: 1000), isTrue);
      expect(await payment.voidTransaction(), isTrue);
      expect(await payment.selectVoidTransaction('abc'), isTrue);
      expect(calls.map((c) => c.method), [
        'initialize',
        'login',
        'checkZoopKey',
        'creditPayment',
        'pixPayment',
        'voidTransaction',
        'selectVoidTransaction',
      ]);
      expect(calls[3].arguments, {'amount': 1000, 'installments': 2});
    });

    test('LICENCE_ERROR: nenhuma operação retorna sucesso e onError é chamado', () async {
      nativeRejectsLicence();
      final operations = <String, Future<bool> Function()>{
        'initialize': () => payment.initialize(),
        'login': () => payment.login(),
        'checkZoopKey': () => payment.checkZoopKey(),
        'creditPayment': () => payment.creditPayment(amount: 1000),
        'pixPayment': () => payment.pixPayment(amount: 1000),
        'voidTransaction': () => payment.voidTransaction(),
        'selectVoidTransaction': () => payment.selectVoidTransaction('abc'),
      };

      for (final entry in operations.entries) {
        handler.errors.clear();
        final result = await entry.value();
        expect(result, isFalse, reason: entry.key);
        expect(handler.errors, [licenceErrorMessage], reason: entry.key);
      }
      expect(handler.successes, isEmpty);
    });

    test('cancelOperation não depende da licença', () async {
      nativeRejectsLicence();
      expect(await payment.cancelOperation(), isTrue);
      expect(handler.errors, isEmpty);
    });
  });
}
