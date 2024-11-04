package br.imd.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.entity.Conta;
import br.imd.repository.DataBaseConnection;

public class TransferenciaService {

    private DataBaseConnection dataBaseConnection;
    private BancoService bancoService;
    private ContaService contaService;
    private ExecutorService executorService;

    public TransferenciaService() {
        this.dataBaseConnection = new DataBaseConnection();
        this.bancoService = new BancoService();
        this.contaService = new ContaService();
        this.executorService = Executors.newFixedThreadPool(10);
        startServers();
    }

    public void startServers() {
        new Thread(() -> startTcpAndHttpServer()).start();
        new Thread(() -> startUdpServer()).start();
    }

    private void startTcpAndHttpServer() {
        try (ServerSocket serverSocket = new ServerSocket(4001)) {
            System.out.println("TCP/HTTP Server rodando na porta 4001...");

            while (true) {
                Socket socket = serverSocket.accept();
                executorService.submit(() -> handleTcpOrHttpClient(socket));
            }
        } catch (Exception e) {
            System.err.println("Erro no servidor TCP/HTTP: " + e.getMessage());
        }
    }

    private void handleTcpOrHttpClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream())) {

            String requestLine = reader.readLine();
            System.out.println("Requisição recebida: " + requestLine);

            if (requestLine != null && (requestLine.startsWith("GET") || requestLine.startsWith("POST"))) {
                handleHttpRequest(reader, writer, requestLine);
            } else {
                handleTcpRequest(socket, requestLine);
            }

        } catch (Exception e) {
            System.err.println("Erro ao processar cliente TCP/HTTP: " + e.getMessage());
        }
    }

    private void handleHttpRequest(BufferedReader reader, PrintWriter writer, String requestLine) {
        System.out.println("Requisição HTTP detectada: " + requestLine);

        if (requestLine.startsWith("POST")) {
            try {
                // Lê as linhas do cabeçalho até encontrar uma linha vazia (fim do cabeçalho)
                String line;
                int contentLength = 0;
                while (!(line = reader.readLine()).isEmpty()) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }

                // Lê o corpo da requisição
                char[] body = new char[contentLength];
                reader.read(body, 0, contentLength);
                String requestData = new String(body);
                System.out.println("Corpo da requisição: " + requestData);

                // Processa a transferência e envia a resposta
                String response = processTransferWithRetry(requestData);

                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: text/plain");
                writer.println();
                writer.println(response);
            } catch (IOException e) {
                writer.println("HTTP/1.1 500 Internal Server Error");
                writer.println("Content-Type: text/plain");
                writer.println();
                writer.println("Erro ao processar a requisição: " + e.getMessage());
            }
        } else {
            writer.println("HTTP/1.1 405 Method Not Allowed");
            writer.println("Content-Type: text/plain");
            writer.println();
            writer.println("Método HTTP não suportado.");
        }

        writer.flush();
    }

    private void handleTcpRequest(Socket socket, String request) {
        try (OutputStream outputStream = socket.getOutputStream()) {
            System.out.println("Requisição TCP detectada: " + request);

            String response = processTransferWithRetry(request);
            System.out.println("Resposta enviada: " + response);
            outputStream.write((response + "\n").getBytes());
            outputStream.flush();
        } catch (Exception e) {
            System.err.println("Erro ao processar cliente TCP: " + e.getMessage());
        }
    }

    private void startUdpServer() {
        try (DatagramSocket datagramSocket = new DatagramSocket(4001)) {
            System.out.println("UDP Server rodando na porta 4001...");

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                executorService.submit(() -> handleUdpClient(packet, datagramSocket));
            }
        } catch (Exception e) {
            System.err.println("Erro no servidor UDP: " + e.getMessage());
        }
    }

    private void handleUdpClient(DatagramPacket packet, DatagramSocket socket) {
        try {
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Requisição UDP recebida: " + request);

            String response = processTransferWithRetry(request);

            byte[] responseBytes = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
            socket.send(responsePacket);

        } catch (Exception e) {
            System.err.println("Erro ao processar cliente UDP: " + e.getMessage());
        }
    }

    public String processTransferWithRetry(String request) {
        System.out.println("Entrou aqui");
        int retryCount = 0;
        int maxRetries = 20;
        long retryDelay = 100;

        while (retryCount < maxRetries) {
            try {
                return processTransferRequest(request);
            } catch (SQLException e) {
                if (e.getMessage().startsWith("Deadlock found")) {
                    retryCount++;
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return "Erro: Interrupção inesperada durante retry.";
                    }
                } else {
                    return "Erro na transferência: " + e.getMessage();
                }
            }
        }
        return "Erro na transferência: Deadlock persistente após " + maxRetries + " tentativas.";
    }

    public String processTransferRequest(String request) throws SQLException {
        String[] params = request.split("-");
        if (params.length != 8) {
            return "Erro: Número de parâmetros inválido.";
        }

        String bancoOrigemNome = params[1];
        String agenciaOrigem = params[2];
        String contaNumOrigem = params[3];
        String bancoDestinoNome = params[4];
        String agenciaDestino = params[5];
        String contaNumDestino = params[6];
        double valor;

        try {
            valor = Double.parseDouble(params[7]);
        } catch (NumberFormatException e) {
            return "Erro: Valor inválido.";
        }

        try (Connection conn = dataBaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Preparar saque
            Conta contaOrigem = bancoService.prepararParaSaque(bancoOrigemNome, conn, agenciaOrigem, contaNumOrigem,
                    valor);
            if (contaOrigem == null) {
                conn.rollback();
                return "Erro: Conta de origem não encontrada.";
            }

            // Preparar depósito
            Conta contaDestino = bancoService.prepararParaDeposito(bancoDestinoNome, conn, agenciaDestino,
                    contaNumDestino);
            if (contaDestino == null) {
                conn.rollback();
                return "Erro: Conta de destino não encontrada.";
            }

            double saldoOrigemAtualizado = contaOrigem.getSaldo() - valor;
            double saldoDestinoAtualizado = contaDestino.getSaldo() + valor;

            contaOrigem.setSaldo(saldoOrigemAtualizado);
            contaDestino.setSaldo(saldoDestinoAtualizado);

            contaService.atualizarSaldo(conn, contaOrigem);
            contaService.atualizarSaldo(conn, contaDestino);

            conn.commit();
            return "Transferência realizada com sucesso! OK";

        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

}
