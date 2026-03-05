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
                n += Noise.get(wx * 0.007f, wz * 0.007f) * 22;
                n += Noise.get(wx * 0.020f, wz * 0.020f) * 10;
                n += Noise.get(wx * 0.060f, wz * 0.060f) *  4;
                n += Noise.get(wx * 0.120f, wz * 0.120f) *  2;
                h[x][z] = Math.max(2, Math.min(Chunk.HEIGHT - 5, 60 + (int) n));
            }
        }
        return h;
    }
}
