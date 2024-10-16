package br.imd.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    public String transferir(String bancoOrigemNome, String agenciaOrigem, String contaNumOrigem,
            String bancoDestinoNome, String agenciaDestino, String contaNumDestino, double valor) {
        Connection conn = null;
        Conta[] contas = new Conta[2]; // Array para armazenar as contas

        try {
            conn = dataBaseConnection.getConnection();
            conn.setAutoCommit(false); // Desativa o auto-commit para iniciar a transação

            // Criação da thread para preparar o saque
            Thread threadSaque = new Thread(() -> {
                try (Connection innerConn = dataBaseConnection.getConnection()) {
                    try {
                        contas[0] = bancoService.prepararParaSaque(bancoOrigemNome, innerConn, agenciaOrigem,
                                contaNumOrigem, valor);
                    } catch (Exception e) {

                    }

                    System.out.println("retorno thread saque:" + contas[0]);
                } catch (SQLException e) {
                    System.err.println("Erro ao preparar saque: " + e.getMessage());
                }
            });

            // Criação da thread para preparar o depósito
            Thread threadDeposito = new Thread(() -> {
                try (Connection innerConn = dataBaseConnection.getConnection()) {
                    contas[1] = bancoService.prepararParaDeposito(bancoDestinoNome, innerConn, agenciaDestino,
                            contaNumDestino);
                } catch (SQLException e) {
                    System.err.println("Erro ao preparar depósito: " + e.getMessage());
                }
            });

            // Inicia as threads
            threadSaque.start();
            threadDeposito.start();

            threadSaque.join();
            threadDeposito.join();

            if (contas[0] == null || contas[1] == null) {
                return "Não foi possivel seguir com a transação";
            }
            long logId = logWriteAhead(contas[0], contas[1], valor, conn); // Grava os logs de desfazer e refazer

            double saldoOrigemAntes = contas[0].getSaldo();
            double saldoDestinoAntes = contas[1].getSaldo();
            contas[0].setSaldo(saldoOrigemAntes - valor);

            contaService.atualizarSaldo(contas[0]);

            contas[1].setSaldo(saldoDestinoAntes + valor);
            contaService.atualizarSaldo(contas[1]);

            // Atualiza o log para refletir que a operação foi realizada
            updateLogStatus(logId, "COMPLETED", conn); // Atualiza o log para status COMPLETED

            // Se tudo ocorrer bem, realiza o commit
            conn.commit();
            return "Transferência realizada com sucesso! OK";

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Desfaz em caso de erro
                } catch (SQLException ex) {
                    return "Erro ao reverter a transação: " + ex.getMessage();
                }
            }
            return "Erro na transferência: " + e.getMessage();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Desfaz em caso de erro inesperado
                } catch (SQLException ex) {
                    return "Erro ao reverter a transação: " + ex.getMessage();
                }
            }
            return "Erro inesperado na transferência: " + e.getMessage();
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Fecha a conexão
                } catch (SQLException e) {
                    return "Erro ao fechar a conexão: " + e.getMessage();
                }
            }
        }
    }

    // Método para gravar logs de Write-Ahead Logging
    private long logWriteAhead(Conta contaOrigem, Conta contaDestino, double valor, Connection conn) {
        long logId = -1; // ID do log que será retornado

        // SQL para inserir o log na tabela wal_logs
        String sql = "INSERT INTO wal_logs (operation_type, conta_identificacao, valor, status, saldo_before) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "TRANSFERENCIA");

            // Identificação da conta como combinação de banco, agência e conta
            String contaIdentificacaoOrigem = contaOrigem.getBanco().getNome() + "," + contaOrigem.getAgencia() + ","
                    + contaOrigem.getConta();
            String contaIdentificacaoDestino = contaDestino.getBanco().getNome() + "," + contaDestino.getAgencia() + ","
                    + contaDestino.getConta();

            pstmt.setString(2, contaIdentificacaoOrigem + " -> " + contaIdentificacaoDestino);
            pstmt.setDouble(3, valor);
            pstmt.setString(4, "PENDING"); // Status inicial como PENDING

            // Saldo antes da operação
            pstmt.setDouble(5, contaOrigem.getSaldo());
            pstmt.executeUpdate();

            // Recupera o ID gerado
            try (var generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    logId = generatedKeys.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao gravar log: " + e.getMessage());
        }

        return logId; // Retorna o ID do log
    }

    // Método para atualizar o status do log
    private void updateLogStatus(long logId, String status, Connection conn) {
        String sql = "UPDATE wal_logs SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setLong(2, logId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar o status do log: " + e.getMessage());
        }
    }

    // Método para realizar um depósito
    public String depositar(String bancoNome, String agencia, String contaNum, double valor) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Conta conta = bancoService.prepararParaDeposito(bancoNome, conn, agencia, contaNum);
            if (conta != null) {
                conta.setSaldo(conta.getSaldo() + valor);
                contaService.atualizarSaldo(conta);
                return "Depósito realizado com sucesso! OK";
            }
            return "Conta não encontrada para depósito.";
        } catch (SQLException e) {
            return "Erro no depósito: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado no depósito: " + e.getMessage();
        }
    }

    // Método para realizar um saque
    public String sacar(String bancoNome, String agencia, String contaNum, double valor) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Conta conta = bancoService.prepararParaSaque(bancoNome, conn, agencia, contaNum, valor);
            if (conta != null) {
                conta.setSaldo(conta.getSaldo() - valor);

                System.out.println("novo saldo: " + conta.getSaldo());
                contaService.atualizarSaldo(conta);
                conn.close();
                return "Saque realizado com sucesso! OK";

            }
            return "Conta não encontrada para saque.";
        } catch (SQLException e) {
            return "Erro no saque: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado no saque: " + e.getMessage();
        }

    }

    // Método para criar um novo banco
    public String criarBanco(Banco banco) {
        try {
            bancoService.criarBanco(banco);
            return "Banco criado com sucesso! OK";
        } catch (SQLException e) {
            return "Erro ao criar banco: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado ao criar banco: " + e.getMessage();
        }
    }

    // Método para criar uma nova conta
    public String criarConta(String bancoNome, Conta conta) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Banco banco = bancoService.buscarBanco(bancoNome);
            if (banco != null) {
                contaService.criarConta(conn, conta);
                return "Conta criada com sucesso! OK";
            }
            return "Banco não encontrado para criação da conta.";
        } catch (SQLException e) {
            return "Erro ao criar conta: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado ao criar conta: " + e.getMessage();
        }
    }

    // Sobrecarga do método para criar uma conta com atributos simples
    public String criarConta(String bancoNome, String agencia, String contaNum, double saldo) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Banco banco = bancoService.buscarBanco(bancoNome);
            if (banco != null) {
                contaService.criarConta(conn, new Conta(contaNum, agencia, saldo, banco));
                return "Conta criada com sucesso! OK";
            }
            return "Banco não encontrado para criação da conta.";
        } catch (SQLException e) {
            return "Erro ao criar conta: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado ao criar conta: " + e.getMessage();
        }
    }

    // Método para excluir um banco
    public String excluirBanco(String bancoNome) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            bancoService.excluirBanco(bancoNome);
            return "Banco excluído com sucesso! OK";
        } catch (SQLException e) {
            return "Erro ao excluir banco: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado ao excluir banco: " + e.getMessage();
        }
    }

    // Método para excluir uma conta
    public String excluirConta(String bancoNome, String agencia, String contaNum) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            contaService.excluirConta(conn, bancoNome, agencia, contaNum);
            return "Conta excluída com sucesso! OK";
        } catch (SQLException e) {
            return "Erro ao excluir conta: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado ao excluir conta: " + e.getMessage();
        }
    }

    public List<Banco> listarBancos() {
        try (Connection conn = dataBaseConnection.getConnection()) {
            return bancoService.listarBancos(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar bancos: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Erro inesperado ao listar bancos: " + e.getMessage());
        }
    }

    public List<Conta> listarContas() {
        try {
            return contaService.listarContas();
        } catch (SQLException e) {
            System.err.println("Erro ao listar contas: " + e.getMessage());
            return new ArrayList<>(); // Retorna uma lista vazia em caso de erro
        } catch (Exception e) {
            System.err.println("Erro inesperado ao listar contas: " + e.getMessage());
            return new ArrayList<>(); // Retorna uma lista vazia em caso de erro
        }
    }

    public List<Conta> listarContas(String banco) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            return contaService.listarContasPorBanco(conn, banco);
        } catch (SQLException e) {
            System.err.println("Erro ao listar contas do banco " + banco + ": " + e.getMessage());
            return new ArrayList<>(); // Retorna uma lista vazia em caso de erro
        } catch (Exception e) {
            System.err.println("Erro inesperado ao listar contas do banco " + banco + ": " + e.getMessage());
            return new ArrayList<>(); // Retorna uma lista vazia em caso de erro
        }
    }
}
