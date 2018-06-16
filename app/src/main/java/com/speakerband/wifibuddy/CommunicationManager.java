package com.speakerband.wifibuddy;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Maneja la lectura y escritura de mensajes con buffers de socket. Utiliza un controlador
 * para publicar mensajes en el hilo de la interfaz de usuario.
 */
public class CommunicationManager implements Runnable {

    private Socket socket = null;
    private Handler handler;
    private OutputStream outputStream;
    private static final String TAG = WifiDirectHandler.TAG + "CommManager";

    /**
     * Constructor
     * @param socket
     * @param handler
     */
    public CommunicationManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    /**
     * Metodo run
     */
    @Override
    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            byte[] messageSizeBuffer = new byte[Integer.SIZE/Byte.SIZE];
            int messageSize;
            byte[] buffer;// = new byte[1024];
            int bytes;
            int totalBytes;
            handler.obtainMessage(WifiDirectHandler.MY_HANDLE, this).sendToTarget();

            while (true) {
                try {
                    // Lee el inputStream con el mensage
                    bytes = inputStream.read(messageSizeBuffer);
                    if (bytes == -1) { break; }
                    messageSize = ByteBuffer.wrap(messageSizeBuffer).getInt();
                    Log.i(TAG, "message size: " + messageSize);

                    buffer = new byte[messageSize];
                    bytes = inputStream.read(buffer);
                    totalBytes = bytes;
                    while (bytes != -1 && totalBytes < messageSize) {
                        bytes = inputStream.read(buffer, totalBytes, messageSize - totalBytes);
                        totalBytes += bytes;
                    }
                    if (bytes == -1) { break; }

                    // Enviar los bytes obtenidos a la Activity IU
                    // Log.i(TAG, "Rec:" + Arrays.toString(buffer));  --> Provoca una excepcion de memoria.
                    handler.obtainMessage(WifiDirectHandler.MESSAGE_READ,
                            bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    // Envía un mensaje a WifiDirectHandler para manejar la desconexión
                    handler.obtainMessage(WifiDirectHandler.COMMUNICATION_DISCONNECTED, this).sendToTarget();
                    Log.i(TAG, "Communication disconnected");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error closing socket");
            }
        }
    }

    /**
     * Escribe el mensaje que se obtine por medio de nun array bytes
     * @param message
     */
    public void write(byte[] message) {
        try {
            ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.SIZE/Byte.SIZE);
            byte[] sizeArray = sizeBuffer.putInt(message.length).array();
            byte[] completeMessage = new byte[sizeArray.length + message.length];
            System.arraycopy(sizeArray, 0, completeMessage, 0, sizeArray.length);
            System.arraycopy(message, 0, completeMessage, sizeArray.length, message.length);
            outputStream.write(completeMessage);
        } catch (IOException e) {
            Log.e(TAG, "Exception durante la escritura", e);
        }
    }
}
