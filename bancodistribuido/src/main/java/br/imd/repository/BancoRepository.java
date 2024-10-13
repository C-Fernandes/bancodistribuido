package br.imd.repository;

import br.imd.entity.Banco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BancoRepository {
    private DataBaseConnection databaseConnection;

    public BancoRepository() {
        this.databaseConnection = new DataBaseConnection();
    }

    // Método para criar um novo banco
    public void criarBanco(Banco banco) {
        String sql = "INSERT INTO bancos (nome) VALUES (?)";

        try (Connection conn = databaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, banco.getNome());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método para buscar um banco pelo nome
    public Banco buscarBanco(String nome) {
        String sql = "SELECT * FROM bancos WHERE nome = ?";
        Banco banco = null;
        try (Connection conn = databaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                banco = new Banco(rs.getString("nome"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return banco;
    }

    // Método para listar todos os bancos
    public List<Banco> listarBancos() {
        String sql = "SELECT * FROM bancos";
        List<Banco> bancos = new ArrayList<>();

        try (Connection conn = databaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Banco banco = new Banco(rs.getString("nome"));
                bancos.add(banco);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bancos;
    }

    // Método para excluir um banco
    public void excluirBanco(String nome) {
        String sql = "DELETE FROM bancos WHERE nome = ?";

        try (Connection conn = databaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
