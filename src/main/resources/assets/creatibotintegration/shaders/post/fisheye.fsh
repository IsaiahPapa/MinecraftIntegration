#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform FisheyeConfig {
    float Strength;
};

out vec4 fragColor;

void main() {
    vec2 center = texCoord - 0.5;
    float r = length(center);
    float distort = 1.0 / (1.0 + Strength * r);
    vec2 tc = 0.5 + center * distort;
    fragColor = texture(InSampler, clamp(tc, 0.0, 1.0));
}