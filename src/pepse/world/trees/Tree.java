
package pepse.world.trees;

import danogl.GameObject;
import danogl.collisions.GameObjectCollection;
import danogl.components.GameObjectPhysics;
import danogl.components.ScheduledTask;
import danogl.components.Transition;
import danogl.gui.rendering.RectangleRenderable;
import danogl.util.Vector2;
import pepse.world.Block;
import pepse.world.Terrain;

import java.awt.*;
import java.util.Objects;
import java.util.Random;

public class Tree {
    //constants
    private static final Color LEAF_COLOUR = new Color(50,200,30);
    private static final Color TRUNK_COLOUR =new Color(100,50,20);
    private static final int MINIMAL_DISTANCE_BETWEEN_TREES = 300 ;
    private static final float FADEOUT_TIME = 10;
    private static final int MAX_HEIGHT = 10;
    private static final int MIN_HEIGHT = 5;
    private static final float ODDS = 0.7f;
    private static final int COLOUR_DELTA = 10;
    private static final int LEAF_COLOUR_DELTA = 20;
    private static final int LEAF_ANIMATION_WAIT_TIME = 19;
    private static final int LEAF_FALL_WAIT_TIME = 60;
    private static final int LEAF_FALL_DELAY = 7;
    private static final int TRANSITION_RANDOM_DELAY = 7;
    private static final int OPAQUENESS = 1;
    private static final int HALF = 2;
    private static final int DELAY = 1;
    private static final int FALL_DELAY = 2;
    private static final int TRANSITION_DELAY = 3;
    private static final int LEAF_FALL_RANDOM = 5;
    // fields
    private final GameObjectCollection gameObjects;
    private final Terrain terrain;
    private final Random rand;
    private final int trunkLayer;
    private final int leavesLayer;
    private final int seed;
    private final String trunkTag;
    private final String leafTag;
    private final String groundTag;
    /**
     * Responsible for the creation and management of trees.
     * @param gameObjects The current game object in use
     * @param terrain The terrain of the game
     * @param seed The amount of seeds in the game
     * @param trunkLayer The value of the root layer
     * @param leavesLayer The value of the leaves layer
     * @param trunkTag Tag of the trunk of the tree
     * @param leafTag Tag of the leaves
     * @param groundTag Tag of the ground
     */
    public Tree(GameObjectCollection gameObjects, Terrain terrain,
                int seed, int trunkLayer, int leavesLayer,
                String trunkTag, String leafTag, String groundTag) {
        this.gameObjects = gameObjects;
        this.terrain = terrain;
        this.seed = seed;
        this.rand = new Random(seed);
        this.trunkLayer = trunkLayer;
        this.leavesLayer = leavesLayer;
        this.trunkTag = trunkTag;
        this.leafTag = leafTag;
        this.groundTag = groundTag;
    } // end of constructor tree

    /**
     * This method creates trees in a given range of x-values.
     * @param minX The lower bound of the given range (will be rounded to a multiple of Block.SIZE).
     * @param maxX The upper bound of the given range (will be rounded to a multiple of Block.SIZE).
     */
    public void createInRange(int minX, int maxX){
        if(minX > maxX){
            int temp = minX;
            minX = maxX;
            maxX = temp;
        }
        int distance = minX % MINIMAL_DISTANCE_BETWEEN_TREES;
        minX = minX - distance;
        for (int i = minX; i <= maxX; i += MINIMAL_DISTANCE_BETWEEN_TREES){ // loop from minimal value for x until maximum value for x
            Random random = new Random(Objects.hash(i, seed)); // adds tree to hash table
            if (random.nextFloat() < ODDS)
                create(i, random.nextInt(MAX_HEIGHT - MIN_HEIGHT) + MIN_HEIGHT);
        } // end of for loop
    } // end of createInRange method

    // creates a tree object
    private void create(int location, int rootHeight) {
        int groundHeight = heightAt(location); // the ground height at a certain location
        createTrunk(groundHeight, location, rootHeight); // creates the trunk
        int sizeTopTree = Block.SIZE*(rootHeight*2/3);
        int col = location-sizeTopTree/HALF;
        int row = rootHeight*Block.SIZE-sizeTopTree/HALF;
        for (int i = col; i <= (col + sizeTopTree); i+=Block.SIZE) {
            for (int j = row; j <= row + sizeTopTree; j+=Block.SIZE) {
                Vector2 locationOrigLeaf = new Vector2(i, groundHeight - j);
                Leaf leaf = createLeaf(locationOrigLeaf); // creates leaf with the original location of the leaf
                leafAnimation(leaf); //uses animation for leaf
                createLeafFall(leaf, locationOrigLeaf); // makes the leaf to fall
                gameObjects.addGameObject(leaf, leavesLayer); // Add leaves to the game
            } // end of inner for loop
        } // end of outer fol loop
    } // end of method create

    // creates a tree trunk
    private void createTrunk(int groundHeight, int location, int rootHeight) {
        Vector2 blockSize = new Vector2(Block.SIZE, Block.SIZE);
        for (int i = 0; i < rootHeight; i++) {
            GameObject trunk = new GameObject(
                    new Vector2(location, groundHeight - (i*Block.SIZE)), blockSize,
                    new RectangleRenderable(pepse.util.ColorSupplier.approximateColor( TRUNK_COLOUR, COLOUR_DELTA) ));
            trunk.setTag(this.trunkTag);
            // so character can stand on the top of the tree
            if (i == rootHeight - 1) {
                gameObjects.addGameObject(trunk, trunkLayer + 1);
                trunk.physics().preventIntersectionsFromDirection(Vector2.UP);
                trunk.physics().setMass(GameObjectPhysics.IMMOVABLE_MASS - 1);
            }
            gameObjects.addGameObject(trunk, trunkLayer);
        } // end of for loop
    } // end of createTrunk method

    // Creates a leaf
    private Leaf createLeaf(Vector2 location) {
        return new Leaf(location, new Vector2(Block.SIZE, Block.SIZE),
                new RectangleRenderable(pepse.util.ColorSupplier.approximateColor(LEAF_COLOUR, LEAF_COLOUR_DELTA))
                ,this.leafTag, this.groundTag);
    } // end of private method create leaf

    // Animation of the leaf
    private void leafAnimation(Leaf leaf) {
        // schedule a delay for leaf angle
        new ScheduledTask(leaf, rand.nextInt(LEAF_ANIMATION_WAIT_TIME) + DELAY, true,
                () -> changeAngleTransition(leaf));
        // schedule a delay for leaf width
        new ScheduledTask(leaf, rand.nextInt(LEAF_ANIMATION_WAIT_TIME) + DELAY, true,
                () -> changeDimensionsTransition(leaf));
    } // end of private method

    // Changes the dimensions transition
    private void changeDimensionsTransition(Leaf leaf) {
        new Transition<>(
                leaf,
                (size) -> leaf.setDimensions(new Vector2(Block.SIZE + size, Block.SIZE + size)),
                -1f, 4f, Transition.CUBIC_INTERPOLATOR_FLOAT, rand.nextInt(TRANSITION_RANDOM_DELAY) + TRANSITION_DELAY,
                Transition.TransitionType.TRANSITION_LOOP, null);
    } // end of private method

    // Changes the angle transition
    private void changeAngleTransition(Leaf leaf) {
        new Transition<>(leaf,
                (angle) -> leaf.renderer().setRenderableAngle(angle), //moves the leaves in the air
                0f, 5f, Transition.CUBIC_INTERPOLATOR_FLOAT, rand.nextInt(TRANSITION_RANDOM_DELAY) + TRANSITION_DELAY,
                Transition.TransitionType.TRANSITION_LOOP, null
        );
    } //end of private method

    // In charge of the falling of the leaf
    private void createLeafFall(Leaf leaf, Vector2 location) {
        leaf.renderer().setOpaqueness(OPAQUENESS);
        leaf.setTopLeftCorner(location);
        new ScheduledTask(
                leaf, rand.nextInt(LEAF_FALL_WAIT_TIME) + LEAF_FALL_DELAY, false,
                () -> {
                    leaf.leafFallTransition(leaf, rand.nextInt(LEAF_FALL_RANDOM) + FALL_DELAY);   //  transition of vertical movement
                    leaf.renderer().fadeOut(FADEOUT_TIME, () -> { // add fadeout time
                        leafAfterFalling(leaf, location, rand.nextInt(LEAF_FALL_RANDOM));}); // end of fadeout
                }); // end of lambda
    } // end of private method

    // Initializes the leaf after task is done
    private void leafAfterFalling(Leaf leaf, Vector2 location, int afterlifeTime) {
        new ScheduledTask(leaf, afterlifeTime, false,
                () -> createLeafFall(leaf, location));
    } // end of private method

    // The height of terrain at a certain point
    private int heightAt(int location){
        int height = (int) terrain.groundHeightAt(location);
        return height -(height % Block.SIZE) - Block.SIZE; // returns the rounded height of tree
    } // end of private method heightAt
} // end of class tree

