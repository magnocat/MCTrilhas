# 🧑‍💻 Desenvolvimento

Como configurar o ambiente para desenvolver o plugin **GodMode-MCTrilhas**.

## Requisitos
- **JDK**: Temurin 17
- **Maven**: 3.9.6 ou superior
- **IDE**: VSCode com Java Extension Pack
- **Paper**: 1.20.4

## Configuração
1. Clone o repositório: `git clone https://github.com/magnocat/GodMode-MCTrilhas.git`
2. Abra no VSCode: `File > Open Folder`.
3. Configure o JDK 17 em `settings.json`:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-17",
         "path": "C:\\Program Files\\Eclipse Adoptium\\temurin-17.0.16+8"
       }
     ]
   }
   ```
4. Compile com: `mvn clean package`

## Estrutura
- `src/main/java/com/magnocat/godmode/`: Código Java.
- `src/main/resources/plugin.yml`: Metadados do plugin.
- `src/main/resources/config.yml`: Configuração padrão geral.
- `src/main/resources/badges.yml`: Configuração padrão das insígnias.
- `pom.xml`: Dependências Maven.

## GitHub Actions
- O repositório usa Actions para build automático.
- **Releases (estáveis)**: Ao criar uma tag `v*`, um novo Release é gerado com o `.jar` oficial.
- **Builds de desenvolvimento**: A cada push na branch `main`, um `.jar` é gerado e pode ser baixado na aba "Actions" do GitHub, dentro do workflow correspondente.

## AutoPlug
- Configure o AutoPlug para atualizações automáticas:
  ```yaml
  plugins:
    GodMode-MCTrilhas:
      github: magnocat/GodMode-MCTrilhas
      check-interval: 3600
  ```

[🔙 Voltar ao Menu](index.md)