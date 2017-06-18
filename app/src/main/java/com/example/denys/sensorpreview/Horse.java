package com.example.denys.sensorpreview;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES30.GL_RGBA8;
import static android.opengl.GLES30.glBindVertexArray;
import static android.opengl.GLES30.glGenVertexArrays;
import static android.opengl.GLES31.GL_COMPUTE_SHADER;
import static android.opengl.GLES31.GL_COMPUTE_SHADER_BIT;
import static android.opengl.GLES31.GL_READ_ONLY;
import static android.opengl.GLES31.GL_READ_WRITE;
import static android.opengl.GLES31.glBindImageTexture;
import static android.opengl.GLES31.glDispatchCompute;
import static android.opengl.GLES31.glMemoryBarrier;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by chuq on 17.06.2017.
 */

public class Horse extends Model {

    private static final String VERTEX_SHADER_FILE = "horse/draw.vert";
    private static final String FRAGMENT_SHADER_FILE = "horse/draw.frag";
    private static final String CLEAR_COMP_SHADER_FILE = "horse/clear.comp";
    private static final String DRAW_COMP_SHADER_FILE = "horse/draw.comp";

    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private float[] mViewMatrix = new float[16];

    /**
     * Store the projection matrix. This is used to project the scene onto a 2D viewport.
     */
    private float[] mProjectionMatrix = new float[16];

    /**
     * Allocate storage for the final combined matrix. This will be passed into the shader program.
     */
    private float[] mMVPMatrix = new float[16];

    /**
     * Store our model data in a float buffer.
     */
    private FloatBuffer mCubePositions;
    private FloatBuffer mCubeColors;

    /**
     * This will be used to pass in the transformation matrix.
     */
    private int mMVPMatrixHandle;

    /**
     * This will be used to pass in the modelview matrix.
     */
    private int mMVMatrixHandle;

    /**
     * This will be used to pass in model position information.
     */
    private int mPositionHandle;

    /**
     * This will be used to pass in model color information.
     */
    private int mColorHandle;

    /**
     * How many bytes per float.
     */
    private final int FLOAT_BYTES = 4;

    /**
     * Size of the position data in elements.
     */
    private final int POS_DATA_SIZE = 3;

    /**
     * Size of the color data in elements.
     */
    private final int COLOR_DATA_SIZE = 4;


    private static final ThreadLocal<int[]> s_BUILD_PROGRAM_STATUS = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_BUILD_SHADER_STATUS = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_CREATE_BUFFER_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_CREATE_FRAMEBUFFER_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_CREATE_TEXTURE_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_CURRENT_FRAMEBUFFER_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_DELETE_BUFFER_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_DELETE_FRAMEBUFFER_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_DELETE_TEXTURE_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_LOAD_TEXTURE_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_GET_INTEGERV_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_GET_PROGRAM_ID = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> s_CREATE_VERTEX_ARRAY_ID = new ThreadLocal<int[]>();

    public Horse() {
        init();
    }


    public void loadShaders() {
        drawProgramID = Loader.buildProgramFromAssets(VERTEX_SHADER_FILE, FRAGMENT_SHADER_FILE, null /*new String[] {"a_Position",  "a_Color", "a_Normal"}*/);
        clearProgramID = Loader.buildProgramFromAssets(CLEAR_COMP_SHADER_FILE, GL_COMPUTE_SHADER, null);
        computeProgramID = Loader.buildProgramFromAssets(DRAW_COMP_SHADER_FILE, GL_COMPUTE_SHADER, null);
    }

    public void changeProjection(int width, int height) {
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    public void draw() {
        runClearShaders();

        long time = System.currentTimeMillis() % 6000L - 3000L;
        float angleInDegrees = 0; // (-90.0f / 10000.0f) * ((int) time);
        float[] mMVPMatrix;

        mMVPMatrix = getMVPMatrix(angleInDegrees + 22.5f, .8f);
        runComputeShader(0, mMVPMatrix);

        mMVPMatrix = getMVPMatrix(angleInDegrees - 22.5f, -.8f);
        runComputeShader(1, mMVPMatrix);

        float mix = (horizontalAngleRotation + 22.5f) / 45.5f;
        mix = Math.max(Math.min(mix, 1.0f), 0.0f);
//        System.out.println("mix " + mix);
        float[] mixer = {1.0f - mix, mix, 0f, 0f};
        finalDraw(mixer);
    }


//    private void repos()
//    {
//        // Do a complete rotation every 10 seconds.
//        long time = SystemClock.uptimeMillis() % 10000L;
//        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
//
//        // Set our per-vertex lighting program.
//        GLES20.glUseProgram(mProgId);
//
//        // Set program handles for cube drawing.
//        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgId, "u_MVPMatrix");
//        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgId, "u_MVMatrix");
//        mPositionHandle = GLES20.glGetAttribLocation(mProgId, "a_Position");
//        mColorHandle = GLES20.glGetAttribLocation(mProgId, "a_Color");
//
//        // Draw some cubes.
//        Matrix.setIdentityM(mModelMatrix, 0);
//        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
//        Matrix.multiplyMM(mModelMatrix, 0, mRotationVectorMatrix, 0, mModelMatrix, 0);
//        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0f);
//    }

    public static final float AMENDMENT_STEP = 0.05f;
    public static final int WINDOW_WIDTH = 568;
    public static final int WINDOW_HEIGHT = 320;
    public static final float ROTATION_STEP_ANGLE = 1.0f;
    // The window handle
    private long window;

    private int drawProgramID, computeProgramID, clearProgramID;

    private int fullScreenVao;

    public static final int IMAGES_COUNT = 2;
    private int[] depthTexturesID = new int[IMAGES_COUNT];
    private int[] resultTexturesID = new int[IMAGES_COUNT];
    private int[] imageTexturesID = new int[IMAGES_COUNT];


    private int workgroupSize;

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private static final String S_COMP_SHADER_HEADER = "#version 310 es\n#define LOCAL_SIZE %d\n";
    private float distanceAmendment = 0f;
    private float horizontalShift;
    private float horizontalAngleRotation = 0f;

    private void finalDraw(float[] mixer) {
        GLES20.glUseProgram(drawProgramID);
        glBindVertexArray(fullScreenVao);

        int mMixerHandle = GLES20.glGetUniformLocation(drawProgramID, "uMixer");
        glUniform4fv(mMixerHandle, 1, mixer, 0);

        for (int i = 0; i < IMAGES_COUNT; i++) {
            glActiveTexture(GL_TEXTURE0 + i * 2);
            glBindTexture(GL_TEXTURE_2D, resultTexturesID[i]);

            glActiveTexture(GL_TEXTURE0 + i * 2 + 1);
            glBindTexture(GL_TEXTURE_2D, imageTexturesID[i]);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    private void runClearShaders() {
        GLES20.glUseProgram(clearProgramID);
        for (int i = 0; i < IMAGES_COUNT; i++) {
            glBindImageTexture(1, resultTexturesID[i], 0, false, 0, GL_READ_WRITE, GL_RGBA8);
            glDispatchCompute(WIDTH / workgroupSize, HEIGHT / workgroupSize, 1);
            glMemoryBarrier(GL_COMPUTE_SHADER_BIT);
        }
    }

    private void runComputeShader(int i, float[] mMVPMatrix) {
        GLES20.glUseProgram(computeProgramID);

        glBindImageTexture(0, depthTexturesID[i], 0, false, 0, GL_READ_ONLY, GL_RGBA8);
        glBindImageTexture(1, resultTexturesID[i], 0, false, 0, GL_READ_WRITE, GL_RGBA8);

        int mMVPMatrixHandle = GLES20.glGetUniformLocation(computeProgramID, "u_MVPMatrix");
        glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        glDispatchCompute(WIDTH / workgroupSize, HEIGHT / workgroupSize, 1);
        // GL_COMPUTE_SHADER_BIT is the same as GL_SHADER_IMAGE_ ACCESS_BARRIER_BIT
        glMemoryBarrier(GL_COMPUTE_SHADER_BIT);
    }


    private float[] getMVPMatrix(float angleInDegrees, float translateX) {
        float[] mModelMatrix = new float[16];
        ;
        float[] mMVPMatrix = new float[16];
        float[] mProjectionMatrix = new float[16];
        ;

        // System.out.println("Angel " + angleInDegrees);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, 0, 1.0f, 0.0f, 0.0f);

        Matrix.rotateM(mModelMatrix, 0, angleInDegrees + horizontalAngleRotation, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, translateX + horizontalShift, 0.0f, 1.0f);


        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        Matrix.frustumM(mProjectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f + distanceAmendment, 40.0f);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        return mMVPMatrix;
    }

    private int createQuadFullScreenVao() {
        int[] buffers = s_CREATE_BUFFER_ID.get();
        if (buffers == null) {
            buffers = new int[1];
            s_CREATE_BUFFER_ID.set(buffers);
        }
        int vao[] = s_CREATE_VERTEX_ARRAY_ID.get();
        if (vao == null) {
            vao = new int[1];
            s_CREATE_VERTEX_ARRAY_ID.set(vao);
        }

        glGenVertexArrays(1, vao, 0);

        glGenBuffers(1, buffers, 0);

        glBindBuffer(GL_ARRAY_BUFFER, buffers[0]);
        int capacity = 2 * 6;
        ByteBuffer bb = ByteBuffer.allocate(capacity);
        bb.put((byte) -1).put((byte) -1);
        bb.put((byte) 1).put((byte) -1);
        bb.put((byte) 1).put((byte) 1);
        bb.put((byte) 1).put((byte) 1);
        bb.put((byte) -1).put((byte) 1);
        bb.put((byte) -1).put((byte) -1);
        bb.flip();
        glBufferData(GL_ARRAY_BUFFER, capacity, bb, GL_STATIC_DRAW);
        GLES20.glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GLES20.GL_BYTE, false, 0, 0);
        glBindVertexArray(0);
        return vao[0];
    }

    public void initVars() {
        // config opengl
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        loadShaders();
        // draw texture directly
//        drawProgramID = GLES20.glCreateProgram();
//        int drawVertID = createShader("draw.vert", GLES20.GL_VERTEX_SHADER);
//        int drawFragID = createShader("draw.frag", GLES20.GL_FRAGMENT_SHADER);
//        GLES20.glAttachShader(drawProgramID, drawVertID);
//        GLES20.glAttachShader(drawProgramID, drawFragID);
//        GLES20.glLinkProgram(drawProgramID);
//        checkProgramStatus(drawProgramID);

//        clearProgramID = GLES20.glCreateProgram();
//        int clearCompID = createShader("clear.comp", GL_COMPUTE_SHADER);
//        GLES20.glAttachShader(clearProgramID, clearCompID);
//        GLES20.glLinkProgram(clearProgramID);
////        checkProgramStatus(clearProgramID);
//
//        computeProgramID =  GLES20.glCreateProgram();
//        int computeCompID = createShader("draw.comp", GL_COMPUTE_SHADER);
//        GLES20.glAttachShader(computeProgramID, computeCompID);
//        GLES20.glLinkProgram(computeProgramID);
////        checkProgramStatus(computeProgramID);

        // we tell how this called in shaders
        GLES20.glBindAttribLocation(drawProgramID, 0, "vertex");
        // FIXME: 18.06.2017 no direct analogue brain application required
//        glBindFragDataLocation(drawProgramID, 0, "color");

        fullScreenVao = createQuadFullScreenVao();

        GLES20.glUseProgram(drawProgramID);
        // Set sampler2d in GLSL fragment shader to texture unit 0
        GLES20.glUniform1i(GLES20.glGetUniformLocation(drawProgramID, "uResult0Tex"), 0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(drawProgramID, "uImage0Tex"), 1);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(drawProgramID, "uResult1Tex"), 2);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(drawProgramID, "uImage1Tex"), 3);

        GLES20.glUseProgram(computeProgramID);
        // Set sampler2d in GLSL fragment shader to texture unit 0
        GLES20.glUniform1i(GLES20.glGetUniformLocation(computeProgramID, "inputImage"), 0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(computeProgramID, "resultImage"), 1);

        //TODO no idea what is this
        workgroupSize = 16;
        System.out.println("Work group size = " + workgroupSize);

        String[] images = {"103621", "103537"};
        for (int i = 0; i < IMAGES_COUNT; i++) {
            imageTexturesID[0] = loadTexture(Loader.res, R.drawable.a103621color, GL_RGBA8);
            depthTexturesID[0] = loadTexture(Loader.res, R.drawable.a103621depth, GL_RGBA8);
            resultTexturesID[0] = createFramebufferTexture();
            imageTexturesID[1] = loadTexture(Loader.res, R.drawable.a103537color, GL_RGBA8);
            depthTexturesID[1] = loadTexture(Loader.res, R.drawable.a103537depth, GL_RGBA8);
            resultTexturesID[1] = createFramebufferTexture();
        }
    }

//    public static String getShaderCode(String name) {
//        // FIXME: 18.06.2017 load shaders
//        InputStream is = ShadersPreview.class.getResourceAsStream(name);
//        final DataInputStream dataStream = new DataInputStream(is);
//        byte[] shaderCode;
//        try {
//            shaderCode = new byte[dataStream.available()];
//            dataStream.readFully(shaderCode);
//            is.close();
//        } catch (Throwable e) {
//            return "";
//        }
//
//        return new String(shaderCode);
//    }

    private int createFramebufferTexture() {
        // FIXME: 18.06.2017 too many parameters
        int texture[] = s_CREATE_TEXTURE_ID.get();
        if (texture == null) {
            texture = new int[1];
            s_CREATE_TEXTURE_ID.set(texture);
        }

        glGenTextures(1, texture, 0);
        int tex = texture[0];
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        ByteBuffer black = null;
        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, WIDTH, HEIGHT, 0, GLES20.GL_RGBA, GLES20.GL_INT, black);
        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }

//    private int createShader(String name, int type) {
//        int shaderID = GLES20.glCreateShader(type);
//        GLES20.glShaderSource(shaderID, getShaderCode(name).toString());
//        GLES20.glCompileShader(shaderID);
////        checkShaderStatus(name, shaderID);
//        return shaderID;
//    }

//    public void checkProgramStatus(int programID) {
//        // FIXME: 18.06.2017 parameters
//        int[] statuses = new int[0];
//        glGetProgramiv(programID, GLES20.GL_LINK_STATUS, statuses, 0 );
////        if (status != GLES20.GL_TRUE) {
////            System.err.println(GLES20.glGetProgramInfoLog(programID));
////        }
//    }

//    private void checkShaderStatus(String name, int shaderID) {
//        int status = glGetShaderiv(shaderID, GLES20.GL_COMPILE_STATUS);
//        if (status != GLES20.GL_TRUE) {
//            System.err.println(name);
//            System.err.println(GLES20.glGetShaderInfoLog(shaderID));
//        }
//    }

    //    public static int loadTexture(String path) {
//        ByteBuffer image;
//        int width, height;
//
//        try (MemoryStack stack = MemoryStack.stackPush()) {
//            /* Prepare image buffers */
//            IntBuffer w = stack.mallocInt(1);
//            IntBuffer h = stack.mallocInt(1);
//            IntBuffer comp = stack.mallocInt(1);
//
//            /* Load image */
//            stbi_set_flip_vertically_on_load(true);
//            image = stbi_load(path, w, h, comp, 4);
//            if (image == null) {
//                throw new RuntimeException("Failed to load a texture file!("+path+")"
//                        + System.lineSeparator() + stbi_failure_reason());
//            }
//
//            /* Get width and height of image */
//            width = w.get();
//            height = h.get();
//        }
//
//        // create texture
//        int textureID = glGenTextures();
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, image);
//
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
//
//        return textureID;
    public static int loadTexture(Resources resources, int resource, int internalFormat) {
        int[] textures = s_LOAD_TEXTURE_ID.get();
        if (textures == null) {
            textures = new int[1];
            s_LOAD_TEXTURE_ID.set(textures);
        }

        glActiveTexture(GL_TEXTURE0);
        glGenTextures(1, textures, 0);

        int texture = textures[0];
        glBindTexture(GL_TEXTURE_2D, texture);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        Bitmap bitmap = BitmapFactory.decodeResource(resources, resource);

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();


        GLUtils.texImage2D(GL_TEXTURE_2D, 0, internalFormat, bitmap, GL_UNSIGNED_BYTE, 0);

        bitmap.recycle();

        glBindTexture(GL_TEXTURE_2D, 0);

        return texture;
    }


    /**
     * have not touched rest of file
     */

    public void run() {
//        System.out.println("LWJGL version " + Version.getVersion());

        init();


//        // Free the window callbacks and destroy the window
//        glfwFreeCallbacks(window);
//        glfwDestroyWindow(window);
//
//        // Terminate GLFW and free the error callback
//        glfwTerminate();
//        glfwSetErrorCallback(null).free();
    }

    private void init() {
//        // Setup an error callback. The default implementation
//        // will print the error message in System.err.
//        GLFWErrorCallback.createPrint(System.err).set();
//
//        // Initialize GLFW. Most GLFW functions will not work before doing this.
//        if ( !glfwInit() )
//            throw new IllegalStateException("Unable to initialize GLFW");
//
//        // Configure GLFW
//        glfwDefaultWindowHints(); // optional, the current window hints are already the default
//        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
//        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
//        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
//        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
//        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
//
//        // Create the window
//        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Shaders Preview", NULL, NULL);
//        if ( window == NULL )
//            throw new RuntimeException("Failed to create the GLFW window");
//
//        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
//        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
//            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE ) {
//                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
//            }else if (key == GLFW_KEY_LEFT||key == GLFW_KEY_RIGHT){
//                System.out.println("Direction button pressed:"+key);
//                horizontalAngleRotation = (key == GLFW_KEY_LEFT )? horizontalAngleRotation - ROTATION_STEP_ANGLE :horizontalAngleRotation+ROTATION_STEP_ANGLE;            }
//        });
//        glfwSetScrollCallback(window, (window, xoffset, yoffset)->{
//            if(yoffset>=0){
//                distanceAmendment += AMENDMENT_STEP;
//            }else{
//                distanceAmendment -= AMENDMENT_STEP;
//            }
//        });
//
//        glfwSetCursorPosCallback(window, (window, xpos, ypos)->{
//            horizontalShift = (float)xpos/(WINDOW_HEIGHT/2)-1;
//        });
//
//
//
//        // Get the thread stack and push a new frame
//        try ( MemoryStack stack = stackPush() ) {
//            IntBuffer pWidth = stack.mallocInt(1); // int*
//            IntBuffer pHeight = stack.mallocInt(1); // int*
//
//            // Get the window size passed to glfwCreateWindow
//            glfwGetWindowSize(window, pWidth, pHeight);
//
//            // Get the resolution of the primary monitor
//            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
//
//            // Center the window
//            glfwSetWindowPos(
//                    window,
//                    (vidmode.width() - pWidth.get(0)) / 2,
//                    (vidmode.height() - pHeight.get(0)) / 2
//            );
//        } // the stack frame is popped automatically
//
//        // Make the OpenGL context current
//        glfwMakeContextCurrent(window);
//        // Enable v-sync
//        glfwSwapInterval(1);
//        // Make the window visible
//        glfwShowWindow(window);
//
//        GL.createCapabilities();



    }

    public void initInitialView() {
        // init camera view
        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -3.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
    }


//    public static void main(String[] args) {
//        new ShadersPreview().run();
//    }
}
