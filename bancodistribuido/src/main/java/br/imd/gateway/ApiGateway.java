package br.imd.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import br.imd.processors.PortManager;

public class ApiGateway {
    private static final int GATEWAY_PORT = 8999;
    private static final int[] SERVER_PORTS = { 9000, 9001, 9002 };
    private static final Set<Integer> activeServers = ConcurrentHashMap.newKeySet(); // Concorrência melhorada
    private static final AtomicInteger serverIndex = new AtomicInteger(0);
    private static final int TIMEOUT = 10000; // Timeout de 10 segundos
    private PortManager portManager;

    public ApiGateway() {
        this.portManager = new PortManager(2000, 4000);
    }

    public void start() {
        System.out.println("Gateway escutando TCP e UDP na porta " + GATEWAY_PORT);

        // Inicia a verificação de heartbeat dos servidores
        new Thread(new Heartbeat(8000, activeServers)).start();

        // Usando um pool de threads fixo para controlar o número de requisições
        ExecutorService executor = new ThreadPoolExecutor(
                100, // core pool size
                300, // maximum pool size
                60L, TimeUnit.SECONDS, // keep-alive time
                new LinkedBlockingQueue<Runnable>(1000), // work queue com capacidade
                new ThreadPoolExecutor.AbortPolicy() // AbortPolicy para rejeitar requisições quando o pool estiver
                                                     // cheio
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
                System.out.println("Requisição TCP recebida");
                executor.submit(new GatewayHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace(); // Imprimir o stack trace da exceção
        }
    }

    private void startUdpServer(ExecutorService executor) {
        try (DatagramSocket udpSocket = new DatagramSocket(GATEWAY_PORT)) {
            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(receivePacket);
                executor.submit(() -> processUdpRequest(receivePacket, udpSocket));
            }
        } catch (IOException e) {
            e.printStackTrace(); // Imprimir o stack trace da exceção
        }
    }

    private void processUdpRequest(DatagramPacket receivePacket, DatagramSocket udpSocket) {
        DatagramSocket serverUdpSocket = null;
        int allocatedPort = -1;
        try {
            String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Recebendo pacote UDP: " + request);
            allocatedPort = portManager.allocatePort();

            if (allocatedPort == -1) {
                System.err.println("Nenhuma porta livre disponível.");
                return;
            }

            serverUdpSocket = new DatagramSocket(allocatedPort);
            byte[] sendData = request.getBytes();
            InetAddress serverAddress = InetAddress.getByName("localhost");

            int availableServerPort = getAvailableServerPort();
            System.out.println("Enviando pacote UDP para porta do servidor: " + availableServerPort);

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress,
                    availableServerPort);
            serverUdpSocket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(receiveData, receiveData.length);
            serverUdpSocket.setSoTimeout(TIMEOUT);

            serverUdpSocket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim();
            System.out.println("Resposta recebida do servidor: " + response);

            DatagramPacket clientResponsePacket = new DatagramPacket(response.getBytes(), response.length(),
                    receivePacket.getAddress(), receivePacket.getPort());
            udpSocket.send(clientResponsePacket);
            System.out.println("Resposta enviada ao cliente.");
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout ao esperar a resposta do servidor.");
        } catch (IOException e) {
            e.printStackTrace(); // Imprimir o stack trace da exceção
        } finally {
            if (allocatedPort != -1) {
                portManager.releasePort(allocatedPort);
            }
            if (serverUdpSocket != null && !serverUdpSocket.isClosed()) {
                serverUdpSocket.close();
            }
        }
    }

    class GatewayHandler implements Callable<Void> {
        private final Socket clientSocket;

        public GatewayHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public Void call() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String request = in.readLine();
                System.out.println("(" + request + ")");
                if (request == null) {
                    return null; // Se não houver requisição, termina a execução
                }

                if (request.startsWith("GET") || request.startsWith("POST")) {
                    System.out.println("Requisição HTTP detectada: " + request);
                    handleHttpRequest(request, outToClient);
                } else {
                    processServerRequest(request, outToClient);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void handleHttpRequest(String request, PrintWriter outToClient) throws IOException {
            processServerRequest(request, outToClient);
        }

        private void processServerRequest(String request, PrintWriter outToClient) throws IOException {
            try (Socket serverSocket = new Socket()) {
                int availableServerPort = getAvailableServerPort();
                System.out.println("Conectando ao servidor na porta: " + availableServerPort);

                serverSocket.connect(new InetSocketAddress("localhost", availableServerPort), 5000);

                PrintWriter outToServer = new PrintWriter(serverSocket.getOutputStream(), true);
                outToServer.println(request); // Envia a requisição ao servidor

                BufferedReader serverResponse = new BufferedReader(
                        new InputStreamReader(serverSocket.getInputStream()));
                String response = serverResponse.readLine(); // Espera pela resposta do servidor
                System.out.println("Response: " + response);

                if (response != null) {
                    outToClient.println("Resposta do servidor: " + response);
                    System.out.println("Resposta enviada ao cliente: " + response);
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Timeout ao esperar a resposta do servidor.");
                // Não envia nada ao cliente neste caso
            } catch (Exception e) {
                e.getMessage();
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
