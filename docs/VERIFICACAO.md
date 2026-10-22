# Verificação da versão 1.1.0

Pacote `br.com.circulo.app`, código de versão 2. Nenhum código do aplicativo foi alterado para a publicação no GitHub.

- Compilação do APK concluída.
- 7 testes da política de chamadas aprovados.
- Android Lint: 0 erros, 0 avisos e 6 informações sobre versões de dependências.
- APK com assinatura de desenvolvimento verificada, igual à da versão 1.0.0.
- Manifesto sem `INTERNET`, `READ_CONTACTS`, `READ_CALL_LOG` ou `CALL_PHONE`.

## Emulador

A versão foi executada em um Pixel 7 com Android 16/API 36. As chamadas usaram o modem virtual do emulador:

| Cenário | Resultado observado |
| --- | --- |
| Número fora da agenda com proteção ativa | Rejeitado e registrado no histórico |
| Mesmo número durante a pausa | Permitido, com estado de chamada tocando |
| Contato salvo com proteção ativa | Permitido pelo Android |

Também foram conferidos pausa e retomada, detalhe do histórico, filtro preservado entre abas, rotação, temas claro e escuro, fonte em 1,3×, janela ampla e navegação com animações desativadas. Os dados temporários de teste foram removidos.

## No aparelho físico

O fabricante e a operadora ainda precisam da conferência pelo [roteiro do celular](../README.md#conferir-no-celular). TalkBack, resposta tátil e desempenho em hardware físico não foram verificados. O histórico do aplicativo Telefone é independente dos registros do Círculo.

## APK publicado

Arquivo: `circulo-1.1.0-teste.apk`.

SHA-256:

```text
f7ad916e07880e54eb22817c1cd18362487ec5e51049a98c1abb2f30d79f91c8
```

Certificado SHA-256:

```text
16c79cbfbdb6f33c78fbe81d258d44f8729bc2ea5094ba99615122e002fbb4d9
```

O APK atende às arquiteturas `arm64-v8a`, `armeabi-v7a`, `x86` e `x86_64`. A compatibilidade mínima é Android 10 com suporte a telefonia e filtragem de chamadas.
