package br.imd.servers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import br.imd.processors.BankActionHandler;
import br.imd.service.BankManager;

public class HTTPServer {
    private BankManager bankManager;
    private ScheduledExecutorService heartbeatExecutor;
    private int serverPort; // Armazenar a porta do servidor

    public HTTPServer(int port) throws IOException {
        this.bankManager = new BankManager();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 40);
        server.createContext("/", new BankHandler(bankManager));
        server.setExecutor(executorService); // Utiliza um pool de threads
        server.start();
        System.out.println("Servidor HTTP iniciado na porta " + port);

        // Configuração do heartbeat
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 0, 3, TimeUnit.SECONDS); // Envia a porta a cada 3
                                                                                            // segundos
    }
    //

    private void sendHeartbeat() {
        String heartbeatMessage = Integer.toString(serverPort); // Enviando a porta em vez de "ping"
        try (Socket socket = new Socket("localhost", 8000)) { // Porta do servidor que receberá a mensagem
            OutputStream os = socket.getOutputStream();
            os.write(heartbeatMessage.getBytes());
            os.flush();
        } catch (IOException e) {
            System.out.println("Falha ao enviar mensagem: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        new HTTPServer(Integer.parseInt(args[0])); // Porta como argumento
    }

    static class BankHandler implements HttpHandler {
        private BankManager bankManager;
        private BankActionHandler bankActionHandler; // Adicionando a classe BankActionHandler

        public BankHandler(BankManager bankManager) {
            this.bankManager = bankManager;
            this.bankActionHandler = new BankActionHandler(bankManager); // Inicializando BankActionHandler
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String responseMessage = "";
            String method = exchange.getRequestMethod();
            if ("POST".equalsIgnoreCase(method)) {
                // Lê a solicitação
                String recv = new String(exchange.getRequestBody().readAllBytes());
                System.out.println("Requisição recebida: " + recv);

                // Aqui você pode usar o BankActionHandler diretamente
                String[] parts = recv.split("-"); // Ajuste conforme necessário para separar os comandos
                try {
                    responseMessage = bankActionHandler.handleAction(parts[0], parts); // Chame o método handleAction
                } catch (SQLException e) {
                    responseMessage = "Erro no banco de dados: " + e.getMessage();
                }

                // Responde ao cliente
                exchange.sendResponseHeaders(200, responseMessage.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseMessage.getBytes());
                os.close();
            } else {
                // Método HTTP não suportado
                responseMessage = "Método HTTP não suportado.";
                exchange.sendResponseHeaders(405, responseMessage.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseMessage.getBytes());
                os.close();
            }
        }
    }
}
