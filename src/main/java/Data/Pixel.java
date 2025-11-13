package Data;

import java.util.Objects;

/**
 * Represents a pixel with exactly 3 normalized channels in [0,1].
 * Each pixel also stores a reference count (number of occurrences).
 */
public final class Pixel {
    public final double[] values; // RGB or LAB components, length = 3
    public int count; // number of pixels represented by this instance

    /**
     * Construct a pixel from three double channel values.
     *
     * @param a first channel value
     * @param b second channel value
     * @param c third channel value
     */
    public Pixel(double a, double b, double c) {
        this.values = new double[]{a, b, c};
        this.count = 1;
    }

    /**
     * Construct a pixel from an array of three values.
     *
     * @param values array containing three normalized channel values
     * @throws IllegalArgumentException if values is null or length is not 3
     */
    public Pixel(double[] values) {
        if (values == null || values.length != 3) {
            throw new IllegalArgumentException("Pixel must have exactly 3 channels.");
        }
        
        this.values = values.clone();
        this.count = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        
        if (!(o instanceof Pixel)) return false;
        
        Pixel pixel = (Pixel) o;
        
        // equality based on channel values only (not count)
        return Double.compare(pixel.values[0], values[0]) == 0 &&
               Double.compare(pixel.values[1], values[1]) == 0 &&
               Double.compare(pixel.values[2], values[2]) == 0;
    }

    @Override
    public int hashCode() {
        // hash is required for using Pixel as a HashMap key (uniqueness detection in color extraction)
        // use channel values only for hash consistency with equals
        return Objects.hash(values[0], values[1], values[2]);
    }
}