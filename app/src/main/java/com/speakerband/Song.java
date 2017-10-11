package com.speakerband;

/**
 * Created by g_electra on 26/9/17.
 */

import java.io.Serializable;
import java.util.List;

/**
 * He hecho la clase Serializable de caso contrario saltaria la NotSerializableException
 */
public class Song implements Serializable
{
    // Dentro de la declaración de la clase, agrega tres variables de instancia para
    // los datos que deseamos guardar de cada pista
    private long id;
    private String title;
    private String  album;
    private String  artist;

    /**
     *
     * @param songID
     * @param songTitle
     * @param songAlbum
     * @param songArtist
     */
    public Song(long songID, String songTitle, String songAlbum, String songArtist)
    {
        id = songID;
        title = songTitle;
        album = songAlbum;
        artist = songArtist;
    }




    //agrega métodos get para estas variables de instancia:
    /**
     *
     * @return
     */
    public long getID()
    {
        return id;
    }

    /**
     *
     * @return
     */
    public String getTITLE()
    {
        return title;
    }

    /**
     *
     * @return
     */
    public String getALBUM()
    {
        return album;
    }

    /**
     *
     * @return
     */
    public String getARTIST()
    {
        return artist;
    }

}
