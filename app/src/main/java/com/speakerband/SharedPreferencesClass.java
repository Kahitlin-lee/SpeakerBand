package com.speakerband;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by g_electra on 29/11/17.
 */

public class SharedPreferencesClass
{
    public static final String LIST_SELECTION = "LIST_SELECTION";

    public SharedPreferencesClass() {
        super();
    }

    /**
     * Metodo que salva la lista de canciones de la lista de seleccion en el SharedPreferences
     * @param context
     * @param listSelectionPreferences
     */
    public static void saveListSelectionPreferences(Context context, ArrayList<Song> listSelectionPreferences)
    {
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(LIST_SELECTION,
                Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonFavorites = gson.toJson(listSelectionPreferences);

        editor.putString(LIST_SELECTION, jsonFavorites);

        editor.commit();
    }

    /**
     * Metodo que agrega una cancion a la lista de seleccion del SharedPreferences
     * @param context
     * @param song
     */
    public static void addListSelectionPreferences(Context context, Song song)
    {
        List<Song> listSelectionPreferences = getListSelectionPreferences(context);
        if (listSelectionPreferences == null)
            listSelectionPreferences = new ArrayList<Song>();
        listSelectionPreferences.add(song);
        saveListSelectionPreferences(context, (ArrayList<Song>) listSelectionPreferences);
    }

    /**
     * Metodo que elimina toda la lista de seleccion del SharedPreferencesClass
     * @param context
     */
    public static void removeSongFromListSelectionPreferences(Context context)
    {
        SharedPreferences sharedPrefs = context.getSharedPreferences(LIST_SELECTION,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * Metodo que obtiene la lista de seleccion del SharedPreferences
     * @param context
     * @return
     */
    public static ArrayList<Song> getListSelectionPreferences(Context context)
    {
        SharedPreferences settings;
        List<Song> listSelectionPreferences;

        settings = context.getSharedPreferences(LIST_SELECTION,
                Context.MODE_PRIVATE);

        if (settings.contains(LIST_SELECTION)) {
            String jsonFavorites = settings.getString(LIST_SELECTION, null);
            Gson gson = new Gson();
            if(!jsonFavorites.equals(null)) {
                Song[] favoriteItems = gson.fromJson(jsonFavorites,
                        Song[].class);
                listSelectionPreferences = Arrays.asList(favoriteItems);
                listSelectionPreferences = new ArrayList<Song>(listSelectionPreferences);
                return (ArrayList<Song>) listSelectionPreferences;
            } else
            return null;
        } else
            return null;
    }
}
