package br.imd.servers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import br.imd.processors.BankActionHandler;
import br.imd.processors.MessageProcessor;

public class UdpServer {
    private MessageProcessor messageProcessor;
    private BankActionHandler actionHandler;
    private ScheduledExecutorService heartbeatExecutor;
    private int serverPort;

    public UdpServer(int port) {
        this.messageProcessor = new MessageProcessor();
        this.actionHandler = new BankActionHandler();
        this.serverPort = port;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 3, TimeUnit.SECONDS);

        startUdpServer();
    }

    private void startUdpServer() {
        try (DatagramSocket socket = new DatagramSocket(serverPort)) {
            System.out.println("Servidor UDP escutando na porta " + serverPort);

            while (true) {
                byte[] receiveData = new byte[1024*2];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Mensagem recebida: " + message);

                String[] parts = messageProcessor.processMessage(message);
                String responseMessage;

                if (parts != null && parts.length > 0) {
                    String action = parts[0];

                    // Verifica se é uma transferência
                    if (action.equalsIgnoreCase("TRANSFERIR")) {
                        System.out.println("Redirecionando comando TRANSFERIR para a porta 4 (UDP)");
                        try (DatagramSocket transferSocket = new DatagramSocket()) {
                            InetAddress address = InetAddress.getByName("localhost");
                            byte[] transferData = message.getBytes();

                            // Cria um pacote UDP e envia para a porta 4
                            DatagramPacket transferPacket = new DatagramPacket(transferData, transferData.length, address, 4001);
                            transferSocket.send(transferPacket);

                            // Espera pela resposta do servidor da porta 4
                            byte[] transferResponseData = new byte[1024];
                            DatagramPacket transferResponsePacket = new DatagramPacket(transferResponseData, transferResponseData.length);
                            transferSocket.receive(transferResponsePacket);
                            responseMessage = new String(transferResponsePacket.getData(), 0, transferResponsePacket.getLength());
                        } catch (Exception e) {
                            responseMessage = "Erro ao redirecionar transferência para a porta 4: " + e.getMessage();
                        }
                    } else {
                        // Caso não seja transferência, processa normalmente
                        responseMessage = actionHandler.handleAction(action, parts);
                    }
                } else {
                    responseMessage = "Mensagem malformada.";
                }

                // Envia a resposta de volta para o cliente original
                byte[] sendData = responseMessage.getBytes();
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                socket.send(sendPacket);
                System.out.println("Resposta enviada: " + responseMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void sendHeartbeat() {
        String heartbeatMessage = Integer.toString(serverPort);
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket sendPacket = new DatagramPacket(heartbeatMessage.getBytes(), heartbeatMessage.length(),
                    address, 1);
            socket.send(sendPacket);
        } catch (Exception e) {
            System.out.println("Falha ao enviar heartbeat: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        new UdpServer(port);
    }
}
