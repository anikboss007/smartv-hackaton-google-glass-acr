package com.kannuu.liveenhance.api;

import java.util.List;

import android.os.AsyncTask;

public class KannuuSearchTask extends AsyncTask<String, Integer, List<KannuuResult>>{

	KannuuApi kApi = new KannuuApi();
	AsyncResponse response = null;
	
	public KannuuSearchTask(AsyncResponse response){
		this.response = response;
	}

	@Override
	protected List<KannuuResult> doInBackground(String... arg0) {
		//just assume a single search term..
		return kApi.getResultsForSearchTerm(arg0[0]);
	}
	
	@Override
	 protected void onPostExecute(List<KannuuResult> result) {
        response.processFinish(result);
     }

}
