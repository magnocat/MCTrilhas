# ⚜️ MCTrilhas ⚜️

<p align="center">
  <a href="https://github.com/magnocat/MCTrilhas/actions/workflows/build.yml" title="Build Status">
    <img src="https://github.com/magnocat/MCTrilhas/actions/workflows/build.yml/badge.svg" alt="Build Status">
  </a>
  <img src="https://img.shields.io/github/v/release/magnocat/MCTrilhas?style=for-the-badge&label=Versão" alt="Versão do Release">
  <img src="https://img.shields.io/github/languages/top/magnocat/MCTrilhas?style=for-the-badge&label=Linguagem" alt="Linguagem Principal">
  <img src="https://img.shields.io/github/license/magnocat/MCTrilhas?style=for-the-badge&label=Licença" alt="Licença">
</p>

**MCTrilhas** é um plugin customizado para servidores Paper/Spigot com temática escoteira, que implementa um sistema de insígnias, progresso e recompensas para engajar os jogadores.

---

### 📜 Índice
1. [Sobre o Projeto](#-sobre-o-projeto)
2. [Funcionalidades](#-funcionalidades)
3. [Tecnologias Utilizadas](#-tecnologias-utilizadas)
4. [Como Jogar](#-como-jogar)
5. [Modos de Jogo](#-modos-de-jogo)
6. [Lista de Insígnias](#-lista-de-insígnias)
7. [Comandos e Permissões](#-comandos-e-permissões)
8. [Roadmap](#-roadmap)
9. [Como Contribuir](#-como-contribuir)
10. [Licença](#-licença)
11. [Contato](#-contato)

## 📖 Sobre o Projeto
O **MCTrilhas** é um plugin customizado para servidores Minecraft (Paper/Spigot) com temática escoteira. Seu núcleo é um sistema de progresso que recompensa jogadores com insígnias e itens por realizarem atividades no jogo. O projeto também inclui um site (PWA) integrado para exibir estatísticas e informações do servidor.

## ✨ Funcionalidades
- **Sistema de Insígnias e Ranques**: Progresso baseado em atividades (mineração, construção, exploração) que recompensa os jogadores com insígnias e os promove em um sistema de ranques escoteiros.
- **Recompensas Configuráveis**: Cada insígnia concede itens customizados, dinheiro (via Vault) e mapas-troféu únicos.
- **Modos de Jogo Competitivos**: Inclui um sistema completo de **Capture a Bandeira (CTF)** com arenas, times, placar e estatísticas.
- **Quests e Recompensas Diárias**: Sistema de Caça ao Tesouro e recompensas diárias para manter os jogadores engajados.
- **API Web e Site Integrado**: Inicia um servidor web que hospeda uma página de estatísticas (PWA) e fornece dados em tempo real para sites externos, com proteção opcional por chave de API.
- **Alta Performance**: O sistema de dados dos jogadores é otimizado com cache e operações assíncronas para evitar sobrecarga no servidor.
- **Integração com PlaceholderAPI**: Expõe dados como ranque e progresso para outros plugins (TAB, scoreboards, etc.).

## 🚀 Tecnologias Utilizadas
O projeto é construído com tecnologias modernas para garantir performance e escalabilidade.

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java 17" />
  <img src="https://img.shields.io/badge/Maven-3-red?style=for-the-badge&logo=apachemaven" alt="Maven" />
  <img src="https://img.shields.io/badge/Paper-API-lightgrey?style=for-the-badge&logo=papermc" alt="Paper API" />
  <img src="https://img.shields.io/badge/Vault-API-yellow?style=for-the-badge" alt="Vault API" />
  <img src="https://img.shields.io/badge/PlaceholderAPI-blue?style=for-the-badge" alt="PlaceholderAPI" />
  <img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white" alt="HTML5" />
  <img src="https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white" alt="Tailwind CSS" />
  <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript" />
</p>

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

## 🤝 Como Contribuir
Contribuições são o que tornam a comunidade de código aberto um lugar incrível para aprender, inspirar e criar. Qualquer contribuição que você fizer será **muito apreciada**.

1. Faça um *Fork* do projeto.
2. Crie uma *Branch* para sua feature (`git checkout -b feature/AmazingFeature`).
3. Faça o *Commit* de suas alterações (`git commit -m 'Add some AmazingFeature'`).
4. Faça o *Push* para a Branch (`git push origin feature/AmazingFeature`).
5. Abra um *Pull Request*.

## 📝 Licença
Distribuído sob a licença MIT. Veja `LICENSE` para mais informações.

## 📧 Contato
**MagnoCat** - @magnocat - contato@magnocat.net

Link do Projeto: https://github.com/magnocat/MCTrilhas

---
*Desenvolvido com carinho para o **MC Trilhas**! 🌲*