import 'dart:developer';

import 'package:flutter/services.dart';
import 'package:zoop_payment_tech/src/interface/handler/izoop_handler.dart';

import 'helper/izoop_helper.dart';

final class Payment {
  final MethodChannel channel;
  IZoopHandler paymentHandler;

  static Payment? _instance;

  Payment._({
    required this.channel,
    required this.paymentHandler,
  });

  static Payment create({
    required MethodChannel channel,
    required IZoopHandler paymentHandler,
  }) {
    if (_instance != null) {
      return _instance!;
    }

    final payment = Payment._(
      channel: channel,
      paymentHandler: paymentHandler,
    );

    _instance = payment;

    payment.channel.setMethodCallHandler((call) => IZoopHelper.callHandler(
          call: call,
          iZoopHandler: paymentHandler,
        ));

    return payment;
  }

  void setHandler(IZoopHandler handler) {
    if (_instance == null) {
      throw Exception('Payment instance not initialized. Call Payment.create() first.');
    }

    _instance?.paymentHandler = handler;
    _instance?.channel.setMethodCallHandler(
      (call) => IZoopHelper.callHandler(
        call: call,
        iZoopHandler: handler,
      ),
    );
  }

  /// Initialize Zoop SDK
  Future<bool> initialize({
    String? marketplace,
    String? seller,
    String? accessKey,
    bool downloadTheme = false,
  }) async {
    try {
      await channel.invokeMethod('initialize', {
        'marketplace': marketplace,
        'seller': seller,
        'accessKey': accessKey,
        'downloadTheme': downloadTheme,
      });
      return true;
    } catch (e) {
      log('Error initializing Zoop: $e');
      return false;
    }
  }

  /// Login/Dashboard Activation
  Future<bool> login({bool downloadTheme = false}) async {
    try {
      await channel.invokeMethod('login', {'downloadTheme': downloadTheme});
      return true;
    } catch (e) {
      log('Error on login: $e');
      return false;
    }
  }

  /// Check Zoop Key
  Future<bool> checkZoopKey() async {
    try {
      await channel.invokeMethod('checkZoopKey');
      return true;
    } catch (e) {
      log('Error checking Zoop key: $e');
      return false;
    }
  }

  /// Credit Payment
  Future<bool> creditPayment({
    required int amount,
    int installments = 1,
  }) async {
    try {
      await channel.invokeMethod('creditPayment', {
        'amount': amount,
        'installments': installments,
      });
      return true;
    } catch (e) {
      log('Error on credit payment: $e');
      return false;
    }
  }

  /// PIX Payment
  Future<bool> pixPayment({
    required int amount,
  }) async {
    try {
      await channel.invokeMethod('pixPayment', {
        'amount': amount,
      });
      return true;
    } catch (e) {
      log('Error on PIX payment: $e');
      return false;
    }
  }

  /// Void/Cancel Transaction
  Future<bool> voidTransaction() async {
    try {
      await channel.invokeMethod('voidTransaction');
      return true;
    } catch (e) {
      log('Error on void transaction: $e');
      return false;
    }
  }

  /// Select transaction for void
  Future<bool> selectVoidTransaction(String transactionKey) async {
    try {
      await channel.invokeMethod('selectVoidTransaction', {
        'transactionKey': transactionKey,
      });
      return true;
    } catch (e) {
      log('Error selecting void transaction: $e');
      return false;
    }
  }

  /// Cancel current operation
  Future<bool> cancelOperation() async {
    try {
      await channel.invokeMethod('cancelOperation');
      return true;
    } catch (e) {
      log('Error canceling operation: $e');
      return false;
    }
  }
}

