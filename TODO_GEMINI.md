# Resumo do Projeto e Próximos Passos (TODO Gemini)

**Data:** 21-09-2025 (Revisão Concluída)

Este documento serve como um resumo completo do estado do projeto MCTrilhas, suas tecnologias, histórico de desenvolvimento e o roadmap de funcionalidades futuras. Ele foi criado para servir como um lembrete para nós quando retomarmos o projeto.

---

## 1. Resumo do Projeto

O **MCTrilhas** é um plugin customizado para servidores Minecraft (Paper/Spigot) com temática escoteira. Seu núcleo é um sistema de progresso que recompensa jogadores com insígnias e itens por realizarem atividades no jogo. O projeto também inclui um site (PWA) integrado para exibir estatísticas e informações do servidor.

---

## 2. Tecnologias Principais

### Backend (Plugin Java)
*   **Linguagem:** Java 17
*   **Plataforma:** API do Paper/Spigot
*   **Build:** Maven
*   **Dependências Principais:**
    *   **Vault:** Para integração com o sistema de economia (Totens).
    *   **PlaceholderAPI:** Para expor dados do plugin (ranques, contagem de insígnias) para outros plugins como o TAB.
    *   **Servidor HTTP Interno:** Uma implementação leve usando a classe `HttpServer` nativa do Java para servir o site e a API de dados.
    *   **Gson:** Para serializar os dados do servidor para o formato JSON na API.

### Frontend (Site PWA)
*   **Estrutura:** HTML5, CSS, JavaScript (Vanilla).
*   **Estilo:** Tailwind CSS (via CDN).
*   **Gráficos:** Chart.js para exibir o gráfico de atividade dos jogadores.
*   **Funcionalidade PWA:** Um **Service Worker** (`sw.js`) permite que o site seja "instalado" em dispositivos móveis e funcione offline, armazenando os arquivos principais em cache.

---

## 3. Estrutura de Arquivos (Simplificada)

*   `src/main/java/com/magnocat/mctrilhas/`: Contém todo o código-fonte Java, organizado em pacotes por funcionalidade:
    *   `badges/`, `ranks/`, `quests/`: Lógica de negócio principal.
    *   `commands/`: Executores de comandos.
    *   `listeners/`: "Ouvintes" de eventos do jogo.
    *   `data/`: Gerenciamento de dados dos jogadores (`PlayerDataManager`).
    *   `integrations/`: Código para interagir com outros plugins (`MCTrilhasExpansion`).
    *   `managers/`: [REMOVIDO] Continha classes de gerenciamento legadas.
    *   `web/`: O servidor web integrado (`HttpApiManager`).
*   `src/main/resources/`: Contém os arquivos de configuração e os recursos estáticos.
    *   `config.yml`, `plugin.yml`, `treasure_locations.yml`: Arquivos de configuração.
    *   `web/`: Contém todos os arquivos do site (PWA), como `index.html`, `sw.js`, `manifest.json` e imagens.

---

## 4. Histórico e Estado Atual

Revisão das principais funcionalidades implementadas e decisões tomadas:

1.  **Sistema de Dados:** Criamos o `PlayerDataManager` para salvar e carregar dados dos jogadores em arquivos `.yml` individuais, garantindo a persistência do progresso.
2.  **API Web e Rankings:** Implementamos um servidor web interno (`HttpApiManager`) para servir uma API de dados (`/api/v1/data`). Isso permitiu a criação de rankings (diário, mensal, geral) de forma assíncrona e segura, evitando lag no servidor.
3.  **Integração com PlaceholderAPI:** Desenvolvemos a classe `MCTrilhasExpansion` para expor dados do plugin, como a contagem de insígnias e o ranque do jogador, para serem usados por outros plugins (ex: TAB). Corrigimos erros de compilação e adicionamos caches para garantir que os placeholders funcionem de forma síncrona e eficiente.
4.  **Site PWA (Progressive Web App):** Evoluímos o site estático para um PWA completo.
    *   Adicionamos um `manifest.json` e um `sw.js` (Service Worker).
    *   O site agora é instalável em dispositivos móveis, funciona offline e carrega mais rápido.
    *   Corrigimos o gráfico de atividade (Chart.js) e melhoramos a estrutura do `index.html` com instruções de como jogar.
5.  **Portal do Jogador e Tokens de Acesso:** Implementamos o comando `/familia token` para gerar um link único e seguro. Criamos o endpoint `/api/v1/player` para servir dados individuais e desenvolvemos a página `pdash.html` que consome esses dados. Adicionamos um cache para otimizar a performance.
6.  **Painel de Administração Completo:** Implementamos um sistema de login seguro com senha criptografada (hash + salt) e sessões baseadas em JSON Web Tokens (JWT). O painel (`admin.html`) agora é um "canivete suíço" com as seguintes ferramentas:
    *   **Dashboard de Métricas:** Gráficos em tempo real de CPU, RAM e TPS.
    *   **Dashboard de Economia:** Exibição do total de Totens em circulação e dos jogadores mais ricos.
    *   **Gerenciamento de Jogadores:** Lista de jogadores online com ações (Kick, Ban) e um modal de detalhes completo.
    *   **Editor de Perfil:** No modal, é possível conceder/revogar insígnias, dar/remover Totens e alterar o ranque do jogador.
    *   **Ferramentas de Inspeção:** Visualizador de inventário e Ender Chest do jogador.
    *   **Comunicação:** Chat do jogo em tempo real (com capacidade de resposta) e um sistema para enviar anúncios globais.
    *   **Console Remoto:** Interface para executar qualquer comando do servidor diretamente pelo painel.
7.  **Melhorias no CTF:**
    *   Adicionamos lógica para vitória por desistência (W.O.) com recompensa parcial.
    *   Tornamos as recompensas de vitória totalmente configuráveis no `config.yml`.
    *   Tornamos o kit de itens dos jogadores totalmente configurável no `config.yml`.
8.  **Refatoração e Qualidade de Código:** Discutimos e aplicamos melhorias, como a atualização periódica dos caches de ranking e a otimização do streaming de arquivos no servidor web. Mantivemos a decisão de manter a lógica de recompensas centralizada no método `grantReward` por preferência do projeto.
    *   Centralizamos a criação de itens na `ItemFactory`, removendo código duplicado do `CTFMilestoneManager` e `PlayerDataManager`.
    *   Limpamos e organizamos o `DailyCommand`, movendo todas as mensagens para o `config.yml`.
    *   Otimizamos a extração de recursos da web (para acontecer apenas na inicialização) e a leitura de dados de jogadores offline pela API.
    *   Adicionamos um cache para ranques de jogadores offline para melhorar a performance do PlaceholderAPI.
    *   Otimizamos a atualização dos caches de ranking para que não rodem se o servidor estiver vazio.
    *   Corrigimos o bug do "token fantasma" no comando `/familia token`, garantindo o salvamento imediato.
9.  **Melhorias em Placeholders:**
    *   Adicionamos o placeholder `%mctrilhas_rank_formatted%` para exibir o nome do ranque com a capitalização correta (ex: `Escoteiro`).
    *   Adicionamos placeholders de posição no ranking (ex: `%mctrilhas_rank_pos_daily%`) para mostrar a colocação do jogador.
    *   Adicionamos o placeholder `%mctrilhas_rank_progress%` para mostrar o requisito mais próximo para o próximo ranque.
10. **HUD de Estatísticas:**
    *   Implementamos o comando `/hud` que ativa/desativa uma Boss Bar na tela do jogador, exibindo ranque, totens e contagem de insígnias em tempo real.
11. **Maratona de Revisão e Refatoração (Agosto/2024):**
    *   Revisamos todos os arquivos do projeto, um por um.
    *   Criamos e atualizamos a `DOCUMENTACAO_TECNICA.md` para refletir a arquitetura atual.
    *   Adicionamos comentários Javadoc em português a todas as classes Java.
    *   Refatoramos todos os comandos para que suas mensagens sejam lidas do `config.yml`, tornando o plugin 100% personalizável.
    *   Corrigimos bugs lógicos e inconsistências (ex: `Rank.java`, `HttpApiManager.java`).
    *   Melhoramos os arquivos de build (`pom.xml`, `.gitignore`) e marcamos classes obsoletas (`BadgeConfigManager`) para remoção futura.

---

## 5. Roadmap de Funcionalidades

Este é o plano de longo prazo para as próximas grandes funcionalidades, conforme discutido e documentado em `docs/DOCUMENTACAO_TECNICA.md`.

*   ### 🎯 EM FOCO: Sistema de Duelos 1v1
    *   **Descrição:** Um sistema de combate justo e competitivo em arenas designadas.
    *   **Funcionalidades Planejadas:**
        *   Sistema de desafios diretos (`/duelo desafiar <jogador>`).
        *   Arenas configuráveis em `duel_arenas.yml`.
        *   Kits de equipamento padronizados e selecionáveis (`duel_kits.yml`).
        *   Contagem regressiva e gerenciamento completo da partida.
        *   Estatísticas de Vitórias/Derrotas e um sistema de ranking (ELO).

*   ### Sistema de Comunidade e Segurança (Graylist Híbrido)
    *   **Descrição:** Um sistema para proteger o servidor de jogadores mal-intencionados, mantendo-o acessível para a comunidade escoteira.
    *   **Funcionalidades Planejadas:**
        *   **Ranque "Visitante":** Novos jogadores entram com permissões limitadas (não podem construir/quebrar).
        *   **Sistema de Apadrinhamento:** Membros existentes podem usar `/apadrinhar <jogador>` para promover um visitante. O padrinho se torna responsável e pode sofrer penalidades (perda de Totens) se o afilhado for banido.
        *   **Sistema de Aplicação via Site:** Visitantes sem padrinho podem preencher um formulário no site interno do servidor.
        *   **Notificação de Aplicação:** O envio do formulário notifica os administradores (via Discord/in-game) para revisão manual.
        *   **Ponte para o Mundo Real:** Se um candidato não for escoteiro, seus dados de aplicação serão coletados e encaminhados para uma sede escoteira parceira, servindo como uma ponte para o recrutamento no mundo real.
        *   **Comandos de Moderação:** `/aprovar <jogador>` para promover manualmente após análise da aplicação.

*   ### Sistema de Clãs
    *   **Descrição:** Permitirá que jogadores se organizem em grupos formais ("patrulhas" ou "tropas").
    *   **Funcionalidades:** Criação de clã, convites, cargos (líder, oficial), base do clã, banco de itens/Totens compartilhado.
    *   **Comandos:** `/cla criar`, `/cla convidar`, `/cla base`, etc.

*   ### Sistema CTF (Capture The Flag)
    *   **Descrição:** Um modo de jogo competitivo baseado em equipes (ex: Vermelha vs. Azul) onde o objetivo é invadir a base inimiga, capturar a bandeira e trazê-la para a própria base para pontuar.
    *   **Funcionalidades:**
        *   Arenas dedicadas com bases, bandeiras e pontos de respawn.
        *   Sistema de times e balanceamento automático de jogadores.
        *   Kits de equipamento pré-definidos para um jogo justo.
        *   Placar em tempo real e recompensas para a equipe vencedora (ex: 100 Totens por vitória).
        *   **Seleção de Kits (Futuro):** Menu para escolher entre diferentes classes (ex: Arqueiro, Tanque, Batedor) antes da partida.
    *   **Integrações Futuras (com Sistema de Clãs):**
        *   Partidas de Clã vs. Clã.
        *   Filas para times pré-formados por membros do mesmo clã.
        *   Rankings de Clãs específicos para o modo CTF.
        *   Ferramentas para organização de torneios.
        *   **Integração com BlueMap:** Exibição em tempo real da posição das bandeiras e status da partida no mapa web.
    *   **Recompensas Especiais (Futuro):**
        *   Troféus customizados (similar aos de insígnias) para campeonatos.

*   ### Sistema "Vale dos Pioneiros" (Terrenos de Jogadores)
    *   **Descrição:** Um mundo de construção criativa onde jogadores de ranque elevado podem comprar e proteger seus próprios terrenos.
    *   **Funcionalidades:** Compra de terrenos usando Totens, proteção automática da área, gerenciamento de permissões para amigos.
    *   **Integrações:** Planejado para usar Multiverse (para o mundo), WorldGuard (para as proteções) e BlueMap (para visualização no mapa web).

*   ### Painel de Administração (Web)
    *   **Descrição:** Uma plataforma web robusta para gerenciamento do servidor, baseada no template AdminLTE. O portal do jogador (`pdash.html`) e o login do admin já foram implementados.
    *   **Funcionalidades Planejadas (Painel do Admin):**
        *   **Dashboard Avançado:**
            *   Análise de retenção de jogadores (novos vs. retornando).
        *   **Gerenciamento do Servidor:**
            *   Editor de `config.yml` pela interface web.

---

## 6. Próximos Passos Imediatos (Sugestões)

Com a refatoração concluída, o projeto está pronto para a próxima grande funcionalidade.

*   **Iniciar Sistema de Duelos 1v1:** Começar a planejar e implementar a arquitetura para o sistema de duelos, conforme definido no roadmap.

---

## 7. Brainstorm e Ideias Futuras (Anotações da Conversa)

Esta seção contém as ideias e tarefas discutidas para o futuro do projeto.

### 7.1. Novos Minigames
*   **BedWars:** Jogo de equipe clássico.
*   **Build Battle:** Jogo de construção criativa por tempo.
*   **Survival Games:** Com forte apelo ao tema de sobrevivência escoteira.
*   **Murder Mystery:** Focado em dedução e trabalho em equipe.
*   **Lobbies:** Criar um lobby dedicado para cada minigame.

### 7.2. Integração Profunda (Prioridade Máxima)
*   A tarefa mais importante é conectar os minigames ao sistema de progressão.
*   As ações dentro dos minigames (vencer, completar objetivos, trabalho em equipe) devem recompensar os jogadores com XP e avanço em novas **Especialidades** (ex: "Liderança", "Esportes").

### 7.3. Melhorias no Painel Web
*   Desenvolver um "Dashboard da Chefia" para que a liderança possa visualizar o progresso de toda a seção de jogadores.

### 7.4. Infraestrutura e Comandos
*   **Backup do Servidor:** Implementar um sistema de backup completo para o servidor, incluindo configurações de plugins.
*   **Comando `/skins`:** Adicionar um comando para que os jogadores possam alterar suas skins, provavelmente integrando com um plugin como o SkinsRestorer.

---

## 8. Detalhes de Ideias Futuras (Brainstorm)

Esta seção detalha as ideias discutidas para referência futura.

### 8.1. Sistema de Clãs (Detalhado)
*   **Conceito:** O sistema de "Clãs" seria a implementação das "Patrulhas" ou "Tropas" escoteiras. A progressão do jogador através dos ranques o moveria automaticamente entre os clãs.
*   **Nomes dos Clãs (ligados aos Ranques):** Filhotes, Lobinhos, Escoteiros, Sêniors, Pioneiros.
*   **Clãs Especiais:**
    *   **`Convidados`:** Um clã/grupo para novos jogadores ou visitantes, com permissões limitadas (modo espectador, sem poder construir ou quebrar blocos).
    *   **`Chefes`:** Um clã para a moderação, com permissões de controle (kick, mute), mas sem ser necessariamente um Admin/OP do servidor.
*   **Progressão:** Cada clã teria metas de insígnias e recompensas que aumentam de nível conforme o jogador avança.
*   **Comunicação:**
    *   Chat privado para o clã (ex: `/c <mensagem>` ou `!<mensagem>`).
    *   Capacidade de enviar mensagens para o chat geral.
*   **Estrutura de Dados (Exemplo `clans/lobinhos.yml`):**
    ```yaml
    display-name: "&ePatrulha dos Lobinhos"
    tag: "&e[Lobinhos]"
    lider: "uuid-do-lider-da-patrulha" # Poderia ser um cargo rotativo ou definido por um Chefe
    membros:
      - "uuid-membro-1"
      - "uuid-membro-2"
    banco-totens: 5000.0
    nivel: 2
    ```

### 8.2. Sistema de Duelos 1v1 (Detalhado)
*   **Comandos:**
    *   `/duelo desafiar <jogador> [kit]` - Desafia um jogador para um duelo, opcionalmente especificando um kit.
    *   `/duelo aceitar <jogador>` - Aceita um desafio pendente.
    *   `/duelo negar <jogador>` - Recusa um desafio.
    *   `/duelo kits` - Abre uma GUI para visualizar os kits disponíveis.
    *   `/duelo stats [jogador]` - Mostra as estatísticas de duelos (vitórias, derrotas, ELO).
*   **Arenas:**
    *   Configuradas em um arquivo `duel_arenas.yml`.
    *   Cada arena teria dois pontos de spawn e seria uma região protegida para evitar interferência.
    *   O sistema gerenciaria o estado das arenas (livre/ocupada) para permitir múltiplos duelos simultâneos.

### 8.3. Paginação para o Menu de Insígnias
*   **Problema:** O inventário do Minecraft tem um limite de 54 slots. Se houver mais de 45 insígnias (deixando espaço para itens de navegação), a GUI não consegue exibir todas.
*   **Solução Proposta:**
    1.  Adicionar botões de "Próxima Página" e "Página Anterior" em slots fixos do inventário (ex: 45 e 53).
    2.  A classe `BadgeMenu` seria modificada para `open(viewer, target, pageNumber)`.
    3.  A lógica de exibição calcularia o subconjunto de insígnias a ser exibido para a página atual.
    4.  O `MenuListener` identificaria cliques nos botões de navegação e reabriria o menu na página correta, passando o novo número da página.

### 8.4. Gerador de Cards de Jogador (Ideia)
*   **Conceito:** Criar um gerador de imagem (JPG/PNG) que crie um "card" (estilo card de jogo colecionável) com a foto da skin do jogador em uma pose e com seus dados (insígnias, conquistas, etc.). O objetivo é criar um item que o jogador possa imprimir ou enviar para amigos.