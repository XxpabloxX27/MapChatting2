package com.example.mapchatting2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput

class MyReciever : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v("NOTREPLY","Llego el  mensaje" + RemoteInput.getResultsFromIntent(intent).getCharSequence("REPLY"))
    }
}