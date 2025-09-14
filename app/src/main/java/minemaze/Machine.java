package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;
import java.util.ArrayList;
import java.util.List;

import ch.aplu.jgamegrid.Actor;

// Abstract class for all vehicles/machines
public abstract class Machine extends Actor {
    protected List<Location> movePath = new ArrayList<>();
    protected int movePathIndex = 0;
    protected boolean isMoving = false;
    protected boolean returningHome = false;
    protected Location moveTarget = null;
    protected Location initialLocation;

    public Machine(boolean rotatable, String spritePath) {
        super(rotatable, spritePath);
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
                if (!canMove(step, grid)) break;
                movePath.add(step);
            }
        }
        // Vertical movement
        Location lastHorizontal = movePath.isEmpty() ? currentLoc : movePath.get(movePath.size() - 1);
        if (lastHorizontal.y != target.y) {
            int dy = target.y > lastHorizontal.y ? 1 : -1;
            for (int y = lastHorizontal.y + dy; y != target.y + dy; y += dy) {
                Location step = new Location(lastHorizontal.x, y);
                if (!canMove(step, grid)) break;
                movePath.add(step);
            }
        }
        isMoving = true;
    }

    public boolean stepMove() {
        if (isMoving && movePathIndex < movePath.size()) {
            setLocation(movePath.get(movePathIndex++));
            return false; // Not yet arrived
        } else if (isMoving) {
            isMoving = false;
            movePath.clear();
            movePathIndex = 0;
            return true; // Arrived
        }
        return false;
    }

    protected boolean canMove(Location location, GameGrid grid) {
        // Override in child if needed, or use grid logic
        return true;
    }

    public boolean isBusy() {
        return isMoving || returningHome;
    }
}