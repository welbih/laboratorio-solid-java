package br.com.treino.solid.pedido;

import br.com.treino.solid.cliente.Cliente;
import java.time.LocalDate;
import java.util.*;

public class Pedido {
    public long id;
    public Cliente cliente;
    public List<Item> itens = new ArrayList<>();
    public LocalDate data;
    public String modalidadeFrete;
    public String status = "ABERTO";
    public String cupom = "";
    public String notificacao = "NENHUM";
    public String pagamento = "";
    public String resultadoPagamento = "";
    public String nota = "";
    public double subtotal;
    public double desconto;
    public double frete;
    public double total;
    public double estornado;
}
