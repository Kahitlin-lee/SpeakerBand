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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.speakerband.MainActivity;
import com.speakerband.R;
import com.speakerband.Song;
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
import static com.speakerband.network.MessageType.SONG_START;

public class SongsFragment extends ListFragment
{
    //
    private EditText textMessageEditText;
    private SongsFragment.SongMessageAdapter adapter = null;
    private List<String> items = new ArrayList<>();
    private ArrayList<String> messages = new ArrayList<>();


    //instancia de la interfaz WiFiDirectHandlerAccessor
    private WiFiDirectHandlerAccessor handlerAccessor;
    private Toolbar toolbar;

    //variables de los botones
    private ImageButton sendSongButton;
    private ImageButton playButton;
    private ImageButton sincronizaButton;

    private static final String TAG = WifiDirectHandler.TAG + "ListFragment";

    public CommunicationManager _communicationManager;


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
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        listSelectionClinteParaReproducir = new ArrayList<>();

        // Boton que envia las canciones
        sendSongButton = (ImageButton) view.findViewById(R.id.songButton);
        // Boton que sicroniza las canciones
        sincronizaButton = (ImageButton) view.findViewById(R.id.sincronizaButton);
        // Boton que le da el play a las canciones
        playButton = (ImageButton) view.findViewById(R.id.play);

        // Los botones tendran un orden de uso especifico
        sincronizaButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.INVISIBLE);
        sendSongButton.setVisibility(View.VISIBLE);

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
        sendSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pushSong();
            }
        });

        //evento del boton  reproducir la cancion desde el dispositivo lider
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
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
                    in = new ByteArrayInputStream(message.content);
                    ObjectInputStream is = new ObjectInputStream(in);
                    _song = loadSong(is);
                    writeSong(_song);
                    if(!listSelectionClinteParaReproducir.contains(_song)) {
                        listSelectionClinteParaReproducir.add(_song);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            //La cancion se termina de llegar
            case SONG_END:
                if (!listSelectionClinteParaReproducir.isEmpty()) {
                    Log.i(TAG, "Han llegado todas las cancion si es que no existien ya en el movil");
                    Toast.makeText(getContext(),
                            "Han llegado todas las cancion", Toast.LENGTH_SHORT).show();
                    escribirMenssge("Han llegado todas las cancion si es que no existien ya en el movil");
                }
                cambiarBotonesUnaVezYaTenemosTodasLasCanciones();
                break;
            case PREPARE_PLAY:
                prepararMovilParaReploduccionParaPlay();
                break;
            case PLAY:
                play();
                break;
            case PREPARADO:
                ponerListaEnPlay();
                break;
        }
    }

    /**
     *
     */
    private void cambiarBotonesUnaVezYaTenemosTodasLasCanciones() {
        // Los botones tendran un orden de uso especifico
        sincronizaButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.VISIBLE);
        sendSongButton.setVisibility(View.INVISIBLE);
    }

    /**
     *
     */
    public void prepararMovilParaReploduccionParaPlay() {
        if(!listSelectionClinteParaReproducir.isEmpty()) {
            //MainActivity.musicService.setSong(listSelectionClinteParaReproducir.get(0));
            //MainActivity.musicService.pausePlay();

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
     *
     */
    public void ponerListaEnPlay() {
        if(!listSelection.isEmpty()) {
            Thread thread;
            byte[] byteArrayPrepararPlay = ("preparar play").getBytes();
            thread = envioMensajesAlOtroDispositivoParaDescarga(MessageType.PLAY, byteArrayPrepararPlay);
            if (thread != null)
                thread.interrupt();
            MainActivity.musicService.setSong(listSelection.get(0));
            MainActivity.musicService.playSong();
            //escribirMenssge("Se esta reproduciendo " + listSelection.get(MainActivity.musicService.getPosn()).getTitle());

        } else {
            escribirMenssge("No hay canciones que reproducir ");
            Toast.makeText(getContext(),
                    "No hay canciones que reproducir", Toast.LENGTH_SHORT).show();
        }
    }

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
            escribirMenssge("Ya se han enviado todas las canciones ");
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

            // retrieve more metadata, duration etc.
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Audio.AudioColumns.DATA, url);
            contentValues.put(MediaStore.Audio.AudioColumns.TITLE, song.getTitleWithExtension());
            contentValues.put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, song.getTitleWithExtension());
            contentValues.put(MediaStore.Audio.AudioColumns.ALBUM,  song.getAlbum());
            contentValues.put(MediaStore.Audio.AudioColumns.ARTIST, song.getArtist());

            // more columns should be filled from here
            Uri uri = getActivity().getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
            Log.d(TAG, uri.toString());

            return file.getAbsolutePath();
        }
        return file.getAbsolutePath();
    }

    /**
     *
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
     *
     */
    private void play()
    {
        MainActivity.musicService.setSong(listSelectionClinteParaReproducir.get(0));
        MainActivity.musicService.playSong();
        //escribirMenssge("Se esta reproduciendo " + listSelectionClinteParaReproducir.get(MainActivity.musicService.getPosn()).getTitle());
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
}


