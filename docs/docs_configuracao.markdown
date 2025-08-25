# ⚙️ Configuração

O plugin **GodMode-MCTrilhas** usa o arquivo `config.yml` em `plugins/GodMode-MCTrilhas/` para definir insígnias e recompensas.

## Estrutura do `config.yml`

```yaml
# Formato da mensagem de progresso. Placeholders: {badgeName}, {progress}, {required}, {percentage}
progress-message-format: "&e{badgeName}: &a{progress}&8/&7{required} &b({percentage}%)"
```

```yaml
badges:
  # Exemplo completo com a nova estrutura de item
  lumberjack:
    name: "Insígnia de Lenhador"
    description: "Corte 1000 árvores."
    required-progress: 1000
    reward-totems: 50
    # A recompensa do item agora é um objeto para mais flexibilidade
    reward-item-data:
      material: "DIAMOND_AXE"
      amount: 1
      name: "&bMachado do Lenhador Mestre"
      lore:
        - "&7Um machado especial para um"
        - "&7escoteiro exemplar."
      enchantments:
        - "efficiency:2"
        - "unbreaking:1"
  miner:
    name: "Insígnia de Minerador"
    description: "Minere 5000 blocos de pedra ou minérios."
    required-progress: 5000
    reward-totems: 100
  fishing:
    name: "Insígnia de Pescador"
    description: "Pesque 250 peixes."
    required-progress: 250
    reward-totems: 75
    reward-item-data:
      material: "FISHING_ROD"
      amount: 1
      name: "&bVara de Pescar do Mestre Pescador"
      lore:
        - "&7Uma vara especial para um"
        - "&7pescador exemplar."
      enchantments:
        - "lure:2"
        - "unbreaking:1"
```

## Campos
- `name`: Nome da insígnia.
- `description`: Descrição da tarefa.
- `reward-totems`: Quantidade de Totens (via Vault).
- `reward-item`: Item do Minecraft (opcional, com encantamentos).
- `reward-amount`: Quantidade do item.
- `required-progress`: Quantidade necessária para conquistar a insígnia.

[🔙 Voltar ao Menu](index.md)