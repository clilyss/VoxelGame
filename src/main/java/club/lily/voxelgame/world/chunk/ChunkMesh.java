package club.lily.voxelgame.world.chunk;

import club.lily.voxelgame.world.World;
import club.lily.voxelgame.world.block.BlockType;
import club.lily.voxelgame.world.light.LightMap;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class ChunkMesh {

    private static final float MIN_LIGHT = 0.05f;

    private int vao = -1, vbo = -1;
    private int vertexCount = 0;

    private enum Face { TOP, BOTTOM, FRONT, BACK, RIGHT, LEFT }

    public void rebuild(Chunk chunk, World world) {
        List<Float> verts = new ArrayList<>(1024 * 7);
        int ox = chunk.cx * Chunk.WIDTH, oz = chunk.cz * Chunk.DEPTH;

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    BlockType b = chunk.getBlock(x, y, z);
                    if (b == BlockType.AIR) continue;
                    float wx = ox + x, wy = y, wz = oz + z;
                    if (isTransparent(chunk, world, x, y+1, z)) emitFace(verts, chunk, world, x,y,z, wx,wy,wz, Face.TOP,    b.tileTop,    1.00f);
                    if (isTransparent(chunk, world, x, y-1, z)) emitFace(verts, chunk, world, x,y,z, wx,wy,wz, Face.BOTTOM, b.tileBottom, 0.50f);
                    if (isTransparent(chunk, world, x, y, z+1)) emitFace(verts, chunk, world, x,y,z, wx,wy,wz, Face.FRONT,  b.tileSide,   0.82f);
                    if (isTransparent(chunk, world, x, y, z-1)) emitFace(verts, chunk, world, x,y,z, wx,wy,wz, Face.BACK,   b.tileSide,   0.82f);
                    if (isTransparent(chunk, world, x+1, y, z)) emitFace(verts, chunk, world, x,y,z, wx,wy,wz, Face.RIGHT,  b.tileSide,   0.65f);
                    if (isTransparent(chunk, world, x-1, y, z)) emitFace(verts, chunk, world, x,y,z, wx,wy,wz, Face.LEFT,   b.tileSide,   0.65f);
                }
            }
        }
        upload(verts);
    }

    private void emitFace(List<Float> verts, Chunk chunk, World world,
                          int lx, int ly, int lz, float wx, float wy, float wz,
                          Face face, int tileCol, float dirBias) {
        float x = wx, y = wy, z = wz;
        float u0 = tileCol / 16f, v0 = 0f;
        float du = BlockType.tileU(), dv = BlockType.tileV();

        float[][] vd = switch (face) {
            case TOP -> {
                float l00=skyAt(chunk,world,lx,  ly+1,lz+1); float ao00=aoTop(chunk,world,lx,ly,lz,-1,+1);
                float l10=skyAt(chunk,world,lx+1,ly+1,lz+1); float ao10=aoTop(chunk,world,lx,ly,lz,+1,+1);
                float l11=skyAt(chunk,world,lx+1,ly+1,lz  ); float ao11=aoTop(chunk,world,lx,ly,lz,+1,-1);
                float l01=skyAt(chunk,world,lx,  ly+1,lz  ); float ao01=aoTop(chunk,world,lx,ly,lz,-1,-1);
                yield new float[][]{{x,y+1,z+1,   u0,v0+dv,    l00*dirBias,ao00},
                        {x+1,y+1,z+1, u0+du,v0+dv, l10*dirBias,ao10},
                        {x+1,y+1,z,   u0+du,v0,    l11*dirBias,ao11},
                        {x,y+1,z,     u0,v0,        l01*dirBias,ao01}};
            }
            case BOTTOM -> {
                float l = skyAt(chunk,world,lx,ly-1,lz);
                yield new float[][]{{x,y,z,       u0,v0,        l*dirBias,1f},
                        {x+1,y,z,     u0+du,v0,     l*dirBias,1f},
                        {x+1,y,z+1,   u0+du,v0+dv,  l*dirBias,1f},
                        {x,y,z+1,     u0,v0+dv,     l*dirBias,1f}};
            }
            case FRONT -> {
                float l=skyAt(chunk,world,lx,ly,lz+1);
                yield new float[][]{{x,y,z+1,     u0,v0+dv,    l*dirBias,aoFront(chunk,world,lx,ly,lz,-1, 0)},
                        {x+1,y,z+1,   u0+du,v0+dv, l*dirBias,aoFront(chunk,world,lx,ly,lz,+1, 0)},
                        {x+1,y+1,z+1, u0+du,v0,    l*dirBias,aoFront(chunk,world,lx,ly,lz,+1,+1)},
                        {x,y+1,z+1,   u0,v0,        l*dirBias,aoFront(chunk,world,lx,ly,lz,-1,+1)}};
            }
            case BACK -> {
                float l=skyAt(chunk,world,lx,ly,lz-1);
                yield new float[][]{{x+1,y,z,     u0,v0+dv,    l*dirBias,aoBack(chunk,world,lx,ly,lz,+1, 0)},
                        {x,y,z,       u0+du,v0+dv, l*dirBias,aoBack(chunk,world,lx,ly,lz,-1, 0)},
                        {x,y+1,z,     u0+du,v0,    l*dirBias,aoBack(chunk,world,lx,ly,lz,-1,+1)},
                        {x+1,y+1,z,   u0,v0,        l*dirBias,aoBack(chunk,world,lx,ly,lz,+1,+1)}};
            }
            case RIGHT -> {
                float l=skyAt(chunk,world,lx+1,ly,lz);
                yield new float[][]{{x+1,y,z+1,   u0,v0+dv,    l*dirBias,aoRight(chunk,world,lx,ly,lz,+1, 0)},
                        {x+1,y,z,     u0+du,v0+dv, l*dirBias,aoRight(chunk,world,lx,ly,lz,-1, 0)},
                        {x+1,y+1,z,   u0+du,v0,    l*dirBias,aoRight(chunk,world,lx,ly,lz,-1,+1)},
                        {x+1,y+1,z+1, u0,v0,        l*dirBias,aoRight(chunk,world,lx,ly,lz,+1,+1)}};
            }
            case LEFT -> {
                float l=skyAt(chunk,world,lx-1,ly,lz);
                yield new float[][]{{x,y,z,       u0,v0+dv,    l*dirBias,aoLeft(chunk,world,lx,ly,lz,-1, 0)},
                        {x,y,z+1,     u0+du,v0+dv, l*dirBias,aoLeft(chunk,world,lx,ly,lz,+1, 0)},
                        {x,y+1,z+1,   u0+du,v0,    l*dirBias,aoLeft(chunk,world,lx,ly,lz,+1,+1)},
                        {x,y+1,z,     u0,v0,        l*dirBias,aoLeft(chunk,world,lx,ly,lz,-1,+1)}};
            }
        };

        boolean flip = (vd[0][6] + vd[2][6]) < (vd[1][6] + vd[3][6]);
        if (flip) { addV(verts,vd[1]); addV(verts,vd[2]); addV(verts,vd[3]); addV(verts,vd[1]); addV(verts,vd[3]); addV(verts,vd[0]); }
        else      { addV(verts,vd[0]); addV(verts,vd[1]); addV(verts,vd[2]); addV(verts,vd[0]); addV(verts,vd[2]); addV(verts,vd[3]); }
    }

    private float skyAt(Chunk chunk, World world, int lx, int ly, int lz) {
        if (ly < 0) return MIN_LIGHT;
        if (ly >= Chunk.HEIGHT) return 1.0f;
        float v = rawSky(chunk, world, lx, ly, lz);
        if (v > MIN_LIGHT) return v;
        float best = MIN_LIGHT;
        best = Math.max(best, rawSky(chunk, world, lx - 1, ly, lz));
        best = Math.max(best, rawSky(chunk, world, lx + 1, ly, lz));
        best = Math.max(best, rawSky(chunk, world, lx, ly, lz - 1));
        best = Math.max(best, rawSky(chunk, world, lx, ly, lz + 1));
        best = Math.max(best, rawSky(chunk, world, lx, ly + 1, lz));
        return best;
    }

    private float rawSky(Chunk chunk, World world, int lx, int ly, int lz) {
        if (ly < 0 || ly >= Chunk.HEIGHT) return ly >= Chunk.HEIGHT ? 1.0f : MIN_LIGHT;
        if (lx >= 0 && lx < Chunk.WIDTH && lz >= 0 && lz < Chunk.DEPTH) {
            LightMap lm = chunk.getLightMap();
            if (lm == null) return 1.0f;
            return lm.getSky(lx, ly, lz) / (float) LightMap.MAX_LIGHT;
        }
        int wx = chunk.cx * Chunk.WIDTH + lx, wz = chunk.cz * Chunk.DEPTH + lz;
        Chunk nb = world.getChunkAt(wx, wz);
        if (nb == null || nb.getLightMap() == null) return 1.0f;
        int nlx = Math.floorMod(wx, Chunk.WIDTH), nlz = Math.floorMod(wz, Chunk.DEPTH);
        return nb.getLightMap().getSky(nlx, ly, nlz) / (float) LightMap.MAX_LIGHT;
    }

    private float aoTop  (Chunk c, World w, int lx, int ly, int lz, int dx, int dz) { return LightMap.ao(solid(c,w,lx+dx,ly+1,lz),    solid(c,w,lx,ly+1,lz+dz),    solid(c,w,lx+dx,ly+1,lz+dz)); }
    private float aoFront(Chunk c, World w, int lx, int ly, int lz, int dx, int dy) { return LightMap.ao(solid(c,w,lx+dx,ly,lz+1),    solid(c,w,lx,ly+dy,lz+1),    solid(c,w,lx+dx,ly+dy,lz+1)); }
    private float aoBack (Chunk c, World w, int lx, int ly, int lz, int dx, int dy) { return LightMap.ao(solid(c,w,lx+dx,ly,lz-1),    solid(c,w,lx,ly+dy,lz-1),    solid(c,w,lx+dx,ly+dy,lz-1)); }
    private float aoRight(Chunk c, World w, int lx, int ly, int lz, int dz, int dy) { return LightMap.ao(solid(c,w,lx+1,ly,lz+dz),    solid(c,w,lx+1,ly+dy,lz),    solid(c,w,lx+1,ly+dy,lz+dz)); }
    private float aoLeft (Chunk c, World w, int lx, int ly, int lz, int dz, int dy) { return LightMap.ao(solid(c,w,lx-1,ly,lz+dz),    solid(c,w,lx-1,ly+dy,lz),    solid(c,w,lx-1,ly+dy,lz+dz)); }

    private boolean solid(Chunk chunk, World world, int lx, int ly, int lz) {
        if (ly < 0 || ly >= Chunk.HEIGHT) return false;
        if (lx >= 0 && lx < Chunk.WIDTH && lz >= 0 && lz < Chunk.DEPTH)
            return chunk.getBlock(lx, ly, lz).isOpaque();
        return world.getBlock(chunk.cx * Chunk.WIDTH + lx, ly, chunk.cz * Chunk.DEPTH + lz).isOpaque();
    }

    private boolean isTransparent(Chunk chunk, World world, int lx, int ly, int lz) {
        if (ly < 0 || ly >= Chunk.HEIGHT) return true;
        if (lx < 0 || lx >= Chunk.WIDTH || lz < 0 || lz >= Chunk.DEPTH)
            return !world.getBlock(chunk.cx * Chunk.WIDTH + lx, ly, chunk.cz * Chunk.DEPTH + lz).isOpaque();
        return !chunk.getBlock(lx, ly, lz).isOpaque();
    }

    private void upload(List<Float> verts) {
        float[] data = new float[verts.size()];
        for (int i = 0; i < data.length; i++) data[i] = verts.get(i);
        vertexCount = data.length / 7;
        if (vao == -1) { vao = glGenVertexArrays(); vbo = glGenBuffers(); }
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);
        int stride = 7 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);               glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L*Float.BYTES);   glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5L*Float.BYTES);   glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 6L*Float.BYTES);   glEnableVertexAttribArray(3);
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

    private static void addV(List<Float> v, float[] d) { for (float f : d) v.add(f); }
}