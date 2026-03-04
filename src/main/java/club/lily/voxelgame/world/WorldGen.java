package club.lily.voxelgame.world;

public class WorldGen {

    public static void generate(Chunk chunk) {
        int[][] hMap = buildHeightMap(chunk);
        fillBlocks(chunk, hMap);
        placeTrees(chunk, hMap);
    }

    private static int[][] buildHeightMap(Chunk chunk) {
        int[][] h = new int[Chunk.WIDTH][Chunk.DEPTH];
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int wx = chunk.cx * Chunk.WIDTH + x;
                int wz = chunk.cz * Chunk.DEPTH + z;

                float n = 0;
                n += noise(wx * 0.007f, wz * 0.007f) * 22;
                n += noise(wx * 0.020f, wz * 0.020f) * 10;
                n += noise(wx * 0.060f, wz * 0.060f) *  4;
                n += noise(wx * 0.120f, wz * 0.120f) *  2;

                h[x][z] = Math.max(2, Math.min(Chunk.HEIGHT - 5, 60 + (int) n));
            }
        }
        return h;
    }

    private static void fillBlocks(Chunk chunk, int[][] hMap) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int surface = hMap[x][z];
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    BlockType block;
                    if (y == 0) {
                        block = BlockType.BEDROCK;
                    } else if (y < surface - 4) {
                        block = BlockType.STONE;
                    } else if (y < surface) {
                        block = (surface < 42) ? BlockType.SAND : BlockType.DIRT;
                    } else if (y == surface) {
                        if      (surface < 42) block = BlockType.SAND;
                        else if (surface > 62) block = BlockType.SNOW;
                        else                   block = BlockType.GRASS;
                    } else {
                        block = BlockType.AIR;
                    }
                    chunk.setBlock(x, y, z, block);
                }
            }
        }
    }

    private static void placeTrees(Chunk chunk, int[][] hMap) {
        for (int x = 2; x < Chunk.WIDTH - 2; x++) {
            for (int z = 2; z < Chunk.DEPTH - 2; z++) {
                int surface = hMap[x][z];
                if (surface < 43 || surface > 61) continue;

                int wx = chunk.cx * Chunk.WIDTH + x;
                int wz = chunk.cz * Chunk.DEPTH + z;

                int gridX = Math.floorDiv(wx, 9);
                int gridZ = Math.floorDiv(wz, 9);

                int jitterX = Math.floorMod(hash(gridX * 7, gridZ * 3), 5) - 2;
                int jitterZ = Math.floorMod(hash(gridX * 11, gridZ * 17), 5) - 2;

                if ((wx - Math.floorMod(hash(gridX, gridZ) & 0xFF, 9) - gridX * 9) == jitterX &&
                    (wz - Math.floorMod(hash(gridX * 5, gridZ * 13) & 0xFF, 9) - gridZ * 9) == jitterZ) {
                    placeTree(chunk, x, surface + 1, z);
                }

                if (noise(wx * 0.18f + 500f, wz * 0.18f + 500f) > 0.88f) {
                    if (noise(wx * 0.35f + 999f, wz * 0.35f + 999f) > 0.60f) {
                        placeTree(chunk, x, surface + 1, z);
                    }
                }
            }
        }
    }

    private static void placeTree(Chunk chunk, int x, int y, int z) {
        int trunkH = 4 + Math.abs(hash(x, y) % 3);

        for (int ty = y; ty < y + trunkH && ty < Chunk.HEIGHT; ty++)
            chunk.setBlock(x, ty, z, BlockType.WOOD);

        int leafBase = y + trunkH - 1;
        for (int ly = leafBase; ly <= leafBase + 2 && ly < Chunk.HEIGHT; ly++) {
            int radius = (ly == leafBase + 2) ? 1 : 2;
            for (int lx = -radius; lx <= radius; lx++) {
                for (int lz = -radius; lz <= radius; lz++) {
                    if (Math.abs(lx) == radius && Math.abs(lz) == radius) continue;
                    int bx = x + lx, bz = z + lz;
                    if (bx < 0 || bx >= Chunk.WIDTH || bz < 0 || bz >= Chunk.DEPTH) continue;
                    if (chunk.getBlock(bx, ly, bz) == BlockType.AIR)
                        chunk.setBlock(bx, ly, bz, BlockType.LEAVES);
                }
            }
        }
    }

    static float noise(float x, float z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        float xf = x - xi;
        float zf = z - zi;

        float u = fade(xf);
        float v = fade(zf);

        float a = grad2D(xi,   zi);
        float b = grad2D(xi+1, zi);
        float c = grad2D(xi,   zi+1);
        float d = grad2D(xi+1, zi+1);

        return lerp(v, lerp(u, a, b), lerp(u, c, d));
    }

    private static float grad2D(int x, int z) {
        return (float) Math.sin(Math.toRadians(hash(x, z) & 0x1FF));
    }

    static int hash(int x, int z) {
        int h = x * 1619 + z * 31337 + 1013904223;
        h ^= (h >>> 16);
        h *= 0x45d9f3b;
        h ^= (h >>> 16);
        return h;
    }

    private static float fade(float t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static float lerp(float t, float a, float b) { return a + t * (b - a); }
}
