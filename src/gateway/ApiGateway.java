package gateway;

import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiGateway {
    private static final int GATEWAY_PORT = 8999;
    private static final int[] SERVER_PORTS = { 9000, 9001, 9002 };
    private static final Set<Integer> activeServers = ConcurrentHashMap.newKeySet(); // Concorrência melhorada
    private static final AtomicInteger serverIndex = new AtomicInteger(0);
    private static final int TIMEOUT = 10000; // Aumentado o timeout para 20 segundos

    public static void main(String[] args) {
        ApiGateway gateway = new ApiGateway();
        gateway.start();
    }

    public void start() {
        System.out.println("Gateway escutando TCP e UDP na porta " + GATEWAY_PORT);

        // Adiciona as portas do servidor à lista de servidores ativos
        for (int port : SERVER_PORTS) {
            activeServers.add(port);
        }

        // Inicia a verificação de heartbeat dos servidores
        new Thread(new Heartbeat(activeServers)).start();

        // Usando um pool de threads fixo para controlar o número de
        ExecutorService executor = new ThreadPoolExecutor(
                50, // core pool size
                150, // maximum pool size
                60L, TimeUnit.SECONDS, // keep-alive time
                new LinkedBlockingQueue<Runnable>(500), // work queue com capacidade
                new ThreadPoolExecutor.CallerRunsPolicy() // política de rejeição
        );

        // Inicia o servidor TCP
        new Thread(() -> startTcpServer(executor)).start();

        // Inicia o servidor UDP
        new Thread(() -> startUdpServer(executor)).start();
    }

    private void startTcpServer(ExecutorService executor) {
        try (ServerSocket tcpSocket = new ServerSocket(GATEWAY_PORT)) {
            while (true) {
                Socket clientSocket = tcpSocket.accept();
                System.out.println("Nova conexão TCP recebida. Processando...");
                executor.submit(new GatewayHandler(clientSocket, executor));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startUdpServer(ExecutorService executor) {
        try (DatagramSocket udpSocket = new DatagramSocket(GATEWAY_PORT)) {
            udpSocket.setReceiveBufferSize(2 * 65536);
            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(receivePacket);
                System.out.println("Pacote UDP recebido. Processando...");
                executor.submit(() -> processUdpRequest(receivePacket, udpSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processUdpRequest(DatagramPacket receivePacket, DatagramSocket udpSocket) {
        DatagramSocket serverUdpSocket = null; // Declarar fora do try
        try {
            String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
            serverUdpSocket = new DatagramSocket();

            byte[] sendData = request.getBytes();
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = getAvailableServerPort();
            System.out.println("Encaminhando para: " + serverPort);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            serverUdpSocket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(receiveData, receiveData.length);
            serverUdpSocket.setSoTimeout(TIMEOUT);
            serverUdpSocket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim();
            DatagramPacket clientResponsePacket = new DatagramPacket(response.getBytes(), response.length(),
                    receivePacket.getAddress(), receivePacket.getPort());
            udpSocket.send(clientResponsePacket);
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout ao esperar a resposta do servidor. Liberando a thread.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverUdpSocket != null && !serverUdpSocket.isClosed()) {
                serverUdpSocket.close(); // Fechar o socket se não estiver fechado
            }
            // System.out.println("Thread de processamento UDP liberada."); // Esta linha
            // deve ser alcançada em qualquer
            // caso
        }
    }

    class GatewayHandler implements Callable<Void> {
        private final Socket clientSocket;
        private final ExecutorService executor;

        public GatewayHandler(Socket socket, ExecutorService executor) {
            this.clientSocket = socket;
            this.executor = executor;
        }

        @Override
        public Void call() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String request = in.readLine();

                if (request.startsWith("GET") || request.startsWith("POST")) {
                    System.out.println("Requisição HTTP detectada: " + request);
                    handleHttpRequest(request);
                } else {
                    System.out.println("Requisição TCP detectada.");
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
            return null;
        }

        private void handleHttpRequest(String request) throws IOException {
            processServerRequest(request);
        }

        private void handleTcpRequest() throws IOException {
            processServerRequest("Requisição TCP recebida");
        }

        private void processServerRequest(String request) throws IOException {
            boolean success = false;

            Callable<String> task = () -> {
                try (Socket serverSocket = new Socket()) {
                    serverSocket.connect(new InetSocketAddress("localhost", getAvailableServerPort()), 5000); // Timeout
                                                                                                              // de
                                                                                                              // conexão
                    serverSocket.setSoTimeout(TIMEOUT); // Timeout para leitura

                    PrintWriter outToServer = new PrintWriter(serverSocket.getOutputStream(), true);
                    outToServer.println(request);

                    BufferedReader serverResponse = new BufferedReader(
                            new InputStreamReader(serverSocket.getInputStream()));
                    return serverResponse.readLine();
                } catch (SocketTimeoutException e) {
                    return "Timeout ao esperar a resposta do servidor.";
                } catch (IOException e) {
                    return "Erro ao conectar ao servidor: " + e.getMessage();
                }
            };

            Future<String> future = executor.submit(task);
            try {
                String response = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
                if (response != null) {
                    PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                    outToClient.println("Resposta do servidor: " + response);
                    success = true;
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                outToClient.println("Erro: Timeout. Nenhum servidor respondeu.");
            } catch (Exception e) {
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                outToClient.println("Erro: Nenhum servidor disponível.");
            }
        }
    }

    private int getAvailableServerPort() {
        int index = serverIndex.getAndIncrement() % SERVER_PORTS.length;
        for (int i = 0; i < SERVER_PORTS.length; i++) {
            int currentIndex = (index + i) % SERVER_PORTS.length;
            int port = SERVER_PORTS[currentIndex];
            if (activeServers.contains(port)) {
                return port;
            }
        }
        throw new RuntimeException("Nenhum servidor disponível.");
    }

}
