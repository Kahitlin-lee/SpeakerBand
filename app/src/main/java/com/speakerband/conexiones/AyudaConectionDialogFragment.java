package com.speakerband.conexiones;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.speakerband.MainActivity;
import com.speakerband.R;
import com.speakerband.WifiBuddy.WifiDirectHandler;

import static com.speakerband.ClaseAplicationGlobal.estaEnElFragmentChat;
import static com.speakerband.ClaseAplicationGlobal.estaEnElFragmentSong;

public class AyudaConectionDialogFragment extends DialogFragment
{
    private StringBuilder log = new StringBuilder();
    private static final String TAG = WifiDirectHandler.TAG + "AyudaDialog";

    /**
     * Creates the AlertDialog, sets the WifiDirectHandler instance, and sets the logs TextView
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater i = getActivity().getLayoutInflater();
        View rootView = i.inflate(R.layout.help_dialog, null);

        TextView helpTextView = (TextView) rootView.findViewById(R.id.helpTextView);

        AlertDialog.Builder alertDialog =  new  AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.ayuda));

        if (getActivity() instanceof MainActivity) {
            alertDialog.setMessage(getString(R.string.texto_ayuda_1));
        } else if ((getActivity() instanceof ConnectionActivity)){
            alertDialog.setMessage(getString(R.string.texto_ayuda_2));
        }else if ((getActivity() instanceof ConnectionActivity)
                && estaEnElFragmentSong){
            alertDialog.setMessage(getString(R.string.texto_ayuda_3));
        }else if ((getActivity() instanceof ConnectionActivity)
                        && estaEnElFragmentChat){
                    alertDialog.setMessage(getString(R.string.texto_ayuda_4));
        }

        alertDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.setView(rootView);

        return alertDialog.create();
    }
}

