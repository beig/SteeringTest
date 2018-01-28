package se.wastedtime.steering.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import se.wastedtime.steering.Box2DLocation;
import se.wastedtime.steering.PhysicsUtils;

public class Character implements Steerable<Vector2> {

    private final World world;
    TextureRegion region;
    Body body;

    float boundingRadius;
    boolean tagged;

    float maxLinearSpeed;
    float maxLinearAcceleration;
    float maxAngularSpeed;
    float maxAngularAcceleration;

    boolean independentFacing;

    protected SteeringBehavior<Vector2> steeringBehavior;

    private Vector2 initPos = new Vector2(5, 5);

    private static final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<>(new Vector2());

    public Character(World world) {
        this.world = world;
        initBody();
    }

    public Character(World world, Vector2 vector2) {
        this.world = world;
        initPos = vector2;
        initBody();
    }

    private void initBody() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(initPos);
        bodyDef.fixedRotation = true;

        PolygonShape shape = new PolygonShape();

        shape.setAsBox(0.3f, 0.3f);


        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 70;
        fixtureDef.friction = 0;
        fixtureDef.restitution = 0;
        fixtureDef.filter.categoryBits = (short) 0x0001;
        fixtureDef.filter.maskBits = (short) 0x0007;
        fixtureDef.isSensor = false;

        body = world.createBody(bodyDef);
        body.setUserData(this);
        body.createFixture(fixtureDef);
        shape.dispose();

    }

    public SteeringBehavior<Vector2> getSteeringBehavior() {
        return steeringBehavior;
    }

    public void setSteeringBehavior(SteeringBehavior<Vector2> steeringBehavior) {
        this.steeringBehavior = steeringBehavior;
    }

    public void update(float deltaTime) {
        if (steeringBehavior != null) {
            // Calculate steering acceleration
            steeringBehavior.calculateSteering(steeringOutput);

            /*
             * Here you might want to add a motor control layer filtering steering accelerations.
             *
             * For instance, a car in a driving game has physical constraints on its movement: it cannot turn while stationary; the
             * faster it moves, the slower it can turn (without going into a skid); it can brake much more quickly than it can
             * accelerate; and it only moves in the direction it is facing (ignoring power slides).
             */

            // Apply steering acceleration
            applySteering(steeringOutput, deltaTime);
        }

        wrapAround(PhysicsUtils.pixelsToMeters(Gdx.graphics.getWidth()), PhysicsUtils.pixelsToMeters(Gdx.graphics.getHeight()));
    }

    protected void applySteering(SteeringAcceleration<Vector2> steering, float deltaTime) {
        boolean anyAccelerations = false;

        // Update position and linear velocity.
        if (!steeringOutput.linear.isZero()) {
            // this method internally scales the force by deltaTime
            body.applyForceToCenter(steeringOutput.linear, true);
            anyAccelerations = true;
        }

        // Update orientation and angular velocity
        if (independentFacing) {
            if (steeringOutput.angular != 0) {
                // this method internally scales the torque by deltaTime
                body.applyTorque(steeringOutput.angular, true);
                anyAccelerations = true;
            }
        } else {
            // If we haven't got any velocity, then we can do nothing.
            Vector2 linVel = getLinearVelocity();
            if (!linVel.isZero(getZeroLinearSpeedThreshold())) {
                float newOrientation = vectorToAngle(linVel);
                body.setAngularVelocity((newOrientation - getAngularVelocity()) * deltaTime); // this is superfluous if independentFacing is always true
                body.setTransform(body.getPosition(), newOrientation);
            }
        }

        if (anyAccelerations) {
            // body.activate();

            // TODO:
            // Looks like truncating speeds here after applying forces doesn't work as expected.
            // We should likely cap speeds form inside an InternalTickCallback, see
            // http://www.bulletphysics.org/mediawiki-1.5.8/index.php/Simulation_Tick_Callbacks

            // Cap the linear speed
            Vector2 velocity = body.getLinearVelocity();
            float currentSpeedSquare = velocity.len2();
            float maxLinearSpeed = getMaxLinearSpeed();
            if (currentSpeedSquare > maxLinearSpeed * maxLinearSpeed) {
                body.setLinearVelocity(velocity.scl(maxLinearSpeed / (float) Math.sqrt(currentSpeedSquare)));
            }

            // Cap the angular speed
            float maxAngVelocity = getMaxAngularSpeed();
            if (body.getAngularVelocity() > maxAngVelocity) {
                body.setAngularVelocity(maxAngVelocity);
            }
        }
    }

    // the display area is considered to wrap around from top to bottom
    // and from left to right
    protected void wrapAround(float maxX, float maxY) {
        float k = Float.POSITIVE_INFINITY;
        Vector2 pos = body.getPosition();

        if (pos.x > maxX) k = pos.x = 0.0f;

        if (pos.x < 0) k = pos.x = maxX;

        if (pos.y < 0) k = pos.y = maxY;

        if (pos.y > maxY) k = pos.y = 0.0f;

        if (k != Float.POSITIVE_INFINITY) body.setTransform(pos, body.getAngle());
    }

    //-----------------------------//

    @Override
    public Vector2 getPosition() {
        return body.getPosition();
    }

    @Override
    public float getOrientation() {
        return body.getAngle();
    }

    @Override
    public void setOrientation(float orientation) {
        body.setTransform(getPosition(), orientation);
    }

    @Override
    public Vector2 getLinearVelocity() {
        return body.getLinearVelocity();
    }

    @Override
    public float getAngularVelocity() {
        return body.getAngularVelocity();
    }

    @Override
    public float getBoundingRadius() {
        return boundingRadius;
    }

    @Override
    public boolean isTagged() {
        return tagged;
    }

    @Override
    public void setTagged(boolean tagged) {
        this.tagged = tagged;
    }

    @Override
    public Location<Vector2> newLocation() {
        return new Box2DLocation();
    }

    @Override
    public float vectorToAngle(Vector2 vector) {
        return PhysicsUtils.vectorToAngle(vector);
    }

    @Override
    public Vector2 angleToVector(Vector2 outVector, float angle) {
        return PhysicsUtils.angleToVector(outVector, angle);
    }

    //
    // Limiter implementation
    //

    @Override
    public float getMaxLinearSpeed() {
        return maxLinearSpeed;
    }

    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed) {
        this.maxLinearSpeed = maxLinearSpeed;
    }

    @Override
    public float getMaxLinearAcceleration() {
        return maxLinearAcceleration;
    }

    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration) {
        this.maxLinearAcceleration = maxLinearAcceleration;
    }

    @Override
    public float getMaxAngularSpeed() {
        return maxAngularSpeed;
    }

    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed) {
        this.maxAngularSpeed = maxAngularSpeed;
    }

    @Override
    public float getMaxAngularAcceleration() {
        return maxAngularAcceleration;
    }

    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration) {
        this.maxAngularAcceleration = maxAngularAcceleration;
    }

    @Override
    public float getZeroLinearSpeedThreshold() {
        return 0.001f;
    }

    @Override
    public void setZeroLinearSpeedThreshold(float value) {
        throw new UnsupportedOperationException();
    }
}
