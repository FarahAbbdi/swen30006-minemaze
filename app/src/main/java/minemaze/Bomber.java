package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;
import java.util.ArrayList;
import java.util.List;

public class Bomber extends Machine {
    private int bombsAvailable;
    private List<Bomb> bombs;
    private List<String> controls;
    private Location initialLocation;
    private boolean returningToStart = false;
    private boolean isMoving = false;
    private GameGrid grid;

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

    // Manual bomb placement (called from MineMaze on right-click)
    public Bomb placeBomb(Location targetLocation) {
        if (bombsAvailable <= 0 || isMoving || returningToStart) return null;
        // Move to target location using horizontal then vertical movement
        boolean blocked = false;
        Location currentLoc = getLocation();
        List<Location> path = new ArrayList<>();
        // Horizontal movement
        if (currentLoc.x != targetLocation.x) {
            int dx = targetLocation.x > currentLoc.x ? 1 : -1;
            for (int x = currentLoc.x + dx; x != targetLocation.x + dx; x += dx) {
                Location step = new Location(x, currentLoc.y);
                if (!canMove(step)) {
                    blocked = true;
                    targetLocation = step;
                    break;
                }
                path.add(step);
            }
        }
        // Vertical movement
        Location lastHorizontal = path.isEmpty() ? currentLoc : path.get(path.size() - 1);
        if (!blocked && lastHorizontal.y != targetLocation.y) {
            int dy = targetLocation.y > lastHorizontal.y ? 1 : -1;
            for (int y = lastHorizontal.y + dy; y != targetLocation.y + dy; y += dy) {
                Location step = new Location(lastHorizontal.x, y);
                if (!canMove(step)) {
                    blocked = true;
                    targetLocation = step;
                    break;
                }
                path.add(step);
            }
        }
        // Follow path
        isMoving = true;
        for (Location step : path) {
            setLocation(step);
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        // Place bomb at final (possibly blocked) location
        Bomb bomb = new Bomb(targetLocation, 6, 1, this, grid);
        bombs.add(bomb);
        bombsAvailable--;
        grid.addActor(bomb, targetLocation);
        bomb.use(this);
        isMoving = false;
        returningToStart = true;
        return bomb;
    }

    // Returns to initial position (after bomb placement)
    public void returnToInitialPosition() {
        setLocation(initialLocation);
        returningToStart = false;
    }

    // Auto-move logic
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

    private boolean canMove(Location location) {
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