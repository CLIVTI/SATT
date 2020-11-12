package utility;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.google.common.primitives.Doubles;

public class MultinomialDistributionSampler implements RandomDistributionSampler {
	private final static Logger log = Logger.getLogger(RandomDistributionSampler.class);
	protected Random random = new Random();
	protected List<Double> weights = null;
	protected double sum = 0;
	protected double[] cdfWeights = null;

	
	public MultinomialDistributionSampler() {
		
	}
	
	public MultinomialDistributionSampler(List<Double> weights) {
		super();
		this.weights = weights;
		this.cdfWeights = cdfWeights(weights);
		this.sum = this.cdfWeights[weights.size() - 1];
	}
	
	public MultinomialDistributionSampler(double[] weights) {
		super();
		List<Double> weightsList =Doubles.asList(weights);
		this.weights = weightsList;
		this.cdfWeights = cdfWeights(weightsList);
		this.sum = this.cdfWeights[weightsList.size() - 1];
	}
	

	public static double[] cdfWeights(List<Double> weights) {
		double[] sortedWeights = new double[weights.size()];
		sortedWeights[0] = weights.get(0);
		for (int i = 1; i < weights.size(); i++) {
			sortedWeights[i] = sortedWeights[i - 1] + weights.get(i);
		}
		return sortedWeights;
	}

	@Override
	public Integer sample() {
		int index = Arrays.binarySearch(this.cdfWeights, random.nextDouble() * this.sum);
		return (index >= 0) ? index : (-index - 1);
	}

	public Integer[] sampleWithReplacement(int n) {
		Integer[] samples = new Integer[n];
		for (int i = 0; i < n; i++) {
			int index = Arrays.binarySearch(this.cdfWeights, random.nextDouble() * this.sum);
			samples[i] = (index >= 0) ? index : (-index - 1);
		}
		;
		return samples;

	}

	public Integer[] sampleWithoutReplacement(int n) {
		if (n > this.sum) {
			log.warn(" number of samples cannot be more than total elements in the weights.");
			return null;
		} else {
			Integer[] samples = new Integer[n];
			int i = 1;
			int attempts = 0;

			double[] cdfWeightShrinking = Arrays.copyOfRange(this.cdfWeights, 0, this.cdfWeights.length);
			double sumShrinking = this.sum;
			while (i <= n) {
				attempts++;
				// System.out.println("Number of attempts: "+ attempts);
				// (1) sample once from the cdf
				int trialIndex1 = Arrays.binarySearch(cdfWeightShrinking, random.nextDouble() * sumShrinking);
				int trialIndex = (trialIndex1 >= 0) ? trialIndex1 : (-trialIndex1 - 1);

				// (2) check if the corresponding weight is over 1, if yes, flag returns true,
				// otherwise flag returns false
				boolean flag = true;
				if (trialIndex == 0 && cdfWeightShrinking[trialIndex] < 1) {
					flag = false;
				} else if (trialIndex > 0 && trialIndex < (cdfWeightShrinking.length)
						&& cdfWeightShrinking[trialIndex] - cdfWeightShrinking[trialIndex - 1] < 1) {
					flag = false;
				} else if (trialIndex == cdfWeightShrinking.length) {
					log.warn(" something wrong in sampling.");
					flag = false;
				}

				// (3) if the corresponding weight is over one (flag true), subtract one from the
				// cdfWeightShrinking and sumShrinking, otherwise do another trial sample
				if (flag == true) {

					for (int j = trialIndex; j < cdfWeightShrinking.length; j++) {
						cdfWeightShrinking[j] = cdfWeightShrinking[j] - 1;
					}
					samples[i - 1] = trialIndex;
					sumShrinking--;
					i++;
				}

				// (4) if we have had many attempts, check if every element in cdf is blow 1
				// that means our distribution is too sparse, like [0.1,0.1,0.1,...], 
				// and then we cant get one sample from this sparse distribution.
				if (attempts > n * 200) {
					boolean exitWhileLoopFlag = true;
					if (cdfWeightShrinking[0] >= 1) {
						exitWhileLoopFlag = false;
					}

					for (int k = 1; k < cdfWeightShrinking.length; k++) {
						if (cdfWeightShrinking[k] - cdfWeightShrinking[k - 1] >= 1) {
							exitWhileLoopFlag = false;
						}
					}

					if (exitWhileLoopFlag == true) {
						log.warn(" MultinomialDistributionSampler.sampleWithoutReplacement: result contains null.");
						break;
					}
				}

			}
			return samples;
		}

	}

	public List<Double> getWeights() {
		return weights;
	}

	public double getSum() {
		return sum;
	}

	public double[] getCdfWeights() {
		return cdfWeights;
	}

	@Override
	public void setSeed(long seed) {
		this.random.setSeed(seed);
	}

}
