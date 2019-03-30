precision mediump float;
uniform vec4 u_Color;
uniform sampler2D texture;
varying vec2 texCoord;

void main() {
    vec4 t = texture2D(texture, texCoord);
    gl_FragColor = u_Color * vec4(1.0, 1.0, 1.0, t.r);
}