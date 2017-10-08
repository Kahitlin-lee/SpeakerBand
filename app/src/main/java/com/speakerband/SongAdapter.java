package com.speakerband;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by g_electra on 28/9/17.
 *  relacionar las canciones a la lista
 */

public class SongAdapter extends BaseAdapter
{
    private List<Song> songs;
    private LayoutInflater songInf;

    /**
     * método constructor
     * @param c
     * @param theSongs
     */
    public SongAdapter(Context c, List<Song> theSongs)
    {
        songs = theSongs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        //mapa a la disposición de la canción
        LinearLayout songLay = (LinearLayout)songInf.inflate
                (R.layout.song, parent, false);
        //obtener las vistas de título y artista
        TextView titleView = (TextView)songLay.findViewById(R.id.song_title);
        //TextView albumView = (TextView)songLay.findViewById(R.id.);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        //obtener canción usando la posición
        Song currSong = songs.get(position);
        //obtener títulos y  artista
        titleView.setText(currSong.getTITLE());
        artistView.setText(currSong.getARTIST());
        //establecer posición como etiqueta
        songLay.setTag(position);
        return songLay;
    }
}
