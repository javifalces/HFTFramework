//+------------------------------------------------------------------+
//|                                                        DXBox.mqh |
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
class CDXBox : public CDXMesh
  {
public:
                     CDXBox();
                    ~CDXBox();
   //--- create bon in specified context
   bool              Create(CDXDispatcher &dispatcher,CDXInput* buffer_scene,const DXVector3 &from,const DXVector3 &to);
   //--- update box
   bool              Update(const DXVector3 &from,const DXVector3 &to);

private:
   //---
   void              PrepareVertices(const DXVector3 &from,const DXVector3 &to);
  };
//+------------------------------------------------------------------+
//| Class constructor                                                |
//+------------------------------------------------------------------+
void CDXBox::CDXBox() : CDXMesh()
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
void CDXBox::~CDXBox(void)
  {
  }
//+------------------------------------------------------------------+
//| Create box in specified context                                  |
//+------------------------------------------------------------------+
bool CDXBox::Create(CDXDispatcher &dispatcher,CDXInput* buffer_scene,const DXVector3 &from,const DXVector3 &to)
  {
//--- release previous buffers
   Shutdown();
//---
   DXVertex vertices[];
   uint indices[];
//--- prepare box vertices and indices
   DXColor white=DXColor(1.0f,1.0f,1.0f,1.0f);
   if(!DXComputeBox(from,to,vertices,indices))
      return(false);
   for(int i=0; i<ArraySize(vertices); i++)
      vertices[i].vcolor=white;
//--- create mesh
   return(CDXMesh::Create(dispatcher,buffer_scene,vertices,indices));
  }
//+------------------------------------------------------------------+
//| Update box bounds                                                |
//+------------------------------------------------------------------+
bool CDXBox::Update(const DXVector3 &from,const DXVector3 &to)
  {
//---
   DXVertex vertices[];
   uint indices[];
//--- prepare box vertices and indices
   DXColor white=DXColor(1.0f,1.0f,1.0f,1.0f);
   if(!DXComputeBox(from,to,vertices,indices))
      return(false);
   for(int i=0; i<ArraySize(vertices); i++)
      vertices[i].vcolor=white;
//--- update mesh vertices, indices are the same
   return(CDXMesh::VerticesSet(vertices));
  }
//+------------------------------------------------------------------+
