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

    public boolean criarConta(Connection conn, Conta conta) throws SQLException {
        Conta contaExistente = contaRepository.buscarConta(conn, conta.getBanco().getNome(), conta.getAgencia(),
                conta.getConta());
        contaExistente.toString();
        if (contaExistente != null) {
            throw new IllegalArgumentException("A conta já existe no banco informado.");
        }

        contaRepository.criarConta(conn, conta);

        return true;
    }

    public boolean criarConta(Connection conn, String banco, String agencia, String contaNum) throws SQLException {
        try {
            Conta contaExistente = contaRepository.buscarConta(conn, banco, agencia, contaNum);
            if (contaExistente != null) {
                throw new IllegalArgumentException("A conta já existe no banco informado.");
            }

            Conta novaConta = new Conta();
            novaConta.setBanco(bancoRepository.buscarBanco(banco));
            novaConta.setAgencia(agencia);
            novaConta.setConta(contaNum);
            novaConta.setSaldo(0.0);

            contaRepository.criarConta(conn, novaConta);
            conn.commit();
            System.out.println("Conta criada com sucesso: " + agencia + "/" + contaNum);
            return true;
        } catch (Exception e) {
            conn.rollback();
            e.getMessage();
            return false;
        }
    }

    public boolean criarConta(Connection conn, String banco, String agencia, String contaNum, double saldoInicial)
            throws SQLException {
        try {
            Conta contaExistente = contaRepository.buscarConta(conn, banco, agencia, contaNum);
            if (contaExistente != null) {
                throw new IllegalArgumentException("A conta já existe no banco informado.");
            }

            Conta novaConta = new Conta();
            novaConta.setBanco(bancoRepository.buscarBanco(banco));
            novaConta.setAgencia(agencia);
            novaConta.setConta(contaNum);
            novaConta.setSaldo(saldoInicial);

            contaRepository.criarConta(conn, novaConta);
            conn.commit();
            System.out.println(
                    "Conta criada com sucesso: " + agencia + "/" + contaNum + " com saldo inicial de: " + saldoInicial);
            return true;
        } catch (Exception e) {
            conn.rollback();
            e.getMessage();
            conn.close();
            return false;
        }
    }

    public List<Conta> listarContas() throws SQLException {
        return contaRepository.listarContas();
    }

    public void atualizarSaldo(Conta conta) throws SQLException {

        contaRepository.atualizarSaldo(conta);
    }

    public void excluirConta(Connection conn, String banco, String agencia, String numero) throws SQLException {
        contaRepository.excluirConta(conn, banco, agencia, numero);
    }

    public Conta buscarContaEBloquear(Connection conn, String banco, String agencia, String numero)
            throws SQLException {
        return contaRepository.buscarContaEBloquear(conn, banco, agencia, numero);
    }

    public List<Conta> listarContasPorBanco(Connection conn, String banco) throws SQLException {
        if (bancoRepository.buscarBanco(banco) == null) {
            throw new IllegalArgumentException("Banco não encontrado: " + banco);
        }

        List<Conta> contas = contaRepository.listarContasPorBanco(conn, banco);
        return contas;
    }

}
