package br.imd.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.imd.entity.Banco;
import br.imd.entity.Conta;
import br.imd.repository.DataBaseConnection;

public class BankManager {
    private ContaService contaService;
    private BancoService bancoService;
    private DataBaseConnection dataBaseConnection;

    public BankManager() {
        this.contaService = new ContaService();
        this.bancoService = new BancoService();
        this.dataBaseConnection = new DataBaseConnection();
    }

    public String sacar(String bancoNome, String agencia, String contaNum, double valor) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Conta conta = bancoService.prepararParaSaque(bancoNome, conn, agencia, contaNum, valor);
            if (conta != null) {
                conta.setSaldo(conta.getSaldo() - valor);

                System.out.println("novo saldo: " + conta.getSaldo());
                contaService.atualizarSaldo(conn, conta);
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

    public String depositar(String bancoNome, String agencia, String contaNum, double valor) {
        try (Connection conn = dataBaseConnection.getConnection()) {
            Conta conta = bancoService.prepararParaDeposito(bancoNome, conn, agencia, contaNum);
            if (conta != null) {
                conta.setSaldo(conta.getSaldo() + valor);
                contaService.atualizarSaldo(conn, conta);
                return "Depósito realizado com sucesso! OK";
            }
            return "Conta não encontrada para depósito.";
        } catch (SQLException e) {
            return "Erro no depósito: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado no depósito: " + e.getMessage();
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
