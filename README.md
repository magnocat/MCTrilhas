# ⚜️ MCTrilhas ⚜️

![Build Status](https://img.shields.io/github/actions/workflow/status/magnocat/MCTrilhas/build.yml?branch=main&label=Build&style=for-the-badge)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/magnocat/MCTrilhas?style=for-the-badge&label=Versão)
![Linguagem Principal](https://img.shields.io/github/languages/top/magnocat/MCTrilhas?style=for-the-badge&label=Linguagem)

**MCTrilhas** é um plugin customizado para servidores Paper/Spigot com temática escoteira, que implementa um sistema de insígnias, progresso e recompensas para engajar os jogadores.

---

### 📜 Índice
1. [Funcionalidades](#-funcionalidades)
2. [Instalação](#-instalação)
3. [Lista de Insígnias](#-lista-de-insígnias)
4. [Comandos e Permissões](#-comandos-e-permissões)
5. [Integrações](#-integrações)
6. [Configuração](#-configuração)
7. [Desenvolvimento](#-desenvolvimento)
8. [Suporte](#-suporte)

## ✨ Funcionalidades
- **Sistema de Insígnias**: Sistema de progresso para diversas atividades (mineração, construção, exploração, etc.) que recompensa os jogadores com insígnias.
- **Recompensas Configuráveis**: Cada insígnia pode conceder itens customizados, dinheiro (via Vault) e mapas-troféu únicos.
- **Interface Gráfica (GUI)**: Um menu interativo para que os jogadores visualizem suas insígnias e progresso.
- **API Web Integrada**: Inicia um servidor web próprio que pode hospedar uma página de estatísticas e fornecer dados em tempo real para sites externos, com proteção opcional por chave de API.
- **Alta Performance**: O sistema de dados dos jogadores é otimizado com cache e operações assíncronas para evitar sobrecarga no servidor.
- **Atualização Automática**: O plugin verifica por novas versões no GitHub e as baixa automaticamente para serem instaladas na próxima reinicialização.

## ⚜️ Lista de Insígnias
Abaixo estão as insígnias disponíveis no servidor e o ID que deve ser usado nos comandos (`/scout getmap <ID>`, `/scout admin addbadge <jogador> <ID>`, etc.).

| ID da Insígnia | Descrição |
|---|---|
| `MINING` | Concedida por minerar uma grande quantidade de blocos. |
| `LUMBERJACK` | Concedida por cortar muitas árvores. |
| `BUILDER` | Concedida por colocar um grande número de blocos. |
| `FARMING` | Concedida por colher diversas plantações. |
| `FISHING` | Concedida por pescar muitos peixes. |
| `COOKING` | Concedida por cozinhar uma variedade de alimentos. |
| `CRAFTING` | Concedida por criar muitos itens. |
| `EXPLORER` | Concedida por visitar todos os diferentes biomas do mundo. |

## 🚀 Instalação
1. Baixe o `.jar` da aba Releases.
2. Coloque o arquivo na pasta `plugins/` do seu servidor Paper/Spigot.
3. Reinicie o servidor.

## 🎮 Comandos e Permissões
Abaixo está a lista completa de comandos disponíveis.

### Comandos de Jogador
| Comando | Descrição | Permissão |
|---|---|---|
| `/scout badges [jogador]` | Exibe as insígnias conquistadas. Pode ver as de outro jogador se tiver permissão. | `mctrilhas.scout.use` |
| `/scout progress [jogador]` | Mostra o progresso para as próximas insígnias. | `mctrilhas.scout.use` |
| `/scout getmap <insignia>` | Recupera o mapa-troféu de uma insígnia já conquistada. | `mctrilhas.scout.getmap` |
| `/scout version` | Exibe a versão atual do plugin. | `mctrilhas.scout.use` |
| `/daily` | Coleta a recompensa diária de Totens e itens. | `mctrilhas.daily` |

### Comandos de Administração
| Comando | Descrição | Permissão |
|---|---|---|
| `/scout admin` | Mostra a lista de todos os comandos de administração. | `mctrilhas.admin` |
| `/scout admin addbadge <jogador> <insignia>` | Concede uma insígnia e sua recompensa a um jogador. | `mctrilhas.admin` |
| `/scout admin removebadge <jogador> <insignia>` | Remove uma insígnia de um jogador e zera seu progresso. | `mctrilhas.admin` |
| `/scout admin stats <jogador>` | Vê as estatísticas de progresso completas de um jogador. | `mctrilhas.admin` |
| `/scout reload` | Recarrega os arquivos de configuração do plugin. | `mctrilhas.admin` |

### Permissões Detalhadas
| Permissão | Descrição | Padrão |
|---|---|---|
| `mctrilhas.*` | Concede acesso a todos os comandos e funcionalidades do MCTrilhas. | `op` |
| `mctrilhas.scout.use` | Permite o uso dos comandos básicos de jogador (`/scout badges`, `/scout progress`). | `true` (todos) |
| `mctrilhas.scout.getmap` | Permite usar o `/scout getmap` para recuperar troféus. | `true` (todos) |
| `mctrilhas.daily` | Permite coletar a recompensa diária com `/daily`. | `true` (todos) |
| `mctrilhas.admin` | Permite o uso de todos os comandos de administração. | `op` |
| `mctrilhas.progress.other` | Permite ver as insígnias e o progresso de outros jogadores. | `op` |

## 🔗 Integrações

| Plugin      | Necessidade | Motivo                               |
| ----------- | ----------- | ------------------------------------ |
| **Vault** | **Obrigatório** | Para o sistema de economia (Totens). |

## ⚙️ Configuração
Todas as insígnias, recompensas e configurações gerais são definidas no arquivo `config.yml`, gerado na pasta `plugins/MCTrilhas/`.

## 🧑‍💻 Desenvolvimento
Este projeto é construído com **Maven** e **Java 17**.
- **Builds Automáticos**: O GitHub Actions compila e cria um release automaticamente a cada nova tag `v*`.
- **Como Contribuir**: Sinta-se à vontade para abrir uma *Issue* para relatar bugs ou sugerir funcionalidades. *Pull Requests* são bem-vindos!

## 📧 Suporte
Contate @magnocat ou abra uma Issue.

---
*Desenvolvido com carinho para o **MC Trilhas**! 🌲*