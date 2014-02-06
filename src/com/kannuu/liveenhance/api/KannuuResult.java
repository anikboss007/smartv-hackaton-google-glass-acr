package com.kannuu.liveenhance.api;

import android.graphics.Bitmap;

public class KannuuResult {
	private String released;
	private Bitmap image;
	private String imageUrl;
	private String id;
	private String title;
	
	public String getReleased() {
		return released;
	}
	public void setReleased(String released) {
		this.released = released;
	}
	public Bitmap getImage() {
		return image;
	}
	
	public void setImage(Bitmap image) {
		this.image = image;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	
}
