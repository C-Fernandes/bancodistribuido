package br.imd.service;


import br.imd.entity.Conta;
import br.imd.repository.ContaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ContaService {
    private ContaRepository contaRepository;
    private BancoService bancoService;

    public ContaService() {
        this.contaRepository = new ContaRepository();
    }

    // Método para criar uma nova conta
    public boolean criarConta(Connection conn, Conta conta) throws SQLException {
        // Verifica se a conta já existe
        Conta contaExistente = contaRepository.buscarConta(conn, conta.getBanco().getNome(), conta.getAgencia(),
                conta.getConta());
        if (contaExistente != null) {
            throw new IllegalArgumentException("A conta já existe no banco informado.");
        }

        // Se a conta não existe, cria a nova conta
        contaRepository.criarConta(conn, conta);

        return true;
    }

    // Método para criar uma nova conta sem saldo inicial (saldo padrão = 0.0)
    public boolean criarConta(Connection conn, String banco, String agencia, String contaNum) throws SQLException {
        try {
            // Verifica se a conta já existe
            Conta contaExistente = contaRepository.buscarConta(conn, banco, agencia, contaNum);
            if (contaExistente != null) {
                throw new IllegalArgumentException("A conta já existe no banco informado.");
            }

            // Se a conta não existe, cria a nova conta
            Conta novaConta = new Conta();
            novaConta.setBanco(bancoService.buscarBanco(banco));
            novaConta.setAgencia(agencia);
            novaConta.setConta(contaNum);
            novaConta.setSaldo(0.0); // Saldo inicial padrão

            // Insere a nova conta no banco de dados
            contaRepository.criarConta(conn, novaConta);
            conn.commit(); // Confirma a transação
            System.out.println("Conta criada com sucesso: " + agencia + "/" + contaNum);
            return true; // Retorna true se a operação foi bem-sucedida
        } catch (Exception e) {
            conn.rollback(); // Faz o rollback em caso de erro
            e.printStackTrace();
            return false; // Retorna false se houve algum erro
        }
    }

    // Método para criar uma nova conta com saldo inicial definido
    public boolean criarConta(Connection conn, String banco, String agencia, String contaNum, double saldoInicial)
            throws SQLException {
        try {
            // Verifica se a conta já existe
            Conta contaExistente = contaRepository.buscarConta(conn, banco, agencia, contaNum);
            if (contaExistente != null) {
                throw new IllegalArgumentException("A conta já existe no banco informado.");
            }

            // Se a conta não existe, cria a nova conta com saldo inicial
            Conta novaConta = new Conta();
            novaConta.setBanco(bancoService.buscarBanco(banco));
            novaConta.setAgencia(agencia);
            novaConta.setConta(contaNum);
            novaConta.setSaldo(saldoInicial); // Saldo inicial definido

            // Insere a nova conta no banco de dados
            contaRepository.criarConta(conn, novaConta);
            conn.commit(); // Confirma a transação
            System.out.println(
                    "Conta criada com sucesso: " + agencia + "/" + contaNum + " com saldo inicial de: " + saldoInicial);
            return true; // Retorna true se a operação foi bem-sucedida
        } catch (Exception e) {
            conn.rollback(); // Faz o rollback em caso de erro
            e.printStackTrace();
            return false; // Retorna false se houve algum erro
        }
    }

    // Método para listar todas as contas
    public List<Conta> listarContas(Connection conn) throws SQLException {
        return contaRepository.listarContas(conn);
    }

    // Método para atualizar o saldo de uma conta
    public void atualizarSaldo(Connection conn, Conta conta) throws SQLException {
        contaRepository.atualizarSaldo(conn, conta);
    }

    // Método para excluir uma conta
    public void excluirConta(Connection conn, String banco, String agencia, String numero) throws SQLException {

        contaRepository.excluirConta(conn, banco, agencia, numero);
    }

    // Método para buscar uma conta pelo número e agência com bloqueio (FOR UPDATE)
    public Conta buscarContaEBloquear(Connection conn, String banco, String agencia, String numero)
            throws SQLException {
        return contaRepository.buscarContaEBloquear(conn, banco, agencia, numero);
    }

}
