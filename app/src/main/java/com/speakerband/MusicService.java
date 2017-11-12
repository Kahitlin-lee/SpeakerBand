package com.speakerband;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.content.ContentUris;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Created by g_electra on 30/9/17.
 */

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener
{
    //media player , de la clase MediaPlayer
    private MediaPlayer mediaPlayer;
    //canción actual
    private Song song;
    //variable de instancia que represente la clase Binder
    private final IBinder musicBind = new MusicBinder();
    //posiblemente esto lo podre volver a quitar
    //pero hasta qu eno funcione mi app con normalidad no lo sacare
    //posición actual
    private int songPosition;
    //lista de canciones, array con las canciones
    private List<Song> songs;
    //
    private String songTitle = "";
    //variables para la modalidad aleatoria
    private boolean mezclar = false;
    private Random random;
    ////
    private static final int NOTIFY_ID = 1;
    private NotificationManager nm;
    public static final String BROADCAST_PLAYBACK_STOP = "stop",
            BROADCAST_PLAYBACK_PAUSE = "pause";
    /**
     * Para la manipulación de notificaciones
     */
    final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(BROADCAST_PLAYBACK_STOP)) stopSelf();
            else if (action.equals(BROADCAST_PLAYBACK_PAUSE))
            {//si esta pausado lo hace correr y esta corriendo lo pausa
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                else mediaPlayer.start();
            }
        }
    };

    /**
     * Creamos el método  onCreate
     */
    public void onCreate()
    {
        //creamos el Servicio
        super.onCreate();
        //inicializamos la posicion
        //Creamos el MediaPlayer
        mediaPlayer = new MediaPlayer();
        //invocamos al merodo initializeMusicPlayer();
        initializeMusicPlayer();
        //inicializamos la posicion
        songPosition = 0;
        //instancio para generar numero aleatorios
        random = new Random();
        //creamos la notificacion
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_PLAYBACK_STOP);
        intentFilter.addAction(BROADCAST_PLAYBACK_PAUSE);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * Clase MusicBinder de Binder
     */
    public class MusicBinder extends Binder
    {
        public MusicService getService()
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
    public void setList(List<Song> theSongs)
    {
        songs = theSongs;
    }

    /**
     *
     */
    public void playSong()
    {
        Uri trackUri;
        //es necesario restablecer el MediaPlayer ya que también se usara cuando el usuario
        //esté reproduciendo las canciones.
        mediaPlayer.reset();
        //obtengo el  id de la cancion
        long currSong = song.getId();
        //cambio uri
        trackUri= ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try
        {
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
            //Prepara el reproductor para su reproducción, de forma asíncrona
            mediaPlayer.prepareAsync();

        } catch (IOException ex) {
            Log.d("", "create failed:", ex);
        // fall through
        } catch (IllegalArgumentException ex) {
            Log.d("", "create failed:", ex);
            // fall through
        } catch (SecurityException ex) {
            Log.d("", "create failed:", ex);
            // fall through
        } catch(Exception e){
            Log.e("MUSIC SERVICE", "Error al establecer la fuente de datos", e);
        }
    }

    /**
     * Metodo para establecer la canción actual.
     * Lo llamaremos cuando el usuario escoja una canción.
     * @param song
     */
    public void setSong(Song song)
    {
        this.song = song;
    }

    /**
     *
     * @param
     */
    @Override
    public void onCompletion(MediaPlayer mp)
    {
        if(mp.getCurrentPosition() > 0)
        {
            mp.reset();
            playNext();
        }
    }

    /**
     *
     * @param
     * @param i
     * @param i1
     * @return
     */
    @Override
    public boolean onError(MediaPlayer mp, int i, int i1)
    {
        mp.reset();
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
        //
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        showNotification();
        //startForeground(NOTIFY_ID, not);
    }

    //__

    /**
     * Mostrar una notificación mientras este servicio se está ejecutando.
     */
    private void showNotification()
    {
        songTitle = song.getTitle();
        String songArtist = song.getArtist();
        // Usaremos el mismo texto para el ticker y la expandir la notificacion
        CharSequence text = songTitle + ", " + songArtist;
        //Se utiliza una PendingIntent para especificar la acción que debe realizarse una vez que el usuario seleccione la notificación.
        //El PendingIntent para iniciar nuestra actividad si el usuario selecciona esta notificación
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Establece la información de las vistas que se muestran en el panel de notificaciones.
        Notification.Builder notification = new Notification.Builder(this);
        notification.setSmallIcon(R.drawable.musicplayer)  //El icono de estado
                .setTicker(text)  // El texto
                .setWhen(System.currentTimeMillis())  // El tiempo
                .setContentTitle(getText(R.string.app_name))  //Etiqueta de entrada
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(contentIntent)  // El intentdel click de entrada
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0))
                .addAction(R.drawable.stop3, "Stop", makePendingIntent(BROADCAST_PLAYBACK_STOP))
                .addAction(R.drawable.pause, "Pause/Play", makePendingIntent(BROADCAST_PLAYBACK_PAUSE));

        //nm.notify(NOTIFY_ID, notification.build( )); al usar este sigue cusando elimino la app
        startForeground (NOTIFY_ID , notification.build( ) ) ;
    }

    /**
     * Post a notification to be shown in the status bar.
     *
     * @param id the id of this notification.
     * @param notification the notification to display.
     */
    public void notify(int id, Notification notification)
    {
        nm.notify(id, notification);
    }

    /**
     *
     * @param broadcast
     * @return
     */
    private PendingIntent makePendingIntent(String broadcast)
    {
        Intent intent = new Intent(broadcast);
        return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
    }

    //Controles de la reproduccion
    //Todos estos métodos se aplican a las funciones de control de
    //reproducción estándar que el usuario esperara.
    /**
     *
     * @return
     */
    public int getPosn()
    {
        return mediaPlayer.getCurrentPosition();
    }

    /**
     *
     * @return
     */
    public int getDur(){
        return mediaPlayer.getDuration();
    }

    /**
     *
     * @return
     */
    public boolean isPng(){
        return mediaPlayer.isPlaying();
    }

    /**
     *
     */
    public void pausePlayer(){
        if(mediaPlayer.isPlaying() == true)
            mediaPlayer.pause();
        else
            mediaPlayer.start();
    }

    /**
     *
     * @param posn
     */
    public void seek(int posn){
        mediaPlayer.seekTo(posn);
    }

    /**
     *
     */
    public void go(){
        mediaPlayer.start();
    }

    //Metodos para pasar a la cancion anterior y siguiente
    /**
     * Metodo para ir a la cancion anteior
     */
    public void playPrev()
    {
        songPosition = songs.indexOf(song);
        songPosition--;
        if(songPosition >= 0)
            song = songs.get(songPosition);
        //lo pongo a falso ya que el unico que podra manejar esto es el lider
        playSong();
    }

    /**
     * Pasar a la siguiente cancion
     */
    public void playNext()
    {
        if(mezclar)
        {
            songPosition = songs.indexOf(song);
            int newSong = songPosition;
            while(newSong == songPosition)
            {
                newSong = random.nextInt(songs.size());
            }
            songPosition = newSong;
        }
        else
        {
            songPosition = songs.indexOf(song);
            songPosition++;
            if(songPosition >= songs.size())
                songPosition = 0;
        }
        song = songs.get(songPosition);
        //lo pongo a falso ya que el unico que podra manejar esto es el lider
        playSong();
    }

    /**
     * Metodo que establece si esta en modo random o no
     */
    public void setMezclar()
    {
        if(mezclar)
            mezclar=false;
        else mezclar = true;
    }

    /**
     * Termina el servicio
     */
    @Override
    public void onDestroy()
    {
        stopForeground(true);
        nm.cancel(NOTIFY_ID);
        mediaPlayer.stop();
    }
}
