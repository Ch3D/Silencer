
package com.ch3d.silencer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

public class CallSilencerService extends Service implements SensorEventListener
{
    private static final int STREAM_TYPE = AudioManager.STREAM_RING;
    private static final String TAG = CallSilencerService.class.getSimpleName();
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float EPSILON = 0;
    private static final float DEGREE_PERCENT = 180f / 100f;

    private final float[] deltaRotationVector = new float[4];

    private int angleTotal = 0;

    private long timestamp;

    private SensorManager mSensorManager;
    private AudioManager mAudioManager;
    private int mInitialVolume;
    private int mInitialRinger;

    private void changeRingerMode(final int mode) {
        if (mAudioManager.getRingerMode() != mode) {
            mAudioManager.setRingerMode(mode);
        }
    }

    private boolean checkSensor()
    {
        return !mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).isEmpty();
    }

    @Override
    public void onAccuracyChanged(final Sensor arg0, final int arg1)
    {

    }

    @Override
    public IBinder onBind(final Intent arg0)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "Starting service...");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mInitialVolume = mAudioManager.getStreamVolume(STREAM_TYPE);
        mInitialRinger = mAudioManager.getRingerMode();
        if (!checkSensor())
        {
            Log.w(TAG, "Service stopped : appropriate sensor not found");
            stopSelf();
        }
        final Sensor gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroSensor == null)
        {
            Log.w(TAG, "Service stopped : unable to get default gyroscope sensor");
            stopSelf();
        }
        Log.d(TAG, "Service started");
        mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "Service stopping...");
        mSensorManager.unregisterListener(this);
        restoreAudioSettings();
    }

    @Override
    public void onSensorChanged(final SensorEvent event)
    {
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            final float omegaMagnitude = (float) Math.sqrt((axisX * axisX) + (axisY * axisY)
                    + (axisZ * axisZ));

            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            final float thetaOverTwo = (omegaMagnitude * dT) / 2.0f;
            final float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            final float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
            deltaRotationVector[0] = sinThetaOverTwo * axisX;
            deltaRotationVector[1] = sinThetaOverTwo * axisY;
            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;
        }
        timestamp = event.timestamp;
        final float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

        final float[] orientation = new float[3];
        SensorManager.getOrientation(deltaRotationMatrix, orientation);
        // final long o1 = Math.round(Math.toDegrees(orientation[0]));
        // final long o2 = Math.round(Math.toDegrees(orientation[1]));
        final long o3 = Math.round(Math.toDegrees(orientation[2]));
        angleTotal += o3;
        final int vol = (int) ((180 - Math.abs(angleTotal)) / DEGREE_PERCENT);
        final float volUnit = mInitialVolume / 100f;
        final int finalVol = (int) (volUnit * vol);
        if (finalVol < 1) {
            changeRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else {
            changeRingerMode(mInitialRinger);
        }
        mAudioManager.setStreamVolume(STREAM_TYPE, finalVol, 0);
        // Log.d(TAG, "angle = " + angleTotal);
        // Log.d(TAG, "angle vol = " + vol);
        // Log.d(TAG, "angle totalVol = " + totalVol);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId)
    {
        return START_NOT_STICKY;
    }

    private void restoreAudioSettings() {
        changeRingerMode(mInitialRinger);
        mAudioManager.setStreamVolume(STREAM_TYPE, mInitialVolume, 0);
    }
}
