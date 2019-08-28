package sim.util;

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;

class LogServer extends Thread {
    private ServerSocket ssock;
    private Socket csock;

    private PrintStream out;

    public final int port;

    public LogServer(final int port) throws IOException {
        this.port = port;
        this.ssock = new ServerSocket(port);

        initOutputFile();
        }

    public LogServer() throws IOException {
        this.ssock = new ServerSocket(0);
        this.port = ssock.getLocalPort();

        initOutputFile();
        }

    private void initOutputFile() throws IOException {
        String fn = new SimpleDateFormat("yyyyMMdd-HHmmss").format(Calendar.getInstance().getTime());
        out = new PrintStream(new File("Log-" + fn));
        }

    public void run() {
        while (true) {
            try {
                csock = ssock.accept();
                new LogServerHandler(csock, out).start();
                } catch (IOException e) {
                break;
                }
            }

        if (!ssock.isClosed())
            closeSock();

        out.close();
        }

    public void closeSock() {
        try {
            ssock.close();
            } catch (IOException e) {
            e.printStackTrace();
            }
        }

    public static void main(String args[]) {
        LogServer es = null;
        int port = 5667;
                
        try {
            es = new LogServer(port);
            } catch (IOException e) {
            e.printStackTrace();
            return;
            }

        es.start();
        }
    }

class LogServerHandler extends Thread {
    Socket socket;
    BufferedReader br;
    PrintStream out;

    public LogServerHandler(Socket s, PrintStream out) throws IOException {
        this.socket = s;
        InputStream in = s.getInputStream();
        InputStreamReader isr = new InputStreamReader(in);
        this.br = new BufferedReader(isr);
        this.out = out;
        }

    public void run() {
        String line;
        try {
            while ((line = br.readLine()) != null)
                handle(line);
            socket.close();
            } catch (IOException e) {
            e.printStackTrace();
            }
        }

    private synchronized void handle(String line) {
        System.out.println(line);
        out.println(line);
        }
    }
