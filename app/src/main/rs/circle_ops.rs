#pragma version(1)
#pragma rs java_package_name(seaskyways.seaskyways.rxcanvas.circle)
#define RS_KERNEL __attribute__((kernel))

float x_in1;
float y_in1;
float radius_in1;
float stroke_width1;
float x_in2;
float y_in2;
float radius_in2;
float stroke_width2;
char result;

void find_intersection(){

    float distanceX = x_in1 - x_in2;
    float distanceY = y_in1 - y_in2;
    float distanceX2 = distanceX * distanceX;
    float distanceY2 = distanceY * distanceY;
    float radii = (radius_in1 + stroke_width1 / 4) +  (radius_in2 + stroke_width2 / 4);
    float radiiS = radii * radii;

    result = (distanceX2 + distanceY2) <= radiiS;
}

int RS_KERNEL find_intersection_new(){

    float distanceX = x_in1 - x_in2;
    float distanceY = y_in1 - y_in2;
    float distanceX2 = distanceX * distanceX;
    float distanceY2 = distanceY * distanceY;
    float radii = (radius_in1 + stroke_width1 / 4) +  (radius_in2 + stroke_width2 / 4);
    float radiiS = radii * radii;
    return (distanceX2 + distanceY2) <= radiiS;
}

int root(void){
    return 1;
}