package com.speakerband;


import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;
import android.widget.Toast;

import com.speakerband.connection.ConnectionActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.speakerband.ListSelection.*;

/**
 * Activity principal
 */
public class MainActivity extends AppCompatActivity implements MediaPlayerControl
{

    private RequestPermissions requerirPermisos;
    //--
    public static MusicService musicService;
    private Intent playIntent;
    //--
    private boolean musicIsConnected = false;
    //Clase MusicController
    private MusicController controller;
    //variables pause y volver atras
    private boolean paused = false, playbackPaused = false;
    //Array que utilizaremospara almacenar la lista de canciones
    private List<Song> songList;
    //Variable auxiliar
    private Song song;
    //Texto que se mostrara si la lista de canciones seleccionada esta vacia
    private TextView textListSelectionEmpty;
    //Texto que se mostrara si la lista de canciones esta vacia
    private TextView textListEmpty;

    private UtilFicheros utilFicheros;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //variables que se ocultan y se muestran dependiendo de si hay cancioones en las listas o no
        textListSelectionEmpty = (TextView) findViewById(R.id.list_selection_empty);
        textListSelectionEmpty.setVisibility(View.GONE);
        textListEmpty = (TextView) findViewById(R.id.list_empty);
        textListEmpty.setVisibility(View.GONE);

        //Creo objeto de la clase UtilFicheros
        utilFicheros = new UtilFicheros();

        requerirPermisos = new RequestPermissions();
        //Administra los permisos de la api mayores a la 23 y mustra el panel al usuario
        requerirPermisos.showWarningWhenNeeded(MainActivity.this, getIntent());

        //Tabs
        tabs();

        //Pinta la aplicacion llamando al metodo del recyclerView
        initRecyclerView(0);

        //Metodo de ayuda para configurar el controlador
        setController();

        if(utilFicheros.createFolderApp(MainActivity.this) == true)
        {
            Toast.makeText(MainActivity.this, R.string.carpeta_creada , Toast.LENGTH_SHORT).show();
        } else
        {
            Toast.makeText(MainActivity.this, R.string.carpeta_no_creada , Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Pinta el RecyclerView con la lista de canciones dependiendo de la pestaña
     * en la que se ubica el usuario.
     * @param typeList este parametro viene del metodo que se usa para elegir la tab
     *                 donde esta ubicado el usuario, dependiendo de esta es la informacion
     *                 y el orden en que se muestra
     */
    private void initRecyclerView(int typeList)
    {
        //Obtengo la lista de canciones del dispositivo
        songList = getSongListByType(this, typeList, listSelection);

        //Si la lista de canciones no esta vacia
        if (songList.size() >= 0) {
            //Actualizamos el Servicio con toda la lista de canciones
            if (musicService != null)
                musicService.setList(songList);

            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv);
            recyclerView.setAdapter(new SongsAdapter(songList, getApplication(), listItemClickListener));
            //indicamos tipo de layout para el recyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

        }
        //TODO no funciona correctamente y estoy arta de mirarlo y saber por que
        showHideTextDependingOnList(textListEmpty, songList);
        textListSelectionEmpty.setVisibility(View.GONE);
        if (songList == listSelection) {
            showHideTextDependingOnList(textListSelectionEmpty, songList);
        }
    }

    /**
     * Metodo que hace visible o invidible un texto informativo de pendiendo de si hay canciones
     * o no en el dispositivo o en la liesta de seleccion
     * @param text
     * @param songList
     */
    private void showHideTextDependingOnList(TextView text, List<Song> songList)
    {
        if(songList.isEmpty() || songList == null) {
            text.setVisibility(View.VISIBLE);
        }else{
        text.setVisibility(View.GONE);
        }
    }

    private RecyclerViewOnItemClickListener listItemClickListener = new RecyclerViewOnItemClickListener()
    {
        /**
         * Click siempre para reproducir la cancion
         * @param v
         * @param position
         */
        @Override
        public void onClick(View v, int position)
        {
            musicService.setSong(songList.get(position));
            //Dependiendo de el valor de isInternalUri hara play sobre la memoria interna o externa
            musicService.playSong();
            //lo pausa si esta a play y lo pone a play si esta pausado
            if(playbackPaused)
            {
                setController();
                playbackPaused = false;
            }//muestra los controles de reproduccion
            controller.show(0);
        }

        /**
         *
         * @param v
         * @param position
         */
        @Override
        public void onLongClick(View v, int position)
        {
            song = songList.get(position);
            if(!listSelection.contains(song.getTitle())) {
                listSelection.add(song);
                utilFicheros.copyFile(song, (MainActivity.this.getString(R.string.app_name_con_barra)));
                Toast.makeText(MainActivity.this, R.string.song_add, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, R.string.song_exist_list_selection, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Tabs de la app
     */
    private void tabs()
    {
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        //Agrego las tabs que tendra mi aplicacion
        tabs.addTab(tabs.newTab().setText("SONGS"));
        tabs.addTab(tabs.newTab().setText("ARTIST"));
        tabs.addTab(tabs.newTab().setText("LIST"));
        tabs.addTab(tabs.newTab().setText("CONNECTION"));
        //Para tab con movimiento
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);

        tabs.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener()
                {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab)
                    {
                        if (tab.getText() == "CONNECTION"){
                            Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
                            startActivity(intent);
                        }else{
                            initRecyclerView(tab.getPosition());
                        }
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        // ...
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        // ...
                    }
                }
        );
    }

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
            //pasamos la lista
            musicService.setList(songList);
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


    //Metodos del ciclo de vida de la aplicacon que no me gusta ponerlos asi todos juntos pero
    //de moemento asi se quedaran
    /**
     * Metodo Llamado después onCreate(Bundle)- o después de onRestart()
     * cuando la actividad se había detenido, pero ahora se muestra nuevamente al usuario.
     * Será seguido por onResume().
     * Queremos iniciar la instancia de Service cuando se inicia la instancia de Activity.
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        if(playIntent == null)
        {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    /**
     *
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    /**
     *
     */
    @Override
    protected void onStop()
    {
        controller.hide();
        super.onStop();
    }

    /**
     *
     */
    @Override
    protected void onDestroy()
    {
        stopService(playIntent);
        musicService.unbindService(musicConnection);
        musicService = null;
        super.onDestroy();
    }

    /**
     * Llamado por Activity después de que el usuario interactue con la solicitud de permiso
     * Lanzará la main activity si todos los permisos se concedieron, salidas de lo contrario
     * Cuando el usuario responde, el sistema invoca el método onRequestPermissionsResult() de la app
     * @param requestCode  The code set by requestPermissions
     * @param permissions  Names of the permissions we got granted or denied
     * @param grantResults Results of the permission requests
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        int grantedPermissions = 0;

        for (int result : grantResults)
        {
            if (result == PackageManager.PERMISSION_GRANTED)
                grantedPermissions++;
        }

        if (grantedPermissions == grantResults.length)
        {
            requerirPermisos.eliminarDialogoPermisos();
            initRecyclerView(0);
        }
    }

    /**
     * Ordena las canciones dependiendo de la tab
     * @param context
     * @param typeList
     * @param listSelection
     * @return
     */
    private List<Song> getSongListByType(Context context, int typeList, List<Song> listSelection)
    {
        List<Song> songs = getSongList(context);
        switch (typeList){
            case 0:
                songs = sortByName(songs);
                break;
            case 1:
                songs = sortByArtist(songs);
                break;
            case 2:
                songs = getSongListSelection(context);
                break;
            default:

        }
        return songs;
    }


    //Metodos que obtienen la musica local del movil y la muestra
    /**
     * Método auxiliar para obtener la información del archivo de audio:
     * Obtiene la lista de todas las canciones que estan en el dispositivo
     */
    public List<Song> getSongList(Context context)
    {
        ArrayList list = new ArrayList();
        if (android.support.v4.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return list;
        }

        //instancia de ContentResolver
        ContentResolver musicResolver = context.getContentResolver();
        //EXTERNAL_CONTENT_URI : URI de estilo para el volumen de almacenamiento externo "primario".
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //instancia de Cursor , usando la instancia de ContentResolver para buscar los archivos de música
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        //iterar los resultados, primero chequeando que tenemos datos válidos:
        if(musicCursor != null && musicCursor.moveToFirst())
        {
            //get Columnas
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int uriDataColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);

            //add songs a la lista
            do
            {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisUri = musicCursor.getString(uriDataColumn);
                list.add(new Song(thisId, thisTitle, thisAlbum, thisArtist, thisUri));
            }
            while (musicCursor.moveToNext());
        }//Por defecto lo ordena por nombre de cancion
        sortByName(list);
        return list;
    }

    /**
     * Método que recupera la lista de canciones del fichero de la app Speakerband
     * y las muestra en la pestaña correspondiente a las canciones de la lista de reproduccion
     * que se comparten entre archivos.
     */
    public List<Song> getSongListSelection(Context context)
    {
        ArrayList list = new ArrayList();
        if (android.support.v4.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return list;
        }

        if (utilFicheros.findFolder(context.getString(R.string.app_name_con_barra)) == true)
        {
            Toast.makeText(MainActivity.this, R.string.carpeta_encontrada , Toast.LENGTH_SHORT).show();
            //instancia de ContentResolver
            ContentResolver musicResolver = context.getContentResolver();

            //EXTERNAL_CONTENT_URI : URI de estilo para el volumen de almacenamiento externo "primario".
            Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            String selection = MediaStore.Audio.Media.DATA + " LIKE '%Speakerband%'";

            String[] projection = {MediaStore.Audio.AudioColumns.DATA,MediaStore.Audio.AudioColumns.TITLE ,
                    MediaStore.Audio.AudioColumns._ID,
                    MediaStore.Audio.ArtistColumns.ARTIST,};

            //instancia de Cursor , usando la instancia de ContentResolver para buscar los archivos de música
            Cursor musicCursor = musicResolver.query(
                    musicUri, projection,
                    MediaStore.Audio.Media.DATA + " like ? ", new String[]{"%Speakerband%"},
                    null);

            int i = musicCursor.getCount();

            //iterar los resultados, primero chequeando que tenemos datos válidos:
            if ((musicCursor != null) && (musicCursor.moveToFirst())) {
                //get Columnas
                int titleColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media._ID);
                int albumColumn = musicCursor.getColumnIndex
                        (MediaStore.Audio.Media.ALBUM);
                int artistColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.ARTIST);
                int uriDataColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);

                //add songs a la lista
                do {
                    long thisId = musicCursor.getLong(idColumn);
                    String thisTitle = musicCursor.getString(titleColumn);
                    String thisAlbum = musicCursor.getString(albumColumn);
                    String thisArtist = musicCursor.getString(artistColumn);
                    String thisUri = musicCursor.getString(uriDataColumn);
                    list.add(new Song(thisId, thisTitle, thisAlbum, thisArtist, thisUri));
                }
                while (musicCursor.moveToNext());
            }//Por defecto lo ordena por nombre de cancion
            sortByName(list);
            musicCursor.close();
            return list;
        } else {
            Toast.makeText(MainActivity.this, R.string.carpeta_no_encontrada , Toast.LENGTH_SHORT).show();
            return null;
        }
    }



    //---------------

    /**
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_shuffle:
                musicService.setMezclar();
                break;
            case R.id.action_end:
                musicService = null;
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //MEtodo de MusicController ,

    /**
     * Metodo de ayuda para configurar el controlador
     * más de una vez en el ciclo de vida de la aplicación
     */
    private void setController()
    {
        // instanciar el controlador:
        controller = new MusicController(this);
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.linear));
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

    //métodos que llamamos cuando establecemos el controlador:

    /**
     * play next
     */
    private void playNext()
    {
        musicService.playNext();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    /**
     * play previous
     */
    private void playPrev()
    {
        musicService.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    //Metodos  de MediaPlayerControl

    /**
     *
     */
    @Override
    public void start()
    {
        musicService.pausePlayer();
    }

    /**
     *
     */
    @Override
    public void pause()
    {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    /**
     *
     * @return
     */
    @Override
    public int getDuration()
    {
        if(musicService!=null && musicIsConnected && musicService.isPng())
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
        if(musicService!=null && musicIsConnected && musicService.isPng())
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
        if(musicService!=null && musicIsConnected)
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


    //Metodos de ordenacion
    /**
     * Metodo que ordena las canciones por nombre
     * @param sl
     * @return
     */
    public static List sortByName(List sl)
    {
        //ordenaremos los datos para que las canciones se presenten alfabéticamente por titulo
        Collections.sort(sl, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        return sl;
    }

    /**
     * Metodo que ordena las canciones por artista
     * @param sl
     * @return
     */
    public static List sortByArtist(List sl)
    {
        //ordenaremos los datos para que las canciones se presenten alfabéticamente por titulo
        Collections.sort(sl, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getArtist().compareTo(b.getArtist());
            }
        });
        return sl;
    }
}
