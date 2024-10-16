package br.imd.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import br.imd.entity.Conta;
import br.imd.repository.BancoRepository;
import br.imd.repository.ContaRepository;

public class ContaService {
    private ContaRepository contaRepository;
    private BancoRepository bancoRepository;

    public ContaService() {
        this.contaRepository = new ContaRepository();
        this.bancoRepository = new BancoRepository();
    }

    // Método para criar uma nova conta
    public boolean criarConta(Connection conn, Conta conta) throws SQLException {
        // Verifica se a conta já existe
        Conta contaExistente = contaRepository.buscarConta(conn, conta.getBanco().getNome(), conta.getAgencia(),
                conta.getConta());
        contaExistente.toString();
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
            novaConta.setBanco(bancoRepository.buscarBanco(banco));
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
            e.getMessage();
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
            novaConta.setBanco(bancoRepository.buscarBanco(banco));
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
            e.getMessage();
            conn.close();
            return false; // Retorna false se houve algum erro
        }
    }

    // Método para listar todas as contas
    public List<Conta> listarContas() throws SQLException {
        return contaRepository.listarContas();
    }

    // Método para atualizar o saldo de uma conta
    public void atualizarSaldo(Conta conta) throws SQLException {

        contaRepository.atualizarSaldo(conta);
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

    public List<Conta> listarContasPorBanco(Connection conn, String banco) throws SQLException {
        // Verifica se o banco existe
        if (bancoRepository.buscarBanco(banco) == null) {
            throw new IllegalArgumentException("Banco não encontrado: " + banco);
        }

        // Busca e retorna as contas do banco
        List<Conta> contas = contaRepository.listarContasPorBanco(conn, banco);
        return contas;
    }

}
