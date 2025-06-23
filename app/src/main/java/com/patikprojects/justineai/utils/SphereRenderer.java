package com.patikprojects.justineai.utils;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SphereRenderer implements GLSurfaceView.Renderer {

    private float angle = 0f;
    private float time = 0f;

    // Toggle and transition variables
    public boolean animate = false;
    private float deformationStrength = 0f;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0f, 0f, 0f, 0.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnable(GL10.GL_POINT_SMOOTH);
        gl.glPointSize(6f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        float aspect = (float) width / height;
        GLU.gluPerspective(gl, 45.0f, aspect, 1f, 100f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        gl.glLoadIdentity();
        GLU.gluLookAt(gl, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f);
        gl.glRotatef(angle, 1f, 1f, 0f);

        // Smooth transition logic
        float target = animate ? 1f : 0f;
        deformationStrength += (target - deformationStrength) * 0.1f;

        FloatBuffer vertexBuffer = generateSpherePoints(40, 40, 1.3f, deformationStrength);
        gl.glColor4f(0.5176f, 0.1490f, 1F, 0.3922f); // 0f, 1f, 1f, 1f
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
        gl.glDrawArrays(GL10.GL_POINTS, 0, vertexBuffer.capacity() / 3);

        angle += 0.5f;
        time += 0.05f;
    }

    private FloatBuffer generateSpherePoints(int latBands, int longBands, float baseRadius, float deformStrength) {
        List<Float> points = new ArrayList<>();

        for (int lat = 0; lat <= latBands; lat++) {
            double theta = lat * Math.PI / latBands;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int lon = 0; lon <= longBands; lon++) {
                double phi = lon * 2 * Math.PI / longBands;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                float x = (float) (cosPhi * sinTheta);
                float y = (float) (cosTheta);
                float z = (float) (sinPhi * sinTheta);

                float ripple = (float) (Math.sin(time + lat * 0.3 + lon * 0.2) * 0.15f);
                float r = baseRadius + ripple * deformStrength;

                points.add(x * r);
                points.add(y * r);
                points.add(z * r);
            }
        }

        float[] pointArray = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            pointArray[i] = points.get(i);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(pointArray.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(pointArray);
        fb.position(0);
        return fb;
    }
}
