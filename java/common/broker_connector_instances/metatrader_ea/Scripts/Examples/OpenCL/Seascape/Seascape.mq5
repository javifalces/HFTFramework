//+------------------------------------------------------------------+
//|                                                     Seascape.mq5 |
//|                        Copyright 2016, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2016, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"

#resource "seascape.cl" as string ExtCL

uint ExtSizeX=640;  // Width
uint ExtSizeY=360;  // Height
//+------------------------------------------------------------------+
//| Initialize OpenCL engine                                         |
//+------------------------------------------------------------------+
bool ModelInitialize(int &cl_ctx,int &cl_prg,int &cl_krn,int &cl_mem)
  {
//--- initializing OpenCL objects
   if((cl_ctx=CLContextCreate())==INVALID_HANDLE)
     {
      Print("OpenCL not found");
      return(false);
     }
//--- compile the program
   string build_log;
   if((cl_prg=CLProgramCreate(cl_ctx,ExtCL,build_log))==INVALID_HANDLE)
     {
      //--- free
      CLContextFree(cl_ctx);
      cl_ctx=INVALID_HANDLE;
      //---
      Print("OpenCL program create failed: ",build_log);
      return(false);
     }
//--- kernel
   if((cl_krn=CLKernelCreate(cl_prg,"Seascape"))==INVALID_HANDLE)
     {
      //--- free
      CLProgramFree(cl_prg);
      cl_prg=INVALID_HANDLE;

      CLContextFree(cl_ctx);
      cl_ctx=INVALID_HANDLE;
      //---
      Print("OpenCL kernel create failed");
      return(false);
     }
//--- create buffer
   ExtSizeX=(uint)ChartGetInteger(0,CHART_WIDTH_IN_PIXELS);
   ExtSizeY=(uint)ChartGetInteger(0,CHART_HEIGHT_IN_PIXELS);

   if((cl_mem=CLBufferCreate(cl_ctx,ExtSizeX*ExtSizeY*sizeof(uint),CL_MEM_READ_WRITE))==INVALID_HANDLE)
     {
      //--- free objects
      CLKernelFree(cl_krn);
      cl_krn=INVALID_HANDLE;

      CLProgramFree(cl_prg);
      cl_prg=INVALID_HANDLE;

      CLContextFree(cl_ctx);
      cl_ctx=INVALID_HANDLE;
      //---
      Print("OpenCL buffer create failed");
      return(false);
     }

   CLSetKernelArgMem(cl_krn,1,cl_mem);
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Check and resize the window                                      |
//+------------------------------------------------------------------+
bool ModelResize(const int cl_ctx,const int cl_krn,int &cl_mem)
  {
//--- the same size?
   if(ExtSizeX!=(uint)ChartGetInteger(0,CHART_WIDTH_IN_PIXELS) || ExtSizeY!=(uint)ChartGetInteger(0,CHART_HEIGHT_IN_PIXELS))
     {
      //--- check the dimention
      ExtSizeX=(uint)ChartGetInteger(0,CHART_WIDTH_IN_PIXELS);
      if(ExtSizeX<8)
         ExtSizeX=8;

      ExtSizeY=(uint)ChartGetInteger(0,CHART_HEIGHT_IN_PIXELS);
      if(ExtSizeY<8)
         ExtSizeY=8;
      //--- create buffer
      if(cl_mem!=INVALID_HANDLE)
         CLBufferFree(cl_mem);

      if((cl_mem=CLBufferCreate(cl_ctx,ExtSizeX*ExtSizeY*sizeof(uint),CL_MEM_READ_WRITE))!=INVALID_HANDLE)
        {
         CLSetKernelArgMem(cl_krn,1,cl_mem);
         return(true);
        }
     }
//--- no changes
   return(false);
  }
//+------------------------------------------------------------------+
//| Shutdown OpenCL engine                                           |
//+------------------------------------------------------------------+
void ModelShutdown(int cl_ctx,int cl_prg,int cl_krn,int cl_mem)
  {
//--- remove OpenCL objects
   if(cl_mem!=INVALID_HANDLE)
     {
      CLBufferFree(cl_mem);
      cl_mem=INVALID_HANDLE;
     }

   if(cl_krn!=INVALID_HANDLE)
     {
      CLKernelFree(cl_krn);
      cl_krn=INVALID_HANDLE;
     }

   if(cl_prg!=INVALID_HANDLE)
     {
      CLProgramFree(cl_prg);
      cl_prg=INVALID_HANDLE;
     }

   if(cl_ctx!=INVALID_HANDLE)
     {
      CLContextFree(cl_ctx);
      cl_ctx=INVALID_HANDLE;
     }
//---
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
   int  cl_ctx=INVALID_HANDLE,cl_prg=INVALID_HANDLE;
   int  cl_krn=INVALID_HANDLE,cl_mem=INVALID_HANDLE;
//--- prepare the chart
   ChartSetInteger(0,CHART_SHOW,false);
   ChartRedraw();
//--- initializing OpenCL objects
   if(ModelInitialize(cl_ctx,cl_prg,cl_krn,cl_mem))
     {
      uint   buf[];
      uint   work[2];
      uint   offset[2]={0,0};
      string objname="OpenCL_"+IntegerToString(ChartID());
      string resname="::Seascape_"+IntegerToString(ChartID());
      //--- create object and empty picture
      ObjectCreate(0,objname,OBJ_BITMAP_LABEL,0,0,0);
      ObjectSetInteger(0,objname,OBJPROP_XDISTANCE,0);
      ObjectSetInteger(0,objname,OBJPROP_YDISTANCE,0);

      ArrayResize(buf,ExtSizeX*ExtSizeY);
      ResourceCreate(resname,buf,ExtSizeX,ExtSizeY,0,0,ExtSizeX,COLOR_FORMAT_XRGB_NOALPHA);
      ObjectSetString(0,objname,OBJPROP_BMPFILE,resname);
      //--- render
      work[0]=ExtSizeX;
      work[1]=ExtSizeY;

      for(float time=0;!IsStopped();time+=0.04f)
        {
         //--- check the resolution
         if(ModelResize(cl_ctx,cl_krn,cl_mem))
           {
            work[0]=ExtSizeX;
            work[1]=ExtSizeY;
            ArrayResize(buf,ExtSizeX*ExtSizeY);
           }
         //--- rendering the frame
         CLSetKernelArg(cl_krn,0,time);
         CLExecute(cl_krn,2,offset,work);
         //--- take the frame data, save in memory and draw it
         CLBufferRead(cl_mem,buf);
         ResourceCreate(resname,buf,ExtSizeX,ExtSizeY,0,0,ExtSizeX,COLOR_FORMAT_XRGB_NOALPHA);

         ChartRedraw();
         Sleep(0);
        }
      //--- remove OpenCL objects
      ModelShutdown(cl_ctx,cl_prg,cl_krn,cl_mem);
      //--- remove object
      ObjectDelete(0,objname);
     }
//---
   ChartSetInteger(0,CHART_SHOW,true);
  }
//+------------------------------------------------------------------+
