package club.lily.voxelgame.player;

import club.lily.voxelgame.engine.camera.Camera;
import club.lily.voxelgame.engine.window.Window;
import club.lily.voxelgame.player.interaction.BlockInteraction;
import club.lily.voxelgame.player.movement.PlayerMovement;
import club.lily.voxelgame.world.World;

public class Player {

    private final Camera           camera;
    private final World            world;
    private final PlayerMovement   movement;
    private final BlockInteraction interaction;

    public Player(Camera camera, World world) {
        this.camera      = camera;
        this.world       = world;
        this.movement    = new PlayerMovement(camera, world);
        this.interaction = new BlockInteraction(camera, world);
    }

    public void spawnOnTerrain(int wx, int wz) {
        int topY = world.getTopY(wx, wz);
        camera.setPosition(wx + 0.5f, topY + 1 + PlayerMovement.EYE_HEIGHT, wz + 0.5f);
    }

    public void update(Window window, float dt) {
        camera.rotate(window.getMouseDX(), window.getMouseDY());
        movement.update(window, dt);
        interaction.update(window, dt);
    }
}
