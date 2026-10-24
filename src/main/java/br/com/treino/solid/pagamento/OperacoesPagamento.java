package br.com.treino.solid.pagamento;

public interface OperacoesPagamento {
    String autorizar(double valor, String credencial, int parcelas);
    double calcularParcela(double valor, int parcelas);
    void estornar(double valor);
    void cancelar();
    String gerarCodigoBarras(long pedidoId);
    String consultarCodigoBarras();
    boolean validarChavePix(String chave);
    boolean validarNumeroCartao(String numero);
}
