//+------------------------------------------------------------------+
//|                                               DXVertexBuffer.mqh |
//|                        Copyright 2019, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2019, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#include "DXHandle.mqh"
//+------------------------------------------------------------------+
//| DX vertex buffer                                                 |
//+------------------------------------------------------------------+
class CDXVertexBuffer : public CDXHandleShared
  {
public:
   //+------------------------------------------------------------------+
   //| Destructor                                                       |
   //+------------------------------------------------------------------+
   virtual          ~CDXVertexBuffer(void)
     {
      Shutdown();
     }
   //+------------------------------------------------------------------+
   //| Create vertex buffer in specified context                        |
   //+------------------------------------------------------------------+
   template<typename TVertex>
   bool              Create(int context_handle,const TVertex &vertices[],uint start=0,uint count=WHOLE_ARRAY)
     {
      Shutdown();
      m_context=context_handle;
      m_handle=DXBufferCreate(m_context,DX_BUFFER_VERTEX,vertices,start,count);
      return(m_handle!=INVALID_HANDLE);
     }
   //+------------------------------------------------------------------+
   //| Render                                                           |
   //+------------------------------------------------------------------+
   bool              Render(uint start=0,uint count=WHOLE_ARRAY)
     {
      return(DXBufferSet(m_context,m_handle,start,count));
     }
   //+------------------------------------------------------------------+
   //| Shutdown                                                         |
   //+------------------------------------------------------------------+
   void              Shutdown(void)
     {
      //--- relase handle
      if(m_handle!=INVALID_HANDLE)
         DXRelease(m_handle);
      m_handle=INVALID_HANDLE;
     }
  };
//+------------------------------------------------------------------+
//| DX index buffer                                                  |
//+------------------------------------------------------------------+
class CDXIndexBuffer : public CDXHandleShared
  {
public:
   //+------------------------------------------------------------------+
   //| Destructor                                                       |
   //+------------------------------------------------------------------+
   virtual          ~CDXIndexBuffer(void)
     {
      Shutdown();
     }
   //+------------------------------------------------------------------+
   //| Create index buffer in specified context                         |
   //+------------------------------------------------------------------+
   bool              Create(int context_handle,const uint &indices[],uint start=0,uint count=WHOLE_ARRAY)
     {
      Shutdown();
      m_context=context_handle;
      m_handle=DXBufferCreate(m_context,DX_BUFFER_INDEX,indices,start,count);
      return(m_handle!=INVALID_HANDLE);
     }
   //+------------------------------------------------------------------+
   //| Render                                                           |
   //+------------------------------------------------------------------+
   bool              Render(uint start=0,uint count=WHOLE_ARRAY)
     {
      return(DXBufferSet(m_context,m_handle,start,count));
     }
   //+------------------------------------------------------------------+
   //| Shutdown                                                         |
   //+------------------------------------------------------------------+
   void              Shutdown(void)
     {
      //--- relase handle
      if(m_handle!=INVALID_HANDLE)
         DXRelease(m_handle);
      m_handle=INVALID_HANDLE;
     }
  };
//+------------------------------------------------------------------+
