package club.lily.voxelgame.world;

import java.util.ArrayDeque;
import java.util.Deque;

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

    public int getBlock(int x, int y, int z) {
        if (oob(x, y, z)) return 0;
        return data[idx(x, y, z)] & 0x0F;
    }

    public void setBlock(int x, int y, int z, int v) {
        if (oob(x, y, z)) return;
        int i = idx(x, y, z);
        data[i] = (byte)((data[i] & 0xF0) | (v & 0x0F));
    }

    public int getLight(int x, int y, int z) {
        return Math.max(getSky(x, y, z), getBlock(x, y, z));
    }

    public void computeSunlight(Chunk chunk, World world) {
        for (int i = 0; i < data.length; i++) data[i] = (byte)(data[i] & 0x0F);

        Deque<int[]> queue = new ArrayDeque<>(4096);

        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                boolean inSun = true;
                for (int y = H - 1; y >= 0; y--) {
                    BlockType b = chunk.getBlock(x, y, z);
                    if (b.isOpaque()) {
                        inSun = false;
                        continue;
                    }
                    if (inSun) {
                        setSky(x, y, z, MAX_LIGHT);
                        queue.add(new int[]{x, y, z, MAX_LIGHT});
                    }
                }
            }
        }

        int[] ddx = {1, -1, 0, 0, 0, 0};
        int[] ddy = {0,  0, 1, -1, 0, 0};
        int[] ddz = {0,  0, 0,  0, 1, -1};

        while (!queue.isEmpty()) {
            int[] node = queue.poll();
            int nx = node[0], ny = node[1], nz = node[2], nl = node[3];
            int spread = nl - 1;
            if (spread <= 0) continue;

            for (int d = 0; d < 6; d++) {
                int bx = nx + ddx[d], by = ny + ddy[d], bz = nz + ddz[d];
                if (oob(bx, by, bz)) continue;
                if (chunk.getBlock(bx, by, bz).isOpaque()) continue;
                if (getSky(bx, by, bz) < spread) {
                    setSky(bx, by, bz, spread);
                    queue.add(new int[]{bx, by, bz, spread});
                }
            }
        }
    }

    public static float smoothLight(LightMap lm, int[][] coords) {
        float sum = 0;
        int count = 0;
        for (int[] c : coords) {
            sum += lm != null ? lm.getSky(c[0], c[1], c[2]) : MAX_LIGHT;
            count++;
        }
        return count == 0 ? 1.0f : (sum / count) / MAX_LIGHT;
    }

    public static float ao(boolean s1, boolean s2, boolean corner) {
        if (s1 && s2) return 0.0f;
        int occ = (s1 ? 1 : 0) + (s2 ? 1 : 0) + (corner ? 1 : 0);
        return 1.0f - occ * 0.12f;
    }

    private boolean oob(int x, int y, int z) {
        return x < 0 || x >= W || y < 0 || y >= H || z < 0 || z >= D;
    }

    private int idx(int x, int y, int z) { return x + y * W + z * W * H; }
}
