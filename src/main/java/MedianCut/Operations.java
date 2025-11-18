package MedianCut;

import MedianCut.PaletteCreator.PaletteToImage;
import MedianCut.PaletteCreator.PixelSorter;
import MedianCut.PaletteCreator.MedianCut;
import MedianCut.PaletteCreator.ColorSpaceConverter;
import Data.ConfigData;
import Data.KdTreeRGB;
import Data.Models.OperationEnum;
import Data.Pixel;
import FileManager.PngReader;
import FileManager.PngSaver;
import MedianCut.PaletteCreator.PaletteImageCreator;
import Windows.ImageViewer;

import static Util.Timing.measure;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the Median Cut pipeline for a single image file.
 *
 * Responsibilities:
 * - Validate configuration.
 * - Read image from disk.
 * - Convert image to a color space and produce a unique-pixel list.
 * - Sort pixels and run Median Cut quantization.
 * - Convert the resulting palette back to RGBA and apply it to the image.
 * - Optionally save and/or display the result.
 */
public class Operations {
    private final ConfigData config;

    // Flags controllable by UI; package-visible for ImageViewer to toggle.
    public boolean skip = false; // when true, skip displaying the result
    public boolean save = true;  // when true, automatically save the final image

    /**
     * Construct operations with the provided configuration.
     *
     * @param config configuration describing operation, order and bucket count
     * @throws IllegalArgumentException when the configuration is invalid
     */
    public Operations(ConfigData config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");

        // For non-grayscale operations, ensure the order is not all NONE.
        if (config.operation != OperationEnum.GRAYSCALE) {
            int noneCount = 0;
            
            for (Enum<?> orderValue : config.order) {
                if ("NONE".equals(orderValue.toString())) {
                    noneCount++;
                }
            }
            
            if (noneCount >= config.order.length) {
                throw new IllegalArgumentException("Operation order cannot be all NONE.");
            }
        }
    }

    /**
     * Execute the full Median Cut pipeline for the given PNG file.
     *
     * Steps:
     * 1) Read image
     * 2) Convert to color space
     * 3) Sort pixels
     * 4) Apply Median Cut quantization
     * 5) Convert palette back to RGBA
     * 6) Build KD-tree for fast nearest lookup
     * 7) Apply palette to original image
     * 8) Optionally save and/or display the final image
     *
     * @param filePath path to the PNG file to process
     * @throws IOException when reading or saving the file fails
     */
    public void startProcess(String filePath) throws IOException {
        // 1) Read the original image from disk
        final BufferedImage original = measure("Reading image", () -> readImage(filePath));

        // 2) Convert to color space and extract unique pixels
        List<Pixel> convertedPixels = measure("Converting to Color Space " + config.operation, () -> ColorSpaceConverter.extract(original, config.operation));

        // 3) Sort pixels according to chosen order
        measure("Sorting Colors", () -> {
            PixelSorter.sort(convertedPixels, config.operation, config.order);
            return null;
        });
        
        // 4) Median Cut quantization
        List<Pixel> palette = measure("Applying Median Cut for " + config.buckets + " buckets", () -> MedianCut.quantize(convertedPixels, config.buckets, config.operation));

        // 5) Convert palette back to RGBA integers
        int[] convertedPalette = measure("Converting back from Color Space " + config.operation, () -> ColorSpaceConverter.toRgba(palette, config.operation));
        
        // 6) Build KD-tree from palette for fast nearest-color lookup
        KdTreeRGB kdt = measure("Building KD-Tree", () -> new KdTreeRGB(convertedPalette));

        // 7) Apply palette to the original image
        BufferedImage finalImage = measure("Applying Palette to Image", () -> PaletteToImage.apply(original, kdt));

        // 8) If skip flag is active, optionally save and exit without displaying
        if (skip) {
            System.out.println("- Display skip");
            
            if (save) {
//                testPalette(convertedPalette, filePath);
                saveImage(finalImage, filePath);
            }
            
            return;
        }
        
//        testPalette(convertedPalette, filePath);

        // 9) Display the final image in the viewer
        System.out.println("- Displaying result");
        new ImageViewer(finalImage, filePath, this);
        System.out.println("FINISHED PROCESS\n");
    }
    
    private void testPalette(int[] rgba, String filePath) {
        BufferedImage testPaletteAfter = measure("Creating palette image", () -> {
            return PaletteImageCreator.createPalette(rgba);
        });
        
        new ImageViewer(testPaletteAfter, filePath, this);
    }

    /**
     * Save the processed image to disk using a descriptive filename prefix.
     *
     * @param image image to save
     * @param filePath original source file path used to derive output location
     */
    public void saveImage(BufferedImage image, String filePath) {
        PngSaver saver = new PngSaver();
        final String prefix;

        if (config.operation == OperationEnum.GRAYSCALE) {
            prefix = String.format("MedianCut[%s;%d]", config.operation, config.buckets);
        } else {
            prefix = String.format(
                "MedianCut[%s;%s;%s;%s;%d]",
                config.operation,
                String.valueOf(config.order[0]),
                String.valueOf(config.order[1]),
                String.valueOf(config.order[2]),
                config.buckets
            );
        }

        saver.saveToFile(prefix, filePath, image);
    }

    // Reads a PNG file and returns it as a BufferedImage.
    private BufferedImage readImage(String path) {
        return new PngReader().readPNG(path, false);
    }
}