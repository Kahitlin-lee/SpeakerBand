package com.speakerband;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;
import android.widget.Toast;

import com.speakerband.conexiones.ConnectionActivity;
import com.speakerband.utils.UtilList;

import java.util.List;

import static com.speakerband.ClaseAplicationGlobal.listSelection;

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

    private ClaseAplicationGlobal mApplication;

    private TabLayout tabsLayout;
    private int tabPosition;

    private Button conexionBoton;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //variables que se ocultan y se muestran dependiendo de si hay cancioones en las listas o no
        textListSelectionEmpty = (TextView) findViewById(R.id.list_selection_empty);
        textListSelectionEmpty.setVisibility(View.INVISIBLE);
        textListEmpty = (TextView) findViewById(R.id.list_empty);
        textListEmpty.setVisibility(View.INVISIBLE);

        requerirPermisos = new RequestPermissions();
        //Administra los permisos de la api mayores a la 23 y mustra el panel al usuario
        requerirPermisos.showWarningWhenNeeded(MainActivity.this, getIntent());

        mApplication = (ClaseAplicationGlobal) getApplicationContext ();
        // TODO arreglar que las preferencias tarden tanto siempre
        mApplication.generarNuevamentePreferenciasYListSeleccion();

        conexionBoton = (Button) findViewById(R.id.conexionBoton);
        conexionBoton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
                    startActivity(intent);
            }
        });

        //Tabs
        tabs();

        //Pinta la aplicacion llamando al metodo del recyclerView
        initRecyclerView(0);

        //Metodo de ayuda para configurar el controlador
        setController();
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

            //indicamos tipo de layout para el recyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            recyclerView.setAdapter(new SongsAdapter(songList, getApplication(), listItemClickListener));
        }

        //TODO no funciona correctamente y estoy arta de mirarlo y saber por que
        showHideTextDependingOnList(textListEmpty, songList);
        textListSelectionEmpty.setVisibility(View.INVISIBLE);
        if (songList.equals(listSelection)) {
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
        if(songList.isEmpty()) {
            text.setVisibility(View.VISIBLE);
        }else{
            text.setVisibility(View.INVISIBLE);
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
                setController();  //Creo que con que este en el onResume ya basta, se puede quitar
                playbackPaused = false;
            }//muestra los controles de reproduccion
            controller.show(0);
        }

        /**
         * Long clcli que agrega las canciones a la lista de preferencias
         * O la quita si es que estamos en la tab de lista de seleccion
         * @param v
         * @param songPosition
         */
        @Override
        public void onLongClick(View v, int songPosition)
        {
            if(tabPosition == 2) {
                song = songList.get(songPosition);
                if (listSelection.contains(song)) {
                    mApplication.eliminarUnaCancionAPreferencess(song);
                    listSelection.remove(listSelection.indexOf(song));
                    Toast.makeText(MainActivity.this, R.string.song_remove, Toast.LENGTH_SHORT).show();
                }
                initRecyclerView(2);
            } else {
                song = songList.get(songPosition);
                if (!listSelection.contains(song)) {
                    listSelection.add(song);
                    //Agregamos la nueva cancion a SharedPreferencesClass desde La clase global aplication

                    mApplication.agregarUnaCancionAPreferencess(song);
                    Toast.makeText(MainActivity.this, R.string.song_add, Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(MainActivity.this, R.string.song_exist_list_selection, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Tabs de la app
     */
    private void tabs()
    {
        tabsLayout = (TabLayout) findViewById(R.id.tabs);

        //Agrego las tabs que tendra mi aplicacion
        tabsLayout.addTab(tabsLayout.newTab().setText(getResources().getString(R.string.canciones)));
        tabsLayout.addTab(tabsLayout.newTab().setText(getResources().getString(R.string.artistas)));
        tabsLayout.addTab(tabsLayout.newTab().setText(getResources().getString(R.string.sincronizacion)));
        //  tabsLayout.addTab(tabsLayout.newTab().setText(getResources().getString(R.string.conexion)));

        //Para tab con movimiento    TODO MODE_SCROLLABLE
        tabsLayout.setTabMode(TabLayout.MODE_FIXED);

        tabsLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener()
                {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab)
                    {
                        initRecyclerView(tab.getPosition());
                        tabPosition = tab.getPosition();
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

        public boolean isServiceConnected() {
            return musicIsConnected;
        }

        /**
         * Metodo por si nos desconectamos el servicio
         * @param name
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
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
        // en teoria al volver de la otra activity tiene q poner tab 0
        initRecyclerView(2);

        //TODO El bug está relacionado con playIntent porque
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
        initRecyclerView(0);
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
        mApplication.salvarTodasLasCancionesEnLaListaDePreferencess();
        stopService(playIntent);

        if (musicService != null) {
            if (musicConnection != null) {
                if(musicIsConnected) {
                    unbindService(musicConnection);
                    //musicService.unbindService(musicConnection);  //TODO Aquí esta el bug cuando cierras la app,
                    //TODO dice que no está unido, raro, porque sí lo está!!!
                    musicService = null;
                }
            }
        }

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
     * @param listSelection   En este parametro deberias de eliminarlo, no vale para nada
     * @return
     */
    private List<Song> getSongListByType(Context context, int typeList, List<Song> listSelection)
    {
        List<Song> songs = SongCursor.getSongList();
        switch (typeList){
            case 0:
                songs = UtilList.sortByName(songs);
                break;
            case 1:
                songs = UtilList.sortByArtist(songs);
                break;
            case 2:
                // TODO De esta forma al hacer el long click es sobre la cancion en si buscada en el dispositivo y
                // no sobre lo que indica la losta de preferencias, puede que de esta forma no la envie cortada
                listSelection = SongCursor.getSongListSelection();
                songs = listSelection;
                break;
            default:
        }
        return songs;
    }


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
            case R.id.action_end:
                musicService = null;
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    //Metodo de MusicController ,

    /**
     * Metodo de ayuda para configurar el controlador
     * más de una vez en el ciclo de vida de la aplicación
     */
    private void setController()
    {
        // Instanciar el controlador:
        controller = new MusicController(this){

            //Manejar el BACK button cuando el reproductor de música está activo
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if(event.getKeyCode() == KeyEvent.KEYCODE_BACK){

                    if(controller.isShown()) {
                        controller.hide();//Oculta el mediaController
                    }
                    else
                        ((MainActivity) getContext()).finish(); //Cierra el activity

                    return true;
                }
                //Si no presiona el back button, pues sigue funcionando igual.
                return super.dispatchKeyEvent(event);
            }
        };
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

    //Metodos  de MediaPlayerControl

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


}
