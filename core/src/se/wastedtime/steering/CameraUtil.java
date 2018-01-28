package se.wastedtime.steering;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraUtil {

    public static void stickToPlayer(Camera camera, Vector2 target) {
        Vector3 position = camera.position;
        position.x = position.x + (target.x - position.x) * 0.1f;
        position.y = position.y + (target.y - position.y) * 0.1f;
        camera.position.set(position);
        camera.update();
    }

    public static void boundary(Camera camera, TiledMap map) {
        float x = camera.viewportWidth / 2f;
        float y = camera.viewportHeight / 2f;
        float w = (float) map.getProperties().get("width", Integer.class);
        float h = (float) map.getProperties().get("height", Integer.class);

        boundary(camera, x, y, w, h);
    }

    private static void boundary(Camera camera, float x, float y, float width, float height) {
        Vector3 position = camera.position;

        if (position.x < x)
            position.x = x;
        if (position.y < y)
            position.y = y;

        if (position.x + x > width)
            position.x = width - x;
        if (position.y + y > height)
            position.y = height - y;

        camera.position.set(position);
        camera.update();
    }

}
