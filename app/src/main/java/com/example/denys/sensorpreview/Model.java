package com.example.denys.sensorpreview;

/**
 * Created by chuq on 17.06.2017.
 */

public class Model {
    protected int mProgId;
    protected float[] mRotationVectorMatrix = new float[16];

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
