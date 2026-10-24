package br.com.treino.solid.fachada;

import br.com.treino.solid.cliente.*;
import br.com.treino.solid.produto.Produto;
import br.com.treino.solid.pedido.*;
import br.com.treino.solid.estoque.EstoqueMemoria;
import br.com.treino.solid.desconto.Cupom;
import br.com.treino.solid.pagamento.GatewayPagamentoSimulado;
import br.com.treino.solid.frete.CalculadoraFrete;
import br.com.treino.solid.notificacao.*;
import br.com.treino.solid.fiscal.EmissorNotaFiscal;
import br.com.treino.solid.relatorio.RelatorioVendas;
import java.math.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class LojaService {
    private final Map<Long, Cliente> clientes = new LinkedHashMap<>();
    private final Map<Long, Produto> produtos = new LinkedHashMap<>();
    private final Map<Long, Carrinho> carrinhos = new LinkedHashMap<>();
    private final RepositorioPedidoMemoria repositorio = new RepositorioPedidoMemoria();
    private final EstoqueMemoria estoque = new EstoqueMemoria();
    private final GatewayPagamentoSimulado pagamentos = new GatewayPagamentoSimulado();
    private final CalculadoraFrete calculadoraFrete = new CalculadoraFrete();
    private final NotificadorEmail email = new NotificadorEmail();
    private final NotificadorSms sms = new NotificadorSms();
    private final DespachanteNotificacao despachante = new DespachanteNotificacao();
    private final EmissorNotaFiscal emissor = new EmissorNotaFiscal();
    private final RelatorioVendas relatorio = new RelatorioVendas();
    private long sequenciaCliente = 1;
    private long sequenciaProduto = 1;
    private long sequenciaCarrinho = 1;
    private long sequenciaPedido = 1;

    public long cadastrarCliente(String nome, String enderecoEmail, String telefone, String cpf,
                                  boolean vip, String cep, String uf) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException();
        }
        if (enderecoEmail == null || !enderecoEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException();
        }
        if (telefone == null || !telefone.matches("[0-9]{11}")) {
            throw new IllegalArgumentException();
        }
        if (!validarCpf(cpf)) {
            throw new IllegalArgumentException();
        }
        if (cep == null || !cep.matches("[0-9]{8}")) {
            throw new IllegalArgumentException();
        }
        if (!Arrays.asList("SP", "RJ", "MG", "ES", "PR", "SC", "RS", "GO", "DF", "BA", "PE", "AM", "PA").contains(uf)) {
            throw new IllegalArgumentException();
        }
        for (Cliente existente : clientes.values()) {
            if (existente.cpf.equals(cpf)) {
                throw new IllegalArgumentException();
            }
        }
        Endereco endereco = new Endereco();
        endereco.cep = cep;
        endereco.uf = uf;
        Cliente cliente = new Cliente();
        cliente.id = sequenciaCliente++;
        cliente.nome = nome.trim();
        cliente.email = enderecoEmail;
        cliente.telefone = telefone;
        cliente.cpf = cpf;
        cliente.vip = vip;
        cliente.endereco = endereco;
        clientes.put(cliente.id, cliente);
        return cliente.id;
    }

    public String consultarCliente(long clienteId) {
        Cliente cliente = cliente(clienteId);
        return cliente.nome + "|" + cliente.email + "|" + cliente.telefone + "|" + cliente.cpf + "|" + cliente.endereco.cep + "|" + cliente.endereco.uf;
    }

    public long cadastrarProduto(String nome, String categoria, double preco, int quantidade) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException();
        }
        if (!Arrays.asList("LIVRO", "ALIMENTO", "ELETRONICO").contains(categoria)) {
            throw new IllegalArgumentException();
        }
        if (!Double.isFinite(preco) || preco <= 0 || preco > 1000000 || quantidade < 0) {
            throw new IllegalArgumentException();
        }
        Produto produto = new Produto();
        produto.id = sequenciaProduto++;
        produto.nome = nome.trim();
        produto.categoria = categoria;
        produto.preco = arredondar(preco);
        produtos.put(produto.id, produto);
        estoque.cadastrar(produto.id, quantidade);
        return produto.id;
    }

    public void alterarPreco(long produtoId, double preco) {
        Produto produto = produto(produtoId);
        if (!Double.isFinite(preco) || preco <= 0 || preco > 1000000) {
            throw new IllegalArgumentException();
        }
        produto.preco = arredondar(preco);
    }

    public void desativarProduto(long produtoId) {
        produto(produtoId).ativo = false;
    }

    public void adicionarEstoque(long produtoId, int quantidade) {
        produto(produtoId);
        int atual = estoque.consultar(produtoId);
        if (quantidade <= 0 || (long) atual + quantidade > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        estoque.alterar(produtoId, atual + quantidade);
    }

    public int consultarEstoque(long produtoId) {
        return estoque.consultar(produtoId);
    }

    public long criarCarrinho(long clienteId) {
        cliente(clienteId);
        Carrinho carrinho = new Carrinho();
        carrinho.id = sequenciaCarrinho++;
        carrinho.clienteId = clienteId;
        carrinhos.put(carrinho.id, carrinho);
        return carrinho.id;
    }

    public void adicionarAoCarrinho(long carrinhoId, long produtoId, int quantidade) {
        Carrinho carrinho = carrinho(carrinhoId);
        adicionar(carrinho.itens, produtoId, quantidade);
    }

    public String consultarCarrinho(long carrinhoId) {
        return formatarItens(carrinho(carrinhoId).itens);
    }

    public long criarPedido(long carrinhoId, String modalidadeFrete, String dataCompra) {
        Carrinho carrinho = carrinho(carrinhoId);
        if (!Arrays.asList("NORMAL", "EXPRESSO", "RETIRADA").contains(modalidadeFrete)) {
            throw new IllegalArgumentException();
        }
        LocalDate data;
        try {
            if (dataCompra == null) throw new IllegalArgumentException();
            data = LocalDate.parse(dataCompra);
        } catch (DateTimeParseException erro) {
            throw new IllegalArgumentException();
        }
        Pedido pedido = new Pedido();
        pedido.id = sequenciaPedido++;
        pedido.cliente = cliente(carrinho.clienteId);
        pedido.modalidadeFrete = modalidadeFrete;
        pedido.data = data;
        for (Item item : carrinho.itens) {
            Item copia = new Item();
            copia.produtoId = item.produtoId;
            copia.nome = item.nome;
            copia.categoria = item.categoria;
            copia.quantidade = item.quantidade;
            copia.preco = item.preco;
            pedido.itens.add(copia);
        }
        repositorio.salvar(pedido);
        return pedido.id;
    }

    public void adicionarItem(long pedidoId, long produtoId, int quantidade) {
        Pedido pedido = repositorio.buscar(pedidoId);
        exigirAberto(pedido);
        adicionar(pedido.itens, produtoId, quantidade);
    }

    public void removerItem(long pedidoId, long produtoId) {
        Pedido pedido = repositorio.buscar(pedidoId);
        exigirAberto(pedido);
        if (!pedido.itens.removeIf(item -> item.produtoId == produtoId)) {
            throw new NoSuchElementException();
        }
    }

    public String consultarItens(long pedidoId) {
        return formatarItens(repositorio.buscar(pedidoId).itens);
    }

    public void aplicarCupom(long pedidoId, String codigo) {
        Pedido pedido = repositorio.buscar(pedidoId);
        exigirAberto(pedido);
        if (!"".equals(codigo)) {
            validarCupom(pedido, carregarCupom(codigo));
        }
        pedido.cupom = codigo;
    }

    public double calcularSubtotal(long pedidoId) {
        return subtotal(repositorio.buscar(pedidoId));
    }

    public double calcularTotal(long pedidoId) {
        Pedido pedido = repositorio.buscar(pedidoId);
        if (pedido.status.equals("ABERTO")) {
            recalcular(pedido);
        }
        return pedido.total;
    }

    public String consultarResumo(long pedidoId) {
        Pedido pedido = repositorio.buscar(pedidoId);
        calcularTotal(pedidoId);
        return String.format(Locale.US, "%s|subtotal=%.2f|desconto=%.2f|frete=%.2f|total=%.2f|estornado=%.2f",
                pedido.status, subtotal(pedido), pedido.desconto, pedido.frete, pedido.total, pedido.estornado);
    }

    public double consultarParcela(String tipoPagamento, double valor, int parcelas) {
        return pagamentos.consultarParcela(tipoPagamento, valor, parcelas);
    }

    public String finalizarPedido(long pedidoId, String tipoPagamento, String credencial, int parcelas, String tipoNotificacao) {
        Pedido pedido = repositorio.buscar(pedidoId);
        exigirAberto(pedido);
        if (pedido.itens.isEmpty()) {
            throw new IllegalStateException();
        }
        if (!Arrays.asList("EMAIL", "SMS", "AMBOS", "NENHUM").contains(tipoNotificacao)) {
            throw new IllegalArgumentException();
        }
        for (Item item : pedido.itens) {
            Produto produto = produto(item.produtoId);
            if (!produto.ativo || estoque.consultar(item.produtoId) < item.quantidade) {
                throw new IllegalStateException();
            }
        }
        recalcular(pedido);
        String resultado = pagamentos.processar(pedidoId, tipoPagamento, pedido.total, credencial, parcelas);
        pedido.resultadoPagamento = resultado;
        if (resultado.equals("RECUSADO")) {
            return resultado;
        }
        for (Item item : pedido.itens) {
            estoque.alterar(item.produtoId, estoque.consultar(item.produtoId) - item.quantidade);
        }
        pedido.pagamento = tipoPagamento;
        pedido.notificacao = tipoNotificacao;
        if (resultado.equals("PENDENTE")) {
            pedido.status = "AGUARDANDO_PAGAMENTO";
        } else if (resultado.equals("APROVADO")) {
            pedido.status = "PAGO";
            pedido.nota = emissor.emitir(pedido.id, pedido.cliente.cpf, pedido.total);
        } else {
            throw new IllegalStateException();
        }
        notificar(pedido);
        return resultado;
    }

    public void confirmarBoleto(long pedidoId) {
        Pedido pedido = repositorio.buscar(pedidoId);
        if (!pedido.status.equals("AGUARDANDO_PAGAMENTO") || !pedido.pagamento.equals("BOLETO")) {
            throw new IllegalStateException();
        }
        pagamentos.confirmarBoleto(pedidoId);
        pedido.status = "PAGO";
        pedido.resultadoPagamento = "APROVADO";
        pedido.nota = emissor.emitir(pedido.id, pedido.cliente.cpf, pedido.total);
        notificar(pedido);
    }

    public void solicitarEstorno(long pedidoId, double valor) {
        Pedido pedido = repositorio.buscar(pedidoId);
        if (!pedido.status.equals("PAGO")) {
            throw new IllegalStateException();
        }
        if (!Double.isFinite(valor) || valor <= 0) {
            throw new IllegalArgumentException();
        }
        double arredondado = arredondar(valor);
        if (arredondado <= 0 || arredondado > arredondar(pedido.total - pedido.estornado)) {
            throw new IllegalArgumentException();
        }
        pagamentos.estornar(pedido.id, arredondado);
        pedido.estornado = arredondar(pedido.estornado + arredondado);
    }

    public void cancelarPedido(long pedidoId) {
        Pedido pedido = repositorio.buscar(pedidoId);
        if (pedido.status.equals("CANCELADO")) {
            throw new IllegalStateException();
        }
        if (!pedido.status.equals("ABERTO")) {
            for (Item item : pedido.itens) {
                if ((long) estoque.consultar(item.produtoId) + item.quantidade > Integer.MAX_VALUE) {
                    throw new IllegalStateException();
                }
            }
            if (pedido.status.equals("PAGO")) {
                double restante = arredondar(pedido.total - pedido.estornado);
                if (restante > 0) {
                    pagamentos.estornar(pedido.id, restante);
                }
                pedido.estornado = pedido.total;
                emissor.cancelar(pedido.nota);
            }
            pagamentos.cancelar(pedidoId);
            for (Item item : pedido.itens) {
                estoque.alterar(item.produtoId, estoque.consultar(item.produtoId) + item.quantidade);
            }
        }
        pedido.status = "CANCELADO";
        notificar(pedido);
    }

    public String consultarStatus(long pedidoId) {
        return repositorio.buscar(pedidoId).status;
    }

    public String consultarResultadoPagamento(long pedidoId) {
        return repositorio.buscar(pedidoId).resultadoPagamento;
    }

    public String consultarCodigoBarras(long pedidoId) {
        repositorio.buscar(pedidoId);
        return pagamentos.consultarCodigoBarras(pedidoId);
    }

    public String consultarNotaFiscal(long pedidoId) {
        return repositorio.buscar(pedidoId).nota;
    }

    public String consultarEventosFiscais() {
        return emissor.consultarEventos();
    }

    public String consultarNotificacoes() {
        String mensagensEmail = email.consultarEnvios();
        String mensagensSms = sms.consultarEnvios();
        if (mensagensEmail.isEmpty()) return mensagensSms;
        if (mensagensSms.isEmpty()) return mensagensEmail;
        return mensagensEmail + ";" + mensagensSms;
    }

    public String consultarHistoricoPagamentos() {
        return pagamentos.consultarHistorico();
    }

    public String gerarRelatorioVendas() {
        return relatorio.gerar(repositorio.listar());
    }

    private void adicionar(List<Item> itens, long produtoId, int quantidade) {
        Produto produto = produto(produtoId);
        if (quantidade <= 0 || quantidade > 100) {
            throw new IllegalArgumentException();
        }
        if (!produto.ativo) {
            throw new IllegalStateException();
        }
        for (Item item : itens) {
            if (item.produtoId == produtoId) {
                if (item.quantidade + quantidade > 100) {
                    throw new IllegalArgumentException();
                }
                item.quantidade += quantidade;
                return;
            }
        }
        Item item = new Item();
        item.produtoId = produto.id;
        item.nome = produto.nome;
        item.categoria = produto.categoria;
        item.quantidade = quantidade;
        item.preco = produto.preco;
        itens.add(item);
    }

    private String formatarItens(List<Item> itens) {
        List<String> linhas = new ArrayList<>();
        for (Item item : itens) {
            linhas.add(String.format(Locale.US, "%d|%s|%s|%d|%.2f", item.produtoId, item.nome, item.categoria, item.quantidade, item.preco));
        }
        return String.join(";", linhas);
    }

    private double subtotal(Pedido pedido) {
        double valor = 0;
        for (Item item : pedido.itens) {
            valor += item.quantidade * item.preco;
        }
        return arredondar(valor);
    }

    private void recalcular(Pedido pedido) {
        double subtotal = subtotal(pedido);
        double desconto = 0;
        for (Item item : pedido.itens) {
            double valor = item.preco * item.quantidade;
            if (item.categoria.equals("LIVRO")) {
                desconto += arredondar(valor * 0.05);
            } else if (item.categoria.equals("ALIMENTO")) {
                if (item.quantidade >= 10) {
                    desconto += arredondar(valor * 0.03);
                }
            } else if (item.categoria.equals("ELETRONICO")) {
                desconto += 0;
            } else {
                throw new IllegalArgumentException();
            }
        }
        desconto = arredondar(desconto);
        if (pedido.cliente.vip) {
            desconto += arredondar((subtotal - desconto) * 0.05);
        }
        double frete = calculadoraFrete.calcular(pedido.cliente.endereco.uf, pedido.modalidadeFrete, subtotal);
        if (!pedido.cupom.isEmpty()) {
            Cupom cupom = carregarCupom(pedido.cupom);
            validarCupom(pedido, cupom);
            if (cupom.tipo.equals("PERCENTUAL")) {
                desconto += arredondar((subtotal - desconto) * cupom.valor);
            } else if (cupom.tipo.equals("FIXO")) {
                desconto += cupom.valor;
            } else if (cupom.tipo.equals("FRETE")) {
                frete = 0;
            } else {
                throw new IllegalArgumentException();
            }
        }
        pedido.subtotal = subtotal;
        pedido.desconto = arredondar(desconto);
        pedido.frete = frete;
        pedido.total = arredondar(Math.max(0, subtotal - desconto) + frete);
    }

    private Cupom carregarCupom(String codigo) {
        Cupom cupom = new Cupom();
        cupom.codigo = codigo;
        cupom.validade = LocalDate.of(2030, 12, 31);
        cupom.categoria = "TODAS";
        if ("DEZ".equals(codigo)) {
            cupom.tipo = "PERCENTUAL";
            cupom.valor = 0.10;
            cupom.minimo = 100;
        } else if ("VINTE".equals(codigo)) {
            cupom.tipo = "FIXO";
            cupom.valor = 20;
            cupom.minimo = 200;
        } else if ("LIVRO15".equals(codigo)) {
            cupom.tipo = "PERCENTUAL";
            cupom.valor = 0.15;
            cupom.minimo = 50;
            cupom.categoria = "LIVRO";
        } else if ("FRETEGRATIS".equals(codigo)) {
            cupom.tipo = "FRETE";
            cupom.minimo = 0;
        } else if ("VENCIDO".equals(codigo)) {
            cupom.tipo = "FIXO";
            cupom.valor = 5;
            cupom.validade = LocalDate.of(2024, 12, 31);
        } else {
            throw new IllegalArgumentException();
        }
        return cupom;
    }

    private void validarCupom(Pedido pedido, Cupom cupom) {
        if (pedido.data.isAfter(cupom.validade) || subtotal(pedido) < cupom.minimo) {
            throw new IllegalArgumentException();
        }
        if (cupom.tipo.equals("FRETE") && !pedido.modalidadeFrete.equals("NORMAL")) {
            throw new IllegalArgumentException();
        }
        if (!cupom.categoria.equals("TODAS")) {
            for (Item item : pedido.itens) {
                if (!item.categoria.equals(cupom.categoria)) {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    private void notificar(Pedido pedido) {
        String texto = "pedido=" + pedido.id + "|" + pedido.status;
        if (pedido.notificacao.equals("EMAIL")) {
            despachante.email(email, pedido.cliente.email, texto);
        } else if (pedido.notificacao.equals("SMS")) {
            despachante.sms(sms, pedido.cliente.telefone, texto);
        } else if (pedido.notificacao.equals("AMBOS")) {
            despachante.email(email, pedido.cliente.email, texto);
            despachante.sms(sms, pedido.cliente.telefone, texto);
        } else if (pedido.notificacao.equals("NENHUM")) {
            return;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private Cliente cliente(long id) {
        Cliente cliente = clientes.get(id);
        if (cliente == null) throw new NoSuchElementException();
        return cliente;
    }

    private Produto produto(long id) {
        Produto produto = produtos.get(id);
        if (produto == null) throw new NoSuchElementException();
        return produto;
    }

    private Carrinho carrinho(long id) {
        Carrinho carrinho = carrinhos.get(id);
        if (carrinho == null) throw new NoSuchElementException();
        return carrinho;
    }

    private void exigirAberto(Pedido pedido) {
        if (!pedido.status.equals("ABERTO")) throw new IllegalStateException();
    }

    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private boolean validarCpf(String cpf) {
        if (cpf == null || !cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1) return false;
        for (int posicao = 9; posicao < 11; posicao++) {
            int soma = 0;
            for (int indice = 0; indice < posicao; indice++) {
                soma += (cpf.charAt(indice) - '0') * (posicao + 1 - indice);
            }
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != cpf.charAt(posicao) - '0') return false;
        }
        return true;
    }
}
