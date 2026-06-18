#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

void main() {
    vec2 mirrored = vec2(1.0 - texCoord.x, texCoord.y);
    fragColor = texture(InSampler, mirrored);
}