package br.imd.servers;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.entity.Banco;
import br.imd.processors.MessageProcessor;
import br.imd.repository.BankManager;

public class HTTPServer {
    private BankManager bankManager;

    public HTTPServer(int port) throws IOException {
        this.bankManager = new BankManager();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 40);
        server.createContext("/bank", new BankHandler(bankManager));
        server.setExecutor(executorService); // Utiliza um pool de threads
        server.start();
        System.out.println("Servidor HTTP iniciado na porta " + port);
    }

    public static void main(String[] args) throws IOException {
        new HTTPServer(Integer.parseInt(args[0])); // Porta como argumento
    }

    static class BankHandler implements HttpHandler {
        private BankManager bankManager;

        public BankHandler(BankManager bankManager) {
            this.bankManager = bankManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String responseMessage = "";
            String method = exchange.getRequestMethod();
            if ("POST".equalsIgnoreCase(method)) {
                // Lê a solicitação
                String recv = new String(exchange.getRequestBody().readAllBytes());
                System.out.println("Requisição recebida: " + recv);

                // Processa a mensagem como no servidor TCP
                MessageProcessor messageProcessor = new MessageProcessor();
                String[] parts = messageProcessor.processMessage(recv);

                // Lógica de processamento (igual ao exemplo TCP)
                try {
                    if (parts[0].equalsIgnoreCase("TRANSFERIR") && parts.length == 8) {
                        String bancoOrigemNome = parts[1];
                        String agenciaOrigem = parts[2];
                        String contaNumOrigem = parts[3];
                        String bancoDestinoNome = parts[4];
                        String agenciaDestino = parts[5];
                        String contaNumDestino = parts[6];
                        double valor = Double.parseDouble(parts[7]);

                        boolean success = bankManager.transferir(bancoOrigemNome, agenciaOrigem, contaNumOrigem,
                                bancoDestinoNome, agenciaDestino, contaNumDestino, valor);
                        responseMessage = success ? "Transferência realizada com sucesso." : "Falha na transferência.";
                    } else if (parts[0].equalsIgnoreCase("SACAR") && parts.length == 5) {
                        String bancoNome = parts[1];
                        String agencia = parts[2];
                        String contaNum = parts[3];
                        double valor = Double.parseDouble(parts[4]);

                        bankManager.sacar(bancoNome, agencia, contaNum, valor);
                        responseMessage = "Saque realizado com sucesso.";
                    } else if (parts[0].equalsIgnoreCase("CRIAR_BANCO")) {
                        if (parts.length == 2) {
                            String bancoNome = parts[1];
                            boolean success = bankManager.criarBanco(new Banco(bancoNome));
                            responseMessage = success ? "Banco criado com sucesso." : "Falha na criação do banco.";
                        } else {
                            responseMessage = "Parâmetros inválidos para criação de banco.";
                        }
                    } else if (parts[0].equalsIgnoreCase("CRIAR_CONTA")) {
                        if (parts.length == 5) {
                            String bancoNome = parts[1];
                            String agencia = parts[2];
                            String contaNum = parts[3];
                            double valor = Double.parseDouble(parts[4]);

                            bankManager.criarConta(bancoNome, agencia, contaNum, valor);
                            responseMessage = "Conta criada com sucesso.";
                        } else {
                            responseMessage = "Parâmetros inválidos para criação de conta.";
                        }
                    } else {
                        responseMessage = "Comando inválido.";
                    }
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
