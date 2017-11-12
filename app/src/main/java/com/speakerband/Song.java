package com.speakerband;

/**
 * Created by g_electra on 26/9/17.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;

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

    /**
     *
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
    }

    /**
     * Metodo que lee el fichero
     */
    public void readFile()
    {
        final File f = new File(uri);

        try {
            songFile = readFile(f);
        } catch (IOException e) {
            e.printStackTrace();
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


    public byte[] getSongBytes()
    {
        return songFile;
    }

    public void setSongBytes(byte[] bytes)
    {
        this.songFile = bytes;
    }

    /**
     * @return
     */
    public long getId()
    {
        return id;
    }

    /**
     *
     * @return
     */
    public String getTitle()
    {
        return title;
    }

    /**
     *
     * @return
     */
    public String getAlbum()
    {
        return album;
    }

    /**
     *
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


    public String getTitleWithExtension() { return titleWithExtension; }

    public void setUri (String uriuri) { this.uri = uriuri; }
}
