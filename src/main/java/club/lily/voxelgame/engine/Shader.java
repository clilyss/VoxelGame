package club.lily.voxelgame.engine;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;

public class Shader {

    private final int programId;

    private static final String VERT = """
        #version 330 core
        layout(location = 0) in vec3  aPos;
        layout(location = 1) in vec2  aUV;
        layout(location = 2) in float aLight;
        layout(location = 3) in float aAO;

        out vec2  vUV;
        out float vLight;
        out float vAO;
        out float vFogFactor;
        out float vHeight;

        uniform mat4 uView;
        uniform mat4 uProjection;

        void main() {
            vec4 viewPos  = uView * vec4(aPos, 1.0);
            gl_Position   = uProjection * viewPos;
            vUV        = aUV;
            vLight     = aLight;
            vAO        = aAO;
            vHeight    = aPos.y;
            float fog  = length(viewPos.xyz) * 0.007;
            vFogFactor = clamp(exp(-fog * fog), 0.0, 1.0);
        }
        """;

    private static final String FRAG = """
        #version 330 core
        in vec2  vUV;
        in float vLight;
        in float vAO;
        in float vFogFactor;
        in float vHeight;

        out vec4 FragColor;

        uniform sampler2D uAtlas;
        uniform vec3  uSkyColor;
        uniform float uAmbient;

        void main() {
            vec4 tex = texture(uAtlas, vUV);
            if (tex.a < 0.1) discard;

            vec3 albedo = pow(max(tex.rgb, vec3(0.001)), vec3(2.2));

            float skyL      = max(vLight, uAmbient);
            float lightSq   = skyL * skyL;

            float aoStrength = 1.0 - skyL;
            float aoFactor   = mix(1.0, mix(0.75, 1.0, vAO), aoStrength);

            float finalLight = max(lightSq * aoFactor, uAmbient * uAmbient);

            vec3 lit = albedo * finalLight;

            float heightFade = clamp(vHeight / 80.0, 0.0, 1.0);
            lit *= mix(0.85, 1.0, heightFade);

            vec3 fogLinear = pow(max(uSkyColor, vec3(0.001)), vec3(2.2));
            vec3 blended   = mix(fogLinear, lit, vFogFactor);

            vec3 out_ = pow(max(blended, vec3(0.001)), vec3(1.0 / 2.2));

            FragColor = vec4(clamp(out_, 0.0, 1.0), tex.a);
        }
        """;

    public Shader() {
        int vert = compile(GL_VERTEX_SHADER,   VERT);
        int frag = compile(GL_FRAGMENT_SHADER, FRAG);
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
