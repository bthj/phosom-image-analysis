package net.nemur.phosom.image.analysis;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import net.nemur.phosom.image.domain.ImageSimilarity;

@Path("similarity")
public class ImageSimilarityAnalyzer {

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public ImageSimilarity getImageSimilarity(
			@QueryParam("url1")String url1, 
			@QueryParam("url2")String url2) {
		ImageSimilarity imageSimilarity = new ImageSimilarity();
		imageSimilarity.setFirstImageUrl(url1);
		imageSimilarity.setSecondImageUrl(url2);
		
		return imageSimilarity;
	}
}
