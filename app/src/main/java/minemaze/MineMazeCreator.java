package minemaze;

import ch.aplu.jgamegrid.Location;

/**
 * MineMazeCreator
 * ----------------
 * GRASP Creator for game actors only.
 * - Spawns grid-based actors (pusher/targets/rocks/bomber)
 * - Spawns extra collectibles from properties (ore/fuel/booster)
 * (No board rendering here.)
 */
public final class MineMazeCreator {
    private MineMazeCreator() {}

    /** Create all actors for this game. */
    public static void createActors(MineMaze game, GameConfig cfg, MapGrid grid) {
        spawnExtras(game, cfg);
        spawnGridActors(game, grid, cfg.maxBombs);
        game.setPaintOrder(Target.class); // ensure target paints under ore
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
                        game.onPusherCreated(p); // inject controls
                    }
                    case TARGET -> game.addActor(new Target(), loc);
                    case BOULDER -> game.addActor(new Rock(), loc);
                    case BOOSTER -> game.addActor(new Booster(), loc);
                    case HARD_ROCK -> game.addActor(new HardRock(), loc);
                    case BOMBER -> {
                        Bomber b = new Bomber(loc, maxBombs, game);
                        game.addActor(b, loc);
                        game.onBomberCreated(b); // inject controls/border color
                    }
                    default -> { /* ignore */ }
                }
            }
        }
    }
}