//+------------------------------------------------------------------+
//|                                                          Wnd.mqh |
//|                   Copyright 2009-2018, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Rect.mqh"
#include "Defines.mqh"
#include <Object.mqh>
class CDragWnd;
//+------------------------------------------------------------------+
//| Class CWnd                                                       |
//| Usage: base class of the control object that creates             |
//|             control panels and indicator panels                  |
//+------------------------------------------------------------------+
class CWnd : public CObject
  {
protected:
   //--- parameters of creation
   long              m_chart_id;            // chart ID
   int               m_subwin;              // chart subwindow
   string            m_name;                // object name
   //--- geometry
   CRect             m_rect;                // chart area
   //--- ID
   long              m_id;                  // object ID
   //--- state flags
   int               m_state_flags;
   //--- properties flags
   int               m_prop_flags;
   //--- alignment
   int               m_align_flags;         // alignment flags
   int               m_align_left;          // fixed offset from left border
   int               m_align_top;           // fixed offset from top border
   int               m_align_right;         // fixed offset from right border
   int               m_align_bottom;        // fixed offset from bottom border
   //--- the last saved state of mouse
   int               m_mouse_x;             // X coordinate
   int               m_mouse_y;             // Y coordinate
   int               m_mouse_flags;         // state of buttons
   uint              m_last_click;          // last click time
   //--- drag object
   CDragWnd         *m_drag_object;         // pointer to the dragged object

public:
                     CWnd(void);
                    ~CWnd(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- release memory
   virtual void      Destroy(const int reason=0);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   virtual bool      OnMouseEvent(const int x,const int y,const int flags);
   //--- naming (read only)
   string            Name(void)                        const { return(m_name);               }
   //--- access the contents of container
   int               ControlsTotal(void)               const { return(0);                    }
   CWnd*             Control(const int ind)            const { return(NULL);                 }
   virtual CWnd*     ControlFind(const long id);
   //--- geometry
   const CRect       Rect(void)                        const { return(m_rect);               }
   int               Left(void)                        const { return(m_rect.left);          }
   virtual void      Left(const int x)                       { m_rect.left=x;                }
   int               Top(void)                         const { return(m_rect.top);           }
   virtual void      Top(const int y)                        { m_rect.top=y;                 }
   int               Right(void)                       const { return(m_rect.right);         }
   virtual void      Right(const int x)                      { m_rect.right=x;               }
   int               Bottom(void)                      const { return(m_rect.bottom);        }
   virtual void      Bottom(const int y)                     { m_rect.bottom=y;              }
   int               Width(void)                       const { return(m_rect.Width());       }
   virtual bool      Width(const int w);
   int               Height(void)                      const { return(m_rect.Height());      }
   virtual bool      Height(const int h);
   CSize             Size(void)                        const { return(m_rect.Size());        }
   virtual bool      Size(const int w,const int h);
   virtual bool      Size(const CSize &size);
   virtual bool      Move(const int x,const int y);
   virtual bool      Move(const CPoint &point);
   virtual bool      Shift(const int dx,const int dy);
   bool              Contains(const int x,const int y) const { return(m_rect.Contains(x,y)); }
   bool              Contains(CWnd *control) const;
   //--- alignment
   void              Alignment(const int flags,const int left,const int top,const int right,const int bottom);
   virtual bool      Align(const CRect &rect);
   //--- ID
   virtual long      Id(const long id);
   long              Id(void)                          const { return(m_id);                 }
   //--- state
   bool              IsEnabled(void)                   const { return(IS_ENABLED);           }
   virtual bool      Enable(void);
   virtual bool      Disable(void);
   bool              IsVisible(void)                   const { return(IS_VISIBLE);           }
   virtual bool      Visible(const bool flag);
   virtual bool      Show(void);
   virtual bool      Hide(void);
   bool              IsActive(void)                    const { return(IS_ACTIVE);            }
   virtual bool      Activate(void);
   virtual bool      Deactivate(void);
   //--- state flags
   int               StateFlags(void)                  const { return(m_state_flags);        }
   void              StateFlags(const int flags)             { m_state_flags=flags;          }
   void              StateFlagsSet(const int flags)          { m_state_flags|=flags;         }
   void              StateFlagsReset(const int flags)        { m_state_flags&=~flags;        }
   //--- properties flags
   int               PropFlags(void)                   const { return(m_prop_flags);         }
   void              PropFlags(const int flags)              { m_prop_flags=flags;           }
   void              PropFlagsSet(const int flags)           { m_prop_flags|=flags;          }
   void              PropFlagsReset(const int flags)         { m_prop_flags&=~flags;         }
   //--- for mouse operations
   int               MouseX(void)                      const { return(m_mouse_x);            }
   void              MouseX(const int value)                 { m_mouse_x=value;              }
   int               MouseY(void)                      const { return(m_mouse_y);            }
   void              MouseY(const int value)                 { m_mouse_y=value;              }
   int               MouseFlags(void)                  const { return(m_mouse_flags);        }
   virtual void      MouseFlags(const int value)             { m_mouse_flags=value;          }
   bool              MouseFocusKill(const long id=CONTROLS_INVALID_ID);
   bool              BringToTop(void);

protected:
   //--- internal event handlers
   virtual bool      OnCreate(void)                          { return(true);                 }
   virtual bool      OnDestroy(void)                         { return(true);                 }
   virtual bool      OnMove(void)                            { return(true);                 }
   virtual bool      OnResize(void)                          { return(true);                 }
   virtual bool      OnEnable(void)                          { return(true);                 }
   virtual bool      OnDisable(void)                         { return(true);                 }
   virtual bool      OnShow(void)                            { return(true);                 }
   virtual bool      OnHide(void)                            { return(true);                 }
   virtual bool      OnActivate(void)                        { return(true);                 }
   virtual bool      OnDeactivate(void)                      { return(true);                 }
   virtual bool      OnClick(void);
   virtual bool      OnDblClick(void);
   virtual bool      OnChange(void)                          { return(true);                 }
   //--- mouse event handlers
   virtual bool      OnMouseDown(void);
   virtual bool      OnMouseUp(void);
   //--- handlers of dragging
   virtual bool      OnDragStart(void);
   virtual bool      OnDragProcess(const int x,const int y);
   virtual bool      OnDragEnd(void);
   //--- methods for drag-object
   virtual bool      DragObjectCreate(void)                  { return(false);                }
   virtual bool      DragObjectDestroy(void);
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
bool CWnd::OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam)
  {
   if((id!=CHARTEVENT_MOUSE_MOVE))
      return(false);
   if(!IS_VISIBLE)
      return(false);
   int x=(int)lparam;
   int y=(int)dparam;
   int flags=(int)StringToInteger(sparam);
//---
   if(m_drag_object!=NULL)
      return(m_drag_object.OnMouseEvent(x,y,flags));
//---
   return(OnMouseEvent(x,y,flags));
  }
//+------------------------------------------------------------------+
//| Common handler of mouse events                                   |
//+------------------------------------------------------------------+
bool CWnd::OnMouseEvent(const int x,const int y,const int flags)
  {
   if(!Contains(x,y))
     {
      //--- if cursor is not inside the element and this element is active - deactivate
      if(IS_ACTIVE)
        {
         //--- reset state and coordinates
         m_mouse_x    =0;
         m_mouse_y    =0;
         m_mouse_flags=MOUSE_INVALID_FLAGS;
         //--- deactivate
         Deactivate();
        }
      return(false);
     }
//--- check the state of the left mouse button
   if((flags&MOUSE_LEFT)!=0)
     {
      //--- left mouse button is pressed
      if(m_mouse_flags==MOUSE_INVALID_FLAGS)
        {
         //--- but not in this control (i.e., cursor entered the element with mouse button pressed)
         //--- activate the control, but there will be no click
         if(!IS_ACTIVE)
           {
            //--- generate event
            EventChartCustom(CONTROLS_SELF_MESSAGE,ON_MOUSE_FOCUS_SET,m_id,0.0,m_name);
            //--- activate
            return(Activate());
           }
         return(true);
        }
      if((m_mouse_flags&MOUSE_LEFT)!=0)
        {
         //--- mouse button has already been pressed
         if(IS_CAN_DRAG)
            return(OnDragProcess(x,y));
         if(IS_CLICKS_BY_PRESS)
           {
            EventChartCustom(CONTROLS_SELF_MESSAGE,ON_CLICK,m_id,0.0,m_name);
            //--- handled
            return(true);
           }
        }
      else
        {
         //--- mouse button has been released (pressing)
         //--- save the state and coordinates
         m_mouse_flags=flags;
         m_mouse_x    =x;
         m_mouse_y    =y;
         //--- call the handler
         return(OnMouseDown());
        }
     }
   else
     {
      //--- left mouse button is released
      if(m_mouse_flags==MOUSE_INVALID_FLAGS)
        {
         //--- cursor entered the control with mouse button released
         //--- activate control and save state to the member
         m_mouse_flags=flags;
         //--- generate event
         EventChartCustom(CONTROLS_SELF_MESSAGE,ON_MOUSE_FOCUS_SET,m_id,0.0,m_name);
         //--- activate
         return(Activate());
        }
      if((m_mouse_flags&MOUSE_LEFT)!=0)
        {
         //--- mouse button has been pressed (clicking)
         //--- save the state and coordinates
         m_mouse_flags=flags;
         m_mouse_x    =x;
         m_mouse_y    =y;
         //--- call the handler
         return(OnMouseUp());
        }
     }
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CWnd::CWnd(void) : m_chart_id(CONTROLS_INVALID_ID),
                   m_subwin(CONTROLS_INVALID_ID),
                   m_name(NULL),
                   m_id(CONTROLS_INVALID_ID),
                   m_state_flags(WND_STATE_FLAG_ENABLE+WND_STATE_FLAG_VISIBLE),
                   m_prop_flags(0),
                   m_align_flags(WND_ALIGN_NONE),
                   m_align_left(0),
                   m_align_top(0),
                   m_align_right(0),
                   m_align_bottom(0),
                   m_mouse_x(0),
                   m_mouse_y(0),
                   m_mouse_flags(MOUSE_INVALID_FLAGS),
                   m_last_click(0),
                   m_drag_object(NULL)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CWnd::~CWnd(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CWnd::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- attach to chart
   m_chart_id=chart;
   m_name    =name;
   m_subwin  =subwin;
//--- set coordinates of area
   Left(x1);
   Top(y1);
   Right(x2);
   Bottom(y2);
//--- always successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Destruction of the control                                       |
//+------------------------------------------------------------------+
void CWnd::Destroy(const int reason)
  {
//--- call virtual event handler
   if(OnDestroy())
      m_name="";
  }
//+------------------------------------------------------------------+
//| Find control by specified ID                                     |
//+------------------------------------------------------------------+
CWnd* CWnd::ControlFind(const long id)
  {
   CWnd *result=NULL;
//--- check
   if(id==m_id)
      result=GetPointer(this);
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
//| Change width of control                                          |
//+------------------------------------------------------------------+
bool CWnd::Width(const int w)
  {
//--- change width
   m_rect.Width(w);
//--- call virtual event handler
   return(OnResize());
  }
//+------------------------------------------------------------------+
//| Change height of control                                         |
//+------------------------------------------------------------------+
bool CWnd::Height(const int h)
  {
//--- change height
   m_rect.Height(h);
//--- call virtual event handler
   return(OnResize());
  }
//+------------------------------------------------------------------+
//| Resize control                                                   |
//+------------------------------------------------------------------+
bool CWnd::Size(const int w,const int h)
  {
//--- change size
   m_rect.Size(w,h);
//--- call virtual event handler
   return(OnResize());
  }
//+------------------------------------------------------------------+
//| Resize control                                                   |
//+------------------------------------------------------------------+
bool CWnd::Size(const CSize &size)
  {
//--- change size
   m_rect.Size(size);
//--- call virtual event handler
   return(OnResize());
  }
//+------------------------------------------------------------------+
//| Absolute movement of the control 	                              |
//+------------------------------------------------------------------+
bool CWnd::Move(const int x,const int y)
  {
//--- moving
   m_rect.Move(x,y);
//--- call virtual event handler
   return(OnMove());
  }
//+------------------------------------------------------------------+
//| Absolute movement of the control 	                              |
//+------------------------------------------------------------------+
bool CWnd::Move(const CPoint &point)
  {
//--- moving
   m_rect.Move(point);
//--- call virtual event handler
   return(OnMove());
  }
//+------------------------------------------------------------------+
//| Relative movement of the control 	                              |
//+------------------------------------------------------------------+
bool CWnd::Shift(const int dx,const int dy)
  {
//--- moving
   m_rect.Shift(dx,dy);
//--- call virtual event handler
   return(OnMove());
  }
//+------------------------------------------------------------------+
//| Check contains                                                   |
//+------------------------------------------------------------------+
bool CWnd::Contains(CWnd *control) const
  {
//--- check
   if(control==NULL)
      return(false);
//--- result
   return(Contains(control.Left(),control.Top()) && Contains(control.Right(),control.Bottom()));
  }
//+------------------------------------------------------------------+
//| Enables event handling by the control                            |
//+------------------------------------------------------------------+
bool CWnd::Enable(void)
  {
//--- if there are now changes, then succeed
   if(IS_ENABLED)
      return(true);
//--- change flag
   StateFlagsSet(WND_STATE_FLAG_ENABLE);
//--- call virtual event handler
   return(OnEnable());
  }
//+------------------------------------------------------------------+
//| Disables event handling by the control                           |
//+------------------------------------------------------------------+
bool CWnd::Disable(void)
  {
//--- if there are now changes, then succeed
   if(!IS_ENABLED)
      return(true);
//--- change flag
   StateFlagsReset(WND_STATE_FLAG_ENABLE);
//--- call virtual event handler
   return(OnDisable());
  }
//+------------------------------------------------------------------+
//| Set the "object is visible" flag for the control                 |
//+------------------------------------------------------------------+
bool CWnd::Visible(const bool flag)
  {
//--- if there are now changes, then succeed
   if(IS_VISIBLE==flag)
      return(true);
//--- call virtual event handler
   return(flag ? Show() : Hide());
  }
//+------------------------------------------------------------------+
//| Makes the control visible                                        |
//+------------------------------------------------------------------+
bool CWnd::Show(void)
  {
//--- change flag
   StateFlagsSet(WND_STATE_FLAG_VISIBLE);
//--- call virtual event handler
   return(OnShow());
  }
//+------------------------------------------------------------------+
//| Makes the control hidden                                         |
//+------------------------------------------------------------------+
bool CWnd::Hide(void)
  {
//--- change flag
   StateFlagsReset(WND_STATE_FLAG_VISIBLE);
//--- call virtual event handler
   return(OnHide());
  }
//+------------------------------------------------------------------+
//| Makes the control active                                         |
//+------------------------------------------------------------------+
bool CWnd::Activate(void)
  {
//--- if there are now changes, then succeed
   if(IS_ACTIVE)
      return(true);
//--- change flag
   StateFlagsSet(WND_STATE_FLAG_ACTIVE);
//--- call virtual event handler
   return(OnActivate());
  }
//+------------------------------------------------------------------+
//| Makes the control inactive                                       |
//+------------------------------------------------------------------+
bool CWnd::Deactivate(void)
  {
//--- if there are now changes, then succeed
   if(!IS_ACTIVE)
      return(true);
//--- change flag
   StateFlagsReset(WND_STATE_FLAG_ACTIVE);
//--- call virtual event handler
   return(OnDeactivate());
  }
//+------------------------------------------------------------------+
//| Set ID of control                                                |
//+------------------------------------------------------------------+
long CWnd::Id(const long id)
  {
   m_id=id;
//--- always use only one ID
   return(1);
  }
//+------------------------------------------------------------------+
//| Set parameters of alignment                                      |
//+------------------------------------------------------------------+
void CWnd::Alignment(const int flags,const int left,const int top,const int right,const int bottom)
  {
   m_align_flags =flags;
   m_align_left  =left;
   m_align_top   =top;
   m_align_right =right;
   m_align_bottom=bottom;
  }
//+------------------------------------------------------------------+
//| Align element in specified chart area                            |
//+------------------------------------------------------------------+
bool CWnd::Align(const CRect &rect)
  {
   int new_value=0;
//--- check
   if(m_align_flags==WND_ALIGN_NONE)
      return(true);
//--- we are interested only in alignment by right and bottom borders,
//--- as left and right borders are processed in the OnMove()
   if((m_align_flags&WND_ALIGN_RIGHT)!=0)
     {
      //--- there is alignment by right border,
      if((m_align_flags&WND_ALIGN_LEFT)!=0)
        {
         //--- and by left border (change size)
         new_value=rect.Width()-m_align_left-m_align_right;
         if(!Size(new_value,Height()))
            return(false);
        }
      else
        {
         //--- no alignment by left border (move)
         new_value=rect.right-Width()-m_align_right;
         if(!Move(new_value,Top()))
            return(false);
        }
     }
   if((m_align_flags&WND_ALIGN_BOTTOM)!=0)
     {
      //--- there is alignment by bottom border,
      if((m_align_flags&WND_ALIGN_TOP)!=0)
        {
         //--- and by top border (change size)
         new_value=rect.Height()-m_align_top-m_align_bottom;
         if(!Size(Width(),new_value))
            return(false);
        }
      else
        {
         //--- no alignment by top border (move)
         new_value=rect.bottom-Height()-m_align_bottom;
         if(!Move(Left(),new_value))
            return(false);
        }
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Remove the mouse focus from control                              |
//+------------------------------------------------------------------+
bool CWnd::MouseFocusKill(const long id)
  {
//--- check
   if(id==m_id)
      return(false);
//--- reset flag
   Deactivate();
//--- clean
   m_mouse_x    =0;
   m_mouse_y    =0;
   m_mouse_flags=MOUSE_INVALID_FLAGS;
//--- call the handler
   return(OnDeactivate());
  }
//+------------------------------------------------------------------+
//| Increases the priority of an element                             |
//+------------------------------------------------------------------+
bool CWnd::BringToTop(void)
  {
//--- generate event
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_BRING_TO_TOP,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "click" event                                     |
//+------------------------------------------------------------------+
bool CWnd::OnClick(void)
  {
//--- send notification
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_CLICK,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "doubl click" event                               |
//+------------------------------------------------------------------+
bool CWnd::OnDblClick(void)
  {
//--- send notification
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_DBL_CLICK,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on the left mouse button                        |
//+------------------------------------------------------------------+
bool CWnd::OnMouseDown(void)
  {
   if(IS_CAN_DRAG)
      return(OnDragStart());
   if(IS_CLICKS_BY_PRESS)
      return(OnClick());
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of releasing the left mouse button                       |
//+------------------------------------------------------------------+
bool CWnd::OnMouseUp(void)
  {
   if(IS_CAN_DBL_CLICK)
     {
      uint last_time=GetTickCount();
      if(m_last_click==0 || last_time-m_last_click>CONTROLS_DBL_CLICK_TIME)
        {
         m_last_click=(last_time==0) ? 1 : last_time;
        }
      else
        {
         m_last_click=0;
         return(OnDblClick());
        }
     }
   if(IS_CAN_DRAG)
      return(OnDragEnd());
   if(!IS_CLICKS_BY_PRESS)
      return(OnClick());
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the control dragging start                            |
//+------------------------------------------------------------------+
bool CWnd::OnDragStart(void)
  {
   if(!IS_CAN_DRAG)
      return(true);
//--- disable scrolling of chart with mouse
   ChartSetInteger(m_chart_id,CHART_MOUSE_SCROLL,false);
//--- generate event
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_DRAG_START,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of control dragging process                              |
//+------------------------------------------------------------------+
bool CWnd::OnDragProcess(const int x,const int y)
  {
   Shift(x-m_mouse_x,y-m_mouse_y);
//--- save
   m_mouse_x=x;
   m_mouse_y=y;
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the control dragging end                              |
//+------------------------------------------------------------------+
bool CWnd::OnDragEnd(void)
  {
   if(!IS_CAN_DRAG)
      return(true);
//--- enable scrolling of chart with mouse
   ChartSetInteger(m_chart_id,CHART_MOUSE_SCROLL,true);
//--- generate event
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_DRAG_END,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Destroy the dragged object                                       |
//+------------------------------------------------------------------+
bool CWnd::DragObjectDestroy(void)
  {
   if(m_drag_object!=NULL)
     {
      delete m_drag_object;
      m_drag_object=NULL;
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CDragWnd                                                   |
//| Usage: base class for drag                                       |
//+------------------------------------------------------------------+
class CDragWnd : public CWnd
  {
protected:
   int               m_limit_left;          // left constraint
   int               m_limit_top;           // top constraint
   int               m_limit_right;         // right constraint
   int               m_limit_bottom;        // bottom constraint

public:
                     CDragWnd(void);
                    ~CDragWnd(void);
   //--- constraints
   void              Limits(const int l,const int t,const int r,const int b);

protected:
   virtual bool      OnDragProcess(const int x,const int y);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CDragWnd::CDragWnd(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CDragWnd::~CDragWnd(void)
  {
  }
//+------------------------------------------------------------------+
//| Constrain the control dragging                                   |
//+------------------------------------------------------------------+
void CDragWnd::Limits(const int l,const int t,const int r,const int b)
  {
//--- save
   m_limit_left  =l;
   m_limit_top   =t;
   m_limit_right =r;
   m_limit_bottom=b;
  }
//+------------------------------------------------------------------+
//| Handler of control dragging process                              |
//+------------------------------------------------------------------+
bool CDragWnd::OnDragProcess(const int x,const int y)
  {
   int dx=x-m_mouse_x;
   int dy=y-m_mouse_y;
//--- check shift
   if(Right()+dx>m_limit_right)
      dx=m_limit_right-Right();
   if(Left()+dx<m_limit_left)
      dx=m_limit_left-Left();
   if(Bottom()+dy>m_limit_bottom)
      dy=m_limit_bottom-Bottom();
   if(Top()+dy<m_limit_top)
      dy=m_limit_top-Top();
//--- shift
   Shift(dx,dy);
//--- save
   m_mouse_x=x;
   m_mouse_y=y;
//--- generate event
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_DRAG_PROCESS,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
