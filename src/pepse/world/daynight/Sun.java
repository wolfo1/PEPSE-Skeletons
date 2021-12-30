package pepse.world.daynight;

import danogl.GameObject;
import danogl.collisions.GameObjectCollection;
import danogl.components.CoordinateSpace;
import danogl.components.Transition;
import danogl.gui.rendering.OvalRenderable;
import danogl.util.Vector2;

import java.awt.*;
import java.util.function.Consumer;

public class Sun {
    private static int index = 0;
    /**
     * A method that creates a sun object.
     * @param gameObjects  The collection of all participating game objects.
     * @param layer The number of the layer to which the created sun should be added.
     * @param windowDimensions The dimensions of the windows.
     * @param cycleLength The amount of seconds it should take the created game object to complete a full cycle.
     * @return A new game object representing the sun.
     */
    public static GameObject create(GameObjectCollection gameObjects, int layer, Vector2 windowDimensions, float cycleLength){
        GameObject sun = new GameObject(Vector2.ZERO, new Vector2(250, 250), new OvalRenderable(Color.YELLOW));
        sun.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
        gameObjects.addGameObject(sun, layer);
        sun.setTag("sun " + Sun.index);
        Sun.index ++;
        Vector2 central = windowDimensions.mult(0.5f).add(new Vector2(0, 200));
        new Transition<>(
                sun,
                angle -> {
                    float cos = (float) Math.cos(Math.toRadians(angle));
                    float sin = (float) Math.sin(Math.toRadians(angle));
                    sun.setCenter(new Vector2((100 + central.x()) * cos + central.x(),central.y() * sin + central.y()));
                },
                270f, // cycle of 360, adjusted to start in a position according to night cycle
                630f,
                Transition.LINEAR_INTERPOLATOR_FLOAT,
                cycleLength,
                Transition.TransitionType.TRANSITION_LOOP,
                null);
        return sun;
    } // end of method create

} // end of class Sum

