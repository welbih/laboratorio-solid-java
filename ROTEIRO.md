# Laboratório SOLID — roteiro de investigação e refatoração

## Contrato do treinamento

Não altere as 31 assinaturas públicas de LojaService nem seus resultados,
formatos ou tipos de exceção. API.md define o comportamento atual. Não edite
os testes de caracterização. Não faça merge das branches individuais na main.
Cada uma das seis branches começa da mesma base problemática, com seu NOTAS.md.
Não há refatorações resolvidas nesta entrega.

Faça passos pequenos, execute mvn test frequentemente e registre a motivação,
alternativas e consequências. O objetivo não é maximizar interfaces, diminuir
contadores ou seguir um padrão por obrigação. Uma abstração precisa proteger
uma responsabilidade ou variação concreta. Uma classe pequena também pode
violar SOLID; uma exceção não prova LSP por si.

O repositório nasce localmente quando você executa criar-branches.sh.
Leia VERIFICACAO.md para distinguir verificações executadas das limitações
do ambiente desta entrega. A execução do Bash e a idempotência não foram
verificadas aqui porque o processo não inicializou e a execução ampliada foi
rejeitada pela política de permissões. A suíte e os comandos PowerShell abaixo
foram executados; não confunda a revisão do script com sua execução.

## Mapa da base e medições

Pacote raiz: br.com.treino.solid. Há 23 arquivos de produção: 21 classes e duas
interfaces. LojaService tem 540 linhas físicas, 31 métodos públicos, 13 privados,
67 condicionais if e nove campos que instanciam colaboradores concretos.
As contagens são lexicais e mantêm a formatação original; não são métricas de
qualidade nem provas de conformidade.

| Arquivo | Métodos públicos | Privados | if | Outros indicadores |
|---|---:|---:|---:|---|
| fachada/LojaService.java | 31 | 13 | 67 | 9 dependências concretas em campos |
| pagamento/GatewayPagamentoSimulado.java | 7 | 2 | 7 | Cria 3 modalidades concretas |
| frete/CalculadoraFrete.java | 1 | 0 | 7 | Região e modalidade no mesmo cálculo |
| pagamento/Pagamento.java | 8 | 0 | 3 | 4 operações lançam UnsupportedOperationException |
| pagamento/PagamentoPix.java | 4 | 0 | 4 | 4 overrides, 2 contratos incompatíveis |
| notificacao/DespachanteNotificacao.java | 2 | 0 | 0 | Cada método usa 1 operação de contrato com 3 |
| notificacao/NotificadorEmail.java | 3 | 0 | 0 | 1 operação sem suporte |
| notificacao/NotificadorSms.java | 3 | 0 | 0 | 1 operação sem suporte |

OperacoesPagamento declara oito operações; OperacoesNotificacao, três.
Há ainda campos de entidades, mapas, objetos de valor simples e exceções
instanciados com new: não os confunda com as nove dependências de serviço.

Para repetir as medições no Windows, a partir da raiz do projeto:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ferramentas/medir.ps1
~~~

O JSON opcional contém todas as contagens, inclusive de arquivos fora da tabela:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ferramentas/medir.ps1 -Json
~~~

Não é necessário Python. O comando usa PowerShell e apenas lê fontes.

## O que a suíte protege

São 141 invocações, contando os casos parametrizados, organizadas em sete
grupos @Nested. Há @Test, @ParameterizedTest, @CsvSource e @MethodSource.

- Cliente válido, nome aparado, unicidade e verificadores de CPF; nome, e-mail,
  telefone, CEP e UF inválidos; falhas de cadastro não consomem ids.
- Três categorias, limites de preço e quantidade, NaN/infinito, HALF_UP,
  saldo inicial, reposição, overflow e produto desativado.
- Carrinho independente do pedido, preço capturado, agrupamento, ordem,
  limite acumulado, remoção/readição, data e modalidade inválidas.
- Subtotal; desconto de categoria por linha; mínimo do alimento; VIP;
  cupons percentuais/fixo/frete; mínimos, substituição, validade inclusiva,
  vencimento e incompatibilidade revalidada após mudar itens.
- Cinco tarifas regionais, NORMAL/EXPRESSO/RETIRADA e limite de gratuidade.
- Pix/cartão aprovados e recusados, tentativa posterior, entradas inválidas
  sem efeitos, boleto pendente/confirmado e reserva única de estoque.
- Fechamento vazio, estoque insuficiente, produto inativo após adição,
  dois pedidos disputando saldo e ausência de baixa parcial.
- Estados e bloqueios de edição; cancelar aberto, pendente e pago; devolução
  única; cancelamento sem efeitos parciais se houver overflow.
- Contrato atual de parcela e estorno Pix, estorno parcial no cartão,
  saldo líquido, cancelamento do restante e estorno inválido.
- Canais, destinos, conteúdo e ordem das notificações; sequência e eventos
  de notas; relatório que exclui abertos, pendentes e cancelados.
- Ids ausentes em todas as operações por id; sequências e instâncias isoladas.

Os testes não dependem de relógio, internet, banco, locale do sistema ou modelos.
Mensagens de exceção não são verificadas. Passe pelo fluxo de pagamento recusado
ao extrair serviços: ele grava histórico, mas não pode emitir nota, notificar
ou baixar estoque. Preserve também o comportamento não ideal documentado.

## 1. SRP — responsabilidade única

Branch: solid-01-responsabilidade-unica.

### Definição e motivação

Uma unidade deve agrupar decisões que mudam pelo mesmo motivo e separar as
que mudam por motivos diferentes. Uma equipe comercial, uma regra fiscal e
um provedor de comunicação podem mudar independentemente.

### Sintomas, locais e evidências

LojaService concentra 44 métodos e nove colaboradores concretos, além de seus
mapas e sequências. Seus 67 if misturam validação e políticas.
Procure cadastrarCliente, cadastrarProduto, criarPedido, recalcular,
finalizarPedido, confirmarBoleto, cancelarPedido, notificar e gerarRelatorioVendas.
Alterar regra de e-mail, desconto comercial ou momento de emitir nota exige
entrar na mesma classe de 540 linhas.

finalizarPedido não apenas coordena: valida canal, inspeciona produtos/estoque,
escolhe a sequência de pagamento, baixa saldo, atualiza estado, emite nota e
notifica. cancelarPedido faz verificações globais antes de aplicar efeitos.
A existência de EmissorNotaFiscal não elimina a responsabilidade fiscal
espalhada na coordenação de transições.

### Perguntas e passos

1. Quem solicita cada mudança? Quais dados e invariantes pertencem juntos?
2. Desenhe o fechamento e o cancelamento, incluindo recusa, boleto e overflow.
3. Extraia uma responsabilidade coesa e preserve sua ordem de efeitos.
4. Separe cálculo puro de efeitos; depois reavalie cadastro, estoque e emissão.
5. Mantenha a fachada como entrada estável e execute testes a cada extração.

Serviços de aplicação e objetos de domínio podem ajudar; não crie um serviço
por método apenas para reduzir tamanho. Uma classe Util ou um segundo
LojaService gigante apenas desloca o problema.

### Critério de conclusão e verificação

É possível explicar uma razão principal de mudança para cada componente.
Mudar formato de notificação não exige editar cálculo de desconto; mudar
cadastro não exige editar emissão fiscal. A fachada coordena sem conter esses
detalhes, e recusa/overflow continuam sem efeitos parciais. Não há meta numérica
arbitrária de linhas para SRP.

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ferramentas/medir.ps1
mvn test
~~~

Compare a concentração antes/depois e registre as razões de mudança. Esses
comandos mostram tamanho/estrutura e preservação, não aprovam SRP automaticamente.

## 2. OCP — aberto para extensão, fechado para modificação

Branch: solid-02-aberto-fechado.

### Definição e motivação

Proteja decisões estáveis contra mudanças recorrentes em variações conhecidas.
O princípio não exige que nenhum arquivo jamais mude; escolha a fronteira de
extensão com base em pressão real de mudança.

### Sintomas, locais e evidências

Há cinco famílias de seleção por texto/tipo:

1. Pagamento: GatewayPagamentoSimulado.criar e LojaService.finalizarPedido.
2. Frete: CalculadoraFrete.calcular, com região e modalidade.
3. Categoria: LojaService.recalcular decide LIVRO/ALIMENTO/ELETRONICO.
4. Cupom: LojaService.carregarCupom e recalcular decidem código e tipo.
5. Notificação: LojaService.notificar decide EMAIL/SMS/AMBOS/NENHUM.

Os três arquivos LojaService, GatewayPagamentoSimulado e CalculadoraFrete
somam 81 if (67 + 7 + 7). Parte é validação legítima, não seleção de estratégia.
O gateway tem sete métodos públicos e dois privados; o cálculo de frete cabe
em um método, mas já reúne sete if. Isso mostra que tamanho sozinho não mede OCP.

### Perguntas e passos

1. Quais famílias realmente mudariam de forma independente? Qual é apenas dado?
2. Escolha uma família, preserve as entradas textuais na fachada e identifique
   a operação comum e as pré-condições de cada alternativa.
3. Separe políticas sem perder composição/ordem de descontos e arredondamentos.
4. Concentre a montagem e a seleção em um lugar explícito e simples.
5. Descreva em NOTAS.md como uma nova política seria adicionada e quais arquivos
   estáveis deixariam de mudar; não é necessário criar comportamento novo na API.

Strategy, polimorfismo e um registro/fábrica pequeno são opções, não obrigações.
Dados de cupom podem pedir configuração; fluxos de estado podem pedir outra
abordagem. Não converta toda validação if em hierarquia.

### Armadilhas e conclusão

Trocar if por switch ou mover a mesma cadeia para outra classe não cria uma
fronteira de extensão por si. Não introduza reflexão ou um contêiner para
três alternativas. Preserve o mínimo do cupom sobre subtotal original e a
gratuidade de NORMAL, que não vale para EXPRESSO.

Pronto: cada família escolhida possui um ponto de extensão coerente; é possível
acrescentar uma política sem editar o algoritmo de fechamento/cálculo estável.
Critique também o custo da abstração. Suíte original verde.

~~~powershell
Select-String -Path src/main/java/br/com/treino/solid/fachada/LojaService.java,src/main/java/br/com/treino/solid/frete/CalculadoraFrete.java,src/main/java/br/com/treino/solid/pagamento/GatewayPagamentoSimulado.java -Pattern 'else if'
mvn test
~~~

Na base, o comando localiza cadeias reais. Depois, examine cada seleção restante:
zero else if não é o objetivo isolado e não prova extensibilidade.

## 3. LSP — substituição de Liskov

Branch: solid-03-substituicao-liskov.

### Definição e motivação

Um cliente que respeita o contrato da abstração deve poder usar seus subtipos
sem novas restrições inesperadas ou perda de garantias. Analise pré-condições,
pós-condições e invariantes; não conte exceções como prova.

### Sintomas, locais e evidências

Leia CONTRATOS-LSP.md antes de refatorar. Pagamento tem oito métodos públicos.
PagamentoPix possui quatro overrides e quatro if; dois overrides restringem
contratos concretos da base:

- calcularParcela: base aceita 1–12, Pix exige 1; entrada (100, 2) é válida para
  a base e rejeitada pelo subtipo.
- estornar: base aceita parcial em saldo aprovado; Pix lança
  UnsupportedOperationException para 0 < valor < saldo, sem realizar a
  redução de saldo prometida.

GatewayPagamentoSimulado.consultarParcela e estornar consomem variáveis do tipo
Pagamento, por isso as diferenças atingem clientes genéricos. A fachada permite
demonstrá-las sem testes internos. PagamentoCartao aceita o parcial; o teste
Pagamentos.contratoAtualDasModalidades caracteriza a restrição Pix.

Saldo não negativo continua sendo invariante preservado no caso Pix rejeitado.
Não alegue quebra desse invariante para justificar o problema de pré/pós-condição.

### Perguntas e passos

1. Escreva o que cada operação promete e exige, sem olhar primeiro os subtipos.
2. Compare as entradas válidas, saídas e alterações de estado em cada subtipo.
3. Separe capacidades comerciais reais de promessas universais indevidas.
4. Reavalie herança, composição e os tipos usados pelo gateway.
5. Preserve na fachada as recusas históricas do Pix; mova a regra comercial
   para a fronteira/capacidade adequada, em vez de mudar os testes para aceitar.

Composição e interfaces por capacidade podem ajudar. Não enfraqueça o contrato
da base até ele deixar de prometer qualquer coisa, e não capture todas as
exceções para fingir que as substituições são equivalentes.

### Critério de conclusão e verificação

Para cada abstração resultante, todos os implementadores cumprem as mesmas
pré/pós-condições declaradas. Nenhum fluxo genérico pressupõe capacidade que
alguma implementação não possui. Os testes públicos continuam verdes, inclusive
a recusa de Pix parcelado e do parcial: compatibilidade externa e correção da
hierarquia interna são avaliadas separadamente.

~~~powershell
Select-String -Path src/main/java/br/com/treino/solid/pagamento/Pagamento*.java -Pattern 'calcularParcela|void estornar|parcelas != 1|valor < saldo'
mvn test
~~~

Revise os contratos e a rota dos clientes; a busca apenas aponta os locais.

## 4. ISP — segregação de interfaces

Branch: solid-04-segregacao-interfaces.

### Definição e motivação

Clientes não devem depender de operações que não usam. A coesão do contrato
é relativa às necessidades dos consumidores; quantidade de métodos é um indício,
não a definição do princípio.

### Sintomas, locais e evidências

OperacoesPagamento impõe oito operações: autorização, parcela, estorno,
cancelamento, geração/consulta de código de barras e validações de Pix/cartão.
Pagamento implementa a interface e lança UnsupportedOperationException em
quatro operações sem significado geral. Os subtipos herdam essa bagagem.

OperacoesNotificacao tem três operações. NotificadorEmail implementa enviarSms
lançando exceção; NotificadorSms faz o inverso. DespachanteNotificacao tem dois
métodos: email precisa apenas de enviarEmail e sms apenas de enviarSms, mas
ambos dependem do contrato inteiro de três operações.

São seis implementações de operação sem suporte ligadas a esses contratos:
quatro na base Pagamento e uma em cada notificador. A sétima ocorrência em
produção é o estorno parcial de PagamentoPix, analisado separadamente em LSP.

### Perguntas e passos

1. Quais operações cada cliente realmente chama? Quem só consulta envios?
2. Faça a matriz cliente × operação para pagamentos e notificações.
3. Recorte contratos coesos pelos usos, sem criar interface por método mecanicamente.
4. Ajuste consumidores para depender só das capacidades necessárias.
5. Revise métodos herdados e operações sem suporte que ficaram escondidas.

Interfaces por papel e adaptadores podem ajudar. Não duplique estados/históricos
ao segregar canais e não crie uma interface vazia apenas como marcador.

### Critério de conclusão e verificação

Cada consumidor depende apenas das operações do seu papel. Implementações
internas não precisam fornecer métodos irrelevantes ou stubs que lançam exceção.
consultarCodigoBarras da fachada preserva seu comportamento histórico sem
impor código de barras a todos os tipos internos. Suíte verde.

~~~powershell
Select-String -Path src/main/java/br/com/treino/solid/pagamento/OperacoesPagamento.java,src/main/java/br/com/treino/solid/notificacao/OperacoesNotificacao.java -Pattern '^    .*;$'
Get-ChildItem src/main/java -Filter '*.java' -Recurse | Select-String -Pattern 'throw new UnsupportedOperationException'
mvn test
~~~

A primeira busca retorna 11 declarações na base; a segunda, sete ocorrências.
Após refatorar, inspecione o motivo de cada ocorrência restante, inclusive
políticas de compatibilidade pública. Remover a palavra da busca não resolve ISP.

## 5. DIP — inversão de dependência

Branch: solid-05-inversao-dependencia.

### Definição e motivação

Políticas de alto nível não devem depender dos detalhes de infraestrutura;
ambos colaboram por contratos que atendem à política. Uma interface colocada
ao lado de uma implementação não garante a direção correta da dependência.

### Sintomas, locais e evidências

LojaService instancia nove colaboradores em campos: RepositorioPedidoMemoria,
EstoqueMemoria, GatewayPagamentoSimulado, CalculadoraFrete, NotificadorEmail,
NotificadorSms, DespachanteNotificacao, EmissorNotaFiscal e RelatorioVendas.
O construtor padrão deixa esses detalhes fixos. Não há ponto para trocar uma
dependência de armazenamento, cobrança ou comunicação sem editar a classe.

Procure finalizarPedido, confirmarBoleto, cancelarPedido, notificar e
gerarRelatorioVendas. O gateway também instancia três pagamentos concretos em
criar. As 61 construções new lexicais na fachada incluem entidades, coleções e
exceções: só nove correspondem aos colaboradores de serviço listados.

### Perguntas e passos

1. Qual política precisa do detalhe e qual informação mínima precisa receber?
2. Defina contratos orientados aos casos de uso, não cópias de todas as APIs concretas.
3. Introduza pontos internos de composição/injeção por construtor, em passos pequenos.
4. Mantenha new LojaService() como configuração padrão e a API pública intacta.
5. Centralize a montagem fora das decisões de negócio e avalie se uma substituição
   de repositório/notificador exigiria alteração na política de checkout.

Injeção manual e uma raiz de composição simples são suficientes. Não use Spring,
framework de DI, registrador global ou service locator. Não injete cada String,
coleção e objeto recém-criado apenas porque foi criado com new.

### Critério de conclusão e verificação

Políticas recebem colaboradores por contratos coesos e não constroem seus
detalhes de infraestrutura. A montagem padrão continua funcionando e os testes
originais passam. Mostre em NOTAS.md onde uma implementação alternativa seria
conectada sem editar o caso de uso. Não transforme a fachada inteira em interface.

~~~powershell
Select-String -Path src/main/java/br/com/treino/solid/fachada/LojaService.java -Pattern 'private final.*= new [A-Z][A-Za-z]+\(\);'
mvn test
~~~

A busca retorna nove campos na base. Uma raiz de composição pode continuar
instanciando concretos; revise onde o new ficou e qual direção os imports seguem.

## Desafio completo

Branch: solid-06-desafio-completo, criada diretamente da main. Não faça merge
automático dos exercícios individuais.

Ordem sugerida:

1. SRP: mapear decisões e separar cálculo de efeitos revela fronteiras reais.
2. ISP: a matriz de consumidores ajuda a evitar contratos de DI grandes demais.
3. LSP: valide promessas dos contratos antes de confiar em substituições.
4. DIP: conecte políticas e implementações pelos contratos já examinados.
5. OCP: abra as famílias de variação que ainda exigem edição repetida.

A ordem não é rígida: OCP pode sugerir uma abstração que precisa de nova análise
LSP; SRP pode revelar novos papéis de ISP. Registre essas sobreposições, o custo
de complexidade e quais abstrações não introduziria em produção.

Conclua com os 141 casos verdes, revisão das cinco matrizes/contratos e a
comparação das medições. O design final precisa preservar a atomicidade lógica
do simulador em memória; a base não promete transações distribuídas com bancos,
serviços externos ou concorrência. Não tente resolver esses problemas ausentes
apenas para praticar um padrão.
