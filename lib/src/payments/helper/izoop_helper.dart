import 'package:flutter/services.dart';
import 'package:zoop_payment_tech/src/interface/handler/izoop_handler.dart';

class IZoopHelper {
  static Future<void> callHandler({
    required MethodCall call,
    required IZoopHandler iZoopHandler,
  }) async {
    try {
      switch (call.method) {
        case 'onSuccess':
          final message = call.arguments['message'] as String? ?? '';
          await iZoopHandler.onSuccess(message);
          break;

        case 'onError':
          final message = call.arguments['message'] as String? ?? 'Erro desconhecido';
          await iZoopHandler.onError(message);
          break;

        case 'onMessage':
          final message = call.arguments['message'] as String? ?? '';
          await iZoopHandler.onMessage(message);
          break;

        case 'onLoading':
          final show = call.arguments['show'] as bool? ?? false;
          await iZoopHandler.onLoading(show);
          break;

        case 'onQRCode':
          final qrCode = call.arguments['qrCode'] as String? ?? '';
          await iZoopHandler.onQRCode(qrCode);
          break;

        case 'onToken':
          final token = call.arguments['token'] as String? ?? '';
          await iZoopHandler.onToken(token);
          break;

        case 'onVoidTransactionList':
          final transactions = call.arguments['transactions'] as List<dynamic>? ?? [];
          final transactionList = transactions
              .map((e) => e as Map<String, dynamic>)
              .toList();
          await iZoopHandler.onVoidTransactionList(transactionList);
          break;

        default:
          break;
      }
    } catch (e) {
      await iZoopHandler.onError('Erro ao processar callback: $e');
    }
  }
}

