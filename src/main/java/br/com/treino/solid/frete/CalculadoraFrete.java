package br.com.treino.solid.frete;

import java.util.*;
import java.math.*;

public class CalculadoraFrete {
    public double calcular(String uf, String modalidade, double subtotal) {
        double base;
        if (Arrays.asList("SP", "RJ", "MG", "ES").contains(uf)) base = 10;
        else if (Arrays.asList("PR", "SC", "RS").contains(uf)) base = 15;
        else if (Arrays.asList("GO", "DF").contains(uf)) base = 20;
        else if (Arrays.asList("BA", "PE").contains(uf)) base = 25;
        else base = 30;
        if (modalidade.equals("RETIRADA")) return 0;
        else if (modalidade.equals("EXPRESSO")) return BigDecimal.valueOf(base * 1.8).setScale(2, RoundingMode.HALF_UP).doubleValue();
        else if (modalidade.equals("NORMAL")) return subtotal >= 300 ? 0 : base;
        else throw new IllegalArgumentException();
    }
}
