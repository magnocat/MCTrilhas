# GodMode para MC Trilhas ⚜️

![GodMode Plugin](https://img.shields.io/github/actions/workflow/status/magnocat/GodMode-MCTrilhas/build.yml?branch=main&label=Build%20Status&style=for-the-badge)

Bem-vindo ao repositório do plugin **GodMode**, desenvolvido exclusivamente para o servidor **MC Trilhas**! Este plugin essencial oferece aos administradores e (futuramente, com permissão) jogadores a capacidade de ativar um modo divino, garantindo invencibilidade e habilidades especiais.

Com uma temática escoteira, o MC Trilhas busca proporcionar uma experiência de jogo única, e o GodMode chega para dar aos líderes as ferramentas necessárias para gerenciar e apoiar a comunidade com segurança e eficiência.

---

## ✨ **Funcionalidades Principais**

* **Modo Deus:** Imunidade total a danos (PvP, PvE, queda, fogo, fome, etc.).
* **Voo Ilimitado:** Capacidade de voar livremente para navegação e construção.
* **Configurável:** Opções para ajustar comportamentos específicos do modo Deus.
* **Integração Futura:** Preparado para integrar com sistemas de permissão (LuckyPerms) e economia (Vault - Totem).

---

## 🛠️ **Compatibilidade**

* **Minecraft:** Paper 1.21.5
* **JDK:** Temurin 21
* **Plataforma:** AMP Release "Phobos" v2.6.2

---

## 🔗 **Dependências & Integrações**

Este plugin foi desenvolvido pensando na integração com os seguintes plugins já presentes no servidor MC Trilhas:

* **Vault:** Para potencial integração com a economia de "Totem".
* **LuckyPerms:** Para gerenciamento de permissões de uso do GodMode.
* **WorldGuard / WorldEdit:** Para considerações em áreas protegidas.
* **PlaceholderAPI:** Para futuras exibições de status ou informações.
* E outros como Citizens, DecentHolograms, Essentials X, etc., para um ambiente coeso.

---

## 🚀 **Instalação**

1.  Faça o download da versão mais recente do plugin GodMode (arquivo `.jar`) na aba [Actions](https://github.com/magnocat/GodMode-MCTrilhas/actions) do repositório (após um build bem-sucedido) ou de um lançamento oficial.
2.  Coloque o arquivo `GodMode.jar` na pasta `plugins/` do seu servidor Paper 1.21.5.
3.  Reinicie ou carregue o servidor (`/reload confirm` - **não recomendado em produção** ou `plugman reload GodMode`).

---

## 🕹️ **Uso**

**Comandos Básicos:**

* `/god [jogador]` - Ativa/desativa o modo Deus para si mesmo ou para um jogador específico.
    * *Permissão:* `godmode.use` (para si)
    * *Permissão:* `godmode.use.other` (para outros)
* `/godmode reload` - Recarrega a configuração do plugin.
    * *Permissão:* `godmode.admin.reload`

---

## ⚙️ **Configuração (`config.yml`)**

Após a primeira execução do plugin, um arquivo `config.yml` será gerado na pasta `plugins/GodMode/`. Você poderá ajustar opções como mensagens e comportamentos padrão do modo Deus.

---

## 🧑‍💻 **Desenvolvimento**

Este projeto é gerenciado com **Maven** e utiliza **GitHub Actions** para automação do processo de build. Contribuições futuras serão consideradas.

---

## 📧 **Suporte & Contato**

Para dúvidas ou problemas, entre em contato diretamente com @magnocat ou abra uma [Issue](https://github.com/magnocat/GodMode-MCTrilhas/issues) neste repositório.

---

## 📜 **Licença**

Atualmente, este projeto não possui uma licença pública definida. Todos os direitos reservados. O código poderá ser aberto em uma data futura.

---

**Desenvolvido com carinho para o MC Trilhas!** 🌲
