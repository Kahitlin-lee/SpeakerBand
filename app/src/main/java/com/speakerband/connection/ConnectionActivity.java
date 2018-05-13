package com.speakerband.connection;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.speakerband.R;

import edu.rit.se.wifibuddy.DnsSdService;
import edu.rit.se.wifibuddy.WifiDirectHandler;

/**
 * Actividad que  que es un contenedor para Fragment y la ActionBar.
 * Contiene WifiDirectHandler, que es el service
 * MainActivity tiene Communication BroadcastReceiver to handle Intents diparado desde WifiDirectHandler.
 * Es el activity que inicializa la conexion entre los moviles
 */
public class ConnectionActivity extends AppCompatActivity implements WiFiDirectHandlerAccessor
{
    private WifiDirectHandler wifiDirectHandler;
    private boolean wifiDirectHandlerBound = false;
    private ChatFragment chatFragment = null;
    private LogsDialogFragment logsDialogFragment;
    private MainFragment mainFragment;
    private TextView deviceInfoTextView;
    private static final String TAG = WifiDirectHandler.TAG + "MainActivity";

    /**
     * Establece el diseño de la interfaz de usuario para la actividad.
     * Registra un Communication BroadcastReceiver para que la actividad pueda ser notificada
     * intents para WifiDirectHandler, como Service Connected y Messaged Received.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Creamos MainActivity de wifi Direct");
        setContentView(R.layout.activity_main_connection);

        // Inicializa ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        deviceInfoTextView = (TextView) findViewById(R.id.thisDeviceInfoTextView);

        registerCommunicationReceiver();
        Log.i(TAG, "MainActivity creado");

        Intent intent = new Intent(this, WifiDirectHandler.class);
        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * Configura CommunicationReceiver para recibir intents disparados de WifiDirectHandler
     * Se utiliza para actualizar la interfaz de usuario y recibir mensajes de comunicación
     */
    private void registerCommunicationReceiver()
    {
        CommunicationReceiver communicationReceiver = new CommunicationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiDirectHandler.Action.SERVICE_CONNECTED);
        filter.addAction(WifiDirectHandler.Action.MESSAGE_RECEIVED);
        filter.addAction(WifiDirectHandler.Action.DEVICE_CHANGED);
        filter.addAction(WifiDirectHandler.Action.WIFI_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(communicationReceiver, filter);
        Log.i(TAG, "Communication Receiver registered");
    }

    /**
     * Agrega al  Main Menu de ActionBar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu_connection, menu);
        return true;
    }

    /**
     * Se llama cuando MenuItem cuando main menu esta seleccionado
     * @param item Item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_view_logs:
                // View Logs MenuItem tapped
                if (logsDialogFragment == null) {
                    logsDialogFragment = new LogsDialogFragment();
                }
                logsDialogFragment.show(getFragmentManager(), "dialog");
                return true;
            case R.id.action_exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Esto se usa para ejecutar WifiDirectHandler como un servicio en lugar de estar acoplado a una
     * Actividad. Esto NO es una conexión a un servicio P2P que se está transmitiendo desde un dispositivo
     */
    private ServiceConnection wifiServiceConnection = new ServiceConnection()
    {
        /**
          * Se invocado cuando se ha establecido una conexión con el Servicio, con el IBinder del
          * canal de comunicación al Servicio.
          * @param name El nombre del componente del servicio que se ha conectado
          * @param service El IBinder del canal de comunicación del Servicio
          */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Binding WifiDirectHandler service");
            Log.i(TAG, "ComponentName: " + name);
            Log.i(TAG, "Service: " + service);
            WifiDirectHandler.WifiTesterBinder binder = (WifiDirectHandler.WifiTesterBinder) service;

            wifiDirectHandler = binder.getService();
            wifiDirectHandlerBound = true;
            Log.i(TAG, "WifiDirectHandler service bound");

            // Agregar MainFragment al 'fragment_container' cuando wifiDirectHandler está vinculado
            mainFragment = new MainFragment();
            replaceFragment(mainFragment);

            deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());
        }

        /**
         * Se invoca cuando se pierde una conexión al Servicio. Esto
         * sucede cuando el proceso que aloja el servicio se ha bloqueado o ha sido asesinado.
         * Esto  elimina el ServiceConnection en sí,
         * el enlace al servicio permanecerá activo y recibirá una llamada
         * a onServiceConnected cuando el servicio se ejecuta nuevamente.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiDirectHandlerBound = false;
            Log.i(TAG, "WifiDirectHandler service unbound");
        }
    };

    /**
     * Cambiamos a Fragment en el 'fragment_container'
     * @param fragment Fragment to add
     */
    public void replaceFragment(Fragment fragment)
    {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);

        // Commit de la transaction
        transaction.commit();
    }

    /**
     * Regresa el wifiDirectHandler
     * @return el wifiDirectHandler
     */
    @Override
    public WifiDirectHandler getWifiHandler() {
        return wifiDirectHandler;
    }

    /**
     * Inicia una conexión P2P a un servicio cuando se escucha un ListItem de servicio.
     * Aparece una invitación en el otro dispositivo para aceptar o rechazar la conexión.
     * @param service El servicio para conectarse a
     */
    public void onServiceClick(DnsSdService service) {
        Log.i(TAG, "\nService List item tapped");

        if (service.getSrcDevice().status == WifiP2pDevice.CONNECTED) {
            if (chatFragment == null) {
                chatFragment = new ChatFragment();
            }
            replaceFragment(chatFragment);
            Log.i(TAG, "Cambiamos a Chat fragment");
        } else if (service.getSrcDevice().status == WifiP2pDevice.AVAILABLE) {
            String sourceDeviceName = service.getSrcDevice().deviceName;
            if (sourceDeviceName.equals("")) {
                sourceDeviceName = "otro movil";
            }
            Toast.makeText(this, "Envitamos " + sourceDeviceName + " A conectarse ", Toast.LENGTH_LONG).show();
            wifiDirectHandler.initiateConnectToService(service);
        } else {
            Log.e(TAG, "Servicio no disponible");
            Toast.makeText(this, "Servicio no disponible\n", Toast.LENGTH_LONG).show();
        }
    }

    protected void onPause() {
        super.onPause();
//        Log.i(TAG, "Pausing MainActivity");
//        if (wifiDirectHandlerBound) {
//            Log.i(TAG, "WifiDirectHandler service unbound");
//            unbindService(wifiServiceConnection);
//            wifiDirectHandlerBound = false;
//        }
        Log.i(TAG, "MainActivity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Resuming MainActivity");
//        Intent intent = new Intent(this, WifiDirectHandler.class);
//        if(!wifiDirectHandlerBound) {
//            bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
//        }
        Log.i(TAG, "MainActivity resumed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Starting MainActivity");
//        Intent intent = new Intent(this, WifiDirectHandler.class);
//        bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "MainActivity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Stopping MainActivity");
        // TODO no se si esto dara problemas aca
//        getApplicationContext().unbindService(wifiServiceConnection);
//        if(wifiDirectHandlerBound) {
//            Intent intent = new Intent(this, WifiDirectHandler.class);
//            stopService(intent);
//            unbindService(wifiServiceConnection);
//            wifiDirectHandlerBound = false;
//        }
        Log.i(TAG, "MainActivity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroying MainActivity");
        if (wifiDirectHandlerBound) {
            Log.i(TAG, "WifiDirectHandler service unbound");
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
            Log.i(TAG, "MainActivity destroyed");
        }
    }

    /**
     * Le llega la foto desde el otro activity
     * Con este metodo se coge la info que manda del activity ChatFragment
     * 2º lugar por donde pasa para enviar la foto
     * 4º despues una vez la foto llega al cliente vuelve a pasar por aqui
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.i(TAG, "Image captured");
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            chatFragment.pushImage(imageBitmap);
    }

    /**
     * BroadcastReceiver utilizado para recibir Intents enviados desde el manejador WifiDirect cuando ocurren eventos P2P
     * Utilizado para actualizar la interfaz de usuario y recibir mensajes de comunicación
     */
    public class CommunicationReceiver extends BroadcastReceiver
    {
        private static final String TAG = WifiDirectHandler.TAG + "CommReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the intent sent by WifiDirectHandler when a service is found
            if (intent.getAction().equals(WifiDirectHandler.Action.SERVICE_CONNECTED)) {
                // This device has connected to another device broadcasting the same service
                Log.i(TAG, "Service conectado");
                if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }
                replaceFragment(chatFragment);
                Log.i(TAG, "Cambiando to Chat fragment");
            } else if (intent.getAction().equals(WifiDirectHandler.Action.DEVICE_CHANGED)) {
                // This device's information has changed
                Log.i(TAG, "Este dispositivo cambia");
                deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());
            } else if (intent.getAction().equals(WifiDirectHandler.Action.MESSAGE_RECEIVED)) {
                // A message from the Communication Manager has been received
                Log.i(TAG, "Mensaje recibido");
                if(chatFragment != null) {
                    //2º lugar donde pasa cuando llega
                    chatFragment.pullMessage(intent.getByteArrayExtra(WifiDirectHandler.MESSAGE_KEY), context);
                }
            } else if (intent.getAction().equals(WifiDirectHandler.Action.WIFI_STATE_CHANGED)) {
                //3º lugar donde pasa cuando llega la foto, depues de esto la muestra
                // Wi-Fi ha sido activado o desactivado
                Log.i(TAG, "Wi-Fi state changed");
                mainFragment.handleWifiStateChanged();
            }
        }
    }
}
