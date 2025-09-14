package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

// Abstract class for all vehicles/machines
public abstract class Machine extends Actor {
    protected List<Location> movePath = new ArrayList<>();
    protected int movePathIndex = 0;
    protected boolean isMoving = false;
    protected boolean returningHome = false;
    protected Location moveTarget = null;
    protected Location initialLocation;
    protected Color borderColor;

    public Machine(boolean rotatable, String spritePath) {
        super(rotatable, spritePath);
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public void startMoveToTarget(Location target, GameGrid grid) {
        movePath.clear();
        movePathIndex = 0;
        moveTarget = target;
        Location currentLoc = getLocation();
        // Horizontal movement
        if (currentLoc.x != target.x) {
            int dx = target.x > currentLoc.x ? 1 : -1;
            for (int x = currentLoc.x + dx; x != target.x + dx; x += dx) {
                Location step = new Location(x, currentLoc.y);
                if (!canMove(step, grid)) {
                    break; // Stop if path is blocked
                }
                movePath.add(step);
            }
        }
        // Vertical movement
        Location lastHorizontal = movePath.isEmpty() ? currentLoc : movePath.get(movePath.size() - 1);
        if (lastHorizontal.y != target.y) {
            int dy = target.y > lastHorizontal.y ? 1 : -1;
            for (int y = lastHorizontal.y + dy; y != target.y + dy; y += dy) {
                Location step = new Location(lastHorizontal.x, y);
                if (!canMove(step, grid)) {
                    break; // Stop if path is blocked
                }
                movePath.add(step);
            }
        }
        isMoving = !movePath.isEmpty();
    }

    public boolean stepMove() {
        if (isMoving && movePathIndex < movePath.size()) {
            setLocation(movePath.get(movePathIndex++));
            return movePathIndex >= movePath.size(); // Return true when we just moved to the final position
        } else if (isMoving) {
            isMoving = false;
            movePath.clear();
            movePathIndex = 0;
            return true; // Arrived
        }
        return false;
    }

    protected boolean canMove(Location location, GameGrid grid) {
        // Check border color
        if (borderColor != null && grid.getBg().getColor(location).equals(borderColor)) {
            return false;
        }

        // Check for obstacles
        Actor hardRock = grid.getOneActorAt(location, minemaze.MineMaze.HardRock.class);
        Actor boulder = grid.getOneActorAt(location, minemaze.MineMaze.Rock.class);
        Actor wall = grid.getOneActorAt(location, minemaze.MineMaze.Wall.class);
        Actor bomber = grid.getOneActorAt(location, Bomber.class);
        Actor pusher = grid.getOneActorAt(location, grid.getClass().getDeclaredClasses()[0]); // Assuming Pusher is the first inner class

        // Don't allow movement onto another machine or obstacle
        return hardRock == null && boulder == null && wall == null &&
                (bomber == null || bomber == this) && (pusher == null || pusher == this);
    }

    public boolean isBusy() {
        return isMoving || returningHome;
    }
}