# Median Cut

This project is a **Java Maven application** built using **NetBeans IDE** that performs **color quantization using the Median Cut algorithm**.
It supports multiple color spaces (RGB, HSL, HSB, OKLAB, and Grayscale), configurable channel sorting, and bucket control to generate optimized color palettes for images.

<p align="center">
  <table align="center">
    <tr>
      <td>
        <img src="images/examples/pexels-pixabay-161251.jpg" width="400" alt="Original Image">
      </td>
      <td>
        <img src="images/examples/pexels-pixabay-161251_MedianCut%5BGRAYSCALE;4%5D.png" width="400" alt="GRAYSCALE Quantized 4">
      </td>
      <td>
        <img src="images/examples/pexels-pixabay-161251_MedianCut%5BGRAYSCALE;64%5D.png" width="400" alt="GRAYSCALE Quantized 16">
      </td>
    </tr>
  </table>
</p>

---

## Table of Contents

1. [Features](#features)
2. [Usage](#usage)
3. [How It Works](#how-it-works)
4. [Color Spaces Supported](#color-spaces-supported)
5. [Additional Examples](#additional-examples)

---

## Features

<ul>
  <li><b>Median Cut Quantization</b>
    <ul>
      <li>Reduces an image to a limited color palette by recursively subdividing the color space.</li>
      <li>Supports an arbitrary number of <b>buckets (palette size)</b>.</li>
    </ul>
  </li>
  <li><b>Multi-Color-Space Conversion</b>
    <ul>
      <li>RGB, HSL, HSB, OKLAB, and Grayscale conversion support via plugin architecture.</li>
    </ul>
  </li>
  <li><b>Channel Sorting</b>
    <ul>
      <li>Custom ordering of channels before quantization for fine control (e.g., prioritize Lightness or Brightness).</li>
    </ul>
  </li>
  <li><b>KD-Tree Palette Mapping</b>
    <ul>
      <li>Efficient color mapping using a KD-Tree nearest-neighbor search.</li>
    </ul>
  </li>
  <li><b>Parallelized Image Reconstruction</b>
    <ul>
      <li>Multi-threaded pixel mapping for fast processing.</li>
    </ul>
  </li>
  <li><b>Automatic File Naming and Saving</b>
    <ul>
      <li>Output images are automatically named and saved with the current configuration.</li>
    </ul>
  </li>
  <li><b>Optional Display</b>
    <ul>
      <li>Configurable flags to display or skip preview (<code>skip</code> / <code>save</code>).</li>
    </ul>
  </li>
</ul>

---

## Usage

<ol>
  <li><b>Build or Run the Application</b>
    <ul>
      <li>Run and execute:</li>
    </ul>

```bash
java -jar MedianCut.jar
```

  </li>

  <li><b>Configure the Process</b>
    <ul>
      <li>Define the color space and your desired order for sorting priority.</li>
      <li>The output will be automatically generated with a descriptive filename such as:</li>

```text
MedianCut[COLOR_SPACE;CHANNEL_1;CHANNEL_2;CHANNEL_3;N_BUCKETS]
```

  </li>

  <li><b>Flags</b>
    <ul>
      <li><code>Skip All</code> → skip all next display windows.</li>
      <li><code>Save</code> → save image to same folder as original.</li>
    </ul>
  </li>
</ol>

---

## How It Works

Below is a high-level description of the Median Cut pipeline:

<ol>
  <li><b>Color Space Conversion</b>
    <ul>
      <li>Converts the input image into the target color space (RGB, HSL, HSB, OKLAB, etc.) using modular plugins.</li>
    </ul>
  </li>
  <li><b>Unique Pixel Extraction</b>
    <ul>
      <li>Each pixel is normalized and stored as a unique 3-channel <code>Pixel</code> object with reference counting.</li>
    </ul>
  </li>
  <li><b>Pixel Sorting</b>
    <ul>
      <li>Pixels are sorted according to a configurable channel order (e.g., prioritize Lightness or Saturation).</li>
    </ul>
  </li>
  <li><b>Recursive Median Cut</b>
    <ul>
      <li>The pixel list is split recursively into “buckets” by the axis with the largest variance until the desired number of clusters is reached.</li>
    </ul>
  </li>
  <li><b>Palette Generation</b>
    <ul>
      <li>Each bucket’s average color forms a palette entry.</li>
    </ul>
  </li>
  <li><b>KD-Tree Construction</b>
    <ul>
      <li>A <code>KdTreeRGB</code> is built from the palette for efficient nearest-color search.</li>
    </ul>
  </li>
  <li><b>Image Reconstruction</b>
    <ul>
      <li>Each pixel in the original image is replaced by the nearest color from the palette (multi-threaded).</li>
    </ul>
  </li>
</ol>

---

## Color Spaces Supported

<p align="center">
  <table align="center">
    <tr>
      <td><b>Color Space</b></td>
      <td><b>Description</b></td>
    </tr>
    <tr>
      <td><b>RGB</b></td>
      <td>Red, Green, Blue</td>
    </tr>
    <tr>
      <td><b>HSL</b></td>
      <td>Hue, Saturation, Lightness</td>
    </tr>
    <tr>
      <td><b>HSB</b></td>
      <td>Hue, Saturation, Brightness</td>
    </tr>
    <tr>
      <td><b>OKLAB</b></td>
      <td>L, A, B</td>
    </tr>
    <tr>
      <td><b>Grayscale</b></td>
      <td>Sum of RGB values</td>
    </tr>
  </table>
</p>

---

## Additional Examples

<p align="center">
  <table align="center">
    <tr>
      <td><b>Input</b></td>
      <td><b>RGB Red Sorted (8 colors)</b></td>
      <td><b>HSL Lightness Sorted (8 colors)</b></td>
      <td><b>OKLAB L Sorted (8 colors)</b></td>
    </tr>
    <tr>
      <td><img src="images/examples/pexels-agk42-2816903.jpg" alt="Input"></td>
      <td><img src="images/examples/pexels-agk42-2816903_MedianCut%5BRGB;RED;NONE;NONE;8%5D.png" alt="RGB Quantized 8"></td>
      <td><img src="images/examples/pexels-agk42-2816903_MedianCut%5BHSL;LIGHTNESS;NONE;NONE;8%5D.png" alt="HSL Quantized 8"></td>
      <td><img src="images/examples/pexels-agk42-2816903_MedianCut%5BOKLAB;L;NONE;NONE;8%5D.png" alt="OKLAB Quantized 8"></td>
    </tr>
    <tr>
      <td><img src="images/examples/pexels-belle-co-99483-402028.jpg" alt="Input"></td>
      <td><img src="images/examples/pexels-belle-co-99483-402028_MedianCut%5BRGB;RED;NONE;NONE;8%5D.png" alt="RGB Quantized 8"></td>
      <td><img src="images/examples/pexels-belle-co-99483-402028_MedianCut%5BHSL;LIGHTNESS;NONE;NONE;8%5D.png" alt="HSL Quantized 8"></td>
      <td><img src="images/examples/pexels-belle-co-99483-402028_MedianCut%5BOKLAB;L;NONE;NONE;8%5D.png" alt="OKLAB Quantized 8"></td>
    </tr>
    <tr>
      <td><img src="images/examples/pexels-chevanon-1335971.jpg" alt="Input"></td>
      <td><img src="images/examples/pexels-chevanon-1335971_MedianCut%5BRGB;RED;NONE;NONE;8%5D.png" alt="RGB Quantized 8"></td>
      <td><img src="images/examples/pexels-chevanon-1335971_MedianCut%5BHSL;LIGHTNESS;NONE;NONE;8%5D.png" alt="HSL Quantized 8"></td>
      <td><img src="images/examples/pexels-chevanon-1335971_MedianCut%5BOKLAB;L;NONE;NONE;8%5D.png" alt="OKLAB Quantized 8"></td>
    </tr>
  </table>
</p>

---
