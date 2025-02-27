#ifdef GL_ES
precision mediump float;
#endif

uniform vec3                iResolution;
uniform float               iGlobalTime;
uniform sampler2D           iChannel0;
varying vec2                texCoord;
uniform vec2                iMouse;


//************************************************************************************************//
//***** the following commented code do the same as below code. it just for shadertoy website ****//
//***** https://www.shadertoy.com/view/XdBSzW ****************************************************//
//************************************************************************************************//
//
//#ifdef GL_ES
//precision mediump float;
//#endif
//
//float normpdf(in float x, in float sigma)
//{
//	return 0.39894*exp(-0.5*x*x/(sigma*sigma))/sigma;
//}
//
//float blendScreen(float base, float blend) {
//	return 1.0-((1.0-base)*(1.0-blend));
//}
//
//vec3 blendScreen(vec3 base, vec3 blend) {
//	return vec3(blendScreen(base.r,blend.r),blendScreen(base.g,blend.g),blendScreen(base.b,blend.b));
//}
//
//vec3 blendScreen(vec3 base, vec3 blend, float opacity) {
//	return (blendScreen(base, blend) * opacity + blend * (1.0 - opacity));
//}
//const float bluramount  = 10.0;
//const float center      = 1.0;
//const float stepSize    = 0.0003;
//const float steps       = 5.0;
//
//const float minOffs     = (float(steps-1.0)) / -2.0;
//const float maxOffs     = (float(steps-1.0)) / +2.0;
//
//void mainImage( out vec4 fragColor, in vec2 fragCoord )
//{
//	vec3 c = texture2D(iChannel0, fragCoord.xy / iResolution.xy).rgb;
//    vec2 tcoord = fragCoord.xy / iResolution.xy;
//	//if (fragCoord.x < iMouse.x)
//	//{
//	//	fragColor = vec4(c, 1.0);
//	//} else {
//
//    float amount;
//    vec4 blurred;
//
//        //Work out how much to blur based on the mid point
//    amount = pow((tcoord.y * center) * 2.0 - 1.0, 2.0) * bluramount;
//
//        //This is the accumulation of color from the surrounding pixels in the texture
//    blurred = vec4(0.0, 0.0, 0.0, 1.0);
//
//        //From minimum offset to maximum offset
//    for (float offsX = minOffs; offsX <= maxOffs; ++offsX) {
//        for (float offsY = minOffs; offsY <= maxOffs; ++offsY) {
//
//                //copy the coord so we can mess with it
//            vec2 temp_tcoord = tcoord.xy;
//
//                //work out which uv we want to sample now
//            temp_tcoord.x += offsX * amount * stepSize;
//            temp_tcoord.y += offsY * amount * stepSize;
//
//                //accumulate the sample
//            blurred += texture2D(iChannel0, temp_tcoord);
//
//        } //for y
//    } //for x
//
//        //because we are doing an average, we divide by the amount (x AND y, hence steps * steps)
//    blurred /= float(steps * steps);
//
//		fragColor = blurred;
//	//}
//}
//
//************************************************************************************************//

float normpdf(in float x, in float sigma)
{
	return 0.39894*exp(-0.5*x*x/(sigma*sigma))/sigma;
}

float blendScreen(float base, float blend) {
	return 1.0-((1.0-base)*(1.0-blend));
}

vec3 blendScreen(vec3 base, vec3 blend) {
	return vec3(blendScreen(base.r,blend.r),blendScreen(base.g,blend.g),blendScreen(base.b,blend.b));
}

vec3 blendScreen(vec3 base, vec3 blend, float opacity) {
	return (blendScreen(base, blend) * opacity + blend * (1.0 - opacity));
}

const float bluramount  = 10.0;
const float center      = 1.0;
const float stepSize    = 0.0003;
const float steps       = 5.0;

const float minOffs     = (float(steps-1.0)) / -2.0;
const float maxOffs     = (float(steps-1.0)) / +2.0;

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
 vec3 c = texture2D(iChannel0, gl_FragCoord.xy / iResolution.xy).rgb;
    vec2 tcoord = gl_FragCoord.xy / iResolution.xy;

    float amountY;
    float amountX;
    vec4 blurred;

    //Work out how much to blur based on the mid point
    amountY = pow((tcoord.y * center) * 2.0 - 1.0, 2.0) * bluramount;
    amountX = pow((tcoord.x * center) * 2.0 - 1.0, 2.0) * bluramount;

    //This is the accumulation of color from the surrounding pixels in the texture
    blurred = vec4(0.0, 0.0, 0.0, 1.0);

    //From minimum offset to maximum offset
    for (float offsX = minOffs; offsX <= maxOffs; ++offsX) {
        for (float offsY = minOffs; offsY <= maxOffs; ++offsY) {

            //copy the coord so we can mess with it
            vec2 temp_tcoord = tcoord.xy;

            //work out which uv we want to sample now
            temp_tcoord.x += (offsX * amountY * stepSize)+(offsX * amountX * stepSize);
            temp_tcoord.y += (offsY * amountY * stepSize)+(offsY * amountX * stepSize);

            //accumulate the sample
            blurred += texture2D(iChannel0, temp_tcoord);

        } //for y
    } //for x

    //because we are doing an average, we divide by the amount (x AND y, hence steps * steps)
    blurred /= float(steps * steps);
    fragColor = blurred;
}

void main() {
	mainImage(gl_FragColor, texCoord);
}