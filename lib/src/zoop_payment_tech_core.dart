import 'package:flutter/services.dart';
import 'package:zoop_payment_tech/src/interface/handler/izoop_handler.dart';

import 'payments/payment.dart';

class ZoopPaymentTechCore {
  static const channelName = "zoop_payment_tech";
  final MethodChannel _channel;
  Payment? _payment;

  static ZoopPaymentTechCore? _instance;

  ZoopPaymentTechCore(this._channel);

  static ZoopPaymentTechCore get I {
    _instance ??= ZoopPaymentTechCore(const MethodChannel(channelName));
    return _instance!;
  }

  /// Function to initialize payment and register the notification handler
  Payment initPayment({
    required IZoopHandler handler,
  }) {
    if (_payment != null) {
      return _payment!;
    }
    _payment = Payment.create(
      channel: _channel,
      paymentHandler: handler,
    );
    return _payment!;
  }

  /// Get the Payment object. Note: It needs to be initialized.
  Payment get payment {
    if (_payment == null) {
      throw "PAYMENT NEEDS TO BE INITIALIZED! \n TRY: ZoopPaymentTechCore.I.initPayment(handler)";
    }
    return _payment!;
  }
}

