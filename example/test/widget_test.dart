// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter_test/flutter_test.dart';

import 'package:zoop_payment_tech_example/main.dart';

void main() {
  testWidgets('Verify app loads correctly', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const MyApp());

    // Verify that the app bar is present
    expect(find.text('Zoop Payment Tech'), findsOneWidget);

    // Verify that login button is present
    expect(find.text('Login/Ativar Terminal'), findsOneWidget);

    // Verify that payment button is present
    expect(find.text('Pagamento Crédito (R\$ 5,00)'), findsOneWidget);
  });
}
