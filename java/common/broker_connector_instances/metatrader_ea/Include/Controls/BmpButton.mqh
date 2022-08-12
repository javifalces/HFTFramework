//+------------------------------------------------------------------+
//|                                                    BmpButton.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndObj.mqh"
#include <ChartObjects\ChartObjectsBmpControls.mqh>
//+------------------------------------------------------------------+
//| Class CBmpButton                                                 |
//| Usage: control that is displayed by                              |
//|             the CChartObjectBmpLabel object                      |
//+------------------------------------------------------------------+
class CBmpButton : public CWndObj
  {
private:
   CChartObjectBmpLabel m_button;           // chart object
   //--- parameters of the chart object
   int               m_border;              // border width
   string            m_bmp_off_name;        // name of BMP file for the "OFF" state (default state)
   string            m_bmp_on_name;         // name of BMP file for the "ON" state
   string            m_bmp_passive_name;
   string            m_bmp_active_name;

public:
                     CBmpButton(void);
                    ~CBmpButton(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- parameters of the chart object
   int               Border(void)          const { return(m_border);                   }
   bool              Border(const int value);
   bool              BmpNames(const string off="",const string on="");
   string            BmpOffName(void)      const { return(m_bmp_off_name);             }
   bool              BmpOffName(const string name);
   string            BmpOnName(void)       const { return(m_bmp_on_name);              }
   bool              BmpOnName(const string name);
   string            BmpPassiveName(void)  const { return(m_bmp_passive_name);         }
   bool              BmpPassiveName(const string name);
   string            BmpActiveName(void)   const { return(m_bmp_active_name);          }
   bool              BmpActiveName(const string name);
   //--- state
   bool              Pressed(void)         const { return(m_button.State());           }
   bool              Pressed(const bool pressed) { return(m_button.State(pressed));    }
   //--- properties
   bool              Locking(void)         const { return(IS_CAN_LOCK);                }
   void              Locking(const bool locking);

protected:
   //--- handlers of object settings
   virtual bool      OnSetZOrder(void)           { return(m_button.Z_Order(m_zorder)); }
   //--- internal event handlers
   virtual bool      OnCreate(void);
   virtual bool      OnShow(void);
   virtual bool      OnHide(void);
   virtual bool      OnMove(void);
   virtual bool      OnChange(void);
   //--- новые обработчики
   virtual bool      OnActivate(void);
   virtual bool      OnDeactivate(void);
   virtual bool      OnMouseDown(void);
   virtual bool      OnMouseUp(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CBmpButton::CBmpButton(void) : m_border(0),
                               m_bmp_off_name(NULL),
                               m_bmp_on_name(NULL),
                               m_bmp_passive_name(NULL),
                               m_bmp_active_name(NULL)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CBmpButton::~CBmpButton(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CBmpButton::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- call method of the parent class
   if(!CWndObj::Create(chart,name,subwin,x1,y1,x2,y2))
      return(false);
//--- create the chart object
   if(!m_button.Create(chart,name,subwin,x1,y1))
      return(false);
//--- call the settings handler
   return(OnChange());
  }
//+------------------------------------------------------------------+
//| Set border width                                                 |
//+------------------------------------------------------------------+
bool CBmpButton::Border(const int value)
  {
//--- save new value of parameter
   m_border=value;
//--- set up the chart object
   return(m_button.Width(value));
  }
//+------------------------------------------------------------------+
//| Set two images at once                                           |
//+------------------------------------------------------------------+
bool CBmpButton::BmpNames(const string off,const string on)
  {
//--- save new values of parameters
   m_bmp_off_name=off;
   m_bmp_on_name =on;
//--- set up the chart object
   if(!m_button.BmpFileOff(off))
      return(false);
   if(!m_button.BmpFileOn(on))
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set image for the "OFF" state                                    |
//+------------------------------------------------------------------+
bool CBmpButton::BmpOffName(const string name)
  {
//--- save new value of parameter
   m_bmp_off_name=name;
//--- set up the chart object
   if(!m_button.BmpFileOff(name))
      return(false);
//--- set size by image dimensions
   Width(m_button.X_Size());
   Height(m_button.Y_Size());
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set image for the "ON" state                                     |
//+------------------------------------------------------------------+
bool CBmpButton::BmpOnName(const string name)
  {
//--- save new value of parameter
   m_bmp_on_name=name;
//--- set up the chart object
   if(!m_button.BmpFileOn(name))
      return(false);
//--- set size by image dimensions
   Width(m_button.X_Size());
   Height(m_button.Y_Size());
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set image for the "OFF" state (passive)                          |
//+------------------------------------------------------------------+
bool CBmpButton::BmpPassiveName(const string name)
  {
//--- save new value of parameter
   m_bmp_passive_name=name;
//--- set up the chart object
   if(!IS_ACTIVE)
      return(BmpOffName(name));
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set image for the "OFF" state (active)                           |
//+------------------------------------------------------------------+
bool CBmpButton::BmpActiveName(const string name)
  {
//--- save new value of parameter
   m_bmp_active_name=name;
//--- set up the chart object
   if(IS_ACTIVE)
      return(BmpOffName(name));
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Locking flag                                                     |
//+------------------------------------------------------------------+
void CBmpButton::Locking(const bool flag)
  {
   if(flag)
      PropFlagsSet(WND_PROP_FLAG_CAN_LOCK);
   else
      PropFlagsReset(WND_PROP_FLAG_CAN_LOCK);
  }
//+------------------------------------------------------------------+
//| Create object on chart                                           |
//+------------------------------------------------------------------+
bool CBmpButton::OnCreate(void)
  {
//--- create the chart object by previously set parameters
   return(m_button.Create(m_chart_id,m_name,m_subwin,m_rect.left,m_rect.top));
  }
//+------------------------------------------------------------------+
//| Display object on chart                                          |
//+------------------------------------------------------------------+
bool CBmpButton::OnShow(void)
  {
   return(m_button.Timeframes(OBJ_ALL_PERIODS));
  }
//+------------------------------------------------------------------+
//| Hide object from chart                                           |
//+------------------------------------------------------------------+
bool CBmpButton::OnHide(void)
  {
   return(m_button.Timeframes(OBJ_NO_PERIODS));
  }
//+------------------------------------------------------------------+
//| Absolute movement of the chart object                            |
//+------------------------------------------------------------------+
bool CBmpButton::OnMove(void)
  {
//--- position the chart object
   return(m_button.X_Distance(m_rect.left) && m_button.Y_Distance(m_rect.top));
  }
//+------------------------------------------------------------------+
//| Set up the chart object                                          |
//+------------------------------------------------------------------+
bool CBmpButton::OnChange(void)
  {
//--- set up the chart object
   return(m_button.Width(m_border) && m_button.BmpFileOff(m_bmp_off_name) && m_button.BmpFileOn(m_bmp_on_name));
  }
//+------------------------------------------------------------------+
//| Handler of activating the group of controls                      |
//+------------------------------------------------------------------+
bool CBmpButton::OnActivate(void)
  {
   if(m_bmp_active_name!=NULL)
      BmpOffName(m_bmp_active_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of deactivating the group of controls                    |
//+------------------------------------------------------------------+
bool CBmpButton::OnDeactivate(void)
  {
   if(m_bmp_passive_name!=NULL)
      BmpOffName(m_bmp_passive_name);
   if(!IS_CAN_LOCK)
      Pressed(false);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on the left mouse button                        |
//+------------------------------------------------------------------+
bool CBmpButton::OnMouseDown(void)
  {
   if(!IS_CAN_LOCK)
      Pressed(!Pressed());
//--- call of the method of the parent class
   return(CWnd::OnMouseDown());
  }
//+------------------------------------------------------------------+
//| Handler of click on the left mouse button                        |
//+------------------------------------------------------------------+
bool CBmpButton::OnMouseUp(void)
  {
//--- depress the button if it is not fixed
   if(m_button.State() && !IS_CAN_LOCK)
      m_button.State(false);
//--- call of the method of the parent class
   return(CWnd::OnMouseUp());
  }
//+------------------------------------------------------------------+
