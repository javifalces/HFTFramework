//+------------------------------------------------------------------+
//|                                                       Dialog.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndContainer.mqh"
#include "WndClient.mqh"
#include "Panel.mqh"
#include "Edit.mqh"
#include "BmpButton.mqh"
#include <Charts\Chart.mqh>
//+------------------------------------------------------------------+
//| Resources                                                        |
//+------------------------------------------------------------------+
#resource "res\\Close.bmp"
#resource "res\\Restore.bmp"
#resource "res\\Turn.bmp"
//+------------------------------------------------------------------+
//| Class CDialog                                                    |
//| Usage: base class to create dialog boxes                         |
//|             and indicator panels                                 |
//+------------------------------------------------------------------+
class CDialog : public CWndContainer
  {
private:
   //--- dependent controls
   CPanel            m_white_border;        // the "white border" object
   CPanel            m_background;          // the background object
   CEdit             m_caption;             // the window title object
   CBmpButton        m_button_close;        // the "Close" button object
   CWndClient        m_client_area;         // the client area object

protected:
   //--- flags
   bool              m_panel_flag;          // the "panel in a separate window" flag
   //--- flags
   bool              m_minimized;           // "create in minimized state" flag
   //--- additional areas
   CRect             m_min_rect;            // minimal area coordinates
   CRect             m_norm_rect;           // normal area coordinates

public:
                     CDialog(void);
                    ~CDialog(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- set up
   string            Caption(void)                   const { return(m_caption.Text());               }
   bool              Caption(const string text)            { return(m_caption.Text(text));           }
   //--- fill
   bool              Add(CWnd *control);
   bool              Add(CWnd &control);
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);

protected:
   //--- create dependent controls
   virtual bool      CreateWhiteBorder(void);
   virtual bool      CreateBackground(void);
   virtual bool      CreateCaption(void);
   virtual bool      CreateButtonClose(void);
   virtual bool      CreateClientArea(void);
   //--- handlers of the dependent controls events
   virtual void      OnClickCaption(void);
   virtual void      OnClickButtonClose(void);
   //--- access properties of caption
   void              CaptionAlignment(const int flags,const int left,const int top,const int right,const int bottom)
                        { m_caption.Alignment(flags,left,top,right,bottom); }
   //--- access properties of client area
   bool              ClientAreaVisible(const bool visible) { return(m_client_area.Visible(visible)); }
   int               ClientAreaLeft(void)            const { return(m_client_area.Left());           }
   int               ClientAreaTop(void)             const { return(m_client_area.Top());            }
   int               ClientAreaRight(void)           const { return(m_client_area.Right());          }
   int               ClientAreaBottom(void)          const { return(m_client_area.Bottom());         }
   int               ClientAreaWidth(void)           const { return(m_client_area.Width());          }
   int               ClientAreaHeight(void)          const { return(m_client_area.Height());         }
   //--- handlers of drag
   virtual bool      OnDialogDragStart(void);
   virtual bool      OnDialogDragProcess(void);
   virtual bool      OnDialogDragEnd(void);
  };
//+------------------------------------------------------------------+
//| Common handler of events                                         |
//+------------------------------------------------------------------+
EVENT_MAP_BEGIN(CDialog)
   ON_EVENT(ON_CLICK,m_button_close,OnClickButtonClose)
   ON_EVENT(ON_CLICK,m_caption,OnClickCaption)
   ON_EVENT(ON_DRAG_START,m_caption,OnDialogDragStart)
   ON_EVENT_PTR(ON_DRAG_PROCESS,m_drag_object,OnDialogDragProcess)
   ON_EVENT_PTR(ON_DRAG_END,m_drag_object,OnDialogDragEnd)
EVENT_MAP_END(CWndContainer)
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CDialog::CDialog(void) : m_panel_flag(false),
                         m_minimized(false)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CDialog::~CDialog(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CDialog::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- call method of parent class
   if(!CWndContainer::Create(chart,name,subwin,x1,y1,x2,y2))
      return(false);
//--- create dependent controls
   if(!m_panel_flag && !CreateWhiteBorder())
      return(false);
   if(!CreateBackground())
      return(false);
   if(!CreateCaption())
      return(false);
   if(!CreateButtonClose())
      return(false);
   if(!CreateClientArea())
      return(false);
//--- set up additional areas
   m_norm_rect.SetBound(m_rect);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Add control to the client area (by pointer)                      |
//+------------------------------------------------------------------+
bool CDialog::Add(CWnd *control)
  {
   return(m_client_area.Add(control));
  }
//+------------------------------------------------------------------+
//| Add control to the client area (by reference)                    |
//+------------------------------------------------------------------+
bool CDialog::Add(CWnd &control)
  {
   return(m_client_area.Add(control));
  }
//+------------------------------------------------------------------+
//| Save                                                             |
//+------------------------------------------------------------------+
bool CDialog::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE)
      return(false);
//--- save
   FileWriteStruct(file_handle,m_norm_rect);
   FileWriteInteger(file_handle,m_min_rect.left);
   FileWriteInteger(file_handle,m_min_rect.top);
   FileWriteInteger(file_handle,m_minimized);
//--- result
   return(CWndContainer::Save(file_handle));
  }
//+------------------------------------------------------------------+
//| Load                                                             |
//+------------------------------------------------------------------+
bool CDialog::Load(const int file_handle)
  {
   if(file_handle==INVALID_HANDLE)
      return(false);
//--- load
   if(!FileIsEnding(file_handle))
     {
      FileReadStruct(file_handle,m_norm_rect);
      int left=FileReadInteger(file_handle);
      int top=FileReadInteger(file_handle);
      m_min_rect.Move(left,top);
      m_minimized=FileReadInteger(file_handle);
     }
//--- result
   return(CWndContainer::Load(file_handle));
  }
//+------------------------------------------------------------------+
//| Create "white border"                                            |
//+------------------------------------------------------------------+
bool CDialog::CreateWhiteBorder(void)
  {
//--- coordinates
   int x1=0;
   int y1=0;
   int x2=Width();
   int y2=Height();
//--- create
   if(!m_white_border.Create(m_chart_id,m_name+"Border",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_white_border.ColorBackground(CONTROLS_DIALOG_COLOR_BG))
      return(false);
   if(!m_white_border.ColorBorder(CONTROLS_DIALOG_COLOR_BORDER_LIGHT))
      return(false);
   if(!CWndContainer::Add(m_white_border))
      return(false);
   m_white_border.Alignment(WND_ALIGN_CLIENT,0,0,0,0);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create background                                                |
//+------------------------------------------------------------------+
bool CDialog::CreateBackground(void)
  {
   int off=(m_panel_flag) ? 0:CONTROLS_BORDER_WIDTH;
//--- coordinates
   int x1=off;
   int y1=off;
   int x2=Width()-off;
   int y2=Height()-off;
//--- create
   if(!m_background.Create(m_chart_id,m_name+"Back",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_background.ColorBackground(CONTROLS_DIALOG_COLOR_BG))
      return(false);
   color border=(m_panel_flag) ? CONTROLS_DIALOG_COLOR_BG : CONTROLS_DIALOG_COLOR_BORDER_DARK;
   if(!m_background.ColorBorder(border))
      return(false);
   if(!CWndContainer::Add(m_background))
      return(false);
   m_background.Alignment(WND_ALIGN_CLIENT,off,off,off,off);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create window title                                              |
//+------------------------------------------------------------------+
bool CDialog::CreateCaption(void)
  {
   int off=(m_panel_flag) ? 0:2*CONTROLS_BORDER_WIDTH;
//--- coordinates
   int x1=off;
   int y1=off;
   int x2=Width()-off;
   int y2=y1+CONTROLS_DIALOG_CAPTION_HEIGHT;
//--- create
   if(!m_caption.Create(m_chart_id,m_name+"Caption",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_caption.Color(CONTROLS_DIALOG_COLOR_CAPTION_TEXT))
      return(false);
   if(!m_caption.ColorBackground(CONTROLS_DIALOG_COLOR_BG))
      return(false);
   if(!m_caption.ColorBorder(CONTROLS_DIALOG_COLOR_BG))
      return(false);
   if(!m_caption.ReadOnly(true))
      return(false);
   if(!m_caption.Text(m_name))
      return(false);
   if(!CWndContainer::Add(m_caption))
      return(false);
   m_caption.Alignment(WND_ALIGN_WIDTH,off,0,off,0);
   if(!m_panel_flag)
      m_caption.PropFlags(WND_PROP_FLAG_CAN_DRAG);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create the "Close" button                                        |
//+------------------------------------------------------------------+
bool CDialog::CreateButtonClose(void)
  {
   int off=(m_panel_flag) ? 0 : 2*CONTROLS_BORDER_WIDTH;
//--- coordinates
   int x1=Width()-off-(CONTROLS_BUTTON_SIZE+CONTROLS_DIALOG_BUTTON_OFF);
   int y1=off+CONTROLS_DIALOG_BUTTON_OFF;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create
   if(!m_button_close.Create(m_chart_id,m_name+"Close",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_button_close.BmpNames("::res\\Close.bmp"))
      return(false);
   if(!CWndContainer::Add(m_button_close))
      return(false);
   m_button_close.Alignment(WND_ALIGN_RIGHT,0,0,off+CONTROLS_DIALOG_BUTTON_OFF,0);
//--- change caption
   CaptionAlignment(WND_ALIGN_WIDTH,off,0,off+(CONTROLS_BUTTON_SIZE+CONTROLS_DIALOG_BUTTON_OFF),0);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create client area                                               |
//+------------------------------------------------------------------+
bool CDialog::CreateClientArea(void)
  {
   int off=(m_panel_flag) ? 0:2*CONTROLS_BORDER_WIDTH;
//--- coordinates
   int x1=off+CONTROLS_DIALOG_CLIENT_OFF;
   int y1=off+CONTROLS_DIALOG_CAPTION_HEIGHT;
   int x2=Width()-(off+CONTROLS_DIALOG_CLIENT_OFF);
   int y2=Height()-(off+CONTROLS_DIALOG_CLIENT_OFF);
//--- create
   if(!m_client_area.Create(m_chart_id,m_name+"Client",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_client_area.ColorBackground(CONTROLS_DIALOG_COLOR_CLIENT_BG))
      return(false);
   if(!m_client_area.ColorBorder(CONTROLS_DIALOG_COLOR_CLIENT_BORDER))
      return(false);
   CWndContainer::Add(m_client_area);
   m_client_area.Alignment(WND_ALIGN_CLIENT,x1,y1,x1,x1);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on the window title                             |
//+------------------------------------------------------------------+
void CDialog::OnClickCaption(void)
  {
  }
//+------------------------------------------------------------------+
//| Handler of click on the "Close" button                           |
//+------------------------------------------------------------------+
void CDialog::OnClickButtonClose(void)
  {
   Visible(false);
  }
//+------------------------------------------------------------------+
//| Start dragging the dialog box                                    |
//+------------------------------------------------------------------+
bool CDialog::OnDialogDragStart(void)
  {
   if(m_drag_object==NULL)
     {
      m_drag_object=new CDragWnd;
      if(m_drag_object==NULL)
         return(false);
     }
//--- calculate coordinates
   int x1=Left()-CONTROLS_DRAG_SPACING;
   int y1=Top()-CONTROLS_DRAG_SPACING;
   int x2=Right()+CONTROLS_DRAG_SPACING;
   int y2=Bottom()+CONTROLS_DRAG_SPACING;
//--- create
   m_drag_object.Create(m_chart_id,"",m_subwin,x1,y1,x2,y2);
   m_drag_object.PropFlags(WND_PROP_FLAG_CAN_DRAG);
//--- constraints
   CChart chart;
   chart.Attach(m_chart_id);
   m_drag_object.Limits(-CONTROLS_DRAG_SPACING,-CONTROLS_DRAG_SPACING,
                        chart.WidthInPixels()+CONTROLS_DRAG_SPACING,
                        chart.HeightInPixels(m_subwin)+CONTROLS_DRAG_SPACING);
   chart.Detach();
//--- set mouse params
   m_drag_object.MouseX(m_caption.MouseX());
   m_drag_object.MouseY(m_caption.MouseY());
   m_drag_object.MouseFlags(m_caption.MouseFlags());
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Continue dragging the dialog box                                 |
//+------------------------------------------------------------------+
bool CDialog::OnDialogDragProcess(void)
  {
//--- checking
   if(m_drag_object==NULL)
      return(false);
//--- calculate coordinates
   int x=m_drag_object.Left()+50;
   int y=m_drag_object.Top()+50;
//--- move dialog
   Move(x,y);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| End dragging the dialog box                                      |
//+------------------------------------------------------------------+
bool CDialog::OnDialogDragEnd(void)
  {
   if(m_drag_object!=NULL)
     {
      m_caption.MouseFlags(m_drag_object.MouseFlags());
      delete m_drag_object;
      m_drag_object=NULL;
     }
//--- set up additional areas
   if(m_minimized)
      m_min_rect.SetBound(m_rect);
   else
      m_norm_rect.SetBound(m_rect);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CAppDialog                                                 |
//| Usage: main dialog box of MQL5 application                       |
//+------------------------------------------------------------------+
class CAppDialog : public CDialog
  {
private:
   //--- dependent controls
   CBmpButton        m_button_minmax;       // the "Minimize/Maximize" button object
   //--- variables
   string            m_program_name;        // name of program
   string            m_instance_id;         // unique string ID
   ENUM_PROGRAM_TYPE m_program_type;        // type of program
   string            m_indicator_name;
   int               m_deinit_reason;
   //--- for mouse
   int               m_subwin_Yoff;         // subwindow Y offset
   CWnd*             m_focused_wnd;         // pointer to object that has mouse focus
   CWnd*             m_top_wnd;             // pointer to object that has priority over mouse events handling

protected:
   CChart            m_chart;               // object to access chart

public:
                     CAppDialog(void);
                    ~CAppDialog(void);
   //--- main application dialog creation and destroy
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   virtual void      Destroy(const int reason=REASON_PROGRAM);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- dialog run
   bool              Run(void);
   //--- chart events processing
   void              ChartEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- set up
   void              Minimized(const bool flag) { m_minimized=flag; }
   //--- to save/restore state
   void              IniFileSave(void);
   void              IniFileLoad(void);
   virtual string    IniFileName(void) const;
   virtual string    IniFileExt(void) const { return(".dat"); }
   virtual bool      Load(const int file_handle);
   virtual bool      Save(const int file_handle);

private:
   bool              CreateCommon(const long chart,const string name,const int subwin);
   bool              CreateExpert(const int x1,const int y1,const int x2,const int y2);
   bool              CreateIndicator(const int x1,const int y1,const int x2,const int y2);

protected:
   //--- create dependent controls
   virtual bool      CreateButtonMinMax(void);
   //--- handlers of the dependent controls events
   virtual void      OnClickButtonClose(void);
   virtual void      OnClickButtonMinMax(void);
   //--- external event handlers
   virtual void      OnAnotherApplicationClose(const long &lparam,const double &dparam,const string &sparam);
   //--- methods
   virtual bool      Rebound(const CRect &rect);
   virtual void      Minimize(void);
   virtual void      Maximize(void);
   string            CreateInstanceId(void);
   string            ProgramName(void) const { return(m_program_name); }
   void              SubwinOff(void);
  };
//+------------------------------------------------------------------+
//| Common handler of events                                         |
//+------------------------------------------------------------------+
EVENT_MAP_BEGIN(CAppDialog)
ON_EVENT(ON_CLICK,m_button_minmax,OnClickButtonMinMax)
ON_EXTERNAL_EVENT(ON_APP_CLOSE,OnAnotherApplicationClose)
EVENT_MAP_END(CDialog)
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CAppDialog::CAppDialog(void) : m_program_type(WRONG_VALUE),
                               m_deinit_reason(WRONG_VALUE),
                               m_subwin_Yoff(0),
                               m_focused_wnd(NULL),
                               m_top_wnd(NULL)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CAppDialog::~CAppDialog(void)
  {
  }
//+------------------------------------------------------------------+
//| Application dialog initialization function                       |
//+------------------------------------------------------------------+
bool CAppDialog::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
   if(!CreateCommon(chart,name,subwin))
      return(false);
//---
   switch(m_program_type)
     {
      case PROGRAM_EXPERT:
         if(!CreateExpert(x1,y1,x2,y2))
         return(false);
         break;
      case PROGRAM_INDICATOR:
         if(!CreateIndicator(x1,y1,x2,y2))
         return(false);
         break;
      default:
         Print("CAppDialog: invalid program type");
         return(false);
     }
//--- Title of dialog window
   if(!Caption(m_program_name))
      return(false);
//--- create dependent controls
   if(!CreateButtonMinMax())
      return(false);
//--- get subwindow offset
   SubwinOff();
//--- if flag is set, minimize the dialog
   if(m_minimized)
      Minimize();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize common area                                           |
//+------------------------------------------------------------------+
bool CAppDialog::CreateCommon(const long chart,const string name,const int subwin)
  {
//--- save parameters
   m_chart_id     =chart;
   m_name         =name;
   m_subwin       =subwin;
   m_program_name =name;
   m_deinit_reason=WRONG_VALUE;
//--- get unique ID
   m_instance_id=CreateInstanceId();
//--- initialize chart object
   m_chart.Attach(chart);
//--- determine type of program
   m_program_type=(ENUM_PROGRAM_TYPE)MQL5InfoInteger(MQL5_PROGRAM_TYPE);
//--- specify object and mouse events
   if(!m_chart.EventObjectCreate() || !m_chart.EventObjectDelete() || !m_chart.EventMouseMove())
     {
      Print("CAppDialog: object events specify error");
      m_chart.Detach();
      return(false);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize in Expert Advisor                                     |
//+------------------------------------------------------------------+
bool CAppDialog::CreateExpert(const int x1,const int y1,const int x2,const int y2)
  {
//--- EA works only in main window
   m_subwin=0;
//--- geometry for the minimized state
   m_min_rect.SetBound(CONTROLS_DIALOG_MINIMIZE_LEFT,
                       CONTROLS_DIALOG_MINIMIZE_TOP,
                       CONTROLS_DIALOG_MINIMIZE_LEFT+CONTROLS_DIALOG_MINIMIZE_WIDTH,
                       CONTROLS_DIALOG_MINIMIZE_TOP+CONTROLS_DIALOG_MINIMIZE_HEIGHT);
//--- call method of the parent class
   if(!CDialog::Create(m_chart.ChartId(),m_instance_id,m_subwin,x1,y1,x2,y2))
     {
      Print("CAppDialog: expert dialog create error");
      m_chart.Detach();
      return(false);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize in Indicator                                          |
//+------------------------------------------------------------------+
bool CAppDialog::CreateIndicator(const int x1,const int y1,const int x2,const int y2)
  {
   int width=m_chart.WidthInPixels();
//--- geometry for the minimized state
   m_min_rect.LeftTop(0,0);
   m_min_rect.Width(width);
   m_min_rect.Height(CONTROLS_DIALOG_MINIMIZE_HEIGHT-2*CONTROLS_BORDER_WIDTH);
//--- determine subwindow
   m_subwin=ChartWindowFind();
   if(m_subwin==-1)
     {
      Print("CAppDialog: find subwindow error");
      m_chart.Detach();
      return(false);
     }
//---
   int total=ChartIndicatorsTotal(m_chart.ChartId(),m_subwin);
   m_indicator_name=ChartIndicatorName(m_chart.ChartId(),m_subwin,total-1);
//--- if subwindow number is 0 (main window), then our program is
//--- not an indicator panel, but is an indicator with built-in settings dialog
//--- dialog of such an indicator should behave as an Expert Advisor dialog
   if(m_subwin==0)
      return(CreateExpert(x1,y1,x2,y2));
//--- if subwindow number is not 0, then our program is an indicator panel
//--- check if subwindow is not occupied by other indicators
   if(total!=1)
     {
      Print("CAppDialog: subwindow busy");
      ChartIndicatorDelete(m_chart.ChartId(),m_subwin,ChartIndicatorName(m_chart.ChartId(),m_subwin,total-1));
      m_chart.Detach();
      return(false);
     }
//--- resize subwindow by dialog height
   if(!IndicatorSetInteger(INDICATOR_HEIGHT,(y2-y1)+1))
     {
      Print("CAppDialog: subwindow resize error");
      ChartIndicatorDelete(m_chart.ChartId(),m_subwin,ChartIndicatorName(m_chart.ChartId(),m_subwin,total-1));
      m_chart.Detach();
      return(false);
     }
//--- indicator short name
   m_indicator_name=m_program_name+IntegerToString(m_subwin);
   if(!IndicatorSetString(INDICATOR_SHORTNAME,m_indicator_name))
     {
      Print("CAppDialog: shortname error");
      ChartIndicatorDelete(m_chart.ChartId(),m_subwin,ChartIndicatorName(m_chart.ChartId(),m_subwin,total-1));
      m_chart.Detach();
      return(false);
     }
//--- set flag 
   m_panel_flag=true;
//--- call method of the parent class
   if(!CDialog::Create(m_chart.ChartId(),m_instance_id,m_subwin,0,0,width,y2-y1))
     {
      Print("CAppDialog: indicator dialog create error");
      ChartIndicatorDelete(m_chart.ChartId(),m_subwin,ChartIndicatorName(m_chart.ChartId(),m_subwin,total-1));
      m_chart.Detach();
      return(false);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Application dialog deinitialization function                     |
//+------------------------------------------------------------------+
void CAppDialog::Destroy(const int reason)
  {
//--- destroyed already?
   if(m_deinit_reason!=WRONG_VALUE)
      return;
//---
   m_deinit_reason=reason;
   IniFileSave();
//--- detach chart object from chart
   m_chart.Detach();
//--- call parent destroy
   CDialog::Destroy();
//---
   if(reason==REASON_PROGRAM)
     {
      if(m_program_type==PROGRAM_EXPERT)
         ExpertRemove();
      if(m_program_type==PROGRAM_INDICATOR)
         ChartIndicatorDelete(m_chart_id,m_subwin,m_indicator_name);
     }
//--- send message
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_APP_CLOSE,m_subwin,0.0,m_program_name);
  }
//+------------------------------------------------------------------+
//| Calculate subwindow offset                                       |
//+------------------------------------------------------------------+
void CAppDialog::SubwinOff(void)
  {
   m_subwin_Yoff=m_chart.SubwindowY(m_subwin);
  }
//+------------------------------------------------------------------+
//| Create the "Minimize/Maximize" button                            |
//+------------------------------------------------------------------+
bool CAppDialog::CreateButtonMinMax(void)
  {
   int off=(m_panel_flag) ? 0:2*CONTROLS_BORDER_WIDTH;
//--- coordinates
   int x1=Width()-off-2*(CONTROLS_BUTTON_SIZE+CONTROLS_DIALOG_BUTTON_OFF);
   int y1=off+CONTROLS_DIALOG_BUTTON_OFF;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create
   if(!m_button_minmax.Create(m_chart_id,m_name+"MinMax",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_button_minmax.BmpNames("::res\\Turn.bmp","::res\\Restore.bmp"))
      return(false);
   if(!CWndContainer::Add(m_button_minmax))
      return(false);
   m_button_minmax.Locking(true);
   m_button_minmax.Alignment(WND_ALIGN_RIGHT,0,0,off+CONTROLS_BUTTON_SIZE+2*CONTROLS_DIALOG_BUTTON_OFF,0);
//--- change caption
   CaptionAlignment(WND_ALIGN_WIDTH,off,0,off+2*(CONTROLS_BUTTON_SIZE+CONTROLS_DIALOG_BUTTON_OFF),0);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Charts event processing                                          |
//+------------------------------------------------------------------+
void CAppDialog::ChartEvent(const int id,const long &lparam,const double &dparam,const string &sparam)
  {
   int mouse_x=(int)lparam;
   int mouse_y=(int)dparam-m_subwin_Yoff;
//--- separate mouse events from others
   switch(id)
     {
      case CHARTEVENT_CHART_CHANGE:
         //--- assumed that the CHARTEVENT_CHART_CHANGE event can handle only the application dialog
         break;
      case CHARTEVENT_OBJECT_CLICK:
         //--- we won't handle the CHARTEVENT_OBJECT_CLICK event, as we are working with the CHARTEVENT_MOUSE_MOVE events
         return;
      case CHARTEVENT_CUSTOM+ON_MOUSE_FOCUS_SET:
         //--- the CHARTEVENT_CUSTOM + ON_MOUSE_FOCUS_SET event
         if(CheckPointer(m_focused_wnd)!=POINTER_INVALID)
           {
            //--- if there is an element with focus, try to take its focus away
            if(!m_focused_wnd.MouseFocusKill(lparam))
               return;
           }
         m_focused_wnd=ControlFind(lparam);
         return;
      case CHARTEVENT_CUSTOM+ON_BRING_TO_TOP:
         m_top_wnd=ControlFind(lparam);
         return;
      case CHARTEVENT_MOUSE_MOVE:
         //--- the CHARTEVENT_MOUSE_MOVE event
         if(CheckPointer(m_top_wnd)!=POINTER_INVALID)
           {
            //--- if a priority element already exists, pass control to it
            if(m_top_wnd.OnMouseEvent(mouse_x,mouse_y,(int)StringToInteger(sparam)))
              {
               //--- event handled
               m_chart.Redraw();
               return;
              }
           }
         if(OnMouseEvent(mouse_x,mouse_y,(int)StringToInteger(sparam)))
            m_chart.Redraw();
         return;
      default:
         //--- call event processing and redraw chart if event handled
         if(OnEvent(id,lparam,dparam,sparam))
         m_chart.Redraw();
         return;
     }
//--- if event was not handled, try to handle the CHARTEVENT_CHART_CHANGE event
   if(id==CHARTEVENT_CHART_CHANGE)
     {
      //--- if subwindow number is not 0, and dialog subwindow has changed its number, then restart
      if(m_subwin!=0 && m_subwin!=ChartWindowFind())
        {
         long fiction=1;
         OnAnotherApplicationClose(fiction,dparam,sparam);
        }
      //--- if subwindow height is less that dialog height, minimize application window (always)
      if(m_chart.HeightInPixels(m_subwin)<Height()+CONTROLS_BORDER_WIDTH)
        {
         m_button_minmax.Pressed(true);
         Minimize();
         m_chart.Redraw();
        }
      //--- if chart width is less that dialog width, and subwindow number is not 0, try to modify dialog width
      if(m_chart.WidthInPixels()!=Width() && m_subwin!=0)
        {
         Width(m_chart.WidthInPixels());
         m_chart.Redraw();
        }
      //--- get subwindow offset
      SubwinOff();
      return;
     }
  }
//+------------------------------------------------------------------+
//| Run application                                                  |
//+------------------------------------------------------------------+
bool CAppDialog::Run(void)
  {
//--- redraw chart for dialog invalidate
   m_chart.Redraw();
//--- here we begin to assign IDs to controls
   if(Id(m_subwin*CONTROLS_MAXIMUM_ID)>CONTROLS_MAXIMUM_ID)
     {
      Print("CAppDialog: too many objects");
      return(false);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Stop application                                                 |
//+------------------------------------------------------------------+
void CAppDialog::OnClickButtonClose(void)
  {
//--- destroy application
   Destroy();
  }
//+------------------------------------------------------------------+
//| Handler of click on the "Minimize/Maximize" button               |
//+------------------------------------------------------------------+
void CAppDialog::OnClickButtonMinMax(void)
  {
   if(m_button_minmax.Pressed())
      Minimize();
   else
      Maximize();
//--- get subwindow offset
   SubwinOff();
  }
//+------------------------------------------------------------------+
//| Resize                                                           |
//+------------------------------------------------------------------+
bool CAppDialog::Rebound(const CRect &rect)
  {
   if(!Move(rect.LeftTop()))
      return(false);
   if(!Size(rect.Size()))
      return(false);
//--- resize subwindow
   if(m_program_type==PROGRAM_INDICATOR && !IndicatorSetInteger(INDICATOR_HEIGHT,rect.Height()+1))
     {
      Print("CAppDialog: subwindow resize error");
      return(false);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Minimize dialog window                                           |
//+------------------------------------------------------------------+
void CAppDialog::Minimize(void)
  {
//--- set flag
   m_minimized=true;
//--- resize
   Rebound(m_min_rect);
//--- hide client area
   ClientAreaVisible(false);
  }
//+------------------------------------------------------------------+
//| Restore dialog window                                            |
//+------------------------------------------------------------------+
void CAppDialog::Maximize(void)
  {
//--- reset flag
   m_minimized=false;
//--- resize
   Rebound(m_norm_rect);
//--- show client area
   ClientAreaVisible(true);
  }
//+------------------------------------------------------------------+
//| Create unique prefix for object names                            |
//+------------------------------------------------------------------+
string CAppDialog::CreateInstanceId(void)
  {
   return(IntegerToString(rand(),5,'0'));
  }
//+------------------------------------------------------------------+
//| Handler of the ON_APP_CLOSE external event                       |
//+------------------------------------------------------------------+
void CAppDialog::OnAnotherApplicationClose(const long &lparam,const double &dparam,const string &sparam)
  {
//--- exit if we are in the main window
   if(m_subwin==0)
      return;
//--- exit if external program was closed in main window
   if(lparam==0)
      return;
//--- get subwindow offset
   SubwinOff();
//--- exit if external program was closed in subwindow with greater number
   if(lparam>=m_subwin)
      return;
//--- after all the checks we must change the subwindow
//--- get the new number of subwindow
   m_subwin=ChartWindowFind();
//--- change short name
   m_indicator_name=m_program_name+IntegerToString(m_subwin);
   IndicatorSetString(INDICATOR_SHORTNAME,m_indicator_name);
//--- change dialog title
   Caption(m_program_name);
//--- reassign IDs
   Run();
  }
//+------------------------------------------------------------------+
//| Save the current state of the program                            |
//+------------------------------------------------------------------+
void CAppDialog::IniFileSave(void)
  {
   string filename=IniFileName()+IniFileExt();
   int handle=FileOpen(filename,FILE_WRITE|FILE_BIN|FILE_ANSI);
//---
   if(handle!=INVALID_HANDLE)
     {
      Save(handle);
      FileClose(handle);
     }
  }
//+------------------------------------------------------------------+
//| Read the previous state of the program                           |
//+------------------------------------------------------------------+
void CAppDialog::IniFileLoad(void)
  {
   string filename=IniFileName()+IniFileExt();
   int handle=FileOpen(filename,FILE_READ|FILE_BIN|FILE_ANSI);
//---
   if(handle!=INVALID_HANDLE)
     {
      Load(handle);
      FileClose(handle);
     }
  }
//+------------------------------------------------------------------+
//| Generate the filename                                            |
//+------------------------------------------------------------------+
string CAppDialog::IniFileName(void) const
  {
   string name;
//---
   name=(m_indicator_name!=NULL) ? m_indicator_name : m_program_name;
//---
   name+="_"+Symbol();
   name+="_Ini";
//---
   return(name);
  }
//+------------------------------------------------------------------+
//| Load data                                                        |
//+------------------------------------------------------------------+
bool CAppDialog::Load(const int file_handle)
  {
   if(CDialog::Load(file_handle))
     {
      if(m_minimized)
        {
         m_button_minmax.Pressed(true);
         Minimize();
        }
      else
        {
         m_button_minmax.Pressed(false);
         Maximize();
        }
      int prev_deinit_reason=FileReadInteger(file_handle);
      if(prev_deinit_reason==REASON_CHARTCLOSE || prev_deinit_reason==REASON_CLOSE)
        {
         //--- if the previous time program ended after closing the chart window,
         //--- delete object left since the last start of the program
         string prev_instance_id=IntegerToString(FileReadInteger(file_handle),5,'0');
         if(prev_instance_id!=m_instance_id)
           {
            long chart_id=m_chart.ChartId();
            int  total=ObjectsTotal(chart_id,m_subwin);
            for(int i=total-1;i>=0;i--)
              {
               string obj_name=ObjectName(chart_id,i,m_subwin);
               if(StringFind(obj_name,prev_instance_id)==0)
                  ObjectDelete(chart_id,obj_name);
              }
           }
        }
      return(true);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Save data                                                        |
//+------------------------------------------------------------------+
bool CAppDialog::Save(const int file_handle)
  {
   if(CDialog::Save(file_handle))
     {
      FileWriteInteger(file_handle,m_deinit_reason);
      FileWriteInteger(file_handle,(int)StringToInteger(m_instance_id));
      return(true);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
