attribute vec2 vertPos;
attribute vec2 texPos;

uniform mat4 mvp;

varying vec2 texCoord;

void main() {
    gl_Position = mvp * vec4(vertPos, 0, 1);
    texCoord = texPos;
}