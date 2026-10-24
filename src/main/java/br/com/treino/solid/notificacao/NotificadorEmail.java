package br.com.treino.solid.notificacao;

import java.util.*;

public class NotificadorEmail implements OperacoesNotificacao {
    private final List<String> envios = new ArrayList<>();

    public void enviarEmail(String destino, String texto) {
        envios.add("EMAIL|" + destino + "|" + texto);
    }

    public void enviarSms(String destino, String texto) {
        throw new UnsupportedOperationException();
    }

    public String consultarEnvios() {
        return String.join(";", envios);
    }
}
