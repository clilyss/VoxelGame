package club.lily.voxelgame.engine.window;

import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {

    private final int    width, height;
    private final String title;
    private       long   handle;

    private double  mouseDX, mouseDY;
    private double  lastX, lastY;
    private boolean firstMouse = true;

    public Window(int width, int height, String title) {
        this.width  = width;
        this.height = height;
        this.title  = title;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new RuntimeException("Failed to init GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,        GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) throw new RuntimeException("Failed to create GLFW window");

        try (MemoryStack stack = stackPush()) {
            IntBuffer pw = stack.mallocInt(1);
            IntBuffer ph = stack.mallocInt(1);
            glfwGetWindowSize(handle, pw, ph);
            GLFWVidMode vm = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vm != null)
                glfwSetWindowPos(handle, (vm.width() - pw.get(0)) / 2, (vm.height() - ph.get(0)) / 2);
        }

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        glfwShowWindow(handle);
        glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        glfwSetCursorPosCallback(handle, (win, xpos, ypos) -> {
            if (firstMouse) { lastX = xpos; lastY = ypos; firstMouse = false; }
            mouseDX = xpos - lastX;
            mouseDY = lastY - ypos;
            lastX = xpos;
            lastY = ypos;
        });
    }

    public void swapAndPoll() {
        mouseDX = 0;
        mouseDY = 0;
        glfwSwapBuffers(handle);
        glfwPollEvents();
    }

    public void cleanup() {
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public boolean shouldClose() { return glfwWindowShouldClose(handle); }
    public long    getHandle()   { return handle; }
    public double  getMouseDX()  { return mouseDX; }
    public double  getMouseDY()  { return mouseDY; }
    public int     getWidth()    { return width; }
    public int     getHeight()   { return height; }
}
