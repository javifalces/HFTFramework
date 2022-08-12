//+------------------------------------------------------------------+
//| OpenCL code                                                      |
//+------------------------------------------------------------------+
//| Based on pixel shader by Alexander Alekseev aka TDM-2014         |
//| https://www.shadertoy.com/view/Ms2SD1                            |
//+------------------------------------------------------------------+
#define NUM_STEPS      (int)8
#define PI	 	        (float)3.1415
#define EPSILON	     (float)1e-3
#define EPSILON_NRM	  ((float)0.1/iResolution.x)
#define ITER_GEOMETRY  (int)3
#define ITER_FRAGMENT  (int)5
#define SEA_HEIGHT     (float)0.6
#define SEA_CHOPPY     (float)4.0
#define SEA_SPEED      (float)0.8
#define SEA_FREQ       (float)0.16
#define SEA_BASE        vec3(0.1,0.19,0.22)
#define SEA_WATER_COLOR vec3(0.8,0.9,0.6)
#define SEA_TIME ((float)1.0 + iGlobalTime * SEA_SPEED)

float  max1(float v1,float v2) { if (v1>v2) return v1; return v2; }
float  dot2(float2 v1,float2 v2) { return v1.x*v2.x+v1.y*v2.y; }
float  dot3(float3 v1,float3 v2) { return v1.x*v2.x+v1.y*v2.y+v1.z*v2.z; }
float  fract1(float  p) { return p-floor(p); }
float2 fract2(float2 p) { return p-floor(p); }
float  mix1(float x1,float x2,float a) { return(x1+(x2-x1)*a); }
float2 mix2(float2 x1,float2 x2,float2 a) { float2 r={x1.x+(x2.x-x1.x)*a.x,x1.y+(x2.y-x1.y)*a.y }; return(r); }
float3 mix3(float3 x1,float3 x2,float3 a) { float3 r={x1.x+(x2.x-x1.x)*a.x,x1.y+(x2.y-x1.y)*a.y,x1.z+(x2.z-x1.z)*a.z }; return(r); }
float3 vec31(float x) { float3 r={x,x,x}; return(r); }
float3 vec3(float x,float y,float z) { float3 r={x,y,z}; return(r); }
float2 vec2(float x,float y) { float2 r={x,y}; return(r); }
float4 vec4(float3 x,float y) { float4 r={x.x,x.y,x.z,y}; return(r); }
float3 reflect3(float3 I,float3 N) { return(I-(float)2.0*dot3(N,I)*N); } 
float  diffuse(float3 n,float3 l,float p) { return pow(dot3(n,l) * (float)0.4 + (float)0.6,p); }
float2 abs2(float2 f) { if(f.x<0) f.x=-f.x; if(f.y<0) f.y=-f.y; return(f); }
float  hash(float2 p) { return fract1(sin(dot2(p,vec2((float)127.1,(float)311.7)))*43758.5453123); }
float  specular(float3 n,float3 l,float3 e,float s) { return pow(max1(dot3(reflect3(e,n),l),0.0),s)*((s+(float)8.0)/(float)(3.1415 * 8.0)); }

float3 getSkyColor(float3 e)
  {
   e.y=max1(e.y,0.0);
   float3 r2;
   r2.x = (float)1.0-e.y;
   r2.y = r2.x;
   r2.z = (float)0.6+r2.x *(float)0.4;
   r2.x = r2.x * r2.x;   
   return r2;
  }

void fromEuler(float3 ang,float3 *r1,float3 *r2,float3 *r3)
  {
	 float2 a1 = vec2(sin(ang.x),cos(ang.x));
   float2 a2 = vec2(sin(ang.y),cos(ang.y));
   float2 a3 = vec2(sin(ang.z),cos(ang.z));
   *r1 = vec3(a1.y*a3.y+a1.x*a2.x*a3.x,a1.y*a2.x*a3.x+a3.y*a1.x,-a2.y*a3.x);
	 *r2 = vec3(-a2.y*a1.x,a1.y*a2.y,a2.x);
	 *r3 = vec3(a3.y*a1.x*a2.x+a1.y*a3.x,a1.x*a3.x-a1.y*a3.y*a2.x,a2.y*a3.y);
  }

float noise(float2 p) 
  {
   float2 i = floor(p);
   float2 f = fract2(p);
	 float2 u = f * f * ((float)3.0 - (float)2.0 * f);
   float mx1=mix1(hash(i+vec2(0.0,0.0)),hash(i+vec2(1.0,0.0)),u.x);
   float mx2=mix1(hash(i+vec2(0.0,1.0)),hash(i+vec2(1.0,1.0)),u.x);
   return (float)-1.0 + (float)2.0*mix(mx1,mx2,u.y);
  }

float sea_octave(float2 uv, float choppy)
  {
   uv += noise(uv);
   float2 wv = (float)1.0-abs2(sin(uv));
   float2 swv = abs2(cos(uv));
   wv = mix2(wv,swv,wv);
   return pow((float)1.0-pow(wv.x * wv.y,(float)0.65),choppy);
  }

float map(float3 p,float iGlobalTime)
  {
   float freq = SEA_FREQ;
   float amp = SEA_HEIGHT;
   float choppy = SEA_CHOPPY;
   float2 uv = p.xz; uv.x *= (float)0.75;
   float d, h = 0.0;
   for(int i = 0; i < ITER_GEOMETRY; i++)
     {
    	 d = sea_octave((uv+SEA_TIME)*freq,choppy);
    	 d += sea_octave((uv-SEA_TIME)*freq,choppy);
      h += d * amp;
      float2 uvt={(float)1.6*uv.x+(float)1.2*uv.y, (float)-1.2*uv.x+(float)1.6*uv.y };
    	 uv = uvt;
      freq *= (float)1.9; amp *= (float)0.22;
      choppy = mix1(choppy,(float)1.0,(float)0.2);
     }
   return p.y - h;
  }

float map_detailed(float3 p,float iGlobalTime)
  {
   float freq = SEA_FREQ;
   float amp = SEA_HEIGHT;
   float choppy = SEA_CHOPPY;
   float2 uv = p.xz; uv.x *= (float)0.75;
   float d, h = 0.0;
   for(int i = 0; i < ITER_FRAGMENT; i++)
     {
    	 d =sea_octave((uv+SEA_TIME)*freq,choppy);
    	 d+=sea_octave((uv-SEA_TIME)*freq,choppy);
      h+=d * amp;
      float2 uvt={ (float)1.6*uv.x+(float)1.2*uv.y, (float)-1.2*uv.x+(float)1.6*uv.y };
    	 uv     = uvt;
      freq  *= (float)1.9; amp *= (float)0.22;
      choppy = mix1(choppy,(float)1.0,(float)0.2);
    }
    return p.y - h;
 }

float3 getSeaColor(float3 p, float3 n, float3 l, float3 eye, float3 dist)
  {
   float fresnel = clamp((float)1.0 - dot3(n,-eye), (float)0.0, (float)1.0);
   fresnel = pow(fresnel,(float)3.0) * (float)0.65;
   float3 reflected = getSkyColor(reflect3(eye,n));
   float3 refracted = SEA_BASE + diffuse(n,l,(float)80.0) * SEA_WATER_COLOR * (float)0.12; 
   float3 color = mix3(refracted,reflected,fresnel);   
   float atten = max1((float)1.0 - dot(dist,dist) * (float)0.001, (float)0.0);
   color += SEA_WATER_COLOR * (p.y - SEA_HEIGHT) * (float)0.18 * atten;
   color += vec31(specular(n,l,eye,(float)60.0));
   
   if(isnan(color.x))
      color.x=0.0;
   if(isnan(color.y))
      color.y=0.0;
   if(isnan(color.z))
      color.z=0.0;
   
   return color;
  }

float3 getNormal(float3 p, float eps,float iGlobalTime)
  {
   float3 n;
   n.y = map_detailed(p,iGlobalTime);
   n.x = map_detailed(vec3(p.x+eps,p.y,p.z),iGlobalTime) - n.y;
   n.z = map_detailed(vec3(p.x,p.y,p.z+eps),iGlobalTime) - n.y;
   n.y = eps;
   return normalize(n);
  }

float heightMapTracing(float3 ori, float3 dir,float3 *p,float iGlobalTime)
  {  
   float tm = (float)0.0;
   float tx = (float)1000.0;
   float hx = map(ori + dir * tx,iGlobalTime);
   if(hx > (float)0.0) return tx;   
   float hm = map(ori + dir * tm,iGlobalTime);
   float tmid = (float)0.0;
   for(int i = 0; i < NUM_STEPS; i++)
      {
       tmid = mix(tm,tx, hm/(hm-hx));
       *p   = ori + dir * tmid;
    	  float hmid = map(*p,iGlobalTime);
		  if(hmid < (float)0.0)
         {
          tx = tmid;
          hx = hmid;
         }
       else
         {
          tm = tmid;
          hm = hmid;
         }
      }
   return tmid;
  }

float4 mainImage(float2 fragCoord,float iGlobalTime,float2 iResolution)
  {
   float4 fragColor;
   float2 iMouse= {0,0};
	 float2 uv = fragCoord.xy / iResolution;
   uv = uv * (float)2.0 - (float)1.0;
   uv.x *= iResolution.x / iResolution.y;
   float time = iGlobalTime * (float)0.3 + iMouse.x*(float)0.01;
   float3 ang = vec3(sin(time*(float)3.0)*(float)0.1,sin(time)*(float)0.2+(float)0.3,time);
   float3 ori = vec3(0.0,3.5,time*(float)5.0);
   float3 dir = normalize(vec3(uv.x,uv.y,(float)-2.0)); dir.z += length(uv) * (float)0.15;
   dir = normalize(dir);
   float3 r1,r2,r3;
   fromEuler(ang,&r1,&r2,&r3);
   float3 r;
   r.x=r1.x*dir.x+r1.y*dir.y+r1.z*dir.z;
   r.y=r2.x*dir.x+r2.y*dir.y+r2.z*dir.z;
   r.z=r3.x*dir.x+r3.y*dir.y+r3.z*dir.z;
   dir=r;
   float3 p;
   heightMapTracing(ori,dir,&p,iGlobalTime);
   float3 dist = p - ori;
   float3 n = getNormal(p, dot(dist,dist) * EPSILON_NRM,iGlobalTime);
   float3 light = normalize(vec3((float)0.0,(float)1.0,(float)0.8));
   float3 seacol=getSeaColor(p,n,light,dir,dist);   
   float3 color = mix(getSkyColor(dir),seacol,pow(smoothstep((float)0.0,(float)-0.05,dir.y),(float)0.3));
	 fragColor = vec4(pow(color,vec31((float)0.75)),(float)1.0);
   return(fragColor);
  }

__kernel void Seascape(float iGlobalTime,__global uint *out)
  {
   size_t  w = get_global_size(0);
   size_t  h = get_global_size(1);
   float2  iRes = {(float)w,(float)h};
   size_t gx = get_global_id(0);
   size_t gy = get_global_id(1);
   float2  coord={gx,gy};
   float4 res=mainImage(coord,iGlobalTime,iRes);
   uint b=(uint)(res.z*255);
   uint g=(uint)(res.y*255);
   uint r=(uint)(res.x*255);
   if(b>255) b=255;
   if(r>255) r=255;
   if(g>255) g=255;
   out[w*((uint)(iRes.y-1)-gy)+gx] = (r<<16)|(g<<8)|b;
  };
