package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.GameGrid;
import java.awt.Color;
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
    private boolean returningToStart = false;
    private boolean movingToBomb = false;
    private BombMarker pendingBombMarker = null;
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

    public void setPendingBombMarker(BombMarker marker) {
        this.pendingBombMarker = marker;
    }

    /**
     * Initiate manual move to bomb and handle bomb placement.
     */
    public void startMoveToBomb(Location target) {
        if (bombsAvailable <= 0 || isBusy())
            return;
        bombTarget = target;
        startMoveToTarget(target, grid); // Use Machine's path logic
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
                startMoveToTarget(initialLocation, grid); // Return home using same logic
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
        return movingToBomb || returningToStart || isMoving;
    }

    /**
     * Place a bomb at the Bomber's current location.
     */
    private void placeBombAtCurrentLocation() {
        // Remove bomb marker, if any
        if (pendingBombMarker != null) {
            grid.removeActor(pendingBombMarker);
            pendingBombMarker = null;
        }
        if (bombsAvailable <= 0) return;
        Bomb bomb = new Bomb(getLocation(), 6, 1, this, grid);
        bombs.add(bomb);
        bombsAvailable--;
        grid.addActor(bomb, getLocation());
        bomb.show(); // show bomb.png
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

    @Override
    protected boolean canMove(Location location, GameGrid grid) {
        // Additional check for ore - bomber cannot push ore
        Actor ore = grid.getOneActorAt(location, minemaze.MineMaze.Ore.class);
        if (ore != null) {
            return false;
        }
        return super.canMove(location, grid);
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