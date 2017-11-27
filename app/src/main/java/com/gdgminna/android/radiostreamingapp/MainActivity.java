package com.gdgminna.android.radiostreamingapp;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    boolean serviceBound = false;
    private ImageButton imageButton;
    private boolean playPause;
    private MediaPlayer mediaPlayer;
    private ProgressDialog progressDialog;
    private boolean initialStage = true;
    private TextView textView;
    private MyService player;
    //Binding this Client to the AudioPlayer Service
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


        textView = findViewById(R.id.textView_play);
        imageButton = findViewById(R.id.audioStreamBtn);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        progressDialog = new ProgressDialog(this);
        playAudio();


        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ConnectivityManager connMgr = (ConnectivityManager)

                        getSystemService(Context.CONNECTIVITY_SERVICE);
                assert connMgr != null;
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();


                if (!playPause && networkInfo != null && networkInfo.isConnected()) {
                    imageButton.setImageResource(R.drawable.ic_pause_black_24dp);
                    textView.setText("You are Streaming Live");

                    if (initialStage) {
                        new Player().execute("http://197.211.34.47:88/broadwavehigh.mp3");
                        playAudio();
                    } else {
                        if (!mediaPlayer.isPlaying())
                            mediaPlayer.start();

                    }
                    playPause = true;

                } else if (networkInfo == null) {
                    textView.setText("Press the play button to start streaming");
                    // Snackbar.make(view, "Your device is not online, check your internet connectivity!", Snackbar.LENGTH_LONG)
                    //       .setAction("Action", null).show();
                    Toast.makeText(getApplicationContext(), "Device internet connection error...\nCheck your internet connectivity!", Toast.LENGTH_LONG).show();

                } else {
                    imageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    textView.setText("Press the play button to start streaming");

                    if (mediaPlayer.isPlaying())
                        mediaPlayer.pause();
                    playPause = false;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void playAudio() {
        //Check is service is active
        if (!serviceBound) {
            Intent playerIntent = new Intent(this, MyService.class);
            playerIntent.putExtra("Media url", "http://197.211.34.47:88/broadwavehigh.mp3");
            startService(playerIntent);
            Toast.makeText(MainActivity.this, "Service Bound now connected", Toast.LENGTH_SHORT).show();
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {

        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }

    class Player extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            Boolean prepared;

            try {
                mediaPlayer.setDataSource(strings[0]);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        initialStage = true;
                        playPause = false;
                        imageButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                        textView.setText("Press the play button to start streaming");
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                    }
                });

                mediaPlayer.prepare();
                prepared = true;

            } catch (IllegalArgumentException e) {
                Log.d("Illegal Argument", e.getMessage());
                prepared = false;
                e.printStackTrace();
            } catch (SecurityException e) {
                Log.d("Security Exception", e.getMessage());
                prepared = false;
                e.printStackTrace();
            } catch (IllegalStateException e) {
                Log.d("Illegal State Exception", e.getMessage());
                prepared = false;
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("IO Exception", e.getMessage());
                prepared = false;
                e.printStackTrace();
            }

            return prepared;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (progressDialog.isShowing()) {
                progressDialog.cancel();
            }
            Log.d("Prepared", "//" + result);
            mediaPlayer.start();
            initialStage = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.setMessage("Buffering....");
            progressDialog.show();
        }
    }


}

