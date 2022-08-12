//+------------------------------------------------------------------+
//|                                                     ListView.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "WndClient.mqh"
#include "Edit.mqh"
#include <Arrays\ArrayString.mqh>
#include <Arrays\ArrayLong.mqh>
//+------------------------------------------------------------------+
//| Class CListView                                                  |
//| Usage: display lists                                             |
//+------------------------------------------------------------------+
class CListView : public CWndClient
  {
private:
   //--- dependent controls
   CEdit             m_rows[];              // array of the row objects
   //--- set up
   int               m_offset;              // index of first visible row in array of rows
   int               m_total_view;          // number of visible rows
   int               m_item_height;         // height of visible row
   bool              m_height_variable;     // признак переменной высоты списка
   //--- data
   CArrayString      m_strings;             // array of rows
   CArrayLong        m_values;              // array of values
   int               m_current;             // index of current row in array of rows

public:
                     CListView(void);
                    ~CListView(void);
   //--- create
   virtual bool      Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2);
   virtual void      Destroy(const int reason=0);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- set up
   bool              TotalView(const int value);
   //--- fill
   virtual bool      AddItem(const string item,const long value=0);
   //--- data
   virtual bool      ItemAdd(const string item,const long value=0);
   virtual bool      ItemInsert(const int index,const string item,const long value=0);
   virtual bool      ItemUpdate(const int index,const string item,const long value=0);
   virtual bool      ItemDelete(const int index);
   virtual bool      ItemsClear(void);
   //--- data
   int               Current(void) { return(m_current);               }
   string            Select(void)  { return(m_strings.At(m_current)); }
   bool              Select(const int index);
   bool              SelectByText(const string text);
   bool              SelectByValue(const long value);
   //--- data (read only)
   long              Value(void) { return(m_values.At(m_current));  }
   //--- state
   virtual bool      Show(void);

protected:
   //--- create dependent controls
   bool              CreateRow(const int index);
   //--- event handlers
   virtual bool      OnResize(void);
   //--- handlers of the dependent controls events
   virtual bool      OnVScrollShow(void);
   virtual bool      OnVScrollHide(void);
   virtual bool      OnScrollLineDown(void);
   virtual bool      OnScrollLineUp(void);
   virtual bool      OnItemClick(const int index);
   //--- redraw
   bool              Redraw(void);
   bool              RowState(const int index,const bool select);
   bool              CheckView(void);
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
EVENT_MAP_BEGIN(CListView)
   ON_INDEXED_EVENT(ON_CLICK,m_rows,OnItemClick)
EVENT_MAP_END(CWndClient)
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CListView::CListView(void) : m_offset(0),
                             m_total_view(0),
                             m_item_height(CONTROLS_LIST_ITEM_HEIGHT),
                             m_current(CONTROLS_INVALID_INDEX),
                             m_height_variable(false)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CListView::~CListView(void)
  {
  }
//+------------------------------------------------------------------+
//| Create a control                                                 |
//+------------------------------------------------------------------+
bool CListView::Create(const long chart,const string name,const int subwin,const int x1,const int y1,const int x2,const int y2)
  {
   int y=y2;
//--- if the number of visible rows is previously determined, adjust the vertical size
   if(!TotalView((y2-y1)/m_item_height))
      y=m_item_height+y1+2*CONTROLS_BORDER_WIDTH;
//--- check the number of visible rows
   if(m_total_view<1)
      return(false);
//--- call method of the parent class
   if(!CWndClient::Create(chart,name,subwin,x1,y1,x2,y))
      return(false);
//--- set up
   if(!m_background.ColorBackground(CONTROLS_LIST_COLOR_BG))
      return(false);
   if(!m_background.ColorBorder(CONTROLS_LIST_COLOR_BORDER))
      return(false);
//--- create dependent controls
   ArrayResize(m_rows,m_total_view);
   for(int i=0;i<m_total_view;i++)
     {
      if(!CreateRow(i))
         return(false);
      if(m_height_variable && i>0)
         m_rows[i].Hide();
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Delete group of controls                                         |
//+------------------------------------------------------------------+
void CListView::Destroy(const int reason)
  {
//--- call of the method of the parent class
   CWndClient::Destroy(reason);
//--- clear items
   m_strings.Clear();
   m_values.Clear();
//---
   m_offset    =0;
   m_total_view=0;
  }
//+------------------------------------------------------------------+
//| Set parameter                                                    |
//+------------------------------------------------------------------+
bool CListView::TotalView(const int value)
  {
//--- if parameter is not equal to 0, modifications are not possible
   if(m_total_view!=0)
     {
      m_height_variable=true;
      return(false);
     }
//--- save value
   m_total_view=value;
//--- parameter has been changed
   return(true);
  }
//+------------------------------------------------------------------+
//| Makes the control visible                                        |
//+------------------------------------------------------------------+
bool CListView::Show(void)
  {
//--- call of the method of the parent class
   CWndClient::Show();
//--- number of items
   int total=m_strings.Total();
//---
   if(total==0)
      total=1;
//---
   if(m_height_variable && total<m_total_view)
      for(int i=total;i<m_total_view;i++)
         m_rows[i].Hide();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Create "row"                                                     |
//+------------------------------------------------------------------+
bool CListView::CreateRow(const int index)
  {
//--- calculate coordinates
   int x1=CONTROLS_BORDER_WIDTH;
   int y1=CONTROLS_BORDER_WIDTH+m_item_height*index;
   int x2=Width()-2*CONTROLS_BORDER_WIDTH;
   int y2=y1+m_item_height;
//--- create
   if(!m_rows[index].Create(m_chart_id,m_name+"Item"+IntegerToString(index),m_subwin,x1,y1,x2,y2))
      return(false);
   if(!m_rows[index].Text(""))
      return(false);
   if(!m_rows[index].ReadOnly(true))
      return(false);
   if(!RowState(index,false))
      return(false);
   if(!Add(m_rows[index]))
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Add item (row)                                                   |
//+------------------------------------------------------------------+
bool CListView::AddItem(const string item,const long value)
  {
//--- method left for compatibility with previous version
   return(ItemAdd(item,value));
  }
//+------------------------------------------------------------------+
//| Add item (row)                                                   |
//+------------------------------------------------------------------+
bool CListView::ItemAdd(const string item,const long value)
  {
//--- add
   if(!m_strings.Add(item))
      return(false);
   if(!m_values.Add((value)?value:m_values.Total()))
      return(false);
//--- number of items
   int total=m_strings.Total();
//--- exit if number of items does not exceed the size of visible area
   if(total<m_total_view+1)
     {
      if(m_height_variable && total!=1)
        {
         Height(total*m_item_height+2*CONTROLS_BORDER_WIDTH);
         if(IS_VISIBLE)
            m_rows[total-1].Show();
        }
      return(Redraw());
     }
//--- if number of items exceeded the size of visible area
   if(total==m_total_view+1)
     {
      //--- enable vertical scrollbar
      if(!VScrolled(true))
         return(false);
      //--- and immediately make it invisible (if needed)
      if(IS_VISIBLE && !OnVScrollShow())
         return(false);
     }
//--- set up the scrollbar
   m_scroll_v.MaxPos(m_strings.Total()-m_total_view);
//--- redraw
   return(Redraw());
  }
//+------------------------------------------------------------------+
//| Insert item (row)                                                |
//+------------------------------------------------------------------+
bool CListView::ItemInsert(const int index,const string item,const long value)
  {
//--- insert
   if(!m_strings.Insert(item,index))
      return(false);
   if(!m_values.Insert(value,index))
      return(false);
//--- number of items
   int total=m_strings.Total();
//--- exit if number of items does not exceed the size of visible area
   if(total<m_total_view+1)
     {
      if(m_height_variable && total!=1)
        {
         Height(total*m_item_height+2*CONTROLS_BORDER_WIDTH);
         if(IS_VISIBLE)
            m_rows[total-1].Show();
        }
      return(Redraw());
     }
//--- if number of items exceeded the size of visible area
   if(total==m_total_view+1)
     {
      //--- enable vertical scrollbar
      if(!VScrolled(true))
         return(false);
      //--- and immediately make it invisible (if needed)
      if(IS_VISIBLE && !OnVScrollShow())
         return(false);
     }
//--- set up the scrollbar
   m_scroll_v.MaxPos(m_strings.Total()-m_total_view);
//--- redraw
   return(Redraw());
  }
//+------------------------------------------------------------------+
//| Update item (row)                                                |
//+------------------------------------------------------------------+
bool CListView::ItemUpdate(const int index,const string item,const long value)
  {
//--- update
   if(!m_strings.Update(index,item))
      return(false);
   if(!m_values.Update(index,value))
      return(false);
//--- redraw
   return(Redraw());
  }
//+------------------------------------------------------------------+
//| Delete item (row)                                                |
//+------------------------------------------------------------------+
bool CListView::ItemDelete(const int index)
  {
//--- delete
   if(!m_strings.Delete(index))
      return(false);
   if(!m_values.Delete(index))
      return(false);
//--- number of items
   int total=m_strings.Total();
//--- exit if number of items does not exceed the size of visible area
   if(total<m_total_view)
     {
      if(m_height_variable && total!=0)
        {
         Height(total*m_item_height+2*CONTROLS_BORDER_WIDTH);
         m_rows[total].Hide();
        }
      return(Redraw());
     }
//--- if number of items exceeded the size of visible area
   if(total==m_total_view)
     {
      //--- disable vertical scrollbar
      if(!VScrolled(false))
         return(false);
      //--- and immediately make it unvisible
      if(!OnVScrollHide())
         return(false);
     }
//--- set up the scrollbar
   m_scroll_v.MaxPos(m_strings.Total()-m_total_view);
//--- redraw
   return(Redraw());
  }
//+------------------------------------------------------------------+
//| Delete all items                                                 |
//+------------------------------------------------------------------+
bool CListView::ItemsClear(void)
  {
   m_offset=0;
//--- clear
   if(!m_strings.Shutdown())
      return(false);
   if(!m_values.Shutdown())
      return(false);
//---
   if(m_height_variable)
     {
      Height(m_item_height+2*CONTROLS_BORDER_WIDTH);
      for(int i=1;i<m_total_view;i++)
         m_rows[i].Hide();
     }
//--- disable vertical scrollbar
   if(!VScrolled(false))
      return(false);
//--- and immediately make it unvisible (if needed)
   if(!OnVScrollHide())
      return(false);
//--- redraw
   return(Redraw());
  }
//+------------------------------------------------------------------+
//| Sett current item                                                |
//+------------------------------------------------------------------+
bool CListView::Select(const int index)
  {
//--- check index
   if(index>=m_strings.Total())
      return(false);
   if(index<0 && index!=CONTROLS_INVALID_INDEX)
      return(false);
//--- unselect
   if(m_current!=CONTROLS_INVALID_INDEX)
      RowState(m_current-m_offset,false);
//--- select
   if(index!=CONTROLS_INVALID_INDEX)
      RowState(index-m_offset,true);
//--- save value
   m_current=index;
//--- succeed
   return(CheckView());
  }
//+------------------------------------------------------------------+
//| Set current item (by text)                                       |
//+------------------------------------------------------------------+
bool CListView::SelectByText(const string text)
  {
//--- find text
   int index=m_strings.SearchLinear(text);
//--- if text is not found, exit without changing the selection
   if(index==CONTROLS_INVALID_INDEX)
      return(false);
//--- change selection
   return(Select(index));
  }
//+------------------------------------------------------------------+
//| Set current item (by value)                                      |
//+------------------------------------------------------------------+
bool CListView::SelectByValue(const long value)
  {
//--- find value
   int index=m_values.SearchLinear(value);
//--- if value is not found, exit without changing the selection
   if(index==CONTROLS_INVALID_INDEX)
      return(false);
//--- change selection
   return(Select(index));
  }
//+------------------------------------------------------------------+
//| Redraw                                                           |
//+------------------------------------------------------------------+
bool CListView::Redraw(void)
  {
//--- loop by "rows"
   for(int i=0;i<m_total_view;i++)
     {
      //--- copy text
      if(!m_rows[i].Text(m_strings.At(i+m_offset)))
         return(false);
      //--- select
      if(!RowState(i,(m_current==i+m_offset)))
         return(false);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Change state                                                     |
//+------------------------------------------------------------------+
bool CListView::RowState(const int index,const bool select)
  {
//--- check index
   if(index<0 || index>=ArraySize(m_rows))
      return(true);
//--- determine colors
   color  text_color=(select) ? CONTROLS_LISTITEM_COLOR_TEXT_SEL : CONTROLS_LISTITEM_COLOR_TEXT;
   color  back_color=(select) ? CONTROLS_LISTITEM_COLOR_BG_SEL : CONTROLS_LISTITEM_COLOR_BG;
//--- get pointer
   CEdit *item=GetPointer(m_rows[index]);
//--- recolor the "row"
   return(item.Color(text_color) && item.ColorBackground(back_color) && item.ColorBorder(back_color));
  }
//+------------------------------------------------------------------+
//| Check visibility of selected row                                 |
//+------------------------------------------------------------------+
bool CListView::CheckView(void)
  {
//--- check visibility
   if(m_current>=m_offset && m_current<m_offset+m_total_view)
      return(true);
//--- selected row is not visible
   int total=m_strings.Total();
   m_offset=(total-m_current>m_total_view) ? m_current : total-m_total_view;
//--- adjust the scrollbar
   m_scroll_v.CurrPos(m_offset);
//--- redraw
   return(Redraw());
  }
//+------------------------------------------------------------------+
//| Handler of resizing                                              |
//+------------------------------------------------------------------+
bool CListView::OnResize(void)
  {
//--- call of the method of the parent class
   if(!CWndClient::OnResize())
      return(false);
//--- set up the size of "row"
   if(VScrolled())
      OnVScrollShow();
   else
      OnVScrollHide();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "Show vertical scrollbar" event                   |
//+------------------------------------------------------------------+
bool CListView::OnVScrollShow(void)
  {
//--- loop by "rows"
   for(int i=0;i<m_total_view;i++)
     {
      //--- resize "rows" according to shown vertical scrollbar
      m_rows[i].Width(Width()-(CONTROLS_SCROLL_SIZE+CONTROLS_BORDER_WIDTH));
     }
//--- check visibility
   if(!IS_VISIBLE)
     {
      m_scroll_v.Visible(false);
      return(true);
     }
//--- event is handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "Hide vertical scrollbar" event                   |
//+------------------------------------------------------------------+
bool CListView::OnVScrollHide(void)
  {
//--- check visibility
   if(!IS_VISIBLE)
      return(true);
//--- loop by "rows"
   for(int i=0;i<m_total_view;i++)
     {
      //--- resize "rows" according to hidden vertical scroll bar
      m_rows[i].Width(Width()-2*CONTROLS_BORDER_WIDTH);
     }
//--- event is handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "Scroll up for one row" event                     |
//+------------------------------------------------------------------+
bool CListView::OnScrollLineUp(void)
  {
//--- get new offset
   m_offset=m_scroll_v.CurrPos();
//--- redraw
   return(Redraw());
  }
//+------------------------------------------------------------------+
//| Handler of the "Scroll down for one row" event                   |
//+------------------------------------------------------------------+
bool CListView::OnScrollLineDown(void)
  {
//--- get new offset
   m_offset=m_scroll_v.CurrPos();
//--- redraw
   return(Redraw());
  }
//+------------------------------------------------------------------+
//| Handler of click on row                                          |
//+------------------------------------------------------------------+
bool CListView::OnItemClick(const int index)
  {
//--- select "row"
   Select(index+m_offset);
//--- send notification
   EventChartCustom(CONTROLS_SELF_MESSAGE,ON_CHANGE,m_id,0.0,m_name);
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
