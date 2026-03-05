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
            vec4 viewPos = uView * vec4(aPos, 1.0);
            gl_Position  = uProjection * viewPos;
            vUV          = aUV;
            vLight       = aLight;
            vAO          = aAO;

            float dist   = length(viewPos.xyz);
            float fog    = dist * 0.006;
            vFogFactor   = clamp(exp(-fog * fog), 0.0, 1.0);
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

        vec3 toLinear(vec3 c) { return c * (c * (c * 0.305306011 + 0.682171111) + 0.012522878); }
        vec3 toSRGB(vec3 c)   { return max(vec3(1.055) * pow(max(c, vec3(0.0)), vec3(0.41666)) - vec3(0.055), vec3(0.0)); }

        void main() {
            vec4 tex = texture(uAtlas, vUV);
            if (tex.a < 0.5) discard;

            vec3 albedo    = toLinear(tex.rgb);
            vec3 skyLinear = toLinear(uSkyColor);

            float lightLevel = max(vLight, uAmbient * 0.6);

            vec3 sunlight   = vec3(1.00, 0.97, 0.90) * uSunStrength;
            vec3 ambientCol = vec3(uAmbient) + skyLinear * 0.15;

            float ao  = mix(0.75, 1.0, vAO);
            vec3  lit = albedo * (sunlight * lightLevel + ambientCol * ao);

            vec3 blended = mix(skyLinear, lit, vFogFactor);
            blended      = blended / (blended + vec3(1.0));

            FragColor = vec4(toSRGB(blended), tex.a);
        }
        """;
}