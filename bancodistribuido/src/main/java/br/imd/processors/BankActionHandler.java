package br.imd.processors;

import java.sql.SQLException;

import br.imd.entity.Banco;
import br.imd.service.BankManager;

public class BankActionHandler {

    private BankManager bankManager;

    public BankActionHandler(BankManager bankManager) {
        this.bankManager = bankManager;
    }

    public String handleAction(String action, String[] parts) throws SQLException {
        switch (action.toUpperCase()) {
            case "TRANSFERIR":
                return handleTransferir(parts);
            case "SACAR":
                return handleSacar(parts);
            case "CRIAR_CONTA":
                return handleCriarConta(parts);
            case "CRIAR_BANCO":
                return handleCriarBanco(parts);
            case "DEPOSITAR": // Adicionando o case para depositar
                return handleDepositar(parts); // Chama o método para depositar
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

            boolean success = bankManager.transferir(bancoOrigem, agenciaOrigem, contaOrigem,
                    bancoDestino, agenciaDestino, contaDestino, valor);
            return success ? "Transferência realizada com sucesso." : "Falha na transferência.";
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

            bankManager.sacar(banco, agencia, conta, valor);
            return "Saque realizado com sucesso.";
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

            boolean success = bankManager.criarConta(banco, agencia, numeroConta, saldoInicial);
            return success ? "Conta criada com sucesso." : "Falha na criação da conta.";
        } else {
            return "Parâmetros inválidos para criação de conta.";
        }
    }

    private String handleCriarBanco(String[] parts) throws SQLException {
        if (parts.length == 2) {
            String bancoNome = parts[1];

            boolean success = bankManager.criarBanco(new Banco(bancoNome));
            return success ? "Banco criado com sucesso." : "Falha na criação do banco.";
        } else {
            return "Parâmetros inválidos para criação de banco.";
        }
    }

    // Método para lidar com a ação de depositar
    private String handleDepositar(String[] parts) throws SQLException {
        System.out.println("tamanho: " + parts.length);

        if (parts.length == 5) {
            String banco = parts[0];
            String agencia = parts[1];
            String conta = parts[2];
            double valor = Double.parseDouble(parts[3]);

            bankManager.depositar(banco, agencia, conta, valor); // Chama o método de depósito
            return "Depósito realizado com sucesso.";
        } else {
            return "Parâmetros inválidos para depósito.";
        }
    }
}
