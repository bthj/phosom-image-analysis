package net.nemur.phosom.image.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;

import net.nemur.phosom.image.domain.ImageSimilarity;

@Path("similarity")
public class ImageSimilarityAnalyzer {
	
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public ImageSimilarity getImageSimilarity(
			@QueryParam("url1")String url1, 
			@QueryParam("url2")String url2) throws InterruptedException, ExecutionException {
		
		// spawn two threads to histograms form urls
		FutureTask<MultidimensionalHistogram> histogramFuture1 =
				new FutureTask<MultidimensionalHistogram>( 
						new GetHistogramFromImageUrlThread(url1) );
		FutureTask<MultidimensionalHistogram> histogramFuture2 =
				new FutureTask<MultidimensionalHistogram>( 
						new GetHistogramFromImageUrlThread(url2) );
		executorService.execute(histogramFuture1);
		executorService.execute(histogramFuture2);
		
		MultidimensionalHistogram histogram1 = histogramFuture1.get();
		MultidimensionalHistogram histogram2 = histogramFuture2.get();
		double distanceBetweenImages = histogram1.compare(histogram2, DoubleFVComparison.EUCLIDEAN);
		
		ImageSimilarity imageSimilarity = new ImageSimilarity();
		imageSimilarity.setFirstImageUrl(url1);
		imageSimilarity.setSecondImageUrl(url2);
		imageSimilarity.setDistance(distanceBetweenImages);
		
		return imageSimilarity;
	}
	
	private class GetHistogramFromImageUrlThread implements Callable<MultidimensionalHistogram> {
		String imageUrl;
		public GetHistogramFromImageUrlThread( String imageUrl ) {
			this.imageUrl = imageUrl;
		}
		public MultidimensionalHistogram call() throws IOException {
			return getHistogramFromInputStream(getInputStreamFromUrl(imageUrl));
		}
		
		private MultidimensionalHistogram getHistogramFromInputStream( InputStream is ) throws IOException {
			HistogramModel model = new HistogramModel(4, 4, 4);
			model.estimateModel(ImageUtilities.readMBF(is));
			return model.histogram;
		}
		private InputStream getInputStreamFromUrl( String urlString ) throws IOException {
			URL url = new URL( urlString );
			HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setConnectTimeout(15 * 1000);
			httpConn.connect();
			return httpConn.getInputStream();
		}
	}
}
