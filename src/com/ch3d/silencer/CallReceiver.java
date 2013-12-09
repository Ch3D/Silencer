
package com.ch3d.silencer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver
{
    private static final String TAG = CallReceiver.class.getSimpleName();

    private TelephonyManager mTelephonyManager;

    private IncomingCallStateListener mCallListener;

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPref.getBoolean(SettingsActivity.KEY_ENABLED, false)) {
            return;
        }
        Log.d(TAG, "Received PhoneStateChanged event");
        mTelephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        final String stateExtra = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(stateExtra))
        {
            mCallListener = new IncomingCallStateListener(context);
            mTelephonyManager.listen(mCallListener,
                    PhoneStateListener.LISTEN_CALL_STATE);
        }
        else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(stateExtra)
                || TelephonyManager.EXTRA_STATE_IDLE.equals(stateExtra))
        {
            mTelephonyManager.listen(mCallListener,
                    PhoneStateListener.LISTEN_NONE);
        }
    }
}
