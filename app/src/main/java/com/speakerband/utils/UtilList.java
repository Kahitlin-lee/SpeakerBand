package com.speakerband.utils;


import com.speakerband.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by g_electra on 9/10/17.
 * Array que usare para coger las canciones seleccionadas para ser reproducidas en todos los
 * dispositivos de forma general en toda la app
 * No estoy segura si esto me facilitara el trabajo, pero lo hago igual
 */
public class UtilList
{
    public static ArrayList<Song> listSelection = new ArrayList<Song>();
    public static Song songPlaying ;

    //Metodos de ordenacion
    /**
     * Metodo que ordena las canciones por nombre
     * @param sl
     * @return
     */
    public static List sortByName(List sl)
    {
        //ordenaremos los datos para que las canciones se presenten alfabéticamente por titulo
        Collections.sort(sl, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        return sl;
    }

    /**
     * Metodo que ordena las canciones por artista
     * @param sl
     * @return
     */
    public static List sortByArtist(List sl)
    {
        //ordenaremos los datos para que las canciones se presenten alfabéticamente por titulo
        Collections.sort(sl, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getArtist().compareTo(b.getArtist());
            }
        });
        return sl;
    }
}
