package br.com.treino.solid.pedido;

import java.util.*;

public class Carrinho {
    public long id;
    public long clienteId;
    public List<Item> itens = new ArrayList<>();
}
