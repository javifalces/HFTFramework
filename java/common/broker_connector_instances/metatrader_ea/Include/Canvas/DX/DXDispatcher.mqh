//+------------------------------------------------------------------+
//|                                                 DXDispatcher.mqh |
//|                        Copyright 2019, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2019, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
//---
#include "DXObjectBase.mqh"
#include "DXData.mqh"
#include "DXBuffers.mqh"
#include "DXInput.mqh"
#include "DXTexture.mqh"
#include "DXShader.mqh"
//--- default shaders
#resource "Shaders/DefaultShaderVertex.hlsl" as string ExtDefaultShaderVertex;
#resource "Shaders/DefaultShaderPixel.hlsl"  as string ExtDefaultShaderPixel;
//+------------------------------------------------------------------+
//| DX dispatcher which holds all resources                          |
//+------------------------------------------------------------------+
class CDXDispatcher : public CDXObjectBase
  {
protected:
   CDXObjectBase     m_dx_resources;         // DX resources list (Textures, Shaders, etc.)
   //--- default shaders
   CDXShader*        m_default_vs;
   CDXShader*        m_default_ps;

public:
                     CDXDispatcher(void);
                    ~CDXDispatcher(void);
   //--- create/destroy
   bool              Create(int context);
   void              Destroy(void);
   //--- check resources
   void              Check(void);
   //--- get DX Context Handle
   int               DXContext(void) const { return(m_context); }
   //--- create shaders
   CDXShader*        ShaderCreateDefault(ENUM_DX_SHADER_TYPE shader_type);
   CDXShader*        ShaderCreateFromFile(ENUM_DX_SHADER_TYPE shader_type,string path,string entry_point);
   CDXShader*        ShaderCreateFromSource(ENUM_DX_SHADER_TYPE shader_type,string source,string entry_point);
   //--- create buffers
   template<typename TVertex>
   CDXVertexBuffer*  VertexBufferCreate(const TVertex &vertices[],uint start=0,uint count=WHOLE_ARRAY);
   CDXIndexBuffer*   IndexBufferCreate(const uint &indicies[],uint start=0,uint count=WHOLE_ARRAY);
   //--- create shader inputs
   template<typename TInput>
   CDXInput*         InputCreate(void);
   CDXTexture*       TextureCreateFromFile(string path,uint data_x=0,uint data_y=0,uint data_width=0,uint data_height=0);
   CDXTexture*       TextureCreateFromData(ENUM_DX_FORMAT format,uint width,uint height,const uint &data[],uint data_x=0,uint data_y=0,uint data_width=0,uint data_height=0);

private:
   //--- add resource to list
   bool              ResourceAdd(CDXObjectBase *resource);
   //--- check resources
   void              ResourcesCheck(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CDXDispatcher::CDXDispatcher(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CDXDispatcher::~CDXDispatcher(void)
  {
   Destroy();
  }
//+------------------------------------------------------------------+
//| Create                                                           |
//+------------------------------------------------------------------+
bool CDXDispatcher::Create(int context)
  {
//--- check if context already exist
   if(m_context!=INVALID_HANDLE)
      return(false);
//--- save context
   m_context=context;
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Destroy                                                          |
//+------------------------------------------------------------------+
void CDXDispatcher::Destroy(void)
  {
//--- release default shaders
   if(m_default_ps)
     {
      m_default_ps.Release();
      m_default_ps=NULL;
     }
   if(m_default_vs)
     {
      m_default_vs.Release();
      m_default_vs=NULL;
     }
//--- release and delete all DX resources
   while(m_dx_resources.Next())
     {
      CDXHandleShared* resource=(CDXHandleShared*)m_dx_resources.Next();
      resource.Release();
     }
//--- forget context
   m_context=INVALID_HANDLE;
  }
//+------------------------------------------------------------------+
//| Check resources                                                  |
//+------------------------------------------------------------------+
void CDXDispatcher::Check(void)
  {
//---
   CDXHandleShared* resource=(CDXHandleShared*)m_dx_resources.Next();
//--- release and delete all DX resources
   while(CheckPointer(resource)!=POINTER_INVALID)
     {
      CDXHandleShared* next=(CDXHandleShared*)resource.Next();
      //--- if references count 1 or less, then only we hold the resource
      if(resource.References()<=1)
         resource.Release();
      //---
      resource=next;
     }
  }
//+------------------------------------------------------------------+
//| Create default shader of specified type                          |
//+------------------------------------------------------------------+
CDXShader* CDXDispatcher::ShaderCreateDefault(ENUM_DX_SHADER_TYPE shader_type)
  {
   switch(shader_type)
     {
      //--- default pixel shader
      case DX_SHADER_PIXEL:
        {
         if(m_default_ps==NULL)
            m_default_ps=ShaderCreateFromSource(DX_SHADER_PIXEL,ExtDefaultShaderPixel,"PSMain");
         return(m_default_ps);
        }
      //--- default vertex shader
      case DX_SHADER_VERTEX:
        {
         if(m_default_vs==NULL)
           {
            m_default_vs=ShaderCreateFromSource(DX_SHADER_VERTEX,ExtDefaultShaderVertex,"VSMain");
            if(m_default_vs && !m_default_vs.LayoutSet<DXVertex>())
              {
               m_default_vs.Release();
               m_default_vs=NULL;
              }
           }
         return(m_default_vs);
        }
     }
//--- return result
   return(NULL);
  }
//+------------------------------------------------------------------+
//| Create new shader of specified type from file                    |
//+------------------------------------------------------------------+
CDXShader* CDXDispatcher::ShaderCreateFromFile(ENUM_DX_SHADER_TYPE shader_type,string path,string entry_point)
  {
//--- open source file
   int file=FileOpen(path,FILE_READ);
   if(file==INVALID_HANDLE)
      return(NULL);
//--- check file size
   uint size=(uint)FileSize(file);
   FileClose(file);
   if(size>16*1024*1024)
      return(NULL);
//--- prepare buffer
   char buffer[];
   ArrayResize(buffer,size);
//--- read file into buffer
   int read=(int)FileLoad(path,buffer);
   if(read<=0)
      return(NULL);
//--- convert vuffer to string
   string source=CharArrayToString(buffer,0,WHOLE_ARRAY,CP_UTF8);
//--- add shader by source
   return(ShaderCreateFromSource(shader_type,source,entry_point));
  }
//+------------------------------------------------------------------+
//| Create new shader of specified type from source code             |
//+------------------------------------------------------------------+
CDXShader* CDXDispatcher::ShaderCreateFromSource(ENUM_DX_SHADER_TYPE shader_type,string source,string entry_point)
  {
//--- check context
   if(m_context==INVALID_HANDLE)
      return(NULL);
//--- allocate new shader
   CDXShader* shader=new CDXShader();
   if(shader==NULL)
      return(NULL);
//--- create shader
   if(!shader.Create(m_context,shader_type,source,entry_point))
     {
      shader.Release();
      return(NULL);
     }
//--- add shader to resources list
   if(!ResourceAdd(shader))
     {
      shader.Release();
      return(NULL);
     }
//--- return shader
   return(shader);
  }
//+------------------------------------------------------------------+
//| Create vertex buffer                                             |
//+------------------------------------------------------------------+
template<typename TVertex>
CDXVertexBuffer*  CDXDispatcher::VertexBufferCreate(const TVertex &vertices[],uint start=0,uint count=WHOLE_ARRAY)
  {
//--- check context
   if(m_context==INVALID_HANDLE)
      return(NULL);
//--- allocate new buffer
   CDXVertexBuffer* buffer=new CDXVertexBuffer();
   if(buffer==NULL)
      return(NULL);
//--- create buffer
   if(!buffer.Create(m_context,vertices,start,count))
     {
      buffer.Release();
      return(NULL);
     }
//--- add buffer to resources list
   if(!ResourceAdd(buffer))
     {
      buffer.Release();
      return(NULL);
     }
//--- return buffer
   return(buffer);
  }
//+------------------------------------------------------------------+
//| Create vertex buffer                                             |
//+------------------------------------------------------------------+
CDXIndexBuffer* CDXDispatcher::IndexBufferCreate(const uint &indicies[],uint start=0,uint count=WHOLE_ARRAY)
  {
//--- check context
   if(m_context==INVALID_HANDLE)
      return(NULL);
//--- allocate new buffer
   CDXIndexBuffer* buffer=new CDXIndexBuffer();
   if(buffer==NULL)
      return(NULL);
//--- create buffer
   if(!buffer.Create(m_context,indicies,start,count))
     {
      buffer.Release();
      return(NULL);
     }
//--- add buffer to resources list
   if(!ResourceAdd(buffer))
     {
      buffer.Release();
      return(NULL);
     }
//--- return input buffer
   return(buffer);
  }
//+------------------------------------------------------------------+
//| Create shader input buffer                                       |
//+------------------------------------------------------------------+
template<typename TInput>
CDXInput* CDXDispatcher::InputCreate(void)
  {
//--- check context
   if(m_context==INVALID_HANDLE)
      return(NULL);
//--- allocate new input buffer
   CDXInput* input_buffer=new CDXInput();
   if(input_buffer==NULL)
      return(NULL);
//--- create buffer
   if(!input_buffer.Create<TInput>(m_context))
     {
      input_buffer.Release();
      return(NULL);
     }
//--- add buffer to resources list
   if(!ResourceAdd(input_buffer))
     {
      input_buffer.Release();
      return(NULL);
     }
//--- return shader
   return(input_buffer);
  }
//+------------------------------------------------------------------+
//| Create texture from bitmap file                                  |
//+------------------------------------------------------------------+
CDXTexture* CDXDispatcher::TextureCreateFromFile(string path,uint data_x=0,uint data_y=0,uint data_width=0,uint data_height=0)
  {
//--- check context
   if(m_context==INVALID_HANDLE)
      return(NULL);
//--- allocate new texture
   CDXTexture* texture=new CDXTexture();
   if(texture==NULL)
      return(NULL);
//--- create texture
   if(!texture.Create(m_context,path,data_x,data_y,data_width,data_height))
     {
      texture.Release();
      return(NULL);
     }
//--- add texture to resources list
   if(!ResourceAdd(texture))
     {
      texture.Release();
      return(NULL);
     }
//--- return shader
   return(texture);
  }
//+------------------------------------------------------------------+
//| Create texture from raw data, only 32-bit pixel formats supported|
//+------------------------------------------------------------------+
CDXTexture* CDXDispatcher::TextureCreateFromData(ENUM_DX_FORMAT format,uint width,uint height,const uint &data[],uint data_x=0,uint data_y=0,uint data_width=0,uint data_height=0)
  {
//--- check context
   if(m_context==INVALID_HANDLE)
      return(NULL);
//--- allocate new texture
   CDXTexture* texture=new CDXTexture();
   if(texture==NULL)
      return(NULL);
//--- create texture
   if(!texture.Create(m_context,format,width,height,data,data_x,data_y,data_width,data_height))
     {
      texture.Release();
      return(NULL);
     }
//--- add texture to resources list
   if(!ResourceAdd(texture))
     {
      texture.Release();
      return(NULL);
     }
//--- return shader
   return(texture);
  }
//+------------------------------------------------------------------+
//| Add DX resource                                                  |
//+------------------------------------------------------------------+
bool CDXDispatcher::ResourceAdd(CDXObjectBase *resource)
  {
//--- add resource
   if(!CheckPointer(resource))
      return(false);
//---
   CDXObjectBase *last=&m_dx_resources;
   while(CheckPointer(last.Next())!=POINTER_INVALID)
     {
      if(last==resource)
         return(false);
      //---
      last=last.Next();
     }
//---
   resource.Next(NULL);
   resource.Prev(last);
   last.Next(resource);
   return(true);
  }
//+------------------------------------------------------------------+
