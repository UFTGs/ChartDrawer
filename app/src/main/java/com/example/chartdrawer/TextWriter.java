package com.example.chartdrawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_REPEAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glVertexAttribPointer;

public class TextWriter {
    Context context;

    public TextWriter(Context context)
    {
        this.context = context;

        arialFont = new BitmapFont();

        try {
            InitFont(arialFont, R.raw.arial);
            InitTexture(arialFont, R.mipmap.arial_0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        shaderProg = new ShaderProg20(FileUtils.readTextFromRaw(context, R.raw.v_tex_shader), FileUtils.readTextFromRaw(context, R.raw.f_tex_shader));
        shaderProg.InitAttribute("texPos");
    }

    ShaderProg20 shaderProg;

    BitmapFont arialFont;

    private boolean InitFont(BitmapFont font, int configResource) throws IOException {
        InputStream fontConfigStream = context.getResources().openRawResource(configResource);
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(fontConfigStream, null);
            parser.nextTag();
            readXmlFont(parser, arialFont);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return false;
        } finally {
            fontConfigStream.close();
        }
        return true;
    }

    private void readXmlFont(XmlPullParser parser, BitmapFont font) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, "", "font");
        while(parser.getEventType() != XmlPullParser.END_DOCUMENT)
        {
            if(parser.getEventType() != XmlPullParser.START_TAG) {
                parser.next();
                continue;
            }
            String name = parser.getName();
            if(parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("common"))
            {
                font.w = Integer.parseInt(parser.getAttributeValue("", "scaleW"));
                font.h = Integer.parseInt(parser.getAttributeValue("", "scaleH"));
                font.lineHeight = Integer.parseInt(parser.getAttributeValue("", "lineHeight")) / (1f * font.h);
                font.baseLine = Integer.parseInt(parser.getAttributeValue("", "base")) / (1f * font.h);
            } else if(parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("char"))
            {
                Glyph glyph = new Glyph();
                glyph.id = Integer.parseInt(parser.getAttributeValue("", "id"));
                glyph.x = Integer.parseInt(parser.getAttributeValue("", "x")) / (1f * font.w);
                glyph.y = Integer.parseInt(parser.getAttributeValue("", "y")) / (1f * font.h);
                glyph.w = Integer.parseInt(parser.getAttributeValue("", "width")) / (1f * font.h);
                glyph.h = Integer.parseInt(parser.getAttributeValue("", "height")) / (1f * font.h);
                glyph.xo = Integer.parseInt(parser.getAttributeValue("", "xoffset")) / (1f * font.h);
                glyph.yo = Integer.parseInt(parser.getAttributeValue("", "yoffset")) / (1f * font.h);
                if(glyph.id == 32)
                    glyph.w = 0.05f;
                font.glyphs.put(glyph.id, glyph);
            }
            parser.next();
        }
    }

    private void readXmlCommon(XmlPullParser parser, BitmapFont font) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, "", "common");
        font.w = Integer.parseInt(parser.getAttributeValue("", "scaleW"));
        font.h = Integer.parseInt(parser.getAttributeValue("", "scaleH"));
        font.lineHeight = Integer.parseInt(parser.getAttributeValue("", "lineHeight")) / (1f * font.h);
        font.baseLine = Integer.parseInt(parser.getAttributeValue("", "base")) / (1f * font.h);
    }

    private void readXmlChars(XmlPullParser parser, BitmapFont font) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, "", "chars");
        while(parser.next() != XmlPullParser.END_TAG)
        {
            if(parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            switch (name)
            {
                case "char":
                    readXmlChar(parser, font);
                    continue;
            }
        }
    }

    private void readXmlChar(XmlPullParser parser, BitmapFont font) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, "", "char");

        Glyph glyph = new Glyph();
        glyph.id = Integer.parseInt(parser.getAttributeValue("", "id"));
        glyph.x = Integer.parseInt(parser.getAttributeValue("", "x")) / (1f * font.w);
        glyph.y = Integer.parseInt(parser.getAttributeValue("", "y")) / (1f * font.h);
        glyph.w = Integer.parseInt(parser.getAttributeValue("", "width")) / (1f * font.w);
        glyph.h = Integer.parseInt(parser.getAttributeValue("", "height")) / (1f * font.h);
        glyph.h = Integer.parseInt(parser.getAttributeValue("", "xoffset")) / (1f * font.w);
        glyph.xo = Integer.parseInt(parser.getAttributeValue("", "yoffset")) / (1f * font.h);
        if(glyph.id == 32)
            glyph.w = 0.05f;
        font.glyphs.put(glyph.id, glyph);
    }

    private void InitTexture(BitmapFont font, int texRes)
    {
        int[] textures;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.arial_0);
        textures = new int[1];
        glGenTextures(1, textures, 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture( GL_TEXTURE_2D, textures[0]);

        glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        glBindTexture(GL_TEXTURE_2D, 0);
        font.texID = textures[0];
    }

    public float TextWidth(String text, float size)
    {
        float res = 0;
        for (int i = 0; i < text.length(); ++i) {
            if (!arialFont.glyphs.containsKey((int) text.charAt(i)))
                continue;
            Glyph g = arialFont.glyphs.get((int) text.charAt(i));
            res += (g.w + g.xo) * size;
        }
        return res / arialFont.lineHeight;
    }

    public void DrawText(String text, float size, float x, float y, float[] matr, float cr, float cg, float cb, float ca)
    {
        // Prepare vertex buffer for text
        FloatBuffer verts = ByteBuffer.allocateDirect(text.length() * 96).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verts.position(0);

        float lineHeight = arialFont.baseLine;
        size /= arialFont.lineHeight;
        float xs = x;
        float par = 1;
        for (int i = 0; i < text.length(); ++i)
        {
            if(!arialFont.glyphs.containsKey((int)text.charAt(i)))
                continue;
            Glyph g = arialFont.glyphs.get((int)text.charAt(i));

            verts.put(xs + g.xo * size);
            verts.put((lineHeight - (g.h + g.yo) + g.h) * size + y);
            verts.put(g.x);
            verts.put(g.y);

            verts.put(xs + (g.w + g.xo) * size);
            verts.put((lineHeight - (g.h + g.yo) + g.h) * size + y);
            verts.put(g.x + g.w);
            verts.put(g.y);

            verts.put(xs + (g.w + g.xo) * size);
            verts.put((lineHeight - (g.h + g.yo)) * size + y);
            verts.put(g.x + g.w);
            verts.put(g.y + g.h);

            verts.put(xs + (g.w + g.xo) * size);
            verts.put((lineHeight - (g.h + g.yo)) * size + y);
            verts.put(g.x + g.w);
            verts.put(g.y + g.h);

            verts.put(xs + g.xo * size);
            verts.put((lineHeight - (g.h + g.yo)) * size + y);
            verts.put(g.x);
            verts.put(g.y + g.h);

            verts.put(xs + g.xo * size);
            verts.put((lineHeight - (g.h + g.yo) + g.h) * size + y);
            verts.put(g.x);
            verts.put(g.y);

            xs += (g.w + g.xo) * size;
        }
        shaderProg.useProgram();
        shaderProg.SetColor(cr, cg, cb, ca);
        shaderProg.enableAttribs();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, arialFont.texID);
        glEnable(GL_BLEND);
        shaderProg.ApplyProjection(matr, null);
        verts.position(0);
        glVertexAttribPointer(shaderProg.getAttr("vertPos"), 2, GL_FLOAT, false, 16, verts);
        verts.position(2);
        glVertexAttribPointer(shaderProg.getAttr("texPos"), 2, GL_FLOAT, false, 16, verts);
        glDrawArrays(GL_TRIANGLES, 0, text.length() * 6);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
