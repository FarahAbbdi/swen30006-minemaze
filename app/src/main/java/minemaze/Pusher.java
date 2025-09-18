package minemaze;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.Location;
import java.util.List;

public class Pusher extends Actor {
    private List<String> controls = null;
    private final MineMaze controller;   // association back to MineMaze

    public Pusher(MineMaze controller) {
        super(true, "sprites/pusher.png");  // Rotatable
        this.controller = controller;
    }

    public void setupPusher(boolean isAutoMode, List<String> pusherControls) {
        this.controls = pusherControls;
    }

    /**
     * Move automatically based on the instructions from the properties file.
     * The current auto-move index is passed in from MineMaze's loop.
     */
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

            // Ask the controller to guide the path
            controller.guidePusherToLocation(targetLocation);
        }
    }
}