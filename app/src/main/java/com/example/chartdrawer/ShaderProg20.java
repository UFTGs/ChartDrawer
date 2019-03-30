package com.example.chartdrawer;

import android.opengl.GLES20;

import java.util.HashMap;

import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;

public class ShaderProg20 implements IShaderProg {

    int vertexShaderHandle;
    int fragmentShaderHandle;
    int programHandle;

    private HashMap<String, Integer> attrs = new HashMap<>();
    private HashMap<String, Integer> uniforms = new HashMap<>();

    boolean initialized = false;

    public ShaderProg20(String vertexShader, String fragmentShader)
    {
        // Load in the vertex shader.
        vertexShaderHandle = CreateShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        fragmentShaderHandle = CreateShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        if(vertexShaderHandle == 0) {
            initialized = false;
            throw new RuntimeException("Error creating vertex shader.");
        }

        if(fragmentShaderHandle == 0) {
            initialized = false;
            throw new RuntimeException("Error creating fragment shader.");
        }

        programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            GLES20.glLinkProgram(programHandle);

            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            initialized = false;
            throw new RuntimeException("Error creating program.");
        }

        InitUnitform("mvp", "u_Color");
        InitAttribute("vertPos");

        initialized = true;
    }

    public int getAttr(String attr)
    {
        return attrs.get(attr);
    }

    public void InitAttribute(String... attrList)
    {
        for(String attr : attrList)
        {
            attrs.remove(attr);

            attrs.put(attr, GLES20.glGetAttribLocation(programHandle, attr));
        }
    }

    public void InitUnitform(String... uniformList)
    {
        for(String uniform : uniformList)
        {
            uniforms.remove(uniform);

            uniforms.put(uniform, GLES20.glGetUniformLocation(programHandle, uniform));
        }
    }

    @Override
    public void ApplyProjection(float[] projMat, float[] scrMat)
    {
        glUniformMatrix4fv(uniforms.get("mvp"), 1, false, projMat, 0);
    }

    @Override
    public void SetColor(float r, float g, float b, float a)
    {
        glUniform4f(uniforms.get("u_Color"),r,g,b,a);
    }

    @Override
    public void useProgram()
    {
        glUseProgram(programHandle);
    }

    @Override
    public void enableAttribs() {
        for(Integer attr : attrs.values())
            glEnableVertexAttribArray(attr);
    }

    @Override
    public void disableAttribs() {
        for(Integer attr : attrs.values())
            glDisableVertexAttribArray(attr);
    }

    private int CreateShader(int type, String code)
    {
        int shaderHandle = GLES20.glCreateShader(type);

        if (shaderHandle != 0)
        {
            GLES20.glShaderSource(shaderHandle, code);

            GLES20.glCompileShader(shaderHandle);

            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            String str = glGetShaderInfoLog(shaderHandle);

            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        return shaderHandle;
    }
}
