package net.nemur.phosom.image.analysis;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
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

import net.nemur.phosom.image.domain.DistanceValues;
import net.nemur.phosom.image.domain.ImageSimilarity;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.BasicMatcher;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.LocalFeatureMatcher;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.transforms.AffineTransformModel;
import org.openimaj.math.model.fit.RANSAC;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;
import org.openimaj.util.pair.Pair;


@Path("similarity")
public class ImageSimilarityAnalyzer {
	
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public ImageSimilarity getImageSimilarity(
			@QueryParam("url1")String url1, 
			@QueryParam("url2")String url2) throws InterruptedException, ExecutionException {
		
		// spawn two threads to get images from urls
		FutureTask<MBFImage> imageFuture1 = new FutureTask<MBFImage>(
				new GetMBFImageFromUrlStringThread(url1) );
		FutureTask<MBFImage> imageFuture2 = new FutureTask<MBFImage>(
				new GetMBFImageFromUrlStringThread(url2) );
		executorService.execute(imageFuture1);
		executorService.execute(imageFuture2);
		
		MBFImage image1 = imageFuture1.get();
		MBFImage image2 = imageFuture2.get();
		

		
		// ...and one thread for each histogram
		FutureTask<MultidimensionalHistogram> histogramFuture1 =
				new FutureTask<MultidimensionalHistogram>( 
						new GetHistogramFromImageThread(image1) );
		executorService.execute(histogramFuture1);
		
		FutureTask<MultidimensionalHistogram> histogramFuture2 =
				new FutureTask<MultidimensionalHistogram>( 
						new GetHistogramFromImageThread(image2) );
		executorService.execute(histogramFuture2);
		
		// wait for threads to finish...
		MultidimensionalHistogram histogram1 = histogramFuture1.get();
		MultidimensionalHistogram histogram2 = histogramFuture2.get();
		
		// one thread for feature matching
		FutureTask<List<Pair<Keypoint>>> featureMatchingRANSACFuture = 
				new FutureTask<List<Pair<Keypoint>>>(
						new GetFeatureMatchesRANSAC(image1, image2));
		executorService.execute(featureMatchingRANSACFuture);
		
		FutureTask<List<Pair<Keypoint>>> featureMatchingBasicFuture = 
				new FutureTask<List<Pair<Keypoint>>>(
						new GetFeatureMatchesBasic(image1, image2));
		executorService.execute(featureMatchingBasicFuture);
		
		List<Pair<Keypoint>> featureMatchesRANSAC = featureMatchingRANSACFuture.get();
		List<Pair<Keypoint>> featureMatchesBasic = featureMatchingBasicFuture.get();
		// ...and all are done, use the data they returned:
		
		DistanceValues distanceValues = new DistanceValues();
		distanceValues.setArccos(histogram1.compare(histogram2, DoubleFVComparison.ARCCOS));
		distanceValues.setBhattacharyya(histogram1.compare(histogram2, DoubleFVComparison.BHATTACHARYYA));
		distanceValues.setChiSquare(histogram1.compare(histogram2, DoubleFVComparison.CHI_SQUARE));
		distanceValues.setCityBlock(histogram1.compare(histogram2, DoubleFVComparison.CITY_BLOCK));
		distanceValues.setCorrelation(histogram1.compare(histogram2, DoubleFVComparison.CORRELATION));
		distanceValues.setCosineDist(histogram1.compare(histogram2, DoubleFVComparison.COSINE_DIST));
		distanceValues.setCosineSim(histogram1.compare(histogram2, DoubleFVComparison.COSINE_SIM));
		distanceValues.setEuclidean(histogram1.compare(histogram2, DoubleFVComparison.EUCLIDEAN));
		distanceValues.setHamming(histogram1.compare(histogram2, DoubleFVComparison.HAMMING));
		distanceValues.setIntersection(histogram1.compare(histogram2, DoubleFVComparison.INTERSECTION));
		distanceValues.setJaccardDistance(histogram1.compare(histogram2, DoubleFVComparison.JACCARD_DISTANCE));
		distanceValues.setPackedHamming(histogram1.compare(histogram2, DoubleFVComparison.PACKED_HAMMING));
		distanceValues.setSumSquare(histogram1.compare(histogram2, DoubleFVComparison.SUM_SQUARE));
		distanceValues.setSymmetricKLDivergence(histogram1.compare(histogram2, DoubleFVComparison.SYMMETRIC_KL_DIVERGENCE));
		
		ImageSimilarity imageSimilarity = new ImageSimilarity();
		imageSimilarity.setFirstImageUrl(url1);
		imageSimilarity.setSecondImageUrl(url2);
		imageSimilarity.setDistanceValues(distanceValues);
		imageSimilarity.setFeatureMatchesCountRANSAC(featureMatchesRANSAC.size());
		imageSimilarity.setFeatureMatchesCountBasic(featureMatchesBasic.size());
		
		return imageSimilarity;
	}
	
	private class GetMBFImageFromUrlStringThread implements Callable<MBFImage> {
		String urlString;
		public GetMBFImageFromUrlStringThread( String urlString ) {
			this.urlString = urlString;
		}
		public MBFImage call() throws MalformedURLException, IOException {
			return ImageUtilities.readMBF(new URL(urlString));
		}
	}
	
	private class GetHistogramFromImageThread implements Callable<MultidimensionalHistogram> {
		MBFImage image;
		public GetHistogramFromImageThread( MBFImage image ) {
			this.image = image;
		}
		public MultidimensionalHistogram call() throws IOException {
			
//			LocalFeatureMatcher<Keypoint> matcher = new BasicMatcher<Keypoint>(80);
//			matcher.get
			
			return getHistogramFromImage( image );
		}
		
		private MultidimensionalHistogram getHistogramFromImage( MBFImage image ) throws MalformedURLException, IOException {
			HistogramModel model = new HistogramModel(4, 4, 4);
			model.estimateModel( image );
			return model.histogram;
		}
	}
	
	private class GetFeatureMatchesRANSAC implements Callable<List<Pair<Keypoint>>> {
		MBFImage query, target;
		public GetFeatureMatchesRANSAC( MBFImage query, MBFImage target) {
			this.query = query;
			this.target = target;
		}
		public List<Pair<Keypoint>> call() {  // as in http://openimaj.org/tutorial/sift-and-feature-matching.html
			
			DoGSIFTEngine engine = new DoGSIFTEngine();	
			LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(query.flatten());
			LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target.flatten());
			
			AffineTransformModel fittingModel = new AffineTransformModel(5);
			RANSAC<Point2d, Point2d> ransac = 
				new RANSAC<Point2d, Point2d>(fittingModel, 1500, new RANSAC.PercentageInliersStoppingCondition(0.5), true);

			LocalFeatureMatcher<Keypoint> matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
					new FastBasicKeypointMatcher<Keypoint>(8), ransac);

			matcher.setModelFeatures(queryKeypoints);
			matcher.findMatches(targetKeypoints);

			return matcher.getMatches();
		}
	}
	private class GetFeatureMatchesBasic implements Callable<List<Pair<Keypoint>>> {
		MBFImage query, target;
		public GetFeatureMatchesBasic( MBFImage query, MBFImage target) {
			this.query = query;
			this.target = target;
		}
		public List<Pair<Keypoint>> call() {  // as in http://openimaj.org/tutorial/sift-and-feature-matching.html
			
			DoGSIFTEngine engine = new DoGSIFTEngine();	
			LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(query.flatten());
			LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target.flatten());
			
			LocalFeatureMatcher<Keypoint> matcher = new BasicMatcher<Keypoint>(80);
			matcher.setModelFeatures(queryKeypoints);
			matcher.findMatches(targetKeypoints);

			return matcher.getMatches();
		}
	}
}
