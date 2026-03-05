package club.lily.voxelgame.engine.shader;

public final class ShaderSource {

    private ShaderSource() {}

    public static final String VERT = """
        #version 330 core
        layout(location = 0) in vec3  aPos;
        layout(location = 1) in vec2  aUV;
        layout(location = 2) in float aLight;
        layout(location = 3) in float aAO;

        out vec2  vUV;
        out float vLight;
        out float vAO;
        out float vFogFactor;

        uniform mat4 uView;
        uniform mat4 uProjection;

        void main() {
            vec4 viewPos  = uView * vec4(aPos, 1.0);
            gl_Position   = uProjection * viewPos;
            vUV           = aUV;
            vLight        = aLight;
            vAO           = aAO;
            float fog     = length(viewPos.xyz) * 0.007;
            vFogFactor    = clamp(exp(-fog * fog), 0.0, 1.0);
        }
        """;

    public static final String FRAG = """
        #version 330 core
        in vec2  vUV;
        in float vLight;
        in float vAO;
        in float vFogFactor;

        out vec4 FragColor;

        uniform sampler2D uAtlas;
        uniform vec3  uSkyColor;
        uniform float uAmbient;
        uniform float uSunStrength;

        void main() {
            vec4 tex = texture(uAtlas, vUV);
            if (tex.a < 0.1) discard;

            vec3 albedo    = pow(max(tex.rgb, vec3(0.001)), vec3(2.2));
            vec3 skyLinear = pow(max(uSkyColor, vec3(0.001)), vec3(2.2));

            vec3 sunlight   = vec3(1.00, 0.97, 0.90) * (uSunStrength * 0.92);
            vec3 ambientCol = vec3(uAmbient) + skyLinear * uAmbient * 0.20;

            float aoFactor  = mix(0.84, 1.0, vAO);

            vec3 lit     = albedo * ((sunlight * vLight * aoFactor) + ambientCol);
            vec3 blended = mix(skyLinear, lit, vFogFactor);

            blended = blended / (blended + vec3(1.0));

            vec3 out_ = pow(max(blended, vec3(0.001)), vec3(1.0 / 2.2));
            FragColor = vec4(out_, tex.a);
        }
        """;
}