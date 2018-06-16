package com.speakerband.wifibuddy;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

/*
 * Esta clase la he sacado de StackOverflow: http://stackoverflow.com/users/726954/graeme
 *  http://stackoverflow.com/a/9944203/2354849
 *  La clase WifiDirectHandler extiende de esta
 */
public abstract class NonStopIntentService extends Service {

    private String mName;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    public NonStopIntentService(String name) {
        super();
        mName = name;
    }

    /**
     *
     */
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
        }
    }

    /**
     *
     */
    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    /**
     *
     * @param intent
     * @param startId
     */
    @Override
    public void onStart(Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    /**
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_STICKY;
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    /**
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        // Auto-generated method stub
        return null;
    }

    /**
     * Este método se invoca en el hilo del trabajador con una solicitud de proceso.
     * Solo se procesa un intent a la vez, pero el procesamiento ocurre en un
     * hilo de trabajo que se ejecuta independientemente de otra lógica de aplicación.
     * Entonces, si este código lleva mucho tiempo, retendrá otras solicitudes para
     * el mismo IntentService, pero no soportará nada más.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    protected abstract void onHandleIntent(Intent intent);
}
