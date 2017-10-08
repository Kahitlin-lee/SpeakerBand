package com.speakerband;

import android.Manifest;
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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.speakerband.connection.ConnectionActivity;

import java.util.*;

/**
 * Activity principal
 */
public class MainActivity extends AppCompatActivity
{
    static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 99;

//    private ArrayList<Song> songList;
    //TODO cambiar por RecyclerView
    private ListView songView;
    private RequestPermissions requerirPermisos;
    //--
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicIsConnected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initActionButton();
        requerirPermisos = new RequestPermissions();
        //Administra los permisos de la api mayores a la 23 y mustra el panel al usuario
        requerirPermisos.showWarningWhenNeeded(MainActivity.this, getIntent());

        //Pinta la aplicacion
        drawScreen();

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
            //musicService.setList(songList);
            musicIsConnected = true;
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


    //Metodos del ciclo de vida de la aplicacon
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
        if(playIntent==null)
        {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy()
    {
        stopService(playIntent);
        musicService.unbindService(musicConnection);
        musicService = null;
        super.onDestroy();
    }

    //---Metodos de la reproduccion junto con los anteriores

    /**
     *
     * @param view
     */
    public void songPicked(View view)
    {
        musicService.setSong(Integer.parseInt(view.getTag().toString()));
        musicService.playSong();
    }

    //Metodosj usados para administrar los permisos de la api mayores a las 26
    /**
     * Una vez aceptados los permisos este metodo es el encargado de pintar la aplicacion
     */
    public void drawScreen()
    {
        songView = (ListView) findViewById(R.id.song_list);
        List songList = getSongList();
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

    }

    private void initActionButton() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
                startActivity(intent);
            }
        });
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
            drawScreen();
        }
    }
    //Metodos que obtienen la musica local del movil y la muestra
    /**
     * método auxiliar para obtener la información del archivo de audio:
     */
    public List<Song> getSongList()
    {
        ArrayList list = new ArrayList();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return list;
        }

        //instancia de ContentResolver
        ContentResolver musicResolver = getContentResolver();
        //EXTERNAL_CONTENT_URI : URI de estilo para el volumen de almacenamiento externo "primario".
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //instancia de Cursor , sando la instancia de ContentResolver para buscar los archivos de música
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        //iterar los resultados, primero chequeando que tenemos datos válidos:
        if(musicCursor!=null && musicCursor.moveToFirst())
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


            //ordenaremos los datos para que las canciones se presenten alfabéticamente por titulo
            Collections.sort(list, new Comparator<Song>() {
                public int compare(Song a, Song b) {
                    return a.getTITLE().compareTo(b.getTITLE());
                }
            });
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch (item.getItemId())
        {
            case R.id.action_shuffle:
                break;
            case R.id.action_end:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }



}
