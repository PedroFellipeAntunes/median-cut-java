package MedianCut.ConverterPlugins;

import Data.Pixel;

// HSL color space converter
public class HslPlugin implements ColorSpacePlugin {
    public static final String ID = "HSL";

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

        double h = 0.0;
        double s = 0.0;
        double l = (max + min) / 2.0;

        if (delta != 0.0) {
            s = (l < 0.5) ? (delta / (max + min)) : (delta / (2.0 - max - min));
            
            if (max == rn) {
                h = ((gn - bn) / delta) % 6.0;
            } else if (max == gn) {
                h = ((bn - rn) / delta) + 2.0;
            } else {
                h = ((rn - gn) / delta) + 4.0;
            }
            
            h = h * 60.0;
            
            if (h < 0) {
                h += 360.0;
            }
        }
        
        return new Pixel(h / 360.0, s, l);
    }

    @Override
    public int toRgba(Pixel p) {
        double h = p.values[0] * 360.0;
        double s = p.values[1];
        double l = p.values[2];

        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = l - c / 2.0;

        double r = 0, g = 0, b = 0;
        
        if (h < 60) {
            r = c;
            g = x;
            b = 0;
        } else if (h < 120) {
            r = x;
            g = c;
            b = 0;
        } else if (h < 180) {
            r = 0;
            g = c;
            b = x;
        } else if (h < 240) {
            r = 0;
            g = x;
            b = c;
        } else if (h < 300) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }

        int ri = (int) Math.round((r + m) * 255.0);
        int gi = (int) Math.round((g + m) * 255.0);
        int bi = (int) Math.round((b + m) * 255.0);
        
        return (0xFF << 24) | (ri << 16) | (gi << 8) | bi;
    }
}