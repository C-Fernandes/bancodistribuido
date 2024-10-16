package br.imd.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Classe Heartbeat que monitora a saúde dos servidores ativos.
 */
public class Heartbeat implements Runnable {
    private static final int SERVER_TIMEOUT = 3000;
    private final int udpPort = 1;
    private final int tcpPort = 2;
    private final int httpPort = 3;
    private DatagramSocket udpSocket;
    private ServerSocket tcpServerSocket;
    private HttpServer httpServer;
    private final Set<Integer> activeServers;
    private final Map<Integer, Long> lastResponseTimes;
    private final ScheduledExecutorService scheduler;

    public Heartbeat(Set<Integer> activeServers) {
        this.activeServers = activeServers;
        activeServers.clear();

        this.lastResponseTimes = new HashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        try {
            this.udpSocket = new DatagramSocket(udpPort);
            this.tcpServerSocket = new ServerSocket(tcpPort);
            new Thread(() -> {
                try {
                    this.httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
                    this.httpServer.createContext("/heartbeat", new HttpHandler() {
                        @Override
                        public void handle(HttpExchange exchange) throws IOException {
                            handleHttpRequest(exchange);
                        }
                    });
                    this.httpServer.setExecutor(Executors.newSingleThreadExecutor());
                    this.httpServer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Falha ao criar o HttpServer");
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Falha ao criar sockets UDP, TCP ou HTTP");
        }
    }

    @Override
    public void run() {
        new Thread(this::listenForUDPMessages).start();
        new Thread(this::listenForTCPConnections).start();
        scheduler.scheduleAtFixedRate(this::checkServers, 0, 3, TimeUnit.SECONDS);
    }

    private void listenForUDPMessages() {
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                int serverPort = Integer.parseInt(message);
                handleServerResponse(serverPort, true);
            } catch (IOException | NumberFormatException e) {
                System.err.println("Erro ao receber pacote UDP: " + e.getMessage());
            }
        }
    }

    private void listenForTCPConnections() {
        while (true) {
            try {
                Socket clientSocket = tcpServerSocket.accept();
                new Thread(() -> handleTCPClient(clientSocket)).start();
            } catch (IOException e) {
                System.err.println("Erro ao aceitar conexão TCP: " + e.getMessage());
            }
        }
    }

    private void handleTCPClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String request = in.readLine();
            int serverPort = Integer.parseInt(request.trim());
            handleServerResponse(serverPort, true);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Erro ao tratar conexão TCP: " + e.getMessage());
        }
    }

    private void handleHttpRequest(HttpExchange exchange) throws IOException {
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes()).trim();
            int serverPort = Integer.parseInt(requestBody);
            handleServerResponse(serverPort, true);

            exchange.sendResponseHeaders(200, 0);
        } catch (Exception e) {
            System.err.println("Erro ao processar requisição HTTP: " + e.getMessage());

            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }

    private void handleServerResponse(int port, boolean isActive) {
        long currentTime = System.currentTimeMillis();
        if (isActive) {
            lastResponseTimes.put(port, currentTime);
            activeServers.add(port);
        }
    }

    private void checkServers() {
        // System.out.println("portas ativas: " + activeServers);
        long currentTime = System.currentTimeMillis();
        for (Integer port : new HashSet<>(activeServers)) {
            if (lastResponseTimes.containsKey(port) &&
                    (currentTime - lastResponseTimes.get(port) > SERVER_TIMEOUT)) {
                System.out.printf("Servidor na porta %d não respondeu a tempo.%n", port);
                activeServers.remove(port);
            }
        }
    }
}
