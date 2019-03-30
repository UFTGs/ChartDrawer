package com.example.chartdrawer;

import android.opengl.GLES32;

import java.util.HashMap;

import static android.opengl.GLES32.glUniform4f;
import static android.opengl.GLES32.glUniformMatrix4fv;
import static android.opengl.GLES32.glUseProgram;

public class ShaderProg32 implements IShaderProg {

    int vertexShaderHandle;
    int fragmentShaderHandle;
    int geomShaderHandle;
    int programHandle;

    private HashMap<String, Integer> attrs = new HashMap<>();
    private HashMap<String, Integer> uniforms = new HashMap<>();

    boolean initialized = false;

    public ShaderProg32(String vertexShader, String fragmentShader, String geometryShader)
    {
        vertexShaderHandle = CreateShader(GLES32.GL_VERTEX_SHADER, vertexShader);
        fragmentShaderHandle = CreateShader(GLES32.GL_FRAGMENT_SHADER, fragmentShader);
        if(!geometryShader.isEmpty())
            geomShaderHandle = CreateShader(GLES32.GL_GEOMETRY_SHADER, geometryShader);

        if(vertexShaderHandle == 0) {
            initialized = false;
            throw new RuntimeException("Error creating vertex shader.");
        }

        if(fragmentShaderHandle == 0) {
            initialized = false;
            throw new RuntimeException("Error creating fragment shader.");
        }

        programHandle = GLES32.glCreateProgram();

        if (programHandle != 0)
        {
            GLES32.glAttachShader(programHandle, vertexShaderHandle);

            if(!geometryShader.isEmpty() && geomShaderHandle != 0)
                GLES32.glAttachShader(programHandle, geomShaderHandle);

            GLES32.glAttachShader(programHandle, fragmentShaderHandle);

            if(!geometryShader.isEmpty() && geomShaderHandle != 0) {
                GLES32.glLinkProgram(programHandle);

                final int[] linkStatus = new int[1];
                GLES32.glGetProgramiv(programHandle, GLES32.GL_LINK_STATUS, linkStatus, 0);
                String str = GLES32.glGetProgramInfoLog(programHandle);

                if (linkStatus[0] == 0)
                {
                    GLES32.glDeleteProgram(programHandle);
                    programHandle = 0;
                }
            } else {
                GLES32.glLinkProgram(programHandle);

                final int[] linkStatus = new int[1];
                GLES32.glGetProgramiv(programHandle, GLES32.GL_LINK_STATUS, linkStatus, 0);

                if (linkStatus[0] == 0)
                {
                    GLES32.glDeleteProgram(programHandle);
                    programHandle = 0;
                }
            }
        }

        if (programHandle == 0) {
            initialized = false;
            throw new RuntimeException("Error creating program.");
        }

        InitUnitform("mvp", "scr", "u_Color");
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

            attrs.put(attr, GLES32.glGetAttribLocation(programHandle, attr));
        }
    }

    public void InitUnitform(String... uniformList)
    {
        for(String uniform : uniformList)
        {
            uniforms.remove(uniform);

            uniforms.put(uniform, GLES32.glGetUniformLocation(programHandle, uniform));
        }
    }

    @Override
    public void ApplyProjection(float[] projMat, float[] scrMat)
    {
        glUniformMatrix4fv(uniforms.get("mvp"), 1, false, projMat, 0);
        glUniformMatrix4fv(uniforms.get("scr"), 1, false, scrMat, 0);
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
            GLES32.glEnableVertexAttribArray(attr);
    }

    @Override
    public void disableAttribs() {
        for(Integer attr : attrs.values())
            GLES32.glDisableVertexAttribArray(attr);
    }

    private int CreateShader(int type, String code)
    {
        int shaderHandle = GLES32.glCreateShader(type);

        if (shaderHandle != 0)
        {
            GLES32.glShaderSource(shaderHandle, code);
            GLES32.glCompileShader(shaderHandle);
            final int[] compileStatus = new int[1];
            GLES32.glGetShaderiv(shaderHandle, GLES32.GL_COMPILE_STATUS, compileStatus, 0);

            if (compileStatus[0] == 0)
            {
                String str = GLES32.glGetShaderInfoLog(shaderHandle);
                GLES32.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        return shaderHandle;
    }
}
