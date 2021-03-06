package org.biojava.nbio.structure.align.symm.order;

import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.util.Pair;
import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.align.model.AFPChain;
import org.biojava.nbio.structure.align.util.RotationAxis;
import org.biojava.nbio.structure.symmetry.internal.OrderDetector;
import org.biojava.nbio.structure.symmetry.internal.RefinerFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines order by smoothing and counting the number of peaks.
 * @author dmyersturnbull
 */
public class PeakCountingOrderDetector implements OrderDetector {
	private static final Logger logger = LoggerFactory.getLogger(PeakCountingOrderDetector.class);

	private int maxOrder = 9;
	private double degreeSampling = 1;
	private double epsilon = 0.000001;
	private double bandwidth = 0.1;
	private int robustnessIterations = LoessInterpolator.DEFAULT_ROBUSTNESS_ITERS;
	private double loessAccuracy = LoessInterpolator.DEFAULT_ACCURACY;

	public PeakCountingOrderDetector(int maxOrder) {
		super();
		this.maxOrder = maxOrder;
	}

	@Override
	public int calculateOrder(AFPChain afpChain, Atom[] ca) throws RefinerFailedException {

		try {

			RotationAxis axis = new RotationAxis(afpChain);
			logger.info("Calculating rotation samples");
			Pair<double[],double[]> pair = RotationOrderDetector.sampleRotations(ca, axis, degreeSampling);
			logger.info("Smoothing with LOESS");
			LoessInterpolator loess = new LoessInterpolator(bandwidth, robustnessIterations, loessAccuracy);

			double[] smoothed = loess.smooth(pair.getKey(), pair.getValue());
			logger.info("Counting Peaks");
			
			int nPeaks = countPeaks(smoothed, epsilon * Math.PI/180);
			logger.info("Found {} peaks",nPeaks);
			
			/*
			 *  TODO Currently this isn't likely to handle order=1 well,
			 *  since C1 cases can easily have, say, exactly 5 peaks.
			 *  We will need to combine this with some smarter fitting method.
			 */

			return nPeaks; // for now
			
//			return nPeaks>maxOrder? 1 : nPeaks;

		} catch (Exception e) {
			throw new RefinerFailedException(e);
		}

	}

	private int countPeaks(double[] values, double epsilon) {

		// TODO There's an off-by-1 error in odd cases
		// TODO I don't think we can actually use epsilon
		
		int nPeaks = 0;
		boolean previouslyIncreased = false;
		double previousValue = values[0]; // we can't have this count as previouslyIncreased
		for (double value : values) {
			if (previouslyIncreased && value < previousValue) {
				nPeaks++;
			}
			if (value > previousValue) {
				previouslyIncreased = true;
			} else {
				previouslyIncreased = false;
			}
			previousValue = value;
		}
		
		return nPeaks;

	}
	
	public void setMaxOrder(int maxOrder) {
		this.maxOrder = maxOrder;
	}

	public void setDegreeSampling(double degreeSampling) {
		this.degreeSampling = degreeSampling;
	}

	/**
	 * <em>After smoothing</em>, a threshold to count a peak.
	 * This should be very small but nonzero.
	 * This helps prevent spurious peaks from rounding errors from being counted.
	 * @param epsilon
	 */
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public void setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
	}

	public void setRobustnessIterations(int robustnessIterations) {
		this.robustnessIterations = robustnessIterations;
	}

	public void setLoessAccuracy(double loessAccuracy) {
		this.loessAccuracy = loessAccuracy;
	}

	@Override
	public String toString() {
		return "PeakCountingOrderDetector [maxOrder=" + maxOrder
				+ ", degreeSampling=" + degreeSampling + ", epsilon=" + epsilon
				+ ", bandwidth=" + bandwidth + ", robustnessIterations="
				+ robustnessIterations + ", loessAccuracy=" + loessAccuracy
				+ "]";
	}
}
