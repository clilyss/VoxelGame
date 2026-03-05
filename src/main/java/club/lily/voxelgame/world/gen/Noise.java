package club.lily.voxelgame.world.gen;

public final class Noise {

    private Noise() {}

    public static float get(float x, float z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        float xf = x - xi;
        float zf = z - zi;
        float u = fade(xf);
        float v = fade(zf);
        float a = grad(xi,     zi);
        float b = grad(xi + 1, zi);
        float c = grad(xi,     zi + 1);
        float d = grad(xi + 1, zi + 1);
        return lerp(v, lerp(u, a, b), lerp(u, c, d));
    }

    public static int hash(int x, int z) {
        int h = x * 1619 + z * 31337 + 1013904223;
        h ^= (h >>> 16);
        h *= 0x45d9f3b;
        h ^= (h >>> 16);
        return h;
    }

    private static float grad(int x, int z) {
        return (float) Math.sin(Math.toRadians(hash(x, z) & 0x1FF));
    }

    private static float fade(float t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static float lerp(float t, float a, float b) { return a + t * (b - a); }
}
