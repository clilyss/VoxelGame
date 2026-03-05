package club.lily.voxelgame.engine.shader;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;

public class Shader {

    private final int programId;

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
        try (MemoryStack stack = stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(glGetUniformLocation(programId, name), false, fb);
        }
    }

    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(glGetUniformLocation(programId, name), x, y, z);
    }

    public void setFloat(String name, float v) {
        glUniform1f(glGetUniformLocation(programId, name), v);
    }

    public void setInt(String name, int v) {
        glUniform1i(glGetUniformLocation(programId, name), v);
    }

    public void cleanup() { glDeleteProgram(programId); }
}
