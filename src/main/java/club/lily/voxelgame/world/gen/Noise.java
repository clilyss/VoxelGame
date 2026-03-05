package club.lily.voxelgame.world.gen;

public final class Noise {

    private Noise() {}

    public static float get(float x, float z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        float xf = x - xi, zf = z - zi;
        float u = fade(xf), v = fade(zf);
        float a = grad2(xi,   zi),   b = grad2(xi+1, zi);
        float c = grad2(xi,   zi+1), d = grad2(xi+1, zi+1);
        return lerp(v, lerp(u, a, b), lerp(u, c, d));
    }

    public static float get3(float x, float y, float z) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        float xf = x-xi, yf = y-yi, zf = z-zi;
        float u = fade(xf), v = fade(yf), w = fade(zf);
        float a000=grad3(xi,  yi,  zi),   a100=grad3(xi+1,yi,  zi);
        float a010=grad3(xi,  yi+1,zi),   a110=grad3(xi+1,yi+1,zi);
        float a001=grad3(xi,  yi,  zi+1), a101=grad3(xi+1,yi,  zi+1);
        float a011=grad3(xi,  yi+1,zi+1), a111=grad3(xi+1,yi+1,zi+1);
        float x0 = lerp(u, a000, a100), x1 = lerp(u, a010, a110);
        float x2 = lerp(u, a001, a101), x3 = lerp(u, a011, a111);
        float y0 = lerp(v, x0, x1),     y1 = lerp(v, x2, x3);
        return lerp(w, y0, y1);
    }

    public static float octave3(float x, float y, float z, int octs, float persistence) {
        float val = 0, amp = 1, freq = 1, max = 0;
        for (int i = 0; i < octs; i++) {
            val += get3(x*freq, y*freq, z*freq) * amp;
            max += amp;
            amp *= persistence;
            freq *= 2f;
        }
        return val / max;
    }

    public static int hash(int x, int z) {
        int h = x * 1619 + z * 31337 + 1013904223;
        h ^= (h >>> 16); h *= 0x45d9f3b; h ^= (h >>> 16);
        return h;
    }

    public static int hash3(int x, int y, int z) {
        int h = x * 1619 + y * 31337 + z * 52391 + 1013904223;
        h ^= (h >>> 16); h *= 0x45d9f3b; h ^= (h >>> 16);
        return h;
    }

    private static float grad2(int x, int z) {
        return (float) Math.sin(Math.toRadians(hash(x, z) & 0x1FF));
    }

    private static float grad3(int x, int y, int z) {
        int h = hash3(x, y, z) & 15;
        float u = h < 8 ? grad2(x, y) : grad2(y, z);
        float v = h < 4 ? grad2(y, z) : (h==12||h==14 ? grad2(x,y) : grad2(x,z));
        return ((h&1)==0?u:-u) + ((h&2)==0?v:-v);
    }

    private static float fade(float t) { return t*t*t*(t*(t*6-15)+10); }
    private static float lerp(float t, float a, float b) { return a+t*(b-a); }
}