//+------------------------------------------------------------------+
//|                                                    Remnant3D.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#resource "Shaders/vertex.hlsl" as string ExtShaderVS;
#resource "Shaders/pixel.hlsl" as string ExtShaderPS;
//+------------------------------------------------------------------+
//| Vertex shader input vertex type                                  |
//+------------------------------------------------------------------+
struct VSInputVertex
  {
   //--- data
   float             position[4];
   //--- data layout
   static const DXVertexLayout s_layout[1];
  };
const DXVertexLayout VSInputVertex::s_layout[1]=
  {
     {"POSITION", 0, DX_FORMAT_R32G32B32A32_FLOAT }
  };
//+------------------------------------------------------------------+
//| Pixel shader input buffer type                                   |
//+------------------------------------------------------------------+
struct PSInputBuffer
  {
   float             resolution[2];
   float             time;
   float             dummy;
  };
//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
void OnStart()
  {
//--- prepare the chart
   ChartSetInteger(0,CHART_SHOW,false);
   ChartRedraw();
//--- get chart window size
   int width=(int)ChartGetInteger(0,CHART_WIDTH_IN_PIXELS);
   int height=(int)ChartGetInteger(0,CHART_HEIGHT_IN_PIXELS);
//---
   uint   image[];
   string objname="Reinmant3D_"+IntegerToString(ChartID());
   string resname="::Reinmant3D_"+IntegerToString(ChartID());
//--- create object and empty picture
   ObjectCreate(0,objname,OBJ_BITMAP_LABEL,0,0,0);
   ObjectSetInteger(0,objname,OBJPROP_XDISTANCE,0);
   ObjectSetInteger(0,objname,OBJPROP_YDISTANCE,0);
//--- create resource and assign to image buffer
   ArrayResize(image,width*height);
   ResourceCreate(resname,image,width,height,0,0,width,COLOR_FORMAT_XRGB_NOALPHA);
   ObjectSetString(0,objname,OBJPROP_BMPFILE,resname);
//--- initialize DX context
   int context=DXContextCreate(width,height);
//--- prepare vertices for rectangle from [-1;-1] to [1;1] in screen space
   VSInputVertex vertices[]= {{{-1,-1,0.5,1.0}},{{-1,1,0.5,1.0}},{{1,1,0.5,1.0}},{{1,-1,0.5,1.0}}};
//--- prepare indices for two triangles
   uint indices[]= {0,1,2, 2,3,0};
//--- create vertex and index buffer
   int buffer_v=DXBufferCreate(context,DX_BUFFER_VERTEX,vertices);
   int buffer_i=DXBufferCreate(context,DX_BUFFER_INDEX, indices);
//--- create shaders from sources
   string str="";
   int shader_v=DXShaderCreate(context,DX_SHADER_VERTEX,ExtShaderVS,"VSMain",str);
   int shader_p=DXShaderCreate(context,DX_SHADER_PIXEL, ExtShaderPS,"PSMain",str);
//--- set vertex layout for vertex shader
   DXShaderSetLayout(shader_v,VSInputVertex::s_layout);
//--- create input buffer and set it to pixel shader
   int inputs_p[1];
   inputs_p[0]=DXInputCreate(context,sizeof(PSInputBuffer));
   DXShaderInputsSet(shader_p,inputs_p);
//--- update input buffer data
   PSInputBuffer frame_data;
   frame_data.resolution[0]=(float)width;
   frame_data.resolution[1]=(float)height;
//--- set topology, buffers and shaders
   DXPrimiveTopologySet(context,DX_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
   DXBufferSet(context,buffer_v);
   DXBufferSet(context,buffer_i);
   DXShaderSet(context,shader_v);
   DXShaderSet(context,shader_p);
//--- initialize fps counter
   float fps=25.0;
//--- main loop
   while(!IsStopped())
     {
      //--- check if ESC pressed
      if((TerminalInfoInteger(TERMINAL_KEYSTATE_ESCAPE)&0x8000)!=0)
         break;
      //--- get current chart window size
      int w=(int)ChartGetInteger(0,CHART_WIDTH_IN_PIXELS);
      int h=(int)ChartGetInteger(0,CHART_HEIGHT_IN_PIXELS);
      //--- update size everywhere it needed
      if(w!=width || h!=height)
        {
         width=w;
         height=h;
         frame_data.resolution[0]=(float)width;
         frame_data.resolution[1]=(float)height;
         ArrayResize(image,width*height);
         DXContextSetSize(context,width,height);
        }
      //--- clear depth buffer
      DXContextClearDepth(context);
      //--- update time in shder's input buffer
      frame_data.time=GetMicrosecondCount()/1000000.0f;
      DXInputSet(inputs_p[0],frame_data);
      //--- draw call
      DXDrawIndexed(context);
      //--- get result into buffer
      DXContextGetColors(context,image);
      //--- calculate delta time and smoothed FPS count
      float delta=GetMicrosecondCount()/1000000.0f-frame_data.time;
      fps=0.95f*fps+0.05f/delta;
      TextOut(StringFormat("FPS: %.0f",fps),10,10,0,image,width,height,clrWhite,COLOR_FORMAT_ARGB_NORMALIZE);
      //--- update resource and redraw chart
      ResourceCreate(resname,image,width,height,0,0,width,COLOR_FORMAT_XRGB_NOALPHA);
      ChartRedraw();
      //---
      Sleep(1);
     }
//--- release handles
   DXRelease(inputs_p[0]);
   DXRelease(shader_p);
   DXRelease(shader_v);
   DXRelease(buffer_v);
   DXRelease(buffer_i);
   DXRelease(context);
//--- release resource and object   
   ResourceFree(resname);
   ObjectDelete(0,objname);
//--- revert chart showing mode
   ChartSetInteger(0,CHART_SHOW,true);
  }
//+------------------------------------------------------------------+
