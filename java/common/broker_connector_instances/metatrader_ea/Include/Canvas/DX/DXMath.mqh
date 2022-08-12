//+------------------------------------------------------------------+
//|                                                       DXMath.mqh |
//|                        Copyright 2019,MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2019,MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
//+------------------------------------------------------------------+
//| DirectX Math Routines                                            |
//+------------------------------------------------------------------+
//| Ported from C++ code of ReactOS, written by David Adam           |
//| and Tony Wasserka                                                |
//|                                                                  |
//| https://doxygen.reactos.org/de/d57/                              |
//| dll_2directx_2wine_2d3dx9__36_2math_8c_source.html               |
//|                                                                  |
//| Copyright (C) 2007 David Adam                                    |
//| Copyright (C) 2007 Tony Wasserka                                 |
//+------------------------------------------------------------------+
#define DX_PI           3.1415926535897932384626f
#define DX_PI_DIV2      1.5707963267948966192313f
#define DX_PI_DIV3      1.0471975511965977461542f
#define DX_PI_DIV4      0.7853981633974483096156f
#define DX_PI_DIV6      0.5235987755982988730771f
#define DX_PI_MUL2      6.2831853071795864769253f
#define DXSH_MINORDER   2
#define DXSH_MAXORDER   6
//+------------------------------------------------------------------+
//| Preliminary declarations                                         |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| DXColor                                                          |
//+------------------------------------------------------------------+
struct DXColor;
struct DXPlane;
struct DXVector2;
struct DXVector3;
struct DXVector4;
struct DXMatrix;
struct DXQuaternion;
struct DViewport;
//+------------------------------------------------------------------+
//| DXColor                                                          |
//+------------------------------------------------------------------+
struct DXColor
  {
   float             r;
   float             g;
   float             b;
   float             a;
   //--- constructors
                     DXColor(void)                                            { r=0.0; g=0.0;   b=0.0;  a=1.0;   }
                     DXColor(float red,float green,float blue, float alpha)   { r=red; g=green; b=blue; a=alpha; }
                     DXColor(const DXVector4 &v)                              { r=v.x; g=v.y;   b=v.z;  a=v.w;   }
                     DXColor(const DXVector3 &v)                              { r=v.x; g=v.y;   b=v.z;  a=1.0;   }
  };
//+------------------------------------------------------------------+
//| DXPlane                                                          |
//+------------------------------------------------------------------+
struct DXPlane
  {
   float             a;
   float             b;
   float             c;
   float             d;
  };
//+------------------------------------------------------------------+
//| DXVector2                                                        |
//+------------------------------------------------------------------+
struct DXVector2
  {
   float             x;
   float             y;
   //--- constructors
                     DXVector2(void)                                          { x=0.0; y=0.0; }
                     DXVector2(float v)                                       { x=v;   y=v;   }
                     DXVector2(float vx,float vy)                             { x=vx;  y=vy;  }
                     DXVector2(const DXVector3 &v)                            { x=v.x; y=v.y; }
                     DXVector2(const DXVector4 &v)                            { x=v.x; y=v.y; }
  };
//+------------------------------------------------------------------+
//| DXVector3                                                        |
//+------------------------------------------------------------------+
struct DXVector3
  {
   float             x;
   float             y;
   float             z;
   //--- constructors
                     DXVector3(void)                                          { x=0.0; y=0.0; z=0.0; }
                     DXVector3(float v)                                       { x=v;   y=v;   z=v;   }
                     DXVector3(float vx,float vy,float vz)                    { x=vx;  y=vy;  z=vz;  }
                     DXVector3(const DXVector2 &v)                            { x=v.x; y=v.y; z=0.0; }
                     DXVector3(const DXVector4 &v)                            { x=v.x; y=v.y; z=v.z; }
  };
//+------------------------------------------------------------------+
//| DXVector4                                                        |
//+------------------------------------------------------------------+
struct DXVector4
  {
   float             x;
   float             y;
   float             z;
   float             w;
   //--- constructors
                     DXVector4(void)                                          { x=0.0; y=0.0; z=0.0; w=1.0; }
                     DXVector4(float v)                                       { x=v;   y=v;   z=v;   w=v;   }
                     DXVector4(float vx,float vy,float vz,float vw)           { x=vx;  y=vy;  z=vz;  w=vw;  }
                     DXVector4(const DXVector2 &v)                            { x=v.x; y=v.y; z=0.0; w=1.0; }
                     DXVector4(const DXVector3 &v)                            { x=v.x; y=v.y; z=v.z; w=1.0; }
  };
//+------------------------------------------------------------------+
//| DXMatrix                                                         |
//+------------------------------------------------------------------+
struct DXMatrix
  {
   float             m[4][4];
  };
//+------------------------------------------------------------------+
//| DXQuaternion                                                     |
//+------------------------------------------------------------------+
struct DXQuaternion
  {
   float             x;
   float             y;
   float             z;
   float             w;
  };
//+------------------------------------------------------------------+
//| DViewport                                                        |
//+------------------------------------------------------------------+
struct DViewport
  {
   ulong             x;
   ulong             y;
   ulong             width;
   ulong             height;
   float             minz;
   float             maxz;
  };

/*
//--- DXColor functions
void  DXColorAdd(DXColor &pout,const DXColor &pc1,const DXColor &pc2);
void  DXColorAdjustContrast(DXColor &pout,const DXColor &pc,float s);
void  DXColorAdjustSaturation(DXColor &pout,const DXColor &pc,float s);
void  DXColorLerp(DXColor &pout,const DXColor &pc1,const DXColor &pc2,float s);
void  DXColorModulate(DXColor &pout,const DXColor &pc1,const DXColor &pc2);
void  DXColorNegative(DXColor &pout,const DXColor &pc);
void  DXColorScale(DXColor &pout,const DXColor &pc,float s);
void  DXColorSubtract(DXColor &pout,const DXColor &pc1,const DXColor &pc2);
float DXFresnelTerm(float costheta,float refractionindex);

//--- DXVector2 functions
void  DXVec2Add(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2);
void  DXVec2BaryCentric(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2,const DXVector2 &pv3,float f,float g);
void  DXVec2CatmullRom(DXVector2 &pout,const DXVector2 &pv0,const DXVector2 &pv1,const DXVector2 &pv2,const DXVector2 &pv3,float s);
float DXVec2CCW(const DXVector2 &pv1,const DXVector2 &pv2);
float DXVec2Dot(const DXVector2 &pv1,const DXVector2 &pv2);
void  DXVec2Hermite(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pt1,const DXVector2 &pv2,const DXVector2 &pt2,float s);
float DXVec2Length(const DXVector2 &v);
float DXVec2LengthSq(const DXVector2 &v);
void  DXVec2Lerp(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2,float s);
void  DXVec2Maximize(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2);
void  DXVec2Minimize(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2);
void  DXVec2Normalize(DXVector2 &pout,const DXVector2 &pv);
void  DXVec2Scale(DXVector2 &pout,const DXVector2 &pv,float s);
void  DXVec2Subtract(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2);
void  DXVec2Transform(DXVector4 &pout,const DXVector2 &pv,const DXMatrix &pm);
void  DXVec2TransformCoord(DXVector2 &pout,const DXVector2 &pv,const DXMatrix &pm);
void  DXVec2TransformNormal(DXVector2 &pout,const DXVector2 &pv,const DXMatrix &pm);

//--- DXVector3 functions
void  DXVec3Add(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2);
void  DXVec3BaryCentric(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2,const DXVector3 &pv3,float f,float g);
void  DXVec3CatmullRom(DXVector3 &pout,const DXVector3 &pv0,const DXVector3 &pv1,const DXVector3 &pv2,const DXVector3 &pv3,float s);
void  DXVec3Cross(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2);
float DXVec3Dot(const DXVector3 &pv1,const DXVector3 &pv2);
void  DXVec3Hermite(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pt1,const DXVector3 &pv2,const DXVector3 &pt2,float s);
float DXVec3Length(const DXVector3 &pv);
float DXVec3LengthSq(const DXVector3 &pv);
void  DXVec3Lerp(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2,float s);
void  DXVec3Maximize(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2);
void  DXVec3Minimize(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2);
void  DXVec3Normalize(DXVector3 &pout,const DXVector3 &pv);
void  DXVec3Project(DXVector3 &pout,const DXVector3 &pv,const DViewport &pviewport,const DXMatrix &pprojection,const DXMatrix &pview,const DXMatrix &pworld);
void  DXVec3Scale(DXVector3 &pout,const DXVector3 &pv,float s);
void  DXVec3Subtract(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2);
void  DXVec3Transform(DXVector4 &pout,const DXVector3 &pv,const DXMatrix &pm);
void  DXVec3TransformCoord(DXVector3 &pout,const DXVector3 &pv,const DXMatrix &pm);
void  DXVec3TransformNormal(DXVector3 &pout,const DXVector3 &pv,const DXMatrix &pm);
void  DXVec3Unproject(DXVector3 &out,const DXVector3 &v,const DViewport &viewport,const DXMatrix &projection,const DXMatrix &view,const DXMatrix &world);

//--- DXVector4 vector functions
void  DXVec4Add(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2);
void  DXVec4BaryCentric(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2,const DXVector4 &pv3,float f,float g);
void  DXVec4CatmullRom(DXVector4 &pout,const DXVector4 &pv0,const DXVector4 &pv1,const DXVector4 &pv2,const DXVector4 &pv3,float s);
void  DXVec4Cross(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2,const DXVector4 &pv3);
float DXVec4Dot(const DXVector4 &pv1,const DXVector4 &pv2);
void  DXVec4Hermite(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pt1,const DXVector4 &pv2,const DXVector4 &pt2,float s);
float DXVec4Length(const DXVector4 &pv);
float DXVec4LengthSq(const DXVector4 &pv);
void  DXVec4Lerp(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2,float s);
void  DXVec4Maximize(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2);
void  DXVec4Minimize(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2);
void  DXVec4Normalize(DXVector4 &pout,const DXVector4 &pv);
void  DXVec4Scale(DXVector4 &pout,const DXVector4 &pv,float s);
void  DXVec4Subtract(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2);
void  DXVec4Transform(DXVector4 &pout,const DXVector4 &pv,const DXMatrix &pm);

//---DXQuaternion functions
void  DXQuaternionBaryCentric(DXQuaternion &pout,DXQuaternion &pq1,DXQuaternion &pq2,DXQuaternion &pq3,float f,float g);
void  DXQuaternionConjugate(DXQuaternion &pout,const DXQuaternion &pq);
float DXQuaternionDot(DXQuaternion &a,DXQuaternion &b);
void  DXQuaternionExp(DXQuaternion &out,const DXQuaternion &q);
void  DXQuaternionIdentity(DXQuaternion &out);
bool  DXQuaternionIsIdentity(DXQuaternion &pq);
float DXQuaternionLength(const DXQuaternion &pq);
float DXQuaternionLengthSq(const DXQuaternion &pq);
void  DXQuaternionInverse(DXQuaternion &pout,const DXQuaternion &pq);
void  DXQuaternionLn(DXQuaternion &out,const DXQuaternion &q);
void  DXQuaternionMultiply(DXQuaternion &pout,const DXQuaternion &pq1,const DXQuaternion &pq2);
void  DXQuaternionNormalize(DXQuaternion &out,const DXQuaternion &q);
void  DXQuaternionRotationAxis(DXQuaternion &out,const DXVector3 &v,float angle);
void  DXQuaternionRotationMatrix(DXQuaternion &out,const DXMatrix &m);
void  DXQuaternionRotationYawPitchRoll(DXQuaternion &out,float yaw,float pitch,float roll);
void  DXQuaternionSlerp(DXQuaternion &out,DXQuaternion &q1,DXQuaternion &q2,float t);
void  DXQuaternionSquad(DXQuaternion &pout,DXQuaternion &pq1,DXQuaternion &pq2,DXQuaternion &pq3,DXQuaternion &pq4,float t);
void  DXQuaternionSquadSetup(DXQuaternion &paout,DXQuaternion &pbout,DXQuaternion &pcout,DXQuaternion &pq0,DXQuaternion &pq1,DXQuaternion &pq2,DXQuaternion &pq3);
void  DXQuaternionToAxisAngle(const DXQuaternion &pq,DXVector3 &paxis,float &pangle);
DXQuaternion add_diff(const DXQuaternion &q1,const DXQuaternion &q2,const float add);

//--- DXMatrix functions
void  DXMatrixIdentity(DXMatrix &out);
bool  DXMatrixIsIdentity(DXMatrix &pm);
void  DXMatrixAffineTransformation(DXMatrix &out,float scaling,const DXVector3 &rotationcenter,const DXQuaternion &rotation,const DXVector3 &translation);
void  DXMatrixAffineTransformation2D(DXMatrix &out,float scaling,const DXVector2 &rotationcenter,float rotation,const DXVector2 &translation);
int   DXMatrixDecompose(DXVector3 &poutscale,DXQuaternion &poutrotation,DXVector3 &pouttranslation,const DXMatrix &pm);
float DXMatrixDeterminant(const DXMatrix &pm);
void  DXMatrixInverse(DXMatrix &pout,float &pdeterminant,const DXMatrix &pm);
void  DXMatrixLookAtLH(DXMatrix &out,const DXVector3 &eye,const DXVector3 &at,const DXVector3 &up);
void  DXMatrixLookAtRH(DXMatrix &out,const DXVector3 &eye,const DXVector3 &at,const DXVector3 &up);
void  DXMatrixMultiply(DXMatrix &pout,const DXMatrix &pm1,const DXMatrix &pm2);
void  DXMatrixMultiplyTranspose(DXMatrix &pout,const DXMatrix &pm1,const DXMatrix &pm2);
void  DXMatrixOrthoLH(DXMatrix &pout,float w,float h,float zn,float zf);
void  DXMatrixOrthoOffCenterLH(DXMatrix &pout,float l,float r,float b,float t,float zn,float zf);
void  DXMatrixOrthoOffCenterRH(DXMatrix &pout,float l,float r,float b,float t,float zn,float zf);
void  DXMatrixOrthoRH(DXMatrix &pout,float w,float h,float zn,float zf);
void  DXMatrixPerspectiveFovLH(DXMatrix &pout,float fovy,float aspect,float zn,float zf);
void  DXMatrixPerspectiveFovRH(DXMatrix &pout,float fovy,float aspect,float zn,float zf);
void  DXMatrixPerspectiveLH(DXMatrix &pout,float w,float h,float zn,float zf);
void  DXMatrixPerspectiveOffCenterLH(DXMatrix &pout,float l,float r,float b,float t,float zn,float zf);
void  DXMatrixPerspectiveOffCenterRH(DXMatrix &pout,float l,float r,float b,float t,float zn,float zf);
void  DXMatrixPerspectiveRH(DXMatrix &pout,float w,float h,float zn,float zf);
void  DXMatrixReflect(DXMatrix &pout,const DXPlane &pplane);
void  DXMatrixRotationAxis(DXMatrix &out,const DXVector3 &v,float angle);
void  DXMatrixRotationQuaternion(DXMatrix &pout,const DXQuaternion &pq);
void  DXMatrixRotationX(DXMatrix &pout,float angle);
void  DXMatrixRotationY(DXMatrix &pout,float angle);
void  DXMatrixRotationYawPitchRoll(DXMatrix &out,float yaw,float pitch,float roll);
void  DXMatrixRotationZ(DXMatrix &pout,float angle);
void  DXMatrixScaling(DXMatrix &pout,float sx,float sy,float sz);
void  DXMatrixShadow(DXMatrix &pout,const DXVector4 &plight,const DXPlane &pplane);
void  DXMatrixTransformation(DXMatrix &pout,const DXVector3 &pscalingcenter,const DXQuaternion &pscalingrotation,const DXVector3 &pscaling,const DXVector3 &protationcenter,const DXQuaternion &protation,const DXVector3 &ptranslation);
void  DXMatrixTransformation2D(DXMatrix &pout,const DXVector2 &pscalingcenter,float scalingrotation,const DXVector2 &pscaling,const DXVector2 &protationcenter,float rotation,const DXVector2 &ptranslation);
void  DXMatrixTranslation(DXMatrix &pout,float x,float y,float z);
void  DXMatrixTranspose(DXMatrix &pout,const DXMatrix &pm);

//--- DXPlane functions                                              |
float DXPlaneDot(const DXPlane &p1,const DXVector4 &p2);
float DXPlaneDotCoord(const DXPlane &pp,const DXVector4 &pv);
float DXPlaneDotNormal(const DXPlane &pp,const DXVector4 &pv);
void  DXPlaneFromPointNormal(DXPlane &pout,const DXVector3 &pvpoint,const DXVector3 &pvnormal);
void  DXPlaneFromPoints(DXPlane &pout,const DXVector3 &pv1,const DXVector3 &pv2,const DXVector3 &pv3);
void  DXPlaneIntersectLine(DXVector3 &pout,const DXPlane &pp,const DXVector3 &pv1,const DXVector3 &pv2);
void  DXPlaneNormalize(DXPlane &out,const DXPlane &p);
void  DXPlaneScale(DXPlane &pout,const DXPlane &p,float s);
void  DXPlaneTransform(DXPlane &pout,const DXPlane &pplane,const DXMatrix &pm);

//---- spherical harmonic functions
void  DXSHAdd(float &out[],int order,const float &a[],const float &b[]);
float DXSHDot(int order,const float &a[],const float &b[]);
int   DXSHEvalConeLight(int order,const DXVector3 &dir,float radius,float Rintensity,float Gintensity,float Bintensity,float &rout[],float &gout[],float &bout[]);
void  DXSHEvalDirection(float &out[],int order,const DXVector3 &dir);
int   DXSHEvalDirectionalLight(int order,const DXVector3 &dir,float Rintensity,float Gintensity,float Bintensity,float &rout[],float &gout[],float &bout[]);
int   DXSHEvalHemisphereLight(int order,const DXVector3 &dir,DXColor &top,DXColor &bottom,float &rout[],float &gout[],float &bout[]);
int   DXSHEvalSphericalLight(int order,const DXVector3 &dir,float radius,float Rintensity,float Gintensity,float Bintensity,float &rout[],float &gout[],float &bout[]);
void  DXSHMultiply2(float &out[],const float &a[],const float &b[]);
void  DXSHMultiply3(float &out[],const float &a[],const float &b[]);
void  DXSHMultiply4(float &out[],const float &a[],const float &b[]);
void  DXSHRotate(float &out[],int order,const DXMatrix &matrix,const float &in[]);
void  DXSHRotateZ(float &out[],int order,float angle,const float &in[]);
void  DXSHScale(float &out[],int order,const float &a[],const float scale);

//--- scalar functions
float DXScalarLerp(const float val1,const float val2,float s)
float DXScalarBiasScale(const float val,const float bias,const float scale)
*/

//+------------------------------------------------------------------+
//| Adds two color values together to create a new color value.      |
//+------------------------------------------------------------------+
void DXColorAdd(DXColor &pout,const DXColor &pc1,const DXColor &pc2)
  {
   pout.r = pc1.r + pc2.r;
   pout.g = pc1.g + pc2.g;
   pout.b = pc1.b + pc2.b;
   pout.a = pc1.a + pc2.a;
  }
//+------------------------------------------------------------------+
//| Adjusts the contrast value of a color.                           |
//+------------------------------------------------------------------+
//| The input alpha channel is copied, unmodified,                   |
//| to the output alpha channel.                                     |
//|                                                                  |
//| This function interpolates the red,green,and blue color          |
//| components of a DXColor structure between fifty percent gray     |
//| and a specified contrast value,as shown in the following example.|
//|                                                                  |
//| pout.r = 0.5f + s*(pc.r - 0.5f);                                 |
//|                                                                  |
//| If s is greater than 0 and less than 1,the contrast is decreased.|
//| If s is greater than 1, the contrast is increased.               |
//+------------------------------------------------------------------+
void DXColorAdjustContrast(DXColor &pout,const DXColor &pc,float s)
  {
   pout.r = 0.5f + s*(pc.r - 0.5f);
   pout.g = 0.5f + s*(pc.g - 0.5f);
   pout.b = 0.5f + s*(pc.b - 0.5f);
   pout.a = pc.a;
  }
//+------------------------------------------------------------------+
//| Adjusts the saturation value of a color.                         |
//+------------------------------------------------------------------+
//| The input alpha channel is copied, unmodified,                   |
//| to the output alpha channel.                                     |
//|                                                                  |
//| This function interpolates the red, green, and blue color        |
//| components of a DXColor structure between an unsaturated color   |
//| and a color, as shown in the following example.                  |
//|                                                                  |
//| Approximate values for each component's contribution to          |
//| luminance. Based upon the NTSC standard described in             |
//| ITU-R Recommendation BT.709.                                     |
//| float grey = pc.r*0.2125f + pc.g*0.7154f + pc.b*0.0721f;         |
//|                                                                  |
//| pout.r = grey + s*(pc.r - grey);                                 |
//| If s is greater than 0 and less than 1, the saturation is        |
//| decreased. If s is greater than 1, the saturation is increased.  |
//|                                                                  |
//| The grayscale color is computed as:                              |
//| r = g = b = 0.2125*r + 0.7154*g + 0.0721*b                       |
//+------------------------------------------------------------------+
void DXColorAdjustSaturation(DXColor &pout,const DXColor &pc,float s)
  {
   float grey = pc.r*0.2125f + pc.g*0.7154f + pc.b*0.0721f;
   pout.r = grey + s*(pc.r - grey);
   pout.g = grey + s*(pc.g - grey);
   pout.b = grey + s*(pc.b - grey);
   pout.a = pc.a;
  }
//+------------------------------------------------------------------+
//| Uses linear interpolation to create a color value.               |
//+------------------------------------------------------------------+
//| This function interpolates the red, green, blue, and alpha       |
//| components of a DXColor structure between two colors, as shown   |
//| in the following example.                                        |
//|                                                                  |
//| pout.r = pC1.r + s * (pC2.r - pC1.r);                            |
//|                                                                  |
//| If you are linearly interpolating between the colors A and B,    |
//| and s is 0, the resulting color is A.                            |
//| If s is 1, the resulting color is color B.                       |
//+------------------------------------------------------------------+
void DXColorLerp(DXColor &pout,const DXColor &pc1,const DXColor &pc2,float s)
  {
   pout.r = (1-s)*pc1.r + s*pc2.r;
   pout.g = (1-s)*pc1.g + s*pc2.g;
   pout.b = (1-s)*pc1.b + s*pc2.b;
   pout.a = (1-s)*pc1.a + s*pc2.a;
  }
//+------------------------------------------------------------------+
//| Blends two colors.                                               |
//+------------------------------------------------------------------+
//| This function blends together two colors by multiplying matching |
//| color components, as shown in the following example.             |
//| pout.r = pC1.r * pC2.r;                                          |
//+------------------------------------------------------------------+
void DXColorModulate(DXColor &pout,const DXColor &pc1,const DXColor &pc2)
  {
   pout.r = pc1.r*pc2.r;
   pout.g = pc1.g*pc2.g;
   pout.b = pc1.b*pc2.b;
   pout.a = pc1.a*pc2.a;
  }
//+------------------------------------------------------------------+
//| Creates the negative color value of a color value.               |
//+------------------------------------------------------------------+
//| The input alpha channel is copied, unmodified, to the output     |
//| alpha channel.                                                   |
//| This function returns the negative color value by subtracting 1.0|
//| from the color components of the DXColor structure,              |
//| as shown in the following example.                               |
//|  pout.r = 1.0f - pc.r;                                           |
//+------------------------------------------------------------------+
void DXColorNegative(DXColor &pout,const DXColor &pc)
  {
   pout.r = 1.0f - pc.r;
   pout.g = 1.0f - pc.g;
   pout.b = 1.0f - pc.b;
   pout.a = pc.a;
  }
//+------------------------------------------------------------------+
//| Scales a color value.                                            |
//+------------------------------------------------------------------+
//| This function computes the scaled color value by multiplying     |
//| the color components of the DXColor structure by the specified   |
//| scale factor, as shown in the following example.                 |
//| pOut.r = pC.r*s;                                                 |
//+------------------------------------------------------------------+
void DXColorScale(DXColor &pout,const DXColor &pc,float s)
  {
   pout.r = s*pc.r;
   pout.g = s*pc.g;
   pout.b = s*pc.b;
   pout.a = s*pc.a;
  }
//+------------------------------------------------------------------+
//| Subtracts two color values to create a new color value.          |
//+------------------------------------------------------------------+
void DXColorSubtract(DXColor &pout,const DXColor &pc1,const DXColor &pc2)
  {
   pout.r = pc1.r - pc2.r;
   pout.g = pc1.g - pc2.g;
   pout.b = pc1.b - pc2.b;
   pout.a = pc1.a - pc2.a;
  }
//+------------------------------------------------------------------+
//| Calculate the Fresnel term.                                      |
//+------------------------------------------------------------------+
//| To find the Fresnel term (F):                                    |
//| If A is angle of incidence and B is the angle of refraction,     |
//| then                                                             |
//| F = 0.5*[tan2(A - B)/tan2(A + B) + sin2(A - B)/sin2(A + B)]      |
//|   = 0.5*sin2(A - B)/sin2(A + B)*[cos2(A + B)/cos2(A - B) + 1]    |
//|                                                                  |
//|  Let r = sina(A)/sin(B)      (the relative refractive index)     |
//|  Let c = cos(A)                                                  |
//|  Let g = (r2 + c2 - 1)1/2                                        |
//|                                                                  |
//| Then,expanding using the trig identities and simplifying,        |
//| you get:                                                         |
//| F = 0.5*(g + c)2/(g - c)2*([c(g + c)-1]2/[c(g - c) + 1]2 + 1)    |
//+------------------------------------------------------------------+
float DXFresnelTerm(float costheta,float refractionindex)
  {
   float g = (float)sqrt(refractionindex*refractionindex + costheta*costheta - 1.0f);
   float a = g + costheta;
   float d = g - costheta;
   float result = (costheta*a - 1.0f)*(costheta*a - 1.0f)/((costheta*d + 1.0f)*(costheta*d + 1.0f)) + 1.0f;
   result *= 0.5f*d*d/(a*a);
//---
   return(result);
  }
//+------------------------------------------------------------------+
//| Adds two 2D vectors.                                             |
//+------------------------------------------------------------------+
void DXVec2Add(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2)
  {
   pout.x = pv1.x + pv2.x;
   pout.y = pv1.y + pv2.y;
  }
//+------------------------------------------------------------------+
//| Returns a point in Barycentric coordinates,                      |
//| using the specified 2D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec2BaryCentric(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2,const DXVector2 &pv3,float f,float g)
  {
   pout.x = (1.0f-f-g)*(pv1.x) + f*(pv2.x) + g*(pv3.x);
   pout.y = (1.0f-f-g)*(pv1.y) + f*(pv2.y) + g*(pv3.y);
  }
//+------------------------------------------------------------------+
//| Performs a Catmull-Rom interpolation,                            |
//| using the specified 2D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec2CatmullRom(DXVector2 &pout,const DXVector2 &pv0,const DXVector2 &pv1,const DXVector2 &pv2,const DXVector2 &pv3,float s)
  {
   pout.x = 0.5f*(2.0f*pv1.x + (pv2.x-pv0.x)*s + (2.0f*pv0.x - 5.0f*pv1.x + 4.0f*pv2.x - pv3.x)*s*s + (pv3.x - 3.0f*pv2.x + 3.0f*pv1.x - pv0.x)*s*s*s);
   pout.y = 0.5f*(2.0f*pv1.y + (pv2.y-pv0.y)*s + (2.0f*pv0.y - 5.0f*pv1.y + 4.0f*pv2.y - pv3.y)*s*s + (pv3.y - 3.0f*pv2.y + 3.0f*pv1.y - pv0.y)*s*s*s);
  }
//+------------------------------------------------------------------+
//| Returns the z-component by taking the cross product              |
//| of two 2D vectors.                                               |
//+------------------------------------------------------------------+
float DXVec2CCW(const DXVector2 &pv1,const DXVector2 &pv2)
  {
   return(pv1.x*pv2.y - pv1.y*pv2.x);
  }
//+------------------------------------------------------------------+
//| Determines the dot product of two 2D vectors.                    |
//+------------------------------------------------------------------+
float DXVec2Dot(const DXVector2 &pv1,const DXVector2 &pv2)
  {
   return(pv1.x*pv2.x + pv1.y*pv2.y);
  }
//+------------------------------------------------------------------+
//| Performs a Hermite spline interpolation,                         |
//| using the specified 2D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec2Hermite(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pt1,const DXVector2 &pv2,const DXVector2 &pt2,float s)
  {
//--- prepare coefficients
   float h1 = 2.0f*s*s*s - 3.0f*s*s + 1.0f;
   float h2 = s*s*s - 2.0f*s*s + s;
   float h3 = -2.0f*s*s*s + 3.0f*s*s;
   float h4 = s*s*s - s*s;
//--- calculate interpolated point
   pout.x = h1*pv1.x + h2*pt1.x + h3*pv2.x + h4*pt2.x;
   pout.y = h1*pv1.y + h2*pt1.y + h3*pv2.y + h4*pt2.y;
  }
//+------------------------------------------------------------------+
//| Returns the length of a 2D vector.                               |
//+------------------------------------------------------------------+
float DXVec2Length(const DXVector2 &v)
  {
   return((float)sqrt(v.x*v.x + v.y*v.y));
  }
//+------------------------------------------------------------------+
//| Returns the square of the length of a 2D vector.                 |
//+------------------------------------------------------------------+
float DXVec2LengthSq(const DXVector2 &v)
  {
   return((float)(v.x*v.x + v.y*v.y));
  }
//+------------------------------------------------------------------+
//| Performs a linear interpolation between two 2D vectors.          |
//+------------------------------------------------------------------+
void DXVec2Lerp(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2,float s)
  {
   pout.x = (1.0f-s)*pv1.x + s*pv2.x;
   pout.y = (1.0f-s)*pv1.y + s*pv2.y;
  }
//+------------------------------------------------------------------+
//| Returns a 2D vector that is made up of the largest components    |
//| of two 2D vectors.                                               |
//+------------------------------------------------------------------+
void DXVec2Maximize(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2)
  {
   pout.x = (float)fmax(pv1.x,pv2.x);
   pout.y = (float)fmax(pv1.y,pv2.y);
  }
//+------------------------------------------------------------------+
//| Returns a 2D vector that is made up of the smallest components   |
//| of two 2D vectors.                                               |
//+------------------------------------------------------------------+
void DXVec2Minimize(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2)
  {
   pout.x = (float)fmin(pv1.x,pv2.x);
   pout.y = (float)fmin(pv1.y,pv2.y);
  }
//+------------------------------------------------------------------+
//| Returns the normalized version of a 2D vector.                   |
//+------------------------------------------------------------------+
void DXVec2Normalize(DXVector2 &pout,const DXVector2 &pv)
  {
//--- calculate length
   float norm = DXVec2Length(pv);
   if(!norm)
     {
      pout.x = 0.0f;
      pout.y = 0.0f;
     }
   else
     {
      pout.x = pv.x/norm;
      pout.y = pv.y/norm;
     }
  }
//+------------------------------------------------------------------+
//| Scales a 2D vector.                                              |
//+------------------------------------------------------------------+
void DXVec2Scale(DXVector2 &pout,const DXVector2 &pv,float s)
  {
   pout.x = s*pv.x;
   pout.y = s*pv.y;
  }
//+------------------------------------------------------------------+
//| DXVec3Subtract                                                   |
//+------------------------------------------------------------------+
void DXVec2Subtract(DXVector2 &pout,const DXVector2 &pv1,const DXVector2 &pv2)
  {
   pout.x = pv1.x - pv2.x;
   pout.y = pv1.y - pv2.y;
  }
//+------------------------------------------------------------------+
//| Transforms a 2D vector by a given matrix.                        |
//| This function transforms the vector pv(x,y,0,1) by the matrix pm.|
//+------------------------------------------------------------------+
void DXVec2Transform(DXVector4 &pout,const DXVector2 &pv,const DXMatrix &pm)
  {
   DXVector4 out;
   out.x = pm.m[0][0]*pv.x + pm.m[1][0]*pv.y + pm.m[3][0];
   out.y = pm.m[0][1]*pv.x + pm.m[1][1]*pv.y + pm.m[3][1];
   out.z = pm.m[0][2]*pv.x + pm.m[1][2]*pv.y + pm.m[3][2];
   out.w = pm.m[0][3]*pv.x + pm.m[1][3]*pv.y + pm.m[3][3];
   pout = out;
  }
//+------------------------------------------------------------------+
//| Transforms a 2D vector by a given matrix,                        |
//| projecting the result back into w = 1.                           |
//| This function transforms the vector pv(x,y,0,1) by the matrix pm.|
//+------------------------------------------------------------------+
void DXVec2TransformCoord(DXVector2 &pout,const DXVector2 &pv,const DXMatrix &pm)
  {
   float norm = pm.m[0][3]*pv.x + pm.m[1][3]*pv.y + pm.m[3][3];
   if(norm)
     {
      pout.x = (pm.m[0][0]*pv.x + pm.m[1][0]*pv.y + pm.m[3][0])/norm;
      pout.y = (pm.m[0][1]*pv.x + pm.m[1][1]*pv.y + pm.m[3][1])/norm;
     }
   else
     {
      pout.x = 0.0f;
      pout.y = 0.0f;
     }
  }
//+------------------------------------------------------------------+
//| Transforms the 2D vector normal by the given matrix.             |
//+------------------------------------------------------------------+
void DXVec2TransformNormal(DXVector2 &pout,const DXVector2 &pv,const DXMatrix &pm)
  {
   pout.x = pm.m[0][0]*pv.x + pm.m[1][0]*pv.y;
   pout.y = pm.m[0][1]*pv.x + pm.m[1][1]*pv.y;
  }
//+------------------------------------------------------------------+
//| Adds two 3D vectors.                                             |
//+------------------------------------------------------------------+
void DXVec3Add(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2)
  {
   pout.x = pv1.x + pv2.x;
   pout.y = pv1.y + pv2.y;
   pout.z = pv1.z + pv2.z;
  }
//+------------------------------------------------------------------+
//| Returns a point in Barycentric coordinates,                      |
//| using the specified 3D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec3BaryCentric(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2,const DXVector3 &pv3,float f,float g)
  {
   pout.x = (1.0f-f-g)*pv1.x + f*pv2.x + g*pv3.x;
   pout.y = (1.0f-f-g)*pv1.y + f*pv2.y + g*pv3.y;
   pout.z = (1.0f-f-g)*pv1.z + f*pv2.z + g*pv3.z;
  }
//+------------------------------------------------------------------+
//| Performs a Catmull-Rom interpolation,                            |
//| using the specified 3D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec3CatmullRom(DXVector3 &pout,const DXVector3 &pv0,const DXVector3 &pv1,const DXVector3 &pv2,const DXVector3 &pv3,float s)
  {
   pout.x = 0.5f*(2.0f*pv1.x + (pv2.x - pv0.x)*s + (2.0f*pv0.x - 5.0f*pv1.x + 4.0f*pv2.x - pv3.x)*s*s + (pv3.x - 3.0f*pv2.x + 3.0f*pv1.x - pv0.x)*s*s*s);
   pout.y = 0.5f*(2.0f*pv1.y + (pv2.y - pv0.y)*s + (2.0f*pv0.y - 5.0f*pv1.y + 4.0f*pv2.y - pv3.y)*s*s + (pv3.y - 3.0f*pv2.y + 3.0f*pv1.y - pv0.y)*s*s*s);
   pout.z = 0.5f*(2.0f*pv1.z + (pv2.z - pv0.z)*s + (2.0f*pv0.z - 5.0f*pv1.z + 4.0f*pv2.z - pv3.z)*s*s + (pv3.z - 3.0f*pv2.z + 3.0f*pv1.z - pv0.z)*s*s*s);
  }
//+------------------------------------------------------------------+
//| Determines the cross-product of two 3D vectors.                  |
//+------------------------------------------------------------------+
void DXVec3Cross(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2)
  {
   pout.x = pv1.y*pv2.z - pv1.z*pv2.y;
   pout.y = pv1.z*pv2.x - pv1.x*pv2.z;
   pout.z = pv1.x*pv2.y - pv1.y*pv2.x;
  }
//+------------------------------------------------------------------+
//| Determines the dot product of two 3D vectors.                    |
//+------------------------------------------------------------------+
float DXVec3Dot(const DXVector3 &pv1,const DXVector3 &pv2)
  {
   return(pv1.x*pv2.x + pv1.y*pv2.y + pv1.z*pv2.z);
  }
//+------------------------------------------------------------------+
//| Performs a Hermite spline interpolation,                         |
//| using the specified 3D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec3Hermite(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pt1,const DXVector3 &pv2,const DXVector3 &pt2,float s)
  {
   float h1 = 2.0f*s*s*s - 3.0f*s*s + 1.0f;
   float h2 = s*s*s - 2.0f*s*s + s;
   float h3 = -2.0f*s*s*s + 3.0f*s*s;
   float h4 = s*s*s - s*s;
//--- calculate interpolated coordinates
   pout.x = h1*pv1.x + h2*pt1.x + h3*pv2.x + h4*pt2.x;
   pout.y = h1*pv1.y + h2*pt1.y + h3*pv2.y + h4*pt2.y;
   pout.z = h1*pv1.z + h2*pt1.z + h3*pv2.z + h4*pt2.z;
  }
//+------------------------------------------------------------------+
//| Returns the length of a 3D vector.                               |
//+------------------------------------------------------------------+
float DXVec3Length(const DXVector3 &pv)
  {
   return((float)sqrt(pv.x*pv.x + pv.y*pv.y + pv.z*pv.z));
  }
//+------------------------------------------------------------------+
//| Returns the square of the length of a 3D vector.                 |
//+------------------------------------------------------------------+
float DXVec3LengthSq(const DXVector3 &pv)
  {
   return((float)(pv.x*pv.x + pv.y*pv.y + pv.z*pv.z));
  }
//+------------------------------------------------------------------+
//| Performs a linear interpolation between two 3D vectors.          |
//+------------------------------------------------------------------+
void DXVec3Lerp(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2,float s)
  {
   pout.x = (1.0f-s)*pv1.x + s*pv2.x;
   pout.y = (1.0f-s)*pv1.y + s*pv2.y;
   pout.z = (1.0f-s)*pv1.z + s*pv2.z;
  }
//+------------------------------------------------------------------+
//| Returns a 3D vector that is made up of the largest components    |
//| of two 3D vectors.                                               |
//+------------------------------------------------------------------+
void DXVec3Maximize(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2)
  {
   pout.x = (float)fmax(pv1.x,pv2.x);
   pout.y = (float)fmax(pv1.y,pv2.y);
   pout.z = (float)fmax(pv1.z,pv2.z);
  }
//+------------------------------------------------------------------+
//| Returns a 3D vector that is made up of the smallest components   |
//| of two 3D vectors.                                               |
//+------------------------------------------------------------------+
void DXVec3Minimize(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2)
  {
   pout.x = (float)fmin(pv1.x,pv2.x);
   pout.y = (float)fmin(pv1.y,pv2.y);
   pout.z = (float)fmin(pv1.z,pv2.z);
  }
//+------------------------------------------------------------------+
//| Returns the normalized version of a 3D vector.                   |
//+------------------------------------------------------------------+
void DXVec3Normalize(DXVector3 &pout,const DXVector3 &pv)
  {
//--- calculate length
   float norm = DXVec3Length(pv);
   if(!norm)
     {
      pout.x = 0.0f;
      pout.y = 0.0f;
      pout.z = 0.0f;
     }
   else
     {
      pout.x = pv.x/norm;
      pout.y = pv.y/norm;
      pout.z = pv.z/norm;
     }
  }
//+------------------------------------------------------------------+
//| Projects a 3D vector from object space into screen space.        |
//+------------------------------------------------------------------+
void DXVec3Project(DXVector3 &pout,const DXVector3 &pv,const DViewport &pviewport,const DXMatrix &pprojection,const DXMatrix &pview,const DXMatrix &pworld)
  {
   DXMatrix m;
   DXMatrixIdentity(m);
//--- pworld
   DXMatrixMultiply(m,m,pworld);
//--- pview
   DXMatrixMultiply(m,m,pview);
//--- pprojection
   DXMatrixMultiply(m,m,pprojection);
   DXVec3TransformCoord(pout,pv,m);
//---pviewport
   pout.x = pviewport.x + (1.0f + pout.x)*pviewport.width/2.0f;
   pout.y = pviewport.y + (1.0f - pout.y)*pviewport.height/2.0f;
   pout.z = pviewport.minz + pout.z*(pviewport.maxz - pviewport.minz);
  }
//+------------------------------------------------------------------+
//| Scales a 3D vector.                                              |
//+------------------------------------------------------------------+
void DXVec3Scale(DXVector3 &pout,const DXVector3 &pv,float s)
  {
   pout.x = s*pv.x;
   pout.y = s*pv.y;
   pout.z = s*pv.z;
  }
//+------------------------------------------------------------------+
//| Subtracts two 3D vectors.                                        |
//+------------------------------------------------------------------+
void DXVec3Subtract(DXVector3 &pout,const DXVector3 &pv1,const DXVector3 &pv2)
  {
   pout.x = pv1.x - pv2.x;
   pout.y = pv1.y - pv2.y;
   pout.z = pv1.z - pv2.z;
  }
//+------------------------------------------------------------------+
//| Transforms vector (x,y,z,1) by a given matrix.                   |
//+------------------------------------------------------------------+
void DXVec3Transform(DXVector4 &pout,const DXVector3 &pv,const DXMatrix &pm)
  {
   DXVector4 out;
//---
   out.x = pm.m[0][0]*pv.x + pm.m[1][0]*pv.y + pm.m[2][0]*pv.z + pm.m[3][0];
   out.y = pm.m[0][1]*pv.x + pm.m[1][1]*pv.y + pm.m[2][1]*pv.z + pm.m[3][1];
   out.z = pm.m[0][2]*pv.x + pm.m[1][2]*pv.y + pm.m[2][2]*pv.z + pm.m[3][2];
   out.w = pm.m[0][3]*pv.x + pm.m[1][3]*pv.y + pm.m[2][3]*pv.z + pm.m[3][3];
   pout = out;
  }
//+------------------------------------------------------------------+
//| Transforms a 3D vector by a given matrix,                        |
//| projecting the result back into w = 1.                           |
//+------------------------------------------------------------------+
void DXVec3TransformCoord(DXVector3 &pout,const DXVector3 &pv,const DXMatrix &pm)
  {
   float norm = pm.m[0][3]*pv.x + pm.m[1][3]*pv.y + pm.m[2][3]*pv.z + pm.m[3][3];
//---
   if(norm)
     {
      pout.x = (pm.m[0][0]*pv.x + pm.m[1][0]*pv.y + pm.m[2][0]*pv.z + pm.m[3][0])/norm;
      pout.y = (pm.m[0][1]*pv.x + pm.m[1][1]*pv.y + pm.m[2][1]*pv.z + pm.m[3][1])/norm;
      pout.z = (pm.m[0][2]*pv.x + pm.m[1][2]*pv.y + pm.m[2][2]*pv.z + pm.m[3][2])/norm;
     }
   else
     {
      pout.x = 0.0f;
      pout.y = 0.0f;
      pout.z = 0.0f;
     }
  }
//+------------------------------------------------------------------+
//| Transforms the 3D vector normal by the given matrix.             |
//+------------------------------------------------------------------+
void DXVec3TransformNormal(DXVector3 &pout,const DXVector3 &pv,const DXMatrix &pm)
  {
   pout.x = pm.m[0][0]*pv.x + pm.m[1][0]*pv.y + pm.m[2][0]*pv.z;
   pout.y = pm.m[0][1]*pv.x + pm.m[1][1]*pv.y + pm.m[2][1]*pv.z;
   pout.z = pm.m[0][2]*pv.x + pm.m[1][2]*pv.y + pm.m[2][2]*pv.z;
  }
//+------------------------------------------------------------------+
//| Projects a vector from screen space into object space.           |
//+------------------------------------------------------------------+
void DXVec3Unproject(DXVector3 &out,const DXVector3 &v,const DViewport &viewport,const DXMatrix &projection,const DXMatrix &view,const DXMatrix &world)
  {
   DXMatrix m;
   DXMatrixIdentity(m);
//--- world
   DXMatrixMultiply(m,m,world);
//--- view
   DXMatrixMultiply(m,m,view);
//--- projection
   DXMatrixMultiply(m,m,projection);
//--- calculate inverse matrix
   float det=0.0f;
   DXMatrixInverse(m,det,m);
   out = v;
//--- viewport
   out.x = 2.0f*(out.x - viewport.x)/viewport.width - 1.0f;
   out.y = 1.0f - 2.0f*(out.y - viewport.y)/viewport.height;
   out.z = (out.z - viewport.minz)/(viewport.maxz - viewport.minz);
//---
   DXVec3TransformCoord(out,out,m);
  }
//+------------------------------------------------------------------+
//| Adds two 4D vectors.                                             |
//+------------------------------------------------------------------+
void DXVec4Add(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2)
  {
   pout.x = pv1.x + pv2.x;
   pout.y = pv1.y + pv2.y;
   pout.z = pv1.z + pv2.z;
   pout.w = pv1.w + pv2.w;
  }
//+------------------------------------------------------------------+
//| Returns a point in Barycentric coordinates,                      |
//| using the specified 4D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec4BaryCentric(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2,const DXVector4 &pv3,float f,float g)
  {
   pout.x = (1.0f-f-g)*(pv1.x) + f*(pv2.x) + g*(pv3.x);
   pout.y = (1.0f-f-g)*(pv1.y) + f*(pv2.y) + g*(pv3.y);
   pout.z = (1.0f-f-g)*(pv1.z) + f*(pv2.z) + g*(pv3.z);
   pout.w = (1.0f-f-g)*(pv1.w) + f*(pv2.w) + g*(pv3.w);
  }
//+------------------------------------------------------------------+
//| Performs a Catmull-Rom interpolation,                            |
//| using the specified 4D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec4CatmullRom(DXVector4 &pout,const DXVector4 &pv0,const DXVector4 &pv1,const DXVector4 &pv2,const DXVector4 &pv3,float s)
  {
   pout.x = 0.5f*(2.0f*pv1.x + (pv2.x - pv0.x)*s + (2.0f*pv0.x - 5.0f*pv1.x + 4.0f*pv2.x - pv3.x)*s*s + (pv3.x -3.0f*pv2.x + 3.0f*pv1.x - pv0.x)*s*s*s);
   pout.y = 0.5f*(2.0f*pv1.y + (pv2.y - pv0.y)*s + (2.0f*pv0.y - 5.0f*pv1.y + 4.0f*pv2.y - pv3.y)*s*s + (pv3.y -3.0f*pv2.y + 3.0f*pv1.y - pv0.y)*s*s*s);
   pout.z = 0.5f*(2.0f*pv1.z + (pv2.z - pv0.z)*s + (2.0f*pv0.z - 5.0f*pv1.z + 4.0f*pv2.z - pv3.z)*s*s + (pv3.z -3.0f*pv2.z + 3.0f*pv1.z - pv0.z)*s*s*s);
   pout.w = 0.5f*(2.0f*pv1.w + (pv2.w - pv0.w)*s + (2.0f*pv0.w - 5.0f*pv1.w + 4.0f*pv2.w - pv3.w)*s*s + (pv3.w -3.0f*pv2.w + 3.0f*pv1.w - pv0.w)*s*s*s);
  }
//+------------------------------------------------------------------+
//| Determines the cross-product in four dimensions.                 |
//+------------------------------------------------------------------+
void DXVec4Cross(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2,const DXVector4 &pv3)
  {
   DXVector4 out;
   out.x = pv1.y*(pv2.z*pv3.w - pv3.z*pv2.w) - pv1.z*(pv2.y*pv3.w - pv3.y*pv2.w) + pv1.w*(pv2.y*pv3.z - pv2.z*pv3.y);
   out.y = -(pv1.x*(pv2.z*pv3.w - pv3.z*pv2.w) - pv1.z*(pv2.x*pv3.w - pv3.x*pv2.w) + pv1.w*(pv2.x*pv3.z - pv3.x*pv2.z));
   out.z = pv1.x*(pv2.y*pv3.w - pv3.y*pv2.w) - pv1.y*(pv2.x*pv3.w - pv3.x*pv2.w) + pv1.w*(pv2.x*pv3.y - pv3.x*pv2.y);
   out.w = -(pv1.x*(pv2.y*pv3.z - pv3.y*pv2.z) - pv1.y*(pv2.x*pv3.z - pv3.x*pv2.z) + pv1.z*(pv2.x*pv3.y - pv3.x*pv2.y));
   pout = out;
  }
//+------------------------------------------------------------------+
//| Determines the dot product of two 4D vectors.                    |
//+------------------------------------------------------------------+
float DXVec4Dot(const DXVector4 &pv1,const DXVector4 &pv2)
  {
   return(pv1.x*pv2.x + pv1.y*pv2.y + pv1.z*pv2.z+ pv1.w*pv2.w);
  }
//+------------------------------------------------------------------+
//| Performs a Hermite spline interpolation,                         |
//| using the specified 4D vectors.                                  |
//+------------------------------------------------------------------+
void DXVec4Hermite(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pt1,const DXVector4 &pv2,const DXVector4 &pt2,float s)
  {
   float h1 = 2.0f*s*s*s - 3.0f*s*s + 1.0f;
   float h2 = s*s*s - 2.0f*s*s + s;
   float h3 = -2.0f*s*s*s + 3.0f*s*s;
   float h4 = s*s*s - s*s;
   pout.x = h1*pv1.x + h2*pt1.x + h3*pv2.x + h4*pt2.x;
   pout.y = h1*pv1.y + h2*pt1.y + h3*pv2.y + h4*pt2.y;
   pout.z = h1*pv1.z + h2*pt1.z + h3*pv2.z + h4*pt2.z;
   pout.w = h1*pv1.w + h2*pt1.w + h3*pv2.w + h4*pt2.w;
  }
//+------------------------------------------------------------------+
//| Returns the length of a 4D vector.                               |
//+------------------------------------------------------------------+
float DXVec4Length(const DXVector4 &pv)
  {
   return((float)sqrt(pv.x*pv.x + pv.y*pv.y + pv.z*pv.z+ pv.w*pv.w));
  }
//+------------------------------------------------------------------+
//| Returns the square of the length of a 4D vector.                 |
//+------------------------------------------------------------------+
float DXVec4LengthSq(const DXVector4 &pv)
  {
   return((float)(pv.x*pv.x + pv.y*pv.y + pv.z*pv.z));
  }
//+------------------------------------------------------------------+
//| Performs a linear interpolation between two 4D vectors.          |
//+------------------------------------------------------------------+
void DXVec4Lerp(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2,float s)
  {
   pout.x = (1.0f-s)*pv1.x + s*pv2.x;
   pout.y = (1.0f-s)*pv1.y + s*pv2.y;
   pout.z = (1.0f-s)*pv1.z + s*pv2.z;
   pout.w = (1.0f-s)*pv1.w + s*pv2.w;
  }
//+------------------------------------------------------------------+
//| Returns a 4D vector that is made up of the largest components    |
//| of two 4D vectors.                                               |
//+------------------------------------------------------------------+
void DXVec4Maximize(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2)
  {
   pout.x = (float)fmax(pv1.x,pv2.x);
   pout.y = (float)fmax(pv1.y,pv2.y);
   pout.z = (float)fmax(pv1.z,pv2.z);
   pout.w = (float)fmax(pv1.w,pv2.w);
  }
//+------------------------------------------------------------------+
//| Returns a 4D vector that is made up of the smallest components   |
//| of two 4D vectors.                                               |
//+------------------------------------------------------------------+
void DXVec4Minimize(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2)
  {
   pout.x = (float)fmin(pv1.x,pv2.x);
   pout.y = (float)fmin(pv1.y,pv2.y);
   pout.z = (float)fmin(pv1.z,pv2.z);
   pout.w = (float)fmin(pv1.w,pv2.w);
  }
//+------------------------------------------------------------------+
//| Returns the normalized version of a 4D vector.                   |
//+------------------------------------------------------------------+
void DXVec4Normalize(DXVector4 &pout,const DXVector4 &pv)
  {
//--- calculate length
   float norm = DXVec4Length(pv);
   if(!norm)
     {
      pout.x = 0.0f;
      pout.y = 0.0f;
      pout.z = 0.0f;
      pout.w = 0.0f;
     }
   else
     {
      pout.x = pv.x/norm;
      pout.y = pv.y/norm;
      pout.z = pv.z/norm;
      pout.w = pv.w/norm;
     }
  }
//+------------------------------------------------------------------+
//| Scales a 4D vector.                                              |
//+------------------------------------------------------------------+
void DXVec4Scale(DXVector4 &pout,const DXVector4 &pv,float s)
  {
   pout.x = s*pv.x;
   pout.y = s*pv.y;
   pout.z = s*pv.z;
   pout.w = s*pv.w;
  }
//+------------------------------------------------------------------+
//| Subtracts two 4D vectors.                                        |
//+------------------------------------------------------------------+
void DXVec4Subtract(DXVector4 &pout,const DXVector4 &pv1,const DXVector4 &pv2)
  {
   pout.x = pv1.x - pv2.x;
   pout.y = pv1.y - pv2.y;
   pout.z = pv1.z - pv2.z;
   pout.w = pv1.w - pv2.w;
  }
//+------------------------------------------------------------------+
//| Transforms a 4D vector by a given matrix.                        |
//+------------------------------------------------------------------+
void DXVec4Transform(DXVector4 &pout,const DXVector4 &pv,const DXMatrix &pm)
  {
   DXVector4 temp;
   temp.x = pm.m[0][0]*pv.x + pm.m[1][0]*pv.y + pm.m[2][0]*pv.z + pm.m[3][0]*pv.w;
   temp.y = pm.m[0][1]*pv.x + pm.m[1][1]*pv.y + pm.m[2][1]*pv.z + pm.m[3][1]*pv.w;
   temp.z = pm.m[0][2]*pv.x + pm.m[1][2]*pv.y + pm.m[2][2]*pv.z + pm.m[3][2]*pv.w;
   temp.w = pm.m[0][3]*pv.x + pm.m[1][3]*pv.y + pm.m[2][3]*pv.z + pm.m[3][3]*pv.w;
   pout=temp;
  }
//+------------------------------------------------------------------+
//| Returns a quaternion in barycentric coordinates.                 |
//+------------------------------------------------------------------+
void DXQuaternionBaryCentric(DXQuaternion &pout,DXQuaternion &pq1,DXQuaternion &pq2,DXQuaternion &pq3,float f,float g)
  {
   DXQuaternion temp1,temp2;
   DXQuaternionSlerp(temp1,pq1,pq2,f+g);
   DXQuaternionSlerp(temp2,pq1,pq3,f+g);
   DXQuaternionSlerp(pout,temp1,temp2,g/(f+g));
  }
//+------------------------------------------------------------------+
//| Returns the conjugate of a quaternion.                           |
//+------------------------------------------------------------------+
void DXQuaternionConjugate(DXQuaternion &pout,const DXQuaternion &pq)
  {
   pout.x = -pq.x;
   pout.y = -pq.y;
   pout.z = -pq.z;
   pout.w = pq.w;
  }
//+------------------------------------------------------------------+
//| Returns the dot product of two quaternions.                      |
//+------------------------------------------------------------------+
float DXQuaternionDot(DXQuaternion &a,DXQuaternion &b)
  {
   return(a.x*b.x + a.y*b.y + a.z*b.z + a.w*b.w);
  }
//+------------------------------------------------------------------+
//| Calculates the exponential.                                      |
//| This method converts a pure quaternion to a unit quaternion.     |
//| DXQuaternionExp expects a pure quaternion, where w is ignored    |
//| in the calculation (w == 0).                                     |
//+------------------------------------------------------------------+
void DXQuaternionExp(DXQuaternion &out,const DXQuaternion &q)
  {
   float norm = (float)sqrt(q.x*q.x + q.y*q.y + q.z*q.z);
   if(norm)
     {
      out.x = (float)sin(norm)*q.x/norm;
      out.y = (float)sin(norm)*q.y/norm;
      out.z = (float)sin(norm)*q.z/norm;
      out.w = (float)cos(norm);
     }
   else
     {
      out.x = 0.0f;
      out.y = 0.0f;
      out.z = 0.0f;
      out.w = 1.0f;
     }
  }
//+------------------------------------------------------------------+
//| Returns the identity quaternion.                                 |
//+------------------------------------------------------------------+
void DXQuaternionIdentity(DXQuaternion &out)
  {
   out.x = 0.0f;
   out.y = 0.0f;
   out.z = 0.0f;
   out.w = 1.0f;
  }
//+------------------------------------------------------------------+
//| Determines if a quaternion is an identity quaternion.            |
//+------------------------------------------------------------------+
bool DXQuaternionIsIdentity(DXQuaternion &pq)
  {
   return ((pq.x == 0.0f) && (pq.y == 0.0f) && (pq.z == 0.0f) && (pq.w == 1.0f));
  }
//+------------------------------------------------------------------+
//| Returns the length of a quaternion.                              |
//+------------------------------------------------------------------+
float DXQuaternionLength(const DXQuaternion &pq)
  {
   return((float)sqrt(pq.x*pq.x + pq.y*pq.y + pq.z*pq.z + pq.w*pq.w));
  }
//+------------------------------------------------------------------+
//| Returns the square of the length of a quaternion.                |
//+------------------------------------------------------------------+
float DXQuaternionLengthSq(const DXQuaternion &pq)
  {
   return((float)(pq.x*pq.x + pq.y*pq.y + pq.z*pq.z + pq.w*pq.w));
  }
//+------------------------------------------------------------------+
//| Conjugates and renormalizes a quaternion.                        |
//+------------------------------------------------------------------+
void DXQuaternionInverse(DXQuaternion &pout,const DXQuaternion &pq)
  {
   float norm = DXQuaternionLengthSq(pq);
   pout.x = -pq.x/norm;
   pout.y = -pq.y/norm;
   pout.z = -pq.z/norm;
   pout.w = pq.w/norm;
  }
//+------------------------------------------------------------------+
//| Calculates the natural logarithm.                                |
//| The DXQuaternionLn function works only for unit quaternions.     |
//+------------------------------------------------------------------+
void DXQuaternionLn(DXQuaternion &out,const DXQuaternion &q)
  {
   float t;
   if((q.w >= 1.0f) || (q.w == -1.0f))
      t = 1.0f;
   else
      t = (float)(acos(q.w)/sqrt(1.0f - q.w*q.w));
   out.x = t*q.x;
   out.y = t*q.y;
   out.z = t*q.z;
   out.w = 0.0f;
  }
//+------------------------------------------------------------------+
//| Multiplies two quaternions.                                      |
//+------------------------------------------------------------------+
void DXQuaternionMultiply(DXQuaternion &pout,const DXQuaternion &pq1,const DXQuaternion &pq2)
  {
   DXQuaternion out;
   out.x = pq2.w*pq1.x + pq2.x*pq1.w + pq2.y*pq1.z - pq2.z*pq1.y;
   out.y = pq2.w*pq1.y - pq2.x*pq1.z + pq2.y*pq1.w + pq2.z*pq1.x;
   out.z = pq2.w*pq1.z + pq2.x*pq1.y - pq2.y*pq1.x + pq2.z*pq1.w;
   out.w = pq2.w*pq1.w - pq2.x*pq1.x - pq2.y*pq1.y - pq2.z*pq1.z;
   pout = out;
  }
//+------------------------------------------------------------------+
//| Computes a unit length quaternion.                               |
//+------------------------------------------------------------------+
void DXQuaternionNormalize(DXQuaternion &out,const DXQuaternion &q)
  {
   float norm = DXQuaternionLength(q);
   if(!norm)
     {
      out.x = 0.0f;
      out.y = 0.0f;
      out.z = 0.0f;
      out.w = 0.0f;
     }
   else
     {
      out.x = q.x/norm;
      out.y = q.y/norm;
      out.z = q.z/norm;
      out.w = q.w/norm;
     }
  }
//+------------------------------------------------------------------+
//| Rotates a quaternion about an arbitrary axis.                    |
//+------------------------------------------------------------------+
void DXQuaternionRotationAxis(DXQuaternion &out,const DXVector3 &v,float angle)
  {
   DXVector3 temp;
   DXVec3Normalize(temp,v);
   out.x = (float)sin(angle/2.0f)*temp.x;
   out.y = (float)sin(angle/2.0f)*temp.y;
   out.z = (float)sin(angle/2.0f)*temp.z;
   out.w = (float)cos(angle/2.0f);
  }
//+------------------------------------------------------------------+
//| Builds a quaternion from a rotation matrix.                      |
//+------------------------------------------------------------------+
void DXQuaternionRotationMatrix(DXQuaternion &out,const DXMatrix &m)
  {
   float s;
   float trace = m.m[0][0] + m.m[1][1] + m.m[2][2] + 1.0f;
   if(trace>1.0f)
     {
      s = 2.0f*(float)sqrt(trace);
      out.x = (m.m[1][2] - m.m[2][1])/s;
      out.y = (m.m[2][0] - m.m[0][2])/s;
      out.z = (m.m[0][1] - m.m[1][0])/s;
      out.w = 0.25f*s;
     }
   else
     {
      int maxi = 0;
      for(int i=1; i<3; i++)
        {
         if(m.m[i][i] > m.m[maxi][maxi])
            maxi = i;
        }
      switch(maxi)
        {
         case 0:
            s = 2.0f*(float)sqrt(1.0f + m.m[0][0] - m.m[1][1] - m.m[2][2]);
            out.x = 0.25f*s;
            out.y = (m.m[0][1] + m.m[1][0])/s;
            out.z = (m.m[0][2] + m.m[2][0])/s;
            out.w = (m.m[1][2] - m.m[2][1])/s;
            break;

         case 1:
            s = 2.0f*(float)sqrt(1.0f + m.m[1][1] - m.m[0][0] - m.m[2][2]);
            out.x = (m.m[0][1] + m.m[1][0])/s;
            out.y = 0.25f*s;
            out.z = (m.m[1][2] + m.m[2][1])/s;
            out.w = (m.m[2][0] - m.m[0][2])/s;
            break;

         case 2:
            s = 2.0f*(float)sqrt(1.0f + m.m[2][2] - m.m[0][0] - m.m[1][1]);
            out.x = (m.m[0][2] + m.m[2][0])/s;
            out.y = (m.m[1][2] + m.m[2][1])/s;
            out.z = 0.25f*s;
            out.w = (m.m[0][1] - m.m[1][0])/s;
            break;
        }
     }
  }
//+------------------------------------------------------------------+
//| Builds a quaternion with the given yaw, pitch, and roll.         |
//+------------------------------------------------------------------+
void DXQuaternionRotationYawPitchRoll(DXQuaternion &out,float yaw,float pitch,float roll)
  {
   float syaw = (float)sin(yaw/2.0f);
   float cyaw = (float)cos(yaw/2.0f);
   float spitch = (float)sin(pitch/2.0f);
   float cpitch = (float)cos(pitch/2.0f);
   float sroll = (float)sin(roll/2.0f);
   float croll = (float)cos(roll/2.0f);
//---
   out.x = syaw*cpitch*sroll + cyaw*spitch*croll;
   out.y = syaw*cpitch*croll - cyaw*spitch*sroll;
   out.z = cyaw*cpitch*sroll - syaw*spitch*croll;
   out.w = cyaw*cpitch*croll + syaw*spitch*sroll;
  }
//+------------------------------------------------------------------+
//| Interpolates between two quaternions, using spherical linear     |
//| interpolation.                                                   |
//+------------------------------------------------------------------+
void DXQuaternionSlerp(DXQuaternion &out,DXQuaternion &q1,DXQuaternion &q2,float t)
  {
   float temp = 1.0f - t;
   float dot = DXQuaternionDot(q1,q2);
   if(dot<0.0f)
     {
      t = -t;
      dot = -dot;
     }
   if(1.0f-dot>0.001f)
     {
      float theta = (float)acos(dot);
      temp = (float)sin(theta*temp)/(float)sin(theta);
      t = (float)sin(theta*t)/(float)sin(theta);
     }
   out.x = temp*q1.x + t*q2.x;
   out.y = temp*q1.y + t*q2.y;
   out.z = temp*q1.z + t*q2.z;
   out.w = temp*q1.w + t*q2.w;
  }
//+------------------------------------------------------------------+
//| Interpolates between quaternions, using spherical quadrangle     |
//| interpolation.                                                   |
//+------------------------------------------------------------------+
void DXQuaternionSquad(DXQuaternion &pout,DXQuaternion &pq1,DXQuaternion &pq2,DXQuaternion &pq3,DXQuaternion &pq4,float t)
  {
   DXQuaternion temp1,temp2;
   DXQuaternionSlerp(temp1,pq1,pq4,t);
   DXQuaternionSlerp(temp2,pq2,pq3,t);
   DXQuaternionSlerp(pout,temp1,temp2,2.0f*t*(1.0f - t));
  }
//+------------------------------------------------------------------+
//| add_diff                                                         |
//+------------------------------------------------------------------+
DXQuaternion add_diff(const DXQuaternion &q1,const DXQuaternion &q2,const float add)
  {
   DXQuaternion temp;
   temp.x = q1.x + add * q2.x;
   temp.y = q1.y + add * q2.y;
   temp.z = q1.z + add * q2.z;
   temp.w = q1.w + add * q2.w;
//---
   return(temp);
  }
//+------------------------------------------------------------------+
//| Sets up control points for spherical quadrangle interpolation.   |
//+------------------------------------------------------------------+
void DXQuaternionSquadSetup(DXQuaternion &paout,DXQuaternion &pbout,DXQuaternion &pcout,DXQuaternion &pq0,DXQuaternion &pq1,DXQuaternion &pq2,DXQuaternion &pq3)
  {
   DXQuaternion q,temp1,temp2,temp3,zero;
   DXQuaternion aout,cout;
   zero.x = 0.0f;
   zero.y = 0.0f;
   zero.z = 0.0f;
   zero.w = 0.0f;
//---
   if(DXQuaternionDot(pq0,pq1) < 0.0f)
      temp2 = add_diff(zero,pq0,-1.0f);
   else
      temp2 = pq0;
//---
   if(DXQuaternionDot(pq1,pq2) < 0.0f)
      cout = add_diff(zero,pq2,-1.0f);
   else
      cout = pq2;
//---
   if(DXQuaternionDot(cout,pq3) < 0.0f)
      temp3 = add_diff(zero,pq3,-1.0f);
   else
      temp3 = pq3;
//---
   DXQuaternionInverse(temp1,pq1);
   DXQuaternionMultiply(temp2,temp1,temp2);
   DXQuaternionLn(temp2,temp2);
   DXQuaternionMultiply(q,temp1,cout);
   DXQuaternionLn(q,q);
   temp1 = add_diff(temp2,q,1.0f);
   temp1.x *= -0.25f;
   temp1.y *= -0.25f;
   temp1.z *= -0.25f;
   temp1.w *= -0.25f;
   DXQuaternionExp(temp1,temp1);
   DXQuaternionMultiply(aout,pq1,temp1);
//---
   DXQuaternionInverse(temp1,cout);
   DXQuaternionMultiply(temp2,temp1,pq1);
   DXQuaternionLn(temp2,temp2);
   DXQuaternionMultiply(q,temp1,temp3);
   DXQuaternionLn(q,q);
   temp1 = add_diff(temp2,q,1.0f);
   temp1.x *= -0.25f;
   temp1.y *= -0.25f;
   temp1.z *= -0.25f;
   temp1.w *= -0.25f;
   DXQuaternionExp(temp1,temp1);
   DXQuaternionMultiply(pbout,cout,temp1);
   paout = aout;
   pcout = cout;
  }
//+------------------------------------------------------------------+
//| Computes a quaternion's axis and angle of rotation.              |
//+------------------------------------------------------------------+
void DXQuaternionToAxisAngle(const DXQuaternion &pq,DXVector3 &paxis,float &pangle)
  {
//--- paxis
   paxis.x = pq.x;
   paxis.y = pq.y;
   paxis.z = pq.z;
//--- pangle
   pangle = 2.0f*(float)acos(pq.w);
  }
//+------------------------------------------------------------------+
//| DXMatrixIdentity creates an identity matrix                      |
//+------------------------------------------------------------------+
void DXMatrixIdentity(DXMatrix &out)
  {
   for(int j=0; j<4; j++)
      for(int i=0; i<4; i++)
        {
         if(i==j)
            out.m[j,i]=1.0f;
         else
            out.m[j,i]=0.0f;
        }
  }
//+------------------------------------------------------------------+
//| Determines if a matrix is an identity matrix.                    |
//+------------------------------------------------------------------+
bool DXMatrixIsIdentity(DXMatrix &pm)
  {
   for(int j=0; j<4; j++)
      for(int i=0; i<4; i++)
        {
         if(i==j)
           {
            if(fabs(pm.m[j,i]-1.0f)>1e-6f)
               return(false);
           }
         else
            if(fabs(pm.m[j,i])>1e-6f)
               return(false);
        }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Builds a 3D affine transformation matrix.                        |
//+------------------------------------------------------------------+
//| This function calculates the affine transformation matrix        |
//| with the following formula, with matrix concatenation            |
//| evaluated in left-to-right order:                                |
//|             Mout = Ms * (Mrc)-1 * Mr * Mrc * Mt                  |
//| where:                                                           |
//| Mout = output matrix (pOut)                                      |
//| Ms = scaling matrix (Scaling)                                    |
//| Mrc = center of rotation matrix (pRotationCenter)                |
//| Mr = rotation matrix (pRotation)                                 |
//| Mt = translation matrix (pTranslation)                           |
//+------------------------------------------------------------------+
void DXMatrixAffineTransformation(DXMatrix &out,float scaling,const DXVector3 &rotationcenter,const DXQuaternion &rotation,const DXVector3 &translation)
  {
   DXMatrixIdentity(out);
//--- rotation
   float temp00 = 1.0f - 2.0f*(rotation.y*rotation.y + rotation.z*rotation.z);
   float temp01 = 2.0f * (rotation.x*rotation.y + rotation.z*rotation.w);
   float temp02 = 2.0f * (rotation.x*rotation.z - rotation.y*rotation.w);
   float temp10 = 2.0f * (rotation.x*rotation.y - rotation.z*rotation.w);
   float temp11 = 1.0f - 2.0f*(rotation.x*rotation.x + rotation.z*rotation.z);
   float temp12 = 2.0f * (rotation.y*rotation.z + rotation.x*rotation.w);
   float temp20 = 2.0f * (rotation.x*rotation.z + rotation.y*rotation.w);
   float temp21 = 2.0f * (rotation.y*rotation.z - rotation.x*rotation.w);
   float temp22 = 1.0f - 2.0f*(rotation.x*rotation.x + rotation.y*rotation.y);
//--- scaling
   out.m[0][0] = scaling*temp00;
   out.m[0][1] = scaling*temp01;
   out.m[0][2] = scaling*temp02;
   out.m[1][0] = scaling*temp10;
   out.m[1][1] = scaling*temp11;
   out.m[1][2] = scaling*temp12;
   out.m[2][0] = scaling*temp20;
   out.m[2][1] = scaling*temp21;
   out.m[2][2] = scaling*temp22;
//--- rotationcenter
   out.m[3][0] = rotationcenter.x*(1.0f - temp00) - rotationcenter.y*temp10 - rotationcenter.z*temp20;
   out.m[3][1] = rotationcenter.y*(1.0f - temp11) - rotationcenter.x*temp01 - rotationcenter.z*temp21;
   out.m[3][2] = rotationcenter.z*(1.0f - temp22) - rotationcenter.x*temp02 - rotationcenter.y*temp12;
//--- translation
   out.m[3][0] += translation.x;
   out.m[3][1] += translation.y;
   out.m[3][2] += translation.z;
  }
//+------------------------------------------------------------------+
//| Builds a 2D affine transformation matrix in the xy plane.        |
//+------------------------------------------------------------------+
//| This function calculates the affine transformation matrix        |
//| with the following formula, with matrix concatenation evaluated  |
//| in left-to-right order:                                          |
//|             Mout = Ms * (Mrc)^(-1) * Mr * Mrc * Mt               |
//| where:                                                           |
//| Mout = output matrix (pOut)                                      |
//| Ms = scaling matrix (Scaling)                                    |
//| Mrc = center of rotation matrix (pRotationCenter)                |
//| Mr = rotation matrix (Rotation)                                  |
//| Mt = translation matrix (pTranslation)                           |
//+------------------------------------------------------------------+
void DXMatrixAffineTransformation2D(DXMatrix &out,float scaling,const DXVector2 &rotationcenter,float rotation,const DXVector2 &translation)
  {
   float s = (float)sin(rotation/2.0f);
   float tmp1 = 1.0f - 2.0f*s*s;
   float tmp2 = 2.0f*s*(float)cos(rotation/2.0f);
//---
   DXMatrixIdentity(out);
   out.m[0][0] = scaling*tmp1;
   out.m[0][1] = scaling*tmp2;
   out.m[1][0] = -scaling*tmp2;
   out.m[1][1] = scaling*tmp1;
//--- rotationcenter
   float x = rotationcenter.x;
   float y = rotationcenter.y;
   out.m[3][0] = y*tmp2 - x*tmp1 + x;
   out.m[3][1] = -x*tmp2 - y*tmp1 + y;
//--- translation
   out.m[3][0] += translation.x;
   out.m[3][1] += translation.y;
  }
#define D3DERR_INVALIDCALL -2005530516
//#define S_OK                 0;
//+------------------------------------------------------------------+
//| Breaks down a general 3D transformation matrix into its scalar,  |
//| rotational, and translational components.                        |
//+------------------------------------------------------------------+
int DXMatrixDecompose(DXVector3 &poutscale,DXQuaternion &poutrotation,DXVector3 &pouttranslation,const DXMatrix &pm)
  {
   DXMatrix normalized;
   DXVector3 vec;
//--- Compute the scaling part
   vec.x=pm.m[0][0];
   vec.y=pm.m[0][1];
   vec.z=pm.m[0][2];
   poutscale.x=DXVec3Length(vec);
   vec.x=pm.m[1][0];
   vec.y=pm.m[1][1];
   vec.z=pm.m[1][2];
   poutscale.y=DXVec3Length(vec);
   vec.x=pm.m[2][0];
   vec.y=pm.m[2][1];
   vec.z=pm.m[2][2];
   poutscale.z=DXVec3Length(vec);
//--- compute the translation part
   pouttranslation.x=pm.m[3][0];
   pouttranslation.y=pm.m[3][1];
   pouttranslation.z=pm.m[3][2];
//--- let's calculate the rotation now
   if((poutscale.x == 0.0f) || (poutscale.y == 0.0f) || (poutscale.z == 0.0f))
      return(D3DERR_INVALIDCALL);
//---
   normalized.m[0][0]=pm.m[0][0]/poutscale.x;
   normalized.m[0][1]=pm.m[0][1]/poutscale.x;
   normalized.m[0][2]=pm.m[0][2]/poutscale.x;
   normalized.m[1][0]=pm.m[1][0]/poutscale.y;
   normalized.m[1][1]=pm.m[1][1]/poutscale.y;
   normalized.m[1][2]=pm.m[1][2]/poutscale.y;
   normalized.m[2][0]=pm.m[2][0]/poutscale.z;
   normalized.m[2][1]=pm.m[2][1]/poutscale.z;
   normalized.m[2][2]=pm.m[2][2]/poutscale.z;
   DXQuaternionRotationMatrix(poutrotation,normalized);
//---
   return(0);
  }
//+------------------------------------------------------------------+
//| Returns the determinant of a matrix.                             |
//+------------------------------------------------------------------+
float DXMatrixDeterminant(const DXMatrix &pm)
  {
   float t[3],v[4];
   t[0] = pm.m[2][2]*pm.m[3][3] - pm.m[2][3]*pm.m[3][2];
   t[1] = pm.m[1][2]*pm.m[3][3] - pm.m[1][3]*pm.m[3][2];
   t[2] = pm.m[1][2]*pm.m[2][3] - pm.m[1][3]*pm.m[2][2];
   v[0] = pm.m[1][1]*t[0] - pm.m[2][1] * t[1] + pm.m[3][1]*t[2];
   v[1] = -pm.m[1][0]*t[0] + pm.m[2][0] * t[1] - pm.m[3][0]*t[2];
//---
   t[0] = pm.m[1][0]*pm.m[2][1] - pm.m[2][0]*pm.m[1][1];
   t[1] = pm.m[1][0]*pm.m[3][1] - pm.m[3][0]*pm.m[1][1];
   t[2] = pm.m[2][0]*pm.m[3][1] - pm.m[3][0]*pm.m[2][1];
   v[2] = pm.m[3][3]*t[0] - pm.m[2][3]*t[1] + pm.m[1][3]*t[2];
   v[3] = -pm.m[3][2]*t[0] + pm.m[2][2]*t[1] - pm.m[1][2]*t[2];
//---
   return(pm.m[0][0]*v[0] + pm.m[0][1]*v[1] + pm.m[0][2]*v[2] + pm.m[0][3]*v[3]);
  }
//+------------------------------------------------------------------+
//| Calculates the inverse of a matrix.                              |
//+------------------------------------------------------------------+
void DXMatrixInverse(DXMatrix &pout,float &pdeterminant,const DXMatrix &pm)
  {
   float t[3],v[16];
   t[0] = pm.m[2][2]*pm.m[3][3] - pm.m[2][3]*pm.m[3][2];
   t[1] = pm.m[1][2]*pm.m[3][3] - pm.m[1][3]*pm.m[3][2];
   t[2] = pm.m[1][2]*pm.m[2][3] - pm.m[1][3]*pm.m[2][2];
   v[0] = pm.m[1][1]*t[0] - pm.m[2][1]*t[1] + pm.m[3][1]*t[2];
   v[4] = -pm.m[1][0]*t[0] + pm.m[2][0]*t[1] - pm.m[3][0]*t[2];
//---
   t[0] = pm.m[1][0]*pm.m[2][1] - pm.m[2][0]*pm.m[1][1];
   t[1] = pm.m[1][0]*pm.m[3][1] - pm.m[3][0]*pm.m[1][1];
   t[2] = pm.m[2][0]*pm.m[3][1] - pm.m[3][0]*pm.m[2][1];
   v[8] = pm.m[3][3]*t[0] - pm.m[2][3]*t[1] + pm.m[1][3]*t[2];
   v[12] = -pm.m[3][2]*t[0] + pm.m[2][2]*t[1] - pm.m[1][2]*t[2];
//---
   float det = pm.m[0][0]*v[0] + pm.m[0][1]*v[4] + pm.m[0][2]*v[8] + pm.m[0][3]*v[12];
   if(det == 0.0f)
     {
      for(int j=0; j<4; j++)
         for(int i=0; i<4; i++)
           {
            pout.m[j,i]=0.0;
           }
      //---
      return;
     }
   if(pdeterminant)
      pdeterminant = det;
//---
   t[0] = pm.m[2][2]*pm.m[3][3] - pm.m[2][3]*pm.m[3][2];
   t[1] = pm.m[0][2]*pm.m[3][3] - pm.m[0][3]*pm.m[3][2];
   t[2] = pm.m[0][2]*pm.m[2][3] - pm.m[0][3]*pm.m[2][2];
   v[1] = -pm.m[0][1]*t[0] + pm.m[2][1]*t[1] - pm.m[3][1]*t[2];
   v[5] = pm.m[0][0]*t[0] - pm.m[2][0]*t[1] + pm.m[3][0]*t[2];
//---
   t[0] = pm.m[0][0]*pm.m[2][1] - pm.m[2][0] * pm.m[0][1];
   t[1] = pm.m[3][0]*pm.m[0][1] - pm.m[0][0] * pm.m[3][1];
   t[2] = pm.m[2][0]*pm.m[3][1] - pm.m[3][0] * pm.m[2][1];
   v[9] = -pm.m[3][3]*t[0] - pm.m[2][3]*t[1]- pm.m[0][3]*t[2];
   v[13] = pm.m[3][2]*t[0] + pm.m[2][2]*t[1] + pm.m[0][2]*t[2];
//---
   t[0] = pm.m[1][2]*pm.m[3][3] - pm.m[1][3] * pm.m[3][2];
   t[1] = pm.m[0][2]*pm.m[3][3] - pm.m[0][3] * pm.m[3][2];
   t[2] = pm.m[0][2]*pm.m[1][3] - pm.m[0][3] * pm.m[1][2];
   v[2] = pm.m[0][1]*t[0] - pm.m[1][1]*t[1] + pm.m[3][1]*t[2];
   v[6] = -pm.m[0][0]*t[0] + pm.m[1][0]*t[1] - pm.m[3][0]*t[2];
//---
   t[0] = pm.m[0][0]*pm.m[1][1] - pm.m[1][0] * pm.m[0][1];
   t[1] = pm.m[3][0]*pm.m[0][1] - pm.m[0][0] * pm.m[3][1];
   t[2] = pm.m[1][0]*pm.m[3][1] - pm.m[3][0] * pm.m[1][1];
   v[10] = pm.m[3][3]*t[0] + pm.m[1][3]*t[1] + pm.m[0][3]*t[2];
   v[14] = -pm.m[3][2]*t[0] - pm.m[1][2]*t[1] - pm.m[0][2]*t[2];
//---
   t[0] = pm.m[1][2]*pm.m[2][3] - pm.m[1][3] * pm.m[2][2];
   t[1] = pm.m[0][2]*pm.m[2][3] - pm.m[0][3] * pm.m[2][2];
   t[2] = pm.m[0][2]*pm.m[1][3] - pm.m[0][3] * pm.m[1][2];
   v[3] = -pm.m[0][1]*t[0] + pm.m[1][1]*t[1] - pm.m[2][1]*t[2];
   v[7] = pm.m[0][0]*t[0] - pm.m[1][0]*t[1] + pm.m[2][0]*t[2];
//---
   v[11] = -pm.m[0][0]*(pm.m[1][1]*pm.m[2][3] - pm.m[1][3]*pm.m[2][1]) +
           pm.m[1][0]*(pm.m[0][1]*pm.m[2][3] - pm.m[0][3]*pm.m[2][1]) -
           pm.m[2][0]*(pm.m[0][1]*pm.m[1][3] - pm.m[0][3]*pm.m[1][1]);
//---
   v[15] = pm.m[0][0]*(pm.m[1][1]*pm.m[2][2] - pm.m[1][2]*pm.m[2][1]) -
           pm.m[1][0]*(pm.m[0][1]*pm.m[2][2] - pm.m[0][2]*pm.m[2][1]) +
           pm.m[2][0]*(pm.m[0][1]*pm.m[1][2] - pm.m[0][2]*pm.m[1][1]);
//---
   det = 1.0f/det;
   for(int i=0; i<4; i++)
      for(int j=0; j<4; j++)
         pout.m[i][j] = v[4*i + j]*det;
  }
//+------------------------------------------------------------------+
//| Builds a left-handed,look-at matrix.                             |
//| This function uses the following formula to compute              |
//| the returned matrix.                                             |
//|                                                                  |
//| zaxis = normal(At - Eye)                                         |
//| xaxis = normal(cross(Up,zaxis))                                  |
//| yaxis = cross(zaxis,xaxis)                                       |
//|                                                                  |
//| xaxis.x           yaxis.x           zaxis.x          0           |
//| xaxis.y           yaxis.y           zaxis.y          0           |
//| xaxis.z           yaxis.z           zaxis.z          0           |
//| -dot(xaxis,eye)  -dot(yaxis,eye)  -dot(zaxis,eye)  1             |
//+------------------------------------------------------------------+
void DXMatrixLookAtLH(DXMatrix &out,const DXVector3 &eye,const DXVector3 &at,const DXVector3 &up)
  {
   DXVector3 right,upn,vec;
   DXVec3Subtract(vec,at,eye);
   DXVec3Normalize(vec,vec);
   DXVec3Cross(right,up,vec);
   DXVec3Cross(upn,vec,right);
   DXVec3Normalize(right,right);
   DXVec3Normalize(upn,upn);
//---
   out.m[0][0] = right.x;
   out.m[1][0] = right.y;
   out.m[2][0] = right.z;
   out.m[3][0] = -DXVec3Dot(right,eye);
   out.m[0][1] = upn.x;
   out.m[1][1] = upn.y;
   out.m[2][1] = upn.z;
   out.m[3][1] = -DXVec3Dot(upn,eye);
   out.m[0][2] = vec.x;
   out.m[1][2] = vec.y;
   out.m[2][2] = vec.z;
   out.m[3][2] = -DXVec3Dot(vec,eye);
   out.m[0][3] = 0.0f;
   out.m[1][3] = 0.0f;
   out.m[2][3] = 0.0f;
   out.m[3][3] = 1.0f;
  }
//+------------------------------------------------------------------+
//| Builds a right-handed, look-at matrix.                           |
//+------------------------------------------------------------------+
//| This function uses the following formula to compute              |
//| the returned matrix.                                             |
//|                                                                  |
//| zaxis = normal(Eye - At)                                         |
//| xaxis = normal(cross(Up,zaxis))                                  |
//| yaxis = cross(zaxis,xaxis)                                       |
//|                                                                  |
//|  xaxis.x           yaxis.x           zaxis.x          0          |
//|  xaxis.y           yaxis.y           zaxis.y          0          |
//|  xaxis.z           yaxis.z           zaxis.z          0          |
//|  dot(xaxis,eye)   dot(yaxis,eye)   dot(zaxis,eye)     1          |
//+------------------------------------------------------------------+
void DXMatrixLookAtRH(DXMatrix &out,const DXVector3 &eye,const DXVector3 &at,const DXVector3 &up)
  {
   DXVector3 right,upn,vec;
   DXVec3Subtract(vec,at,eye);
   DXVec3Normalize(vec,vec);
   DXVec3Cross(right,up,vec);
   DXVec3Cross(upn,vec,right);
   DXVec3Normalize(right,right);
   DXVec3Normalize(upn,upn);
//---
   out.m[0][0] = -right.x;
   out.m[1][0] = -right.y;
   out.m[2][0] = -right.z;
   out.m[3][0] = DXVec3Dot(right,eye);
   out.m[0][1] = upn.x;
   out.m[1][1] = upn.y;
   out.m[2][1] = upn.z;
   out.m[3][1] = -DXVec3Dot(upn,eye);
   out.m[0][2] = -vec.x;
   out.m[1][2] = -vec.y;
   out.m[2][2] = -vec.z;
   out.m[3][2] = DXVec3Dot(vec,eye);
   out.m[0][3] = 0.0f;
   out.m[1][3] = 0.0f;
   out.m[2][3] = 0.0f;
   out.m[3][3] = 1.0f;
  }
//+------------------------------------------------------------------+
//| Determines the product of two matrices.                          |
//+------------------------------------------------------------------+
void DXMatrixMultiply(DXMatrix &pout,const DXMatrix &pm1,const DXMatrix &pm2)
  {
   DXMatrix out= {};
   for(int i=0; i<4; i++)
     {
      for(int j=0; j<4; j++)
        {
         out.m[i][j] = pm1.m[i][0]*pm2.m[0][j] + pm1.m[i][1]*pm2.m[1][j] + pm1.m[i][2]*pm2.m[2][j] + pm1.m[i][3]*pm2.m[3][j];
        }
     }
   pout = out;
  }
//+------------------------------------------------------------------+
//| Calculates the transposed product of two matrices.               |
//+------------------------------------------------------------------+
void DXMatrixMultiplyTranspose(DXMatrix &pout,const DXMatrix &pm1,const DXMatrix &pm2)
  {
   DXMatrix temp= {};
   for(int i = 0; i < 4; i++)
      for(int j = 0; j < 4; j++)
         temp.m[j][i] = pm1.m[i][0]*pm2.m[0][j] + pm1.m[i][1]*pm2.m[1][j] + pm1.m[i][2]*pm2.m[2][j] + pm1.m[i][3]*pm2.m[3][j];
   pout = temp;
  }
//+------------------------------------------------------------------+
//| Builds a left-handed orthographic projection matrix.             |
//+------------------------------------------------------------------+
void DXMatrixOrthoLH(DXMatrix &pout,float w,float h,float zn,float zf)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 2.0f/w;
   pout.m[1][1] = 2.0f/h;
   pout.m[2][2] = 1.0f/(zf - zn);
   pout.m[3][2] = zn/(zn - zf);
  }
//+------------------------------------------------------------------+
//| Builds a customized,left-handed orthographic projection matrix.  |
//+------------------------------------------------------------------+
void DXMatrixOrthoOffCenterLH(DXMatrix &pout,float l,float r,float b,float t,float zn,float zf)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 2.0f/(r - l);
   pout.m[1][1] = 2.0f/(t - b);
   pout.m[2][2] = 1.0f/(zf -zn);
   pout.m[3][0] = -1.0f -2.0f*l/(r - l);
   pout.m[3][1] = 1.0f + 2.0f*t/(b - t);
   pout.m[3][2] = zn/(zn -zf);
  }
//+------------------------------------------------------------------+
//| Builds a customized,right-handed orthographic projection matrix. |
//+------------------------------------------------------------------+
void DXMatrixOrthoOffCenterRH(DXMatrix &pout,float l,float r,float b,float t,float zn,float zf)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 2.0f/(r - l);
   pout.m[1][1] = 2.0f/(t - b);
   pout.m[2][2] = 1.0f/(zn -zf);
   pout.m[3][0] = -1.0f -2.0f*l/(r - l);
   pout.m[3][1] = 1.0f + 2.0f*t/(b - t);
   pout.m[3][2] = zn/(zn -zf);
  }
//+------------------------------------------------------------------+
//| Builds a right-handed orthographic projection matrix.            |
//+------------------------------------------------------------------+
//| All the parameters of the DXMatrixOrthoRH function               |
//| are distances in camera space. The parameters describe           |
//| the dimensions of the view volume.                               |
//|                                                                  |
//| This function uses the following formula to compute              |
//| the returned matrix:                                             |
//| 2/w  0    0           0                                          |
//| 0    2/h  0           0                                          |
//| 0    0    1/(zn-zf)   0                                          |
//| 0    0    zn/(zn-zf)  1                                          |
//+------------------------------------------------------------------+
void DXMatrixOrthoRH(DXMatrix &pout,float w,float h,float zn,float zf)
  {
   DXMatrixIdentity(pout);
   pout.m[0][0] = 2.0f/w;
   pout.m[1][1] = 2.0f/h;
   pout.m[2][2] = 1.0f/(zn - zf);
   pout.m[3][2] = zn/(zn - zf);
  }
//+------------------------------------------------------------------+
//| Builds a left-handed perspective projection matrix               |
//| based on a field of view.                                        |
//+------------------------------------------------------------------+
//| This function computes the returned matrix as shown:             |
//| xScale     0          0               0                          |
//| 0        yScale       0               0                          |
//| 0          0       zf/(zf-zn)         1                          |
//| 0          0       -zn*zf/(zf-zn)     0                          |
//| where:                                                           |
//| yScale = cot(fovY/2)                                             |
//| xScale = yScale / aspect ratio                                   |
//+------------------------------------------------------------------+
void DXMatrixPerspectiveFovLH(DXMatrix &pout,float fovy,float aspect,float zn,float zf)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 1.0f/(aspect*(float)tan(fovy/2.0f));
   pout.m[1][1] = 1.0f/(float)tan(fovy/2.0f);
   pout.m[2][2] = zf/(zf - zn);
   pout.m[2][3] = 1.0f;
   pout.m[3][2] = (zf*zn)/(zn - zf);
   pout.m[3][3] = 0.0f;
  }
//+------------------------------------------------------------------+
//| Builds a right-handed perspective projection matrix              |
//| based on a field of view.                                        |
//+------------------------------------------------------------------+
//| This function computes the returned matrix as shown.             |
//| xScale     0          0              0                           |
//| 0        yScale       0              0                           |
//| 0        0        zf/(zn-zf)        -1                           |
//| 0        0        zn*zf/(zn-zf)      0                           |
//| where:                                                           |
//| yScale = cot(fovY/2)                                             |
//| xScale = yScale / aspect ratio                                   |
//+------------------------------------------------------------------+
void DXMatrixPerspectiveFovRH(DXMatrix &pout,float fovy,float aspect,float zn,float zf)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 1.0f/(aspect*(float)tan(fovy/2.0f));
   pout.m[1][1] = 1.0f/(float)tan(fovy/2.0f);
   pout.m[2][2] = zf/(zn - zf);
   pout.m[2][3] = -1.0f;
   pout.m[3][2] = (zf*zn)/(zn - zf);
   pout.m[3][3] = 0.0f;
  }
//+------------------------------------------------------------------+
//| Builds a left-handed perspective projection matrix               |
//+------------------------------------------------------------------+
//| This function uses the following formula to compute              |
//| the returned matrix.                                             |
//| 2*zn/w  0       0              0                                 |
//| 0       2*zn/h  0              0                                 |
//| 0       0       zf/(zf-zn)     1                                 |
//| 0       0       zn*zf/(zn-zf)  0                                 |
//+------------------------------------------------------------------+
void DXMatrixPerspectiveLH(DXMatrix &pout,float w,float h,float zn,float zf)
  {
   DXMatrixIdentity(pout);
   pout.m[0][0] = 2.0f*zn/w;
   pout.m[1][1] = 2.0f*zn/h;
   pout.m[2][2] = zf/(zf - zn);
   pout.m[3][2] = (zn*zf)/(zn - zf);
   pout.m[2][3] = 1.0f;
   pout.m[3][3] = 0.0f;
  }
//+------------------------------------------------------------------+
//| Builds a customized, left-handed perspective projection matrix.  |
//+------------------------------------------------------------------+
//| All the parameters of the DXMatrixPerspectiveOffCenterLH         |
//| function are distances in camera space. The parameters describe  |
//| the dimensions of the view volume.                               |
//|                                                                  |
//| This function uses the following formula to compute              |
//| the returned matrix.                                             |
//| 2*zn/(r-l)   0            0              0                       |
//| 0            2*zn/(t-b)   0              0                       |
//| (l+r)/(l-r)  (t+b)/(b-t)  zf/(zf-zn)     1                       |
//| 0            0            zn*zf/(zn-zf)  0                       |
//+------------------------------------------------------------------+
void DXMatrixPerspectiveOffCenterLH(DXMatrix &pout,float l,float r,float b,float t,float zn,float zf)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 2.0f*zn/(r - l);
   pout.m[1][1] = -2.0f*zn/(b - t);
   pout.m[2][0] = -1.0f - 2.0f*l/(r - l);
   pout.m[2][1] = 1.0f + 2.0f*t/(b - t);
   pout.m[2][2] = - zf/(zn - zf);
   pout.m[3][2] = (zn*zf)/(zn -zf);
   pout.m[2][3] = 1.0f;
   pout.m[3][3] = 0.0f;
  }
//+------------------------------------------------------------------+
//| Builds a customized, right-handed perspective projection matrix. |
//+------------------------------------------------------------------+
//| All the parameters of the DXMatrixPerspectiveOffCenterRH         |
//| function are distances in camera space. The parameters describe  |
//| the dimensions of the view volume.                               |
//|                                                                  |
//| This function uses the following formula to compute              |
//| the returned matrix.                                             |
//| 2*zn/(r-l)   0            0                0                     |
//| 0            2*zn/(t-b)   0                0                     |
//| (l+r)/(r-l)  (t+b)/(t-b)  zf/(zn-zf)      -1                     |
//| 0            0            zn*zf/(zn-zf)    0                     |
//+------------------------------------------------------------------+
void DXMatrixPerspectiveOffCenterRH(DXMatrix &pout,float l,float r,float b,float t,float zn,float zf)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 2.0f*zn/(r - l);
   pout.m[1][1] = -2.0f*zn/(b - t);
   pout.m[2][0] = 1.0f + 2.0f*l/(r - l);
   pout.m[2][1] = -1.0f -2.0f*t/(b - t);
   pout.m[2][2] = zf/(zn - zf);
   pout.m[3][2] = (zn*zf)/(zn -zf);
   pout.m[2][3] = -1.0f;
   pout.m[3][3] = 0.0f;
  }
//+------------------------------------------------------------------+
//| Builds a right-handed perspective projection matrix.             |
//+------------------------------------------------------------------+
//| All the parameters of the DXMatrixPerspectiveRH function         |
//| are distances in camera space. The parameters describe           |
//| the dimensions of the view volume.                               |
//|                                                                  |
//| This function uses the following formula to compute              |
//| the returned matrix.                                             |
//| 2*zn/w  0       0              0                                 |
//| 0       2*zn/h  0              0                                 |
//| 0       0       zf/(zn-zf)    -1                                 |
//| 0       0       zn*zf/(zn-zf)  0                                 |
//+------------------------------------------------------------------+
void DXMatrixPerspectiveRH(DXMatrix &pout,float w,float h,float zn,float zf)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 2.0f*zn/w;
   pout.m[1][1] = 2.0f*zn/h;
   pout.m[2][2] = zf/(zn - zf);
   pout.m[3][2] = (zn*zf)/(zn - zf);
   pout.m[2][3] = -1.0f;
   pout.m[3][3] = 0.0f;
  }
//+------------------------------------------------------------------+
//| Builds a matrix that reflects the coordinate system about a plane|
//| This function normalizes the plane equation before it creates    |
//| the reflected matrix.                                            |
//|                                                                  |
//| This function uses the following formula to compute              |
//| the returned matrix.                                             |
//| P = normalize(Plane);                                            |
//| -2 * P.a * P.a + 1  -2 * P.b * P.a      -2 * P.c * P.a        0  |
//| -2 * P.a * P.b      -2 * P.b * P.b + 1  -2 * P.c * P.b        0  |
//| -2 * P.a * P.c      -2 * P.b * P.c      -2 * P.c * P.c + 1    0  |
//| -2 * P.a * P.d      -2 * P.b * P.d      -2 * P.c * P.d        1  |
//+------------------------------------------------------------------+
void DXMatrixReflect(DXMatrix &pout,const DXPlane &pplane)
  {
   DXPlane Nplane;
   DXPlaneNormalize(Nplane,pplane);
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 1.0f - 2.0f*Nplane.a*Nplane.a;
   pout.m[0][1] = -2.0f*Nplane.a*Nplane.b;
   pout.m[0][2] = -2.0f*Nplane.a*Nplane.c;
   pout.m[1][0] = -2.0f*Nplane.a*Nplane.b;
   pout.m[1][1] = 1.0f - 2.0f*Nplane.b*Nplane.b;
   pout.m[1][2] = -2.0f*Nplane.b*Nplane.c;
   pout.m[2][0] = -2.0f*Nplane.c*Nplane.a;
   pout.m[2][1] = -2.0f*Nplane.c*Nplane.b;
   pout.m[2][2] = 1.0f - 2.0f*Nplane.c*Nplane.c;
   pout.m[3][0] = -2.0f*Nplane.d*Nplane.a;
   pout.m[3][1] = -2.0f*Nplane.d*Nplane.b;
   pout.m[3][2] = -2.0f*Nplane.d*Nplane.c;
  }
//+------------------------------------------------------------------+
//| Builds a matrix that rotates around an arbitrary axis.           |
//+------------------------------------------------------------------+
void DXMatrixRotationAxis(DXMatrix &out,const DXVector3 &v,float angle)
  {
   DXVector3 nv;
   DXVec3Normalize(nv,v);
//---
   float sangle = (float)sin(angle);
   float cangle = (float)cos(angle);
   float cdiff = 1.0f - cangle;
//---
   out.m[0][0] = cdiff*nv.x*nv.x + cangle;
   out.m[1][0] = cdiff*nv.x*nv.y - sangle*nv.z;
   out.m[2][0] = cdiff*nv.x*nv.z + sangle*nv.y;
   out.m[3][0] = 0.0f;
   out.m[0][1] = cdiff*nv.y*nv.x + sangle*nv.z;
   out.m[1][1] = cdiff*nv.y*nv.y + cangle;
   out.m[2][1] = cdiff*nv.y*nv.z - sangle*nv.x;
   out.m[3][1] = 0.0f;
   out.m[0][2] = cdiff*nv.z*nv.x - sangle*nv.y;
   out.m[1][2] = cdiff*nv.z*nv.y + sangle*nv.x;
   out.m[2][2] = cdiff*nv.z*nv.z + cangle;
   out.m[3][2] = 0.0f;
   out.m[0][3] = 0.0f;
   out.m[1][3] = 0.0f;
   out.m[2][3] = 0.0f;
   out.m[3][3] = 1.0f;
  }
//+------------------------------------------------------------------+
//| Builds a rotation matrix from a quaternion.                      |
//+------------------------------------------------------------------+
void DXMatrixRotationQuaternion(DXMatrix &pout,const DXQuaternion &pq)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = 1.0f - 2.0f*(pq.y*pq.y + pq.z*pq.z);
   pout.m[0][1] = 2.0f*(pq.x*pq.y + pq.z*pq.w);
   pout.m[0][2] = 2.0f*(pq.x*pq.z - pq.y*pq.w);
   pout.m[1][0] = 2.0f*(pq.x*pq.y - pq.z*pq.w);
   pout.m[1][1] = 1.0f - 2.0f * (pq.x*pq.x + pq.z*pq.z);
   pout.m[1][2] = 2.0f*(pq.y*pq.z + pq.x*pq.w);
   pout.m[2][0] = 2.0f*(pq.x*pq.z + pq.y*pq.w);
   pout.m[2][1] = 2.0f*(pq.y*pq.z - pq.x*pq.w);
   pout.m[2][2] = 1.0f - 2.0f*(pq.x*pq.x + pq.y*pq.y);
  }
//+------------------------------------------------------------------+
//| Builds a matrix that rotates around the x-axis.                  |
//+------------------------------------------------------------------+
void DXMatrixRotationX(DXMatrix &pout,float angle)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[1][1] = (float)cos(angle);
   pout.m[2][2] = (float)cos(angle);
   pout.m[1][2] = (float)sin(angle);
   pout.m[2][1] = -(float)sin(angle);
  }
//+------------------------------------------------------------------+
//| Builds a matrix that rotates around the y-axis.                  |
//+------------------------------------------------------------------+
void DXMatrixRotationY(DXMatrix &pout,float angle)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = (float)cos(angle);
   pout.m[2][2] = (float)cos(angle);
   pout.m[0][2] = -(float)sin(angle);
   pout.m[2][0] = (float)sin(angle);
  }
//+------------------------------------------------------------------+
//| Builds a matrix with a specified yaw, pitch, and roll.           |
//+------------------------------------------------------------------+
//| The order of transformations is roll first, then pitch, then yaw.|
//| Relative to the object's local coordinate axis, this is          |
//| equivalent to rotation around the z-axis, followed by rotation   |
//| around the x-axis, followed by rotation around the y-axis.       |
//+------------------------------------------------------------------+
void DXMatrixRotationYawPitchRoll(DXMatrix &out,float yaw,float pitch,float roll)
  {
   float sroll = (float)sin(roll);
   float croll = (float)cos(roll);
   float spitch = (float)sin(pitch);
   float cpitch = (float)cos(pitch);
   float syaw = (float)sin(yaw);
   float cyaw = (float)cos(yaw);
//---
   out.m[0][0] = sroll * spitch * syaw + croll * cyaw;
   out.m[0][1] = sroll * cpitch;
   out.m[0][2] = sroll * spitch * cyaw - croll * syaw;
   out.m[0][3] = 0.0f;
   out.m[1][0] = croll * spitch * syaw - sroll * cyaw;
   out.m[1][1] = croll * cpitch;
   out.m[1][2] = croll * spitch * cyaw + sroll * syaw;
   out.m[1][3] = 0.0f;
   out.m[2][0] = cpitch * syaw;
   out.m[2][1] = -spitch;
   out.m[2][2] = cpitch * cyaw;
   out.m[2][3] = 0.0f;
   out.m[3][0] = 0.0f;
   out.m[3][1] = 0.0f;
   out.m[3][2] = 0.0f;
   out.m[3][3] = 1.0f;
  }
//+------------------------------------------------------------------+
//| Builds a matrix that rotates around the z-axis.                  |
//+------------------------------------------------------------------+
void DXMatrixRotationZ(DXMatrix &pout,float angle)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[0][0] = (float)cos(angle);
   pout.m[1][1] = (float)cos(angle);
   pout.m[0][1] = (float)sin(angle);
   pout.m[1][0] = -(float)sin(angle);
  }
//+------------------------------------------------------------------+
//| Builds a matrix that scales along the x-axis,                    |
//| the y-axis,and the z-axis.                                       |
//+------------------------------------------------------------------+
void DXMatrixScaling(DXMatrix &pout,float sx,float sy,float sz)
  {
   DXMatrixIdentity(pout);
   pout.m[0][0] = sx;
   pout.m[1][1] = sy;
   pout.m[2][2] = sz;
  }
//+------------------------------------------------------------------+
//| Builds a matrix that flattens geometry into a plane.             |
//+------------------------------------------------------------------+
//| The DXMatrixShadow function flattens geometry into a plane, as   |
//| if casting a shadow from a light.                                |
//| This function uses the following formula to compute the returned |
//| matrix.                                                          |
//|                                                                  |
//| P = normalize(Plane);                                            |
//| L = Light;                                                       |
//| d = -dot(P,L)                                                    |
//|                                                                  |
//| P.a * L.x + d  P.a * L.y      P.a * L.z      P.a * L.w           |
//| P.b * L.x      P.b * L.y + d  P.b * L.z      P.b * L.w           |
//| P.c * L.x      P.c * L.y      P.c * L.z + d  P.c * L.w           |
//| P.d * L.x      P.d * L.y      P.d * L.z      P.d * L.w + d       |
//|                                                                  |
//| If the light's w-component is 0, the ray from the origin to the  |
//| light represents a directional light. If it is 1,the light is    |
//| a point light.                                                   |
//+------------------------------------------------------------------+
void DXMatrixShadow(DXMatrix &pout,const DXVector4 &plight,const DXPlane &pplane)
  {
   DXPlane Nplane;
   DXPlaneNormalize(Nplane,pplane);
   float dot = DXPlaneDot(Nplane,plight);
//---
   pout.m[0][0] = dot - Nplane.a*plight.x;
   pout.m[0][1] = -Nplane.a*plight.y;
   pout.m[0][2] = -Nplane.a*plight.z;
   pout.m[0][3] = -Nplane.a*plight.w;
   pout.m[1][0] = -Nplane.b*plight.x;
   pout.m[1][1] = dot - Nplane.b*plight.y;
   pout.m[1][2] = -Nplane.b*plight.z;
   pout.m[1][3] = -Nplane.b*plight.w;
   pout.m[2][0] = -Nplane.c*plight.x;
   pout.m[2][1] = -Nplane.c*plight.y;
   pout.m[2][2] = dot - Nplane.c*plight.z;
   pout.m[2][3] = -Nplane.c*plight.w;
   pout.m[3][0] = -Nplane.d*plight.x;
   pout.m[3][1] = -Nplane.d*plight.y;
   pout.m[3][2] = -Nplane.d*plight.z;
   pout.m[3][3] = dot - Nplane.d*plight.w;
  }
//+------------------------------------------------------------------+
//| Builds a transformation matrix.                                  |
//+------------------------------------------------------------------+
//| This function calculates the transformation matrix with the      |
//| following formula, with matrix concatenation evaluated           |
//| in left-to-right order:                                          |
//|                                                                  |
//| Mout = (Msc)^(-1)*(Msr)^(-1)*Ms*Msr*Msc*(Mrc)^(-1)*Mr*Mrc*Mt     |
//|                                                                  |
//| where:                                                           |
//| Mout = output matrix (pOut)                                      |
//| Msc = scaling center matrix (pScalingCenter)                     |
//| Msr = scaling rotation matrix (pScalingRotation)                 |
//| Ms = scaling matrix (pScaling)                                   |
//| Mrc = center of rotation matrix (pRotationCenter)                |
//| Mr = rotation matrix (pRotation)                                 |
//| Mt = translation matrix (pTranslation)                           |
//+------------------------------------------------------------------+
void DXMatrixTransformation(DXMatrix &pout,const DXVector3 &pscalingcenter,const DXQuaternion &pscalingrotation,const DXVector3 &pscaling,const DXVector3 &protationcenter,const DXQuaternion &protation,const DXVector3 &ptranslation)
  {
   DXMatrix m1,m2,m3,m4,m5,m6,m7;
   DXQuaternion prc;
   DXVector3 psc,pt;
//--- pscalingcenter
   psc.x = pscalingcenter.x;
   psc.y = pscalingcenter.y;
   psc.z = pscalingcenter.z;
//--- protationcenter
   prc.x = protationcenter.x;
   prc.y = protationcenter.y;
   prc.z = protationcenter.z;
//--- ptranslation
   pt.x = ptranslation.x;
   pt.y = ptranslation.y;
   pt.z = ptranslation.z;
   DXMatrixTranslation(m1,-psc.x,-psc.y,-psc.z);
//---
   DXQuaternion temp;
   DXMatrixRotationQuaternion(m4,pscalingrotation);
   temp.w =  pscalingrotation.w;
   temp.x = -pscalingrotation.x;
   temp.y = -pscalingrotation.y;
   temp.z = -pscalingrotation.z;
   DXMatrixRotationQuaternion(m2,temp);
//--- pscaling
   DXMatrixScaling(m3,pscaling.x,pscaling.y,pscaling.z);
//--- protation
   DXMatrixRotationQuaternion(m6,protation);
//---
   DXMatrixTranslation(m5,psc.x - prc.x,psc.y - prc.y,psc.z - prc.z);
   DXMatrixTranslation(m7,prc.x + pt.x,prc.y + pt.y,prc.z + pt.z);
   DXMatrixMultiply(m1,m1,m2);
   DXMatrixMultiply(m1,m1,m3);
   DXMatrixMultiply(m1,m1,m4);
   DXMatrixMultiply(m1,m1,m5);
   DXMatrixMultiply(m1,m1,m6);
   DXMatrixMultiply(pout,m1,m7);
  }
//+------------------------------------------------------------------+
//| Builds a 2D transformation matrix that represents                |
//| transformations in the xy plane.                                 |
//+------------------------------------------------------------------+
//| This function calculates the transformation matrix with the      |
//| following formula, with matrix concatenation evaluated           |
//| in left-to-right order:                                          |
//|                                                                  |
//| Mout = (Msc)^(-1)*(Msr)^(-1)*Ms*Msr*Msc*(Mrc)^(-1)*Mr*Mrc*Mt     |
//|                                                                  |
//| where:                                                           |
//| Mout = output matrix (pOut)                                      |
//| Msc = scaling center matrix (pScalingCenter)                     |
//| Msr = scaling rotation matrix (pScalingRotation)                 |
//| Ms = scaling matrix (pScaling)                                   |
//| Mrc = center of rotation matrix (pRotationCenter)                |
//| Mr = rotation matrix (Rotation)                                  |
//| Mt = translation matrix (pTranslation)                           |
//+------------------------------------------------------------------+
void DXMatrixTransformation2D(DXMatrix &pout,const DXVector2 &pscalingcenter,float scalingrotation,const DXVector2 &pscaling,const DXVector2 &protationcenter,float rotation,const DXVector2 &ptranslation)
  {
   DXQuaternion rot,sca_rot;
   DXVector3 rot_center,sca,sca_center,trans;
//--- pscalingcenter
   sca_center.x=pscalingcenter.x;
   sca_center.y=pscalingcenter.y;
   sca_center.z=0.0f;
//--- pscaling
   sca.x=pscaling.x;
   sca.y=pscaling.y;
   sca.z=1.0f;
//--- protationcenter
   rot_center.x=protationcenter.x;
   rot_center.y=protationcenter.y;
   rot_center.z=0.0f;
//--- ptranslation
   trans.x=ptranslation.x;
   trans.y=ptranslation.y;
   trans.z=0.0f;
//---
   rot.w=(float)cos(rotation/2.0f);
   rot.x=0.0f;
   rot.y=0.0f;
   rot.z=(float)sin(rotation/2.0f);
//---
   sca_rot.w=(float)cos(scalingrotation/2.0f);
   sca_rot.x=0.0f;
   sca_rot.y=0.0f;
   sca_rot.z=(float)sin(scalingrotation/2.0f);
   DXMatrixTransformation(pout,sca_center,sca_rot,sca,rot_center,rot,trans);
  }
//+------------------------------------------------------------------+
//| Builds a matrix using the specified offsets.                     |
//+------------------------------------------------------------------+
void DXMatrixTranslation(DXMatrix &pout,float x,float y,float z)
  {
   DXMatrixIdentity(pout);
//---
   pout.m[3][0] = x;
   pout.m[3][1] = y;
   pout.m[3][2] = z;
  }
//+------------------------------------------------------------------+
//| Returns the matrix transpose of a matrix.                        |
//+------------------------------------------------------------------+
void DXMatrixTranspose(DXMatrix &pout,const DXMatrix &pm)
  {
   const DXMatrix m = pm;
   for(int i=0; i<4; i++)
      for(int j=0; j<4; j++)
         pout.m[i][j] = m.m[j][i];
  }
//+------------------------------------------------------------------+
//| Computes the dot product of a plane and a 4D vector.             |
//+------------------------------------------------------------------+
float DXPlaneDot(const DXPlane &p1,const DXVector4 &p2)
  {
   return(p1.a*p2.x + p1.b*p2.y + p1.c*p2.z + p1.d*p2.w);
  }
//+------------------------------------------------------------------+
//| Computes the dot product of a plane and a 3D vector.             |
//| The w parameter of the vector is assumed to be 1.                |
//+------------------------------------------------------------------+
float DXPlaneDotCoord(const DXPlane &pp,const DXVector4 &pv)
  {
   return(pp.a*pv.x + pp.b*pv.y + pp.c*pv.z + pp.d);
  }
//+------------------------------------------------------------------+
//| Computes the dot product of a plane and a 3D vector.             |
//| The w parameter of the vector is assumed to be 0.                |
//+------------------------------------------------------------------+
float DXPlaneDotNormal(const DXPlane &pp,const DXVector4 &pv)
  {
   return(pp.a*pv.x + pp.b*pv.y + pp.c*pv.z);
  }
//+------------------------------------------------------------------+
//| Constructs a plane from a point and a normal.                    |
//+------------------------------------------------------------------+
void DXPlaneFromPointNormal(DXPlane &pout,const DXVector3 &pvpoint,const DXVector3 &pvnormal)
  {
   pout.a = pvnormal.x;
   pout.b = pvnormal.y;
   pout.c = pvnormal.z;
   pout.d = -DXVec3Dot(pvpoint,pvnormal);
  }
//+------------------------------------------------------------------+
//| Constructs a plane from three points.                            |
//+------------------------------------------------------------------+
void DXPlaneFromPoints(DXPlane &pout,const DXVector3 &pv1,const DXVector3 &pv2,const DXVector3 &pv3)
  {
   DXVector3 edge1,edge2,normal,Nnormal;
//---
   edge1.x = 0.0f;
   edge1.y = 0.0f;
   edge1.z = 0.0f;
   edge2.x = 0.0f;
   edge2.y = 0.0f;
   edge2.z = 0.0f;
//---
   DXVec3Subtract(edge1,pv2,pv1);
   DXVec3Subtract(edge2,pv3,pv1);
   DXVec3Cross(normal,edge1,edge2);
   DXVec3Normalize(Nnormal,normal);
   DXPlaneFromPointNormal(pout,pv1,Nnormal);
  }
//+------------------------------------------------------------------+
//| Finds the intersection between a plane and a line.               |
//| If the line is parallel to the plane, null vector is returned.   |
//+------------------------------------------------------------------+
void DXPlaneIntersectLine(DXVector3 &pout,const DXPlane &pp,const DXVector3 &pv1,const DXVector3 &pv2)
  {
   DXVector3 direction,normal;
   normal.x = pp.a;
   normal.y = pp.b;
   normal.z = pp.c;
   direction.x = pv2.x - pv1.x;
   direction.y = pv2.y - pv1.y;
   direction.z = pv2.z - pv1.z;
//---
   float dot = DXVec3Dot(normal,direction);
   if(!dot)
     {
      pout.x=0.0f;
      pout.y=0.0f;
      pout.z=0.0f;
     }
   float temp = (pp.d + DXVec3Dot(normal,pv1))/dot;
   pout.x = pv1.x - temp*direction.x;
   pout.y = pv1.y - temp*direction.y;
   pout.z = pv1.z - temp*direction.z;
  }
//+------------------------------------------------------------------+
//| Normalizes the plane coefficients so that the plane normal       |
//| has unit length.                                                 |
//| This function normalizes a plane so that |a,b,c| == 1.           |
//+------------------------------------------------------------------+
void DXPlaneNormalize(DXPlane &out,const DXPlane &p)
  {
   float norm = (float)sqrt(p.a*p.a + p.b*p.b + p.c*p.c);
   if(norm)
     {
      out.a = p.a/norm;
      out.b = p.b/norm;
      out.c = p.c/norm;
      out.d = p.d/norm;
     }
   else
     {
      out.a = 0.0f;
      out.b = 0.0f;
      out.c = 0.0f;
      out.d = 0.0f;
     }
  }
//+------------------------------------------------------------------+
//| Scale the plane with the given scaling factor.                   |
//+------------------------------------------------------------------+
void DXPlaneScale(DXPlane &pout,const DXPlane &p,float s)
  {
   pout.a = p.a*s;
   pout.b = p.b*s;
   pout.c = p.c*s;
   pout.d = p.d*s;
  };
//+------------------------------------------------------------------+
//| Transforms a plane by a matrix.                                  |
//| The input matrix is the inverse transpose of the actual          |
//| transformation.                                                  |
//+------------------------------------------------------------------+
void DXPlaneTransform(DXPlane &pout,const DXPlane &pplane,const DXMatrix &pm)
  {
   DXPlane plane = pplane;
//---
   pout.a = pm.m[0][0]*plane.a + pm.m[1][0]*plane.b + pm.m[2][0]*plane.c + pm.m[3][0]*plane.d;
   pout.b = pm.m[0][1]*plane.a + pm.m[1][1]*plane.b + pm.m[2][1]*plane.c + pm.m[3][1]*plane.d;
   pout.c = pm.m[0][2]*plane.a + pm.m[1][2]*plane.b + pm.m[2][2]*plane.c + pm.m[3][2]*plane.d;
   pout.d = pm.m[0][3]*plane.a + pm.m[1][3]*plane.b + pm.m[2][3]*plane.c + pm.m[3][3]*plane.d;
  }
//+------------------------------------------------------------------+
//| Adds two spherical harmonic (SH) vectors; in other words,        |
//| out[i] = a[i] + b[i].                                            |
//+------------------------------------------------------------------+
//| Each coefficient of the basis function Y(l,m) is stored          |
//| at memory location l^2 + m + l,where:                            |
//| l is the degree of the basis function.                           |
//| m is the basis function index for the given l value              |
//|   and ranges from -l to l, inclusive.                            |
//+------------------------------------------------------------------+
void DXSHAdd(float &out[],int order,const float &a[],const float &b[])
  {
   for(int i = 0; i<order*order; i++)
      out[i] = a[i] + b[i];
  }
//+------------------------------------------------------------------+
//| Computes the dot product of two spherical harmonic (SH) vectors. |
//+------------------------------------------------------------------+
//| Each coefficient of the basis function Y(l,m) is stored          |
//| at memory location l^2 + m + l,where:                            |
//| l is the degree of the basis function.                           |
//| m is the basis function index for the given l value              |
//|   and ranges from -l to l,inclusive.                             |
//+------------------------------------------------------------------+
float DXSHDot(int order,const float &a[],const float &b[])
  {
   float s = a[0]*b[0];
   for(int i = 1; i < order*order; i++)
      s += a[i]*b[i];
//---
   return(s);
  }
//+------------------------------------------------------------------+
//| weightedcapintegrale                                             |
//+------------------------------------------------------------------+
void weightedcapintegrale(float &out[],uint order,float angle)
  {
   float coeff[3];
   coeff[0] = (float)cos(angle);

   out[0] = 2.0f*DX_PI*(1.0f - coeff[0]);
   out[1] = DX_PI*(float)sin(angle)*(float)sin(angle);
   if(order <= 2)
      return;

   out[2] = coeff[0]*out[1];
   if(order == 3)
      return;

   coeff[1] = coeff[0]*coeff[0];
   coeff[2] = coeff[1]*coeff[1];

   out[3] = DX_PI*(-1.25f*coeff[2] + 1.5f*coeff[1] - 0.25f);
   if(order == 4)
      return;

   out[4] = -0.25f*DX_PI*coeff[0]*(7.0f*coeff[2] - 10.0f*coeff[1] + 3.0f);
   if(order == 5)
      return;

   out[5] = DX_PI*(-2.625f*coeff[2]*coeff[1] + 4.375f*coeff[2] - 1.875f*coeff[1] + 0.125f);
  }
//+------------------------------------------------------------------+
//| Evaluates a light that is a cone of constant intensity           |
//| and returns spectral spherical harmonic (SH) data.               |
//+------------------------------------------------------------------+
//| Evaluates a light that is a cone of constant intensity and       |
//| returns spectral SH data.                                        |
//| The output vector is computed so that if the intensity ratio     |
//| R/G/B is equal to 1, the exit radiance of a point                |
//| directly under the light (oriented in the cone direction         |
//| on a diffuse object with an albedo of 1) would be 1.0.           |
//| This will compute three spectral samples;                        |
//| rout[], gout[] and bout[] will be computed.                      |
//+------------------------------------------------------------------+
int DXSHEvalConeLight(int order,const DXVector3 &dir,float radius,float Rintensity,float Gintensity,float Bintensity,float &rout[],float &gout[],float &bout[])
  {
   float cap[6];
//---
   if(radius <= 0.0f)
      return(DXSHEvalDirectionalLight(order,dir,Rintensity,Gintensity,Bintensity,rout,gout,bout));
//---
   float clamped_angle = (radius > DX_PI/2.0f) ? (DX_PI/2.0f) : radius;
   float norm = (float)sin(clamped_angle)*(float)sin(clamped_angle);
   if(order > DXSH_MAXORDER)
     {
      //--- order clamped at DXSH_MAXORDER
      order = DXSH_MAXORDER;
     }
//---
   weightedcapintegrale(cap,order,radius);
   DXSHEvalDirection(rout,order,dir);
//---
   for(int i = 0; i<order; i++)
     {
      float scale = cap[i]/norm;
      for(int j = 0; j<2*i + 1; j++)
        {
         int index = i*i + j;
         float temp = rout[index]*scale;
         rout[index] = temp*Rintensity;
         gout[index] = temp*Gintensity;
         bout[index] = temp*Bintensity;
        }
     }
   return(0);
  }
//+------------------------------------------------------------------+
//| Evaluates the spherical harmonic (SH) basis functions            |
//| from an input direction vector.                                  |
//+------------------------------------------------------------------+
//| Each coefficient of the basis function Y(l,m) is stored          |
//| at memory location l^2 + m + l, where:                           |
//| l is the degree of the basis function.                           |
//| m is the basis function index for the given l value              |
//|   and ranges from -l to l, inclusive.                            |
//+------------------------------------------------------------------+
void DXSHEvalDirection(float &out[],int order,const DXVector3 &dir)
  {
   const float dirxx = dir.x*dir.x;
   const float dirxy = dir.x*dir.y;
   const float dirxz = dir.x*dir.z;
   const float diryy = dir.y*dir.y;
   const float diryz = dir.y*dir.z;
   const float dirzz = dir.z*dir.z;
   const float dirxxxx = dirxx*dirxx;
   const float diryyyy = diryy*diryy;
   const float dirzzzz = dirzz*dirzz;
   const float dirxyxy = dirxy*dirxy;
//---
   if((order < DXSH_MINORDER) || (order > DXSH_MAXORDER))
      return;

   out[0] = 0.5f/(float)sqrt(DX_PI);
   out[1] = -0.5f/(float)sqrt(DX_PI/3.0f)*dir.y;
   out[2] = 0.5f/(float)sqrt(DX_PI/3.0f)*dir.z;
   out[3] = -0.5f/(float)sqrt(DX_PI/3.0f)*dir.x;
   if(order == 2)
      return;

   out[4] = 0.5f/(float)sqrt(DX_PI/15.0f)*dirxy;
   out[5] = -0.5f/(float)sqrt(DX_PI/15.0f)*diryz;
   out[6] = 0.25f/(float)sqrt(DX_PI/5.0f)*(3.0f*dirzz - 1.0f);
   out[7] = -0.5f/(float)sqrt(DX_PI/15.0f)*dirxz;
   out[8] = 0.25f/(float)sqrt(DX_PI/15.0f)*(dirxx - diryy);
   if(order == 3)
      return;

   out[9] = -(float)sqrt(70.0f/DX_PI)/8.0f*dir.y*(3.0f*dirxx - diryy);
   out[10] = (float)sqrt(105.0f/DX_PI)/2.0f*dirxy*dir.z;
   out[11] = -(float)sqrt(42.0f/DX_PI)/8.0f*dir.y*(-1.0f + 5.0f*dirzz);
   out[12] = (float)sqrt(7.0f/DX_PI)/4.0f*dir.z*(5.0f*dirzz - 3.0f);
   out[13] = (float)sqrt(42.0f/DX_PI)/8.0f*dir.x*(1.0f - 5.0f*dirzz);
   out[14] = (float)sqrt(105.0f/DX_PI)/4.0f*dir.z*(dirxx - diryy);
   out[15] = -(float)sqrt(70.0f/DX_PI)/8.0f*dir.x*(dirxx - 3.0f*diryy);
   if(order == 4)
      return;

   out[16] = 0.75f*float(sqrt(35.0f/DX_PI))*dirxy*(dirxx - diryy);
   out[17] = 3.0f*dir.z*out[9];
   out[18] = 0.75f*(float)sqrt(5.0f/DX_PI)*dirxy*(7.0f*dirzz - 1.0f);
   out[19] = 0.375f*(float)sqrt(10.0f/DX_PI)*diryz*(3.0f - 7.0f*dirzz);
   out[20] = 3.0f/(16.0f*(float)sqrt(DX_PI))*(35.0f*dirzzzz - 30.f*dirzz + 3.0f);
   out[21] = 0.375f*(float)sqrt(10.0f/DX_PI)*dirxz*(3.0f - 7.0f*dirzz);
   out[22] = 0.375f*(float)sqrt(5.0f/DX_PI)*(dirxx - diryy)*(7.0f*dirzz - 1.0f);
   out[23] = 3.0f*dir.z*out[15];
   out[24] = 3.0f / 16.0f*float(sqrt(35.0f/DX_PI))*(dirxxxx - 6.0f*dirxyxy + diryyyy);
   if(order == 5)
      return;

   out[25] = -3.0f/32.0f*(float)sqrt(154.0f/DX_PI)*dir.y*(5.0f*dirxxxx - 10.0f*dirxyxy + diryyyy);
   out[26] = 0.75f*(float)sqrt(385.0f/DX_PI)*dirxy*dir.z*(dirxx - diryy);
   out[27] = (float)sqrt(770.0f/DX_PI)/32.0f*dir.y*(3.0f*dirxx - diryy)*(1.0f - 9.0f*dirzz);
   out[28] = (float)sqrt(1155.0f/DX_PI)/4.0f*dirxy*dir.z*(3.0f*dirzz - 1.0f);
   out[29] = (float)sqrt(165.0f/DX_PI)/16.0f*dir.y*(14.0f*dirzz - 21.0f*dirzzzz - 1.0f);
   out[30] = (float)sqrt(11.0f/DX_PI)/16.0f*dir.z*(63.0f*dirzzzz - 70.0f*dirzz + 15.0f);
   out[31] = (float)sqrt(165.0f/DX_PI)/16.0f*dir.x*(14.0f*dirzz - 21.0f*dirzzzz - 1.0f);
   out[32] = (float)sqrt(1155.0f/DX_PI)/8.0f*dir.z*(dirxx - diryy)*(3.0f*dirzz - 1.0f);
   out[33] = (float)sqrt(770.0f/DX_PI)/32.0f*dir.x*(dirxx - 3.0f*diryy)*(1.0f - 9.0f*dirzz);
   out[34] = 3.0f/16.0f*(float)sqrt(385.0f/DX_PI)*dir.z*(dirxxxx - 6.0f*dirxyxy + diryyyy);
   out[35] = -3.0f/32.0f*(float)sqrt(154.0f/DX_PI)*dir.x*(dirxxxx - 10.0f*dirxyxy + 5.0f*diryyyy);
  }
//+------------------------------------------------------------------+
//| Evaluates a directional light and                                |
//| returns spectral spherical harmonic (SH) data.                   |
//+------------------------------------------------------------------+
//| The output vector is computed so that if the intensity ratio     |
//| R/G/B is equal to 1 ,the resulting exit radiance of a point      |
//| directly under the light on a diffuse object with an albedo      |
//| of 1 would be 1.0. This will compute three spectral samples;     |
//| rout[], gout[] and bout[] will be returned.                      |
//+------------------------------------------------------------------+
int DXSHEvalDirectionalLight(int order,const DXVector3 &dir,float Rintensity,float Gintensity,float Bintensity,float &rout[],float &gout[],float &bout[])
  {
   float s = 0.75f;
   if(order > 2)
      s += 5.0f/16.0f;
   if(order > 4)
      s -= 3.0f/32.0f;
   s /= DX_PI;

   DXSHEvalDirection(rout,order,dir);
   for(int j=0; j<order*order; j++)
     {
      float temp = rout[j]/s;
      rout[j] = Rintensity*temp;
      gout[j] = Gintensity*temp;
      bout[j] = Bintensity*temp;
     }
//---
   return(0);
  }
//+------------------------------------------------------------------+
//| Evaluates a light that is a linear interpolation                 |
//| between two colors over the sphere.                              |
//+------------------------------------------------------------------+
//| The interpolation is done linearly between the two points,       |
//| not over the surface of the sphere (that is, if the axis was     |
//| (0,0,1) it is linear in Z, not in the azimuthal angle).          |
//| The resulting spherical lighting function is normalized so that  |
//| a point on a perfectly diffuse surface with no shadowing         |
//| and a normal pointed in the direction pDir would result in exit  |
//| radiance with a value of 1 (if the top color was white           |
//| and the bottom color was black). This is a very simple model     |
//| where Top represents the intensity of the "sky"                  |
//| and Bottom represents the intensity of the "ground".             |
//+------------------------------------------------------------------+
int DXSHEvalHemisphereLight(int order,const DXVector3 &dir,DXColor &top,DXColor &bottom,float &rout[],float &gout[],float &bout[])
  {
   float a[2],temp[4];
   DXSHEvalDirection(temp,2,dir);
//--- rout
   a[0] = (top.r + bottom.r)*3.0f*DX_PI;
   a[1] = (top.r - bottom.r)*DX_PI;
   for(int i=0; i<order; i++)
      for(int j=0; j<2*i + 1; j++)
         if(i < 2)
            rout[i*i + j] = temp[i*i + j]*a[i];
         else
            rout[i*i + j] = 0.0f;
//--- gout
   a[0] = (top.g + bottom.g)*3.0f*DX_PI;
   a[1] = (top.g - bottom.g)*DX_PI;
   for(int i = 0; i < order; i++)
      for(int j=0; j<2*i+1; j++)
         if(i < 2)
            gout[i*i + j] = temp[i*i + j]*a[i];
         else
            gout[i*i + j] = 0.0f;
//--- bout
   a[0] = (top.b + bottom.b)*3.0f*DX_PI;
   a[1] = (top.b - bottom.b)*DX_PI;
   for(int i=0; i<order; i++)
      for(int j=0; j<2*i + 1; j++)
         if(i < 2)
            bout[i*i + j] = temp[i*i + j]*a[i];
         else
            bout[i*i + j] = 0.0f;
//---
   return(0);
  }
//+------------------------------------------------------------------+
//| Evaluates a spherical light and returns                          |
//| spectral spherical harmonic (SH) data.                           |
//+------------------------------------------------------------------+
//| There is no normalization of the intensity of the light like     |
//| there is for directional lights, so care has to be taken when    |
//| specifying the intensities.                                      |
//| This will compute three spectral samples;                        |
//| rout[], gout[], bout[] will be returned.                         |
//+------------------------------------------------------------------+
int DXSHEvalSphericalLight(int order,const DXVector3 &dir,float radius,float Rintensity,float Gintensity,float Bintensity,float &rout[],float &gout[],float &bout[])
  {
   DXVector3 normal;
   float cap[6];
//--- check order
   if(order > DXSH_MAXORDER)
      order = DXSH_MAXORDER;
//--- check radius
   if(radius < 0.0f)
      radius = -radius;

   float dist = DXVec3Length(dir);
   float clamped_angle = (dist <= radius) ? DX_PI/2.0f : (float)asin(radius / dist);

   weightedcapintegrale(cap,order,clamped_angle);
   DXVec3Normalize(normal,dir);
   DXSHEvalDirection(rout,order,normal);

   for(int i=0; i<order; i++)
      for(int j=0; j<2*i+1; j++)
        {
         int index = i*i + j;
         float temp = rout[index]*cap[i];
         rout[index] = temp*Rintensity;
         gout[index] = temp*Gintensity;
         bout[index] = temp*Bintensity;
        }
//---
   return(0);
  }
//+------------------------------------------------------------------+
//| Computes the product of two functions represented                |
//| using Spherical Harmonics (f and g).                             |
//+------------------------------------------------------------------+
//| The order is a number between 2 and 6 inclusive.                 |
//| So it's the same for the several functions:                      |
//| DXSHMultiply2, DXSHMultiply3, ... DXSHMultiply6.                 |
//|                                                                  |
//| Computes the product of two functions represented                |
//| using SH (f and g),where                                         |
//|               out[i] = int(y_i(s) * f(s) * g(s)),                |
//| where                                                            |
//|       y_i(s) is the ith SH basis function,                       |
//|       f(s) and g(s) are SH functions (sum_i(y_i(s)*c_i)).        |
//| The order determines the lengths of the arrays, where there      |
//| should always be l^2 coefficients.                               |
//|                                                                  |
//| In general the product of two SH functions of order l generates  |
//| an SH function of order 2*l - 1, but the results are truncated.  |
//|                                                                  |
//| This means that the product commutes (f*g == g*f)                |
//| but doesn't associate (f*(g*h) != (f*g)*h.                       |
//+------------------------------------------------------------------+
void DXSHMultiply2(float &out[],const float &a[],const float &b[])
  {
   float ta = 0.28209479f*a[0];
   float tb = 0.28209479f*b[0];
   out[0] = 0.28209479f*DXSHDot(2,a,b);
   out[1] = ta*b[1] + tb*a[1];
   out[2] = ta*b[2] + tb*a[2];
   out[3] = ta*b[3] + tb*a[3];
  }
//+------------------------------------------------------------------+
//| Computes the product of two functions represented using          |
//| Spherical Harmonics (f and g). Both functions are of order N=3.  |
//+------------------------------------------------------------------+
//| The order is a number between 2 and 6 inclusive.                 |
//| So it's the same for the several functions:                      |
//| DXSHMultiply2, DXSHMultiply3, ... DXSHMultiply6.                 |
//|                                                                  |
//| Computes the product of two functions represented                |
//| using SH (f and g),where                                         |
//|               out[i] = int(y_i(s) * f(s) * g(s)),                |
//| where                                                            |
//|       y_i(s) is the ith SH basis function,                       |
//|       f(s) and g(s) are SH functions (sum_i(y_i(s)*c_i)).        |
//| The order determines the lengths of the arrays,where there       |
//| should always be l^2 coefficients.                               |
//|                                                                  |
//| In general the product of two SH functions of order l generates  |
//| an SH function of order 2*l - 1, but the results are truncated.  |
//|                                                                  |
//| This means that the product commutes (f*g == g*f)                |
//| but doesn't associate (f*(g*h) != (f*g)*h.                       |
//+------------------------------------------------------------------+
void DXSHMultiply3(float &out[],const float &a[],const float &b[])
  {
   out[0] = 0.28209479f*a[0]*b[0];
   float ta = 0.28209479f*a[0] - 0.12615663f*a[6] - 0.21850969f*a[8];
   float tb = 0.28209479f*b[0] - 0.12615663f*b[6] - 0.21850969f*b[8];
   out[1] = ta*b[1] + tb*a[1];
   float t = a[1]*b[1];
   out[0] += 0.28209479f*t;
   out[6] = -0.12615663f*t;
   out[8] = -0.21850969f*t;

   ta = 0.21850969f*a[5];
   tb = 0.21850969f*b[5];
   out[1] += ta*b[2] + tb*a[2];
   out[2] = ta*b[1] + tb*a[1];
   t = a[1]*b[2] +a[2]*b[1];
   out[5] = 0.21850969f*t;

   ta = 0.21850969f*a[4];
   tb = 0.21850969f*b[4];
   out[1] += ta*b[3] + tb*a[3];
   out[3]  = ta*b[1] + tb*a[1];
   t = a[1]*b[3] + a[3]*b[1];
   out[4] = 0.21850969f*t;

   ta = 0.28209480f*a[0] + 0.25231326f*a[6];
   tb = 0.28209480f*b[0] + 0.25231326f*b[6];
   out[2] += ta*b[2] + tb*a[2];
   t = a[2]*b[2];
   out[0] += 0.28209480f*t;
   out[6] += 0.25231326f*t;

   ta = 0.21850969f*a[7];
   tb = 0.21850969f*b[7];
   out[2] += ta*b[3] + tb*a[3];
   out[3] += ta*b[2] + tb*a[2];
   t = a[2]*b[3] + a[3]*b[2];
   out[7] = 0.21850969f*t;

   ta = 0.28209479f*a[0] - 0.12615663f*a[6] + 0.21850969f*a[8];
   tb = 0.28209479f*b[0] - 0.12615663f*b[6] + 0.21850969f*b[8];
   out[3] += ta*b[3] + tb*a[3];
   t = a[3]*b[3];
   out[0] += 0.28209479f*t;
   out[6] -= 0.12615663f*t;
   out[8] += 0.21850969f*t;

   ta = 0.28209479f*a[0] - 0.18022375f*a[6];
   tb = 0.28209479f*b[0] - 0.18022375f*b[6];
   out[4] += ta*b[4] + tb*a[4];
   t = a[4]*b[4];
   out[0] += 0.28209479f*t;
   out[6] -= 0.18022375f*t;

   ta = 0.15607835f*a[7];
   tb = 0.15607835f*b[7];
   out[4] += ta*b[5] + tb*a[5];
   out[5] += ta*b[4] + tb*a[4];
   t = a[4]*b[5] + a[5]*b[4];
   out[7] += 0.15607835f*t;

   ta = 0.28209479f*a[0] + 0.09011188f*a[6] - 0.15607835f*a[8];
   tb = 0.28209479f*b[0] + 0.09011188f*b[6] - 0.15607835f*b[8];
   out[5] += ta*b[5] + tb*a[5];
   t = a[5]*b[5];
   out[0] += 0.28209479f*t;
   out[6] += 0.09011188f*t;
   out[8] -= 0.15607835f*t;

   ta = 0.28209480f*a[0];
   tb = 0.28209480f*b[0];
   out[6] += ta*b[6] + tb*a[6];
   t = a[6]*b[6];
   out[0] += 0.28209480f*t;
   out[6] += 0.18022376f*t;

   ta = 0.28209479f*a[0] + 0.09011188f*a[6] + 0.15607835f*a[8];
   tb = 0.28209479f*b[0] + 0.09011188f*b[6] + 0.15607835f*b[8];
   out[7] += ta*b[7] + tb*a[7];
   t = a[7]*b[7];
   out[0] += 0.28209479f*t;
   out[6] += 0.09011188f*t;
   out[8] += 0.15607835f*t;

   ta = 0.28209479f*a[0] - 0.18022375f*a[6];
   tb = 0.28209479f*b[0] - 0.18022375f*b[6];
   out[8] += ta*b[8] + tb*a[8];
   t = a[8]*b[8];
   out[0] += 0.28209479f*t;
   out[6] -= 0.18022375f*t;
  }
//+------------------------------------------------------------------+
//| Computes the product of two functions represented using          |
//| Spherical Harmonics (f and g). Both functions are of order N=4.  |
//+------------------------------------------------------------------+
//| The order is a number between 2 and 6 inclusive.                 |
//| So it's the same for the several functions:                      |
//| DXSHMultiply2, DXSHMultiply3, ... DXSHMultiply6.                 |
//|                                                                  |
//| Computes the product of two functions represented                |
//| using SH (f and g), where                                        |
//|               out[i] = int(y_i(s) * f(s) * g(s)),                |
//| where                                                            |
//|       y_i(s) is the ith SH basis function,                       |
//|       f(s) and g(s) are SH functions (sum_i(y_i(s)*c_i)).        |
//| The order determines the lengths of the arrays,where there       |
//| should always be l^2 coefficients.                               |
//|                                                                  |
//| In general the product of two SH functions of order l generates  |
//| an SH function of order 2*l - 1, but the results are truncated.  |
//|                                                                  |
//| This means that the product commutes (f*g == g*f)                |
//| but doesn't associate (f*(g*h) != (f*g)*h.                       |
//+------------------------------------------------------------------+
void DXSHMultiply4(float &out[],const float &a[],const float &b[])
  {
   out[0] = 0.28209479f*a[0]*b[0];
   float ta = 0.28209479f*a[0] - 0.12615663f*a[6] - 0.21850969f*a[8];
   float tb = 0.28209479f*b[0] - 0.12615663f*b[6] - 0.21850969f*b[8];
   out[1] = ta*b[1] + tb*a[1];
   float t = a[1]*b[1];
   out[0] += 0.28209479f*t;
   out[6] = -0.12615663f*t;
   out[8] = -0.21850969f*t;

   ta = 0.21850969f*a[3] - 0.05839917f*a[13] - 0.22617901f*a[15];
   tb = 0.21850969f*b[3] - 0.05839917f*b[13] - 0.22617901f*b[15];
   out[1] += ta*b[4] + tb*a[4];
   out[4] = ta*b[1] + tb*a[1];
   t = a[1]*b[4] + a[4]*b[1];
   out[3] = 0.21850969f*t;
   out[13] = -0.05839917f*t;
   out[15] = -0.22617901f*t;

   ta = 0.21850969f*a[2] - 0.14304817f*a[12] - 0.18467439f*a[14];
   tb = 0.21850969f*b[2] - 0.14304817f*b[12] - 0.18467439f*b[14];
   out[1] += ta*b[5] + tb*a[5];
   out[5] = ta*b[1] + tb*a[1];
   t = a[1]*b[5] + a[5]*b[1];
   out[2] = 0.21850969f*t;
   out[12] = -0.14304817f*t;
   out[14] = -0.18467439f*t;

   ta = 0.20230066f*a[11];
   tb = 0.20230066f*b[11];
   out[1] += ta*b[6] + tb*a[6];
   out[6] += ta*b[1] + tb*a[1];
   t = a[1]*b[6] + a[6]*b[1];
   out[11] = 0.20230066f*t;

   ta = 0.22617901f*a[9] + 0.05839917f*a[11];
   tb = 0.22617901f*b[9] + 0.05839917f*b[11];
   out[1] += ta*b[8] + tb*a[8];
   out[8] += ta*b[1] + tb*a[1];
   t = a[1]*b[8] + a[8]*b[1];
   out[9] = 0.22617901f*t;
   out[11] += 0.05839917f*t;

   ta = 0.28209480f*a[0] + 0.25231326f*a[6];
   tb = 0.28209480f*b[0] + 0.25231326f*b[6];
   out[2] += ta*b[2] + tb*a[2];
   t = a[2]*b[2];
   out[0] += 0.28209480f*t;
   out[6] += 0.25231326f*t;

   ta = 0.24776671f*a[12];
   tb = 0.24776671f*b[12];
   out[2] += ta*b[6] + tb*a[6];
   out[6] += ta*b[2] + tb*a[2];
   t = a[2]*b[6] + a[6]*b[2];
   out[12] += 0.24776671f*t;

   ta = 0.28209480f*a[0] - 0.12615663f*a[6] + 0.21850969f*a[8];
   tb = 0.28209480f*b[0] - 0.12615663f*b[6] + 0.21850969f*b[8];
   out[3] += ta*b[3] + tb*a[3];
   t = a[3]*b[3];
   out[0] += 0.28209480f*t;
   out[6] -= 0.12615663f*t;
   out[8] += 0.21850969f*t;

   ta = 0.20230066f*a[13];
   tb = 0.20230066f*b[13];
   out[3] += ta*b[6] + tb*a[6];
   out[6] += ta*b[3] + tb*a[3];
   t = a[3]*b[6] + a[6]*b[3];
   out[13] += 0.20230066f*t;

   ta = 0.21850969f*a[2] - 0.14304817f*a[12] + 0.18467439f*a[14];
   tb = 0.21850969f*b[2] - 0.14304817f*b[12] + 0.18467439f*b[14];
   out[3] += ta*b[7] + tb*a[7];
   out[7] = ta*b[3] + tb*a[3];
   t = a[3]*b[7] + a[7]*b[3];
   out[2] += 0.21850969f*t;
   out[12] -= 0.14304817f*t;
   out[14] += 0.18467439f*t;

   ta = -0.05839917f*a[13] + 0.22617901f*a[15];
   tb = -0.05839917f*b[13] + 0.22617901f*b[15];
   out[3] += ta*b[8] + tb*a[8];
   out[8] += ta*b[3] + tb*a[3];
   t = a[3]*b[8] + a[8]*b[3];
   out[13] -= 0.05839917f*t;
   out[15] += 0.22617901f*t;

   ta = 0.28209479f*a[0] - 0.18022375f*a[6];
   tb = 0.28209479f*b[0] - 0.18022375f*b[6];
   out[4] += ta*b[4] + tb*a[4];
   t = a[4]*b[4];
   out[0] += 0.28209479f*t;
   out[6] -= 0.18022375f*t;

   ta = 0.15607835f*a[7];
   tb = 0.15607835f*b[7];
   out[4] += ta*b[5] + tb*a[5];
   out[5] += ta*b[4] + tb*a[4];
   t = a[4]*b[5] + a[5]*b[4];
   out[7] += 0.15607835f*t;

   ta = 0.22617901f*a[3] - 0.09403160f*a[13];
   tb = 0.22617901f*b[3] - 0.09403160f*b[13];
   out[4] += ta*b[9] + tb*a[9];
   out[9] += ta*b[4] + tb*a[4];
   t = a[4]*b[9] + a[9]*b[4];
   out[3] += 0.22617901f*t;
   out[13] -= 0.09403160f*t;

   ta = 0.18467439f*a[2] - 0.18806319f*a[12];
   tb = 0.18467439f*b[2] - 0.18806319f*b[12];
   out[4] += ta*b[10] + tb*a [10];
   out[10] = ta*b[4] + tb*a[4];
   t = a[4]*b[10] + a[10]*b[4];
   out[2] += 0.18467439f*t;
   out[12] -= 0.18806319f*t;

   ta = -0.05839917f*a[3] + 0.14567312f*a[13] + 0.09403160f*a[15];
   tb = -0.05839917f*b[3] + 0.14567312f*b[13] + 0.09403160f*b[15];
   out[4] += ta*b[11] + tb*a[11];
   out[11] += ta*b[4] + tb*a[4];
   t = a[4]*b[11] + a[11]*b[4];
   out[3] -= 0.05839917f*t;
   out[13] += 0.14567312f*t;
   out[15] += 0.09403160f*t;

   ta = 0.28209479f*a[0] + 0.09011186f*a[6] - 0.15607835f*a[8];
   tb = 0.28209479f*b[0] + 0.09011186f*b[6] - 0.15607835f*b[8];
   out[5] += ta*b[5] + tb*a[5];
   t = a[5]*b[5];
   out[0] += 0.28209479f*t;
   out[6] += 0.09011186f*t;
   out[8] -= 0.15607835f*t;

   ta = 0.14867701f*a[14];
   tb = 0.14867701f*b[14];
   out[5] += ta*b[9] + tb*a[9];
   out[9] += ta*b[5] + tb*a[5];
   t = a[5]*b[9] + a[9]*b[5];
   out[14] += 0.14867701f*t;

   ta = 0.18467439f*a[3] + 0.11516472f*a[13] - 0.14867701f*a[15];
   tb = 0.18467439f*b[3] + 0.11516472f*b[13] - 0.14867701f*b[15];
   out[5] += ta*b[10] + tb*a[10];
   out[10] += ta*b[5] + tb*a[5];
   t = a[5]*b[10] + a[10]*b[5];
   out[3] += 0.18467439f*t;
   out[13] += 0.11516472f*t;
   out[15] -= 0.14867701f*t;

   ta = 0.23359668f*a[2] + 0.05947080f*a[12] - 0.11516472f*a[14];
   tb = 0.23359668f*b[2] + 0.05947080f*b[12] - 0.11516472f*b[14];
   out[5] += ta*b[11] + tb*a[11];
   out[11] += ta*b[5] + tb*a[5];
   t = a[5]*b[11] + a[11]*b[5];
   out[2] += 0.23359668f*t;
   out[12] += 0.05947080f*t;
   out[14] -= 0.11516472f*t;

   ta = 0.28209479f*a[0];
   tb = 0.28209479f*b[0];
   out[6] += ta*b[6] + tb*a[6];
   t = a[6]*b[6];
   out[0] += 0.28209479f*t;
   out[6] += 0.18022376f*t;

   ta = 0.09011186f*a[6] + 0.28209479f*a[0] + 0.15607835f*a[8];
   tb = 0.09011186f*b[6] + 0.28209479f*b[0] + 0.15607835f*b[8];
   out[7] += ta*b[7] + tb*a[7];
   t = a[7]*b[7];
   out[6] += 0.09011186f*t;
   out[0] += 0.28209479f*t;
   out[8] += 0.15607835f*t;

   ta = 0.14867701f*a[9] + 0.18467439f*a[1] + 0.11516472f*a[11];
   tb = 0.14867701f*b[9] + 0.18467439f*b[1] + 0.11516472f*b[11];
   out[7] += ta*b[10] + tb*a[10];
   out[10] += ta*b[7] + tb*a[7];
   t = a[7]*b[10] + a[10]*b[7];
   out[9] += 0.14867701f*t;
   out[1] += 0.18467439f*t;
   out[11] += 0.11516472f*t;

   ta = 0.05947080f*a[12] + 0.23359668f*a[2] + 0.11516472f*a[14];
   tb = 0.05947080f*b[12] + 0.23359668f*b[2] + 0.11516472f*b[14];
   out[7] += ta*b[13] + tb*a[13];
   out[13] += ta*b[7]+ tb*a[7];
   t = a[7]*b[13] + a[13]*b[7];
   out[12] += 0.05947080f*t;
   out[2] += 0.23359668f*t;
   out[14] += 0.11516472f*t;

   ta = 0.14867701f*a[15];
   tb = 0.14867701f*b[15];
   out[7] += ta*b[14] + tb*a[14];
   out[14] += ta*b[7] + tb*a[7];
   t = a[7]*b[14] + a[14]*b[7];
   out[15] += 0.14867701f*t;

   ta = 0.28209479f*a[0] - 0.18022375f*a[6];
   tb = 0.28209479f*b[0] - 0.18022375f*b[6];
   out[8] += ta*b[8] + tb*a[8];
   t = a[8]*b[8];
   out[0] += 0.28209479f*t;
   out[6] -= 0.18022375f*t;

   ta = -0.09403160f*a[11];
   tb = -0.09403160f*b[11];
   out[8] += ta*b[9] + tb*a[9];
   out[9] += ta*b[8] + tb*a[8];
   t = a[8]*b[9] + a[9]*b[8];
   out[11] -= 0.09403160f*t;

   ta = -0.09403160f*a[15];
   tb = -0.09403160f*b[15];
   out[8] += ta*b[13] + tb*a[13];
   out[13] += ta*b[8] + tb*a[8];
   t = a[8]*b[13] + a[13]*b[8];
   out[15] -= 0.09403160f*t;

   ta = 0.18467439f*a[2] - 0.18806319f*a[12];
   tb = 0.18467439f*b[2] - 0.18806319f*b[12];
   out[8] += ta*b[14] + tb*a[14];
   out[14] += ta*b[8] + tb*a[8];
   t = a[8]*b[14] + a[14]*b[8];
   out[2] += 0.18467439f*t;
   out[12] -= 0.18806319f*t;

   ta = -0.21026104f*a[6] + 0.28209479f*a[0];
   tb = -0.21026104f*b[6] + 0.28209479f*b[0];
   out[9] += ta*b[9] + tb*a[9];
   t = a[9]*b[9];
   out[6] -= 0.21026104f*t;
   out[0] += 0.28209479f*t;

   ta = 0.28209479f*a[0];
   tb = 0.28209479f*b[0];
   out[10] += ta*b[10] + tb*a[10];
   t = a[10]*b[10];
   out[0] += 0.28209479f*t;

   ta = 0.28209479f*a[0] + 0.12615663f*a[6] - 0.14567312f*a[8];
   tb = 0.28209479f*b[0] + 0.12615663f*b[6] - 0.14567312f*b[8];
   out[11] += ta*b[11] + tb*a[11];
   t = a[11]*b[11];
   out[0] += 0.28209479f*t;
   out[6] += 0.12615663f*t;
   out[8] -= 0.14567312f*t;

   ta = 0.28209479f*a[0] + 0.16820885f*a[6];
   tb = 0.28209479f*b[0] + 0.16820885f*b[6];
   out[12] += ta*b[12] + tb*a[12];
   t = a[12]*b[12];
   out[0] += 0.28209479f*t;
   out[6] += 0.16820885f*t;

   ta =0.28209479f*a[0] + 0.14567312f*a[8] + 0.12615663f*a[6];
   tb =0.28209479f*b[0] + 0.14567312f*b[8] + 0.12615663f*b[6];
   out[13] += ta*b[13] + tb*a[13];
   t = a[13]*b[13];
   out[0] += 0.28209479f*t;
   out[8] += 0.14567312f*t;
   out[6] += 0.12615663f*t;

   ta = 0.28209479f*a[0];
   tb = 0.28209479f*b[0];
   out[14] += ta*b[14] + tb*a[14];
   t = a[14]*b[14];
   out[0] += 0.28209479f*t;

   ta = 0.28209479f*a[0] - 0.21026104f*a[6];
   tb = 0.28209479f*b[0] - 0.21026104f*b[6];
   out[15] += ta*b[15] + tb*a[15];
   t = a[15]*b[15];
   out[0] += 0.28209479f*t;
   out[6] -= 0.21026104f*t;
  }
//+------------------------------------------------------------------+
//| rotate_X                                                         |
//+------------------------------------------------------------------+
void rotate_X(float &out[],uint order,float a,float &in[])
  {
   out[0] = in[0];
   out[1] = a*in[2];
   out[2] = -a*in[1];
   out[3] = in[3];
   out[4] = a*in[7];
   out[5] = -in[5];
   out[6] = -0.5f*in[6] - 0.8660253882f*in[8];
   out[7] = -a*in[4];
   out[8] = -0.8660253882f*in[6] + 0.5f*in[8];
   out[9] = -a*0.7905694842f*in[12] + a*0.6123724580f*in[14];
   out[10] = -in[10];
   out[11] = -a*0.6123724580f*in[12] - a*0.7905694842f*in[14];
   out[12] = a*0.7905694842f*in[9] + a*0.6123724580f*in[11];
   out[13] = -0.25f*in[13] - 0.9682458639f*in[15];
   out[14] = -a*0.6123724580f*in[9] + a*0.7905694842f*in[11];
   out[15] = -0.9682458639f*in[13] + 0.25f*in[15];
   if(order == 4)
      return;

   out[16] = -a*0.9354143739f*in[21] + a*0.3535533845f*in[23];
   out[17] = -0.75f*in[17] + 0.6614378095f*in[19];
   out[18] = -a*0.3535533845f*in[21] - a*0.9354143739f*in[23];
   out[19] = 0.6614378095f*in[17] + 0.75f*in[19];
   out[20] = 0.375f*in[20] + 0.5590170026f*in[22] + 0.7395099998f*in[24];
   out[21] = a*0.9354143739f*in[16] + a*0.3535533845f*in[18];
   out[22] = 0.5590170026f*in[20] + 0.5f*in[22] - 0.6614378691f*in[24];
   out[23] = -a*0.3535533845f*in[16] + a*0.9354143739f*in[18];
   out[24] = 0.7395099998f*in[20] - 0.6614378691f*in[22] + 0.125f*in[24];
   if(order == 5)
      return;

   out[25] = a*0.7015607357f*in[30] - a*0.6846531630f*in[32] + a*0.1976423711f*in[34];
   out[26] = -0.5f*in[26] + 0.8660253882f*in[28];
   out[27] = a*0.5229125023f*in[30] + a*0.3061861992f*in[32] - a*0.7954951525f*in[34];
   out[28] = 0.8660253882f*in[26] + 0.5f*in[28];
   out[29] = a*0.4841229022f*in[30] + a*0.6614378691f*in[32] + a*0.5728219748f*in[34];
   out[30] = -a*0.7015607357f*in[25] - a*0.5229125023f*in[27] - a*0.4841229022f*in[29];
   out[31] = 0.125f*in[31] + 0.4050463140f*in[33] + 0.9057110548f*in[35];
   out[32] = a*0.6846531630f*in[25] - a*0.3061861992f*in[27] - a*0.6614378691f*in[29];
   out[33] = 0.4050463140f*in[31] + 0.8125f*in[33] - 0.4192627370f*in[35];
   out[34] = -a*0.1976423711f*in[25] + a*0.7954951525f*in[27] - a*0.5728219748f*in[29];
   out[35] = 0.9057110548f*in[31] - 0.4192627370f*in[33] + 0.0624999329f*in[35];
  }
//+------------------------------------------------------------------+
//| Rotates the spherical harmonic (SH) vector by the given matrix.  |
//+------------------------------------------------------------------+
//| Each coefficient of the basis function Y(l,m)                    |
//| is stored at memory location l^2 + m + l, where:                 |
//| l is the degree of the basis function.                           |
//| m is the basis function index for the given l value              |
//|   and ranges from -l to l, inclusive.                            |
//+------------------------------------------------------------------+
void DXSHRotate(float &out[],int order,const DXMatrix &matrix,const float &in[])
  {
   float alpha,beta,gamma,sinb,temp[36],temp1[36];
   out[0] = in[0];

   if((order>DXSH_MAXORDER) || (order<DXSH_MINORDER))
      return;

   if(order <= 3)
     {
      out[1] = matrix.m[1][1]*in[1] - matrix.m[2][1]*in[2] + matrix.m[0][1]*in[3];
      out[2] = -matrix.m[1][2]*in[1] + matrix.m[2][2]*in[2] - matrix.m[0][2]*in[3];
      out[3] = matrix.m[1][0]*in[1] - matrix.m[2][0]*in[2] + matrix.m[0][0]*in[3];

      if(order == 3)
        {
         float coeff[12]= {};
         coeff[0] = matrix.m[1][0]*matrix.m[0][0];
         coeff[1] = matrix.m[1][1]*matrix.m[0][1];
         coeff[2] = matrix.m[1][1]*matrix.m[2][1];
         coeff[3] = matrix.m[1][0]*matrix.m[2][0];
         coeff[4] = matrix.m[2][0]*matrix.m[2][0];
         coeff[5] = matrix.m[2][1]*matrix.m[2][1];
         coeff[6] = matrix.m[0][0]*matrix.m[2][0];
         coeff[7] = matrix.m[0][1]*matrix.m[2][1];
         coeff[8] = matrix.m[0][1]*matrix.m[0][1];
         coeff[9] = matrix.m[1][0]*matrix.m[1][0];
         coeff[10] = matrix.m[1][1]*matrix.m[1][1];
         coeff[11] = matrix.m[0][0]*matrix.m[0][0];

         out[4] = (matrix.m[1][1]*matrix.m[0][0] + matrix.m[0][1]*matrix.m[1][0])*in[4];
         out[4] -= (matrix.m[1][0]*matrix.m[2][1] + matrix.m[1][1]*matrix.m[2][0])*in[5];
         out[4] += 1.7320508076f*matrix.m[2][0]*matrix.m[2][1]*in[6];
         out[4] -= (matrix.m[0][1]*matrix.m[2][0] + matrix.m[0][0]*matrix.m[2][1])*in[7];
         out[4] += (matrix.m[0][0]*matrix.m[0][1] - matrix.m[1][0]*matrix.m[1][1])*in[8];

         out[5] = (matrix.m[1][1]*matrix.m[2][2] + matrix.m[1][2]*matrix.m[2][1])*in[5];
         out[5] -= (matrix.m[1][1]*matrix.m[0][2] + matrix.m[1][2]*matrix.m[0][1])*in[4];
         out[5] -= 1.7320508076f*matrix.m[2][2]*matrix.m[2][1]*in[6];
         out[5] += (matrix.m[0][2]*matrix.m[2][1] + matrix.m[0][1]*matrix.m[2][2])*in[7];
         out[5] -= (matrix.m[0][1]*matrix.m[0][2] - matrix.m[1][1]*matrix.m[1][2])*in[8];

         out[6] = (matrix.m[2][2]*matrix.m[2][2] - 0.5f*(coeff[4] + coeff[5]))*in[6];
         out[6] -= (0.5773502692f*(coeff[0] + coeff[1]) - 1.1547005384f*matrix.m[1][2]*matrix.m[0][2])*in[4];
         out[6] += (0.5773502692f*(coeff[2] + coeff[3]) - 1.1547005384f*matrix.m[1][2]*matrix.m[2][2])*in[5];
         out[6] += (0.5773502692f*(coeff[6] + coeff[7]) - 1.1547005384f*matrix.m[0][2]*matrix.m[2][2])*in[7];
         out[6] += (0.2886751347f*(coeff[9] - coeff[8] + coeff[10] - coeff[11]) - 0.5773502692f*
                    (matrix.m[1][2]*matrix.m[1][2] - matrix.m[0][2]*matrix.m[0][2]))*in[8];

         out[7] = (matrix.m[0][0]*matrix.m[2][2] + matrix.m[0][2]*matrix.m[2][0])*in[7];
         out[7] -= (matrix.m[1][0]*matrix.m[0][2] + matrix.m[1][2]*matrix.m[0][0])*in[4];
         out[7] += (matrix.m[1][0]*matrix.m[2][2] + matrix.m[1][2]*matrix.m[2][0])*in[5];
         out[7] -= 1.7320508076f*matrix.m[2][2]*matrix.m[2][0]*in[6];
         out[7] -= (matrix.m[0][0]*matrix.m[0][2] - matrix.m[1][0]*matrix.m[1][2])*in[8];

         out[8] = 0.5f*(coeff[11] - coeff[8] - coeff[9] + coeff[10])*in[8];
         out[8] += (coeff[0] - coeff[1])*in[4];
         out[8] += (coeff[2] - coeff[3])*in[5];
         out[8] += 0.86602540f*(coeff[4] - coeff[5])*in[6];
         out[8] += (coeff[7] - coeff[6])*in[7];
        }
      return;
     }

   if((float)fabs(matrix.m[2][2]) != 1.0f)
     {
      sinb = (float)sqrt(1.0f - matrix.m[2][2]*matrix.m[2][2]);
      alpha = (float)atan2(matrix.m[2][1] / sinb,matrix.m[2][0] / sinb);
      beta = (float)atan2(sinb,matrix.m[2][2]);
      gamma = (float)atan2(matrix.m[1][2] / sinb,-matrix.m[0][2] / sinb);
     }
   else
     {
      alpha = (float)atan2(matrix.m[0][1],matrix.m[0][0]);
      beta = 0.0f;
      gamma = 0.0f;
     }
//---
   DXSHRotateZ(temp,order,gamma,in);
   rotate_X(temp1,order,1.0f,temp);
   DXSHRotateZ(temp,order,beta,temp1);
   rotate_X(temp1,order,-1.0f,temp);
   DXSHRotateZ(out,order,alpha,temp1);
  }
//+------------------------------------------------------------------+
//| Rotates the spherical harmonic (SH) vector                       |
//| in the z-axis by the given angle.                                |
//+------------------------------------------------------------------+
//| Each coefficient of the basis function Y(l,m)                    |
//| is stored at memory location l^2 + m + l, where:                 |
//| l is the degree of the basis function.                           |
//| m is the basis function index for the given l value              |
//|   and ranges from -l to l, inclusive.                            |
//+------------------------------------------------------------------+
void DXSHRotateZ(float &out[],int order,float angle,const float &in[])
  {
   int sum = 0;
   float c[5],s[5];
   order = (int)fmin(fmax(order,DXSH_MINORDER),DXSH_MAXORDER);
   out[0] = in[0];
//---
   for(int i=1; i<order; i++)
     {
      c[i - 1] = (float)cos(i*angle);
      s[i - 1] = (float)sin(i*angle);
      sum += i*2;
      //---
      out[sum - i] = c[i - 1]*in[sum - i];
      out[sum - i] += s[i - 1]*in[sum + i];
      for(int j= i-1; j>0; j--)
        {
         out[sum - j] = 0.0f;
         out[sum - j] = c[j - 1]*in[sum - j];
         out[sum - j] += s[j - 1]*in[sum + j];
        }
      out[sum] = in[sum];
      //---
      for(int j=1; j<i; j++)
        {
         out[sum + j] = 0.0f;
         out[sum + j] = -s[j - 1]*in[sum - j];
         out[sum + j] += c[j - 1]*in[sum + j];
        }
      out[sum + i] = -s[i - 1]*in[sum - i];
      out[sum + i] += c[i - 1]*in[sum + i];
     }
  }
//+------------------------------------------------------------------+
//| Scales a spherical harmonic (SH) vector;                         |
//| in other words, out[i] = a[i]*scale.                             |
//+------------------------------------------------------------------+
void DXSHScale(float &out[],int order,const float &a[],const float scale)
  {
   for(int i=0; i<order*order; i++)
      out[i] = a[i]*scale;
  }
//+---------------------------------------------------------------------+
//| Interpolates y0 to y1 according to value from 0.0 to 1.0            |
//+---------------------------------------------------------------------+
float DXScalarLerp(const float val1,const float val2,float s)
  {
   return((1-s)*val1 + s*val2);
  }
//+---------------------------------------------------------------------+
//| Interpolates y0 to y1 according to value from 0.0 to 1.0            |
//+---------------------------------------------------------------------+
float DXScalarBiasScale(const float val,const float bias,const float scale)
  {
   return((val+bias)*scale);
  }
//+------------------------------------------------------------------+
