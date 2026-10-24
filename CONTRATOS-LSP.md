# Contratos internos pretendidos e incompatibilidades atuais

Este documento separa intenção da abstração e comportamento atual do legado.
A suíte da main caracteriza a fachada e permanece verde. Nenhum teste vermelho
é necessário para evidenciar a incompatibilidade de substituição.

## Contrato A — Pagamento.calcularParcela

Pré-condições: valor finito > 0; parcelas inteiras entre 1 e 12.
Pós-condição: retorna valor/parcelas, HALF_UP com duas casas; não altera estado.
Invariante: consultar parcelas não altera saldo nem situação do pagamento.

Pagamento cumpre esse contrato; PagamentoCartao o herda. PagamentoPix sobrescreve
calcularParcela e exige parcelas == 1. Para valor 100 e parcelas 2, todas as
pré-condições da base valem; a base retorna 50, mas Pix lança IllegalArgumentException.
Isso fortalece uma pré-condição e elimina uma pós-condição prometida pela base.
Não é a presença da exceção que prova o problema: é a rejeição de uma entrada
explicitamente aceita pelo contrato do tipo usado pelo cliente.

GatewayPagamentoSimulado.consultarParcela cria a implementação e a usa por uma
variável Pagamento, sem uma capacidade distinta para parcelamento. Por essa rota:

~~~java
LojaService loja = new LojaService();
loja.consultarParcela("CARTAO", 100, 2); // 50.00
loja.consultarParcela("PIX", 100, 2);    // IllegalArgumentException
~~~

O teste Pagamentos.contratoAtualDasModalidades verifica ambos pela fachada.

## Contrato B — Pagamento.estornar

Pré-condições: situação APROVADO; valor finito > 0 e <= saldo autorizado restante.
Pós-condição: saldo novo = HALF_UP(saldo anterior - valor), sem mudar a situação.
Invariantes: saldo não negativo; soma dos estornos aceitos não excede valor
autorizado; uma operação rejeitada não muda saldo.

Após um pagamento aprovado de 110, estornar 10 é aceito pela base e por cartão.
PagamentoPix recusa o mesmo caso com UnsupportedOperationException quando
0 < valor < saldo. A pré-condição da base não exclui estornos parciais; o
subtipo restringe o domínio e não cumpre a redução de saldo prometida.
O invariante saldo >= 0 continua preservado: não se deve inventar uma violação
de invariante onde o problema demonstrado é de pré/pós-condição.

Pagamentos.contratoAtualDasModalidades verifica a recusa do parcial Pix e a
aceitação do integral. EfeitosERelatorios.cartaoEstornoParcialERelatorioLiquido
verifica um parcial aceito no cartão, o saldo líquido e o cancelamento do restante.

## O que NÃO foi apresentado como prova automática de LSP

- Exceção por estoque insuficiente: não envolve substituição de abstração.
- Estornar boleto ainda PENDENTE: a pré-condição de estorno exige APROVADO.
- Métodos de cartão/Pix/código de barras sem significado em todos os canais:
  são evidência de ISP; só seriam prova adicional de LSP se o contrato comum
  prometesse suporte universal, o que não foi declarado para essas operações.
- Qualquer sobrescrita isolada: herança e overrides não são defeitos por si.

## Preservar a fachada durante a refatoração

A política pública atual continua: Pix não parcela nem aceita parcial; não
mude esses resultados nem os testes. Corrija a promessa indevida dos tipos
internos: examine capacidades, composição e onde a política de cada modalidade
deve ser validada. A restrição comercial pode continuar na entrada apropriada
sem fazer um subtipo descumprir um contrato que afirma aceitar a operação.

Antes de escolher um padrão, escreva quais clientes precisam de parcelamento,
autorização, cancelamento e estorno. Confirme os contratos resultantes e revise
o fluxo genérico de GatewayPagamentoSimulado, em vez de apenas renomear classes.
