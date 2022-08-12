//+------------------------------------------------------------------+
//|                                                      Scrolls.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndContainer.mqh"
#include "Panel.mqh"
#include "BmpButton.mqh"
//+------------------------------------------------------------------+
//| Resources                                                        |
//+------------------------------------------------------------------+
#resource "res\\Up.bmp"
#resource "res\\ThumbVert.bmp"
#resource "res\\Down.bmp"
#resource "res\\Left.bmp"
#resource "res\\ThumbHor.bmp"
#resource "res\\Right.bmp"
//+------------------------------------------------------------------+
//| Class CScroll                                                    |
//| Usage: base class for scrollbars                                 |
//+------------------------------------------------------------------+
class CScroll : public CWndContainer
  {
protected:
   //--- dependent controls
   CPanel            m_back;                // the "scrollbar background" object
   CBmpButton        m_inc;                 // the "increment button" object ("down" for vertical scrollbar, "right" for horizontal scrollbar)
   CBmpButton        m_dec;                 // the "decrement button" object ("up" for vertical scrollbar, "left" for horizontal scrollbar)
   CBmpButton        m_thumb;               // the "scroll box" object
   //--- set up
   int               m_min_pos;             // minimum value
   int               m_max_pos;             // maximum value
   //--- state
   int               m_curr_pos;            // current value

public:
                     CScroll(void);
                    ~CScroll(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- set up
   int               MinPos(void)           const { return(m_min_pos);  }
   void              MinPos(const int value);
   int               MaxPos(void)           const { return(m_max_pos);  }
   void              MaxPos(const int value);
   //--- state
   int               CurrPos(void)          const { return(m_curr_pos); }
   bool              CurrPos(int value);

protected:
   //--- create dependent controls
   virtual bool      CreateBack(void);
   virtual bool      CreateInc(void)              { return(true);       }
   virtual bool      CreateDec(void)              { return(true);       }
   virtual bool      CreateThumb(void)            { return(true);       }
   //--- handlers of the dependent controls events
   virtual bool      OnClickInc(void);
   virtual bool      OnClickDec(void);
   //--- internal event handlers
   virtual bool      OnShow(void);
   virtual bool      OnHide(void);
   virtual bool      OnChangePos(void)             { return(true);       }
   //--- handlers of dragging
   virtual bool      OnThumbDragStart(void)        { return(true);       }
   virtual bool      OnThumbDragProcess(void)      { return(true);       }
   virtual bool      OnThumbDragEnd(void)          { return(true);       }
   //--- calculate position by coordinate
   virtual int       CalcPos(const int coord)      { return(0);          }
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
EVENT_MAP_BEGIN(CScroll)
   ON_EVENT(ON_CLICK,m_inc,OnClickInc)
   ON_EVENT(ON_CLICK,m_dec,OnClickDec)
   ON_EVENT(ON_DRAG_START,m_thumb,OnThumbDragStart)
   ON_EVENT_PTR(ON_DRAG_PROCESS,m_drag_object,OnThumbDragProcess)
   ON_EVENT_PTR(ON_DRAG_END,m_drag_object,OnThumbDragEnd)
EVENT_MAP_END(CWndContainer)
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CScroll::CScroll(void) : m_curr_pos(0),
                         m_min_pos(0),
                         m_max_pos(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CScroll::~CScroll(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CScroll::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- call method of the parent class
   if(!CWndContainer::Create(chart,name,subwin,x1,y1,x2,y2))
      return(false);
//--- create dependent controls
   if(!CreateBack())
      return(false);
   if(!CreateInc())
      return(false);
   if(!CreateDec())
      return(false);
   if(!CreateThumb())
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create scrollbar background                                      |
//+------------------------------------------------------------------+
bool CScroll::CreateBack(void)
  {
//--- create
   if(!m_back.Create(m_chart_id,m_name+"Back",m_subwin,0,0,Width(),Height()))
      return(false);
   if(!m_back.ColorBackground(CONTROLS_SCROLL_COLOR_BG))
      return(false);
   if(!m_back.ColorBorder(CONTROLS_SCROLL_COLOR_BORDER))
      return(false);
   if(!Add(m_back))
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set current value                                                |
//+------------------------------------------------------------------+
bool CScroll::CurrPos(int value)
  {
//--- check value
   if(value<m_min_pos)
      value=m_min_pos;
   if(value>m_max_pos)
      value=m_max_pos;
//--- if value was changed
   if(m_curr_pos!=value)
     {
      m_curr_pos=value;
      //--- call virtual handler
      return(OnChangePos());
     }
//--- value has not been changed
   return(false);
  }
//+------------------------------------------------------------------+
//| Set minimum value                                                |
//+------------------------------------------------------------------+
void CScroll::MinPos(const int value)
  {
//--- if value was changed
   if(m_min_pos!=value)
     {
      m_min_pos=value;
      //--- adjust the scroll box position
      CurrPos(m_curr_pos);
     }
  }
//+------------------------------------------------------------------+
//| Set maximum value                                                |
//+------------------------------------------------------------------+
void CScroll::MaxPos(const int value)
  {
//--- if value was changed
   if(m_max_pos!=value)
     {
      m_max_pos=value;
      //--- adjust the scroll box position
      CurrPos(m_curr_pos);
     }
  }
//+------------------------------------------------------------------+
//| Handler of the "Show scrollbar" event                            |
//+------------------------------------------------------------------+
bool CScroll::OnShow(void)
  {
   if(m_id==CONTROLS_INVALID_ID)
      return(true);
//--- send notification
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_SHOW,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "Hide scrollbar" event                            |
//+------------------------------------------------------------------+
bool CScroll::OnHide(void)
  {
   if(m_id==CONTROLS_INVALID_ID)
      return(true);
//--- send notification
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_HIDE,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on the "increment" button                       |
//+------------------------------------------------------------------+
bool CScroll::OnClickInc(void)
  {
//--- try to increment current value
   if(!CurrPos(m_curr_pos+1))
      return(true);
//--- if value was changed, send notification
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_SCROLL_INC,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on the "decrement" button                       |
//+------------------------------------------------------------------+
bool CScroll::OnClickDec(void)
  {
//--- try to decrement current value
   if(!CurrPos(m_curr_pos-1))
      return(true);
//--- if value was changed, send notification
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_SCROLL_DEC,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CScrollV                                                   |
//| Usage: class of vertical scrollbar                               |
//+------------------------------------------------------------------+
class CScrollV : public CScroll
  {
public:
                     CScrollV(void);
                    ~CScrollV(void);

protected:
   //--- create dependent controls
   virtual bool      CreateInc(void);
   virtual bool      CreateDec(void);
   virtual bool      CreateThumb(void);
   //--- internal event handlers
   virtual bool      OnResize(void);
   virtual bool      OnChangePos(void);
   //--- handlers of dragging
   virtual bool      OnThumbDragStart(void);
   virtual bool      OnThumbDragProcess(void);
   virtual bool      OnThumbDragEnd(void);
   //--- calculate position by coordinate
   virtual int       CalcPos(const int coord);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CScrollV::CScrollV(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CScrollV::~CScrollV(void)
  {
  }
//+------------------------------------------------------------------+
//| Create the "Increment" button                                    |
//+------------------------------------------------------------------+
bool CScrollV::CreateInc(void)
  {
//--- calculate coordinates
   int x1=CONTROLS_BORDER_WIDTH;
   int y1=Height()-CONTROLS_SCROLL_SIZE+CONTROLS_BORDER_WIDTH;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create
   if(!m_inc.Create(m_chart_id,m_name+"Inc",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_inc.BmpNames("::res\\Down.bmp"))
      return(false);
   if(!Add(m_inc))
      return(false);
//--- property
   m_inc.PropFlags(WND_PROP_FLAG_CLICKS_BY_PRESS);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create the "Decrement" button                                    |
//+------------------------------------------------------------------+
bool CScrollV::CreateDec(void)
  {
//--- calculate coordinates
   int x1=CONTROLS_BORDER_WIDTH;
   int y1=CONTROLS_BORDER_WIDTH;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create
   if(!m_dec.Create(m_chart_id,m_name+"Dec",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_dec.BmpNames("::res\\Up.bmp"))
      return(false);
   if(!Add(m_dec))
      return(false);
//--- property
   m_dec.PropFlags(WND_PROP_FLAG_CLICKS_BY_PRESS);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create scroll box                                                |
//+------------------------------------------------------------------+
bool CScrollV::CreateThumb(void)
  {
//--- calculate coordinates
   int x1=CONTROLS_BORDER_WIDTH;
   int y1=CONTROLS_SCROLL_SIZE-CONTROLS_BORDER_WIDTH;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_SCROLL_THUMB_SIZE;
//--- create
   if(!m_thumb.Create(m_chart_id,m_name+"Thumb",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_thumb.BmpNames("::res\\ThumbVert.bmp"))
      return(false);
   if(!Add(m_thumb))
      return(false);
   m_thumb.PropFlags(WND_PROP_FLAG_CAN_DRAG);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of changing current state                                |
//+------------------------------------------------------------------+
bool CScrollV::OnChangePos(void)
  {
//--- check if scrolling is possible
   if(m_max_pos-m_min_pos<=0)
      return(Visible(false));
   else
      if(!Visible(true))
         return(false);
//--- calculate new coordinated of the scrollbar
   int steps    =m_max_pos-m_min_pos;           // number of steps to change position
   int min_coord=m_dec.Bottom();                // minimum possible coordinate (corresponds to the m_min_pos value)
   int max_coord=m_inc.Top()-m_thumb.Height();  // maximum possible coordinate (corresponds to the m_max_pos value)
   int new_coord=min_coord+(max_coord-min_coord)*m_curr_pos/steps;  // new coordinate
//--- adjust the scroll box position
   return(m_thumb.Move(m_thumb.Left(),new_coord));
  }
//+------------------------------------------------------------------+
//| Handler of resizing                                              |
//+------------------------------------------------------------------+
bool CScrollV::OnResize(void)
  {
//--- can not change the lateral size
   if(Width()!=CONTROLS_SCROLL_SIZE)
      m_rect.Width(CONTROLS_SCROLL_SIZE);
//--- resize the scrollbar background
   if(!m_back.Size(Size()))
      return(false);
//--- move the "Increment" button
   if(!m_inc.Move(m_inc.Left(),Bottom()-CONTROLS_SCROLL_SIZE))
      return(false);
//--- adjust the scroll box position
   return(OnChangePos());
  }
//+------------------------------------------------------------------+
//| Start dragging the "slider"                                      |
//+------------------------------------------------------------------+
bool CScrollV::OnThumbDragStart(void)
  {
   if(m_drag_object==NULL)
     {
      m_drag_object=new CDragWnd;
      if(m_drag_object==NULL)
         return(false);
     }
//--- calculate coordinates
   int x1=m_thumb.Left()-CONTROLS_DRAG_SPACING;
   int y1=m_thumb.Top()-CONTROLS_DRAG_SPACING;
   int x2=m_thumb.Right()+CONTROLS_DRAG_SPACING;
   int y2=m_thumb.Bottom()+CONTROLS_DRAG_SPACING;
//--- create
   m_drag_object.Create(m_chart_id,"",m_subwin,x1,y1,x2,y2);
   m_drag_object.PropFlags(WND_PROP_FLAG_CAN_DRAG);
//--- ограничения
   m_drag_object.Limits(x1,m_dec.Bottom()-CONTROLS_DRAG_SPACING,x2,m_inc.Top()+CONTROLS_DRAG_SPACING);
//--- set mouse params
   m_drag_object.MouseX(m_thumb.MouseX());
   m_drag_object.MouseY(m_thumb.MouseY());
   m_drag_object.MouseFlags(m_thumb.MouseFlags());
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Continue dragging the "slider"                                   |
//+------------------------------------------------------------------+
bool CScrollV::OnThumbDragProcess(void)
  {
//--- checking
   if(m_drag_object==NULL)
      return(false);
//--- calculate coordinates
   int x=m_drag_object.Left()+CONTROLS_DRAG_SPACING;
   int y=m_drag_object.Top()+CONTROLS_DRAG_SPACING;
//--- calculate new position
   int new_pos=CalcPos(y);
   if(new_pos!=m_curr_pos)
     {
      ushort event_id=(m_curr_pos<new_pos) ? ON_SCROLL_INC : ON_SCROLL_DEC;
      m_curr_pos=new_pos;
      EventChartCustom(CONTROLS_SELF_MESSAGE,event_id,m_id,0.0,m_name);
     }
//--- move thumb
   m_thumb.Move(x,y);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| End dragging the "slider"                                        |
//+------------------------------------------------------------------+
bool CScrollV::OnThumbDragEnd(void)
  {
   if(m_drag_object!=NULL)
     {
      m_thumb.MouseFlags(m_drag_object.MouseFlags());
      delete m_drag_object;
      m_drag_object=NULL;
     }
//--- succeed
   return(m_thumb.Pressed(false));
  }
//+------------------------------------------------------------------+
//| Calculate position by coordinate                                 |
//+------------------------------------------------------------------+
int CScrollV::CalcPos(const int coord)
  {
//--- calculate new position of the scrollbar
   int steps    =m_max_pos-m_min_pos;           // number of steps to change position
   int min_coord=m_dec.Bottom();                // minimum possible coordinate (corresponds to the m_min_pos value)
   int max_coord=m_inc.Top()-m_thumb.Height();  // maximum possible coordinate (corresponds to the m_max_pos value)
//--- checkeng
   if(max_coord==min_coord)
      return(0);
   if(coord<min_coord || coord>max_coord)
      return(m_curr_pos);
//---
   int new_pos=(int)MathRound((((double)(coord-min_coord))/(max_coord-min_coord))*steps);  // new position
//---
   return(new_pos);
  }
//+------------------------------------------------------------------+
//| Class CScrollH                                                   |
//| Usage: class of horizontal scrollbar                             |
//+------------------------------------------------------------------+
class CScrollH : public CScroll
  {
public:
                     CScrollH(void);
                    ~CScrollH(void);

protected:
   //--- create dependent controls
   virtual bool      CreateInc(void);
   virtual bool      CreateDec(void);
   virtual bool      CreateThumb(void);
   //--- internal event handlers
   virtual bool      OnResize(void);
   virtual bool      OnChangePos(void);
   //--- handlers of dragging
   virtual bool      OnThumbDragStart(void);
   virtual bool      OnThumbDragProcess(void);
   virtual bool      OnThumbDragEnd(void);
   //--- calculate position by coordinate
   virtual int       CalcPos(const int coord);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CScrollH::CScrollH(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CScrollH::~CScrollH(void)
  {
  }
//+------------------------------------------------------------------+
//| Create the "Increment" button                                    |
//+------------------------------------------------------------------+
bool CScrollH::CreateInc(void)
  {
//--- calculate coordinates
   int x1=Width()-CONTROLS_SCROLL_SIZE+CONTROLS_BORDER_WIDTH;
   int y1=CONTROLS_BORDER_WIDTH;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create
   if(!m_inc.Create(m_chart_id,m_name+"Inc",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_inc.BmpNames("::res\\Right.bmp"))
      return(false);
   if(!Add(m_inc))
      return(false);
//--- property
   m_inc.PropFlags(WND_PROP_FLAG_CLICKS_BY_PRESS);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create the "Decrement" button                                    |
//+------------------------------------------------------------------+
bool CScrollH::CreateDec(void)
  {
//--- calculate coordinates
   int x1=CONTROLS_BORDER_WIDTH;
   int y1=CONTROLS_BORDER_WIDTH;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create
   if(!m_dec.Create(m_chart_id,m_name+"Dec",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_dec.BmpNames("::res\\Left.bmp"))
      return(false);
   if(!Add(m_dec))
      return(false);
//--- property
   m_dec.PropFlags(WND_PROP_FLAG_CLICKS_BY_PRESS);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create scroll box                                                |
//+------------------------------------------------------------------+
bool CScrollH::CreateThumb(void)
  {
//--- calculate coordinates
   int x1=CONTROLS_SCROLL_SIZE-CONTROLS_BORDER_WIDTH;
   int y1=CONTROLS_BORDER_WIDTH;
   int x2=x1+CONTROLS_SCROLL_THUMB_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create
   if(!m_thumb.Create(m_chart_id,m_name+"Thumb",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_thumb.BmpNames("::res\\ThumbHor.bmp"))
      return(false);
   if(!Add(m_thumb))
      return(false);
   m_thumb.PropFlags(WND_PROP_FLAG_CAN_DRAG);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of changing current state                                |
//+------------------------------------------------------------------+
bool CScrollH::OnChangePos(void)
  {
//--- check if scrolling is possible
   if(m_max_pos-m_min_pos<=0)
      return(Visible(false));
   else
      if(!Visible(true))
         return(false);
//--- calculate new coordinated of the scrollbar
   int steps=m_max_pos-m_min_pos;            // number of steps to change position
   int min_coord=m_dec.Right();                   // minimum possible coordinate (corresponds to the m_min_pos value)
   int max_coord=m_inc.Left()-m_thumb.Width();  // maximum possible coordinate (corresponds to the m_max_pos value)
   int new_coord=min_coord+(max_coord-min_coord)*m_curr_pos/steps;  // new coordinate
//--- adjust the scroll box position
   return(m_thumb.Move(new_coord,m_thumb.Top()));
  }
//+------------------------------------------------------------------+
//| Handler of resizing                                              |
//+------------------------------------------------------------------+
bool CScrollH::OnResize(void)
  {
//--- can not change the lateral size
   if(Height()!=CONTROLS_SCROLL_SIZE)
      m_rect.Height(CONTROLS_SCROLL_SIZE);
//--- resize the scrollbar background
   if(!m_back.Size(Size()))
      return(false);
//--- move the "Increment" button
   if(!m_inc.Move(Right()-CONTROLS_SCROLL_SIZE,m_inc.Top()))
      return(false);
//--- adjust the scroll box position
   return(OnChangePos());
  }
//+------------------------------------------------------------------+
//| Start dragging the "slider"                                      |
//+------------------------------------------------------------------+
bool CScrollH::OnThumbDragStart(void)
  {
   if(m_drag_object==NULL)
     {
      m_drag_object=new CDragWnd;
      if(m_drag_object==NULL)
         return(false);
     }
//--- calculate coordinates
   int x1=m_thumb.Left()-CONTROLS_DRAG_SPACING;
   int y1=m_thumb.Top()-CONTROLS_DRAG_SPACING;
   int x2=m_thumb.Right()+CONTROLS_DRAG_SPACING;
   int y2=m_thumb.Bottom()+CONTROLS_DRAG_SPACING;
//--- create
   m_drag_object.Create(m_chart_id,"",m_subwin,x1,y1,x2,y2);
   m_drag_object.PropFlags(WND_PROP_FLAG_CAN_DRAG);
//--- ограничения
   m_drag_object.Limits(m_dec.Right()-CONTROLS_DRAG_SPACING,y1,m_inc.Left()+CONTROLS_DRAG_SPACING,y2);
//--- set mouse params
   m_drag_object.MouseX(m_thumb.MouseX());
   m_drag_object.MouseY(m_thumb.MouseY());
   m_drag_object.MouseFlags(m_thumb.MouseFlags());
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Continue dragging the "slider"                                   |
//+------------------------------------------------------------------+
bool CScrollH::OnThumbDragProcess(void)
  {
//--- checking
   if(m_drag_object==NULL)
      return(false);
//--- calculate coordinates
   int x=m_drag_object.Left()+CONTROLS_DRAG_SPACING;
   int y=m_drag_object.Top()+CONTROLS_DRAG_SPACING;
//--- calculate new position
   int new_pos=CalcPos(x);
   if(new_pos!=m_curr_pos)
     {
      ushort event_id=(m_curr_pos<new_pos)?ON_SCROLL_INC:ON_SCROLL_DEC;
      m_curr_pos=new_pos;
      EventChartCustom(CONTROLS_SELF_MESSAGE,event_id,m_id,0.0,m_name);
     }
//--- move thumb
   m_thumb.Move(x,y);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| End dragging the "slider"                                        |
//+------------------------------------------------------------------+
bool CScrollH::OnThumbDragEnd(void)
  {
   if(m_drag_object!=NULL)
     {
      m_thumb.MouseFlags(m_drag_object.MouseFlags());
      delete m_drag_object;
      m_drag_object=NULL;
     }
//--- succeed
   return(m_thumb.Pressed(false));
  }
//+------------------------------------------------------------------+
//| Calculate position by coordinate                                 |
//+------------------------------------------------------------------+
int CScrollH::CalcPos(const int coord)
  {
//--- calculate new position of the scrollbar
   int steps    =m_max_pos-m_min_pos;           // number of steps to change position
   int min_coord=m_dec.Right();                 // minimum possible coordinate (corresponds to the m_min_pos value)
   int max_coord=m_inc.Left()-m_thumb.Width();  // maximum possible coordinate (corresponds to the m_max_pos value)
//--- checkeng
   if(max_coord==min_coord)
      return(0);
   if(coord<min_coord || coord>max_coord)
      return(m_curr_pos);
//---
   int new_pos=(int)MathRound((((double)(coord-min_coord))/(max_coord-min_coord))*steps);  // new position
//---
   return(new_pos);
  }
//+------------------------------------------------------------------+
