package club.lily.voxelgame.world.chunk;

import club.lily.voxelgame.world.World;
import club.lily.voxelgame.world.block.BlockType;
import club.lily.voxelgame.world.light.LightMap;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL33.*;

public class ChunkMesh {

    private static final float MIN_LIGHT = 0.15f;
    private static final int MAX_VERTS = Chunk.WIDTH * Chunk.HEIGHT * Chunk.DEPTH * 6 * 6 * 7;

    private static final ThreadLocal<float[]> THREAD_BUFFER = ThreadLocal.withInitial(() -> new float[MAX_VERTS]);

    private volatile float[] pendingBuffer = null;
    private volatile int pendingSize = 0;

    private int lastAllocatedSize = 0;
    private int vao = -1, vbo = -1;
    private int vertexCount = 0;

    public final AtomicBoolean building = new AtomicBoolean(false);

    public void buildGeometry(Chunk chunk, World world) {
        int pos = 0;
        int ox = chunk.cx * Chunk.WIDTH, oz = chunk.cz * Chunk.DEPTH;

        Chunk nX = world.getChunkAt((chunk.cx + 1) * Chunk.WIDTH, chunk.cz * Chunk.DEPTH);
        Chunk pX = world.getChunkAt((chunk.cx - 1) * Chunk.WIDTH, chunk.cz * Chunk.DEPTH);
        Chunk nZ = world.getChunkAt(chunk.cx * Chunk.WIDTH, (chunk.cz + 1) * Chunk.DEPTH);
        Chunk pZ = world.getChunkAt(chunk.cx * Chunk.WIDTH, (chunk.cz - 1) * Chunk.DEPTH);

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    BlockType b = chunk.getBlock(x, y, z);
                    if (b == BlockType.AIR) continue;
                    float wx = ox + x, wy = y, wz = oz + z;
                    if (isTransparent(chunk,nX,pX,nZ,pZ,x,y+1,z)) pos=emitTop   (pos,chunk,nX,pX,nZ,pZ,x,y,z,wx,wy,wz,b.tileTop,   1.00f);
                    if (isTransparent(chunk,nX,pX,nZ,pZ,x,y-1,z)) pos=emitBottom(pos,chunk,nX,pX,nZ,pZ,x,y,z,wx,wy,wz,b.tileBottom,0.50f);
                    if (isTransparent(chunk,nX,pX,nZ,pZ,x,y,z+1)) pos=emitFront (pos,chunk,nX,pX,nZ,pZ,x,y,z,wx,wy,wz,b.tileSide,  0.82f);
                    if (isTransparent(chunk,nX,pX,nZ,pZ,x,y,z-1)) pos=emitBack  (pos,chunk,nX,pX,nZ,pZ,x,y,z,wx,wy,wz,b.tileSide,  0.82f);
                    if (isTransparent(chunk,nX,pX,nZ,pZ,x+1,y,z)) pos=emitRight (pos,chunk,nX,pX,nZ,pZ,x,y,z,wx,wy,wz,b.tileSide,  0.65f);
                    if (isTransparent(chunk,nX,pX,nZ,pZ,x-1,y,z)) pos=emitLeft  (pos,chunk,nX,pX,nZ,pZ,x,y,z,wx,wy,wz,b.tileSide,  0.65f);
                }
            }
        }

        float[] snap = new float[pos];
        System.arraycopy(THREAD_BUFFER.get(), 0, snap, 0, pos);
        pendingBuffer = snap;
        pendingSize   = pos;
        building.set(false);
    }

    public boolean uploadIfReady() {
        float[] buf = pendingBuffer;
        if (buf == null) return false;
        pendingBuffer = null;
        int size = pendingSize;
        vertexCount = size / 7;
        if (vao == -1) { vao = glGenVertexArrays(); vbo = glGenBuffers(); }
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        if (size <= lastAllocatedSize) {
            glBufferSubData(GL_ARRAY_BUFFER, 0L, buf);
        } else {
            glBufferData(GL_ARRAY_BUFFER, buf, GL_DYNAMIC_DRAW);
            lastAllocatedSize = size;
        }
        int stride = 7 * Float.BYTES;
        glVertexAttribPointer(0,3,GL_FLOAT,false,stride,0L);              glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,2,GL_FLOAT,false,stride,3L*Float.BYTES);  glEnableVertexAttribArray(1);
        glVertexAttribPointer(2,1,GL_FLOAT,false,stride,5L*Float.BYTES);  glEnableVertexAttribArray(2);
        glVertexAttribPointer(3,1,GL_FLOAT,false,stride,6L*Float.BYTES);  glEnableVertexAttribArray(3);
        glBindVertexArray(0);
        return true;
    }

    private int put(int pos,float x,float y,float z,float u,float v,float light,float ao) {
        float[] b=THREAD_BUFFER.get(); b[pos++]=x; b[pos++]=y; b[pos++]=z;
        b[pos++]=u; b[pos++]=v;
        b[pos++]=light; b[pos++]=ao;
        return pos;
    }

    private int emitTop(int pos,Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,
                        int lx,int ly,int lz,float wx,float wy,float wz,int tileCol,float dir) {
        float u0=tileCol/16f,v0=0f,du=BlockType.tileU(),dv=BlockType.tileV(),x=wx,y=wy,z=wz;
        float l00=skyAt(chunk,nX,pX,nZ,pZ,lx,  ly+1,lz+1)*dir; float ao00=aoTop(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,+1);
        float l10=skyAt(chunk,nX,pX,nZ,pZ,lx+1,ly+1,lz+1)*dir; float ao10=aoTop(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,+1);
        float l11=skyAt(chunk,nX,pX,nZ,pZ,lx+1,ly+1,lz  )*dir; float ao11=aoTop(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,-1);
        float l01=skyAt(chunk,nX,pX,nZ,pZ,lx,  ly+1,lz  )*dir; float ao01=aoTop(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,-1);
        if ((ao00+ao11)<(ao10+ao01)) {
            pos=put(pos,x,  y+1,z+1,u0,   v0+dv,l10,ao10); pos=put(pos,x+1,y+1,z+1,u0+du,v0+dv,l11,ao11);
            pos=put(pos,x+1,y+1,z,  u0+du,v0,   l11,ao11); pos=put(pos,x,  y+1,z+1,u0,   v0+dv,l10,ao10);
            pos=put(pos,x+1,y+1,z,  u0+du,v0,   l11,ao11); pos=put(pos,x,  y+1,z,  u0,   v0,   l01,ao01);
        } else {
            pos=put(pos,x,  y+1,z+1,u0,   v0+dv,l00,ao00); pos=put(pos,x+1,y+1,z+1,u0+du,v0+dv,l10,ao10);
            pos=put(pos,x+1,y+1,z,  u0+du,v0,   l11,ao11); pos=put(pos,x,  y+1,z+1,u0,   v0+dv,l00,ao00);
            pos=put(pos,x+1,y+1,z,  u0+du,v0,   l11,ao11); pos=put(pos,x,  y+1,z,  u0,   v0,   l01,ao01);
        }
        return pos;
    }

    private int emitBottom(int pos,Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,
                           int lx,int ly,int lz,float wx,float wy,float wz,int tileCol,float dir) {
        float u0=tileCol/16f,v0=0f,du=BlockType.tileU(),dv=BlockType.tileV(),x=wx,y=wy,z=wz;
        float l=skyAt(chunk,nX,pX,nZ,pZ,lx,ly-1,lz)*dir;
        pos=put(pos,x,  y,z,  u0,   v0,   l,1f); pos=put(pos,x+1,y,z,  u0+du,v0,   l,1f);
        pos=put(pos,x+1,y,z+1,u0+du,v0+dv,l,1f); pos=put(pos,x,  y,z,  u0,   v0,   l,1f);
        pos=put(pos,x+1,y,z+1,u0+du,v0+dv,l,1f); pos=put(pos,x,  y,z+1,u0,   v0+dv,l,1f);
        return pos;
    }

    private int emitFront(int pos,Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,
                          int lx,int ly,int lz,float wx,float wy,float wz,int tileCol,float dir) {
        float u0=tileCol/16f,v0=0f,du=BlockType.tileU(),dv=BlockType.tileV(),x=wx,y=wy,z=wz;
        float l=skyAt(chunk,nX,pX,nZ,pZ,lx,ly,lz+1)*dir;
        float ao0=aoFront(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,0),ao1=aoFront(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,0);
        float ao2=aoFront(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,+1),ao3=aoFront(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,+1);
        if ((ao0+ao2)<(ao1+ao3)) {
            pos=put(pos,x+1,y,  z+1,u0+du,v0+dv,l,ao1); pos=put(pos,x+1,y+1,z+1,u0+du,v0,   l,ao2);
            pos=put(pos,x,  y+1,z+1,u0,   v0,   l,ao3); pos=put(pos,x+1,y,  z+1,u0+du,v0+dv,l,ao1);
            pos=put(pos,x,  y+1,z+1,u0,   v0,   l,ao3); pos=put(pos,x,  y,  z+1,u0,   v0+dv,l,ao0);
        } else {
            pos=put(pos,x,  y,  z+1,u0,   v0+dv,l,ao0); pos=put(pos,x+1,y,  z+1,u0+du,v0+dv,l,ao1);
            pos=put(pos,x+1,y+1,z+1,u0+du,v0,   l,ao2); pos=put(pos,x,  y,  z+1,u0,   v0+dv,l,ao0);
            pos=put(pos,x+1,y+1,z+1,u0+du,v0,   l,ao2); pos=put(pos,x,  y+1,z+1,u0,   v0,   l,ao3);
        }
        return pos;
    }

    private int emitBack(int pos,Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,
                         int lx,int ly,int lz,float wx,float wy,float wz,int tileCol,float dir) {
        float u0=tileCol/16f,v0=0f,du=BlockType.tileU(),dv=BlockType.tileV(),x=wx,y=wy,z=wz;
        float l=skyAt(chunk,nX,pX,nZ,pZ,lx,ly,lz-1)*dir;
        float ao0=aoBack(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,0),ao1=aoBack(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,0);
        float ao2=aoBack(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,+1),ao3=aoBack(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,+1);
        if ((ao0+ao2)<(ao1+ao3)) {
            pos=put(pos,x,  y,  z,u0+du,v0+dv,l,ao1); pos=put(pos,x,  y+1,z,u0+du,v0,   l,ao2);
            pos=put(pos,x+1,y+1,z,u0,   v0,   l,ao3); pos=put(pos,x,  y,  z,u0+du,v0+dv,l,ao1);
            pos=put(pos,x+1,y+1,z,u0,   v0,   l,ao3); pos=put(pos,x+1,y,  z,u0,   v0+dv,l,ao0);
        } else {
            pos=put(pos,x+1,y,  z,u0,   v0+dv,l,ao0); pos=put(pos,x,  y,  z,u0+du,v0+dv,l,ao1);
            pos=put(pos,x,  y+1,z,u0+du,v0,   l,ao2); pos=put(pos,x+1,y,  z,u0,   v0+dv,l,ao0);
            pos=put(pos,x,  y+1,z,u0+du,v0,   l,ao2); pos=put(pos,x+1,y+1,z,u0,   v0,   l,ao3);
        }
        return pos;
    }

    private int emitRight(int pos,Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,
                          int lx,int ly,int lz,float wx,float wy,float wz,int tileCol,float dir) {
        float u0=tileCol/16f,v0=0f,du=BlockType.tileU(),dv=BlockType.tileV(),x=wx,y=wy,z=wz;
        float l=skyAt(chunk,nX,pX,nZ,pZ,lx+1,ly,lz)*dir;
        float ao0=aoRight(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,0),ao1=aoRight(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,0);
        float ao2=aoRight(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,+1),ao3=aoRight(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,+1);
        if ((ao0+ao2)<(ao1+ao3)) {
            pos=put(pos,x+1,y,  z,  u0+du,v0+dv,l,ao1); pos=put(pos,x+1,y+1,z,  u0+du,v0,   l,ao2);
            pos=put(pos,x+1,y+1,z+1,u0,   v0,   l,ao3); pos=put(pos,x+1,y,  z,  u0+du,v0+dv,l,ao1);
            pos=put(pos,x+1,y+1,z+1,u0,   v0,   l,ao3); pos=put(pos,x+1,y,  z+1,u0,   v0+dv,l,ao0);
        } else {
            pos=put(pos,x+1,y,  z+1,u0,   v0+dv,l,ao0); pos=put(pos,x+1,y,  z,  u0+du,v0+dv,l,ao1);
            pos=put(pos,x+1,y+1,z,  u0+du,v0,   l,ao2); pos=put(pos,x+1,y,  z+1,u0,   v0+dv,l,ao0);
            pos=put(pos,x+1,y+1,z,  u0+du,v0,   l,ao2); pos=put(pos,x+1,y+1,z+1,u0,   v0,   l,ao3);
        }
        return pos;
    }

    private int emitLeft(int pos,Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,
                         int lx,int ly,int lz,float wx,float wy,float wz,int tileCol,float dir) {
        float u0=tileCol/16f,v0=0f,du=BlockType.tileU(),dv=BlockType.tileV(),x=wx,y=wy,z=wz;
        float l=skyAt(chunk,nX,pX,nZ,pZ,lx-1,ly,lz)*dir;
        float ao0=aoLeft(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,0),ao1=aoLeft(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,0);
        float ao2=aoLeft(chunk,nX,pX,nZ,pZ,lx,ly,lz,+1,+1),ao3=aoLeft(chunk,nX,pX,nZ,pZ,lx,ly,lz,-1,+1);
        if ((ao0+ao2)<(ao1+ao3)) {
            pos=put(pos,x,y,  z+1,u0+du,v0+dv,l,ao1); pos=put(pos,x,y+1,z+1,u0+du,v0,   l,ao2);
            pos=put(pos,x,y+1,z,  u0,   v0,   l,ao3); pos=put(pos,x,y,  z+1,u0+du,v0+dv,l,ao1);
            pos=put(pos,x,y+1,z,  u0,   v0,   l,ao3); pos=put(pos,x,y,  z,  u0,   v0+dv,l,ao0);
        } else {
            pos=put(pos,x,y,  z,  u0,   v0+dv,l,ao0); pos=put(pos,x,y,  z+1,u0+du,v0+dv,l,ao1);
            pos=put(pos,x,y+1,z+1,u0+du,v0,   l,ao2); pos=put(pos,x,y,  z,  u0,   v0+dv,l,ao0);
            pos=put(pos,x,y+1,z+1,u0+du,v0,   l,ao2); pos=put(pos,x,y+1,z,  u0,   v0,   l,ao3);
        }
        return pos;
    }

    private float skyAt(Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz) {
        if (ly<0) return MIN_LIGHT;
        if (ly>=Chunk.HEIGHT) return 1.0f;
        float v=rawSky(chunk,nX,pX,nZ,pZ,lx,ly,lz);
        if (v>MIN_LIGHT) return v;
        float best=MIN_LIGHT;
        best=Math.max(best,rawSky(chunk,nX,pX,nZ,pZ,lx-1,ly,lz));
        best=Math.max(best,rawSky(chunk,nX,pX,nZ,pZ,lx+1,ly,lz));
        best=Math.max(best,rawSky(chunk,nX,pX,nZ,pZ,lx,ly,lz-1));
        best=Math.max(best,rawSky(chunk,nX,pX,nZ,pZ,lx,ly,lz+1));
        best=Math.max(best,rawSky(chunk,nX,pX,nZ,pZ,lx,ly+1,lz));
        return best;
    }

    private float rawSky(Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz) {
        if (ly<0||ly>=Chunk.HEIGHT) return ly>=Chunk.HEIGHT?1.0f:MIN_LIGHT;
        if (lx>=0&&lx<Chunk.WIDTH&&lz>=0&&lz<Chunk.DEPTH) {
            LightMap lm=chunk.getLightMap();
            return lm==null?1.0f:lm.getSky(lx,ly,lz)/(float)LightMap.MAX_LIGHT;
        }
        int nlx=lx,nlz=lz; Chunk nb;
        if      (lx<0          &&lz>=0&&lz<Chunk.DEPTH)  { nb=pX; nlx=lx+Chunk.WIDTH; }
        else if (lx>=Chunk.WIDTH&&lz>=0&&lz<Chunk.DEPTH) { nb=nX; nlx=lx-Chunk.WIDTH; }
        else if (lz<0          &&lx>=0&&lx<Chunk.WIDTH)  { nb=pZ; nlz=lz+Chunk.DEPTH; }
        else if (lz>=Chunk.DEPTH&&lx>=0&&lx<Chunk.WIDTH) { nb=nZ; nlz=lz-Chunk.DEPTH; }
        else return 1.0f;
        if (nb==null||nb.getLightMap()==null) return 1.0f;
        return nb.getLightMap().getSky(nlx,ly,nlz)/(float)LightMap.MAX_LIGHT;
    }

    private float aoTop  (Chunk c,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz,int dx,int dz) {
        return LightMap.ao(solid(c,nX,pX,nZ,pZ,lx+dx,ly+1,lz),solid(c,nX,pX,nZ,pZ,lx,ly+1,lz+dz),solid(c,nX,pX,nZ,pZ,lx+dx,ly+1,lz+dz));
    }
    private float aoFront(Chunk c,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz,int dx,int dy) {
        return LightMap.ao(solid(c,nX,pX,nZ,pZ,lx+dx,ly,lz+1),solid(c,nX,pX,nZ,pZ,lx,ly+dy,lz+1),solid(c,nX,pX,nZ,pZ,lx+dx,ly+dy,lz+1));
    }
    private float aoBack (Chunk c,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz,int dx,int dy) {
        return LightMap.ao(solid(c,nX,pX,nZ,pZ,lx+dx,ly,lz-1),solid(c,nX,pX,nZ,pZ,lx,ly+dy,lz-1),solid(c,nX,pX,nZ,pZ,lx+dx,ly+dy,lz-1));
    }
    private float aoRight(Chunk c,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz,int dz,int dy) {
        return LightMap.ao(solid(c,nX,pX,nZ,pZ,lx+1,ly,lz+dz),solid(c,nX,pX,nZ,pZ,lx+1,ly+dy,lz),solid(c,nX,pX,nZ,pZ,lx+1,ly+dy,lz+dz));
    }
    private float aoLeft (Chunk c,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz,int dz,int dy) {
        return LightMap.ao(solid(c,nX,pX,nZ,pZ,lx-1,ly,lz+dz),solid(c,nX,pX,nZ,pZ,lx-1,ly+dy,lz),solid(c,nX,pX,nZ,pZ,lx-1,ly+dy,lz+dz));
    }

    private BlockType neighborBlock(Chunk chunk,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz) {
        if (ly<0||ly>=Chunk.HEIGHT) return BlockType.AIR;
        if (lx>=0&&lx<Chunk.WIDTH&&lz>=0&&lz<Chunk.DEPTH) return chunk.getBlock(lx,ly,lz);
        int nlx=lx,nlz=lz; Chunk nb;
        if      (lx<0          &&lz>=0&&lz<Chunk.DEPTH)  { nb=pX; nlx=lx+Chunk.WIDTH; }
        else if (lx>=Chunk.WIDTH&&lz>=0&&lz<Chunk.DEPTH) { nb=nX; nlx=lx-Chunk.WIDTH; }
        else if (lz<0          &&lx>=0&&lx<Chunk.WIDTH)  { nb=pZ; nlz=lz+Chunk.DEPTH; }
        else if (lz>=Chunk.DEPTH&&lx>=0&&lx<Chunk.WIDTH) { nb=nZ; nlz=lz-Chunk.DEPTH; }
        else return BlockType.AIR;
        if (nb==null) return BlockType.AIR;
        return nb.getBlock(nlx,ly,nlz);
    }

    private boolean solid(Chunk c,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz) {
        return neighborBlock(c,nX,pX,nZ,pZ,lx,ly,lz).isOpaque();
    }
    private boolean isTransparent(Chunk c,Chunk nX,Chunk pX,Chunk nZ,Chunk pZ,int lx,int ly,int lz) {
        if (ly<0||ly>=Chunk.HEIGHT) return true;
        return !neighborBlock(c,nX,pX,nZ,pZ,lx,ly,lz).isOpaque();
    }

    public void render() {
        if (vao==-1||vertexCount==0) return;
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES,0,vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (vao!=-1) { glDeleteVertexArrays(vao); glDeleteBuffers(vbo); vao=-1; }
    }
}