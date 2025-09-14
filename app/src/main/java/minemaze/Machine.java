package minemaze;

import ch.aplu.jgamegrid.Actor;

// Abstract class for all vehicles/machines
public abstract class Machine extends Actor {
    public Machine(boolean rotatable, String spritePath) {
        super(rotatable, spritePath);
    }
}