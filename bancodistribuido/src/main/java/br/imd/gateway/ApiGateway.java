package br.imd.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import br.imd.processors.PortManager;

public class ApiGateway {
    private static final int GATEWAY_PORT_UDP = 8999;
    private static final int GATEWAY_PORT_TCP = 8998;
    private static final int GATEWAY_PORT_HTTP = 8997;
    private static final Set<Integer> activeServers = ConcurrentHashMap.newKeySet(); // Concorrência melhorada
    private static final AtomicInteger serverIndex = new AtomicInteger(0);
    private static final int TIMEOUT = 10000; // Timeout de 10 segundos
    private PortManager portManager;

    public ApiGateway() {
        this.portManager = new PortManager(2000, 4000);
    }

    public void start() {

        // Inicia a verificação de heartbeat dos servidores
        new Thread(new Heartbeat(activeServers)).start();

        // Usando um pool de threads fixo para controlar o número de requisições
        ExecutorService executor = new ThreadPoolExecutor(
                100, // core pool size
                300, // maximum pool size
                60L, TimeUnit.SECONDS, // keep-alive time
                new LinkedBlockingQueue<Runnable>(1000), // work queue com capacidade
                new ThreadPoolExecutor.AbortPolicy() // AbortPolicy para rejeitar requisições quando o pool estiver
                                                     // cheio
        );

        new Thread(() -> startTcpServer(executor)).start();
        new Thread(() -> startUdpServer(executor)).start();
        new Thread(() -> startHttpServer(executor)).start();
    }

    private void startHttpServer(ExecutorService executor) {
        System.out.println("Gateway escutando HTTP na porta " + GATEWAY_PORT_HTTP);
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(GATEWAY_PORT_HTTP), 0);
            httpServer.createContext("/", exchange -> {
                if ("POST".equals(exchange.getRequestMethod()) || "GET".equals(exchange.getRequestMethod()) || "PUT"
                        .equals(exchange.getRequestMethod())) {
                    executor.submit(() -> {
                        try {
                            handleHttpRequest(exchange);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    String response = "Método HTTP não suportado";
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            });
            httpServer.setExecutor(executor);
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startUdpServer(ExecutorService executor) {
        System.out.println("Gateway escutando UDP na porta " + GATEWAY_PORT_UDP);
        try (DatagramSocket udpSocket = new DatagramSocket(GATEWAY_PORT_UDP)) {
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

    private void startTcpServer(ExecutorService executor) {
        System.out.println("Gateway escutando TCP na porta " + GATEWAY_PORT_TCP);
        try (ServerSocket tcpSocket = new ServerSocket(GATEWAY_PORT_TCP)) {
            while (true) {
                Socket clientSocket = tcpSocket.accept();
                executor.submit(() -> processTcpRequest(clientSocket));

            }
        } catch (IOException e) {
            e.printStackTrace(); // Imprimir o stack trace da exceção
        }
    }

    private void handleHttpRequest(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath(); // Obtém o caminho da requisição
        InputStream requestBody = exchange.getRequestBody();
        byte[] requestData = requestBody.readAllBytes();
        int availableServerPort = getAvailableServerPort();
        String serverUrl = "http://localhost:" + availableServerPort + requestPath; // Adiciona o caminho à URL do
        HttpURLConnection conn = (HttpURLConnection) new URL(serverUrl).openConnection();
        conn.setRequestMethod(exchange.getRequestMethod());
        conn.setDoOutput(true);

        try {
            // Envia o corpo da requisição se existir
            if (requestData.length > 0) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestData);
                    os.flush();
                }
            }
        } catch (Exception e) {
            System.out.println(
                    "Não foi possivel se conectar com o servidor: " + availableServerPort + ". " + e.getMessage());
        }

        // Aqui você formata a URL com base no caminho da requisição

        // Recebe a resposta do servidor
        int responseCode = conn.getResponseCode();
        InputStream responseStream = (responseCode == HttpURLConnection.HTTP_OK) ? conn.getInputStream()
                : conn.getErrorStream();
        byte[] responseData = responseStream.readAllBytes();
        System.out.println("Resposta recebida do servidor: " + responseCode);

        // Envia a resposta de volta ao cliente
        exchange.sendResponseHeaders(responseCode, responseData.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseData);
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
            System.out.println("Terminando processo");
            if (allocatedPort != -1) {
                portManager.releasePort(allocatedPort);
            }
            if (serverUdpSocket != null && !serverUdpSocket.isClosed()) {
                serverUdpSocket.close();
            }
        }
    }

    private void processTcpRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = in.readLine();
            System.out.println("(" + request + ")");
            try (Socket serverSocket = new Socket()) {
                int availableServerPort = getAvailableServerPort();
                System.out.println("Conectando ao servidor na porta: " +
                        availableServerPort);
                serverSocket.connect(new InetSocketAddress("localhost", availableServerPort), 5000);

                PrintWriter outToServer = new PrintWriter(serverSocket.getOutputStream(), true);
                outToServer.println(request);

                BufferedReader serverResponse = new BufferedReader(
                        new InputStreamReader(serverSocket.getInputStream()));
                String response = serverResponse.readLine();
                System.out.println("Response: " + response);

                if (response != null) {
                    outToClient.println("Resposta do servidor: " + response);
                    // System.out.println("Resposta enviada ao cliente: " + response);
                } else {

                    // System.err.println("Resposta do servidor é null.");
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Timeout ao esperar a resposta do servidor.");

            } catch (Exception e) {
                e.getMessage();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getAvailableServerPort() {
        synchronized (activeServers) {
            if (activeServers.isEmpty()) {
                throw new RuntimeException("Nenhum servidor disponível.");
            }

            List<Integer> activeServersList = new ArrayList<>(activeServers);
            int size = activeServersList.size();
            int currentIndex = serverIndex.get();

            int selectedPort = activeServersList.get(currentIndex % size);
            serverIndex.set((currentIndex + 1) % size);
            // System.out.println("porta enviada: " + selectedPort);
            return selectedPort;
        }
    }

}
