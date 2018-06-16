package com.speakerband.wifibuddy;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Client Socke tHandler
 */
public class ClientSocketHandler extends Thread {

    private static final int SOCKET_TIMEOUT = 5000;
    private static final String TAG = WifiDirectHandler.TAG + "ClientSocketHandler";
    private Handler handler;
    private InetAddress inetAddress;

    /**
     * Constructor
     * @param handler
     * @param groupOwnerAddress para manipular  direcciones IP , direccion IP con el puerto determinado
     */
    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.inetAddress = groupOwnerAddress;
    }

    /**
     *
     */
    @Override
    public void run() {
        Log.i(TAG, "Socket Cliente el hilo running");
        Socket socket = new Socket();
        try {
            Log.i(TAG, "Abre el  socket cliente ");
            socket.bind(null);
            socket.connect(new InetSocketAddress(inetAddress.getHostAddress(),
                    WifiDirectHandler.SERVER_PORT), SOCKET_TIMEOUT);
            Log.i(TAG, "Dirección de host del socket del cliente - " + inetAddress.getHostAddress());
            Log.i(TAG, "Client socket - " + socket.isConnected());

            Log.i(TAG, "Lanza el I/O handler");
            CommunicationManager communicationManager = new CommunicationManager(socket, handler);
            new Thread(communicationManager).start();
        } catch (IOException e) {
            Log.e(TAG, "Error al enviar I/O handler");
            Log.e(TAG, e.getMessage());
            try {
                socket.close();
                Log.i(TAG, "se cierra el socket Cliente  ");
            } catch (IOException e1) {
                Log.e(TAG, "Error al cerrar el socket client ");
                e1.printStackTrace();
            }
        }
    }
}
