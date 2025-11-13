package MedianCut.PaletteCreator;

import Data.KdTreeRGB;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Applies a palette to an image using a KD-tree for nearest-color lookup.
 *
 * Each source pixel is replaced with the nearest palette color in RGB while
 * preserving the original alpha channel. Work is split across available CPU
 * cores by horizontal stripes.
 */
public final class PaletteToImage {
    private PaletteToImage() {}

    /**
     * Map the input image to the nearest palette colors found in the provided KD-tree.
     *
     * @param image non-null source image
     * @param tree non-null KD-tree built from the target palette
     * @return new BufferedImage (TYPE_INT_ARGB) with mapped colors
     */
    public static BufferedImage apply(BufferedImage image, KdTreeRGB tree) {
        if (image == null) throw new IllegalArgumentException("image cannot be null");
        if (tree == null) throw new IllegalArgumentException("tree cannot be null");

        final int width = image.getWidth();
        final int height = image.getHeight();

        // Prepare output image of the same dimensions with ARGB type.
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Read all source pixels into an array for faster indexed access.
        final int[] srcPixels = image.getRGB(0, 0, width, height, null, 0, width);
        final int[] outPixels = new int[srcPixels.length];

        // Determine number of worker threads and a per-thread stripe height.
        final int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final int chunkSize = (height + threads - 1) / threads;

        // Submit tasks: each task processes a horizontal stripe of rows.
        for (int t = 0; t < threads; t++) {
            final int yStart = t * chunkSize;
            final int yEnd = Math.min(height, yStart + chunkSize);

            executor.submit(() -> {
                for (int y = yStart; y < yEnd; y++) {
                    final int rowStart = y * width;
                    
                    for (int x = 0; x < width; x++) {
                        final int idx = rowStart + x;
                        final int src = srcPixels[idx];

                        // Extract ARGB components
                        final int a = (src >> 24) & 0xFF;
                        final int r = (src >> 16) & 0xFF;
                        final int g = (src >> 8) & 0xFF;
                        final int b = src & 0xFF;

                        // Find nearest palette color in RGB space
                        final int nearestIdx = tree.findNearest(r, g, b);
                        final int nearestColor = tree.palette[nearestIdx];

                        // Preserve original alpha, apply nearest RGB
                        outPixels[idx] = (a << 24) | (nearestColor & 0x00FFFFFF);
                    }
                }
            });
        }

        // Shutdown executor and wait for tasks to finish.
        executor.shutdown();
        
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Write back computed pixels into the output image.
        out.setRGB(0, 0, width, height, outPixels, 0, width);
        
        return out;
    }
}