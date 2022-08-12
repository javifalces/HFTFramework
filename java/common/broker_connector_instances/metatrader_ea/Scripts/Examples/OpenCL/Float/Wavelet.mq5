//+------------------------------------------------------------------+
//|                                                      Wavelet.mq5 |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2016-2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"

#include <Math/Stat/Math.mqh>
#include <Graphics/Graphic.mqh>
#include <OpenCL/OpenCL.mqh>

#define CPU_DATA 1
#define GPU_DATA 2

#define SIZE_X 600
#define SIZE_Y 200

#resource "Kernels/wavelet.cl" as string cl_program
//+------------------------------------------------------------------+
//| CWavelet                                                         |
//+------------------------------------------------------------------+
class CWavelet
  {
protected:
   int               m_xsize;
   int               m_ysize;
   int               m_maxcolor;
   string            m_res_name;
   string            m_label_name;
   uchar             m_palette[3*256];
   //---
   float             m_data[];
   float             m_wavelet_data_CPU[];
   float             m_wavelet_data_GPU[];
   uint              m_bmp_buffer[];

   COpenCL           m_OpenCL;

   float             Morlet(const float t);
   void              ShowWaveletData(const float &m_wavelet_data[]);
   int               GetPalColor(const int index);
   void              Blend(const uint c1,const uint c2,const uint r1,const uint g1,const uint b1,const uint r2,const uint g2,const uint b2);
   bool              WaveletCPU(const float &data[],const int datacount,const int x_size,const int y_size,const int i,const int j,const bool norm,float &result[]);
public:
   //---
   void              Create(const string name,const int x0,const int y0,const int x_size,const int y_size);
   bool              CalculateWavelet_CPU(const float &data[],uint &time);
   bool              CalculateWavelet_GPU(float &data[],uint &time);
   void              ShowWavelet(const int mode);
  };
//+------------------------------------------------------------------+
//| Morlet wavelet function                                          |
//+------------------------------------------------------------------+
float CWavelet::Morlet(const float t)
  {
   double v=t;
   double res=MathExp(-v*v*0.5)*MathCos(M_2_PI*v);
   return ((float)res);
  }
//+------------------------------------------------------------------+
//| GetPalColor                                                      |
//+------------------------------------------------------------------+
int CWavelet::GetPalColor(const int index)
  {
   int ind=index;
   if(ind<=0)
      ind=0;
   if(ind>255)
      ind=255;
   int idx=3*(ind);
   uchar r=m_palette[idx];
   uchar g=m_palette[idx+1];
   uchar b=m_palette[idx+2];
//---
   return(b+256*g+65536*r);
  }
//+------------------------------------------------------------------+
//| Gradient palette                                                 |
//+------------------------------------------------------------------+
void CWavelet::Blend(const uint c1,const uint c2,const uint r1,const uint g1,const uint b1,const uint r2,const uint g2,const uint b2)
  {
   int n=int(c2-c1);
   for(int i=0; i<=n; i++)
     {
      if((c1+i+2)<ArraySize(m_palette))
        {
         m_palette[3*(c1+i)]=uchar(MathRound(1*(r1*(n-i)+r2*i)*1.0/n));
         m_palette[3*(c1+i)+1]=uchar(MathRound(1*(g1*(n-i)+g2*i)*1.0/n));
         m_palette[3*(c1+i)+2]=uchar(MathRound(1*(b1*(n-i)+b2*i)*1.0/n));
        }
     }
  }
//+------------------------------------------------------------------+
//| Create                                                           |
//+------------------------------------------------------------------+
void CWavelet::Create(const string name,const int x0,const int y0,const int x_size,const int y_size)
  {
//---
   m_xsize=x_size;
   m_ysize=y_size;
   int size=m_xsize*m_ysize;
   ArrayResize(m_bmp_buffer,size);
   ArrayFill(m_bmp_buffer,0,size,0);
   ArrayResize(m_wavelet_data_CPU,size);
   ArrayResize(m_wavelet_data_GPU,size);
   ArrayFill(m_wavelet_data_CPU,0,size,0);
   ArrayFill(m_wavelet_data_GPU,0,size,0);
   m_res_name=name;
   m_label_name=m_res_name;
   StringToUpper(m_label_name);
   ResourceCreate(m_res_name,m_bmp_buffer,m_xsize,m_ysize,0,0,0,COLOR_FORMAT_XRGB_NOALPHA);
   ObjectCreate(0,m_label_name,OBJ_BITMAP_LABEL,0,0,0);
   ObjectSetInteger(0,m_label_name,OBJPROP_XDISTANCE,x0);
   ObjectSetInteger(0,m_label_name,OBJPROP_YDISTANCE,y0);
   ObjectSetString(0,m_label_name,OBJPROP_BMPFILE,NULL);
   ObjectSetString(0,m_label_name,OBJPROP_BMPFILE,"::"+m_label_name);
   m_maxcolor=100;
//Blend(0,100,0,0,0,255,255,255);
   Blend(0,20,0,0,95,0,0,246);
   Blend(21,40,0,0,246,0,236,226);
   Blend(41,60,0,236,226,226,246,0);
   Blend(61,80,226,246,0,226,0,0);
   Blend(81,100,226,0,0,123,0,0);
  }
//+------------------------------------------------------------------+
//| WaveletCPU                                                       |
//+------------------------------------------------------------------+
bool CWavelet::WaveletCPU(const float &data[],const int datacount,const int x_size,const int y_size,const int i,const int j,const bool norm,float &result[])
  {
   float a1=(float)10e-10;
   float a2=(float)15.0;
   float da=(float)(a2-a1)/y_size;
   float db=(float)(datacount-0)/x_size;
   int pos=j*x_size+i;
//---
   float a=a1+j*da;
   float b=i*db;
   float B=(float)1.0; //Morlet
   float B_inv=(float)1.0/B;
   float a_inv=(float)1/a;
   float dt=(float)1.0;
   float coef=(float)0.0;
   if(!norm)
      coef=(float)MathSqrt(a_inv);
   else
     {
      for(int k=0; k<datacount; k++)
        {
         float arg=(dt*k-b)*a_inv;
         arg=-B_inv*arg*arg;
         coef+=(float)MathExp(arg);
        }
     }
   float sum=0.0;
   for(int k=0; k<datacount; k++)
     {
      float arg=(dt*k-b)*a_inv;
      sum+=data[k]*Morlet(arg);
     }
   sum/=coef;
   result[pos]=sum;
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| CalculateWavelet_CPU                                             |
//+------------------------------------------------------------------+
bool CWavelet::CalculateWavelet_CPU(const float &data[],uint &time)
  {
   time=GetTickCount();
   int datacount=ArraySize(data);
   ArrayCopy(m_data,data,0,0,WHOLE_ARRAY);
   for(int i=0; i<m_xsize; i++)
     {
      for(int j=0; j<m_ysize; j++)
        {
         WaveletCPU(m_data,datacount,m_xsize,m_ysize,i,j,true,m_wavelet_data_CPU);
        }
     }
   time=GetTickCount()-time;
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| CalculateWavelet_GPU                                             |
//+------------------------------------------------------------------+
bool CWavelet::CalculateWavelet_GPU(float &data[],uint &time)
  {
   int datacount=ArraySize(data);
   if(!m_OpenCL.Initialize(cl_program,true))
     {
      PrintFormat("Error in OpenCL initialization. Error code=%d",GetLastError());
      return(false);
     }
//---
   m_OpenCL.SetKernelsCount(1);
   m_OpenCL.KernelCreate(0,"Wavelet_GPU");
//---
   m_OpenCL.SetBuffersCount(2);
   if(!m_OpenCL.BufferFromArray(0,data,0,datacount,CL_MEM_READ_ONLY))
     {
      PrintFormat("Error in BufferFromArray for data array. Error code=%d",GetLastError());
      return(false);
     }
   if(!m_OpenCL.BufferCreate(1,m_xsize*m_ysize*sizeof(float),CL_MEM_READ_WRITE))
     {
      PrintFormat("Error in BufferCreate for data array. Error code=%d",GetLastError());
      return(false);
     }
   m_OpenCL.SetArgumentBuffer(0,0,0);
   m_OpenCL.SetArgumentBuffer(0,4,1);
//---
   ArrayResize(m_wavelet_data_GPU,m_xsize*m_ysize);
   uint work[2];
   uint offset[2]={0,0};
//--- set dimensions
   work[0]=m_xsize;
   work[1]=m_ysize;
//--- set parameters and write data to buffer
   m_OpenCL.SetArgument(0,1,datacount);
   m_OpenCL.SetArgument(0,2,m_xsize);
   m_OpenCL.SetArgument(0,3,m_ysize);
   time=GetTickCount();
//--- GPU calculation start
   if(!m_OpenCL.Execute(0,2,offset,work))
     {
      PrintFormat("Error in Execute. Error code=%d",GetLastError());
      return(false);
     }
   if(!m_OpenCL.BufferRead(1,m_wavelet_data_GPU,0,0,m_xsize*m_ysize))
     {
      PrintFormat("Error in BufferRead for m_wavelet_data_GPU array. Error code=%d",GetLastError());
      return(false);
     }
//--- GPU calculation finish
   time=GetTickCount()-time;
//---
   m_OpenCL.Shutdown();
   return(true);
  }
//+------------------------------------------------------------------+
//| ShowWavelet                                                      |
//+------------------------------------------------------------------+
void CWavelet::ShowWavelet(const int mode)
  {
   if(mode==CPU_DATA)
      ShowWaveletData(m_wavelet_data_CPU);
   else
   if(mode==GPU_DATA)
      ShowWaveletData(m_wavelet_data_GPU);
  }
//+------------------------------------------------------------------+
//| ShowWaveletData                                                  |
//+------------------------------------------------------------------+
void CWavelet::ShowWaveletData(const float &m_wavelet_data[])
  {
//--- calculate min/max and range
   int count=ArraySize(m_wavelet_data);
   float min_value=m_wavelet_data[0];
   float max_value=m_wavelet_data[0];
   for(int i=1; i<count; i++)
     {
      min_value=MathMin(min_value,m_wavelet_data[i]);
      max_value=MathMax(max_value,m_wavelet_data[i]);
     }
   float range=max_value-min_value;
   if(range>0)
     {
      for(int j=0; j<m_ysize; j++)
        {
         for(int i=0; i<m_xsize; i++)
           {
            int pos=j*m_xsize+i;
            int colindex=int(m_maxcolor*(m_wavelet_data[pos]-min_value)/range);
            m_bmp_buffer[pos]=GetPalColor(colindex);
           }
        }
      //--- show image
      ResourceCreate(m_res_name,m_bmp_buffer,m_xsize,m_ysize,0,0,0,COLOR_FORMAT_XRGB_NOALPHA);
      ChartRedraw();
     }
  }
//+------------------------------------------------------------------+
//| Weirstrass function                                              |
//+------------------------------------------------------------------+
float Weirstrass(float x,float a,float b)
  {
   float sum=0.0;
   float b0=b;
   float a0=a;
   for(int n=0; n<35; n++)
     {
      float v=b0*(float)MathCos(a0*M_PI*x);
      sum=sum+v;
      a0=a0*a;
      b0=b0*b;
     }
   return(sum);
  }
//+------------------------------------------------------------------+
//| PrepareModelData                                                 |
//+------------------------------------------------------------------+
void PrepareModelData(float &price_data[],const int datacount)
  {
   ArrayResize(price_data,datacount);
//--- Weirstrass function
   float x1=0;
   float x2=2;
   float dx=(x2-x1)/datacount;
   for(int i=0; i<datacount; i++)
     {
      price_data[i]=Weirstrass(x1+dx*i,(float)3,(float)0.62);
     }
  }
//+------------------------------------------------------------------+
//| PreparePriceData                                                 |
//+------------------------------------------------------------------+
void PreparePriceData(const string symbol,ENUM_TIMEFRAMES timeframe,float &price_data[],const int datacount)
  {
   ArrayResize(price_data,datacount);

   double price_data_double[];
   CopyClose(symbol,timeframe,0,datacount,price_data_double);

   int size=ArraySize(price_data_double);
   for(int i=0; i<size; i++)
     {
      price_data[i]=(float)price_data_double[i];
     }
  }
//+------------------------------------------------------------------+
//| PrepareMomentumData                                              |
//+------------------------------------------------------------------+
void PrepareMomentumData(float &price_data[],float &momentum_data[],const int momentum_period)
  {
   int size=ArraySize(price_data);
   int datacount=size-momentum_period;
//---
   ArrayResize(momentum_data,datacount);
   for(int i=0; i<datacount; i+=1)
     {
      momentum_data[i]=price_data[i+momentum_period]-price_data[i];
     }
   ArrayCopy(price_data,price_data,momentum_period,0,datacount);
   ArrayResize(price_data,datacount);
//--- rescale momentum data
   float min_value=momentum_data[0];
   float max_value=momentum_data[0];
   for(int i=1; i<datacount; i++)
     {
      float value=momentum_data[i];
      if(momentum_data[i]>max_value)
         max_value=value;

      if(momentum_data[i]<min_value)
         min_value=value;
     }
   float range=max_value-min_value;
   for(int i=0; i<datacount; i+=1)
      momentum_data[i]=-1+2*(momentum_data[i]-min_value)/range;
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
//---
   int momentum_period=8;
   float price_data[];
   float momentum_data[];
   PrepareModelData(price_data,SIZE_X+momentum_period);
//PreparePriceData("EURUSD",PERIOD_M1,price_data,SIZE_X+momentum_period);
   PrepareMomentumData(price_data,momentum_data,momentum_period);

   double price_data_double[];
   double momentum_data_double[];

   int datacount=ArraySize(price_data);
   ArrayResize(price_data_double,datacount);
   for(int i=0; i<datacount; i++)
     {
      price_data_double[i]=(double)price_data[i];
     }
   datacount=ArraySize(momentum_data);
   ArrayResize(momentum_data_double,datacount);
   for(int i=0; i<datacount; i++)
     {
      momentum_data_double[i]=(double)momentum_data[i];
     }

   CGraphic graph_price;
   CGraphic graph_momentum;
   graph_price.Create(0,"price",0,0,0,SIZE_X+130+6,SIZE_Y);
   graph_price.XAxis().MaxGrace(0);
   graph_price.HistorySymbolSize(10);
   graph_price.CurveAdd(price_data_double,ColorToARGB(clrRed,255),CURVE_LINES,"Price");
   graph_price.CurvePlotAll();
   graph_price.Redraw(true);
   graph_price.Update();
//---
   graph_momentum.Create(0,"momentum",0,0,SIZE_Y,SIZE_X+130+6,SIZE_Y+SIZE_Y);
   graph_momentum.XAxis().MaxGrace(0);
   graph_momentum.HistorySymbolSize(10);
   graph_momentum.CurveAdd(momentum_data_double,ColorToARGB(clrBlue,255),CURVE_LINES,"Momentum");
   graph_momentum.CurvePlotAll();
   graph_momentum.Redraw(true);
   graph_momentum.Update();
//---
   uint time_cpu=0;
   CWavelet wavelet;
   wavelet.Create("Wavelet",50,2*SIZE_Y,SIZE_X,SIZE_Y);
   if(!wavelet.CalculateWavelet_CPU(momentum_data,time_cpu))
     {
      PrintFormat("Error in calculation on CPU. Error code=%d",GetLastError());
      return;
     }
//wavelet.ShowWavelet(CPU_DATA);
   uint time_gpu=0;
   if(!wavelet.CalculateWavelet_GPU(momentum_data,time_gpu))
     {
      PrintFormat("Error in calculation on GPU. Error code=%d",GetLastError());
      return;
     }
   wavelet.ShowWavelet(GPU_DATA);
//---
   double CPU_GPU_ratio=0;
   if(time_gpu!=0)
      CPU_GPU_ratio=1.0*time_cpu/time_gpu;
//---
   PrintFormat("time CPU=%d ms, time GPU=%d ms, CPU/GPU ratio: %f",time_cpu,time_gpu,CPU_GPU_ratio);
//--- Sleep 10 seconds
   Sleep(10000);
  }
//+------------------------------------------------------------------+
