package br.imd.servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.processors.MessageProcessor;
import br.imd.repository.BankManager;

public class TcpServer {
    private BankManager bankManager; // Instância do BankManager

    public TcpServer(String port) throws IOException {
        this.bankManager = new BankManager(); // Inicializa o BankManager
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port));
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 40);
        service(serverSocket, executorService); // Passa o ServerSocket e ExecutorService
    }

    public void service(ServerSocket serverSocket, ExecutorService executorService) {
        System.out.println("Conexão iniciada");
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

    public static void main(String[] args) throws IOException {
        new TcpServer(args[0]); // Inicia o servidor TCP
    }
}

class Handler implements Runnable {
    private Socket socket;
    private BankManager bankManager; // Instância do BankManager

    public Handler(Socket socket, BankManager bankManager) {
        this.socket = socket;
        this.bankManager = bankManager; // Recebe o BankManager
    }

    private BufferedReader getReader(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(in));
    }

    public void run() {
        BufferedReader br = null;
        PrintWriter out = null;

        System.out.println("Nova conexão aceita " + socket.getInetAddress() + ":" + socket.getPort());
        try {
            br = getReader(socket);
            String recv = br.readLine(); // Lê a linha do cliente
            System.out.println("Mensagem recebida: " + recv);

            // Processa a mensagem usando o MessageProcessor
            MessageProcessor messageProcessor = new MessageProcessor();
            String[] parts = messageProcessor.processMessage(recv); // Processa a mensagem recebida

            String responseMessage = ""; // Mensagem de resposta
            if (parts != null) {
                // Verifica se a mensagem é uma solicitação de transferência ou saque
                if (parts[0].equalsIgnoreCase("TRANSFERIR") && parts.length == 7) {
                    // Transferir-Banco1-Agencia1-Conta1-Banco2-Agencia2-Conta2
                    String bancoOrigemNome = parts[1];
                    String agenciaOrigem = parts[2];
                    String contaNumOrigem = parts[3];
                    String bancoDestinoNome = parts[4];
                    String agenciaDestino = parts[5];
                    String contaNumDestino = parts[6];
                    double valor = Double.parseDouble(parts[7]); // Supondo que o valor seja o último argumento

                    // Chama o método transferir do BankManager
                    boolean success = bankManager.transferir(bancoOrigemNome, agenciaOrigem, contaNumOrigem,
                            bancoDestinoNome, agenciaDestino, contaNumDestino, valor);
                    responseMessage = success ? "Transferência realizada com sucesso." : "Falha na transferência.";
                } else if (parts[0].equalsIgnoreCase("SACAR") && parts.length == 5) {
                    // Sacar-Banco1-Agencia1-Conta1
                    String bancoNome = parts[1];
                    String agencia = parts[2];
                    String contaNum = parts[3];
                    double valor = Double.parseDouble(parts[4]); // Supondo que o valor seja o último argumento

                    // Chama o método sacar do BankManager
                    try {
                        bankManager.sacar(bancoNome, agencia, contaNum, valor);
                        responseMessage = "Saque realizado com sucesso.";
                    } catch (Exception e) {
                        responseMessage = "Falha ao realizar o saque: " + e.getMessage();
                    }
                } else {
                    responseMessage = "Comando inválido.";
                }
            } else {
                responseMessage = "Mensagem malformada.";
            }

            // Envia a resposta de volta ao cliente
            out = new PrintWriter(socket.getOutputStream());
            out.println(responseMessage); // Usando println para adicionar a quebra de linha
            out.flush();

        } catch (IOException e) {
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
