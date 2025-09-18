package minemaze;

import ch.aplu.jgamegrid.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Pusher actor with path planning, movement, fuel, and booster logic migrated from MineMaze.
 */
public class Pusher extends Machine {
    private List<String> controls = null;
    private final MineMaze controller;

    // Path planning state
    private List<Location> pusherPath = new ArrayList<>();
    private int currentPathIndex = 0;

    // Fuel and booster state
    private int fuel;
    private final int fuelRefillAmount = 100;
    private boolean boosterReady = false;
    private int boosterCharges = 0;
    private boolean boosterActivated = false;

    public Pusher(MineMaze controller) {
        super(true, "sprites/pusher.png");
        this.controller = controller;
        this.fuel = 100; // Default, can be set via setupPusher if needed
    }

    public void setupPusher(boolean isAutoMode, List<String> pusherControls) {
        this.controls = pusherControls;
    }

    public void autoMoveNext(int autoMovementIndex) {
        if (controls == null || autoMovementIndex >= controls.size()) {
            return;
        }

        String currentMove = controls.get(autoMovementIndex);
        String[] parts = currentMove.split("-");

        if (parts.length == 2) {
            int targetX = Integer.parseInt(parts[0]);
            int targetY = Integer.parseInt(parts[1]);
            Location targetLocation = new Location(targetX, targetY);

            if (controller.isFinished()) return;

            guideToLocation(targetLocation);
        }
    }

    /**
     * Plan a simple straight-line path (horizontal then vertical) to the given target.
     */
    public void guideToLocation(Location target) {
        if (controller.isFinished()) return;

        Location start = getLocation();
        pusherPath.clear();
        currentPathIndex = 0;

        // Horizontal leg
        if (start.x != target.x) {
            int dx = target.x > start.x ? 1 : -1;
            for (int x = start.x + dx; x != target.x + dx; x += dx) {
                Location step = new Location(x, start.y);
                if (canMove(step, controller)) pusherPath.add(step); else break;
            }
        }
        // Vertical leg
        Location last = pusherPath.isEmpty() ? start : pusherPath.get(pusherPath.size() - 1);
        if (last.y != target.y) {
            int dy = target.y > last.y ? 1 : -1;
            for (int y = last.y + dy; y != target.y + dy; y += dy) {
                Location step = new Location(last.x, y);
                if (canMove(step, controller)) pusherPath.add(step); else break;
            }
        }
    }

    /**
     * Execute the next step along the planned path.
     */
    public void executeNextPathStep() {
        if (fuel <= 0) { pusherPath.clear(); currentPathIndex = 0; controller.refresh(); return; }
        if (currentPathIndex >= pusherPath.size()) return;

        Location next = pusherPath.get(currentPathIndex);
        Location cur  = getLocation();

        // Orient pusher for correct pushing behavior
        if      (next.x > cur.x) setDirection(Location.EAST);
        else if (next.x < cur.x) setDirection(Location.WEST);
        else if (next.y > cur.y) setDirection(Location.SOUTH);
        else if (next.y < cur.y) setDirection(Location.NORTH);

        // Booster: push rock 1 tile ahead (if active)
        Rock rockAtNext = (Rock) controller.getOneActorAt(next, Rock.class);
        if (rockAtNext != null && boosterReady && boosterCharges > 0) {
            Location pushTo = next.getNeighbourLocation(getDirection());
            if (canMove(pushTo, controller)) {
                rockAtNext.setLocation(pushTo);
                if (!boosterActivated && boosterCharges == 3) boosterActivated = true;
                if (--boosterCharges == 0) boosterReady = false;
            }
        }

        // Attempt movement (includes ore-push rule)
        if (canMoveWithOrePushing(next)) {
            setLocation(next);

            // Fuel consumption
            if (fuel > 0) fuel--;

            // Pickup: Fuel → refill
            Fuel can = (Fuel) controller.getOneActorAt(getLocation(), Fuel.class);
            if (can != null) { can.removeSelf(); fuel = fuel + fuelRefillAmount; }

            // Pickup: Booster → 3 charges
            Booster booster = (Booster) controller.getOneActorAt(getLocation(), Booster.class);
            if (booster != null) {
                if (!boosterReady && boosterCharges == 0 || boosterActivated) {
                    booster.removeSelf();
                    boosterReady = true; boosterCharges = 3; boosterActivated = false;
                }
            }

            // Reveal target under pusher (visual only)
            Target tgt = (Target) controller.getOneActorAt(getLocation(), Target.class);
            if (tgt != null) tgt.show();

            currentPathIndex++;
            if (fuel == 0) { pusherPath.clear(); currentPathIndex = 0; }
            controller.refresh();
        } else {
            // Blocked: discard remaining plan
            pusherPath.clear(); currentPathIndex = 0; controller.refresh();
        }
    }

    private boolean canMoveWithOrePushing(Location next) {
        Color c = controller.getBg().getColor(next);
        if (c.equals(controller.getBorderColor())) return false;

        // Impassables
        if (controller.getOneActorAt(next, Wall.class) != null) return false;
        if (controller.getOneActorAt(next, HardRock.class) != null) return false;
        if (controller.getOneActorAt(next, Rock.class) != null) return false;
        if (controller.getOneActorAt(next, Bomber.class) != null) return false;

        // Ore push rule
        Ore ore = (Ore) controller.getOneActorAt(next, Ore.class);
        if (ore != null) {
            Location dirFrom = getLocation();
            Location.CompassDirection pushDir = getPushDirection(dirFrom, next);
            Location dest = next.getNeighbourLocation(pushDir);
            ore.setDirection(pushDir);
            if (canOreMoveToLocation(ore, dest)) { moveOreToLocation(ore, dest); return true; }
            return false;
        }
        return true;
    }

    private Location.CompassDirection getPushDirection(Location from, Location to) {
        if (to.x > from.x) return Location.EAST;
        if (to.x < from.x) return Location.WEST;
        if (to.y > from.y) return Location.SOUTH;
        if (to.y < from.y) return Location.NORTH;
        return Location.EAST;
    }

    private boolean canOreMoveToLocation(Ore ore, Location dest) {
        if (dest.x < 0 || dest.x >= controller.getNbHorzCells() || dest.y < 0 || dest.y >= controller.getNbVertCells()) return false;
        Color c = controller.getBg().getColor(dest);
        if (c.equals(controller.getBorderColor())) return false;
        if (controller.getOneActorAt(dest, Rock.class) != null) return false;
        if (controller.getOneActorAt(dest, Wall.class) != null) return false;
        if (controller.getOneActorAt(dest, HardRock.class) != null) return false;
        if (controller.getOneActorAt(dest, Pusher.class) != null) return false;
        if (controller.getOneActorAt(dest, Bomber.class) != null) return false;
        Ore other = (Ore) controller.getOneActorAt(dest, Ore.class);
        return other == null || other == ore;
    }

    private void moveOreToLocation(Ore ore, Location dest) {
        Location cur = ore.getLocation();
        Target t = (Target) controller.getOneActorAt(cur, Target.class);
        if (t != null) { t.show(); ore.show(0); }

        ore.setLocation(dest);

        // Arrived: hide ore when sitting on target and count towards win
        Target newT = (Target) controller.getOneActorAt(dest, Target.class);
        if (newT != null) { controller.incrementOresCollected(); ore.hide(); }
    }

    @Override
    protected boolean canMove(Location loc, GameGrid grid) {
        Color c = controller.getBg().getColor(loc);
        if (c.equals(controller.getBorderColor())) return false;
        if (controller.getOneActorAt(loc, Wall.class) != null) return false;
        if (controller.getOneActorAt(loc, HardRock.class) != null) return false;

        // Rock is generally blocking unless a booster push is feasible (one tile ahead is free)
        if (controller.getOneActorAt(loc, Rock.class) != null) {
            if (boosterReady && boosterCharges > 0) {
                Location pLoc = getLocation();
                int dx = Integer.compare(loc.x, pLoc.x);
                int dy = Integer.compare(loc.y, pLoc.y);
                if (Math.abs(dx) + Math.abs(dy) == 1) {
                    Location pushTo = new Location(loc.x + dx, loc.y + dy);
                    Color c2 = controller.getBg().getColor(pushTo);
                    if (!c2.equals(controller.getBorderColor())
                            && controller.getOneActorAt(pushTo, Rock.class) == null
                            && controller.getOneActorAt(pushTo, Wall.class) == null
                            && controller.getOneActorAt(pushTo, HardRock.class) == null
                            && controller.getOneActorAt(pushTo, Bomber.class) == null) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (controller.getOneActorAt(loc, Bomber.class) != null) return false;
        return true;
    }

    // Accessors for MineMaze logging
    public int getFuel() { return fuel; }
}