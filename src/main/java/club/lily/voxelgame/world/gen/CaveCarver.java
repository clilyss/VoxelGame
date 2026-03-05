package club.lily.voxelgame.world.gen;

import club.lily.voxelgame.world.block.BlockType;
import club.lily.voxelgame.world.chunk.Chunk;

public final class CaveCarver {

    private static final int   SURFACE_BUFFER      = 20;
    private static final float CHEESE_THRESHOLD    = 0.055f;
    private static final float SPAGHETTI_THRESHOLD = 0.018f;

    private static final int   WORM_COUNT   = 4;
    private static final int   WORM_MAX_LEN = 140;
    private static final int   WORM_MIN_LEN = 60;
    private static final float WORM_RADIUS  = 2.0f;

    private CaveCarver() {}

    public static void carve(Chunk chunk, int[][] heightMap) {
        int ox = chunk.cx * Chunk.WIDTH;
        int oz = chunk.cz * Chunk.DEPTH;
        carveCheeseNoise(chunk, ox, oz, heightMap);
        carveSpaghettiNoise(chunk, ox, oz, heightMap);
        carveWorms(chunk, ox, oz, heightMap);
    }

    private static int caveRoof(int surface) {
        return surface - SURFACE_BUFFER;
    }

    private static void carveCheeseNoise(Chunk chunk, int ox, int oz, int[][] heightMap) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int roof = caveRoof(heightMap[x][z]);
                for (int y = 1; y < roof; y++) {
                    if (!carveable(chunk, x, y, z)) continue;

                    float wx = ox + x, wy = y, wz = oz + z;

                    float n1 = Noise.octave3(wx*0.022f,        wy*0.015f,        wz*0.022f,        3, 0.45f);
                    float n2 = Noise.octave3(wx*0.022f+500f,   wy*0.015f+500f,   wz*0.022f+500f,   3, 0.45f);
                    float cheese = n1*n1 + n2*n2;

                    if (cheese < CHEESE_THRESHOLD * depthBias(y, heightMap[x][z])) {
                        setAir(chunk, x, y, z);
                    }
                }
            }
        }
    }

    private static void carveSpaghettiNoise(Chunk chunk, int ox, int oz, int[][] heightMap) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int roof = caveRoof(heightMap[x][z]);
                for (int y = 1; y < roof; y++) {
                    if (!carveable(chunk, x, y, z)) continue;

                    float wx = ox + x, wy = y, wz = oz + z;

                    float s1 = Noise.get3(wx*0.040f,        wy*0.026f,        wz*0.040f);
                    float s2 = Noise.get3(wx*0.040f+300f,   wy*0.026f+300f,   wz*0.040f+300f);

                    if (Math.abs(s1) < SPAGHETTI_THRESHOLD && Math.abs(s2) < SPAGHETTI_THRESHOLD * 2.5f) {
                        setAir(chunk, x, y, z);
                        if (y + 1 < roof) setAir(chunk, x, y + 1, z);
                    }
                }
            }
        }
    }

    private static void carveWorms(Chunk chunk, int ox, int oz, int[][] heightMap) {
        for (int i = 0; i < WORM_COUNT; i++) {
            int seed = Noise.hash3(chunk.cx * 73 + i * 17, chunk.cz * 179 + i * 11, i * 31 + 7);

            int startX = Math.floorMod(seed,      Chunk.WIDTH);
            int startZ = Math.floorMod(seed >> 4, Chunk.DEPTH);
            int roof   = caveRoof(heightMap[startX][startZ]);
            if (roof < 10) continue;

            int startY = 4 + Math.floorMod(Math.abs(seed >> 8), Math.max(1, roof - 8));
            if (startY >= roof) continue;

            float px = ox + startX, py = startY, pz = oz + startZ;
            float yaw   = (float)((seed & 0x1FF) * Math.PI * 2.0 / 512.0);
            float pitch = (float)(((seed >> 9) & 0xFF) * Math.PI / 256.0) - (float)(Math.PI * 0.15);

            int   len    = WORM_MIN_LEN + Math.floorMod(Math.abs(seed >> 17), WORM_MAX_LEN - WORM_MIN_LEN);
            float radius = WORM_RADIUS + Math.floorMod(Math.abs(seed >> 20), 10) * 0.1f;

            for (int step = 0; step < len; step++) {
                float t = (float) step / len;
                yaw   += Noise.get(px * 0.07f + step * 0.2f,       pz * 0.07f + step * 0.2f)       * 0.18f;
                pitch += Noise.get(px * 0.07f + step * 0.2f + 50f, pz * 0.07f + step * 0.2f + 50f) * 0.08f;
                pitch  = Math.max(-(float)(Math.PI * 0.25), Math.min((float)(Math.PI * 0.25), pitch));

                float dx = (float)(Math.cos(pitch) * Math.cos(yaw));
                float dy = (float) Math.sin(pitch);
                float dz = (float)(Math.cos(pitch) * Math.sin(yaw));

                px += dx; py += dy; pz += dz;

                float r  = radius * (1f - 0.2f * Math.abs(t * 2f - 1f));
                float rH = r * 1.2f;
                float rV = r * 0.8f;

                int bx0 = (int) Math.floor(px - rH), bx1 = (int) Math.ceil(px + rH);
                int by0 = (int) Math.floor(py - rV), by1 = (int) Math.ceil(py + rV);
                int bz0 = (int) Math.floor(pz - rH), bz1 = (int) Math.ceil(pz + rH);

                for (int bx = bx0; bx <= bx1; bx++) {
                    int lx = bx - ox;
                    if (lx < 0 || lx >= Chunk.WIDTH) continue;
                    for (int bz = bz0; bz <= bz1; bz++) {
                        int lz = bz - oz;
                        if (lz < 0 || lz >= Chunk.DEPTH) continue;
                        int localRoof = caveRoof(heightMap[lx][lz]);
                        for (int by = by0; by <= by1; by++) {
                            if (by < 1 || by >= localRoof) continue;
                            float ddx = (bx - px) / rH;
                            float ddy = (by - py) / rV;
                            float ddz = (bz - pz) / rH;
                            if (ddx*ddx + ddy*ddy + ddz*ddz <= 1.0f) {
                                setAir(chunk, lx, by, lz);
                            }
                        }
                    }
                }
            }
        }
    }

    private static float depthBias(int y, int surface) {
        float norm = (float) y / surface;
        if (norm < 0.12f) return 1.4f;
        if (norm < 0.35f) return 1.1f;
        if (norm < 0.60f) return 0.85f;
        return 0.3f;
    }

    private static boolean carveable(Chunk chunk, int x, int y, int z) {
        BlockType b = chunk.getBlock(x, y, z);
        return b != BlockType.BEDROCK && b != BlockType.AIR;
    }

    private static void setAir(Chunk chunk, int x, int y, int z) {
        if (chunk.getBlock(x, y, z) == BlockType.BEDROCK) return;
        chunk.setBlock(x, y, z, BlockType.AIR);
    }
}