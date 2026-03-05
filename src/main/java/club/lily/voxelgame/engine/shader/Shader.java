package club.lily.voxelgame.engine.shader;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;

public class Shader {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public Shader() {
        int vert = compile(GL_VERTEX_SHADER,   ShaderSource.VERT);
        int frag = compile(GL_FRAGMENT_SHADER, ShaderSource.FRAG);
        programId = glCreateProgram();
        glAttachShader(programId, vert);
        glAttachShader(programId, frag);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader link:\n" + glGetProgramInfoLog(programId));
        glDeleteShader(vert);
        glDeleteShader(frag);
        cacheUniforms();
    }

    private void cacheUniforms() {
        int count = glGetProgrami(programId, GL_ACTIVE_UNIFORMS);
        try (MemoryStack stack = stackPush()) {
            java.nio.IntBuffer size = stack.mallocInt(1);
            java.nio.IntBuffer type = stack.mallocInt(1);
            for (int i = 0; i < count; i++) {
                String name = glGetActiveUniform(programId, i, size, type);
                uniformCache.put(name, glGetUniformLocation(programId, name));
            }
        }
    }

    private int loc(String name) {
        return uniformCache.getOrDefault(name, -1);
    }

    private static int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader compile:\n" + glGetShaderInfoLog(id));
        return id;
    }

    public void use() { glUseProgram(programId); }

    public void setMatrix4f(String name, Matrix4f m) {
        int l = loc(name); if (l == -1) return;
        try (MemoryStack stack = stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(l, false, fb);
        }
    }

    public void setVec3(String name, float x, float y, float z) {
        int l = loc(name); if (l == -1) return;
        glUniform3f(l, x, y, z);
    }

    public void setFloat(String name, float v) {
        int l = loc(name); if (l == -1) return;
        glUniform1f(l, v);
    }

    public void setInt(String name, int v) {
        int l = loc(name); if (l == -1) return;
        glUniform1i(l, v);
    }

    public void cleanup() { glDeleteProgram(programId); }
}