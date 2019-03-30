#version 320 es
layout (lines_adjacency) in;
layout (triangle_strip, max_vertices = 8) out;
uniform mat4 mvp;
uniform mat4 scr;

float pscal(vec4 v1, vec4 v2)
{
    return v1.x * v2.y - v1.y * v2.x;
}

void main() {
    vec4 bef = normalize(gl_in[1].gl_Position - gl_in[0].gl_Position);
    vec4 line = normalize(gl_in[2].gl_Position - gl_in[1].gl_Position);
    vec4 aft = normalize(gl_in[3].gl_Position - gl_in[2].gl_Position);

    vec4 aftN = vec4(-aft.y, aft.x, aft.z, aft.w);
    vec4 lineN = vec4(-line.y, line.x, line.z, line.w);
    vec4 befN = vec4(-bef.y, bef.x, bef.z, bef.w);

    vec4 n1 = normalize(befN + lineN);
    vec4 n2 = normalize(lineN + aftN);

    float w = 3.2;

    float cosa1 = dot(n1, lineN);
    float cosa2 = dot(n2, aftN);

    n1 = n1 * w;
    n2 = n2 * w;

    aftN = aftN * w;
    lineN = lineN * w;
    befN = befN * w;

    gl_Position = gl_in[1].gl_Position + scr * n1;
    EmitVertex();

    gl_Position = gl_in[1].gl_Position - scr * n1;
    EmitVertex();

    gl_Position = gl_in[1].gl_Position + scr * lineN;
    EmitVertex();

    gl_Position = gl_in[1].gl_Position - scr * lineN;
    EmitVertex();

    gl_Position = gl_in[2].gl_Position + scr * lineN;
    EmitVertex();

    gl_Position = gl_in[2].gl_Position - scr * lineN;
    EmitVertex();

    gl_Position = gl_in[2].gl_Position + scr * n2;
    EmitVertex();

    gl_Position = gl_in[2].gl_Position - scr * n2;
    EmitVertex();

    EndPrimitive();
}