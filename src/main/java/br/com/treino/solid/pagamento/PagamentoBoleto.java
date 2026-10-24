package br.com.treino.solid.pagamento;

public class PagamentoBoleto extends Pagamento {
    private String codigo = "";

    @Override
    public String autorizar(double valor, String credencial, int parcelas) {
        calcularParcela(valor, parcelas);
        if (!"GERAR".equals(credencial) || parcelas != 1) throw new IllegalArgumentException();
        saldo = valor;
        situacao = "PENDENTE";
        return situacao;
    }

    @Override
    public String gerarCodigoBarras(long pedidoId) {
        codigo = "BOLETO-" + pedidoId;
        return codigo;
    }

    @Override
    public String consultarCodigoBarras() {
        return codigo;
    }

    public void confirmar() {
        if (!situacao.equals("PENDENTE")) throw new IllegalStateException();
        situacao = "APROVADO";
    }
}
