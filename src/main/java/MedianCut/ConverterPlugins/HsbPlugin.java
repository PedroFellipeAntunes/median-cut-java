package MedianCut.ConverterPlugins;

import Data.Pixel;
import java.awt.Color;

// HSB color space converter
public class HsbPlugin implements ColorSpacePlugin {
    public static final String ID = "HSB";

    @Override
    public Pixel fromRgba(int rgba) {
        int r = (rgba >> 16) & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = rgba & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        
        return new Pixel(hsb[0], hsb[1], hsb[2]);
    }

    @Override
    public int toRgba(Pixel p) {
        float h = (float) p.values[0];
        float s = (float) p.values[1];
        float b = (float) p.values[2];
        
        return Color.HSBtoRGB(h, s, b);
    }
}