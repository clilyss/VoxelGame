package club.lily.voxelgame;

import club.lily.voxelgame.engine.AtlasTexture;
import club.lily.voxelgame.engine.Camera;
import club.lily.voxelgame.engine.Shader;
import club.lily.voxelgame.engine.Window;
import club.lily.voxelgame.player.Player;
import club.lily.voxelgame.world.World;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class Game {

    private static final int   WIDTH  = 1280;
    private static final int   HEIGHT = 720;
    private static final float FOV    = 70.0f;
    private static final float NEAR   = 0.1f;
    private static final float FAR    = 600.0f;

    private static final float[] SKY_DAY   = {0.53f, 0.81f, 0.98f};
    private static final float[] SKY_DAWN  = {0.95f, 0.55f, 0.25f};
    private static final float[] SKY_NIGHT = {0.02f, 0.03f, 0.08f};

    private Window       window;
    private Shader       shader;
    private Camera       camera;
    private World        world;
    private Player       player;
    private AtlasTexture atlas;

    private float timeOfDay = 0.25f;

    public void run() {
        window = new Window(WIDTH, HEIGHT, "VoxelGame  |  WASD=Move  Space=Jump  LMB=Break  RMB=Place  ESC=Quit");
        window.init();
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader = new Shader();
        atlas  = new AtlasTexture();
        camera = new Camera(WIDTH, HEIGHT, FOV, NEAR, FAR);
        world  = new World();

        System.out.println("Generating world...");
        world.generate();
        System.out.println("Building meshes...");
        world.buildAllMeshes();
        System.out.println("Ready!");

        player = new Player(camera, world);
        player.spawnOnTerrain(8, 8);

        shader.use();
        shader.setInt("uAtlas", 0);

        long lastTime = System.nanoTime();
        while (!window.shouldClose()) {
            long  now   = System.nanoTime();
            float delta = Math.min((now - lastTime) / 1_000_000_000.0f, 0.05f);
            lastTime = now;

            if (glfwGetKey(window.getHandle(), GLFW_KEY_ESCAPE) == GLFW_PRESS)
                glfwSetWindowShouldClose(window.getHandle(), true);

            float timeSpeed = glfwGetKey(window.getHandle(), GLFW_KEY_T) == GLFW_PRESS ? 0.05f : 0.0008f;
            timeOfDay = (timeOfDay + delta * timeSpeed) % 1.0f;

            player.update(window, delta);

            org.joml.Vector3f pos = camera.getPosition();
            world.updateAroundPlayer(pos.x, pos.z);

            float[] sky = skyColor(timeOfDay);
            float ambient = ambientLevel(timeOfDay);
            glClearColor(sky[0], sky[1], sky[2], 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            shader.use();
            atlas.bind(0);
            shader.setMatrix4f("uView",       camera.getViewMatrix());
            shader.setMatrix4f("uProjection", camera.getProjectionMatrix());
            shader.setVec3("uSkyColor",       sky[0], sky[1], sky[2]);
            shader.setFloat("uAmbient",       ambient);

            world.render(shader);
            window.swapAndPoll();
        }

        shader.cleanup();
        atlas.cleanup();
        world.cleanup();
        window.cleanup();
    }

    private float[] skyColor(float t) {
        float[] result = new float[3];
        if (t < 0.2f) {
            lerp3(result, SKY_NIGHT, SKY_DAWN, t / 0.2f);
        } else if (t < 0.3f) {
            lerp3(result, SKY_DAWN, SKY_DAY, (t - 0.2f) / 0.1f);
        } else if (t < 0.7f) {
            result[0] = SKY_DAY[0]; result[1] = SKY_DAY[1]; result[2] = SKY_DAY[2];
        } else if (t < 0.8f) {
            lerp3(result, SKY_DAY, SKY_DAWN, (t - 0.7f) / 0.1f);
        } else {
            lerp3(result, SKY_DAWN, SKY_NIGHT, (t - 0.8f) / 0.2f);
        }
        return result;
    }

    private float ambientLevel(float t) {
        if (t < 0.2f)      return lerp(0.05f, 0.08f, t / 0.2f);
        else if (t < 0.3f) return lerp(0.08f, 0.15f, (t - 0.2f) / 0.1f);
        else if (t < 0.7f) return 0.15f;
        else if (t < 0.8f) return lerp(0.15f, 0.08f, (t - 0.7f) / 0.1f);
        else               return lerp(0.08f, 0.05f, (t - 0.8f) / 0.2f);
    }

    private static void lerp3(float[] out, float[] a, float[] b, float t) {
        out[0] = a[0] + (b[0] - a[0]) * t;
        out[1] = a[1] + (b[1] - a[1]) * t;
        out[2] = a[2] + (b[2] - a[2]) * t;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
