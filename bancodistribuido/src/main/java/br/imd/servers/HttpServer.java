package br.imd.servers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import br.imd.processors.BankActionHandler;
import br.imd.processors.MessageProcessor;

public class HTTPServer {
    private int port;
    private ScheduledExecutorService heartbeatExecutor;
    private HttpServer server;

    public HTTPServer(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        server.createContext("/", new BankHandler());
        server.setExecutor(executorService);
        server.start();
        System.out.println("Servidor HTTP iniciado na porta " + port);

        heartbeatExecutor = Executors.newScheduledThreadPool(1);
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 0, 3, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws IOException {
        new HTTPServer(Integer.parseInt(args[0]));
    }

    private void sendHeartbeat() {
        try {
            URL url = new URL("http://localhost:3/heartbeat");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain");

            String heartbeatMessage = String.valueOf(port);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(heartbeatMessage.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();

        } catch (IOException e) {
            System.out.println("Erro ao enviar heartbeat via HTTP: " + e.getMessage());
        }
    }

    static class BankHandler implements HttpHandler {
        private MessageProcessor messageProcessor;
        private BankActionHandler bankActionHandler;

        public BankHandler() {
            this.bankActionHandler = new BankActionHandler();
            this.messageProcessor = new MessageProcessor();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String responseMessage = "";
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                String action = "";

                switch (method) {
                    case "POST":
                        switch (path) {
                            case "/depositar":
                                action = "DEPOSITAR";
                                requestBody = "DEPOSITAR-" + requestBody;
                                break;
                            case "/criarConta":
                                action = "CRIAR_CONTA";
                                requestBody = "CRIAR_CONTA-" + requestBody;
                                break;
                            case "/criarBanco":
                                action = "CRIAR_BANCO";
                                requestBody = "CRIAR_BANCO-" + requestBody;
                                break;
                            default:
                                responseMessage = "Ação não reconhecida para POST.";
                        }
                        break;

                    case "PUT":
                        switch (path) {
                            case "/sacar":
                                action = "SACAR";
                                requestBody = "SACAR-" + requestBody;
                                break;
                            case "/transferir":
                                action = "TRANSFERIR";
                                requestBody = "TRANSFERIR-" + requestBody;
                                // Aguardar resposta da transferência
                                responseMessage = sendHttpTransferRequest(requestBody);
                                break;
                            default:
                                responseMessage = "Ação não reconhecida para PUT.";
                        }
                        break;

                    case "GET":
                        switch (path) {
                            case "/listarContas":
                                action = "LISTAR_CONTAS";
                                requestBody = "LISTAR_CONTAS-" + requestBody;
                                break;
                            case "/listarBancos":
                                action = "LISTAR_BANCOS";
                                requestBody = "LISTAR_BANCOS-" + requestBody;
                                break;
                            default:
                                responseMessage = "Ação não reconhecida para GET.";
                        }
                        break;

                    case "DELETE":
                        switch (path) {
                            case "/excluirConta":
                                action = "EXCLUIR_CONTA";
                                requestBody = "EXCLUIR_CONTA-" + requestBody;
                                break;
                            case "/excluirBanco":
                                action = "EXCLUIR_BANCO";
                                requestBody = "EXCLUIR_BANCO-" + requestBody;
                                break;
                            default:
                                responseMessage = "Ação não reconhecida para DELETE.";
                        }
                        break;
                    default:
                        responseMessage = "Método HTTP não suportado.";
                        exchange.sendResponseHeaders(405, responseMessage.getBytes().length);
                        break;
                }

                if (!action.isEmpty() && responseMessage.isEmpty()) {
                    String[] parts = messageProcessor.processMessage(requestBody);
                    responseMessage = bankActionHandler.handleAction(action, parts);
                }

                if (responseMessage.contains("OK")) {
                    exchange.sendResponseHeaders(200, responseMessage.getBytes().length);
                } else {
                    exchange.sendResponseHeaders(400, responseMessage.getBytes().length);
                }

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseMessage.getBytes());
                }

            } catch (SQLException e) {
                responseMessage = "Erro no banco de dados: " + e.getMessage();
                exchange.sendResponseHeaders(500, responseMessage.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseMessage.getBytes());
                }
            } catch (NumberFormatException e) {
                responseMessage = "Valor inválido: " + e.getMessage();
                exchange.sendResponseHeaders(400, responseMessage.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseMessage.getBytes());
                }
            } catch (Exception e) {
                responseMessage = "Erro inesperado: " + e.getMessage();
                exchange.sendResponseHeaders(500, responseMessage.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseMessage.getBytes());
                }
            }
        }

        private String sendHttpTransferRequest(String requestBody) {
            String responseMessage = "";
            try {
                URL url = new URL("http://localhost:4001/transferir"); // Substitua pela URL correta
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "text/plain");

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(requestBody.getBytes());
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Lê a resposta do servidor de transferência
                    responseMessage = new String(connection.getInputStream().readAllBytes());
                    responseMessage = "Transferência realizada com sucesso: " + responseMessage;
                } else {
                    responseMessage = "Erro ao redirecionar transferência: " + responseCode;
                }
            } catch (IOException e) {
                responseMessage = "Erro ao enviar requisição de transferência: " + e.getMessage();
            }
            return responseMessage;
        }
    }
}
