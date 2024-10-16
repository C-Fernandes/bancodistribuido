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
        Conta[] contas = new Conta[2];

        try {
            conn = dataBaseConnection.getConnection();
            conn.setAutoCommit(false);
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

            Thread threadDeposito = new Thread(() -> {
                try (Connection innerConn = dataBaseConnection.getConnection()) {
                    contas[1] = bancoService.prepararParaDeposito(bancoDestinoNome, innerConn, agenciaDestino,
                            contaNumDestino);
                } catch (SQLException e) {
                    System.err.println("Erro ao preparar depósito: " + e.getMessage());
                }
            });

            threadSaque.start();
            threadDeposito.start();

            threadSaque.join();
            threadDeposito.join();

            if (contas[0] == null || contas[1] == null) {
                return "Não foi possivel seguir com a transação";
            }
            long logId = logWriteAhead(contas[0], contas[1], valor, conn);

            double saldoOrigemAntes = contas[0].getSaldo();
            double saldoDestinoAntes = contas[1].getSaldo();
            contas[0].setSaldo(saldoOrigemAntes - valor);

            contaService.atualizarSaldo(contas[0]);

            contas[1].setSaldo(saldoDestinoAntes + valor);
            contaService.atualizarSaldo(contas[1]);

            updateLogStatus(logId, "COMPLETED", conn);

            conn.commit();
            return "Transferência realizada com sucesso! OK";

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    return "Erro ao reverter a transação: " + ex.getMessage();
                }
            }
            return "Erro na transferência: " + e.getMessage();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    return "Erro ao reverter a transação: " + ex.getMessage();
                }
            }
            return "Erro inesperado na transferência: " + e.getMessage();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    return "Erro ao fechar a conexão: " + e.getMessage();
                }
            }
        }
    }

    private long logWriteAhead(Conta contaOrigem, Conta contaDestino, double valor, Connection conn) {
        long logId = -1;

        String sql = "INSERT INTO wal_logs (operation_type, conta_identificacao, valor, status, saldo_before) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "TRANSFERENCIA");

            String contaIdentificacaoOrigem = contaOrigem.getBanco().getNome() + "," + contaOrigem.getAgencia() + ","
                    + contaOrigem.getConta();
            String contaIdentificacaoDestino = contaDestino.getBanco().getNome() + "," + contaDestino.getAgencia() + ","
                    + contaDestino.getConta();

            pstmt.setString(2, contaIdentificacaoOrigem + " -> " + contaIdentificacaoDestino);
            pstmt.setDouble(3, valor);
            pstmt.setString(4, "PENDING");

            pstmt.setDouble(5, contaOrigem.getSaldo());
            pstmt.executeUpdate();

            try (var generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    logId = generatedKeys.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao gravar log: " + e.getMessage());
        }

        return logId;
    }

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
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Erro inesperado ao listar contas: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Conta> listarContas(String banco) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            return contaService.listarContasPorBanco(conn, banco);
        } catch (SQLException e) {
            System.err.println("Erro ao listar contas do banco " + banco + ": " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Erro inesperado ao listar contas do banco " + banco + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
