package se.wastedtime.steering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ai.steer.behaviors.Arrive;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import se.wastedtime.steering.entities.Character;

public class GameScreen implements Screen {

    SpriteBatch batch;

    String map = "maps/first.tmx";
    private StretchViewport viewport;
    private OrthographicCamera camera;
    private MapRenderer mapRenderer;
    private TiledMap loadedTileMap;
    private Box2DDebugRenderer physicsDebugRenderer;
    private MapBodyManager mapBodyManager;
    private World world;
    private final Character character;
    private final Character target;

    public GameScreen() {
        batch = new SpriteBatch();

        loadedTileMap = new TmxMapLoader().load(map);
        mapRenderer = new MapRenderer(loadedTileMap, 1f / 32f);

        camera = new OrthographicCamera();
        viewport = new StretchViewport(30f, 20f, camera);
        viewport.apply(true);
        camera.position.set(0, 0, 0);

        mapRenderer.setView(camera);

        /*
        Physics
         */
        Box2D.init();
        world = new World(new Vector2(0, 0), true);
        mapBodyManager = new MapBodyManager(world, 32f, Gdx.files.internal("materials.json"));
        mapBodyManager.createPhysics(loadedTileMap, "physics", map);

        physicsDebugRenderer = new Box2DDebugRenderer();

        character = new Character(world);

        target = new Character(world, new Vector2(10, 10));

        final Arrive<Vector2> arriveSB = new Arrive<>(character, target)
                .setTimeToTarget(0.1f)
                .setArrivalTolerance(0.001f)
                .setDecelerationRadius(1);

        character.setSteeringBehavior(arriveSB);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        viewport.apply();

//        if (Gdx.input.isKeyPressed(Input.Keys.Q))
        character.update(delta);

        Vector2 mouse = new Vector2(Gdx.input.getX(), Gdx.input.getY());

        Vector2 unproj = viewport.unproject(mouse);

        System.out.println(character.getPosition() + "||" + target.getPosition());

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        CameraUtil.boundary(camera, loadedTileMap);
        camera.update();

        mapRenderer.setView(camera);
        mapRenderer.renderBefore();

        batch.begin();
        batch.end();

        mapRenderer.renderAfter();

        world.step(1f / 45f, 8, 3);

        physicsDebugRenderer.render(world, camera.combined);

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
