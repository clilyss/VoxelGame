package club.lily.voxelgame.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    private final Matrix4f view       = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    private final Vector3f position = new Vector3f(0, 80, 0);
    private final Vector3f front    = new Vector3f(0,  0, -1);
    private final Vector3f right    = new Vector3f(1,  0,  0);
    private final Vector3f up       = new Vector3f(0,  1,  0);

    private float yaw   = -90.0f;
    private float pitch =   0.0f;

    private static final float SENSITIVITY = 0.1f;

    public Camera(int width, int height, float fovDeg, float near, float far) {
        projection.perspective(
                (float) Math.toRadians(fovDeg),
                (float) width / height,
                near, far);
        updateVectors();
    }

    public void rotate(double dx, double dy) {
        yaw   += (float) dx * SENSITIVITY;
        pitch += (float) dy * SENSITIVITY;
        pitch  = Math.max(-89.0f, Math.min(89.0f, pitch));
        updateVectors();
    }

    private void updateVectors() {
        double yR = Math.toRadians(yaw);
        double pR = Math.toRadians(pitch);
        front.set(
            (float)(Math.cos(yR) * Math.cos(pR)),
            (float) Math.sin(pR),
            (float)(Math.sin(yR) * Math.cos(pR))
        ).normalize();
        right.set(front).cross(new Vector3f(0, 1, 0)).normalize();
        up   .set(right).cross(front).normalize();
    }

    public Matrix4f getViewMatrix() {
        Vector3f target = new Vector3f(position).add(front);
        return view.identity().lookAt(position, target, new Vector3f(0, 1, 0));
    }

    public Matrix4f getProjectionMatrix() { return projection; }

    public Vector3f getPosition() { return position; }
    public void     setPosition(float x, float y, float z) { position.set(x, y, z); }
    public Vector3f getFront()    { return front; }
    public Vector3f getRight()    { return right; }
}
