package minemaze;

import ch.aplu.jgamegrid.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * MineMaze
 * ---------
 * Main game controller and orchestrator.
 */
public class MineMaze extends GameGrid implements GGMouseListener {

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

        public static ElementType getElementByShortType(String shortType) {
            for (ElementType t : values()) if (t.getShortType().equals(shortType)) return t;
            return EMPTY;
        }
    }

    public static final String BOMB_COMMAND = "Bomb";

    private final MapGrid grid;
    private final int nbHorzCells;
    private final int nbVertCells;
    private final Color borderColor = new Color(100, 100, 100);

    // Rendering & HUD
    private final BoardRenderer boardRenderer = new BoardRenderer(borderColor);
    private final HudRenderer hud = new HudRenderer();

    // Config
    private final GameConfig cfg;

    // Primary actors
    private Pusher pusher;
    private Bomber bomber;

    // Game state
    private boolean finished = false;
    private double gameDuration;
    private final int oresWinning;
    private int oresCollected = 0;
    private int autoMovementIndex = 0;

    // Log buffer used by tests to verify game progress
    private final StringBuilder logResult = new StringBuilder();

    public MineMaze(Properties properties, MapGrid grid) {
        super(grid.getNbHorzCells(), grid.getNbVertCells(), 30, false);
        this.grid = grid;
        this.nbHorzCells = grid.getNbHorzCells();
        this.nbVertCells = grid.getNbVertCells();

        this.cfg = new GameConfig(properties);
        setSimulationPeriod(cfg.simulationPeriodMs);
        this.gameDuration = cfg.durationSeconds;
        this.oresWinning = cfg.oresWinning;
    }

    public String runApp(boolean showUI) {
        // Draw the static board (rendering responsibility stays in BoardRenderer)
        boardRenderer.drawBoard(getBg(), grid);

        // Create actors (creation responsibility is in MineMazeCreator)
        MineMazeCreator.createActors(this, cfg, grid);

        // HUD & input (unchanged)
        getBg().setFont(new Font("Arial", Font.BOLD, 14));
        hud.drawControlsHelp(getBg(), 30, nbVertCells);
        addMouseListener(this, GGMouse.lPress | GGMouse.rPress);

        if (showUI) show();
        if (cfg.autoMode) doRun();

        while (oresCollected < oresWinning && gameDuration >= 0) {
            try {
                Thread.sleep(getSimulationPeriod());
                gameDuration -= getSimulationPeriod() / 1000.0;
                setTitle(String.format("Ores: %d/%d | Time: %.1fs", oresCollected, oresWinning, gameDuration));

                if (cfg.autoMode) {
                    if (pusher != null) pusher.autoMoveNext(autoMovementIndex);
                    if (bomber != null) bomber.autoMoveNext(autoMovementIndex, BOMB_COMMAND, this::refresh);
                    if (pusher != null) pusher.executeNextPathStep();
                    autoMovementIndex++;
                } else {
                    if (pusher != null) pusher.executeNextPathStep();
                }

                if (bomber != null) bomber.handleMovement();

                updateBombs();
                refresh();
                updateLogResult();
                hud.updateStatusDisplay(getBg());

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        doPause();
        setTitle(oresCollected == oresWinning ? "Mission Complete. Well done!" : "Mission Failed. You ran out of time");
        logResult.append(oresCollected == oresWinning ? "You won" : "You lost");
        finished = true;
        return logResult.toString();
    }

    void onPusherCreated(Pusher p) {
        this.pusher = p;
        p.setupPusher(cfg.autoMode, cfg.pusherMoves);
    }

    void onBomberCreated(Bomber b) {
        this.bomber = b;
        b.setupBomberControls(cfg.bomberMoves);
        b.setBorderColor(borderColor);
    }

    @Override
    public boolean mouseEvent(GGMouse mouse) {
        Location loc = toLocationInGrid(mouse.getX(), mouse.getY());
        if (mouse.getEvent() == GGMouse.lPress) {
            if (pusher != null) pusher.guideToLocation(loc);
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

    private void updateLogResult() {
        List<Actor> pushers = getActors(Pusher.class);
        List<Actor> ores = getActors(Ore.class);
        List<Actor> targets = getActors(Target.class);
        List<Actor> rocks = getActors(Rock.class);
        List<Actor> bombers = getActors(Bomber.class);
        List<Actor> markers = getActors(BombMarker.class);
        List<Actor> boosters = getActors(Booster.class);
        List<Actor> heavyRocks = getActors(HardRock.class);

        int pusherFuel = pusher != null ? pusher.getFuel() : 0;

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

    public void incrementOresCollected() {
        oresCollected++;
    }

    public int getNbHorzCells() { return nbHorzCells; }
    public int getNbVertCells() { return nbVertCells; }
    public Color getBorderColor() { return borderColor; }
    public boolean isFinished() { return finished; }
}