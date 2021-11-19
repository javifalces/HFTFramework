package com.lambda.investing.trading_engine_connector.paper.latency;

import com.lambda.investing.model.Util;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.util.MathUtils;

import java.util.Date;
import java.util.Random;

public class PoissonLatencyEngine extends FixedLatencyEngine {

	private long meanLatencyMs;
	private Random r;

	private double L;
	private PoissonDistribution pd;
	private long seed;

	public PoissonLatencyEngine(long meanLatencyMs, long seed) {
		super(meanLatencyMs);
		this.meanLatencyMs = meanLatencyMs;
		this.seed = seed;
		r = new Random(seed);
		init();
	}

	public PoissonLatencyEngine(long meanLatencyMs) {
		super(meanLatencyMs);
		this.meanLatencyMs = meanLatencyMs;
		r = new Random();
		init();
	}

	private void init() {
		if (meanLatencyMs > 0) {
			L = Math.exp(-meanLatencyMs);
			pd = new PoissonDistribution(null, meanLatencyMs, PoissonDistribution.DEFAULT_EPSILON,
					PoissonDistribution.DEFAULT_MAX_ITERATIONS);
			if (this.seed != 0) {
				pd.reseedRandomGenerator(seed);
			}
		}

	}

	/**
	 * after k iterations the loop condition becomes
	 * https://en.wikipedia.org/wiki/Poisson_distribution#Generating_Poisson-distributed_random_variables
	 * <p>
	 * p1 * p2 * ... * pk > L
	 * <p>
	 * which is equivalent to
	 * <p>
	 * -ln(p1)/mean -ln(p2)/mean ... -ln(pk)/mean > 1
	 *
	 * @return
	 */
	protected long getLatencyMs() {
		//		org.apache.commons.math3
		//		long output = pd.sample();

		//		// https://stackoverflow.com/a/9832977
		if (meanLatencyMs < 0) {
			return 0L;
		}
		int k = 0;
		double p = 1.0;
		do {
			p = p * r.nextDouble();
			k++;
		} while (p > L);
		long output = k - 1;
		//
		output = Math.min(2 * meanLatencyMs, output);
		output = Math.max(meanLatencyMs / 2, output);
		//

		return output;

	}

	@Override public void delay() {
		long delay = getLatencyMs();
		delayThread(delay);
	}

}
