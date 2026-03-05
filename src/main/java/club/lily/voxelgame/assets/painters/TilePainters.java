package club.lily.voxelgame.assets.painters;

import java.util.Random;

public final class TilePainters {

    public static final int TILE = 16;

    private TilePainters() {}

    public static void grassTop(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(12);
            if      (t < 5)  px[i] = vc(rng, 71, 109, 37, 10);
            else if (t < 9)  px[i] = vc(rng, 82, 122, 42,  8);
            else if (t < 11) px[i] = vc(rng, 58,  92, 28,  8);
            else             px[i] = vc(rng, 91, 130, 50,  6);
        }
        for (int i = 0; i < 5; i++) {
            int ix = rng.nextInt(TILE), iy = rng.nextInt(TILE);
            px[iy * TILE + ix] = vc(rng, 55, 84, 22, 5);
        }
    }

    public static void grassSide(int[] px, Random rng) {
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int i = y * TILE + x;
                if      (y == 0)                      px[i] = vc(rng, 71, 109, 37, 8);
                else if (y == 1 && rng.nextInt(3)==0) px[i] = vc(rng, 66, 100, 33, 6);
                else                                  px[i] = dirtPx(rng);
            }
        }
    }

    public static void dirt(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) px[i] = dirtPx(rng);
    }

    public static void stone(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(8);
            if      (t < 5) px[i] = vc(rng, 108, 108, 112, 10);
            else if (t < 7) px[i] = vc(rng,  96,  96, 100,  8);
            else            px[i] = vc(rng, 120, 120, 124,  8);
        }
        crack(px,  3,  2, 9,  5, 96);
        crack(px, 11,  7, 5, 13, 96);
        crack(px,  1, 10, 7, 15, 96);
    }

    public static void bedrock(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(6);
            if      (t < 3) px[i] = vc(rng, 32, 32, 32, 6);
            else if (t < 5) px[i] = vc(rng, 22, 22, 22, 4);
            else            px[i] = vc(rng, 44, 44, 44, 5);
        }
        for (int k = 0; k < 8; k++) {
            int bx = rng.nextInt(TILE-2), by = rng.nextInt(TILE-2);
            for (int dy = 0; dy < 2; dy++)
                for (int dx = 0; dx < 2; dx++)
                    px[(by+dy)*TILE+(bx+dx)] = rgb(12, 12, 12);
        }
    }

    public static void sand(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(8);
            if      (t < 5) px[i] = vc(rng, 196, 178, 112, 10);
            else if (t < 7) px[i] = vc(rng, 180, 163,  98,  8);
            else            px[i] = vc(rng, 210, 192, 126,  8);
        }
        for (int k = 0; k < 8; k++)
            px[rng.nextInt(TILE)*TILE + rng.nextInt(TILE)] = vc(rng, 168, 152, 86, 6);
    }

    public static void snow(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++)
            px[i] = rng.nextInt(25) == 0 ? rgb(255,255,255) : vc(rng, 220, 226, 234, 10);
        for (int k = 0; k < 4; k++)
            px[rng.nextInt(TILE)*TILE + rng.nextInt(TILE)] = vc(rng, 200, 208, 218, 6);
    }

    public static void woodTop(int[] px, Random rng) {
        int cx = TILE/2, cy = TILE/2;
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int ring = (int)(Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy)) * 1.5) % 3;
                if      (ring == 0) px[y*TILE+x] = vc(rng, 118, 88, 44, 8);
                else if (ring == 1) px[y*TILE+x] = vc(rng, 100, 72, 34, 8);
                else                px[y*TILE+x] = vc(rng,  84, 60, 26, 8);
            }
        }
    }

    public static void woodSide(int[] px, Random rng) {
        int[] shade = new int[TILE];
        for (int x = 0; x < TILE; x++) shade[x] = rng.nextInt(4);
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int s = shade[x];
                if      (s == 0) px[y*TILE+x] = vc(rng,  84, 62, 30, 6);
                else if (s == 1) px[y*TILE+x] = vc(rng,  96, 72, 36, 6);
                else if (s == 2) px[y*TILE+x] = vc(rng,  74, 54, 24, 6);
                else             px[y*TILE+x] = vc(rng, 104, 78, 42, 6);
            }
        }
        if (rng.nextBoolean()) {
            int kx = 3+rng.nextInt(9), ky = 3+rng.nextInt(9);
            px[ky*TILE+kx]     = rgb(52, 36, 16);
            px[(ky+1)*TILE+kx] = rgb(52, 36, 16);
            px[ky*TILE+kx+1]   = rgb(52, 36, 16);
        }
    }

    public static void leaves(int[] px, Random rng) {
        for (int i = 0; i < px.length; i++) {
            int t = rng.nextInt(12);
            if      (t == 0) px[i] = rgba(0, 0, 0, 0);
            else if (t < 6)  px[i] = vc(rng, 38, 86, 20, 12);
            else if (t < 9)  px[i] = vc(rng, 28, 70, 14,  8);
            else if (t < 11) px[i] = vc(rng, 50, 98, 28, 10);
            else             px[i] = vc(rng, 44, 92, 24,  8);
        }
    }

    private static int dirtPx(Random rng) {
        int t = rng.nextInt(10);
        if (t < 5) return vc(rng, 112, 80, 46, 10);
        if (t < 8) return vc(rng,  96, 66, 36,  8);
        return           vc(rng, 128, 92, 56,  8);
    }

    private static void crack(int[] px, int x1, int y1, int x2, int y2, int shade) {
        int dx = Math.abs(x2-x1), dy = Math.abs(y2-y1);
        int sx = x1<x2?1:-1, sy = y1<y2?1:-1, err = dx-dy, x=x1, y=y1;
        while (true) {
            if (x>=0&&x<TILE&&y>=0&&y<TILE) px[y*TILE+x] = rgb(shade, shade, shade+4);
            if (x==x2&&y==y2) break;
            int e2 = 2*err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
        }
    }

    public static int rgb(int r, int g, int b)         { return (0xFF<<24)|(r<<16)|(g<<8)|b; }
    public static int rgba(int r, int g, int b, int a) { return (a<<24)|(r<<16)|(g<<8)|b; }

    private static int vary(Random rng, int base, int amt) {
        return Math.max(0, Math.min(255, base + rng.nextInt(amt*2+1) - amt));
    }

    public static int vc(Random rng, int r, int g, int b, int amt) {
        return rgb(vary(rng,r,amt), vary(rng,g,amt), vary(rng,b,amt));
    }
}
