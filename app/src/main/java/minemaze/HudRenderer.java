package minemaze;

import ch.aplu.jgamegrid.GGBackground;

import java.awt.*;

/**
 * HudRenderer
 * ------------
 * Responsible for drawing simple HUD text (controls & placeholders for status).
 */
public final class HudRenderer {
    public void drawControlsHelp(GGBackground bg, int cellSize, int gridHeight) {
        bg.setPaintColor(Color.DARK_GRAY);
        bg.drawText("Controls: Left Click=Guide Pusher", new Point(0, gridHeight * cellSize - 30));
        bg.drawText("Right Click=Place Bomb", new Point(0, gridHeight * cellSize - 15));
    }

    public void updateStatusDisplay(GGBackground bg) {
        bg.setPaintColor(new Color(240, 240, 240));
        drawStatusBar(bg, 10, 20, "PUSHER");
        drawBombCountdown(bg);
    }

    private void drawStatusBar(GGBackground bg, int x, int y, String name) {
        bg.setPaintColor(Color.BLACK);
        bg.drawText(name + ":", new Point(x, y));
        bg.drawText("Fuel: 0", new Point(x + 70, y));
        bg.drawText("Durability: 0", new Point(x + 120, y));
    }

    private void drawBombCountdown(GGBackground bg) {
        bg.setPaintColor(Color.RED);
        bg.drawText("BOMBS: 3s", new Point(10, 45));
    }
}