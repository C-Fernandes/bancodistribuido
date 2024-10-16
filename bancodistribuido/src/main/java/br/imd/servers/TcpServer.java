package br.imd.servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
        this.port = port;
        ServerSocket serverSocket = new ServerSocket(port);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 60);
        service(serverSocket, executorService);
    }

    public void service(ServerSocket serverSocket, ExecutorService executorService) {
        System.out.println("Conexão iniciada");

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
        try (Socket socket = new Socket("localhost", 2)) {
            String message = Integer.toString(port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (IOException e) {
            System.err.println("Erro ao enviar a porta via TCP: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        new TcpServer(Integer.parseInt(args[0]));
    }
}

class Handler implements Runnable {
    private Socket socket;
    private BankActionHandler actionHandler;
    private MessageProcessor messageProcessor;

    public Handler(Socket socket) {
        this.socket = socket;
        this.actionHandler = new BankActionHandler();
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

            String recv = br.readLine();

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
            System.out.println("mensagem enviada:" + responseMessage);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(responseMessage);

        } catch (IOException e) {
            e.printStackTrace();
            responseMessage = "Erro de I/O: " + e.getMessage();
            if (out != null) {
                out.println(responseMessage);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            responseMessage = "Erro de SQL: " + e.getMessage();
            if (out != null) {
                out.println(responseMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage = "Erro inesperado: " + e.getMessage();
            if (out != null) {
                out.println(responseMessage);
            }
        } finally {
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
