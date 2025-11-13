package Data;

import java.util.Comparator;
import java.util.List;

/**
 * Bucket variant that treats the first component as a circular value (hue).
 * Splitting orders pixels by hue (circularly) and computes circular-aware
 * averages and variation for the hue channel while computing linear measures
 * for the remaining channels.
 */
public class BucketCircular extends Bucket {
    public BucketCircular(List<Pixel> pixels) {
        super(pixels);
    }

    protected BucketCircular(List<Pixel> pixels, int start, int end) {
        super(pixels, start, end);
    }

    /**
     * Split this bucket into two halves. Before splitting, sort the
     * sublist by normalized hue so halves are contiguous in hue space.
     *
     * @return array with two buckets: [firstHalf, secondHalf]. If the bucket has 
     * size <= 1 the second element will be null.
     */
    @Override
    public Bucket[] split() {
        if (size <= 1) return new Bucket[]{this, null};

        int mid = start + size / 2;

        // Sort the interval [start, end) by normalized hue value.
        pixels.subList(start, end).sort(Comparator.comparingDouble(p -> normalizeHue(p.values[0])));

        Bucket first = new BucketCircular(pixels, start, mid);
        Bucket second = new BucketCircular(pixels, mid, end);
        
        return new Bucket[]{first, second};
    }

    /**
     * Compute the weighted average pixel for this bucket where the hue
     * (component 0) is treated as a circular quantity. The hue average is
     * computed via mean angle using unit-circle aggregation. The remaining
     * channels are computed as simple arithmetic means.
     *
     * @return new Pixel containing averaged components [hue, c1, c2]
     */
    @Override
    public Pixel averagePixel() {
        if (size == 0) return new Pixel(0.0, 0.0, 0.0);

        double[] avg = new double[3];
        double sumCos = 0.0;
        double sumSin = 0.0;
        int totalWeight = 0;

        // For hue (circular), aggregate unit vectors. Use pixel weights if available.
        for (int i = start; i < end; i++) {
            Pixel p = pixels.get(i);
            int w = p.count;
            double theta = p.values[0] * 2.0 * Math.PI;
            sumCos += w * Math.cos(theta);
            sumSin += w * Math.sin(theta);
            totalWeight += w;
        }

        if (totalWeight == 0) totalWeight = size; // fallback to uniform weights

        double meanAngle = Math.atan2(sumSin, sumCos);
        
        if (meanAngle < 0) meanAngle += 2.0 * Math.PI;
        
        avg[0] = meanAngle / (2.0 * Math.PI);

        // Compute arithmetic mean for other channels using weights if available.
        for (int channel = 1; channel < 3; channel++) {
            double sum = 0.0;
            
            for (int i = start; i < end; i++) {
                Pixel p = pixels.get(i);
                int w = p.count;
                sum += p.values[channel] * w;
            }
            
            avg[channel] = sum / totalWeight;
        }

        return new Pixel(avg[0], avg[1], avg[2]);
    }

    /**
     * Calculate combined variation for this bucket. Circular variation for the
     * hue channel is computed from the resultant vector length R (circVar = 1 - R).
     * Linear variance is computed for the remaining channels and added to the
     * circular variance to produce a single scalar measure.
     *
     * @return computed variation value
     */
    @Override
    protected double calculateVariation() {
        if (size == 0) return 0.0;

        double sumCos = 0.0;
        double sumSin = 0.0;
        double[] mean = new double[3];
        int totalWeight = 0;

        // Weighted aggregation for the circular channel
        for (int i = start; i < end; i++) {
            Pixel p = pixels.get(i);
            int w = p.count;
            double theta = p.values[0] * 2.0 * Math.PI;
            sumCos += w * Math.cos(theta);
            sumSin += w * Math.sin(theta);
            totalWeight += w;
        }

        if (totalWeight == 0) totalWeight = size; // fallback to uniform weights

        // Resultant length normalized by total weight
        double R = Math.sqrt(sumCos * sumCos + sumSin * sumSin) / totalWeight;
        double circVar = 1.0 - R;

        double meanAngle = Math.atan2(sumSin, sumCos);
        
        if (meanAngle < 0) meanAngle += 2.0 * Math.PI;
        
        mean[0] = meanAngle / (2.0 * Math.PI);

        // Compute weighted means for linear channels
        for (int channel = 1; channel < 3; channel++) {
            double s = 0.0;
            
            for (int i = start; i < end; i++) {
                Pixel p = pixels.get(i);
                int w = p.count;
                s += p.values[channel] * w;
            }
            
            mean[channel] = s / totalWeight;
        }

        // Start variation with circular variance contribution
        double var = circVar;

        // Add linear variance contributions for other channels (weighted)
        for (int channel = 1; channel < 3; channel++) {
            double sumSq = 0.0;
            
            for (int i = start; i < end; i++) {
                Pixel p = pixels.get(i);
                int w = p.count;
                double d = p.values[channel] - mean[channel];
                sumSq += w * d * d;
            }
            
            var += sumSq / totalWeight;
        }

        return var;
    }

    private static double normalizeHue(double h) {
        h = h % 1.0;
        
        if (h < 0.0) h += 1.0;
        
        return h;
    }
}