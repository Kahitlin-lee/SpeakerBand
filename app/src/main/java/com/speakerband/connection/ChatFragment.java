package com.speakerband.connection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.speakerband.R;
import com.speakerband.Song;
import com.speakerband.network.Message;
import com.speakerband.network.MessageType;
import static com.speakerband.ListSelection.*;

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
import java.util.Arrays;
import java.util.List;
import edu.rit.se.wifibuddy.CommunicationManager;
import edu.rit.se.wifibuddy.WifiDirectHandler;



/**
 *  Este fragmento gestiona la interfaz de usuario relacionada con el chat,
 *  que incluye una vista de lista de mensajes
 *  y un campo de entrada de mensaje con un botón de envío.
 *  aquí está la movida para enviar cosas
 */
public class ChatFragment extends ListFragment
{
    private EditText textMessageEditText;
    private ChatMessageAdapter adapter = null;
    private List<String> items = new ArrayList<>();
    private ArrayList<String> messages = new ArrayList<>();
    //instancia de la interfaz WiFiDirectHandlerAccessor
    private WiFiDirectHandlerAccessor handlerAccessor;
    private Toolbar toolbar;
    //variables de los botones
    private Button sendButton;
    private ImageButton cameraButton;
    private ImageButton songButton;
    private ImageButton playButton;

    private static final String TAG = WifiDirectHandler.TAG + "ListFragment";

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
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        sendButton = (Button) view.findViewById(R.id.sendButton);
        sendButton.setEnabled(false);

        cameraButton = (ImageButton) view.findViewById(R.id.cameraButton);
        //Agrego y anlazo el boton para pasar una cancion
        songButton = (ImageButton) view.findViewById(R.id.songButton);
        playButton = (ImageButton) view.findViewById(R.id.play);

        textMessageEditText = (EditText) view.findViewById(R.id.textMessageEditText);
        textMessageEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(true);
            }
        });

        //el adaptador es solo usado para los mensajes
        ListView messagesListView = (ListView) view.findViewById(android.R.id.list);
        adapter = new ChatMessageAdapter(getActivity(), android.R.id.text1, items);
        messagesListView.setAdapter(adapter);
        messagesListView.setDividerHeight(0);

        // Evita que el teclado empuje el fragmento y los mensajes hacia arriba y hacia fuera de la pantalla
        messagesListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        messagesListView.setStackFromBottom(true);

        //Evento del boton enviar de el chat
        //1º lugar donde pasa al mandar el texto con esto ya llego al cliente y lo pienta
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                Log.i(WifiDirectHandler.TAG, "Send button tapped");

                //TODO probar hacer esta variable de clase
                CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();

                if (communicationManager != null && !textMessageEditText.toString().equals(""))
                {
                    String message = textMessageEditText.getText().toString();
                    // Obtiene la primera palabra del nombre del dispositivo
                    String author = handlerAccessor.getWifiHandler().getThisDevice().deviceName.split(" ")[0];
                    byte[] messageBytes = (author + ": " + message).getBytes();
                    Message finalMessage = new Message(MessageType.TEXT, messageBytes);
                    communicationManager.write(SerializationUtils.serialize(finalMessage));
                }
                else {
                    Log.e(TAG, "Communication Manager is null");
                }

                String message = textMessageEditText.getText().toString();

                if (!message.equals(""))
                {
                    pushMessage("Me: " + message);
                    messages.add(message);
                    Log.i(TAG, "Message: " + message);
                    textMessageEditText.setText("");
                }
                sendButton.setEnabled(false);
            }
        });

        //evento del boton que saca la foto y la envia
        //1º lugar por dodne pasa para ssacar la foto y enviearla
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            // la accion de tomar la foto
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
                    //se envia la informacion de un activity a otro con
                    //startActivityForResult
                    startActivityForResult(takePictureIntent, 1);
                }
            }
        });

        //evento del boton  enviar una cancion
        songButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pushSong();
                pushSongPath();
            }
        });

        //evento del boton  enviar una cancion
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSign();
            }
        });

        toolbar = (Toolbar) getActivity().findViewById(R.id.mainToolbar);

        return view;
    }

    public interface MessageTarget {
        Handler getHandler();
    }

    /**
     * deserializa lo que llegue
     * 1º lugar donde pasa cuando llega la foto
     * @param readMessage
     * @param context
     */
    //TODO cambiarle el nombre a este metodo por pullMessage., aqui agregue en parametro context pero esto me puede dar mucho problema
    public void pushMessage(byte[] readMessage, Context context)
    {
        Message message = SerializationUtils.deserialize(readMessage);
        Bitmap bitmap;
        ByteArrayInputStream in;

        switch(message.messageType)
        {
            case TEXT:
                Log.i(TAG, "Text message received");
                //aca esta cogiendo el mensaje
                pushMessage(new String(message.content));
                break;
            case IMAGE:
                //con esto coge la imagen
                Log.i(TAG, "Image message received");
                bitmap = BitmapFactory.decodeByteArray(message.content, 0, message.content.length);
                ImageView imageView = new ImageView(getContext());
                imageView.setImageBitmap(bitmap);
                //lo envia al metodo que crea nuevamente la imagen
                loadPhoto(imageView, bitmap.getWidth(), bitmap.getHeight());
                break;
            //cancion
            case SONG_START:
                Log.i(TAG, "SongPath");
                try
                {
                    in = new ByteArrayInputStream(message.content);
                    ObjectInputStream is = new ObjectInputStream(in);
                    loadSongPath(is );
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            case SONG:
                Log.i(TAG, "Song");
                try
                {
                    in = new ByteArrayInputStream(message.content);
                    ObjectInputStream is = new ObjectInputStream(in);
                    loadSong(is);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case SONG_END:
                writeSong();
                break;
        }
    }

    /**
     * Envia el texto
     * Este metodo se usa en el onClick de el envio de texto
     * e internamente en el otro metodo pushMenssage
     * @param message
     */
    public void pushMessage(String message)
    {
        adapter.add(message);
        adapter.notifyDataSetChanged();
    }

    /**
     * 3º lugar por donde pasa para enviar la foto
     * Envia la imagen al cliente
     * Este metodo se implementa en ConnectionActivity
     * @param image
     */
    public void pushImage(Bitmap image)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        //indica que tipo de mensaje es
        Message message = new Message(MessageType.IMAGE, byteArray);
        CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();
        Log.i(TAG, "Attempting to send image");
        communicationManager.write(SerializationUtils.serialize(message));
    }

    /**
     * Envia la cancion
     */
    public void pushSongPath()
    {
        //recorrerlo he ir enviando cancion a cancion
        for(Song song : listSelection)
        {
            song.readFile();

            //Vamos a ver cuantas veces dividimos el vector de arrays:
            List<byte[]> partes = divideArray(song.getSongBytes(), 512);

            byte[] byteArraySong = null;

            for(int i=0; i < partes.size(); i++)
            {
                song.setSongBytes(partes.get(i));
                //Lo que sea lo tiene que transformar en byte (en este caso la cancion)
                //toByteArray(Crea una matriz de bytes recién asignada.
                // en los 3 casos lo pasa a un array de bytes
                byteArraySong = convertirObjetoArrayBytes (song);

                if (i == 0)  //Primera vez
                {
                    enviarMensaje(MessageType.SONG_START, byteArraySong);
                }
                else {
                    enviarMensaje(MessageType.SONG , byteArraySong);
                }

            }
            enviarMensaje(MessageType.SONG_END , byteArraySong);  // No nos importa el contenido, solo el mensaje de terminacion.
        }
    }

    /**
     *
     * @param tipo
     * @param contenido
     */
    public void enviarMensaje (MessageType tipo, byte[] contenido)
    {
        //armamos el mensaje con el tipo de mensaje y la cantidad de bytes en array, tambien en los 3 casos
        Message message = new Message(tipo , contenido);
        //esto lo hace en los 3 casos
        CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();
        //lo serializa, esto tambien en los 3 casos
        communicationManager.write(SerializationUtils.serialize(message));
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
     *
     * @param source
     * @param chunksize
     * @return
     */
    public static List<byte[]> divideArray(byte[] source, int chunksize)
    {
        List<byte[]> result = new ArrayList<byte[]>();
        int start = 0;
        while (start < source.length)
        {
            int end = Math.min(source.length, start + chunksize);
            result.add(Arrays.copyOfRange(source, start, end));
            start += chunksize;
        }
        return result;
    }

    /**
     * Envia la cancion que se tiene q poner a  play
     * TOdavia no hace nada
     */
    public void playSign()
    {
        //recorrerlo he ir enviando cancion a cancion
        for(Song s : listSelection)
        {
            if (s.equals(songPlaying))
            {
                //implementa un flujo de salida en el que los datos se escriben en una matriz de bytes
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                //convertir la cancion en bytes
                try {
                    ObjectOutputStream os = new ObjectOutputStream(stream);
                    os.writeObject(s);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Lo que sea lo tiene que transformar en byte (en este caso la cancion)
                //toByteArray(Crea una matriz de bytes recién asignada.
                // en los 3 casos lo pasa a un array de bytes
                byte[] byteArraySong = stream.toByteArray();
                //armamos el mensaje con el tipo de mensaje y la cantidad de bytes en array, tambien en los 3 casos
                Message message = new Message(MessageType.PLAY, byteArraySong);
                //esto lo hace en los 3 casos
                CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();
                //lo serializa, esto tambien en los 3 casos
                communicationManager.write(SerializationUtils.serialize(message));
            }
        }
    }

    /**
     * ArrayAdapter para administrar mensajes de chat.
     * Solo lo utiliza para los mensajes de texto
     * 2º lugar donde pasa al mandar el mensaje y recibir mensaje
     * mas bien es cuando lo pienta en en movil
     */
    public class ChatMessageAdapter extends ArrayAdapter<String>
    {
        public ChatMessageAdapter(Context context, int textViewResourceId, List<String> items)
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

    @Override
    public void onResume() {
        super.onResume();
        toolbar.setTitle("Chat");
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(textMessageEditText.getWindowToken(), 0);
    }

    /**
     * This is called when the Fragment is opened and is attached to MainActivity
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
     * Carga la foto en el movil que la coge
     * Y la muestra con el boton de OK en un layout
     * @param imageView
     * @param width
     * @param height
     */
    private void loadPhoto(ImageView imageView, int width, int height)
    {
        ImageView tempImageView = imageView;

        AlertDialog.Builder imageDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.custom_fullimage_dialog,
                (ViewGroup) getActivity().findViewById(R.id.layout_root));
        ImageView image = (ImageView) layout.findViewById(R.id.fullimage);
        image.setImageDrawable(tempImageView.getDrawable());
        imageDialog.setView(layout);
        imageDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener(){

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });
        imageDialog.create();
        imageDialog.show();
    }

    //Variable para los metodos para enviar alchivo a trozos uasadas en los metodos loadSongPath,
    // loadSong, writeSong,
    private Song _song;
    private ArrayList<byte[]> arrayTrozos;

    /**
     *
     * @param is
     * @throws IOException
     */
    private void loadSongPath(ObjectInputStream is ) throws IOException
    {
        try
        {
            _song = (Song)is.readObject();
            arrayTrozos = new ArrayList<byte[]>();
            //arrayTrozos.add(_song.getSongBytes());   // Escribimos el primer trozo.

        } catch (IOException e) {
            e.printStackTrace();//writeabortemexeption
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * En este metodo es donde querria que se coja la cacion y la sume a la lista
     * de reproduccion del cliente
     */
    private void loadSong(ObjectInputStream is)
    {
        try
        {
            Song songTemp = (Song)is.readObject();
            arrayTrozos.add(songTemp.getSongBytes());

        } catch (IOException e) {
            e.printStackTrace();//writeabortemexeption
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeSong()
    {
        try
        {
            //Unimos todos los trozos de arrays
            //Concatenamos Arrays:
            for (byte[] trozito : arrayTrozos )
            {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(_song.getSongBytes());
                outputStream.write(trozito);
                _song.setSongBytes(outputStream.toByteArray());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        _song.setUri(writeSongOnExternalMemory(_song));
        listSelection.add(_song);
    }

    /**
     * Metodo que escribe un archivo de musica en la memoria externa
     * TODO mejorar esta explicaicon del metodo
     * @param song
     * @return
     */
    private String writeSongOnExternalMemory(Song song)
    {
        //ContextWrapper contextWrapper = new ContextWrapper(getActivity());
        //File directory = contextWrapper.getDir(getActivity().getFilesDir().getName(), Context.MODE_PRIVATE);
        File file =  new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                + song.getTitleWithExtension());
        FileOutputStream fileOutputStream = null; // save

        // Get length of file in bytes
        long fileSizeInBytes = file.length();
        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
        long fileSizeInKB = fileSizeInBytes / 1024;
        // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
        long fileSizeInMB = fileSizeInKB / 1024;

        try
        {
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
         fileSizeInBytes = file.length();
        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
         fileSizeInKB = fileSizeInBytes / 1024;
        // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
         fileSizeInMB = fileSizeInKB / 1024;

        return file.getAbsolutePath();
    }



//    private void play(ObjectInputStream is)
//    {
//        try
//        {
//            Song song = (Song)is.readObject();
//            musicService.setSong(song);
//            musicService.playSong();
//        } catch (IOException e) {
//            e.printStackTrace();//writeabortemexeption
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }


}
