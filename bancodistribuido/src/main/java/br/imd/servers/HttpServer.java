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

        // Inicia o agendamento para enviar heartbeat via HTTP
        heartbeatExecutor = Executors.newScheduledThreadPool(1);
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 0, 3, TimeUnit.SECONDS); // A cada 3 segundos
    }

    public static void main(String[] args) throws IOException {
        new HTTPServer(Integer.parseInt(args[0])); // Porta como argumento
    }

    private void sendHeartbeat() {
        try {
            URL url = new URL("http://localhost:3/heartbeat"); // URL do endpoint do heartbeat
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain"); // Mudado para text/plain

            // Enviando apenas a porta como string
            String heartbeatMessage = String.valueOf(port); // Corpo do heartbeat

            try (OutputStream os = connection.getOutputStream()) {
                os.write(heartbeatMessage.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // System.out.println("Heartbeat enviado com sucesso via HTTP.");
            } else {
                // System.out.println("Falha ao enviar heartbeat. Código de resposta: " +
                // responseCode);
            }
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
            System.out.println(path);
            String method = exchange.getRequestMethod();
            System.out.println(method);
            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());

                // Determina a ação com base no caminho
                String action = "";

                switch (method) {
                    case "POST":
                        switch (path) {
                            case "/depositar":
                                action = "DEPOSITAR";
                                requestBody = "DEPOSITAR-" + requestBody; // Ação DEPOSITAR como primeira parte do
                                                                          // requestBody
                                break;
                            case "/criarConta":
                                action = "CRIAR_CONTA";
                                requestBody = "CRIAR_CONTA-" + requestBody; // Ação CRIAR_CONTA como primeira parte do
                                                                            // requestBody
                                break;
                            case "/criarBanco":
                                action = "CRIAR_BANCO";
                                requestBody = "CRIAR_BANCO-" + requestBody; // Ação CRIAR_BANCO como primeira parte do
                                                                            // requestBody
                                break;
                            default:
                                responseMessage = "Ação não reconhecida para POST.";
                        }
                        break;

                    case "PUT":
                        switch (path) {
                            case "/sacar":
                                action = "SACAR";
                                requestBody = "SACAR-" + requestBody; // Ação SACAR como primeira parte do requestBody
                                break;
                            case "/transferir":
                                action = "TRANSFERIR";
                                requestBody = "TRANSFERIR-" + requestBody; // Ação TRANSFERIR como primeira parte do
                                                                           // requestBody
                                break;
                            default:
                                responseMessage = "Ação não reconhecida para PUT.";
                        }
                        break;

                    case "GET":
                        switch (path) {
                            case "/listarContas":
                                action = "LISTAR_CONTAS";
                                requestBody = "LISTAR_CONTAS-" + requestBody; // Ação LISTAR_CONTAS como primeira parte
                                                                              // do requestBody
                                break;
                            case "/listarBancos":
                                action = "LISTAR_BANCOS";
                                requestBody = "LISTAR_BANCOS-" + requestBody; // Ação LISTAR_BANCOS como primeira parte
                                                                              // do requestBody
                                break;
                            default:
                                responseMessage = "Ação não reconhecida para GET.";
                        }
                        break;

                    case "DELETE":
                        switch (path) {
                            case "/excluirConta":
                                action = "EXCLUIR_CONTA";
                                requestBody = "EXCLUIR_CONTA-" + requestBody; // Ação EXCLUIR_CONTA como primeira parte
                                                                              // do requestBody
                                break;
                            case "/excluirBanco":
                                action = "EXCLUIR_BANCO";
                                requestBody = "EXCLUIR_BANCO-" + requestBody; // Ação EXCLUIR_BANCO como primeira parte
                                                                              // do requestBody
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

                // Se a ação for válida, chama handleAction
                if (!action.isEmpty() && responseMessage.isEmpty()) {
                    String[] parts = messageProcessor.processMessage(requestBody);
                    System.out.println("Mensagem para ser processada: " + requestBody);
                    responseMessage = bankActionHandler.handleAction(action, parts);
                    System.out.println("resposta banck action: " + responseMessage);
                }

                if (responseMessage.contains("OK")) {
                    exchange.sendResponseHeaders(200, responseMessage.getBytes().length);
                } else {

                    System.out.println("400");
                    exchange.sendResponseHeaders(400, responseMessage.getBytes().length);
                }

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseMessage.getBytes());
                }

            } catch (SQLException e) {
                System.out.println("Entrou no sql exception");
                responseMessage = "Erro no banco de dados: " + e.getMessage();
                exchange.sendResponseHeaders(500, responseMessage.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseMessage.getBytes());
                }
            } catch (NumberFormatException e) {
                System.out.println("Number Format exception");
                responseMessage = "Valor inválido: " + e.getMessage();
                exchange.sendResponseHeaders(400, responseMessage.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseMessage.getBytes());
                }
            } catch (Exception e) {

                System.out.println("Exception exception");
                responseMessage = "Erro inesperado: " + e.getMessage();
                exchange.sendResponseHeaders(500, responseMessage.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseMessage.getBytes());
                }
            }
        }
    }
}
