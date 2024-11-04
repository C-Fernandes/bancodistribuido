package br.imd.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.imd.entity.Conta;

public class ContaRepository {
    private DataBaseConnection dataBaseConnection;
    private BancoRepository bancoRepository;

    public ContaRepository() {
        this.dataBaseConnection = new DataBaseConnection();
        this.bancoRepository = new BancoRepository();
    }

    public void criarConta(Connection conn, Conta conta) throws SQLException {
        String sql = "INSERT INTO conta (banco, agencia, conta, saldo) VALUES (?, ?, ?, ?)";
        boolean closeConnection = false;

        if (conn == null) {
            conn = dataBaseConnection.getConnection();
            closeConnection = true;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, conta.getBanco().getNome());
            pstmt.setString(2, conta.getAgencia());
            pstmt.setString(3, conta.getConta());
            pstmt.setDouble(4, conta.getSaldo());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (closeConnection && conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.getMessage();
                }
            }
        }
    }

    public Conta buscarConta(Connection conn, String banco, String agencia, String conta) throws SQLException {
        String sql = "SELECT * FROM conta WHERE banco = ? AND agencia = ? AND conta = ?";
        Conta contaResult = null;
        boolean closeConnection = false;

        if (conn == null) {
            conn = dataBaseConnection.getConnection();
            closeConnection = true;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, banco);
            pstmt.setString(2, agencia);
            pstmt.setString(3, conta);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                contaResult = new Conta(rs.getString("agencia"), rs.getString("conta"), rs.getDouble("saldo"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (closeConnection && conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.getMessage();
                }
            }
        }
        return contaResult;
    }

    public List<Conta> listarContas() throws SQLException {
        String sql = "SELECT * FROM conta";
        List<Conta> contas = new ArrayList<>();

        try (Connection conn = dataBaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Conta conta = new Conta(
                        rs.getString("agencia"),
                        rs.getString("conta"),
                        rs.getDouble("saldo"),
                        bancoRepository.buscarBanco(rs.getString("banco")));
                contas.add(conta);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        return contas;
    }

    public void atualizarSaldo(Connection conn , Conta conta) throws SQLException {
        String sql = "UPDATE Conta SET saldo = ? WHERE banco = ? AND agencia = ? AND conta = ?";

        try (
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, conta.getSaldo());
            pstmt.setString(2, conta.getBanco().getNome());
            pstmt.setString(3, conta.getAgencia());
            pstmt.setString(4, conta.getConta());

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new IllegalArgumentException("Conta não encontrada para atualização.");
            }
            System.out.println("Saldo atualizado com sucesso para a conta: " + conta.getConta());

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar saldo de conta: " + conta.getConta() + ". " + e.getMessage());
            throw e;
        }
    }

    public void excluirConta(Connection conn, String banco, String agencia, String conta) throws SQLException {
        String sql = "DELETE FROM conta WHERE banco=? AND agencia = ? AND  conta = ? ";
        boolean closeConnection = false;

        if (conn == null) {
            conn = dataBaseConnection.getConnection();
            closeConnection = true;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, agencia);
            pstmt.setString(2, conta);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (closeConnection && conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.getMessage();
                }
            }
        }
    }

    public Conta buscarContaEBloquear(Connection conn, String bancoNome, String agencia, String conta)
            throws SQLException {

        String sql = "SELECT * FROM Conta WHERE banco = ? AND agencia = ? AND conta = ? FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bancoNome);
            stmt.setString(2, agencia);
            stmt.setString(3, conta);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Conta contaObj = new Conta();
                    contaObj.setAgencia(rs.getString("agencia"));
                    contaObj.setConta(rs.getString("conta"));
                    contaObj.setSaldo(rs.getDouble("saldo"));
                    return contaObj;
                } else {
                    return null;
                }
            }
        }
    }

    public List<Conta> listarContasPorBanco(Connection conn, String bancoNome) throws SQLException {
        String sql = "SELECT * FROM conta WHERE banco = ?";
        List<Conta> contas = new ArrayList<>();
        boolean closeConnection = false;

        if (conn == null) {
            conn = dataBaseConnection.getConnection();
            closeConnection = true;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bancoNome);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Conta conta = new Conta(rs.getString("agencia"), rs.getString("conta"), rs.getDouble("saldo"),
                        bancoRepository.buscarBanco(rs.getString("banco")));
                contas.add(conta);
            }
        } catch (SQLException e) {
            e.getMessage();
            throw new SQLException("Erro ao listar contas: " + e.getMessage(), e);
        } finally {
            if (closeConnection && conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.getMessage();
                }
            }
        }

        return contas;
    }

}
