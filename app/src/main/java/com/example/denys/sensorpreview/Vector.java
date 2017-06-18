package com.example.denys.sensorpreview;

/**
 * Created by chuq on 17.06.2017.
 */

public class Vector {
    public double x;
    public double y;
    public double z;

    Vector(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    Vector( final Vector other )
    {
        this(other.x, other.y, other.z);
    }

    Vector(float[] arr)
    {
        x = arr[0];
        y = arr[1];
        z = arr[2];
    }

    void set(float[] arr)
    {
        x = arr[0];
        y = arr[1];
        z = arr[2];
    }

    void mul(double n)
    {
        x *= n;
        y *= n;
        z *= n;
    }

    void div(double n)
    {
        if(n==0)
            throw new ArithmeticException("div by zero");

        x /= n;
        y /= n;
        z /= n;
    }

    void add(final Vector v)
    {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    void sub(final Vector v)
    {
        x -= v.x;
        y -= v.y;
        z -= v.z;
    }

    double abs()
    {
        return Math.sqrt(x*x + y*y + z*z);
    }

    void normalize()
    {
        div(abs());
    }

    void rotate(final Vector angles)
    {
        x *= Math.cos(angles.x);
        y *= Math.cos(angles.y);
        z *= Math.cos(angles.z);
    }

    public String toString()
    {
        return String.format("[%.3f,%.3f,%.3f]", x, y, z);
    }

    static Vector mul(final Vector v, double n)
    {
        Vector res = new Vector(v);
        res.mul(n);
        return res;
    }

    static Vector div(final Vector v, double n)
    {
        Vector res = new Vector(v);
        res.div(n);
        return res;
    }

    static Vector add(final Vector lv, final Vector rv)
    {
        return new Vector(lv.x+rv.x, lv.y+rv.y, lv.z+rv.z);
    }

    static Vector sub(final Vector lv, final Vector rv)
    {
        return new Vector(lv.x-rv.x, lv.y-rv.y, lv.z-rv.z);
    }


    public static Vector linearInterpolation(final Vector a, final Vector b, final double position) {
        final double deltaX = b.x - a.x;
        final double deltaY = b.y - a.y;
        final double deltaZ = b.z - a.z;

        return new Vector(a.x + deltaX * position, a.y + deltaY * position, a.z + deltaZ * position);
    }
}
