import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClient {
    public static void main(String args[]) {
        Socket conexao = null;
        try {
            conexao = new Socket("localhost", 8080);
            ObjectOutputStream output = new ObjectOutputStream(conexao.getOutputStream());
            MsgTCP msg = new MsgTCP();
            msg.setMem(20);
            output.writeObject(msg);
            output.flush();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conexao != null) {
                    conexao.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
