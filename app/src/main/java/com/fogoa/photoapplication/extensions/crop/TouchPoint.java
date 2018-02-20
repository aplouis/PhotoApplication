package com.fogoa.photoapplication.extensions.crop;


public class TouchPoint {
    private float x;
    private float y;

    public TouchPoint() {
    }

    public TouchPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getLength() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public TouchPoint copy(TouchPoint other) {
        x = other.getX();
        y = other.getY();
        return this;
    }

    public TouchPoint set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public TouchPoint add(TouchPoint value) {
        this.x += value.getX();
        this.y += value.getY();
        return this;
    }

    public static TouchPoint subtract(TouchPoint lhs, TouchPoint rhs) {
        return new TouchPoint(lhs.x - rhs.x, lhs.y - rhs.y);
    }

    @Override
    public String toString() {
        return String.format("(%.4f, %.4f)", x, y);
    }
}
