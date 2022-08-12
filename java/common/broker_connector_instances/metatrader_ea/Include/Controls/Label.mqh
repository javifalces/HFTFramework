//+------------------------------------------------------------------+
//|                                                        Label.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndObj.mqh"
#include <ChartObjects\ChartObjectsTxtControls.mqh>
//+------------------------------------------------------------------+
//| Class CLabel                                                     |
//| Usage: control that is displayed by                              |
//|             the CChartObjectLabel object                         |
//+------------------------------------------------------------------+
class CLabel : public CWndObj
  {
private:
   CChartObjectLabel m_label;               // chart object

public:
                     CLabel(void);
                    ~CLabel(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);

protected:
   //--- handlers of object settings
   virtual bool      OnSetText(void)     { return(m_label.Description(m_text));   }
   virtual bool      OnSetColor(void)    { return(m_label.Color(m_color));        }
   virtual bool      OnSetFont(void)     { return(m_label.Font(m_font));          }
   virtual bool      OnSetFontSize(void) { return(m_label.FontSize(m_font_size)); }
   //--- internal event handlers
   virtual bool      OnCreate(void);
   virtual bool      OnShow(void);
   virtual bool      OnHide(void);
   virtual bool      OnMove(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CLabel::CLabel(void)
  {
   m_color=CONTROLS_LABEL_COLOR;
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CLabel::~CLabel(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CLabel::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- call method of the parent class
   if(!CWndObj::Create(chart,name,subwin,x1,y1,x2,y2))
      return(false);
//--- create the chart object
   if(!m_label.Create(chart,name,subwin,x1,y1))
      return(false);
//--- call the settings handler
   return(OnChange());
  }
//+------------------------------------------------------------------+
//| Create object on chart                                           |
//+------------------------------------------------------------------+
bool CLabel::OnCreate(void)
  {
//--- create the chart object by previously set parameters
   return(m_label.Create(m_chart_id,m_name,m_subwin,m_rect.left,m_rect.top));
  }
//+------------------------------------------------------------------+
//| Display object on chart                                          |
//+------------------------------------------------------------------+
bool CLabel::OnShow(void)
  {
   return(m_label.Timeframes(OBJ_ALL_PERIODS));
  }
//+------------------------------------------------------------------+
//| Hide object from chart                                           |
//+------------------------------------------------------------------+
bool CLabel::OnHide(void)
  {
   return(m_label.Timeframes(OBJ_NO_PERIODS));
  }
//+------------------------------------------------------------------+
//| Absolute movement of the chart object                            |
//+------------------------------------------------------------------+
bool CLabel::OnMove(void)
  {
//--- position the chart object
   return(m_label.X_Distance(m_rect.left) && m_label.Y_Distance(m_rect.top));
  }
//+------------------------------------------------------------------+
