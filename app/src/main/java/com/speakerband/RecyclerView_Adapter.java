package com.speakerband;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

/**
 * Created by Valdio Veliu on 16-07-08.
 */
public class RecyclerView_Adapter extends RecyclerView.Adapter<RecyclerView_Adapter.ViewHolder>
{

    List<Song> list = Collections.emptyList();
    Context context;
    private RecyclerViewOnItemClickListener recyclerViewOnItemClickListener;

    public RecyclerView_Adapter(List<Song> list, Context context, RecyclerViewOnItemClickListener
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        //Inflate the layout, inicializo the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    /**
     * Metodo para especificar el contenido de cada elemento del RecyclerView
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        // Se usa el Titular de vista provisto en el método onCreateViewHolder
        // para completar la fila actual en el RecyclerView
        //cogemos la cancion en su posición
        Song song = list.get(position);
        //el titulo de la cancion
        holder.title.setText(song.getTITLE());
        //nombre del grupo o artista
        holder.artist.setText(song.getARTIST());
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
     *
     * @param recyclerView
     */
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }


    class ViewHolder  extends RecyclerView.ViewHolder implements View
            .OnClickListener, View.OnLongClickListener
    {
        TextView title;
        TextView artist;

        public ViewHolder(View itemView)
        {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.song_title);
            artist = (TextView) itemView.findViewById(R.id.song_artist);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }

        public void onClick(View v)
        {
            recyclerViewOnItemClickListener.onClick(v, getAdapterPosition());
        }

        public boolean onLongClick(View v)
        {
            recyclerViewOnItemClickListener.onLongClick(v, getAdapterPosition());
            return true;
        }

        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Select Action");
            MenuItem edit = menu.add(Menu.NONE,1,1,"Agregar a la lista de reproduccion");

            edit.setOnMenuItemClickListener(onChange);
        }
        private final MenuItem.OnMenuItemClickListener onChange = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case 1:
                        Toast.makeText(context,"Edit", Toast.LENGTH_LONG).show();
                        return true;

                }
                return false;
            }
        };

    }
}