//+------------------------------------------------------------------+
//|                                                          FFT.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"

#include <Math/Stat/Math.mqh>
#include <OpenCL/OpenCL.mqh>

#resource "Kernels/fft.cl" as string cl_program
#define kernel_init "fft_init"
#define kernel_stage "fft_stage"
#define kernel_scale "fft_scale"
#define NUM_POINTS 16384
#define FFT_DIRECTION 1
//+------------------------------------------------------------------+
//| Fast Fourier transform and its inverse (both recursively)        |
//| Copyright (C) 2004, Jerome R. Breitenbach.  All rights reserved. |
//| Reference:                                                       |
//| Matthew Scarpino, "OpenCL in Action: How to accelerate graphics  |
//| and computations", Manning, 2012, Chapter 14.                    |
//+------------------------------------------------------------------+
//| Recursive direct FFT transform                                   |
//+------------------------------------------------------------------+
void fft(const int N,float &x_real[],float &x_imag[],float &X_real[],float &X_imag[])
  {
//--- prepare temporary arrays
   float XX_real[],XX_imag[];
   ArrayResize(XX_real,N);
   ArrayResize(XX_imag,N);
//--- calculate FFT by a recursion
   fft_rec(N,0,1,x_real,x_imag,X_real,X_imag,XX_real,XX_imag);
  }
//+------------------------------------------------------------------+
//| Recursive inverse FFT transform                                  |
//+------------------------------------------------------------------+
void ifft(const int N,float &x_real[],float &x_imag[],float &X_real[],float &X_imag[])
  {
   int N2=N/2;  // half the number of points in IFFT
//--- calculate IFFT via reciprocity property of DFT
   fft(N,X_real,X_imag,x_real,x_imag);
   x_real[0]=x_real[0]/N;
   x_imag[0]=x_imag[0]/N;
   x_real[N2]=x_real[N2]/N;
   x_imag[N2]=x_imag[N2]/N;
   for(int i=1; i<N2; i++)
     {
      float tmp0=x_real[i]/N;
      float tmp1=x_imag[i]/N;
      x_real[i]=x_real[N-i]/N;
      x_imag[i]=x_imag[N-i]/N;
      x_real[N-i]=tmp0;
      x_imag[N-i]=tmp1;
     }
  }
//+------------------------------------------------------------------+
//| FFT recursion                                                    |
//+------------------------------------------------------------------+
void fft_rec(const int N,const int offset,const int delta,float &x_real[],float &x_imag[],float &X_real[],float &X_imag[],float &XX_real[],float &XX_imag[])
  {
   static const float TWO_PI=(float)(2*M_PI);
   int N2=N/2;          // half the number of points in FFT
   int k00,k01,k10,k11; // indices for butterflies
   if(N!=2)
     {
      //--- perform recursive step
      //--- calculate two (N/2)-point DFT's
      fft_rec(N2,offset,2*delta,x_real,x_imag,XX_real,XX_imag,X_real,X_imag);
      fft_rec(N2,offset+delta,2*delta,x_real,x_imag,XX_real,XX_imag,X_real,X_imag);
      //--- combine the two (N/2)-point DFT's into one N-point DFT
      for(int k=0; k<N2; k++)
        {
         k00 = offset + k*delta;
         k01 = k00 + N2*delta;
         k10 = offset + 2*k*delta;
         k11 = k10 + delta;
         float cs=(float)MathCos(TWO_PI*k/(float)N);
         float sn=(float)MathSin(TWO_PI*k/(float)N);
         float tmp0 = cs*XX_real[k11] + sn*XX_imag[k11];
         float tmp1 = cs*XX_imag[k11] - sn*XX_real[k11];
         X_real[k01] = XX_real[k10] - tmp0;
         X_imag[k01] = XX_imag[k10] - tmp1;
         X_real[k00] = XX_real[k10] + tmp0;
         X_imag[k00] = XX_imag[k10] + tmp1;
        }
     }
   else
     {
      //--- perform 2-point DFT
      k00=offset;
      k01=k00+delta;
      X_real[k01] = x_real[k00] - x_real[k01];
      X_imag[k01] = x_imag[k00] - x_imag[k01];
      X_real[k00] = x_real[k00] + x_real[k01];
      X_imag[k00] = x_imag[k00] + x_imag[k01];
     }
  }
//+------------------------------------------------------------------+
//| FFT_CPU                                                          |
//+------------------------------------------------------------------+
bool FFT_CPU(int direction,int power,float &data_real[],float &data_imag[],ulong &time_cpu)
  {
//--- calculate the number of points
   int N=1;
   for(int i=0;i<power;i++)
      N*=2;
//---prepare temporary arrays
   float XX_real[],XX_imag[];
   ArrayResize(XX_real,N);
   ArrayResize(XX_imag,N);
//--- CPU calculation start
   time_cpu=GetMicrosecondCount();
   if(direction>0)
      fft(N,data_real,data_imag,XX_real,XX_imag);
   else
      ifft(N,XX_real,XX_imag,data_real,data_imag);
//--- CPU calculation finished
   time_cpu=ulong((GetMicrosecondCount()-time_cpu));
//--- copy calculated data
   ArrayCopy(data_real,XX_real,0,0,WHOLE_ARRAY);
   ArrayCopy(data_imag,XX_imag,0,0,WHOLE_ARRAY);
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| FFT_GPU                                                          |
//+------------------------------------------------------------------+
bool FFT_GPU(int direction,int power,float &data_real[],float &data_imag[],ulong &time_gpu)
  {
//--- calculate the number of points
   int num_points=1;
   for(int i=0;i<power;i++)
      num_points*=2;
//--- prepare data array for GPU calculation
   float data[];
   ArrayResize(data,2*num_points);
   for(int i=0; i<num_points; i++)
     {
      data[2*i]=data_real[i];
      data[2*i+1]=data_imag[i];
     }

   COpenCL OpenCL;
   if(!OpenCL.Initialize(cl_program,true))
     {
      PrintFormat("Error in OpenCL initialization. Error code=%d",GetLastError());
      return(false);
     }
//--- create kernels
   OpenCL.SetKernelsCount(3);
   OpenCL.KernelCreate(0,kernel_init);
   OpenCL.KernelCreate(1,kernel_stage);
   OpenCL.KernelCreate(2,kernel_scale);
//--- create buffers
   OpenCL.SetBuffersCount(2);
   if(!OpenCL.BufferFromArray(0,data,0,2*num_points,CL_MEM_READ_ONLY))
     {
      PrintFormat("Error in BufferFromArray for input buffer. Error code=%d",GetLastError());
      return(false);
     }
   if(!OpenCL.BufferCreate(1,2*num_points*sizeof(float),CL_MEM_READ_WRITE))
     {
      PrintFormat("Error in BufferCreate for data buffer. Error code=%d",GetLastError());
      return(false);
     }
//--- determine maximum work-group size 
   int local_size=(int)CLGetInfoInteger(OpenCL.GetKernel(0),CL_KERNEL_WORK_GROUP_SIZE);
//--- determine local memory size 
   uint local_mem_size=(uint)CLGetInfoInteger(OpenCL.GetContext(),CL_DEVICE_LOCAL_MEM_SIZE);
   int points_per_group=(int)local_mem_size/(2*sizeof(float));
   if(points_per_group>num_points)
      points_per_group=num_points;
//--- set kernel arguments
   OpenCL.SetArgumentBuffer(0,0,0);
   OpenCL.SetArgumentBuffer(0,1,1);
   OpenCL.SetArgumentLocalMemory(0,2,local_mem_size);
   OpenCL.SetArgument(0,3,points_per_group);
   OpenCL.SetArgument(0,4,num_points);
   OpenCL.SetArgument(0,5,direction);
//--- OpenCL execute settings
   int task_dimension=1;
   uint global_size=(uint)((num_points/points_per_group)*local_size);
   uint global_work_offset[1]={0};
   uint global_work_size[1];
   global_work_size[0]=global_size;
   uint local_work_size[1];
   local_work_size[0]=local_size;
//--- GPU calculation start
   time_gpu=GetMicrosecondCount();
//-- execute kernel fft_init
   if(!OpenCL.Execute(0,task_dimension,global_work_offset,global_work_size,local_work_size))
     {
      PrintFormat("fft_init: Error in CLExecute. Error code=%d",GetLastError());
      return(false);
     }
//-- further stages of the FFT 
   if(num_points>points_per_group)
     {
      //--- set arguments for kernel 1
      OpenCL.SetArgumentBuffer(1,0,1);
      OpenCL.SetArgument(1,2,points_per_group);
      OpenCL.SetArgument(1,3,direction);
      for(int stage=2; stage<=num_points/points_per_group; stage<<=1)
        {
         OpenCL.SetArgument(1,1,stage);
         //-- execute kernel fft_stage
         if(!OpenCL.Execute(1,task_dimension,global_work_offset,global_work_size,local_work_size))
           {
            PrintFormat("fft_stage: Error in CLExecute. Error code=%d",GetLastError());
            return(false);
           }
        }
     }
//--- scale values if performing the inverse FFT 
   if(direction<0)
     {
      OpenCL.SetArgumentBuffer(2,0,1);
      OpenCL.SetArgument(2,1,points_per_group);
      OpenCL.SetArgument(2,2,num_points);
      //-- execute kernel fft_scale
      if(!OpenCL.Execute(2,task_dimension,global_work_offset,global_work_size,local_work_size))
        {
         PrintFormat("fft_scale: Error in CLExecute. Error code=%d",GetLastError());
         return(false);
        }
     }
//--- read the results from GPU memory
   if(!OpenCL.BufferRead(1,data,0,0,2*num_points))
     {
      PrintFormat("Error in BufferRead for data_buffer2. Error code=%d",GetLastError());
      return(false);
     }
//--- GPU calculation finished
   time_gpu=ulong((GetMicrosecondCount()-time_gpu));
//--- copy calculated data and release OpenCL handles
   for(int i=0; i<num_points; i++)
     {
      data_real[i]=data[2*i];
      data_imag[i]=data[2*i+1];
     }
   OpenCL.Shutdown();
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
   int datacount=NUM_POINTS;
   int power=(int)(MathLog(NUM_POINTS)/M_LN2);
   if(MathPow(2,power)!=datacount)
     {
      PrintFormat("Number of elements must be power of 2. Elements: %d",datacount);
      return;
     }
//--- prepare data for FFT calculation
   float data_real[],data_imag[];
   ArrayResize(data_real,datacount);
   ArrayResize(data_imag,datacount);
   for(int i=0; i<datacount; i++)
     {
      data_real[i]=(float)i;
      data_imag[i]=0;
     }

   int direction=FFT_DIRECTION;
//--- data arrays for CPU calculation
   float CPU_real[],CPU_imag[];
   ArrayCopy(CPU_real,data_real,0,0,WHOLE_ARRAY);
   ArrayCopy(CPU_imag,data_imag,0,0,WHOLE_ARRAY);
   ulong time_cpu=0;
//--- calculate FFT using CPU
   FFT_CPU(direction,power,CPU_real,CPU_imag,time_cpu);

//--- data arrays for GPU calculation
   float GPU_real[],GPU_imag[];
   ArrayCopy(GPU_real,data_real,0,0,WHOLE_ARRAY);
   ArrayCopy(GPU_imag,data_imag,0,0,WHOLE_ARRAY);
   ulong time_gpu=0;
//--- calculate FFT using GPU
   if(!FFT_GPU(direction,power,GPU_real,GPU_imag,time_gpu))
     {
      PrintFormat("Error in calculation FFT on GPU.");
      return;
     }
//--- calculate CPU/GPU ratio
   double CPU_GPU_ratio=0;
   if(time_gpu!=0)
      CPU_GPU_ratio=1.0*time_cpu/time_gpu;
   PrintFormat("FFT calculation for %d points.",datacount);
   PrintFormat("time CPU=%d microseconds, time GPU =%d microseconds, CPU/GPU ratio: %f",time_cpu,time_gpu,CPU_GPU_ratio);
//--- determine average error
   float average_error=0.0;
   for(int i=0; i<datacount; i++)
     {
      average_error += (float)MathAbs(CPU_real[i]-GPU_real[i]);
      average_error += (float)MathAbs(CPU_imag[i]-GPU_imag[i]);
     }
   average_error=average_error/(datacount*2);
   PrintFormat("Average error = %f",average_error);
  }
//+------------------------------------------------------------------+
