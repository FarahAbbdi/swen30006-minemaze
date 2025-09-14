import minemaze.MapGrid;
import minemaze.MineMaze;
import minemaze.PropertiesLoader;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class GameTest {
    @Test
    public void testOriginal() {
        String propertiesPath = "properties/test1.properties";
        final Properties properties = PropertiesLoader.loadPropertiesFile(propertiesPath);
        MapGrid grid = new MapGrid();

        MineMaze game = new MineMaze(properties, grid);
        String logResult = game.runApp(true);
        Assert.assertTrue(logResult.contains("You won"));
        LogParser parser = new LogParser();
        String logLine = parser.getLogLine(logResult, 82);
        Assert.assertTrue(logLine.contains("Ore:7-5"));
    }

    @Test
    public void testFuel() {
        String propertiesPath = "properties/test2.properties";
        final Properties properties = PropertiesLoader.loadPropertiesFile(propertiesPath);
        MapGrid grid = new MapGrid();

        MineMaze game = new MineMaze(properties, grid);
        String logResult = game.runApp(true);
        Assert.assertTrue(logResult.contains("You lost"));
        LogParser parser = new LogParser();
        String logLine83 = parser.getLogLine(logResult, 82);
        String logLine103 = parser.getLogLine(logResult, 102);
        Assert.assertTrue(logLine83.contains("Fuel:17"));

        Assert.assertTrue(logLine103.contains("Fuel:0"));
        Assert.assertTrue(logLine103.contains("Pusher:14-5"));
    }

    /**
     * This tests that the bomber can go to a specific point in the map
     *  leave the bomb and then 6 game tick later, the bomb explodes. There is a maximum number of bombs the bomber can place
     *  The test will check if the logResult still contains the data about the exploded boulder or hark rock
     */
    @Test
    public void testBombPlacement() {
        String propertiesPath = "properties/test3.properties";
        final Properties properties = PropertiesLoader.loadPropertiesFile(propertiesPath);
        MapGrid grid = new MapGrid();

        MineMaze game = new MineMaze(properties, grid);
        String logResult = game.runApp(true);
        LogParser parser = new LogParser();

        String logLine1 = parser.getLogLine(logResult, 0);
        Assert.assertTrue(logLine1.contains("HardRock:7-5,13-7"));
        Assert.assertTrue(logLine1.contains("Boulder:5-6,15-6"));

        String logLine9 = parser.getLogLine(logResult, 8);
        Assert.assertTrue(logLine9.contains("HardRock:13-7"));

        String logLine18 = parser.getLogLine(logResult, 17);
        Assert.assertTrue(logLine18.contains("Boulder:15-6"));

        String logLine29 = parser.getLogLine(logResult, 28);
        Assert.assertTrue(logLine29.contains("Boulder#"));

        String logLine36 = parser.getLogLine(logResult, 35);
        Assert.assertTrue(logLine36.contains("HardRock:13-7"));
    }


    /**
     * This tests that after bomber explodes a boulder, the pusher can acquire the booster and then the fuel
     *  When the pusher acquired the booster, the pusher can push the boulder up to 3 tiles.
     *  The test will try to move the boulder 4 tiles and the app should not allow.
     *  The test will also check the final fuel value
     */
    @Test
    public void testBoosterFuelWithBomb() {
        String propertiesPath = "properties/test4.properties";
        final Properties properties = PropertiesLoader.loadPropertiesFile(propertiesPath);
        MapGrid grid = new MapGrid();

        MineMaze game = new MineMaze(properties, grid);
        String logResult = game.runApp(true);
        LogParser parser = new LogParser();

        String logLine30 = parser.getLogLine(logResult, 29);
        Assert.assertTrue(logLine30.contains("Boulder:2-6,15-6"));
        Assert.assertTrue(logLine30.contains("Fuel:186"));
    }

    @Test
    public void testBoosterFuelWithoutBomb() {
        String propertiesPath = "properties/test5.properties";
        final Properties properties = PropertiesLoader.loadPropertiesFile(propertiesPath);
        MapGrid grid = new MapGrid();

        MineMaze game = new MineMaze(properties, grid);
        String logResult = game.runApp(true);
        LogParser parser = new LogParser();

        String logLine15 = parser.getLogLine(logResult, 14);
        Assert.assertTrue(logLine15.contains("Boulder:2-6,15-6"));
        Assert.assertTrue(logLine15.contains("Fuel:285"));
    }



}
