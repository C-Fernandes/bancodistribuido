package br.imd.entity;

public class Banco {
    private String nome;

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

    @Override
    public String toString() {
        return String.format(
                "Nome: %s",
                getNome());
    }

}
