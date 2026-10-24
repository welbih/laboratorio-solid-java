package br.com.treino.solid.pagamento;

public class PagamentoCartao extends Pagamento {
    @Override
    public String autorizar(double valor, String credencial, int parcelas) {
        calcularParcela(valor, parcelas);
        if (!validarNumeroCartao(credencial)) throw new IllegalArgumentException();
        if (credencial.equals("4000000000000002")) return "RECUSADO";
        return super.autorizar(valor, credencial, parcelas);
    }

    @Override
    public boolean validarNumeroCartao(String numero) {
        return "4111111111111111".equals(numero) || "4000000000000002".equals(numero);
    }
}
