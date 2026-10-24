package br.com.treino.solid.notificacao;

public class DespachanteNotificacao {
    public void email(OperacoesNotificacao operacoes, String destino, String texto) {
        operacoes.enviarEmail(destino, texto);
    }

    public void sms(OperacoesNotificacao operacoes, String destino, String texto) {
        operacoes.enviarSms(destino, texto);
    }
}
