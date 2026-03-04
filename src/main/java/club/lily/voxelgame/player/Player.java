package club.lily.voxelgame.player;

import club.lily.voxelgame.engine.Camera;
import club.lily.voxelgame.engine.Window;
import org.joml.Vector3f;
import club.lily.voxelgame.world.BlockType;
import club.lily.voxelgame.world.Chunk;
import club.lily.voxelgame.world.World;

import static org.lwjgl.glfw.GLFW.*;

public class Player {

    
    private static final float WALK_SPEED   = 8.0f;
    private static final float GRAVITY      = -28.0f;
    private static final float JUMP_FORCE   =  9.0f;
    private static final float EYE_HEIGHT   =  1.65f;   
    private static final float HALF_W       =  0.30f;   
    private static final float AABB_HEIGHT  =  1.80f;

    
    private static final float REACH        =  5.0f;
    private static final float RAY_STEP     =  0.04f;
    private static final float INTERACT_CD  =  0.25f;   

    private final Camera camera;
    private final World  world;

    private float velY        = 0;
    private boolean onGround  = false;

    private float breakTimer = 0;
    private float placeTimer = 0;

    
    private BlockType heldBlock = BlockType.STONE;

    public Player(Camera camera, World world) {
        this.camera = camera;
        this.world  = world;
    }

    

    
    public void spawnOnTerrain(int wx, int wz) {
        int topY = world.getTopY(wx, wz);
        camera.setPosition(wx + 0.5f, topY + 1 + EYE_HEIGHT, wz + 0.5f);
    }

    

    public void update(Window window, float dt) {
        camera.rotate(window.getMouseDX(), window.getMouseDY());
        move(window, dt);
        interact(window, dt);
    }

    

    private void move(Window window, float dt) {
        long win = window.getHandle();
        Vector3f pos = camera.getPosition();

        
        Vector3f front = flatDir(camera.getFront());
        Vector3f right = flatDir(camera.getRight());
        Vector3f move  = new Vector3f();

        if (key(win, GLFW_KEY_W)) move.add(front);
        if (key(win, GLFW_KEY_S)) move.sub(front);
        if (key(win, GLFW_KEY_D)) move.add(right);
        if (key(win, GLFW_KEY_A)) move.sub(right);
        if (move.lengthSquared() > 0) move.normalize().mul(WALK_SPEED * dt);

        
        velY += GRAVITY * dt;
        if (onGround && key(win, GLFW_KEY_SPACE)) {
            velY = JUMP_FORCE;
            onGround = false;
        }
        
        if (key(win, GLFW_KEY_F)) { velY = WALK_SPEED; }

        
        float nx = pos.x + move.x;
        float ny = pos.y + velY * dt;
        float nz = pos.z + move.z;

        
        float feetY = ny - EYE_HEIGHT;
        if (velY < 0) {
            
            if (solidAtFeet(nx, feetY, nz)) {
                ny = (float)(Math.floor(feetY) + 1) + EYE_HEIGHT;
                velY = 0;
                onGround = true;
            } else {
                onGround = false;
            }
        } else {
            
            if (solidAt((int)Math.floor(nx), (int)Math.floor(ny), (int)Math.floor(nz))) {
                ny = (float)Math.floor(ny) - 0.01f;
                velY = 0;
            }
            onGround = false;
        }

        
        float fy = ny - EYE_HEIGHT;
        if (move.x != 0) {
            float checkX = nx + Math.signum(move.x) * HALF_W;
            if (solidAtColumn((int)Math.floor(checkX), fy, (int)Math.floor(nz))) nx = pos.x;
        }

        
        if (move.z != 0) {
            float checkZ = nz + Math.signum(move.z) * HALF_W;
            if (solidAtColumn((int)Math.floor(nx), fy, (int)Math.floor(checkZ))) nz = pos.z;
        }

        camera.setPosition(nx, ny, nz);
    }

    
    private boolean solidAtColumn(int bx, float feetY, int bz) {
        return solidAt(bx, (int) Math.floor(feetY + 0.1f), bz)
            || solidAt(bx, (int) Math.floor(feetY + 1.0f), bz);
    }

    private boolean solidAtFeet(float x, float feetY, float z) {
        int by = (int) Math.floor(feetY);
        return solidAt((int) Math.floor(x - HALF_W), by, (int) Math.floor(z - HALF_W))
            || solidAt((int) Math.floor(x + HALF_W), by, (int) Math.floor(z - HALF_W))
            || solidAt((int) Math.floor(x - HALF_W), by, (int) Math.floor(z + HALF_W))
            || solidAt((int) Math.floor(x + HALF_W), by, (int) Math.floor(z + HALF_W));
    }

    private boolean solidAt(int bx, int by, int bz) {
        return world.getBlock(bx, by, bz).isSolid();
    }

    

    private void interact(Window window, float dt) {
        breakTimer -= dt;
        placeTimer -= dt;

        long win = window.getHandle();

        if (glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && breakTimer <= 0) {
            breakBlock();
            breakTimer = INTERACT_CD;
        }
        if (glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS && placeTimer <= 0) {
            placeBlock();
            placeTimer = INTERACT_CD;
        }
    }

    
    private int[] raycast(boolean returnPrevious) {
        Vector3f origin = camera.getPosition();
        Vector3f dir    = new Vector3f(camera.getFront()).normalize();

        int[] prev = null;
        for (float d = 0; d < REACH; d += RAY_STEP) {
            int bx = (int) Math.floor(origin.x + dir.x * d);
            int by = (int) Math.floor(origin.y + dir.y * d);
            int bz = (int) Math.floor(origin.z + dir.z * d);

            if (world.getBlock(bx, by, bz).isSolid()) {
                return returnPrevious ? prev : new int[]{bx, by, bz};
            }
            prev = new int[]{bx, by, bz};
        }
        return null;
    }

    private void breakBlock() {
        int[] hit = raycast(false);
        if (hit != null) world.setBlock(hit[0], hit[1], hit[2], BlockType.AIR);
    }

    private void placeBlock() {
        int[] hit = raycast(true); 
        if (hit == null) return;
        
        Vector3f pos = camera.getPosition();
        float px = pos.x, py = pos.y - EYE_HEIGHT, pz = pos.z;
        boolean insidePlayer =
            hit[0] == (int) Math.floor(px) &&
            hit[1] >= (int) Math.floor(py) && hit[1] <= (int) Math.floor(py + AABB_HEIGHT) &&
            hit[2] == (int) Math.floor(pz);
        if (!insidePlayer) world.setBlock(hit[0], hit[1], hit[2], heldBlock);
    }

    

    private static boolean key(long win, int code) {
        return glfwGetKey(win, code) == GLFW_PRESS;
    }

    
    private static Vector3f flatDir(Vector3f v) {
        Vector3f r = new Vector3f(v.x, 0, v.z);
        return r.lengthSquared() > 0 ? r.normalize() : r;
    }
}
