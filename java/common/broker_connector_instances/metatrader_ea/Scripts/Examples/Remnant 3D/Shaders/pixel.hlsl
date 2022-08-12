// Remnant X
// by David Hoskins.
// Thanks to boxplorer and the folks at 'Fractalforums.com'
// HD Video:- https://www.youtube.com/watch?v=BjkK9fLXXo0
// https://www.shadertoy.com/view/4sjSW1

struct PSInput
  {
   float4 position : SV_POSITION;
  };

cbuffer Input
  {
   float2   iResolution;
   float    iTime;
   float    iDummy;
  };

#define PI 3.14159265f
#define SCALE 2.8
#define MINRAD2 .25
#define scale (float4(SCALE, SCALE, SCALE, abs(SCALE)) / minRad2)

static const float minRad2 = clamp(MINRAD2, 1.0e-9, 1.0);
static const float absScalem1 = abs(SCALE - 1.0);
static const float AbsScaleRaisedTo1mIters = pow(abs(SCALE), float(1-10));
static const float3 surfaceColour1 = float3(.8, .0, 0.);
static const float3 surfaceColour2 = float3(.4, .4, 0.5);
static const float3 surfaceColour3 = float3(.5, 0.3, 0.00);
static const float3 fogCol = float3(0.4, 0.4, 0.4);

static const float3 sunDir=normalize(float3(0.35,0.1,0.3));
static const float4 sunColour=float4(1.0,0.95,0.8,0.2);


float2 Rand(float2 p)
  {
   return(float2(frac(sin(dot(p,float2(12.9898,78.233)))*6.7416516),frac(sin(dot(p,float2(58.6542,22.6546)))*6.5465145)));
  }

float2 Texture(float2 p)
  {
   float2 p1=Rand(floor(p*256+float2(0,0)));
   float2 p2=Rand(floor(p*256+float2(0,1)));
   float2 p3=Rand(floor(p*256+float2(1,0)));
   float2 p4=Rand(floor(p*256+float2(1,1)));
   float2 f =frac(p*256);
    
   return lerp(lerp(p1,p2,f.y),lerp(p3,p4,f.y),f.x);
  }

//----------------------------------------------------------------------------------------
float Noise(in float3 x)
  {
   float3 p=floor(x);
   float3 f=frac(x);
   f=f*f*(3.0-2.0*f);

   float2 uv=(p.xy+float2(37.0,17.0)*p.z)+f.xy;
   float2 rg=Rand((uv+0.5)/256.0).yx;//texture(iChannel0,(uv+0.5)/256.0,-99.0).yx;
   return lerp(rg.x,rg.y,f.z);
  }

//----------------------------------------------------------------------------------------
float Map(float3 pos)
  {

   float4 p=float4(pos,1);
   float4 p0=p;  // p.w is the distance estimate

   for(int i = 0; i < 9; i++)
     {
      p.xyz=clamp(p.xyz,-1.0,1.0)*2.0-p.xyz;

      float r2=dot(p.xyz,p.xyz);
      p*=clamp(max(minRad2/r2,minRad2),0.0,1.0);

      // scale, translate
      p=p*scale+p0;
     }
   return ((length(p.xyz)-absScalem1)/p.w-AbsScaleRaisedTo1mIters);
  }

//----------------------------------------------------------------------------------------
float3 Colour(float3 pos, float sphereR)
  {
   float3 p=pos;
   float3 p0=p;
   float trap=1.0;

   for(int i = 0; i < 6; i++)
     {
      p.xyz=clamp(p.xyz,-1.0,1.0)*2.0-p.xyz;
      float r2 = dot(p.xyz,p.xyz);
      p*=clamp(max(minRad2/r2,minRad2), 0.0, 1.0);

      p=p*scale.xyz+p0.xyz;
      trap=min(trap,r2);
     }
// |c.x|: log final distance (fractional iteration count)
// |c.y|: spherical orbit trap at (0,0,0)
   float2 c=clamp(float2(0.3333*log(dot(p,p))-1.0,sqrt(trap)),0.0, 1.0);

   float t=fmod(length(pos)-iTime*1.5,16.0);
   float3 surfaceColour=lerp(surfaceColour1,float3(0.4,3.0,5.0),pow(smoothstep(0.0,0.3,t)*smoothstep(0.6,.3,t),10.0));
   return lerp(lerp(surfaceColour,surfaceColour2,c.y),surfaceColour3,c.x);
  }


//----------------------------------------------------------------------------------------
float3 GetNormal(float3 pos, float distance)
  {
   distance*=0.001+.0001;
   float2 eps=float2(distance,0.0);
   float3 nor=float3(Map(pos+eps.xyy) - Map(pos-eps.xyy),
                     Map(pos+eps.yxy) - Map(pos-eps.yxy),
                     Map(pos+eps.yyx) - Map(pos-eps.yyx));
   return normalize(nor);
  }

//----------------------------------------------------------------------------------------
float GetSky(float3 pos)
  {
   pos*=0.02;
   float2 t=0.5  *Texture(pos.xy*2.1)  +0.5*Texture(pos.yz*2.3-(float2)0.53)  +0.5*Texture(pos.zx*1.9+(float2)0.47)+
            0.25 *Texture(pos.xy*4.7) +0.25*Texture(pos.yz*4.1+(float2)0.71) +0.25*Texture(pos.zx*4.3+(float2)0.59)+
            0.125*Texture(pos.xy*8.7)+0.125*Texture(pos.yz*8.6-(float2)0.69)+0.125*Texture(pos.zx*8.3+(float2)0.95);

   return(pow(t.x*t.y,0.5));
  }

//----------------------------------------------------------------------------------------
float BinarySubdivision(in float3 rO, in float3 rD, float2 t)
  {
   float halfwayT;

   for(int i = 0; i < 6; i++)
     {

      halfwayT=dot(t,float2(0.5,0.5));
      float d = Map(rO + halfwayT*rD);
      //if (abs(d) < 0.001) break;
      t=lerp(float2(t.x,halfwayT),float2(halfwayT,t.y),step(0.0005, d));
     }

   return halfwayT;
  }

//----------------------------------------------------------------------------------------
float2 Scene(in float3 rO, in float3 rD, in float2 fragCoord)
  {
   float t=.05+0.05*Texture(fragCoord.xy/256).y;//texture(iChannel0, fragCoord.xy / iChannelResolution[0].xy).y;
   float3 p=float3(0.0,0.0,0.0);
   float oldT=0.0;
   bool hit=false;
   float glow=0.0;
   float2 dist;
   for(int j=0; j < 100; j++)
     {
      if(t > 12.0)
         break;
      p=rO+t*rD;

      float h=Map(p);

      if(h<0.0005)
        {
         dist=float2(oldT,t);
         hit=true;
         break;
        }
      glow+=clamp(.05-h,0.0,.4);
      oldT=t;
      t+=h+t*0.001;
     }
   if(!hit)
      t=1000.0;
   else
      t=BinarySubdivision(rO, rD, dist);
   return float2(t,clamp(glow*.25, 0.0, 1.0));

  }

//----------------------------------------------------------------------------------------
float Hash(float2 p)
  {
   return frac(sin(dot(p,float2(12.9898,78.233)))*33758.5453)-.5;
  }

//----------------------------------------------------------------------------------------
float3 PostEffects(float3 rgb, float2 xy)
  {
// Gamma first...


// Then...
#define CONTRAST 1.08
#define SATURATION 1.5
#define BRIGHTNESS 1.5
   float tmp=dot(float3(.2125, .7154, .0721),rgb*BRIGHTNESS);
   rgb=lerp(float3(0.5,0.5,0.5),lerp(float3(tmp,tmp,tmp),rgb*BRIGHTNESS,SATURATION),CONTRAST);
// Noise...
//rgb = clamp(rgb+Hash(xy*iTime)*.1, 0.0, 1.0);
// Vignette...
   rgb*=.5+0.5*pow(20.0*xy.x*xy.y*(1.0-xy.x)*(1.0-xy.y), 0.2);

   rgb=pow(rgb,float3(0.47,0.47,0.47));
   return rgb;
  }

//----------------------------------------------------------------------------------------
float Shadow(in float3 ro, in float3 rd)
  {
   float res=1.0;
   float t=0.05;
   float h;

   for(int i = 0; i < 8; i++)
     {
      h=Map(ro+rd*t);
      res=min(6.0*h/t,res);
      t+=h;
     }
   return max(res, 0.0);
  }

//----------------------------------------------------------------------------------------
float3x3 RotationMatrix(float3 axis, float angle)
  {
   axis = normalize(axis);
   float s = sin(angle);
   float c = cos(angle);
   float oc = 1.0 - c;

   return float3x3(oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,
               oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,
               oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c);
  }

//----------------------------------------------------------------------------------------
float3 LightSource(float3 spotLight, float3 dir, float dis)
  {
   float g=0.0;
   if(length(spotLight) < dis)
     {
      g=pow(max(dot(normalize(spotLight),dir),0.0),200.0);
     }

   return(g*sunColour.rgb*sunColour.a);
  }

//----------------------------------------------------------------------------------------
float3 CameraPath(float t)
  {
   float3 p=float3(-0.78+3.0*sin(2.14*t),0.05+2.5*sin(0.942*t+1.3),.05+3.5*cos(3.594*t));
   return p;
  }

//----------------------------------------------------------------------------------------
float4 mainImage(float2 fragCoord)
  {
   //float m=(iMouse.x/iResolution.x)*300.0;
   //gTime = iTime+m*.01 + 15.00;
   float gTime=iTime*0.01;
   float2 xy =fragCoord/iResolution;
   float2 uv =(-1.0+2.0*xy)*float2(iResolution.x/iResolution.y,1.0);
   
   //return(float4(uv,0.0,1.0));
   
   float3 cameraPos=CameraPath(gTime);
   float3 camTar   =CameraPath(gTime+.01);

   float roll=13.0*sin(gTime*.5+.4);
   float3 cw=normalize(camTar-cameraPos);

   float3 cp=float3(sin(roll),cos(roll),0.0);
   float3 cu=normalize(cross(cw,cp));

   float3 cv=normalize(cross(cu,cw));
   cw=mul(cw,RotationMatrix(cv,sin(-gTime*20.0)*.7));
   float3 dir=normalize(uv.x*cu+uv.y*cv+1.3*cw);

   float3 spotLight=CameraPath(gTime+.03)+float3(sin(gTime*18.4),cos(gTime*17.98),sin(gTime*22.53))*.2;
   float3 col=float3(0.0,0.0,0.0);
   float3 sky=float3(0.03,.04,.05)*GetSky(dir);
   float2 ret=Scene(cameraPos,dir,fragCoord);

   if(ret.x<900.0)
     {
      float3 p=cameraPos+ret.x*dir;
      float3 nor=GetNormal(p,ret.x);

      float3 spot=spotLight-p;
      float  atten=length(spot);

      spot/=atten;

      float shaSpot=Shadow(p,spot);
      float shaSun=Shadow(p,sunDir);

      float bri=max(dot(spot,nor),0.0)/pow(atten,1.5)*.15;
      float briSun=max(dot(sunDir,nor),0.0)*.3;

      col=Colour(p,ret.x);
      col=(col*bri*shaSpot)+(col*briSun*shaSun);

      float3 ref=reflect(dir,nor);
      col+=pow(max(dot(spot,ref),0.0),  10.0)*2.0*shaSpot*bri;
      col+=pow(max(dot(sunDir,ref),0.0),10.0)*2.0*shaSun *bri;
     }

   col=lerp(sky,col,min(exp(-ret.x+1.5),1.0));
   col+=(float3)pow(abs(ret.y),2.) * float3(.02, .04, .1);

   col+=LightSource(spotLight-cameraPos,dir,ret.x);
   col=PostEffects(col, xy);

   return(float4(col,1.0));
  }

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
float4 PSMain(PSInput input) : SV_TARGET
  {
   return(mainImage(input.position.xy));
  }
//+------------------------------------------------------------------+
