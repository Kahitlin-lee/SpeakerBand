package com.speakerband;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by g_electra on 30/9/17.
 */

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener
{
    //media player , de la clase MediaPlayer
    private MediaPlayer mediaPlayer;
    //lista de canciones, array con las canciones
    private ArrayList<Song> songs;
    //posición actual
    private int songPosition;
    //variable de instancia que represente la clase Binder
    private final IBinder musicBind = new MusicBinder();

    /**
     * Creamos el método  onCreate
     */
    public void onCreate()
    {
        //creamos el Servicio
        super.onCreate();
        //inicializamos la posicion
        songPosition = 0;
        //Creamos el MediaPlayer
        mediaPlayer = new MediaPlayer();
        //invocamos al merodo initializeMusicPlayer();
        initializeMusicPlayer();
    }

    /**
     * Clase MusicBinder de Binder
     */
    public class MusicBinder extends Binder
    {
        MusicService getService()
        {
            return MusicService.this;
        }
    }

    /**
     * Metodo para devolver el objeto Bind (IBinder)
     * @param arg0
     * @return
     */
    @Override
    public IBinder onBind(Intent arg0)
    {
        return musicBind;
    }

    /**
     * Metodo para liberar recursos cuando la instancia de servicio no está unida
     * @param intent
     * @return
     */
    @Override
    public boolean onUnbind(Intent intent)
    {
        mediaPlayer.stop();
        mediaPlayer.release();
        return false;
    }

    /**
     * Método para inicializar la clase
     */
    public void initializeMusicPlayer()
    {
        //propiedades
        //setWakeMode: Este método puede ser llamado en cualquier estado y llamarlo no cambia el estado del objeto.
        mediaPlayer.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);//set Tipo de secuencia de audio
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    /**
     *
     * @param theSongs
     */
    public void setList(ArrayList<Song> theSongs)
    {
        songs = theSongs;
    }

    /**
     *
     */
    public void playSong()
    {
        //es necesario restablecer el MediaPlayer ya que también se usara cuando el usuario
        //esté reproduciendo las canciones.
        mediaPlayer.reset();
        //obtengo song la cancion
        Song playSong = songs.get(songPosition);
        //obtengo el  id de la cancion
        long currSong = playSong.getID();
        //cambio uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        try
        {
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error al establecer la fuente de datos", e);
        }
        //Prepara el reproductor para su reproducción, de forma asíncrona
        mediaPlayer.prepareAsync();
    }

    /**
     * Merodo para establecer la canción actual.
     * Lo llamaremos cuando el usuario escoja una canción de la lista.
     * @param songIndex
     */
    public void setSong(int songIndex)
    {
        songPosition = songIndex;
    }

    /**
     *
     * @param mediaPlayer
     */
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    /**
     *
     * @param mediaPlayer
     * @param i
     * @param i1
     * @return
     */
    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    /**
     * Dentro de este método, se inicia  la reproducción
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp)
    {
        //iniciar la reproducción
        mp.start();
    }


}
