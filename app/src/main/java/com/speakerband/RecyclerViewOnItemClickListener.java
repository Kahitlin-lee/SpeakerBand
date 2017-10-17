package com.speakerband;

import android.view.View;

/**
 * Created by g_electra on 16/10/17.
 */

public interface  RecyclerViewOnItemClickListener
{
    void onClick(View v, int position);
    void onLongClick(View v, int position);

}
