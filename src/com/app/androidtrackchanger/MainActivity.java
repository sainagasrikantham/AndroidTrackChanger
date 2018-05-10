package com.app.androidtrackchanger;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

    /* Buttons Enumeration */
    private enum ButtonsE
    {
        BUTTON_PREVIOUS,
        BUTTON_PLAY_PAUSE,
        BUTTON_NEXT,
        BUTTON_REPEAT,
        BUTTON_PREVIOUS_SONG_INFO,
        BUTTON_NEXT_SONG_INFO
    };

    /* Play/Pause Button State Enumeration */
    private enum PlayPauseStateE
    {
        STATE_PLAYING,
        STATE_PAUSED
    };

    /* 'Blue' image fling enumeration */
    private enum FlingBlueIconPosE
    {
        POSITION_LEFT,
        POSITION_MIDDLE,
        POSITION_RIGHT
    }

    /* Track Change enumeration */
    private enum TrackChangeE
    {
        TRACK_CHANGED_TO_NEXT,
        TRACK_CHANGED_TO_PREVIOUS
    }

    /* Track Info enumeration */
    private enum TrackInfoE
    {
        TRACK_INFO_CURRENT,
        TRACK_INFO_PREVIOUS,
        TRACK_INFO_NEXT
    }

    /* Global creation flag */
    private boolean mResourcesCreated = false;

    /* Previous Button */
    private Button    			 mPreviousButton 					 = null;
    private Drawable  			 mPreviousButtonDrawableImage 		 = null;
    private Drawable  		     mPreviousButtonPressedDrawableImage = null;
    private View.OnClickListener mPreviousButtonListener			 = null;

    /* Play/Pause Button */
    private Button    			 mPlayPauseButton					 = null;
    private Drawable  			 mPlayButtonDrawableImage			 = null;
    private Drawable  			 mPlayButtonPressedDrawableImage 	 = null;
    private Drawable  			 mPauseButtonDrawableImage			 = null;
    private Drawable  			 mPauseButtonPressedDrawableImage	 = null;
    private View.OnClickListener mPlayPauseButtonListener			 = null;

    /* Next Button */
    private Button    			 mNextButton 						 = null;
    private Drawable  			 mNextButtonDrawableImage			 = null;
    private Drawable  			 mNextButtonPressedDrawableImage	 = null;
    private View.OnClickListener mNextButtonListener				 = null;

    /* Repeat Button */
    private Button    			 mRepeatButton						 = null;
    private Drawable  			 mRepeatButtonActiveDrawableImage	 = null;
    private Drawable  			 mRepeatButtonInactiveDrawableImage	 = null;
    private View.OnClickListener mRepeatButtonListener				 = null;

    /* Current State of the music */
    private PlayPauseStateE mPlayPauseState;

    /* Audio Manager */
    private AudioManager mAudioManager;

    /* Image change delay */
    private int mSleepDelay 	 = 100;
    private int mFlingIconDelay  = 500;
    private int mPlaybackDelay   = 500;

    /* Constants */
    private int ONE_SECOND       = 1000;
    private int SECS_IN_A_MINUTE = 60;

    /* UI Refresh Resources */
    private Runnable 	   mUIRefreshThread      = null;
    private Runnable 	   mTrackDurationThread  = null;
    private Handler  	   mHandler          	 = null;
    private CountDownTimer mPreviousButtonTimer  = null;
    private CountDownTimer mPlayButtonTimer      = null;
    private CountDownTimer mPauseButtonTimer     = null;
    private CountDownTimer mNextButtonTimer      = null;
    private CountDownTimer mPlaybackStartTimer   = null;

    /* Previous track fling resources */
    private ImageView mPreviousFlingPosLeft   = null;
    private ImageView mPreviousFlingPosMiddle = null;
    private ImageView mPreviousFlingPosRight  = null;

    private int mPreviousFlingGrayImage;
    private int mPreviousFlingBlueImage;

    /* Next track fling resources */
    private ImageView mNextFlingPosLeft   = null;
    private ImageView mNextFlingPosMiddle = null;
    private ImageView mNextFlingPosRight  = null;

    private int mNextFlingGrayImage;
    private int mNextFlingBlueImage;

    /* Fling 'Blue' icon position */
    private FlingBlueIconPosE mPreviousTrackFlingBlueIconPos;

    /* Gesture/fling detection resources */
    private static final int SWIPE_MIN_DISTANCE 	  = 35;
    private static final int SWIPE_MAX_OFF_PATH 	  = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private GestureDetector mSkipTrackGestureDetector = null;
    View.OnTouchListener    mSkipTrackGestureListener = null;

    /* Application Context */
    private Context mApplicationContext = null;

    /* Notification Manager */
    NotificationManager mNotificationManager = null;

    /* Notification Ids */
    private int mNotificationID;
    private final int SKIP_TRACK_ONGOING_NOTIFICATION_ID         = 0;
    private final int REPEAT_SONG_ACTIVE_ONGOING_NOTIFICATION_ID = 1;
    private final int REGULAR_NOTIFICATION_IDS_START 		   	 = 2;

    /* Keyguard Resources */
    private KeyguardManager mKeyguardManager = null;
    private KeyguardLock    mKeyguardlock    = null;
    private boolean         mScreenOn        = false;

    /* Broadcast Receiver */
    private BroadcastReceiver mBroadcastReceiver = null;

    /* Foreground activity resources */
    private Intent       mActivityIntent = null;
    private IntentFilter mIntentFilter   = null;

    /* Broadcast intents */
    private Intent mPreviousSongIntent = null;
    private Intent mNextSongIntent     = null;
    private Intent mTogglePauseIntent  = null;

    /* Metadata Resources */
    private TextView mMetadataArtist     = null;
    private TextView mMetadataAlbum      = null;
    private TextView mMetadataTrack      = null;
    private TextView mMetadataNowPlaying = null;

    /* Notification Resources */
    private Notification.Builder mBackButtonDisabledBuilder        = null;
    private Notification.Builder mPlaybackFailedBuilder            = null;
    private Notification.Builder mOngoingNotificationBuilder       = null;
    private Notification.Builder mRepeatSongOnNotificationBuilder  = null;
    private Notification.Builder mRepeatSongOffNotificationBuilder = null;

    /* Launch app from notification resources */
    private PendingIntent mPendingContentIntent = null;

    /* Logging resources */
    private final boolean PRINT_LOG_STMTS = false;

    /* Repeat flag */
    private boolean mRepeatSongActive = false;

    /* Track Change request flag, set when the user intends to change track */
    private boolean mTrackChangeRequested   = false;
    private boolean mPreviousTrackRequested = false;

    /* Orientation Change flag */
    private boolean mOrientationChanged = false;
    private boolean mOrientationPotrait = true;

    /* Screen just turned on flag */
    private boolean mScreenJustTurnedOn = false;

    /* Text Colors */
    private final String mMainTextColor   = "#000000";
    private final String mSecondTextColor = "#800000";

    /* Track Duration */
    private int TRACK_DURATION_INVALID = -2; /* When the app first start up */
    private int TRACK_DURATION_INITIAL = -1; /* When we reset the counter   */
    private int mTrackDurationSeconds  = TRACK_DURATION_INVALID;

    private final String DEFAULT_TRACK_TITLE  = "Track Title";
    private final String DEFAULT_TRACK_ALBUM  = "Track Album";
    private final String DEFAULT_TRACK_ARTIST = "Track Artist";

    /* Activity Background Resources */
    private Window mActivityWindow  = null;

    /* Track info structure */
    public class TrackInfoStruct
    {
        public TrackInfoStruct() {

            if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "TrackInfoStruct() START.");

            setDefaults();

            if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "TrackInfoStruct() END.");
        }

        // Set Defaults
        public void setDefaults() {

            mTrackDuration = TRACK_DURATION_INITIAL;
            mTrackArtist   = DEFAULT_TRACK_ARTIST;
            mTrackTitle    = DEFAULT_TRACK_TITLE;
            mTrackAlbum    = DEFAULT_TRACK_ALBUM;

        }

        // Track duration
        public int    mTrackDuration;

        // Track Artist
        public String mTrackArtist;

        // Track Title
        public String mTrackTitle;

        // Track Album
        public String mTrackAlbum;
    };

    /* Track info constants */
    private int TRACK_INFO_PREVIOUS	   = 0;
    private int TRACK_INFO_CURRENT 	   = 1;
    private int TRACK_INFO_NEXT    	   = 2;
    private int MAX_TRACK_INFO_STRUCTS = 3;

    private View.OnClickListener mNextSongInfoButtonListener     = null;
    private View.OnClickListener mPreviousSongInfoButtonListener = null;

    private Drawable mNextSongInfoButtonDrawableImage	  = null;
    private Drawable mPreviousSongInfoButtonDrawableImage = null;

    private TrackInfoE mTrackInfoDisplayed = TrackInfoE.TRACK_INFO_CURRENT;

    /* Track info array */
    private TrackInfoStruct [] mTrackInfoStruct;

    /* Gesture detector (fling/swipe) class */
    class SkipTrackGestureDetector extends SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {

            if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onFling START");

            try {
                if (Math.abs(event1.getY() - event2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;

                if ( event1.getX() - event2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY ) { // Left Swipe

                    mTrackChangeRequested   = true;
                    mPreviousTrackRequested = true;

                    // Broadcast the Previous song intent
                    broadcastPreviousSongIntent();

                }  else if ( event2.getX() - event1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY ) { // Right Swipe

                    mTrackChangeRequested   = true;
                    mPreviousTrackRequested = false;

                    // Broadcast the Next song intent
                    broadcastNextSongIntent();

                }
            } catch (Exception e) {
                // Do nothing
            }

            if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onFling END");

            return false;

        } // End of function onFling

    } // End of class SkipTrackGestureDetector

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onCreate START");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the Activity's window
        mActivityWindow = getWindow();

        // If all resources are not already created
        if ( !mResourcesCreated ) {

            // Get all the resources needed
            createEverything();

            // Refresh handler and runnable thread
            createRefreshHandlers();

            // Register the UI Refresh thread with its handler
            registerUIRefreshHandler();

            // Register the Track Duration thread with its handler
            registerTrackDurationHandler();

            // Register stuff
            registerEverything();

        } else {

            if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onCreate :: Full-on creation skipped.");
        }

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onCreate END.");
    }

    /* Get all the resources needed, create objects and stuff */
    private void createEverything() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createEverything START.");

        // Storing Application Context for future usage
        mApplicationContext = getApplicationContext();

        // Notification Manager
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Previous Track Button
        mPreviousButton 					 = (Button)findViewById(R.id.previous_button);
        mPreviousButtonDrawableImage 		 = getResources().getDrawable(R.drawable.ic_previous);
        mPreviousButtonPressedDrawableImage  = getResources().getDrawable(R.drawable.ic_previous_pressed);

        // Play Pause Button
        mPlayPauseButton 					 = (Button)findViewById(R.id.play_pause_button);
        mPlayButtonDrawableImage 		     = getResources().getDrawable(R.drawable.ic_play);
        mPlayButtonPressedDrawableImage 	 = getResources().getDrawable(R.drawable.ic_play_pressed);
        mPauseButtonDrawableImage 		     = getResources().getDrawable(R.drawable.ic_pause);
        mPauseButtonPressedDrawableImage	 = getResources().getDrawable(R.drawable.ic_pause_pressed);

        // Next Track Button
        mNextButton					   		 = (Button)findViewById(R.id.next_button);
        mNextButtonDrawableImage 			 = getResources().getDrawable(R.drawable.ic_next);
        mNextButtonPressedDrawableImage		 = getResources().getDrawable(R.drawable.ic_next_pressed);

        // Repeat Track Button
        mRepeatButton					   	 = (Button)findViewById(R.id.repeat_button);
        mRepeatButtonActiveDrawableImage 	 = getResources().getDrawable(R.drawable.ic_repeat_active);
        mRepeatButtonInactiveDrawableImage	 = getResources().getDrawable(R.drawable.ic_repeat_inactive);

        // Audio Manager
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

        // Previous track fling resources
        mPreviousFlingPosLeft   = (ImageView)findViewById(R.id.fling_previous_left);
        mPreviousFlingPosMiddle = (ImageView)findViewById(R.id.fling_previous_middle);
        mPreviousFlingPosRight  = (ImageView)findViewById(R.id.fling_previous_right);

        mPreviousFlingGrayImage = R.drawable.ic_fling_previous_1;
        mPreviousFlingBlueImage = R.drawable.ic_fling_previous_2;

        // Next track fling resources
        mNextFlingPosLeft       = (ImageView)findViewById(R.id.fling_next_left);
        mNextFlingPosMiddle     = (ImageView)findViewById(R.id.fling_next_middle);
        mNextFlingPosRight      = (ImageView)findViewById(R.id.fling_next_right);

        mNextFlingGrayImage		= R.drawable.ic_fling_foward_1;
        mNextFlingBlueImage		= R.drawable.ic_fling_foward_2;

        // Initializations
        mPreviousTrackFlingBlueIconPos = FlingBlueIconPosE.POSITION_MIDDLE;

        // Metadata TextView resources
        mMetadataArtist     = (TextView)findViewById(R.id.artist_textview);
        mMetadataAlbum      = (TextView)findViewById(R.id.album_textview);
        mMetadataTrack      = (TextView)findViewById(R.id.track_textview);
        mMetadataNowPlaying = (TextView)findViewById(R.id.now_playing_textview);

        // 'Next' and 'Previous' song info buttons are only available in "Potrait" orientation
        if ( mOrientationPotrait ) {
        
        	// Song info change buttons
        	mNextSongInfoButtonDrawableImage	 = getResources().getDrawable(R.drawable.ic_next_track_info);
        	mPreviousSongInfoButtonDrawableImage = getResources().getDrawable(R.drawable.ic_previous_track_info);
       }

        // Create Button Click Listeners
        createButtonClickListeners();

        // Gesture detection
        mSkipTrackGestureDetector = new GestureDetector(this, new SkipTrackGestureDetector());
        mSkipTrackGestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mSkipTrackGestureDetector.onTouchEvent(event);
            }
        };

        // Global Notification ID
        mNotificationID = REGULAR_NOTIFICATION_IDS_START;

        // Keyguard resources
        mKeyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        mKeyguardlock    = mKeyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        mScreenOn        = true;

        // Screen turned on boolean
        mScreenJustTurnedOn = true;

        // Create and register a Broadcast Receiver object
        createBroadcastReceiver();

        // Create count down timers
        createCountDownTimers();

        // Create the song broadcast intents
        createSongBroadcastIntents();

        // Create notification builders
        createNotificationBuilders();

        // Track info structures
        mTrackInfoStruct = new TrackInfoStruct[MAX_TRACK_INFO_STRUCTS];

        for ( int index=TRACK_INFO_PREVIOUS; index<MAX_TRACK_INFO_STRUCTS; ++index) {

            mTrackInfoStruct[index] = new TrackInfoStruct();
        }

        // All resources have been created
        mResourcesCreated = true;

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createEverything END.");
    }

    /* Register the receivers and things like that */
    private void registerEverything() {

        // Set the button images and stuff
        displayUI();

        // Register Button Clicks
        registerButtonClickListeners();

        // Register the Broadcast Receiver
        registerBroadcastReceiver();

        // Registering for Swipe events
        registerForSwipeEvents();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Set the flag that the orientation just changed
        mOrientationChanged = true;
        
        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onConfigurationChanged START.");

        // Checks the orientation of the screen
        if ( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ) {

            // Set the "Landscape" view (tilted phone)
            setContentView(R.layout.activity_land_main);
            
            mOrientationPotrait = false;

        } else if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ) {

            // Set the "Potrait" view (default)
            setContentView(R.layout.activity_main);

            mOrientationPotrait = true;
        }

        // Get all the resources needed
        createEverything();

        // Register stuff
        registerEverything();

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onConfigurationChanged END.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onResume START.");

        // Re-create the UI
        displayUI();

        // Clear all the previously shown notifications
        clearAllNotifications();

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "onResume END.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /* Create the button click listeners */
    private void createButtonClickListeners() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createButtonClickListeners START.");

        // Previous button click listener
        mPreviousButtonListener = new View.OnClickListener() {
            public void onClick(View v) {

                // Now toggle the UI again
                try {

                    toggleUi(ButtonsE.BUTTON_PREVIOUS);

                    // Broadcast the Previous song intent
                    broadcastPreviousSongIntent();

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        // Play/Pause button click listener
        mPlayPauseButtonListener = new View.OnClickListener() {
            public void onClick(View v) {

                // Now toggle the UI again
                try {

                    toggleUi(ButtonsE.BUTTON_PLAY_PAUSE);

                    broadcastPlayOrPauseIntent();

                    if ( mPlayPauseState == PlayPauseStateE.STATE_PAUSED ) {

                        // Music just stopped playing
                        mMetadataNowPlaying.setText(R.string.no_music_playing);
                        mMetadataNowPlaying.setTextColor(Color.parseColor(mMainTextColor));

                    } else if ( mPlayPauseState == PlayPauseStateE.STATE_PLAYING ) {

                        // Music just started playing
                        //mMetadataNowPlaying.setText("");
                        mMetadataNowPlaying.setTextColor(Color.parseColor(mSecondTextColor));

                        // Incase playback fails, handle it
                        mPlaybackStartTimer.start();
                    }

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        // Next button click listener
        mNextButtonListener = new View.OnClickListener() {
            public void onClick(View v) {

                // Now toggle the UI again
                try {

                    toggleUi(ButtonsE.BUTTON_NEXT);

                    // Broadcast the Next song intent
                    broadcastNextSongIntent();

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        // Repeat button click listener
        mRepeatButtonListener = new View.OnClickListener() {
            public void onClick(View v) {

                // Now toggle the UI again
                try {
                    toggleUi(ButtonsE.BUTTON_REPEAT);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        // Next Song info button click listener
        mNextSongInfoButtonListener = new View.OnClickListener() {
            public void onClick(View v) {

                // Now toggle the UI again
                try {
                    toggleUi(ButtonsE.BUTTON_NEXT_SONG_INFO);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        // Previous Song info button click listener
        mPreviousSongInfoButtonListener = new View.OnClickListener() {
            public void onClick(View v) {

                // Now toggle the UI again
                try {
                    toggleUi(ButtonsE.BUTTON_PREVIOUS_SONG_INFO);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createButtonClickListeners END.");

    } // End of function createButtonClickListeners()

    /* Registers the onClick listeners for all the UI buttons */
    private void registerButtonClickListeners() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "setButtonClickListener START.");

        // Previous Track Button
        mPreviousButton         .setOnClickListener(mPreviousButtonListener);

        // Play/Pause Button
        mPlayPauseButton        .setOnClickListener(mPlayPauseButtonListener);

        // Next Track Button
        mNextButton             .setOnClickListener(mNextButtonListener);

        // Repeat Track Button
        mRepeatButton           .setOnClickListener(mRepeatButtonListener);

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "setButtonClickListener END.");

    } // End of function setButtonClickListener

    /* Toggles the image shown on the buttons */
    private void toggleUi(ButtonsE button) throws InterruptedException {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "toggleUi START.");

        switch ( button ) {

        case BUTTON_PREVIOUS:

            mPreviousButton      .setBackground(mPreviousButtonPressedDrawableImage);
            mPreviousButtonTimer .start();

            mTrackChangeRequested   = true;
            mPreviousTrackRequested = true;

            break;

        case BUTTON_PLAY_PAUSE:

            switch ( mPlayPauseState ) {

            case STATE_PLAYING: // Currently playing music (STATE_PLAYING); new state will be STATE_PAUSED

                mPlayPauseState = PlayPauseStateE.STATE_PAUSED;

                mPlayPauseButton .setBackground(mPauseButtonPressedDrawableImage);
                mPlayButtonTimer .start();

                break;
            case STATE_PAUSED: // Currently NOT playing music (STATE_PAUSED); new state will be STATE_PLAYING

                mPlayPauseState = PlayPauseStateE.STATE_PLAYING;

                mPlayPauseButton  .setBackground(mPlayButtonPressedDrawableImage);
                mPauseButtonTimer .start();

                mTrackChangeRequested = true;

                break;
            }

            break;

        case BUTTON_NEXT:

            mNextButton      .setBackground(mNextButtonPressedDrawableImage);
            mNextButtonTimer .start();

            mTrackChangeRequested   = true;
            mPreviousTrackRequested = false;

            break;

        case BUTTON_REPEAT:

            if ( mRepeatSongActive ) {

                // Repeat was active till now, the user doesn't want it anymore
                mRepeatButton.setBackground(mRepeatButtonInactiveDrawableImage);
                mRepeatSongActive = false;

                // Clear all the previously shown notifications
                clearAllNotifications();
                
                // Cancel the "ongoing" 'Repeat Song Active' notification
                mNotificationManager.cancel(REPEAT_SONG_ACTIVE_ONGOING_NOTIFICATION_ID);

                // Show the Repeat OFF notification to the user
                mNotificationManager.notify(mNotificationID++, mRepeatSongOffNotificationBuilder.build());

            } else {

                // The user wants the song to repeat
                mRepeatButton.setBackground(mRepeatButtonActiveDrawableImage);
                
                mRepeatSongActive = true;

                // Clear all the previously shown notifications
                clearAllNotifications();

                // Show the Repeat ON notification to the user
                mNotificationManager.notify(REPEAT_SONG_ACTIVE_ONGOING_NOTIFICATION_ID, mRepeatSongOnNotificationBuilder.build());
            }

            break;

        case BUTTON_PREVIOUS_SONG_INFO: // "Previous" song info button pushed

            // Which track's info is being displayed?
            switch ( mTrackInfoDisplayed ) {

            // Currently displaying "Current" song info, need to display the "Previous" track info
            case TRACK_INFO_CURRENT:

                mMetadataArtist .setText(mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackArtist);
                mMetadataAlbum  .setText(mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackAlbum);
                mMetadataTrack  .setText(mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackTitle);

                mMetadataNowPlaying.setText(R.string.previous_track);

                mTrackInfoDisplayed = TrackInfoE.TRACK_INFO_PREVIOUS;
                break;

                // Currently displaying "Next" song info, need to display the "Current" track info
            case TRACK_INFO_NEXT:

                mMetadataArtist .setText(mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackArtist);
                mMetadataAlbum  .setText(mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackAlbum);
                mMetadataTrack  .setText(mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackTitle);

                mMetadataNowPlaying.setText(R.string.now_playing);

                mTrackInfoDisplayed = TrackInfoE.TRACK_INFO_CURRENT;
                break;

                // Currently displaying "Previous" song info, need to display the "Next" track info
            case TRACK_INFO_PREVIOUS:

                mMetadataArtist .setText(mTrackInfoStruct[TRACK_INFO_NEXT].mTrackArtist);
                mMetadataAlbum  .setText(mTrackInfoStruct[TRACK_INFO_NEXT].mTrackAlbum);
                mMetadataTrack  .setText(mTrackInfoStruct[TRACK_INFO_NEXT].mTrackTitle);

                mMetadataNowPlaying.setText(R.string.next_track);

                mTrackInfoDisplayed = TrackInfoE.TRACK_INFO_NEXT;
                break;

            }

            break;

        case BUTTON_NEXT_SONG_INFO: // "Previous" song info button pushed

            // Which track's info is being displayed?
            switch ( mTrackInfoDisplayed ) {

            // Currently displaying "Current" song info, need to display the "Next" track info
            case TRACK_INFO_CURRENT:

                mMetadataArtist .setText(mTrackInfoStruct[TRACK_INFO_NEXT].mTrackArtist);
                mMetadataAlbum  .setText(mTrackInfoStruct[TRACK_INFO_NEXT].mTrackAlbum);
                mMetadataTrack  .setText(mTrackInfoStruct[TRACK_INFO_NEXT].mTrackTitle);

                mMetadataNowPlaying.setText(R.string.next_track);

                mTrackInfoDisplayed = TrackInfoE.TRACK_INFO_NEXT;
                break;

                // Currently displaying "Next" song info, need to display the "Previous" track info
            case TRACK_INFO_NEXT:

                mMetadataArtist .setText(mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackArtist);
                mMetadataAlbum  .setText(mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackAlbum);
                mMetadataTrack  .setText(mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackTitle);

                mMetadataNowPlaying.setText(R.string.previous_track);

                mTrackInfoDisplayed = TrackInfoE.TRACK_INFO_PREVIOUS;
                break;

                // Currently displaying "Previous" song info, need to display the "Current" track info
            case TRACK_INFO_PREVIOUS:

                mMetadataArtist .setText(mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackArtist);
                mMetadataAlbum  .setText(mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackAlbum);
                mMetadataTrack  .setText(mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackTitle);

                mMetadataNowPlaying.setText(R.string.now_playing);

                mTrackInfoDisplayed = TrackInfoE.TRACK_INFO_CURRENT;
                break;

            }

            break;

        } // End of switch ( button )

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "toggleUi END.");

    } // End of function toggleUi

    /* Sets the correct image backgrounds for the buttons */
    private void displayUI() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "displayUI START.");

        // Setting the default 'blue' button images
        mPreviousButton .setBackground(mPreviousButtonDrawableImage);
        mNextButton     .setBackground(mNextButtonDrawableImage);

        if ( mRepeatSongActive ) {

            // Set the "Repeat Active" icon
            mRepeatButton.setBackground(mRepeatButtonActiveDrawableImage);

        } else {

            // Set the "Repeat Inactive" icon
            mRepeatButton.setBackground(mRepeatButtonInactiveDrawableImage);
        }

        mPlayPauseState = mAudioManager.isMusicActive() ? PlayPauseStateE.STATE_PLAYING : PlayPauseStateE.STATE_PAUSED;

        switch ( mPlayPauseState ) {

        case STATE_PLAYING: // Currently playing music (STATE_PLAYING)
            mPlayPauseButton.setBackground(mPauseButtonDrawableImage);
            //mMetadataNowPlaying.setText("");
            mMetadataNowPlaying.setTextColor(Color.parseColor(mSecondTextColor));
            break;
        case STATE_PAUSED: // Currently NOT playing music (STATE_PAUSED)
            mPlayPauseButton.setBackground(mPlayButtonDrawableImage);
            mMetadataNowPlaying.setText(R.string.no_music_playing);
            mMetadataNowPlaying.setTextColor(Color.parseColor(mMainTextColor));
            break;
        }

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "displayUI END.");

    } // End of function displayUI()

    /* Create song broadcast intents */
    private void createSongBroadcastIntents() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createSongBroadcastIntents START.");

        // Previous Song
        mPreviousSongIntent = new Intent();
        mPreviousSongIntent.setAction("com.sec.android.app.music.musicservicecommand.previous");

        // Next Song
        mNextSongIntent = new Intent();
        mNextSongIntent.setAction("com.sec.android.app.music.musicservicecommand.next");

        // Toggle Pause
        mTogglePauseIntent = new Intent();
        mTogglePauseIntent.setAction("com.sec.android.app.music.musicservicecommand.togglepause");

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createSongBroadcastIntents END.");

    } // End of function createSongBroadcastIntents()

    /* Change song to next one */
    private void broadcastNextSongIntent() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "broadcastNextSongIntent() Called.");

        // Reset the current track duration
        // As the next song has been requested
        // We need to start counting from zero again
        mTrackDurationSeconds  = TRACK_DURATION_INITIAL;

        // Broadcast the intent
        sendBroadcast(mNextSongIntent);

    } // End of function broadcastNextSongIntent()

    /* Change song to previous one */
    private void broadcastPreviousSongIntent() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "broadcastPreviousSongIntent() Called.");

        // Reset the current track duration
        // As the same song will be played from the beginning
        // or the previous song will be played.
        // In any case we need to start counting from zero again
        mTrackDurationSeconds  = TRACK_DURATION_INITIAL;

        // Broadcast the intent
        sendBroadcast(mPreviousSongIntent);

    } // End of function broadcastPreviousSongIntent

    /* Play or Pause the song */
    private void broadcastPlayOrPauseIntent() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "broadcastPlayOrPauseIntent() Called.");

        // Broadcast the intent
        sendBroadcast(mTogglePauseIntent);

    } // End of function broadcastPlayOrPauseIntent()

    /* Creating the UI refresh callback */
    private void registerUIRefreshHandler() {

        // Register the UI refresh thread with the handler
        mHandler.postDelayed ( mUIRefreshThread, mFlingIconDelay );

    } // End of function registerUIRefreshHandler()

    /* Creating the Track Duration callback */
    private void registerTrackDurationHandler() {

        // Register the UI refresh thread with the handler
        mHandler.postDelayed ( mTrackDurationThread, ONE_SECOND );

    } // End of function registerTrackDurationHandler()

    /* Refresh the UI */
    private void refreshUI() {

        /* We want the 'Blue' icon to behave in the following way:
         * When the icon is in the left-most position for the "Previous Track" fling, it needs to be in the right most position for the 
         * "Next Track" fling. */
        switch ( mPreviousTrackFlingBlueIconPos ) {

        case POSITION_LEFT:   // Put the 'Blue' icon in the Left position

            mPreviousFlingPosLeft   .setImageResource(mPreviousFlingBlueImage);
            mPreviousFlingPosMiddle .setImageResource(mPreviousFlingGrayImage);
            mPreviousFlingPosRight  .setImageResource(mPreviousFlingGrayImage);

            // Based on the icon's position on the "Previous Track" fling image, 
            // we decide the position for the "Next Track" fling
            mNextFlingPosLeft       .setImageResource(mNextFlingGrayImage);
            mNextFlingPosMiddle     .setImageResource(mNextFlingGrayImage);
            mNextFlingPosRight      .setImageResource(mNextFlingBlueImage);

            mPreviousTrackFlingBlueIconPos = FlingBlueIconPosE.POSITION_RIGHT;

            break;

        case POSITION_MIDDLE: // Put the 'Blue' icon in the Middle position

            mPreviousFlingPosLeft   .setImageResource(mPreviousFlingGrayImage);
            mPreviousFlingPosMiddle .setImageResource(mPreviousFlingBlueImage);
            mPreviousFlingPosRight  .setImageResource(mPreviousFlingGrayImage);

            // Based on the icon's position on the "Previous Track" fling image, 
            // we decide the position for the "Next Track" fling
            mNextFlingPosLeft  	    .setImageResource(mNextFlingGrayImage);
            mNextFlingPosMiddle     .setImageResource(mNextFlingBlueImage);
            mNextFlingPosRight      .setImageResource(mNextFlingGrayImage);

            mPreviousTrackFlingBlueIconPos = FlingBlueIconPosE.POSITION_LEFT;

            break;

        case POSITION_RIGHT:  // Put the 'Blue' icon in the Right position

            mPreviousFlingPosLeft   .setImageResource(mPreviousFlingGrayImage);
            mPreviousFlingPosMiddle .setImageResource(mPreviousFlingGrayImage);
            mPreviousFlingPosRight  .setImageResource(mPreviousFlingBlueImage);

            // Based on the icon's position on the "Previous Track" fling image, 
            // we decide the position for the "Next Track" fling
            mNextFlingPosLeft       .setImageResource(mNextFlingBlueImage);
            mNextFlingPosMiddle	    .setImageResource(mNextFlingGrayImage);
            mNextFlingPosRight      .setImageResource(mNextFlingGrayImage);

            mPreviousTrackFlingBlueIconPos = FlingBlueIconPosE.POSITION_MIDDLE;

            break;

        } // End of switch ( mPreviousTrackFlingBlueIconPos )

    } // End of function refreshUI()

    /* Create a Broadcast Receiver */
    private void createBroadcastReceiver() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createBroadcastReceiver START.");

        // Bring the application back to the foreground intent
        mActivityIntent = new Intent(mApplicationContext, MainActivity.class);

        // Adding the necessary flags
        mActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // Needed to bring the app to the foreground
        mActivityIntent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);     // Needed to launch Home screen when back is pressed

        mActivityIntent.setAction(Intent.ACTION_MAIN);
        mActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // The Broadcast listener object
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (Intent.ACTION_SCREEN_ON.equals(action)) {

                    // Screen is now ON
                    mScreenOn 			= true;
                    mScreenJustTurnedOn = true;

                    // Evaluate if the Keyguard lock needs to be enabled
                    evaluateKeyguardLock();

                    // Bring the application back to the foreground
                    if ( mAudioManager.isMusicActive() ) {

                        mApplicationContext.startActivity(mActivityIntent);

                    }

                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {

                    // Screen is now OFF
                    mScreenOn		    = false;
                    mScreenJustTurnedOn = false;

                    // Re-enable the keyguard
                    mKeyguardlock.reenableKeyguard();

                } else {

                    if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createBroadcastReceiver::onReceive::else START.");

                    // Extra song info
                    String artist = DEFAULT_TRACK_ARTIST;
                    String track  = DEFAULT_TRACK_TITLE;
                    String album  = DEFAULT_TRACK_ALBUM;

                    // Fetch the "Extra" info from the Intent object
                    artist = intent.getStringExtra("artist");
                    track  = intent.getStringExtra("track");
                    album  = intent.getStringExtra("album");
                    
                    // To fix Issue # 1: Current playing song detail is lost
                    // Put data in the "Current" Track Info structure
                    mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackAlbum  = album;
                    mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackTitle  = track;
                    mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackArtist = artist;

                    if ( PRINT_LOG_STMTS ) {

                        Log.v("Skip Track", "## Incoming Track Info ##");
                        Log.v("Skip Track", "Album  : " + album);
                        Log.v("Skip Track", "Title  : " + track);
                        Log.v("Skip Track", "Artist : " + artist);
                    }

                    if ( mTrackChangeRequested ) {

                        // Update the track info structures that we have
                        updateTrackInfo( mPreviousTrackRequested ? TrackChangeE.TRACK_CHANGED_TO_PREVIOUS : TrackChangeE.TRACK_CHANGED_TO_NEXT , artist, track, album );
                    }

                    // If orientation didn't just change, and "Repeat" is active, replay the song
                    if ( !mOrientationChanged && mRepeatSongActive ) {

                        // Broadcast the previous song intent
                        broadcastPreviousSongIntent();

                    }

                    // Change track requested AND repeat song is NOT active AND app didn't just launch
                    // meaning, a song which was playing has finished playing
                    // reset track duration. !!BAM!!
                    if ( !mOrientationChanged && !mScreenJustTurnedOn /*&& !mRepeatSongActive*/ && mTrackDurationSeconds != TRACK_DURATION_INVALID ) {

                        mTrackDurationSeconds = TRACK_DURATION_INITIAL;

                        // Reset the flag
                        mScreenJustTurnedOn = false;
                    }

                    if ( !mRepeatSongActive || mTrackChangeRequested || mOrientationChanged ) {

                        // Set the Track name, its Artist and album to the fields
                        mMetadataArtist.setText(artist);
                        mMetadataTrack .setText(track);
                        mMetadataAlbum .setText(album);

                        // Reset the flag
                        mTrackChangeRequested = false;
                    }

                    // Reset the flag
                    mOrientationChanged = false;

                } // End of main "else"

            } // End of onReceive
        };

        // Create a filter with the broadcast intents we are interested in
        mIntentFilter = new IntentFilter();

        // The actions we are interested in
        mIntentFilter.addAction(Intent.ACTION_SCREEN_ON);         // Notification for Screen turning ON
        mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);		  // Notification for Screen turning OFF
        mIntentFilter.addAction("com.android.music.metachanged"); // Notification for the music track being changed

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createBroadcastReceiver END.");

    } // End of function createAndRegisterBroadcastReceiver()

    /* Create the handler thread that refreshes the UI */
    private void createRefreshHandlers() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createRefreshHandlers START.");

        mHandler         = new Handler();

        // UI Refresh thread
        mUIRefreshThread = new Runnable() {
            @Override
            public void run() {

                // Refresh the user interface
                refreshUI();

                // Register again for next callback
                registerUIRefreshHandler();
            }
        };

        // Track duration thread
        mTrackDurationThread = new Runnable() {
            @Override
            public void run() {

                // Only if Music is Playing
                if ( mAudioManager.isMusicActive() ) {

                    // Set the "Play/Pause" state
                    mPlayPauseState = PlayPauseStateE.STATE_PLAYING;

                    // Set the "Play/Pause" button icon to 'Pause'
                    mPlayPauseButton.setBackground(mPauseButtonDrawableImage);

                    if ( mPlayPauseState == PlayPauseStateE.STATE_PLAYING ) {

                        // We have a valid Track Duration
                        if ( mTrackDurationSeconds != TRACK_DURATION_INVALID ) {

                            // Increment the number of seconds of track duration
                            mTrackDurationSeconds++;

                            // Only if the "Current Track" info is being displayed
                            if ( mTrackInfoDisplayed == TrackInfoE.TRACK_INFO_CURRENT ) {

                                // Set the new Track Duration
                                setTrackCurrentDuration();
                            }

                        } else {

                            // Only if the "Current Track" info is being displayed
                            if ( mTrackInfoDisplayed == TrackInfoE.TRACK_INFO_CURRENT ) {

                                // We don't have a valid track duration, just set the text "Now Playing"
                                mMetadataNowPlaying.setText(R.string.now_playing);

                            }
                        }
                    }

                } else {

                    // No music is currently playing

                    // Set the "Play/Pause" button icon to 'Play'
                    mPlayPauseButton.setBackground(mPlayButtonDrawableImage);

                    // Set the "Play/Pause" state
                    mPlayPauseState = PlayPauseStateE.STATE_PAUSED;
                }

                // Register again for next callback
                registerTrackDurationHandler();

            } // End of function run()

        };

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createRefreshHandlers END.");

    } // End of function createRefreshHandlers()

    // Register the Broadcaster Receiver
    private void registerBroadcastReceiver() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "registerBroadcastReceiver START.");

        // Register for broadcasts of interest.
        registerReceiver(mBroadcastReceiver, mIntentFilter, null, null);

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "registerBroadcastReceiver END.");

    } // End of function registerBroadcastReceiver()

    @Override
    public void onClick(View arg0) {

        // Sample code for notifications
        /*Notification.Builder mBuilder = null;
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mBuilder =
				new Notification.Builder(getApplicationContext())
		.setSmallIcon(R.drawable.ic_fling_foward_1)
		.setContentTitle("Generic Notification")
		.setContentText("Nothing to see here!")
		.setTicker("On CLick Called");

		mNotificationManager.notify(6, mBuilder.build());
         */
    }; // End of function onClick()

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // event.startTracking(); // Uncomment this line to track for a "long press" of the key

            // Clear all the previously shown notifications
            clearAllNotifications();

            // Show to the user that the "Back Button is Disabled" for this application
            mNotificationManager.notify(mNotificationID++, mBackButtonDisabledBuilder.build());

            return true;

        } // End of case KEYCODE_BACK
        else if (keyCode == KeyEvent.KEYCODE_HOME) {

            // If the screen is ON, it doesn't matter if the music is active, we have to show the Keyguard
            if ( mScreenOn ) {

                // Re-enable the keyguard
                mKeyguardlock.reenableKeyguard();

            } else { // Screen is OFF

                // Do nothing for now
            }

            return true;
        } // End of case KEYCODE_HOME

        return super.onKeyDown(keyCode, event);

    } // End of function onKeyDown()

    /* Evaluates if the Keyguard lock needs to be enabled */
    private void evaluateKeyguardLock() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "evaluateKeyguardLock START.");

        if ( mAudioManager.isMusicActive() ) {

            // Disable the keyguard as the music is active
            mKeyguardlock.disableKeyguard();

        } else {

            // Re-enable the keyguard as no music is active
            mKeyguardlock.reenableKeyguard();

        }

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "evaluateKeyguardLock END.");

    } // End of function evaluateKeyguardLock()

    /* Creates the countdown timers for the delayed image to be shown on the Previous, Play/Pause and Next buttons */
    private void createCountDownTimers() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createCountDownTimers START.");

        // Previous Track button's timer
        mPreviousButtonTimer = new CountDownTimer(mSleepDelay, mSleepDelay) {

            @Override
            public void onTick(long milliseconds){}

            @Override
            public void onFinish() {
                // After 100 milliseconds display the next image
                mPreviousButton.setBackground(mPreviousButtonDrawableImage);
            }
        };

        // Next Track button's timer
        mNextButtonTimer = new CountDownTimer(mSleepDelay, mSleepDelay) {

            @Override
            public void onTick(long milliseconds){}

            @Override
            public void onFinish() {
                //After 100 milliseconds display the next image
                mNextButton.setBackground(mNextButtonDrawableImage);
            }

        };

        // Pause button's timer
        mPauseButtonTimer = new CountDownTimer(mSleepDelay, mSleepDelay) {

            @Override
            public void onTick(long milliseconds){}

            @Override
            public void onFinish() {
                // After 100 milliseconds display the next image
                mPlayPauseButton.setBackground(mPauseButtonDrawableImage);
            }
        };

        // Play button's timer
        mPlayButtonTimer = new CountDownTimer(mSleepDelay, mSleepDelay) {

            @Override
            public void onTick(long milliseconds){}

            @Override
            public void onFinish() {
                // After 100 milliseconds display the next image
                mPlayPauseButton.setBackground(mPlayButtonDrawableImage);
            }
        };

        // Timer to look for playback when the user pushes the 'Play' button
        mPlaybackStartTimer = new CountDownTimer(mPlaybackDelay, mPlaybackDelay) {

            @Override
            public void onTick(long milliseconds){}

            @Override
            public void onFinish() {

                if ( !mAudioManager.isMusicActive() ) {

                    // Play button was pressed, but the playback didn't start
                    // Now after 500 milliseconds, take evasive action

                    // Show the blue 'Play' button
                    mPlayPauseButton.setBackground(mPlayButtonDrawableImage);

                    // Set the global play state to 'Paused'
                    mPlayPauseState = PlayPauseStateE.STATE_PAUSED;

                    // Couldn't start the music
                    mMetadataNowPlaying.setText(R.string.no_music_playing);
                    mMetadataNowPlaying.setTextColor(Color.parseColor(mMainTextColor));

                    // Clear all the previously shown notifications
                    clearAllNotifications();

                    // Show the playback failure notification
                    mNotificationManager.notify(mNotificationID++, mPlaybackFailedBuilder.build());

                    // Reset the track change requested flag
                    mTrackChangeRequested = true;

                } else {

                    // The music is playing
                    //mMetadataNowPlaying.setText("");
                    mMetadataNowPlaying.setTextColor(Color.parseColor(mSecondTextColor));
                }
            }
        };

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createCountDownTimers END.");

    } // End of function createCountDownTimers()

    /* Register for swipe/fling events */
    private void registerForSwipeEvents() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "registerForSwipeEvents START.");

        // Previous track leftmost icon
        mPreviousFlingPosLeft   .setOnClickListener(MainActivity.this);
        mPreviousFlingPosLeft   .setOnTouchListener(mSkipTrackGestureListener);

        // Previous track middle icon
        mPreviousFlingPosMiddle .setOnClickListener(MainActivity.this);
        mPreviousFlingPosMiddle .setOnTouchListener(mSkipTrackGestureListener);

        // Previous track rightmost icon
        mPreviousFlingPosRight  .setOnClickListener(MainActivity.this);
        mPreviousFlingPosRight  .setOnTouchListener(mSkipTrackGestureListener);

        // Next track leftmost icon
        mNextFlingPosLeft  		.setOnClickListener(MainActivity.this);
        mNextFlingPosLeft  	    .setOnTouchListener(mSkipTrackGestureListener);

        // Next track middle icon
        mNextFlingPosMiddle  	.setOnClickListener(MainActivity.this);
        mNextFlingPosMiddle     .setOnTouchListener(mSkipTrackGestureListener);

        // Next track rightmost icon
        mNextFlingPosRight  	.setOnClickListener(MainActivity.this);
        mNextFlingPosRight      .setOnTouchListener(mSkipTrackGestureListener);

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "registerForSwipeEvents END.");

    } // End of function registerForSwipeEvents()

    /* Create the notification builders */
    private void createNotificationBuilders() {

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createNotificationBuilders START.");

        // Creating a notification to show to the user 
        // that the "Back Button is Disabled" for this application
        mBackButtonDisabledBuilder = new Notification.Builder(mApplicationContext)
        .setSmallIcon(R.drawable.ic_nav_back)
        .setContentTitle("Skip Track")
        .setContentText("Back button disabled for this app")
        .setTicker("Back button is disabled. Press the 'Home' key.");

        // Creating a notification to show to the user 
        // that the play-back has failed
        mPlaybackFailedBuilder = new Notification.Builder(mApplicationContext)
        .setSmallIcon(R.drawable.ic_play_pressed)
        .setContentTitle("Skip Track")
        .setContentText("Playback failed!")
        .setTicker("Playback failed! Use MusicPlayer to start music.");

        // Create the "OnGoing" notification to be shown permanently to launch the app
        mPendingContentIntent = PendingIntent.getActivity(mApplicationContext, 0, mActivityIntent, 0);

        mOngoingNotificationBuilder = new Notification.Builder(mApplicationContext)
        .setSmallIcon(R.drawable.ic_launcher)
        .setOngoing(true)
        .setWhen(System.currentTimeMillis())
        .setContentTitle("Skip Track")
        .setContentText("Touch here to launch the app.")
        .setContentIntent(mPendingContentIntent)
        .setTicker("");

        // This "ongoing" notification takes the ID 0 which is reserved for this
        mNotificationManager.notify(SKIP_TRACK_ONGOING_NOTIFICATION_ID, mOngoingNotificationBuilder.build());

        // Creating a notification to show to the user 
        // that the repeat has been turned ON
        mRepeatSongOnNotificationBuilder = new Notification.Builder(mApplicationContext)
        .setSmallIcon(R.drawable.ic_noti_repeat_active)
        .setOngoing(true)
        .setWhen(System.currentTimeMillis())
        .setContentTitle("Skip Track")
        .setContentText("Repeat Track is ACTIVE.")
        .setTicker("Repeat Turned ON.");

        // Creating a notification to show to the user 
        // that the repeat has been turned ON
        mRepeatSongOffNotificationBuilder = new Notification.Builder(mApplicationContext)
        .setSmallIcon(R.drawable.ic_repeat)
        .setContentTitle("Skip Track")
        .setContentText("Repeat Turned OFF")
        .setTicker("Repeat Turned OFF.");

        if ( PRINT_LOG_STMTS ) Log.v("Skip Track", "createNotificationBuilders END.");

    } // End of function createNotificationBuilders()

    /* Calculate and set the current duration of the track */
    private void setTrackCurrentDuration() {

        String trackDurationString  = "";
        int    trackDurationValue   = mTrackDurationSeconds;
        int    trackDurationMinutes = 0;
        int    trackDurationSeconds = 0;

        // Get the number of minutes from the seconds value of the duration
        trackDurationMinutes = (trackDurationValue / SECS_IN_A_MINUTE);

        // Add a zero prefix if the value is less than ten
        if ( trackDurationMinutes < 10 ) {

            trackDurationString += "0";
        }

        // Convert the minutes value into String and put it in the variable trackDurationString
        trackDurationString  += Integer.toString(trackDurationMinutes);

        // Append the colon
        trackDurationString += ":";

        // Figure out the seconds values from the track duration
        // Seconds = Track Duration Value [in seconds] - number of minutes calculated above
        trackDurationSeconds = (trackDurationValue - (trackDurationMinutes * SECS_IN_A_MINUTE));

        // Add a zero prefix if the value is less than ten
        if ( trackDurationSeconds < 10 ) {

            trackDurationString += "0";
        }

        // Convert the seconds value into String and append it in the variable trackDurationString
        trackDurationString += Integer.toString(trackDurationSeconds);

        // Set the created Track Duration to the "Now Playing" text view
        mMetadataNowPlaying.setText(trackDurationString);

    } // End of function setTrackCurrentDuration()

    /* Update the Track Info structures */
    private void updateTrackInfo(TrackChangeE trackChangedTo, String artist, String track, String album) {

        // Update Track Info structures

        switch ( trackChangedTo ) {

        case TRACK_CHANGED_TO_NEXT:

            // Put data in the "Previous" Track Info structure
            mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackAlbum  = mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackAlbum;
            mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackTitle  = mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackTitle;
            mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackArtist = mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackArtist;

            // Put data in the "Current" Track Info structure
            mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackAlbum  = album;
            mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackTitle  = track;
            mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackArtist = artist;

            // Put data in the "Next" Track Info structure
            mTrackInfoStruct[TRACK_INFO_NEXT].setDefaults();

            break;
        case TRACK_CHANGED_TO_PREVIOUS:

            // Put data in the "Previous" Track Info structure
            mTrackInfoStruct[TRACK_INFO_PREVIOUS].setDefaults();

            // Put data in the "Next" Track Info structure
            mTrackInfoStruct[TRACK_INFO_NEXT].mTrackAlbum  = mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackAlbum;
            mTrackInfoStruct[TRACK_INFO_NEXT].mTrackTitle  = mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackTitle;
            mTrackInfoStruct[TRACK_INFO_NEXT].mTrackArtist = mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackArtist;

            // Put data in the "Current" Track Info structure
            mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackAlbum  = album;
            mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackTitle  = track;
            mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackArtist = artist;

            break;

        } // End of switch ( trackChangedTo )

        if ( PRINT_LOG_STMTS ) {

            Log.v("Skip Track", "*** Track Info START. ***");

            Log.v("Skip Track", "## Previous Track Info ##");
            Log.v("Skip Track", "Album  : " + mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackAlbum);
            Log.v("Skip Track", "Title  : " + mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackTitle);
            Log.v("Skip Track", "Artist : " + mTrackInfoStruct[TRACK_INFO_PREVIOUS].mTrackArtist);

            Log.v("Skip Track", "## Current Track Info ##");
            Log.v("Skip Track", "Album  : " + mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackAlbum);
            Log.v("Skip Track", "Title  : " + mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackTitle);
            Log.v("Skip Track", "Artist : " + mTrackInfoStruct[TRACK_INFO_CURRENT].mTrackArtist);

            Log.v("Skip Track", "## Next Track Info ##");
            Log.v("Skip Track", "Album  : " + mTrackInfoStruct[TRACK_INFO_NEXT].mTrackAlbum);
            Log.v("Skip Track", "Title  : " + mTrackInfoStruct[TRACK_INFO_NEXT].mTrackTitle);
            Log.v("Skip Track", "Artist : " + mTrackInfoStruct[TRACK_INFO_NEXT].mTrackArtist);

            Log.v("Skip Track", "*** Track Info END.   ***");
        }

    } // End of function updateTrackInfo()

    // Start of function clearAllNotifications()
    private void clearAllNotifications() {

        // Clear all the previously shown notifications
        if ( mNotificationManager != null ) {

            // Cancelling the notifications
            for ( int notificationId = REGULAR_NOTIFICATION_IDS_START; notificationId <= mNotificationID; ++notificationId ) {

                // Cancel the notification with the specified id
                mNotificationManager.cancel(notificationId);

            }

            mNotificationID = REGULAR_NOTIFICATION_IDS_START; // Reset the notification ID
        }
    } // End of function clearAllNotifications()

} // End of class MainActivity
