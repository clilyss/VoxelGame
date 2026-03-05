package club.lily.voxelgame.player.interaction;

import club.lily.voxelgame.engine.camera.Camera;
import club.lily.voxelgame.world.World;
import org.joml.Vector3f;

public final class Raycast {

    private static final float REACH    = 5.0f;
    private static final float RAY_STEP = 0.04f;

    private Raycast() {}

    public static int[] hit(Camera camera, World world) {
        return cast(camera, world, false);
    }

    public static int[] adjacent(Camera camera, World world) {
        return cast(camera, world, true);
    }

    private static int[] cast(Camera camera, World world, boolean returnPrev) {
        Vector3f origin = camera.getPosition();
        Vector3f dir    = new Vector3f(camera.getFront()).normalize();
        int[] prev = null;
        for (float d = 0; d < REACH; d += RAY_STEP) {
            int bx = (int) Math.floor(origin.x + dir.x * d);
            int by = (int) Math.floor(origin.y + dir.y * d);
            int bz = (int) Math.floor(origin.z + dir.z * d);
            if (world.getBlock(bx, by, bz).isSolid())
                return returnPrev ? prev : new int[]{bx, by, bz};
            prev = new int[]{bx, by, bz};
        }
        return null;
    }
}
