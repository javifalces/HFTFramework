//+------------------------------------------------------------------+
//|                                                   DatePicker.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndContainer.mqh"
#include "Edit.mqh"
#include "BmpButton.mqh"
#include "DateDropList.mqh"
//+------------------------------------------------------------------+
//| Resources                                                        |
//+------------------------------------------------------------------+
//--- Can not place the same file into resource twice
#resource "res\\DateDropOn.bmp"                 // image file
#resource "res\\DateDropOff.bmp"                // image file
//+------------------------------------------------------------------+
//| Class CDatePicker                                                |
//| Usage: date picker                                               |
//+------------------------------------------------------------------+
class CDatePicker : public CWndContainer
  {
private:
   //--- dependent controls
   CEdit             m_edit;                // the entry field object
   CBmpButton        m_drop;                // the button object
   CDateDropList     m_list;                // the drop-down list object
   //--- data
   datetime          m_value;               // current value

public:
                     CDatePicker(void);
                    ~CDatePicker(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- data
   datetime          Value(void)              const { return(m_value);                                    }
   void              Value(datetime value)          { m_edit.Text(TimeToString(m_value=value,TIME_DATE)); }
   //--- state
   virtual bool      Show(void);
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);

protected:
   //--- create dependent controls
   virtual bool      CreateEdit(void);
   virtual bool      CreateButton(void);
   virtual bool      CreateList(void);
   //--- handlers of the dependent controls events
   virtual bool      OnClickEdit(void);
   virtual bool      OnClickButton(void);
   virtual bool      OnChangeList(void);
   //--- show drop-down list
   bool              ListShow(void);
   bool              ListHide(void);
   void              CheckListHide(const int id,int x,int y);
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
EVENT_MAP_BEGIN(CDatePicker)
   ON_EVENT(ON_CLICK,m_edit,OnClickEdit)
   ON_EVENT(ON_CLICK,m_drop,OnClickButton)
   ON_EVENT(ON_CHANGE,m_list,OnChangeList)
CheckListHide(id,(int)lparam,(int)dparam);
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
EVENT_MAP_END(CWndContainer)
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CDatePicker::CDatePicker(void) : m_value(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CDatePicker::~CDatePicker(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CDatePicker::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- call method of the parent class
   if(!CWndContainer::Create(chart,name,subwin,x1,y1,x2,y2))
      return(false);
//--- create dependent controls
   if(!CreateEdit())
      return(false);
   if(!CreateButton())
      return(false);
   if(!CreateList())
      return(false);
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Create main entry field                                          |
//+------------------------------------------------------------------+
bool CDatePicker::CreateEdit(void)
  {
//--- create
   if(!m_edit.Create(m_chart_id,m_name+"Edit",m_subwin,0,0,Width(),Height()))
      return(false);
   if(!m_edit.Text(""))
      return(false);
   if(!m_edit.ReadOnly(true))
      return(false);
   if(!Add(m_edit))
      return(false);
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Create button                                                    |
//+------------------------------------------------------------------+
bool CDatePicker::CreateButton(void)
  {
//--- right align button (try to make equal offsets from top and bottom)
   int x1=Width()-(2*CONTROLS_BUTTON_SIZE+CONTROLS_COMBO_BUTTON_X_OFF);
   int y1=(Height()-CONTROLS_BUTTON_SIZE)/2;
   int x2=x1+2*CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create
   if(!m_drop.Create(m_chart_id,m_name+"Drop",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_drop.BmpNames("::res\\DateDropOff.bmp","::res\\DateDropOn.bmp"))
      return(false);
   if(!Add(m_drop))
      return(false);
   m_drop.Locking(true);
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Create drop-down list                                            |
//+------------------------------------------------------------------+
bool CDatePicker::CreateList(void)
  {
//--- create
   if(!m_list.Create(m_chart_id,m_name+"List",m_subwin,0,Height()-1,Width(),0))
      return(false);
   if(!Add(m_list))
      return(false);
   m_list.Hide();
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Makes the control visible                                        |
//+------------------------------------------------------------------+
bool CDatePicker::Show(void)
  {
   m_edit.Show();
   m_drop.Show();
   m_list.Hide();
//--- call method of the parent class
   return(CWnd::Show());
  }
//+------------------------------------------------------------------+
//| save                                                             |
//+------------------------------------------------------------------+
bool CDatePicker::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE)
      return(false);
//--- write
   FileWriteLong(file_handle,Value());
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| load                                                             |
//+------------------------------------------------------------------+
bool CDatePicker::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE)
      return(false);
//--- load
   if(!FileIsEnding(file_handle))
      Value(FileReadLong(file_handle));
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on main entry field                             |
//+------------------------------------------------------------------+
bool CDatePicker::OnClickEdit(void)
  {
//--- change button state
   if(!m_drop.Pressed(!m_drop.Pressed()))
      return(false);
//--- call the click on button handler
   return(OnClickButton());
  }
//+------------------------------------------------------------------+
//| Handler of click on button                                       |
//+------------------------------------------------------------------+
bool CDatePicker::OnClickButton(void)
  {
//--- show or hide the drop-down list depending on the button state
   return((m_drop.Pressed()) ? ListShow():ListHide());
  }
//+------------------------------------------------------------------+
//| Handler of change on drop-down list                              |
//+------------------------------------------------------------------+
bool CDatePicker::OnChangeList(void)
  {
   string text=TimeToString(m_value=m_list.Value(),TIME_DATE);
//--- hide the list, depress the button
   ListHide();
   m_drop.Pressed(false);
//--- set text in the main entry field
   m_edit.Text(text);
//--- send notification
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_CHANGE,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Show the drop-down list                                          |
//+------------------------------------------------------------------+
bool CDatePicker::ListShow(void)
  {
//--- set value
   m_list.Value(m_value);
//--- show the list
   return(m_list.Show());
  }
//+------------------------------------------------------------------+
//| Hide drop-down list                                              |
//+------------------------------------------------------------------+
bool CDatePicker::ListHide(void)
  {
//--- hide the list
   return(m_list.Hide());
  }
//+------------------------------------------------------------------+
//| Hide the drop-down element if necessary                          |
//+------------------------------------------------------------------+
void CDatePicker::CheckListHide(const int id,int x,int y)
  {
//--- check event ID
   if(id!=CHARTEVENT_CLICK)
      return;
//--- check visibility of the drop-down element
   if(!m_list.IsVisible())
      return;
//--- check mouse cursor's position
   y-=(int)ChartGetInteger(m_chart_id,CHART_WINDOW_YDISTANCE,m_subwin);
   if(!m_edit.Contains(x,y) && !m_list.Contains(x,y))
     {
      m_drop.Pressed(false);
      m_list.Hide();
     }
  }
//+------------------------------------------------------------------+
