package com.example.denys.sensorpreview;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import static android.opengl.GLES20.*;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class Renderer implements GLSurfaceView.Renderer, SensorEventListener
{
    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;

    /** Used for debug logs. */
    private static final String TAG = "Juicy-Reality";
    Horse model;

    /**
     * Initialize the model data.
     */
    public Renderer(SensorManager sensorManager)
    {
        mSensorManager = sensorManager;
        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        model = new Horse();
    }

    public void start()
    {
        mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
    }

    public void stop()
    {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        // Set the background clear color to black.
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Use culling to remove back faces.
        glEnable(GL_CULL_FACE);

        // Enable depth testing
        glEnable(GL_DEPTH_TEST);

        model.loadShaders();
        model.initVars();
        model.initInitialView();

    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        model.changeProjection(width, height);
    }

    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        model.draw();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR )
        {
            //Log.d("Rotation", Arrays.toString(mRotationVectorMatrix));

            float[] rotationMatrix = new float[16];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            model.rotate(rotationMatrix);
        }
    }
}
