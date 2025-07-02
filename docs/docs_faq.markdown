# ❓ FAQ

Perguntas frequentes sobre o plugin **GodMode-MCTrilhas**.

## Como adiciono uma nova insígnia?
Edite o `config.yml` em `plugins/GodMode-MCTrilhas/` e adicione uma nova entrada em `badges`. Exemplo:
```yaml
badges:
  newbadge:
    name: "Nova Insígnia"
    description: "Faça algo legal."
    reward-totems: 50
    required-progress: 10
```

## Como configuro uma região no WorldGuard?
Use os comandos:
```bash
/rg define builder_area
/rg flag builder_area entry deny
/rg flag builder_area entry -g nonmembers
```

## Como ativo atualizações automáticas?
Instale o AutoPlug e configure o `config.yml` do AutoPlug com o repositório GitHub.

## O que fazer se o plugin não carregar?
- Verifique o Java (Temurin 17).
- Confirme as dependências (Vault, WorldGuard).
- Veja os logs em `latest.log`.

## Como contribuir?
Abra uma Issue ou Pull Request em [GitHub](https://github.com/magnocat/GodMode-MCTrilhas).

[🔙 Voltar ao Menu](index.md)