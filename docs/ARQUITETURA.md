# Filtro de chamadas do Círculo

O Círculo usa `CallScreeningService`, disponibilizado pelo Android ao aplicativo que ocupa `RoleManager.ROLE_CALL_SCREENING`. O projeto suporta Android 10 ou superior, tem `minSdk = 29`, e compila com SDK 36.

## A agenda permanece com o Android

O manifesto não declara `READ_CONTACTS`. Nessa condição, o Android entrega ao serviço apenas chamadas de números que não reconhece na agenda. O aplicativo não copia contatos e não implementa sua própria correspondência de números.

Essa condição é parte do contrato de `ScreeningPolicy`: adicionar `READ_CONTACTS` exige revisar a decisão de bloqueio. Com essa permissão, o sistema também pode entregar chamadas de contatos ao serviço.

Referências oficiais: [CallScreeningService](https://developer.android.com/reference/android/telecom/CallScreeningService) e [visão geral do Telecom](https://developer.android.com/develop/connectivity/telecom).

## A resposta precede o histórico

`CirculoScreeningService` ignora chamadas de saída. Para chamadas de entrada com esquema `tel`, consulta a preferência local e responde com `setDisallowCall(true)` e `setRejectCall(true)` quando a proteção está ativa e fora de pausa.

`respondToCall` acontece antes da gravação no banco. O Android exige a resposta em até cinco segundos. Depois da resposta, uma fila de IO serializa gravações, leituras e exclusões do histórico, sem bloquear novos callbacks na thread principal.

Uma falha de banco ou o encerramento do processo antes da gravação podem impedir um registro no histórico. Isso não altera a decisão já enviada ao Android.

O serviço mantém o registro do sistema (`setSkipCallLog(false)`) e pede para suprimir a notificação dos bloqueios. O aplicativo não requer uma notificação persistente nem um serviço em primeiro plano.

## O prazo da pausa faz parte da decisão

`ProtectionSettings` guarda `enabled` e `pauseUntilMillis`. Cada chamada compara o horário atual com esse prazo. O bloqueio volta quando `nowMillis >= pauseUntilMillis`, inclusive se o aplicativo estiver fechado normalmente.

O prazo usa o relógio do aparelho. Alterar manualmente esse relógio pode alterar a duração restante. Não há alarme, agendamento periódico ou trabalho em servidor.

## A interface reflete a função concedida

O estado ativo exige a função de filtragem concedida, a preferência de bloqueio ligada e uma pausa expirada. A resposta da tela de permissão é conferida com `isRoleHeld`; o código não presume sucesso pelo retorno da atividade.

`MainActivity.onResume` consulta novamente a função e o histórico. A gravação de novos bloqueios emite uma revisão que atualiza a lista enquanto o aplicativo está aberto. Revogar a função fora do aplicativo faz o indicador voltar a **Configurar** ao reabrir a tela. As preferências só anunciam uma alteração depois de `SharedPreferences.commit()` concluir.

## O histórico fica no aparelho

O banco SQLite armazena número e horário. A listagem contém até 200 bloqueios dos últimos 30 dias. Registros fora desse limite são removidos ao ler o histórico ou registrar um bloqueio. Sem essas operações, o arquivo pode conservar registros antigos até a próxima limpeza.

O banco está na área privada do aplicativo. O manifesto desativa backup e transferência dos dados do aplicativo. Não há permissão `INTERNET`, `READ_CALL_LOG`, `READ_CONTACTS` ou `CALL_PHONE`. Salvar um contato abre o editor do Android para confirmação da pessoa.

## Há chamadas fora do alcance do serviço

O Android não entrega números com apresentação restrita, oculta ou indisponível ao `CallScreeningService`. Chamadas de WhatsApp e outros aplicativos também ficam fora deste filtro de chamadas telefônicas.

O Círculo não promete bloquear toda modalidade de ligação. O funcionamento em um fabricante, operadora e versão do Android depende da integração do sistema e precisa ser conferido no aparelho.

## A interface usa componentes nativos

O aplicativo tem três destinos: proteção, histórico e ajustes. Usa navegação inferior em janelas compactas e `NavigationRail` a partir de 600 dp. Painéis e listas rolam para acomodar conteúdo e texto ampliado.

Os títulos usam [Manrope, distribuída pelo Google Fonts](https://github.com/google/fonts/tree/main/ofl/manrope), incluída sob a licença OFL. Os demais textos usam a fonte do sistema. Ícones vetoriais e a marca foram desenhados no projeto.

As animações Compose seguem a escala de duração do sistema. O círculo muda apenas com o estado da proteção. Não há animação contínua em repouso. A abertura usa `core-splashscreen`, um vetor animado e uma saída de 280 ms; quando as animações do Android estão desativadas, a saída remove a splash imediatamente. Não há condição de espera artificial.

O tema fornece uma cópia de `LocalConfiguration` com o modo noturno correspondente à aparência escolhida no aplicativo. Isso permite que os painéis Material 3 usem ícones legíveis nas barras do sistema mesmo quando o tema do app difere do tema do Android, preservando densidade e escala de fonte.

A versão 1.1.0 teve navegação, temas, texto ampliado, rotação e chamadas simuladas conferidos em um emulador Android 16. Essa evidência não mede fluidez em telefone físico nem integração com uma operadora. Veja [as verificações da entrega](VERIFICACAO.md).
