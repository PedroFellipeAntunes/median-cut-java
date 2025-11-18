package MedianCut.ConverterPlugins;

import Data.Pixel;
import java.awt.Color;

/**
 * OkLab color space converter with normalization.
 *
 * Responsibilities:
 * - fromRgba: RGBA 0–255 → OKLab normalized [0,1]
 * - toRgba: OKLab normalized [0,1] → RGBA 0–255
 */
public class OkLabPlugin implements ColorSpacePlugin {
    public static final String ID = "OKLAB";

    private static final int MAX_ITER = 20; // max steps for gamut mapping
    private static final double STEP = 0.05; // decrement per iteration

    // Reference range for normalization
    private static final double A_RANGE = 0.4; // approximate max absolute a in OKLab
    private static final double B_RANGE = 0.4; // approximate max absolute b in OKLab

    @Override
    public Pixel fromRgba(int rgba) {
        Color c = new Color(rgba, true);

        double r = c.getRed() / 255.0;
        double g = c.getGreen() / 255.0;
        double b = c.getBlue() / 255.0;

        // Linearize sRGB
        r = linearize(r);
        g = linearize(g);
        b = linearize(b);

        // sRGB -> LMS
        double l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b;
        double m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b;
        double s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b;

        // Cube root
        double l_ = Math.cbrt(l);
        double m_ = Math.cbrt(m);
        double s_ = Math.cbrt(s);

        // LMS -> OKLab
        double L = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_;
        double a = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_;
        double b_ = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_;

        // Normalize to [0,1] for aggregation
        double Ln = L; // L is already 0..1
        double an = (a / (2.0 * A_RANGE)) + 0.5; // map [-A_RANGE, A_RANGE] → [0,1]
        double bn = (b_ / (2.0 * B_RANGE)) + 0.5; // map [-B_RANGE, B_RANGE] → [0,1]

        return new Pixel(Ln, an, bn);
    }

    @Override
    public int toRgba(Pixel p) {
        // Desnormalize OKLab
        double L = p.values[0];
        double a = (p.values[1] - 0.5) * 2.0 * A_RANGE;
        double b_ = (p.values[2] - 0.5) * 2.0 * B_RANGE;

        double r = 0, g = 0, b = 0;
        double scale = 1.0;

        // Iterative gamut mapping
        for (int i = 0; i < MAX_ITER; i++) {
            // OKLab -> LMS
            double l_ = L + 0.3963377774 * a + 0.2158037573 * b_;
            double m_ = L - 0.1055613458 * a - 0.0638541728 * b_;
            double s_ = L - 0.0894841775 * a - 1.2914855480 * b_;

            // Apply scale for gamut mapping
            l_ *= scale;
            m_ *= scale;
            s_ *= scale;

            // Cube to undo cube root
            l_ = l_ * l_ * l_;
            m_ = m_ * m_ * m_;
            s_ = s_ * s_ * s_;

            // LMS -> linear sRGB
            r = +4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_;
            g = -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_;
            b = -0.0041960863 * l_ - 0.7034186147 * m_ + 1.7076147010 * s_;

            // Stop if in gamut
            if (r >= 0 && r <= 1 && g >= 0 && g <= 1 && b >= 0 && b <= 1) break;

            scale -= STEP;
            
            if (scale < 0) scale = 0;
        }

        // Linear RGB -> sRGB gamma
        r = delinearize(r);
        g = delinearize(g);
        b = delinearize(b);

        return new Color(clamp(r), clamp(g), clamp(b)).getRGB();
    }

    private static double linearize(double c) {
        if (c <= 0.04045) return c / 12.92;
        
        return Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double delinearize(double c) {
        if (c <= 0.0031308) return 12.92 * c;
        
        return 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
    }

    private static int clamp(double c) {
        int v = (int) Math.round(c * 255.0);
        
        if (v < 0) return 0;
        
        if (v > 255) return 255;
        
        return v;
    }
}