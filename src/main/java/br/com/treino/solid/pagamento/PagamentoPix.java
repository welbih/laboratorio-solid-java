package br.com.treino.solid.pagamento;

public class PagamentoPix extends Pagamento {
    @Override
    public double calcularParcela(double valor, int parcelas) {
        if (parcelas != 1) throw new IllegalArgumentException();
        return super.calcularParcela(valor, parcelas);
    }

    @Override
    public String autorizar(double valor, String credencial, int parcelas) {
        calcularParcela(valor, parcelas);
        if (!validarChavePix(credencial)) throw new IllegalArgumentException();
        if (credencial.equals("RECUSAR")) return "RECUSADO";
        return super.autorizar(valor, credencial, parcelas);
    }

    @Override
    public void estornar(double valor) {
        if (valor > 0 && valor < saldo) throw new UnsupportedOperationException();
        super.estornar(valor);
    }

    @Override
    public boolean validarChavePix(String chave) {
        return "APROVAR".equals(chave) || "RECUSAR".equals(chave);
    }
}
