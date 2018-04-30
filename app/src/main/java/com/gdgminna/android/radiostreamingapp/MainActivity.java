package com.gdgminna.android.radiostreamingapp;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String Broadcast_PLAY_AUDIO = "com.gdgminna.android.radiostreamingapp.PlayAudio";
    public static final String Broadcast_PAUSE_AUDIO = "com.gdgminna.android.radiostreamingapp.PauseAudio";
    boolean serviceBound = false;
    //Binding this Client to the AudioPlayer Service
    Intent playerIntent;
    private ImageButton imageButton;
    private ProgressDialog progressDialog;
    private boolean playPause = true;
    //private ProgressDialog progressDialog;
    private boolean initialStage = true;
    private TextView textView;
    private MyService player;
    //Binding this Client to the Player Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Toast.makeText(MainActivity.this, "Service Bound Disconnected", Toast.LENGTH_SHORT).show();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkStatues(PlaybackStatus.PAUSED);

        textView = findViewById(R.id.textView_play);
        imageButton = findViewById(R.id.audioStreamBtn);

        progressDialog = new ProgressDialog(this);
        playerIntent = new Intent(this, MyService.class);


        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ConnectivityManager connMgr = (ConnectivityManager)

                        getSystemService(Context.CONNECTIVITY_SERVICE);
                assert connMgr != null;
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()) {
                    if (initialStage) {
                        playAudio();
                        imageButton.setImageResource(R.drawable.ic_pause_black_24dp);
                        initialStage = false;
                    } else {
                        if (playPause) {
                            imageButton.setImageResource(R.drawable.ic_pause_black_24dp);
                            textView.setText("Press the pause button to start streaming");
                            playAudio();
                            playPause = false;
                        } else {
                            imageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                            textView.setText("Press the play button to pause streaming");
                            pauseAudio();
                            playPause = true;
                        }
                    }
                } else if (networkInfo == null || !networkInfo.isConnected()) {
                    imageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    textView.setText("Press the play button to start streaming");
                    Snackbar.make(view, "Your device is not online, check your internet connectivity!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    //Toast.makeText(getApplicationContext(), "Device internet connection error...\nCheck your internet connectivity!", Toast.LENGTH_LONG).show();

                } else {
                    imageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    textView.setText("Press the play button to start streaming");
                    stopAudio();

                }

            }
        });
    }

    public void checkStatues(PlaybackStatus playbackStatus) {
        if (playbackStatus == PlaybackStatus.PLAYING) {
            imageButton.setImageResource(R.drawable.ic_pause_black_24dp);
            textView.setText("Press the pause button to start streaming");
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            imageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            textView.setText("Press the play button to start streaming");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean("serviceStatus", serviceBound);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("serviceStatus");
        checkStatues(PlaybackStatus.PAUSED);
    }

    private void playAudio() {
        //Check is service is active
        if (!serviceBound) {
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    private void pauseAudio() {
        Intent broadcastIntent = new Intent(Broadcast_PAUSE_AUDIO);
        sendBroadcast(broadcastIntent);
    }

    private void stopAudio() {
        stopService(playerIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         if (serviceBound) {
         unbindService(serviceConnection);
         service is active
         player.stopSelf();
         stopAudio();
         } */
    }

}

