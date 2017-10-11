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
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;

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
    //TODO cambiar por RecyclerView
    ListView songView;
    private RequestPermissions requerirPermisos;
    //--
    private MusicService musicService;
    private Intent playIntent;
    //--
    private boolean musicIsConnected = false;
    //Clase MusicController
    private MusicController controller;
    //variables pause y volver atras
    private boolean paused = false, playbackPaused = false;
    //
    private List<Song> songList;
    SongAdapter songAdt;
    //Pestañas
    TabLayout tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //initActionButton();
        requerirPermisos = new RequestPermissions();
        //Administra los permisos de la api mayores a la 23 y mustra el panel al usuario
        requerirPermisos.showWarningWhenNeeded(MainActivity.this, getIntent());

        //Tabs
        tabs();

        //Pinta la aplicacion
        drawScreenSongs();
        setController();
        //TODO hacer que tire el long clik para q salga un menu para que se
        // agregue a la lista de reproduccion para enviar
        //songView.setAdapter(songAdt);
        //registerForContextMenu(songView);
        //para probar mandar una canciong

        //TODO borrar estas 4 lineas, ya que lo que estoy haciendo ahora es meterlas a piñon
        //y la idea es que sea el array real de seleccionadas
        // Una vez tenga resuelto el cambio a Recyclerview y el longclick
        Song songOne = songList.get(0);
        Song songTwo = songList.get(1);
        listSelection.add(songOne);
        listSelection.add(songTwo);

    }

    /**
     * Tabs de la app
     */
    private void tabs()
    {
        tabs = (TabLayout) findViewById(R.id.tabs);
        //Agrego las tabs que tendra mi aplicacion
        tabs.addTab(tabs.newTab().setText("SONGS"));
        tabs.addTab(tabs.newTab().setText("ARTIST"));
        tabs.addTab(tabs.newTab().setText("LIST"));
        tabs.addTab(tabs.newTab().setText("CONNECTION"));
        //Para tab con movimiento
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);

        tabs.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        int position = tab.getPosition();
                        switch (position) {
                            case 0:
                                drawScreenSongs();
                                break;
                            case 1:
                                drawScreenArtist();
                                break;
                            case 2:
                                drawScreenSelection();
                                break;
                            case 3:
                                Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
                                startActivity(intent);
                                break;
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

    //---Metodos de la reproduccion
    /**
     * Metodo que genera la accion cuando se pulsa la cancion
     * para reproducirla
     * @param view
     */
    public void songPicked(View view)
    {
        musicService.setSong((Song)view.getTag());
        musicService.playSong();
        if(playbackPaused)
        {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    //Metodosj usados para administrar los permisos de la api mayores a las 26
    /**
     * Una vez aceptados los permisos este metodo es el encargado de pintar la aplicacion
     */
    public void drawScreenSongs()
    {
        //initActionButton();
        songView = (ListView) findViewById(R.id.song_list);
        songList = getSongList(this);
        songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        //ordenaremos los datos para que las canciones se presenten alfabéticamente por titulo
        sortByName((ArrayList) songList);

        //Actualizamos el Servicio con toda la lista de canciones
        if(musicService != null)
            musicService.setList(songList);
    }

    /**
     * Pinta la pestaña ordenada por nombre de artistas
     * correspondiente a la segunda pestaña
     */
    public void drawScreenArtist()
    {
        songList = getSongList(this);
        songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        //ordenaremos los datos para que las canciones se presenten alfabéticamente por Artista
        sortByArtist((ArrayList) songList);

        //Actualizamos el Servicio con toda la lista de canciones
        if(musicService != null)
            musicService.setList(songList);
    }

    /**
     * Pinta el tab en el que figuran las canciones seleccionadas
     * para reproducirlas en los otros dispositivos
     *
     */
    public void drawScreenSelection()
    {
        songAdt = new SongAdapter(this, listSelection);
        songView.setAdapter(songAdt);

        //ordenaremos los datos para que las canciones se presenten alfabéticamente por Artista
        sortByArtist((ArrayList) listSelection);

        //Actualizamos el Servicio con toda la lista de canciones
        if (musicService != null)
            musicService.setList(listSelection);
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
            drawScreenSongs();
        }
    }

    //Metodos que obtienen la musica local del movil y la muestra
    /**
     * método auxiliar para obtener la información del archivo de audio:
     */
    public static List<Song> getSongList(Context context)
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

            //add songs a la lista
            do
            {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                list.add(new Song(thisId, thisTitle, thisAlbum, thisArtist));
            }
            while (musicCursor.moveToNext());

        }
        return list;
    }

    //---------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Menu que se despliega con el long click
     * proporcionaremos la implementación de onCreateContextMenu.
     * Aquí quiero asegurarse de que el evento proviene de la ListView y si es así,
     * quiero determinar en qué elemento en el ListView el usuario hizo clic de largo.
     * @param
     * @return
     */
    //TODO no puedo perder mas timepo en esto, preguntar a los chicos o ver otro dia
//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v,
//                                                   ContextMenu.ContextMenuInfo menuInfo)
//    {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        MenuInflater inflater = getMenuInflater();
//        if(v.getId() == R.id.song_list) {
//            AdapterView.AdapterContextMenuInfo info =
//                    (AdapterView.AdapterContextMenuInfo) menuInfo;
//
//            menu.setHeaderTitle(
//                    songView.getAdapter().getItem(info.position).toString());
//
//            inflater.inflate(R.menu.menu_long_click, menu);
//        }
//    }
//
//    /**
//     *
//     * @param item
//     * @return
//     */
//    @Override
//    public boolean onContextItemSelected (MenuItem item)
//    {
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
//                .getMenuInfo();
//
//        switch (item.getItemId()) {
//            case R.id.pass_list:
//            //-------
//                return true;
//        }
//        return false;
//    }

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
    public static ArrayList sortByName(ArrayList sl)
    {
        //ordenaremos los datos para que las canciones se presenten alfabéticamente por titulo
        Collections.sort(sl, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTITLE().compareTo(b.getTITLE());
            }
        });
        return sl;
    }

    /**
     * Metodo que ordena las canciones por artista
     * @param sl
     * @return
     */
    public static ArrayList sortByArtist(ArrayList sl)
    {
        //ordenaremos los datos para que las canciones se presenten alfabéticamente por titulo
        Collections.sort(sl, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getARTIST().compareTo(b.getARTIST());
            }
        });
        return sl;
    }


}
