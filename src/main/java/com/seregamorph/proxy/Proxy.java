package com.seregamorph.proxy;

import com.seregamorph.proxy.escape.EscapeUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Simple logging proxy
 *
 * java -jar target/proxy-1.0-SNAPSHOT.jar localhost:9161 localhost:9160 tc
 * java -jar proxy-1.0-SNAPSHOT.jar localhost:9041 localhost:9042 tc
 */
public class Proxy {
    private final boolean logTimestamp;
    private final boolean logContent;
    private final SocketAddress bindAddress;
    private final SocketAddress remoteAddress;

    public Proxy(String[] args) {
        String[] bindPair = args[0].split(":");
        String[] remotePair = args[1].split(":");
        String options = args.length >= 3 ? args[2] : "";

        this.bindAddress = new InetSocketAddress(bindPair[0], Integer.parseInt(bindPair[1]));
        this.remoteAddress = new InetSocketAddress(remotePair[0], Integer.parseInt(remotePair[1]));

        this.logTimestamp = options.contains("t");
        this.logContent = options.contains("c");
    }

    private void execute() throws IOException {
        ServerSocket server = new ServerSocket();
        server.bind(bindAddress);
        log("Bound server " + bindAddress);

        Socket acceptedSocket;
        int connCounter = 0;
        while ((acceptedSocket = server.accept()) != null) {
            int connNumber = ++connCounter;
            log("Accepted [" + connNumber + "]" +
                    " local:" + acceptedSocket.getLocalAddress() +
                    " remote:" + acceptedSocket.getRemoteSocketAddress());

            InputStream inAccepted = acceptedSocket.getInputStream();
            OutputStream outAccepted = acceptedSocket.getOutputStream();

            Socket remoteSocket = new Socket();
            remoteSocket.connect(remoteAddress);
            log("Connected [" + connNumber + "] to remote " + remoteAddress);
            InputStream inRemote = remoteSocket.getInputStream();
            OutputStream outRemote = remoteSocket.getOutputStream();

            new Thread(pipe(inAccepted, outRemote, connNumber, "++++")).start();
            new Thread(pipe(inRemote, outAccepted, connNumber, "----")).start();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }

        new Proxy(args).execute();
    }

    private Runnable pipe(final InputStream in, final OutputStream out, final int connNumber, final String direction) {
        return new Runnable() {
            @Override
            public void run() {
                byte[] bb = new byte[8192];
                try {
                    int read;
                    while ((read = in.read(bb, 0, bb.length)) >= 0) {
                        String content = logContent ? EscapeUtils.escape(new String(bb, 0, read, "utf-8")) : "";
                        log(direction + " [" + connNumber + "] read  " + read + " bytes " + content);
                        out.write(bb, 0, read);
                        log(direction + " [" + connNumber + "] write " + read + " bytes");
                    }
                    log(direction + " [" + connNumber + "] closed ");
                } catch (IOException e) {
                    log(direction + " [" + connNumber + "] Error in connection " + e.toString());
                } finally {
                    close(in);
                    close(out);
                }
            }
        };
    }

    private synchronized void log(String msg) {
        if (logTimestamp) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String timestamp = df.format(System.currentTimeMillis());
            msg = timestamp + " " + msg;
        }
        System.out.println(msg);
    }

    private static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
        }
    }

    private static void printUsage() {
        System.out.println("java -jar proxy.jar <bind_address:bind_port> <remote_host:remote_port> [options]");
        System.out.println("option t log timestamp");
        System.out.println("option c log content");
    }
}
