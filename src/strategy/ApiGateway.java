package strategy;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiGateway {
    private static final int GATEWAY_PORT = 8999; // Porta única para TCP e UDP
    private static final int[] SERVER_PORTS = { 9000, 9001, 9002 }; // Servidores backend
    private static AtomicInteger serverIndex = new AtomicInteger(0); // Índice para Round-Robin
    private static final int THREAD_POOL_SIZE = 40; // Defina um tamanho de pool adequado

    public static void main(String[] args) throws IOException {
        ApiGateway gateway = new ApiGateway();
        gateway.start();
    }

    // Método para iniciar o ApiGateway
    public void start() {
        System.out.println("Gateway escutando TCP e UDP na porta " + GATEWAY_PORT);

        // ExecutorService para gerenciar o pool de threads
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // ExecutorService também para TCP
        executor.submit(() -> handleTcpConnections(executor));

        // Thread para lidar com pacotes UDP
        executor.submit(() -> handleUdpConnections(executor));
    }

    // Método para lidar com as conexões TCP
    private void handleTcpConnections(ExecutorService executor) {
        try (ServerSocket tcpSocket = new ServerSocket(GATEWAY_PORT)) {
            while (true) {
                Socket clientSocket = tcpSocket.accept();
                System.out.println("Nova conexão TCP recebida. Processando...");

                // Submeter o processamento TCP ao ExecutorService
                executor.submit(new GatewayHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para lidar com pacotes UDP
    private void handleUdpConnections(ExecutorService executor) {
        try (DatagramSocket udpSocket = new DatagramSocket(GATEWAY_PORT)) {
            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(receivePacket); // Recebe o pacote UDP
                System.out.println("Pacote UDP recebido. Processando...");

                // Submete a tarefa UDP ao ExecutorService
                executor.submit(() -> processUdpRequest(receivePacket, udpSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para processar a requisição UDP
    private void processUdpRequest(DatagramPacket receivePacket, DatagramSocket udpSocket) {
        try {
            String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Requisição UDP recebida: " + request);

            // Cria um novo DatagramSocket para enviar dados ao servidor backend
            try (DatagramSocket serverUdpSocket = new DatagramSocket()) {
                byte[] sendData = request.getBytes(); // Converte a requisição para bytes
                InetAddress serverAddress = InetAddress.getByName("localhost"); // Endereço do servidor backend

                // Escolhe uma porta de servidor backend disponível usando Round-Robin
                int serverPort = getAvailableServerPort();

                // Cria um DatagramPacket com os dados a serem enviados ao servidor backend
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                serverUdpSocket.send(sendPacket); // Envia o pacote ao servidor backend

                // Prepara o buffer para receber a resposta
                byte[] receiveData = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(receiveData, receiveData.length);
                serverUdpSocket.receive(responsePacket); // Recebe a resposta do servidor backend
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim(); // Converte
                                                                                                              // a
                                                                                                              // resposta
                                                                                                              // para
                                                                                                              // string

                // Cria um DatagramPacket para enviar a resposta de volta ao cliente
                DatagramPacket clientResponsePacket = new DatagramPacket(response.getBytes(), response.length(),
                        receivePacket.getAddress(), receivePacket.getPort());
                udpSocket.send(clientResponsePacket); // Envia a resposta ao cliente via UDP
            }
        } catch (IOException e) {
            e.printStackTrace(); // Imprime a pilha de erro em caso de exceção
        }
    }

    // Classe interna que trata conexões TCP (inclui HTTP)
    class GatewayHandler implements Runnable {
        private Socket clientSocket;

        public GatewayHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String request = in.readLine(); // Ler a requisição do cliente

                // Detectar se é HTTP ou apenas TCP puro
                if (request.startsWith("GET") || request.startsWith("POST")) {
                    handleHttpRequest(request);
                } else {
                    handleTcpRequest();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Método que trata requisições HTTP
        private void handleHttpRequest(String request) throws IOException {
            System.out.println("Requisição HTTP detectada: " + request);
            processServerRequest(request);
        }

        // Método que trata requisições TCP
        private void handleTcpRequest() throws IOException {
            System.out.println("Requisição TCP detectada.");
            processServerRequest("Requisição TCP recebida");
        }

        // Método genérico para processar requisições de servidores backend
        private void processServerRequest(String request) throws IOException {
            Socket serverSocket = null;
            boolean success = false;

            // Tentativas de reconexão
            for (int attempts = 0; attempts < SERVER_PORTS.length && !success; attempts++) {
                try {
                    serverSocket = new Socket();
                    serverSocket.connect(new InetSocketAddress("localhost", getAvailableServerPort()), 5000); // Timeout
                                                                                                              // de 5s
                    PrintWriter outToServer = new PrintWriter(serverSocket.getOutputStream(), true);
                    outToServer.println(request);

                    BufferedReader serverResponse = new BufferedReader(
                            new InputStreamReader(serverSocket.getInputStream()));
                    String response = serverResponse.readLine();

                    PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                    outToClient.println("Resposta do servidor: " + response);
                    success = true;
                } catch (IOException e) {
                    System.err.println("Erro ao conectar ao servidor: " + e.getMessage());
                } finally {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                }

                // Mover para o próximo servidor
                if (!success) {
                    serverIndex.getAndUpdate(i -> (i + 1) % SERVER_PORTS.length);
                }
            }

            if (!success) {
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                outToClient.println("Erro: Nenhum servidor disponível.");
            }
        }
    }

    // Método para escolher um servidor disponível usando Round-Robin
    private int getAvailableServerPort() {
        return SERVER_PORTS[serverIndex.getAndIncrement() % SERVER_PORTS.length]; // Round-Robin
    }
}
