package br.imd.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Classe Heartbeat que monitora a saúde dos servidores ativos.
 */
public class Heartbeat implements Runnable {
    private static final int SERVER_TIMEOUT = 4000; // Timeout em ms
    private final int port; // Porta em que o Heartbeat escuta
    private DatagramSocket udpSocket; // Socket UDP para escutar
    private ServerSocket tcpServerSocket; // Socket TCP para escutar
    private final Set<Integer> activeServers; // Conjunto de servidores ativos
    private final Map<Integer, Long> lastResponseTimes; // Último tempo de resposta de cada servidor
    private final ScheduledExecutorService scheduler; // Executor para verificação periódica

    public Heartbeat(int port, Set<Integer> activeServers) {
        this.port = port;
        this.activeServers = activeServers;
        activeServers.clear();

        this.lastResponseTimes = new HashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        try {
            // Inicializa o DatagramSocket para escuta UDP
            this.udpSocket = new DatagramSocket(port);
            // Inicializa o ServerSocket para escuta TCP
            this.tcpServerSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Falha ao criar sockets UDP ou TCP");
        }
    }

    /**
     * Inicia o monitoramento dos servidores.
     */
    @Override
    public void run() {
        // Inicia um thread para escutar mensagens UDP
        new Thread(this::listenForUDPMessages).start();
        // Inicia um thread para escutar mensagens TCP
        new Thread(this::listenForTCPConnections).start();
        // Inicia o agendador para verificar a saúde dos servidores periodicamente
        scheduler.scheduleAtFixedRate(this::checkServers, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Escuta mensagens UDP para verificar a saúde dos servidores.
     */
    private void listenForUDPMessages() {
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                // Recebe pacotes UDP
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                // System.out.printf("Recebido UDP: %s de %s:%d%n", message,
                // packet.getAddress(), packet.getPort());

                // A mensagem UDP deve ser interpretada como porta do servidor
                int serverPort = Integer.parseInt(message); // Converte a mensagem recebida para int
                handleServerResponse(serverPort, true); // Marcar a porta do servidor como ativa
            } catch (IOException e) {
                System.err.println("Erro ao receber pacote UDP: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Formato de número inválido: " + e.getMessage());
            }
        }
    }

    /**
     * Escuta conexões TCP para verificar a saúde dos servidores.
     */
    private void listenForTCPConnections() {
        while (true) {
            try {
                // Aceita conexões TCP
                Socket clientSocket = tcpServerSocket.accept();
                new Thread(() -> handleTCPClient(clientSocket)).start();
            } catch (IOException e) {
                System.err.println("Erro ao aceitar conexão TCP: " + e.getMessage());
            }
        }
    }

    /**
     * Trata uma conexão de cliente TCP.
     *
     * @param clientSocket Socket do cliente
     */
    private void handleTCPClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String request = in.readLine();
            // System.out.printf("Recebido TCP/HTTP: %s de %s:%d%n", request,
            // clientSocket.getInetAddress(),
            // clientSocket.getPort());

            // A porta recebida é a porta do servidor que está se reportando
            int serverPort = Integer.parseInt(request.trim()); // Converte a mensagem recebida para int
            handleServerResponse(serverPort, true); // Marcar a porta do servidor como ativa
        } catch (IOException e) {
            System.err.println("Erro ao tratar conexão TCP: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Formato de número inválido: " + e.getMessage());
        }
    }

    /**
     * Trata a resposta do servidor.
     *
     * @param port     Porta do servidor
     * @param isActive Indica se o servidor está ativo
     */
    private void handleServerResponse(int port, boolean isActive) {
        long currentTime = System.currentTimeMillis(); // Tempo atual
        if (isActive) {
            // Marcar o servidor como ativo e atualizar o tempo da última resposta
            lastResponseTimes.put(port, currentTime); // Atualiza o tempo da última resposta
            /// System.out.printf("Servidor na porta %d está ativo.%n", port);
            activeServers.add(port);
        }
        printServerStatus(); // Imprime o status dos servidores
    }

    /**
     * Imprime o status dos servidores ativos.
     */
    private void printServerStatus() {
        // System.out.println("Status dos Servidores Ativos:");
        // System.out.println("Ativos: " + activeServers);
    }

    /**
     * Verifica a saúde dos servidores ativos.
     */
    private void checkServers() {
        long currentTime = System.currentTimeMillis(); // Tempo atual

        // Verifica servidores ativos
        for (Integer port : new HashSet<>(activeServers)) {
            // Se o servidor não enviou resposta no tempo limite, remova-o da lista de
            // ativos
            if (lastResponseTimes.containsKey(port) &&
                    (currentTime - lastResponseTimes.get(port) > SERVER_TIMEOUT)) {
                System.out.printf("Servidor na porta %d não respondeu a tempo e será considerado nulo.%n", port);
                activeServers.remove(port); // Remove da lista de ativos
            }
        }

        // O status dos servidores é impresso apenas para servidores ativos
        printServerStatus(); // Atualiza e imprime o status dos servidores
    }

    /**
     * Fecha os sockets UDP e TCP quando a aplicação é encerrada.
     */
    public void close() {
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
            try {
                tcpServerSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o ServerSocket TCP: " + e.getMessage());
            }
        }
        scheduler.shutdown();
    }
}
