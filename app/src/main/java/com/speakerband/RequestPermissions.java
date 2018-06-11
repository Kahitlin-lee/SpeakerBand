package com.speakerband;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.speakerband.musica.MainActivity;

/**
 * Created by Catalina Saavedra
 * Clase con metodos para administrar los permisos de la apis mayores a la 23
 */
public class RequestPermissions
{
    /**
     * Permisos "peligrosos" no concedidos por el manifiesto en versiones >= Marshmallow
     */
    static final String[] NEEDED_PERMISSIONS = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
            , android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            , android.Manifest.permission.ACCESS_FINE_LOCATION
            , android.Manifest.permission.RECORD_AUDIO};        //TODO No sé para qué quiere grabar audio!

    View dialogoPermisos = null;
    ViewGroup vistaPadre = null;

    /**
     * Inicia una advertencia de que no tenemos permisos de lectura en el diseño de la actividad.
     *
     * @param activity Reference to LibraryActivity
     * @param intent   The intent starting the parent activity
     */
    public void showWarningWhenNeeded(final MainActivity activity, final Intent intent)
    {
        if (havePermissions(activity))
        {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        dialogoPermisos = inflater.inflate(R.layout.permission_request, null, false);

        dialogoPermisos.setOnClickListener(new View.OnClickListener()
        {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v)
            {
                requestPermissions(activity, intent);
            }
        });
        vistaPadre = (ViewGroup) activity.findViewById(R.id.relativeLayoutMainContent); // main layout of library_content
        vistaPadre.addView(dialogoPermisos, -1);
    }

    /**
     * Inicia un diálogo de solicitud de permiso si es necesario
     * Este método funciona de manera asincrónica: realiza la devolución inmediatamente y, cuando el
     * usuario responde al cuadro de diálogo, el sistema llama al método callback de la app con los resultados
     *
     * @param activity El contexto de las actividades a utilizar para la verificación de permisos
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestPermissions(Activity activity, Intent callbackIntent)
    {
        activity.requestPermissions(NEEDED_PERMISSIONS, 1);
    }

    /**
     * Comprueba si se han concedido todos los permisos requeridos     *
     *
     * @param context El contexto a utilizar
     * @return boolean true si se han creado todos los permisos
     */
    public boolean havePermissions(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            for (String permission : NEEDED_PERMISSIONS)
            {//Para comprobar si tienes un permiso, llama al metodo checkSelfPermission
                if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Metodo Eliminar Dialogo Permisos
     */
    public void eliminarDialogoPermisos()
    {
        if (vistaPadre != null && dialogoPermisos != null)
        {
            vistaPadre.removeView(dialogoPermisos);
            dialogoPermisos = null;
            vistaPadre = null;
        }
    }
}
