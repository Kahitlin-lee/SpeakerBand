package com.speakerband;

import android.content.Context;
import android.os.Environment;

import com.speakerband.utils.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by g_electra on 22/11/17.
 * Clase con metodos para el manejo de ficheros
 */

public class UtilFiles
{
    /**
     * Busca la carpeta con el nombre del fichero mas /
     * que se ingrese por parametro
     * EJ : "/SpeakerBand"
     * @param nombreFicheroEncontrar nombre del fichero que se espera encotrar en el dispositivo
     * @return
     */
    public static boolean findFolder(String nombreFicheroEncontrar)
    {
        File path = new File (Environment.getExternalStorageDirectory().getAbsolutePath());
        File fileSpeakerBand = new File(Environment.getExternalStorageDirectory() + nombreFicheroEncontrar);
        File[] files = path.listFiles();

        for (File file : files) {
            if (file.getName().equals(fileSpeakerBand.getName())) {
              files = fileSpeakerBand.listFiles();
                return true;
            }
        }
        return false;
    }

    /**
     * Busca la carpeta con el nombre del fichero mas /
     * que se ingrese por parametro
     * EJ : "/SpeakerBand"
     * @param nombreFicheroEncontrar nombre del fichero que se espera encotrar en el dispositivo
     * @return
     */
    public static String returnPath(String nombreFicheroEncontrar)
    {
        File path = new File (Environment.getExternalStorageDirectory().getAbsolutePath());
        File fileSpeakerBand = new File(Environment.getExternalStorageDirectory() + nombreFicheroEncontrar);
        File[] files = path.listFiles();

        for (File file : files) {
            if (file.getName().equals(fileSpeakerBand.getName())) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public static File[] listFileSongs(String nombreFicheroEncontrar)
    {
        File path = new File (Environment.getExternalStorageDirectory().getAbsolutePath());
        File fileSpeakerBand = new File(Environment.getExternalStorageDirectory() + nombreFicheroEncontrar);
        File[] files = path.listFiles();

        for (File file : files) {
            if (file.getName().equals(fileSpeakerBand.getName()))
            {
                return fileSpeakerBand.listFiles();
            }
        }
        return null;
    }

    /**
     * Crea el archivo de de la aplicacion Speakerband
     * @return
     */
    public static boolean  createFolderApp(Context context)
    {
        File file = new File(Environment.getExternalStorageDirectory() + Constants.NOMBRE_APP_DIRECTORIO);
        if (file.exists())
        {
             return true;
        } else
            {
            try
            {
                if (file.mkdir()) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Metodo que escribe un archivo de musica en la memoria externa
     * TODO mejorar esta explicaicon del metodo
     * @param song
     * @param nombreFicheroDondeSeEscribe nombre del fichero donde se escribira la cancion
     * @return
     */
    public static String writeSongOnExternalMemory(Song song, String nombreFicheroDondeSeEscribe)
    { //guarda las canciones en la carpeta de Speakerband del storage
        File path = Environment.getExternalStoragePublicDirectory(nombreFicheroDondeSeEscribe);
        File file = new File(path, song.getTitleWithExtension());
        FileOutputStream fileOutputStream = null; // save
        if (path.exists() && (!file.exists())) {
            try {
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(song.getSongBytes());
                fileOutputStream.flush();
                fileOutputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Get length of file in bytes
            long fileSizeInBytes = file.length();
            // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
            long fileSizeInKB = fileSizeInBytes / 1024;
            // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
            long fileSizeInMB = fileSizeInKB / 1024;

            return file.getAbsolutePath();
        }
        return null;
    }

    /**
     *
     * @param song
     * @param nombreFicheroDondeSeEscribe
     */
    public static void copyFile(Song song, String nombreFicheroDondeSeEscribe)
    {
        FileChannel source = null;
        FileChannel destination = null;
        File path = null;
        File destFile = null;
        File sourceFile = null;

        try
        {

            path = Environment.getExternalStoragePublicDirectory(nombreFicheroDondeSeEscribe);
            destFile = new File(path, song.getTitleWithExtension());
            sourceFile = new File (song.getUri());

            if (!destFile.getParentFile().exists())
                destFile.getParentFile().mkdirs();

            if (!destFile.exists()) {
                destFile.createNewFile();
            }

            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                 if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
