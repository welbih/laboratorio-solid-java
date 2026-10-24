package br.com.treino.solid.fiscal;

import java.util.*;

public class EmissorNotaFiscal {
    private long sequencia = 1;
    private final List<String> eventos = new ArrayList<>();

    public String emitir(long pedidoId, String cpf, double total) {
        String nota = String.format(Locale.US, "NF-%06d|pedido=%d|cpf=%s|valor=%.2f", sequencia++, pedidoId, cpf, total);
        eventos.add("EMITIDA|" + nota);
        return nota;
    }

    public void cancelar(String nota) {
        eventos.add("CANCELADA|" + nota);
    }

    public String consultarEventos() {
        return String.join(";", eventos);
    }
}
