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