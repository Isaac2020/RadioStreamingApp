package com.gdgminna.android.radiostreamingapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by MAGANI on 30/10/2017.
 */
public class MyService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "com.gdgminna.android.radiostreamingapp.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.gdgminna.android.radiostreamingapp.ACTION_PAUSE";


    private static final int NOTIFICATION_ID = 1;
    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();
    public MediaPlayer mediaPlayer;
    ProgressDialog progressDialog;
    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private AudioManager audioManager;
    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };
    /**
     * Play Audio Receiver
     */
    private BroadcastReceiver playAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //A PLAY_AUDIO action received
            //reset mediaPlayer to play the Audio
            if (mediaPlayer == null) {
                stopSelf();
                stopMedia();
                mediaPlayer.reset();
                initMediaPlayer();
                buildNotification(PlaybackStatus.PLAYING);

            } else {
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }
            /**} else if (mediaPlayer.isPlaying()) {
             //  pauseMedia();
             //  buildNotification(PlaybackStatus.PAUSED);
             //Toast.makeText(MyService.this, "PAUSE", Toast.LENGTH_SHORT).show();
             } else if (!mediaPlayer.isPlaying()) {
             resumeMedia();
             buildNotification(PlaybackStatus.PLAYING);
             //Toast.makeText(MyService.this, "RESUME", Toast.LENGTH_SHORT).show();
             }*/
        }
    };

    private BroadcastReceiver pauseAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //A PAUSE_AUDIO action received
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
            /** if (mediaPlayer.isPlaying()) {
             pauseMedia();
             buildNotification(PlaybackStatus.PAUSED);
             Toast.makeText(MyService.this, "pause received", Toast.LENGTH_SHORT).show();
             } else if (!mediaPlayer.isPlaying()) {
             resumeMedia();
             buildNotification(PlaybackStatus.PLAYING);
             Toast.makeText(MyService.this, "resume received", Toast.LENGTH_SHORT).show();
             }*/
        }
    };

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);

        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the url location
            String url = "http://51.15.152.81:8476/stream.mp3";
            mediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();


    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Toast.makeText(MyService.this, "Playing", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMedia() {

        if (mediaPlayer == null)
            return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            Toast.makeText(MyService.this, "Stopping", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            //resumePosition = mediaPlayer.getCurrentPosition();
            Toast.makeText(MyService.this, "Pause stream", Toast.LENGTH_SHORT).show();

        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            // mediaPlayer.seekTo(mediaPlayer.getCurrentPosition());
            Toast.makeText(MyService.this, "Resume Stream", Toast.LENGTH_SHORT).show();
            mediaPlayer.getCurrentPosition();
            mediaPlayer.start();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mediaSession.release();
        removeNotification();
        Toast.makeText(MyService.this, "Unbinding", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(MyService.this, "Service Destroyed", Toast.LENGTH_SHORT).show();

        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
            removeAudioFocus();
        }
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        removeNotification();
        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playAudio);
        unregisterReceiver(pauseAudio);
    }

    //Handle errors
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }
    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        playMedia();
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = 0;
        if (audioManager != null) {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
        //return
    }

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Perform one-time setup procedures
        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playAudio();
        register_pauseAudio();
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null)
            return; //mediaSessionManager exists

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        }
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "PrestigeFM91.70");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);


        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            public void onStop() {
                super.onStop();
                removeNotification();
                stopForeground(true);
                //Stop the service
                stopSelf();
            }

        });
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        /**
         * Notification actions -> playbackAction()
         *  0 -> Play
         *  1 -> Pause
         */
        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_play_circle_filled_black_24dp); //replace with your own image

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(true)
                // Set the Notification style
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
                .setSmallIcon(android.R.drawable.ic_media_play)
                // Set Notification content information
                .setTicker("You are about to stream Live form Prestige")
                .setContentTitle("Prestige Radio FM")
                .setContentText("You are streaming Live")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentInfo("Music is playing")
                // Add playback actions
                .addAction(notificationAction, "pause", play_pauseAction);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notificationBuilder.setContentIntent(contentIntent);

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
        assert ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)) != null;
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MyService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null)
            return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        }
    }

    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    private void register_playAudio() {
        //Register playMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_AUDIO);
        registerReceiver(playAudio, filter);
    }

    private void register_pauseAudio() {
        //Register PauseMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PAUSE_AUDIO);
        registerReceiver(pauseAudio, filter);
    }

    class LocalBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }

    }
}



