package br.imd.service;

import java.sql.Connection;
import java.sql.SQLException;

import br.imd.entity.Banco;
import br.imd.entity.Conta;
import br.imd.repository.BancoRepository;
import br.imd.repository.DataBaseConnection;

public class BancoService {
    private BancoRepository bancoRepository;
    private DataBaseConnection dataBaseConnection;
    private ContaService contaService;

    public BancoService() {
        this.bancoRepository = new BancoRepository();
        this.dataBaseConnection = new DataBaseConnection();
        this.contaService = new ContaService();
    }

    public Banco buscarBanco(String bancoNome) {
        Banco banco = bancoRepository.buscarBanco(bancoNome);
        if (banco == null) {
            throw new IllegalArgumentException("Banco não encontrado.");
        }
        return banco;
    }

    // Função para preparar a conta de destino para o depósito
    public Conta prepararParaDeposito(String bancoNome, Connection conn, String agencia, String contaNum)
            throws SQLException {
        buscarBanco(bancoNome);

        Conta conta = contaService.buscarContaEBloquear(conn, bancoNome, agencia, contaNum); // Buscar e bloquear a
                                                                                             // conta

        if (conta == null) {
            conn.rollback();
            throw new IllegalArgumentException("Conta de destino não encontrada no banco informado.");
        }

        // Se a conta de destino foi encontrada, continue com o fluxo
        System.out.println("Conta de destino verificada e pronta para o depósito.");
        return conta; // Retorna a conta, pronta para o depósito
    }

    // Função para preparar a conta de origem para o saque
    public Conta prepararParaSaque(String bancoNome, Connection conn, String agencia, String contaNum, double valor)
            throws SQLException {
        buscarBanco(bancoNome);

        Conta conta = contaService.buscarContaEBloquear(conn, bancoNome, agencia, contaNum); // Buscar e bloquear a
                                                                                             // conta

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
        return conta; // Retorna a conta, pronta para o saque
    }

    // Função para criar um novo banco
    public void criarBanco(Banco banco) throws SQLException {
        try {
            // Verifica se o banco já existe
            Banco bancoExistente = bancoRepository.buscarBanco(banco.getNome());
            if (bancoExistente != null) {
                throw new IllegalArgumentException("Banco já existe com o nome: " + banco.getNome());
            }

            // Cria o novo banco
            bancoRepository.criarBanco(banco);

            System.out.println("Banco criado com sucesso: " + banco.getNome());
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro geral: " + e.getMessage(), e);
        }
    } // Função para excluir um banco pelo nome

    public boolean excluirBanco(String bancoNome) {
        try {
            // Verifica se o banco existe
            Banco bancoExistente = bancoRepository.buscarBanco(bancoNome);
            if (bancoExistente == null) {
                throw new IllegalArgumentException("Banco não encontrado: " + bancoNome);
            }

            // Exclui o banco
            bancoRepository.excluirBanco(bancoNome);
            System.out.println("Banco excluído com sucesso: " + bancoNome);
            return true; // Indica sucesso
        } catch (Exception e) {
            System.out.println("Erro ao excluir o banco: " + e.getMessage());
            return false; // Indica falha
        }
    }
}
