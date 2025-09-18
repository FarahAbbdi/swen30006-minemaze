package minemaze;

import ch.aplu.jgamegrid.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * MineMaze
 * ---------
 * Main game controller and orchestrator.
 * - Owns the game loop, input handling, movement/collision rules, and logging.
 * - Coordinates actors (Pusher, Bomber, collectibles) placed on a MapGrid.
 * - Renders the board and simple HUD via helper renderers.
 */
public class MineMaze extends GameGrid implements GGMouseListener {

    // ------------------------------------------------------------------------
    // Enum: ElementType
    // Maps map symbols to semantic cell types used by MapGrid and spawning.
    // ------------------------------------------------------------------------
    public enum ElementType {
        OUTSIDE("Outside", ' '), EMPTY("Empty", '.'), BORDER("Border", 'x'),
        PUSHER("Pusher", 'P'), ORE("Ore", '*'), BOULDER("Boulder", 'r'), TARGET("Target", 'o'),
        BOMB_MARKER("BombMarker", 'm'), BOOSTER("Booster", 'b'), HARD_ROCK("HardRock", 'h'),
        BOMBER("Bomber", 'B');

        private final String shortType;
        private final char mapElement;

        ElementType(String s, char c) { this.shortType = s; this.mapElement = c; }
        public String getShortType() { return shortType; }
        public char getMapElement() { return mapElement; }

        /** Look up an element type by its short string name; defaults to EMPTY. */
        public static ElementType getElementByShortType(String shortType) {
            for (ElementType t : values()) if (t.getShortType().equals(shortType)) return t;
            return EMPTY;
        }
    }

    /** Command token used in scripted bomber moves. */
    public static final String BOMB_COMMAND = "Bomb";

    // =========================================================================
    // Fields & Associations
    // =========================================================================
    private final MapGrid grid;
    private final int nbHorzCells;
    private final int nbVertCells;
    private final Color borderColor = new Color(100, 100, 100);

    // Helpers for configuration and rendering
    private final GameConfig cfg;
    private final BoardRenderer boardRenderer = new BoardRenderer(borderColor);
    private final HudRenderer hud = new HudRenderer();

    // Primary actors
    private Pusher pusher;
    private Bomber bomber;

    // Game state
    private boolean finished = false;
    private double gameDuration;
    private final int oresWinning;
    private int oresCollected = 0;
    private int autoMovementIndex = 0;

    // Pusher path planning state (simple H-then-V path)
    private List<Location> pusherPath = new ArrayList<>();
    private int currentPathIndex = 0;

    // Fuel / Booster state (currently tracked here; candidate to move into Pusher)
    private int pusherFuel = 100;
    private final int fuelRefillAmount = 100;
    private boolean boosterReady = false;
    private int boosterCharges = 0;
    private boolean boosterActivated = false;

    // Log buffer used by tests to verify game progress
    private final StringBuilder logResult = new StringBuilder();

    // =========================================================================
    // Construction
    // =========================================================================
    /**
     * Construct a MineMaze controller bound to a MapGrid and configuration.
     *
     * @param properties Game properties (movement mode, durations, placements, scripts, etc.)
     * @param grid       Map grid defining cell types and size
     */
    public MineMaze(Properties properties, MapGrid grid) {
        super(grid.getNbHorzCells(), grid.getNbVertCells(), 30, false);
        this.grid = grid;
        this.nbHorzCells = grid.getNbHorzCells();
        this.nbVertCells = grid.getNbVertCells();

        this.cfg = new GameConfig(properties);
        setSimulationPeriod(cfg.simulationPeriodMs);
        this.gameDuration = cfg.durationSeconds;
        this.oresWinning = cfg.oresWinning;
        this.pusherFuel = cfg.initialFuel;

        // (Minimal diff) No board/actors/HUD/mouse setup here.
        // That work is performed at the start of runApp(), matching legacy timing.
    }

    // =========================================================================
    // Game Loop
    // =========================================================================
    /**
     * Run the main game loop until win condition or time-out.
     *
     * @param showUI whether to show the UI window
     * @return textual log of state used by tests (win/lose string appended at end)
     */
    public String runApp(boolean showUI) {
        // (Minimal diff) Setup moved here to match original behavior
        boardRenderer.drawBoard(getBg(), grid);
        ActorFactory factory = new ActorFactory(this, grid, cfg.maxBombs);
        factory.spawnExtra(cfg.oreLocations, cfg.fuelLocations, cfg.boosterLocations);
        factory.spawnGridActors();
        getBg().setFont(new Font("Arial", Font.BOLD, 14));
        hud.drawControlsHelp(getBg(), 30, nbVertCells);
        addMouseListener(this, GGMouse.lPress | GGMouse.rPress);

        if (showUI) show();
        if (cfg.autoMode) doRun(); // jGameGrid internal run

        while (oresCollected < oresWinning && gameDuration >= 0) {
            try {
                Thread.sleep(getSimulationPeriod());
                gameDuration -= getSimulationPeriod() / 1000.0;
                setTitle(String.format("Ores: %d/%d | Time: %.1fs", oresCollected, oresWinning, gameDuration));

                // Advance scripted moves (if in auto mode); otherwise just follow the current planned path
                if (cfg.autoMode) {
                    if (pusher != null) pusher.autoMoveNext(autoMovementIndex);
                    if (bomber != null) bomber.autoMoveNext(autoMovementIndex, BOMB_COMMAND, this::refresh);
                    executeNextPathStep();
                    autoMovementIndex++;
                } else {
                    executeNextPathStep();
                }

                // Bomber performs its per-tick movement updates
                if (bomber != null) bomber.handleMovement();

                // Update bombs and refresh display/log/HUD
                updateBombs();
                refresh();
                updateLogResult();
                hud.updateStatusDisplay(getBg());

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Finalization
        doPause();
        setTitle(oresCollected == oresWinning ? "Mission Complete. Well done!" : "Mission Failed. You ran out of time");
        logResult.append(oresCollected == oresWinning ? "You won" : "You lost");
        finished = true;
        return logResult.toString();
    }

    // =========================================================================
    // Actor creation callbacks (invoked by ActorFactory)
    // =========================================================================
    /** Capture the constructed Pusher and inject its control script. */
    void onPusherCreated(Pusher p) {
        this.pusher = p;
        p.setupPusher(cfg.autoMode, cfg.pusherMoves);
    }

    /** Capture the constructed Bomber and inject its control script & visual border color. */
    void onBomberCreated(Bomber b) {
        this.bomber = b;
        b.setupBomberControls(cfg.bomberMoves);
        b.setBorderColor(borderColor);
    }

    // =========================================================================
    // Mouse Input
    // =========================================================================
    /**
     * Handle left-click path guidance and right-click bomb placement marker.
     */
    @Override
    public boolean mouseEvent(GGMouse mouse) {
        Location loc = toLocationInGrid(mouse.getX(), mouse.getY());
        if (mouse.getEvent() == GGMouse.lPress) {
            guidePusherToLocation(loc);
        } else if (mouse.getEvent() == GGMouse.rPress) {
            if (bomber != null && !bomber.isBusy() && bomber.getBombsAvailable() > 0) {
                BombMarker marker = new BombMarker();
                addActor(marker, loc);
                marker.show();
                refresh();
                bomber.startMoveToBomb(loc);
                bomber.setPendingBombMarker(marker);
            }
        }
        return true;
    }

    // =========================================================================
    // Path Planning & Movement
    // =========================================================================
    /**
     * Plan a simple straight-line path (horizontal then vertical) from the pusher
     * to the given target. Stops early if a step is not traversable.
     */
    public void guidePusherToLocation(Location target) {
        if (pusher == null || finished) return;

        Location start = pusher.getLocation();
        pusherPath.clear();
        currentPathIndex = 0;

        // Horizontal leg
        if (start.x != target.x) {
            int dx = target.x > start.x ? 1 : -1;
            for (int x = start.x + dx; x != target.x + dx; x += dx) {
                Location step = new Location(x, start.y);
                if (canMove(step)) pusherPath.add(step); else break;
            }
        }
        // Vertical leg
        Location last = pusherPath.isEmpty() ? start : pusherPath.get(pusherPath.size() - 1);
        if (last.y != target.y) {
            int dy = target.y > last.y ? 1 : -1;
            for (int y = last.y + dy; y != target.y + dy; y += dy) {
                Location step = new Location(last.x, y);
                if (canMove(step)) pusherPath.add(step); else break;
            }
        }
    }

    /**
     * Execute the next step along the planned path:
     * - Turn pusher to face the step direction
     * - Optionally booster-push a rock 1 tile
     * - Move if legal (incl. ore push rule), decrement fuel
     * - Apply pickups (Fuel/Booster), show Target under pusher
     */
    private void executeNextPathStep() {
        if (pusherFuel <= 0) { pusherPath.clear(); currentPathIndex = 0; refresh(); return; }
        if (pusher == null || currentPathIndex >= pusherPath.size()) return;

        Location next = pusherPath.get(currentPathIndex);
        Location cur  = pusher.getLocation();

        // Orient pusher for correct pushing behavior
        if      (next.x > cur.x) pusher.setDirection(Location.EAST);
        else if (next.x < cur.x) pusher.setDirection(Location.WEST);
        else if (next.y > cur.y) pusher.setDirection(Location.SOUTH);
        else if (next.y < cur.y) pusher.setDirection(Location.NORTH);

        // Booster: push rock 1 tile ahead (if active)
        Rock rockAtNext = (Rock) getOneActorAt(next, Rock.class);
        if (rockAtNext != null && boosterReady && boosterCharges > 0) {
            Location pushTo = next.getNeighbourLocation(pusher.getDirection());
            if (canMove(pushTo)) {
                rockAtNext.setLocation(pushTo);
                if (!boosterActivated && boosterCharges == 3) boosterActivated = true;
                if (--boosterCharges == 0) boosterReady = false;
            }
        }

        // Attempt movement (includes ore-push rule)
        if (canMoveWithOrePushing(next)) {
            pusher.setLocation(next);

            // Fuel consumption
            if (pusherFuel > 0) pusherFuel--;

            // Pickup: Fuel → refill
            Fuel can = (Fuel) getOneActorAt(pusher.getLocation(), Fuel.class);
            if (can != null) { can.removeSelf(); pusherFuel = pusherFuel + fuelRefillAmount; }

            // Pickup: Booster → 3 charges
            Booster booster = (Booster) getOneActorAt(pusher.getLocation(), Booster.class);
            if (booster != null) {
                if (!boosterReady && boosterCharges == 0 || boosterActivated) {
                    booster.removeSelf();
                    boosterReady = true; boosterCharges = 3; boosterActivated = false;
                }
            }

            // Reveal target under pusher (visual only)
            Target tgt = (Target) getOneActorAt(pusher.getLocation(), Target.class);
            if (tgt != null) tgt.show();

            currentPathIndex++;
            if (pusherFuel == 0) { pusherPath.clear(); currentPathIndex = 0; }
            refresh();
        } else {
            // Blocked: discard remaining plan
            pusherPath.clear(); currentPathIndex = 0; refresh();
        }
    }

    // ------------------------------------------------------------------------
    // Movement legality with ore-pushing rule
    // ------------------------------------------------------------------------
    /**
     * Return true if the pusher can move into 'next' location. If an Ore is in
     * the way, attempt pushing it one step further in the movement direction.
     */
    private boolean canMoveWithOrePushing(Location next) {
        Color c = getBg().getColor(next);
        if (c.equals(borderColor)) return false;

        // Impassables
        if (getOneActorAt(next, Wall.class) != null) return false;
        if (getOneActorAt(next, HardRock.class) != null) return false;
        if (getOneActorAt(next, Rock.class) != null) return false;
        if (getOneActorAt(next, Bomber.class) != null) return false;

        // Ore push rule
        Ore ore = (Ore) getOneActorAt(next, Ore.class);
        if (ore != null) {
            Location dirFrom = pusher.getLocation();
            Location.CompassDirection pushDir = getPushDirection(dirFrom, next);
            Location dest = next.getNeighbourLocation(pushDir);
            ore.setDirection(pushDir);
            if (canOreMoveToLocation(ore, dest)) { moveOreToLocation(ore, dest); return true; }
            return false;
        }
        return true;
    }

    /** Compute compass direction from 'from' to 'to' (axis-aligned step). */
    private Location.CompassDirection getPushDirection(Location from, Location to) {
        if (to.x > from.x) return Location.EAST;
        if (to.x < from.x) return Location.WEST;
        if (to.y > from.y) return Location.SOUTH;
        if (to.y < from.y) return Location.NORTH;
        return Location.EAST;
    }

    /**
     * Validate ore destination for push: inside bounds, no blocking actors, no border.
     */
    private boolean canOreMoveToLocation(Ore ore, Location dest) {
        if (dest.x < 0 || dest.x >= nbHorzCells || dest.y < 0 || dest.y >= nbVertCells) return false;
        Color c = getBg().getColor(dest);
        if (c.equals(borderColor)) return false;
        if (getOneActorAt(dest, Rock.class) != null) return false;
        if (getOneActorAt(dest, Wall.class) != null) return false;
        if (getOneActorAt(dest, HardRock.class) != null) return false;
        if (getOneActorAt(dest, Pusher.class) != null) return false;
        if (getOneActorAt(dest, Bomber.class) != null) return false;
        Ore other = (Ore) getOneActorAt(dest, Ore.class);
        return other == null || other == ore;
    }

    /**
     * Move ore to destination and update target/collection visuals & counters.
     */
    private void moveOreToLocation(Ore ore, Location dest) {
        Location cur = ore.getLocation();
        Target t = (Target) getOneActorAt(cur, Target.class);
        if (t != null) { t.show(); ore.show(0); } // leaving a target: reveal it

        ore.setLocation(dest);

        // Arrived: hide ore when sitting on target and count towards win
        Target newT = (Target) getOneActorAt(dest, Target.class);
        if (newT != null) { oresCollected++; ore.hide(); }
    }

    // ------------------------------------------------------------------------
    // Basic traversability (used by path planner and booster rock-push preview)
    // ------------------------------------------------------------------------
    /**
     * Check if the pusher may enter 'loc' ignoring ore-push logic.
     * Considers borders/walls/hard-rocks/rocks/bomber presence.
     */
    private boolean canMove(Location loc) {
        Color c = getBg().getColor(loc);
        if (c.equals(borderColor)) return false;
        if (getOneActorAt(loc, Wall.class) != null) return false;
        if (getOneActorAt(loc, HardRock.class) != null) return false;

        // Rock is generally blocking unless a booster push is feasible (one tile ahead is free)
        if (getOneActorAt(loc, Rock.class) != null) {
            if (boosterReady && boosterCharges > 0 && pusher != null) {
                Location pLoc = pusher.getLocation();
                int dx = Integer.compare(loc.x, pLoc.x);
                int dy = Integer.compare(loc.y, pLoc.y);
                if (Math.abs(dx) + Math.abs(dy) == 1) {
                    Location pushTo = new Location(loc.x + dx, loc.y + dy);
                    Color c2 = getBg().getColor(pushTo);
                    if (!c2.equals(borderColor)
                            && getOneActorAt(pushTo, Rock.class) == null
                            && getOneActorAt(pushTo, Wall.class) == null
                            && getOneActorAt(pushTo, HardRock.class) == null
                            && getOneActorAt(pushTo, Bomber.class) == null) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (getOneActorAt(loc, Bomber.class) != null) return false;
        return true;
    }

    // =========================================================================
    // Bomb Updates
    // =========================================================================
    /**
     * Tick active bombs and remove them once finished.
     */
    private void updateBombs() {
        if (bomber == null) return;
        Iterator<Bomb> it = bomber.getBombs().iterator();
        while (it.hasNext()) {
            Bomb b = it.next();
            if (b.isActive()) {
                b.tick();
                if (!b.isActive()) it.remove();
            }
        }
    }

    // =========================================================================
    // Logging (for tests)
    // =========================================================================
    /**
     * Append a compact snapshot of visible actors and key state each tick.
     * Format matches test harness expectations.
     */
    private void updateLogResult() {
        List<Actor> pushers = getActors(Pusher.class);
        List<Actor> ores = getActors(Ore.class);
        List<Actor> targets = getActors(Target.class);
        List<Actor> rocks = getActors(Rock.class);
        List<Actor> bombers = getActors(Bomber.class);
        List<Actor> markers = getActors(BombMarker.class);
        List<Actor> boosters = getActors(Booster.class);
        List<Actor> heavyRocks = getActors(HardRock.class);

        logResult.append(autoMovementIndex).append("#")
                .append(ElementType.PUSHER.getShortType()).append(actorLocations(pushers)).append("-Fuel:").append(pusherFuel).append("#")
                .append(ElementType.ORE.getShortType()).append(actorLocations(ores)).append("#")
                .append(ElementType.TARGET.getShortType()).append(actorLocations(targets)).append("#")
                .append(ElementType.BOULDER.getShortType()).append(actorLocations(rocks)).append("#")
                .append(ElementType.BOMBER.getShortType()).append(actorLocations(bombers)).append("#")
                .append(ElementType.BOMB_MARKER.getShortType()).append(actorLocations(markers)).append("#")
                .append(ElementType.BOOSTER.getShortType()).append(actorLocations(boosters)).append("#")
                .append(ElementType.HARD_ROCK.getShortType()).append(actorLocations(heavyRocks))
                .append("\n");
    }

    /** Convert a list of actors into a condensed ":x-y,..." location string. */
    private String actorLocations(List<Actor> actors) {
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (Actor a : actors) {
            if (a.isVisible()) {
                if (!any) { sb.append(":"); any = true; }
                sb.append(a.getX()).append("-").append(a.getY()).append(",");
            }
        }
        if (any) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    // =========================================================================
    // Accessors
    // =========================================================================
    /** Whether the game loop has concluded (win or time out). */
    public boolean isFinished() { return finished; }
}