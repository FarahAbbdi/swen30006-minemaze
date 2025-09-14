package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;
import java.util.Timer;
import java.util.TimerTask;

public class Bomb extends Actor implements Usable {
    private int fuseTicks;
    private int explosionRadius;
    private boolean isActive;
    private Machine placedBy;
    private GameGrid grid;

    public Bomb(Location location, int fuseTicks, int explosionRadius, Machine placedBy, GameGrid grid) {
        super("sprites/bomb_marker.png");
        setLocation(location);
        this.fuseTicks = fuseTicks;
        this.explosionRadius = explosionRadius;
        this.isActive = true;
        this.placedBy = placedBy;
        this.grid = grid;
    }

    @Override
    public void use(Machine machine) {
        arm();
    }

    // Starts fuse countdown and triggers explosion after fuseTicks
    public void arm() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                explode();
            }
        }, fuseTicks * 1000); // Each tick = 1s, adjust as needed
    }

    public void explode() {
        if (!isActive) return;
        isActive = false;
        // Remove obstacles in radius and reveal resources
        int x0 = getLocation().x;
        int y0 = getLocation().y;
        for (int dx = -explosionRadius; dx <= explosionRadius; dx++) {
            for (int dy = -explosionRadius; dy <= explosionRadius; dy++) {
                int x = x0 + dx;
                int y = y0 + dy;
                Location loc = new Location(x, y);

                // Remove hard rocks and boulders in radius
                Actor hardRock = grid.getOneActorAt(loc, minemaze.MineMaze.HardRock.class);
                Actor boulder = grid.getOneActorAt(loc, minemaze.MineMaze.Rock.class);
                if (hardRock != null) grid.removeActor(hardRock);
                if (boulder != null) grid.removeActor(boulder);

                // Reveal resources (ore, booster, fuel) hidden beneath obstacles
                Actor ore = grid.getOneActorAt(loc, minemaze.MineMaze.Ore.class);
                Actor booster = grid.getOneActorAt(loc, minemaze.MineMaze.Booster.class);
                Actor fuel = grid.getOneActorAt(loc, minemaze.MineMaze.Fuel.class);
                if (ore != null) ore.show();
                if (booster != null) booster.show();
                if (fuel != null) fuel.show();
            }
        }
        System.out.println("Bomb exploded at " + getLocation() + " with radius " + explosionRadius);
        hide();
    }

    public boolean isActive() {
        return isActive;
    }

    public int getExplosionRadius() {
        return explosionRadius;
    }

    public Machine getPlacedBy() {
        return placedBy;
    }
}