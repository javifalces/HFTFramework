//+------------------------------------------------------------------+
//|                                                   MatrixMult.mq5 |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2016-2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"
#include <OpenCL/OpenCL.mqh>
//--- OpenCL kernels
#resource "Kernels/matrixmult.cl" as string cl_program
#define BLOCK_SIZE 10
//+------------------------------------------------------------------+
//| MatrixMult_CPU                                                   |
//+------------------------------------------------------------------+
bool MatrixMult_CPU(const double &matrix_a[],const double &matrix_b[],double &matrix_c[],
                    const int rows_a,const int cols_a,const int cols_b,ulong &time_cpu)
  {
   int size=rows_a*cols_b;
   if(ArrayResize(matrix_c,size)!=size)
      return(false);
//--- CPU calculation started
   time_cpu=GetMicrosecondCount();
   for(int i=0; i<rows_a; i++)
     {
      for(int j=0; j<cols_b; j++)
        {
         double sum=0.0;
         for(int k=0; k<cols_a; k++)
           {
            sum+=matrix_a[cols_a*i+k]*matrix_b[cols_b*k+j];
           }
         matrix_c[cols_b*i+j]=sum;
        }
     }
//--- CPU calculation finished
   time_cpu=ulong((GetMicrosecondCount()-time_cpu)/1000);
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| MatrixMult_GPU                                                   |
//+------------------------------------------------------------------+
bool MatrixMult_GPU(const double &matrix_a[],const double &matrix_b[],double &matrix1_c[],double &matrix2_c[],
                    const int rows_a,const int cols_a,const int cols_b,const int size_a,const int size_b,
                    const int size_c,ulong &time1_gpu,ulong &time2_gpu)
  {
   const int task_dimension=2;
//--- prepare matrices for result
   if(ArrayResize(matrix1_c,size_c)!=size_c || ArrayResize(matrix2_c,size_c)!=size_c)
      return(false);
   ArrayFill(matrix1_c,0,size_c,(double)0.0);
   ArrayFill(matrix2_c,0,size_c,(double)0.0);
//--- OpenCL
   COpenCL OpenCL;
   if(!OpenCL.Initialize(cl_program,true))
     {
      PrintFormat("Error in OpenCL initialization. Error code=%d",GetLastError());
      return(false);
     }
//--- check support working with double
   if(!OpenCL.SupportDouble())
     {
      PrintFormat("Working with double (cl_khr_fp64) is not supported on the device.");
      return(false);
     }
//--- create kernels
   OpenCL.SetKernelsCount(2);
   OpenCL.KernelCreate(0,"MatrixMult_GPU1");
   OpenCL.KernelCreate(1,"MatrixMult_GPU2");
//--- create buffers
   OpenCL.SetBuffersCount(3);
//---
   if(!OpenCL.BufferFromArray(0,matrix_a,0,size_a,CL_MEM_READ_ONLY))
     {
      PrintFormat("Error in BufferFromArray for matrix A. Error code=%d",GetLastError());
      return(false);
     }
   if(!OpenCL.BufferFromArray(1,matrix_b,0,size_b,CL_MEM_READ_ONLY))
     {
      PrintFormat("Error in BufferFromArray for matrix B. Error code=%d",GetLastError());
      return(false);
     }
   if(!OpenCL.BufferCreate(2,size_c*sizeof(double),CL_MEM_WRITE_ONLY))
     {
      PrintFormat("Error in BufferCreate for matrix C. Error code=%d",GetLastError());
      return(false);
     }
//--- prepare arguments for kernel 0
   int kernel_index=0;
   OpenCL.SetArgumentBuffer(kernel_index,0,0);
   OpenCL.SetArgumentBuffer(kernel_index,1,1);
   OpenCL.SetArgumentBuffer(kernel_index,2,2);
   OpenCL.SetArgument(kernel_index,3,rows_a);
   OpenCL.SetArgument(kernel_index,4,cols_a);
   OpenCL.SetArgument(kernel_index,5,cols_b);
//--- set task dimension a_rows x b_cols
   uint global_work_size[2];
//--- set dimensions
   global_work_size[0]=rows_a;
   global_work_size[1]=cols_b;
   uint global_work_offset[2]={0,0};
//--- GPU calculation start kernel 0
   time1_gpu=GetMicrosecondCount();
   if(!OpenCL.Execute(kernel_index,task_dimension,global_work_offset,global_work_size))
     {
      PrintFormat("Error in Execute. Error code=%d",GetLastError());
      return(false);
     }
   if(!OpenCL.BufferRead(2,matrix1_c,0,0,size_c))
     {
      PrintFormat("Error in BufferRead for matrix1 C. Error code=%d",GetLastError());
      return(false);
     }
//--- GPU calculation finished
   time1_gpu=ulong((GetMicrosecondCount()-time1_gpu)/1000);

//--- prepare arguments for kernel 1
   kernel_index=1;
//--- set arguments
   OpenCL.SetArgumentBuffer(kernel_index,0,0);
   OpenCL.SetArgumentBuffer(kernel_index,1,1);
   OpenCL.SetArgumentBuffer(kernel_index,2,2);
   OpenCL.SetArgument(kernel_index,3,rows_a);
   OpenCL.SetArgument(kernel_index,4,cols_a);
   OpenCL.SetArgument(kernel_index,5,cols_b);
   uint local_work_size[2];
   local_work_size[0]=BLOCK_SIZE;
   local_work_size[1]=BLOCK_SIZE;
//--- GPU calculation start, kernel1
   time2_gpu=GetMicrosecondCount();
   if(!OpenCL.Execute(kernel_index,task_dimension,global_work_offset,global_work_size,local_work_size))
     {
      PrintFormat("Error in Execute. Error code=%d",GetLastError());
      return(false);
     }
   if(!OpenCL.BufferRead(2,matrix2_c,0,0,size_c))
     {
      PrintFormat("Error in BufferRead for matrix2 C. Error code=%d",GetLastError());
      return(false);
     }
//--- GPU calculation finished
   time2_gpu=ulong((GetMicrosecondCount()-time2_gpu)/1000);
//--- remove OpenCL objects
   OpenCL.Shutdown();
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
//--- matrix A 1000x2000
   int rows_a=1000;
   int cols_a=2000;
//--- matrix B 2000x1000
   int rows_b=cols_a;
   int cols_b=1000;
//--- matrix C 1000x1000
   int rows_c=rows_a;
   int cols_c=cols_b;
//--- matrix A: size=rows_a*cols_a
   int size_a=rows_a*cols_a;
   int size_b=rows_b*cols_b;
   int size_c=rows_c*cols_c;
//--- prepare matrix A
   double matrix_a[];
   ArrayResize(matrix_a,rows_a*cols_a);
   for(int i=0; i<rows_a; i++)
      for(int j=0; j<cols_a; j++)
        {
         matrix_a[i*cols_a+j]=(double)(10*MathRand()/32767);
        }
//--- prepare matrix B
   double matrix_b[];
   ArrayResize(matrix_b,rows_b*cols_b);
   for(int i=0; i<rows_b; i++)
      for(int j=0; j<cols_b; j++)
        {
         matrix_b[i*cols_b+j]=(double)(10*MathRand()/32767);
        }
//--- CPU: calculate matrix product matrix_a*matrix_b
   double matrix_c_cpu[];
   ulong time_cpu=0;
   if(!MatrixMult_CPU(matrix_a,matrix_b,matrix_c_cpu,rows_a,cols_a,cols_b,time_cpu))
     {
      PrintFormat("Error in calculation on CPU. Error code=%d",GetLastError());
      return;
     }
//--- calculate matrix product using GPU
   double matrix_c_gpu_method1[];
   double matrix_c_gpu_method2[];
   ulong time_gpu_method1=0;
   ulong time_gpu_method2=0;
   if(!MatrixMult_GPU(matrix_a,matrix_b,matrix_c_gpu_method1,matrix_c_gpu_method2,rows_a,cols_a,cols_b,size_a,size_b,size_c,time_gpu_method1,time_gpu_method2))
     {
      PrintFormat("Error in calculation on GPU. Error code=%d",GetLastError());
      return;
     }
//--- calculate CPU/GPU ratio
   double CPU_GPU_ratio1=0;
   double CPU_GPU_ratio2=0;
   if(time_gpu_method1!=0)
      CPU_GPU_ratio1=1.0*time_cpu/time_gpu_method1;
   if(time_gpu_method2!=0)
      CPU_GPU_ratio2=1.0*time_cpu/time_gpu_method2;
   PrintFormat("time CPU=%d ms, time GPU global work groups =%d ms, CPU/GPU ratio: %f",time_cpu,time_gpu_method1,CPU_GPU_ratio1);
   PrintFormat("time CPU=%d ms, time GPU local work groups  =%d ms, CPU/GPU ratio: %f",time_cpu,time_gpu_method2,CPU_GPU_ratio2);
//--- check calculations
   double total_error1=0;
   double total_error2=0;
   for(int i=0; i<rows_c; i++)
     {
      for(int j=0; j<cols_c; j++)
        {
         int pos=cols_c*i+j;
         total_error1+=MathAbs(matrix_c_gpu_method1[pos]-matrix_c_cpu[pos]);
         total_error2+=MathAbs(matrix_c_gpu_method2[pos]-matrix_c_cpu[pos]);
        }
     }
   PrintFormat("Total error for method 1 = %f",total_error1);
   PrintFormat("Total error for method 2 = %f",total_error2);
  }
//+------------------------------------------------------------------+
