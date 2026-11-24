## Não lançado

- Licença conforme o contrato do jylabtech_core (JYN-234): o cache offline vale por no máximo 72 horas (antes 7 dias) e só é usado quando o servidor não responde (sem internet, timeout, 408, 429, 5xx ou resposta ilegível).
- A primeira operação depois de abrir o app consulta o servidor, e a licença é revalidada a cada 4 horas de uso. Após uma falha de rede, o plugin espera 5 minutos antes de tentar o servidor de novo.
- Recusa do servidor (cliente bloqueado, licença vencida/cancelada/suspensa, não encontrada, aparelho desativado) apaga o cache e bloqueia na hora. Com `"reason":"customer_blocked"` a mensagem é "Cliente bloqueado. Entre em contato com a JY Labtech.".
- O plugin não tem binário pré-compilado em `android/libs`: nada a regerar; o código Kotlin é compilado pelo app.

## 1.1.0

**Validação de licença mais rígida.** Apps com licença válida e ativa não precisam mudar nada; licenças vencidas, suspensas ou canceladas passam a ser bloqueadas em todos os casos.

- A URL de validação fica travada no servidor oficial da JY Labtech (`https://portal-api.jylabtech.com.br/pos-payments/licence/check`). O `licenceInternalKey` passou a ser opcional; valores que não apontem para o servidor oficial (outro host, `http`, base64 inválido, host parecido, userinfo ou o endereço antigo do Cloud Run) são ignorados e o servidor oficial é usado.
- Só libera o uso com licença vigente: resposta 200 com licença vencida (`expiredAt` no passado ou `isExpired`), inativa/suspensa (`isInactive`) ou cancelada (`isCanceled`), sem `expiredAt` ou com JSON inválido agora é erro e não é salva no cache. Novo campo `isCanceled` na entidade.
- Cache offline mais seguro: só vale com licença vigente, salvo há no máximo 7 dias, sem data no futuro (tolerância de 5 min), da mesma chave e validado no servidor oficial. Caso contrário é apagado e a licença é revalidada online.
- Gate por operação: `initialize`, `login`, `checkZoopKey`, `creditPayment`, `pixPayment`, `voidTransaction` e `selectVoidTransaction` validam a licença antes de chamar o SDK da Zoop e respondem `LICENCE_ERROR` (o `onError` recebe `"Sua licença não foi ativada. <motivo>"`, com a mensagem do servidor). `cancelOperation` não é mais bloqueado pela licença.
- A validação de rede saiu da thread principal (thread de background com resposta na main thread) e ganhou timeouts de 15 s para conexão e leitura. Mensagens de erro HTTP sem corpo JSON viram `Erro ao validar licença (HTTP <código>)`.
- Lógica de licença separada em classes puras (`LicenceConfig`, `LicenceRules`, `LicenceValidator`) com testes JVM (JUnit 4) e novos testes Dart do canal (`flutter test`).
- Removido o teste Android de template `ZoopPaymentTechPluginTest.java`: ele só conferia o texto de `getPlatformVersion` e não compilava sem o SDK da Zoop (GitHub Packages) nem rodava sem Mockito. A lógica relevante (licença) é coberta pelos testes JVM em `android/src/test/kotlin`; a dependência de teste do Mockito, que ficou sem uso, saiu do `build.gradle`.
- Adicionado `.pubignore`: o pacote publicado não inclui mais `local.properties`, `key.properties`, keystores (`*.jks`/`*.keystore`), `.DS_Store` nem caches de Gradle/IDE.
- Validação de licença migrada para a nova API da JY Labtech (`https://portal-api.jylabtech.com.br/pos-payments/licence/check`). Contrato, header e respostas são os mesmos.
- Constante `licenceCheck` atualizada para o novo endereço (base64).
- README: nova seção "Licença do Plugin (validação)" com o portal https://portal.jylabtech.com.br, novo valor de `licenceInternalKey`, planos (debug de cortesia e mensal por dispositivo), cache offline de 7 dias, tabela de erros e solução de problemas. As chaves de licença existentes continuam válidas.
- README: correções para refletir a API real (`selectVoidTransaction` recebe a chave posicional; os métodos retornam `false` e reportam erros via `onError` em vez de lançar `PlatformException`) e snippet de dependência do pub.dev.

## 1.0.0

- Initial release
