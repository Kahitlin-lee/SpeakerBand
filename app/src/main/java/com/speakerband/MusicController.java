package com.speakerband;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.MediaController;

/**
 * Created by g_electra on 1/10/17.
 */
public class MusicController extends MediaController
{
    /**
     * Constructor
     * @param contexto
     */
    public MusicController(Context contexto){
        super(contexto);
    }

    public void hide(){}

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }
}
