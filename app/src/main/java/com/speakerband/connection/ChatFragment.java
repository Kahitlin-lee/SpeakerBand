package com.speakerband.connection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

import com.speakerband.MainActivity;
import com.speakerband.MusicService.*;
import com.speakerband.MusicService;
import com.speakerband.R;
import com.speakerband.Song;
import com.speakerband.network.Message;
import com.speakerband.network.MessageType;
import static com.speakerband.ListSelection.*;

import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import edu.rit.se.wifibuddy.CommunicationManager;
import edu.rit.se.wifibuddy.WifiDirectHandler;


/**
 *  Este fragmento gestiona la interfaz de usuario relacionada con el chat,
 *  que incluye una vista de lista de mensajes
 *  y un campo de entrada de mensaje con un botón de envío.
 *  aquí está la movida para enviar cosas
 */
public class ChatFragment extends ListFragment {

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

    private static final String TAG = WifiDirectHandler.TAG + "ListFragment";

    /**
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        sendButton = (Button) view.findViewById(R.id.sendButton);
        sendButton.setEnabled(false);

        cameraButton = (ImageButton) view.findViewById(R.id.cameraButton);
        //Agrego y anlazo el boton para pasar una cancion
        songButton = (ImageButton) view.findViewById(R.id.songButton);

        textMessageEditText = (EditText) view.findViewById(R.id.textMessageEditText);
        textMessageEditText.addTextChangedListener(new TextWatcher() {

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
                CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();
                if (communicationManager != null && !textMessageEditText.toString().equals(""))
                {
                    String message = textMessageEditText.getText().toString();
                    // Obtiene la primera palabra del nombre del dispositivo
                    String author = handlerAccessor.getWifiHandler().getThisDevice().deviceName.split(" ")[0];
                    byte[] messageBytes = (author + ": " + message).getBytes();
                    Message finalMessage = new Message(MessageType.TEXT, messageBytes);
                    communicationManager.write(SerializationUtils.serialize(finalMessage));
                } else {
                    Log.e(TAG, "Communication Manager is null");
                }
                String message = textMessageEditText.getText().toString();
                if (!message.equals("")) {
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
                pushSong();
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
     * 1º lugar donde pasa cuaando llega la foto
     * @param readMessage
     */
    //TODO cambiarle el nombre a este metodo por pullMessage.
    public void pushMessage(byte[] readMessage)
    {
        Message message = SerializationUtils.deserialize(readMessage);
        Bitmap bitmap;
        switch(message.messageType) {
            case TEXT:
                Log.i(TAG, "Text message received");
                //aca esta cogiendo el mensaje
                pushMessage(new String(message.message));
                break;
            case IMAGE:
                //con esto coge la imagen
                Log.i(TAG, "Image message received");
                bitmap = BitmapFactory.decodeByteArray(message.message, 0, message.message.length);
                ImageView imageView = new ImageView(getContext());
                imageView.setImageBitmap(bitmap);
                //lo envia al metodo que crea nuevamente la imagen
                loadPhoto(imageView, bitmap.getWidth(), bitmap.getHeight());
                break;
            //cancion
            case SONG:
                Log.i(TAG, "Song");
                ByteArrayInputStream in = new ByteArrayInputStream(message.message);
                try {
                    ObjectInputStream is = new ObjectInputStream(in);
                    loadSong(is);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
    public void pushSong()
    {
        //implementa un flujo de salida en el que los datos se escriben en una matriz de bytes
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //recorrerlo he ir enviando cancion a cancion
        for(Song s : listSelection)
        {
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
            Message message = new Message(MessageType.SONG, byteArraySong);
            //esto lo hace en los 3 casos
            CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();
            //lo serializa, esto tambien en los 3 casos
            communicationManager.write(SerializationUtils.serialize(message));
        }
    }

    /**
     * ArrayAdapter para administrar mensajes de chat.
     * Solo lo utiliza para los mensajes de texto
     * //2º lugar donde pasa al mandar el mensaje y recibir mensaje
     * mas bien es cuando lo pienta en en movil
     */
    public class ChatMessageAdapter extends ArrayAdapter<String>
    {
        public ChatMessageAdapter(Context context, int textViewResourceId, List<String> items) {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
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
    public void onAttach(Context context) {
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

    private MusicService musicService;
    private boolean musicIsConnected = false;


    /**
     * Declaración e inicialización del objeto que representa al servicio
     * que correrá en background para la reproducción de los audios. Al instanciarlo,
     * sobreescribimos sus dos principales métodos para su inicialización.
     */
    private ServiceConnection musicConnection = new ServiceConnection()
    {
        /**
         * Metodo de cuando el servicio esta conectado
         * @param name
         * @param service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //obtenemos el servicio
            musicService = binder.getService();
            musicIsConnected = true;
        }

        /**
         * Metodo por si nos desconectamos el servicio
         * @param name
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //TODO
            musicIsConnected = false;
        }
    };

    /**
     * En este metodo es donde querria que se coja la cacion y la sume a la lista
     * de reproduccion del cliente
     */
    private void loadSong(ObjectInputStream is)
    {
        try {
            Song song = (Song)is.readObject();
            listSelection.add(song);
        } catch (IOException e) {
            e.printStackTrace();//writeabortemexeption
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
