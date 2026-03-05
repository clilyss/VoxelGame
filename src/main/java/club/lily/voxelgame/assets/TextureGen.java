package club.lily.voxelgame.assets;

import club.lily.voxelgame.assets.painters.TilePainters;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

public class TextureGen {

    public static final int TILE = 16;
    public static final int COLS = 16;
    public static final int ROWS = 16;
    public static final int SIZE = TILE * COLS;

    public static final int T_GRASS_TOP  = 0;
    public static final int T_GRASS_SIDE = 1;
    public static final int T_DIRT       = 2;
    public static final int T_STONE      = 3;
    public static final int T_BEDROCK    = 4;
    public static final int T_SAND       = 5;
    public static final int T_SNOW       = 6;
    public static final int T_WOOD_TOP   = 7;
    public static final int T_WOOD_SIDE  = 8;
    public static final int T_LEAVES     = 9;

    @FunctionalInterface
    public interface TilePainter { void paint(int[] px, Random rng); }

    public static BufferedImage generate() {
        BufferedImage atlas = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        drawTile(atlas, T_GRASS_TOP,  TilePainters::grassTop);
        drawTile(atlas, T_GRASS_SIDE, TilePainters::grassSide);
        drawTile(atlas, T_DIRT,       TilePainters::dirt);
        drawTile(atlas, T_STONE,      TilePainters::stone);
        drawTile(atlas, T_BEDROCK,    TilePainters::bedrock);
        drawTile(atlas, T_SAND,       TilePainters::sand);
        drawTile(atlas, T_SNOW,       TilePainters::snow);
        drawTile(atlas, T_WOOD_TOP,   TilePainters::woodTop);
        drawTile(atlas, T_WOOD_SIDE,  TilePainters::woodSide);
        drawTile(atlas, T_LEAVES,     TilePainters::leaves);
        return atlas;
    }

    public static void saveAtlas(BufferedImage atlas, String path) {
        try {
            File f = new File(path);
            f.getParentFile().mkdirs();
            ImageIO.write(atlas, "PNG", f);
        } catch (IOException e) {
            System.err.println("[TextureGen] " + e.getMessage());
        }
    }

    private static void drawTile(BufferedImage atlas, int col, TilePainter painter) {
        int[] px = new int[TILE * TILE];
        painter.paint(px, new Random(col * 31337L));
        atlas.setRGB(col * TILE, 0, TILE, TILE, px, 0, TILE);
    }

    public static void main(String[] args) {
        saveAtlas(generate(), "assets/atlas.png");
    }
}
