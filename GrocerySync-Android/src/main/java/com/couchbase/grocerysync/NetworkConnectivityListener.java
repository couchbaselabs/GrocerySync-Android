package com.couchbase.grocerysync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.couchbase.lite.AsyncTask;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.util.List;

/**
 * This code was adapted from:
 *
 * https://code.google.com/p/androidwisprclient/source/browse/trunk/src/com/joan/pruebas/NetworkConnectivityListener.java?r=2
 */
public class NetworkConnectivityListener {

    private boolean listening;
    private Context context;
    private ConnectivityBroadcastReceiver receiver;
    private State state;
    private List<Database> databases;

    public enum State {
        UNKNOWN,

        /** This state is returned if there is connectivity to any network **/
        CONNECTED,
        /**
         * This state is returned if there is no connectivity to any network. This is set to true
         * under two circumstances:
         * <ul>
         * <li>When connectivity is lost to one network, and there is no other available network to
         * attempt to switch to.</li>
         * <li>When connectivity is lost to one network, and the attempt to switch to another
         * network fails.</li>
         */
        NOT_CONNECTED
    }

    public NetworkConnectivityListener(List<Database> databases) {
        this.databases = databases;
        receiver = new ConnectivityBroadcastReceiver();
    }

    /**
     * This method starts listening for network connectivity state changes.
     *
     * @param context
     */
    public synchronized void startListening(Context context) {
        if (!listening) {
            this.context = context;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(receiver, filter);
            listening = true;
        }
    }

    /**
     * This method stops this class from listening for network changes.
     */
    public synchronized void stopListening() {
        if (listening) {
            context.unregisterReceiver(receiver);
            context = null;
            listening = false;
        }
    }

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION) || listening == false) {
                Log.w(MainActivity.TAG, "onReceived() called with " + state.toString() + " and " + intent);
                return;
            }

            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

            if (noConnectivity) {
                state = State.NOT_CONNECTED;
            } else {
                state = State.CONNECTED;
            }

            if (state == State.NOT_CONNECTED) {
                for (Database database : databases) {
                    for (final Replication replication : database.getActiveReplications()) {
                        replication.getLocalDatabase().runAsync(new AsyncTask() {
                            @Override
                            public void run(Database database) {
                                replication.goOffline();
                            }
                        });
                    }
                }
            }

            if (state == State.CONNECTED) {
                for (Database database : databases) {
                    for (final Replication replication : database.getActiveReplications()) {
                        replication.getLocalDatabase().runAsync(new AsyncTask() {
                            @Override
                            public void run(Database database) {
                                if (replication.isRunning() == false) {
                                    // replication.start();
                                } else {
                                    replication.goOnline();
                                }
                            }
                        });
                    }
                }
            }

        }
    };

}
