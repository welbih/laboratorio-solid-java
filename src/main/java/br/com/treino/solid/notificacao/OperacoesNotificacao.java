package br.com.treino.solid.notificacao;

public interface OperacoesNotificacao {
    void enviarEmail(String destino, String texto);
    void enviarSms(String destino, String texto);
    String consultarEnvios();
}
