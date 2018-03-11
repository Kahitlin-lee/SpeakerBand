package com.speakerband;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

/**
 * Created by Catalina Saavedra
 */
public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SongViewHolder>
{

    List<Song> list = Collections.emptyList();
    Context context;
    private RecyclerViewOnItemClickListener recyclerViewOnItemClickListener;

    public SongsAdapter(List<Song> list, Context context, RecyclerViewOnItemClickListener
                                      recyclerViewOnItemClickListener)
    {
        this.list = list;
        this.context = context;
        this.recyclerViewOnItemClickListener = recyclerViewOnItemClickListener;
    }

    /**
     *
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        //Inflate the layout, inicializo the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song, parent, false);
        return new SongViewHolder(v);
    }

    /**
     * Metodo para especificar el contenido de cada elemento del RecyclerView
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(SongViewHolder holder, int position)
    {
        // Se usa el Titular de vista provisto en el método onCreateViewHolder
        // para completar la fila actual en el RecyclerView
        // cogemos la cancion en su posición
        Song song = list.get(position);
        //el titulo de la cancion
        holder.title.setText(song.getTitle());
        //nombre del grupo o artista
        holder.artist.setText(song.getArtist());
    }

    /**
     * Como mis datos están en forma de una List,
     * sólo necesitamos llamar al método size en el objeto List
     * @return
     */
    @Override
    public int getItemCount()
    {
        //Devuelve la cantidad de elementos que mostrará RecyclerView.
        return list.size();
    }

    /**
     * Clase SongViewHolder
     */
    class SongViewHolder extends RecyclerView.ViewHolder implements View
            .OnClickListener, View.OnLongClickListener
    {
        TextView title;
        TextView artist;

        public SongViewHolder(View itemView)
        {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.song_title);
            artist = (TextView) itemView.findViewById(R.id.song_artist);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }

        /**
         * listener para cada elemento de una colección, con click
         * @param v
         */
        public void onClick(View v)
        {//getAdapterPosition() que devolverá la posición del item asociado al SongViewHolder
            recyclerViewOnItemClickListener.onClick(v, getAdapterPosition());
        }

        /**
         *  listener para cada elemento de una colección, con longClick
         * @param v
         * @return
         */
        public boolean onLongClick(View v)
        {//getAdapterPosition() que devolverá la posición del item asociado al SongViewHolder
            recyclerViewOnItemClickListener.onLongClick(v, getAdapterPosition());
            return true;
        }
    }
}
