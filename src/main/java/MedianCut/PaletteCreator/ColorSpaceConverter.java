package MedianCut.PaletteCreator;

import Data.Models.OperationEnum;
import Data.Pixel;
import MedianCut.ConverterPlugins.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * Central color-space converter:
 *   Register available ColorSpacePlugin implementations.
 *   Convert a BufferedImage into a list of unique Pixels for a given color space.
 *   Convert a list of Pixels back to an expanded RGBA int[] for a given color space.
 */
public final class ColorSpaceConverter {
    private static final Map<String, ColorSpacePlugin> REGISTRY = new HashMap<>();

    static {
        // Register built-in converters
        register(new RgbPlugin());
        register(new HsbPlugin());
        register(new HslPlugin());
        register(new OkLabPlugin());

        // Grayscale reuses the RGB converter implementation
        REGISTRY.put("GRAYSCALE", REGISTRY.get("RGB"));
    }

    // Private helper to register a plugin by reading its public static ID field.
    // Throws a runtime exception if the plugin lacks the required ID field.
    private static void register(ColorSpacePlugin plugin) {
        try {
            String id = (String) plugin.getClass().getField("ID").get(null);
            REGISTRY.put(id.toUpperCase(Locale.ROOT), plugin);
        } catch (Exception e) {
            throw new RuntimeException("Plugin missing ID field: " + plugin.getClass().getName(), e);
        }
    }

    private ColorSpaceConverter() {}

    /**
     * Convert the input image into a list of unique Pixels for the requested
     * color space using multithreading.
     *
     * The returned list contains one Pixel instance per distinct color
     * (according to Pixel.equals/hashCode). Each Pixel.count is incremented to
     * reflect how many source pixels had the same value.
     *
     * @param image source image (non-null)
     * @param space target color space enum (non-null)
     * @return list of unique Pixel instances (order not guaranteed)
     * @throws IllegalArgumentException if image or space is null or no plugin
     * is available
     */
    public static List<Pixel> extract(BufferedImage image, OperationEnum space) {
        // Validate parameters
        if (image == null || space == null) {
            throw new IllegalArgumentException("Image and OperationEnum cannot be null");
        }

        // Lookup converter plugin for the requested color space (e.g. RGB, HSL, OKLab)
        ColorSpacePlugin plugin = REGISTRY.get(space.name());

        if (plugin == null) {
            throw new IllegalArgumentException("No converter for " + space);
        }

        int w = image.getWidth();
        int h = image.getHeight();

        // Thread-safe map used to deduplicate pixels; Pixel.equals/hashCode determine uniqueness
        ConcurrentMap<Pixel, Pixel> uniquePixels = new ConcurrentHashMap<>();

        // Iterate over every row in parallel
        ForkJoinPool.commonPool().submit(()
                -> IntStream.range(0, h).parallel().forEach(y -> {
                    for (int x = 0; x < w; x++) {
                        // Convert RGBA integer to Pixel (color-space representation)
                        Pixel p = plugin.fromRgba(image.getRGB(x, y));

                        // Atomically check if this color already exists and update count
                        uniquePixels.merge(p, p, (existing, newPixel) -> {
                            existing.count++; // same color, increment count
                            return existing;
                        });
                    }
                })
        ).join();

        // Return a list containing one Pixel per unique color (count preserved)
        return new ArrayList<>(uniquePixels.values());
    }

    /**
     * Expand the provided list of Pixels into an RGBA int[] using the specified color space plugin.
     *
     * The resulting array length equals the sum of all Pixel.count values. The order of pixels in the
     * output follows the iteration order of the input list (each Pixel repeated count times).
     *
     * @param pixels list of pixels to convert and expand (non-null)
     * @param space color space to use for conversion (non-null)
     * @return expanded array of packed RGBA integers
     * @throws IllegalArgumentException if pixels or space is null or no plugin is available
     */
    public static int[] toRgba(List<Pixel> pixels, OperationEnum space) {
        // Validate parameters
        if (pixels == null || space == null) {
            throw new IllegalArgumentException("Pixels and OperationEnum cannot be null");
        }

        // Lookup converter plugin for the chosen color space
        ColorSpacePlugin plugin = REGISTRY.get(space.name());
        
        if (plugin == null) {
            throw new IllegalArgumentException("No converter for " + space);
        }

        // Calculate total number of pixels after expanding counts
        int totalPixels = pixels.stream().mapToInt(p -> p.count).sum();

        int[] rgba = new int[totalPixels];
        int idx = 0;

        // Convert each Pixel back to RGBA and repeat it according to its count
        for (Pixel p : pixels) {
            for (int i = 0; i < p.count; i++) {
                rgba[idx++] = plugin.toRgba(p);
            }
        }

        return rgba;
    }
}