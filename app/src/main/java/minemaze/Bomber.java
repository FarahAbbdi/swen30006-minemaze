package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;
import java.util.ArrayList;
import java.util.List;

/**
 * Bomber can be controlled manually (right-click) or by auto-move commands.
 * In manual mode, it moves to the bomb location, drops a bomb, and returns home before accepting new commands.
 */
public class Bomber extends Machine {
    private int bombsAvailable;
    private List<Bomb> bombs;
    private List<String> controls;
    private Location initialLocation;
    private GameGrid grid;
    private List<Location> path = new ArrayList<>();
    private int pathIndex = 0;
    private boolean returningToStart = false;
    private boolean movingToBomb = false;
    private Location bombTarget = null;

    public Bomber(Location startLocation, int bombsAvailable, GameGrid grid) {
        super(true, "sprites/bomber.png");
        this.initialLocation = startLocation;
        this.bombsAvailable = bombsAvailable;
        this.bombs = new ArrayList<>();
        this.controls = new ArrayList<>();
        setLocation(startLocation);
        this.grid = grid;
    }

    public void setupBomberControls(List<String> bomberControls) {
        this.controls = bomberControls;
    }

    /**
     * Initiate manual move to bomb and handle bomb placement.
     */
    public void startMoveToBomb(Location target) {
        if (bombsAvailable <= 0 || isBusy())
            return;
        bombTarget = target;
        path = buildPath(getLocation(), target);
        pathIndex = 0;
        movingToBomb = true;
        returningToStart = false;
    }

    /**
     * Called each game tick in MineMaze to process step-by-step movement and state transitions.
     */
    public void handleMovement() {
        if (movingToBomb) {
            if (stepMove()) { // Arrived at bomb target or blocked
                placeBombAtCurrentLocation();
                path = buildPath(getLocation(), initialLocation);
                pathIndex = 0;
                movingToBomb = false;
                returningToStart = true;
            }
        } else if (returningToStart) {
            if (stepMove()) { // Arrived home
                returningToStart = false;
                bombTarget = null;
            }
        }
    }

    /**
     * @return true if Bomber is busy (either going to bomb or returning home)
     */
    public boolean isBusy() {
        return movingToBomb || returningToStart;
    }

    /**
     * Moves one step along the path. Returns true if reached the end.
     */
    public boolean stepMove() {
        if (path == null || pathIndex >= path.size())
            return true; // Already at destination
        setLocation(path.get(pathIndex));
        pathIndex++;
        return pathIndex >= path.size(); // Done if just moved to final step
    }

    /**
     * Place a bomb at the Bomber's current location.
     */
    private void placeBombAtCurrentLocation() {
        if (bombsAvailable <= 0) return;
        Bomb bomb = new Bomb(getLocation(), 6, 1, this, grid);
        bombs.add(bomb);
        bombsAvailable--;
        grid.addActor(bomb, getLocation());
        bomb.show();
        bomb.use(this);
        grid.refresh();
    }

    /**
     * For auto mode: instantly move and place bomb if needed.
     */
    public void autoMoveNext(int autoMovementIndex, String bombCommand, Runnable refresh) {
        if (controls != null && autoMovementIndex < controls.size()) {
            String currentMove = controls.get(autoMovementIndex);
            String[] parts = currentMove.split("-");
            if (currentMove.equals(bombCommand)) {
                if (bombsAvailable > 0) {
                    Bomb bomb = new Bomb(getLocation(), 6, 1, this, grid);
                    bombs.add(bomb);
                    bombsAvailable--;
                    grid.addActor(bomb, getLocation());
                    bomb.use(this);
                    refresh.run();
                }
                return;
            }
            if (parts.length == 2) {
                int bombX = Integer.parseInt(parts[0]);
                int bombY = Integer.parseInt(parts[1]);
                setLocation(new Location(bombX, bombY));
                refresh.run();
            }
        }
    }

    /**
     * Build horizontal-then-vertical path, stopping if blocked.
     */
    private List<Location> buildPath(Location from, Location to) {
        List<Location> result = new ArrayList<>();
        Location current = from;
        // Horizontal
        int dx = Integer.compare(to.x, from.x);
        while (current.x != to.x) {
            Location step = new Location(current.x + dx, current.y);
            if (!canMove(step, grid)) break;
            result.add(step);
            current = step;
        }
        // Vertical
        int dy = Integer.compare(to.y, current.y);
        while (current.y != to.y) {
            Location step = new Location(current.x, current.y + dy);
            if (!canMove(step, grid)) break;
            result.add(step);
            current = step;
        }
        return result;
    }

    @Override
    protected boolean canMove(Location location, GameGrid grid) {
        Actor hardRock = grid.getOneActorAt(location, minemaze.MineMaze.HardRock.class);
        Actor boulder = grid.getOneActorAt(location, minemaze.MineMaze.Rock.class);
        Actor wall = grid.getOneActorAt(location, minemaze.MineMaze.Wall.class);
        return hardRock == null && boulder == null && wall == null;
    }

    public int getBombsAvailable() {
        return bombsAvailable;
    }

    public List<Bomb> getBombs() {
        return bombs;
    }

    public boolean isReturningToStart() {
        return returningToStart;
    }
}