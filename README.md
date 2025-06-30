# GodMode para MC Trilhas ⚜️

![GodMode Plugin](https://img.shields.io/github/actions/workflow/status/magnocat/GodMode-MCTrilhas/build.yml?branch=main&label=Build%20Status&style=for-the-badge)

Plugin para o servidor **MC Trilhas** com temática escoteira, implementando um sistema de insígnias para recompensar jogadores por conquistas no Minecraft.

## ✨ Funcionalidades
- **Sistema de Insígnias**: Conquiste insígnias como Lenhador (100 árvores), Minerador (500 blocos), Cozinheiro (50 alimentos) e Construtor (1000 blocos).
- **Recompensas**: Totens (economia via Vault), itens personalizados e acesso a áreas protegidas (WorldGuard).
- **Comandos**:
  - `/scout badges`: Lista insígnias do jogador.
  - `/scout progress`: Mostra progresso para insígnias.
  - `/scout removebadge <jogador> <badgeId>`: Remove insígnias (admin).
- **Integrações**: Vault, WorldGuard, Paper 1.21.5.

## 🛠️ Compatibilidade
- **Minecraft**: Paper 1.21.5
- **JDK**: Temurin 17
- **Plataforma**: AMP Release "Phobos" v2.6.2

## 🔗 Dependências
- Vault
- WorldGuard
- LuckyPerms (futuro)
- PlaceholderAPI (futuro)

## 🚀 Instalação
1. Baixe o `.jar` da aba [Releases](https://github.com/magnocat/GodMode-MCTrilhas/releases).
2. Coloque em `plugins/` do servidor Paper.
3. Reinicie o servidor.

## 🕹️ Uso
- `/scout badges`: Veja suas insígnias.
- `/scout progress`: Verifique progresso.
- `/scout removebadge <jogador> <badgeId>` (perm: `godmode.scout.admin`).

## ⚙️ Configuração
Arquivo `config.yml` gerado em `plugins/GodMode-MCTrilhas/` define insígnias e recompensas.

## 🧑‍💻 Desenvolvimento
- **Maven**: Gerenciamento de dependências.
- **GitHub Actions**: Build automático.
- **Contribuições**: Abertas via Issues/Pull Requests.

## 📧 Suporte
Contate @magnocat ou abra uma [Issue](https://github.com/magnocat/GodMode-MCTrilhas/issues).

## 📜 Licença
Todos os direitos reservados. Código será aberto futuramente.

Desenvolvido para MC Trilhas! 🌲
