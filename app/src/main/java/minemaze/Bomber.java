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
    private final List<Bomb> bombs;
    private List<String> controls;
    private final GameGrid grid;
    private boolean returningToStart = false;
    private boolean movingToBomb = false;
    private BombMarker pendingBombMarker = null;
    private Location bombTarget;

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
        startMoveToTarget(target, grid);
        outboundPath.addAll(movePath); // Save the path before moving
        movingToBomb = true;
        returningToStart = true; // Manual mode requires return to start
    }

    public void handleMovement() {
        if (movingToBomb) {
            if (stepMove()) {
                // Reached destination or stopped due to obstacle

                if (returningToStart) {
                    // Manual mode: drop bomb and return home using the same path in reverse
                    placeBombAtCurrentLocation();
                    movePath.clear();
                    if (outboundPath != null) {
                        movePath.addAll(reversePath(outboundPath));
                        movePathIndex = 0;
                        isMoving = true;
                    }
                    movingToBomb = false;
                } else {
                    // Auto mode: movement complete, ready for next command (no bomb dropping here)
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
     * @return true if Bomber is busy (not used in auto mode)
     */
    @Override
    public boolean isBusy() {
        return movingToBomb || returningToStart; // Remove isMoving check for auto mode
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

        if (bombsAvailable <= 0) {
            return;
        }

        Bomb bomb = new Bomb(getLocation(), 6, 1, grid);
        bombs.add(bomb);
        bombsAvailable--;
        grid.addActor(bomb, getLocation());
        System.out.println("[Bomber Debug] Placing bomb at " + getLocation() + " (Bombs left after placing: " + bombsAvailable + ")");
        bomb.show();
        bomb.use(this);
        grid.refresh();
    }

    /**
     * For auto mode: process movement commands and bomb placement
     * Execute exactly one command per tick
     * Returns true if command was processed, and we can move to next command
     */
    public boolean autoMoveNext(int autoMovementIndex, String bombCommand, Runnable refresh) {
        if (controls == null || autoMovementIndex >= controls.size()) {
            return false;
        }

        String currentMove = controls.get(autoMovementIndex);

        // --- DEBUG LOG ---
        System.out.println("[Bomber Debug] Command #" + autoMovementIndex + ": '" + currentMove +
                "', Bomber at: " + getLocation() + ", Bombs left: " + bombsAvailable);

        // Bomb command: place bomb at current location (no movement this tick)
        if (currentMove.equals(bombCommand)) {
            if (bombsAvailable > 0) {
                placeBombAtCurrentLocation();
                refresh.run();
            } else {
                System.out.println("[Bomber Debug] No bombs available for bomb command - no action this tick");
            }
            return true; // Command processed, move to next command
        }

        // Movement command (format: x-y): move one step toward target
        String[] parts = currentMove.split("-");
        if (parts.length == 2) {
            try {
                int targetX = Integer.parseInt(parts[0]);
                int targetY = Integer.parseInt(parts[1]);
                Location targetLocation = new Location(targetX, targetY);

                if (!getLocation().equals(targetLocation)) {
                    // Move one step toward the target
                    Location nextStep = getNextStepToward(targetLocation);
                    if (nextStep != null && canMove(nextStep, grid)) {
                        setLocation(nextStep);
                        System.out.println("[Bomber Debug] Moved one step to: " + nextStep + " (target: " + targetLocation + ")");
                        refresh.run();
                    } else {
                        System.out.println("[Bomber Debug] Cannot move toward: " + targetLocation + " (blocked or invalid)");
                    }
                } else {
                    System.out.println("[Bomber Debug] Already at target: " + targetLocation);
                }
                return true; // Command processed, move to next command
            } catch (NumberFormatException e) {
                System.err.println("Invalid bomber movement command: " + currentMove);
                return true; // Skip invalid commands
            }
        }

        return true; // Skip unknown commands
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
     * Calculate the next step toward a target location (one step per tick)
     */
    private Location getNextStepToward(Location target) {
        Location current = getLocation();
        int currentX = current.getX();
        int currentY = current.getY();
        int targetX = target.getX();
        int targetY = target.getY();

        // Calculate direction (one step at a time)
        int deltaX = Integer.signum(targetX - currentX);
        int deltaY = Integer.signum(targetY - currentY);

        // Move one step toward target (prioritize X movement, then Y)
        if (deltaX != 0) {
            return new Location(currentX + deltaX, currentY);
        } else if (deltaY != 0) {
            return new Location(currentX, currentY + deltaY);
        }

        return current; // Already at target
    }



    /**
     * Bomber cannot move through ore (unlike Pusher which can push ore)
     */
    @Override
    protected boolean canMove(Location location, GameGrid grid) {
        // Check for ore - bomber cannot push ore
        Actor ore = grid.getOneActorAt(location, minemaze.Ore.class);
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
}