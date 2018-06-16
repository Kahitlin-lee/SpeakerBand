package com.speakerband;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;

import com.speakerband.musica.MusicController;
import com.speakerband.musica.cursor.SongCursor;
import com.speakerband.musica.modelo.Song;
import com.speakerband.musica.servicios.MusicService;
import com.speakerband.utils.UtilList;

import java.util.ArrayList;
import java.util.List;

public class ClaseApplicationGlobal extends Application {



    public static ArrayList<Song> listSelection;
    public static ArrayList<Song> listSelectionClinteParaReproducir;
    public static boolean estaEnElFragmentChat;
    public static boolean estaEnElFragmentSong;
    public static boolean estaEnElFragmentMain;
    public static WifiManager wifiManager;
    private static Context mContext;

    public static MusicService musicService;
    //--
    public static boolean musicIsConnected = false;
    public static boolean soyElLider;

    public static String sourceDeviceName;

    public static boolean yaSePreguntoQuienEsElLider;
    public static boolean yaSePreguntoQuienEsElLiderCliente;
    public static String sourceDeviceNameOtroMovil;
    public static MusicController controllerSongFragmen;



    @Override
    public void onCreate() {
        super.onCreate();
        listSelection = new ArrayList<Song>();
        listSelectionClinteParaReproducir = new ArrayList<Song>();
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }

    public WifiManager getWifiManager()
    {
        return wifiManager;
    }


    // -------- Metodos que trabajan con  SharedPreferencesClass
    //Variable para el metodo relacionados con SharedPreferencesClass
    List<Song> _listSangDevice = null;
    ArrayList<Song> _realList = null;
    List<Song>  _listSelectionPreferences = null;
    SharedPreferencesClass preferences = new SharedPreferencesClass();

    //Si las preferencias no es nula la genera nuevamente con las canciones existentes
    public void generarNuevamentePreferenciasYListSeleccion() {
        if(preferences.getListSelectionPreferences(getApplicationContext()) != null) {
            eliminarYRegenrearLaPreferencess(saberSiExisteLasCancionesDeListaDePreferenciasEnELMovil());
            UtilList.sortByName(saberSiExisteLasCancionesDeListaDePreferenciasEnELMovil());
            listSelection = (ArrayList<Song>) UtilList.sortByName(saberSiExisteLasCancionesDeListaDePreferenciasEnELMovil());
        }
    }

    /**
     * Comprobar que las canciones de la lista de seleccion de SharedPreferences exista en el movil
     * en el dispositivo
     */
    public ArrayList<Song> saberSiExisteLasCancionesDeListaDePreferenciasEnELMovil() {
        if(preferences.getListSelectionPreferences(getApplicationContext()) != null) {
            //Recuperamos la lista de seleccion
            // List con las canciones que hay en el dispositivo
            _listSangDevice = SongCursor.getSongList();
            // List con las canciones que estan en las preferencias
            _listSelectionPreferences = new ArrayList<Song>(preferences.getListSelectionPreferences(getApplicationContext()));
            // List con las canciones que hay en el dispositivo
            _realList = new ArrayList<>();

            if (!_listSangDevice.isEmpty()
                    && !_listSelectionPreferences.isEmpty())
                for (Song s : _listSangDevice) {
                    for (Song sS : _listSelectionPreferences) {
                        if (sS.getUri().equals(s.getUri())) {
                            _realList.add(s);
                        }
                    }
                }
        }
        return _realList;
    }


    /**
     * Metodo que borra y genera nuevamente con nuevoas canciones la lista de preferencias
     * @param _realList lista nueva
     */
    public void eliminarYRegenrearLaPreferencess(ArrayList<Song> _realList) {
        // Eliminar la lista actual de preferencias
        preferences.removeSongFromListSelectionPreferences(getApplicationContext());

        // Volvemos a rellenar las preferencias con la lista de canciones que si existe
        preferences.saveListSelectionPreferences(getApplicationContext(), _realList );
    }

    /**
     * Agrega una cancion a la preferencias desde cualquier parte de la aplicacion
     * @param
     */
    public void agregarUnaCancionAPreferencess(Song song) {
        preferences.addUnaSongListSelectionPreferences(getApplicationContext(), song);
    }

    /**
     * Elimina una cancion a la preferencias desde cualquier parte de la aplicacion
     * @param
     */
    public void eliminarUnaCancionAPreferencess(Song song) {
        preferences.removeUnaSongListSelectionPreferences(getApplicationContext(), song);
    }

    /**
     * Agrega una cancion a la preferencias desde cualquier parte de la aplicacion
     * @param
     */
    public void salvarTodasLasCancionesEnLaListaDePreferencess() {
        if(!listSelection.isEmpty())
            preferences.saveListSelectionPreferences(getApplicationContext(), listSelection);
    }

    public  void noMostrarMasDialogo(boolean isChecked, int typeList){
        preferences.storeDialogStatus(isChecked,getApplicationContext() , typeList);
    }

    public boolean obtenerDialogStatus(int typeList){
        return preferences.getDialogStatus(getApplicationContext(), typeList );
    }

}
