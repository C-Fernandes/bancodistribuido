package br.imd.service;

import java.sql.Connection;
import java.sql.SQLException;

import br.imd.entity.Banco;
import br.imd.entity.Conta;
import br.imd.repository.DataBaseConnection;

public class BankManager {

    private DataBaseConnection dataBaseConnection;
    private ContaService contaService;
    private BancoService bancoService;

    public BankManager() {
        this.contaService = new ContaService();
        this.bancoService = new BancoService();
        this.dataBaseConnection = new DataBaseConnection();
    }

    public boolean transferir(String bancoOrigemNome, String agenciaOrigem, String contaNumOrigem,
            String bancoDestinoNome, String agenciaDestino, String contaNumDestino, double valor) {
        Connection conn = null;

        try {
            conn = dataBaseConnection.getConnection();
            conn.setAutoCommit(false); // Desativa o auto-commit para iniciar a transação

            Conta contaOrigem = bancoService.prepararParaSaque(bancoOrigemNome, conn, agenciaOrigem, contaNumOrigem,
                    valor);
            Conta contaDestino = bancoService.prepararParaDeposito(bancoDestinoNome, conn, agenciaDestino,
                    contaNumDestino);

            // Simulação de confirmação de preparação
            System.out.println("Fase 1: Preparado para commit");

            if (contaOrigem != null && contaDestino != null) {
                // 2. Fase de commit (COMMIT)
                try {
                    // Remover saldo da conta de origem
                    contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
                    contaService.atualizarSaldo(conn, contaOrigem);

                    // Adicionar saldo à conta de destino
                    contaDestino.setSaldo(contaDestino.getSaldo() + valor);
                    contaService.atualizarSaldo(conn, contaDestino);

                    // Se tudo ocorrer bem, faz o commit
                    conn.commit();
                    System.out.println("Fase 2: Commit realizado com sucesso");
                    return true; // Commit bem-sucedido

                } catch (Exception e) {
                    System.out.println("Fase 2: Commit falhou, realizando rollback");
                    e.printStackTrace();
                    conn.rollback(); // Rollback em caso de falha no commit
                    return false; // Falha no commit
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Exceção geral
        } finally {
            if (conn != null) {
                try {
                    conn.close();

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }// Método para realizar um depósito

    public void depositar(String bancoNome, String agencia, String contaNum, double valor) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Conta conta = bancoService.prepararParaDeposito(bancoNome, conn, agencia, contaNum);
            conta.setSaldo(conta.getSaldo() + valor); // Adiciona o valor ao saldo
            contaService.atualizarSaldo(conn, conta); // Atualiza a conta no banco
            System.out.println("Depósito realizado com sucesso.");
        }
    }

    // Método para realizar um saque
    public void sacar(String bancoNome, String agencia, String contaNum, double valor) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Conta conta = bancoService.prepararParaSaque(bancoNome, conn, agencia, contaNum, valor);

            conta.setSaldo(conta.getSaldo() - valor);
            contaService.atualizarSaldo(conn, conta); // Atualiza a conta no banco
            System.out.println("Saque realizado com sucesso.");
            conn.close();
        }
    }

    // Método para criar um novo banco
    public boolean criarBanco(Banco banco) throws SQLException {

        bancoService.criarBanco(banco); // Cria o banco
        System.out.println("Banco criado com sucesso.");

        return true;
    }

    // Método para criar uma nova conta
    public void criarConta(String bancoNome, Conta conta) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            bancoService.buscarBanco(bancoNome); // Verifica se o banco existe
            contaService.criarConta(conn, conta); // Cria a conta
            System.out.println("Conta criada com sucesso.");
        }
    }

    public boolean criarConta(String bancoNome, String agencia, String contaNum, double saldo) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Banco banco = bancoService.buscarBanco(bancoNome); // Verifica se o banco existe
            contaService.criarConta(conn, new Conta(contaNum, agencia, saldo, banco)); // Cria a conta
            System.out.println("Conta criada com sucesso.");
            conn.close();
        }
        return true;
    }

    // Método para excluir um banco
    public boolean excluirBanco(String bancoNome) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            bancoService.excluirBanco(bancoNome); // Exclui o banco
            System.out.println("Banco excluído com sucesso.");
        }
        return true;
    }

    // Método para excluir uma conta
    public void excluirConta(String bancoNome, String agencia, String contaNum) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            contaService.excluirConta(conn, bancoNome, agencia, contaNum); // Exclui a conta
            System.out.println("Conta excluída com sucesso.");
        }
    }

}