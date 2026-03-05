package club.lily.voxelgame.world;

import club.lily.voxelgame.engine.shader.Shader;
import club.lily.voxelgame.world.block.BlockType;
import club.lily.voxelgame.world.chunk.Chunk;
import club.lily.voxelgame.world.gen.WorldGen;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class World {

    public static final int RENDER_DISTANCE = 8;

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final ExecutorService meshPool = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
    );

    public void generate() {
        for (int cx = -RENDER_DISTANCE; cx <= RENDER_DISTANCE; cx++)
            for (int cz = -RENDER_DISTANCE; cz <= RENDER_DISTANCE; cz++)
                loadChunk(cx, cz);
    }

    public void buildAllMeshes() {
        for (Chunk chunk : chunks.values()) chunk.computeLight(this);
        for (Chunk chunk : chunks.values()) chunk.buildMesh(this);
    }

    public void updateAroundPlayer(float wx, float wz) {
        int pcx = Math.floorDiv((int) wx, Chunk.WIDTH);
        int pcz = Math.floorDiv((int) wz, Chunk.DEPTH);

        for (int cx = pcx - RENDER_DISTANCE; cx <= pcx + RENDER_DISTANCE; cx++) {
            for (int cz = pcz - RENDER_DISTANCE; cz <= pcz + RENDER_DISTANCE; cz++) {
                long k = key(cx, cz);
                if (!chunks.containsKey(k)) {
                    Chunk chunk = new Chunk(cx, cz);
                    WorldGen.generate(chunk);
                    chunks.put(k, chunk);
                    chunk.computeLight(this);
                    chunk.tryScheduleBuild(this, meshPool);
                    relightAsync(cx-1,cz); relightAsync(cx+1,cz);
                    relightAsync(cx,cz-1); relightAsync(cx,cz+1);
                }
            }
        }
    }

    private void loadChunk(int cx, int cz) {
        long k = key(cx, cz);
        if (chunks.containsKey(k)) return;
        Chunk chunk = new Chunk(cx, cz);
        WorldGen.generate(chunk);
        chunks.put(k, chunk);
    }

    public BlockType getBlock(int wx, int wy, int wz) {
        Chunk c = chunkAt(wx, wz);
        if (c == null) return BlockType.AIR;
        return c.getBlock(localX(wx), wy, localZ(wz));
    }

    public void setBlock(int wx, int wy, int wz, BlockType type) {
        Chunk c = chunkAt(wx, wz);
        if (c == null) return;
        c.setBlock(localX(wx), wy, localZ(wz), type);
        int ccx = chunkCoord(wx), ccz = chunkCoord(wz);
        relightAsync(ccx, ccz);
        relightAsync(ccx-1,ccz); relightAsync(ccx+1,ccz);
        relightAsync(ccx,ccz-1); relightAsync(ccx,ccz+1);
    }

    private void relightAsync(int cx, int cz) {
        Chunk c = chunks.get(key(cx, cz));
        if (c == null) return;
        c.computeLight(this);
        c.markDirty();
        c.tryScheduleBuild(this, meshPool);
    }

    public void render(Shader shader) {
        for (Chunk chunk : chunks.values()) {
            chunk.uploadIfReady();
            chunk.render();
        }
    }

    public void cleanup() {
        meshPool.shutdownNow();
        chunks.values().forEach(Chunk::cleanup);
    }

    public Chunk getChunkAt(int wx, int wz) { return chunkAt(wx, wz); }

    public int getTopY(int wx, int wz) {
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--)
            if (getBlock(wx, y, wz).isSolid()) return y;
        return -1;
    }

    public Collection<Chunk> getChunks() { return chunks.values(); }

    private Chunk chunkAt(int wx, int wz) { return chunks.get(key(chunkCoord(wx), chunkCoord(wz))); }

    private static int  chunkCoord(int w)   { return Math.floorDiv(w, Chunk.WIDTH); }
    private static int  localX(int wx)      { return Math.floorMod(wx, Chunk.WIDTH); }
    private static int  localZ(int wz)      { return Math.floorMod(wz, Chunk.DEPTH); }
    private static long key(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }
}