package br.com.treino.solid.estoque;

import java.util.*;

public class EstoqueMemoria {
    private final Map<Long, Integer> saldos = new LinkedHashMap<>();

    public void cadastrar(long produtoId, int quantidade) {
        saldos.put(produtoId, quantidade);
    }

    public int consultar(long produtoId) {
        if (!saldos.containsKey(produtoId)) throw new NoSuchElementException();
        return saldos.get(produtoId);
    }

    public void alterar(long produtoId, int quantidade) {
        if (!saldos.containsKey(produtoId)) throw new NoSuchElementException();
        saldos.put(produtoId, quantidade);
    }
}
