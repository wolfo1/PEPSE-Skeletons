package pepse;

import danogl.GameManager;
import danogl.GameObject;
import danogl.collisions.Layer;
import danogl.gui.*;
import danogl.gui.rendering.Camera;
import danogl.util.Counter;
import danogl.util.Vector2;
import pepse.ui.Score;
import pepse.world.Avatar;
import pepse.world.Block;
import pepse.world.NPC.NPCFactory;
import pepse.world.Sky;
import pepse.world.Terrain;
import pepse.world.daynight.Moon;
import pepse.world.daynight.Night;
import pepse.world.daynight.Sun;
import pepse.world.daynight.SunHalo;
import pepse.world.phenomenon.Rain;
import pepse.world.trees.Tree;

import java.awt.*;
import java.util.Random;


public class PepseGameManager extends GameManager {
    // assets
    private static final String SOUNDTRACK_PATH = "src/assets/soundtrack.wav";
    private static final String GAME_OVER_MSG = "Game Over! Do you want to play again?";
    private static final int SEED = 123456;
    // constants
    private static final int MAX_ENEMIES = 2;
    private static final int NIGHT_CYCLE = 30;
    private static final int CHANCE_FOR_RAIN = 3500;
    private static final int MIN_RAIN_DURATION = 400;
    private static final int MAX_RAIN_DUARTION = 1600;
    private static final float MIN_GAP = 50;
    private static final Color SUN_HALO_COLOR = new Color(255, 0, 0, 20);
    private static final Color MOON_HALO_COLOR = new Color(255, 255, 255, 80);
    //layers
    private static final int SKY_LAYER = Layer.BACKGROUND;
    private static final int SUN_LAYER = Layer.BACKGROUND + 1;
    private static final int SUN_HALO_LAYER = Layer.BACKGROUND + 2;
    private static final int MOON_LAYER = Layer.BACKGROUND + 3;
    private static final int MOON_HALO_LAYER = Layer.BACKGROUND + 4;
    private static final int RAIN_LAYER = Layer.STATIC_OBJECTS - 11;
    private static final int LOWER_GROUND_LAYER = Layer.STATIC_OBJECTS - 10;
    private static final int GROUND_LAYER = Layer.STATIC_OBJECTS;
    private static final int PROJECTILES_LAYER = Layer.DEFAULT - 9;
    private static final int TRUNK_LAYER = Layer.DEFAULT - 9;
    private static final int LEAVES_LAYER = Layer.DEFAULT - 7;
    private static final int AVATAR_LAYER = Layer.DEFAULT;
    private static final int NIGHT_LAYER = Layer.FOREGROUND;
    //tags
    private static final String TRUNK_TAG = "trunk";
    private static final String LEAF_TAG = "leaf";
    private static final String GROUND_TAG = "ground";
    private static final String LOWER_GROUND_TAG = "lower ground";
    private static final String ENEMY_TAG = "enemy";
    // game objects
    private Tree tree;
    private GameObject night;
    private GameObject sun;
    private GameObject sunHalo;
    private GameObject moon;
    private GameObject moonHalo;
    private Avatar avatar;
    private Camera camera;
    // infinite world
    private int leftPointer;
    private int rightPointer;
    private static final int extendBy = 10 * Block.SIZE;
    private Terrain terrain;
    private Random random;
    private NPCFactory npcFactory;
    // fields
    private ImageReader imageReader;
    private SoundReader soundReader;
    private WindowController windowController;
    private Vector2 windowDimensions;
    // static fields
    public static Counter score;
    public static Counter numOfEnemiesAlive;

    @Override
    public void initializeGame(ImageReader imageReader, SoundReader soundReader, UserInputListener inputListener, WindowController windowController) {
        score = new Counter();
        numOfEnemiesAlive = new Counter();
        //
        this.random = new Random(SEED);
        this.windowController = windowController;
        super.initializeGame(imageReader, soundReader, inputListener, windowController);
        this.windowDimensions = windowController.getWindowDimensions(); // gets window dimensions
        this.imageReader = imageReader;
        this.soundReader = soundReader;

        Sound sountrack = soundReader.readSound(SOUNDTRACK_PATH);
        sountrack.playLooped();
        //create sky
        Sky.create( gameObjects(), windowDimensions , SKY_LAYER);
        // create terrain
        this.terrain = new Terrain(this.gameObjects(), GROUND_LAYER, windowDimensions, SEED);
        // create trees
        this.tree = new Tree(this.gameObjects(), terrain, SEED, TRUNK_LAYER, LEAVES_LAYER, TRUNK_TAG, LEAF_TAG, GROUND_TAG);
        // create night
        this.night = Night.create(gameObjects(), NIGHT_LAYER, windowDimensions, NIGHT_CYCLE);
        // create sun
        this.sun = Sun.create(gameObjects(), SUN_LAYER, windowDimensions, NIGHT_CYCLE);
        // create halo
        this.sunHalo = SunHalo.create(gameObjects(), SUN_HALO_LAYER, sun, SUN_HALO_COLOR);
        // create moon
        this.moon = Moon.create(gameObjects(), MOON_LAYER, windowDimensions, NIGHT_CYCLE, imageReader);
        // create moon halo
        this.moonHalo = SunHalo.create(gameObjects(), MOON_HALO_LAYER, moon, MOON_HALO_COLOR);
        // create avatar
        this.avatar = Avatar.create(gameObjects(), AVATAR_LAYER, windowDimensions.mult(0.5f), inputListener, imageReader);
        this.avatar.setTerrain(terrain);
        this.avatar.setSounds(soundReader);
        this.avatar.setProjectileLayer(PROJECTILES_LAYER);
        // create camera
        this.camera = new Camera(this.avatar, Vector2.ZERO, windowDimensions, windowDimensions);
        setCamera(camera);
        // create NPCFactory
        this.npcFactory = new NPCFactory(SEED, avatar, gameObjects(), imageReader, soundReader,
                AVATAR_LAYER, windowController, terrain, ENEMY_TAG);
        // create Score UI
        Score score = new Score(PepseGameManager.score, new Vector2(50, this.windowDimensions.y() - 100),
                new Vector2(20, 20), gameObjects());
        gameObjects().addGameObject(score, Layer.UI);
        // create world
        initialWorld();
        // Leaf and block colliding, projectiles colliding with specific layers
        gameObjects().layers().shouldLayersCollide(LEAVES_LAYER, GROUND_LAYER, true);
        gameObjects().layers().shouldLayersCollide(PROJECTILES_LAYER, TRUNK_LAYER, true);
        gameObjects().layers().shouldLayersCollide(PROJECTILES_LAYER, LEAVES_LAYER, true);
        gameObjects().layers().shouldLayersCollide(PROJECTILES_LAYER, Layer.STATIC_OBJECTS, true);
        gameObjects().layers().shouldLayersCollide(PROJECTILES_LAYER, AVATAR_LAYER, true);
        // enemy remains should collide with floor
        gameObjects().layers().shouldLayersCollide(Layer.STATIC_OBJECTS, Layer.STATIC_OBJECTS, true);
    }// overrides initializeGame

    /**
     * Updates the frame
     * @param deltaTime Current time.
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        if (avatar.isDead())
            endGame();
        //the most right x coordinate
        float rightXCoordinate = camera.screenToWorldCoords(windowDimensions).x();
        // the most left x coordinate
        float leftXCoordinate = camera.screenToWorldCoords(windowDimensions).x() - windowDimensions.x();
        // checks if I need to extend to right
        if (rightXCoordinate >= this.rightPointer)
            extendRight(this.rightPointer, rightXCoordinate + extendBy);
        // checks if I need to extend to left
        if (leftXCoordinate <= this.leftPointer)
            extendLeft(leftXCoordinate + MIN_GAP, this.leftPointer - extendBy);
        // check for rain
        if (!Rain.isInstantiated && random.nextInt(CHANCE_FOR_RAIN) == 0) {
            // length can be between 400 and 1600 frames - approx. 10 to 40 seconds.
            int duration = random.nextInt( MAX_RAIN_DUARTION - MIN_RAIN_DURATION) + MIN_RAIN_DURATION;
            Rain.create(gameObjects(), RAIN_LAYER, windowDimensions, imageReader, soundReader, duration);
        }
    } //end of update

    private void initialWorld() {
        Rain.isInstantiated = false;
        float rightXCoordinate = camera.screenToWorldCoords(windowDimensions).x();
        float leftXCoordinate = camera.screenToWorldCoords(windowDimensions).x() - windowDimensions.x();
        this.leftPointer = (int) (Math.floor(leftXCoordinate / Block.SIZE) * Block.SIZE) - extendBy;
        this.rightPointer = (int) (Math.floor(rightXCoordinate / Block.SIZE) * Block.SIZE) + extendBy;
        this.terrain.createInRange(leftPointer, rightPointer);
        this.tree.createInRange(leftPointer, rightPointer);
    } // end of initial world

    private void buildWorld(int start, int end){
        this.terrain.createInRange(start, end);
        this.tree.createInRange(start, end);
        if (numOfEnemiesAlive.value() <= MAX_ENEMIES) {
            npcFactory.createEnemy(random.nextInt(Math.min(start, end), Math.max(start, end)));
            numOfEnemiesAlive.increment();
        }
    } // end of build world

    private void extendRight(float start, float end){
        int normalizeStart = (int) (Math.floor(start / Block.SIZE) * Block.SIZE); // normalize start position
        int normalizeEnd = (int) (Math.floor(end / Block.SIZE) * Block.SIZE); // normalize end position
        // extend right
        buildWorld(normalizeStart, normalizeEnd);
        // remove irrelevant objects from right
        for (GameObject obj : gameObjects()){
            if (obj.getCenter().x() < leftPointer)
                removeObjects(obj);
        } //end of for loop
        this.rightPointer = normalizeEnd; //update right pointer
        this.leftPointer += (normalizeEnd - normalizeStart); //update left pointer
    } // end of extendRight method

    // extends the world to the left
    private void extendLeft(float start, float end){
        int normalizeStart = (int) (Math.floor(start / Block.SIZE) * Block.SIZE); // normalize start position
        int normalizeEnd = (int) (Math.floor(end / Block.SIZE) * Block.SIZE); // normalize end position
        // extend left
        buildWorld(normalizeStart, normalizeEnd);
        // remove irrelevant objects from right
        for (GameObject obj : gameObjects()){
            if (obj.getCenter().x() > this.rightPointer)
                removeObjects(obj);
        }// end of for loop
        this.leftPointer = normalizeEnd; //update left pointer
        this.rightPointer -= (normalizeStart - normalizeEnd); //update right pointer
    } // end of extend left method

    // removes the objects
    private void removeObjects(GameObject obj){
        // remove ground
        if (obj.getTag() .equals(GROUND_TAG))
            gameObjects().removeGameObject(obj, GROUND_LAYER);
        // remove tree trunk
        else if (obj.getTag().equals(TRUNK_TAG))
             gameObjects().removeGameObject(obj, TRUNK_LAYER);
        // remove leaves
        else if (obj.getTag().equals(LEAF_TAG))
             gameObjects().removeGameObject(obj, LEAVES_LAYER);
        // remove bottom bricks
        else if (obj.getTag().equals(LOWER_GROUND_TAG))
            gameObjects().removeGameObject(obj, LOWER_GROUND_LAYER);
        else if (obj.getTag().equals(ENEMY_TAG)) {
            if (gameObjects().removeGameObject(obj, AVATAR_LAYER))
                numOfEnemiesAlive.decrement();
        // delete UI elements
        else
            gameObjects().removeGameObject(obj, Layer.UI);
        }
    } // end of method remove objects

    public void endGame() {
        if (this.windowController.openYesNoDialog(GAME_OVER_MSG))
            windowController.resetGame();
        else
            windowController.closeWindow();
    }
    /**
     * Runs the entire simulation.
     * @param args This argument should not be used.
     */
    public static void main(String[] args){
    new PepseGameManager().run();
    } // end of main

} // end of PepseGameManager
