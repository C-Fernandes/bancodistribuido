package repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConnection {

    private final String url = "jdbc:mysql://localhost:3306/banco"; // Substitua pelo seu banco de dados
    private final String user = "root"; // Substitua pelo seu usuário
    private final String password = "Clara.951"; // Substitua pela sua senha

    // Método para obter a conexão com o banco de dados
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
