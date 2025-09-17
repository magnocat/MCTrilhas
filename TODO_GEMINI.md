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
5.  **Refatoração e Qualidade de Código:** Discutimos e aplicamos melhorias, como a atualização periódica dos caches de ranking e a otimização do streaming de arquivos no servidor web. Mantivemos a decisão de manter a lógica de recompensas centralizada no método `grantReward` por preferência do projeto.

---

## 5. Roadmap de Funcionalidades Futuras

Este é o plano de longo prazo para as próximas grandes funcionalidades, conforme discutido e documentado em `docs/DOCUMENTACAO_TECNICA.md`.

*   ### Sistema de Clãs
    *   **Descrição:** Permitirá que jogadores se organizem em grupos formais ("patrulhas" ou "tropas").
    *   **Funcionalidades:** Criação de clã, convites, cargos (líder, oficial), base do clã, banco de itens/Totens compartilhado.
    *   **Comandos:** `/cla criar`, `/cla convidar`, `/cla base`, etc.

*   ### Sistema de Duelos 1v1
    *   **Descrição:** Um sistema de combate justo e competitivo em arenas designadas.
    *   **Funcionalidades:** Desafios, filas, kits de equipamento padronizados, estatísticas de vitórias/derrotas.
    *   **Integrações:** Planejado para usar WorldGuard (para arenas) e Citizens (para NPCs de gerenciamento).

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

---

## 6. Próximos Passos Imediatos (Sugestões)

Estas são as tarefas menores e mais imediatas que podemos abordar quando retomarmos o desenvolvimento:

*   **Formatar o Nome do Ranque:** O placeholder `%mctrilhas_rank%` exibe o nome em maiúsculas (ex: `ESCOTEIRO`). O próximo passo lógico seria criar um novo placeholder (ex: `%mctrilhas_rank_formatted%`) para exibir o nome com a capitalização correta (ex: `Escoteiro`).

*   **Placeholder de Posição no Ranking:** Criar um placeholder que mostre a posição exata do jogador nos rankings (diário, mensal, geral), como `#1`, `#5`, etc., em vez de apenas a pontuação.

*   **Placeholder de Progresso para o Próximo Ranque:** Desenvolver um placeholder que mostre o progresso do jogador para o próximo ranque de forma visual ou textual (ex: `5/6 insígnias`).

*   **Monetização:** Integrar o Google AdSense nos locais marcados no `index.html` para ajudar a custear o projeto.