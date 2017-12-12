package TCP;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import raft.Protocol;
import static raft.Protocol.Operation.DEL;
import static raft.Protocol.Operation.EXIT;
import static raft.Protocol.Operation.GET;
import static raft.Protocol.Operation.PUT;
import static raft.Protocol.Operation.STORE;
import raft.kvstore;

public class Server {

    private final kvstore store;
    private final String Addr;
    private final int portNumber;
    private final String mbpAddr;
    private final int mbpPortNumber;

    private ServerSocket serverSocket;
    private volatile static ArrayList<InetSocketAddress> mbpList;

    private boolean then_exit;

    public Server(int portNumber, String mbpAddr, int mbpPortNumber) throws UnknownHostException {
        this.Addr = InetAddress.getLocalHost().getHostAddress();
        this.portNumber = portNumber;
        mbpList = new ArrayList<>();
        this.store = new kvstore();
        this.mbpAddr = mbpAddr;
        this.mbpPortNumber = mbpPortNumber;
    }

    private String exit() {
        then_exit = true;
        return "<the server then exits>";
    }

    private void publish() {
        Client client = new Client(mbpAddr, mbpPortNumber,
                Protocol.Operation.PUT, Protocol.TYPE.MBPSTORE);
        client.runClient(Addr, String.valueOf(portNumber));
    }

    private void unpublish() {
        Client client = new Client(mbpAddr, mbpPortNumber,
                Protocol.Operation.DEL, Protocol.TYPE.MBPSTORE);
        client.runClient(Addr, String.valueOf(portNumber));
    }

    private void refresh() {
        Client client = new Client(mbpAddr, mbpPortNumber,
                Protocol.Operation.STORE, Protocol.TYPE.MBPSTORE);
        String list = client.runClient(Addr, String.valueOf(portNumber));

        String L[] = list.replaceAll("\n", "").split(":");
        //key:a:value:123:key:b:value:321:
        //key a value 123 key b value 321 $
        mbpList.clear();
        for (int i = 0; i < L.length - 1; i += 4) {
            String addr = L[i + 1];
            int port = Integer.parseInt(L[i + 3]);
            mbpList.add(new InetSocketAddress(addr, port));
        }
    }

    public static void forward2Leader(Protocol.Operation opt, String key, String val, int id) {

        String addr = mbpList.get(id).getHostString();
        int port = mbpList.get(id).getPort();
        Client client = new Client(addr, port,
                opt, Protocol.TYPE.RKVSTORE);
        client.runClient(key, val);
    }
    public static ArrayList<InetSocketAddress> getMbpList()
    {
        //return (ArrayList<InetSocketAddress>) mbpList.clone();
        return mbpList;
    }

    public void runServer() throws IOException {

        System.out.println("TCP kvstore server... trying to listen port: " + portNumber);
        class Monitor extends Thread {

            public void run() {
                while (true) {
                    if (then_exit) {
                        unpublish();
                        System.exit(1);
                    }
                    refresh();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        Monitor monitor = new Monitor();
        monitor.start();
        try {
            publish();
            serverSocket = new ServerSocket(portNumber);
            while (!then_exit) {
                new ServerThread(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + portNumber);

        } finally {
            unpublish();
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
