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
import br.imd.processors.MessageProcessor;

public class TcpServer {
    private int port;

    public TcpServer(int port) throws IOException {
        this.port = port; // Armazena a porta do servidor
        ServerSocket serverSocket = new ServerSocket(port);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 60);
        service(serverSocket, executorService); // Passa o ServerSocket e ExecutorService
    }

    public void service(ServerSocket serverSocket, ExecutorService executorService) {
        System.out.println("Conexão iniciada");

        // Inicia um agendador para enviar a porta a cada 3 segundos
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::sendTcpPort, 0, 3, TimeUnit.SECONDS);

        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                executorService.execute(new Handler(socket));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendTcpPort() {
        try (Socket socket = new Socket("localhost", 2)) { // Conecta ao endereço e porta desejados
            String message = Integer.toString(port); // Mensagem a ser enviada
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // Flush automático
            out.println(message); // Envia a mensagem com uma quebra de linha
        } catch (IOException e) {
            System.err.println("Erro ao enviar a porta via TCP: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        new TcpServer(Integer.parseInt(args[0])); // Inicia o servidor TCP
    }
}

class Handler implements Runnable {
    private Socket socket;
    private BankActionHandler actionHandler; // Instância do BankActionHandler
    private MessageProcessor messageProcessor;

    public Handler(Socket socket) {
        this.socket = socket;
        this.actionHandler = new BankActionHandler(); // Inicializa o BankActionHandler
        this.messageProcessor = new MessageProcessor();
    }

    private BufferedReader getReader(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(in));
    }

    public void run() {
        BufferedReader br = null;
        PrintWriter out = null;
        String responseMessage = "";

        try {
            br = getReader(socket);

            String recv = br.readLine(); // Lê a linha do cliente

            if (recv == null) {
                responseMessage = "Conexão fechada pelo cliente.";
            }

            System.out.println("Mensagem recebida: (" + recv + ")");
            String[] parts = messageProcessor.processMessage(recv);
            if (parts.length == 0) {
                responseMessage = "Comando inválido.";

            }

            if (responseMessage.equals("")) {
                responseMessage = actionHandler.handleAction(parts[0], parts);
            }
            // Envia a resposta de volta ao cliente
            System.out.println("mensagem enviada:" + responseMessage);
            out = new PrintWriter(socket.getOutputStream(), true); // Flush automático
            out.println(responseMessage); // Usando println para adicionar a quebra de linha

        } catch (IOException e) {
            // Trate erros de I/O e notifique o cliente
            e.printStackTrace();
            responseMessage = "Erro de I/O: " + e.getMessage();
            if (out != null) {
                out.println(responseMessage); // Envia o erro ao cliente antes de fechar a conexão
            }
        } catch (SQLException e) {
            // Trate erros de SQL e notifique o cliente
            e.printStackTrace();
            responseMessage = "Erro de SQL: " + e.getMessage();
            if (out != null) {
                out.println(responseMessage); // Envia o erro ao cliente antes de fechar a conexão
            }
        } catch (Exception e) {
            // Trate qualquer outro tipo de exceção e notifique o cliente
            e.printStackTrace();
            responseMessage = "Erro inesperado: " + e.getMessage();
            if (out != null) {
                out.println(responseMessage); // Envia o erro ao cliente antes de fechar a conexão
            }
        } finally {
            // Fechamento seguro dos recursos
            try {
                if (br != null) {
                    br.close();
                }
                if (out != null) {
                    out.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
