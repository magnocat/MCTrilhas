# Estrutura Visual do Projeto MCTrilhas

Este documento fornece uma visão geral da estrutura de pastas e arquivos do projeto, com uma breve descrição da finalidade de cada componente principal.

```
MCTrilhas/
├── .github/workflows/
│   └── build.yml       # Automação de build (CI/CD) via GitHub Actions. Compila o projeto e cria releases.
│
├── .vscode/
│   └── settings.json   # Configurações do VSCode para padronizar o ambiente de desenvolvimento.
│
├── docs/
│   ├── APRESENTACAO_CHEFIA.md    # Texto de apresentação do projeto para a liderança escoteira.
│   ├── DOCUMENTACAO_TECNICA.md   # Documentação central da arquitetura e sistemas do plugin.
│   ├── ESTRUTURA_PROJETO.md      # Este arquivo, com a visão geral da estrutura do projeto.
│   ├── GUIA_CRIACAO_ARENAS.md    # Tutorial para administradores criarem arenas de CTF e Duelo.
│   └── GUIA_CRIACAO_NPC.md       # Guia passo a passo para criar e configurar novos NPCs.
│   └── TODO_GEMINI.md            # Resumo do projeto, regras de interação e backlog de tarefas.
│
├── src/main/java/com/magnocat/mctrilhas/
│   ├── MCTrilhasPlugin.java      # Classe principal do plugin. Ponto de entrada que inicializa todos os sistemas.
│   │
│   └── badges/
│       ├── AddBadgeSubCommand.java       # Implementa o subcomando de admin `/scout admin addbadge` para conceder uma insígnia.
│       ├── Badge.java                    # Classe `record` imutável que representa a definição de uma insígnia.
│       ├── BadgeConfigManager.java       # (Arquivo vazio) Classe obsoleta, marcada para remoção futura.
│       ├── BadgeManager.java             # Gerencia o carregamento e o acesso às definições de todas as insígnias a partir do `config.yml`.
│       ├── BadgeMenu.java                # Cria e gerencia a interface gráfica (GUI) para visualização das insígnias.
│       ├── BadgesSubCommand.java         # Implementa o subcomando `/scout badges` para exibir as insígnias de um jogador.
│       ├── BadgeType.java                # Enum que define as categorias de progresso rastreáveis (MINING, COOKING, etc.).
│       ├── BuilderListener.java          # Ouve o evento de colocar blocos para o progresso da insígnia de Construtor.
│       ├── CookingListener.java          # Ouve o evento de retirar itens da fornalha para o progresso da insígnia de Cozinheiro.
│       ├── CraftingListener.java         # Ouve o evento de criar itens para o progresso da insígnia de Artesão.
│       ├── ExplorerListener.java         # Ouve o movimento do jogador para rastrear biomas visitados (insígnia de Explorador).
│       ├── FarmingListener.java          # Ouve o evento de quebrar plantações maduras para o progresso da insígnia de Agricultor.
│       ├── FishingListener.java          # Ouve o evento de pescar peixes para o progresso da insígnia de Pescador.
│       ├── LumberjackListener.java       # Ouve o evento de quebrar troncos para o progresso da insígnia de Lenhador.
│       ├── MiningListener.java           # Ouve o evento de quebrar pedras/minérios para o progresso da insígnia de Minerador.
│       ├── MobKillListener.java          # Ouve o evento de morte de monstros para o progresso da insígnia de Caçador.
│       ├── ProgressSubCommand.java       # Implementa o subcomando `/scout progress` para mostrar o progresso atual do jogador.
│       ├── RemoveBadgeSubCommand.java    # Implementa o subcomando de admin `/scout admin removebadge` para remover uma insígnia.
│       ├── StatsSubCommand.java          # Implementa o subcomando de admin `/scout admin stats` para ver estatísticas detalhadas.
│       ├── TamingListener.java           # Ouve o evento de domar animais para o progresso da insígnia de Domador.
│       └── ToggleProgressSubCommand.java # Implementa o subcomando `/scout toggleprogress` para ativar/desativar mensagens de progresso.
│   │
│   └── commands/
│       ├── AdminSubCommand.java        # Roteador para todos os subcomandos de administração (`/scout admin`).
│       ├── DailyCommand.java           # Implementa o comando `/daily` para recompensas diárias.
│       ├── EmoteCommand.java           # Implementa o comando `/emote` para executar animações.
│       ├── FamilyCommand.java          # Implementa o comando `/familia token` para gerar o link do painel do jogador.
│       ├── GetMapSubCommand.java       # Implementa o subcomando `/scout getmap` para recuperar mapas-troféu de insígnias.
│       ├── ReloadSubCommand.java       # Implementa o subcomando `/scout admin reload` para recarregar as configurações.
│       ├── RulesCommand.java           # Implementa o comando `/regras` para exibir as regras do servidor.
│       ├── ScoutCommandExecutor.java   # Executor principal e roteador para o comando `/scout` e seus subcomandos.
│       ├── SubCommand.java             # Interface que define o contrato padrão para todos os subcomandos.
│       └── VersionSubCommand.java      # Implementa o subcomando `/scout version` para exibir a versão do plugin.

│   │
│   └── ctf/
│       ├── AdminSubCommand.java            # Roteador para os subcomandos de admin do CTF (`/ctf admin`).
│       ├── ArenaBuilder.java               # Classe temporária para armazenar dados de uma arena durante sua criação.
│       ├── CancelCTFAdminSubCommand.java   # Implementa o comando `/ctf admin cancel`.
│       ├── CreateCTFAdminSubCommand.java   # Implementa o comando `/ctf admin create`.
│       ├── CTFArena.java                   # Classe de dados que representa uma arena de CTF carregada e pronta para uso.
│       ├── CTFCommand.java                 # Executor principal do comando `/ctf`, que roteia para todos os seus subcomandos.
│       ├── CTFFlag.java                    # Gerencia o estado e a lógica de uma única bandeira (na base, carregada, caída).
│       ├── CTFGame.java                    # Classe central que gerencia uma partida de CTF em andamento.
│       ├── CTFListener.java                # Ouve eventos do jogo (morte, movimento, chat) e os direciona para a partida correta.
│       ├── CTFManager.java                 # Gerenciador principal do sistema CTF. Controla filas, arenas e o ciclo de vida das partidas.
│       ├── CTFMilestoneManager.java        # Gerencia e concede recompensas por marcos históricos (ex: 100 vitórias).
│       ├── CTFPlayerStats.java             # Armazena as estatísticas de um jogador durante uma única partida.
│       ├── CTFScoreboard.java              # Gerencia o placar (scoreboard) da barra lateral para uma partida.
│       ├── CTFTeam.java                    # Representa uma equipe dentro de uma partida, armazenando jogadores e pontuação.
│       ├── FlagState.java                  # Enum que define os estados possíveis de uma bandeira (AT_BASE, CARRIED, DROPPED).
│       ├── GameState.java                  # Enum que define os estados de uma partida (WAITING, STARTING, IN_PROGRESS, ENDING).
│       ├── JoinSubCommand.java             # Implementa o comando `/ctf join` para entrar na fila.
│       ├── LeaveSubCommand.java            # Implementa o comando `/ctf leave` para sair da fila ou partida.
│       ├── ListSubCommand.java             # Implementa o comando `/ctf list` para listar as arenas.
│       ├── SaveCTFAdminSubCommand.java     # Implementa o comando `/ctf admin save`.
│       ├── SetCTFAdminSubCommand.java      # Implementa o comando `/ctf admin set` para definir um local da arena.
│       ├── StatsSubCommand.java            # Implementa o comando `/ctf stats` para ver as estatísticas.
│       ├── StatusCTFAdminSubCommand.java   # Implementa o comando `/ctf admin status` para ver o status da criação da arena.
│       └── TeamColor.java                  # Enum que define as cores das equipes, incluindo suas cores de chat, armadura e estandarte.
│   │
│   └── data/
│       ├── ActivityTracker.java    # Rastreia o tempo de jogo ativo dos jogadores, ignorando o tempo AFK.
│       ├── PlayerCTFStats.java     # Armazena as estatísticas permanentes de um jogador no modo CTF.
│       ├── PlayerData.java         # Objeto que armazena todos os dados de um jogador em memória (insígnias, progresso, ranque, etc.).
│       ├── PlayerDataManager.java  # Gerenciador central que carrega, salva e gerencia os dados de todos os jogadores.
│       └── PlayerState.java        # Salva um "snapshot" do estado de um jogador (inventário, vida) para restauração posterior.
│   │
│   └── duels/
│       ├── AcceptSubCommand.java           # Implementa o comando `/duelo aceitar`.
│       ├── AdminDuelSubCommand.java        # Roteador para os subcomandos de admin de duelo (`/scout admin duel ...`).
│       ├── CancelArenaSubCommand.java      # Implementa o comando `/scout admin duel cancel`.
│       ├── Challenge.java                  # Classe `record` que representa um desafio de duelo pendente.
│       ├── ChallengeSubCommand.java        # Implementa o comando `/duelo desafiar`.
│       ├── CreateArenaSubCommand.java      # Implementa o comando `/scout admin duel createarena`.
│       ├── DenySubCommand.java             # Implementa o comando `/duelo negar`.
│       ├── DuelArena.java                  # Classe de dados que representa uma arena de duelo e seus spawns.
│       ├── DuelCommand.java                # Executor principal do comando `/duelo`, que roteia para todos os seus subcomandos.
│       ├── DuelGame.java                   # Classe central que gerencia uma partida de duelo em andamento.
│       ├── DuelKit.java                    # Representa um kit de itens e armadura para um duelo.
│       ├── DuelListener.java               # Ouve eventos de inventário relacionados à seleção de kits.
│       ├── DuelManager.java                # Gerenciador principal do sistema de Duelos (arenas, kits, filas, partidas).
│       ├── DuelRewardManager.java          # Gerencia a distribuição de recompensas semanais do ranking ELO.
│       ├── EloCalculator.java              # Utilitário para calcular a mudança de ELO após uma partida.
│       ├── ForfeitSubCommand.java          # Implementa o comando `/duelo desistir`.
│       ├── GameListener.java               # Ouve eventos do jogo (morte, desconexão, comandos) para controlar a partida.
│       ├── KitSelectionMenu.java           # Constrói e gerencia a GUI para seleção de kits.
│       ├── KitsSubCommand.java             # Implementa o comando `/duelo kits`.
│       ├── LeaveQueueSubCommand.java       # Implementa o comando `/duelo sairfila`.
│       ├── LeaveSpectateSubCommand.java    # Implementa o comando `/duelo sair` para parar de assistir.
│       ├── PlayerDuelStats.java            # Armazena as estatísticas permanentes de um jogador no modo Duelo (ELO, vitórias, derrotas).
│       ├── QueuedDuel.java                 # Classe `record` que representa um duelo aguardando na fila.
│       ├── ReloadKitsSubCommand.java       # Implementa o comando `/scout admin duel reloadkits`.
│       ├── SaveArenaSubCommand.java        # Implementa o comando `/scout admin duel save`.
│       ├── SetSpawnSubCommand.java         # Implementa os comandos `/scout admin duel setspawn1` e `setspawn2`.
│       ├── SetSpecSubCommand.java          # Implementa o comando `/scout admin duel setspec`.
│       ├── SpectateSubCommand.java         # Implementa o comando `/duelo assistir`.
│       ├── StatsSubCommand.java            # Implementa o comando `/duelo stats`.
│       └── TopSubCommand.java              # Implementa o comando `/duelo top` para exibir o ranking ELO.
│   │
│   └── hud/
│       ├── HUDCommand.java         # (Arquivo vazio) Classe placeholder para um comando legado.
│       ├── HUDManager.java         # Gerencia a criação, atualização e remoção da BossBar de estatísticas (HUD).
│       └── HUDSubCommand.java      # Implementa o subcomando `/scout hud` para ativar/desativar a BossBar.
│   │
│   └── integrations/
│       └── MCTrilhasExpansion.java # Implementa a expansão para o PlaceholderAPI, fornecendo placeholders customizados.
│   │
│   └── listeners/
│       ├── AdminPrivacyListener.java       # Gerencia a privacidade de admins, escondendo-os de rankings e ativando vanish.
│       ├── CommandBlockerListener.java     # Bloqueia comandos para não-admins, exceto os permitidos na config.
│       ├── GameChatListener.java           # Captura mensagens do chat para exibir no painel de administração web.
│       ├── MenuListener.java               # Gerencia cliques em GUIs customizadas (menus de insígnias, diálogos).
│       ├── PlayerJoinListener.java         # Executa ações quando um jogador entra (carrega dados, envia boas-vindas, etc.).
│       ├── PlayerProtectionListener.java   # Protege o mundo contra interações de jogadores com o ranque VISITANTE.
│       ├── PlayerQuitListener.java         # Executa ações quando um jogador sai (salva dados, limpa caches).
│       ├── PunishmentListener.java         # Ouve eventos de banimento para aplicar penalidades ao padrinho do jogador.
│       └── TreasureHuntListener.java       # Ouve o movimento do jogador para verificar se ele encontrou um local de tesouro.
│   │
│   └── maps/
│       ├── ImageMapRenderer.java   # Renderizador customizado que desenha uma imagem estática em um item de mapa.
│       └── MapRewardManager.java   # Gerencia a criação e restauração dos mapas-troféu dados como recompensa.
│   │
│   └── npc/
│       ├── Dialogue.java           # Classe `record` que representa uma tela de diálogo com texto e escolhas.
│       ├── DialogueChoice.java     # Classe `record` que representa uma única opção de escolha em um diálogo.
│       ├── DialogueManager.java    # Gerencia o carregamento de diálogos do `dialogues.yml` e inicia as conversas.
│       ├── DialogueMenu.java       # Constrói e gerencia a interface gráfica (GUI) para os diálogos.
│       ├── Npc.java                # Classe `record` que armazena os dados de um NPC (ID, nome, localização, skin).
│       ├── NpcAdminSubCommand.java # Implementa o subcomando de admin `/scout admin npc` para gerenciar NPCs.
│       ├── NPCListener.java        # Ouve a interação do jogador com entidades NPC para iniciar diálogos.
│       └── NPCManager.java         # Gerenciador principal que carrega, gera, salva e gerencia os NPCs no mundo.

│   │
│   └── pet/
│       ├── AdminPetSubCommand.java         # Roteador para os subcomandos de admin de pet (`/scout admin pet`).
│       ├── CatPet.java                     # Implementação do pet Gato, com habilidade de alerta de monstros.
│       ├── ParrotPet.java                  # Implementação do pet Papagaio, que pode sentar no ombro do jogador.
│       ├── Pet.java                        # Classe base abstrata que define o comportamento de todos os pets.
│       ├── PetData.java                    # Armazena os dados persistentes de um pet (nível, XP, felicidade).
│       ├── PetFeedSubCommand.java          # Implementa o comando `/scout pet alimentar`.
│       ├── PetInfoSubCommand.java          # Implementa o comando `/scout pet info`.
│       ├── PetInteractionMenu.java         # Constrói a GUI para interagir com o pet (renomear, guardar).
│       ├── PetListener.java                # Ouve eventos do jogo para ganho de XP, defesa do dono e interações.
│       ├── PetManager.java                 # Gerenciador principal do sistema de pets (invocação, compra, XP, felicidade).
│       ├── PetNameSubCommand.java          # Implementa o comando `/scout pet nome`.
│       ├── PetReleaseSubCommand.java       # Implementa o comando `/scout pet liberar`.
│       ├── PetResetAdminSubCommand.java    # Implementa o comando de admin `/scout admin pet reset`.
│       ├── PetShopMenu.java                # Constrói a GUI da loja de pets.
│       ├── PetShopSubCommand.java          # Implementa o comando `/scout pet loja`.
│       ├── PetSubCommand.java              # Roteador principal para os comandos de pet do jogador (`/scout pet`).
│       ├── PetSummonSubCommand.java        # Implementa o comando `/scout pet invocar`.
│       ├── PigPet.java                     # Implementação do pet Porco, com habilidade de coletar itens.
│       └── WolfPet.java                    # Implementação do pet Lobo, focado em combate.
│   │
│   └── quests/
│       ├── TreasureHuntCommand.java        # Implementa o comando `/tesouro` e seus subcomandos.
│       ├── TreasureHuntManager.java        # Gerencia a lógica central da Caça ao Tesouro (iniciar, avançar, dar pistas).
│       ├── TreasureHuntRewardManager.java  # Gerencia a distribuição de recompensas ao completar a caça.
│       └── TreasureLocationsManager.java   # Carrega e gerencia a lista de locais de tesouro do `treasure_locations.yml`.
│   │
│   └── ranks/
│       ├── ApadrinharCommand.java  # Implementa o comando `/apadrinhar` para promover novos jogadores.
│       ├── Rank.java               # Enum que define a hierarquia, nome e cor de cada ranque.
│       ├── RankCommand.java        # Implementa o comando `/ranque` para exibir o progresso do jogador.
│       └── RankManager.java        # Gerencia a lógica de verificação de requisitos e promoção de ranques.
│   │
│   └── scoreboard/
│       ├── BoardSubCommand.java    # Implementa o subcomando `/scout board` para ativar/desativar o placar.
│       └── ScoreboardManager.java  # Gerencia a criação e atualização do placar lateral (scoreboard) de estatísticas.
│   │
│   └── storage/
│       └── BlockPersistenceManager.java # Gerencia a persistência de dados em blocos para identificar os que foram colocados por jogadores e evitar farming.
│   │
│   └── web/
│       ├── HttpApiManager.java         # Gerencia o servidor web, a API RESTful, caches e autenticação de admin.
│       └── SecureHttpFileHandler.java  # Servidor de arquivos estáticos seguro, previne ataques de Path Traversal.
│   │
│   └── updater/
│       └── UpdateChecker.java      # Verifica se há novas versões do plugin no GitHub.
│   │
│   └── utils/
│       ├── ItemFactory.java        # Utilitário moderno para criar `ItemStack` a partir de seções de configuração, com logging.
│       ├── MessageUtils.java       # Utilitário para criar e exibir mensagens formatadas, como o progresso de ranques.
│       ├── PlayerStateManager.java # Gerencia o salvamento e a restauração do estado completo de um jogador (inventário, vida, etc.).
│       └── SecurityUtils.java      # Utilitário para funções de segurança, como gerar 'salts' e 'hashes' de senhas.
│
└── src/main/resources/
    ├── config.yml            # Arquivo de configuração central. Define insígnias, ranques, recompensas e todas as mensagens.
    ├── biome_locations.yml   # Coordenadas de biomas para o NPC "Chefe Magno".
    ├── plugin.yml            # Arquivo de definição do plugin para o servidor (nome, versão, comandos, permissões).
    ├── duel_arenas.yml       # Configuração das arenas de Duelo.
    ├── duel_kits.yml         # Configuração dos kits de Duelo.
    ├── npcs.yml              # Configuração dos NPCs.
    ├── treasure_locations.yml # Coordenadas para a Caça ao Tesouro.
    │
    ├── maps/                   # Imagens para os mapas-troféu das insígnias.
    │   ├── builder_badge.png
    │   ├── cooking_badge.png
    │   ├── crafting_badge.png
    │   ├── explorer_badge.png
    │   ├── farming_badge.png
    │   ├── fishing_badge.png
    │   ├── hunter_badge.png
    │   ├── lumberjack_badge.png
    │   ├── mctrilhas_badge.png
    │   ├── mining_badge.png
    │   ├── scout_badge.png
    │   ├── tamer_badge.png
    │   ├── treasure_badge.png
    │   └── welcome_badge.png
    │
    └── web/                  # Pasta raiz dos arquivos do site e painéis.
        ├── index.html        # Página principal pública do servidor.
        ├── sw.js             # Service Worker para a funcionalidade PWA (Progressive Web App).
        ├── manifest.json     # Manifesto do PWA, permite que o site seja "instalável".
        ├── css/
        │   └── styles.css    # Arquivo de estilo customizado para a `index.html`.
        └── js/
        │   └── main.js       # Script principal da `index.html` (busca dados da API, renderiza gráficos).
        └── admin/            # Pasta para os painéis de acesso restrito.
            ├── login.html    # Página de login do painel de administração.
            ├── admin.html    # Painel de Administração completo.
            ├── pdash.html    # Portal da Família (painel do jogador).
            ├── css/
            │   └── adminlte.css # Arquivo de estilo principal do template AdminLTE.
            └── js/
            │   ├── adminlte.js # Script principal de funcionalidades do template AdminLTE.
            │   └── auth.js     # Script que gerencia a lógica de login e o armazenamento do token JWT.
            └── assets/
                └── img/      # Imagens e avatares de exemplo para o template AdminLTE.
                    ├── AdminLTEFullLogo.png
                    ├── AdminLTELogo.png
                    ├── avatar.png
                    ├── avatar2.png
                    ├── avatar3.png
                    ├── avatar4.png
                    ├── avatar5.png
                    ├── boxed-bg.jpg
                    ├── boxed-bg.png
                    ├── default-150x150.png
                    ├── erro404.png
                    ├── icons.png
                    ├── photo1.png
                    ├── photo2.png
                    └── photo3.jpg

MCTrilhas/ (Raiz do Projeto)
├── .gitignore          # Define quais arquivos e pastas devem ser ignorados pelo Git (ex: pasta 'target').
│
├── pom.xml             # Arquivo de configuração do Maven. Gerencia as dependências e o processo de compilação do projeto.
│
└── README.md           # Arquivo de apresentação principal do projeto no repositório GitHub.