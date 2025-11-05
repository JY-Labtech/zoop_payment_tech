class ZoopResponse {
  final bool success;
  final String? message;
  final Map<String, dynamic>? data;

  ZoopResponse({
    required this.success,
    this.message,
    this.data,
  });

  factory ZoopResponse.fromJson(Map<String, dynamic> json) {
    return ZoopResponse(
      success: json['success'] ?? false,
      message: json['message'],
      data: json['data'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'success': success,
      'message': message,
      'data': data,
    };
  }
}

class ZoopCredentials {
  final String marketplace;
  final String seller;
  final String terminal;
  final String accessKey;
  final String? sellerName;

  ZoopCredentials({
    required this.marketplace,
    required this.seller,
    required this.terminal,
    required this.accessKey,
    this.sellerName,
  });

  factory ZoopCredentials.fromJson(Map<String, dynamic> json) {
    return ZoopCredentials(
      marketplace: json['marketplace'] ?? '',
      seller: json['seller'] ?? '',
      terminal: json['terminal'] ?? '',
      accessKey: json['accessKey'] ?? '',
      sellerName: json['sellerName'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'marketplace': marketplace,
      'seller': seller,
      'terminal': terminal,
      'accessKey': accessKey,
      'sellerName': sellerName,
    };
  }
}

class ZoopVoidTransaction {
  final String amount;
  final String date;
  final String time;
  final String transactionKey;

  ZoopVoidTransaction({
    required this.amount,
    required this.date,
    required this.time,
    required this.transactionKey,
  });

  factory ZoopVoidTransaction.fromJson(Map<String, dynamic> json) {
    return ZoopVoidTransaction(
      amount: json['amount'] ?? '',
      date: json['date'] ?? '',
      time: json['time'] ?? '',
      transactionKey: json['transactionKey'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'amount': amount,
      'date': date,
      'time': time,
      'transactionKey': transactionKey,
    };
  }
}

