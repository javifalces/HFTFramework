//+------------------------------------------------------------------+
//|                                      ChartObjectsTxtControls.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//| All text objects.                                                |
//+------------------------------------------------------------------+
#include "ChartObject.mqh"
//+------------------------------------------------------------------+
//| Class CChartObjectText.                                          |
//| Purpose: Class of the "Text" object of chart.                    |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectText : public CChartObject
  {
public:
                     CChartObjectText(void);
                    ~CChartObjectText(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_TEXT); }
   //--- methods of access to properties of the object
   double            Angle(void) const;
   bool              Angle(const double angle) const;
   string            Font(void) const;
   bool              Font(const string font) const;
   int               FontSize(void) const;
   bool              FontSize(const int size) const;
   ENUM_ANCHOR_POINT Anchor(void) const;
   bool              Anchor(const ENUM_ANCHOR_POINT anchor) const;
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectText::CChartObjectText(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectText::~CChartObjectText(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Text"                                             |
//+------------------------------------------------------------------+
bool CChartObjectText::Create(long chart_id,const string name,const int window,
                              const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_TEXT,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get value of the "Angle" property                                |
//+------------------------------------------------------------------+
double CChartObjectText::Angle(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_ANGLE));
  }
//+------------------------------------------------------------------+
//| Set value of the "Angle" property                                |
//+------------------------------------------------------------------+
bool CChartObjectText::Angle(const double angle) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_ANGLE,angle));
  }
//+------------------------------------------------------------------+
//| Get font name                                                    |
//+------------------------------------------------------------------+
string CChartObjectText::Font(void) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//--- result
   return(ObjectGetString(m_chart_id,m_name,OBJPROP_FONT));
  }
//+------------------------------------------------------------------+
//| Set font name                                                    |
//+------------------------------------------------------------------+
bool CChartObjectText::Font(const string font) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetString(m_chart_id,m_name,OBJPROP_FONT,font));
  }
//+------------------------------------------------------------------+
//| Get font size                                                    |
//+------------------------------------------------------------------+
int CChartObjectText::FontSize(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_FONTSIZE));
  }
//+------------------------------------------------------------------+
//| Set font size                                                    |
//+------------------------------------------------------------------+
bool CChartObjectText::FontSize(const int size) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_FONTSIZE,size));
  }
//+------------------------------------------------------------------+
//| Get anchor point                                                 |
//+------------------------------------------------------------------+
ENUM_ANCHOR_POINT CChartObjectText::Anchor(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(WRONG_VALUE);
//--- result
   return((ENUM_ANCHOR_POINT)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ANCHOR));
  }
//+------------------------------------------------------------------+
//| Set anchor point                                                 |
//+------------------------------------------------------------------+
bool CChartObjectText::Anchor(const ENUM_ANCHOR_POINT anchor) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_ANCHOR,anchor));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectText::Save(const int file_handle)
  {
   int    len;
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObject::Save(file_handle))
      return(false);
//--- write value of the "Angle" property
   if(FileWriteDouble(file_handle,ObjectGetDouble(m_chart_id,m_name,OBJPROP_ANGLE))!=sizeof(double))
      return(false);
//--- write value of the "Font Name" property
   str=ObjectGetString(m_chart_id,m_name,OBJPROP_FONT);
   len=StringLen(str);
   if(FileWriteInteger(file_handle,len,INT_VALUE)!=INT_VALUE)
      return(false);
   if(len!=0 && FileWriteString(file_handle,str,len)!=len)
      return(false);
//--- write value of the "Font Size" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_FONTSIZE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Anchor Point" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ANCHOR),INT_VALUE)!=sizeof(int))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectText::Load(const int file_handle)
  {
   int    len;
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObject::Load(file_handle))
      return(false);
//--- reading value of the "Angle" property
   if(!ObjectSetDouble(m_chart_id,m_name,OBJPROP_ANGLE,0,FileReadDouble(file_handle)))
      return(false);
//--- read value of the "Font Name" property
   len=FileReadInteger(file_handle,INT_VALUE);
   str=(len!=0) ? FileReadString(file_handle,len) : "";
   if(!ObjectSetString(m_chart_id,m_name,OBJPROP_FONT,str))
      return(false);
//--- read value of the "Font Size" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_FONTSIZE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "Anchor Point" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_ANCHOR,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectLabel.                                         |
//| Purpose: Class of the "Label" object of chart.                   |
//|          Derives from class CChartObjectText.                    |
//+------------------------------------------------------------------+
class CChartObjectLabel : public CChartObjectText
  {
public:
                     CChartObjectLabel(void);
                    ~CChartObjectLabel(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const int X,const int Y);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_LABEL); }
   //--- methods of access to properties of the object
   int               X_Distance(void) const;
   bool              X_Distance(const int X) const;
   int               Y_Distance(void) const;
   bool              Y_Distance(const int Y) const;
   int               X_Size(void) const;
   int               Y_Size(void) const;
   
   ENUM_BASE_CORNER  Corner(void) const;
   bool              Corner(const ENUM_BASE_CORNER corner) const;
   //--- change of time/price coordinates is blocked
   datetime          Time(const int point) const { return(CChartObjectText::Time(point)); }
   bool              Time(const int point,const datetime time) const { return(false); }
   double            Price(const int point) const { return(CChartObjectText::Price(point)); }
   bool              Price(const int point,const double price) const { return(false); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectLabel::CChartObjectLabel(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectLabel::~CChartObjectLabel(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Label"                                            |
//+------------------------------------------------------------------+
bool CChartObjectLabel::Create(long chart_id,const string name,const int window,const int X,const int Y)
  {
   if(!ObjectCreate(chart_id,name,OBJ_LABEL,window,0,0.0))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
   if(!Description(name))
      return(false);
   if(!X_Distance(X) || !Y_Distance(Y))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get the X-distance                                               |
//+------------------------------------------------------------------+
int CChartObjectLabel::X_Distance(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XDISTANCE));
  }
//+------------------------------------------------------------------+
//| Set the X-distance                                               |
//+------------------------------------------------------------------+
bool CChartObjectLabel::X_Distance(const int X) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_XDISTANCE,X));
  }
//+------------------------------------------------------------------+
//| Get the Y-distance                                               |
//+------------------------------------------------------------------+
int CChartObjectLabel::Y_Distance(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YDISTANCE));
  }
//+------------------------------------------------------------------+
//| Set the Y-distance                                               |
//+------------------------------------------------------------------+
bool CChartObjectLabel::Y_Distance(const int Y) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_YDISTANCE,Y));
  }
//+------------------------------------------------------------------+
//| Get the X-size                                                   |
//+------------------------------------------------------------------+
int CChartObjectLabel::X_Size(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XSIZE));
  }
//+------------------------------------------------------------------+
//| Get the Y-size                                                   |
//+------------------------------------------------------------------+
int CChartObjectLabel::Y_Size(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YSIZE));
  }
//+------------------------------------------------------------------+
//| Get base corner                                                  |
//+------------------------------------------------------------------+
ENUM_BASE_CORNER CChartObjectLabel::Corner(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(WRONG_VALUE);
//--- result
   return((ENUM_BASE_CORNER)ObjectGetInteger(m_chart_id,m_name,OBJPROP_CORNER));
  }
//+------------------------------------------------------------------+
//| Set base corner                                                  |
//+------------------------------------------------------------------+
bool CChartObjectLabel::Corner(const ENUM_BASE_CORNER corner) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_CORNER,corner));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectLabel::Save(const int file_handle)
  {
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObjectText::Save(file_handle))
      return(false);
//--- write value of the "X-distance" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XDISTANCE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Y-distance" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YDISTANCE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Corner" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_CORNER),INT_VALUE)!=sizeof(int))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectLabel::Load(const int file_handle)
  {
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObjectText::Load(file_handle))
      return(false);
//--- reading value of the "X-distance" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_XDISTANCE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "Y-distance" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_YDISTANCE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "Corner" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_CORNER,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectEdit.                                          |
//| Purpose: Class of the "Edit" object of chart.                    |
//|          Derives from class CChartObjectLabel.                   |
//+------------------------------------------------------------------+
class CChartObjectEdit : public CChartObjectLabel
  {
public:
                     CChartObjectEdit(void);
                    ~CChartObjectEdit(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const int X,const int Y,const int sizeX,const int sizeY);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_EDIT); }
   //--- methods of access to properties of the object
   bool              X_Size(const int X) const;
   int               X_Size(void) const { return(CChartObjectLabel::X_Size()); }
   bool              Y_Size(const int Y) const;
   int               Y_Size(void) const { return(CChartObjectLabel::Y_Size()); }
   color             BackColor(void) const;
   bool              BackColor(const color new_color) const;
   color             BorderColor(void) const;
   bool              BorderColor(const color new_color) const;
   bool              ReadOnly(void) const;
   bool              ReadOnly(const bool flag) const;
   ENUM_ALIGN_MODE   TextAlign(void) const;
   bool              TextAlign(const ENUM_ALIGN_MODE align) const;
   //--- change of angle is blocked
   bool              Angle(const double angle) const { return(false); }
   double            Angle(void) const { return(CChartObjectLabel::Angle()); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectEdit::CChartObjectEdit(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectEdit::~CChartObjectEdit(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Edit"                                             |
//+------------------------------------------------------------------+
bool CChartObjectEdit::Create(long chart_id,const string name,const int window,const int X,const int Y,const int sizeX,const int sizeY)
  {
   if(!ObjectCreate(chart_id,name,(ENUM_OBJECT)Type(),window,0,0,0))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
   if(!X_Distance(X) || !Y_Distance(Y))
      return(false);
   if(!X_Size(sizeX) || !Y_Size(sizeY))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Set X-size                                                       |
//+------------------------------------------------------------------+
bool CChartObjectEdit::X_Size(const int X) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_XSIZE,X));
  }
//+------------------------------------------------------------------+
//| Set Y-size                                                       |
//+------------------------------------------------------------------+
bool CChartObjectEdit::Y_Size(const int Y) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_YSIZE,Y));
  }
//+------------------------------------------------------------------+
//| Get background color                                             |
//+------------------------------------------------------------------+
color CChartObjectEdit::BackColor(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ObjectGetInteger(m_chart_id,m_name,OBJPROP_BGCOLOR));
  }
//+------------------------------------------------------------------+
//| Set background color                                             |
//+------------------------------------------------------------------+
bool CChartObjectEdit::BackColor(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_BGCOLOR,new_color));
  }
//+------------------------------------------------------------------+
//| Get border color                                                 |
//+------------------------------------------------------------------+
color CChartObjectEdit::BorderColor(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ObjectGetInteger(m_chart_id,m_name,OBJPROP_BORDER_COLOR));
  }
//+------------------------------------------------------------------+
//| Set border color                                                 |
//+------------------------------------------------------------------+
bool CChartObjectEdit::BorderColor(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_BORDER_COLOR,new_color));
  }
//+------------------------------------------------------------------+
//| Get the "Read only" property                                     |
//+------------------------------------------------------------------+
bool CChartObjectEdit::ReadOnly(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_READONLY));
  }
//+------------------------------------------------------------------+
//| Set the "Read only" property                                     |
//+------------------------------------------------------------------+
bool CChartObjectEdit::ReadOnly(const bool flag) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_READONLY,flag));
  }
//+------------------------------------------------------------------+
//| Get the "Align" property                                         |
//+------------------------------------------------------------------+
ENUM_ALIGN_MODE CChartObjectEdit::TextAlign(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((ENUM_ALIGN_MODE)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ALIGN));
  }
//+------------------------------------------------------------------+
//| Set the "Align" property                                         |
//+------------------------------------------------------------------+
bool CChartObjectEdit::TextAlign(const ENUM_ALIGN_MODE align) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_ALIGN,align));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectEdit::Save(const int file_handle)
  {
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObjectLabel::Save(file_handle))
      return(false);
//--- write value of the "X-size" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XSIZE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Y-size" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YSIZE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write background color
   if(FileWriteLong(file_handle,ObjectGetInteger(m_chart_id,m_name,OBJPROP_BGCOLOR))!=sizeof(long))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectEdit::Load(const int file_handle)
  {
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObjectLabel::Load(file_handle))
      return(false);
//--- read value of the "X-size" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_XSIZE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "Y-size" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_YSIZE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read background color
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_BGCOLOR,FileReadLong(file_handle)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectButton.                                        |
//| Purpose: Class of the "Button" object of chart.                  |
//|          Derives from class CChartObjectEdit.                    |
//+------------------------------------------------------------------+
class CChartObjectButton : public CChartObjectEdit
  {
public:
                     CChartObjectButton(void);
                    ~CChartObjectButton(void);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_BUTTON); }
   //--- methods of access to properties of the object
   bool              State(void) const;
   bool              State(const bool state) const;
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectButton::CChartObjectButton(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectButton::~CChartObjectButton(void)
  {
  }
//+------------------------------------------------------------------+
//| Get state                                                        |
//+------------------------------------------------------------------+
bool CChartObjectButton::State(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_STATE));
  }
//+------------------------------------------------------------------+
//| Set state                                                        |
//+------------------------------------------------------------------+
bool CChartObjectButton::State(const bool state) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_STATE,state));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectButton::Save(const int file_handle)
  {
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObjectEdit::Save(file_handle))
      return(false);
//--- write state
   if(FileWriteLong(file_handle,ObjectGetInteger(m_chart_id,m_name,OBJPROP_STATE))!=sizeof(long))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectButton::Load(const int file_handle)
  {
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObjectEdit::Load(file_handle))
      return(false);
//--- read state
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_STATE,FileReadLong(file_handle)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectRectLabel.                                     |
//| Purpose: Class of the "Rectangle Label" object of chart.         |
//|          Derives from class CChartObjectLabel.                   |
//+------------------------------------------------------------------+
class CChartObjectRectLabel : public CChartObjectLabel
  {
public:
                     CChartObjectRectLabel(void);
                    ~CChartObjectRectLabel(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const int X,const int Y,const int sizeX,const int sizeY);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_RECTANGLE_LABEL); }
   //--- methods of access to properties of the object
   bool              X_Size(const int X) const;
   int               X_Size(void)const { return(CChartObjectLabel::X_Size()); }
   bool              Y_Size(const int Y) const;
   int               Y_Size(void)const { return(CChartObjectLabel::Y_Size()); }
   color             BackColor(void) const;
   bool              BackColor(const color new_color) const;
   ENUM_BORDER_TYPE  BorderType(void) const;
   bool              BorderType(const ENUM_BORDER_TYPE flag) const;
   //--- change of angle is blocked
   bool              Angle(const double angle) const { return(false); }
   double            Angle(void) const { return(CChartObjectLabel::Angle()); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectRectLabel::CChartObjectRectLabel(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectRectLabel::~CChartObjectRectLabel(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Ractangle Label"                                  |
//+------------------------------------------------------------------+
bool CChartObjectRectLabel::Create(long chart_id,const string name,const int window,const int X,const int Y,const int sizeX,const int sizeY)
  {
   if(!ObjectCreate(chart_id,name,(ENUM_OBJECT)Type(),window,0,0,0))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
   if(!X_Distance(X) || !Y_Distance(Y))
      return(false);
   if(!X_Size(sizeX) || !Y_Size(sizeY))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Set X-size                                                       |
//+------------------------------------------------------------------+
bool CChartObjectRectLabel::X_Size(const int X) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_XSIZE,X));
  }
//+------------------------------------------------------------------+
//| Set Y-size                                                       |
//+------------------------------------------------------------------+
bool CChartObjectRectLabel::Y_Size(const int Y) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_YSIZE,Y));
  }
//+------------------------------------------------------------------+
//| Get background color                                             |
//+------------------------------------------------------------------+
color CChartObjectRectLabel::BackColor(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ObjectGetInteger(m_chart_id,m_name,OBJPROP_BGCOLOR));
  }
//+------------------------------------------------------------------+
//| Set background color                                             |
//+------------------------------------------------------------------+
bool CChartObjectRectLabel::BackColor(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_BGCOLOR,new_color));
  }
//+------------------------------------------------------------------+
//| Get the "Border type" property                                   |
//+------------------------------------------------------------------+
ENUM_BORDER_TYPE CChartObjectRectLabel::BorderType(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((ENUM_BORDER_TYPE)ObjectGetInteger(m_chart_id,m_name,OBJPROP_BORDER_TYPE));
  }
//+------------------------------------------------------------------+
//| Set the "Border type" property                                   |
//+------------------------------------------------------------------+
bool CChartObjectRectLabel::BorderType(const ENUM_BORDER_TYPE type) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_BORDER_TYPE,type));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectRectLabel::Save(const int file_handle)
  {
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObjectLabel::Save(file_handle))
      return(false);
//--- write value of the "X-size" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XSIZE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Y-size" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YSIZE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write background color
   if(FileWriteLong(file_handle,ObjectGetInteger(m_chart_id,m_name,OBJPROP_BGCOLOR))!=sizeof(long))
      return(false);
//--- write value of the "Border type" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_BORDER_TYPE),INT_VALUE)!=sizeof(int))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectRectLabel::Load(const int file_handle)
  {
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObjectLabel::Load(file_handle))
      return(false);
//--- read value of the "X-size" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_XSIZE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "Y-size" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_YSIZE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read background color
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_BGCOLOR,FileReadLong(file_handle)))
      return(false);
//--- read value of the "Border type" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_BORDER_TYPE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
