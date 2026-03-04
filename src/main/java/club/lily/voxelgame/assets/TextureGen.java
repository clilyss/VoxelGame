package club.lily.voxelgame.assets;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

public class TextureGen {

    public static final int TILE = 16;
    public static final int COLS = 16;
    public static final int ROWS = 16;
    public static final int SIZE = TILE * COLS;

    public static final int T_GRASS_TOP  = 0;
    public static final int T_GRASS_SIDE = 1;
    public static final int T_DIRT       = 2;
    public static final int T_STONE      = 3;
    public static final int T_BEDROCK    = 4;
    public static final int T_SAND       = 5;
    public static final int T_SNOW       = 6;
    public static final int T_WOOD_TOP   = 7;
    public static final int T_WOOD_SIDE  = 8;
    public static final int T_LEAVES     = 9;

    public static BufferedImage generate() {
        BufferedImage atlas = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

        drawTile(atlas, T_GRASS_TOP,  TextureGen::grassTop);
        drawTile(atlas, T_GRASS_SIDE, TextureGen::grassSide);
        drawTile(atlas, T_DIRT,       TextureGen::dirt);
        drawTile(atlas, T_STONE,      TextureGen::stone);
        drawTile(atlas, T_BEDROCK,    TextureGen::bedrock);
        drawTile(atlas, T_SAND,       TextureGen::sand);
        drawTile(atlas, T_SNOW,       TextureGen::snow);
        drawTile(atlas, T_WOOD_TOP,   TextureGen::woodTop);
        drawTile(atlas, T_WOOD_SIDE,  TextureGen::woodSide);
        drawTile(atlas, T_LEAVES,     TextureGen::leaves);

        return atlas;
    }

    public static void saveAtlas(BufferedImage atlas, String path) {
        try {
            File f = new File(path);
            f.getParentFile().mkdirs();
            ImageIO.write(atlas, "PNG", f);
        } catch (IOException e) {
            System.err.println("[TextureGen] " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface TilePainter { void paint(int[] px, Random rng); }

    private static void drawTile(BufferedImage atlas, int col, TilePainter painter) {
        int[] px = new int[TILE * TILE];
        painter.paint(px, new Random(col * 31337L));
        atlas.setRGB(col * TILE, 0, TILE, TILE, px, 0, TILE);
    }

    private static int rgb(int r, int g, int b) {
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int vary(Random rng, int base, int amt) {
        return Math.max(0, Math.min(255, base + rng.nextInt(amt * 2 + 1) - amt));
    }

    private static int vc(Random rng, int r, int g, int b, int amt) {
        return rgb(vary(rng, r, amt), vary(rng, g, amt), vary(rng, b, amt));
    }

    private static void grassTop(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(12);
            if (t < 5)       px[i] = vc(rng, 71, 109, 37, 10);
            else if (t < 9)  px[i] = vc(rng, 82, 122, 42, 8);
            else if (t < 11) px[i] = vc(rng, 58,  92, 28, 8);
            else             px[i] = vc(rng, 91, 130, 50, 6);
        }
        for (int i = 0; i < 5; i++) {
            int ix = rng.nextInt(TILE), iy = rng.nextInt(TILE);
            px[iy * TILE + ix] = vc(rng, 55, 84, 22, 5);
        }
    }

    private static void grassSide(int[] px, Random rng) {
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int i = y * TILE + x;
                if (y == 0) {
                    px[i] = vc(rng, 71, 109, 37, 8);
                } else if (y == 1 && rng.nextInt(3) == 0) {
                    px[i] = vc(rng, 66, 100, 33, 6);
                } else {
                    px[i] = dirtPx(rng);
                }
            }
        }
    }

    private static void dirt(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) px[i] = dirtPx(rng);
    }

    private static int dirtPx(Random rng) {
        int t = rng.nextInt(10);
        if (t < 5) return vc(rng, 112, 80,  46, 10);
        if (t < 8) return vc(rng,  96, 66,  36,  8);
        return            vc(rng, 128, 92,  56,  8);
    }

    private static void stone(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(8);
            if (t < 5)      px[i] = vc(rng, 108, 108, 112, 10);
            else if (t < 7) px[i] = vc(rng,  96,  96, 100,  8);
            else            px[i] = vc(rng, 120, 120, 124,  8);
        }
        stoneCrack(px, 3, 2, 9, 5, 96);
        stoneCrack(px, 11, 7, 5, 13, 96);
        stoneCrack(px, 1, 10, 7, 15, 96);
    }

    private static void stoneCrack(int[] px, int x1, int y1, int x2, int y2, int shade) {
        int dx = Math.abs(x2-x1), dy = Math.abs(y2-y1);
        int sx = x1<x2?1:-1, sy = y1<y2?1:-1, err = dx-dy, x=x1, y=y1;
        while (true) {
            if (x>=0&&x<TILE&&y>=0&&y<TILE) px[y*TILE+x] = rgb(shade, shade, shade+4);
            if (x==x2&&y==y2) break;
            int e2=2*err;
            if (e2>-dy){err-=dy;x+=sx;}
            if (e2<dx) {err+=dx;y+=sy;}
        }
    }

    private static void bedrock(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(6);
            if (t < 3)      px[i] = vc(rng, 32, 32, 32, 6);
            else if (t < 5) px[i] = vc(rng, 22, 22, 22, 4);
            else            px[i] = vc(rng, 44, 44, 44, 5);
        }
        for (int k = 0; k < 8; k++) {
            int bx=rng.nextInt(TILE-2), by=rng.nextInt(TILE-2);
            for (int dy=0;dy<2;dy++) for (int dx=0;dx<2;dx++)
                px[(by+dy)*TILE+(bx+dx)] = rgb(12,12,12);
        }
    }

    private static void sand(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(8);
            if (t < 5)      px[i] = vc(rng, 196, 178, 112, 10);
            else if (t < 7) px[i] = vc(rng, 180, 163,  98,  8);
            else            px[i] = vc(rng, 210, 192, 126,  8);
        }
        for (int k = 0; k < 8; k++) {
            int sx=rng.nextInt(TILE), sy=rng.nextInt(TILE);
            px[sy*TILE+sx] = vc(rng, 168, 152, 86, 6);
        }
    }

    private static void snow(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            if (rng.nextInt(25) == 0) px[i] = rgb(255,255,255);
            else px[i] = vc(rng, 220, 226, 234, 10);
        }
        for (int k = 0; k < 4; k++) {
            int sx=rng.nextInt(TILE), sy=rng.nextInt(TILE);
            px[sy*TILE+sx] = vc(rng, 200, 208, 218, 6);
        }
    }

    private static void woodTop(int[] px, Random rng) {
        int cx = TILE/2, cy = TILE/2;
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                double d = Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy));
                int ring = (int)(d * 1.5) % 3;
                if (ring == 0)      px[y*TILE+x] = vc(rng, 118, 88, 44, 8);
                else if (ring == 1) px[y*TILE+x] = vc(rng, 100, 72, 34, 8);
                else                px[y*TILE+x] = vc(rng,  84, 60, 26, 8);
            }
        }
    }

    private static void woodSide(int[] px, Random rng) {
        int[] shade = new int[TILE];
        for (int x = 0; x < TILE; x++) shade[x] = rng.nextInt(4);
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int s = shade[x];
                if (s == 0)      px[y*TILE+x] = vc(rng,  84, 62, 30, 6);
                else if (s == 1) px[y*TILE+x] = vc(rng,  96, 72, 36, 6);
                else if (s == 2) px[y*TILE+x] = vc(rng,  74, 54, 24, 6);
                else             px[y*TILE+x] = vc(rng, 104, 78, 42, 6);
            }
        }
        if (rng.nextBoolean()) {
            int kx=3+rng.nextInt(9), ky=3+rng.nextInt(9);
            px[ky*TILE+kx]     = rgb(52, 36, 16);
            px[(ky+1)*TILE+kx] = rgb(52, 36, 16);
            px[ky*TILE+kx+1]   = rgb(52, 36, 16);
        }
    }

    private static void leaves(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(12);
            if (t == 0)      px[i] = rgba(0, 0, 0, 0);
            else if (t < 6)  px[i] = vc(rng, 38, 86, 20, 12);
            else if (t < 9)  px[i] = vc(rng, 28, 70, 14,  8);
            else if (t < 11) px[i] = vc(rng, 50, 98, 28, 10);
            else             px[i] = vc(rng, 44, 92, 24,  8);
        }
    }

    public static void main(String[] args) {
        BufferedImage atlas = generate();
        saveAtlas(atlas, "assets/atlas.png");
    }
}
