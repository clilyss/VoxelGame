package club.lily.voxelgame.world.light;

import club.lily.voxelgame.world.chunk.Chunk;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SunlightPropagator {

    private static final int[] DDX = {1, -1, 0,  0, 0,  0};
    private static final int[] DDY = {0,  0, 1, -1, 0,  0};
    private static final int[] DDZ = {0,  0, 0,  0, 1, -1};

    private SunlightPropagator() {}

    public static void propagate(LightMap lm, Chunk chunk) {
        int W = Chunk.WIDTH, H = Chunk.HEIGHT, D = Chunk.DEPTH;
        Deque<int[]> queue = new ArrayDeque<>(4096);

        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                boolean inSun = true;
                for (int y = H - 1; y >= 0; y--) {
                    if (chunk.getBlock(x, y, z).isOpaque()) { inSun = false; continue; }
                    if (inSun) {
                        lm.setSky(x, y, z, LightMap.MAX_LIGHT);
                        queue.add(new int[]{x, y, z, LightMap.MAX_LIGHT});
                    }
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] node   = queue.poll();
            int nx = node[0], ny = node[1], nz = node[2];
            int spread = node[3] - 1;
            if (spread <= 0) continue;

            for (int d = 0; d < 6; d++) {
                int bx = nx + DDX[d], by = ny + DDY[d], bz = nz + DDZ[d];
                if (lm.oob(bx, by, bz)) continue;
                if (chunk.getBlock(bx, by, bz).isOpaque()) continue;
                if (lm.getSky(bx, by, bz) < spread) {
                    lm.setSky(bx, by, bz, spread);
                    queue.add(new int[]{bx, by, bz, spread});
                }
            }
        }
    }
}
