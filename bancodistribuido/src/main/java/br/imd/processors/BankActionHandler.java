package br.imd.processors;

import java.sql.SQLException;
import java.util.List;

import br.imd.entity.Banco;
import br.imd.entity.Conta;
import br.imd.service.BankManager;

public class BankActionHandler {

    private BankManager bankManager;

    public BankActionHandler() {
        this.bankManager = new BankManager();
    }

    public String handleAction(String action, String[] parts) throws SQLException {

        switch (action.toUpperCase().trim()) {
            case "TRANSFERIR":
                return handleTransferir(parts);
            case "SACAR":
                return handleSacar(parts);
            case "CRIAR_CONTA":
                return handleCriarConta(parts);
            case "CRIAR_BANCO":
                return handleCriarBanco(parts);
            case "DEPOSITAR":
                return handleDepositar(parts);
            case "LISTAR_CONTAS":
                return handleListarContas(parts);
            case "LISTAR_BANCOS":
                return handleListarBancos(parts);
            case "EXCLUIR_CONTA":
                return handleExcluirConta(parts);
            case "EXCLUIR_BANCO":
                return handleExcluirBanco(parts);
            default:
                return "Ação não reconhecida.";
        }
    }

    private String handleTransferir(String[] parts) {
        if (parts.length == 8) {
            String bancoOrigem = parts[1];
            String agenciaOrigem = parts[2];
            String contaOrigem = parts[3];
            String bancoDestino = parts[4];
            String agenciaDestino = parts[5];
            String contaDestino = parts[6];
            double valor = Double.parseDouble(parts[7]);

            return bankManager.transferir(bancoOrigem, agenciaOrigem, contaOrigem,
                    bancoDestino, agenciaDestino, contaDestino, valor);
        } else {
            return "Parâmetros inválidos para transferência.";
        }
    }

    private String handleSacar(String[] parts) throws SQLException {
        if (parts.length == 5) {
            String banco = parts[1];
            String agencia = parts[2];
            String conta = parts[3];
            double valor = Double.parseDouble(parts[4]);

            return bankManager.sacar(banco, agencia, conta, valor);
        } else {
            return "Parâmetros inválidos para saque.";
        }
    }

    private String handleCriarConta(String[] parts) throws SQLException {
        if (parts.length == 5) {
            String banco = parts[1];
            String agencia = parts[2];
            String numeroConta = parts[3];
            double saldoInicial = Double.parseDouble(parts[4]);

            return bankManager.criarConta(banco, agencia, numeroConta, saldoInicial);
        } else {
            return "Parâmetros inválidos para criação de conta.";
        }
    }

    private String handleCriarBanco(String[] parts) throws SQLException {
        if (parts.length == 2) {
            String bancoNome = parts[1];

            return bankManager.criarBanco(new Banco(bancoNome));
        } else {
            return "Parâmetros inválidos para criação de banco.";
        }
    }

    private String handleDepositar(String[] parts) throws SQLException {
        if (parts.length == 5) {
            String banco = parts[1];
            String agencia = parts[2];
            String conta = parts[3];
            double valor = Double.parseDouble(parts[4]);

            return bankManager.depositar(banco, agencia, conta, valor);
        } else {
            return "Parâmetros inválidos para depósito.";
        }
    }

    private String handleListarContas(String[] parts) throws SQLException {
        List<Conta> contas;

        if (parts.length == 2) {
            String banco = parts[1];
            contas = bankManager.listarContas(banco);
        } else {
            contas = bankManager.listarContas();
        }

        StringBuilder resposta = new StringBuilder("Contas: ");
        for (Conta conta : contas) {
            resposta.append(conta.toString()).append(", ");
        }
        return resposta + " OK";
    }

    private String handleListarBancos(String[] parts) throws SQLException {
        List<Banco> bancos = bankManager.listarBancos();
        StringBuilder resposta = new StringBuilder("Bancos: ");
        for (Banco banco : bancos) {
            resposta.append(banco.getNome()).append(", ");
        }
        return resposta + "OK";
    }

    private String handleExcluirConta(String[] parts) throws SQLException {
        if (parts.length == 4) {
            String banco = parts[1];
            String agencia = parts[2];
            String conta = parts[3];

            return bankManager.excluirConta(banco, agencia, conta);
        } else {
            return "Parâmetros inválidos para excluir conta.";
        }
    }

    private String handleExcluirBanco(String[] parts) throws SQLException {
        if (parts.length == 2) {
            String bancoNome = parts[1];

            return bankManager.excluirBanco(bancoNome);
        } else {
            return "Parâmetros inválidos para excluir banco.";
        }
    }
}
