package com.speakerband.conexiones;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.speakerband.MusicController;
import com.speakerband.R;
import com.speakerband.Song;
import com.speakerband.SongCursor;
import com.speakerband.WifiBuddy.CommunicationManager;
import com.speakerband.WifiBuddy.WiFiDirectHandlerAccessor;
import com.speakerband.WifiBuddy.WifiDirectHandler;
import com.speakerband.network.Message;
import com.speakerband.network.MessageType;

import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.speakerband.ClaseAplicationGlobal.listSelection;
import static com.speakerband.ClaseAplicationGlobal.listSelectionClinteParaReproducir;
import static com.speakerband.ClaseAplicationGlobal.musicService;
import static com.speakerband.ClaseAplicationGlobal.soyElLider;
import static com.speakerband.network.MessageType.SONG_START;

public class SongsFragment extends ListFragment implements MediaPlayerControl
{
    //
    private EditText textMessageEditText;
    private SongsFragment.SongMessageAdapter adapter = null;
    private List<String> items = new ArrayList<>();
    private ArrayList<String> messages = new ArrayList<>();

    private ProgressBar progressBar;
    private int progressStatus = 0;

    //instancia de la interfaz WiFiDirectHandlerAccessor
    private WiFiDirectHandlerAccessor handlerAccessor;
    private Toolbar toolbar;

    //variables de los botones
    private ImageButton syncButton;
    private ImageButton playButton;

    private static final String TAG = WifiDirectHandler.TAG + "ListFragment";

    private CommunicationManager _communicationManager;

    private MusicController controller;
    private boolean paused = false, playbackPaused = false, primerReproduccion;


    /**
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.fragment_songs, container, false);

        listSelectionClinteParaReproducir = new ArrayList<>();

        // Boton que envia las canciones
        syncButton = (ImageButton) view.findViewById(R.id.sync_blacco);
        // Boton que le da el play a las canciones
        playButton = (ImageButton) view.findViewById(R.id.playButton);

        // Los botones tendran un orden de uso especifico
        playButton.setEnabled(false);
        syncButton.setEnabled(true);
        //playButton.setColorFilter(Color.argb(255, 118, 118, 118)); // White Tint
        playButton.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.imageview_border, null));
        syncButton.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.imageview_border, null));

        progressBar = (ProgressBar) view.findViewById(R.id.progressBarSong);
        progressBar.setVisibility(View.INVISIBLE);

        //el adaptador es solo usado para los mensajes de texto
        ListView messagesListView = (ListView) view.findViewById(android.R.id.list);
        adapter = new SongsFragment.SongMessageAdapter(getActivity(), android.R.id.text1, items);
        messagesListView.setAdapter(adapter);
        messagesListView.setDividerHeight(0);

        // Evita que el teclado empuje el fragmento y los mensajes hacia arriba y hacia fuera de la pantalla
        messagesListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        messagesListView.setStackFromBottom(true);

        if(_communicationManager == null)
            _communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();

        //evento del boton  enviar una cancion
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                pushSong();
            }
        });

        //evento del boton  reproducir la cancion desde el dispositivo lider
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(playbackPaused)
                {
                    setController(view);  //Creo que con que este en el onResume ya basta, se puede quitar
                    playbackPaused = false;
                }//muestra los controles de reproduccion
                controller.show(0);

                if (_communicationManager != null)
                    preparePlay();
                else {
                    Log.i(TAG, "Los telefonos no estan conectados");
                    Toast.makeText(getContext(),
                            "Los telefonos no estan conectados", Toast.LENGTH_SHORT).show();
                }

            }
        });

        toolbar = (Toolbar) getActivity().findViewById(R.id.mainToolbar);

        setController(view);

        primerReproduccion = true;

        return view;
    }

    /**
     * El fragment se ha adjuntado al Activity
     */
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        try {
            handlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }


    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        toolbar.setTitle("Songs");
        if (paused) {
            setController(getView());
            paused = false;
        }
    }

    /**
     *
     */
    public interface MessageTarget {
        Handler getHandler();
    }

    /**
     * deserializa lo que llegue
     * 1º lugar donde pasa cuando llega la foto
     * @param readMessage
     * @param context
     */
    public void pullMessage(byte[] readMessage, Context context)
    {
        Message message = SerializationUtils.deserialize(readMessage);
        Bitmap bitmap;
        ByteArrayInputStream in;
        Song _song = null;

        switch(message.messageType)
        {
            //cancion
            //Comienza a recibir la cancion
            case SONG_START:
                Log.i(TAG, "SongPath");
                try
                {
                    Song s;
                    in = new ByteArrayInputStream(message.content);
                    ObjectInputStream is = new ObjectInputStream(in);
                    _song = loadSong(is);
                    _song.setProvenincia(true);
                    listSelectionClinteParaReproducir.add(_song);
                    writeSong(_song);
                    //-----
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            //La cancion se termina de llegar
            case SONG_END:
                if (!listSelectionClinteParaReproducir.isEmpty()) {
                    if(soyElLider) {
                        Log.i(TAG, "Han llegado todas las cancion si es que no existien ya en el movil");
                        escribirMenssge("Han llegado todas las cancion si es que no existien ya en el movil \n El boton de sync queda deshabilitado");
                    } else {
                        Log.i(TAG, "Han llegado todas las cancion si es que no existien ya en el movil");
                        escribirMenssge("Han llegado todas las cancion si es que no existien ya en el movil \n Los botones estan deshabilitados");
                    }
                    SongCursor.encontrarLaCancionEnMilista();
                }
                //Actualizamos el Servicio con toda la lista de canciones
                if (musicService != null)
                    musicService.setList(listSelectionClinteParaReproducir);
                break;
            case PREPARE_PLAY:
                prepararMovilParaReploduccionParaPlay(listSelectionClinteParaReproducir);
                break;
            case PLAY:
                playClienteEnPlay(listSelectionClinteParaReproducir);
                break;
            case PREPARADO:
                ponerListaDeCancionesDelLiderEnPlay();
                break;
            case PAUSAR:
                pause();
                break;
            case PASAR_CANCION:
                playNext();
                break;
        }
    }

    /**
     *
     */
    private void cambiarBotonesUnaVezYaTenemosTodasLasCanciones() {
        // Los botones tendran un orden de uso especifico
        playButton.setEnabled(true);
        syncButton.setEnabled(false);
        progressBar.setVisibility(View.INVISIBLE);
    }

    /**
     *
     */
    private void botonesEnablesFalseCliente() {
        // Los botones tendran un orden de uso especifico
        playButton.setEnabled(false);
        syncButton.setEnabled(false);
        escribirMenssge("Eres miembro del grupo \n Disfruta de la musica del lider del grupo");
    }

    // ------METODOS PARA PONER LAS CANCIONES EN PLAY EN ORDEN DE EJECUCION
    /**
     * Cuando s epulsa el boton envia el mensaje al cliente para que pongo en play pause la cancion
     */
    private void preparePlay()
    {
        Thread thread;
        byte[] byteArrayPrepararPlay  = ("preparar play").getBytes();

        escribirMenssge("Preparando Play 1");
        Toast.makeText(getContext(),
                "Preparando Play", Toast.LENGTH_SHORT).show();
        thread = envioMensajesAlOtroDispositivoParaDescarga(MessageType.PREPARE_PLAY, byteArrayPrepararPlay);
        if(thread != null)
            thread.interrupt();
    }

    /**
     * Pone la cancion del cliente en play/pause
     * @param listS
     */
    public void prepararMovilParaReploduccionParaPlay(ArrayList<Song> listS) {
        if(!listS.isEmpty()) {
            if (musicService != null){
                musicService.setList(listS);
                musicService.setSong(listS.get(0));
                start();
                pause();
            }

            Thread thread;
            byte[] byteArrayPrepararPlay = ("preparar play").getBytes();

            escribirMenssge("Preparando Play 2 ");
            Toast.makeText(getContext(),
                    "Preparando Play", Toast.LENGTH_SHORT).show();
            thread = envioMensajesAlOtroDispositivoParaDescarga(MessageType.PREPARADO, byteArrayPrepararPlay);
            if (thread != null)
                thread.interrupt();

        }
    }

    /**
     * Pone en play la cancion en el cliente
     * @param listSelectionClinteParaReproducir
     */
    private void playClienteEnPlay(ArrayList<Song> listSelectionClinteParaReproducir)
    {
        musicService.setList(listSelection);
        if(primerReproduccion) {
            musicService.setSong(listSelectionClinteParaReproducir.get(0));
            primerReproduccion = false;
        }
        musicService.playSong();

        escribirMenssge("Se esta reproduciendo " + listSelectionClinteParaReproducir.get(musicService.getPosn()).getTitle());
    }

    /**
     * Pone la cancion del lider en play
     */
    public void ponerListaDeCancionesDelLiderEnPlay() {
        if(!listSelection.isEmpty()) {
            Thread thread;
            byte[] byteArrayPrepararPlay = ("preparar play").getBytes();
            thread = envioMensajesAlOtroDispositivoParaDescarga(MessageType.PLAY, byteArrayPrepararPlay);
            if (thread != null)
                thread.interrupt();

            if(primerReproduccion) {
                musicService.setSong(listSelection.get(0));
                primerReproduccion = false;
            }

            start();

            escribirMenssge("Se esta reproduciendo " + listSelection.get(musicService.getPosn()).getTitle());

        } else {
            escribirMenssge("No hay canciones que reproducir ");
            Toast.makeText(getContext(),
                    "No hay canciones que reproducir", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Pone la cancion del lider en play
     */
    public void pausarCancion() {
        if(!listSelection.isEmpty()) {
            Thread thread;
            byte[] byteArrayPrepararPlay = ("pausar cancion").getBytes();
            thread = envioMensajesAlOtroDispositivoParaDescarga(MessageType.PAUSAR, byteArrayPrepararPlay);
            if (thread != null)
                thread.interrupt();
            escribirMenssge("Se ha pausado la cancion " + listSelection.get(musicService.getPosn()).getTitle());
        } else {
            escribirMenssge("No hay canciones que reproducir ");
            Toast.makeText(getContext(),
                    "No hay canciones que reproducir", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Pone la cancion del lider en play
     */
    public void pasarDeCancion() {
        if(!listSelection.isEmpty()) {
            Thread thread;
            byte[] byteArrayPrepararPlay = ("pasar de cancion").getBytes();
            thread = envioMensajesAlOtroDispositivoParaDescarga(MessageType.PAUSAR, byteArrayPrepararPlay);
            if (thread != null)
                thread.interrupt();
            escribirMenssge("Se ha pausado la cancion " + listSelection.get(musicService.getPosn()).getTitle());
        } else {
            escribirMenssge("No hay canciones que reproducir ");
            Toast.makeText(getContext(),
                    "No hay canciones que reproducir", Toast.LENGTH_SHORT).show();
        }
    }

    //------------------------

    /**
     * Envia el texto
     * Este metodo se usa en el onClick de el envio de texto
     * e internamente en el otro metodo pushMenssage
     * @param message
     */
    private void pullMessage(String message)
    {
        adapter.add(message);
        adapter.notifyDataSetChanged();
    }

    /**
     *
     * @param message
     */
    public void escribirMenssge(String message){
        pullMessage("" + message);
        messages.add(message);
        Log.i(TAG, "Message: " + message);
    }

    //--------- TODOS LOS METODOS DE OS ENVIOS/RECIBIR DE CANCIONES
    /**
     * Envia la cancion trozo a trozo
     */
    public void pushSong()
    {

        byte[] byteArraySong = null;

        Thread thread;

        //Recorre la lista de seleccion y  envia cancion a cancion
        for(int  x = 0 ; x < listSelection.size() ; x++) {
            // Esto no hay que tacarlo
            listSelection.get(x).readFile();

            // Pasa la cancion que se va a enviar a Bytes
            byteArraySong = convertirObjetoArrayBytes(listSelection.get(x));

            thread = envioMensajesAlOtroDispositivoParaDescarga(SONG_START, byteArraySong);

            if(dormirApp3Segundos(thread)) {
                escribirMenssge("Si la cancion no existia en el otro movil, ha sido enviada. \n Cancion : " + listSelection.get(x).getTitle());
                Toast.makeText(getContext(),
                        "Se ha enviado una cancion" + listSelection.get(x).getTitle(), Toast.LENGTH_SHORT).show();
            } else {
                escribirMenssge("No se ha enviado la cancion : " + listSelection.get(x).getTitle());
                Toast.makeText(getContext(),
                        "No se ha podido enviar la cancion" + listSelection.get(x).getTitle(), Toast.LENGTH_SHORT).show();
            }
        }

        if(listSelection.size() <= 0) {
            escribirMenssge("No hay canciones para enviar. ");
            Toast.makeText(getContext(),
                    "No hay canciones para enviar", Toast.LENGTH_SHORT).show();
            thread = envioMensajesAlOtroDispositivoParaDescarga(MessageType.SONG_END, byteArraySong);
            if(thread != null)
                thread.interrupt();
        } else {
            escribirMenssge("Ya se han sincronizado todas las canciones \n El boton de sync queda deshabilitado ");
            Toast.makeText(getContext(),
                    "Ya se han enviado todas las canciones", Toast.LENGTH_SHORT).show();
            thread = envioMensajesAlOtroDispositivoParaDescarga(MessageType.SONG_END, byteArraySong);
            if(thread != null)
                thread.interrupt();
        }
        cambiarBotonesUnaVezYaTenemosTodasLasCanciones();
    }

    /**
     * Metodo que convierte cualquier objeto en un array de bytes
     * @param object
     * @return
     */
    public byte[] convertirObjetoArrayBytes (Object object)
    {
        //implementa un flujo de salida en el que los datos se escriben en una matriz de bytes
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //convertir la cancion en bytes
        try
        {
            ObjectOutputStream os = new ObjectOutputStream(stream);
            os.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }


    /**
     * Metodo para los mensajes que se iran mostrando sobre la ejecucion de este Fragmen
     */
    public class SongMessageAdapter extends ArrayAdapter<String>
    {
        public SongMessageAdapter(Context context, int textViewResourceId, List<String> items)
        {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View v = convertView;
            if (v == null)
            {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_1, null);
            }
            String message = items.get(position);
            if (message != null && !message.isEmpty()) {
                TextView nameText = (TextView) v.findViewById(android.R.id.text1);
                if (nameText != null) {
                    nameText.setText(message);
                    if (message.startsWith("Me: ")) {
                        // My message
                        nameText.setGravity(Gravity.RIGHT);
                    } else {
                        // Buddy's message
                        nameText.setGravity(Gravity.LEFT);
                    }
                }
            }
            return v;
        }
    }

    /**
     * Comenza a recibir la cancCION
     * lee el objeto
     * @param is
     * @throws IOException
     */
    private Song loadSong(ObjectInputStream is) throws IOException
    {
        Song _song = null;
        try
        {
            _song = (Song)is.readObject();
        } catch (IOException e) {
            e.printStackTrace();//writeabortemexeption
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return _song;
    }

    /**
     * Escribe el array de canciones en memoria
     */
    private void writeSong(Song s)
    {
        try
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(s.getSongBytes());
            s.setSongBytes(outputStream.toByteArray());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //Esto es porque si la cancion ya esta en la carpeta de descargas no se escribe de nuevo y y al no existir y hacer un set peta
        String uri = writeSongOnExternalMemory(s , "Download");
        if(uri != null) {
            s.setUri(uri);
        }
    }

    /**
     * Metodo que escribe un archivo de musica en la memoria externa
     * TODO mejorar esta explicaicon del metodo
     * @param song
     * @param nombreFicheroDondeSeEscribe nombre del fichero donde se escribira la cancion
     * @return
     */
    public String writeSongOnExternalMemory(Song song, String nombreFicheroDondeSeEscribe)
    {
        File path = Environment.getExternalStoragePublicDirectory(nombreFicheroDondeSeEscribe);
        String url = path +"/"+ song.getTitleWithExtension();
        File file = new File(path, song.getTitleWithExtension());
        FileOutputStream fileOutputStream = null; // save

        if ((!file.exists()) && path.exists()) {
            try {
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(song.getSongBytes());
                fileOutputStream.flush();
                fileOutputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Get length of file in bytes
            long fileSizeInBytes = file.length();
            // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
            long fileSizeInKB = fileSizeInBytes / 1024;
            // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
            long fileSizeInMB = fileSizeInKB / 1024;

            escribirMenssge("Se ha guardado la cancion " + song.getTitle() + " en la carpeta de descargas");

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Audio.AudioColumns.TITLE, song.getTitle());
            contentValues.put(MediaStore.Audio.AudioColumns._ID, song.getId());
            contentValues.put(MediaStore.Audio.AudioColumns.ALBUM,  song.getAlbum());
            contentValues.put(MediaStore.Audio.AudioColumns.ARTIST, song.getArtist());
            contentValues.put(MediaStore.Audio.AudioColumns.DATA, url);

            //contentValues.put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, song.getTitleWithExtension());

            // more columns should be filled from here
            Uri uri = getActivity().getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
            Log.d(TAG, uri.toString());

            return file.getAbsolutePath();
        }
        return file.getAbsolutePath();
    }

    // TODOS los metodos que usan hilos Clases.
    /**
     *
     */
    ArrayList <Thread> threadsDeLaClas = new ArrayList();

    /**
     * Metodo que simula la descarga de 4 archivos al pulsar el botón de descarga
     */
    public Thread envioMensajesAlOtroDispositivoParaDescarga(final MessageType tipo,final byte[] contenido)
    {
        Thread thread;

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
        threadsDeLaClas.add(thread);
        thread.start();

        return thread;
    }

    /**
     * Metodo Que duerme el hilo unos segundos.
     */
    public Boolean dormirApp3Segundos(Thread t)
    {
        try {
            Thread.sleep (4000);
            if(!t.isInterrupted()) {
                t.interrupt();
            }
        }
        catch (InterruptedException ex) {
            Log.d ("", ex.toString ());
            return false;
        }
        return true;
    }

    /**
     *
     */
    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focusedView = getActivity().getCurrentFocus();
        // TODO Con esta linea Por fin he arreglado este pete del null, solo espero que no afecte a la conexion
//        if (focusedView != null) {
//            if(textMessageEditText.getWindowToken()== null)
//                imm.hideSoftInputFromWindow(textMessageEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
//        }
    }

    /**
     * Metodo Llamado después onCreate(Bundle)- o después de onRestart()
     * cuando la actividad se había detenido, pero ahora se muestra nuevamente al usuario.
     * Será seguido por onResume().
     * Queremos iniciar la instancia de Service cuando se inicia la instancia de Activity.
     */
    @Override
    public void onStart()
    {
        super.onStart();
        if(!soyElLider) {
            botonesEnablesFalseCliente();
        } else {
            escribirMenssge("Eres el lider del grupo \n ");
        }
    }

    /**
     *
     */
    @Override
    public void onStop() {
        super.onStop();
        //listQueYaHasidoEnviada.clear();
        matarTodosLoshilos();
    }


    /**
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        //listQueYaHasidoEnviada.clear();
        matarTodosLoshilos();
    }

    public void matarTodosLoshilos() {
        for (Thread tr : threadsDeLaClas ) {
            if(tr != null)
                tr.interrupt();
        }
    }


    //Metodo de MusicController ,

    /**
     * Metodo de ayuda para configurar el controlador
     * más de una vez en el ciclo de vida de la aplicación
     */
    private void setController(View convertView)
    {
        // Instanciar el controlador:
        controller = new MusicController(getActivity()){

            //Manejar el BACK button cuando el reproductor de música está activo
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if(event.getKeyCode() == KeyEvent.KEYCODE_BACK){

                    if(controller.isShown()) {
                        controller.hide();//Oculta el mediaController
                    }

                    return true;
                }
                //Si no presiona el back button, pues sigue funcionando igual.
                return super.dispatchKeyEvent(event);
            }
        };
        controller.setMediaPlayer(this);
        controller.setAnchorView(convertView.findViewById(R.id.linear2));
        controller.setEnabled(true);

        controller.setPrevNextListeners(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });


    }

    //  Métodos que llamamos cuando establecemos el controlador:

    /**
     * play next
     */
    private void playNext()
    {
        musicService.playNext();
        if(playbackPaused){
            playbackPaused = false;
        }
        if(soyElLider)
            pasarDeCancion();
        controller.show(0);
    }

    /**
     * play previous
     */
    private void playPrev()
    {
        musicService.playPrev();
        if(playbackPaused){
            playbackPaused = false;
        }
        controller.show(0);
    }

    /**
     *
     */
    @Override
    public void start()
    {
        musicService.pausePlay();
    }

    /**
     *
     */
    @Override
    public void pause()
    {
        playbackPaused = true;
        musicService.pausePlay();
        if(soyElLider)
            pausarCancion();
    }

    /**
     *
     * @return
     */
    @Override
    public int getDuration()
    {
        if(musicService!=null && musicService.isPng())
            return musicService.getDur();
        else return 0;
    }

    /**
     *
     * @return
     */
    @Override
    public int getCurrentPosition()
    {
        if(musicService!=null  && musicService.isPng())
            return musicService.getPosn();
        else return 0;
    }

    /**
     *
     * @param pos
     */
    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isPlaying()
    {
        if(musicService!=null )
            return musicService.isPng();
        return false;
    }

    /**
     *
     * @return
     */
    @Override
    public int getBufferPercentage() {
        return 0;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean canPause() {
        return true;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean canSeekBackward() {
        return true;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}


