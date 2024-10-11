package servers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpServer {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Por favor, forneça a porta como argumento.");
            return;
        }

        int port = Integer.parseInt(args[0]); // Lê a porta a partir dos argumentos

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
                String responseMessage = message.toUpperCase();
                if (message.equals("ping")) {
                    responseMessage = "pong"; // Exemplo de processamento
                }
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
