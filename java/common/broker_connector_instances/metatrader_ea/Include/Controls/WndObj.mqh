//+------------------------------------------------------------------+
//|                                                       WndObj.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Wnd.mqh"
//+------------------------------------------------------------------+
//| Class CWndObj                                                    |
//| Usage: base class to work with chart objects                     |
//+------------------------------------------------------------------+
class CWndObj : public CWnd
  {
private:
   //--- flags of object
   bool              m_undeletable;         // "object is not deletable" flag
   bool              m_unchangeable;        // "object is not changeable" flag
   bool              m_unmoveable;          // "object is not movable" flag

protected:
   //--- parameters of the chart object
   string            m_text;                // object text
   color             m_color;               // object color
   color             m_color_background;    // object background color
   color             m_color_border;        // object border color
   string            m_font;                // object font
   int               m_font_size;           // object font size
   long              m_zorder;              // Z order

public:
                     CWndObj(void);
                    ~CWndObj(void);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   //--- set up the object
   string            Text(void) const { return(m_text); }
   bool              Text(const string value);
   color             Color(void) const { return(m_color); }
   bool              Color(const color value);
   color             ColorBackground(void) const { return(m_color_background); }
   bool              ColorBackground(const color value);
   color             ColorBorder(void) const { return(m_color_border); }
   bool              ColorBorder(const color value);
   string            Font(void) const { return(m_font); }
   bool              Font(const string value);
   int               FontSize(void) const { return(m_font_size); }
   bool              FontSize(const int value);
   long              ZOrder(void) const { return(m_zorder); }
   bool              ZOrder(const long value);

protected:
   //--- handlers of object events
   virtual bool      OnObjectCreate(void);
   virtual bool      OnObjectChange(void);
   virtual bool      OnObjectDelete(void);
   virtual bool      OnObjectDrag(void);
   //--- handlers of object settings
   virtual bool      OnSetText(void)                  { return(true); }
   virtual bool      OnSetColor(void)                 { return(true); }
   virtual bool      OnSetColorBackground(void)       { return(true); }
   virtual bool      OnSetColorBorder(void)           { return(true); }
   virtual bool      OnSetFont(void)                  { return(true); }
   virtual bool      OnSetFontSize(void)              { return(true); }
   virtual bool      OnSetZOrder(void)                { return(true); }
   //--- internal event handlers
   virtual bool      OnDestroy(void) { return(ObjectDelete(m_chart_id,m_name)); }
   virtual bool      OnChange(void);
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
bool CWndObj::OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam)
  {
   if(m_name==sparam)
     {
      //--- object name and string parameters are equal
      //--- this means that event should be handled
      switch(id)
        {
         case CHARTEVENT_OBJECT_CREATE: return(OnObjectCreate());
         case CHARTEVENT_OBJECT_CHANGE: return(OnObjectChange());
         case CHARTEVENT_OBJECT_DELETE: return(OnObjectDelete());
         case CHARTEVENT_OBJECT_DRAG  : return(OnObjectDrag());
        }
     }
//--- event was not handled
   return(CWnd::OnEvent(id,lparam,dparam,sparam));
  }
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CWndObj::CWndObj(void) : m_color(clrNONE),
                         m_color_background(clrNONE),
                         m_color_border(clrNONE),
                         m_font(CONTROLS_FONT_NAME),
                         m_font_size(CONTROLS_FONT_SIZE),
                         m_zorder(0),
                         m_undeletable(true),
                         m_unchangeable(true),
                         m_unmoveable(true)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CWndObj::~CWndObj(void)
  {
  }
//+------------------------------------------------------------------+
//| Set the "Text" parameter                                         |
//+------------------------------------------------------------------+
bool CWndObj::Text(const string value)
  {
//--- save new value of parameter
   m_text=value;
//--- call virtual event handler
   return(OnSetText());
  }
//+------------------------------------------------------------------+
//| Set the "Color" parameter                                        |
//+------------------------------------------------------------------+
bool CWndObj::Color(const color value)
  {
//--- save new value of parameter
   m_color=value;
//--- call virtual event handler
   return(OnSetColor());
  }
//+------------------------------------------------------------------+
//| Setting the "Background color" parameter                         |
//+------------------------------------------------------------------+
bool CWndObj::ColorBackground(const color value)
  {
//--- save new value of parameter
   m_color_background=value;
//--- call virtual event handler
   return(OnSetColorBackground());
  }
//+------------------------------------------------------------------+
//| Set the "Border color" parameter                                 |
//+------------------------------------------------------------------+
bool CWndObj::ColorBorder(const color value)
  {
//--- save new value of parameter
   m_color_border=value;
//--- call virtual event handler
   return(OnSetColorBorder());
  }
//+------------------------------------------------------------------+
//| Set the "Font" parameter                                         |
//+------------------------------------------------------------------+
bool CWndObj::Font(const string value)
  {
//--- save new value of parameter
   m_font=value;
//--- call virtual event handler
   return(OnSetFont());
  }
//+------------------------------------------------------------------+
//| Set the "Font size" parameter                                    |
//+------------------------------------------------------------------+
bool CWndObj::FontSize(const int value)
  {
//--- save new value of parameter
   m_font_size=value;
//--- call virtual event handler
   return(OnSetFontSize());
  }
//+------------------------------------------------------------------+
//| Set the "Z order" parameter                                      |
//+------------------------------------------------------------------+
bool CWndObj::ZOrder(const long value)
  {
//--- save new value of parameter
   m_zorder=value;
//--- call virtual event handler
   return(OnSetZOrder());
  }
//+------------------------------------------------------------------+
//| Handler of the "Object creation" event                           |
//+------------------------------------------------------------------+
bool CWndObj::OnObjectCreate(void)
  {
//--- event is handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "Object modification" event                       |
//+------------------------------------------------------------------+
bool CWndObj::OnObjectChange(void)
  {
//--- if object is not changeable
   if(m_unchangeable)
     {
      //--- restore position
      if(!OnMove())
         return(false);
      //--- restore size
      if(!OnResize())
         return(false);
      //--- restore settings
      if(!OnChange())
         return(false);
     }
//--- event is handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "Object deletion" event                           |
//+------------------------------------------------------------------+
bool CWndObj::OnObjectDelete(void)
  {
//--- if object is not deletable
   if(m_undeletable)
     {
      //--- restore the object
      if(!OnCreate())
         return(false);
      //--- restore settings
      if(!OnChange())
         return(false);
      //--- restore visibility
      return(IS_VISIBLE ? OnShow():OnHide());
     }
//--- event is handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of the "Object dragging" event                           |
//+------------------------------------------------------------------+
bool CWndObj::OnObjectDrag(void)
  {
//--- if object is not movable
   if(m_unmoveable)
     {
      //--- restore position
      return(OnMove());
     }
//--- event is handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Set up the object                                                |
//+------------------------------------------------------------------+
bool CWndObj::OnChange(void)
  {
//--- set up the chart object according to previously set parameters
   if(!OnSetText())
      return(false);
   if(!OnSetFont())
      return(false);
   if(!OnSetFontSize())
      return(false);
   if(!OnSetColor())
      return(false);
   if(!OnSetColorBackground())
      return(false);
   if(!OnSetColorBorder())
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
