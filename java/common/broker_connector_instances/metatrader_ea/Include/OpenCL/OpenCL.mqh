//+------------------------------------------------------------------+
//|                                                       OpenCL.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
//| Class for working with OpenCL                                    |
//+------------------------------------------------------------------+
class COpenCL
  {
protected:
   int               m_context;
   int               m_program;
   //--- kernel
   string            m_kernel_names[];
   int               m_kernels[];
   int               m_kernels_total;
   //--- buffers
   int               m_buffers[];
   int               m_buffers_total;
   string            m_device_extensions;
   bool              m_support_cl_khr_fp64;
public:
   //--- constructor/destructor
                     COpenCL();
                    ~COpenCL();
   //--- get handles
   int               GetContext(void)    const { return(m_context);             }
   int               GetProgram(void)    const { return(m_program);             }
   int               GetKernel(const int kernel_index) const;
   string            GetKernelName(const int kernel_index) const;
   //--- global memory size
   bool              GetGlobalMemorySize(long &global_memory_size);
   //--- check support working with double
   bool              SupportDouble(void) const { return(m_support_cl_khr_fp64); }
   //--- initialization and shutdown
   bool              Initialize(const string program,const bool show_log=true);
   void              Shutdown();
   //--- set buffers/kernels count
   bool              SetBuffersCount(const int total_buffers);
   bool              SetKernelsCount(const int total_kernels);
   //--- kernel operations
   bool              KernelCreate(const int kernel_index,const string kernel_name);
   bool              KernelFree(const int kernel_index);
   //--- buffer operations
   bool              BufferCreate(const int buffer_index,const uint size_in_bytes,const uint flags);
   bool              BufferFree(const int buffer_index);
   template<typename T>
   bool              BufferFromArray(const int buffer_index,T &data[],const uint data_array_offset,const uint data_array_count,const uint flags);
   template<typename T>
   bool              BufferRead(const int buffer_index,T &data[],const uint cl_buffer_offset,const uint data_array_offset,const uint data_array_count);
   template<typename T>
   bool              BufferWrite(const int buffer_index,T &data[],const uint cl_buffer_offset,const uint data_array_offset,const uint data_array_count);
   //--- set kernel arguments
   template<typename T>
   bool              SetArgument(const int kernel_index,const int arg_index,T value);
   bool              SetArgumentBuffer(const int kernel_index,const int arg_index,const int buffer_index);
   bool              SetArgumentLocalMemory(const int kernel_index,const int arg_index,const int local_memory_size);
   //--- kernel execution
   bool              Execute(const int kernel_index,const int work_dim,const uint &work_offset[],const uint &work_size[]);
   bool              Execute(const int kernel_index,const int work_dim,const uint &work_offset[],const uint &work_size[],const uint &local_work_size[]);
  };
//+------------------------------------------------------------------+
//| COpenCL class constructor                                        |
//+------------------------------------------------------------------+
COpenCL::COpenCL()
  {
   m_context=INVALID_HANDLE;
   m_program=INVALID_HANDLE;
   m_buffers_total=0;
   m_kernels_total=0;
   m_device_extensions="";
   m_support_cl_khr_fp64=false;
  }
//+------------------------------------------------------------------+
//| COpenCL class destructor                                         |
//+------------------------------------------------------------------+
COpenCL::~COpenCL()
  {
   Shutdown();
  }
//+------------------------------------------------------------------+
//| GetKernel                                                        |
//+------------------------------------------------------------------+
int COpenCL::GetKernel(const int kernel_index) const
  {
   if(m_kernels_total<=0 || kernel_index<0 || kernel_index>=m_kernels_total)
      return(INVALID_HANDLE);
//---
   return m_kernels[kernel_index];
  }
//+------------------------------------------------------------------+
//| GetKernelName                                                    |
//+------------------------------------------------------------------+
string COpenCL::GetKernelName(const int kernel_index) const
  {
   if(m_kernels_total<=0 || kernel_index<0 || kernel_index>=m_kernels_total)
      return("");
//---
   return m_kernel_names[kernel_index];
  }
//+------------------------------------------------------------------+
//| GetGlobalMemorySize                                              |
//+------------------------------------------------------------------+
bool COpenCL::GetGlobalMemorySize(long &global_memory_size)
  {
   if(m_context==INVALID_HANDLE)
      return(false);

//--- get global memory size
   global_memory_size=CLGetInfoInteger(m_context,CL_DEVICE_GLOBAL_MEM_SIZE);
   if(global_memory_size==-1)
      return(false);
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize                                                       |
//+------------------------------------------------------------------+
bool COpenCL::Initialize(const string program,const bool show_log=true)
  {
   if((m_context=CLContextCreate(CL_USE_ANY))==INVALID_HANDLE)
     {
      Print("OpenCL not found. Error code=",GetLastError());
      return(false);
     }
//--- check support working with doubles (cl_khr_fp64)
   if(CLGetInfoString(m_context,CL_DEVICE_EXTENSIONS,m_device_extensions))
     {
      string extenstions[];
      StringSplit(m_device_extensions,' ',extenstions);
      m_support_cl_khr_fp64=false;
      int size=ArraySize(extenstions);
      for(int i=0; i<size; i++)
        {
         if(extenstions[i]=="cl_khr_fp64")
            m_support_cl_khr_fp64=true;
        }
     }
//--- compile the program
   string build_error_log;
   if((m_program=CLProgramCreate(m_context,program,build_error_log))==INVALID_HANDLE)
     {
      if(show_log)
        {
         string loglines[];
         StringSplit(build_error_log,'\n',loglines);
         int lines_count=ArraySize(loglines);
         for(int i=0; i<lines_count; i++)
            Print(loglines[i]);
        }
      CLContextFree(m_context);
      Print("OpenCL program create failed. Error code=",GetLastError());
      return(false);
     }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Shutdown                                                         |
//+------------------------------------------------------------------+
void COpenCL::Shutdown()
  {
//--- remove buffers
   if(m_buffers_total>0)
     {
      for(int i=0; i<m_buffers_total; i++)
         BufferFree(i);
      m_buffers_total=0;
     }
//--- remove buffers
   if(m_kernels_total>0)
     {
      for(int i=0; i<m_kernels_total; i++)
         KernelFree(i);
      m_kernels_total=0;
     }
//--- remove program and context
   if(m_program!=INVALID_HANDLE)
     {
      CLProgramFree(m_program);
      m_program=INVALID_HANDLE;
     }
   if(m_context!=INVALID_HANDLE)
     {
      CLContextFree(m_context);
      m_context=INVALID_HANDLE;
     }
  }
//+------------------------------------------------------------------+
//| SetBuffersCount                                                  |
//+------------------------------------------------------------------+
bool COpenCL::SetBuffersCount(const int total_buffers)
  {
   if(total_buffers<=0)
      return(false);
//---
   m_buffers_total=total_buffers;
   if(ArraySize(m_buffers)<m_buffers_total)
      ArrayResize(m_buffers,m_buffers_total);
   for(int i=0; i<m_buffers_total; i++)
      m_buffers[i]=INVALID_HANDLE;
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| SetKernelsCount                                                  |
//+------------------------------------------------------------------+
bool COpenCL::SetKernelsCount(const int total_kernels)
  {
   if(total_kernels<=0)
      return(false);
//---
   m_kernels_total=total_kernels;
   if(ArraySize(m_kernels)<m_kernels_total)
      ArrayResize(m_kernels,m_kernels_total);
   if(ArraySize(m_kernel_names)<m_kernels_total)
      ArrayResize(m_kernel_names,m_kernels_total);
//---
   for(int i=0; i<m_kernels_total; i++)
     {
      m_kernel_names[i]="";
      m_kernels[i]=INVALID_HANDLE;
     }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| KernelCreate                                                     |
//+------------------------------------------------------------------+
bool COpenCL::KernelCreate(const int kernel_index,const string kernel_name)
  {
   if(m_context==INVALID_HANDLE || m_program==INVALID_HANDLE)
      return(false);
//--- check parameters
   if(kernel_index<0 || kernel_index>=m_kernels_total)
      return(false);
//---
   int kernel_handle=m_kernels[kernel_index];
   if(kernel_handle==INVALID_HANDLE || m_kernel_names[kernel_index]!=kernel_name)
     {
      //--- create kernel
      if((kernel_handle=CLKernelCreate(m_program,kernel_name))==INVALID_HANDLE)
        {
         CLProgramFree(m_program);
         CLContextFree(m_context);
         Print("OpenCL kernel create failed. Error code=",GetLastError());
         return(false);
        }
      else
        {
         m_kernels[kernel_index]=kernel_handle;
         m_kernel_names[kernel_index]=kernel_name;
        }
     }
   return(true);
  }
//+------------------------------------------------------------------+
//| KernelFree                                                       |
//+------------------------------------------------------------------+
bool COpenCL::KernelFree(const int kernel_index)
  {
//--- check kernel index
   if(kernel_index<0 || kernel_index>=m_kernels_total)
      return(false);
   if(m_kernels[kernel_index]==INVALID_HANDLE)
      return(false);
//--- free kernel handle
   CLKernelFree(m_kernels[kernel_index]);
   m_kernels[kernel_index]=INVALID_HANDLE;
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| BufferCreate                                                     |
//+------------------------------------------------------------------+
bool COpenCL::BufferCreate(const int buffer_index,const uint size_in_bytes,const uint flags)
  {
//--- check parameters
   if(buffer_index<0 || buffer_index>=m_buffers_total)
      return(false);

   if(m_context==INVALID_HANDLE || m_program==INVALID_HANDLE)
      return(false);
//---
   int buffer_handle=CLBufferCreate(m_context,size_in_bytes,flags);
   if(buffer_handle!=INVALID_HANDLE)
     {
      m_buffers[buffer_index]=buffer_handle;
      return(true);
     }
   else
      return(false);
  }
//+------------------------------------------------------------------+
//| BufferFree                                                       |
//+------------------------------------------------------------------+
bool COpenCL::BufferFree(const int buffer_index)
  {
//--- check buffer index
   if(buffer_index<0 || buffer_index>=m_buffers_total)
      return(false);
   if(m_buffers[buffer_index]==INVALID_HANDLE)
      return(false);
//--- free buffer handle
   CLBufferFree(m_buffers[buffer_index]);
   m_buffers[buffer_index]=INVALID_HANDLE;
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| BufferFromArray                                                  |
//+------------------------------------------------------------------+
template<typename T>
bool COpenCL::BufferFromArray(const int buffer_index,T &data[],const uint data_array_offset,const uint data_array_count,const uint flags)
  {
//--- check parameters
   if(m_context==INVALID_HANDLE || m_program==INVALID_HANDLE)
      return(false);
   if(buffer_index<0 || buffer_index>=m_buffers_total || data_array_count<=0)
      return(false);

//--- buffer does not exists, create it
   if(m_buffers[buffer_index]==INVALID_HANDLE)
     {
      uint size_in_bytes=data_array_count*sizeof(T);
      int buffer_handle=CLBufferCreate(m_context,size_in_bytes,flags);
      if(buffer_handle!=INVALID_HANDLE)
        {
         m_buffers[buffer_index]=buffer_handle;
        }
      else
         return(false);
     }
//--- write data to OpenCL buffer
   uint data_written=CLBufferWrite(m_buffers[buffer_index],data,0,data_array_offset,data_array_count);
   if(data_written!=data_array_count)
      return(false);
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| BufferRead                                                       |
//+------------------------------------------------------------------+
template<typename T>
bool COpenCL::BufferRead(const int buffer_index,T &data[],const uint cl_buffer_offset,const uint data_array_offset,const uint data_array_count)
  {
//--- check parameters
   if(buffer_index<0 || buffer_index>=m_buffers_total || data_array_count<=0)
      return(false);
   if(m_buffers[buffer_index]==INVALID_HANDLE)
      return(false);
   if(m_context==INVALID_HANDLE || m_program==INVALID_HANDLE)
      return(false);
//--- read data from OpenCL buffer
   uint data_read=CLBufferRead(m_buffers[buffer_index],data,cl_buffer_offset,data_array_offset,data_array_count);
   if(data_read!=data_array_count)
      return(false);
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| BufferWrite                                                      |
//+------------------------------------------------------------------+
template<typename T>
bool COpenCL::BufferWrite(const int buffer_index,T &data[],const uint cl_buffer_offset,const uint data_array_offset,const uint data_array_count)
  {
//--- check parameters
   if(buffer_index<0 || buffer_index>=m_buffers_total || data_array_count<=0)
      return(false);
   if(m_buffers[buffer_index]==INVALID_HANDLE)
      return(false);
   if(m_context==INVALID_HANDLE || m_program==INVALID_HANDLE)
      return(false);
//--- write data to OpenCL buffer
   uint data_written=CLBufferWrite(m_buffers[buffer_index],data,cl_buffer_offset,data_array_offset,data_array_count);
   if(data_written!=data_array_count)
      return(false);
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| SetArgument                                                      |
//+------------------------------------------------------------------+
template<typename T>
bool COpenCL::SetArgument(const int kernel_index,const int arg_index,T value)
  {
   if(kernel_index<0 || kernel_index>=m_kernels_total)
      return(false);

   int kernel_handle=m_kernels[kernel_index];
   if(kernel_handle==INVALID_HANDLE)
      return(false);
//---
   return CLSetKernelArg(kernel_handle,arg_index,value);
  }
//+------------------------------------------------------------------+
//| SetArgumentBuffer                                                |
//+------------------------------------------------------------------+
bool COpenCL::SetArgumentBuffer(const int kernel_index,const int arg_index,const int buffer_index)
  {
   if(m_context==INVALID_HANDLE || m_program==INVALID_HANDLE)
      return(false);
   if(kernel_index<0 || kernel_index>=m_kernels_total)
      return(false);
   if(buffer_index<0 || buffer_index>=m_buffers_total)
      return(false);
   if(m_buffers[buffer_index]==INVALID_HANDLE)
      return(false);
//---
   return CLSetKernelArgMem(m_kernels[kernel_index],arg_index,m_buffers[buffer_index]);
  }
//+------------------------------------------------------------------+
//| SetArgumentLocalMemory                                           |
//+------------------------------------------------------------------+
bool COpenCL::SetArgumentLocalMemory(const int kernel_index,const int arg_index,const int local_memory_size)
  {
   if(m_context==INVALID_HANDLE || m_program==INVALID_HANDLE)
      return(false);
   if(kernel_index<0 || kernel_index>=m_kernels_total)
      return(false);
//--- check device local memory size 
   long device_local_memory_size=CLGetInfoInteger(m_context,CL_DEVICE_LOCAL_MEM_SIZE);
   if(local_memory_size>device_local_memory_size)
      return(false);
//---
   return CLSetKernelArgMemLocal(m_kernels[kernel_index],arg_index,local_memory_size);
  }
//+------------------------------------------------------------------+
//| Execute                                                          |
//+------------------------------------------------------------------+
bool COpenCL::Execute(const int kernel_index,const int work_dim,const uint &work_offset[],const uint &work_size[])
  {
   if(kernel_index<0 || kernel_index>=m_kernels_total)
      return(false);
   int kernel_handle=m_kernels[kernel_index];
   if(kernel_handle==INVALID_HANDLE)
      return(false);
//---
   return CLExecute(kernel_handle,work_dim,work_offset,work_size);
  }
//+------------------------------------------------------------------+
//| Execute                                                          |
//+------------------------------------------------------------------+
bool COpenCL::Execute(const int kernel_index,const int work_dim,const uint &work_offset[],const uint &work_size[],const uint &local_work_size[])
  {
   if(kernel_index<0 || kernel_index>=m_kernels_total)
      return(false);
//---
   return CLExecute(m_kernels[kernel_index],work_dim,work_offset,work_size,local_work_size);
  }
//+------------------------------------------------------------------+
