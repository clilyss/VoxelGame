package club.lily.voxelgame.world.gen;

import club.lily.voxelgame.world.chunk.Chunk;

public final class HeightMap {

    private HeightMap() {}

    public static int[][] build(Chunk chunk) {
        int[][] h = new int[Chunk.WIDTH][Chunk.DEPTH];
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int wx = chunk.cx * Chunk.WIDTH + x;
                int wz = chunk.cz * Chunk.DEPTH + z;
                float n = 0;
                n += Noise.get(wx * 0.006f, wz * 0.006f) * 38;
                n += Noise.get(wx * 0.018f, wz * 0.018f) * 18;
                n += Noise.get(wx * 0.055f, wz * 0.055f) *  8;
                n += Noise.get(wx * 0.110f, wz * 0.110f) *  3;
                n += Noise.get(wx * 0.220f, wz * 0.220f) *  1;
                h[x][z] = Math.max(4, Math.min(Chunk.HEIGHT - 8, 96 + (int) n));
            }
        }
        return h;
    }
}