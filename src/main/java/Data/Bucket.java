package Data;

import java.util.List;

/**
 * Represents a contiguous bucket (interval) of pixels inside a shared list.
 * The bucket provides utilities to split itself, compute the weighted average
 * pixel and a variance-like measure (variation) computed lazily.
 */
public class Bucket {
    protected final List<Pixel> pixels;
    
    // index interval [start, end)
    protected final int start;
    protected final int end;
    
    protected double variation = -1; // lazy-evaluated cached variation

    public int size;

    /**
     * Create a bucket that covers the entire list.
     *
     * @param pixels list containing pixel entries
     */
    public Bucket(List<Pixel> pixels) {
        this(pixels, 0, pixels.size());
    }

    /**
     * Create a bucket that covers the sub-interval [start, end).
     *
     * @param pixels shared pixel list
     * @param start inclusive start index
     * @param end exclusive end index
     */
    protected Bucket(List<Pixel> pixels, int start, int end) {
        this.pixels = pixels;
        this.start = start;
        this.end = end;
        this.size = end - start;
    }

    /**
     * Split this bucket into two halves and return them as an array:
     * [firstHalf, secondHalf].
     *
     * @return two buckets that partition this bucket's interval
     */
    public Bucket[] split() {
        int mid = start + size / 2;
        Bucket first = new Bucket(pixels, start, mid);
        Bucket second = new Bucket(pixels, mid, end);
        
        return new Bucket[]{first, second};
    }

    /**
     * Compute the weighted average color of the pixels contained in this bucket.
     * Each Pixel has a weight stored in Pixel.count and three component values in
     * Pixel.values. If the total weight is zero, 1 is used to avoid division by zero.
     *
     * @return a new Pixel representing the averaged color (weights applied)
     */
    public Pixel averagePixel() {
        double[] avg = new double[3];
        int totalCount = 0;

        for (int i = start; i < end; i++) {
            Pixel p = pixels.get(i);
            int w = p.count;
            avg[0] += p.values[0] * w;
            avg[1] += p.values[1] * w;
            avg[2] += p.values[2] * w;
            totalCount += w;
        }

        if (totalCount == 0) totalCount = 1;

        avg[0] /= totalCount;
        avg[1] /= totalCount;
        avg[2] /= totalCount;

        return new Pixel(avg[0], avg[1], avg[2]);
    }

    /**
     * Retrieve the cached variation value, computing it on first call.
     *
     * @return the computed variation
     */
    public double getVariation() {
        if (variation < 0) variation = calculateVariation();
        
        return variation;
    }

    /**
     * Calculate the weighted sum of squared distances from the mean across the
     * three components, divided by the total weight. If total weight is zero,
     * 1 is used to avoid division by zero.
     *
     * @return computed variation for this bucket
     */
    protected double calculateVariation() {
        double[] mean = new double[3];
        int totalCount = 0;

        for (int i = start; i < end; i++) {
            Pixel p = pixels.get(i);
            int w = p.count;
            mean[0] += p.values[0] * w;
            mean[1] += p.values[1] * w;
            mean[2] += p.values[2] * w;
            totalCount += w;
        }
        
        if (totalCount == 0) totalCount = 1;

        mean[0] /= totalCount;
        mean[1] /= totalCount;
        mean[2] /= totalCount;

        double var = 0;
        
        for (int i = start; i < end; i++) {
            Pixel p = pixels.get(i);
            int w = p.count;
            double dx = p.values[0] - mean[0];
            double dy = p.values[1] - mean[1];
            double dz = p.values[2] - mean[2];
            var += w * (dx*dx + dy*dy + dz*dz);
        }
        
        return var / totalCount;
    }
}