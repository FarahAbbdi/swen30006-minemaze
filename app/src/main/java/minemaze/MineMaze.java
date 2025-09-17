package minemaze;

import ch.aplu.jgamegrid.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Properties;

public class MineMaze extends GameGrid implements GGMouseListener {

    public enum ElementType {
        OUTSIDE("Outside", ' '), EMPTY("Empty", '.'), BORDER("Border", 'x'),
        PUSHER("Pusher", 'P'), ORE("Ore", '*'), BOULDER("Boulder", 'r'), TARGET("Target", 'o'),
        BOMB_MARKER("BombMarker", 'm'), BOOSTER("Booster", 'b'), HARD_ROCK("HardRock", 'h'),
        BOMBER("Bomber", 'B');
        private String shortType;
        private char mapElement;

        ElementType(String shortType, char mapElement) {
            this.shortType = shortType;
            this.mapElement = mapElement;
        }

        public String getShortType() {
            return shortType;
        }

        public char getMapElement() {
            return mapElement;
        }

        public static ElementType getElementByShortType(String shortType) {
            ElementType[] types = ElementType.values();
            for (ElementType type : types) {
                if (type.getShortType().equals(shortType)) {
                    return type;
                }
            }

            return ElementType.EMPTY;
        }
    }

    private class Target extends Actor {
        public Target() {
            super("sprites/target.gif");
        }
    }

    protected class Ore extends Actor {
        public Ore() {
            super("sprites/ore.png", 2);
        }
    }

    protected class Pusher extends Actor {
        private List<String> controls = null;

        public Pusher() {
            super(true, "sprites/pusher.png");  // Rotatable
        }

        public void setupPusher(boolean isAutoMode, List<String> pusherControls) {
            this.controls = pusherControls;
        }

        /**
         * Method to move pusher automatically based on the instructions input from properties file
         */
        public void autoMoveNext() {
            if (controls != null && autoMovementIndex < controls.size()) {
                String currentMove = controls.get(autoMovementIndex);
                String[] parts = currentMove.split("-");

                if (parts.length == 2) {
                    int targetX = Integer.parseInt(parts[0]);
                    int targetY = Integer.parseInt(parts[1]);
                    Location targetLocation = new Location(targetX, targetY);

                    if (isFinished)
                        return;

                    // Guide pusher to target location
                    MineMaze.this.guidePusherToLocation(targetLocation);
                }
            }
        }
    }


    protected class Rock extends Actor {
        public Rock() {
            super("sprites/rock.png");
        }
    }

    protected class Wall extends Actor {
        public Wall() {
            super("sprites/wall.png");
        }
    }

    protected class Booster extends Actor {
        public Booster() {
            super("sprites/booster.png");
        }
    }

    protected class Fuel extends Actor {
        public Fuel() {
            super("sprites/fuel.png");
        }
    }

    protected class HardRock extends Actor {
        public HardRock() {
            super("sprites/hard_rock.png");
        }
    }

    public static final String BOMB_COMMAND = "Bomb";

    // ------------- End of inner classes ------
    //
    private MapGrid grid;
    private int nbHorzCells;
    private int nbVertCells;
    private final Color borderColor = new Color(100, 100, 100);
    private Ore[] ores;
    private Pusher pusher;
    private Bomber bomber;
    private boolean isFinished = false;
    private boolean isAutoMode;
    private double gameDuration;
    private List<String> pusherControls;
    private List<String> bomberControls;
    private StringBuilder logResult = new StringBuilder();
    private List<Location> pusherPath;
    private int currentPathIndex;
    private final int oresWinning;
    private int oresCollected;
    private int maxNumberOfBombs;
    private int autoMovementIndex = 0;

    public MineMaze(Properties properties, MapGrid grid) {
        super(grid.getNbHorzCells(), grid.getNbVertCells(), 30, false);
        this.grid = grid;
        nbHorzCells = grid.getNbHorzCells();
        nbVertCells = grid.getNbVertCells();

        isAutoMode = properties.getProperty("movement.mode").equals("auto");
        gameDuration = Integer.parseInt(properties.getProperty("duration"));
        setSimulationPeriod(Integer.parseInt(properties.getProperty("simulationPeriod")));
        String pusherMovementsStr = properties.getProperty("pusher.movements", "");
        String bomberMovementsStr = properties.getProperty("bomber.movements", "");
        oresWinning = Integer.parseInt(properties.getProperty("ores.winning"));
        maxNumberOfBombs = Integer.parseInt(properties.getProperty("bomb.max"));
        String oreLocations = properties.getProperty("ore.locations");
        String fuelLocations = properties.getProperty("fuel.locations");
        String boosterLocations = properties.getProperty("booster.locations");
        int initialFuelAmount = Integer.parseInt(properties.getProperty("fuel.initial")); // Used to set the initial amount of fuel for pusher

        drawExtraActors(oreLocations, fuelLocations, boosterLocations);

        pusherControls = pusherMovementsStr.isEmpty() ? new ArrayList<>() :
                Arrays.asList(pusherMovementsStr.split(";"));
        bomberControls = bomberMovementsStr.isEmpty() ? new ArrayList<>() :
                Arrays.asList(bomberMovementsStr.split(";"));
    }

    /**
     * The main method to run the game
     *
     * @param isDisplayingUI
     * @return
     */
    public String runApp(boolean isDisplayingUI) {
        GGBackground bg = getBg();
        drawBoard(bg);
        drawActors();
        Font myFont = new Font("Arial", Font.BOLD, 14);
        bg.setFont(myFont);
        drawControlsHelp(bg);
        addMouseListener(this, GGMouse.lPress | GGMouse.rPress);
        pusherPath = new ArrayList<>();
        currentPathIndex = 0;

        if (isDisplayingUI) show();

        if (isAutoMode) doRun();

        double ONE_SECOND = 1000.0;
        double gameTick = 0;
        // Updated section of the main game loop in MineMaze.runApp()
        while (oresCollected < oresWinning && gameDuration >= 0) {
            try {
                Thread.sleep(simulationPeriod);
                gameTick++;
                double minusDuration = (simulationPeriod / ONE_SECOND);
                gameDuration -= minusDuration;
                setTitle(generateGameTitle(gameDuration));

                if (isAutoMode) {
                    // Process pusher auto movement
                    pusher.autoMoveNext();

                    // Process bomber auto movement
                    if (bomber != null) {
                        boolean commandProcessed = bomber.autoMoveNext(autoMovementIndex, BOMB_COMMAND, this::refresh);

                        // Only increment if command was fully processed
                        if (commandProcessed) {
                            autoMovementIndex++;
                        }
                    } else {
                        // No bomber, still increment to keep pusher and bomber in sync
                        autoMovementIndex++;
                    }

                    // Execute pusher path movement
                    executeNextPathStep();
                    // Increment movement index for next iteration
                    autoMovementIndex++;
                } else {
                    // Manual mode - just execute pusher path steps
                    executeNextPathStep();
                }

                // Handle bomber movement in both modes (this processes the step-by-step movement)
                if (bomber != null) {
                    bomber.handleMovement();
                }

                // Update all active bombs (tick countdown)
                updateBombs();

                // Update display
                refresh();
                updateLogResult();
                updateStatusDisplay();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        doPause();
        if (oresCollected == oresWinning) {
            setTitle("Mission Complete. Well done!");
            logResult.append("You won");
        } else if (gameDuration < 0) {
            setTitle("Mission Failed. You ran out of time");
            logResult.append("You lost");
        }
        isFinished = true;
        return logResult.toString();
    }

    /**
     * Transform the list of actors to a string of location for a specific kind of actor.
     *
     * @param actors
     * @return
     */
    private String actorLocations(List<Actor> actors) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean hasAddedColon = false;
        boolean hasAddedLastComma = false;
        for (int i = 0; i < actors.size(); i++) {
            Actor actor = actors.get(i);
            if (actor.isVisible()) {
                if (!hasAddedColon) {
                    stringBuilder.append(":");
                    hasAddedColon = true;
                }
                stringBuilder.append(actor.getX() + "-" + actor.getY());
                stringBuilder.append(",");
                hasAddedLastComma = true;
            }
        }

        if (hasAddedLastComma) {
            stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), "");
        }

        return stringBuilder.toString();
    }

    private void drawExtraActors(String oreLocationString, String fuelLocationString, String boosterLocationString) {
        String[] oreLocations = oreLocationString.split(";");
        ores = new Ore[oreLocations.length];
        for (int i = 0; i < oreLocations.length; i++) {
            String[] coordinates = oreLocations[i].split("-");
            ores[i] = new Ore();
            addActor(ores[i], new Location(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
        }

        String[] fuelLocations = fuelLocationString.split(";");
        for (int i = 0; i < fuelLocations.length; i++) {
            String[] coordinates = fuelLocations[i].split("-");
            addActor(new Fuel(), new Location(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
        }

        String[] boosterLocations = boosterLocationString.split(";");
        for (int i = 0; i < boosterLocations.length; i++) {
            String[] coordinates = boosterLocations[i].split("-");
            addActor(new Booster(), new Location(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
        }
    }

    /**
     * Draw all different actors on the board: pusher, ore, target, rock, clay, bulldozer, excavator
     */
    private void drawActors() {
        for (int y = 0; y < nbVertCells; y++) {
            for (int x = 0; x < nbHorzCells; x++) {
                Location location = new Location(x, y);
                ElementType a = grid.getCell(location);
                if (a == ElementType.PUSHER) {
                    pusher = new Pusher();
                    addActor(pusher, location);
                    pusher.setupPusher(isAutoMode, pusherControls);
                }
                if (a == ElementType.TARGET) {
                    addActor(new Target(), location);
                }
                if (a == ElementType.BOULDER) {
                    addActor(new Rock(), location);
                }
                if (a == ElementType.BOOSTER) {
                    addActor(new Booster(), location);
                }
                if (a == ElementType.HARD_ROCK) {
                    addActor(new HardRock(), location);
                }
                if (a == ElementType.BOMBER) {
                    bomber = new Bomber(location, maxNumberOfBombs, this);
                    addActor(bomber, location);
                    bomber.setupBomberControls(bomberControls);
                    bomber.setBorderColor(borderColor);  // Add this line
                }
            }
        }
        setPaintOrder(Target.class);
    }

    /**
     * Draw the basic board with outside color and border color
     *
     * @param bg
     */

    private void drawBoard(GGBackground bg) {
        bg.clear(new Color(230, 230, 230));
        bg.setPaintColor(Color.darkGray);
        for (int y = 0; y < nbVertCells; y++) {
            for (int x = 0; x < nbHorzCells; x++) {
                Location location = new Location(x, y);
                ElementType a = grid.getCell(location);
                if (a != ElementType.OUTSIDE) {
                    bg.fillCell(location, Color.lightGray);
                }
                if (a == ElementType.BORDER)  // Border
                    bg.fillCell(location, borderColor);
            }
        }
    }

    public boolean mouseEvent(GGMouse mouse) {
        Location location = toLocationInGrid(mouse.getX(), mouse.getY());
        if (mouse.getEvent() == GGMouse.lPress) {
            guidePusherToLocation(location);
        } else if (mouse.getEvent() == GGMouse.rPress) {
            if (bomber != null && !bomber.isBusy() && bomber.getBombsAvailable() > 0) {
                // Add bomb marker at the clicked location
                BombMarker marker = new BombMarker();
                addActor(marker, location);
                marker.show();
                refresh(); // update UI

                // Start bomber movement toward the marker
                bomber.startMoveToBomb(location);
                bomber.setPendingBombMarker(marker);
            }
        }
        return true;
    }

    private void guidePusherToLocation(Location target) {
        if (pusher == null || isFinished) {
            return;
        }

        Location pusherLoc = pusher.getLocation();
        pusherPath.clear();
        currentPathIndex = 0;
        // Calculate straight-line path (horizontal first, then vertical)
        if (pusherLoc.x != target.x) {
            // Move horizontally
            int dx = target.x > pusherLoc.x ? 1 : -1;
            for (int x = pusherLoc.x + dx; x != target.x + dx; x += dx) {
                Location step = new Location(x, pusherLoc.y);
                if (canMove(step)) {
                    pusherPath.add(step);
                } else {
                    break; // Stop if path is blocked
                }
            }
        }

        // Move vertically from the end of horizontal movement
        Location lastHorizontal = pusherPath.isEmpty() ? pusherLoc : pusherPath.get(pusherPath.size() - 1);
        if (lastHorizontal.y != target.y) {
            int dy = target.y > lastHorizontal.y ? 1 : -1;
            for (int y = lastHorizontal.y + dy; y != target.y + dy; y += dy) {
                Location step = new Location(lastHorizontal.x, y);
                if (canMove(step)) {
                    pusherPath.add(step);
                } else {
                    break; // Stop if path is blocked
                }
            }
        }
    }

    private void executeNextPathStep() {
        if (currentPathIndex < pusherPath.size()) {
            Location nextStep = pusherPath.get(currentPathIndex);
            Location currentLoc = pusher.getLocation();

            // Set direction first for proper ore pushing
            if (nextStep.x > currentLoc.x) pusher.setDirection(Location.EAST);
            else if (nextStep.x < currentLoc.x) pusher.setDirection(Location.WEST);
            else if (nextStep.y > currentLoc.y) pusher.setDirection(Location.SOUTH);
            else if (nextStep.y < currentLoc.y) pusher.setDirection(Location.NORTH);

            if (canMoveWithOrePushing(nextStep)) {
                pusher.setLocation(nextStep);

                // Handle target visibility after movement
                Target curTarget = (Target) getOneActorAt(pusher.getLocation(), Target.class);
                if (curTarget != null) {
                    curTarget.show();
                }

                currentPathIndex += 1;
                refresh();
            } else {
                // Clear path if blocked
                pusherPath.clear();
                currentPathIndex = 0;
                refresh();
            }
        }
    }

    private boolean canMoveWithOrePushing(Location nextLocation) {
        // First check if location has impassable obstacles (walls, rocks, etc.)
        Color c = getBg().getColor(nextLocation);
        Wall wall = (Wall) getOneActorAt(nextLocation, Wall.class);
        HardRock heavyRock = (HardRock) getOneActorAt(nextLocation, HardRock.class);
        Rock rock = (Rock) getOneActorAt(nextLocation, Rock.class);
        Bomber bomberAtLocation = (Bomber) getOneActorAt(nextLocation, Bomber.class);

        // Check for impassable obstacles
        if (c.equals(borderColor) || wall != null || bomberAtLocation != null) {
            return false;
        }

        if (heavyRock != null) {
            return false;
        }

        // Regular rocks block movement
        if (rock != null) {
            return false;
        }

        // Check if there's an ore at the target location
        Ore ore = (Ore) getOneActorAt(nextLocation, Ore.class);
        if (ore != null) {
            // Calculate where the ore should be pushed to
            Location pusherCurrent = pusher.getLocation();
            Location.CompassDirection pushDirection = getPushDirection(pusherCurrent, nextLocation);
            Location oreDestination = nextLocation.getNeighbourLocation(pushDirection);

            // Set ore direction to match push direction
            ore.setDirection(pushDirection);

            // Check if ore can be pushed to destination
            if (canOreMoveToLocation(ore, oreDestination)) {
                // Move the ore to its destination
                moveOreToLocation(ore, oreDestination);
                return true; // Pusher can now move to the ore's original location
            } else {
                return false; // Ore can't be pushed, so pusher can't move
            }
        }

        return true; // No obstacles, can move
    }

    private Location.CompassDirection getPushDirection(Location from, Location to) {
        if (to.x > from.x) return Location.EAST;
        if (to.x < from.x) return Location.WEST;
        if (to.y > from.y) return Location.SOUTH;
        if (to.y < from.y) return Location.NORTH;
        return Location.EAST; // Default
    }

    private boolean canOreMoveToLocation(Ore ore, Location destination) {
        // Check if destination is valid
        if (destination.x < 0 || destination.x >= nbHorzCells ||
                destination.y < 0 || destination.y >= nbVertCells) {
            return false;
        }

        // Check for obstacles at destination
        Color c = getBg().getColor(destination);
        Rock rock = (Rock) getOneActorAt(destination, Rock.class);
        Wall wall = (Wall) getOneActorAt(destination, Wall.class);
        HardRock heavyRock = (HardRock) getOneActorAt(destination, HardRock.class);
        Pusher pusherAtDest = (Pusher) getOneActorAt(destination, Pusher.class);
        Bomber bomberAtDest = (Bomber) getOneActorAt(destination, Bomber.class);

        if (c.equals(borderColor) || rock != null || wall != null || heavyRock != null ||
                pusherAtDest != null || bomberAtDest != null) {
            return false;
        }

        // Check for another ore at destination
        Ore otherOre = (Ore) getOneActorAt(destination, Ore.class);
        if (otherOre != null && otherOre != ore) {
            return false;
        }

        return true;
    }

    private void moveOreToLocation(Ore ore, Location destination) {
        // Handle target visibility at current location
        Location currentLocation = ore.getLocation();
        Target currentTarget = (Target) getOneActorAt(currentLocation, Target.class);
        if (currentTarget != null) {
            currentTarget.show(); // Show target when ore leaves
            ore.show(0); // Show ore in normal state
        }

        // Move ore to new location
        ore.setLocation(destination);

        // Check if ore is now on a target
        Target newTarget = (Target) getOneActorAt(destination, Target.class);
        if (newTarget != null) {
          oresCollected ++;
          ore.hide();
        }
    }

    private Location getLocationAtDistance(Location start, int direction, int distance) {
        Location current = start;
        for (int i = 0; i < distance; i++) {
            current = current.getNeighbourLocation(direction);
        }
        return current;
    }

    /**
     * Update all active bombs - called each game tick
     */
    private void updateBombs() {
        if (bomber == null) return;

        List<Bomb> bombs = bomber.getBombs();

        // Create a copy to avoid concurrent modification during iteration
        List<Bomb> bombsCopy = new ArrayList<>(bombs);

        for (Bomb bomb : bombsCopy) {
            if (bomb.isActive()) {
                bomb.tick();

                // Remove exploded bombs from the bomber's list
                if (!bomb.isActive()) {
                    bombs.remove(bomb);
                }
            }
        }
    }

    /**
     * Check if we can move the pusher into the location
     *
     * @param location
     * @return
     */
    private boolean canMove(Location location) {
        // Test if try to move into border, rock, wall, or heavy rock
        Color c = getBg().getColor(location);
        Rock rock = (Rock) getOneActorAt(location, Rock.class);
        Wall wall = (Wall) getOneActorAt(location, Wall.class);
        HardRock heavyRock = (HardRock) getOneActorAt(location, HardRock.class);
        Bomber bomber = (Bomber) getOneActorAt(location, Bomber.class);
        // Check if heavy rock requires strength booster
        if (heavyRock != null) {
            return false;
        }

        if (c.equals(borderColor) || rock != null || wall != null || bomber != null)
            return false;

        return true;
    }


    /**
     * The method will generate a log result for all the movements of all actors
     * The log result will be tested against our expected output.
     * Your code will need to pass all the 3 test suites with 9 test cases.
     */
    private void updateLogResult() {
        List<Actor> pushers = getActors(Pusher.class);
        List<Actor> ores = getActors(Ore.class);
        List<Actor> targets = getActors(Target.class);
        List<Actor> rocks = getActors(Rock.class);
        List<Actor> bombers = getActors(Bomber.class);
        List<Actor> bombMarkers = getActors(BombMarker.class);
        List<Actor> boosters = getActors(Booster.class);
        List<Actor> heavyRocks = getActors(HardRock.class);

        logResult.append(autoMovementIndex + "#");
        logResult.append(ElementType.PUSHER.getShortType()).append(actorLocations(pushers)).append("-Fuel:100").append("#");
        logResult.append(ElementType.ORE.getShortType()).append(actorLocations(ores)).append("#");
        logResult.append(ElementType.TARGET.getShortType()).append(actorLocations(targets)).append("#");
        logResult.append(ElementType.BOULDER.getShortType()).append(actorLocations(rocks)).append("#");
        logResult.append(ElementType.BOMBER.getShortType()).append(actorLocations(bombers)).append("#");
        logResult.append(ElementType.BOMB_MARKER.getShortType()).append(actorLocations(bombMarkers)).append("#");
        logResult.append(ElementType.BOOSTER.getShortType()).append(actorLocations(boosters)).append("#");
        logResult.append(ElementType.HARD_ROCK.getShortType()).append(actorLocations(heavyRocks));

        logResult.append("\n");
    }

    private String generateGameTitle(double timeLeft) {
        StringBuilder title = new StringBuilder();

        // Basic game info
        title.append(String.format("Ores: %d/%d | Time: %.1fs",
                oresCollected, oresWinning, timeLeft));

        return title.toString();
    }

    private void updateStatusDisplay() {
        if (pusher == null) return;

        GGBackground bg = getBg();

        // Clear previous status text area (top portion of the game)
        bg.setPaintColor(new Color(240, 240, 240)); // Light gray background

        // Draw status bars for pusher
        drawStatusBar(bg, 10, 20, "PUSHER");

        drawBombCountdown(bg);

        // Draw controls help
    }

    private void drawStatusBar(GGBackground bg, int x, int y, String name) {
        bg.setPaintColor(Color.BLACK);
        bg.drawText(name + ":", new Point(x, y));

        // Fuel bar
        bg.drawText("Fuel: 0", new Point(x + 70, y));

        // Durability bar
        bg.drawText("Durability: 0", new Point(x + 120, y));
    }

    private void drawBombCountdown(GGBackground bg) {
        bg.setPaintColor(Color.RED);
        bg.drawText("BOMBS: 3s", new Point(10, 45));
    }

    private void drawControlsHelp(GGBackground bg) {
        bg.setPaintColor(Color.DARK_GRAY);
        String controls = "Controls: Left Click=Guide Pusher";
        bg.drawText(controls, new Point(0, nbVertCells * 30 - 30)); // Bottom of screen
        String controls2 = "Right Click=Place Bomb";
        bg.drawText(controls2, new Point(0, nbVertCells * 30 - 15)); // Bottom of screen
    }

}
