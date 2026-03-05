package club.lily.voxelgame.player.interaction;

import club.lily.voxelgame.engine.camera.Camera;
import club.lily.voxelgame.engine.window.Window;
import club.lily.voxelgame.world.World;
import club.lily.voxelgame.world.block.BlockType;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class BlockInteraction {

    private static final float INTERACT_CD  = 0.25f;
    private static final float EYE_HEIGHT   = 1.65f;
    private static final float AABB_HEIGHT  = 1.80f;

    private final Camera camera;
    private final World  world;

    private BlockType heldBlock  = BlockType.STONE;
    private float     breakTimer = 0;
    private float     placeTimer = 0;

    public BlockInteraction(Camera camera, World world) {
        this.camera = camera;
        this.world  = world;
    }

    public void update(Window window, float dt) {
        breakTimer -= dt;
        placeTimer -= dt;
        long win = window.getHandle();
        if (glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_LEFT)  == GLFW_PRESS && breakTimer <= 0) { breakBlock(); breakTimer = INTERACT_CD; }
        if (glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS && placeTimer <= 0) { placeBlock(); placeTimer = INTERACT_CD; }
    }

    private void breakBlock() {
        int[] hit = Raycast.hit(camera, world);
        if (hit != null) world.setBlock(hit[0], hit[1], hit[2], BlockType.AIR);
    }

    private void placeBlock() {
        int[] hit = Raycast.adjacent(camera, world);
        if (hit == null) return;
        Vector3f pos = camera.getPosition();
        float px = pos.x, py = pos.y - EYE_HEIGHT, pz = pos.z;
        boolean insidePlayer =
            hit[0] == (int) Math.floor(px) &&
            hit[1] >= (int) Math.floor(py) && hit[1] <= (int) Math.floor(py + AABB_HEIGHT) &&
            hit[2] == (int) Math.floor(pz);
        if (!insidePlayer) world.setBlock(hit[0], hit[1], hit[2], heldBlock);
    }
}
