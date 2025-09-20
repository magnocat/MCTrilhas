# Resumo do Projeto e Próximos Passos (TODO Gemini)

**Data:** 2024-08-01 (Atualizado)

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

Estas são as tarefas menores e mais imediatas que podemos abordar quando retomarmos o desenvolvimento:

*   **Placeholder de Progresso para o Próximo Ranque:** Desenvolver um placeholder que mostre o progresso do jogador para o próximo ranque de forma visual ou textual (ex: `5/6 insígnias`).