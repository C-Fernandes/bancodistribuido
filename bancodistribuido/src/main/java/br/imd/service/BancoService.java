package br.imd.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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

    public Conta prepararParaDeposito(String bancoNome, Connection conn, String agencia, String contaNum)
            throws SQLException {

        Conta conta = contaService.buscarContaEBloquear(conn, bancoNome, agencia, contaNum);

        if (conta == null) {
            conn.rollback();System.out.println("Problema com conta destino.");

            throw new IllegalArgumentException("Conta de destino não encontrada no banco informado.");
        }

        System.out.println("Conta de destino verificada e pronta para o depósito.");

        conta.setBanco(buscarBanco(bancoNome));
        return conta;
    }

    public Conta prepararParaSaque(String bancoNome, Connection conn, String agencia, String contaNum, double valor)
            throws SQLException {
        buscarBanco(bancoNome);
        Conta conta = contaService.buscarContaEBloquear(conn, bancoNome, agencia, contaNum);
        conn.setAutoCommit(false);
        if (conta == null) {
            conn.rollback();
            throw new IllegalArgumentException("Conta de origem não encontrada no banco informado.");
        }
        System.out.println("Saldo: " + conta.getSaldo());
        System.out.println("Valor: " + valor);
        if (conta.getSaldo() < valor) {
            conn.rollback();
            conta = null;
            throw new IllegalArgumentException("Saldo insuficiente na conta.");

        }

        System.out.println("Conta de origem verificada e saldo suficiente.");
        conta.setBanco(buscarBanco(bancoNome));
        return conta;
    }

    public void criarBanco(Banco banco) throws SQLException {
        try {
            Banco bancoExistente = bancoRepository.buscarBanco(banco.getNome());
            if (bancoExistente != null) {
                throw new IllegalArgumentException("Banco já existe com o nome: " + banco.getNome());
            }

            bancoRepository.criarBanco(banco);

            System.out.println("Banco criado com sucesso: " + banco.getNome());
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro geral: " + e.getMessage(), e);
        }
    }

    public boolean excluirBanco(String bancoNome) {
        try {
            Banco bancoExistente = bancoRepository.buscarBanco(bancoNome);
            if (bancoExistente == null) {
                throw new IllegalArgumentException("Banco não encontrado: " + bancoNome);
            }

            bancoRepository.excluirBanco(bancoNome);
            System.out.println("Banco excluído com sucesso: " + bancoNome);
            return true;
        } catch (Exception e) {
            System.out.println("Erro ao excluir o banco: " + e.getMessage());
            return false;
        }
    }

    public List<Banco> listarBancos(Connection conn) throws SQLException {
        return bancoRepository.listarBancos(conn);
    }
}
