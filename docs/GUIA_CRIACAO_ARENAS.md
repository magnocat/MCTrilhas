### Passo 1: Crie o Mundo das Arenas de CTF

Use o comando do Multiverse para criar um mundo novo, vazio (void), que servirá como contêiner para todas as suas arenas de CTF.

```bash
/mv create arenas_ctf normal -g VoidGenerator
```

Após a criação, teleporte-se para este novo mundo:

```bash
/mv tp arenas_ctf
```

### Passo 2: Importe e Cole sua Arena

1.  Coloque o arquivo da sua arena (ex: `castelo.schem`) na pasta `plugins/WorldEdit/schematics/` do seu servidor.
2.  No jogo, dentro do mundo `arenas_ctf`, carregue o arquivo:
    ```bash
    //schem load nome_da_sua_arena
    ```
3.  Posicione-se onde deseja que a arena apareça e cole-a no mundo:
    ```bash
    //paste
    ```
    > **Dica:** Se a arena não ficou no lugar certo, use `//undo` e tente colar novamente.

### Passo 3: Registre a Arena no Plugin MCTrilhas

Agora, você precisa dizer ao plugin onde ficam os pontos importantes da arena.

1.  **Inicie a criação:**
    ```bash
    /ctf admin create <id_da_arena>
    ```
    *(Exemplo: `/ctf admin create castelo`)*

2.  **Defina os pontos:** Voe até cada local e use o comando `/ctf admin set <tipo>`:
    *   `/ctf admin set lobby` (Onde os jogadores esperam antes da partida)
    *   `/ctf admin set redspawn` (Spawn do time vermelho)
    *   `/ctf admin set bluespawn` (Spawn do time azul)
    *   `/ctf admin set redflag` (Local da bandeira vermelha)
    *   `/ctf admin set blueflag` (Local da bandeira azul)

3.  **Verifique o status:** Use `/ctf admin status` para garantir que todos os 5 pontos foram definidos.

4.  **Salve a arena:**
    ```bash
    /ctf admin save
    ```

---

## Parte 2: Criando Arenas de Duelo 1v1

O processo é quase idêntico, mas usando os comandos do sistema de Duelos.

1.  **Crie o Mundo das Arenas de Duelo:**
    ```bash
    /mv create arenas_duelo normal -g VoidGenerator
    /mv tp arenas_duelo
    ```

2.  **Importe e Cole sua Arena:**
    *   Siga o mesmo processo da Parte 1, Passo 2: coloque o arquivo `.schem` na pasta do WorldEdit, carregue-o e cole-o no mundo `arenas_duelo`.

3.  **Registre a Arena no Plugin MCTrilhas:**
    *   **Inicie a criação:**
        ```bash
        /scout admin duel createarena <id_da_arena>
        ```
        *(Exemplo: `/scout admin duel coliseu`)*

    *   **Defina os pontos:** Fique em cada local e use os comandos:
        *   `/scout admin duel setspawn1` (Spawn do jogador 1)
        *   `/scout admin duel setspawn2` (Spawn do jogador 2)
        *   `/scout admin duel setspec` (Onde os espectadores aparecerão)

    *   **Salve a arena:**
        ```bash
        /scout admin duel save
        ```

---

Com esses passos, suas arenas estarão prontas e serão automaticamente utilizadas pelo sistema de fila dos minigames.