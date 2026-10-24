# Laboratório de SOLID em Java

Uma loja virtual em memória, com legado propositalmente problemático para
praticar SRP, OCP, LSP, ISP e DIP. As seis branches começam da mesma base,
sem soluções prontas. O desafio é melhorar o design preservando o comportamento.

## Pré-requisitos

- JDK 17 ou superior e Maven 3.9+ disponíveis no terminal.
- Git para Windows com Git Bash, ou Git e Bash no Linux/macOS.
- PowerShell 5.1+ para as medições opcionais. Não é necessário Python.
- Acesso ao Maven Central na primeira execução para baixar JUnit/plugins.

Não há Spring, Lombok, banco, rede em tempo de execução ou injeção por framework.
Pagamentos, notificações e notas fiscais são simulados e determinísticos.
As datas são informadas em cada pedido; não há uso do relógio atual.

## Começar no Windows

Extraia o ZIP em uma pasta própria e abra essa pasta no IntelliJ IDEA ou VS Code
(importação Maven; no VS Code, com suporte a Java). No PowerShell da pasta:

```powershell
java -version
mvn -version
mvn test
```

No **Git Bash**, aberto na mesma pasta:

```bash
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@example.com"
bash criar-branches.sh
```

Use sua identidade real. Se preferir identidade apenas local, execute
`git init -b main` e depois `git config user.name` / `git config user.email`
com seus valores, antes do script. Ele exige nome e e-mail configurados e
verifica a identidade efetiva de autor/committer antes de fazer commits.

De volta ao PowerShell, ou continuando no Git Bash:

```powershell
git branch --list
git switch solid-01-responsabilidade-unica
mvn test
```

O script Bash verifica árvore limpa, preserva branches existentes, cria um commit
de NOTAS.md em cada branch nova e volta à main no final. Em repositório existente,
se main não existir, o script a cria no HEAD salvo, sem mover a branch de origem.
Só cria o commit inicial quando não há histórico. Não move branches, não faz merges e
não apaga alterações. Pode ser executado novamente após salvar o trabalho.
Se uma execução for interrompida depois de criar uma branch, inspecione seu
estado antes de repetir: branches existentes são preservadas, não reparadas.

Em Linux/macOS também é possível `./criar-branches.sh` (bit executável no ZIP e
no commit inicial); `bash criar-branches.sh` funciona independentemente desse bit.

## Estrutura

```text
src/main/java/br/com/treino/solid/
  fachada/       LojaService e coordenação do caso de uso
  cliente/       Cliente e Endereco
  produto/       Produto e categoria textual
  pedido/        Carrinho, Item, Pedido e repositório concreto
  estoque/       EstoqueMemoria
  desconto/      Cupom
  pagamento/     hierarquia, interface ampla e gateway simulado
  frete/         CalculadoraFrete
  notificacao/   interface ampla, despachante e canais concretos
  fiscal/        EmissorNotaFiscal
  relatorio/     RelatorioVendas
src/test/java/br/com/treino/solid/fachada/
  LojaServiceTest.java
ferramentas/medir.ps1
```

## Fluxo dos exercícios e contrato

1. Leia API.md e o princípio correspondente no ROTEIRO.md.
2. Entre na branch escolhida e execute a suíte antes de mudar o código.
3. Identifique razões de mudança, contratos e dependências antes de escolher padrões.
4. Faça uma pequena refatoração, execute os testes e registre decisões em NOTAS.md.
5. Não edite testes de caracterização, assinaturas ou comportamento público da
   LojaService. Modelos e colaboradores internos podem ser substituídos.
6. Não faça merge dos exercícios na main. Use solid-06-desafio-completo para
   integrar os cinco princípios partindo novamente da base original.

Os testes só conhecem a fachada e tipos simples; não usam modelos internos,
reflexão ou mensagens de exceção. Casos de LSP preservam o comportamento atual
por asserções de exceção; os contratos pretendidos estão separados no roteiro.
Não se exige quebrar esses testes para corrigir a hierarquia.

## Comandos úteis no PowerShell

```powershell
mvn test
mvn test "-Duser.language=pt" "-Duser.country=BR"
mvn test "-Duser.language=en" "-Duser.country=US"
powershell -NoProfile -ExecutionPolicy Bypass -File ferramentas/medir.ps1
git status --short
git switch main
```

`-ExecutionPolicy Bypass` afeta apenas o processo de medição mostrado; não muda
a política global. A medição é opcional e não declara se um design respeita SOLID.
Os comandos e limitações efetivamente verificados constam de VERIFICACAO.md.

O ZIP não contém .git/ ou target/: as branches são criadas localmente pelo script.
Para explorar o comportamento, instancie `new LojaService()` em seu ambiente Java
ou use a suíte como exemplos; não é uma aplicação web nem um programa de console.
