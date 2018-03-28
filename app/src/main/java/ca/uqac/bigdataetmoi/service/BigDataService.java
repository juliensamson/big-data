package ca.uqac.bigdataetmoi.service;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ca.uqac.bigdataetmoi.MainApplication;
import ca.uqac.bigdataetmoi.database.DatabaseManager;
import ca.uqac.bigdataetmoi.database.DataCollection;
import ca.uqac.bigdataetmoi.service.info_provider.AccelerometerInfoProvider;
import ca.uqac.bigdataetmoi.service.info_provider.BasicSensorInfoProvider;
import ca.uqac.bigdataetmoi.service.info_provider.BluetoothInfoProvider;
import ca.uqac.bigdataetmoi.service.info_provider.DataReadyListener;
import ca.uqac.bigdataetmoi.service.info_provider.GPSInfoProvider;
import ca.uqac.bigdataetmoi.service.info_provider.InfoProvider;
import ca.uqac.bigdataetmoi.service.info_provider.MicroInfoProvider;
import ca.uqac.bigdataetmoi.service.info_provider.PodometerInfoProvider;
import ca.uqac.bigdataetmoi.service.info_provider.WifiInfoProvider;

/*
Créé le 2017-11-16 par Patrick Lapointe
But : Service qui récupère les infos des différents capteurs et qui envoie les données à la base de donnée.
*/

public class BigDataService extends Service implements DataReadyListener
{
    private final static int REPETITION_DELAY = 5; // Mise à jour des données au x minutes
    private final static int MAXIMUM_WAITING_TIME = 45; // On arrête le service si nous avons attendu plus de x secondes

    private List<InfoProvider> mProviders = new ArrayList<>();
    private DataCollection mDataCollection;

    // Le service va rouler à une intervalle donnée. Le but est de récupérer les données voulues puis
    // s'arrête de lui-même.
    public static void startRecurrence(Context context)
    {
        Intent intent = new Intent(context, BigDataService.class);

        // Si L'intent n'est pas déjà scédulé, on le crée
        if(PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE) == null)
        {
            PendingIntent scheduledIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager scheduler = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * REPETITION_DELAY, scheduledIntent);
        }
    }

    @Override
    public void onCreate()
    {
        mDataCollection = new DataCollection();

        // On met l'identifieur du téléphone dans la classe MainApplication
        MainApplication.setUserID(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

        Log.v("BigDataService", "BigDataService service has been created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.w("BigDataService", "BigDataService service has started");

        BasicSensorInfoProvider basicSensorProvider = new BasicSensorInfoProvider(this);
        basicSensorProvider.addDataReadyListener(this);
        mProviders.add(basicSensorProvider);

        AccelerometerInfoProvider accelerometerInfoProvider = new AccelerometerInfoProvider(this);
        accelerometerInfoProvider.addDataReadyListener(this);
        mProviders.add(accelerometerInfoProvider);

        GPSInfoProvider GPSInfoProvider = new GPSInfoProvider(this);
        GPSInfoProvider.addDataReadyListener(this);
        mProviders.add(GPSInfoProvider);

        WifiInfoProvider wifiInfoProvider = new WifiInfoProvider(this);
        wifiInfoProvider.addDataReadyListener(this);
        mProviders.add(wifiInfoProvider);

        BluetoothInfoProvider bluetoothInfoProvider = new BluetoothInfoProvider(this);
        bluetoothInfoProvider.addDataReadyListener(this);
        mProviders.add(bluetoothInfoProvider);

        PodometerInfoProvider podometerInfoProvider = new PodometerInfoProvider(this);
        podometerInfoProvider.addDataReadyListener(this);
        mProviders.add(podometerInfoProvider);

        MicroInfoProvider microInfoProvider = new MicroInfoProvider();
        microInfoProvider.addDataReadyListener(this);
        mProviders.add(microInfoProvider);

        // On n'attends pas nécéssairement d'avoir toutes les données,
        // on écris dans la bd après un délais prédéterminé.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                writeData();
            }
        }, MAXIMUM_WAITING_TIME * 1000);

        return Service.START_STICKY;
    }

    @Override
    public void dataReady(DataCollection data) {
        // Stocker les données dans le SensorDataCollection
        mDataCollection.receiveData(data);

        // Si on a stocké tous les données, il est temps d'enregistrer le tout dans la bd
        if(mDataCollection.allDataReceived())
            writeData();
    }

    @Override
    public IBinder onBind(Intent arg) {
        Log.i("BigDataService", "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        for (InfoProvider provider : mProviders)
            provider.unregisterDataReadyListener(this);

        Log.i("BigDataService", "Service onDestroy");
    }

    private void writeData() {
        // Stockage dans la bd
        DatabaseManager.getInstance().storeSensorDataCollection(mDataCollection);
        // On arrête le service
        stopSelf();
    }
}