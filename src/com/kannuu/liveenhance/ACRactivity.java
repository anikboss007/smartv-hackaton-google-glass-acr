package com.kannuu.liveenhance;

import java.io.InvalidObjectException;
import com.example.acrtutorial.R;
import com.google.android.glass.app.Card;
import com.google.android.glass.app.Card.ImageLayout;
import com.gracenote.gnsdk.Acr.GnAcr;
import com.gracenote.gnsdk.Acr.GnAcrAudioConfig;
import com.gracenote.gnsdk.Acr.GnAcrStatus;
import com.gracenote.gnsdk.Acr.IGnAcrDelegate;
import com.gracenote.gnsdk.Android.GnAudioSourceMic;
import com.gracenote.gnsdk.Android.IGnAudioSourceDelegate;
import com.gracenote.gnsdk.Manager.GnException;
import com.gracenote.gnsdk.Manager.GnSdk;
import com.gracenote.gnsdk.Manager.GnUser;
import com.gracenote.gnsdk.Metadata.GnIterator;
import com.gracenote.gnsdk.Metadata.GnResult;
import com.gracenote.gnsdk.Metadata.GnTypes.GnUserRegistrationType;
import com.gracenote.gnsdk.MetadataACR.IGnAcrMatch;
import com.gracenote.gnsdk.MetadataEPG.IGnTvAiring;
import com.gracenote.gnsdk.MetadataEPG.IGnTvChannel;
import com.gracenote.gnsdk.MetadataVideo.IGnVideoTitle;
import com.kannuu.liveenhance.api.KannuuApi;
import com.kannuu.liveenhance.api.KannuuSearchTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;



public class ACRactivity extends Activity  implements IGnAcrDelegate, IGnAudioSourceDelegate   {

	// Get these from Gracenote
	private static String client_id = null;
	private static String client_tag = null;
	private static String license_data = null;

	
	//Glass UI 
	private Card card;
	private AudioManager audioManager;
	//private GestureDetector gestureDetector;

	private boolean             isListening    = false; /* Mic listening flag */

	// Needed Gracenote SDK objects
	private GnUser              acrUser        = null;  /* GN user object */
	private GnAcrAudioConfig    acrAudioConfig = null;  /* GN audio config object */
	private GnAcr               acr            = null;  /* GN acr object */
	private GnAudioSourceMic    audioSource    = null;  /* GN mic object */
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
      

        card = new Card(this);
        card.setText("Tap to start Listening");
        //card.setImageLayout(ImageLayout.LEFT);
        //card.addImage(R.raw.gracenote);
        
        
        View cardView = card.toView();
        setContentView(cardView);
        
        client_id    = getString(R.string.client_id);
        client_tag   = getString(R.string.client_tag);
        license_data = getString(R.string.license_text);
        
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        try {
            // Initialize the SDK. The license enables both video and music.
            GnSdk.initialize(license_data, license_data.length());

            // Create user for video. Set as default user.
            acrUser = loadOrCreateUser(client_id, client_tag);
            GnSdk.setDefaultUser(acrUser);
       
            
        } catch (GnException e) {
            Log.e("ACR", "GNSDK initialize failed with an error: " + e.getLocalizedMessage());
        }
        
    }
    
    
    private void startACR() {

    	card.setText("Tap to stop");
    	
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            updateStatusScreenInMainThread("Unable to start ACR. The external storage is not available.", false);
            return;
        }

        try {

            // Create ACR object with user
            acr = new GnAcr(acrUser, this);

            // Set up audio configuration
            if (acrAudioConfig == null) setupAudioConfig();

            // Initialize ACR.
            acr.audioInit(acrAudioConfig);

            // Create the audio source by connecting it to the ACR object
            audioSource = new GnAudioSourceMic();
            audioSource.init(acrAudioConfig, this);

            // Start the audio Source
            audioSource.start();

        } catch (GnException e) {
            updateStatusScreenInMainThread(e.getLocalizedMessage(), false);
        } catch (Exception e) {
            updateStatusScreenInMainThread(e.getLocalizedMessage(), false);
        }
    }
    
 // Allocate and initialize Audio Configuration object
    private void setupAudioConfig() {

        int sampleRate = 44100;
        if(checkSampleRate(sampleRate) < 0){
            Log.i("SampleRate Support", "SampleRate 44100 is not supported falling back to 8000");
            sampleRate = 8000;
        }

        int channel = 1;
    
        acrAudioConfig = new GnAcrAudioConfig(
                    GnAcrAudioConfig.GnAcrAudioSourceMic,
                    GnAcrAudioConfig.GnAudioSampleFormatPCM16,
                    sampleRate,
                    channel);
    }


    private int checkSampleRate(int sampleRate) {

        int err = -1;
        try{
             int bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                         AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

             AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
    					sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
    					bufferSize * 5);
            err = 0;
            audioRecord.release();
            audioRecord = null;
        } catch (Exception e) {
            err = -1;
        }
        return err;
    }


    private void stopACR() {

        audioSource.stop();
        //acr.release();
        //audioSource = null;
        //acr = null;
        card.setText("Tap to start listening...");
        updateStatusScreenInMainThread("Listening stopped", false);
    
    }

    private enum CallbackType {STATUS, RESULT}  // Add as class variable

 // Update result message UI in main thread
 private void updateResultScreenInMainThread(String msg, boolean b) {
    if (msg == null || msg.length() <= 0) {
       return;
    }
    MainThread mThread = new MainThread(msg, b, CallbackType.RESULT);
    this.runOnUiThread(mThread);

 }

 // Update status message UI in main thread
 private void updateStatusScreenInMainThread(String msg, boolean b) {

     if (msg == null || msg.length() <= 0) {
        return;
     }
     MainThread mThread = new MainThread(msg, b, CallbackType.STATUS);
     this.runOnUiThread(mThread);
 }

 /*
  * Our MainActivity class implements IGnAudioSourceDelegate and we need to code an audioBytesReady() method for it.
  * The SDK calls this method when recorded audio is available.
  */
 @Override
 public void audioBytesReady(byte[] data, int length) {
 try {
         if (acr != null)
         {
             acr.writeBytes(data, length);
         }

     } catch (GnException e) {
         updateResultScreenInMainThread(e.getLocalizedMessage(), false);
     }
 }

 private String getFormattedPosition(String adjustedPosition) {
     int msPosition = Integer.valueOf(adjustedPosition);
     int secPosition = msPosition / 1000;

     int hour = secPosition / 3600;
     int min = (secPosition / 60) % 60;
     int sec = secPosition % 60;

     return String.format("%02d:%02d:%02d", hour, min, sec);
 }

 
 private class StopAudioThread implements Runnable {

	@Override
	public void run() {
		System.err.println("Stopping the ACR"); 
		card.setText("Tap to start listening");
		stopACR();
	}
	 
 }
 
 
 // Callbacks will be called in background thread. Use MainThread to update UI.
 private class MainThread implements Runnable {

     String msg;
     boolean clear;
     CallbackType callbackType;

     public MainThread(String inMsg, boolean inClear,
                       CallbackType inCallbackType) {
            this.clear = true;
            this.msg = inMsg;
 	        this.callbackType = inCallbackType;
     }

     @Override
     public void run() {
    	 
         Log.d("output", "message:"  + this.msg);
   
         if (this.clear) {
            if (callbackType == CallbackType.STATUS) {
            	card.setFootnote("Operation Status\n" + this.msg);
            } else if (callbackType == CallbackType.RESULT) {
            	card.setText("Match Result\n" + this.msg);
            	//finish();
            }

         } else {
        	 card.setFootnote(this.msg + "\n else condition");
         }
           Log.i("ACR", this.msg);
          setContentView(card.toView());
       }
     

    }
 

    
    private GnUser loadOrCreateUser(String clientId, String clientTag) throws GnException {

        if (clientId == null || clientId.isEmpty() || clientTag == null || clientTag.isEmpty())
        {
            throw new GnException("No client ID or tag provided.");
        }

        String userIdFlag = "ACRUSER";

        // Check if user is already saved in preferences
        GnUser joe = null;
        SharedPreferences prefs = getSharedPreferences("GnEntouragePrefs", Context.MODE_PRIVATE);
        String serializedUser = prefs.getString(userIdFlag, null);

        if (serializedUser == null) {
            joe = new GnUser(clientId, clientTag, "1.0", GnUserRegistrationType.newUser);

            serializedUser = joe.getSerializedUser();

            // Save new user to shared preferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(userIdFlag, serializedUser);
            editor.commit();
        } else {
            joe = new GnUser(new String(serializedUser));
        }

        return joe;
    }
    
    @Override
    public void acrStatusReady(GnAcrStatus status) {

        String statusMessage = "";
        boolean doShowStatus = true;

        switch (status.getStatusType()) {
    		case GnAcrStatus.SILENCE_DETECTED:
    			 statusMessage = String.format("Silence %.2f", status.getValue());
    			 break;
    		case GnAcrStatus.SILENCE_RATIO:
    			 statusMessage = String.format("Silence ratio %.2f",status.getValue());
    			 break;
    		case GnAcrStatus.TRANSITION_DETECTED:
    			 statusMessage = "Transition detected...";
    			 break;
    		case GnAcrStatus.QUERY_BEGIN:
    			 statusMessage = "Query begin...";
    			 break;
    		case GnAcrStatus.ONLINE_LOOKUP_COMPLETE:
    			 statusMessage = "Online lookup complete...";
    			 break;
    		case GnAcrStatus.CLASSIFICATION_MUSIC:
    			 statusMessage = "Music...";
    			 break;
    		case GnAcrStatus.CLASSIFICATION_NOISE:
    			 statusMessage = "Noise...";
    			 break;
    		case GnAcrStatus.CLASSIFICATION_SPEECH:
    			 statusMessage = "Speech...";
    			 break;
    		case GnAcrStatus.NORMAL_MATCH_MODE:
    			 statusMessage = "Normal match mode...";
    			 break;
    		case GnAcrStatus.NO_MATCH_MODE:
    			 statusMessage = "No match mode...";
    			 break;
    		case GnAcrStatus.ERROR:
    			 statusMessage = String.format("Error %s(0x%x)", status.getErrorDescpription(), status.getError());
    			 break;
    		case GnAcrStatus.DEBUG:
    		default:
    			 statusMessage = "Debug: " + status.getMessage();
    			 break;
        }

        if (statusMessage != null & statusMessage.length() > 0) {
            updateStatusScreenInMainThread(statusMessage, false);
        }
     }
    
    @Override
    public void acrResultReady(GnResult result) {

        // Get the enumerator to access the ACR Match objects (GnAcrMatch)

        try {
                int nMatch = 0;
                GnIterator matches = result.getAcrMatch();

                // Iterate through matches
                while (matches.hasNext()) {
                    IGnAcrMatch match = (IGnAcrMatch)matches.next();
                    String matchPosition = getFormattedPosition(match.getAdjustedPosition());

                    // Get the GnTvAiring from the GnAcrMatch
                    IGnTvAiring airing = match.getTvAiring();

                    if (airing != null) {

                        // Get the GnTvChannel from the GnTvAiring
                        IGnTvChannel channel = airing.getChannel();

                        IGnVideoTitle title = match.getTitle();
                        IGnVideoTitle subTitle = match.getSubTitle();

                        
                        // Get the title, subtitle, Channel callsign and position (ms from beginning of work/program) from this GnAcrMatch
                        String displayString = String.format(
                               "ACR: %s(%s) %s %s (Match #%d)", (title != null ? title.getDisplay() : "N/A"),
                                (subTitle != null ? subTitle.getDisplay() : "N/A"), channel.getCallsign(), matchPosition, ++nMatch);

                        // Display result
                        StopAudioThread mThread = new StopAudioThread();
                        this.runOnUiThread(mThread);
                        Intent myIntent=new Intent(this.getBaseContext(),ResultActivity.class);
                        myIntent.putExtra("searchTitle", title.getDisplay());
               		 	startActivity(myIntent);
               		 
                        break;
                        
                    }
                }

            } catch (InvalidObjectException e) {
                  e.printStackTrace();
            } catch (GnException e) {

                  e.printStackTrace();
            }
    }

	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
			 audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
			if(isListening){
				stopACR();
				isListening = false;
			}else{
				startACR();
				isListening = true;
			}
			
		}

		return super.onKeyDown(keycode, event);
	}

}
