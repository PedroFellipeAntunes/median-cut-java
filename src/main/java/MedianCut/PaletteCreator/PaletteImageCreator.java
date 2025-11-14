package MedianCut.PaletteCreator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Palette image generator:
 *   Create a square BufferedImage that visualizes an array of packed RGBA ints as a palette.
 *   The image size (in pixels) is derived from the array length and kept square by using
 *   ceil(sqrt(length)). Each palette entry maps to one "cell" in that square.
 */
public final class PaletteImageCreator {
    private PaletteImageCreator() {}

    /**
     * Create a palette image where each entry in the array is rendered as a single pixel.
     *
     * @param rgba packed ARGB/ RGBA integers (non-null)
     * @return BufferedImage visualizing the palette (square)
     * @throws IllegalArgumentException if rgba is null or empty
     */
    public static BufferedImage createPalette(int[] rgba) {
        return createPalette(rgba, 1);
    }

    /**
     * Create a palette image where each entry in the array is rendered as a pixelSize x pixelSize block.
     *
     * The final image will be square. Let n = rgba.length and s = ceil(sqrt(n)).
     * The returned image dimensions will be (s * pixelSize) x (s * pixelSize).
     *
     * @param rgba packed ARGB/ RGBA integers (non-null)
     * @param pixelSize size of the drawn pixel block (must be >= 1)
     * @return BufferedImage visualizing the palette (square)
     * @throws IllegalArgumentException if rgba is null, empty, or pixelSize < 1
     */
    public static BufferedImage createPalette(int[] rgba, int pixelSize) {
        // Validate parameters
        Objects.requireNonNull(rgba, "rgba array cannot be null");
        
        if (rgba.length == 0) {
            throw new IllegalArgumentException("rgba array cannot be empty");
        }
        
        if (pixelSize < 1) {
            throw new IllegalArgumentException("pixelSize must be >= 1");
        }

        // Determine square dimension (number of cells per row/column)
        int cellsPerSide = (int) Math.ceil(Math.sqrt(rgba.length));

        // Compute image dimensions in pixels
        int width = cellsPerSide * pixelSize;
        int height = cellsPerSide * pixelSize;

        // Create ARGB image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Acquire Graphics2D for block drawing
        Graphics2D g = image.createGraphics();
        try {
            // Iterate over palette entries and draw each as a block of size pixelSize x pixelSize
            for (int i = 0; i < rgba.length; i++) {
                int cellX = i % cellsPerSide;
                int cellY = i / cellsPerSide;

                int px = cellX * pixelSize;
                int py = cellY * pixelSize;

                // Create Color preserving alpha (true)
                Color color = new Color(rgba[i], true);

                // Set paint and fill rectangle for the cell
                g.setColor(color);
                g.fillRect(px, py, pixelSize, pixelSize);
            }
        } finally {
            // Always dispose Graphics2D to free resources
            g.dispose();
        }

        return image;
    }
}