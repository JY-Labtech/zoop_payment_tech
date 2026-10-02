# zoop_payment_tech

<div align="center">
  <img src="assets/zoop payment tech.png" alt="Stone" height="200" />
</div>

---

## 🚨 Aviso

> **Este plugin é _não oficial_** e foi desenvolvido de forma independente para facilitar a integração de pagamentos via SDK da Zoop em dispositivos Android Smart POS.

##### 🚨 Este plugin ainda não foi testado em dispositivos reais, caso tenha interesse em utilizá-lo entre em contato com nossa equipe.🚨

---

Plugin Flutter para integração com o SDK Zoop SmartPOS. Este plugin abstrai toda a complexidade do código nativo Android, permitindo que desenvolvedores Flutter integrem facilmente funcionalidades de pagamento através de terminais SmartPOS da Zoop usando apenas código Dart/Flutter.

## 📋 Índice

- [Sobre o Plugin](#sobre-o-plugin)
- [Como Funciona](#como-funciona)
- [Funcionalidades](#funcionalidades)
- [Requisitos](#requisitos)
- [Configuração](#configuração)
- [Licença do Plugin (validação)](#licença-do-plugin-validação)
- [Guia de Uso](#guia-de-uso)
- [Exemplos Práticos](#exemplos-práticos)
- [Tratamento de Erros](#tratamento-de-erros)
- [Boas Práticas](#boas-práticas)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Testes](#testes)
- [Licença](#licença)

## 🎯 Sobre o Plugin

O `zoop_payment_tech` é um plugin Flutter que serve como uma camada de abstração entre seu aplicativo Flutter e o SDK nativo Android da Zoop. Ele foi desenvolvido seguindo o mesmo padrão arquitetural do plugin `stone_payment_tech`, garantindo consistência e facilidade de uso.

### Objetivo

Facilitar a integração de pagamentos via terminais SmartPOS da Zoop em aplicativos Flutter, eliminando a necessidade de escrever código Kotlin/Java. Toda a comunicação é feita através de Method Channels, e o desenvolvedor trabalha apenas com código Dart.

### Benefícios

- ✅ **Abstração completa**: Não precisa lidar com código nativo
- ✅ **Type-safe**: Interface Dart com tipagem forte
- ✅ **Callbacks assíncronos**: Sistema de callbacks para feedback em tempo real
- ✅ **Validação de licença**: Sistema integrado de validação de licenças
- ✅ **Multi-terminal**: Suporte para diferentes modelos de terminais

## 🔧 Como Funciona

### Arquitetura

O plugin utiliza uma arquitetura em camadas:

```
┌─────────────────────────────────────┐
│      Aplicação Flutter (Dart)      │
│                                     │
│  ┌───────────────────────────────┐ │
│  │   ZoopPaymentTech (API)      │ │
│  │   Payment (Métodos)           │ │
│  │   IZoopHandler (Callbacks)   │ │
│  └───────────────┬───────────────┘ │
└──────────────────┼──────────────────┘
                   │ Method Channel
                   ▼
┌─────────────────────────────────────┐
│      Plugin Android (Kotlin)         │
│                                     │
│  ┌───────────────────────────────┐ │
│  │  ZoopPaymentTechPlugin        │ │
│  │  CheckLicenceTech             │ │
│  │  (Validação de Licença)       │ │
│  └───────────────┬───────────────┘ │
└──────────────────┼──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│      SDK Zoop SmartPOS              │
│      (Biblioteca Nativa)            │
└─────────────────────────────────────┘
```

### Fluxo de Validação de Licença

Antes de **cada** operação (inicialização, login, pagamentos, cancelamento etc.), o plugin valida automaticamente a licença:

1. **Validação Local**: Verifica se há uma licença válida salva no dispositivo
2. **Validação Remota**: Se não houver cache válido, consulta sempre o servidor oficial da JY Labtech (`https://licence-server.invalid/pos-payments/licence/check`), fora da thread principal
3. **Armazenamento Seguro**: Salva a licença de forma criptografada no dispositivo
4. **Cache offline**: A licença salva vale até a data de vencimento (`expiredAt`), por no máximo 7 dias sem nova consulta
5. **Bloqueio**: Se a licença for inválida, vencida, suspensa ou cancelada, a operação é recusada com o erro `LICENCE_ERROR` e o SDK da Zoop não é chamado

Detalhes em [Licença do Plugin (validação)](#licença-do-plugin-validação).

### Sistema de Callbacks

O plugin utiliza callbacks assíncronos para comunicação em tempo real:

- **onSuccess**: Operação concluída com sucesso
- **onError**: Erro durante a operação
- **onMessage**: Mensagens informativas durante o processo
- **onLoading**: Indicador de carregamento
- **onQRCode**: QR Code gerado (para PIX)
- **onToken**: Token de ativação (para login)
- **onVoidTransactionList**: Lista de transações para cancelamento

## ✨ Funcionalidades

- ✅ **Login/Ativação**: Ativação do terminal via Dashboard Zoop
- ✅ **Validação de Chave**: Verificação da chave Zoop no terminal
- ✅ **Pagamento no Crédito**: Transações de crédito com suporte a parcelamento
- ✅ **Pagamento PIX**: Pagamentos via PIX com geração de QR Code
- ✅ **Cancelamento**: Cancelamento de transações realizadas
- ✅ **Validação de Licença**: Sistema integrado de controle de licenças
- ✅ **Callbacks em Tempo Real**: Feedback imediato durante as operações

## 📦 Requisitos

### Requisitos de Sistema

- **Flutter SDK**: >= 3.3.0
- **Dart SDK**: >= 3.9.2
- **Android**: minSdk 23 (Android 6.0+)
- **Java**: 17+
- **Gradle**: 8.11.1+

### Terminais Suportados

O plugin suporta os seguintes modelos de terminais SmartPOS:

- **PAX A910**: Terminal PAX modelo A910
- **Gertec GPOS720**: Terminal Gertec modelo GPOS720
- **Tectoy T4**: Terminal Tectoy modelo T4

### Requisitos de Configuração

Para utilizar o plugin, você precisará de:

1. **Credenciais GitHub**: Para acessar o repositório Maven do SDK Zoop

   - Usuário GitHub
   - Personal Access Token (PAT) com permissão de leitura de pacotes

2. **Licenças**: Chaves de licença válidas para o plugin

   - `licenceKey`: sua chave de licença, adquirida no portal [https://portal.jylabtech.com.br](https://portal.jylabtech.com.br)
   - `licenceInternalKey` (opcional): endereço da API de validação. Se omitido, o plugin usa o servidor oficial da JY Labtech

3. **Terminal Físico**: Um terminal SmartPOS compatível conectado ao dispositivo Android

## ⚙️ Configuração

### 1. Adicionar Dependência

Adicione o plugin ao seu `pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  zoop_payment_tech: ^1.1.0
```

Execute:

```bash
flutter pub get
```

### 2. Configurar Credenciais do GitHub

O SDK Zoop está hospedado em um repositório Maven privado do GitHub. Você precisa configurar as credenciais em **dois lugares**:

#### No Plugin (`android/local.properties`):

```properties
GITHUB_USER=seu_usuario_github
GITHUB_PAT=sua_personal_access_token
```

#### No Seu Projeto (`seu_projeto/android/local.properties`):

Se você estiver usando o plugin em um projeto Flutter separado, também precisa configurar as credenciais no `local.properties` do seu projeto:

```properties
GITHUB_USER=seu_usuario_github
GITHUB_PAT=sua_personal_access_token
```

**Nota**: Se você estiver usando o projeto exemplo (`example`), configure também em `example/android/local.properties`.

**Importante**: Além das credenciais, você também precisa configurar o repositório Maven do GitHub no arquivo `build.gradle.kts` ou `build.gradle` do seu projeto. O projeto exemplo já está configurado automaticamente, mas se você criar um novo projeto, adicione o seguinte no arquivo `android/build.gradle.kts`:

```kotlin
val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/getzoop/zoop-package-public")
            credentials {
                username = project.findProperty("GITHUB_USER")?.toString()
                    ?: localProperties.getProperty("GITHUB_USER")
                    ?: System.getenv("GITHUB_USER")
                    ?: ""
                password = project.findProperty("GITHUB_PAT")?.toString()
                    ?: localProperties.getProperty("GITHUB_PAT")
                    ?: System.getenv("GITHUB_PAT")
                    ?: ""
            }
        }
    }
}
```

Para projetos usando **Groovy DSL** (`build.gradle`), adicione no arquivo `android/build.gradle`:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/getzoop/zoop-package-public")
            credentials {
                username = project.findProperty("GITHUB_USER") ?: System.getenv("GITHUB_USER") ?: ""
                password = project.findProperty("GITHUB_PAT") ?: System.getenv("GITHUB_PAT") ?: ""
            }
        }
    }
}
```

**Como criar um Personal Access Token**:

1. Acesse GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Clique em "Generate new token"
3. Selecione a permissão `read:packages`
4. Copie o token gerado e use como `GITHUB_PAT`

**Alternativamente**, você pode configurar via variáveis de ambiente antes de compilar:

```bash
export GITHUB_USER=seu_usuario_github
export GITHUB_PAT=sua_personal_access_token
```

### 3. Configurar Licenças

O plugin requer uma licença válida para funcionar. A chave é lida no momento do build a partir do `android/local.properties` **do seu aplicativo**:

```properties
licenceKey=SUA_CHAVE_DE_LICENCA
# opcional: se omitido, o plugin usa o servidor oficial da JY Labtech
licenceInternalKey=***REMOVIDO***
```

**Alternativamente**, via variáveis de ambiente (têm prioridade sobre o `local.properties`):

```bash
export LICENCE_KEY=SUA_CHAVE_DE_LICENCA
export LICENCE_INTERNAL_KEY=***REMOVIDO***  # opcional
```

**Importante**:

- Sem o `licenceKey` configurado, o plugin bloqueia todas as operações com o erro `LICENCE_ERROR`
- O `licenceInternalKey` é **opcional**. O plugin sempre valida no servidor oficial da JY Labtech (`https://licence-server.invalid/pos-payments/licence/check`); o valor acima é o base64 desse endereço (também é aceita a URL em texto puro). Qualquer valor que não aponte para o servidor oficial (outro host, `http` sem TLS, base64 inválido ou o endereço antigo do Cloud Run) é ignorado e o servidor oficial é usado no lugar
- Como os valores entram no `BuildConfig`, após alterá-los faça um build limpo (`flutter clean`) do aplicativo
- Veja a seção [Licença do Plugin (validação)](#licença-do-plugin-validação) para planos, limites, erros e solução de problemas

### 4. Configurar Flavor do Terminal

No arquivo de build do seu aplicativo, configure o flavor correspondente ao seu terminal. O exemplo abaixo mostra a configuração para **Groovy DSL** (`build.gradle`) e **Kotlin DSL** (`build.gradle.kts`):

#### Groovy DSL (`android/app/build.gradle`):

```gradle
android {
    flavorDimensions += "terminal"

    productFlavors {
        paxA910 {
            dimension = "terminal"
        }
        gertecGpos720 {
            dimension = "terminal"
        }
        tectoyT4 {
            dimension = "terminal"
        }
    }
}
```

#### Kotlin DSL (`android/app/build.gradle.kts`):

```kotlin
android {
    flavorDimensions += "terminal"

    productFlavors {
        create("paxA910") {
            dimension = "terminal"
        }
        create("gertecGpos720") {
            dimension = "terminal"
        }
        create("tectoyT4") {
            dimension = "terminal"
        }
    }
}
```

**Nota**: Certifique-se de usar nomes em **camelCase** (sem hífens) para os flavors, pois o Flutter espera esse formato ao usar `--flavor`.

**Compilar para um terminal específico**:

```bash
# Para PAX A910
flutter build apk --flavor paxA910

# Para Gertec GPOS720
flutter build apk --flavor gertecGpos720

# Para Tectoy T4
flutter build apk --flavor tectoyT4
```

### 5. Permissões Android

Adicione as permissões necessárias no `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 🔑 Licença do Plugin (validação)

### Onde adquirir

As licenças são adquiridas e gerenciadas no portal **[https://portal.jylabtech.com.br](https://portal.jylabtech.com.br)** (o login é o mesmo e-mail/senha do portal anterior). O produto é a **"Licença plugin de pagamento"**, que vale para qualquer plugin de pagamento da JY Labtech.

- **Chaves existentes continuam válidas**: quem já possui uma chave não precisa trocá-la. Chaves novas no formato `JYL-...` também funcionam.
- **Plano `debug` (cortesia)**: concedido automaticamente ao se cadastrar no portal; permite até 2 aparelhos por 365 dias, ideal para desenvolvimento e testes.
- **Plano `mensal`**: cobrado por dispositivo.
- **Limite por dispositivo**: cada aparelho que valida a licença ocupa uma vaga. Aparelhos podem ser desativados pelo portal para liberar vagas (um aparelho desativado deixa de ser aceito).

### Como configurar

Configure o `licenceKey` conforme [3. Configurar Licenças](#3-configurar-licenças). O `licenceInternalKey` é opcional; se quiser informá-lo, o valor é:

```
***REMOVIDO***
```

> **A partir da 1.1.0:** a validação usa sempre o servidor oficial da JY Labtech. Valores do `licenceInternalKey` que apontem para outro endereço, inclusive o antigo (que começa com `***REMOVIDO***...`), são ignorados. Não é preciso trocar a chave de licença.

### Como funciona a validação

- A validação é feita automaticamente antes de cada operação: `initialize`, `login`, `checkZoopKey`, `creditPayment`, `pixPayment`, `voidTransaction` e `selectVoidTransaction`. Ficam de fora apenas `getPlatformVersion` e `cancelOperation` (cancelar uma operação em andamento nunca é bloqueado).
- A consulta de rede roda fora da thread principal, com timeout de 15 s para conexão e 15 s para leitura.
- O plugin envia ao servidor a chave (`Authorization: Bearer licence:<licenceKey>`) e a identificação do aparelho (ANDROID_ID, número de série e modelo).
- Só é aceita uma licença vigente: a resposta do servidor precisa ter `expiredAt` no futuro e não estar marcada como vencida (`isExpired`), inativa/suspensa (`isInactive`) ou cancelada (`isCanceled`). Uma resposta 200 com licença vencida, sem `expiredAt` ou com JSON inválido também bloqueia.
- Se aprovada, a licença é guardada de forma criptografada no aparelho e reutilizada offline até o vencimento (`expiredAt`), por no máximo **7 dias** sem nova consulta. O cache é descartado se for de outra chave, se a data em que foi salvo estiver no futuro (relógio adulterado) ou se tiver sido validado em outro servidor. Depois disso, o aparelho precisa de internet para validar novamente.
- Se a licença for inválida, vencida, suspensa ou cancelada, a operação é bloqueada: o nativo responde `PlatformException` com código `LICENCE_ERROR`, o callback `onError` recebe `"Sua licença não foi ativada. <motivo>"` (por exemplo, a `message` devolvida pelo servidor) e o método do `Payment` retorna `false`. O SDK da Zoop não é chamado. O motivo também aparece no Logcat (tag `ZoopPaymentTechPlugin`).

### Erros de licença

| HTTP | Situação | O que fazer |
| ---- | -------- | ----------- |
| 400 | Aparelho sem identificador estável | Verifique se o app consegue ler o ANDROID_ID / número de série do aparelho |
| 403 | Chave não enviada | Configure `licenceKey` e refaça o build |
| 404 | Licença não encontrada | Confira a chave no portal (sem espaços ou quebras de linha) |
| 412 | Limite de aparelhos atingido ou aparelho desativado | Desative aparelhos não usados ou contrate mais dispositivos no portal |
| 428 | Licença vencida, suspensa ou cancelada | Renove ou regularize a licença no portal |
| 429 | Muitas requisições (rate limit) | Aguarde alguns instantes e tente novamente |

Todas as respostas de erro trazem um JSON `{"message": "..."}` com a descrição, repassada ao `onError`. Sem corpo JSON, a mensagem é `Erro ao validar licença (HTTP <código>)`.

### Solução de problemas

- **"Licença não configurada"**: `licenceKey` vazio. Confira o `android/local.properties` do app (ou as variáveis de ambiente) e faça `flutter clean` antes de compilar.
- **Trocou a chave e nada mudou**: os valores são gravados no `BuildConfig`; faça um build limpo. Ao trocar a chave, o cache local é descartado e a licença é consultada novamente.
- **Funciona offline e depois para**: o cache local expira em 7 dias (ou no vencimento da licença). Conecte o aparelho à internet e repita a operação.
- **Erro de conexão/timeout**: o aparelho precisa acessar `https://licence-server.invalid`. Verifique rede, proxy ou firewall.

## 📖 Guia de Uso

### 1. Inicializar o Plugin

Primeiro, crie uma classe que implementa `IZoopHandler` para receber os callbacks:

```dart
import 'package:zoop_payment_tech/zoop_payment_tech.dart';

class MyZoopHandler implements IZoopHandler {
  @override
  Future<void> onSuccess(String message) async {
    print('✅ Sucesso: $message');
    // Atualizar UI, mostrar mensagem de sucesso, etc.
  }

  @override
  Future<void> onError(String message) async {
    print('❌ Erro: $message');
    // Mostrar erro ao usuário, registrar erro, etc.
  }

  @override
  Future<void> onMessage(String message) async {
    print('ℹ️ Mensagem: $message');
    // Atualizar status na UI, mostrar mensagem informativa
  }

  @override
  Future<void> onLoading(bool show) async {
    print('⏳ Loading: $show');
    // Mostrar/esconder indicador de carregamento
  }

  @override
  Future<void> onQRCode(String qrCode) async {
    print('📱 QR Code recebido');
    // Exibir QR Code para o usuário escanear
  }

  @override
  Future<void> onToken(String token) async {
    print('🔑 Token: $token');
    // Exibir token para o usuário inserir no dashboard
  }

  @override
  Future<void> onVoidTransactionList(
    List<Map<String, dynamic>> transactions,
  ) async {
    print('📋 Transações: ${transactions.length}');
    // Exibir lista de transações para seleção
  }
}
```

Agora, inicialize o plugin:

```dart
// Obter instância singleton
final payment = ZoopPaymentTech.I.initPayment(
  handler: MyZoopHandler(),
);
```

### 2. Login/Ativação do Terminal

#### Primeira Ativação (sem credenciais)

```dart
// Inicializar SDK sem credenciais
await payment.initialize();

// Iniciar processo de login
await payment.login(downloadTheme: false);

// O callback onToken será chamado com o token
// O usuário deve inserir esse token no dashboard Zoop
// Após confirmação, onSuccess será chamado com as credenciais
```

#### Login com Credenciais Salvas

Se você já possui as credenciais salvas (recomendado salvar após primeira ativação):

```dart
await payment.initialize(
  marketplace: 'seu_marketplace_id',
  seller: 'seu_seller_id',
  accessKey: 'sua_access_key',
);
```

**Salvando credenciais após login**:

```dart
class MyZoopHandler implements IZoopHandler {
  // ... outros métodos ...

  @override
  Future<void> onSuccess(String message) async {
    // Quando receber sucesso no login, salvar credenciais
    if (message.contains('Login realizado')) {
      // Salvar credenciais em SharedPreferences ou banco de dados
      // await _saveCredentials(marketplace, seller, accessKey);
    }
  }
}
```

### 3. Verificar Chave Zoop

Antes de realizar pagamentos, verifique se a chave Zoop está instalada no terminal:

```dart
await payment.checkZoopKey();

// O callback onMessage será chamado com:
// - "Chave Zoop encontrada" (se houver chave)
// - "Chave ausente, necessário envio para PAX para gravação de chave" (se não houver)
```

### 4. Realizar Pagamento no Crédito

```dart
await payment.creditPayment(
  amount: 5000, // R$ 50,00 (valor em centavos)
  installments: 3, // 3 parcelas
);

// Durante o processo:
// - onLoading(true) será chamado
// - onMessage será chamado com mensagens como "Insira o cartão", "Digite o PIN", etc.
// - onSuccess será chamado quando o pagamento for aprovado
// - onError será chamado se houver erro
// - onLoading(false) será chamado ao final
```

### 5. Realizar Pagamento PIX

```dart
await payment.pixPayment(
  amount: 5000, // R$ 50,00 (valor em centavos)
);

// Durante o processo:
// - onQRCode será chamado com o QR Code em formato string
// - Exiba o QR Code para o cliente escanear
// - onSuccess será chamado quando o pagamento for confirmado
```

### 6. Cancelar Transação

```dart
// Iniciar processo de cancelamento
await payment.voidTransaction();

// O callback onVoidTransactionList será chamado com a lista de transações
// Exiba a lista para o usuário selecionar

// Quando o usuário selecionar uma transação:
await payment.selectVoidTransaction('chave_da_transacao_selecionada');

// onSuccess será chamado quando o cancelamento for concluído
```

### 7. Cancelar Operação Atual

Para cancelar uma operação em andamento:

```dart
await payment.cancelOperation();
```

## 💻 Exemplos Práticos

### Exemplo Completo: Tela de Pagamentos

```dart
import 'package:flutter/material.dart';
import 'package:zoop_payment_tech/zoop_payment_tech.dart';
import 'dart:convert';

class PaymentScreen extends StatefulWidget {
  const PaymentScreen({super.key});

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> implements IZoopHandler {
  late Payment _payment;
  String _statusMessage = 'Pronto para operações';
  bool _isLoading = false;
  String? _qrCode;
  String? _token;
  List<Map<String, dynamic>> _voidTransactions = [];

  @override
  void initState() {
    super.initState();
    _initializePayment();
  }

  void _initializePayment() {
    _payment = ZoopPaymentTech.I.initPayment(handler: this);
  }

  Future<void> _handleLogin() async {
    setState(() {
      _statusMessage = 'Iniciando login...';
      _isLoading = true;
    });

    // Primeiro, inicializar sem credenciais
    await _payment.initialize();

    // Depois, iniciar login
    await _payment.login(downloadTheme: false);
  }

  Future<void> _handleCheckKey() async {
    setState(() {
      _statusMessage = 'Verificando chave Zoop...';
      _isLoading = true;
    });
    await _payment.checkZoopKey();
  }

  Future<void> _handleCreditPayment(double value) async {
    final amountInCents = (value * 100).toInt();

    setState(() {
      _statusMessage = 'Preparando pagamento de R\$ ${value.toStringAsFixed(2)}...';
      _isLoading = true;
    });

    await _payment.creditPayment(
      amount: amountInCents,
      installments: 1,
    );
  }

  Future<void> _handlePixPayment(double value) async {
    final amountInCents = (value * 100).toInt();

    setState(() {
      _statusMessage = 'Gerando QR Code PIX...';
      _isLoading = true;
      _qrCode = null;
    });

    await _payment.pixPayment(amount: amountInCents);
  }

  Future<void> _handleVoidTransaction() async {
    setState(() {
      _statusMessage = 'Carregando transações...';
      _isLoading = true;
      _voidTransactions = [];
    });

    await _payment.voidTransaction();
  }

  Future<void> _handleSelectTransaction(String transactionKey) async {
    setState(() {
      _statusMessage = 'Cancelando transação...';
      _isLoading = true;
    });

    await _payment.selectVoidTransaction(transactionKey);
  }

  Future<void> _handleCancelOperation() async {
    await _payment.cancelOperation();
    setState(() {
      _statusMessage = 'Operação cancelada';
      _isLoading = false;
    });
  }

  // Implementação dos callbacks
  @override
  Future<void> onSuccess(String message) async {
    setState(() {
      _statusMessage = '✅ $message';
      _isLoading = false;
    });

    // Limpar QR Code após sucesso
    if (message.contains('PIX') || message.contains('pagamento')) {
      Future.delayed(const Duration(seconds: 2), () {
        if (mounted) {
          setState(() {
            _qrCode = null;
          });
        }
      });
    }
  }

  @override
  Future<void> onError(String message) async {
    setState(() {
      _statusMessage = '❌ Erro: $message';
      _isLoading = false;
    });

    // Mostrar dialog de erro
    if (mounted) {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Erro'),
          content: Text(message),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('OK'),
            ),
          ],
        ),
      );
    }
  }

  @override
  Future<void> onMessage(String message) async {
    setState(() {
      _statusMessage = message;
    });
  }

  @override
  Future<void> onLoading(bool show) async {
    setState(() {
      _isLoading = show;
    });
  }

  @override
  Future<void> onQRCode(String qrCode) async {
    setState(() {
      _qrCode = qrCode;
      _statusMessage = 'QR Code gerado. Escaneie para pagar.';
    });
  }

  @override
  Future<void> onToken(String token) async {
    setState(() {
      _token = token;
      _statusMessage = 'Token gerado: $token\nInsira no dashboard Zoop.';
    });

    // Mostrar dialog com token
    if (mounted) {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Token de Ativação'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                token,
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 2,
                ),
              ),
              const SizedBox(height: 16),
              const Text('Insira este token no dashboard Zoop para continuar.'),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('OK'),
            ),
          ],
        ),
      );
    }
  }

  @override
  Future<void> onVoidTransactionList(
    List<Map<String, dynamic>> transactions,
  ) async {
    setState(() {
      _voidTransactions = transactions;
      _isLoading = false;
      _statusMessage = 'Selecione uma transação para cancelar';
    });

    // Mostrar dialog com lista de transações
    if (mounted && transactions.isNotEmpty) {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Transações para Cancelamento'),
          content: SizedBox(
            width: double.maxFinite,
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: transactions.length,
              itemBuilder: (context, index) {
                final transaction = transactions[index];
                return ListTile(
                  title: Text('R\$ ${transaction['amount']}'),
                  subtitle: Text(
                    '${transaction['date']} ${transaction['time']}',
                  ),
                  onTap: () {
                    Navigator.pop(context);
                    _handleSelectTransaction(transaction['transactionKey']);
                  },
                );
              },
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancelar'),
            ),
          ],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Zoop Payment Tech'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Status Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    if (_isLoading)
                      const Padding(
                        padding: EdgeInsets.all(8.0),
                        child: CircularProgressIndicator(),
                      ),
                    Text(
                      _statusMessage,
                      style: const TextStyle(fontSize: 16),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            // Token Card (se houver)
            if (_token != null)
              Card(
                color: Colors.amber[50],
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      const Text(
                        'Token para Ativação',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _token!,
                        style: const TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 2,
                        ),
                      ),
                    ],
                  ),
                ),
              ),

            // QR Code Card (se houver)
            if (_qrCode != null)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      const Text(
                        'QR Code PIX',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                        ),
                      ),
                      const SizedBox(height: 16),
                      // Aqui você pode usar um pacote como qr_flutter para exibir o QR Code
                      Container(
                        width: 200,
                        height: 200,
                        color: Colors.grey[200],
                        child: const Center(
                          child: Text(
                            'QR Code\n(Use pacote qr_flutter\npara exibir)',
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        _qrCode!,
                        style: const TextStyle(fontSize: 10),
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ),

            const SizedBox(height: 16),

            // Botões de Ação
            ElevatedButton.icon(
              onPressed: _isLoading ? null : _handleLogin,
              icon: const Icon(Icons.login),
              label: const Text('Login/Ativar Terminal'),
            ),

            const SizedBox(height: 8),

            ElevatedButton.icon(
              onPressed: _isLoading ? null : _handleCheckKey,
              icon: const Icon(Icons.vpn_key),
              label: const Text('Verificar Chave Zoop'),
            ),

            const SizedBox(height: 8),

            ElevatedButton.icon(
              onPressed: _isLoading ? null : () => _handleCreditPayment(50.0),
              icon: const Icon(Icons.credit_card),
              label: const Text('Pagamento Crédito (R\$ 50,00)'),
            ),

            const SizedBox(height: 8),

            ElevatedButton.icon(
              onPressed: _isLoading ? null : () => _handlePixPayment(50.0),
              icon: const Icon(Icons.qr_code),
              label: const Text('Pagamento PIX (R\$ 50,00)'),
            ),

            const SizedBox(height: 8),

            ElevatedButton.icon(
              onPressed: _isLoading ? null : _handleVoidTransaction,
              icon: const Icon(Icons.cancel),
              label: const Text('Cancelar Transação'),
            ),

            const SizedBox(height: 8),

            OutlinedButton.icon(
              onPressed: _isLoading ? _handleCancelOperation : null,
              icon: const Icon(Icons.stop),
              label: const Text('Cancelar Operação Atual'),
            ),
          ],
        ),
      ),
    );
  }
}
```

### Exemplo: Gerenciamento de Estado com Provider

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:zoop_payment_tech/zoop_payment_tech.dart';

// Provider para gerenciar estado
class PaymentProvider extends ChangeNotifier implements IZoopHandler {
  late Payment _payment;
  String _statusMessage = '';
  bool _isLoading = false;
  String? _qrCode;
  String? _token;

  PaymentProvider() {
    _payment = ZoopPaymentTech.I.initPayment(handler: this);
  }

  Payment get payment => _payment;
  String get statusMessage => _statusMessage;
  bool get isLoading => _isLoading;
  String? get qrCode => _qrCode;
  String? get token => _token;

  Future<void> login() async {
    await _payment.login();
  }

  Future<void> creditPayment(int amount) async {
    await _payment.creditPayment(amount: amount);
  }

  // Implementação dos callbacks
  @override
  Future<void> onSuccess(String message) async {
    _statusMessage = message;
    _isLoading = false;
    notifyListeners();
  }

  @override
  Future<void> onError(String message) async {
    _statusMessage = 'Erro: $message';
    _isLoading = false;
    notifyListeners();
  }

  @override
  Future<void> onMessage(String message) async {
    _statusMessage = message;
    notifyListeners();
  }

  @override
  Future<void> onLoading(bool show) async {
    _isLoading = show;
    notifyListeners();
  }

  @override
  Future<void> onQRCode(String qrCode) async {
    _qrCode = qrCode;
    notifyListeners();
  }

  @override
  Future<void> onToken(String token) async {
    _token = token;
    notifyListeners();
  }

  @override
  Future<void> onVoidTransactionList(
    List<Map<String, dynamic>> transactions,
  ) async {
    // Implementar lógica de lista de transações
    notifyListeners();
  }
}

// Uso no MaterialApp
void main() {
  runApp(
    ChangeNotifierProvider(
      create: (_) => PaymentProvider(),
      child: const MyApp(),
    ),
  );
}

// Widget consumindo o Provider
class PaymentWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<PaymentProvider>(
      builder: (context, provider, child) {
        return Column(
          children: [
            if (provider.isLoading)
              const CircularProgressIndicator(),
            Text(provider.statusMessage),
            ElevatedButton(
              onPressed: () => provider.creditPayment(5000),
              child: const Text('Pagar R\$ 50,00'),
            ),
          ],
        );
      },
    );
  }
}
```

### Exemplo: Salvando Credenciais

```dart
import 'package:shared_preferences/shared_preferences.dart';

class CredentialsManager {
  static const String _keyMarketplace = 'zoop_marketplace';
  static const String _keySeller = 'zoop_seller';
  static const String _keyAccessKey = 'zoop_access_key';

  static Future<void> saveCredentials({
    required String marketplace,
    required String seller,
    required String accessKey,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyMarketplace, marketplace);
    await prefs.setString(_keySeller, seller);
    await prefs.setString(_keyAccessKey, accessKey);
  }

  static Future<Map<String, String>?> loadCredentials() async {
    final prefs = await SharedPreferences.getInstance();
    final marketplace = prefs.getString(_keyMarketplace);
    final seller = prefs.getString(_keySeller);
    final accessKey = prefs.getString(_keyAccessKey);

    if (marketplace != null && seller != null && accessKey != null) {
      return {
        'marketplace': marketplace,
        'seller': seller,
        'accessKey': accessKey,
      };
    }
    return null;
  }

  static Future<void> clearCredentials() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyMarketplace);
    await prefs.remove(_keySeller);
    await prefs.remove(_keyAccessKey);
  }
}

// Uso no handler
class MyZoopHandler implements IZoopHandler {
  @override
  Future<void> onSuccess(String message) async {
    if (message.contains('Login realizado')) {
      // Parsear credenciais da mensagem ou usar um callback específico
      // Salvar credenciais
      await CredentialsManager.saveCredentials(
        marketplace: 'marketplace_id',
        seller: 'seller_id',
        accessKey: 'access_key',
      );
    }
  }

  // ... outros métodos ...
}
```

## ⚠️ Tratamento de Erros

### Erros Comuns e Soluções

#### 1. Erro de Licença

Os métodos do `Payment` não lançam exceções: retornam `false` em caso de falha e o motivo chega pelo callback `onError`. Quando a licença é inválida, vencida, suspensa ou cancelada, a operação não é executada e `onError` recebe `"Sua licença não foi ativada. <motivo>"` (veja [Erros de licença](#erros-de-licença)).

```dart
@override
Future<void> onError(String message) async {
  if (message.contains('licença')) {
    // Licença inválida, vencida ou não configurada
    // Verifique a licença em https://portal.jylabtech.com.br
  }
}
```

#### 2. Erro de Sessão Inválida

```dart
// No callback onError
@override
Future<void> onError(String message) async {
  if (message.contains('invalid session') ||
      message.contains('Não foi realizado um login')) {
    // Sessão expirada ou não iniciada
    // Re-executar login
    await _payment.login();
  }
}
```

#### 3. Erro de Conexão

```dart
final ok = await payment.login();
if (!ok) {
  // Falha na operação (o motivo chega em onError)
  // Verificar conectividade e tentar novamente
}
```

### Padrão de Tratamento de Erros

```dart
class ErrorHandler {
  static void handlePaymentError(String error, BuildContext context) {
    String title = 'Erro';
    String message = error;

    if (error.contains('licença')) {
      title = 'Erro de Licença';
      message = 'Sua licença não está ativa. Verifique-a em https://portal.jylabtech.com.br.';
    } else if (error.contains('session')) {
      title = 'Sessão Expirada';
      message = 'Por favor, faça login novamente.';
    } else if (error.contains('network')) {
      title = 'Erro de Conexão';
      message = 'Verifique sua conexão com a internet.';
    }

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }
}
```

## 💡 Boas Práticas

### 1. Gerenciamento de Estado

- Use um gerenciador de estado (Provider, Riverpod, Bloc) para manter o estado do pagamento
- Separe a lógica de negócio da UI
- Mantenha o handler como parte do gerenciador de estado

### 2. Tratamento de Callbacks

- Sempre trate todos os callbacks, mesmo que seja apenas para logging
- Atualize a UI de forma reativa usando `setState` ou gerenciadores de estado
- Implemente timeouts para operações que podem ficar travadas

### 3. Armazenamento de Credenciais

- Sempre salve as credenciais após login bem-sucedido
- Use armazenamento seguro (SharedPreferences com criptografia ou banco de dados seguro)
- Nunca logue credenciais em produção

### 4. Validação de Entrada

```dart
bool _validateAmount(double amount) {
  if (amount <= 0) {
    // Mostrar erro: valor inválido
    return false;
  }
  if (amount > 999999.99) {
    // Mostrar erro: valor muito alto
    return false;
  }
  return true;
}

Future<void> _makePayment(double amount) async {
  if (!_validateAmount(amount)) {
    return;
  }

  final amountInCents = (amount * 100).toInt();
  await payment.creditPayment(amount: amountInCents);
}
```

### 5. Timeout de Operações

```dart
Future<bool> _makePaymentWithTimeout(int amount) async {
  try {
    await payment.creditPayment(amount: amount)
        .timeout(
          const Duration(minutes: 5),
          onTimeout: () {
            throw TimeoutException('Operação de pagamento expirou');
          },
        );
    return true;
  } catch (e) {
    print('Erro: $e');
    return false;
  }
}
```

### 6. Logging e Debug

```dart
class MyZoopHandler implements IZoopHandler {
  final bool _enableLogging;

  MyZoopHandler({bool enableLogging = false})
      : _enableLogging = enableLogging;

  void _log(String message) {
    if (_enableLogging) {
      print('[ZoopPaymentTech] $message');
    }
  }

  @override
  Future<void> onSuccess(String message) async {
    _log('Success: $message');
    // ... resto da implementação
  }
}
```

## 📁 Estrutura do Projeto

```
zoop_payment_tech/
├── android/
│   ├── build.gradle                 # Configuração do módulo Android
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── kotlin/
│   │   │       └── br/com/jylabtech/zoop_payment_tech/
│   │   │           ├── ZoopPaymentTechPlugin.kt    # Plugin principal
│   │   │           └── licence/                     # Sistema de licenças
│   │   │               ├── CheckLicenceTech.kt      # Cache criptografado + fluxo (Android)
│   │   │               ├── CheckLicenceEntity.kt
│   │   │               ├── LicenceConfig.kt         # URL oficial e resolveLicenceUrl
│   │   │               ├── LicenceRules.kt          # Regras puras de validade/cache/HTTP
│   │   │               ├── LicenceValidator.kt      # Fluxo cache -> servidor (puro)
│   │   │               ├── LicenceProvider.kt       # HTTP
│   │   │               └── LicenceResult.kt
│   │   └── test/kotlin/...licence/       # Testes JVM da licença
│   └── gradle/wrapper/
├── lib/
│   ├── src/
│   │   ├── app/
│   │   │   └── controller/
│   │   │       └── zoop_controller.dart
│   │   ├── interface/
│   │   │   ├── handler/
│   │   │   │   └── izoop_handler.dart
│   │   │   └── model/
│   │   │       └── zoop_response.dart
│   │   ├── licence/
│   │   │   └── constants/
│   │   │       └── licence_constants.dart
│   │   ├── payments/
│   │   │   ├── payment.dart
│   │   │   └── helper/
│   │   │       └── izoop_helper.dart
│   │   ├── zoop_payment_tech_core.dart
│   │   └── zoop_payment_tech.dart
│   └── zoop_payment_tech.dart       # Arquivo principal de exportação
├── test/                             # Testes Dart (flutter test)
├── example/                          # Projeto de exemplo
├── pubspec.yaml
└── README.md
```

## 🧪 Testes

Testes Dart (canal mockado, incluindo o bloqueio por `LICENCE_ERROR`):

```bash
flutter test
```

Testes JVM da validação de licença no nativo (`android/src/test/kotlin`), pelo projeto de exemplo:

```bash
cd example/android
./gradlew :zoop_payment_tech:testPaxA910DebugUnitTest
```

Esse comando precisa das credenciais do GitHub Packages da Zoop (`GITHUB_USER`/`GITHUB_PAT`), porque o módulo depende do SDK. As classes testadas (`LicenceConfig`, `LicenceRules`, `LicenceValidator`, `CheckLicenceEntity`, `LicenceResult`) não dependem do Android nem do SDK Zoop e também podem ser compiladas e testadas num projeto Kotlin/JVM comum com `org.json:json` e JUnit 4.

## 🔐 Licença

Este plugin é protegido por uma licença restritiva que proíbe modificações, uso indevido e venda sem autorização expressa.

**⚠️ IMPORTANTE**:

- **Modificações são proibidas** sem autorização prévia e por escrito
- **Venda e comercialização são proibidas** sem autorização expressa
- **Uso pessoal e interno** é permitido
- **Distribuição** é permitida apenas com a licença completa incluída

Para solicitar permissões especiais (modificações, uso comercial, etc.), entre em contato através do GitHub: https://github.com/JY-Labtech

Leia o arquivo [LICENSE](LICENSE) para mais detalhes sobre os termos completos da licença.

Este plugin é um wrapper não oficial do SDK Zoop SmartPOS. Certifique-se de seguir os termos de uso do SDK oficial da Zoop ao utilizar este plugin.

O sistema de licenças integrado valida automaticamente se o plugin está autorizado para uso antes de permitir operações de pagamento.

## 📞 Suporte

Para questões relacionadas ao SDK Zoop, consulte a documentação oficial da Zoop.

Para questões específicas sobre este plugin, verifique os issues do repositório ou entre em contato com a equipe de desenvolvimento.

---

<div align="left">
  <table>
  <tr>
    <td align="center"><a href="https://github.com/JY-Labtech"><img src="https://avatars.githubusercontent.com/u/185066406?s=200&v=4" width="100px;" alt=""/><br /><sub><b>JY Labtech</b></sub></a><br /> <a href="https://github.com/JY-Labtech" title="Autor"></a></td>
  </tr>

</table>

</div>

---
