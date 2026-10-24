package br.com.treino.solid.fachada;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class LojaServiceTest {
    private LojaService loja;
    private long cliente;
    private long produto;
    private long carrinho;
    private long pedido;

    @BeforeEach
    void preparar() {
        loja = new LojaService();
        cliente = cadastrar(false, "SP");
        produto = loja.cadastrarProduto(" Produto ", "ELETRONICO", 100, 20);
        carrinho = loja.criarCarrinho(cliente);
        pedido = loja.criarPedido(carrinho, "NORMAL", "2026-10-24");
    }

    private long cadastrar(boolean vip, String uf) {
        return loja.cadastrarCliente(" Ana ", "ana@exemplo.com", "11999999999", "52998224725", vip, "01001000", uf);
    }

    private void cenario(double preco, String categoria, int quantidade, boolean vip, String uf, String frete, String data) {
        loja = new LojaService();
        cliente = cadastrar(vip, uf);
        produto = loja.cadastrarProduto("Produto", categoria, preco, 100);
        carrinho = loja.criarCarrinho(cliente);
        pedido = loja.criarPedido(carrinho, frete, data);
        loja.adicionarItem(pedido, produto, quantidade);
    }

    private String pagar(String tipo, String credencial, int parcelas, String notificacao) {
        loja.adicionarItem(pedido, produto, 1);
        return loja.finalizarPedido(pedido, tipo, credencial, parcelas, notificacao);
    }

    static Stream<Arguments> clientesInvalidos() {
        return Stream.of(
            Arguments.of(null,"a@b.com","11999999999","11144477735","01001000","SP"),
            Arguments.of(" ","a@b.com","11999999999","11144477735","01001000","SP"),
            Arguments.of("B",null,"11999999999","11144477735","01001000","SP"),
            Arguments.of("B","a@b","11999999999","11144477735","01001000","SP"),
            Arguments.of("B","a b@c.com","11999999999","11144477735","01001000","SP"),
            Arguments.of("B","a@@b.com","11999999999","11144477735","01001000","SP"),
            Arguments.of("B","a@b.com",null,"11144477735","01001000","SP"),
            Arguments.of("B","a@b.com","11999","11144477735","01001000","SP"),
            Arguments.of("B","a@b.com","abcdefghijk","11144477735","01001000","SP"),
            Arguments.of("B","a@b.com","11999999999",null,"01001000","SP"),
            Arguments.of("B","a@b.com","11999999999","11111111111","01001000","SP"),
            Arguments.of("B","a@b.com","11999999999","11144477734","01001000","SP"),
            Arguments.of("B","a@b.com","11999999999","11144477725","01001000","SP"),
            Arguments.of("B","a@b.com","11999999999","111.444.777-35","01001000","SP"),
            Arguments.of("B","a@b.com","11999999999","11144477735",null,"SP"),
            Arguments.of("B","a@b.com","11999999999","11144477735","01001-000","SP"),
            Arguments.of("B","a@b.com","11999999999","11144477735","abcdefgh","SP"),
            Arguments.of("B","a@b.com","11999999999","11144477735","01001000",null),
            Arguments.of("B","a@b.com","11999999999","11144477735","01001000","sp"),
            Arguments.of("B","a@b.com","11999999999","11144477735","01001000","AC")
        );
    }

    @Nested
    class Cadastros {
        @Test void cadastroValidoEUnicidade() {
            assertEquals("Ana|ana@exemplo.com|11999999999|52998224725|01001000|SP", loja.consultarCliente(cliente));
            assertThrowsExactly(IllegalArgumentException.class, () -> cadastrar(false,"SP"));
            assertEquals(2,loja.cadastrarCliente("B","b@b.com","21999999999","11144477735",false,"01001000","RJ"));
            assertEquals(1,cliente);
        }
        @ParameterizedTest @MethodSource("br.com.treino.solid.fachada.LojaServiceTest#clientesInvalidos")
        void cadastroInvalido(String nome,String email,String telefone,String cpf,String cep,String uf) {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.cadastrarCliente(nome,email,telefone,cpf,false,cep,uf));
            assertEquals(2,loja.cadastrarCliente("B","b@b.com","21999999999","11144477735",false,"01001000","RJ"));
        }
        @ParameterizedTest @ValueSource(doubles={0,-1,1000000.01,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
        void rejeitaPreco(double preco) {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.cadastrarProduto("P","LIVRO",preco,1));
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.alterarPreco(produto,preco));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "})
        void rejeitaNomeProduto(String nome) {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.cadastrarProduto(nome,"LIVRO",1,1));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"livro","OUTRO"})
        void rejeitaCategoria(String categoria) {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.cadastrarProduto("P",categoria,1,1));
        }
        @ParameterizedTest @ValueSource(strings={"LIVRO","ALIMENTO","ELETRONICO"})
        void aceitaCategorias(String categoria) {
            assertEquals(0,loja.consultarEstoque(loja.cadastrarProduto("P",categoria,1000000,0)));
        }
        @Test void reposicaoEOverflow() {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.cadastrarProduto("P","LIVRO",1,-1));
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.adicionarEstoque(produto,0));
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.adicionarEstoque(produto,-1));
            loja.adicionarEstoque(produto,2);
            assertEquals(22,loja.consultarEstoque(produto));
            long maximo=loja.cadastrarProduto("M","LIVRO",1,Integer.MAX_VALUE);
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.adicionarEstoque(maximo,1));
            assertEquals(Integer.MAX_VALUE,loja.consultarEstoque(maximo));
        }
    }

    @Nested
    class CarrinhosEItens {
        @Test void preservaPrecoEAgrupamentoECopiaCarrinho() {
            loja.adicionarAoCarrinho(carrinho,produto,1);
            loja.alterarPreco(produto,150);
            loja.adicionarAoCarrinho(carrinho,produto,1);
            long criado=loja.criarPedido(carrinho,"RETIRADA","2026-10-24");
            loja.adicionarAoCarrinho(carrinho,produto,1);
            assertEquals("1|Produto|ELETRONICO|3|100.00",loja.consultarCarrinho(carrinho));
            assertEquals("1|Produto|ELETRONICO|2|100.00",loja.consultarItens(criado));
            assertEquals(200,loja.calcularSubtotal(criado));
            assertEquals("",loja.consultarItens(pedido));
            assertEquals(20,loja.consultarEstoque(produto));
            loja.removerItem(criado,produto);
            loja.adicionarItem(criado,produto,1);
            assertEquals("1|Produto|ELETRONICO|1|150.00",loja.consultarItens(criado));
        }
        @Test void ordemELimiteAcumulado() {
            long outro=loja.cadastrarProduto(" Outro ","ALIMENTO",1.005,0);
            loja.adicionarItem(pedido,produto,99);
            loja.adicionarItem(pedido,produto,1);
            loja.adicionarItem(pedido,outro,1);
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.adicionarItem(pedido,produto,1));
            assertEquals("1|Produto|ELETRONICO|100|100.00;2|Outro|ALIMENTO|1|1.01",loja.consultarItens(pedido));
            assertEquals(10001.01,loja.calcularSubtotal(pedido),.000001);
        }
        @ParameterizedTest @ValueSource(ints={-1,0,101,Integer.MAX_VALUE})
        void quantidadeInvalida(int quantidade) {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.adicionarItem(pedido,produto,quantidade));
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.adicionarAoCarrinho(carrinho,produto,quantidade));
            assertEquals("",loja.consultarItens(pedido));
        }
        @Test void inativoEItemAusente() {
            loja.desativarProduto(produto);
            assertThrowsExactly(IllegalStateException.class, () -> loja.adicionarItem(pedido,produto,1));
            assertThrowsExactly(IllegalStateException.class, () -> loja.adicionarAoCarrinho(carrinho,produto,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.removerItem(pedido,produto));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"2025-02-29","24/10/2026","nao-data"})
        void dataInvalida(String data) {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.criarPedido(carrinho,"NORMAL",data));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"normal","OUTRO"})
        void modalidadeInvalida(String modalidade) {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.criarPedido(carrinho,modalidade,"2026-10-24"));
        }
    }

    @Nested
    class CalculosECupons {
        @ParameterizedTest @CsvSource({"SP,10","RJ,10","MG,10","ES,10","PR,15","SC,15","RS,15","GO,20","DF,20","BA,25","PE,25","AM,30","PA,30"})
        void freteRegional(String uf,int frete) {
            cenario(100,"ELETRONICO",1,false,uf,"NORMAL","2026-10-24");
            assertEquals(100+frete,loja.calcularTotal(pedido));
        }
        @ParameterizedTest @CsvSource({"299.99,NORMAL,309.99","300,NORMAL,300","300,EXPRESSO,318","100,RETIRADA,100","100,EXPRESSO,118"})
        void modalidadesELimite(double preco,String modalidade,double total) {
            cenario(preco,"ELETRONICO",1,false,"SP",modalidade,"2026-10-24");
            assertEquals(total,loja.calcularTotal(pedido),.000001);
        }
        @ParameterizedTest @CsvSource({
            "100,ELETRONICO,1,false,DEZ,10.00,100.00",
            "100,LIVRO,1,false,DEZ,14.50,95.50",
            "100,LIVRO,1,true,DEZ,18.78,91.22",
            "200,ELETRONICO,1,false,VINTE,20.00,190.00",
            "50,LIVRO,1,false,LIVRO15,9.63,50.37",
            "10,ALIMENTO,10,false,DEZ,12.70,97.30"})
        void composicaoDescontos(double preco,String categoria,int quantidade,boolean vip,String cupom,String desconto,String total) {
            cenario(preco,categoria,quantidade,vip,"SP","NORMAL","2026-10-24");
            loja.aplicarCupom(pedido,cupom);
            String subtotal=String.format(Locale.US,"%.2f",preco*quantidade);
            assertEquals("ABERTO|subtotal="+subtotal+"|desconto="+desconto+"|frete=10.00|total="+total+"|estornado=0.00",loja.consultarResumo(pedido));
        }
        @ParameterizedTest @CsvSource({"9,100","10,107"})
        void limiteAlimento(int quantidade,double total) {
            cenario(10,"ALIMENTO",quantidade,false,"SP","NORMAL","2026-10-24");
            assertEquals(total,loja.calcularTotal(pedido));
        }
        @Test void arredondamentoEVazio() {
            assertEquals(0,loja.calcularSubtotal(pedido));
            assertEquals(10,loja.calcularTotal(pedido));
            cenario(1.10,"ELETRONICO",1,true,"SP","NORMAL","2026-10-24");
            assertEquals(11.04,loja.calcularTotal(pedido),.000001);
            cenario(.004,"ELETRONICO",1,false,"SP","RETIRADA","2026-10-24");
            assertEquals(0,loja.calcularTotal(pedido));
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM"));
        }
        @Test void cupomFreteESubstituicao() {
            loja.adicionarItem(pedido,produto,1);
            loja.aplicarCupom(pedido,"DEZ");
            loja.aplicarCupom(pedido,"FRETEGRATIS");
            assertEquals(100,loja.calcularTotal(pedido));
            loja.aplicarCupom(pedido,"");
            assertEquals(110,loja.calcularTotal(pedido));
        }
        @ParameterizedTest @NullSource @ValueSource(strings={"dez","INVALIDO"," "})
        void cupomInvalido(String codigo) {
            loja.adicionarItem(pedido,produto,1);
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.aplicarCupom(pedido,codigo));
        }
        @ParameterizedTest @CsvSource({"99.99,DEZ","199.99,VINTE","49.99,LIVRO15"})
        void minimoCupom(double preco,String codigo) {
            cenario(preco,"LIVRO",1,false,"SP","NORMAL","2026-10-24");
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.aplicarCupom(pedido,codigo));
        }
        @Test void validadeInclusivaEExpirado() {
            cenario(100,"ELETRONICO",1,false,"SP","NORMAL","2030-12-31");
            loja.aplicarCupom(pedido,"DEZ");
            assertEquals(100,loja.calcularTotal(pedido));
            cenario(100,"ELETRONICO",1,false,"SP","NORMAL","2031-01-01");
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.aplicarCupom(pedido,"DEZ"));
            cenario(100,"ELETRONICO",1,false,"SP","NORMAL","2024-12-31");
            loja.aplicarCupom(pedido,"VENCIDO");
            assertEquals(105,loja.calcularTotal(pedido));
            cenario(100,"ELETRONICO",1,false,"SP","NORMAL","2025-01-01");
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.aplicarCupom(pedido,"VENCIDO"));
        }
        @Test void incompatibilidadeERevalidacao() {
            loja.adicionarItem(pedido,produto,1);
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.aplicarCupom(pedido,"LIVRO15"));
            long expresso=loja.criarPedido(carrinho,"EXPRESSO","2026-10-24");
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.aplicarCupom(expresso,"FRETEGRATIS"));
            cenario(100,"LIVRO",1,false,"SP","NORMAL","2026-10-24");
            loja.aplicarCupom(pedido,"LIVRO15");
            long outro=loja.cadastrarProduto("Outro","ELETRONICO",100,1);
            loja.adicionarItem(pedido,outro,1);
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.calcularTotal(pedido));
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM"));
            assertEquals("",loja.consultarHistoricoPagamentos());
            assertEquals(100,loja.consultarEstoque(produto));
        }
    }

    @Nested
    class Pagamentos {
        @ParameterizedTest @CsvSource({"PIX,APROVAR,1","CARTAO,4111111111111111,3"})
        void aprovado(String tipo,String credencial,int parcelas) {
            assertEquals("APROVADO",pagar(tipo,credencial,parcelas,"NENHUM"));
            assertEquals("PAGO",loja.consultarStatus(pedido));
            assertEquals("APROVADO",loja.consultarResultadoPagamento(pedido));
            assertEquals(19,loja.consultarEstoque(produto));
            assertEquals("1|"+tipo+"|APROVADO",loja.consultarHistoricoPagamentos());
            assertEquals("NF-000001|pedido=1|cpf=52998224725|valor=110.00",loja.consultarNotaFiscal(pedido));
        }
        @ParameterizedTest @CsvSource({"PIX,RECUSAR","CARTAO,4000000000000002"})
        void recusadoSemEfeitosEPermiteNovaTentativa(String tipo,String credencial) {
            assertEquals("RECUSADO",pagar(tipo,credencial,1,"AMBOS"));
            assertEquals("ABERTO",loja.consultarStatus(pedido));
            assertEquals("RECUSADO",loja.consultarResultadoPagamento(pedido));
            assertEquals(20,loja.consultarEstoque(produto));
            assertEquals("",loja.consultarNotaFiscal(pedido));
            assertEquals("",loja.consultarNotificacoes());
            assertEquals("",loja.consultarEventosFiscais());
            assertEquals("APROVADO",loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM"));
            assertEquals("1|"+tipo+"|RECUSADO;1|PIX|APROVADO",loja.consultarHistoricoPagamentos());
        }
        @ParameterizedTest @CsvSource({"PIX,INVALIDA,1","PIX,APROVAR,2","CARTAO,123,1","CARTAO,4111111111111111,0","CARTAO,4111111111111111,13","BOLETO,GERAR,2","BOLETO,INVALIDA,1","OUTRO,APROVAR,1"})
        void entradaInvalidaSemEfeitos(String tipo,String credencial,int parcelas) {
            loja.adicionarItem(pedido,produto,1);
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.finalizarPedido(pedido,tipo,credencial,parcelas,"NENHUM"));
            assertEquals("ABERTO",loja.consultarStatus(pedido));
            assertEquals(20,loja.consultarEstoque(produto));
            assertEquals("",loja.consultarHistoricoPagamentos());
        }
        @Test void boletoReservaConfirmaENaoDuplica() {
            assertEquals("PENDENTE",pagar("BOLETO","GERAR",1,"EMAIL"));
            assertEquals("AGUARDANDO_PAGAMENTO",loja.consultarStatus(pedido));
            assertEquals("BOLETO-1",loja.consultarCodigoBarras(pedido));
            assertEquals(19,loja.consultarEstoque(produto));
            assertEquals("",loja.consultarNotaFiscal(pedido));
            assertEquals("pedidos=0|bruto=0.00|estornos=0.00|liquido=0.00",loja.gerarRelatorioVendas());
            loja.confirmarBoleto(pedido);
            assertEquals("PAGO",loja.consultarStatus(pedido));
            assertEquals("APROVADO",loja.consultarResultadoPagamento(pedido));
            assertEquals("1|BOLETO|PENDENTE;1|BOLETO|CONFIRMADO",loja.consultarHistoricoPagamentos());
            assertEquals("EMAIL|ana@exemplo.com|pedido=1|AGUARDANDO_PAGAMENTO;EMAIL|ana@exemplo.com|pedido=1|PAGO",loja.consultarNotificacoes());
            assertEquals(19,loja.consultarEstoque(produto));
            assertThrowsExactly(IllegalStateException.class, () -> loja.confirmarBoleto(pedido));
            assertEquals("EMITIDA|NF-000001|pedido=1|cpf=52998224725|valor=110.00",loja.consultarEventosFiscais());
        }
        @Test void contratoAtualDasModalidades() {
            assertEquals(50,loja.consultarParcela("CARTAO",100,2));
            assertEquals(33.33,loja.consultarParcela("CARTAO",100,3));
            assertEquals(100,loja.consultarParcela("PIX",100,1));
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.consultarParcela("PIX",100,2));
            assertEquals(50,loja.consultarParcela("BOLETO",100,2));
            pagar("PIX","APROVAR",1,"NENHUM");
            assertThrowsExactly(UnsupportedOperationException.class, () -> loja.solicitarEstorno(pedido,10));
            assertThrowsExactly(UnsupportedOperationException.class, () -> loja.consultarCodigoBarras(pedido));
            assertEquals("PAGO|subtotal=100.00|desconto=0.00|frete=10.00|total=110.00|estornado=0.00",loja.consultarResumo(pedido));
            loja.solicitarEstorno(pedido,110);
            assertEquals("pedidos=1|bruto=110.00|estornos=110.00|liquido=0.00",loja.gerarRelatorioVendas());
        }
        @ParameterizedTest @ValueSource(doubles={0,-1,Double.NaN,Double.POSITIVE_INFINITY})
        void parcelaValorInvalido(double valor) {
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.consultarParcela("CARTAO",valor,1));
        }
        @Test void consultaCodigoAntesDoPagamento() {
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarCodigoBarras(pedido));
            assertThrowsExactly(IllegalStateException.class, () -> loja.confirmarBoleto(pedido));
        }
    }

    @Nested
    class EstoqueEEstados {
        @Test void vazioNaoFinaliza() {
            assertThrowsExactly(IllegalStateException.class, () -> loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM"));
        }
        @Test void validacaoEstoqueAtomicaAntesDePagamento() {
            long outro=loja.cadastrarProduto("Outro","LIVRO",10,0);
            loja.adicionarItem(pedido,produto,20);
            loja.adicionarItem(pedido,outro,1);
            assertThrowsExactly(IllegalStateException.class, () -> loja.finalizarPedido(pedido,"PIX","APROVAR",1,"EMAIL"));
            assertEquals(20,loja.consultarEstoque(produto));
            assertEquals("",loja.consultarHistoricoPagamentos());
            assertEquals("",loja.consultarNotificacoes());
            loja.adicionarEstoque(outro,1);
            loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM");
            assertEquals(0,loja.consultarEstoque(produto));
            assertEquals(0,loja.consultarEstoque(outro));
        }
        @Test void inativoAposAdicaoNaoFinaliza() {
            loja.adicionarItem(pedido,produto,1);
            loja.desativarProduto(produto);
            assertThrowsExactly(IllegalStateException.class, () -> loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM"));
            assertEquals(20,loja.consultarEstoque(produto));
        }
        @Test void pedidosDisputamSaldo() {
            long outro=loja.criarPedido(carrinho,"NORMAL","2026-10-24");
            loja.adicionarItem(pedido,produto,20);
            loja.adicionarItem(outro,produto,1);
            loja.finalizarPedido(pedido,"BOLETO","GERAR",1,"NENHUM");
            assertThrowsExactly(IllegalStateException.class, () -> loja.finalizarPedido(outro,"PIX","APROVAR",1,"NENHUM"));
            loja.cancelarPedido(pedido);
            assertEquals("APROVADO",loja.finalizarPedido(outro,"PIX","APROVAR",1,"NENHUM"));
            assertEquals(19,loja.consultarEstoque(produto));
        }
        @ParameterizedTest @ValueSource(strings={"PAGO","AGUARDANDO_PAGAMENTO","CANCELADO"})
        void impedeEdicao(String estado) {
            loja.adicionarItem(pedido,produto,1);
            if (estado.equals("PAGO")) loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM");
            else if (estado.equals("AGUARDANDO_PAGAMENTO")) loja.finalizarPedido(pedido,"BOLETO","GERAR",1,"NENHUM");
            else loja.cancelarPedido(pedido);
            assertThrowsExactly(IllegalStateException.class, () -> loja.adicionarItem(pedido,produto,1));
            assertThrowsExactly(IllegalStateException.class, () -> loja.removerItem(pedido,produto));
            assertThrowsExactly(IllegalStateException.class, () -> loja.aplicarCupom(pedido,"DEZ"));
            assertThrowsExactly(IllegalStateException.class, () -> loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM"));
        }
        @Test void cancelaAbertoSemPagamentoOuReposicao() {
            loja.adicionarItem(pedido,produto,1);
            loja.cancelarPedido(pedido);
            assertEquals(20,loja.consultarEstoque(produto));
            assertEquals("",loja.consultarHistoricoPagamentos());
            assertEquals("",loja.consultarNotificacoes());
            assertEquals("CANCELADO|subtotal=100.00|desconto=0.00|frete=0.00|total=0.00|estornado=0.00",loja.consultarResumo(pedido));
            assertThrowsExactly(IllegalStateException.class, () -> loja.cancelarPedido(pedido));
        }
        @Test void cancelaPagoEstornaUmaVezERegistraFiscal() {
            pagar("PIX","APROVAR",1,"EMAIL");
            loja.cancelarPedido(pedido);
            assertEquals("CANCELADO",loja.consultarStatus(pedido));
            assertEquals(20,loja.consultarEstoque(produto));
            assertEquals("1|PIX|APROVADO;1|ESTORNO|110.00;1|CANCELADO",loja.consultarHistoricoPagamentos());
            assertEquals("EMITIDA|NF-000001|pedido=1|cpf=52998224725|valor=110.00;CANCELADA|NF-000001|pedido=1|cpf=52998224725|valor=110.00",loja.consultarEventosFiscais());
            assertThrowsExactly(IllegalStateException.class, () -> loja.cancelarPedido(pedido));
            assertEquals(20,loja.consultarEstoque(produto));
        }
        @Test void cancelaBoletoSemNotaOuEstorno() {
            pagar("BOLETO","GERAR",1,"SMS");
            loja.cancelarPedido(pedido);
            assertEquals("1|BOLETO|PENDENTE;1|CANCELADO",loja.consultarHistoricoPagamentos());
            assertEquals("",loja.consultarEventosFiscais());
            assertEquals("SMS|11999999999|pedido=1|AGUARDANDO_PAGAMENTO;SMS|11999999999|pedido=1|CANCELADO",loja.consultarNotificacoes());
            assertThrowsExactly(IllegalStateException.class, () -> loja.confirmarBoleto(pedido));
        }
        @Test void cancelamentoContraOverflowNaoProduzEfeitosParciais() {
            long outro=loja.cadastrarProduto("Outro","ELETRONICO",1,1);
            loja.adicionarItem(pedido,produto,1);
            loja.adicionarItem(pedido,outro,1);
            loja.finalizarPedido(pedido,"PIX","APROVAR",1,"NENHUM");
            loja.adicionarEstoque(outro,Integer.MAX_VALUE);
            String pagamentos=loja.consultarHistoricoPagamentos();
            String fiscal=loja.consultarEventosFiscais();
            assertThrowsExactly(IllegalStateException.class, () -> loja.cancelarPedido(pedido));
            assertEquals("PAGO",loja.consultarStatus(pedido));
            assertEquals(19,loja.consultarEstoque(produto));
            assertEquals(pagamentos,loja.consultarHistoricoPagamentos());
            assertEquals(fiscal,loja.consultarEventosFiscais());
        }
    }

    @Nested
    class EfeitosERelatorios {
        @ParameterizedTest @CsvSource({"EMAIL,EMAIL|ana@exemplo.com|pedido=1|PAGO","SMS,SMS|11999999999|pedido=1|PAGO","NENHUM,''","AMBOS,EMAIL|ana@exemplo.com|pedido=1|PAGO;SMS|11999999999|pedido=1|PAGO"})
        void canais(String canal,String esperado) {
            pagar("PIX","APROVAR",1,canal);
            assertEquals(esperado,loja.consultarNotificacoes());
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"email","OUTRO"})
        void canalInvalido(String canal) {
            loja.adicionarItem(pedido,produto,1);
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.finalizarPedido(pedido,"PIX","APROVAR",1,canal));
            assertEquals("",loja.consultarHistoricoPagamentos());
        }
        @Test void cartaoEstornoParcialERelatorioLiquido() {
            pagar("CARTAO","4111111111111111",2,"AMBOS");
            loja.solicitarEstorno(pedido,10.005);
            assertEquals("pedidos=1|bruto=110.00|estornos=10.01|liquido=99.99",loja.gerarRelatorioVendas());
            assertEquals(19,loja.consultarEstoque(produto));
            assertEquals("EMAIL|ana@exemplo.com|pedido=1|PAGO;SMS|11999999999|pedido=1|PAGO",loja.consultarNotificacoes());
            loja.cancelarPedido(pedido);
            assertEquals("1|CARTAO|APROVADO;1|ESTORNO|10.01;1|ESTORNO|99.99;1|CANCELADO",loja.consultarHistoricoPagamentos());
            assertEquals("pedidos=0|bruto=0.00|estornos=0.00|liquido=0.00",loja.gerarRelatorioVendas());
        }
        @ParameterizedTest @ValueSource(doubles={0,-1,.004,110.01,Double.NaN,Double.POSITIVE_INFINITY})
        void estornoInvalido(double valor) {
            pagar("CARTAO","4111111111111111",1,"NENHUM");
            assertThrowsExactly(IllegalArgumentException.class, () -> loja.solicitarEstorno(pedido,valor));
            assertEquals("pedidos=1|bruto=110.00|estornos=0.00|liquido=110.00",loja.gerarRelatorioVendas());
        }
        @Test void relatorioExcluiAbertosPendentesECancelados() {
            pagar("PIX","APROVAR",1,"NENHUM");
            long aberto=loja.criarPedido(carrinho,"NORMAL","2026-10-24");
            loja.adicionarItem(aberto,produto,1);
            long pendente=loja.criarPedido(carrinho,"NORMAL","2026-10-24");
            loja.adicionarItem(pendente,produto,1);
            loja.finalizarPedido(pendente,"BOLETO","GERAR",1,"NENHUM");
            assertEquals("pedidos=1|bruto=110.00|estornos=0.00|liquido=110.00",loja.gerarRelatorioVendas());
            loja.confirmarBoleto(pendente);
            assertEquals("NF-000002|pedido=3|cpf=52998224725|valor=110.00",loja.consultarNotaFiscal(pendente));
            assertEquals("pedidos=2|bruto=220.00|estornos=0.00|liquido=220.00",loja.gerarRelatorioVendas());
            loja.cancelarPedido(pedido);
            assertEquals("pedidos=1|bruto=110.00|estornos=0.00|liquido=110.00",loja.gerarRelatorioVendas());
        }
        @Test void valoresCongeladosAposFechar() {
            pagar("PIX","APROVAR",1,"NENHUM");
            loja.alterarPreco(produto,999);
            assertEquals(110,loja.calcularTotal(pedido));
            assertEquals(100,loja.calcularSubtotal(pedido));
            assertEquals("PAGO|subtotal=100.00|desconto=0.00|frete=10.00|total=110.00|estornado=0.00",loja.consultarResumo(pedido));
        }
        @Test void estornoExigePago() {
            assertThrowsExactly(IllegalStateException.class, () -> loja.solicitarEstorno(pedido,10));
        }
    }

    @Nested
    class Identificadores {
        @Test void ausentes() {
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarCliente(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.criarCarrinho(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarCarrinho(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.adicionarAoCarrinho(999,produto,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.criarPedido(999,"NORMAL","2026-10-24"));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarEstoque(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.alterarPreco(999,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.desativarProduto(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.adicionarEstoque(999,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.adicionarItem(999,produto,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.adicionarItem(pedido,999,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.removerItem(999,produto));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarItens(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.aplicarCupom(999,"DEZ"));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.calcularSubtotal(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.calcularTotal(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarResumo(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.finalizarPedido(999,"PIX","APROVAR",1,"NENHUM"));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.confirmarBoleto(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.solicitarEstorno(999,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.cancelarPedido(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarStatus(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarResultadoPagamento(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarCodigoBarras(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarNotaFiscal(999));
        }
        @Test void sequenciasIndependentesEInstanciasIsoladas() {
            assertEquals(1,cliente);assertEquals(1,produto);assertEquals(1,carrinho);assertEquals(1,pedido);
            assertEquals(2,loja.criarCarrinho(cliente));
            assertEquals(2,loja.criarPedido(carrinho,"NORMAL","2026-10-24"));
            LojaService outra=new LojaService();
            assertThrowsExactly(NoSuchElementException.class, () -> outra.consultarStatus(pedido));
            assertEquals("",outra.consultarNotificacoes());
            assertEquals("",outra.consultarEventosFiscais());
        }
    }
}
