//+------------------------------------------------------------------+
//|                                             ChartObjectPanel.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <ChartObjects\ChartObjectsTxtControls.mqh>
#include <Arrays\ArrayObj.mqh>
#include <Arrays\ArrayInt.mqh>
//+------------------------------------------------------------------+
//| Class CChartObjectPanel.                                         |
//| Purpose: Class for grouping objects for managing a chart         |
//+------------------------------------------------------------------+
class CChartObjectPanel : public CChartObjectButton
  {
protected:
   CArrayObj         m_attachment;       // array of attached objects
   CArrayInt         m_dX;               // array of dX attached objects
   CArrayInt         m_dY;               // array of dY attached objects
   bool              m_expanded;         // collapsed/expanded flag

public:
                     CChartObjectPanel();
                    ~CChartObjectPanel();
   //--- method for attaching objects
   bool              Attach(CChartObjectLabel *chart_object);
   bool              X_Distance(const int X);
   int               X_Distance(void) const { return(CChartObjectButton::X_Distance()); }
   bool              Y_Distance(const int Y);
   int               Y_Distance(void) const { return(CChartObjectButton::Y_Distance()); }
   int               X_Size() const;
   int               X_Size(const int Y) const { return(CChartObjectButton::X_Size()); }
   int               Y_Size() const;
   int               Y_Size(const int Y) const { return(CChartObjectButton::Y_Size()); }
   
   int               Timeframes(void) const { return(CChartObjectButton::Timeframes()); }
   virtual bool      Timeframes(const int timeframes);
   bool              State(const bool state);
   bool              State(void) const  { return(CChartObjectButton::State()); }
   bool              CheckState();

protected:
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CChartObjectPanel::CChartObjectPanel(void) : m_expanded(true)
  {
  }
//+------------------------------------------------------------------+
//| Destructor.                                                      |
//+------------------------------------------------------------------+
void CChartObjectPanel::~CChartObjectPanel(void)
  {
//--- All objects added by the method Add(), deleted automatically
  }
//+------------------------------------------------------------------+
//| Method Attach                                                    |
//+------------------------------------------------------------------+
bool CChartObjectPanel::Attach(CChartObjectLabel *chart_object)
  {
   if(m_attachment.Add(chart_object))
     {
      int x,y;
      x=chart_object.X_Distance();
      m_dX.Add(chart_object.X_Distance());
      x+=X_Distance();
      chart_object.X_Distance(X_Distance()+chart_object.X_Distance());
      y=CChartObjectButton::Y_Size();
      y+=chart_object.Y_Distance();
      m_dY.Add(chart_object.Y_Distance()+CChartObjectButton::Y_Size()+2);
      chart_object.Y_Distance(Y_Distance()+chart_object.Y_Distance()+CChartObjectButton::Y_Size()+2);
      return(true);
     }
//---
   return(false);
  }
//+------------------------------------------------------------------+
//| Method X_Distance                                                |
//+------------------------------------------------------------------+
bool CChartObjectPanel::X_Distance(const int X)
  {
   CChartObjectLabel *chart_object;
//---
   for(int i=0;i<m_attachment.Total();i++)
     {
      chart_object=m_attachment.At(i);
      chart_object.X_Distance(X+m_dX.At(i));
     }
//---
   return(CChartObjectButton::X_Distance(X));
  }
//+------------------------------------------------------------------+
//| Method Y_Distance                                                |
//+------------------------------------------------------------------+
bool CChartObjectPanel::Y_Distance(const int Y)
  {
   CChartObjectLabel *chart_object;
//---
   for(int i=0;i<m_attachment.Total();i++)
     {
      chart_object=m_attachment.At(i);
      chart_object.Y_Distance(Y+m_dY.At(i));
     }
//---
   return(CChartObjectButton::Y_Distance(Y));
  }
//+------------------------------------------------------------------+
//| Method X_Size                                                    |
//+------------------------------------------------------------------+
int CChartObjectPanel::X_Size() const
  {
   int                max_x=CChartObjectButton::X_Size()+X_Distance();
   CChartObjectLabel *chart_object;
//---
   if(m_expanded)
     {
      for(int i=0;i<m_attachment.Total();i++)
         if((chart_object=m_attachment.At(i))!=NULL)
            if(max_x<chart_object.X_Distance()+chart_object.X_Size())
               max_x=chart_object.X_Distance()+chart_object.X_Size();
      return(max_x-X_Distance()+2);
     }
//---
   return(CChartObjectButton::X_Size()+2);
  }
//+------------------------------------------------------------------+
//| Method Y_Size                                                    |
//+------------------------------------------------------------------+
int CChartObjectPanel::Y_Size() const
  {
   int                max_y=CChartObjectButton::Y_Size()+Y_Distance();
   CChartObjectLabel *chart_object;
//---
   if(m_expanded)
     {
      for(int i=0;i<m_attachment.Total();i++)
         if((chart_object=m_attachment.At(i))!=NULL)
            if(max_y<chart_object.Y_Distance()+chart_object.Y_Size())
               max_y=chart_object.Y_Distance()+chart_object.Y_Size();
      return(max_y-Y_Distance()+2);
     }
//---
   return(CChartObjectButton::Y_Size()+2);
  }
//+------------------------------------------------------------------+
//| Method Timeframes                                                |
//+------------------------------------------------------------------+
bool CChartObjectPanel::Timeframes(const int timeframes)
  {
   int                i;
   bool               res=CChartObject::Timeframes(timeframes);
   CChartObjectLabel *chart_object;
//---
   if(m_expanded)
      for(i=0;i<m_attachment.Total();i++)
        {
         chart_object=m_attachment.At(i);
         res&=chart_object.Timeframes(timeframes);
        }
//---
   return(res);
  }
//+------------------------------------------------------------------+
//| Method State                                                     |
//+------------------------------------------------------------------+
bool CChartObjectPanel::State(const bool state)
  {
   if(CChartObjectButton::State(state))
     {
      m_expanded=state;
      return(true);
     }
//---
   return(false);
  }
//+------------------------------------------------------------------+
//| Method CheckState                                                |
//+------------------------------------------------------------------+
bool CChartObjectPanel::CheckState(void)
  {
   int                i;
   CChartObjectLabel *chart_object;
//---
   if(m_expanded!=State())
     {
      if(m_expanded=State())
         //--- make all objects visible
         for(i=0;i<m_attachment.Total();i++)
           {
            chart_object=m_attachment.At(i);
            chart_object.Timeframes(-1);
           }
      else
         //--- make all objects invisible
         for(i=0;i<m_attachment.Total();i++)
           {
            chart_object=m_attachment.At(i);
            chart_object.Timeframes(0x100000);
           }
      return(true);
     }
//---
   return(false);
  }
//+------------------------------------------------------------------+
