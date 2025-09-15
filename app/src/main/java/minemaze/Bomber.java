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
    private List<Location> outboundPath = null; // Store the outbound path

    public void startMoveToBomb(Location target) {
        if (bombsAvailable <= 0 || isBusy())
            return;
        bombTarget = target;
        outboundPath = new ArrayList<>(); // Reset outbound path
        // Use Machine's movement logic but save the path
        super.startMoveToTarget(target, grid);
        outboundPath.addAll(movePath); // Save the path before moving
        movingToBomb = true;
        returningToStart = false;
    }

    public void handleMovement() {
        if (movingToBomb) {
            if (stepMove()) {
                placeBombAtCurrentLocation();
                // Return home using the same path in reverse
                movePath.clear();
                movePath.addAll(reversePath(outboundPath));
                movePathIndex = 0;
                isMoving = true;
                movingToBomb = false;
                returningToStart = true;
            }
        } else if (returningToStart) {
            if (stepMove()) {
                returningToStart = false;
                bombTarget = null;
            }
        }
    }

    private List<Location> reversePath(List<Location> path) {
        List<Location> reversed = new ArrayList<>(path);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    /**
     * @return true if Bomber is busy (either going to bomb or returning home)
     */
    @Override
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
     * Bomber cannot move through ore (unlike Pusher which can push ore)
     */
    @Override
    protected boolean canMove(Location location, GameGrid grid) {
        // Check for ore - bomber cannot push ore
        Actor ore = grid.getOneActorAt(location, minemaze.MineMaze.Ore.class);
        if (ore != null) {
            return false;
        }

        // Check for other bombers too
        Actor otherBomber = grid.getOneActorAt(location, Bomber.class);
        if (otherBomber != null && otherBomber != this) {
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