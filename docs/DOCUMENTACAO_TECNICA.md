# Documentação Técnica: Plugin MCTrilhas

**Versão do Documento:** 1.0
**Data:** 2024-08-01

## 1. Visão Geral

Este documento detalha a arquitetura técnica, a estrutura de arquivos e os sistemas atuais e futuros do plugin MCTrilhas. O objetivo é servir como um guia central para o desenvolvimento, garantindo consistência e clareza na implementação de novas funcionalidades.

---

## 2. Estrutura de Pastas e Arquivos do Projeto

A estrutura do projeto foi organizada para separar as responsabilidades, facilitando a manutenção e a expansão do código.

```
MCTrilhas/
├── .github/workflows/
│   └── build.yml       # Automação de build (CI) e release (CD) no GitHub.
├── src/main/java/com/magnocat/mctrilhas/
│   ├── badges/         # Lógica relacionada às insígnias (definições, tipos).
│   ├── commands/       # Classes que gerenciam os comandos e subcomandos.
│   ├── data/           # Classes de gerenciamento de dados dos jogadores (PlayerData, PlayerDataManager).
│   ├── integrations/   # Classes para integração com outros plugins (PlaceholderAPI, BlueMap, etc.).
│   ├── listeners/      # "Ouvintes" de eventos do jogo (quebrar blocos, pescar, etc.).
│   ├── maps/           # Lógica para criar os mapas-troféu customizados.
│   ├── menus/          # Lógica para a GUI (interface gráfica) das insígnias.
│   ├── ranks/          # Lógica para o sistema de progressão de ranques.
│   ├── quests/         # Lógica para sistemas de missões, como a Caça ao Tesouro.
│   ├── duels/          # (Futuro) Lógica para o sistema de duelos.
│   ├── clans/          # (Futuro) Lógica para o sistema de clãs.
│   ├── plots/          # (Futuro) Lógica para gerenciar os terrenos no "Vale dos Pioneiros".
│   ├── storage/        # Gerenciamento de persistência de dados (ex: blocos colocados por jogadores).
│   ├── updater/        # Sistema de verificação e download automático de atualizações.
│   ├── web/            # Lógica para a API web integrada (servidor HTTP).
│   └── MCTrilhasPlugin.java # Classe principal do plugin, ponto de entrada.
└── src/main/resources/
    ├── config.yml      # Arquivo principal de configuração (insígnias, recompensas, etc.).
    ├── plugin.yml      # Arquivo de definição do plugin para o servidor.
    └── maps/           # Pasta para as imagens (128x128) dos mapas-troféu.
```

---

## 3. Sistemas Principais (Implementados)

### 3.1. Sistema de Insígnias e Progresso
*   **Descrição:** O sistema central do plugin. Rastreia as ações dos jogadores e concede insígnias ao atingirem metas.
*   **Fluxo de Trabalho:**
    1.  **`listeners/`**: Classes como `MiningListener` capturam eventos do jogo.
    2.  **`PlayerDataManager.addProgress()`**: O listener chama este método para registrar o progresso no cache do jogador.
    3.  **Verificação:** O método `addProgress` verifica se o progresso atingiu o `required-progress` definido no `config.yml`.
    4.  **Concessão:** Se o requisito for atingido, a insígnia é adicionada ao jogador e o método `grantReward()` é chamado.

### 3.2. Sistema de Recompensas
*   **Descrição:** Entrega recompensas configuráveis quando uma insígnia é conquistada.
*   **Fluxo de Trabalho:**
    1.  **`PlayerDataManager.grantReward()`**: Método central que lê a seção de recompensas da insígnia no `config.yml`.
    2.  **Tipos de Recompensa:**
        *   **Totens (`reward-totems`):** Usa a API do Vault para depositar a moeda.
        *   **Itens (`reward-item-data`):** Cria um `ItemStack` com material, nome, lore e encantamentos customizados.
        *   **Mapas-Troféu (`reward-map`):** Chama o `MapRewardManager` para criar um mapa com uma imagem customizada e o nome do jogador.

### 3.3. Integração Web
### 3.3. Sistema de Progressão de Ranques
*   **Descrição:** Um sistema de progressão passivo que promove jogadores com base em tempo de jogo, insígnias conquistadas e tempo de conta.
*   **Estrutura:** `ranks/Rank.java` (Enum), `ranks/RankManager.java`.
*   **Lógica:** O `RankManager` é chamado sempre que um jogador ganha uma insígnia, verificando se ele atende aos requisitos para o próximo ranque definidos no `config.yml`.

### 3.4. Caça ao Tesouro
*   **Descrição:** Uma quest repetível e aleatória para jogadores que já conquistaram a insígnia de Explorador.
*   **Estrutura:** `quests/TreasureHuntManager.java`, `quests/TreasureLocationsManager.java`, `quests/TreasureHuntRewardManager.java`.
*   **Dados:** Uma lista de coordenadas pré-definidas no `treasure_locations.yml`. O progresso de cada jogador é salvo em seu arquivo de dados.
*   **Lógica:** O sistema sorteia uma sequência única de locais para cada jogador e o guia através de bússolas mágicas. Oferece recompensas por conclusão e um grande prêmio após múltiplas conclusões.

### 3.5. Integração com PlaceholderAPI
*   **Descrição:** Expõe dados do plugin (como o ranque do jogador) para serem usados em outros plugins (de chat, nametags, scoreboards, etc.).
*   **Estrutura:** `integrations/MCTrilhasExpansion.java`.
*   **Placeholders Disponíveis:**
    *   `%mctrilhas_rank_prefix%`: Exibe o prefixo do ranque (ex: `[Pioneiro]`).
    *   `%mctrilhas_rank_name%`: Exibe apenas o nome do ranque.
    *   `%mctrilhas_rank_progress_bar%`: Exibe uma barra de progresso visual para o próximo ranque.
    *   `%mctrilhas_rank_progress_percentage%`: Exibe a porcentagem de progresso.

### 3.6. API Web Integrada
*   **Descrição:** Inicia um servidor HTTP leve dentro do próprio plugin. Este servidor tem duas funções: servir uma página web estática (localizada em `resources/web`) e fornecer um endpoint de API com dados dinâmicos do servidor.
*   **Estrutura:** `web/HttpApiManager.java`.
*   **Endpoints:**
    *   `http://<IP>:<porta>/`: Serve o arquivo `index.html` e outros arquivos estáticos (`.css`, `.js`) da pasta `web/` do plugin.
    *   `http://<IP>:<porta>/api/v1/data`: Endpoint que retorna um JSON com dados do servidor (rankings, status, etc.).
    *   `http://<IP>:<porta>/api/v1/player?token=<token>`: Retorna um JSON com dados detalhados de um jogador específico, validado pelo token.
    *   `http://<IP>:<porta>/api/v1/admin/login`: Valida as credenciais de administrador.
    *   `http://<IP>:<porta>/api/v1/admin/players/online`: (Protegido por JWT) Retorna uma lista de jogadores online.
*   **Lógica:** Responde a requisições GET (para dados) e POST (para login). Utiliza um cache interno para os endpoints de dados (`/api/v1/data` e `/api/v1/player`) para evitar sobrecarga.
*   **Segurança:**
    *   **Login do Admin:** A senha do administrador é armazenada como um hash SHA-256 com salt no `config.yml`.
    *   **Sessão do Admin:** Após o login, um token JWT é gerado e usado para autenticar requisições a endpoints protegidos (ex: `/api/v1/admin/*`).
    *   **Acesso do Jogador:** O acesso aos dados do jogador é protegido por um token único e secreto gerado em jogo.
*   **Configuração:** A porta, ativação, URL base, tempo de cache e configurações de segurança (salt, jwt-secret) são controlados pela seção `web-api` no `config.yml`.

### 3.7. Portal da Família (Painel do Jogador)
*   **Descrição:** Uma página web individual e segura para cada jogador (e sua família) acompanhar seu progresso, estatísticas e tempo de jogo.
*   **Estrutura:** `web/admin/player_dashboard.html`, `commands/FamilyCommand.java`.
*   **Fluxo de Trabalho:**
    1.  **`commands/FamilyCommand`**: O jogador usa `/familia token`. O comando gera um token de acesso único e seguro (se não existir) e o salva no `PlayerData`.
    2.  **Link Clicável**: O jogador recebe uma mensagem no chat com um link clicável para o painel, contendo o token como parâmetro de URL (ex: `.../player_dashboard.html?token=XYZ`).
    3.  **`web/HttpApiManager`**: O endpoint `/api/v1/player` recebe a requisição do painel. Ele valida o token, busca os dados do jogador (usando um cache para performance) e retorna um JSON completo com estatísticas, progresso de insígnias e requisitos.
    4.  **`web/admin/player_dashboard.html`**: O JavaScript da página consome o JSON da API e preenche dinamicamente o painel com os dados do jogador.

---

## 4. Sistemas Futuros e em Desenvolvimento

### 4.1. Sistema de Duelos 1v1 (EM FOCO)
*   **Descrição:** Um sistema de combate justo e competitivo.
*   **Estrutura:** `duels/DuelManager.java`, `duels/DuelArena.java`, `duels/DuelGame.java`, `duels/DuelKit.java`.
*   **Dados:** Novos arquivos `duel_arenas.yml` e `duel_kits.yml` serão criados. As estatísticas (vitórias, derrotas, ELO) serão adicionadas ao arquivo de dados de cada jogador.
*   **Lógica:**
    1.  Um jogador usa `/duelo desafiar <jogador>`.
    2.  O `DuelManager` registra o desafio e notifica o alvo.
    3.  Se aceito, o `DuelManager` encontra uma `DuelArena` livre.
    4.  Uma nova instância de `DuelGame` é criada, que teleporta os jogadores, aplica o kit, inicia a contagem regressiva e gerencia o combate.
    5.  Ao final, o `DuelGame` restaura o estado dos jogadores e os teleporta de volta, registrando o resultado.

### 4.2. Sistema de Clãs
*   **Descrição:** Permitirá que jogadores se organizem em grupos formais.
*   **Estrutura:** `clans/Clan.java`, `clans/ClanManager.java`.
*   **Dados:** Nova pasta `plugins/MCTrilhas/clans/` com um arquivo `.yml` para cada clã.
*   **Comandos:** `/cla criar`, `/cla convidar`, `/cla sair`, `/cla base fundar`.

### 4.3. Sistema de Comunidade e Segurança (Graylist Híbrido)
*   **Descrição:** Uma abordagem em camadas para proteger o servidor, combinando automação e interação da comunidade.
*   **Estrutura:** `community/PromotionManager.java`, `listeners/PlayerProtectionListener.java`.
*   **Lógica:**
    1.  **Graylist:** O `PlayerProtectionListener` cancela eventos (quebrar blocos, abrir baús) para jogadores com o ranque "Visitante" (a ser criado).
    2.  **Apadrinhamento:** O comando `/apadrinhar <jogador>` verifica se o autor é um membro e se o alvo é um "Visitante". Se sim, promove o alvo e registra o padrinho nos dados do novo membro. Um sistema de penalidades será acionado se o afilhado for banido.
    3.  **Aplicação Web:** Um novo endpoint no `HttpApiManager` receberá dados de um formulário do `index.html`. O formulário perguntará se o candidato já é escoteiro.
        *   Se **sim**, a aplicação é registrada para aprovação manual do admin.
        *   Se **não**, a aplicação é registrada em um arquivo separado (ex: `recrutamento.log`) e/ou enviada via webhook para um canal específico do Discord, para ser encaminhada a uma sede escoteira parceira.
    4.  **Aprovação Manual:** O comando `/aprovar <jogador>` permitirá que um admin promova um "Visitante" a membro, finalizando o processo de aplicação.

### 4.4. Sistema "Vale dos Pioneiros" (Terrenos de Jogadores)
*   **Descrição:** Um mundo de construção criativa onde jogadores podem comprar terrenos.
*   **Estrutura:** `plots/PlotManager.java`.
*   **Lógica:** Verificará o ranque e a economia (via **Vault**) do jogador para autorizar a compra.
*   **Integração:**
    *   **Multiverse:** Para criar e gerenciar o mundo "Vale dos Pioneiros".
    *   **WorldGuard:** Para criar e gerenciar as regiões protegidas de cada terreno.
    *   **BlueMap:** Para adicionar marcadores 3D customizados no mapa web, indicando a localização e o dono de cada terreno/base.

### 4.5. Painel de Administração (Web)
*   **Descrição:** Uma expansão da API Web para criar um painel de gerenciamento completo para administradores, usando o template AdminLTE. O sistema de login, sessão JWT e um dashboard básico com a lista de jogadores online já foram implementados.
*   **Estrutura:**
    *   Arquivos na pasta `resources/web/admin/`.
    *   Novos endpoints na classe `HttpApiManager` para lidar com autenticação e requisições de dados/ações.
*   **Lógica (Painel do Admin):**
    1.  **Dashboard Avançado:** Adicionar mais informações ao dashboard, como gráficos de uso de RAM/CPU e estatísticas gerais do servidor.
    2.  **Gerenciamento de Jogadores:** Criar uma interface para visualizar e editar dados de jogadores (conceder insígnias, ajustar Totens, etc.) via API.
    3.  **Ações de Moderação:** Adicionar botões na lista de jogadores para executar comandos (kick, ban) remotamente.

---

## 5. Estrutura de Comandos

### 5.1. Comandos Atuais
| Comando | Executor | Descrição |
|---|---|---|
| `/scout <subcomando>` | `ScoutCommandExecutor` | Comando principal que delega para todos os subcomandos de jogador e admin. |
| `/daily` | `DailyCommand` | Permite que jogadores coletem sua recompensa diária. |
| `/ranque` | `RankCommand` | Mostra o progresso do jogador para o próximo ranque. |
| `/tesouro <subcomando>` | `TreasureHuntCommand` | Gerencia a participação na Caça ao Tesouro. |

### 5.2. Comandos Planejados
| Comando | Descrição |
|---|---|
| `/cla <subcomando>` | Gerenciará todas as ações relacionadas a clãs (criar, convidar, etc.). |
| `/duelo <subcomando>` | Gerenciará os desafios e a participação em duelos. |
| `/acampamento <subcomando>` | Permitirá que jogadores com ranque suficiente comprem seu terreno individual. |

---

## 6. Arquivos de Dados e Configuração

*   **`config.yml`:**
    *   **Propósito:** Arquivo de configuração central. Define todas as insígnias, requisitos, recompensas, mensagens e configurações globais do plugin.
    *   **Localização:** `plugins/MCTrilhas/config.yml`

*   **`playerdata/`:**
    *   **Propósito:** Armazena os dados individuais de cada jogador em um arquivo YAML separado, nomeado com o UUID do jogador.
    *   **Conteúdo:** Insígnias conquistadas (com timestamp), progresso em cada categoria, biomas visitados e outras configurações pessoais.
    *   **Localização:** `plugins/MCTrilhas/playerdata/`

*   **`treasure_locations.yml`:**
    *   **Propósito:** Armazena a lista de todas as coordenadas possíveis para a Caça ao Tesouro.
    *   **Conteúdo:** Uma lista de strings no formato `mundo,x,y,z,descrição`.
    *   **Localização:** `plugins/MCTrilhas/treasure_locations.yml`

*   **`clans/` (Futuro):**
    *   **Propósito:** Armazenará os dados de cada clã em um arquivo YAML separado.
    *   **Conteúdo:** Nome do clã, tag, líder, oficiais, membros, nível, banco de Totens, etc.
    *   **Localização:** `plugins/MCTrilhas/clans/`