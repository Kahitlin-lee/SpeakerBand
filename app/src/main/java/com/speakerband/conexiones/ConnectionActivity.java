package com.speakerband.conexiones;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.speakerband.ClaseApplicationGlobal;
import com.speakerband.R;
import com.speakerband.conexiones.dialogos.AyudaConectionDialogFragment;
import com.speakerband.conexiones.dialogos.LogsDialogFragment;
import com.speakerband.conexiones.fragments.ChatFragment;
import com.speakerband.conexiones.fragments.MainFragment;
import com.speakerband.conexiones.fragments.SongsFragment;
import com.speakerband.conexiones.network.Message;
import com.speakerband.conexiones.network.MessageType;
import com.speakerband.wifibuddy.CommunicationManager;
import com.speakerband.wifibuddy.DnsSdService;
import com.speakerband.wifibuddy.WiFiDirectHandlerAccessor;
import com.speakerband.wifibuddy.WifiDirectHandler;

import org.apache.commons.lang3.SerializationUtils;

import static com.speakerband.ClaseApplicationGlobal.controllerSongFragmen;
import static com.speakerband.ClaseApplicationGlobal.estaEnElFragmentChat;
import static com.speakerband.ClaseApplicationGlobal.estaEnElFragmentMain;
import static com.speakerband.ClaseApplicationGlobal.estaEnElFragmentSong;
import static com.speakerband.ClaseApplicationGlobal.getContext;
import static com.speakerband.ClaseApplicationGlobal.listSelectionClinteParaReproducir;
import static com.speakerband.ClaseApplicationGlobal.musicService;
import static com.speakerband.ClaseApplicationGlobal.sourceDeviceName;
import static com.speakerband.ClaseApplicationGlobal.sourceDeviceNameOtroMovil;
import static com.speakerband.ClaseApplicationGlobal.soyElLider;
import static com.speakerband.ClaseApplicationGlobal.yaSePreguntoQuienEsElLider;
import static com.speakerband.ClaseApplicationGlobal.yaSePreguntoQuienEsElLiderCliente;


/**
 * Created by Catalina Saavedra
 * Actividad que  es un contenedor para Fragment y la ActionBar.
 * Contiene WifiDirectHandler, que es el service
 * MainActivity tiene Communication BroadcastReceiver to handle Intents diparado desde WifiDirectHandler.
 * Es la activity que inicializa la conexion entre los moviles
 */
public class ConnectionActivity extends AppCompatActivity implements WiFiDirectHandlerAccessor
{
    private WifiDirectHandler wifiDirectHandler;
    private boolean wifiDirectHandlerBound = false;

    private ChatFragment chatFragment = null;
    private SongsFragment songsFragment = null;
    private LogsDialogFragment logsDialogFragment;
    private AyudaConectionDialogFragment ayudaConectionDialogFragment;
    private MainFragment mainFragment;

    private TextView deviceInfoTextView;
    private static final String TAG = WifiDirectHandler.TAG + "MainActivity";

    private Button abrirChatButton;
    private Button abrirCancionesButton;
    private LinearLayout layoutBotones;
    private ClaseApplicationGlobal mApplication;

    private CommunicationReceiver communicationReceiver;
    private Intent intentWifiDirect;

    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;
    private Boolean yoInvite;
    private Boolean mensageDeEstaActivity;

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

        abrirChatButton = (Button) findViewById(R.id.chat);
        abrirCancionesButton = (Button) findViewById(R.id.sincronizarCanciones);
        layoutBotones = (LinearLayout) findViewById(R.id.layoutbotones);
        layoutBotones.setVisibility(View.INVISIBLE);
        deviceInfoTextView = (TextView) findViewById(R.id.thisDeviceInfoTextView);


        // Inicializa ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        mApplication = (ClaseApplicationGlobal) getApplicationContext ();

        // Establecer el Oyente Click para el botón abrirChatButton
        abrirChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "\nSe abre el fragment del chat");
                // se supone que ya exite el objeto , creado al conectarnos
                if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }
                estaEnElFragmentChat = true;
                desapareceLayout();
                replaceFragment(chatFragment);
            }
        });

        // Establecer el Oyente Click para el botón Discover Services
        abrirCancionesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 Log.i(TAG, "\n");
                 if (songsFragment == null) {
                        songsFragment = new SongsFragment();
                    }
                    estaEnElFragmentSong= true;
                    desapareceLayout();
                    replaceFragment(songsFragment);
                    }
                });

        registerCommunicationReceiver();

        yoInvite = false;
        mensageDeEstaActivity = true;
        yaSePreguntoQuienEsElLiderCliente = false;
        yaSePreguntoQuienEsElLider = false;

        if (intentWifiDirect == null)
            intentWifiDirect = new Intent(this, WifiDirectHandler.class);

        if(!wifiDirectHandlerBound) {
            bindService(intentWifiDirect, wifiServiceConnection, BIND_AUTO_CREATE);
        }

        Log.i(TAG, "MainActivity creado");
    }

    /**
     *
     */
    @Override
    protected void onResume()
    {
        Log.i(TAG, "Resuming MainActivity");
        super.onResume();
        Log.i(TAG, "MainActivity resumed");
    }

    /**
     *
     */
    private void desapareceLayout() {
        abrirCancionesButton.setEnabled(false);
        abrirChatButton.setEnabled(false);
        layoutBotones.setVisibility(View.INVISIBLE);
    }

    /**
     *
     */
    private void aparececeLayout() {
        abrirCancionesButton.setEnabled(true);
        abrirChatButton.setEnabled(true);
        layoutBotones.setVisibility(View.VISIBLE);
    }

    /**
     * Configura CommunicationReceiver para recibir intents lanzados de WifiDirectHandler
     * Se utiliza para actualizar la interfaz de usuario y recibir mensajes de comunicación
     */
    private void registerCommunicationReceiver()
    {
        if(communicationReceiver == null)
            communicationReceiver = new CommunicationReceiver();

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
            case R.id.mostrar_barra_control2:
                if(controllerSongFragmen!=null)
                    controllerSongFragmen.show(musicService.getPosn());
                return true;
            case R.id.action_ayuda:
                if(ayudaConectionDialogFragment==null)
                    ayudaConectionDialogFragment  = new AyudaConectionDialogFragment();
                ayudaConectionDialogFragment.show(getFragmentManager(), "ayuda");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     *  Se utiliza para ejecutar WifiDirectHandler como un servicio en lugar de estar acoplado a una
     * Actividad. Esto NO es una conexión a un servicio P2P.
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
            if(mainFragment == null)
                mainFragment = new MainFragment();
            replaceFragment(mainFragment);

            deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());
        }

        /**
         * Se invoca cuando se pierde una conexión al Servicio. Esto
         * sucede cuando el proceso que aloja el servicio se ha bloqueado o ha sido destruido.
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
     * @param service El servicio para conectarse
     */
    public void onServiceClick(DnsSdService service) {
        Log.i(TAG, "\nService List item tapped");

        if (service.getSrcDevice().status == WifiP2pDevice.CONNECTED) {

            layoutBotones.setVisibility(View.VISIBLE);

            // ELiminamos los fragment que ya no usamos de la pila/cola
            if(mainFragment != null)
                getSupportFragmentManager().beginTransaction().remove(mainFragment).commitAllowingStateLoss();

            eliminarTodosFragments();

            if (chatFragment == null) {
                chatFragment = new ChatFragment();
            }

            if (songsFragment == null) {
                songsFragment = new SongsFragment();
            }

            Log.i(TAG, "Conectados");
        } else if (service.getSrcDevice().status == WifiP2pDevice.AVAILABLE) {
            if (sourceDeviceName.equals("")) {
                sourceDeviceName = "otro movil";
            }
            yoInvite = true;
            Toast.makeText(this, "Invitamos " + sourceDeviceName + " A conectarse ", Toast.LENGTH_LONG).show();
            wifiDirectHandler.initiateConnectToService(service);
        } else {
            Log.e(TAG, "Servicio no disponible");
            Toast.makeText(this, "Servicio no disponible\n", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onDestroy() {

        pararServicioAntesCerrar();

        yaSePreguntoQuienEsElLider = false;
        super.onDestroy();
    }

    public void pararServicioAntesCerrar()
    {
        Log.i(TAG, "Destroying MainActivity");
        if (wifiDirectHandlerBound) {
            Log.i(TAG, "WifiDirectHandler service unbound");
            stopService(intentWifiDirect);
            unbindService(wifiServiceConnection);
            wifiDirectHandlerBound = false;
            Log.i(TAG, "MainActivity destroyed");
        }
    }


    /**
     * Cuando el usuario termina de utilizar la actividad subsiguiente y vuelve,
     * Le llega la foto desde el otro activity
     * Con este metodo se coge la info que manda del activity
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
     * Desde el movil que brinda la conexion
     */
    public class CommunicationReceiver extends BroadcastReceiver
    {
        private static final String TAG = WifiDirectHandler.TAG + "CommReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            // Obtenga el intent enviado por WifiDirectHandler cuando se encuentre un servicio
            if (intent.getAction().equals(WifiDirectHandler.Action.SERVICE_CONNECTED)) {
                // Este dispositivo se ha conectado a otro dispositivo que transmite el mismo servicio
                Log.i(TAG, "Service conectado");
                layoutBotones.setVisibility(View.VISIBLE);

                eliminarTodosFragments();

                if(!yaSePreguntoQuienEsElLider && yoInvite)
                    preguntarQuienEsELLider();

                // crea los que si usamos
                if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }

                if (songsFragment == null) {
                    songsFragment = new SongsFragment();
                }

                Log.i(TAG, "Conectados");
            } else if (intent.getAction().equals(WifiDirectHandler.Action.DEVICE_CHANGED)) {
                // This device's information has changed
                Log.i(TAG, "Este dispositivo cambia");
                deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());
            } else if (intent.getAction().equals(WifiDirectHandler.Action.MESSAGE_RECEIVED)) {
                // Se recibió un mensaje del Communication Manager
                Log.i(TAG, "Mensaje recibido");
                if(chatFragment != null &&  estaEnElFragmentChat) {
                    //2º lugar donde pasa cuando llega
                    chatFragment.pullMessage(intent.getByteArrayExtra(WifiDirectHandler.MESSAGE_KEY), context);
                } else if(songsFragment != null && estaEnElFragmentSong) {
                    //2º lugar donde pasa cuando llega
                    songsFragment.pullMessage(intent.getByteArrayExtra(WifiDirectHandler.MESSAGE_KEY), context);
                } else if(mensageDeEstaActivity) {
                    pullMessage(intent.getByteArrayExtra(WifiDirectHandler.MESSAGE_KEY), context);
                    mensageDeEstaActivity = false;
                }

            } else if (intent.getAction().equals(WifiDirectHandler.Action.WIFI_STATE_CHANGED)) {
                //3º lugar donde pasa cuando llega la foto, depues de esto la muestra
                // Wi-Fi ha sido activado o desactivado
                Log.i(TAG, "Wi-Fi state changed");
                //puede q esto joda porque lo elimino antes
                if(mainFragment != null)
                     mainFragment.handleWifiStateChanged();
            }
        }
    }

    public void eliminarFragment()
    {
        int numeroFragments = getSupportFragmentManager().getBackStackEntryCount();
        if( numeroFragments != 0)
            getSupportFragmentManager().popBackStack();
    }

    public void eliminarTodosFragments()
    {
        estaEnElFragmentMain = false;
        int numeroFragments = getSupportFragmentManager().getBackStackEntryCount();
        for (int i=0; i<numeroFragments; i++)
        {// TODO cada tanto peta aca     java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            getSupportFragmentManager().popBackStackImmediate();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    // --- Dejo uno generico para usar en la propia clase Connection
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
        transaction.commitAllowingStateLoss();
    }


    // SOBRE CONEXION ,  LIDER Y dialogo
    /**
     * deserializa lo que llegue
     * 1º lugar donde pasa cuando llega la foto
     * @param readMessage
     * @param context
     */
    public void pullMessage(byte[] readMessage, Context context) {
        Message message = SerializationUtils.deserialize(readMessage);

        switch (message.messageType) {
            case SOY_LIDER:
                Log.i(TAG, "SongPath");
                sourceDeviceNameOtroMovil = (new String(message.content));
                yaSePreguntoQuienEsElLiderCliente = true;
                yaSePreguntoQuienEsElLider = true;
                soyElLider = false;
                mensageDeEstaActivity = false;
                preguntarQuienEsELLider();
                break;
        }
    }

    /**
     * Metodo Llamado después onCreate(Bundle)- o después de onRestart()
     * cuando la actividad se había detenido, pero ahora se muestra nuevamente al usuario.
     * Será seguido por onResume().
     * Queremos iniciar la instancia de Service cuando se inicia la instancia de Activity.
     */
    public void preguntarQuienEsELLider()
    {
        String textoBoton;
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_conection, null);

        alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        final TextView text1 = (TextView) promptsView.findViewById(R.id.textView1);
        final TextView text2 = (TextView) promptsView.findViewById(R.id.textView2);

        if(yaSePreguntoQuienEsElLiderCliente) {
            text2.setVisibility(View.INVISIBLE);
            textoBoton = "Ok";
        } else {
            textoBoton = "Si!!";
        }

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(textoBoton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                if(!yaSePreguntoQuienEsElLiderCliente) {
                                    soyLider(userInput.getText().toString(), true);
                                }
                                yaSePreguntoQuienEsElLider = true;
                                sourceDeviceName = userInput.getText().toString();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                soyElLider = false;
                                soyLider(userInput.getText().toString(), false);
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        alertDialog = alertDialogBuilder.create();

        // TODO ARREGLAR EL PETE DEL DIALOGO
        if(alertDialog!=null)
        {
            if(!((ConnectionActivity)this).isFinishing())

                alertDialog.show();

        }
    }

    /**
     * Metodo que simula la descarga de 4 archivos al pulsar el botón de descarga
     */
    private Thread envioMensajesAlOtroDispositivoParaDescarga(final MessageType tipo, final byte[] contenido)
    {
        Thread thread;
        final CommunicationManager _communicationManager  = getWifiHandler().getCommunicationManager();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    //armamos el mensaje con el tipo de mensaje y la cantidad de bytes en array
                    Message messageEnviar = new Message(tipo, contenido);
                    //lo serializa, esto tambien en los todos casos
                    _communicationManager.write(SerializationUtils.serialize(messageEnviar));
                }catch (Exception e) {
                    Log.e(TAG, "Error" + e.getMessage());
                }
            }
        };

        thread = new Thread(runnable);
        thread.start();

        return thread;
    }


    /**
     * Metodo para el envio de quien es el lider
     */
    private  void soyLider(String nuevoNombre, Boolean canceladoOaceptado){
        byte[] byteArrayPrepararPlay  = (nuevoNombre).getBytes();
        if(canceladoOaceptado)
            soyElLider = true;
        else
            soyElLider = false;

        Toast.makeText(getContext(),
                "Eres el lider del grupo!!", Toast.LENGTH_SHORT).show();

        Thread thread;
        thread =envioMensajesAlOtroDispositivoParaDescarga(MessageType.SOY_LIDER, byteArrayPrepararPlay);
        if (thread != null)
            thread.interrupt();
    }

    @Override
    public void onBackPressed() {

        if (estaEnElFragmentSong) {
            if (songsFragment != null) {
                eliminarFragment();
                aparececeLayout();
                estaEnElFragmentSong = false;
            }
        } else if (estaEnElFragmentChat) {
            if (chatFragment != null) {
                eliminarFragment();
                aparececeLayout();
                estaEnElFragmentChat = false;
            }
        } else {

            musicService.pausar();
            pararServicioAntesCerrar();

            super.onBackPressed();
            listSelectionClinteParaReproducir.clear();
            yaSePreguntoQuienEsElLider = false;
            finish();
        }
    }
}
