package minemaze;

import ch.aplu.jgamegrid.GGBackground;
import ch.aplu.jgamegrid.Location;

import java.awt.*;

/**
 * MineMazeCreator
 * ----------------
 * Minimal creator (GRASP Creator) that:
 *  - draws the static board
 *  - spawns grid-based actors (pusher/targets/rocks/bomber)
 *  - spawns extra collectibles from properties (ore/fuel/booster)
 *
 * No GoF factory patterns; just concentrated construction logic.
 */
public final class MineMazeCreator {
    private MineMazeCreator() {}

    public static void setup(MineMaze game, GameConfig cfg, MapGrid grid) {
        // 1) Draw board
        drawBoard(game.getBg(), grid, game.getBorderColor());

        // 2) Spawn extras from properties
        spawnExtras(game, cfg);

        // 3) Spawn grid-based actors defined by the map
        spawnGridActors(game, grid, cfg.maxBombs);

        // Paint order so targets show beneath ores
        game.setPaintOrder(Target.class);
    }

    private static void drawBoard(GGBackground bg, MapGrid grid, Color borderColor) {
        int w = grid.getNbHorzCells();
        int h = grid.getNbVertCells();
        bg.clear(new Color(230, 230, 230));
        bg.setPaintColor(Color.darkGray);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Location loc = new Location(x, y);
                MineMaze.ElementType t = grid.getCell(loc);
                if (t != MineMaze.ElementType.OUTSIDE) {
                    bg.fillCell(loc, Color.lightGray);
                }
                if (t == MineMaze.ElementType.BORDER) {
                    bg.fillCell(loc, borderColor);
                }
            }
        }
    }

    private static void spawnExtras(MineMaze game, GameConfig cfg) {
        // Ores
        if (cfg.oreLocations != null && !cfg.oreLocations.isEmpty()) {
            for (String s : cfg.oreLocations.split(";")) {
                String[] xy = s.split("-");
                game.addActor(new Ore(), new Location(
                        Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
        // Fuel
        if (cfg.fuelLocations != null && !cfg.fuelLocations.isEmpty()) {
            for (String s : cfg.fuelLocations.split(";")) {
                String[] xy = s.split("-");
                game.addActor(new Fuel(), new Location(
                        Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
        // Boosters
        if (cfg.boosterLocations != null && !cfg.boosterLocations.isEmpty()) {
            for (String s : cfg.boosterLocations.split(";")) {
                String[] xy = s.split("-");
                game.addActor(new Booster(), new Location(
                        Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
    }

    private static void spawnGridActors(MineMaze game, MapGrid grid, int maxBombs) {
        int w = grid.getNbHorzCells();
        int h = grid.getNbVertCells();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Location loc = new Location(x, y);
                MineMaze.ElementType t = grid.getCell(loc);
                switch (t) {
                    case PUSHER -> {
                        Pusher p = new Pusher(game);
                        game.addActor(p, loc);
                        game.onPusherCreated(p); // inject scripts into pusher
                    }
                    case TARGET -> game.addActor(new Target(), loc);
                    case BOULDER -> game.addActor(new Rock(), loc);
                    case BOOSTER -> game.addActor(new Booster(), loc);
                    case HARD_ROCK -> game.addActor(new HardRock(), loc);
                    case BOMBER -> {
                        Bomber b = new Bomber(loc, maxBombs, game);
                        game.addActor(b, loc);
                        game.onBomberCreated(b); // inject scripts/border color
                    }
                    default -> { /* ignore others */ }
                }
            }
        }
    }
}