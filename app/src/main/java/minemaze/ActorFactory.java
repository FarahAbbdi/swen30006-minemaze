package minemaze;

import ch.aplu.jgamegrid.Location;

/**
 * ActorFactory
 * -------------
 * Centralizes construction & placement of all actors.
 * Keeps MineMaze free from creation details.
 */
public final class ActorFactory {
    private final MineMaze game;
    private final MapGrid grid;
    private final int maxBombs;

    public ActorFactory(MineMaze game, MapGrid grid, int maxBombs) {
        this.game = game;
        this.grid = grid;
        this.maxBombs = maxBombs;
    }

    /**
     * Spawn actors that are part of the grid layout (pusher, targets, rocks, bomber...).
     */
    public void spawnGridActors() {
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
                        game.onPusherCreated(p);
                    }
                    case TARGET -> game.addActor(new Target(), loc);
                    case BOULDER -> game.addActor(new Rock(), loc);
                    case BOOSTER -> game.addActor(new Booster(), loc);
                    case HARD_ROCK -> game.addActor(new HardRock(), loc);
                    case BOMBER -> {
                        Bomber b = new Bomber(loc, maxBombs, game);
                        game.addActor(b, loc);
                        game.onBomberCreated(b);
                    }
                    default -> {}
                }
            }
        }
        game.setPaintOrder(Target.class);
    }

    /**
     * Spawn extra collectibles specified via properties (ore/fuel/booster scattered).
     */
    public void spawnExtra(String oreLocations, String fuelLocations, String boosterLocations) {
        if (!oreLocations.isEmpty()) {
            String[] ores = oreLocations.split(";");
            for (String s : ores) {
                String[] xy = s.split("-");
                game.addActor(new Ore(), new Location(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
        if (!fuelLocations.isEmpty()) {
            String[] fuels = fuelLocations.split(";");
            for (String s : fuels) {
                String[] xy = s.split("-");
                game.addActor(new Fuel(), new Location(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
        if (!boosterLocations.isEmpty()) {
            String[] boosters = boosterLocations.split(";");
            for (String s : boosters) {
                String[] xy = s.split("-");
                game.addActor(new Booster(), new Location(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
    }
}