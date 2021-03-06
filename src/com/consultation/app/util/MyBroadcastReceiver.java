package com.consultation.app.util;

import com.consultation.app.listener.ConsultationCallbackHandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyBroadcastReceiver extends BroadcastReceiver {

    private static ConsultationCallbackHandler callbackHandler;

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        if(callbackHandler != null){
            callbackHandler.onSuccess("", arg1.getIntExtra("isOpen", 1));
        }
    }

    public static void setHander(ConsultationCallbackHandler handler) {
        callbackHandler=handler;
    }
}
