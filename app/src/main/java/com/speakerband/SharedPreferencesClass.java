package com.speakerband;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by g_electra on 29/11/17.
 */

public class SharedPreferencesClass
{
    /**
     *
     */
    private static final String LIST_SELECTION = "LIST_SELECTION";
    /**
     *
     */
    private Gson gson = new Gson();

    /**
     *
     */
    public SharedPreferencesClass() {
        super();
    }

    /**
     * Metodo que salva la lista de canciones de la lista de seleccion en el SharedPreferences
     * @param context
     * @param listSelectionPreferences
     */
    protected void saveListSelectionPreferences(Context context, ArrayList<Song> listSelectionPreferences)
    {
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(LIST_SELECTION,
                Context.MODE_PRIVATE);
        editor = settings.edit();

        //Comprobaremos que las canciones no contienen el array de bytes de la canción al completo.
        listSelectionPreferences=comprobarListaCanciones(listSelectionPreferences);

        String jsonFavorites = gson.toJson(listSelectionPreferences);

        editor.putString(LIST_SELECTION, jsonFavorites);

        editor.apply();
    }

    /**
     * Metodo que agrega una cancion a la lista de seleccion del SharedPreferences
     * @param context
     * @param song
     */
    public void addUnaSongListSelectionPreferences(Context context, Song song)
    {
        List<Song> listSelectionPreferences = getListSelectionPreferences(context);
        if (listSelectionPreferences == null)
            listSelectionPreferences = new ArrayList<Song>();
        listSelectionPreferences.add(song);
        saveListSelectionPreferences(context, (ArrayList<Song>) listSelectionPreferences);
        Log.d("SharedPerefencesClass", "Guardando cancion en la lista del shared preferences");
    }

    /**
     * Metodo que elimina toda la lista de seleccion del SharedPreferencesClass
     * @param context
     */
    protected void removeSongFromListSelectionPreferences(Context context)
    {
        SharedPreferences sharedPrefs = context.getSharedPreferences(LIST_SELECTION,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Metodo que obtiene la lista de seleccion del SharedPreferences
     * @param context
     * @return
     */
    protected ArrayList<Song> getListSelectionPreferences(Context context)
    {
        SharedPreferences settings;

        settings = context.getSharedPreferences(LIST_SELECTION,
                Context.MODE_PRIVATE);

        if (settings.contains(LIST_SELECTION)) {
            String jsonFavorites = settings.getString(LIST_SELECTION, null);

            if(!jsonFavorites.equals(null)) {
//                Song[] favoriteItems = gson.fromJson(jsonFavorites, Song[].class);
//                listSelectionPreferences = Arrays.asList(favoriteItems);
//                listSelectionPreferences = new ArrayList<Song>(listSelectionPreferences);
                Type type = new TypeToken<ArrayList<Song>>() {}.getType();
                ArrayList<Song> arrayList = gson.fromJson(jsonFavorites, type);

                return arrayList;
            } else
            return null;
        } else
            return null;
    }

    /**
     *  Comprobar que la lista de canciones no contengan los bites de la canción en sí para
     *  evitar guardarlos en SharedPreferences.
     * @param lista
     * @return
     */
    public  ArrayList<Song> comprobarListaCanciones (ArrayList<Song> lista)
    {
        if (lista == null )
            return lista;

        ArrayList<Song> nuevaLista = new ArrayList<Song>();

        for (Song song: lista)
        {
                if ( song.getSongBytes() == null || song.getSongBytes().length == 0)
                    nuevaLista.add(song);
                else
                    nuevaLista.add(new Song (song.getId(),song.getTitle(),song.getAlbum(),song.getArtist(),song.getUri()));
        }

        return nuevaLista;
    }
}
