# prontuario-unico — Spec do MVP

12/10/2026 · Marcos Antônio Albano Junior

## Objetivo e escopo

O MVP precisa ser usado todo dia: registrar sintomas e hábitos pelo Telegram em segundos, receber lembretes e ver tudo num painel web. Exames entram nesta versão como cadastro manual com o PDF anexado; a extração automática fica para a v0.2.

É um projeto pessoal, open source e self-hosted, para um único usuário por instância. Não é SaaS, não tem multi-tenancy e não faz diagnóstico.

**Entra no MVP (v0.1)**

- Registro pelo Telegram: texto livre vira um evento proposto, com botões Confirmar / Editar / Descartar.
- Comandos rápidos sem IA: `/agua`, `/sono`, `/treino`, `/peso`, `/dor`.
- Lembretes configuráveis (água, sono, treino) e metas semanais manuais.
- Exames: cadastro manual de analitos (valor, unidade, faixa de referência do laudo), com o PDF original guardado como fonte.
- Consultas e orientações médicas registradas, com lembrete de acompanhamento ("refazer exame em 3 meses").
- Painel web com três telas: Visão geral (mapa do corpo, metas, detalhe), Linha do tempo e Exames.
- Exportação completa em JSON.

**Fica para depois**

| Versão | O que entra |
| --- | --- |
| v0.2 | Extração de laudos em PDF com tela de revisão; resumo para consulta em PDF |
| v0.3 | Perguntas em linguagem natural ao histórico; padrões de coocorrência com n e período |
| Futuro | Fotos de laudos, export FHIR, wearables, mais de um usuário (família) |

**Fora do escopo:** pontos, níveis e badges; diagnóstico ou sugestão de conduta; app mobile nativo; hospedagem para terceiros.

## Stack e decisões

O backend é um único executável nativo GraalVM que também serve o frontend já compilado. Na VPS ficam só dois containers: o app e o Postgres. A regra que orienta as escolhas é evitar bibliotecas que dependem de reflexão em tempo de execução, porque elas são a principal fonte de problemas no native image.

| Camada | Versão | Por quê |
| --- | --- | --- |
| Linguagem | Java 25 (LTS) | LTS mais recente; o Java 26 existe, mas não é LTS |
| Framework | Spring Boot 4.1.1 (Spring Framework 7.0.x) | Última versão estável; suporte oficial a AOT e native image |
| Build nativo | GraalVM Community 25.4.4.1.1 (base OpenJDK 25.0.4.1) + Native Build Tools 1.1.8 | O Boot 4.1 exige GraalVM 25 ou superior |
| Build | Maven 3.10.0 | Versão recomendada; o Maven 4 ainda é release candidate |
| Banco | PostgreSQL 18.6 | Última versão estável; o 19 ainda está em beta |
| Acesso a dados | Spring Data JDBC + `JdbcClient` | Sem Hibernate: menos reflexão, menos memória, SQL explícito |
| Migrações, testes | Flyway, JUnit e Testcontainers nas versões gerenciadas pelo Boot 4.1.1 | Sem fixar versão à mão: o BOM do Boot garante compatibilidade |
| Telegram | `RestClient` puro com long polling (`getUpdates`) | Sem biblioteca de bot e sem porta pública para webhook |
| LLM | `RestClient` contra API compatível com OpenAI | Funciona com Ollama local, OpenRouter, OpenAI ou outro, só trocando a URL |
| Frontend | React 19.3 + Vite 8.3 | Últimas estáveis |
| TypeScript | 7.0, com o pacote `@typescript/typescript6` para o typescript-eslint | O 7.0 ainda não tem API programática estável; é o caminho que o próprio time do TypeScript recomenda |
| Node (build) | 24 LTS agora; 26 a partir de 28/10/2026 | O 24 entra em manutenção em 20/10/2026 e o 26 vira Active LTS em 28/10/2026 |
| Licença | Apache-2.0 | Permissiva, com concessão de patentes |

**Fica de fora de propósito:** Spring AI e bibliotecas de bot do Telegram (dependem de reflexão e pesam no native), Kafka (desnecessário para um usuário), Redis (o cache em memória basta) e Kubernetes.

Versões verificadas em 12/10/2026 nas páginas oficiais: [Spring Boot](https://spring.io/projects/spring-boot) e seus [requisitos](https://docs.spring.io/spring-boot/system-requirements.html), [GraalVM](https://www.graalvm.org/release-notes/25.4/), [Maven](https://maven.apache.org/download.cgi), [PostgreSQL](https://www.postgresql.org/support/versioning/), [Node.js](https://github.com/nodejs/release), [Vite](https://vite.dev/releases), [React](https://react.dev/versions) e [TypeScript 7.0](https://devblogs.microsoft.com/typescript/announcing-typescript-7-0/). Fixe as versões no `pom.xml`, no `package.json` (`engines`) e nas tags das imagens Docker, e deixe o Dependabot propor as atualizações.

## Idioma

Código e documentação do repositório ficam em inglês; tudo o que o usuário vê fica em português (pt-BR). O suporte a inglês na interface fica para depois do MVP.

| Em inglês | Em português (pt-BR) |
| --- | --- |
| Classes, métodos, pacotes, variáveis | Telas, botões e mensagens do painel |
| Tabelas, colunas e valores de enum no banco (`symptom`, `right_knee`) | Mensagens, cartões e lembretes do bot |
| Rotas e campos da API (`/api/events`, `occurred_start`) | Comandos do Telegram (`/agua`, `/sono`, `/treino`, `/peso`, `/dor`) |
| Commits, PRs, issues, README, ADRs, comentários | Rótulos dos enums na interface ("Joelho direito", "Sintoma") |
| Logs e mensagens de erro técnicas | Conteúdo dos registros e a saída do LLM (`concept` = "dor de cabeça") |

- Os textos da interface ficam centralizados desde o início: `messages_pt_BR.properties` no backend (bot) e um arquivo `pt-BR.ts` no frontend. Isso deixa o inglês para depois sem reescrever telas.
- Formatação brasileira em tudo que é exibido: vírgula decimal, data `dd/MM/aaaa`, relógio de 24 h. Os comandos aceitam `82,4` e `82.4`.
- Comandos do Telegram não aceitam acento, por isso `/agua`. O prompt do LLM é escrito em inglês no código, mas pede `concept` e textos em pt-BR.

## Arquitetura e repositório

Um monólito modular: um único app, separado em pacotes por funcionalidade, mais o Postgres. Nada recebe conexão vinda da internet, porque é o app que busca as mensagens no Telegram.

```text
Seu celular (Telegram) <──> Telegram API <── long polling ── ┌─ app · binário nativo GraalVM ─┐
                                                              │  telegram   (polling, allowlist)│
                                                              │  entries    (comandos, propostas)│──> PostgreSQL 18
                                                              │  goals      (metas, lembretes)  │
Navegador ── HTTPS via proxy ou VPN ────────────────────────> │  dashboard  (API REST + SPA)    │──> Volume de documentos
                                                              │  llm        (texto livre → JSON)│──> LLM opcional (Ollama ou nuvem)
                                                              └─────────────────────────────────┘
```

O pacote `entries` recebe tudo o que chega (comandos, texto livre e respostas a lembretes) e decide se precisa chamar o `llm`. O `dashboard` lê e grava no mesmo banco.

**Estrutura do repositório (monorepo, nomes em inglês)**

```text
prontuario-unico/
├── backend/                Spring Boot, Maven
│   └── src/main/java/io/github/<user>/prontuario/
│       ├── telegram/       polling, commands, reply cards
│       ├── entries/        events, proposals, dedupe
│       ├── exams/          exams, analytes, documents
│       ├── appointments/   appointments, guidance
│       ├── goals/          goals, reminders
│       ├── llm/            OpenAI-compatible client
│       ├── dashboard/      REST API, read models
│       └── common/         config, security, time, i18n
├── frontend/               React + Vite + TypeScript
├── deploy/                 docker-compose.yml, .env.example, backup.sh, Caddyfile
├── docs/                   install, architecture, adr/
├── scripts/verify.sh       todas as verificações de uma mudança (backend e frontend)
└── .github/dependabot.yml  atualizações de dependências (sem workflows)
```

- Pacotes só se acessam pela camada de serviço pública de cada um; repositórios ficam internos ao pacote.
- As decisões grandes viram ADRs curtos, em inglês, em `docs/adr/`: native image, sem Hibernate, long polling, LLM opcional. Cada uma rende um post.

## Modelo de dados

Tudo gira em torno de `event`. Cada evento aponta para a `source` de onde veio, e nada vira fato sem estar `confirmed`. Separar quando algo aconteceu de quando foi registrado resolve a maior parte das datas ambíguas.

| Tabela | Para que serve | Campos principais |
| --- | --- | --- |
| `source` | Origem bruta de qualquer registro | `id`, `kind` (telegram_text, telegram_command, reminder_reply, web, document), `raw_text`, `telegram_message_id`, `document_id`, `received_at` |
| `document` | Arquivo original (PDF, foto de receita) | `id`, `filename`, `mime`, `size_bytes`, `sha256` (único), `storage_path`, `uploaded_at` |
| `event` | Sintomas e hábitos na linha do tempo | `id`, `type`, `concept`, `body_region`, `value_num`, `value_text`, `unit`, `intensity` (0–10), `occurred_start`, `occurred_end`, `time_precision`, `recorded_at`, `status`, `source_id`, `source_locator`, `created_via`, `extraction_run_id`, `dedupe_key` (único) |
| `exam` | Um exame (uma coleta de um laboratório) | `id`, `collected_on`, `lab_name`, `document_id` (opcional), `source_id`, `created_at` |
| `lab_result` | Um analito de um exame | `id`, `exam_id`, `analyte_code`, `analyte_name_raw`, `value_num`, `value_text`, `unit`, `ref_low`, `ref_high`, `ref_text`, `status`, `source_locator` (página) |
| `analyte` | Catálogo de analitos | `code`, `display_name`, `canonical_unit`, `body_region`, `aliases` (text[]) |
| `appointment` | Consultas | `id`, `occurred_on`, `specialty`, `professional_name`, `notes`, `source_id` |
| `guidance` | Orientação médica registrada | `id`, `appointment_id`, `text`, `follow_up_on`, `analyte_code` ou `concept`, `done_at` |
| `goal` | Metas | `id`, `type` (water, sleep, exercise, custom), `target_value`, `unit`, `period` (day, week), `active` |
| `reminder` | Lembretes do Telegram | `id`, `kind`, `cron`, `message`, `goal_id`, `enabled`, `last_sent_at` |
| `extraction_run` | Auditoria de cada chamada ao LLM | `id`, `model`, `prompt_version`, `input_hash`, `output_json`, `created_at` |

**Regras do modelo**

- Todo `id` é UUID v7 (ordenado pelo tempo), gerado no domínio e com `DEFAULT uuidv7()` no banco como reserva. Exceção: `analyte`, cuja chave é o `code`.
- Um `exam` agrupa os analitos de uma mesma coleta. O PDF é opcional no cadastro e pode ser anexado depois.
- `event.type`: symptom, sleep, water, exercise, weight, meal, medication, note. Consultas e exames têm tabelas próprias, mas aparecem na mesma linha do tempo por uma view.
- `event.created_via`: command (comandos rápidos), free_text (texto livre, com ou sem LLM; `extraction_run_id` diz se houve LLM), reminder_reply ou web.
- `status`: proposed → confirmed ou discarded. Só `confirmed` entra em contagens, metas e no mapa do corpo.
- Datas em `timestamptz`, exibidas no fuso `APP_TIMEZONE` (padrão `America/Sao_Paulo`). `time_precision` vale exact, day, month ou approx; "ontem à noite" vira `approx`, resolvido a partir de `recorded_at`.
- `body_region` é um enum fechado (head, neck, chest, abdomen, upper_back, lower_back, left_arm, right_arm, left_knee, right_knee, left_leg, right_leg, systemic). Cada região tem uma posição fixa no desenho do corpo.
- `dedupe_key` = hash de tipo + conceito + início arredondado ao minuto + `source_id`. Um mesmo PDF é barrado pelo `sha256`.
- A faixa de referência vem do laudo e é guardada junto do resultado. O sistema não usa faixas "padrão" próprias.
- Arquivos ficam em `DATA_DIR/documents/<sha256>`, num volume Docker, e nunca são servidos sem autenticação.

## Integração Telegram

O bot usa long polling: o app chama `getUpdates` e o Telegram nunca precisa alcançar a sua VPS, então nenhuma porta fica exposta. Só o `TELEGRAM_ALLOWED_CHAT_ID` configurado é atendido; qualquer outra mensagem é ignorada e registrada em log sem conteúdo.

**Funcionamento do polling**

- Uma virtual thread faz `getUpdates` com `timeout=50` (long poll) e processa os updates em ordem.
- O último `update_id` processado fica salvo no banco. Depois de um reinício, nada é processado em dobro.
- Em erro de rede, espera com backoff exponencial (1 s até 60 s) e tenta de novo.

**Comandos rápidos (sem LLM, gravam como confirmed na hora)**

| Comando | Exemplo | Vira |
| --- | --- | --- |
| `/agua` | `/agua 500` | water, 500 ml, agora |
| `/sono` | `/sono 6h40` ou `/sono 23:30 06:10` | sleep com duração, ou início e fim |
| `/treino` | `/treino musculação 50` | exercise, 50 min |
| `/peso` | `/peso 82.4` | weight, kg |
| `/dor` | `/dor joelho direito 4` | symptom, região right_knee, intensidade 4 |
| `/hoje` | `/hoje` | Resposta: metas do dia e da semana |
| `/desfazer` | `/desfazer` | Descarta o último registro |

**Texto livre (com LLM)**

1. Você escreve, por exemplo: "acordei com dor de cabeça, passou umas 10h".
2. O app grava a `source` e pede ao LLM um JSON estruturado (seção seguinte).
3. O app valida o JSON e cria um ou mais eventos `proposed`.
4. O bot responde com um cartão: "Dor de cabeça · hoje 08:00–10:00 · cabeça", com os botões Confirmar, Editar e Descartar.
5. Confirmar muda o status para `confirmed`. Editar abre um link para a tela do evento no painel. Descartar muda para `discarded`.
6. Propostas não respondidas em 24 h continuam visíveis no painel, marcadas como pendentes.

Sem LLM configurado, o texto livre vira um evento `note` proposto, e os comandos continuam funcionando normalmente.

**Documentos**

Um PDF ou uma foto enviados ao bot são baixados via `getFile`, gravados em `document` e respondidos com um link: "Cadastrar os resultados deste exame". Na v0.2 esse mesmo fluxo passa a extrair os valores automaticamente.

**Lembretes**

- Cada `reminder` tem um `cron` no fuso `APP_TIMEZONE` e é agendado com o `TaskScheduler` do Spring. Os agendamentos são recarregados quando um lembrete é editado.
- Padrões sugeridos na primeira execução: água às 10h, 14h e 17h; sono às 08h ("quanto você dormiu?"); treino aos domingos às 19h, com o progresso da semana.
- Os lembretes trazem botões de resposta rápida ("+250 ml", "+500 ml"; "6h", "7h", "8h"). Tocar num botão grava o evento com `source.kind = reminder_reply`.
- Os lembretes de acompanhamento vêm de `guidance.follow_up_on`: "Hoje faz 3 meses do exame de TGP. Orientação: refazer o exame."

## LLM e regras determinísticas

No MVP, o LLM faz uma única coisa: transformar texto livre em eventos propostos. Todo o resto é código determinístico, para ser previsível e testável.

**Chamada ao LLM**

- `POST {LLM_BASE_URL}/chat/completions`, com `LLM_MODEL` e `LLM_API_KEY` vindos do ambiente.
- `response_format` com JSON Schema, e a saída é mapeada para records Java: `{ events: [{ type, concept, body_region, value_num, unit, intensity, occurred_start, occurred_end, time_precision }] }`.
- O prompt recebe a data e hora atuais, o fuso e a lista fechada de `type` e `body_region`. O texto do usuário vai delimitado e tratado como dado.
- Validação depois da resposta: enums válidos, intensidade de 0 a 10, fim depois do início, data no máximo 1 h no futuro. Um item que falha a validação é descartado e o cartão avisa "não entendi parte da mensagem".
- Timeout de 30 s e uma nova tentativa. Toda chamada fica registrada em `extraction_run`, com o hash da entrada e a versão do prompt.
- `LLM_ENABLED=false` desliga a chamada por completo. Rodar com Ollama local (`http://ollama:11434/v1`) mantém os dados na sua máquina.

**Sem LLM (código puro)**

- Parser dos comandos rápidos.
- Progresso de metas: soma ou contagem de eventos `confirmed` no período, comparada com `goal.target_value`.
- Situação de cada exame: abaixo de `ref_low` → "Abaixo"; acima de `ref_high` → "Acima"; dentro → "Normal"; sem faixa → "Sem referência".
- Estado de cada ponto do corpo (regras na seção do Frontend).
- Deduplicação, normalização de unidades por tabela fixa (mL e L; minutos e horas) e exportação.

Um erro do LLM nunca vira dado sozinho: no máximo vira uma proposta errada, que você corrige ou descarta.

## API REST

A API é consumida só pelo próprio frontend, toda sob `/api`, com sessão via cookie `HttpOnly` + `SameSite=Strict` e token CSRF. Erros seguem o formato `application/problem+json`.

| Método e rota | Uso |
| --- | --- |
| `POST /api/auth/login` · `POST /api/auth/logout` | Login com senha única (hash em `APP_PASSWORD_HASH`) |
| `GET /api/overview` | Tudo da Visão geral numa chamada: pontos do corpo, metas da semana, pendências |
| `GET /api/body/{region}` | Detalhe de um ponto: série, fonte, orientação, próximo lembrete |
| `GET /api/timeline?from&to&types` | Itens da linha do tempo, paginados por cursor |
| `GET /api/timeline/lanes?from&to` | Marcas resumidas para a visão de 90 dias |
| `GET·POST·PATCH /api/events` · `/{id}` | CRUD de eventos; `PATCH` também confirma ou descarta |
| `GET·POST /api/exams` · `GET·PATCH /api/exams/{id}` | Exames e seus analitos |
| `GET /api/analytes/{code}/series` | Evolução de um analito com as faixas de referência |
| `POST /api/documents` · `GET /api/documents/{id}/file` | Upload (máx. 20 MB, PDF/JPEG/PNG) e download autenticado |
| `GET·POST·PATCH /api/appointments` · `/api/guidance` | Consultas e orientações |
| `GET·PUT /api/goals` · `GET·PUT /api/reminders` | Metas e lembretes |
| `GET /api/export` | Download de um JSON com todos os dados e o `schema_version` |
| `GET /actuator/health` | Healthcheck; o único endpoint público além do login |

O contrato fica em OpenAPI, gerado a partir dos controllers, e o cliente TypeScript do frontend é gerado dele no build. O Swagger UI fica desligado em produção.

## Frontend

O frontend é uma SPA em React que segue o design já feito no canvas: o corpo no centro, as metas à esquerda e o detalhe do ponto à direita. No build, o `dist/` vai para `src/main/resources/static` e o Spring serve tudo da mesma origem, sem precisar de CORS.

**Telas do MVP**

| Tela | Conteúdo | Observação |
| --- | --- | --- |
| Login | Senha única | Bloqueio de 15 min após 5 tentativas |
| Visão geral | Mapa do corpo, metas da semana, detalhe do ponto, pendências | Tela inicial |
| Linha do tempo | Busca (desativada até a v0.3), filtros, visão de 90 dias, histórico por dia com a fonte | Igual ao design da linha do tempo |
| Exames | Lista de exames, cadastro de analitos com o PDF ao lado, gráfico por analito | O cadastro já nasce no formato da futura tela de revisão |
| Metas e lembretes | Metas semanais e horários dos lembretes | Edição simples |
| Evento | Editar ou confirmar um registro | Destino do botão Editar do Telegram; precisa funcionar bem no celular |

**Estado de cada ponto do corpo (determinístico)**

- **Atenção:** o último exame ligado à região está fora da faixa e não melhorou em relação ao anterior; ou o sintoma teve episódio nos últimos 14 dias e a contagem dos últimos 30 dias é maior ou igual à dos 30 anteriores.
- **Em evolução:** o exame está fora da faixa mas se aproximou dela, ou existe uma orientação ativa com acompanhamento; ou a contagem do sintoma nos últimos 30 dias caiu em relação aos 30 anteriores.
- **Bom:** o exame está dentro da faixa, ou nenhum episódio do sintoma em 30 dias (desde que tenha havido algum nos últimos 180).
- Região sem dados nos últimos 180 dias não ganha ponto. O painel só mostra o que foi registrado.
- Cada estado sempre mostra o motivo no detalhe ("52 U/L, acima de 41 U/L do laudo"), nunca só a cor.

**Implementação visual**

- Corpo em SVG no MVP, igual ao design, com os pontos posicionados por `body_region`. A versão 3D com Three.js e um modelo `.glb` fica para depois, sem mudar a API.
- Gráficos em SVG próprio ou numa biblioteca leve; cada ponto do gráfico leva à sua fonte.
- Layout responsivo: no celular, as colunas viram uma pilha (corpo, detalhe, metas).

## Build nativo, memória e deploy

Desenvolva na JVM e compile o nativo só no CI. O build nativo leva minutos e usa vários GB de RAM; por isso a VPS nunca compila nada e só baixa a imagem pronta do GHCR.

O CI/CD fica num repositório privado do mantenedor, não neste (ver `docs/adr/0003-ci-cd-outside-public-repo.md`). Este repositório traz o que o CI precisa e o que permite auditar a imagem: o `Dockerfile`, o `scripts/verify.sh` e as tags `v*`, cada uma correspondente a uma imagem publicada. Quem preferir pode montar a imagem a partir do código com `docker compose build`.

**Fluxo de build**

1. No dia a dia: `./mvnw spring-boot:run` na JVM, com o Vite em `npm run dev` fazendo proxy para `/api`.
2. Antes de cada commit ou PR: `scripts/verify.sh` (testes na JVM com Testcontainers, lint, testes e build do frontend). O CI privado roda o mesmo script e depois `./mvnw -Pnative native:test` para rodar os testes no binário nativo.
3. Numa tag `v*`: Dockerfile multi-stage. Estágio 1, Node LTS (24, depois 26), compila o frontend. Estágio 2, a imagem `native-image` do GraalVM para JDK 25, roda `native:compile`. Estágio 3, runtime `distroless/base` como usuário não root, só com o binário.
4. Imagens para `amd64` e `arm64`, em runners nativos de cada arquitetura (sem emulação), publicadas no GHCR com um manifesto único.

**Cuidados no native**

- Records de JSON (Telegram, LLM, API) registrados com `@RegisterReflectionForBinding`.
- Nenhuma biblioteca que gere proxies ou leia classes em runtime sem metadados do GraalVM Reachability Metadata.
- Um teste de fumaça no CI sobe o binário com Postgres e chama `/actuator/health` e um fluxo completo de evento.

**Memória (metas a medir, não garantias)**

- App nativo: alvo de até 80 MB de RSS ocioso, com `-Xmx64m` passado em runtime. O GC padrão do GraalVM CE (Serial) é adequado para um usuário.
- Hikari com `maximum-pool-size=4`; virtual threads ligadas (`spring.threads.virtual.enabled=true`).
- Postgres enxuto: `shared_buffers=32MB`, `max_connections=10`, `work_mem=4MB`.
- Documente no README o `docker stats` medido na sua VPS, antes e depois. É um bom post para o LinkedIn.

**Docker Compose**

- Serviços: `app` e `db`; `ollama` opcional num profile (`docker compose --profile local-llm up`).
- `app` publicado em `127.0.0.1:8080`. Para acessar de fora, use um proxy reverso com TLS (Caddy) ou uma VPN (Tailscale), conforme o guia do README.
- Volumes: `pgdata` e `appdata` (documentos). Os dois precisam entrar no backup.
- Backup documentado: `pg_dump` diário mais uma cópia de `appdata`, com o script em `deploy/backup.sh`.

**Variáveis de ambiente**

| Variável | Obrigatória | Exemplo |
| --- | --- | --- |
| `APP_PASSWORD_HASH` | Sim | hash bcrypt, gerado por `docker compose run app hash-password` |
| `APP_TIMEZONE` | Não | `America/Sao_Paulo` |
| `APP_BASE_URL` | Sim | `https://saude.seudominio.com`, usado nos links do bot |
| `DB_URL` · `DB_USER` · `DB_PASSWORD` | Sim | Postgres do compose |
| `TELEGRAM_BOT_TOKEN` | Sim | Token do BotFather |
| `TELEGRAM_ALLOWED_CHAT_ID` | Sim | Seu chat ID (o bot informa no `/start`) |
| `LLM_ENABLED` · `LLM_BASE_URL` · `LLM_MODEL` · `LLM_API_KEY` | Não | `true` · `http://ollama:11434/v1` · `qwen3:8b` · vazio |
| `DATA_DIR` | Não | `/data` |

## Segurança por padrão

O código deve ser seguro mesmo quando quem instala não configura nada além do obrigatório. A operação (TLS, firewall, backup, criptografia de disco) é responsabilidade de quem roda a instância, com um guia no README.

**No código**

- O app não sobe sem `APP_PASSWORD_HASH`, `TELEGRAM_BOT_TOKEN` e `TELEGRAM_ALLOWED_CHAT_ID`. Não existe senha padrão nem usuário de demonstração.
- Porta publicada só em `127.0.0.1`; o Postgres não publica porta nenhuma.
- Sessão com cookie `HttpOnly`, `Secure` (quando atrás de HTTPS) e `SameSite=Strict`; CSRF ativo; limite de tentativas no login.
- Cabeçalhos: CSP restrita (sem scripts de terceiros), `X-Content-Type-Options`, `Referrer-Policy: no-referrer`, `frame-ancestors 'none'`.
- Upload: tipo verificado pelo conteúdo (magic bytes), não pela extensão; limite de 20 MB; nome no disco = sha256; download só autenticado e com `Content-Disposition: attachment`.
- Logs sem conteúdo clínico: registram ids, tipos e durações, nunca o texto das mensagens nem os valores.
- Sem telemetria e sem chamadas externas, exceto Telegram e o LLM configurado.
- Ao ligar um LLM de nuvem, o log de inicialização e o README avisam que o texto das mensagens vai para aquele provedor.
- CI com Dependabot, `mvn dependency-check` (ou OSV-Scanner), `npm audit` e scan da imagem com Trivy. Imagem com usuário não root, sistema de arquivos somente leitura e `cap_drop: ALL` no compose.

**Riscos de interpretação**

- O painel mostra fatos registrados, nunca diagnósticos. O README e o rodapé dizem que o projeto não substitui orientação médica.
- Faixas de referência são sempre as do laudo, e a cor de um ponto sempre vem acompanhada do motivo.

## Marcos de entrega e critérios de aceite

São cinco marcos, cada um utilizável por si só. A M2 já ajuda no dia a dia, mesmo antes de existir painel. Com cerca de 10 h por semana, a estimativa total é de 6 a 8 semanas; as maiores incertezas são o ajuste do native image e o tempo de frontend.

**M0 · Fundação (3–5 h)**

- [ ] Repositório com Apache-2.0, README inicial, `CONTRIBUTING.md` e `SECURITY.md`
- [ ] Projeto Spring Boot e Vite criados, Flyway com o schema da seção Modelo de dados
- [ ] `scripts/verify.sh` rodando os testes na JVM e as verificações do frontend; CI e `native:test` no repositório privado
- [ ] `docker compose up` sobe app e banco, e `/actuator/health` responde

**M1 · Telegram + comandos (8–12 h)**

- [ ] Polling com allowlist e offset persistido
- [ ] Todos os comandos rápidos gravando eventos confirmados
- [ ] `/hoje` responde com o progresso das metas

**M2 · Texto livre + lembretes (8–12 h)**

- [ ] Texto livre vira proposta com botões Confirmar / Editar / Descartar
- [ ] Lembretes de água, sono e treino com botões de resposta rápida
- [ ] Funciona com Ollama e com um provedor de nuvem, trocando apenas variáveis
- [ ] Usar no dia a dia por 2 semanas antes de seguir; se o hábito não pegar, rever o escopo

**M3 · Painel: visão geral e linha do tempo (15–20 h)**

- [ ] Login, Visão geral com corpo, metas e detalhe, conforme o design
- [ ] Linha do tempo com filtros, visão de 90 dias e a fonte em cada item
- [ ] Tela de Evento usada pelo botão Editar, funcionando bem no celular

**M4 · Exames, consultas e release (12–16 h)**

- [ ] Upload de PDF pelo Telegram e pela web, cadastro de analitos e gráfico por analito
- [ ] Consultas, orientações e lembretes de acompanhamento
- [ ] Exportação JSON e restauração de backup testada
- [ ] Seed com dados sintéticos para demo e screenshots
- [ ] Release `v0.1.0` com imagem nativa `amd64` + `arm64` no GHCR

**O MVP está pronto quando**

- [ ] Registrar água, sono ou uma dor leva menos de 10 s no Telegram
- [ ] "Quando começou e quantas vezes?" de um sintoma é respondido no painel em menos de 30 s
- [ ] Cada número do painel leva à sua fonte em um clique
- [ ] RSS ocioso do app nativo medido e publicado no README
- [ ] Alguém que nunca viu o projeto instala seguindo só o README, em menos de 15 min