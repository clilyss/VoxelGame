package club.lily.voxelgame.world.light;

import club.lily.voxelgame.world.chunk.Chunk;

public class LightMap {

    public static final int MAX_LIGHT = 15;

    private final byte[] data;
    private final int W, H, D;

    public LightMap(int width, int height, int depth) {
        this.W = width; this.H = height; this.D = depth;
        data = new byte[W * H * D];
    }

    public int getSky(int x, int y, int z) {
        if (oob(x, y, z)) return MAX_LIGHT;
        return (data[idx(x, y, z)] & 0xFF) >>> 4;
    }

    public void setSky(int x, int y, int z, int v) {
        if (oob(x, y, z)) return;
        int i = idx(x, y, z);
        data[i] = (byte)(((v & 0xF) << 4) | (data[i] & 0x0F));
    }

    public int getBlockLight(int x, int y, int z) {
        if (oob(x, y, z)) return 0;
        return data[idx(x, y, z)] & 0x0F;
    }

    public void setBlockLight(int x, int y, int z, int v) {
        if (oob(x, y, z)) return;
        int i = idx(x, y, z);
        data[i] = (byte)((data[i] & 0xF0) | (v & 0x0F));
    }

    public int getLight(int x, int y, int z) {
        return Math.max(getSky(x, y, z), getBlockLight(x, y, z));
    }

    public void computeSunlight(Chunk chunk) {
        for (int i = 0; i < data.length; i++) data[i] = (byte)(data[i] & 0x0F);
        SunlightPropagator.propagate(this, chunk);
    }

    public static float ao(boolean s1, boolean s2, boolean corner) {
        if (s1 && s2) return 0.0f;
        int occ = (s1 ? 1 : 0) + (s2 ? 1 : 0) + (corner ? 1 : 0);
        return 1.0f - occ * 0.12f;
    }

    public boolean oob(int x, int y, int z) {
        return x < 0 || x >= W || y < 0 || y >= H || z < 0 || z >= D;
    }

    private int idx(int x, int y, int z) { return x + y * W + z * W * H; }
}
