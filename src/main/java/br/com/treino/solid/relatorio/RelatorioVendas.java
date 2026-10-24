package br.com.treino.solid.relatorio;

import br.com.treino.solid.pedido.Pedido;
import java.util.*;

public class RelatorioVendas {
    public String gerar(Collection<Pedido> pedidos) {
        int quantidade = 0;
        double bruto = 0;
        double estornos = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.status.equals("PAGO")) {
                quantidade++;
                bruto += pedido.total;
                estornos += pedido.estornado;
            }
        }
        return String.format(Locale.US, "pedidos=%d|bruto=%.2f|estornos=%.2f|liquido=%.2f", quantidade, bruto, estornos, bruto - estornos);
    }
}
