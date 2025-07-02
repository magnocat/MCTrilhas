# Configuração

   O plugin **GodMode-MCTrilhas** usa o arquivo `config.yml` em `plugins/GodMode-MCTrilhas/` para definir insígnias e recompensas.

   ## Estrutura do `config.yml`
   ```yaml
   badges:
     lumberjack:
       name: "Insígnia de Lenhador"
       description: "Corte 100 árvores."
       reward-totems: 50
       reward-item: "minecraft:diamond_axe{Enchantments:[{id:'minecraft:efficiency',lvl:2}]}"
       reward-amount: 1
       required-progress: 100
     miner:
       name: "Insígnia de Minerador"
       description: "Minere 500 blocos de pedra ou minérios."
       reward-totems: 100
       reward-item: "minecraft:diamond_pickaxe{Enchantments:[{id:'minecraft:fortune',lvl:1}]}"
       reward-amount: 1
       required-progress: 500
   ```

   ## Campos
   - `name`: Nome da insígnia.
   - `description`: Descrição da tarefa.
   - `reward-totems`: Quantidade de Totens (via Vault).
   - `reward-item`: Item do Minecraft (opcional, com encantamentos).
   - `reward-amount`: Quantidade do item.
   - `reward-region`: Região do WorldGuard (opcional).
   - `required-progress`: Quantidade necessária para conquistar a insígnia.

   ## Edição
   - Edite o `config.yml` e use `/plugman reload GodMode-MCTrilhas` para aplicar mudanças.