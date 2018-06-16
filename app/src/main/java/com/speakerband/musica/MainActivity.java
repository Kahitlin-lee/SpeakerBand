package com.speakerband.musica;


import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

import com.speakerband.ClaseApplicationGlobal;
import com.speakerband.R;
import com.speakerband.RequestPermissions;
import com.speakerband.conexiones.ConnectionActivity;
import com.speakerband.conexiones.dialogos.AyudaConectionDialogFragment;
import com.speakerband.musica.adapter.SongsAdapter;
import com.speakerband.musica.cursor.SongCursor;
import com.speakerband.musica.modelo.Song;
import com.speakerband.musica.servicios.MusicService;
import com.speakerband.utils.UtilList;

import java.util.List;

import static com.speakerband.ClaseApplicationGlobal.listSelection;
import static com.speakerband.ClaseApplicationGlobal.musicService;

/**
 * Activity principal
 * Created by Catalina Saavedra
 */
public class MainActivity extends AppCompatActivity implements MediaPlayerControl, NavigationView.OnNavigationItemSelectedListener
{

    private RequestPermissions requerirPermisos;
    //--
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

    private AyudaConectionDialogFragment ayudaConectionDialogFragment;

    private DrawerLayout drawerLayout;

    private RecyclerView rv;

    private ClaseApplicationGlobal mApplication;

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

        requerirPermisos = new RequestPermissions();
        //Administra los permisos de la api mayores a la 23 y mustra el panel al usuario
        requerirPermisos.showWarningWhenNeeded(MainActivity.this, getIntent());

        mApplication = (ClaseApplicationGlobal) getApplicationContext ();
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

        //Codigo del menu lateral. No funciona drawer_layout me da null, se veq ue este tendria que ser el activiti principal
//        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
//        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
//                this, drawerLayout, R.string.abrir_navegacion_lateral, R.string.cerrar_navegacion_lateral);
//        drawerLayout.addDrawerListener(actionBarDrawerToggle);
//        actionBarDrawerToggle.syncState();
//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//        navigationView.setNavigationItemSelectedListener(this);
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

        //Actualizamos el Servicio con toda la lista de canciones
        if (musicService != null)
            musicService.setList(songList);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv);

        //indicamos tipo de layout para el recyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setAdapter(new SongsAdapter(songList, getApplication(), listItemClickListener));

        showHideTextDependingOnList(typeList);
    }

    /**
     * Metodo que muestra  un dialog informativo de pendiendo de si hay canciones
     * o no en el dispositivo o en la liesta de seleccion
     * @param typeList
     */
    private void showHideTextDependingOnList(int typeList)
    {
        if(songList.isEmpty()) {

            if (typeList == 2) {
                AlertDialog alertDialog2 = new AlertDialog.Builder(MainActivity.this).create();
                View mView2 = getLayoutInflater().inflate(R.layout.info, null);
                final CheckBox mCheckBox2 = (CheckBox)mView2.findViewById(R.id.checkBox);
                alertDialog2.setTitle("Info");
                alertDialog2.setMessage(getString(R.string.list_selection_empty));
                dialogCheckBox(alertDialog2, mView2, mCheckBox2, typeList);
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                View mView = getLayoutInflater().inflate(R.layout.info, null);
                final CheckBox mCheckBox = (CheckBox)mView.findViewById(R.id.checkBox);
                alertDialog.setTitle("Info");
                alertDialog.setMessage(getString(R.string.list_empty));
                dialogCheckBox(alertDialog, mView, mCheckBox, typeList);
            }
        }
    }

    private void dialogCheckBox(AlertDialog alertDialog, View mView, CheckBox mCheckBox, final int  typeList) {
        alertDialog.setView(mView);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    mApplication.noMostrarMasDialogo(true, typeList);
                }else{
                    mApplication.noMostrarMasDialogo(false, typeList);
                }
            }
        });

        if(mApplication.obtenerDialogStatus(typeList)){
            alertDialog.hide();
        }else{
            // TODO ARREGLAR EL PETE DEL DIALOGO
            if(alertDialog!=null) {
                if (!((MainActivity) this).isFinishing())
                    alertDialog.show();
            }
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
        //initRecyclerView(2);

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
        //initRecyclerView(0);
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
            case R.id.mostrar_barra_control:
                if(musicService!=null)
                    controller.show(musicService.getPosn());
                break;
            case R.id.ayuda:
                if(ayudaConectionDialogFragment==null)
                    ayudaConectionDialogFragment  = new AyudaConectionDialogFragment();
                ayudaConectionDialogFragment.show(getFragmentManager(), "ayuda");
                break;
        }
        return super.onOptionsItemSelected(item);
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
                    // TODO no se si es culpa de esto q ahora hay momentos que el conroller desaparece sin que se toque nada cuando no se debe
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


    //Metodos  de MediaPlayerControl
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
        controller.show(getCurrentPosition());
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

    /**
     * Called when an item in the navigation menu is selected.
     *
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //abrir el activity de futuras visitas
        if(item.getItemId()==R.id.action_end2){
            musicService = null;
            finish();
            return true;
            //Abre la activity del buscador.
        }else if(item.getItemId()==R.id.mostrar_barra_control2) {
            controller.show(musicService.getPosn());
            return true;
            //Abre las preferencias de la aplicación.
        }else if(item.getItemId()==R.id.ayuda2) {

            return true;
        }
        return false;
    }

    /**
     * Método para realizar las acciones del toolbar de la aplicación. Solo realiza la acción de abrir o cerrar el
     * menú lateral
     * @return
     */
    @Override
    public boolean onSupportNavigateUp() {
        //Si el menú está abierto
        if(drawerLayout.isDrawerOpen(Gravity.LEFT)){
            //Cerrará el menú
            drawerLayout.closeDrawer(Gravity.LEFT);
        }else{
            //Abrirá el menu
            drawerLayout.openDrawer(Gravity.LEFT);
        }
        return super.onSupportNavigateUp();
    }
}