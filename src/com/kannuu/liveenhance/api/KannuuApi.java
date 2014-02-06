package com.kannuu.liveenhance.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class KannuuApi {

	private static final int IO_BUFFER_SIZE =  4 * 1024;
	
	public KannuuApi(){
		
	}
	
	public void runOnTv(String id){
		String urlToOpen = "http://api.kannuu.com/api/2/user/send?api_key=kannuuapikey7718&token=76f0a6e804e385e54f6969ea78a6f208&cid=710f08502954167a9dbb7c6e601aadaa&id=" + id + "&type=tv%3Atitle&op=show";
		try {
			System.err.println("opening:" + urlToOpen);
			URL url = new URL(urlToOpen);
			url.getContent();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public List<KannuuResult> getResultsForSearchTerm(String term){
		List<KannuuResult> results = new ArrayList<KannuuResult>();
		String raw = getJSONForSearchTerm(term);
		
		try {
			JSONObject rawJSON = new JSONObject(raw);
			JSONArray _results = rawJSON.getJSONObject("response").getJSONArray("results");
			for(int i=0; i<_results.length(); i++){
				results.add(map(_results.getJSONObject(i)));
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
		
	}
	
	private KannuuResult map(JSONObject json){
		KannuuResult r = new KannuuResult();
		try {
			Bitmap bitmap = BitmapFactory.decodeStream((InputStream)new URL(json.getString("image")).getContent());
			r.setTitle(json.getString("title"));
			r.setId(json.getString("id"));
			r.setReleased(json.getString("released"));
			r.setImage(bitmap);
			r.setImageUrl(json.getString("image"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return r;	
	}
	
	
	private String getJSONForSearchTerm(String term){
		String searchTerm;
	
		searchTerm = term.replaceAll(" " , "%20");
		String url = "http://api.kannuu.com/api/2/tv/title?api_key=kannuuapikey7718&search=" + searchTerm + "&page=1&page_size=10";
		System.err.println("loading with URL:" + url);
		String result = loadJSON(url);
		return result;
	}
	
	private String loadJSON(String url) {
		StringBuffer sb = new StringBuffer();	
		try 
		{
			URL _url = new URL(url);

			// Read all the text returned by the server
			BufferedReader in = new BufferedReader(new InputStreamReader(_url.openStream()));
			String str;
			
			while ((str = in.readLine()) != null) 
			{
				sb.append(str);
			}
			in.close();
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
}
