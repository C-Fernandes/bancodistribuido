package entity;

import repository.DataBaseConnection;
import service.BancoService;
import service.ContaService;

import java.util.ArrayList;
import java.util.List;

public class Banco { // Identificador único do banco
    private String nome; // Nome do banco
    private List<Conta> contas;
    private BancoService bancoService;

    public Banco() {
        this.contas = new ArrayList<>();
        this.dataBaseConnection = new DataBaseConnection();
        this.contaService = new ContaService();
    }

    public Banco(String nome) {
        this.contas = new ArrayList<>();
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
