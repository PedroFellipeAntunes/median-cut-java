package Data;

import java.util.Arrays;
import java.util.Comparator;

/**
 * KD-Tree optimized for RGB palette nearest-neighbor lookup.
 * The tree is built once from an ARGB palette (int[]), and supports
 * searching for the index of the nearest palette color to a given (r,g,b).
 */
public class KdTreeRGB {
    private final Node root;
    public final int[] palette; // reference to the original palette array

    // Internal KD node storing an index into the palette and split axis.
    private static class Node {
        final int index; // index of color in palette[]
        final int axis; // 0 = R, 1 = G, 2 = B
        Node left;
        Node right;

        Node(int index, int axis) {
            this.index = index;
            this.axis = axis;
        }
    }

    /**
     * Construct a KD-tree from a non-empty ARGB palette.
     *
     * @param palette array of ARGB colors (0xAARRGGBB or 0xRRGGBB)
     * @throws IllegalArgumentException if palette is null or empty
     */
    public KdTreeRGB(int[] palette) {
        if (palette == null || palette.length == 0) {
            throw new IllegalArgumentException("palette cannot be null or empty");
        }
        
        this.palette = palette;
        Integer[] indices = new Integer[palette.length];
        
        for (int i = 0; i < palette.length; i++) indices[i] = i;
        
        this.root = build(indices, 0);
    }

    // Recursively build the KD-tree from an array of palette indices.
    private Node build(Integer[] indices, int depth) {
        if (indices.length == 0) return null;

        int axis = depth % 3; // cycle through R, G, B

        Arrays.sort(indices, Comparator.comparingInt(i -> getChannel(palette[i], axis)));

        int mid = indices.length / 2;
        Node node = new Node(indices[mid], axis);

        node.left = build(Arrays.copyOfRange(indices, 0, mid), depth + 1);
        node.right = build(Arrays.copyOfRange(indices, mid + 1, indices.length), depth + 1);

        return node;
    }

    // Extract the specified color channel from an ARGB integer.
    private static int getChannel(int argb, int axis) {
        return switch (axis) {
            case 0 -> (argb >> 16) & 0xFF; // R
            case 1 -> (argb >> 8) & 0xFF;  // G
            case 2 -> argb & 0xFF;         // B
            default -> 0;
        };
    }

    /**
     * Find the index in the palette array of the color nearest to the given (r,g,b).
     *
     * @param r red component 0-255
     * @param g green component 0-255
     * @param b blue component 0-255
     * @return index into palette[] of the nearest color
     */
    public int findNearest(int r, int g, int b) {
        // root is guaranteed non-null because palette length > 0
        return nearest(root, r, g, b, root.index, distanceSq(r, g, b, root.index));
    }

    // Recursive nearest-neighbor search in the KD-tree.
    private int nearest(Node node, int r, int g, int b, int bestIdx, long bestDist) {
        if (node == null) return bestIdx;

        int nodeIdx = node.index;
        long d = distanceSq(r, g, b, nodeIdx);
        
        if (d < bestDist) {
            bestDist = d;
            bestIdx = nodeIdx;
        }

        int val = switch (node.axis) {
            case 0 -> r;
            case 1 -> g;
            case 2 -> b;
            default -> 0;
        };

        int nodeVal = getChannel(palette[nodeIdx], node.axis);

        Node first = val < nodeVal ? node.left : node.right;
        Node second = val < nodeVal ? node.right : node.left;

        bestIdx = nearest(first, r, g, b, bestIdx, bestDist);
        long bestDistAfter = distanceSq(r, g, b, bestIdx);

        long diff = val - nodeVal;
        
        if (diff * diff < bestDistAfter) {
            bestIdx = nearest(second, r, g, b, bestIdx, bestDistAfter);
        }

        return bestIdx;
    }

    // Compute squared Euclidean distance between the target color and the palette entry at idx.
    private long distanceSq(int r, int g, int b, int idx) {
        int pr = (palette[idx] >> 16) & 0xFF;
        int pg = (palette[idx] >> 8) & 0xFF;
        int pb = palette[idx] & 0xFF;
        
        long dr = r - pr;
        long dg = g - pg;
        long db = b - pb;
        
        return dr * dr + dg * dg + db * db;
    }
}