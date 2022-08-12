//+------------------------------------------------------------------+
//|                                             Default Pixel Shader |
//|                        Copyright 2019, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Inputs for whole scene                                           |
//+------------------------------------------------------------------+
cbuffer InputScene : register(b0)
  {
   matrix view;
   matrix projection;
   float4 light_direction;
   float4 light_color;
   float4 ambient_color;
  };
//+------------------------------------------------------------------+
//| Inputs for single object                                         |
//+------------------------------------------------------------------+
cbuffer InputObject : register(b1)
  {
   matrix transform;
   float4 diffuse_color;
   float4 emission_color;
   float4 specular_color;
   float  specular_power;
   float  dummy[3];
  };
//+------------------------------------------------------------------+
//| Input texture                                                    |
//+------------------------------------------------------------------+
Texture2D<float4> diffuse_tex : register(t0);
//+------------------------------------------------------------------+
//| Texture sampler                                                  |
//+------------------------------------------------------------------+
SamplerState diffuse_samp
  {
    Filter  =MIN_MAG_MIP_LINEAR;
    AddressU=Wrap;
    AddressV=Wrap;
  };
//+------------------------------------------------------------------+
//| Pixel shader input type                                          |
//+------------------------------------------------------------------+
struct PSInput
  {
   float4 position : SV_POSITION;
   float4 camera   : CAMERA;
   float4 normal   : NORMAL;
   float2 tcoord   : TEXCOORD;
   float4 color    : COLOR;
  };
//+------------------------------------------------------------------+
//| Pixel shader entry point                                         |
//+------------------------------------------------------------------+
float4 PSMain(PSInput input) : SV_TARGET
  {
   float3 diffuse =saturate(-dot(light_direction.xyz,input.normal.xyz))*light_color.rgb*light_color.a;
   float3 ambient =ambient_color.rgb *ambient_color.a;
   float3 light   =(diffuse+ambient)*diffuse_color.rgb*diffuse_color.a+emission_color.rgb*emission_color.a;
   float4 specular=float4(light_color.rgb*specular_color.rgb,pow(saturate(dot(reflect(normalize(light_direction.xyz),input.normal.xyz),normalize(input.camera.xyz))),specular_power)*light_color.a*specular_color.a);
   float4 clr=input.color;
   //--- use texture if it exist
   uint width,height;
   diffuse_tex.GetDimensions(width,height);
   if(width*height>0)
      clr*=diffuse_tex.Sample(diffuse_samp,frac(input.tcoord));
   //--- combine light with colors   
   return(lerp(float4(light*clr.rgb,clr.a),float4(specular.rgb,1.0),specular.a));
  }
//+------------------------------------------------------------------+
