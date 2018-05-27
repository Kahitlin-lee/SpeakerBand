package com.speakerband;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;

import com.speakerband.utils.UtilList;

import java.util.ArrayList;
import java.util.List;

import static com.speakerband.ClaseAplicationGlobal.listSelection;
import static com.speakerband.ClaseAplicationGlobal.myList;

public class SongCursor {

    private ClaseAplicationGlobal mApplication  = (ClaseAplicationGlobal) getApplicationContext();

    //Metodos que obtienen la musica local del movil y asi poder mostrarla

    /**
     * Método auxiliar para obtener la información del archivo de audio:
     * Obtiene la lista de todas las canciones que estan en el dispositivo
     */
    public static List<Song> getSongList(Context context)
    {
        Cursor _musicCursor;

        ArrayList list = new ArrayList();
        if (android.support.v4.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return list;
        }

        //instancia de ContentResolver
        ContentResolver musicResolver = context.getContentResolver();
        //EXTERNAL_CONTENT_URI : URI de estilo para el volumen de almacenamiento externo "primario".
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //instancia de Cursor , usando la instancia de ContentResolver para buscar los archivos de música
        _musicCursor = musicResolver.query(musicUri, null, null, null, null);

        //iterar los resultados, primero chequeando que tenemos datos válidos:
        if(_musicCursor != null && _musicCursor.moveToFirst())
        {
            //get Columnas
            int titleColumn = _musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = _musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int albumColumn = _musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int artistColumn = _musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int uriDataColumn = _musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);

            //add songs a la lista
            do
            {
                long thisId = _musicCursor.getLong(idColumn);
                String thisTitle = _musicCursor.getString(titleColumn);
                String thisAlbum = _musicCursor.getString(albumColumn);
                String thisArtist = _musicCursor.getString(artistColumn);
                String thisUri = _musicCursor.getString(uriDataColumn);
                list.add(new Song(thisId, thisTitle, thisAlbum, thisArtist, thisUri));
            }
            while (_musicCursor.moveToNext());
        }//Por defecto lo ordena por nombre de cancion
        UtilList.sortByName(list);

        if (_musicCursor != null )
            _musicCursor.close();

        myList = list;
        return list;
    }


    /**
     * Método que recupera la lista de canciones de los ficheros del dispositivo que existan en la lista de preferencias
     * y las muestra en la pestaña correspondiente a las canciones de la lista de reproduccion
     *  que se comparten entre archivos.
     */
    public List<Song> getSongListSelection(Context context)
    {
        Cursor _musicCursor;

        ArrayList list = new ArrayList();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return list;
        }

        //instancia de ContentResolver
        ContentResolver musicResolver = context.getContentResolver();
        //EXTERNAL_CONTENT_URI : URI de estilo para el volumen de almacenamiento externo "primario".
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //instancia de Cursor , usando la instancia de ContentResolver para buscar los archivos de música
        _musicCursor = musicResolver.query(musicUri, null, null, null, null);

        //iterar los resultados, primero chequeando que tenemos datos válidos:
        if(_musicCursor != null && _musicCursor.moveToFirst())
        {
            //get Columnas
            int titleColumn = _musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = _musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int albumColumn = _musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int artistColumn = _musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int uriDataColumn = _musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            // Lista de canciones que existen actualmente en la lista de preferencias
            ArrayList<Song> _currentList = mApplication.saberSiExisteLasCancionesDeListaDePreferenciasEnELMovil();

            //add songs a la lista
            do
            {
                long thisId = _musicCursor.getLong(idColumn);
                String thisTitle = _musicCursor.getString(titleColumn);
                String thisAlbum = _musicCursor.getString(albumColumn);
                String thisArtist = _musicCursor.getString(artistColumn);
                String thisUri = _musicCursor.getString(uriDataColumn);

                song = new Song(thisId, thisTitle, thisAlbum, thisArtist, thisUri);
                if (_currentList != null && !_currentList.isEmpty()) {
                    for (Song s : _currentList)
                    {
                        if (s.getUri().equals(song.getUri())) {
                            list.add(song);
                        }
                    }
                }
            }
            while (_musicCursor.moveToNext());
        }//Por defecto lo ordena por nombre de cancion
        UtilList.sortByName(list);

        if (_musicCursor != null )
            _musicCursor.close();
        // TODO solo por probar que sea el el preferences me este jodiendo el tamaño de las canvciones
        listSelection = list;
        return list;
    }

    //---------------

}
