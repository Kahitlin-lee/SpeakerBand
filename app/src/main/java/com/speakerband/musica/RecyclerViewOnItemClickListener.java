package com.speakerband.musica;

import android.view.View;

/**
 * Created by Catalina Saavedra 16/10/17.
 */

/**
 * Interfaz que especifica el comportamiento del oyente.
 */
public interface  RecyclerViewOnItemClickListener
{
    void onClick(View v, int position);
    void onLongClick(View v, int position);
}
