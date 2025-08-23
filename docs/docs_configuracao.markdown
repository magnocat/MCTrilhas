# ⚙️ Configuração

O plugin **GodMode-MCTrilhas** usa o arquivo `config.yml` em `plugins/GodMode-MCTrilhas/` para definir insígnias e recompensas.

## Estrutura do `config.yml`

# Formato da mensagem de progresso. Placeholders: {badgeName}, {progress}, {required}, {percentage}
progress-message-format: "&e{badgeName}: &a{progress}&8/&7{required} &b({percentage}%)"

```yaml
badges:
  lumberjack:
    name: "Insígnia de Lenhador"
    description: "Corte 1000 árvores."
    reward-totems: 50
    reward-item: "minecraft:diamond_axe{Enchantments:[{id:'minecraft:efficiency',lvl:2}]}"
    reward-amount: 1
    required-progress: 1000
  miner:
    name: "Insígnia de Minerador"
    description: "Minere 5000 blocos de pedra ou minérios."
    reward-totems: 100
    reward-item: "minecraft:diamond_pickaxe{Enchantments:[{id:'minecraft:fortune',lvl:1}]}"
    reward-amount: 1
    required-progress: 5000
  cook:
    name: "Insígnia de Cozinheiro"
    description: "Cozinhe 500 itens."
    reward-totems: 50
    reward-item: "minecraft:smoker"
    required-progress: 500
  builder:
    name: "Insígnia de Construtor"
    description: "Coloque 10000 blocos."
    reward-totems: 150
    reward-item: "minecraft:shulker_box"
    required-progress: 10000
  fishing:
    name: "Insígnia de Pescador"
    description: "Pesque 250 peixes."
    reward-totems: 75
    reward-item: "minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:lure\",lvl:2}]}"
    required-progress: 250
```

## Campos
- `name`: Nome da insígnia.
- `description`: Descrição da tarefa.
- `reward-totems`: Quantidade de Totens (via Vault).
- `reward-item`: Item do Minecraft (opcional, com encantamentos).
- `reward-amount`: Quantidade do item.
- `required-progress`: Quantidade necessária para conquistar a insígnia.

## Edição
- Edite o `config.yml` e use `/plugman reload GodMode-MCTrilhas` para aplicar mudanças.

[🔙 Voltar ao Menu](index.md)