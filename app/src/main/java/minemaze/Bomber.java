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
        returningToStart = true; // Manual mode requires return to start
    }

    public void handleMovement() {
        if (movingToBomb) {
            if (stepMove()) {
                // Reached destination or stopped due to obstacle
                placeBombAtCurrentLocation();

                if (returningToStart) {
                    // Manual mode: return home using the same path in reverse
                    movePath.clear();
                    if (outboundPath != null) {
                        movePath.addAll(reversePath(outboundPath));
                        movePathIndex = 0;
                        isMoving = true;
                    }
                    movingToBomb = false;
                } else {
                    // Auto mode: movement complete, ready for next command
                    movingToBomb = false;
                    bombTarget = null;
                }
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
     * For auto mode: process movement commands and bomb placement using same logic as manual mode
     * Returns true if command was processed and we can move to next command
     */
    public boolean autoMoveNext(int autoMovementIndex, String bombCommand, Runnable refresh) {
        if (controls == null || autoMovementIndex >= controls.size()) {
            return false; // No more commands
        }

        String currentMove = controls.get(autoMovementIndex);

        // Check if this is a bomb command
        if (currentMove.equals(bombCommand)) {
            if (bombsAvailable > 0) {
                placeBombAtCurrentLocation();
                refresh.run();
            }
            return true; // Command processed
        }

        // Process movement command (format: "x-y")
        String[] parts = currentMove.split("-");
        if (parts.length == 2) {
            try {
                int targetX = Integer.parseInt(parts[0]);
                int targetY = Integer.parseInt(parts[1]);
                Location targetLocation = new Location(targetX, targetY);

                // Check if we're already at the target location
                if (getLocation().equals(targetLocation)) {
                    // Already at target, just wait this tick
                    refresh.run();
                    return true; // Command processed
                }

                // Need to move to target location
                if (!isBusy()) {
                    // Use the same movement logic as manual mode
                    super.startMoveToTarget(targetLocation, grid);

                    // Set flags for auto mode movement (no return to start needed)
                    movingToBomb = true;
                    returningToStart = false; // Auto mode doesn't return to start
                    bombTarget = targetLocation;

                    refresh.run();
                }
                // Don't mark as processed until movement is complete
                return false; // Still processing this command

            } catch (NumberFormatException e) {
                System.err.println("Invalid bomber movement command: " + currentMove);
                return true; // Skip invalid command
            }
        }

        return true; // Unknown command format, skip it
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