package club.lily.voxelgame.world.light;

import club.lily.voxelgame.world.World;
import club.lily.voxelgame.world.chunk.Chunk;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SunlightPropagator {

    private static final int[] DDX = {1, -1, 0,  0, 0,  0};
    private static final int[] DDY = {0,  0, 1, -1, 0,  0};
    private static final int[] DDZ = {0,  0, 0,  0, 1, -1};

    private SunlightPropagator() {}

    public static void propagate(LightMap lm, Chunk chunk, World world) {
        int W = Chunk.WIDTH, H = Chunk.HEIGHT, D = Chunk.DEPTH;
        Deque<int[]> queue = new ArrayDeque<>(8192);

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

        seedFromNeighbour(lm, chunk, world, queue, -1,  0);
        seedFromNeighbour(lm, chunk, world, queue, +1,  0);
        seedFromNeighbour(lm, chunk, world, queue,  0, -1);
        seedFromNeighbour(lm, chunk, world, queue,  0, +1);

        while (!queue.isEmpty()) {
            int[] node  = queue.poll();
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

    private static void seedFromNeighbour(LightMap lm, Chunk chunk, World world,
                                          Deque<int[]> queue, int dcx, int dcz) {
        int nbWx = (chunk.cx + dcx) * Chunk.WIDTH;
        int nbWz = (chunk.cz + dcz) * Chunk.DEPTH;
        Chunk nb = world.getChunkAt(nbWx, nbWz);
        if (nb == null || nb.getLightMap() == null) return;

        LightMap nbLm = nb.getLightMap();

        if (dcx == -1) {
            for (int z = 0; z < Chunk.DEPTH; z++)
                for (int y = 0; y < Chunk.HEIGHT; y++)
                    tryInject(lm, chunk, queue, 0, y, z, nbLm.getSky(Chunk.WIDTH - 1, y, z));
        } else if (dcx == +1) {
            for (int z = 0; z < Chunk.DEPTH; z++)
                for (int y = 0; y < Chunk.HEIGHT; y++)
                    tryInject(lm, chunk, queue, Chunk.WIDTH - 1, y, z, nbLm.getSky(0, y, z));
        } else if (dcz == -1) {
            for (int x = 0; x < Chunk.WIDTH; x++)
                for (int y = 0; y < Chunk.HEIGHT; y++)
                    tryInject(lm, chunk, queue, x, y, 0, nbLm.getSky(x, y, Chunk.DEPTH - 1));
        } else {
            for (int x = 0; x < Chunk.WIDTH; x++)
                for (int y = 0; y < Chunk.HEIGHT; y++)
                    tryInject(lm, chunk, queue, x, y, Chunk.DEPTH - 1, nbLm.getSky(x, y, 0));
        }
    }

    private static void tryInject(LightMap lm, Chunk chunk, Deque<int[]> queue,
                                  int x, int y, int z, int neighbourLevel) {
        int spread = neighbourLevel - 1;
        if (spread <= 0) return;
        if (chunk.getBlock(x, y, z).isOpaque()) return;
        if (lm.getSky(x, y, z) < spread) {
            lm.setSky(x, y, z, spread);
            queue.add(new int[]{x, y, z, spread});
        }
    }
}