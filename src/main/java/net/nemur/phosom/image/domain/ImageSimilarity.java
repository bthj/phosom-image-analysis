package net.nemur.phosom.image.domain;



public class ImageSimilarity {

	private String firstImageUrl;
	private String secondImageUrl;
	private DistanceValues distanceValues;
	private Integer featureMatchesCountBasic;
	private Integer featureMatchesCountRANSAC;
	
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
	public DistanceValues getDistanceValues() {
		return distanceValues;
	}
	public void setDistanceValues(DistanceValues distanceValues) {
		this.distanceValues = distanceValues;
	}
	public Integer getFeatureMatchesCountBasic() {
		return featureMatchesCountBasic;
	}
	public void setFeatureMatchesCountBasic(Integer featureMatchesCountBasic) {
		this.featureMatchesCountBasic = featureMatchesCountBasic;
	}
	public Integer getFeatureMatchesCountRANSAC() {
		return featureMatchesCountRANSAC;
	}
	public void setFeatureMatchesCountRANSAC(Integer featureMatchesCountRANSAC) {
		this.featureMatchesCountRANSAC = featureMatchesCountRANSAC;
	}
	
}
