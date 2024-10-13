package br.imd.servers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import br.imd.processors.MessageProcessor;
import br.imd.repository.BankManager;
import br.imd.entity.Banco;

public class UdpServer {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Por favor, forneça a porta como argumento.");
            return;
        }

        int port = Integer.parseInt(args[0]); // Lê a porta a partir dos argumentos
        BankManager bankManager = new BankManager(); // Instância do BankManager
        MessageProcessor messageProcessor = new MessageProcessor(); // Instância do MessageProcessor

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Servidor UDP escutando na porta " + port);

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if (!message.equals("ping")) {
                    System.out.println("Mensagem recebida: " + message);
                }

                String[] parts = messageProcessor.processMessage(message);
                String responseMessage = "";

                if (parts != null) {
                    String action = parts[0]; // Ação extraída da mensagem

                    try {
                        switch (action.toUpperCase()) {
                            case "TRANSFERIR":
                                if (parts.length == 8) { // Verifica se tem todos os parâmetros necessários
                                    String bancoOrigem = parts[1];
                                    String agenciaOrigem = parts[2];
                                    String contaOrigem = parts[3];
                                    String bancoDestino = parts[4];
                                    String agenciaDestino = parts[5];
                                    String contaDestino = parts[6];
                                    double valor = Double.parseDouble(parts[7]); // O valor deve ser o último parâmetro

                                    // Chama o método de transferência
                                    boolean success = bankManager.transferir(bancoOrigem, agenciaOrigem, contaOrigem,
                                            bancoDestino, agenciaDestino, contaDestino, valor);
                                    responseMessage = success ? "Transferência realizada com sucesso."
                                            : "Falha na transferência.";
                                } else {
                                    responseMessage = "Parâmetros inválidos para transferência.";
                                }
                                break;

                            case "SACAR":
                                if (parts.length == 5) {
                                    String banco = parts[1];
                                    String agencia = parts[2];
                                    String conta = parts[3];
                                    double valor = Double.parseDouble(parts[4]); // O valor deve ser o último parâmetro

                                    // Chama o método de saque
                                    bankManager.sacar(banco, agencia, conta, valor);
                                    responseMessage = "Saque realizado com sucesso.";
                                } else {
                                    responseMessage = "Parâmetros inválidos para saque.";
                                }
                                break;

                            case "CRIAR_CONTA":
                                if (parts.length == 5) {
                                    String banco = parts[1];
                                    String agencia = parts[2];
                                    String numeroConta = parts[3];
                                    double saldoInicial = Double.parseDouble(parts[4]); // O saldo inicial deve ser o
                                                                                        // último parâmetro

                                    // Chama o método de criar conta
                                    boolean success = bankManager.criarConta(banco, agencia, numeroConta, saldoInicial);
                                    responseMessage = success ? "Conta criada com sucesso."
                                            : "Falha na criação da conta.";
                                } else {
                                    responseMessage = "Parâmetros inválidos para criação de conta.";
                                }
                                break;

                            case "CRIAR_BANCO": // Nova ação para criar banco
                                if (parts.length == 2) {
                                    String bancoNome = parts[1];

                                    // Chama o método de criar banco
                                    boolean success = bankManager.criarBanco(new Banco(bancoNome));
                                    responseMessage = success ? "Banco criado com sucesso."
                                            : "Falha na criação do banco.";
                                } else {
                                    responseMessage = "Parâmetros inválidos para criação de banco.";
                                }
                                break;

                            default:
                                responseMessage = "Ação não reconhecida.";
                                break;
                        }
                    } catch (Exception e) {
                        responseMessage = "Erro: " + e.getMessage(); // Captura o erro e envia uma mensagem
                    }
                } else {
                    responseMessage = "Mensagem malformada.";
                }

                // Envia a resposta de volta ao cliente
                byte[] sendData = responseMessage.getBytes();
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);

                socket.send(sendPacket);
                if (!message.equals("ping")) {
                    System.out.println("Resposta enviada: " + responseMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
