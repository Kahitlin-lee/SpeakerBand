package com.speakerband.wifibuddy;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of a ServerSocket handler. This is used by the Wi-Fi P2P group owner.
 */
public class OwnerSocketHandler extends Thread {

    private ServerSocket serverSocket = null;
    private final int THREAD_COUNT = 10;
    private Handler handler;
    private static final String TAG = WifiDirectHandler.TAG + "Ownerw";

    /**
     * Cosntructor
     * @param handler
     * @throws IOException
     */
    public OwnerSocketHandler(Handler handler) throws IOException {
        try {
            serverSocket = new ServerSocket(WifiDirectHandler.SERVER_PORT);
            this.handler = handler;
            Log.i(TAG, "Se inició el socket del servidor propietario del grupo");
        } catch (IOException e) {
            Log.e(TAG, " socket");
            Log.e(TAG, e.getMessage());
            pool.shutdownNow();
            throw e;
        }
    }

    /**
     * A ThreadPool para los sockets clientes.
     * Ejecuta cada tarea enviada usando uno de posiblemente varios subprocesos agrupados.
     */
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    /**
     * Tira el Hilo
     */
    @Override
    public void run() {
        Log.i(TAG, "Hilo de socket del servidor propietario del grupo ejecutándose");
        while (true) {
            try {
                // Una operación de bloqueo. Iniciar una instancia de CommunicationManager cuando
                // hay una nueva conexión
                pool.execute(new CommunicationManager(serverSocket.accept(), handler));
                Log.i(TAG, "iniciando el handler I/O ");
            } catch (IOException e) {
                Log.e(TAG, "Error iniciando handler I/O ");
                Log.e(TAG, e.getMessage());
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                        Log.i(TAG, "Server socket cerrado");
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "Error cerrando el socket");
                    Log.e(TAG, ioe.getMessage());
                }
                pool.shutdownNow();
                break;
            }
        }
    }
}
