//+------------------------------------------------------------------+
//|                                            Default Vertex Shader |
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
//| Vertex shader input type                                         |
//+------------------------------------------------------------------+
struct VSInput
  {
   float4 position : POSITION;
   float4 normal   : NORMAL;
   float2 tcoord   : TEXCOORD;
   float4 color    : COLOR;
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
//| Vertex shader entry point                                        |
//+------------------------------------------------------------------+
PSInput VSMain(VSInput input)
  {
   PSInput output;
   //--- posiiton and camera direction
   output.position=mul(input .position,transform);
   output.position=mul(output.position,view);
   output.camera  =-output.position;
   output.position=mul(output.position,projection);
   //--- transform normals
   output.normal = mul(input.normal, transform);
   output.normal = mul(output.normal, view);
   output.normal = normalize(output.normal);
   //--- color and texture coordinates
   output.tcoord   =input.tcoord;
   output.color    =input.color;
//---
   return(output);
  }
//+------------------------------------------------------------------+
