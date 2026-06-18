#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform LsdConfig {
    float HueShift;
    float SaturationBoost;
    float WaveScale;
};

out vec4 fragColor;

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 sizeRatio = OutSize / InSize;
    vec4 color = texture(InSampler, texCoord);

    vec3 hsv = rgb2hsv(color.rgb);
    float spatialShift = sin(texCoord.x * WaveScale) * 0.15 + cos(texCoord.y * WaveScale * 0.7) * 0.15;
    hsv.x = fract(hsv.x + HueShift + spatialShift);
    hsv.y = clamp(hsv.y * SaturationBoost, 0.0, 1.0);
    vec3 shifted = hsv2rgb(hsv);

    fragColor = vec4(shifted, 1.0);
}