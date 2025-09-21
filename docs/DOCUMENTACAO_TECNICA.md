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

### 3.1.1. Estrutura de uma Insígnia (`badges/Badge.java`)
*   **O que é:** É uma classe de dados (Record) imutável que representa a definição de uma única insígnia.
*   **O que faz:** Armazena as propriedades estáticas de uma insígnia, carregadas a partir do `config.yml` pelo `BadgeManager`.
*   **Propriedades:**
    *   `id`: O identificador único da insígnia (ex: "mining").
    *   `name`: O nome de exibição (ex: "Minerador Mestre").
    *   `description`: A descrição que aparece na GUI.
    *   `type`: O `BadgeType` associado, que liga a insígnia a uma ação específica do jogo.
    *   `requirement`: O valor numérico necessário para conquistar a insígnia.
    *   `icon`: O material do ícone que representa a insígnia na GUI.

### 3.1.2. Gerenciador de Insígnias (`badges/BadgeManager.java`)
*   **O que é:** É a classe responsável por carregar e gerenciar todas as definições de insígnias.
*   **O que faz:**
    *   **Carregamento:** No início do plugin, o método `loadBadgesFromConfig` lê a seção `badges` do `config.yml`.
    *   **Criação:** Para cada entrada válida, ele cria um `record` do tipo `Badge` e o armazena em um mapa interno (`Map<String, Badge>`).
    *   **Acesso:** Fornece métodos para obter uma insígnia específica por seu ID (`getBadge`) ou uma lista de todas as insígnias carregadas (`getAllBadges`).
*   **Dependências:** `MCTrilhasPlugin`, `BadgeConfigManager`, `Badge`, `BadgeType`.

### 3.1.2.1. Gerenciador de Configuração de Insígnias (`managers/BadgeConfigManager.java`) - [OBSOLETO]
*   **O que é:** Uma classe legada que foi originalmente criada para gerenciar um arquivo `badges.yml` separado.
*   **Status Atual:** **OBSOLETA**. Esta classe agora é redundante, pois todas as configurações de insígnias foram movidas para o `config.yml` principal, que é gerenciado diretamente pela classe `MCTrilhasPlugin`.
*   **Plano de Ação:** A classe foi marcada como `@Deprecated`. Seu uso deve ser evitado. As classes que ainda a utilizam serão refatoradas para usar `plugin.getConfig()` para acesso geral à configuração e `plugin.getBadgeManager()` para obter dados específicos de insígnias, respeitando a arquitetura de "fonte única da verdade". A classe será removida completamente após a refatoração.

### 3.1.3. Tipos de Insígnia (`badges/BadgeType.java`)
*   **O que é:** Um `enum` que define as categorias de progresso rastreáveis.
*   **O que faz:** Cada constante do enum (ex: `MINING`) serve como um ID único para um tipo de ação no jogo. Ele atua como a "cola" que liga um evento (capturado por um `Listener`) à sua respectiva insígnia no `config.yml` e ao seu contador de progresso no `PlayerData`.
*   **Propriedades:**
    *   **Constante Enum:** O nome da constante (ex: `MINING`) é usado como o ID interno e deve corresponder à chave da insígnia no `config.yml`.
    *   **`displayName`:** Um nome amigável para a categoria (ex: "Mineração"), usado para exibição em GUIs ou mensagens.

### 3.2. Sistema de Recompensas
*   **Descrição:** Entrega recompensas configuráveis quando uma insígnia é conquistada.
*   **Fluxo de Trabalho:**
    1.  **`PlayerDataManager.grantReward()`**: Método central que lê a seção de recompensas da insígnia no `config.yml`.
    2.  **Tipos de Recompensa:**
        *   **Totens (`reward-totems`):** Usa a API do Vault para depositar a moeda.
        *   **Itens (`reward-item-data`):** Cria um `ItemStack` com material, nome, lore e encantamentos customizados.
        *   **Mapas-Troféu (`reward-map`):** Chama o `MapRewardManager` para criar um mapa com uma imagem customizada e o nome do jogador.

### 3.2.1. Sistema de Renderização de Mapas
*   **Descrição:** Um sistema para criar mapas customizados com imagens estáticas.
*   **Estrutura:** `maps/MapRewardManager.java`, `maps/ImageMapRenderer.java`.
*   **Lógica:**
    1.  O `MapRewardManager` é chamado quando uma recompensa de mapa é concedida.
    2.  Ele cria uma nova `MapView` no mundo do jogador.
    3.  Ele instancia o `ImageMapRenderer`, passando o caminho da imagem da insígnia (localizada em `resources/maps/`).
    4.  O `ImageMapRenderer` carrega a imagem do JAR do plugin.
    5.  O `ImageMapRenderer` é adicionado à `MapView`. Na primeira vez que o mapa é renderizado para um jogador, o renderer desenha a imagem no `MapCanvas`.
    6.  Uma otimização garante que a imagem seja desenhada apenas uma vez por mapa, economizando recursos.
    7.  O `MapRewardManager` então cria um `ItemStack` do tipo `FILLED_MAP`, associa a `MapView` a ele e o entrega ao jogador.

### 3.3. Sistema de Progressão de Ranques
*   **Descrição:** Um sistema de progressão passivo que promove jogadores com base em tempo de jogo, insígnias conquistadas e tempo de conta.
*   **Estrutura:** `ranks/Rank.java` (Enum), `ranks/RankManager.java`.
*   **Lógica:** O `RankManager` é chamado sempre que um jogador ganha uma insígnia, verificando se ele atende aos requisitos para o próximo ranque definidos no `config.yml`.

### 3.3.1. Definição de Ranques (`ranks/Rank.java`)
*   **O que é:** Um `enum` que define todos os ranques disponíveis no servidor e sua hierarquia.
*   **O que faz:**
    *   **Hierarquia:** A ordem das constantes no enum define a sequência de progressão (de `FILHOTE` a `PIONEIRO`).
    *   **Propriedades:** Cada ranque armazena um `displayName` (nome formatado) e um `color` (código de cor para o chat).
    *   **Lógica de Progressão:** Contém o método `getNext()` que retorna o próximo ranque na hierarquia, crucial para o `RankManager` determinar qual é a próxima meta do jogador.
    *   **Ranques Especiais:** O ranque `CHEFE` é tratado como um ranque especial, geralmente atribuído manualmente, e não faz parte da linha de progressão automática.
*   **Dependências:** Nenhuma. É uma classe de definição autossuficiente.

### 3.3.2. Gerenciador de Ranques (`ranks/RankManager.java`)
*   **O que é:** A classe que contém a lógica de promoção de ranques.
*   **O que faz:**
    1.  O método `checkAndPromote` é chamado sempre que um jogador completa uma ação significativa (como ganhar uma insígnia).
    2.  Ele verifica o ranque atual do jogador e os requisitos para o próximo ranque, definidos no `config.yml` (tempo de jogo, número de insígnias, idade da conta).
    3.  Se o jogador atende a todos os requisitos, ele é promovido.
    4.  O processo se repete em um loop, permitindo que um jogador seja promovido por múltiplos ranques de uma só vez se ele atender aos requisitos.
*   **Dependências:** `MCTrilhasPlugin`, `PlayerDataManager`, `Rank`.

### 3.4. Caça ao Tesouro
*   **Descrição:** Uma quest repetível e aleatória para jogadores que já conquistaram a insígnia de Explorador.
*   **Estrutura:** `quests/TreasureHuntManager.java`, `quests/TreasureLocationsManager.java`, `quests/TreasureHuntRewardManager.java`.
*   **Dados:** Uma lista de coordenadas pré-definidas no `treasure_locations.yml`. O progresso de cada jogador é salvo em seu arquivo de dados.
*   **Lógica:** O sistema sorteia uma sequência única de locais para cada jogador e o guia através de bússolas mágicas. Oferece recompensas por conclusão e um grande prêmio após múltiplas conclusões.

### 3.5. Integração com PlaceholderAPI
*   **Descrição:** Expõe dados do plugin (como o ranque do jogador) para serem usados em outros plugins (de chat, nametags, scoreboards, etc.).
*   **Estrutura:** `integrations/MCTrilhasExpansion.java`.
*   **Placeholders Disponíveis:**
    *   `%mctrilhas_rank%`: Retorna o ID do ranque (ex: `ESCOTEIRO`).
    *   `%mctrilhas_rank_formatted%`: Retorna o nome formatado do ranque (ex: `Escoteiro`).
    *   `%mctrilhas_rank_progress%`: Mostra o progresso para o próximo ranque (ex: `15/20 Insígnias`).
    *   `%mctrilhas_badges_daily%`: Contagem de insígnias ganhas hoje.
    *   `%mctrilhas_badges_monthly%`: Contagem de insígnias ganhas no mês.
    *   `%mctrilhas_badges_alltime%`: Contagem total de insígnias.
    *   `%mctrilhas_rank_pos_daily%`: Posição no ranking diário (ex: `#1`).
    *   `%mctrilhas_rank_pos_monthly%`: Posição no ranking mensal.
    *   `%mctrilhas_rank_pos_alltime%`: Posição no ranking geral.

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

### 3.6.1. Servidor de Arquivos Seguro (`web/SecureHttpFileHandler.java`)
*   **O que é:** Um manipulador de requisições HTTP (`HttpHandler`) especializado em servir arquivos estáticos.
*   **O que faz:** É responsável por entregar todos os arquivos dos painéis web (HTML, CSS, JS, imagens) ao navegador do usuário.
*   **Segurança:** Sua principal característica é a proteção contra ataques de "Path Traversal". Ele valida o caminho de cada arquivo solicitado para garantir que ele esteja estritamente dentro do diretório `web/` do plugin, impedindo o acesso a arquivos sensíveis do servidor.
*   **Lógica Adicional:**
    *   **MIME Types:** Identifica a extensão do arquivo para enviar o `Content-Type` correto ao navegador (ex: `text/html`, `image/png`).
    *   **Página 404:** Se um arquivo não é encontrado, ele tenta servir uma página `404.html` personalizada, se existir.
*   **Dependências:** Utilizado pelo `HttpApiManager` para lidar com todas as requisições que não são para a API.

### 3.7. Portal da Família (Painel do Jogador)
*   **Descrição:** Uma página web individual e segura para cada jogador (e sua família) acompanhar seu progresso, estatísticas e tempo de jogo.
*   **Estrutura:** `web/admin/pdash.html`, `commands/FamilyCommand.java`.
*   **Fluxo de Trabalho:**
    1.  **`commands/FamilyCommand`**: O jogador usa `/familia token`. O comando gera um token de acesso único e seguro (se não existir) e o salva no `PlayerData`.
    2.  **Link Clicável**: O jogador recebe uma mensagem no chat com um link clicável para o painel, contendo o token como parâmetro de URL (ex: `.../admin/pdash.html?token=XYZ`).
    3.  **`web/HttpApiManager`**: O endpoint `/api/v1/player` recebe a requisição do painel. Ele valida o token, busca os dados do jogador (usando um cache para performance) e retorna um JSON completo com estatísticas, progresso de insígnias e requisitos.
    4.  **`web/admin/pdash.html`**: O JavaScript da página consome o JSON da API e preenche dinamicamente o painel com os dados do jogador.

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

### 3.8. Sistema de Capture The Flag (CTF)
*   **Descrição:** Um minigame competitivo onde duas equipes se enfrentam para capturar a bandeira inimiga.
*   **Estrutura:** `ctf/CTFManager.java`, `ctf/CTFGame.java`, `ctf/CTFArena.java`, `ctf/CTFTeam.java`.
*   **Dados:**
    *   **`ctf_arenas.yml`**: Armazena a configuração de todas as arenas, incluindo nome, número de jogadores e as coordenadas do lobby, spawns e bandeiras.
    *   **`playerdata/<UUID>.yml`**: As estatísticas de cada jogador (vitórias, abates, capturas) são salvas em uma seção `ctf-stats` dentro do seu arquivo de dados.
*   **Lógica:**
    1.  **Fila:** O `CTFManager` gerencia uma fila de jogadores (`/ctf join`).
    2.  **Início da Partida:** Quando há jogadores suficientes e uma arena livre, uma contagem regressiva é iniciada. Ao final, uma instância de `CTFGame` é criada.
    3.  **Gerenciamento do Jogo:** A classe `CTFGame` controla toda a lógica da partida: teleporta os jogadores, aplica kits, gerencia o placar, o tempo e as regras de captura e retorno da bandeira.
    4.  **Fim da Partida:** Ao final, `CTFGame` anuncia o vencedor, salva as estatísticas dos jogadores e os retorna ao lobby. O `CTFManager` então remove o jogo da lista de partidas ativas.
*   **Comandos:**
    *   `/ctf join`: Entra na fila.
    *   `/ctf leave`: Sai da fila ou da partida.
    *   `/ctf admin create/set/save`: Comandos para administradores criarem novas arenas.

### 3.9. Sistema de HUD (Heads-Up Display)
*   **Descrição:** Um sistema de exibição de informações na tela do jogador através de uma `BossBar`.
*   **Estrutura:** `hud/HUDManager.java`.
*   **Lógica:**
    1.  O jogador usa o comando `/hud` para ativar ou desativar a exibição.
    2.  O `HUDManager` cria uma `BossBar` para o jogador e a adiciona a um mapa de HUDs ativos.
    3.  Uma tarefa assíncrona (`BukkitRunnable`) é executada periodicamente para atualizar as informações.
    4.  Para cada jogador com HUD ativo, a tarefa busca os dados mais recentes (ranque, Totens, contagem de insígnias) e atualiza o título da `BossBar`.
    5.  A `BossBar` é removida automaticamente quando o jogador sai do servidor ou quando o plugin é desativado/recarregado.

---

## 5. Estrutura de Comandos

### 5.0.1. Arquitetura de Comandos (Interface SubCommand)
*   **O que é:** A interface `SubCommand.java` é o "molde" para todos os subcomandos do plugin.
*   **O que faz:** Ela define um contrato padrão que todas as classes de subcomando devem seguir. Isso garante que cada comando tenha um nome, descrição, sintaxe, permissão e um método de execução, além de um método padrão para autocompletar.
*   **Vantagens:** Esta abordagem torna o sistema de comandos modular e fácil de expandir. Para adicionar um novo subcomando, basta criar uma nova classe que implemente esta interface e registrá-la no roteador de comandos apropriado (como `ScoutCommandExecutor` ou `AdminSubCommand`).

### 5.1. Comandos Atuais
| Comando | Executor | Descrição |
|---|---|---|
| `/scout <subcomando>` | `ScoutCommandExecutor` | Comando principal que delega para todos os subcomandos de jogador e admin. |
| `/daily` | `DailyCommand` | Permite que um jogador colete sua recompensa diária (Totens e/ou itens) após um cooldown configurável. |
| `/ranque` | `RankCommand` | Exibe o ranque atual do jogador e os requisitos para o próximo. |
| `/familia token` | `FamilyCommand` | Gera um link de acesso único e seguro para o "Portal da Família" do jogador. |
| `/hud` | `HUDCommand` | Ativa ou desativa a exibição de informações na tela (HUD). |
| `/tesouro <subcomando>` | `TreasureHuntCommand` | Gerencia a participação do jogador na Caça ao Tesouro (iniciar, obter pista, cancelar). |

### 5.1.1. Subcomandos de Jogador (`/scout ...`)
| Subcomando | Classe | Descrição |
|---|---|---|
| `badges [jogador]` | `BadgesSubCommand` | Exibe as insígnias conquistadas pelo jogador (em GUI ou chat). |
| `progress [jogador]` | `ProgressSubCommand` | Mostra o progresso detalhado para as próximas insígnias e ranque. |
| `getmap <insignia>` | `GetMapSubCommand` | Recupera o mapa-troféu de uma insígnia já conquistada. |
| `toggleprogress` | `ToggleProgressSubCommand` | Ativa ou desativa as mensagens de progresso de insígnias no chat. |
| `version` | `VersionSubCommand` | Exibe a versão atual do plugin. |

#### Detalhes do `/scout getmap`
*   **Fluxo de Trabalho:**
    1.  Verifica se o jogador que executou o comando já conquistou a insígnia especificada.
    2.  Verifica se a insígnia em questão possui uma recompensa do tipo `reward-map` no `config.yml`.
    3.  Verifica se o jogador já possui uma cópia do mapa-troféu em seu inventário para evitar duplicação.
    4.  Se todas as condições forem atendidas e o jogador não tiver o mapa, o `MapRewardManager` é chamado para criar e entregar uma nova cópia do troféu.
*   **Autocompletar Inteligente:** O comando sugere apenas as insígnias que o jogador já conquistou e que possuem um mapa-troféu associado.

### 5.1.2. Subcomandos de Administração (`/scout admin ...`)
O comando `/scout admin` é gerenciado pela classe `AdminSubCommand`, que atua como um roteador, delegando a execução para os seguintes subcomandos específicos:

| Subcomando | Classe | Descrição |
|---|---|---|
| `addbadge <jogador> <insignia>` | `AddBadgeSubCommand` | Concede uma insígnia e sua recompensa a um jogador online. |
| `removebadge <jogador> <insignia>` | `RemoveBadgeSubCommand` | Remove uma insígnia de um jogador (online ou offline) e zera o progresso associado. |
| `stats <jogador>` | `StatsSubCommand` | Exibe as estatísticas de progresso detalhadas de um jogador (online ou offline). |
| `reload` | `ReloadSubCommand` | Recarrega os arquivos de configuração (`config.yml`, `treasure_locations.yml`) e os caches dependentes (insígnias, locais de tesouro). |


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

### 6.1. Gerenciador de Dados do Jogador (`data/PlayerDataManager.java`)
*   **O que é:** É a classe central para toda a persistência de dados dos jogadores. Ela atua como uma camada de abstração entre o plugin e os arquivos YAML individuais de cada jogador.
*   **O que faz:**
    *   **Carregamento e Salvamento:** Gerencia a leitura (`loadPlayerData`) e a escrita (`savePlayerData`) dos dados dos jogadores em arquivos `playerdata/<UUID>.yml`.
    *   **Cache:** Mantém um cache em memória (`playerDataCache`) com os dados de todos os jogadores online para evitar operações de disco constantes e garantir alta performance.
    *   **Operações Assíncronas:** Realiza operações pesadas, como o cálculo de rankings (`getAllTimeBadgeCountsAsync`), de forma assíncrona para não travar o servidor.
    *   **Gerenciamento de Tokens:** Possui um cache otimizado (`tokenToUuidCache`) para validar rapidamente os tokens de acesso do Portal da Família.
    *   **Lógica de Migração:** Contém a lógica para atualizar automaticamente os arquivos de dados de jogadores de formatos antigos para novos quando eles fazem login.
*   **Dependências:** `MCTrilhasPlugin`, `PlayerData`, `Rank`, `BadgeType`, `ItemFactory`.

---

## 7. Automação e Build (CI/CD)

### 7.1. Workflow de Build (`.github/workflows/build.yml`)
*   **O que é:** É um workflow do GitHub Actions que automatiza o processo de compilação e distribuição do plugin.
*   **O que faz:**
    *   **Gatilhos:** É acionado automaticamente em duas situações:
        1.  Quando um novo código é enviado (`push`) para a branch `main`.
        2.  Quando uma nova tag de versão (ex: `v1.2.3`) é criada.
    *   **Processo de Build:**
        1.  Configura um ambiente Linux com Java 17.
        2.  Valida a sintaxe do arquivo `plugin.yml` para evitar erros de configuração.
        3.  Usa o Maven para compilar o código-fonte e empacotar o plugin em um arquivo `.jar`. A versão do plugin é definida dinamicamente com base no gatilho (versão de desenvolvimento para a `main`, versão da tag para releases).
    *   **Distribuição:**
        *   **Build de Desenvolvimento:** Para pushes na `main`, o `.jar` resultante é salvo como um "artefato" do build, disponível para download e testes.
        *   **Release Oficial:** Para novas tags, o workflow cria automaticamente um novo "Release" na página do GitHub e anexa o arquivo `.jar` a ele, tornando-o público.
*   **Dependências:** O workflow depende do ambiente do GitHub Actions, do Maven e de actions da comunidade para tarefas específicas como `actions/checkout`, `actions/setup-java`, e `softprops/action-gh-release`.

---

## 8. Ambiente de Desenvolvimento

### 8.1. Configurações do VSCode (`.vscode/settings.json`)
*   **O que é:** Um arquivo que define configurações específicas para o editor Visual Studio Code, aplicadas automaticamente quando o projeto é aberto.
*   **O que faz:**
    *   **`java.compile.nullAnalysis.mode`:** Ativa a análise de código em tempo real para detectar possíveis erros de `NullPointerException`, aumentando a qualidade e a segurança do código.
    *   **`java.configuration.updateBuildConfiguration`:** Garante que o VSCode atualize automaticamente sua configuração de build sempre que o arquivo `pom.xml` for modificado, mantendo as dependências e a estrutura do projeto sempre sincronizadas.
*   **Propósito:** O objetivo deste arquivo é padronizar o ambiente de desenvolvimento para todos os colaboradores que usam o VSCode, garantindo consistência e ajudando a evitar erros comuns. Ele não afeta o resultado final da compilação do plugin.