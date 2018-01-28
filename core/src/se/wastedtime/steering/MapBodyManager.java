package se.wastedtime.steering;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Populates box2D world with static bodies using data from a map object<p>
 * It uses a JSON formatted materials file to assign properties to the static
 * bodies it creates. To assign a material to a shape add a "material" custom
 * property to the shape in question using your editor of choice (Tiled, Gleed,
 * Tide...). Such file uses the following structure:
 * <pre>
 * {@code
 * [
 *      { "name" : "ice", "density" : 1.0, "restitution" : 0.0, "friction" : 0.1 },
 *      { "name" : "elastic", "density" : 1.0, "restitution" : 0.8, "friction" : 0.8 }
 *    ]
 * }
 * </pre>
 * In case no material property is found, it'll get a default one.
 *
 * @author David Saltares Márquez david.saltares at gmail.com
 */
public class MapBodyManager {

    private World world;
    private float units;
    private List<Body> bodies = new ArrayList<>();
    private ObjectMap<String, FixtureDef> materials = new ObjectMap<>();

    /**
     * @param world         box2D world to work with.
     * @param unitsPerPixel conversion ratio from pixel units to box2D metres.
     * @param materialsFile json file with specific physics properties to be assigned to newly created bodies.
     */
    public MapBodyManager(World world, float unitsPerPixel, FileHandle materialsFile) {

        this.world = world;
        this.units = unitsPerPixel;

        if (materialsFile != null) {
            loadMaterialsFile(materialsFile);
        }
    }

    /**
     * @param map       map to be used to create the static bodies.
     * @param layerName name of the layer that contains the shapes.
     */
    public void createPhysics(TiledMap map, String layerName, String mapName) {

        MapLayer layer = null;

        for (MapLayer mapLayer : map.getLayers()) {
            if (mapLayer instanceof MapGroupLayer) {
                MapGroupLayer group = (MapGroupLayer) mapLayer;
                layer = group.getLayers().get(layerName);
            }
        }

        if (layer == null)
            layer = map.getLayers().get(layerName);
        if (layer == null) {
            return;
        }

        MapObjects objects = layer.getObjects();

        for (MapObject object : objects) {
            if (object instanceof TextureMapObject) {
                continue;
            }

            Shape shape;
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.StaticBody;

            if (object instanceof RectangleMapObject) {
                RectangleMapObject rectangle = (RectangleMapObject) object;
                shape = getRectangle(rectangle);
            } else if (object instanceof PolygonMapObject) {
                shape = getPolygon((PolygonMapObject) object);
            } else if (object instanceof PolylineMapObject) {
                shape = getPolyline((PolylineMapObject) object);
            } else if (object instanceof CircleMapObject) {
                shape = getCircle((CircleMapObject) object);
            } else {
                continue;
            }

            Body body = world.createBody(bodyDef);

            FixtureDef fixtureDef;

            if (layer.getProperties().containsKey("material")) {
                fixtureDef = materials.get(layer.getProperties().get("material", String.class));
                if (fixtureDef == null)
                    System.out.println("no fixture");
                else {
                    fixtureDef.shape = shape;
                    body.setUserData("LOS");
                }
            } else {
                MapProperties properties = object.getProperties();
                String material = properties.get("material", "default", String.class);
                fixtureDef = materials.get(material);

                if (material.equalsIgnoreCase("npc")) {
                    if (object instanceof RectangleMapObject)
                        fixtureDef.shape = getHollowRectangle(((RectangleMapObject) object).getRectangle());
                } else {
                    fixtureDef.shape = shape;
                }
            }

            body.createFixture(fixtureDef);
            bodies.add(body);

            fixtureDef.shape = null;
            shape.dispose();
        }
    }

    /**
     * Destroys every static body that has been created using the manager.
     */
    public void destroyPhysics() {
        for (Body body : bodies) {
            world.destroyBody(body);
        }

        bodies.clear();
    }

    private void loadMaterialsFile(FileHandle materialsFile) {

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 1.0f;
        fixtureDef.restitution = 0.0f;
        materials.put("default", fixtureDef);


        fixtureDef = new FixtureDef();
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 1.0f;
        fixtureDef.restitution = 0.0f;
        fixtureDef.filter.maskBits = -1;
        materials.put("physics", fixtureDef);


        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(materialsFile);

            for (JsonValue materialValue : root) {
                String name = materialValue.name();

                fixtureDef = new FixtureDef();
                fixtureDef.density = materialValue.getFloat("density", 1.0f);
                fixtureDef.friction = materialValue.getFloat("friction", 1.0f);
                fixtureDef.restitution = materialValue.getFloat("restitution", 0.0f);
                fixtureDef.filter.categoryBits = materialValue.getShort("category", (short) 0x0001);
                fixtureDef.filter.maskBits = materialValue.getShort("mask", (short) 0x0000);

                materials.put(name, fixtureDef);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a custom FixtureDef to the map
     *
     * @param fixtureDef
     * @param name
     */
    public void addFixture(FixtureDef fixtureDef, String name) {
        materials.put(name, fixtureDef);
    }

    /**
     * Creates a hollow Rectangle using a ChainShape from a rectangle
     *
     * @param rectangle
     * @return
     */
    private ChainShape getHollowRectangle(Rectangle rectangle) {
        Vector2 position = rectangle.getPosition(new Vector2());
        Vector2 size = rectangle.getSize(new Vector2());

        Vector2[] chainVector = new Vector2[5];
        chainVector[0] = position;
        chainVector[1] = new Vector2(position.x, position.y + size.y);
        chainVector[2] = new Vector2(position.x + size.x, position.y + size.y);
        chainVector[3] = new Vector2(position.x + size.x, position.y);
        chainVector[4] = new Vector2(position.x, position.y);
        ChainShape chain = new ChainShape();
        chain.createChain(chainVector);

        return chain;
    }

    private Shape getRectangle(RectangleMapObject rectangleObject) {
        Rectangle rectangle = rectangleObject.getRectangle();
        PolygonShape polygon = new PolygonShape();
        Vector2 size = new Vector2((rectangle.x + rectangle.width * 0.5f) / units,
                (rectangle.y + rectangle.height * 0.5f) / units);
        polygon.setAsBox(rectangle.width * 0.5f / units,
                rectangle.height * 0.5f / units,
                size,
                0.0f);
        return polygon;
    }

    private Shape getCircle(CircleMapObject circleObject) {
        Circle circle = circleObject.getCircle();
        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(circle.radius / units);
        circleShape.setPosition(new Vector2(circle.x / units, circle.y / units));
        return circleShape;
    }

    private Shape getPolygon(PolygonMapObject polygonObject) {
        PolygonShape polygon = new PolygonShape();
        float[] vertices = polygonObject.getPolygon().getTransformedVertices();

        float[] worldVertices = new float[vertices.length];

        for (int i = 0; i < vertices.length; ++i) {
            worldVertices[i] = vertices[i] / units;
        }

        polygon.set(worldVertices);
        return polygon;
    }

    private Shape getPolyline(PolylineMapObject polylineObject) {
        float[] vertices = polylineObject.getPolyline().getTransformedVertices();
        Vector2[] worldVertices = new Vector2[vertices.length / 2];

        for (int i = 0; i < vertices.length / 2; ++i) {
            worldVertices[i] = new Vector2();
            worldVertices[i].x = vertices[i * 2] / units;
            worldVertices[i].y = vertices[i * 2 + 1] / units;
        }

        ChainShape chain = new ChainShape();
        chain.createChain(worldVertices);
        return chain;
    }
}