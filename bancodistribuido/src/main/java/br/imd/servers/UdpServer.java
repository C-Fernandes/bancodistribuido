package br.imd.servers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import br.imd.processors.BankActionHandler;
import br.imd.processors.MessageProcessor;
import br.imd.service.BankManager;

public class UdpServer {
    private BankManager bankManager;
    private MessageProcessor messageProcessor;
    private BankActionHandler actionHandler; // Adicionando o manipulador de ações
    private ScheduledExecutorService heartbeatExecutor;
    private int serverPort; // Armazenar a porta do servidor

    public UdpServer(int port) {
        this.bankManager = new BankManager();
        this.messageProcessor = new MessageProcessor();
        this.actionHandler = new BankActionHandler(bankManager); // Instanciando o manipulador de ações
        this.serverPort = port; // Armazenando a porta do servidor

        // Iniciar o executor agendado para enviar heartbeat
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 0, 3, TimeUnit.SECONDS); // Envia a porta a cada 3
                                                                                            // segundos

        // Iniciar o servidor UDP
        startUdpServer();
    }

    private void startUdpServer() {
        try (DatagramSocket socket = new DatagramSocket(serverPort)) {
            System.out.println("Servidor UDP escutando na porta " + serverPort);

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Mensagem recebida: " + message);

                String[] parts = messageProcessor.processMessage(message);
                String responseMessage = "-";

                if (parts != null) {
                    String action = parts[0]; // Ação extraída da mensagem
                    responseMessage = actionHandler.handleAction(action, parts); // Chama o manipulador de ações
                } else {
                    responseMessage = "Mensagem malformada.";
                }

                // Envia a resposta de volta ao cliente
                byte[] sendData = responseMessage.getBytes();
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                // System.out.println("Enviando para a porta: " + clientPort);
                socket.send(sendPacket);
                System.out.println("Resposta enviada: " + responseMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendHeartbeat() {
        String heartbeatMessage = Integer.toString(serverPort); // Enviando a porta
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket sendPacket = new DatagramPacket(heartbeatMessage.getBytes(), heartbeatMessage.length(),
                    address, 8000); // Enviar para a porta 8000
            socket.send(sendPacket);
        } catch (Exception e) {
            System.out.println("Falha ao enviar heartbeat: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Lê a porta a partir dos argumentos
        new UdpServer(port);
    }
}
