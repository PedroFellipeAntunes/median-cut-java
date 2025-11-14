package MedianCut.ConverterPlugins;

import Data.Pixel;

/**
 * HSL color space converter (robust).
 */
public class HslPlugin implements ColorSpacePlugin {
    public static final String ID = "HSL";

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    @Override
    public Pixel fromRgba(int rgba) {
        int r = (rgba >> 16) & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = rgba & 0xFF;

        double rn = r / 255.0;
        double gn = g / 255.0;
        double bn = b / 255.0;

        double max = Math.max(rn, Math.max(gn, bn));
        double min = Math.min(rn, Math.min(gn, bn));
        double delta = max - min;

        double h;
        double s;
        double l = (max + min) / 2.0;

        if (delta > 0.0) {
            // compute hPrime (sector index, not modulo)
            double hPrime;
            if (max == rn) {
                hPrime = (gn - bn) / delta;
            } else if (max == gn) {
                hPrime = (bn - rn) / delta + 2.0;
            } else {
                hPrime = (rn - gn) / delta + 4.0;
            }

            double hDeg = hPrime * 60.0;
            // Normalize degrees into [0,360)
            hDeg = ((hDeg % 360.0) + 360.0) % 360.0;
            h = hDeg / 360.0;

            // canonical HSL saturation
            double denom = 1.0 - Math.abs(2.0 * l - 1.0);
            if (denom <= 0.0) s = 0.0;
            else s = delta / denom;
        } else {
            h = 0.0;
            s = 0.0;
        }

        h = clamp(h, 0.0, 1.0);
        s = clamp(s, 0.0, 1.0);
        l = clamp(l, 0.0, 1.0);

        return new Pixel(h, s, l);
    }

    @Override
    public int toRgba(Pixel p) {
        double h = p.values[0];
        double s = p.values[1];
        double l = p.values[2];

        // degrees in [0,360)
        double hDeg = ((h * 360.0) % 360.0 + 360.0) % 360.0;

        double c = (1.0 - Math.abs(2.0 * l - 1.0)) * s;
        double hSection = hDeg / 60.0;
        double x = c * (1.0 - Math.abs((hSection % 2.0) - 1.0));
        double m = l - c / 2.0;

        double r1, g1, b1;

        if (hSection < 1.0) {
            r1 = c; g1 = x; b1 = 0.0;
        } else if (hSection < 2.0) {
            r1 = x; g1 = c; b1 = 0.0;
        } else if (hSection < 3.0) {
            r1 = 0.0; g1 = c; b1 = x;
        } else if (hSection < 4.0) {
            r1 = 0.0; g1 = x; b1 = c;
        } else if (hSection < 5.0) {
            r1 = x; g1 = 0.0; b1 = c;
        } else {
            r1 = c; g1 = 0.0; b1 = x;
        }

        double rd = clamp(r1 + m, 0.0, 1.0);
        double gd = clamp(g1 + m, 0.0, 1.0);
        double bd = clamp(b1 + m, 0.0, 1.0);

        int ri = (int) Math.round(rd * 255.0);
        int gi = (int) Math.round(gd * 255.0);
        int bi = (int) Math.round(bd * 255.0);

        ri = Math.max(0, Math.min(255, ri));
        gi = Math.max(0, Math.min(255, gi));
        bi = Math.max(0, Math.min(255, bi));

        return (0xFF << 24) | (ri << 16) | (gi << 8) | bi;
    }
}