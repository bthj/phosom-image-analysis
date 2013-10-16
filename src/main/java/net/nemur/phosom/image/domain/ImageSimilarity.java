package net.nemur.phosom.image.domain;


public class ImageSimilarity {

	private String firstImageUrl;
	private String secondImageUrl;
	private Double distance;
	
	public String getFirstImageUrl() {
		return firstImageUrl;
	}
	public void setFirstImageUrl(String firstImageUrl) {
		this.firstImageUrl = firstImageUrl;
	}
	public String getSecondImageUrl() {
		return secondImageUrl;
	}
	public void setSecondImageUrl(String secondImageUrl) {
		this.secondImageUrl = secondImageUrl;
	}
	public Double getDistance() {
		return distance;
	}
	public void setDistance(Double distance) {
		this.distance = distance;
	}
}
