#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform PixelateConfig {
    float PixelsPerBlock;
};

out vec4 fragColor;

void main() {
    vec2 sizeRatio = OutSize / InSize;
    vec2 blockSize = vec2(PixelsPerBlock) / InSize;
    vec2 snapped = (floor(texCoord / blockSize) + 0.5) * blockSize;
    fragColor = texture(InSampler, snapped);
}