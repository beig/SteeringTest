package se.wastedtime.steering;

import com.badlogic.gdx.math.Vector2;

public class PhysicsUtils {

    /**
     * Adds a distance to a Vector on a line to the target Vector
     * All vectors in WorldUnits
     *
     * @param start    start
     * @param target   target
     * @param distance distance to add
     * @return {@link Vector2} with the distance added
     */
    public static Vector2 addDistance(Vector2 start, Vector2 target, float distance) {

        start = start.cpy();

        double angle = Math.atan2(target.y - start.y, target.x - start.x);
        float x = (float) (distance * Math.cos(angle));
        float y = (float) (distance * Math.sin(angle));

        return start.add(x, y);
    }

    /**
     * Adds a distance to a Vector
     * Vector in WorldUnits
     *
     * @param point    point
     * @param distance distance to add
     * @return {@link Vector2} with the distance added
     */
    public static Vector2 addDistance(Vector2 point, float distance) {

        point = point.cpy();

        double angle = Math.atan2(point.y, point.x);
        float x = (float) (distance * Math.cos(angle));
        float y = (float) (distance * Math.sin(angle));

        return point.add(x, y);

    }

    /**
     * Subtracts a distance from a Vector on a line to the target Vector
     * All vectors in WorldUnits
     *
     * @param start    start
     * @param target   target
     * @param distance distance to subtract
     * @return {@link Vector2} with the distance subtracted
     */
    public static Vector2 subDistance(Vector2 start, Vector2 target, float distance) {
        start = start.cpy();

        double angle = Math.atan2(target.y - start.y, target.x - start.x);
        float x = (float) (distance * Math.cos(angle));
        float y = (float) (distance * Math.sin(angle));

        return start.sub(x, y);

    }

    /**
     * Subtracts a distance from a Vector
     * Vector in WorldUnits
     *
     * @param point    start
     * @param distance distance to subtract
     * @return {@link Vector2} with the distance subtracted
     */
    public static Vector2 subDistance(Vector2 point, float distance) {
        point = point.cpy();

        double angle = Math.atan2(point.y, point.x);
        float x = (float) (distance * Math.cos(angle));
        float y = (float) (distance * Math.sin(angle));

        return point.sub(x, y);

    }

    public static float vectorToAngle(Vector2 vector) {
        return (float) Math.atan2(-vector.x, vector.y);
    }

    public static Vector2 angleToVector(Vector2 outVector, float angle) {
        outVector.x = -(float) Math.sin(outVector.x);
        outVector.y = (float) Math.cos(outVector.y);
        return outVector;
    }

    public static float pixelsToMeters (int pixels) {
        return (float)pixels * 0.02f;
    }

    public static int metersToPixels (float meters) {
        return (int)(meters * 50.0f);
    }
}
