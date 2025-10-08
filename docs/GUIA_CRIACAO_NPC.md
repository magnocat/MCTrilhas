# Guia Rápido: Criando e Testando NPCs

Este guia descreve o passo a passo para criar um novo NPC interativo no servidor, associá-lo a um diálogo e testá-lo no jogo.

---

## Passo 1: Criar a Entidade do NPC no Jogo

O primeiro passo é criar a "casca" do NPC no mundo.

1.  Vá até a localização exata onde você deseja que o NPC apareça.
2.  Use o comando de administrador para criar o NPC. Este comando usará a sua skin atual para o novo NPC.

**Comando:**
```
/scout admin npc create <id_unico> <nome_de_exibicao>
```

*   `<id_unico>`: Um nome curto e sem espaços para identificar o NPC internamente (ex: `guia_floresta`, `vendedor_itens`).
*   `<nome_de_exibicao>`: O nome que aparecerá acima da cabeça do NPC. Use `&` para códigos de cor e coloque entre aspas se tiver espaços.

**Exemplo:**
```
/scout admin npc create chefe_magno "&eChefe Magno"
```

Após executar o comando, um NPC com a sua aparência e o nome "Chefe Magno" aparecerá na sua frente.

---

## Passo 2: Criar o Diálogo do NPC

Agora, precisamos criar o roteiro que o NPC seguirá. Isso é feito no arquivo `dialogues.yml`.

1.  Abra o arquivo `plugins/MCTrilhas/dialogues.yml`.
2.  Adicione um novo bloco de diálogo com o ID que você desejar.

**Exemplo de Diálogo Simples:**
```yaml
dialogues:
  meu_novo_dialogo:
    npc-text:
      - "&e[Guia da Floresta]: &fBem-vindo à floresta encantada!"
      - "&fTome cuidado com os caminhos."
    choices:
      1:
        text: "&aObrigado pela dica!"
        action: "close" # Fecha o diálogo
```

---

## Passo 3: Ligar o NPC ao Diálogo

Com o NPC criado no mundo e o diálogo pronto, precisamos conectá-los.

1.  Abra o arquivo `plugins/MCTrilhas/npcs.yml`.
2.  Você encontrará a entrada do NPC que você criou no Passo 1.
3.  Adicione ou edite a linha `start-dialogue-id` para apontar para o ID do diálogo que você criou no Passo 2.

**Exemplo (`npcs.yml`):**
```yaml
npcs:
  chefe_magno:
    name: '&eChefe Magno'
    # ... outras configurações ...
    start-dialogue-id: 'chefe_magno_principal'
```

---

## Passo 4: Recarregar e Testar

Para que as alterações nos arquivos `.yml` tenham efeito sem reiniciar o servidor, use o comando de reload.

**Comando:**
```
/scout admin reload
```

Após o recarregamento, aproxime-se do seu NPC e clique com o botão direito nele. O diálogo que você criou deverá aparecer.

> **Nota:** Este guia é específico para o sistema de NPCs do plugin MCTrilhas. Ele não se aplica à criação de entidades genéricas do Minecraft (como `Villagers` ou `Zombies`).