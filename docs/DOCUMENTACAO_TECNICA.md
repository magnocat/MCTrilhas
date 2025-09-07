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
│   ├── integrations/   # (Futuro) Classes para integração com outros plugins (BlueMap, Citizens, etc.).
│   ├── listeners/      # "Ouvintes" de eventos do jogo (quebrar blocos, pescar, etc.).
│   ├── maps/           # Lógica para criar os mapas-troféu customizados.
│   ├── menus/          # Lógica para a GUI (interface gráfica) das insígnias.
│   ├── ranks/          # (Futuro) Lógica para o sistema de progressão de ranques.
│   ├── duels/          # (Futuro) Lógica para o sistema de duelos.
│   ├── quests/         # (Futuro) Lógica para sistemas de missões, como a Caça ao Tesouro.
│   ├── clans/          # (Futuro) Lógica para o sistema de clãs.
│   ├── plots/          # (Futuro) Lógica para gerenciar os terrenos no "Vale dos Pioneiros".
│   ├── storage/        # Gerenciamento de persistência de dados (ex: blocos colocados por jogadores).
│   ├── updater/        # Sistema de verificação e download automático de atualizações.
│   ├── web/            # Gerenciador de dados para a página web (rankings, estatísticas).
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
*   **Descrição:** Gera dados dinâmicos do servidor para serem exibidos em uma página web externa.
*   **Componentes:**
    *   **`WebDataManager`**: Classe que contém tarefas agendadas (assíncronas) para gerar arquivos.
    *   **Rankings (`leaderboard-*.json`):** Uma tarefa periódica lê os dados de todos os jogadores de forma assíncrona (`getFilteredBadgeCountsAsync`) e gera arquivos JSON com os rankings diário, mensal e geral.
    *   **Gráfico de Atividade (`player_stats.csv`):** Uma tarefa anexa a contagem de jogadores online a cada 10 minutos. Outra tarefa, menos frequente, limpa o arquivo para manter apenas os dados das últimas 24 horas.

---

## 4. Sistemas Futuros (Planejados)

### 4.1. Sistema de Progressão de Ranques
*   **Descrição:** Um sistema de progressão passivo que promove jogadores com base em tempo de jogo e insígnias conquistadas.
*   **Estrutura:** `ranks/Rank.java` (Enum), `ranks/RankManager.java`.
*   **Dados:** Um novo campo no `PlayerData` para armazenar o ranque atual do jogador.
*   **Lógica:** O `RankManager` verificará os requisitos no login do jogador e, se necessário, o promoverá.
*   **Integração:** Criará uma expansão para o **PlaceholderAPI** (`%mctrilhas_rank_prefix%`) para exibir o ranque no chat e acima da cabeça do jogador.

### 4.2. Sistema de Clãs
*   **Descrição:** Permitirá que jogadores se organizem em grupos formais.
*   **Estrutura:** `clans/Clan.java`, `clans/ClanManager.java`.
*   **Dados:** Nova pasta `plugins/MCTrilhas/clans/` com um arquivo `.yml` para cada clã.
*   **Comandos:** `/cla criar`, `/cla convidar`, `/cla sair`, `/cla base fundar`.

### 4.3. Sistema de Duelos 1v1
*   **Descrição:** Um sistema de combate justo e competitivo.
*   **Estrutura:** `duels/DuelManager.java`, `duels/Arena.java`.
*   **Lógica:** Gerenciamento de desafios, teleporte para arenas, aplicação de kits de equipamento padronizados e restauração de inventário.
*   **Integração:** Usará **WorldGuard** para definir as arenas e **Citizens** para criar um NPC "Mestre de Duelos" que gerenciará as filas e estatísticas.

### 4.4. Caça ao Tesouro do Explorador
*   **Descrição:** Uma quest interativa e aleatória para a insígnia de Explorador.
*   **Estrutura:** `quests/TreasureHuntManager.java`.
*   **Dados:** Uma lista de coordenadas pré-definidas no `config.yml`. O progresso de cada jogador (locais sorteados e visitados) será salvo em seu arquivo de dados.
*   **Lógica:** O sistema sorteará uma sequência única de locais para cada jogador e o guiará através de bússolas ou mapas de pista.

### 4.5. Sistema "Vale dos Pioneiros" (Terrenos de Jogadores)
*   **Descrição:** Um mundo de construção criativa onde jogadores podem comprar terrenos.
*   **Estrutura:** `plots/PlotManager.java`.
*   **Lógica:** Verificará o ranque e a economia (via **Vault**) do jogador para autorizar a compra.
*   **Integração:**
    *   **Multiverse:** Para criar e gerenciar o mundo "Vale dos Pioneiros".
    *   **WorldGuard:** Para criar e gerenciar as regiões protegidas de cada terreno.
    *   **BlueMap:** Para adicionar marcadores 3D customizados no mapa web, indicando a localização e o dono de cada terreno/base.

---

## 5. Estrutura de Comandos

### 5.1. Comandos Atuais
| Comando | Executor | Descrição |
|---|---|---|
| `/scout <subcomando>` | `ScoutCommandExecutor` | Comando principal que delega para todos os subcomandos de jogador e admin. |
| `/daily` | `DailyCommand` | Permite que jogadores coletem sua recompensa diária. |

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

*   **`clans/` (Futuro):**
    *   **Propósito:** Armazenará os dados de cada clã em um arquivo YAML separado.
    *   **Conteúdo:** Nome do clã, tag, líder, oficiais, membros, nível, banco de Totens, etc.
    *   **Localização:** `plugins/MCTrilhas/clans/`