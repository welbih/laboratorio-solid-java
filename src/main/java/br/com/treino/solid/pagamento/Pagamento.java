package br.com.treino.solid.pagamento;

import java.math.*;

public class Pagamento implements OperacoesPagamento {
    protected double saldo;
    protected String situacao = "NOVO";

    public String autorizar(double valor, String credencial, int parcelas) {
        calcularParcela(valor, parcelas);
        saldo = valor;
        situacao = "APROVADO";
        return situacao;
    }

    public double calcularParcela(double valor, int parcelas) {
        if (!Double.isFinite(valor) || valor <= 0 || parcelas < 1 || parcelas > 12) throw new IllegalArgumentException();
        return BigDecimal.valueOf(valor).divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP).doubleValue();
    }

    public void estornar(double valor) {
        if (!Double.isFinite(valor) || valor <= 0 || valor > saldo) throw new IllegalArgumentException();
        if (!situacao.equals("APROVADO")) throw new IllegalStateException();
        saldo = BigDecimal.valueOf(saldo - valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public void cancelar() {
        situacao = "CANCELADO";
    }

    public String gerarCodigoBarras(long pedidoId) {
        throw new UnsupportedOperationException();
    }

    public String consultarCodigoBarras() {
        throw new UnsupportedOperationException();
    }

    public boolean validarChavePix(String chave) {
        throw new UnsupportedOperationException();
    }

    public boolean validarNumeroCartao(String numero) {
        throw new UnsupportedOperationException();
    }
}
