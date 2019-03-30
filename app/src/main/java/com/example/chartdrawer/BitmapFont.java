package com.example.chartdrawer;

import java.util.HashMap;

public class BitmapFont {
    public HashMap<Integer, Glyph> glyphs = new HashMap<Integer, Glyph>();
    public byte[] pixelsData = null;
    public int w = 0;
    public int h = 0;
    public float lineHeight = 0;
    public float baseLine = 0;
    public int texID = 0;
}
