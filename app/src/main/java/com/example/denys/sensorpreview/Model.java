package com.example.denys.sensorpreview;

import android.util.Log;
import android.view.Display;

/**
 * Created by chuq on 17.06.2017.
 */

public class Model {
    protected int mProgId;
    protected float[] mRotationVectorMatrix = new float[16];
    protected float ax;
    protected float ay;
    protected float az;
    protected boolean screenup;

    public Model()
    {
        mRotationVectorMatrix[0] = 1;
        mRotationVectorMatrix[4] = 1;
        mRotationVectorMatrix[8] = 1;
        mRotationVectorMatrix[12] = 1;
    }

    public void rotate(final float[] rotationMatrix)
    {
        mRotationVectorMatrix = rotationMatrix;
    }

    public void rotateAngles(final float[] angles)
    {
        final float x = angles[1];
        final float y = angles[2];
        final float z = angles[0];

        az = - z*180/(float)Math.PI;
        ay = - y*180/(float)Math.PI;

        screenup = (Math.abs(ay) < 90 || Math.abs(az) < 90);

        ax = - x*180/(float)Math.PI;
        if(screenup)
        {
            if(ax < 0)
                ax += 360;
        }
        else
            ax = 180 - ax;

        if(ay < 0)
            ay += 360;

        ay *= -1;

        if(az<0)
            az += 360;
    }

    public void loadShaders()
    {

    }

    public void changeProjection(int width, int height)
    {

    }

    public void draw()
    {

    }
}
