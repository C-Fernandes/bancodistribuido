package servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer implements RequestHandler {
    @Override
    public void handleRequest(String request) {
        System.out.println("Handling TCP request: " + request);
    }

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final int POOL_SIZE = 40;

    public TcpServer(String port) throws IOException {
        serverSocket = new ServerSocket(Integer.parseInt(port));
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);
    }

    public TcpServer() {
    }

    public void service() {
        System.out.println("Conexão iniciada");
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

    public static void main(String[] args) throws IOException {
        new TcpServer(args[0]).service();
    }
}

class Handler implements Runnable {
    private Socket socket = null;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    private BufferedReader getReader(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(in));
    }

    public void run() {
        BufferedReader br = null;
        PrintWriter out = null;

        System.out.println("New connection accepted " + socket.getInetAddress() + ":" + socket.getPort());
        try {
            br = getReader(socket);
            String recv = null;
            recv = br.readLine();

            System.out.println(recv);
            out = new PrintWriter(socket.getOutputStream());
            out.write("Hello Client Number:" + recv + "\r\n");
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
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
