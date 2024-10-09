package entity;

import java.util.ArrayList;
import java.util.List;

public class Banco {
    private int id; // Identificador único do banco
    private String nome; // Nome do banco
    private List<Conta> contas;

    public Banco(String nome) {
        this.contas = new ArrayList<>();
    }

    public Banco() {
        this.contas = new ArrayList<>();
    }

    public void deletarConta(String conta, String agencia) {

    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<Conta> getContas() {
        return contas;
    }

    public void setContas(List<Conta> contas) {
        this.contas = contas;
    }
}
