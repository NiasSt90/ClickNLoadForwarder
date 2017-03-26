package de.nullzero.cln.clicknloadforwarder;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;

/**
 * User: Markus Schulz <msc@0zero.de>
 */
public class ForwarderServiceImpl extends Service implements ForwarderService {

    private HttpForwarder forwarder;

    @Override
    public void onCreate() {
        super.onCreate();
        forwarder = new HttpForwarder(this, "localhost", 9666);
        try {
            forwarder.start();
        } catch (IOException e) {
            e.printStackTrace();
            final NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.ic_cloud_done_white_24dp)
                    .setContentTitle("Fehler beim Server-Start")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(e.getMessage()));
            notifyManager.notify(0, builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
