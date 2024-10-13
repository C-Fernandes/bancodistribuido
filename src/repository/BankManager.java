package repository;

import entity.Banco;
import entity.Conta;
import service.BancoService;
import service.ContaService;

import java.sql.Connection;
import java.sql.SQLException;

public class BankManager {

    private DataBaseConnection dataBaseConnection;
    private ContaService contaService;
    private BancoService bancoService;
    private Banco banco;

    public BankManager() {
        this.contaRepository = new ContaRepository();
    }

    public boolean transferir(String bancoOrigemNome, String agenciaOrigem, String contaNumOrigem, String bancoDestinoNome, String agenciaDestino, String contaNumDestino, double valor) {
        Connection conn = null;

        try {
            conn = dataBaseConnection.getConnection();
            conn.setAutoCommit(false); // Desativa o auto-commit para iniciar a transação

            Conta contaOrigem = bancoService.prepararParaSaque(bancoOrigemNome, conn, agenciaOrigem, agenciaOrigem);
             Conta contaDestino = bancoService.prepararParaDeposito(bancoDestinoNome, conn, agenciaDestino, agenciaDestino);


            // Simulação de confirmação de preparação
            System.out.println("Fase 1: Preparado para commit");

            if (contaOrigem != null && contaDestino !=null) {
                // 2. Fase de commit (COMMIT)
                try {
                    // Remover saldo da conta de origem
                    contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
                    contaService.atualizarSaldo(conn,contaOrigem);

                    // Adicionar saldo à conta de destino
                    contaDestino.setSaldo(contaDestino.getSaldo() + valor);
                    contaService.atualizarSaldo(conn, contaDestino);

                    // Se tudo ocorrer bem, faz o commit
                    conn.commit();
                    System.out.println("Fase 2: Commit realizado com sucesso");
                    return true; // Commit bem-sucedido

                } catch (Exception e) {
                    System.out.println("Fase 2: Commit falhou, realizando rollback");
                    conn.rollback(); // Rollback em caso de falha no commit
                    return false; // Falha no commit
                }
            }
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
            Conta conta = bancoService.prepararParaSaque(bancoNome, conn, agencia, contaNum);
            conta.setSaldo(conta.getSaldo() - valor); // Subtrai o valor do saldo
            contaService.atualizarSaldo(conn, conta); // Atualiza a conta no banco
            System.out.println("Saque realizado com sucesso.");
        }
    }

    // Método para criar um novo banco
    public void criarBanco(Banco banco) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            bancoService.criarBanco(conn, banco); // Cria o banco
            System.out.println("Banco criado com sucesso.");
        }
    }

    // Método para criar uma nova conta
    public void criarConta(String bancoNome, Conta conta) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            bancoService.buscarBanco(bancoNome); // Verifica se o banco existe
            contaService.criarConta(conn, conta); // Cria a conta
            System.out.println("Conta criada com sucesso.");
        }
    }

    // Método para excluir um banco
    public void excluirBanco(String bancoNome) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            bancoService.excluirBanco(conn, bancoNome); // Exclui o banco
            System.out.println("Banco excluído com sucesso.");
        }
    }

    // Método para excluir uma conta
    public void excluirConta(String bancoNome, String agencia, String contaNum) throws SQLException {
        try (Connection conn = dataBaseConnection.getConnection()) {
            contaService.excluirConta(conn, bancoNome, agencia, contaNum); // Exclui a conta
            System.out.println("Conta excluída com sucesso.");
        }
    }

}