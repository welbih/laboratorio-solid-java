package br.com.treino.solid.notificacao;

import java.util.*;

public class NotificadorSms implements OperacoesNotificacao {
    private final List<String> envios = new ArrayList<>();

    public void enviarSms(String destino, String texto) {
        envios.add("SMS|" + destino + "|" + texto);
    }

    public void enviarEmail(String destino, String texto) {
        throw new UnsupportedOperationException();
    }

    public String consultarEnvios() {
        return String.join(";", envios);
    }
}
