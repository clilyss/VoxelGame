package club.lily.voxelgame.world;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class Chunk {

    public static final int WIDTH  = 16;
    public static final int HEIGHT = 128;
    public static final int DEPTH  = 16;

    public final int cx, cz;

    private final BlockType[] blocks = new BlockType[WIDTH * HEIGHT * DEPTH];
    private LightMap lightMap;

    private int  vao = -1, vbo = -1;
    private int  vertexCount = 0;
    private boolean dirty = true;

    public Chunk(int cx, int cz) {
        this.cx = cx; this.cz = cz;
        for (int i = 0; i < blocks.length; i++) blocks[i] = BlockType.AIR;
    }

    public BlockType getBlock(int x, int y, int z) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH)
            return BlockType.AIR;
        return blocks[index(x, y, z)];
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) return;
        blocks[index(x, y, z)] = type;
        dirty = true;
    }

    private int index(int x, int y, int z) { return x + y * WIDTH + z * WIDTH * HEIGHT; }

    public void computeLight(World world) {
        lightMap = new LightMap(WIDTH, HEIGHT, DEPTH);
        lightMap.computeSunlight(this, world);
    }

    public LightMap getLightMap() { return lightMap; }

    public void buildMesh(World world) {
        if (!dirty) return;
        dirty = false;

        List<Float> verts = new ArrayList<>(1024 * 7);
        int ox = cx * WIDTH, oz = cz * DEPTH;

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    BlockType b = getBlock(x, y, z);
                    if (b == BlockType.AIR) continue;

                    float wx = ox + x, wy = y, wz = oz + z;

                    if (transparent(world, x, y+1, z)) emitFace(verts, world, x,y,z, wx,wy,wz, Face.TOP,    b.tileTop,    1.00f);
                    if (transparent(world, x, y-1, z)) emitFace(verts, world, x,y,z, wx,wy,wz, Face.BOTTOM, b.tileBottom, 0.50f);
                    if (transparent(world, x, y, z+1)) emitFace(verts, world, x,y,z, wx,wy,wz, Face.FRONT,  b.tileSide,   0.82f);
                    if (transparent(world, x, y, z-1)) emitFace(verts, world, x,y,z, wx,wy,wz, Face.BACK,   b.tileSide,   0.82f);
                    if (transparent(world, x+1, y, z)) emitFace(verts, world, x,y,z, wx,wy,wz, Face.RIGHT,  b.tileSide,   0.65f);
                    if (transparent(world, x-1, y, z)) emitFace(verts, world, x,y,z, wx,wy,wz, Face.LEFT,   b.tileSide,   0.65f);
                }
            }
        }

        uploadMesh(verts);
    }

    private enum Face { TOP, BOTTOM, FRONT, BACK, RIGHT, LEFT }

    private void emitFace(List<Float> verts, World world,
                          int lx, int ly, int lz,
                          float wx, float wy, float wz,
                          Face face, int tileCol, float dirBias) {
        float x = wx, y = wy, z = wz;
        float u0 = (float) tileCol / 16f;
        float v0 = 0f;
        float du = BlockType.tileU();
        float dv = BlockType.tileV();

        float[][] vd = switch (face) {
            case TOP -> {
                float l00 = skyAt(world, lx,   ly+1, lz+1); float ao00 = aoTop(world, lx, ly, lz, -1, +1);
                float l10 = skyAt(world, lx+1, ly+1, lz+1); float ao10 = aoTop(world, lx, ly, lz, +1, +1);
                float l11 = skyAt(world, lx+1, ly+1, lz  ); float ao11 = aoTop(world, lx, ly, lz, +1, -1);
                float l01 = skyAt(world, lx,   ly+1, lz  ); float ao01 = aoTop(world, lx, ly, lz, -1, -1);
                yield new float[][]{{x,y+1,z+1,   u0,v0+dv, l00*dirBias,ao00},
                        {x+1,y+1,z+1, u0+du,v0+dv, l10*dirBias,ao10},
                        {x+1,y+1,z,   u0+du,v0,    l11*dirBias,ao11},
                        {x,y+1,z,     u0,v0,        l01*dirBias,ao01}};
            }
            case BOTTOM -> {
                float l = skyAt(world, lx, ly-1, lz);
                yield new float[][]{{x,y,z,       u0,v0,      l*dirBias,1f},
                        {x+1,y,z,     u0+du,v0,   l*dirBias,1f},
                        {x+1,y,z+1,   u0+du,v0+dv,l*dirBias,1f},
                        {x,y,z+1,     u0,v0+dv,   l*dirBias,1f}};
            }
            case FRONT -> {
                float l = skyAt(world, lx, ly, lz+1);
                float ao0 = aoFront(world, lx, ly, lz, -1,  0);
                float ao1 = aoFront(world, lx, ly, lz, +1,  0);
                float ao2 = aoFront(world, lx, ly, lz, +1, +1);
                float ao3 = aoFront(world, lx, ly, lz, -1, +1);
                yield new float[][]{{x,y,z+1,     u0,v0+dv,   l*dirBias,ao0},
                        {x+1,y,z+1,   u0+du,v0+dv, l*dirBias,ao1},
                        {x+1,y+1,z+1, u0+du,v0,    l*dirBias,ao2},
                        {x,y+1,z+1,   u0,v0,        l*dirBias,ao3}};
            }
            case BACK -> {
                float l = skyAt(world, lx, ly, lz-1);
                float ao0 = aoBack(world, lx, ly, lz, +1,  0);
                float ao1 = aoBack(world, lx, ly, lz, -1,  0);
                float ao2 = aoBack(world, lx, ly, lz, -1, +1);
                float ao3 = aoBack(world, lx, ly, lz, +1, +1);
                yield new float[][]{{x+1,y,z,     u0,v0+dv,   l*dirBias,ao0},
                        {x,y,z,       u0+du,v0+dv, l*dirBias,ao1},
                        {x,y+1,z,     u0+du,v0,    l*dirBias,ao2},
                        {x+1,y+1,z,   u0,v0,        l*dirBias,ao3}};
            }
            case RIGHT -> {
                float l = skyAt(world, lx+1, ly, lz);
                float ao0 = aoRight(world, lx, ly, lz, +1,  0);
                float ao1 = aoRight(world, lx, ly, lz, -1,  0);
                float ao2 = aoRight(world, lx, ly, lz, -1, +1);
                float ao3 = aoRight(world, lx, ly, lz, +1, +1);
                yield new float[][]{{x+1,y,z+1,   u0,v0+dv,   l*dirBias,ao0},
                        {x+1,y,z,     u0+du,v0+dv, l*dirBias,ao1},
                        {x+1,y+1,z,   u0+du,v0,    l*dirBias,ao2},
                        {x+1,y+1,z+1, u0,v0,        l*dirBias,ao3}};
            }
            case LEFT -> {
                float l = skyAt(world, lx-1, ly, lz);
                float ao0 = aoLeft(world, lx, ly, lz, -1,  0);
                float ao1 = aoLeft(world, lx, ly, lz, +1,  0);
                float ao2 = aoLeft(world, lx, ly, lz, +1, +1);
                float ao3 = aoLeft(world, lx, ly, lz, -1, +1);
                yield new float[][]{{x,y,z,       u0,v0+dv,   l*dirBias,ao0},
                        {x,y,z+1,     u0+du,v0+dv, l*dirBias,ao1},
                        {x,y+1,z+1,   u0+du,v0,    l*dirBias,ao2},
                        {x,y+1,z,     u0,v0,        l*dirBias,ao3}};
            }
        };

        boolean flip = (vd[0][6] + vd[2][6]) < (vd[1][6] + vd[3][6]);
        if (flip) {
            addV(verts,vd[1]); addV(verts,vd[2]); addV(verts,vd[3]);
            addV(verts,vd[1]); addV(verts,vd[3]); addV(verts,vd[0]);
        } else {
            addV(verts,vd[0]); addV(verts,vd[1]); addV(verts,vd[2]);
            addV(verts,vd[0]); addV(verts,vd[2]); addV(verts,vd[3]);
        }
    }

    private static void addV(List<Float> v, float[] d) { for (float f : d) v.add(f); }

    private static final float MIN_LIGHT = 0.05f;

    private float skyAt(World world, int lx, int ly, int lz) {
        if (ly < 0) return MIN_LIGHT;
        if (ly >= HEIGHT) return 1.0f;

        if (lx >= 0 && lx < WIDTH && lz >= 0 && lz < DEPTH) {
            if (lightMap == null) return 1.0f;
            float v = lightMap.getSky(lx, ly, lz) / (float) LightMap.MAX_LIGHT;
            if (v <= 0f) v = searchUpForLight(lx, ly, lz);
            return Math.max(v, MIN_LIGHT);
        }

        int wx = cx * WIDTH + lx, wz = cz * DEPTH + lz;
        Chunk nb = world.getChunkAt(wx, wz);
        if (nb == null || nb.getLightMap() == null) return 1.0f;
        int nlx = Math.floorMod(wx, WIDTH), nlz = Math.floorMod(wz, DEPTH);
        float v = nb.getLightMap().getSky(nlx, ly, nlz) / (float) LightMap.MAX_LIGHT;
        return Math.max(v, MIN_LIGHT);
    }

    private float searchUpForLight(int lx, int ly, int lz) {
        for (int y = ly + 1; y < HEIGHT; y++) {
            if (getBlock(lx, y, lz) == BlockType.AIR && lightMap != null) {
                float v = lightMap.getSky(lx, y, lz) / (float) LightMap.MAX_LIGHT;
                if (v > 0f) return v * 0.85f;
            }
        }
        return MIN_LIGHT;
    }

    private float aoTop(World world, int lx, int ly, int lz, int dx, int dz) {
        boolean s1 = solid(world, lx + dx, ly + 1, lz);
        boolean s2 = solid(world, lx,      ly + 1, lz + dz);
        boolean c  = solid(world, lx + dx, ly + 1, lz + dz);
        return LightMap.ao(s1, s2, c);
    }

    private float aoFront(World world, int lx, int ly, int lz, int dx, int dy) {
        boolean s1 = solid(world, lx + dx, ly,      lz + 1);
        boolean s2 = solid(world, lx,      ly + dy, lz + 1);
        boolean c  = solid(world, lx + dx, ly + dy, lz + 1);
        return LightMap.ao(s1, s2, c);
    }

    private float aoBack(World world, int lx, int ly, int lz, int dx, int dy) {
        boolean s1 = solid(world, lx + dx, ly,      lz - 1);
        boolean s2 = solid(world, lx,      ly + dy, lz - 1);
        boolean c  = solid(world, lx + dx, ly + dy, lz - 1);
        return LightMap.ao(s1, s2, c);
    }

    private float aoRight(World world, int lx, int ly, int lz, int dz, int dy) {
        boolean s1 = solid(world, lx + 1, ly,      lz + dz);
        boolean s2 = solid(world, lx + 1, ly + dy, lz);
        boolean c  = solid(world, lx + 1, ly + dy, lz + dz);
        return LightMap.ao(s1, s2, c);
    }

    private float aoLeft(World world, int lx, int ly, int lz, int dz, int dy) {
        boolean s1 = solid(world, lx - 1, ly,      lz + dz);
        boolean s2 = solid(world, lx - 1, ly + dy, lz);
        boolean c  = solid(world, lx - 1, ly + dy, lz + dz);
        return LightMap.ao(s1, s2, c);
    }

    private boolean solid(World world, int lx, int ly, int lz) {
        if (ly < 0 || ly >= HEIGHT) return false;
        if (lx >= 0 && lx < WIDTH && lz >= 0 && lz < DEPTH)
            return getBlock(lx, ly, lz).isOpaque();
        return world.getBlock(cx * WIDTH + lx, ly, cz * DEPTH + lz).isOpaque();
    }

    private boolean transparent(World world, int lx, int ly, int lz) {
        if (ly < 0 || ly >= HEIGHT) return true;
        if (lx < 0 || lx >= WIDTH || lz < 0 || lz >= DEPTH)
            return !world.getBlock(cx * WIDTH + lx, ly, cz * DEPTH + lz).isOpaque();
        return !getBlock(lx, ly, lz).isOpaque();
    }

    private void uploadMesh(List<Float> verts) {
        float[] data = new float[verts.size()];
        for (int i = 0; i < data.length; i++) data[i] = verts.get(i);
        vertexCount = data.length / 7;

        if (vao == -1) { vao = glGenVertexArrays(); vbo = glGenBuffers(); }

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);

        int stride = 7 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
    }

    public void render() {
        if (vao == -1 || vertexCount == 0) return;
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (vao != -1) { glDeleteVertexArrays(vao); glDeleteBuffers(vbo); }
    }

    public void markDirty() { dirty = true; }
    public boolean isDirty() { return dirty; }
}
