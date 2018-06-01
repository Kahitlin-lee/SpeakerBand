package com.speakerband.conexiones;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.speakerband.R;
import com.speakerband.WifiBuddy.WifiDirectHandler;

public class AyudaConectionDialogFragment extends DialogFragment
{
    private StringBuilder log = new StringBuilder();
    private static final String TAG = WifiDirectHandler.TAG + "LogsDialog";

    /**
     * Creates the AlertDialog, sets the WifiDirectHandler instance, and sets the logs TextView
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
       //nada de momento
        // Sets the Layout for the UI
        LayoutInflater i = getActivity().getLayoutInflater();
        View rootView = i.inflate(R.layout.fragment_logs_dialog, null);
        // Creates and returns the AlertDialog for the logs
        AlertDialog.Builder dialogBuilder =  new  AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.title_logs))
                .setNegativeButton(getString(R.string.action_close),
                        new DialogInterface.OnClickListener() {
                            /**
                             * Closes the Dialog when the Close Button is tapped
                             */
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                ).setView(rootView);
        return dialogBuilder.create();

    }
}
