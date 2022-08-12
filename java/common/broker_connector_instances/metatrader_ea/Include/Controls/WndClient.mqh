//+------------------------------------------------------------------+
//|                                                    WndClient.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndContainer.mqh"
#include "Panel.mqh"
#include "Scrolls.mqh"
//+------------------------------------------------------------------+
//| Class CWndClient                                                 |
//| Usage: base class to create areas with                           |
//|             the scrollbars                                       |
//+------------------------------------------------------------------+
class CWndClient : public CWndContainer
  {
protected:
   //--- flags
   bool              m_v_scrolled;          // "vertical scrolling is possible" flag
   bool              m_h_scrolled;          // "horizontal scrolling is possible" flag
   //--- dependent controls
   CPanel            m_background;          // the "scrollbar background" object
   CScrollV          m_scroll_v;            // the vertical scrollbar object
   CScrollH          m_scroll_h;            // the horizontal scrollbar object

public:
                     CWndClient(void);
                    ~CWndClient(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- parameters
   virtual bool      ColorBackground(const color value)          { return(m_background.ColorBackground(value)); }
   virtual bool      ColorBorder(const color value)              { return(m_background.ColorBorder(value));     }
   virtual bool      BorderType(const ENUM_BORDER_TYPE flag)     { return(m_background.BorderType(flag));       }
   //--- settings
   virtual bool      VScrolled(void) { return(m_v_scrolled); }
   virtual bool      VScrolled(const bool flag);
   virtual bool      HScrolled(void) { return(m_h_scrolled); }
   virtual bool      HScrolled(const bool flag);
   //--- ID
   virtual long      Id(const long id);
   virtual long      Id(void) const { return(CWnd::Id()); }
   //--- state
   virtual bool      Show(void);

protected:
   //--- create dependent controls
   virtual bool      CreateBack(void);
   virtual bool      CreateScrollV(void);
   virtual bool      CreateScrollH(void);
   //--- internal event handlers
   virtual bool      OnResize(void);
   //--- handlers of the dependent controls events
   virtual bool      OnVScrollShow(void)                         { return(true); }
   virtual bool      OnVScrollHide(void)                         { return(true); }
   virtual bool      OnHScrollShow(void)                         { return(true); }
   virtual bool      OnHScrollHide(void)                         { return(true); }
   virtual bool      OnScrollLineDown(void)                      { return(true); }
   virtual bool      OnScrollLineUp(void)                        { return(true); }
   virtual bool      OnScrollLineLeft(void)                      { return(true); }
   virtual bool      OnScrollLineRight(void)                     { return(true); }
   //--- resize
   virtual bool      Rebound(const CRect &rect);
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
EVENT_MAP_BEGIN(CWndClient)
   ON_NAMED_EVENT(ON_SHOW,m_scroll_v,OnVScrollShow)
   ON_NAMED_EVENT(ON_HIDE,m_scroll_v,OnVScrollHide)
   ON_EVENT(ON_SCROLL_DEC,m_scroll_v,OnScrollLineUp)
   ON_EVENT(ON_SCROLL_INC,m_scroll_v,OnScrollLineDown)
   ON_NAMED_EVENT(ON_SHOW,m_scroll_h,OnHScrollShow)
   ON_NAMED_EVENT(ON_HIDE,m_scroll_h,OnHScrollHide)
   ON_EVENT(ON_SCROLL_DEC,m_scroll_h,OnScrollLineLeft)
   ON_EVENT(ON_SCROLL_INC,m_scroll_h,OnScrollLineRight)
EVENT_MAP_END(CWndContainer)
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CWndClient::CWndClient(void) : m_v_scrolled(false),
                               m_h_scrolled(false)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CWndClient::~CWndClient(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CWndClient::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- call of the method of the parent class
   if(!CWndContainer::Create(chart,name,subwin,x1,y1,x2,y2))
      return(false);
//--- create dependent controls
   if(!CreateBack())
      return(false);
   if(m_v_scrolled && !CreateScrollV())
      return(false);
   if(m_h_scrolled && !CreateScrollH())
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create scrollbar background                                      |
//+------------------------------------------------------------------+
bool CWndClient::CreateBack(void)
  {
//--- create
   if(!m_background.Create(m_chart_id,m_name+"Back",m_subwin,0,0,Width(),Height()))
      return(false);
   if(!m_background.ColorBorder(CONTROLS_CLIENT_COLOR_BORDER))
      return(false);
   if(!m_background.ColorBackground(CONTROLS_CLIENT_COLOR_BG))
      return(false);
   if(!Add(m_background))
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create vertical scrollbar                                        |
//+------------------------------------------------------------------+
bool CWndClient::CreateScrollV(void)
  {
//--- calculate coordinates
   int x1=Width()-CONTROLS_SCROLL_SIZE-CONTROLS_BORDER_WIDTH;
   int y1=CONTROLS_BORDER_WIDTH;
   int x2=Width()-CONTROLS_BORDER_WIDTH;
   int y2=Height()-CONTROLS_BORDER_WIDTH;
   if(m_h_scrolled) y2-=CONTROLS_SCROLL_SIZE;
//--- create
   if(!m_scroll_v.Create(m_chart_id,m_name+"VScroll",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!Add(m_scroll_v))
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create horizontal scrollbar                                      |
//+------------------------------------------------------------------+
bool CWndClient::CreateScrollH(void)
  {
//--- calculate coordinates
   int x1=CONTROLS_BORDER_WIDTH;
   int y1=Height()-CONTROLS_SCROLL_SIZE-CONTROLS_BORDER_WIDTH;
   int x2=Width()-CONTROLS_BORDER_WIDTH;
   int y2=Height()-CONTROLS_BORDER_WIDTH;
   if(m_v_scrolled) x2-=CONTROLS_SCROLL_SIZE;
//--- create
   if(!m_scroll_h.Create(m_chart_id,m_name+"HScroll",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!Add(m_scroll_h))
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set up vertical scrollbar                                        |
//+------------------------------------------------------------------+
bool CWndClient::VScrolled(const bool flag)
  {
   if(m_v_scrolled==flag)
      return(true);
//--- there are changes
   int d_size=0;
   if(flag)
     {
      //--- create vertical scrollbar
      if(!CreateScrollV())
         return(false);
      //--- need to shorten horizontal scrollbar (if there is one)
      d_size=-CONTROLS_SCROLL_SIZE;
     }
   else
     {
      //--- delete vertical scrollbar
      m_scroll_v.Destroy();
      if(!Delete(m_scroll_v))
         return(false);
      //--- need to lengthen horizontal scrollbar (if there is one)
      d_size=CONTROLS_SCROLL_SIZE;
     }
   m_v_scrolled=flag;
//--- change width of horizontal scrollbar (if there is one)
   if(m_h_scrolled)
     {
      if(!m_scroll_h.Width(m_scroll_h.Width()+d_size))
         return(false);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set up horizontal scrollbar                                      |
//+------------------------------------------------------------------+
bool CWndClient::HScrolled(const bool flag)
  {
   if(m_h_scrolled==flag)
      return(true);
//--- there are changes
   int d_size=0;
   if(flag)
     {
      //--- create horizontal scrollbar
      if(!CreateScrollH())
         return(false);
      //--- need to shorten vertical scrollbar (if there is one)
      d_size=-CONTROLS_SCROLL_SIZE;
     }
   else
     {
      //--- delete horizontal scrollbar
      m_scroll_h.Destroy();
      if(!Delete(m_scroll_h))
         return(false);
      //--- need to lengthen vertical scrollbar (if there is one)
      d_size=CONTROLS_SCROLL_SIZE;
     }
   m_h_scrolled=flag;
//--- change width of vertical scrollbar (if there is one)
   if(m_v_scrolled)
     {
      if(!m_scroll_v.Height(m_scroll_v.Height()+d_size))
         return(false);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set ID of control                                                |
//+------------------------------------------------------------------+
long CWndClient::Id(const long id)
  {
//--- reserve ID for container
   long id_used=CWndContainer::Id(id);
//---
   if(!m_v_scrolled)
      id_used+=m_scroll_v.Id(id+id_used);
   if(!m_h_scrolled)
      id_used+=m_scroll_h.Id(id+id_used);
//--- return number of used IDs
   return(id_used);
  }
//+------------------------------------------------------------------+
//| Makes the control visible                                        |
//+------------------------------------------------------------------+
bool CWndClient::Show(void)
  {
//--- call of the method of the parent class
   CWndContainer::Show();
//---
   if(!m_v_scrolled)
      m_scroll_v.Hide();
   if(!m_h_scrolled)
      m_scroll_h.Hide();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of resizing                                              |
//+------------------------------------------------------------------+
bool CWndClient::OnResize(void)
  {
//--- call of the method of the parent class
   if(!CWndContainer::OnResize())
      return(false);
//--- resize background
   int d_size=0;
   m_background.Width(Width());
   m_background.Height(Height());
//---
   if(m_v_scrolled)
     {
      //--- move vertical scrollbar
      m_scroll_v.Move(Right()-CONTROLS_SCROLL_SIZE,Top());
      //--- modify vertical scrollbar
      d_size=(m_h_scrolled) ? CONTROLS_SCROLL_SIZE : 0;
      m_scroll_v.Height(Height()-d_size);
     }
   if(m_h_scrolled)
     {
      //--- move horizontal scrollbar
      m_scroll_h.Move(Left(),Bottom()-CONTROLS_SCROLL_SIZE);
      //--- modify horizontal scrollbar
      d_size=(m_v_scrolled) ? CONTROLS_SCROLL_SIZE : 0;
      m_scroll_h.Width(Width()-d_size);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Resize                                                           |
//+------------------------------------------------------------------+
bool CWndClient::Rebound(const CRect &rect)
  {
   m_rect.SetBound(rect);
//--- call virtual event handler
   return(OnResize());
  }
//+------------------------------------------------------------------+
