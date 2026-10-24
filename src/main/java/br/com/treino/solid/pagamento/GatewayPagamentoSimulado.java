package br.com.treino.solid.pagamento;

import java.util.*;

public class GatewayPagamentoSimulado {
    private final Map<Long, Pagamento> pagamentos = new LinkedHashMap<>();
    private final List<String> historico = new ArrayList<>();

    public String processar(long pedidoId, String tipo, double valor, String credencial, int parcelas) {
        Pagamento pagamento = criar(tipo);
        String resultado = pagamento.autorizar(valor, credencial, parcelas);
        if (!resultado.equals("RECUSADO")) {
            if (tipo.equals("BOLETO")) pagamento.gerarCodigoBarras(pedidoId);
            pagamentos.put(pedidoId, pagamento);
        }
        historico.add(pedidoId + "|" + tipo + "|" + resultado);
        return resultado;
    }

    public double consultarParcela(String tipo, double valor, int parcelas) {
        Pagamento pagamento = criar(tipo);
        return pagamento.calcularParcela(valor, parcelas);
    }

    public void confirmarBoleto(long pedidoId) {
        Pagamento pagamento = buscar(pedidoId);
        if (!(pagamento instanceof PagamentoBoleto)) throw new IllegalStateException();
        ((PagamentoBoleto) pagamento).confirmar();
        historico.add(pedidoId + "|BOLETO|CONFIRMADO");
    }

    public void estornar(long pedidoId, double valor) {
        buscar(pedidoId).estornar(valor);
        historico.add(String.format(Locale.US, "%d|ESTORNO|%.2f", pedidoId, valor));
    }

    public void cancelar(long pedidoId) {
        buscar(pedidoId).cancelar();
        historico.add(pedidoId + "|CANCELADO");
    }

    public String consultarCodigoBarras(long pedidoId) {
        return buscar(pedidoId).consultarCodigoBarras();
    }

    public String consultarHistorico() {
        return String.join(";", historico);
    }

    private Pagamento buscar(long id) {
        Pagamento pagamento = pagamentos.get(id);
        if (pagamento == null) throw new NoSuchElementException();
        return pagamento;
    }

    private Pagamento criar(String tipo) {
        if ("PIX".equals(tipo)) return new PagamentoPix();
        else if ("CARTAO".equals(tipo)) return new PagamentoCartao();
        else if ("BOLETO".equals(tipo)) return new PagamentoBoleto();
        else throw new IllegalArgumentException();
    }
}
