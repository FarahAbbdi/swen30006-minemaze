package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for machine actors that can move within the maze.
 * Handles path calculation and step-by-step movement with collision detection.
 */
public abstract class Machine extends Actor {
    protected List<Location> movePath = new ArrayList<>();
    protected int movePathIndex = 0;
    protected boolean isMoving = false;
    protected Location initialLocation;
    protected Color borderColor;

    public Machine(boolean rotatable, String spritePath) {
        super(rotatable, spritePath);
    }

    /**
     * Set the border color for collision detection
     */
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    /**
     * Start movement toward a target location
     * Calculates a path going horizontally then vertically
     */
    public void startMoveToTarget(Location target, GameGrid grid) {
        movePath.clear();
        movePathIndex = 0;
        Location current = getLocation();
        movePath.add(current); // Add initial position to path

        boolean moveHorizontally = true; // Start with horizontal move

        while (!current.equals(target)) {
            boolean moved = false;

            // Try both axes (horizontal and vertical)
            for (int attempt = 0; attempt < 2; attempt++) {
                Location nextStep = null;

                if (moveHorizontally && current.x != target.x) {
                    int dx = current.x < target.x ? 1 : -1;
                    nextStep = new Location(current.x + dx, current.y);
                } else if (!moveHorizontally && current.y != target.y) {
                    int dy = current.y < target.y ? 1 : -1;
                    nextStep = new Location(current.x, current.y + dy);
                }

                if (nextStep != null) {
                    Actor hardrock = grid.getOneActorAt(nextStep, minemaze.HardRock.class);
                    Actor rock = grid.getOneActorAt(nextStep, minemaze.Rock.class);
                    Actor ore = grid.getOneActorAt(nextStep, minemaze.Ore.class);
                    Actor wall = grid.getOneActorAt(nextStep, minemaze.Wall.class);

                    if (hardrock != null || rock != null) {
                        // Hit hardrock or rock - stop here and allow bomb drop only if we've moved
                        isMoving = movePath.size() > 1;
                        return;
                    } else if (wall != null) {
                        // Hit wall - try the other axis instead of stopping
                        moveHorizontally = !moveHorizontally;
                        continue;
                    } else if (ore != null) {
                        // Hit ore - try the other axis (bomber can't push ore)
                        moveHorizontally = !moveHorizontally;
                        continue;
                    } else if (canMove(nextStep, grid)) {
                        // Path is clear - move to this location
                        movePath.add(nextStep);
                        current = nextStep;
                        moved = true;
                        break;
                    } else {
                        // Other obstacle - try the other axis
                        moveHorizontally = !moveHorizontally;
                        continue;
                    }
                }

                // Switch to the other axis for next attempt
                moveHorizontally = !moveHorizontally;
            }

            if (!moved) {
                // No movement possible in either direction - stop
                break;
            }
        }

        // Set moving state based on whether we have a valid path
        isMoving = movePath.size() > 1;
    }

    /**
     * Move one step along the calculated path
     * @return true if reached end of path or no more movement possible
     */
    public boolean stepMove() {
        if (!isMoving || movePathIndex >= movePath.size()) {
            isMoving = false;
            return true;  // Done moving
        }

        setLocation(movePath.get(movePathIndex++));

        if (movePathIndex >= movePath.size()) {
            isMoving = false;
            return true;  // Reached end of path
        }

        return false;  // Not done yet
    }

    /**
     * Check if this machine can move to the specified location
     */
    protected boolean canMove(Location location, GameGrid grid) {
        // Check for border color
        if (borderColor != null && grid.getBg().getColor(location).equals(borderColor)) {
            return false;
        }

        // Check for walls and rocks
        Actor wall = grid.getOneActorAt(location, minemaze.Wall.class);
        Actor rock = grid.getOneActorAt(location, minemaze.Rock.class);
        Actor hardRock = grid.getOneActorAt(location, minemaze.HardRock.class);

        return wall == null && rock == null && hardRock == null;
    }

    /**
     * Check if machine is currently moving
     */
    public boolean isBusy() {
        return isMoving;
    }
}