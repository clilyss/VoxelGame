package club.lily.voxelgame.world.chunk;

import club.lily.voxelgame.world.World;
import club.lily.voxelgame.world.block.BlockType;
import club.lily.voxelgame.world.light.LightMap;

public class Chunk {

    public static final int WIDTH  = 16;
    public static final int HEIGHT = 128;
    public static final int DEPTH  = 16;

    public final int cx, cz;

    private final BlockType[] blocks = new BlockType[WIDTH * HEIGHT * DEPTH];
    private LightMap  lightMap;
    private final ChunkMesh mesh = new ChunkMesh();
    private volatile boolean dirty = true;

    public Chunk(int cx, int cz) {
        this.cx = cx; this.cz = cz;
        for (int i = 0; i < blocks.length; i++) blocks[i] = BlockType.AIR;
    }

    public BlockType getBlock(int x, int y, int z) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) return BlockType.AIR;
        return blocks[idx(x, y, z)];
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) return;
        blocks[idx(x, y, z)] = type;
        dirty = true;
    }

    public void computeLight(World world) {
        lightMap = new LightMap(WIDTH, HEIGHT, DEPTH);
        lightMap.computeSunlight(this, world);
    }

    public void buildMesh(World world) {
        if (!dirty) return;
        dirty = false;
        mesh.buildGeometry(this, world);
    }

    public boolean tryScheduleBuild(World world, java.util.concurrent.ExecutorService pool) {
        if (!dirty) return false;
        if (!mesh.building.compareAndSet(false, true)) return false;
        dirty = false;
        pool.submit(() -> mesh.buildGeometry(this, world));
        return true;
    }

    public boolean uploadIfReady() { return mesh.uploadIfReady(); }

    public void render()    { mesh.render(); }
    public void cleanup()   { mesh.cleanup(); }
    public void markDirty() { dirty = true; }
    public boolean isDirty()      { return dirty; }
    public LightMap getLightMap() { return lightMap; }
    public ChunkMesh getMesh()    { return mesh; }

    private int idx(int x, int y, int z) { return x + y * WIDTH + z * WIDTH * HEIGHT; }
}