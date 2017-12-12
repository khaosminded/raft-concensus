
package TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import static raft.Protocol.Operation.DEL;
import static raft.Protocol.Operation.EXIT;
import static raft.Protocol.Operation.GET;
import static raft.Protocol.Operation.PUT;
import static raft.Protocol.Operation.STORE;
import raft.kvstore;

/**
 *
 * @author hanxinlei
 */
public class mbpServer {

    private final kvstore store ;
    private final int portNumber;
    private ServerSocket serverSocket;
    private  boolean then_exit ;

    public mbpServer(int portNumber) {
        this.store= new kvstore();
        this.portNumber = portNumber;
        this.then_exit= false;
    }

    public String exit() {
        then_exit = true;
        return "<the server then exits>";
    }

    public void runMbpServer() throws IOException {
        class Check extends Thread {

            public void run() {
                while (true) {
                    if (then_exit) {
                        System.exit(1);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        Check check = new Check();
        check.start();

        System.out.println("TCP membership server...");
        try {
            serverSocket = new ServerSocket(portNumber);
            while (!then_exit) {

                new ServerThread(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + portNumber);
        } finally {
            serverSocket.close();
        }

    }

    private class ServerThread extends Thread {

        private final Socket clientSocket;

        public ServerThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            try (
                    PrintWriter out
                    = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));) {
                String opt = in.readLine();
                String response = "";
                if (opt.equals(PUT.name())) {
                    String key = in.readLine();
                    String val = in.readLine();
                    store.put(key, val);
                    response = "put key=" + key + "\n";
                } else if (opt.equals(GET.name())) {
                    String key = in.readLine();
                    String val = store.get(key);
                    if (val == null) {
                        response = "invalid_key\n";
                    } else {
                        response = "get key=" + key + " get val=" + val + "\n";
                    }
                } else if (opt.equals(DEL.name())) {
                    String key = in.readLine();
                    store.del(key);
                    response = "delete key=" + key + "\n";
                } else if (opt.equals(STORE.name())) {
                    response = store.list();
                } else if (opt.equals(EXIT.name())) {
                    response = exit();
                } else {
                    System.out.println("Wrong command received!");
                }
                System.out.println("TO " + clientSocket.getInetAddress() + ":" + response);
                out.print(response);

            } catch (IOException e) {
                System.out.println("Exception caught when listening for a connection");
                System.out.println(e.getMessage());
            }
        }
    }
}
