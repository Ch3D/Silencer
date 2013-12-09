
package com.ch3d.silencer;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.ch3d.silencer.service.CallSilencerService;

public class IncomingCallStateListener extends PhoneStateListener
{
    private final Context mContext;

    private boolean isIncoming;

    public IncomingCallStateListener(final Context context)
    {
        mContext = context;
    }

    @Override
    public void onCallStateChanged(final int state, final String incomingNumber)
    {
        switch (state)
        {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                mContext.startService(new Intent(mContext, CallSilencerService.class));
                break;

            case TelephonyManager.CALL_STATE_IDLE:
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (isIncoming)
                {
                    mContext.stopService(new Intent(mContext, CallSilencerService.class));
                    isIncoming = false;
                }
                break;

            default:
                break;
        }
    }
}
