package de.nullzero.cln.clicknloadforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * User: Markus Schulz <msc@0zero.de>
 */
public class BootCompletedIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Log.d(getClass().getName(), "Boot Completed, start ForwarderService");
            Intent pushIntent = new Intent(context, ForwarderServiceImpl.class);
            context.startService(pushIntent);
        }
    }

}
