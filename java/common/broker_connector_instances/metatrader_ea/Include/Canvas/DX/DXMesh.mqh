//+------------------------------------------------------------------+
//|                                                       DXMesh.mqh |
//|                        Copyright 2019, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2019, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#include "DXObject.mqh"
#include "DXDispatcher.mqh"
#include "DXData.mqh"
#include "DXUtils.mqh"
//+------------------------------------------------------------------+
//| DX simple mesh object with default shader                        |
//+------------------------------------------------------------------+
class CDXMesh : public CDXObject
  {
protected:
   //--- DX resources
   CDXVertexBuffer*  m_buffer_vertex;
   CDXIndexBuffer*   m_buffer_index;
   CDXInput*         m_buffer_object;
   CDXInput*         m_buffer_scene;
   CDXShader*        m_shader_vertex;
   CDXShader*        m_shader_pixel;
   CDXTexture*       m_texture;
   //--- object data
   DXMatrix          m_transform_matrix;
   DXColor           m_diffuse_color;
   DXColor           m_emission_color;
   DXColor           m_specular_color;
   float             m_specular_power;

   ENUM_DX_PRIMITIVE_TOPOLOGY m_topology;

public:
   //+------------------------------------------------------------------+
   //| Constructor                                                      |
   //+------------------------------------------------------------------+
                     CDXMesh(void):m_buffer_vertex(NULL),m_buffer_index(NULL),m_buffer_object(NULL),m_buffer_scene(NULL),m_shader_vertex(NULL),m_shader_pixel(NULL),m_topology(WRONG_VALUE)
     {
     }
   //+------------------------------------------------------------------+
   //| Destructor                                                       |
   //+------------------------------------------------------------------+
                    ~CDXMesh(void)
     {
      Shutdown();
     }
   //+------------------------------------------------------------------+
   //| Create mesh object from vertices and indices list                |
   //+------------------------------------------------------------------+
   bool              Create(CDXDispatcher &dispatcher,CDXInput* buffer_scene,const DXVertex &vertices[],const uint &indices[],ENUM_DX_PRIMITIVE_TOPOLOGY topology=DX_PRIMITIVE_TOPOLOGY_TRIANGLELIST)
     {
      Shutdown();
      //--- check data
      if(buffer_scene==NULL || buffer_scene.Handle()==INVALID_HANDLE ||
         ArraySize(vertices)<1 || ArraySize(indices)<1)
         return(false);
      //--- update context
      m_context=dispatcher.DXContext();
      //--- initialize default parameters
      DXMatrixIdentity(m_transform_matrix);
      m_diffuse_color =DXColor(1.0,1.0,1.0,1.0);
      m_emission_color=DXColor(0.0,0.0,0.0,0.0);
      m_specular_color=DXColor(0.0,0.0,0.0,0.0);
      m_specular_power=16.0f;
      //--- create object input
      m_buffer_object=dispatcher.InputCreate<DXInputObject>();
      if(m_buffer_object!=NULL)
        {
         m_buffer_object.AddRef();
         //--- use default vertex shader
         m_shader_vertex=dispatcher.ShaderCreateDefault(DX_SHADER_VERTEX);
         if(m_shader_vertex!=NULL)
           {
            m_shader_vertex.AddRef();
            //--- use default pixel shader
            m_shader_pixel=dispatcher.ShaderCreateDefault(DX_SHADER_PIXEL);
            if(m_shader_pixel!=NULL)
              {
               m_shader_pixel.AddRef();
               //--- create vertex buffer
               m_buffer_vertex=dispatcher.VertexBufferCreate(vertices);
               if(m_buffer_vertex!=NULL)
                 {
                  m_buffer_vertex.AddRef();
                  //--- create index buffer
                  m_buffer_index=dispatcher.IndexBufferCreate(indices);
                  if(m_buffer_index!=NULL)
                     m_buffer_index.AddRef();
                 }
              }
           }
        }
      //--- set object buffer to shaders
      if(m_buffer_object==NULL || m_shader_vertex==NULL || m_shader_pixel==NULL || m_buffer_vertex==NULL || m_buffer_index==NULL)
        {
         Shutdown();
         return(false);
        }
      //--- save scene buffer
      m_buffer_scene=buffer_scene;
      m_buffer_scene.AddRef();
      //--- save topology
      m_topology=topology;
      //---
      return(true);
     }
   //+------------------------------------------------------------------+
   //| Create mesh object from OBJ mesh file                            |
   //+------------------------------------------------------------------+
   bool              Create(CDXDispatcher &dispatcher,CDXInput* buffer_scene,string obj_path,float scale=1.0f,bool inverse_winding=false)
     {
      DXVertex vertices[];
      uint     indices[];
      //--- load model
      if(!DXLoadObjModel(obj_path,vertices,indices,scale))
         return(false);
      //--- set white vertices color
      DXColor white=DXColor(1.0f,1.0f,1.0f,1.0f);
      int count=ArraySize(vertices);
      for(int i=0; i<count; i++)
         vertices[i].vcolor=white;
      //--- inverse winding
      if(inverse_winding)
         DXInverseWinding(vertices,indices);
      //--- create mesh
      return(Create(dispatcher,buffer_scene,vertices,indices));
     }
   //+------------------------------------------------------------------+
   //| Update vertices                                                  |
   //+------------------------------------------------------------------+
   bool              VerticesSet(const DXVertex &vertices[])
     {
      //--- check size
      if(ArraySize(vertices)<1)
         return(false);
      //--- create new vertex buffer
      if(!m_buffer_vertex.Create(m_context,vertices))
        {
         Shutdown();
         return(false);
        }
      //---
      return(true);
     }
   //+------------------------------------------------------------------+
   //| Update indices                                                   |
   //+------------------------------------------------------------------+
   bool              IndicesSet(const uint &indices[])
     {
      //--- check size
      if(ArraySize(indices)<1)
         return(false);
      //--- create new index buffer
      if(!m_buffer_index.Create(m_context,indices))
        {
         Shutdown();
         return(false);
        }
      //---
      return(true);
     }
   //+------------------------------------------------------------------+
   //| Update indices                                                   |
   //+------------------------------------------------------------------+
   bool              TopologySet(ENUM_DX_PRIMITIVE_TOPOLOGY topology)
     {
      m_topology=topology;
      //---
      return(true);
     }
   //+------------------------------------------------------------------+
   //| Get transform matrix                                             |
   //+------------------------------------------------------------------+
   void              TransformMatrixGet(DXMatrix &matrix) const
     {
      matrix=m_transform_matrix;
     }
   //+------------------------------------------------------------------+
   //| Set transform matrix                                             |
   //+------------------------------------------------------------------+
   void              TransformMatrixSet(const DXMatrix &matrix)
     {
      m_transform_matrix=matrix;
     }
   //+------------------------------------------------------------------+
   //| Get diffuse color                                                |
   //+------------------------------------------------------------------+
   void              DiffuseColorGet(DXColor &clr) const
     {
      clr=m_diffuse_color;
     }
   //+------------------------------------------------------------------+
   //| Set diffuse color                                                |
   //+------------------------------------------------------------------+
   void              DiffuseColorSet(const DXColor &clr)
     {
      m_diffuse_color=clr;
     }
   //+------------------------------------------------------------------+
   //| Get specular color                                               |
   //+------------------------------------------------------------------+
   void              SpecularColorGet(DXColor &clr) const
     {
      clr=m_specular_color;
     }
   //+------------------------------------------------------------------+
   //| Set specular color                                               |
   //+------------------------------------------------------------------+
   void              SpecularColorSet(const DXColor &clr)
     {
      m_specular_color=clr;
     }
   //+------------------------------------------------------------------+
   //| Get specular power                                               |
   //+------------------------------------------------------------------+
   void              SpecularPowerGet(float &power) const
     {
      power=m_specular_power;
     }
   //+------------------------------------------------------------------+
   //| Set specular power                                               |
   //+------------------------------------------------------------------+
   void              SpecularPowerSet(float power)
     {
      m_specular_power=power;
     }
   //+------------------------------------------------------------------+
   //| Get emission color                                               |
   //+------------------------------------------------------------------+
   void              EmissionColorGet(DXColor &clr) const
     {
      clr=m_emission_color;
     }
   //+------------------------------------------------------------------+
   //| Set emission color                                               |
   //+------------------------------------------------------------------+
   void              EmissionColorSet(const DXColor &clr)
     {
      m_emission_color=clr;
     }
   //+------------------------------------------------------------------+
   //| Render mesh                                                      |
   //+------------------------------------------------------------------+
   virtual bool      Render(void) override
     {
      //--- update object buffer
      DXInputObject input_data;
      DXMatrixTranspose(input_data.transform,m_transform_matrix);
      input_data.diffuse_color =m_diffuse_color;
      input_data.emission_color=m_emission_color;
      input_data.specular_color=m_specular_color;
      input_data.specular_power=m_specular_power;
      //--- clear shader textures
      m_shader_pixel.TexturesClear();
      //--- set buffers, shaders, topology and render
      if(m_buffer_object.InputSet(input_data))
         if(m_buffer_vertex.Render())
            if(m_buffer_index.Render())
               if(m_shader_vertex.InputSet(0,m_buffer_scene) && m_shader_vertex.InputSet(1,m_buffer_object))
                  if(m_shader_pixel.InputSet(0,m_buffer_scene) && m_shader_pixel.InputSet(1,m_buffer_object))
                     if(CheckPointer(m_texture)==POINTER_INVALID || m_shader_pixel.TextureSet(0,m_texture))
                        if(m_shader_vertex.Render())
                           if(m_shader_pixel.Render())
                              if(DXPrimiveTopologySet(m_context,m_topology))
                                 return(DXDrawIndexed(m_context));
      //---
      return(false);
     }
   //+------------------------------------------------------------------+
   //| Set texture from image file                                      |
   //+------------------------------------------------------------------+
   bool              TextureSet(CDXDispatcher &dispatcher,string path,uint data_x=0,uint data_y=0,uint data_width=0,uint data_height=0)
     {
      //--- check if texture already exist
      if(CheckPointer(m_texture)==POINTER_INVALID)
        {
         m_texture=dispatcher.TextureCreateFromFile(path,data_x,data_y,data_width,data_height);
         if(m_texture==NULL)
            return(false);
         m_texture.AddRef();
         return(true);
        }
      //--- update texture data
      if(!m_texture.Create(m_context,path,data_x,data_y,data_width,data_height))
        {
         m_texture.Release();
         m_texture=NULL;
         return(false);
        }
      //---
      return(true);
     }
   //+------------------------------------------------------------------+
   //| Set texture from image data                                      |
   //+------------------------------------------------------------------+
   bool              TextureSet(CDXDispatcher &dispatcher,ENUM_DX_FORMAT format,uint width,uint height,const uint &data[],uint data_x=0,uint data_y=0,uint data_width=0,uint data_height=0)
     {
      //--- check if texture already exist
      if(CheckPointer(m_texture)==POINTER_INVALID)
        {
         m_texture=dispatcher.TextureCreateFromData(format,width,height,data,data_x,data_y,data_width,data_height);
         if(m_texture==NULL)
            return(false);
         m_texture.AddRef();
         return(true);
        }
      //--- update texture data
      if(!m_texture.Create(m_context,format,width,height,data,data_x,data_y,data_width,data_height))
        {
         m_texture.Release();
         m_texture=NULL;
         return(false);
        }
      //---
      return(true);
     }
   //+------------------------------------------------------------------+
   //| Delete used texture                                              |
   //+------------------------------------------------------------------+
   void              TextureDelete()
     {
      //--- release texture
      if(CheckPointer(m_texture)!=POINTER_INVALID)
        {
         m_texture.Release();
         m_texture=NULL;
        }
     }
   //+------------------------------------------------------------------+
   //| Shutdown                                                         |
   //+------------------------------------------------------------------+
   virtual void      Shutdown(void)
     {
      //--- release all dx resources
      if(CheckPointer(m_buffer_vertex)!=POINTER_INVALID)
        {
         m_buffer_vertex.Release();
         m_buffer_vertex=NULL;
        }
      if(CheckPointer(m_buffer_index)!=POINTER_INVALID)
        {
         m_buffer_index.Release();
         m_buffer_index=NULL;
        }
      if(CheckPointer(m_buffer_object)!=POINTER_INVALID)
        {
         m_buffer_object.Release();
         m_buffer_object=NULL;
        }
      if(CheckPointer(m_texture)!=POINTER_INVALID)
        {
         m_texture.Release();
         m_texture=NULL;
        }
      if(CheckPointer(m_buffer_scene)!=POINTER_INVALID)
        {
         m_buffer_scene.Release();
         m_buffer_scene=NULL;
        }
      if(CheckPointer(m_shader_pixel)!=POINTER_INVALID)
        {
         m_shader_pixel.Release();
         m_shader_pixel=NULL;
        }
      if(CheckPointer(m_shader_vertex)!=POINTER_INVALID)
        {
         m_shader_vertex.Release();
         m_shader_vertex=NULL;
        }
      m_topology =WRONG_VALUE;
     }
  };
//+------------------------------------------------------------------+
