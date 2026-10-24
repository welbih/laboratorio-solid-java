package br.com.treino.solid.pedido;

import java.util.*;

public class RepositorioPedidoMemoria {
    private final Map<Long, Pedido> pedidos = new LinkedHashMap<>();

    public void salvar(Pedido pedido) {
        pedidos.put(pedido.id, pedido);
    }

    public Pedido buscar(long id) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) throw new NoSuchElementException();
        return pedido;
    }

    public Collection<Pedido> listar() {
        return pedidos.values();
    }
}
