package club.lily.voxelgame.engine.texture;

import club.lily.voxelgame.assets.TextureGen;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

public class AtlasTexture {

    private final int textureId;

    public AtlasTexture() {
        BufferedImage img = TextureGen.generate();
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);

        ByteBuffer buf = memAlloc(w * h * 4);
        for (int px : pixels) {
            buf.put((byte)((px >> 16) & 0xFF));
            buf.put((byte)((px >>  8) & 0xFF));
            buf.put((byte)( px        & 0xFF));
            buf.put((byte)((px >> 24) & 0xFF));
        }
        buf.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);

        memFree(buf);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void cleanup() { glDeleteTextures(textureId); }
}
