package com.speakerband;

/**
 * Created by g_electra on 26/9/17.
 */
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Objects;

/**
 * He hecho la clase Serializable de caso contrario saltaria la NotSerializableException
 */
public class Song implements Serializable
{
    // Dentro de la declaración de la clase, agrega tres variables de instancia para
    // los datos que deseamos guardar de cada pista
    private long id;
    private String title;
    private String album;
    private String artist;
    private String uri;
    private byte[] songFile;
    private String titleWithExtension;
    private long songSize;
    private boolean provieneDeOtroMovil;

    /**
     * Constructor de la clase song
     * @param songID
     * @param songTitle
     * @param songAlbum
     * @param songArtist
     */
    public Song(long songID, String songTitle, String songAlbum, String songArtist, String songUri)
    {
        id = songID;
        title = songTitle;
        album = songAlbum;
        artist = songArtist;
        uri = songUri;
        titleWithExtension = uri.substring(uri.lastIndexOf("/") + 1, uri.length() ) ;
        songFile = null;
        provieneDeOtroMovil = false;
    }

    /**
     * Metodo que lee el fichero
     */
    public void readFile()
    {
        if (uri!=null) {
            final File f = new File(uri);
            if(f!=null) {
                try {
                    songFile = readFile(f);

                    long fileSizeInBytes = f.length();
                    long fileSizeInKB = fileSizeInBytes / 1024;
                    long fileSizeInMB = fileSizeInKB / 1024;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Metodo para leer el fichero de la cancion
     * @param file
     * @return
     * @throws IOException
     */
    public byte[] readFile(File file) throws IOException
    {
            // Open file
        RandomAccessFile f = null;
        try
        {
            f = new RandomAccessFile(file, "r");
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);

            // Get length of file in bytes
            long fileSizeInBytes = f.length();
            // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
            long fileSizeInKB = fileSizeInBytes / 1024;
            // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
            long fileSizeInMB = fileSizeInKB / 1024;

            songSize = fileSizeInMB;

            return data;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            f.close();
        }
    }


    /**
     * Metodo obtener la cancion en bytes
     * @return
     */
    public byte[] getSongBytes()
    {
        return songFile;
    }

    /**
     * Obtener ID
     * @return
     */
    public long getId()
    {
        return id;
    }

    /**
     * Obtener titulo
     * @return
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Obtener Album
     * @return
     */
    public String getAlbum()
    {
        return album;
    }

    /**
     * Obtener Artista
     * @return
     */
    public String getArtist()
    {
        return artist;
    }

    /**
     *
     * @return
     */
    public String getUri()
    {
        return uri;
    }

    /**
     * Obtener el nombre de la cancion con la extencion
     * @return
     */
    public String getTitleWithExtension() { return titleWithExtension; }

    /**
     * Metodo que indica si la cancion Proviene de otro telefono externo
     * Se pone en true si es externo
     * @return
     */
    public boolean getProvenincia()
    {
        return provieneDeOtroMovil;
    }

    /**
     * Metodo set la cancion en bytes
     * @param bytes
     */
    public void setSongBytes(byte[] bytes)
    {
        this.songFile = bytes;
    }

    /**
     *
     * @param uriuri
     */
    public void setUri (String uriuri) { this.uri = uriuri; }

    /**
     * Metodo que cambiamos a true o false dependiendo de si la cancion
     * objeto es de un telefono externo
     * @param p
     */
    public void setProvenincia (boolean p) {
        this.provieneDeOtroMovil = true;
    }


    /**
     * Metodo para sobreescribir el metodo equals
     * @param object
     * @return true si son iguales
     * false si no lo son
     */
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Song))
            return false;

        Song that = (Song) object;
        return  ((this.title.equals(that.title)
                ||  (this.getTitleWithExtension().equals(that.getTitle())
                ||  (this.getTitle().equals(that.getTitleWithExtension())))))
                && this.album.equals(that.album)
                && this.artist.equals(that.artist);
    }

    /**
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {

        return Objects.hash(id, title, album, artist, uri);
    }

}
