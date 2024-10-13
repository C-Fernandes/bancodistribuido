package service;

import entity.Conta;
import repository.ContaRepository;

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
    public void criarConta(Connection conn, Conta conta) throws SQLException {
        // Verifica se a conta já existe
        Conta contaExistente = contaRepository.buscarConta(conn, conta.getBanco().getNome(), conta.getAgencia(), conta.getConta());
        if (contaExistente != null) {
            throw new IllegalArgumentException("A conta já existe no banco informado.");
        }

        // Se a conta não existe, cria a nova conta
        contaRepository.criarConta(conn, conta);
    }

    // Método sobrecarregado para criar uma nova conta
    public void criarConta(Connection conn, String banco, String agencia, String contaNum) throws SQLException {
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
        // Você pode definir o saldo inicial se necessário
        novaConta.setSaldo(0.0); // ou qualquer valor inicial que você desejar

        contaRepository.criarConta(conn, novaConta);
    }


    public Conta buscarConta(Connection conn, String banco, String agencia, String numero) throws SQLException {
        Conta conta = contaRepository.buscarConta(conn, banco, agencia, numero);
        if (conta == null) {
            throw new IllegalArgumentException("Conta não encontrada no banco: " + banco + ", Agência: " + agencia + ", Número: " + numero);
        }
        return conta;
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
    public Conta buscarContaEBloquear(Connection conn, String banco, String agencia, String numero) throws SQLException {
        return contaRepository.buscarContaEBloquear(conn, banco, agencia, numero);
    }


}
