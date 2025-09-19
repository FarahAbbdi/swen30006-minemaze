package minemaze;

import ch.aplu.jgamegrid.GGBackground;
import ch.aplu.jgamegrid.Location;

import java.awt.*;

/**
 * BoardRenderer
 * --------------
 * Renders static board tiles based on MapGrid contents.
 */
public final class BoardRenderer {
    private final Color borderColor;

    public BoardRenderer(Color borderColor) {
        this.borderColor = borderColor;
    }

    /** Draw basic board with outside color and border color. */
    public void drawBoard(GGBackground bg, MapGrid grid) {
        int w = grid.getNbHorzCells();
        int h = grid.getNbVertCells();
        bg.clear(new Color(230, 230, 230));
        bg.setPaintColor(Color.darkGray);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Location loc = new Location(x, y);
                MineMaze.ElementType t = grid.getCell(loc);
                if (t != MineMaze.ElementType.OUTSIDE) {
                    bg.fillCell(loc, Color.lightGray);
                }
                if (t == MineMaze.ElementType.BORDER) {
                    bg.fillCell(loc, borderColor);
                }
            }
        }
    }
}