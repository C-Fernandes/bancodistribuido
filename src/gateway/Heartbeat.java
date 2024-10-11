package gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.TimeUnit;

/**
 * Classe Heartbeat que monitora a saúde dos servidores ativos e inativos.
 */
public class Heartbeat implements Runnable {
    private static final int SERVER_TIMEOUT = 2000; // Timeout em ms
    private final ScheduledExecutorService scheduler; // Executor para verificação periódica
    private DatagramSocket udpSocket; // Socket UDP para reutilização
    private final Set<Integer> activeServers; // Conjunto de servidores ativos
    private final Set<Integer> inactiveServers; // Conjunto de servidores inativos

    public Heartbeat(Set<Integer> activeServers) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.activeServers = activeServers;
        this.inactiveServers = new HashSet<>();
        try {
            // Inicializa o DatagramSocket apenas uma vez
            this.udpSocket = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Falha ao criar o socket UDP");
        }
    }

    /**
     * Inicia o monitoramento dos servidores.
     */
    @Override
    public void run() {
        scheduler.scheduleAtFixedRate(this::checkServers, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Verifica a saúde dos servidores ativos e inativos.
     */
    private void checkServers() {
        System.out.println("Verificando servidores...");
        checkActiveServers();
        checkInactiveServers();
    }

    private void checkActiveServers() {
        Set<Integer> activeServersCopy = new HashSet<>(activeServers); // Cópia dos servidores ativos
        for (Integer port : activeServersCopy) {
            boolean isTCPAlive = isTCPServerAlive(port);
            boolean isUDPAlive = isUDPServerAlive(port);

            if (!isTCPAlive && !isUDPAlive) {
                // System.out.printf("Servidor na porta %d inativo. Movendo para inativos.%n",
                // port);
                activeServers.remove(port);
                inactiveServers.add(port);
            } else {
                // System.out.printf("Servidor na porta %d está ativo. TCP: %s, UDP: %s%n",
                // port, isTCPAlive, isUDPAlive);
            }
        }
    }

    /**
     * Verifica servidores inativos e move para ativos, se necessário.
     */
    private void checkInactiveServers() {
        Set<Integer> inactiveServersCopy = new HashSet<>(inactiveServers); // Cópia dos servidores inativos
        for (Integer port : inactiveServersCopy) {
            boolean isTCPAlive = isTCPServerAlive(port);
            boolean isUDPAlive = isUDPServerAlive(port);

            // Verifica se o servidor está ativo em ambos os protocolos antes de mover para
            // ativos
            if (isTCPAlive || isUDPAlive) {
                // System.out.printf("Servidor na porta %d voltou a ficar ativo. Movendo para
                // ativos.%n", port);
                inactiveServers.remove(port);
                activeServers.add(port);
            } else {
                // System.out.printf("Servidor na porta %d ainda está inativo. TCP: %s, UDP:
                // %s%n", port, isTCPAlive,isUDPAlive);
            }
        }
    }

    /**
     * Verifica se o servidor está ativo usando ambos os protocolos (TCP e UDP).
     *
     * @param port Porta do servidor
     * @return true se o servidor estiver ativo em TCP ou UDP, false caso contrário
     */
    private boolean isServerAlive(int port) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Boolean> tcpFuture = executor.submit(() -> isTCPServerAlive(port));
        Future<Boolean> udpFuture = executor.submit(() -> isUDPServerAlive(port));

        try {
            boolean isTCPAlive = tcpFuture.get(); // Espera o resultado da verificação TCP
            boolean isUDPAlive = udpFuture.get(); // Espera o resultado da verificação UDP

            return isTCPAlive || isUDPAlive;
        } catch (Exception e) {
            return false;
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Verifica se o servidor TCP está ativo.
     *
     * @param port Porta do servidor
     * @return true se o servidor TCP estiver ativo, false caso contrário
     */
    private boolean isTCPServerAlive(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), SERVER_TIMEOUT);

            // Envia a mensagem "ping" para o servidor
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("ping");

            // Aguardar e ler a resposta do servidor
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();

            // Verifica se a resposta é "pong"
            return "pong".equalsIgnoreCase(response);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Verifica se o servidor UDP está ativo.
     *
     * @param port Porta do servidor
     * @return true se o servidor UDP estiver ativo, false caso contrário
     */
    private boolean isUDPServerAlive(int port) {
        try {
            // Define o timeout para o socket UDP
            udpSocket.setSoTimeout(SERVER_TIMEOUT); // Timeout de 2 segundos, conforme SERVER_TIMEOUT
            byte[] buf = "ping".getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("localhost", port));
            udpSocket.send(packet);

            // Prepara o pacote para receber a resposta
            DatagramPacket responsePacket = new DatagramPacket(new byte[buf.length], buf.length);
            udpSocket.receive(responsePacket); // Espera pela resposta do servidor, respeitando o timeout
            return true; // Se recebemos resposta, o servidor está ativo
        } catch (IOException e) {
            return false; // Se o timeout ou uma exceção acontecer, considera o servidor como inativo
        }
    }

    /**
     * Fecha o socket UDP quando a aplicação é encerrada.
     */
    public void close() {
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }
}
