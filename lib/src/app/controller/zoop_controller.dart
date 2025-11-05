import 'package:zoop_payment_tech/src/zoop_payment_tech_core.dart';
import 'package:zoop_payment_tech/src/interface/handler/izoop_handler.dart';
import 'package:zoop_payment_tech/src/payments/payment.dart';

class ZoopController {
  Payment? _payment;

  /// Initialize payment with handler
  Payment initPayment({required IZoopHandler handler}) {
    _payment = ZoopPaymentTechCore.I.initPayment(handler: handler);
    return _payment!;
  }

  /// Get payment instance
  Payment get payment {
    if (_payment == null) {
      throw Exception('Payment not initialized. Call initPayment() first.');
    }
    return _payment!;
  }
}

