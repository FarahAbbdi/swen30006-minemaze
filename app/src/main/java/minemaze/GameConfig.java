package minemaze;

import java.util.*;

/**
 * GameConfig
 * -----------
 * Immutable snapshot of game configuration parsed from Properties.
 * Centralizes string parsing and provides typed accessors.
 */
public final class GameConfig {
    public final boolean autoMode;
    public final int simulationPeriodMs;
    public final double durationSeconds;
    public final int oresWinning;
    public final int maxBombs;
    public final String oreLocations;
    public final String fuelLocations;
    public final String boosterLocations;
    public final List<String> pusherMoves;
    public final List<String> bomberMoves;
    public final int initialFuel;

    public GameConfig(Properties props) {
        this.autoMode = "auto".equals(props.getProperty("movement.mode"));
        this.simulationPeriodMs = Integer.parseInt(props.getProperty("simulationPeriod"));
        this.durationSeconds = Double.parseDouble(props.getProperty("duration"));
        this.oresWinning = Integer.parseInt(props.getProperty("ores.winning"));
        this.maxBombs = Integer.parseInt(props.getProperty("bomb.max"));
        this.oreLocations = props.getProperty("ore.locations", "");
        this.fuelLocations = props.getProperty("fuel.locations", "");
        this.boosterLocations = props.getProperty("booster.locations", "");
        this.initialFuel = Integer.parseInt(props.getProperty("fuel.initial", "100"));

        String pusherMovementsStr = props.getProperty("pusher.movements", "");
        this.pusherMoves = pusherMovementsStr.isEmpty() ? new ArrayList<>() : Arrays.asList(pusherMovementsStr.split(";"));

        String bomberMovementsStr = props.getProperty("bomber.movements", "");
        this.bomberMoves = bomberMovementsStr.isEmpty() ? new ArrayList<>() : Arrays.asList(bomberMovementsStr.split(";"));
    }
}