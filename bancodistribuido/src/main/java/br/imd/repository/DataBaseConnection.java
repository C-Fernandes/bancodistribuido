package br.imd.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConnection {
    private final String url = "jdbc:mysql://localhost:3306/banco?useSSL=false"; // Adicione `?useSSL=false` se você não
                                                                                 // estiver usando SSL
    private final String user = "root"; // Substitua pelo seu usuário
    private final String password = "Clara.951"; // Substitua pela sua senha

    public Connection getConnection() {
        Connection connection = null;
        try {
            // Carregar o driver JDBC
            Class.forName("com.mysql.cj.jdbc.Driver"); // Isso pode ser opcional em versões mais recentes

            // Estabelecendo a conexão
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Conexão estabelecida com sucesso!");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver JDBC não encontrado: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Erro ao conectar ao banco de dados: " + e.getMessage());
        }
        return connection;
    }
}
