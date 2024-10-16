package br.imd.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.imd.entity.Banco;

public class BancoRepository {
    private DataBaseConnection databaseConnection;

    public BancoRepository() {
        this.databaseConnection = new DataBaseConnection();
    }

    public void criarBanco(Banco banco) {
        String sql = "INSERT INTO banco (nome) VALUES (?)";

        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, banco.getNome());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.getMessage();
        }
    }

    public Banco buscarBanco(String nome) {
        String sql = "SELECT * FROM banco WHERE nome = ?";
        Banco banco = null;
        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                banco = new Banco(rs.getString("nome"));
            }
        } catch (SQLException e) {
            e.getMessage();
        }
        return banco;
    }

    public List<Banco> listarBancos() {
        String sql = "SELECT * FROM banco";
        List<Banco> bancos = new ArrayList<>();

        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Banco banco = new Banco(rs.getString("nome"));
                bancos.add(banco);
            }
        } catch (SQLException e) {
            e.getMessage();
        }

        return bancos;
    }

    public void excluirBanco(String nome) {
        String sql = "DELETE FROM banco WHERE nome = ?";

        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.getMessage();
        }
    }

    public List<Banco> listarBancos(Connection conn) throws SQLException {
        String sql = "SELECT * FROM banco";
        List<Banco> bancos = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Banco banco = new Banco(rs.getString("nome"));
                bancos.add(banco);
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao listar bancos: " + e.getMessage(), e);
        }

        return bancos;
    }
}
