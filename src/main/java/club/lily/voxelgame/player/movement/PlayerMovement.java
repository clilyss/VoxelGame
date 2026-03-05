package club.lily.voxelgame.player.movement;

import club.lily.voxelgame.engine.camera.Camera;
import club.lily.voxelgame.engine.window.Window;
import club.lily.voxelgame.world.World;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class PlayerMovement {

    public static final float EYE_HEIGHT = 1.65f;

    private static final float WALK_SPEED  = 8.0f;
    private static final float GRAVITY     = -28.0f;
    private static final float JUMP_FORCE  =  9.0f;
    private static final float HALF_W      =  0.30f;

    private final Camera camera;
    private final World  world;

    private float   velY     = 0;
    private boolean onGround = false;

    public PlayerMovement(Camera camera, World world) {
        this.camera = camera;
        this.world  = world;
    }

    public void update(Window window, float dt) {
        long win = window.getHandle();
        Vector3f pos = camera.getPosition();

        Vector3f front = flat(camera.getFront());
        Vector3f right = flat(camera.getRight());
        Vector3f move  = new Vector3f();

        if (key(win, GLFW_KEY_W)) move.add(front);
        if (key(win, GLFW_KEY_S)) move.sub(front);
        if (key(win, GLFW_KEY_D)) move.add(right);
        if (key(win, GLFW_KEY_A)) move.sub(right);
        if (move.lengthSquared() > 0) move.normalize().mul(WALK_SPEED * dt);

        velY += GRAVITY * dt;
        if (onGround && key(win, GLFW_KEY_SPACE)) { velY = JUMP_FORCE; onGround = false; }
        if (key(win, GLFW_KEY_F)) velY = WALK_SPEED;

        float nx = pos.x + move.x;
        float ny = pos.y + velY * dt;
        float nz = pos.z + move.z;

        float feetY = ny - EYE_HEIGHT;
        if (velY < 0) {
            if (solidAtFeet(nx, feetY, nz)) {
                ny = (float)(Math.floor(feetY) + 1) + EYE_HEIGHT;
                velY = 0; onGround = true;
            } else { onGround = false; }
        } else {
            if (solidAt((int)Math.floor(nx), (int)Math.floor(ny), (int)Math.floor(nz))) {
                ny = (float)Math.floor(ny) - 0.01f; velY = 0;
            }
            onGround = false;
        }

        float fy = ny - EYE_HEIGHT;
        if (move.x != 0) {
            float cx = nx + Math.signum(move.x) * HALF_W;
            if (solidColumn((int)Math.floor(cx), fy, (int)Math.floor(nz))) nx = pos.x;
        }
        if (move.z != 0) {
            float cz = nz + Math.signum(move.z) * HALF_W;
            if (solidColumn((int)Math.floor(nx), fy, (int)Math.floor(cz))) nz = pos.z;
        }

        camera.setPosition(nx, ny, nz);
    }

    private boolean solidColumn(int bx, float feetY, int bz) {
        return solidAt(bx, (int)Math.floor(feetY + 0.1f), bz)
            || solidAt(bx, (int)Math.floor(feetY + 1.0f), bz);
    }

    private boolean solidAtFeet(float x, float fy, float z) {
        int by = (int) Math.floor(fy);
        return solidAt((int)Math.floor(x-HALF_W), by, (int)Math.floor(z-HALF_W))
            || solidAt((int)Math.floor(x+HALF_W), by, (int)Math.floor(z-HALF_W))
            || solidAt((int)Math.floor(x-HALF_W), by, (int)Math.floor(z+HALF_W))
            || solidAt((int)Math.floor(x+HALF_W), by, (int)Math.floor(z+HALF_W));
    }

    private boolean solidAt(int bx, int by, int bz) { return world.getBlock(bx, by, bz).isSolid(); }

    private static boolean key(long win, int code) { return glfwGetKey(win, code) == GLFW_PRESS; }

    private static Vector3f flat(Vector3f v) {
        Vector3f r = new Vector3f(v.x, 0, v.z);
        return r.lengthSquared() > 0 ? r.normalize() : r;
    }
}
