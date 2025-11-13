package MedianCut.PaletteCreator;

import Data.Pixel;
import Data.Models.OperationEnum;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class that provides efficient in-place sorting for lists of Pixels.
 * Sorting order depends on the selected color space and the specified channel priority.
 */
public final class PixelSorter {
    private PixelSorter() {}

    /**
     * Sorts a list of pixels according to a color space operation and channel order.
     * Sorting modifies the list in place and uses a stable O(n log n) algorithm.
     *
     * @param pixels list of pixels to be sorted (modified in place)
     * @param operation color space type (e.g., RGB, HSB, HSL, GRAYSCALE, OKLAB)
     * @param order 3-element array defining the order of priority for channels (NONE ignored)
     */
    public static void sort(List<Pixel> pixels, OperationEnum operation, Enum<?>[] order) {
        if (pixels == null || pixels.isEmpty() || operation == null) return;

        // Grayscale is treated as a single-channel case; use intensity-based sorting.
        if (operation == OperationEnum.GRAYSCALE) {
            pixels.sort(Comparator.comparingDouble(p -> p.values[0] + p.values[1] + p.values[2]));
            
            return;
        }

        // Build priority array with channel indices (ordinal - 1 to skip NONE)
        int[] priority = new int[3];
        int count = 0;
        
        for (Enum<?> e : order) {
            if (e == null) continue;
            
            int idx = e.ordinal() - 1;
            
            if (idx >= 0) priority[count++] = idx;
        }

        if (count == 0) return; // Nothing to sort if no valid channels are defined

        final int innerCount = count;

        // Comparator comparing pixel values based on channel priority
        Comparator<Pixel> comparator = (a, b) -> {
            for (int i = 0; i < innerCount; i++) {
                int idx = priority[i];
                double diff = a.values[idx] - b.values[idx];
                
                if (diff < 0) return -1;
                
                if (diff > 0) return 1;
            }
            
            return 0;
        };

        // Stable in-place sort
        Collections.sort(pixels, comparator);
    }
}