package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import raft.Protocol;
import static raft.Protocol.Operation.*;
import static raft.Protocol.TYPE.*;

/**
 * TCP socket based client which is used to send requests to server
 *
 */
public class Client {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Protocol.Operation opt;
    private String serverResp;
    private Protocol.TYPE type;

    public Client(String addr, int port, Protocol.Operation opt, Protocol.TYPE type) {
        super();
        try {
            this.type = type;
            this.clientSocket = new Socket(addr, port);

            clientSocket.setSoTimeout(1000);

            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            this.opt = opt;
        } catch (IOException e) {
            System.out.println("Client exception caught:" + e.toString());
            System.out.println(e.getMessage());

        }
    }

    public String runClient(String key, String value) {
        try {

            switch (opt) {
                case EXIT:
                    out.println(EXIT.name());
                    this.serverResp = in.readLine();
                    break;
                case DEL:
                    out.println(DEL.name());
                    out.println(key);
                    this.serverResp = in.readLine();
                    break;
                case GET:
                    out.println(GET.name());
                    out.println(key);
                    this.serverResp = in.readLine();
                    break;
                case STORE:
                    out.println(STORE.name());
                    char[] cbuf = new char[1024 * 128];
                    in.read(cbuf, 0, 1024 * 128);
                    this.serverResp = String.valueOf(cbuf);
                    break;
                case PUT:
                    out.println(PUT.name());
                    out.println(key);
                    out.println(value);
                    this.serverResp = in.readLine();
                    break;
                default:
                    break;
            }
            if (type.equals(CLIENT)) {
                System.out.println("server respond:" + serverResp);
            } else {
                System.out.println("From " + clientSocket.getInetAddress() + ":" + serverResp);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
        return serverResp;
    }

    public void closeConnections() {
        try {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
