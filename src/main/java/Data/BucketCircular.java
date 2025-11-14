package Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bucket variant that treats the first component as circular (hue).
 *
 * This implementation:
 *  - when splitting, finds the largest gap on the hue circle and rotates
 *    the pixels so that gap becomes the boundary before performing a half/half split.
 *  - computes circular-aware weighted averages and variation for hue, and
 *    weighted (or unweighted fallback) linear measures for the remaining channels.
 */
public class BucketCircular extends Bucket {
    public BucketCircular(List<Pixel> pixels) {
        super(pixels);
    }

    protected BucketCircular(List<Pixel> pixels, int start, int end) {
        super(pixels, start, end);
    }

    /**
     * Split this bucket into two halves.
     *
     * For circular hue we:
     *  1) sort the sub-interval by normalized hue,
     *  2) find the largest gap between consecutive hues (including circular gap),
     *  3) rotate the sub-interval so that the largest gap lies between end and start,
     *  4) split the rotated interval into two halves (start..mid) and (mid..end).
     *
     * This prevents splitting a tight cluster that wraps around the 0/1 hue boundary.
     *
     * @return two buckets partitioning this interval, or {this, null} if not splittable.
     */
    @Override
    public Bucket[] split() {
        if (size <= 1) return new Bucket[]{this, null};

        // Work on the view of the shared list
        List<Pixel> sub = pixels.subList(start, end);

        // 1) sort by normalized hue (0..1)
        sub.sort(Comparator.comparingDouble(p -> normalizeHue(p.values[0])));

        int n = size;
        double[] h = new double[n];
        
        for (int i = 0; i < n; i++) {
            h[i] = normalizeHue(sub.get(i).values[0]);
        }

        // 2) find largest gap (including circular gap between last and first)
        double maxGap = -1.0;
        int maxGapIndex = -1; // gap located between h[i] and h[(i+1)%n]
        
        for (int i = 0; i < n; i++) {
            double cur = h[i];
            double next = h[(i + 1) % n];
            double gap;
            
            if (i < n - 1) {
                gap = next - cur;
            } else {
                // circular gap from last to first: first + 1.0 - last
                gap = (h[0] + 1.0) - cur;
            }
            
            if (gap < 0) gap += 1.0;
            
            if (gap > maxGap) {
                maxGap = gap;
                maxGapIndex = i;
            }
        }

        // 3) rotate the sublist so that the largest gap lies between end and start.
        // If the largest gap is already the circular boundary (i == n-1), no rotation needed.
        if (maxGapIndex != n - 1) {
            int cut = (maxGapIndex + 1);
            // create a temporary copy and write rotated order back into the sublist
            List<Pixel> temp = new ArrayList<>(sub);
            
            for (int i = 0; i < n; i++) {
                sub.set(i, temp.get((cut + i) % n));
            }
            // After this operation, the ordering inside pixels[start:end] has been rotated in-place.
        }

        // 4) now perform simple half/half split (contiguous ranges)
        int mid = start + n / 2;
        Bucket first = new BucketCircular(pixels, start, mid);
        Bucket second = new BucketCircular(pixels, mid, end);

        return new Bucket[]{first, second};
    }

    /**
     * Compute weighted circular mean for hue (channel 0) and weighted/unweighted mean
     * for the other channels. If all pixel counts are zero, fall back to unweighted
     * (uniform) averaging.
     *
     * @return a new Pixel with averaged channels [hue (0..1), channel1, channel2]
     */
    @Override
    public Pixel averagePixel() {
        if (size == 0) return new Pixel(0.0, 0.0, 0.0);

        double[] avg = new double[3];
        double sumCos = 0.0;
        double sumSin = 0.0;
        int totalWeight = 0;
        boolean anyWeight = false;

        // accumulate weighted vector for hue and track total weight
        for (int i = start; i < end; i++) {
            Pixel p = pixels.get(i);
            int w = p.count;
            
            if (w > 0) anyWeight = true;
            
            double theta = normalizeHue(p.values[0]) * 2.0 * Math.PI;
            sumCos += w * Math.cos(theta);
            sumSin += w * Math.sin(theta);
            totalWeight += w;
        }

        if (!anyWeight) {
            // fallback to unweighted sums: treat every pixel with weight 1
            totalWeight = size;
            sumCos = 0.0;
            sumSin = 0.0;
            
            for (int i = start; i < end; i++) {
                double theta = normalizeHue(pixels.get(i).values[0]) * 2.0 * Math.PI;
                sumCos += Math.cos(theta);
                sumSin += Math.sin(theta);
            }
        }

        // compute circular mean angle
        double meanAngle = Math.atan2(sumSin, sumCos);
        
        if (meanAngle < 0) meanAngle += 2.0 * Math.PI;
        
        avg[0] = meanAngle / (2.0 * Math.PI);

        // compute channel 1 and 2 means: weighted if anyWeight, else uniform
        for (int channel = 1; channel < 3; channel++) {
            double s = 0.0;
            
            if (anyWeight) {
                for (int i = start; i < end; i++) {
                    Pixel p = pixels.get(i);
                    s += p.values[channel] * p.count;
                }
                
                avg[channel] = s / totalWeight;
            } else {
                for (int i = start; i < end; i++) {
                    s += pixels.get(i).values[channel];
                }
                
                avg[channel] = s / size;
            }
        }

        return new Pixel(avg[0], avg[1], avg[2]);
    }

    /**
     * Compute a scalar variation measure combining:
     *  - circular variance for hue (1 - R, where R is resultant length normalized by total weight),
     *  - plus linear (weighted or unweighted) variances for the other channels.
     *
     * @return a single double used to choose which bucket to split next.
     */
    @Override
    protected double calculateVariation() {
        if (size == 0) return 0.0;

        double sumCos = 0.0;
        double sumSin = 0.0;
        double[] mean = new double[3];
        int totalWeight = 0;
        boolean anyWeight = false;

        // aggregate for circular channel
        for (int i = start; i < end; i++) {
            Pixel p = pixels.get(i);
            int w = p.count;
            
            if (w > 0) anyWeight = true;
            
            double theta = normalizeHue(p.values[0]) * 2.0 * Math.PI;
            sumCos += w * Math.cos(theta);
            sumSin += w * Math.sin(theta);
            totalWeight += w;
        }

        if (!anyWeight) {
            // fallback to unweighted aggregation
            totalWeight = size;
            sumCos = 0.0;
            sumSin = 0.0;
            
            for (int i = start; i < end; i++) {
                double theta = normalizeHue(pixels.get(i).values[0]) * 2.0 * Math.PI;
                sumCos += Math.cos(theta);
                sumSin += Math.sin(theta);
            }
        }

        // resultant vector length R (normalized)
        double R = Math.sqrt(sumCos * sumCos + sumSin * sumSin) / (double) totalWeight;
        double circVar = 1.0 - R;

        // mean hue (circular) in 0..1
        double meanAngle = Math.atan2(sumSin, sumCos);
        
        if (meanAngle < 0) meanAngle += 2.0 * Math.PI;
        
        mean[0] = meanAngle / (2.0 * Math.PI);

        // weighted or unweighted means for linear channels
        for (int ch = 1; ch < 3; ch++) {
            double s = 0.0;
            
            if (anyWeight) {
                for (int i = start; i < end; i++) {
                    Pixel p = pixels.get(i);
                    s += p.values[ch] * p.count;
                }
                
                mean[ch] = s / totalWeight;
            } else {
                for (int i = start; i < end; i++) {
                    s += pixels.get(i).values[ch];
                }
                
                mean[ch] = s / size;
            }
        }

        // start with circular variance contribution
        double var = circVar;

        // add linear variances (weighted or unweighted)
        for (int ch = 1; ch < 3; ch++) {
            double sumSq = 0.0;
            
            if (anyWeight) {
                for (int i = start; i < end; i++) {
                    Pixel p = pixels.get(i);
                    int w = p.count;
                    double d = p.values[ch] - mean[ch];
                    sumSq += w * d * d;
                }
                
                var += sumSq / totalWeight;
            } else {
                for (int i = start; i < end; i++) {
                    double d = pixels.get(i).values[ch] - mean[ch];
                    sumSq += d * d;
                }
                
                var += sumSq / (double) size;
            }
        }

        return var;
    }

    private static double normalizeHue(double h) {
        h = h % 1.0;
        
        if (h < 0.0) h += 1.0;
        
        return h;
    }
}