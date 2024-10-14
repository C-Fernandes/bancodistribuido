package br.imd.entity;

public class Conta {
    private String conta, agencia;
    private double saldo;
    private Banco banco;

    public Conta(String conta, String agencia, double saldo, Banco banco) {
        this.conta = conta;
        this.agencia = agencia;
        this.saldo = saldo;
        this.banco = banco;
    }

    public Conta(String conta, String agencia, double saldo) {
        this.conta = conta;
        this.agencia = agencia;
        this.saldo = saldo;

    }

    public Conta(String conta, String agencia) {
        this.conta = conta;
        this.agencia = agencia;
        this.saldo = 0;
    }

    public Conta() {
    }

    public String getConta() {
        return conta;
    }

    public void setConta(String conta) {
        this.conta = conta;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public String getAgencia() {
        return agencia;
    }

    public void setAgencia(String agencia) {
        this.agencia = agencia;
    }

    public Banco getBanco() {
        return banco;
    }

    public void setBanco(Banco banco) {
        this.banco = banco;
    }

    @Override
    public String toString() {
        return String.format(
                "Conta{" +
                        "    Banco: %s ," +
                        "    Agência: %s ," +
                        "    Conta: %s ," +
                        "    Saldo: %.2f " +
                        "}",
                getBanco(), // ou getBanco().getNome() se Banco for um objeto
                getAgencia(),
                getConta(),
                getSaldo());
    }

}
