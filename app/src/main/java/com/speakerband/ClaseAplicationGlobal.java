package com.speakerband;

import android.app.Application;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;

public class ClaseAplicationGlobal extends Application {



    public static ArrayList<Song> listSelection;
    public static ArrayList<Song> listQueYaHasidoEnviada;
    public static boolean estaEnElFragmentChat;
    public static boolean estaEnElFragmentSong;
    public static WifiManager wifiManager;



    @Override
    public void onCreate() {
        super.onCreate();

        listSelection = new ArrayList<Song>();
        listQueYaHasidoEnviada = new ArrayList<Song>();
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
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
            listSelection = saberSiExisteLasCancionesDeListaDePreferenciasEnELMovil();
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
            _listSangDevice = MainActivity.getSongList(getApplicationContext());
            // List con las canciones que estan en las preferencias
            _listSelectionPreferences = new ArrayList<Song>(preferences.getListSelectionPreferences(getApplicationContext()));
            // List con las canciones que hay en el dispositivo
            _realList = new ArrayList<>();

            if (!_listSangDevice.isEmpty()
                    && !_listSelectionPreferences.isEmpty())
                for (Song s : _listSangDevice) {
                    for (Song sS : _listSelectionPreferences) {
                        if (sS.getUri() != null && s.getUri() != null) {
                            if (sS.getUri().equals(s.getUri())) {
                                _realList.add(s);
                            }
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
     * Agrega una cancion a la preferencias desde cualquier parte de la aplicacion
     * @param
     */
    public void salvarTodasLasCancionesEnLaListaDePreferencess() {
        preferences.saveListSelectionPreferences(getApplicationContext(), listSelection);
    }
}
