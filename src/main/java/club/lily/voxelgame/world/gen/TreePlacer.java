package club.lily.voxelgame.world.gen;

import club.lily.voxelgame.world.block.BlockType;
import club.lily.voxelgame.world.chunk.Chunk;

public final class TreePlacer {

    private TreePlacer() {}

    public static void place(Chunk chunk, int[][] hMap) {
        for (int x = 2; x < Chunk.WIDTH - 2; x++) {
            for (int z = 2; z < Chunk.DEPTH - 2; z++) {
                int surface = hMap[x][z];
                if (surface < 43 || surface > 61) continue;

                int wx = chunk.cx * Chunk.WIDTH + x;
                int wz = chunk.cz * Chunk.DEPTH + z;

                if (isGridSlot(wx, wz)) {
                    placeTree(chunk, x, surface + 1, z);
                } else if (isExtraSlot(wx, wz)) {
                    placeTree(chunk, x, surface + 1, z);
                }
            }
        }
    }

    private static boolean isGridSlot(int wx, int wz) {
        int gridX = Math.floorDiv(wx, 9);
        int gridZ = Math.floorDiv(wz, 9);
        int jitterX = Math.floorMod(Noise.hash(gridX * 7,  gridZ * 3),  5) - 2;
        int jitterZ = Math.floorMod(Noise.hash(gridX * 11, gridZ * 17), 5) - 2;
        return (wx - Math.floorMod(Noise.hash(gridX,    gridZ)      & 0xFF, 9) - gridX * 9) == jitterX &&
               (wz - Math.floorMod(Noise.hash(gridX*5, gridZ * 13)  & 0xFF, 9) - gridZ * 9) == jitterZ;
    }

    private static boolean isExtraSlot(int wx, int wz) {
        return Noise.get(wx * 0.18f + 500f, wz * 0.18f + 500f) > 0.88f
            && Noise.get(wx * 0.35f + 999f, wz * 0.35f + 999f) > 0.60f;
    }

    private static void placeTree(Chunk chunk, int x, int y, int z) {
        int trunkH = 4 + Math.abs(Noise.hash(x, y) % 3);
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
}
