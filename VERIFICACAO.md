# Verificação da entrega — 24/10/2026

## Resultado

141 testes passaram na base original, novamente após restaurar todas as
mutações e nas duas execuções com idioma/país pt-BR e en-US. Zero falhas,
erros ou casos ignorados nas execuções finais. Os fontes e testes empacotados
foram comparados byte a byte com os da cópia de validação restaurada.

Ambiente: Windows, Maven 3.9.9, Temurin JDK 25.0.4.1. Compilação com release 17.
Bytecode de LojaService conferido como major version 61. Não houve execução
em uma JVM 17; compatibilidade de compilação não equivale a essa execução.
Os testes não usam internet, relógio, banco ou locale da máquina.

~~~powershell
mvn test
mvn test "-Duser.language=pt" "-Duser.country=BR"
mvn test "-Duser.language=en" "-Duser.country=US"
~~~

Os comandos de idioma foram também executados exatamente como documentados no
Windows PowerShell. As aspas são necessárias: a versão sem aspas foi rejeitada
pelo Maven como fase .language=pt, e foi corrigida antes da entrega.

## Mutações manuais executadas

Cada mutação foi aplicada isoladamente em uma cópia, compilou e produziu falhas
ou erros de teste. O arquivo original foi restaurado após cada execução.
Todas as nove foram detectadas. As execuções são amostragem manual, não um
escore PIT nem medição de cobertura JaCoCo.

| Mutação | Falhas de asserção | Erros de teste |
|---|---:|---:|
| frete-limiar | 1 | 0 |
| categoria-desconto | 3 | 0 |
| cupom-validade | 0 | 1 |
| estoque-baixa | 7 | 1 |
| arredondamento | 5 | 0 |
| pix-recusado | 13 | 1 |
| nota-sequencia | 1 | 0 |
| sms-canal | 4 | 0 |
| relatorio-estado | 2 | 0 |

Substituições exatas usadas (uma ocorrência cada):

- frete-limiar: em frete/CalculadoraFrete.java, trocar subtotal >= 300 por subtotal > 300.
- categoria-desconto: em fachada/LojaService.java, trocar valor * 0.05 por valor * 0.04.
- cupom-validade: em fachada/LojaService.java, trocar pedido.data.isAfter(cupom.validade) por !pedido.data.isBefore(cupom.validade).
- estoque-baixa: em fachada/LojaService.java, trocar estoque.consultar(item.produtoId) - item.quantidade por estoque.consultar(item.produtoId) + item.quantidade.
- arredondamento: em fachada/LojaService.java, trocar RoundingMode.HALF_UP por RoundingMode.DOWN.
- pix-recusado: em pagamento/PagamentoPix.java, trocar if (credencial.equals("RECUSAR")) por if (credencial.equals("APROVAR")).
- nota-sequencia: em fiscal/EmissorNotaFiscal.java, trocar sequencia++ por sequencia.
- sms-canal: em notificacao/NotificadorSms.java, trocar envios.add("SMS|" por envios.add("EMAIL|".
- relatorio-estado: em relatorio/RelatorioVendas.java, trocar pedido.status.equals("PAGO") por !pedido.status.equals("CANCELADO").

Para reproduzir, faça uma alteração de cada vez, rode mvn test, restaure o
arquivo e confirme os testes verdes antes de iniciar a próxima.

## Documentação, medições e integridade

- 23 arquivos Java de produção: 21 classes e duas interfaces.
- LojaService: 540 linhas, 31 métodos públicos, 13 privados, 67 if e nove
  colaboradores concretos criados em campos.
- Os três arquivos de coordenação/políticas destacados em OCP somam 81 if.
- As interfaces têm oito e três operações. A busca de métodos retorna 11;
  a busca por UnsupportedOperationException retorna sete ocorrências.
- Os comandos PowerShell de cada seção do ROTEIRO.md foram executados.
  A contagem de declarações foi ajustada para excluir as linhas de package.
- Classes/métodos citados foram conferidos nos fontes e as assinaturas
  públicas foram comparadas com API.md.
- Sete grupos @Nested; uso de @Test, @ParameterizedTest, @CsvSource e
  @MethodSource. Os testes não importam classes internas nem verificam mensagens.
- ZIP inspecionado sem .git/, target/, arquivos .class ou temporários.
  Fontes e testes são idênticos aos testados. Roteiro e script externos são
  idênticos aos internos. O script tem LF e permissão Unix de execução no ZIP.

Os arquivos em verificacao/ são evidências intencionais de execução, não
artefatos de compilação. Incluem logs das mutações, testes restaurados,
idiomas, comandos documentados e resultados estruturados.

## Limitação obrigatória: script Bash não executado com sucesso

O Git Bash não inicializou no ambiente restrito: retorno 0xC0000142
(3221225794 sem sinal). A tentativa de executar fora desse ambiente
foi rejeitada automaticamente porque a política proíbe aprovação de sandbox.
Nem mesmo bash -n pôde ser executado. Não houve permissão para continuar
essa validação, e nenhum mecanismo alternativo foi usado para contorná-la.

Portanto NÃO foram confirmados por execução:

- inicialização/commit inicial pelo script;
- duas execuções bem-sucedidas e sua idempotência;
- existência real das seis branches, pais e commits de NOTAS.md;
- ausência de NOTAS.md na main e preservação dos SHAs entre execuções;
- cenários de identidade ausente, árvore suja e branches preexistentes.

Foi feita apenas revisão estática: seis nomes únicos corretos, seis objetivos,
nove seções do modelo de notas, criação a partir de main, preservação das
referências existentes, retorno para main e ausência de comandos destrutivos.
Essa revisão não substitui a execução. Não se afirma que os testes rodaram
numa branch main criada pelo script: rodaram sobre a base destinada a ela.

## Como concluir a validação de branches no seu Windows

Use uma cópia extraída do ZIP em pasta independente. Configure sua identidade
Git conforme README.md e execute no Git Bash. Os comandos abaixo são um
procedimento pendente, NÃO uma verificação executada nesta entrega:

~~~bash
bash criar-branches.sh
base=$(git rev-parse main)
referencias=$(git show-ref --heads)
bash criar-branches.sh
test "$base" = "$(git rev-parse main)"
test "$referencias" = "$(git show-ref --heads)"
test "$(git for-each-ref --format='%(refname)' refs/heads | wc -l)" -eq 7
test "$(git rev-list --count main)" -eq 1
test "$(git branch --show-current)" = main
test -z "$(git status --porcelain)"
! git cat-file -e main:NOTAS.md 2>/dev/null
for branch in $(git for-each-ref --format='%(refname:short)' refs/heads/solid-*); do
    git show "$branch:NOTAS.md"
    test "$(git rev-parse "$branch^")" = "$base"
    test "$(git diff --name-only main "$branch")" = NOTAS.md
done
~~~

Esse cenário espera repositório novo, com sete branches ao todo e só um commit
na main. Branches de exercícios já modificadas devem ser preservadas; nesse
caso, não use o teste de diferença exclusiva de NOTAS.md para julgá-las.

## Limites do simulador

Sem persistência, concorrência ou serviços externos reais. Notas, cartões,
Pix, boletos, e-mails e SMS são simulações locais. UFs e credenciais são
deliberadamente limitadas; textos não escapam delimitadores. Contratos e
comportamentos históricos estão em API.md e CONTRATOS-LSP.md.
