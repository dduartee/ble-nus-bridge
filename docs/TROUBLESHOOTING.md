# Solução de Problemas — BLE NUS Bridge

## BLE Scan não encontra dispositivos

**Causas possíveis:**
- Permissões de localização não concedidas (obrigatório para BLE scanning no Android)
- Bluetooth desligado
- Dispositivo alvo não está anunciando

**Solução:**
1. Vá em **Configurações → Apps → BLE NUS Bridge → Permissões**
2. Ative **Localização** (e Bluetooth, se disponível)
3. Confirme que o Bluetooth está ligado
4. Verifique se o dispositivo alvo está ligado e anunciando via BLE

## Conexão falha ao selecionar dispositivo

**Causas possíveis:**
- O nome do dispositivo não é exatamente `"track-kinesis"` (único nome aceito pelo `BridgeService`)
- O dispositivo não está mais no alcance
- O dispositivo não está anunciando o Nordic UART Service

**Solução:**
1. Confirme que o nome do dispositivo BLE corresponde exatamente a `track-kinesis` (case-sensitive)
2. Aproxime o dispositivo do telefone
3. Verifique se o firmware do dispositivo está anunciando o NUS corretamente
4. Toque em **"🔄 Pareados"** ou escaneie novamente

## Conexão TCP recusada

**Causa:**
O `BridgeService` precisa estar rodando em foreground. O servidor TCP na porta 8090 só é iniciado após a conexão BLE ser estabelecida com sucesso.

**Solução:**
1. Verifique se a notificação "BLE NUS Bridge" está visível na barra de notificações
2. Se não estiver, o serviço não está ativo — reconecte pelo app
3. Confirme a porta: `nc localhost 8090`
4. Verifique se nenhum outro processo está usando a porta 8090

## Notificação não aparece (Android 13+)

**Causa:**
Android 13 (API 33) introduziu a permissão `POST_NOTIFICATIONS`. Sem ela, o foreground service não pode exibir notificação e pode ser interrompido pelo sistema.

**Solução:**
1. Vá em **Configurações → Apps → BLE NUS Bridge → Notificações**
2. Ative **"Mostrar notificações"**
3. Alternativamente, o app solicita esta permissão na primeira execução — conceda quando solicitado
