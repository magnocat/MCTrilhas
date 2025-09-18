# ⚜️ MCTrilhas ⚜️

<p align="center">
  <a href="https://github.com/magnocat/MCTrilhas/actions/workflows/build.yml" title="Build Status">
    <img src="https://img.shields.io/github/actions/workflow/status/magnocat/MCTrilhas/build.yml?branch=main&style=for-the-badge&logo=github" alt="Build Status">
  </a>
  <a href="https://github.com/magnocat/MCTrilhas/releases" title="Latest Release">
    <img src="https://img.shields.io/github/v/release/magnocat/MCTrilhas?style=for-the-badge&label=Versão" alt="Versão do Release">
  </a>
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java 17" />
  <img src="https://img.shields.io/badge/Paper-API-lightgrey?style=for-the-badge&logo=papermc" alt="Paper API" />
  <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript" />
</p>
**MCTrilhas** é um plugin customizado para servidores Paper/Spigot com temática escoteira, que implementa um sistema de insígnias, progresso e recompensas para engajar os jogadores.

---

### 📜 Índice
1. [Sobre o Projeto](#-sobre-o-projeto)
2. [Funcionalidades](#-funcionalidades)
3. [Como Jogar](#-como-jogar)
4. [Modos de Jogo](#-modos-de-jogo)
5. [Lista de Insígnias](#-lista-de-insígnias)
6. [Comandos e Permissões](#-comandos-e-permissões)
7. [Roadmap](#-roadmap)

## 📖 Sobre o Projeto
O **MCTrilhas** é um plugin customizado para servidores Minecraft (Paper/Spigot) com temática escoteira. Seu núcleo é um sistema de progresso que recompensa jogadores com insígnias e itens por realizarem atividades no jogo. O projeto também inclui um site (PWA) integrado para exibir estatísticas e informações do servidor.
- **Sistema de Insígnias e Ranques**: Progresso baseado em atividades (mineração, construção, exploração) que recompensa os jogadores com insígnias e os promove em um sistema de ranques escoteiros.
- **Recompensas Configuráveis**: Cada insígnia concede itens customizados, dinheiro (via Vault) e mapas-troféu únicos.
- **Modos de Jogo Competitivos**: Inclui um sistema completo de **Capture a Bandeira (CTF)** com arenas, times, placar e estatísticas.
- **Quests e Recompensas Diárias**: Sistema de Caça ao Tesouro e recompensas diárias para manter os jogadores engajados.
- **API Web e Site Integrado**: Inicia um servidor web que hospeda uma página de estatísticas (PWA) e fornece dados em tempo real para sites externos, com proteção opcional por chave de API.
- **Alta Performance**: O sistema de dados dos jogadores é otimizado com cache e operações assíncronas para evitar sobrecarga no servidor.
- **Integração com PlaceholderAPI**: Expõe dados como ranque e progresso para outros plugins (TAB, scoreboards, etc.).

## 🎮 Como Jogar
Para entrar no servidor, siga as instruções abaixo:

| Edição | Endereço do Servidor | Porta |
|---|---|---|
| ☕ **Java Edition** | `mcj.magnocat.net` | (Padrão) |
| 📱 **Bedrock Edition** | `mcb.magnocat.net` | `19132` |

## 🕹️ Modos de Jogo

### 🏴 Capture a Bandeira (CTF)
Um modo de jogo competitivo onde duas equipes se enfrentam para invadir a base inimiga, roubar a bandeira e trazê-la de volta para marcar pontos.
- **Objetivo**: Marcar 3 pontos ou ter a maior pontuação em 10 minutos.
- **Equipamento Justo**: Todos recebem o mesmo kit de itens.
- **Estratégia**: A comunicação é chave! Use o chat de equipe (`!sua mensagem`) para coordenar ataques e defesas.
- **Comando**: Use `/ctf join` para entrar na fila.

## ⚜️ Lista de Insígnias
Abaixo estão as insígnias disponíveis e seus IDs para uso em comandos.

| ID da Insígnia | Descrição |
|---|---|
| `MINING` | Concedida por minerar uma grande quantidade de blocos. |
| `LUMBERJACK` | Concedida por cortar muitas árvores. |
| `BUILDER` | Concedida por colocar um grande número de blocos. |
| `FARMING` | Concedida por realizar colheitas. |
| `FISHING` | Concedida por pescar muitos peixes. |
| `COOKING` | Concedida por cozinhar alimentos. |
| `CRAFTING` | Concedida por criar muitos itens. |
| `EXPLORER` | Concedida por visitar diferentes biomas. |

## 🎮 Comandos e Permissões
Abaixo está a lista completa de comandos disponíveis.

### Comandos de Jogador
| Comando | Descrição | Permissão |
|---|---|---|
| `/scout badges [jogador]` | Exibe as insígnias conquistadas. Pode ver as de outro jogador se tiver permissão. | `mctrilhas.scout.use` |
| `/scout progress [jogador]` | Mostra o progresso para as próximas insígnias. | `mctrilhas.scout.use` |
| `/scout getmap <insignia>` | Recupera o mapa-troféu de uma insígnia. | `mctrilhas.scout.getmap` |
| `/daily` | Coleta a recompensa diária de Totens e itens. | `mctrilhas.daily` |
| `/ranque` | Mostra seu progresso para o próximo ranque. | `mctrilhas.ranque` |
| `/tesouro` | Inicia ou gerencia sua caça ao tesouro. | `mctrilhas.tesouro` |
| `/ctf join` | Entra na fila para uma partida de CTF. | `mctrilhas.ctf.join` |
| `/ctf leave` | Sai da fila ou da partida de CTF. | `mctrilhas.ctf.leave` |

### Comandos de Administração
| Comando | Descrição | Permissão |
|---|---|---|
| `/scout admin addbadge <jogador> <insignia>` | Concede uma insígnia e sua recompensa a um jogador. | `mctrilhas.admin` |
| `/scout admin removebadge <jogador> <insignia>` | Remove uma insígnia de um jogador e zera seu progresso. | `mctrilhas.admin` |
| `/scout reload` | Recarrega os arquivos de configuração do plugin. | `mctrilhas.admin` |
| `/ctf admin create <id>` | Inicia a criação de uma nova arena de CTF. | `mctrilhas.ctf.admin` |
| `/ctf admin set <tipo>` | Define um local (lobby, spawn, etc.) para a arena. | `mctrilhas.ctf.admin` |
| `/ctf admin save` | Salva a arena de CTF que está sendo criada. | `mctrilhas.ctf.admin` |

## 🗺️ Roadmap
- **🎯 EM FOCO: Sistema de Duelos 1v1**
  - Desafios diretos, arenas dedicadas e kits de equipamento padronizados.
- **Sistema de Clãs**
  - Organização de jogadores em "patrulhas", com base, banco e chat próprios.
- **"Vale dos Pioneiros"**
  - Mundo de construção criativa onde jogadores de ranque elevado podem comprar terrenos.

---
*Desenvolvido com carinho para o **MC Trilhas**! 🌲*