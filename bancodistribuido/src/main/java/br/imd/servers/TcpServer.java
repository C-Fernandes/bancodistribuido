package br.imd.servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import br.imd.processors.BankActionHandler;
import br.imd.service.BankManager;

public class TcpServer {
    private BankManager bankManager; // Instância do BankManager
    private int port; // Porta do servidor

    public TcpServer(int port) throws IOException {
        this.bankManager = new BankManager(); // Inicializa o BankManager
        this.port = port; // Armazena a porta do servidor
        ServerSocket serverSocket = new ServerSocket(port);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 40);
        service(serverSocket, executorService); // Passa o ServerSocket e ExecutorService
    }

    public void service(ServerSocket serverSocket, ExecutorService executorService) {
        System.out.println("Conexão iniciada");

        // Inicia um agendador para enviar a porta a cada 3 segundos
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::sendPort, 0, 3, TimeUnit.SECONDS);

        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                executorService.execute(new Handler(socket, bankManager)); // Passa o BankManager para o Handler
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPort() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String message = Integer.toString(port); // Mensagem a ser enviada
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName("localhost"); // Ou o endereço do receptor

            // Cria o pacote de datagramas
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 8000);
            socket.send(packet); // Envia o pacote
        } catch (IOException e) {
            System.err.println("Erro ao enviar a porta: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        new TcpServer(Integer.parseInt(args[0])); // Inicia o servidor TCP
    }
}

class Handler implements Runnable {
    private Socket socket;
    private BankManager bankManager; // Instância do BankManager
    private BankActionHandler actionHandler; // Instância do BankActionHandler

    public Handler(Socket socket, BankManager bankManager) {
        this.socket = socket;
        this.bankManager = bankManager; // Recebe o BankManager
        this.actionHandler = new BankActionHandler(bankManager); // Inicializa o BankActionHandler
    }

    private BufferedReader getReader(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(in));
    }

    public void run() {
        BufferedReader br = null;
        PrintWriter out = null;
        String responseMessage = ""; // Mensagem de resposta

        try {
            br = getReader(socket);
            String recv = br.readLine(); // Lê a linha do cliente

            if (recv == null) {
                responseMessage = "Conexão fechada pelo cliente.";
                return; // Sai do método se a conexão foi fechada
            }

            System.out.println("Mensagem recebida: (" + recv + ")");
            String[] parts = recv.split(" "); // Supondo que os comandos são separados por espaço

            // Chama o BankActionHandler para processar a ação
            responseMessage = actionHandler.handleAction(parts[0], parts); // O primeiro elemento é a ação

            // Envia a resposta de volta ao cliente
            System.out.println("Enviando resposta: " + responseMessage);
            out = new PrintWriter(socket.getOutputStream(), true); // Flush automático
            out.println(responseMessage); // Usando println para adicionar a quebra de linha

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (out != null) {
                    out.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
