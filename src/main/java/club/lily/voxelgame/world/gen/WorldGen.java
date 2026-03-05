package club.lily.voxelgame.world.gen;

import club.lily.voxelgame.world.block.BlockType;
import club.lily.voxelgame.world.chunk.Chunk;

public final class WorldGen {

    private WorldGen() {}

    public static void generate(Chunk chunk) {
        int[][] hMap = HeightMap.build(chunk);
        fillBlocks(chunk, hMap);
        TreePlacer.place(chunk, hMap);
    }

    private static void fillBlocks(Chunk chunk, int[][] hMap) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int surface = hMap[x][z];
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, blockAt(y, surface));
                }
            }
        }
    }

    private static BlockType blockAt(int y, int surface) {
        if (y == 0)              return BlockType.BEDROCK;
        if (y < surface - 4)    return BlockType.STONE;
        if (y < surface)        return surface < 42 ? BlockType.SAND : BlockType.DIRT;
        if (y == surface) {
            if (surface < 42)   return BlockType.SAND;
            if (surface > 62)   return BlockType.SNOW;
                                return BlockType.GRASS;
        }
        return BlockType.AIR;
    }
}
