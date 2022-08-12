//+------------------------------------------------------------------+
//|                                                    DXSurface.mqh |
//|                        Copyright 2019, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2019, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
//---
#include "DXMesh.mqh"
#include "DXMath.mqh"
#include "DXUtils.mqh"
//+------------------------------------------------------------------+
//| 3D Box object                                                    |
//+------------------------------------------------------------------+
class CDXSurface : public CDXMesh
  {
public:
   //--- surface creation flags
   enum EN_SURFACE_FLAGS
     {
      SF_NONE        =0x0,
      SF_TWO_SIDED   =0x1,
      SF_USE_NORMALS =0x2,
     };
   //--- surface color scheme
   enum EN_COLOR_SCHEME
     {
      CS_NONE        =0,
      CS_JET         =1,
      CS_COLD_TO_HOT =2,
      CS_RED_TO_GREEN=3
     };

protected:
   uint              m_data_width;
   uint              m_data_height;
   uint              m_flags;
   EN_COLOR_SCHEME   m_color_scheme;

public:
                     CDXSurface();
                    ~CDXSurface();
   //--- create bon in specified context
   bool              Create(CDXDispatcher &dispatcher,CDXInput* buffer_scene,double &data[],uint m_data_widht,uint m_data_height,float data_range,const DXVector3 &from,const DXVector3 &to,DXVector2 &texture_size,uint flags=SF_NONE,EN_COLOR_SCHEME color_scheme=CS_NONE);
   //--- update box
   bool              Update(double &data[],uint m_data_widht,uint m_data_height,float data_range,const DXVector3 &from,const DXVector3 &to,DXVector2 &texture_size,uint flags=0,EN_COLOR_SCHEME color_scheme=CS_NONE);

private:
   //---
   void              PrepareColors(DXVertex &vertices[],const DXVector3 &from,const DXVector3 &to);
  };
//+------------------------------------------------------------------+
//| Class constructor                                                |
//+------------------------------------------------------------------+
void CDXSurface::CDXSurface() : CDXMesh()
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
void CDXSurface::~CDXSurface(void)
  {
  }
//+------------------------------------------------------------------+
//| Create box in specified context                                  |
//+------------------------------------------------------------------+
bool CDXSurface::Create(CDXDispatcher &dispatcher,CDXInput* buffer_scene,
                        double &data[],uint data_width,uint data_height,float data_range,
                        const DXVector3 &from,const DXVector3 &to,DXVector2 &texture_size,
                        uint flags=SF_NONE,EN_COLOR_SCHEME color_scheme=CS_NONE)
  {
//--- release previous buffers
   Shutdown();
//--- save parameters
   m_data_width  =data_width;
   m_data_height =data_height;
   m_flags       =flags;
   m_color_scheme=color_scheme;
//---
   DXVertex vertices[];
   uint indices[];
//--- prepare surface vertices and indices
   DXColor white=DXColor(1.0f,1.0f,1.0f,1.0f);
   if(!DXComputeSurface(data,data_width,data_height,data_range,from,to,texture_size,flags&SF_TWO_SIDED,flags&SF_USE_NORMALS,vertices,indices))
      return(false);
//--- calculate colors
   PrepareColors(vertices,from,to);
//--- create mesh
   return(CDXMesh::Create(dispatcher,buffer_scene,vertices,indices));
  }
//+------------------------------------------------------------------+
//| Update box bounds                                                |
//+------------------------------------------------------------------+
bool CDXSurface::Update(double &data[],uint data_width,uint data_height,float data_range,
                        const DXVector3 &from,const DXVector3 &to,DXVector2 &texture_size,
                        uint flags=0,EN_COLOR_SCHEME color_scheme=CS_NONE)
  {
//---
   DXVertex vertices[];
   uint indices[];
//--- prepare surface vertices and indices
   DXColor white=DXColor(1.0f,1.0f,1.0f,1.0f);
   if(!DXComputeSurface(data,data_width,data_height,data_range,from,to,texture_size,flags&SF_TWO_SIDED,flags&SF_USE_NORMALS,vertices,indices))
      return(false);
//--- calculate colors
   m_color_scheme=color_scheme;
   PrepareColors(vertices,from,to);
//--- update vertices
   bool res=CDXMesh::VerticesSet(vertices);
//--- do not update indices if vertices relations are the same
   if(m_data_width!=data_width || m_data_height!=data_height || (m_flags&SF_TWO_SIDED)!=(flags&SF_TWO_SIDED))
      res=res && CDXMesh::IndicesSet(indices);
//--- check result
   if(!res)
     {
      Shutdown();
      return(false);
     }
//--- save parameters
   m_data_width  =data_width;
   m_data_height =data_height;
   m_flags       =flags;
//--- success
   return(true);
  }
//+------------------------------------------------------------------+
//| Update box bounds                                                |
//+------------------------------------------------------------------+
void CDXSurface::PrepareColors(DXVertex &vertices[],const DXVector3 &from,const DXVector3 &to)
  {
   uint count=ArraySize(vertices);
   float scale=1.0f/(fmax(FLT_EPSILON,to.y-from.y));
   switch(m_color_scheme)
     {
      case CS_JET:
        {
         for(uint i=0; i<count; i++)
            DXComputeColorJet((vertices[i].position.y-from.y)*scale,vertices[i].vcolor);
         break;
        }
      case CS_COLD_TO_HOT:
        {
         for(uint i=0; i<count; i++)
            DXComputeColorColdToHot((vertices[i].position.y-from.y)*scale,vertices[i].vcolor);
         break;
        }
      case CS_RED_TO_GREEN:
        {
         for(uint i=0; i<count; i++)
            DXComputeColorRedToGreen((vertices[i].position.y-from.y)*scale,vertices[i].vcolor);
         break;
        }  
      default:
        {
         DXColor white=DXColor(1.0f,1.0f,1.0f,1.0f);
         for(uint i=0; i<count; i++)
            vertices[i].vcolor=white;
         break;
        }
     }
  }
//+------------------------------------------------------------------+
