package com.vuforia.samples.image_target.renderer.shaders;

public class Shaders {

    public static final String CUBE_MESH_VERTEX_SHADER = " \n" + "\n"
            + "attribute vec3 vertexPosition; \n"
            + "attribute vec3 vertexNormal; \n"
            + "attribute vec2 vertexTexCoord; \n" + "\n"
            + "varying vec2 texCoord; \n" + "\n"
            + "varying vec3 normal; \n"
            + "varying vec3 vertex; \n"
            + "uniform mat4 modelViewProjectionMatrix; \n" + "\n"
            + "void main() { \n"
            + "    vertex = vertexPosition; \n"
            + "    texCoord = vertexTexCoord; \n"
            + "    vec3 n_normal = normalize(vertexNormal); \n"
            + "    normal = n_normal; \n"
            + "    gl_Position = modelViewProjectionMatrix * vec4(vertexPosition, 1.0); \n"
            + "} \n";

    public static final String CUBE_MESH_FRAGMENT_SHADER = " \n" + "\n"
            + "precision mediump float; \n" + " \n"
            + "varying vec2 texCoord; \n"
            + "varying vec3 normal; \n"
            + "varying vec3 vertex; \n" + " \n"
            + "uniform vec3 cameraPosition; \n"
            + "uniform vec3 lightPosition; \n"
            + "uniform sampler2D texSampler2D; \n" + " \n"
            + "void main() {\n"
            + "    vec3 n_normal = normalize(normal); \n"
            + "    vec4 n_color = texture2D(texSampler2D, texCoord); \n"
            + "    vec3 lightvector = normalize(lightPosition - vertex); \n"
            + "    vec3 lookvector = normalize(cameraPosition - vertex); \n"
            + "    float ambient = 0.0; \n"
            + "    float k_diffuse = 0.8; \n"
            + "    float k_specular = 0.4; \n"
            + "    float diffuse = k_diffuse * max(dot(n_normal, lightvector), 0.0); \n"
            + "    vec3 reflectvector = reflect(-lightvector, n_normal); \n"
            + "    float specular = k_specular * pow(max(dot(lookvector,reflectvector),0.0), 1.0); \n"
            + "    vec4 one = vec4(1.0,1.0,1.0,1.0); \n"
            + "    vec4 lightColor = (ambient + diffuse + specular) * one; \n"
            + "    gl_FragColor = mix(lightColor, n_color, 0.5); \n"
            + "} \n";
}
