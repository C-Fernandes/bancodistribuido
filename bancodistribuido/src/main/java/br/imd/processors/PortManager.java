package br.imd.processors;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PortManager {
    private final Queue<Integer> freePorts;
    private final Queue<Integer> occupiedPorts;
    private int endPort;

    public PortManager(int startPort, int endPort) {
        this.endPort = endPort;
        freePorts = new ConcurrentLinkedQueue<>();
        occupiedPorts = new ConcurrentLinkedQueue<>();
        for (int port = startPort; port <= endPort; port++) {
            freePorts.add(port);
        }
    }

    public int allocatePort() {
        Integer port = freePorts.poll(); // Remove uma porta livre
        if (port != null) {
            occupiedPorts.add(port); // Adiciona à lista de ocupadas
        }

        // Verifica se a porta alocada é maior que o endPort
        if (port != null && port > endPort) {
            System.out.println("Porta alocada excede o limite, reiniciando para a primeira porta livre.");
            port = freePorts.peek(); // Define a porta como a primeira porta livre
        }

        return port != null ? port : -1; // Retorna -1 se não houver porta livre
    }

    public void releasePort(int port) {
        if (occupiedPorts.remove(port)) {
            freePorts.add(port); // Adiciona à lista de livres novamente
        }
    }

    // Método para retornar o valor de endPort

}
