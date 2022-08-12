//+------------------------------------------------------------------+
//|                                                      Picture.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndObj.mqh"
#include <ChartObjects\ChartObjectsBmpControls.mqh>
//+------------------------------------------------------------------+
//| Class CPicture                                                   |
//| Note: image displayed by                                         |
//|             the CChartObjectBmpLabel object                      |
//+------------------------------------------------------------------+
class CPicture : public CWndObj
  {
private:
   CChartObjectBmpLabel m_picture;          // chart object
   //--- parameters of the chart object
   int               m_border;              // border width
   string            m_bmp_name;            // filename

public:
                     CPicture(void);
                    ~CPicture(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- parameters of the chart object
   int               Border(void) const { return(m_border); }
   bool              Border(const int value);
   string            BmpName(void) const { return(m_bmp_name); }
   bool              BmpName(const string name);

protected:
   //--- internal event handlers
   virtual bool      OnCreate(void);
   virtual bool      OnShow(void);
   virtual bool      OnHide(void);
   virtual bool      OnMove(void);
   virtual bool      OnChange(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CPicture::CPicture(void) : m_border(0),
                           m_bmp_name(NULL)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CPicture::~CPicture(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CPicture::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- call method of the parent class
   if(!CWndObj::Create(chart,name,subwin,x1,y1,x2,y2))
      return(false);
//--- create the chart object
   if(!m_picture.Create(chart,name,subwin,x1,y1))
      return(false);
//--- call the settings handler
   return(OnChange());
  }
//+------------------------------------------------------------------+
//| Set border width                                                 |
//+------------------------------------------------------------------+
bool CPicture::Border(const int value)
  {
//--- save new value of parameter
   m_border=value;
//--- set up the chart object
   return(m_picture.Width(value));
  }
//+------------------------------------------------------------------+
//| Set image                                                        |
//+------------------------------------------------------------------+
bool CPicture::BmpName(const string name)
  {
//--- save new value of parameter
   m_bmp_name=name;
//--- set up the chart object
   return(m_picture.BmpFileOn(name));
  }
//+------------------------------------------------------------------+
//| Create object on chart                                           |
//+------------------------------------------------------------------+
bool CPicture::OnCreate(void)
  {
//--- create the chart object by previously set parameters
   return(m_picture.Create(m_chart_id,m_name,m_subwin,m_rect.left,m_rect.top));
  }
//+------------------------------------------------------------------+
//| Display object on chart                                          |
//+------------------------------------------------------------------+
bool CPicture::OnShow(void)
  {
   return(m_picture.Timeframes(OBJ_ALL_PERIODS));
  }
//+------------------------------------------------------------------+
//| Hide object from chart                                           |
//+------------------------------------------------------------------+
bool CPicture::OnHide(void)
  {
   return(m_picture.Timeframes(OBJ_NO_PERIODS));
  }
//+------------------------------------------------------------------+
//| Absolute movement of the chart object                            |
//+------------------------------------------------------------------+
bool CPicture::OnMove(void)
  {
//--- position the chart object
   return(m_picture.X_Distance(m_rect.left) && m_picture.Y_Distance(m_rect.top));
  }
//+------------------------------------------------------------------+
//| Set up the chart object                                          |
//+------------------------------------------------------------------+
bool CPicture::OnChange(void)
  {
//--- set up the chart object
   return(m_picture.Width(m_border) && m_picture.BmpFileOn(m_bmp_name));
  }
//+------------------------------------------------------------------+
