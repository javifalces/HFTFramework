//+------------------------------------------------------------------+
//|                                                 DateDropList.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndContainer.mqh"
#include "BmpButton.mqh"
#include "Picture.mqh"
#include <Canvas\Canvas.mqh>
#include <Tools\DateTime.mqh>
//+------------------------------------------------------------------+
//| Enumerations                                                     |
//+------------------------------------------------------------------+
//--- date modes
enum ENUM_DATE_MODES
  {
   DATE_MODE_MON,                           // month mode
   DATE_MODE_YEAR                           // year mode
  };
//+------------------------------------------------------------------+
//| Resources                                                        |
//+------------------------------------------------------------------+
//--- Can not place the same file into resource twice
#resource "res\\LeftTransp.bmp"
#resource "res\\RightTransp.bmp"
//+------------------------------------------------------------------+
//| Class CDateDropList                                              |
//| Usage: drop-down list                                            |
//+------------------------------------------------------------------+
class CDateDropList : public CWndContainer
  {
private:
   //--- dependent controls
   CBmpButton        m_dec;                 // the button object
   CBmpButton        m_inc;                 // the button object
   CPicture          m_list;                // the drop-down list object
   CCanvas           m_canvas;              // and its canvas
   //--- data
   CDateTime         m_value;               // current value
   //--- variable
   ENUM_DATE_MODES   m_mode;                // operation mode
   CRect             m_click_rect[32];      // array of click sensibility areas on canvas

public:
                     CDateDropList(void);
                    ~CDateDropList(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- data
   datetime          Value(void)                    { return(StructToTime(m_value)); }
   void              Value(datetime value)          { m_value.Date(value);           }
   void              Value(MqlDateTime& value)      { m_value=value;                 }
   //--- state
   virtual bool      Show(void);

protected:
   //--- internal event handlers
   virtual bool      OnClick(void);
   //--- create dependent controls
   virtual bool      CreateButtons(void);
   virtual bool      CreateList(void);
   //--- draw
   void              DrawCanvas(void);
   void              DrawClickRect(const int idx,int x,int y,string text,const uint clr,uint alignment=0);
   //--- handlers of the dependent controls events
   virtual bool      OnClickDec(void);
   virtual bool      OnClickInc(void);
   virtual bool      OnClickList(void);
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
EVENT_MAP_BEGIN(CDateDropList)
   ON_EVENT(ON_CLICK,m_dec,OnClickDec)
   ON_EVENT(ON_CLICK,m_inc,OnClickInc)
   ON_EVENT(ON_CLICK,m_list,OnClickList)
EVENT_MAP_END(CWndContainer)
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CDateDropList::CDateDropList(void) : m_mode(DATE_MODE_MON)
  {
   ZeroMemory(m_value);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CDateDropList::~CDateDropList(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CDateDropList::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
//--- need to find dimensions depending on font size
//--- width 7 columns + 2 offsets
   int w=7*(2*CONTROLS_FONT_SIZE)+2*CONTROLS_FONT_SIZE;
//--- header height + 7 rows
   int h=(CONTROLS_BUTTON_SIZE+4*CONTROLS_BORDER_WIDTH)+7*(2*CONTROLS_FONT_SIZE);
//--- call method of the parent class
   if(!CWndContainer::Create(chart,name,subwin,x1,y1,x1+w,y1+h))
      return(false);
//--- create dependent controls
   if(!CreateList())
      return(false);
   if(!CreateButtons())
      return(false);
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Create drop-down list                                            |
//+------------------------------------------------------------------+
bool CDateDropList::CreateList(void)
  {
//--- create object
   if(!m_list.Create(m_chart_id,m_name+"List",m_subwin,0,0,Width(),Height()))
      return(false);
   if(!Add(m_list))
      return(false);
//--- create canvas
   if(!m_canvas.Create(m_name,Width(),Height()))
      return(false);
   m_canvas.FontSet(CONTROLS_FONT_NAME,CONTROLS_FONT_SIZE*(-10));
   m_list.BmpName(m_canvas.ResourceName());
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Create buttons                                                   |
//+------------------------------------------------------------------+
bool CDateDropList::CreateButtons(void)
  {
//--- right align button (try to make equal offsets from top and bottom)
   int x1=2*CONTROLS_BORDER_WIDTH;
   int y1=2*CONTROLS_BORDER_WIDTH;
   int x2=x1+CONTROLS_BUTTON_SIZE;
   int y2=y1+CONTROLS_BUTTON_SIZE;
//--- create "Dec" button
   if(!m_dec.Create(m_chart_id,m_name+"Dec",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_dec.BmpNames("::res\\LeftTransp.bmp"))
      return(false);
   if(!Add(m_dec))
      return(false);
//---
   x2=Width()-2*CONTROLS_BORDER_WIDTH;
   x1=x2-CONTROLS_BUTTON_SIZE;
//--- create "Inc" button
   if(!m_inc.Create(m_chart_id,m_name+"Inc",m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_inc.BmpNames("::res\\RightTransp.bmp"))
      return(false);
   if(!Add(m_inc))
      return(false);
//--- succeeded
   return(true);
  }
//+------------------------------------------------------------------+
//| Makes the control visible                                        |
//+------------------------------------------------------------------+
bool CDateDropList::Show(void)
  {
//--- draw canvas
   DrawCanvas();
//--- call method of the parent class
   return(CWndContainer::Show());
  }
//+------------------------------------------------------------------+
//| Handler of click on button                                       |
//+------------------------------------------------------------------+
bool CDateDropList::OnClickDec(void)
  {
   switch(m_mode)
     {
      //--- within the month
      case DATE_MODE_MON:
         m_value.MonDec();
         break;
         //--- within the year
      case DATE_MODE_YEAR:
         m_value.YearDec();
         break;
     }
   DrawCanvas();
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on button                                       |
//+------------------------------------------------------------------+
bool CDateDropList::OnClickInc(void)
  {
   switch(m_mode)
     {
      //--- within the month
      case DATE_MODE_MON:
         m_value.MonInc();
         break;
         //--- within the year
      case DATE_MODE_YEAR:
         m_value.YearInc();
         break;
     }
   DrawCanvas();
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of click on picture                                      |
//+------------------------------------------------------------------+
bool CDateDropList::OnClickList(void)
  {
   m_mouse_x=m_list.MouseX();
   m_mouse_y=m_list.MouseY();
//---
   OnClick();
//---
   m_mouse_x=0;
   m_mouse_y=0;
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "click" event                                     |
//+------------------------------------------------------------------+
bool CDateDropList::OnClick(void)
  {
   for(int i=0;i<32;i++)
     {
      if(m_click_rect[i].Contains(m_mouse_x,m_mouse_y))
        {
         if(i==0)
           {
            //--- clicked on the header
            switch(m_mode)
              {
               //--- within the month
               case DATE_MODE_MON:
                  //--- switch to the "within the year" mode
                  m_mode=DATE_MODE_YEAR;
                  DrawCanvas();
                  break;
                  //--- within the year
               case DATE_MODE_YEAR:
                  //--- do nothing for now
                  break;
              }
           }
         else
           {
            //--- selected
            switch(m_mode)
              {
               //--- within the month
               case DATE_MODE_MON:
                  m_value.Day(i);
                  Hide();
                  //--- send notification
                  EventChartCustom(CONTROLS_SELF_MESSAGE,ON_CHANGE,m_id,0.0,m_name);
                  break;
                  //--- within the year
               case DATE_MODE_YEAR:
                  m_value.Mon(i);
                  m_mode=DATE_MODE_MON;
                  DrawCanvas();
                  break;
               default:
                  break;
              }
           }
         break;
        }
     }
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Draw canvas                                                      |
//+------------------------------------------------------------------+
void CDateDropList::DrawCanvas(void)
  {
   int         x,y;
   int         dx,dy;
   string      text;
   uint        text_al=TA_CENTER|TA_VCENTER;
   CDateTime   tmp_date;
   int         rows,cols;
   int         idx;
//--- zero out array of areas
   for(int i=0;i<32;i++)
      ZeroMemory(m_click_rect[i]);
//---
   m_canvas.Erase(COLOR2RGB(CONTROLS_EDIT_COLOR_BG));
   m_canvas.Rectangle(0,0,Width()-1,Height()-1,COLOR2RGB(CONTROLS_EDIT_COLOR_BORDER));
   x=Width()/2;
   y=CONTROLS_BUTTON_SIZE/2+2*CONTROLS_BORDER_WIDTH;
   switch(m_mode)
     {
      //--- within the month
      case DATE_MODE_MON:
         text=m_value.MonthName()+" "+IntegerToString(m_value.year);
         DrawClickRect(0,x,y,text,COLOR2RGB(CONTROLS_EDIT_COLOR),text_al);
         rows=6;
         cols=7;
         x=dx=Width()/(cols+1);
         y+=y;
         dy=(Height()-y-2*CONTROLS_BORDER_WIDTH)/(rows+1);
         y+=dy/2;
         for(int i=0;i<cols;i++,x+=dx)
            m_canvas.TextOut(x,y,m_value.ShortDayName(i),COLOR2RGB(CONTROLS_EDIT_COLOR),text_al);
         //--- backup data
         tmp_date=m_value;
         //--- find the beginning of the first displayed week
         tmp_date.DayDec(tmp_date.day_of_week);
         while(tmp_date.mon==m_value.mon && tmp_date.day!=1)
            tmp_date.DayDec(cols);
         //--- draw
         idx=1;
         y+=dy;
         for(int i=0;i<rows;i++,y+=dy)
           {
            x=dx;
            for(int j=0;j<cols;j++,x+=dx)
              {
               text=IntegerToString(tmp_date.day);
               if(tmp_date.mon==m_value.mon)
                 {
                  if(tmp_date.day==m_value.day)
                     m_canvas.FillRectangle(x-dx/2,y-dy/2,x+dx/2,y+dy/2,COLOR2RGB(CONTROLS_COLOR_BG_SEL));
                  DrawClickRect(idx++,x,y,text,COLOR2RGB(CONTROLS_EDIT_COLOR),text_al);
                 }
               else
                  m_canvas.TextOut(x,y,text,COLOR2RGB(CONTROLS_BUTTON_COLOR_BORDER),text_al);
               tmp_date.DayInc();
              }
           }
         break;
         //--- within the year
      case DATE_MODE_YEAR:
         text=IntegerToString(m_value.year);
         DrawClickRect(0,x,y,text,COLOR2RGB(CONTROLS_EDIT_COLOR),text_al);
         rows=3;
         cols=4;
         x=dx=Width()/(cols+1);
         y+=y;
         dy=(Height()-y)/rows;
         y+=dy/2;
         for(int i=0;i<rows*cols;i++)
           {
            if(i+1==m_value.mon)
               m_canvas.FillRectangle(x-dx/2,y-dy/4,x+dx/2,y+dy/4,COLOR2RGB(CONTROLS_COLOR_BG_SEL));
            DrawClickRect(i+1,x,y,m_value.ShortMonthName(i+1),COLOR2RGB(CONTROLS_EDIT_COLOR),text_al);
            if(i%cols==cols-1)
              {
               x=dx;
               y+=dy;
              }
            else
               x+=dx;
           }
         break;
      default:
         break;
     }
   m_canvas.Update();
  }
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void CDateDropList::DrawClickRect(const int idx,int x,int y,string text,const uint clr,uint alignment)
  {
   int text_w,text_h;
//--- display the text
   m_canvas.TextOut(x,y,text,clr,alignment);
//--- determine area occupied by text
   m_canvas.TextSize(text,text_w,text_h);
//--- convert relative coordinated to absolute ones
   x+=Left();
   y+=Top();
//--- check flags of horizontal alignment
   switch(alignment&(TA_LEFT|TA_CENTER|TA_RIGHT))
     {
      case TA_LEFT:
         m_click_rect[idx].left=x;
         break;
      case TA_CENTER:
         m_click_rect[idx].left=x-text_w/2;
         break;
      case TA_RIGHT:
         m_click_rect[idx].left=x-text_w;
         break;
     }
   m_click_rect[idx].Width(text_w);
//--- check flags of vertical alignment
   switch(alignment&(TA_TOP|TA_VCENTER|TA_BOTTOM))
     {
      case TA_TOP:
         m_click_rect[idx].top=y;
         break;
      case TA_VCENTER:
         m_click_rect[idx].top=y-text_h/2;
         break;
      case TA_BOTTOM:
         m_click_rect[idx].top=y-text_h;
         break;
     }
   m_click_rect[idx].Height(text_h);
  }
//+------------------------------------------------------------------+
