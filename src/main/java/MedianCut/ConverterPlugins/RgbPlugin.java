package MedianCut.ConverterPlugins;

import Data.Pixel;
import java.awt.Color;

// RGB color space converter
public class RgbPlugin implements ColorSpacePlugin {
    public static final String ID = "RGB";

    @Override
    public Pixel fromRgba(int rgba) {
        Color c = new Color(rgba, true);
        
        double r = c.getRed() / 255.0;
        double g = c.getGreen() / 255.0;
        double b = c.getBlue() / 255.0;
        
        return new Pixel(r, g, b);
    }

    @Override
    public int toRgba(Pixel p) {
        int r = (int) Math.round(p.values[0] * 255.0);
        int g = (int) Math.round(p.values[1] * 255.0);
        int b = (int) Math.round(p.values[2] * 255.0);
        
        return new Color(r, g, b).getRGB();
    }
}