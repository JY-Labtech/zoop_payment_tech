// Contract payment handler
abstract class IZoopHandler {
  Future<void> onSuccess(String message);

  Future<void> onError(String message);

  Future<void> onMessage(String message);

  Future<void> onLoading(bool show);

  Future<void> onQRCode(String qrCode);

  Future<void> onToken(String token);

  Future<void> onVoidTransactionList(List<Map<String, dynamic>> transactions);
}

