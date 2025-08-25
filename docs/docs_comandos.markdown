# 🎮 Comandos

Lista de comandos do plugin **GodMode-MCTrilhas**.

## Comandos do Jogador
- `/scout badges`: Exibe as insígnias conquistadas.
  - Permissão: `godmode.scout.use`
- `/scout progress [jogador]`: Mostra o progresso para insígnias não conquistadas.
  - Permissão: `godmode.scout.use`
  - Permissão para ver outros: `godmode.scout.progress.other`
- `/scout toggleprogress`: Ativa ou desativa as mensagens de progresso a cada 10%.
  - Permissão: `godmode.scout.use`
- `/scout top`: Mostra os jogadores com mais insígnias.
  - Permissão: `godmode.scout.use`

## Comandos Administrativos
- `/scout addbadge <jogador> <badgeId>`: Concede uma insígnia a um jogador.
  - Permissão: `godmode.scout.admin`
  - Exemplo: `/scout addbadge MagnoCat miner`
- `/scout removebadge <jogador> <badgeId>`: Remove uma insígnia de um jogador.
  - Permissão: `godmode.scout.admin`
  - Exemplo: `/scout removebadge MagnoCat lumberjack`
- `/scout reload`: Recarrega o arquivo de configuração `config.yml`.
  - Permissão: `godmode.scout.admin`

## 🔐 Configuração de Permissões (LuckyPerms)

Para que os comandos funcionem corretamente, você precisa configurar as permissões. Se você usa o **LuckyPerms**, aqui estão os comandos para configurar os grupos mais comuns:

### Grupo `default` (Jogadores)
Concede acesso aos comandos básicos de jogador (`/scout badges`, `/scout progress`, etc.).
```bash
/lp group default permission set godmode.scout.use true
```

### Grupo `admin` (Administradores)
Concede acesso a todos os comandos do plugin. Jogadores com OP (operador) já possuem estas permissões por padrão, não sendo necessário configurar para eles.
```bash
/lp group admin permission set godmode.scout.admin true
/lp group admin permission set godmode.scout.progress.other true
```
> **Nota**: O grupo `admin` também precisa da permissão `godmode.scout.use`, que geralmente é herdada do grupo `default`.

## IDs de Insígnias
- `lumberjack`: Insígnia de Lenhador
- `miner`: Insígnia de Minerador
- `cook`: Insígnia de Cozinheiro
- `builder`: Insígnia de Construtor
- `fishing`: Insígnia de Pescador

[🔙 Voltar ao Menu](index.md)