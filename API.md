# API pública estável

Classe: br.com.treino.solid.fachada.LojaService. Construtor padrão:
new LojaService(). Todos os testes usam apenas essa fachada.
Os modelos e colaboradores não fazem parte do contrato público do laboratório.

~~~java
public long cadastrarCliente(String nome, String enderecoEmail, String telefone, String cpf, boolean vip, String cep, String uf);
public String consultarCliente(long clienteId);
public long cadastrarProduto(String nome, String categoria, double preco, int quantidade);
public void alterarPreco(long produtoId, double preco);
public void desativarProduto(long produtoId);
public void adicionarEstoque(long produtoId, int quantidade);
public int consultarEstoque(long produtoId);
public long criarCarrinho(long clienteId);
public void adicionarAoCarrinho(long carrinhoId, long produtoId, int quantidade);
public String consultarCarrinho(long carrinhoId);
public long criarPedido(long carrinhoId, String modalidadeFrete, String dataCompra);
public void adicionarItem(long pedidoId, long produtoId, int quantidade);
public void removerItem(long pedidoId, long produtoId);
public String consultarItens(long pedidoId);
public void aplicarCupom(long pedidoId, String codigo);
public double calcularSubtotal(long pedidoId);
public double calcularTotal(long pedidoId);
public String consultarResumo(long pedidoId);
public double consultarParcela(String tipoPagamento, double valor, int parcelas);
public String finalizarPedido(long pedidoId, String tipoPagamento, String credencial, int parcelas, String tipoNotificacao);
public void confirmarBoleto(long pedidoId);
public void solicitarEstorno(long pedidoId, double valor);
public void cancelarPedido(long pedidoId);
public String consultarStatus(long pedidoId);
public String consultarResultadoPagamento(long pedidoId);
public String consultarCodigoBarras(long pedidoId);
public String consultarNotaFiscal(long pedidoId);
public String consultarEventosFiscais();
public String consultarNotificacoes();
public String consultarHistoricoPagamentos();
public String gerarRelatorioVendas();

~~~

## Cadastros e valores

Nome de cliente/produto não vazio, aparado nas pontas. E-mail simples com um @,
ponto no domínio e sem espaços. Telefone de 11 dígitos, CPF de 11 dígitos sem
pontuação, verificadores corretos e sem repetição; CPF é único por instância.
Endereço postal simplificado: CEP de oito dígitos e UF entre SP/RJ/MG/ES,
PR/SC/RS, GO/DF, BA/PE e AM/PA. Outras UFs são rejeitadas nesta base.

Categorias: LIVRO, ALIMENTO, ELETRONICO. Preço finito em (0, 1.000.000],
arredondado HALF_UP a duas casas no cadastro/alteração. Assim, 0.004 é aceito e
vira 0.00. Estoque inicial >= 0, reposição > 0 sem ultrapassar Integer.MAX_VALUE.
Produto pode ser desativado, inclusive depois de já estar num pedido.

Cliente, produto, carrinho e pedido têm sequências independentes iniciadas em 1.
Validações rejeitadas antes de inserir não consomem ids. Instâncias não compartilham estado.

## Carrinho e pedido

Carrinho pertence a um cliente e guarda itens em ordem de inserção. Quantidade
de 1 a 100 por produto, inclusive acumulada. Adições repetidas preservam o
primeiro preço capturado, mesmo após alteração de preço no catálogo. Não há
reserva de estoque no carrinho nem no pedido aberto.

criarPedido copia os itens do carrinho; futuras mudanças no carrinho não afetam
o pedido. O carrinho pode gerar mais de um pedido. Pedidos podem nascer vazios;
adicionarItem/removerItem operam no pedido aberto. Remover e readicionar captura
o novo preço. Data explícita ISO (AAAA-MM-DD); modalidade NORMAL/EXPRESSO/RETIRADA.

## Cálculos e cupons

1. Subtotal: soma de preços capturados × quantidades, arredondada a duas casas.
2. Desconto de categoria por linha, arredondado por linha: LIVRO 5%; ALIMENTO
   3% quando quantidade >= 10; ELETRONICO sem desconto.
3. VIP: 5% do subtotal após descontos de categoria, arredondado.
4. Cupom percentual incide no restante após categoria e VIP; fixo soma seu valor.
5. Frete é calculado sobre o subtotal original. Total: máximo entre zero e
   subtotal menos descontos, mais frete; HALF_UP. Valores double na fronteira.

| Cupom | Tipo | Mínimo do subtotal | Compatibilidade | Validade inclusiva |
|---|---|---:|---|---|
| DEZ | 10% | 100 | Todas as categorias | 2030-12-31 |
| VINTE | Fixo 20 | 200 | Todas | 2030-12-31 |
| LIVRO15 | 15% | 50 | Todos os itens devem ser LIVRO | 2030-12-31 |
| FRETEGRATIS | Zera frete | 0 | Somente NORMAL | 2030-12-31 |
| VENCIDO | Fixo 5 | 0 | Todas | 2024-12-31 |

VENCIDO é válido para um pedido datado até 2024-12-31; o nome não determina a
expiração. A data do pedido é a referência, nunca o relógio da máquina.
Cupom vazio limpa; outro substitui o anterior; códigos são sensíveis a maiúsculas.
Aplicação valida data, mínimo e compatibilidade. Cálculo/finalização revalidam:
é permitido alterar itens e tornar um cupom inválido; até limpá-lo ou restaurar
a compatibilidade, o próximo cálculo/finalização lança IllegalArgumentException.

Frete NORMAL: Sudeste 10, Sul 15, Centro-Oeste 20, Nordeste 25, Norte 30.
NORMAL é grátis para subtotal >= 300. EXPRESSO custa 1,8 × tarifa regional e
nunca recebe essa gratuidade por limite. RETIRADA custa zero.

## Pagamento e ciclo de vida

finalizarPedido exige itens, pedido ABERTO, canal válido, produtos ativos e
estoque para todos. Valida antes de chamar pagamento. Credenciais fictícias:

| Tipo | Aprovação | Recusa | Parcelas na finalização |
|---|---|---|---|
| PIX | APROVAR | RECUSAR | 1 |
| CARTAO | 4111111111111111 | 4000000000000002 | 1 a 12 |
| BOLETO | GERAR produz PENDENTE | Não há recusa simulada | 1 |

Não use cartões reais. Outros tipos, credenciais ou parcelas são inválidos.
finalizarPedido retorna APROVADO, RECUSADO ou PENDENTE. Recusa mantém ABERTO e
registra a tentativa, sem baixa, nota ou notificação; uma nova tentativa é permitida.
Dados inválidos não geram histórico de pagamento.

Aceito: baixa estoque uma única vez, muda para PAGO ou AGUARDANDO_PAGAMENTO e
notifica conforme EMAIL/SMS/AMBOS/NENHUM. PAGO emite nota uma única vez.
BOLETO reserva estoque na finalização, gera BOLETO-id e só emite nota quando
confirmarBoleto muda o estado para PAGO. Confirmar novamente é inválido.

Estados fechados bloqueiam edição, cupom e finalização. Cálculos consultam os
valores congelados após fechamento. ABERTO vazio pode ser calculado e cobra frete;
finalizar vazio falha. Cancelar ABERTO não reserva/devolve estoque. Cancelar
AGUARDANDO_PAGAMENTO devolve estoque e cancela pagamento sem nota/estorno.
Cancelar PAGO estorna o restante, registra cancelamento da nota, devolve estoque
e notifica. Repetir cancelamento é inválido. Overflow de devolução é checado
para todos os itens antes de qualquer pagamento, nota, estoque ou status mudar.

solicitarEstorno exige PAGO e valor positivo finito, arredondado HALF_UP,
não zero após arredondamento, até o saldo restante. CARTAO e boleto confirmado
aceitam estornos parciais. PIX aceita só o saldo inteiro; parcial lança
UnsupportedOperationException. Estorno não muda status, estoque, nota ou
notificações. Cancelamento posterior estorna só o restante; relatório desconta
estornos de pedidos ainda PAGO. Essa é uma simplificação comercial do simulador.

consultarParcela retorna divisão HALF_UP, com valor finito > 0 e parcelas 1–12
para CARTAO/BOLETO. PIX rejeita parcelas diferentes de 1. Portanto consultar
BOLETO em 2 parcelas é permitido, mas finalizar BOLETO em 2 não é. Esse
comportamento incoerente está caracterizado e também é material de análise.
consultarCodigoBarras só tem significado após pagamento BOLETO: antes de pagar
lança NoSuchElementException; para PIX/CARTAO aceitos lança UnsupportedOperationException.

## Efeitos e representações

- Cliente: nome|email|telefone|cpf|cep|uf.
- Itens/carrinho: id|nome|categoria|quantidade|preco, separados por ponto e vírgula.
- Resumo: STATUS|subtotal=...|desconto=...|frete=...|total=...|estornado=...
- Nota: NF-000001|pedido=1|cpf=...|valor=...; sequência só avança ao emitir.
- Eventos fiscais: EMITIDA|nota ou CANCELADA|nota, separados por ponto e vírgula.
- Notificações: EMAIL|destino|pedido=id|STATUS ou SMS|destino|pedido=id|STATUS.
  A consulta agrupa todos os e-mails antes de todos os SMS; dentro do canal,
  preserva ordem de envio. Não promete ordem cronológica entre canais.
- Histórico: id|TIPO|RESULTADO, id|BOLETO|CONFIRMADO, id|ESTORNO|valor e
  id|CANCELADO, separados por ponto e vírgula, em ordem de operação.
- Relatório: pedidos=N|bruto=...|estornos=...|liquido=...; conta apenas PAGO,
  incluindo frete. Pedidos cancelados são excluídos por completo.

Montantes são formatados com Locale.US e duas casas. Não há escape de
delimitadores nos nomes. Antes de qualquer cálculo, valores armazenados são
zero: cancelar um aberto preserva a última simulação ou zeros, se nunca calculado.

## Exceções e limites do laboratório

Id ausente: NoSuchElementException. Entrada inválida: IllegalArgumentException.
Estoque insuficiente, produto inativo e transições inválidas: IllegalStateException.
Operações sem suporte: UnsupportedOperationException nos casos documentados.
As mensagens não fazem parte do contrato. Se houver vários erros simultâneos,
a ordem de validação do legado determina qual é observado primeiro.

As simulações não implementam integração bancária/fiscal real, persistência,
concorrência, entrega física, cobrança de juros, fechamento contábil ou validação
de existência real de CPF/CEP. O objetivo é treinar design com um contrato observável.
