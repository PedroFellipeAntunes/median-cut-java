package MedianCut.ConverterPlugins;

import Data.Pixel;

// Interface for color space converters.
public interface ColorSpacePlugin {
    Pixel fromRgba(int rgba);
    
    int toRgba(Pixel p);
}