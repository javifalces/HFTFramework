//+------------------------------------------------------------------+
//|                                                     CheckBox.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndContainer.mqh"
#include "BmpButton.mqh"
#include "Edit.mqh"
//+------------------------------------------------------------------+
//| Resources                                                        |
//+------------------------------------------------------------------+
#resource "res\\CheckBoxOn.bmp"
#resource "res\\CheckBoxOff.bmp"
//+------------------------------------------------------------------+
//| Class CCheckBox                                                  |
//| Usage: class that implements the "CheckBox" control              |
//+------------------------------------------------------------------+
class CCheckBox : public CWndContainer
  {
private:
   //--- dependent controls
   CBmpButton        m_button;              // button object
   CEdit             m_label;               // label object
   //--- data
   int               m_value;               // value

public:
                     CCheckBox(void);
                    ~CCheckBox(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- settings
   string            Text(void)          const { return(m_label.Text());         }
   bool              Text(const string value)  { return(m_label.Text(value));    }
   color             Color(void)         const { return(m_label.Color());        }
   bool              Color(const color value)  { return(m_label.Color(value));   }
   //--- state
   bool              Checked(void)       const { return(m_button.Pressed());     }
   bool              Checked(const bool flag)  { return(m_button.Pressed(flag)); }
   //--- data
   int               Value(void)         const { return(m_value);                }
   void              Value(const int value)    { m_value=value;                  }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);

protected:
   //--- create dependent controls
   virtual bool      CreateButton(void);
   virtual bool      CreateLabel(void);
   //--- handlers of the dependent controls events
   virtual bool      OnClickButton(void);
   virtual bool      OnClickLabel(void);
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
EVENT_MAP_BEGIN(CCheckBox)
   ON_EVENT(ON_CLICK,m_button,OnClickButton)
   ON_EVENT(ON_CLICK,m_label,OnClickLabel)
EVENT_MAP_END(CWndContainer)
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CCheckBox::CCheckBox(void) : m_value(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CCheckBox::~CCheckBox(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CCheckBox::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- call method of the parent class
   if(!CWndContainer::Create(chart,name,subwin,x1,y1,x2,y2))
      return(false);
//--- create dependent controls
   if(!CreateButton())
      return(false);
   if(!CreateLabel())
      return(false);
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Create button                                                    |
//+------------------------------------------------------------------+
bool CCheckBox::CreateButton(void)
  {
//--- calculate coordinates
   int x1=CONTROLS_CHECK_BUTTON_X_OFF;
   int y1=CONTROLS_CHECK_BUTTON_Y_OFF;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE-CONTROLS_BORDER_WIDTH;
//--- create
   if(!m_button.Create(m_chart_id,m_name+"Button",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_button.BmpNames("::res\\CheckBoxOff.bmp","::res\\CheckBoxOn.bmp"))
      return(false);
   if(!Add(m_button))
      return(false);
   m_button.Locking(true);
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Create label                                                     |
//+------------------------------------------------------------------+
bool CCheckBox::CreateLabel(void)
  {
//--- calculate coordinates
   int x1=CONTROLS_CHECK_LABEL_X_OFF;
   int y1=CONTROLS_CHECK_LABEL_Y_OFF;
   int x2=Width();
   int y2=Height();
//--- create
   if(!m_label.Create(m_chart_id,m_name+"Label",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_label.Text(m_name))
      return(false);
   if(!Add(m_label))
      return(false);
   m_label.ReadOnly(true);
   m_label.ColorBackground(CONTROLS_CHECKGROUP_COLOR_BG);
   m_label.ColorBorder(CONTROLS_CHECKGROUP_COLOR_BG);
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
bool CCheckBox::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE)
      return(false);
//---
   FileWriteInteger(file_handle,Checked());
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
bool CCheckBox::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE)
      return(false);
//---
   if(!FileIsEnding(file_handle))
      Checked(FileReadInteger(file_handle));
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on button                                       |
//+------------------------------------------------------------------+
bool CCheckBox::OnClickButton(void)
  {
//--- send the "changed state" event
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_CHANGE,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on label                                        |
//+------------------------------------------------------------------+
bool CCheckBox::OnClickLabel(void)
  {
//--- change button state
   m_button.Pressed(!m_button.Pressed());
//--- return the result of the button click handler
   return(OnClickButton());
  }
//+------------------------------------------------------------------+
