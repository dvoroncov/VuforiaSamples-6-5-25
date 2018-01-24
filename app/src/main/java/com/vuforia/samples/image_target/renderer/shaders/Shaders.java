package com.vuforia.samples.image_target.renderer.shaders;

public class Shaders {

    public static final String CUBE_MESH_VERTEX_SHADER = " \n" + "\n"
            + "attribute vec3 vertexPosition; \n"
            + "attribute vec3 vertexNormal; \n"
            + "attribute vec2 vertexTexCoord; \n" + "\n"

            + "varying vec2 texCoord; \n"
            + "varying vec3 normal; \n"
            + "varying vec3 vertex; \n"

            + "uniform vec3 vertexPositionOffset; \n"
            + "uniform float vertexPositionAngle; \n"
            + "uniform mat4 modelViewProjectionMatrix; \n" + "\n"

            + "mat4 rotationMatrix(vec3 axis, float angle) { \n"
            + "    axis = normalize(axis); \n"
            + "    float s = sin(angle); \n"
            + "    float c = cos(angle); \n"
            + "    float oc = 1.0 - c; \n" + "\n"

            + "    return mat4(oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0.0, \n"
            + "            oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0.0, \n"
            + "            oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0.0, \n"
            + "            0.0,                                0.0,                                0.0,                                1.0); \n"
            + "}" + "\n"

            + "vec3 rotate(vec3 v, vec3 axis, float angle) { \n"
            + "    mat4 m = rotationMatrix(axis, angle); \n"
            + "    return (m * vec4(v, 1.0)).xyz; \n"
            + "} \n" + "\n"

            + "void main() { \n"
            + "    vertex = vertexPosition; \n"
            + "    texCoord = vertexTexCoord; \n"
            + "    vec3 n_normal = rotate(vertexNormal, vec3(0.0, 0.0, 1.0), radians(vertexPositionAngle)); \n"
            + "    n_normal = normalize(n_normal); \n"
            + "    normal = n_normal; \n" + "\n"

            + "    vec3 n_vertexPosition = rotate(vertexPosition, vec3(0.0, 0.0, 1.0), radians(vertexPositionAngle)); \n" + "\n"

            + "    n_vertexPosition.x = n_vertexPosition.x + vertexPositionOffset.x; \n"
            + "    n_vertexPosition.y = n_vertexPosition.y + vertexPositionOffset.y; \n"
            + "    n_vertexPosition.z = n_vertexPosition.z + vertexPositionOffset.z; \n" + "\n"

            + "    gl_Position = modelViewProjectionMatrix * vec4(n_vertexPosition, 1.0); \n"
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
            + "    vec3 lookvector = normalize(cameraPosition - vertex); \n" + "\n"

            + "    float ambient = 0.0; \n"
            + "    float k_diffuse = 0.64; \n"
            + "    float k_specular = 0.8; \n"
            + "    vec4 one = vec4(1.0,1.0,1.0,1.0); \n" + "\n"

            + "    float diffuse = k_diffuse * max(dot(n_normal, lightvector), 0.0); \n"
            + "    vec3 reflectvector = reflect(-lightvector, n_normal); \n"
            + "    float specular = k_specular * pow(max(dot(lookvector,reflectvector),0.0), 1.0); \n"
            + "    vec4 lightColor = (ambient + diffuse + specular) * one; \n" + "\n"

            + "    gl_FragColor = mix(lightColor, n_color, 0.5); \n"
            + "} \n";
}
