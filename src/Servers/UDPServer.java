package Servers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServer {
    public UDPServer(String port) {
        System.out.println("UDP server started");
        try {
            DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(port));
            byte[] replyMsg;
            while (true) {
                byte[] receiveMessage = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
                serverSocket.receive(receivePacket);
                String message = new String(receivePacket.getData());
                System.out.println("Received message: " + message + "\nFrom: " + receivePacket);
                String reply = "Confirmo recebimento de " + message;
                replyMsg = reply.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(replyMsg, replyMsg.length, receivePacket.getAddress(),
                        receivePacket.getPort());
                serverSocket.send(sendPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("UDP server terminating");
    }

    public static void main(String[] args) {
        new UDPServer("9003");
    }
}
