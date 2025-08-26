# ⚜️ GodMode-MCTrilhas ⚜️

![Build Status](https://img.shields.io/github/actions/workflow/status/magnocat/GodMode-MCTrilhas/build.yml?branch=main&label=Build&style=for-the-badge)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/magnocat/GodMode-MCTrilhas?style=for-the-badge&label=Versão)
![Linguagem Principal](https://img.shields.io/github/languages/top/magnocat/GodMode-MCTrilhas?style=for-the-badge&label=Linguagem)

**GodMode-MCTrilhas** é um plugin para o servidor **MC Trilhas** com temática escoteira, que implementa um sistema de insígnias para recompensar jogadores por suas conquistas no Minecraft.

---

### 📜 Índice
1. [Funcionalidades](#-funcionalidades)
2. [Instalação](#-instalação)
3. [Comandos](#-comandos)
4. [Dependências](#-dependências)
5. [Configuração](#-configuração)
6. [Desenvolvimento](#-desenvolvimento)
7. [Suporte](#-suporte)

## ✨ Funcionalidades
- **Sistema de Insígnias**: Conquiste insígnias como Lenhador (100 árvores), Minerador (500 blocos), Cozinheiro (50 alimentos) e Construtor (1000 blocos).
- **Recompensas Configuráveis**: Ofereça Totens (economia via Vault), itens personalizados (com NBT) e acesso a áreas protegidas (via WorldGuard).
- **Interface de Comandos**: Permite que jogadores consultem seu progresso e administradores gerenciem as insígnias dos usuários.
- **Alta Performance**: O sistema de dados de jogadores é otimizado com cache para evitar sobrecarga no servidor.

## 🚀 Instalação
1. Baixe o `.jar` da aba Releases.
2. Coloque em `plugins/` do servidor Paper.
3. Reinicie o servidor.

## 🎮 Comandos

| Comando                                     | Descrição                                    | Permissão                 |
| ------------------------------------------- | -------------------------------------------- | ------------------------- |
| `/scout badges`                             | Exibe as insígnias que você já conquistou.   | `godmode.scout.use`       |
| `/scout progress`                           | Mostra seu progresso para as próximas insígnias. | `godmode.scout.use`       |
| `/scout removebadge <jogador> <badgeId>`    | Remove uma insígnia de um jogador.           | `godmode.scout.admin`     |

## 🔗 Dependências

| Plugin      | Necessidade | Motivo                               |
| ----------- | ----------- | ------------------------------------ |
| **Vault**   | Obrigatório | Para o sistema de economia (Totens). |
| **WorldGuard**| Obrigatório | Para recompensas de acesso a áreas.  |

## ⚙️ Configuração
Todas as insígnias e suas recompensas são definidas no arquivo `config.yml`, gerado na pasta `plugins/GodMode-MCTrilhas/`. Para mais detalhes, consulte a Wiki de Configuração.

## 🧑‍💻 Desenvolvimento
Este projeto é construído com **Maven** e **Java 17 (Temurin)**.
- **Builds Automáticos**: O GitHub Actions compila e cria um release automaticamente a cada nova tag `v*`.
- **Como Contribuir**: Sinta-se à vontade para abrir uma *Issue* para relatar bugs ou sugerir funcionalidades. *Pull Requests* são bem-vindos!

## 📧 Suporte
Contate @magnocat ou abra uma Issue.

## 📜 Licença
O código-fonte será disponibilizado sob uma licença open-source no futuro. Por enquanto, todos os direitos são reservados.

---
*Desenvolvido com carinho para o **MC Trilhas**! 🌲*