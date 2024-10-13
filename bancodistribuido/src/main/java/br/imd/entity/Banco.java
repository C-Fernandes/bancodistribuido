package br.imd.entity;

public class Banco { // Identificador único do banco
    private String nome; // Nome do banco

    public Banco(String nome) {
        this.nome = nome;
    }

    public Banco() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

}
