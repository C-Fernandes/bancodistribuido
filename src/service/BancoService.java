package service;

import entity.Banco;
import entity.Conta;
import repository.BancoRepository;
import repository.DataBaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class BancoService {
    private BancoRepository bancoRepository;
    private DataBaseConnection dataBaseConnection;
    private ContaService contaService;


    public Banco buscarBanco(String bancoDestinoNome) {
        Banco banco = bancoRepository.buscarBanco(nomeBanco);
        if (banco == null) {
            throw new IllegalArgumentException("Banco de destino não encontrado.");
        }
        return banco;
    }

    // Função para preparar a conta de destino para o depósito
    public Conta prepararParaDeposito(String bancoNome, Connection conn, String agencia, String contaNum) throws SQLException {
        buscarBanco(bancoNome);

        Conta conta = contaService.buscarContaEBloquear(conn, bancoNome, agencia, contaNum); // Buscar e bloquear a conta

        if (conta == null) {
            conn.rollback();
            throw new IllegalArgumentException("Conta de destino não encontrada no banco informado.");
        }

        // Se a conta de destino foi encontrada, continue com o fluxo
        System.out.println("Conta de destino verificada e pronta para o depósito.");
        return conta;  // Retorna a conta, pronta para o depósito
    }

    // Função para preparar a conta de origem para o saque
    public Conta prepararParaSaque(String bancoNome, Connection conn, String agencia, String contaNum) throws SQLException {
        buscarBanco(bancoNome);

        Conta conta = contaService.buscarContaEBloquear(conn, bancoNome, agencia, contaNum); // Buscar e bloquear a conta

        if (conta == null) {
            conn.rollback();
            throw new IllegalArgumentException("Conta de origem não encontrada no banco informado.");
        }

        // Verificar se a conta tem saldo suficiente
        if (conta.getSaldo() < valor) {
            conn.rollback();
            throw new IllegalArgumentException("Saldo insuficiente na conta.");
        }

        // Se a conta existe e o saldo é suficiente, continue com o fluxo
        System.out.println("Conta de origem verificada e saldo suficiente.");
        return conta;  // Retorna a conta, pronta para o saque
    }

}
