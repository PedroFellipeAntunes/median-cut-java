package MedianCut.PaletteCreator;

import Data.Bucket;
import Data.BucketCircular;
import Data.Models.OperationEnum;
import Data.Pixel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Implements the Median Cut quantization algorithm over a list of unique Pixels.
 *
 * The algorithm repeatedly splits the bucket with the highest variation until the
 * requested number of buckets is reached (or no further split is possible). For
 * hue-based color spaces (HSL/HSB) a circular-aware bucket implementation is used.
 */
public final class MedianCut {
    private MedianCut() {}

    /**
     * Quantize the provided list of unique pixels into the requested number of buckets.
     *
     * Each returned Pixel represents the averaged color of a bucket. The input list must
     * contain Pixel instances with correct counts and channel values.
     *
     * @param pixels non-null, non-empty list of unique Pixel instances
     * @param buckets desired number of output buckets (must be > 0)
     * @param operation color space operation (used to decide if hue is circular)
     * @return list of averaged Pixels (one per resulting bucket)
     * @throws IllegalArgumentException when arguments are invalid
     */
    public static List<Pixel> quantize(List<Pixel> pixels, int buckets, OperationEnum operation) {
        if (pixels == null || pixels.isEmpty()) {
            throw new IllegalArgumentException("pixels cannot be null or empty");
        }
        
        if (buckets <= 0) {
            throw new IllegalArgumentException("buckets must be > 0");
        }
        
        if (operation == null) {
            throw new IllegalArgumentException("operation cannot be null");
        }

        // Choose bucket type depending on whether the color space treats hue circularly.
        boolean useCircular = isCircularSpace(operation);

        // Priority queue selects the bucket with the largest variation first.
        PriorityQueue<Bucket> queue = new PriorityQueue<>(Comparator.comparingDouble(Bucket::getVariation).reversed());

        // Start with a single bucket covering all pixels.
        Bucket root = useCircular ? new BucketCircular(pixels) : new Bucket(pixels);
        queue.add(root);

        // Split the highest-variation bucket until we reach the requested number.
        while (queue.size() < buckets) {
            Bucket toSplit = queue.poll(); // bucket with highest variation
            
            if (toSplit == null || toSplit.size <= 1) {
                // Nothing left to split meaningfully.
                break;
            }

            Bucket[] parts = toSplit.split();
            
            // Add valid non-empty halves back into the queue.
            if (parts[0] != null && parts[0].size > 0) queue.add(parts[0]);
            if (parts[1] != null && parts[1].size > 0) queue.add(parts[1]);
        }

        // Collect averaged pixels from remaining buckets.
        List<Pixel> palette = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            palette.add(queue.poll().averagePixel());
        }

        return palette;
    }

    // Determine if the given operation treats hue as circular.
    private static boolean isCircularSpace(OperationEnum operation) {
        // HSL and HSB treat the first channel as a circular hue.
        return switch (operation) {
            case HSB, HSL -> true;
            default -> false;
        };
    }
}