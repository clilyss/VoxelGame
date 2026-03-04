package club.lily.voxelgame.world;

import club.lily.voxelgame.assets.TextureGen;

public enum BlockType {
    
    AIR    (-1,                   -1,                   -1),
    GRASS  (T.GRASS_TOP,          T.GRASS_SIDE,         T.DIRT),
    DIRT   (T.DIRT,               T.DIRT,               T.DIRT),
    STONE  (T.STONE,              T.STONE,              T.STONE),
    BEDROCK(T.BEDROCK,            T.BEDROCK,            T.BEDROCK),
    SAND   (T.SAND,               T.SAND,               T.SAND),
    SNOW   (T.SNOW,               T.SNOW,               T.DIRT),
    WOOD   (T.WOOD_TOP,           T.WOOD_SIDE,          T.WOOD_TOP),
    LEAVES (T.LEAVES,             T.LEAVES,             T.LEAVES);

    public final int tileTop, tileSide, tileBottom;

    BlockType(int tileTop, int tileSide, int tileBottom) {
        this.tileTop    = tileTop;
        this.tileSide   = tileSide;
        this.tileBottom = tileBottom;
    }

    
    public static float[] uv(int tileCol) {
        float u = (float) tileCol / TextureGen.COLS;
        float v = 0.0f;
        return new float[]{u, v};
    }

    public static float tileU() { return 1.0f / TextureGen.COLS; }
    public static float tileV() { return 1.0f / TextureGen.ROWS; }

    public boolean isSolid()  { return this != AIR; }
    public boolean isOpaque() { return this != AIR && this != LEAVES; }

    private static class T {
        static final int GRASS_TOP  = TextureGen.T_GRASS_TOP;
        static final int GRASS_SIDE = TextureGen.T_GRASS_SIDE;
        static final int DIRT       = TextureGen.T_DIRT;
        static final int STONE      = TextureGen.T_STONE;
        static final int BEDROCK    = TextureGen.T_BEDROCK;
        static final int SAND       = TextureGen.T_SAND;
        static final int SNOW       = TextureGen.T_SNOW;
        static final int WOOD_TOP   = TextureGen.T_WOOD_TOP;
        static final int WOOD_SIDE  = TextureGen.T_WOOD_SIDE;
        static final int LEAVES     = TextureGen.T_LEAVES;
    }
}
