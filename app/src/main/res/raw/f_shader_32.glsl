#version 320 es
precision mediump float;
uniform vec4 u_Color;
out vec4 fragmentColor;

void main() {
    fragmentColor = u_Color;
}