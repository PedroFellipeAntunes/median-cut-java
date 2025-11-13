package MedianCut;

import Data.ConfigData;
import Data.Models.*;
import Data.Pixel;
import MedianCut.PaletteCreator.ColorSpaceConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.List;

/**
 * Test runner that exercises the MedianCut pipeline for a set of test images.
 *
 * The class:
 * - Loads each test image.
 * - Extracts unique pixels (using RGB for uniqueness).
 * - Iterates a range of bucket counts and runs the pipeline for each configuration.
 */
public class OperationTest {
    private static final String[] TEST_IMAGES = {
        "/images/tests/HSB/Color Test 1000 HSB.png",
        "/images/tests/HSL/Color Test 1000 HSL.png"
    };

    private static int BUCKETS = 2;

    public static void main(String[] args) {
        for (String image : TEST_IMAGES) {
            try {
                // Load image from disk
                BufferedImage img = ImageIO.read(new File(image));

                // Extract unique pixels (use RGB conversion since uniqueness is color-space independent)
                List<Pixel> uniquePixels = ColorSpaceConverter.extract(img, OperationEnum.RGB);
                int uniqueCount = uniquePixels.size();
                System.out.println("Unique pixels in image: " + uniqueCount);

                // Step size = 10% of unique pixels, at least 1
                int step = Math.max(1, (int) Math.ceil(uniqueCount * 0.1));

                // Iterate through bucket sizes in steps (approx. 1% increments)
                for (int i = 1; i <= uniqueCount; i += step - 1) {
                    BUCKETS = i;

                    testGrayscale(image);
//                    testRgb(image);
//                    testHsl(image);
//                    testHsb(image);
//                    testOklab(image);
                }
            } catch (Exception e) {
                System.err.println("Failed to load or process " + image + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void testGrayscale(String path) {
        Enum<?>[] order = new Enum<?>[]{null, null, null};
        
        runTest(path, OperationEnum.GRAYSCALE, order);
    }

    private static void testRgb(String path) {
        Enum<?>[] order = new Enum<?>[]{
            RgbOrderEnum.RED,
            RgbOrderEnum.NONE,
            RgbOrderEnum.NONE
        };
        
        runTest(path, OperationEnum.RGB, order);
    }

    private static void testHsl(String path) {
        Enum<?>[] order = new Enum<?>[]{
            HslOrderEnum.LIGHTNESS,
            HslOrderEnum.NONE,
            HslOrderEnum.NONE
        };
        
        runTest(path, OperationEnum.HSL, order);
    }

    private static void testHsb(String path) {
        Enum<?>[] order = new Enum<?>[]{
            HsbOrderEnum.BRIGHTNESS,
            HsbOrderEnum.NONE,
            HsbOrderEnum.NONE
        };
        
        runTest(path, OperationEnum.HSB, order);
    }

    private static void testOklab(String path) {
        Enum<?>[] order = new Enum<?>[]{
            OkLabOrderEnum.L,
            OkLabOrderEnum.NONE,
            OkLabOrderEnum.NONE
        };
        
        runTest(path, OperationEnum.OKLAB, order);
    }

    private static void runTest(String path, OperationEnum operation, Enum<?>[] order) {
        System.out.println("\n=== Testing " + operation + " with " + BUCKETS + " buckets ===");

        try {
            ConfigData config = new ConfigData(operation, order, BUCKETS);
            Operations op = new Operations(config);
            op.save = true;
            op.skip = true;
            op.startProcess(path);

            System.out.println("✓ Success for " + operation);
        } catch (Exception ex) {
            System.err.println("✗ Error during " + operation + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}