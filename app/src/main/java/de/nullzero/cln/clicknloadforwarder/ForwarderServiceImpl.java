package de.nullzero.cln.clicknloadforwarder;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

/**
 * User: Markus Schulz <msc@0zero.de>
 */
public class ForwarderServiceImpl extends Service implements ForwarderService {

    private final IBinder forwarderServiceBinder = new ForwarderServiceBinder();
    private HttpForwarder forwarder;

    @Override
    public IBinder onBind(Intent intent) {
        return forwarderServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        forwarder = new HttpForwarder(this, "localhost", 9666);
        try {
            forwarder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }



    public class ForwarderServiceBinder extends Binder {
        public ForwarderServiceImpl getService() {
            return ForwarderServiceImpl.this;
        }
    }

}
