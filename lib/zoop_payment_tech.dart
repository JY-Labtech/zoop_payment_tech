import 'package:zoop_payment_tech/src/interface/handler/izoop_handler.dart';
import 'package:zoop_payment_tech/src/payments/payment.dart';
import 'package:zoop_payment_tech/src/zoop_payment_tech_core.dart';

export 'src/zoop_payment_tech.dart';

class ZoopPaymentTech {
  static ZoopPaymentTech? _instance;

  ZoopPaymentTech._();

  static ZoopPaymentTech get I {
    _instance ??= ZoopPaymentTech._();
    return _instance!;
  }

  /// Function to initialize payment and register the notification handler
  Payment initPayment({required IZoopHandler handler}) {
    return ZoopPaymentTechCore.I.initPayment(handler: handler);
  }

  /// Get the Payment object. Note: It needs to be initialized.
  Payment get payment {
    return ZoopPaymentTechCore.I.payment;
  }
}
