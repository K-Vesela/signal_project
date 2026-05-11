package com.cardio_generator.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * A {@link OutputStrategy} that transmits patient health data to a connected TCP client
 * on construction, a server socket is opened on the specified port and listens for an
 * incoming client connection in a background thread. Once a client connects, a call to
 * {@link #output} sends the data to that client as a comma-separated text.
 */
public class TcpOutputStrategy implements OutputStrategy {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;

    /**
     * Constructs a TcpOutputStrategy that looks for a TCP client on the specified port.
     * The server accepts the client connection asynchronously on a separate thread,
     * so this constructor returns immediately without blocking.
     *
     * @param port the TCP port number on which the server looks for a client connection
     */
    public TcpOutputStrategy(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server started on port " + port);

            // Accept clients in a new thread to not block the main thread
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    clientSocket = serverSocket.accept();
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *Send the patient health data to the connected TCP client as a comma-separated string.
     * The message format is: {@code patientId, timestamp, label, data}
     * If on client is currently connected, this method does nothing.
     *
     * @param patientId unique identifier for the patient
     * @param timestamp the time in which the data was generated
     * @param label the type of health data being generated (for example, "Saturation" or "Alert"
     * @param data the data value to be the output, represented as a string
     */
    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        if (out != null) {
            String message = String.format("%d,%d,%s,%s", patientId, timestamp, label, data);
            out.println(message);
        }
    }
}
