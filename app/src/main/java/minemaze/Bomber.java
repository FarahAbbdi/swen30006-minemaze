package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;

import java.util.List;

public class Bomber extends Actor {
    private List<String> controls = null;

    public Bomber() {
        super(true, "sprites/bomber.png");  // Rotatable
    }

    public void setupBomber(List<String> bomberControls) {
        this.controls = bomberControls;
    }

    /**
     * Auto move for Bomber. Receives context from MineMaze.
     *
     * @param autoMovementIndex current movement index
     * @param isFinished        whether the game is finished
     * @param bombCommand       the bomb command string
     * @param refresh           a callback to refresh the UI
     */
    public void autoMoveNext(int autoMovementIndex, boolean isFinished, String bombCommand, Runnable refresh) {
        if (controls != null && autoMovementIndex < controls.size()) {
            String currentMove = controls.get(autoMovementIndex);
            String[] parts = currentMove.split("-");
            if (currentMove.equals(bombCommand)) {
                // Place bomb here
                System.out.println("Place bomb at current position");
                refresh.run();
                return;
            }
            if (parts.length == 2) {
                int bombX = Integer.parseInt(parts[0]);
                int bombY = Integer.parseInt(parts[1]);
                setLocation(new Location(bombX, bombY));
                if (isFinished)
                    return;

                refresh.run();
            }
        }
    }
}
