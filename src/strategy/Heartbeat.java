package strategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Set;
import java.util.HashSet;

/**
 * Classe Heartbeat que monitora a saúde dos servidores ativos e inativos.
 */
public class Heartbeat implements Runnable {

    private static final int SERVER_TIMEOUT = 2000; // Timeout em ms
    private final Set<Integer> activeServers; // Conjunto de servidores ativos
    private final Set<Integer> inactiveServers; // Conjunto de servidores inativos

    /**
     * Construtor da classe Heartbeat.
     *
     * @param activeServers   Conjunto de portas dos servidores ativos
     * @param inactiveServers Conjunto de portas dos servidores inativos
     */
    public Heartbeat(Set<Integer> activeServers, Set<Integer> inactiveServers) {
        this.activeServers = activeServers;
        this.inactiveServers = inactiveServers;
    }

    public Heartbeat(Set<Integer> activeServers) {
        this(activeServers, new HashSet<>());
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            checkServers();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Verifica a saúde dos servidores ativos e inativos.
     */
    private void checkServers() {
        System.out.println("Iniciando verificação de servidores...");

        checkActiveServers();
        checkInactiveServers();
    }

    /**
     * Verifica servidores ativos e move para inativos, se necessário.
     */
    private void checkActiveServers() {
        for (Integer port : new HashSet<>(activeServers)) {
            if (!isServerAlive(port)) {
                System.out.printf("Servidor na porta %d inativo. Movendo para inativos.%n", port);
                activeServers.remove(port);
                inactiveServers.add(port);
            } else {
                System.out.printf("Servidor na porta %d está ativo.%n", port);
            }
        }
    }

    /**
     * Verifica servidores inativos e move para ativos, se necessário.
     */
    private void checkInactiveServers() {
        for (Integer port : new HashSet<>(inactiveServers)) {
            if (isServerAlive(port)) {
                System.out.printf("Servidor na porta %d voltou a ficar ativo. Movendo para ativos.%n", port);
                inactiveServers.remove(port);
                activeServers.add(port);
            } else {
                System.out.printf("Servidor na porta %d ainda está inativo.%n", port);
            }
        }
    }

    /**
     * Verifica se o servidor está ativo (TCP ou UDP).
     *
     * @param port Porta do servidor
     * @return true se o servidor estiver ativo, false caso contrário
     */
    private boolean isServerAlive(int port) {
        return isTCPServerAlive(port) || isUDPServerAlive(port);
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

            // Enviar a mensagem "ping" para o servidor
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("ping");

            // Aguardar e ler a resposta do servidor
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();

            // Verificar se a resposta é "pong"
            return "pong".equalsIgnoreCase(response); // Verifica a resposta esperada
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
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SERVER_TIMEOUT);
            byte[] buf = "ping".getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("localhost", port));
            socket.send(packet);

            DatagramPacket response = new DatagramPacket(buf, buf.length);
            socket.receive(response);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
