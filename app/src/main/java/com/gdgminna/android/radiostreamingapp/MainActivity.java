package com.gdgminna.android.radiostreamingapp;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
    private boolean playPause = true;
    private boolean initialStage = true;
    private TextView textView;
    private MyService buildNotification;
    //private PlaybackStatus playbackStatus;
    //Binding this Client to the Player Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            MyService player = binder.getService();
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


        textView = findViewById(R.id.textView_play);
        imageButton = findViewById(R.id.audioStreamBtn);


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
                        textView.setText("Press the Pause button to Pause streaming");
                        initialStage = false;
                    } else {
                        if (playPause) {
                            imageButton.setImageResource(R.drawable.ic_pause_black_24dp);
                            textView.setText("Press the Pause button to Pause streaming");
                            playAudio();
                            playPause = false;
                        } else {
                            imageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                            textView.setText("Press the play button to Resume streaming");
                            // pauseAudio();
                            playAudio();
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
                    textView.setText("Press the Play button to start streaming");
                    stopAudio();

                }

            }
        });
    }

    public void checkStatues() {


        if (buildNotification(PlaybackStatus.PLAYING)) {
            imageButton.setImageResource(R.drawable.ic_pause_black_24dp);
            textView.setText("Press the pause button to Pause streaming");
        } else if (buildNotification(PlaybackStatus.PAUSED)) {
            imageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            textView.setText("Press the play button to Start streaming");
        }
    }

    private boolean buildNotification(PlaybackStatus playing) {
        return true;
    }



    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean("serviceStatus", serviceBound);
        outState.putString("serviceStatus", String.valueOf(textView));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("serviceStatus");
        checkStatues();
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
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you wish to Continue playing in the background or Stop playing and Exit?")
                .setCancelable(false)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNeutralButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stopAudio();
                        MainActivity.this.finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
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

