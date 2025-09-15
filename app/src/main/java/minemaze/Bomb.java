package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;

public class Bomb extends Actor implements Usable {
    private int fuseTicksRemaining;
    private int explosionRadius;
    private boolean isActive;
    private boolean isArmed;
    private Machine placedBy;
    private GameGrid grid;

    public Bomb(Location location, int fuseTicks, int explosionRadius, Machine placedBy, GameGrid grid) {
        super("sprites/bomb.png");
        setLocation(location);
        this.fuseTicksRemaining = fuseTicks;
        this.explosionRadius = explosionRadius;
        this.isActive = true;
        this.isArmed = false;
        this.placedBy = placedBy;
        this.grid = grid;
    }

    @Override
    public void use(Machine machine) {
        arm();
    }

    // Arms the bomb to start countdown
    public void arm() {
        isArmed = true;
    }

    // Called each game tick to update the bomb state
    public void tick() {
        if (!isActive || !isArmed) return;

        fuseTicksRemaining--;

        if (fuseTicksRemaining <= 0) {
            explode();
        }
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

        // Remove the bomb from the grid
        grid.removeActor(this);
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isArmed() {
        return isArmed;
    }

    public int getFuseTicksRemaining() {
        return fuseTicksRemaining;
    }

    public int getExplosionRadius() {
        return explosionRadius;
    }

    public Machine getPlacedBy() {
        return placedBy;
    }
}