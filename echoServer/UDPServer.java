package echoServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public abstract class UDPServer implements Runnable {
    private final int bufferSize;
    private final int port;
    private volatile boolean isShutDown = false;

    public UDPServer(int port, int bufferSize) {
        this.port = port;
        this.bufferSize = bufferSize;
    }

    public UDPServer(int port) {
        this(port, 8192);
    }

    public UDPServer() {
        this(12345, 8192);
    }

    public void shutDown() {
        this.isShutDown = true;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[bufferSize];
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setSoTimeout(10000);
            while (!isShutDown) {
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(incoming);
                    this.respond(socket, incoming);
                } catch (IOException e) {
                    System.err.println(e.getMessage() + "\n" + e);
                }
            }
        } catch (SocketException e) {
            System.err.println("Could not bind to port: " + port + "\n" + e);
        }
    }

    protected abstract void respond(DatagramSocket socket, DatagramPacket request) throws IOException;
}
