package MedianCut.ConverterPlugins;

import Data.Pixel;
import java.awt.Color;

/**
 * OkLab color space converter with iterative gamut mapping.
 * Tries to map OKLab -> RGB in steps to stay within gamut.
 */
public class OkLabPlugin implements ColorSpacePlugin {
    public static final String ID = "OKLAB";

    private static final int MAX_ITER = 20; // max steps
    private static final double STEP = 0.05; // decrement per iteration

    @Override
    public Pixel fromRgba(int rgba) {
        Color c = new Color(rgba, true);
        
        double r = c.getRed() / 255.0;
        double g = c.getGreen() / 255.0;
        double b = c.getBlue() / 255.0;

        // linearize sRGB
        r = linearize(r);
        g = linearize(g);
        b = linearize(b);

        // sRGB -> LMS
        double l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b;
        double m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b;
        double s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b;

        // cube root
        double l_ = Math.cbrt(l);
        double m_ = Math.cbrt(m);
        double s_ = Math.cbrt(s);

        // LMS -> OKLab
        double L = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_;
        double a = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_;
        double b_ = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_;

        return new Pixel(L, a, b_);
    }

    @Override
    public int toRgba(Pixel p) {
        double L = p.values[0];
        double a = p.values[1];
        double b_ = p.values[2];

        double scale = 1.0;
        double r = 0, g = 0, b = 0;
        
        for (int i = 0; i < MAX_ITER; i++) {
            // scaled OKLab -> LMS cube root
            double l_ = L + scale * (0.3963377774 * a + 0.2158037573 * b_);
            double m_ = L - scale * (0.1055613458 * a + 0.0638541728 * b_);
            double s_ = L - scale * (0.0894841775 * a + 1.2914855480 * b_);

            // cube
            l_ = l_ * l_ * l_;
            m_ = m_ * m_ * m_;
            s_ = s_ * s_ * s_;

            // LMS -> linear sRGB
            r = +4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_;
            g = -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_;
            b = -0.0041960863 * l_ - 0.7034186147 * m_ + 1.7076147010 * s_;

            // check if all channels are in gamut
            if (r >= 0 && r <= 1 && g >= 0 && g <= 1 && b >= 0 && b <= 1) {
                break;
            }

            scale -= STEP;
            
            if (scale < 0) {
                scale = 0;
            }
        }

        // gamma and clamp if needed
        r = delinearize(r);
        g = delinearize(g);
        b = delinearize(b);

        int ri = clamp(r);
        int gi = clamp(g);
        int bi = clamp(b);

        return new Color(ri, gi, bi).getRGB();
    }

    private static double linearize(double c) {
        if (c <= 0.04045) {
            return c / 12.92;
        } else {
            return Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    private static double delinearize(double c) {
        if (c <= 0.0031308) {
            return 12.92 * c;
        } else {
            return 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
        }
    }

    private static int clamp(double c) {
        int v = (int) Math.round(c * 255.0);
        
        if (v < 0) {
            return 0;
        }
        
        if (v > 255) {
            return 255;
        }
        
        return v;
    }
}