package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;

public class Bomb extends Actor implements Usable {
    private int fuseTicksRemaining;
    private final int explosionRadius;
    private boolean isActive;
    private boolean isArmed;
    private final GameGrid grid;

    public Bomb(Location location, int fuseTicks, int explosionRadius, GameGrid grid) {
        super("sprites/bomb.png");
        setLocation(location);
        this.fuseTicksRemaining = fuseTicks;
        this.explosionRadius = explosionRadius;
        this.isActive = true;
        this.isArmed = false;
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

        int x0 = getLocation().x;
        int y0 = getLocation().y;

        // Center tile
        int[][] directions = {
                {0, 0}, // bomb's own tile
                {0, -explosionRadius}, // up
                {0, explosionRadius},  // down
                {-explosionRadius, 0}, // left
                {explosionRadius, 0}   // right
        };

        for (int[] dir : directions) {
            int x = x0 + dir[0];
            int y = y0 + dir[1];
            Location loc = new Location(x, y);

            // Remove hard rocks and boulders in radius
            Actor hardRock = grid.getOneActorAt(loc, minemaze.HardRock.class);
            Actor boulder = grid.getOneActorAt(loc, minemaze.Rock.class);
            if (hardRock != null) grid.removeActor(hardRock);
            if (boulder != null) grid.removeActor(boulder);

            // Reveal resources (ore, booster, fuel) hidden beneath obstacles
            Actor ore = grid.getOneActorAt(loc, minemaze.Ore.class);
            Actor booster = grid.getOneActorAt(loc, minemaze.Booster.class);
            Actor fuel = grid.getOneActorAt(loc, minemaze.Fuel.class);
            if (ore != null) ore.show();
            if (booster != null) booster.show();
            if (fuel != null) fuel.show();
        }

        System.out.println("Bomb exploded at " + getLocation() + " with radius " + explosionRadius);

        grid.removeActor(this);
    }

    public boolean isActive() {
        return isActive;
    }


}