package com.example.denys.sensorpreview;

import android.content.res.Resources;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.opengl.GLES20.*;

/**
 * Created by chuq on 17.06.2017.
 */

public class Loader {
    private static final String TAG = "Juicy-Reality";

    private static int[] status = new int[1];
    public static Resources res;

    public static void Init(Resources resources)
    {
        res = resources;
    }

    private static String loadFromAssets(String fileName) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(res.getAssets().open(fileName), "UTF-8"));

        // do reading, usually loop until end of file reading
        String mLine = reader.readLine();
        while (mLine != null)
        {
            sb.append(mLine).append('\n');
            mLine = reader.readLine();
        }

        reader.close();
        return sb.toString();
    }

    public static int buildProgramFromAssets(String vertexFileName, String fragmentFileName, final String[] attrs)
    {
        int result = 0;

        try {
            String vertexSource = loadFromAssets(vertexFileName);
            String fragmentSource = loadFromAssets( fragmentFileName);
            result = buildProgram(vertexSource, fragmentSource, attrs);
        }
        catch(IOException e)
        {
            Log.d(TAG, e.getMessage());
            return 0;
        }

        return result;
    }
    public static int buildProgramFromAssets(String computingFileName, int shaderType,   final String[] attrs)
    {
        int result = 0;

        try {
            String geometryShaderSource = loadFromAssets(computingFileName);
            result = buildProgram(geometryShaderSource, shaderType);
        }
        catch(IOException e)
        {
            Log.d(TAG, e.getMessage());
            return 0;
        }

        return result;
    }

    public static int buildProgram(String vertexSource, String fragmentSource, final String[] attrs )
    {
        int vertexShader = buildShader(vertexSource, GL_VERTEX_SHADER);
        if (vertexShader == 0)
            return 0;

        int fragmentShader = buildShader(fragmentSource, GL_FRAGMENT_SHADER);
        if (fragmentShader == 0)
            return 0;

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);

        // Bind attributes
        if (attrs != null)
        {
            final int size = attrs.length;
            for (int i = 0; i < size; i++)
            {
                glBindAttribLocation(program, i, attrs[i]);
            }
        }

        glLinkProgram(program);

        glGetProgramiv(program, GL_LINK_STATUS, status, 0);
        if (status[0] != GL_TRUE)
        {
            String error = glGetProgramInfoLog(program);
            Log.d(TAG, "Error while linking program:\n" +error);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            return 0;
        }

        return program;
    }

    public static int buildProgram(String shaderSource, int shaderType)
    {
        int computeShader = buildShader(shaderSource, shaderType);
        if (computeShader == 0)
            return 0;

        int program = glCreateProgram();
        glAttachShader(program, computeShader);

        glLinkProgram(program);

        glGetProgramiv(program, GL_LINK_STATUS, status, 0);
        if (status[0] != GL_TRUE)
        {
            String error = glGetProgramInfoLog(program);
            Log.d(TAG, "Error while linking program:\n" + error);
            glDeleteShader(computeShader);
            glDeleteProgram(program);
            return 0;
        }

        return program;
    }



    public static int buildShader(String source, int type)
    {
        int shader = glCreateShader(type);

        glShaderSource(shader, source);

        glCompileShader(shader);

        glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);
        if (status[0] != GL_TRUE)
        {
            String error = glGetShaderInfoLog(shader);
            Log.e(TAG, "Error while compiling shader:\n" +error);
            glDeleteShader(shader);
//            System.exit(1);
            return 0;
        }

        return shader;
    }

    public static int checkGlError()
    {
        int error = glGetError();

        if (error != GL_NO_ERROR)
            Log.d(TAG, "GL error = 0x" +Integer.toHexString(error));

        return error;
    }

}
