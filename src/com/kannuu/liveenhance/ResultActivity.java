package com.kannuu.liveenhance;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.List;

import com.google.android.glass.app.Card;
import com.kannuu.liveenhance.api.AsyncResponse;
import com.kannuu.liveenhance.api.KannuuApi;
import com.kannuu.liveenhance.api.KannuuResult;
import com.kannuu.liveenhance.api.KannuuSearchTask;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

public class ResultActivity extends Activity implements AsyncResponse {
	

	private Card card;
	private KannuuSearchTask searchTask = new KannuuSearchTask(this);
	private String title;
	private String id = "0";
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		card = new Card(this);
        card.setText("Getting info for:" +getIntent().getExtras().getString("searchTitle") );
        
        View cardView = card.toView();
        setContentView(cardView);
        
        this.title = getIntent().getExtras().getString("searchTitle");
        searchTask.execute(this.title);
	}

	@Override
	public void processFinish(List<KannuuResult> output) {
		Card results = new Card(this);
		results.setImageLayout(Card.ImageLayout.FULL);
		for(KannuuResult result: output){
			card.setText(result.getTitle());
			card.addImage(BitmapToUri(result.getImage()));
			this.id = result.getId();
			card.setFootnote("Tap to send to TV");
		}
		setContentView(card.toView());
	}
	
	private Uri BitmapToUri(Bitmap b){
		//create a file to write bitmap data
		File f = new File(this.getBaseContext().getCacheDir(), "stuff.png");
		try {
			
			System.err.println("Writing to:" + f.getAbsolutePath());
			f.createNewFile();

			//Convert bitmap to byte array
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			b.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
			byte[] bitmapdata = bos.toByteArray();

			//write the bytes in file
			FileOutputStream fos;

			fos = new FileOutputStream(f);
			fos.write(bitmapdata);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Uri.fromFile(f);
	}
	
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
	
		if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {

			Toast t = Toast.makeText(getApplicationContext(), "Sending to TV...", Toast.LENGTH_SHORT);
			t.show();
			
			
			AsyncTask<String, Void, Void> remoteExecution = new AsyncTask<String, Void, Void>(){

				@Override
				protected Void doInBackground(String... params) {
					KannuuApi api = new KannuuApi();
					api.runOnTv(params[0]);
					return null;
				}
				
			};
			
			remoteExecution.execute(id);
			
		}

		return super.onKeyDown(keycode, event);
	}

}
