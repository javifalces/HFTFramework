//+------------------------------------------------------------------+
//|                                                  ChartObject.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
//+------------------------------------------------------------------+
//| Class CChartObject.                                              |
//| Pupose: Base class of chart objects.                             |
//|              Derives from class CObject.                         |
//+------------------------------------------------------------------+
class CChartObject : public CObject
  {
protected:
   long              m_chart_id;           // identifier of chart the object belongs to
   int               m_window;             // number of subwindow (0 - main window)
   string            m_name;               // unique name object name
   int               m_num_points;         // number of anchor points of object

public:
                     CChartObject(void);
                    ~CChartObject(void);
   //--- method of identifying the object
   virtual int       Type(void) const { return(0x8888); }
   //--- methods of access to protected data
   long              ChartId(void) const { return(m_chart_id); }
   int               Window(void) const { return(m_window); }
   string            Name(void) const { return(m_name); }
   bool              Name(const string name);
   int               NumPoints(void) const { return(m_num_points); }
   //--- methods of filling the object
   bool              Attach(long chart_id,const string name,const int window,const int points);
   bool              SetPoint(const int point,const datetime time,const double price) const;
   //--- methods of deleting
   bool              Delete(void);
   void              Detach(void);
   //--- methods of access to properties of the object
   datetime          Time(const int point) const;
   bool              Time(const int point,const datetime time) const;
   double            Price(const int point) const;
   bool              Price(const int point,const double price) const;
   color             Color(void) const;
   bool              Color(const color new_color) const;
   ENUM_LINE_STYLE   Style(void) const;
   bool              Style(const ENUM_LINE_STYLE new_style) const;
   int               Width(void) const;
   bool              Width(const int new_width) const;
   bool              Background(void) const;
   bool              Background(const bool new_back) const;
   bool              Fill(void) const;
   bool              Fill(const bool new_fill) const;
   long              Z_Order(void) const;
   bool              Z_Order(const long value) const;
   bool              Selected(void) const;
   bool              Selected(const bool new_sel) const;
   bool              Selectable(void) const;
   bool              Selectable(const bool new_sel) const;
   string            Description(void) const;
   bool              Description(const string new_text) const;
   string            Tooltip(void) const;
   bool              Tooltip(const string new_text) const;
   int               Timeframes(void) const;
   virtual bool      Timeframes(const int timeframes) const;
   datetime          CreateTime(void) const;
   int               LevelsCount(void) const;
   bool              LevelsCount(const int new_count) const;
   //--- methods to access the properties of levels of objects
   color             LevelColor(const int level) const;
   bool              LevelColor(const int level,const color new_color) const;
   ENUM_LINE_STYLE   LevelStyle(const int level) const;
   bool              LevelStyle(const int level,const ENUM_LINE_STYLE new_style) const;
   int               LevelWidth(const int level) const;
   bool              LevelWidth(const int level,const int new_width) const;
   double            LevelValue(const int level) const;
   bool              LevelValue(const int level,const double new_value) const;
   string            LevelDescription(const int level) const;
   bool              LevelDescription(const int level,const string new_text) const;
   //--- access methods to the API functions of MQL5
   long              GetInteger(const ENUM_OBJECT_PROPERTY_INTEGER prop_id,const int modifier=-1) const;
   bool              GetInteger(const ENUM_OBJECT_PROPERTY_INTEGER prop_id,const int modifier,long &value) const;
   bool              SetInteger(const ENUM_OBJECT_PROPERTY_INTEGER prop_id,const int modifier,const long value) const;
   bool              SetInteger(const ENUM_OBJECT_PROPERTY_INTEGER prop_id,const long value) const;
   double            GetDouble(const ENUM_OBJECT_PROPERTY_DOUBLE prop_id,const int modifier=-1) const;
   bool              GetDouble(const ENUM_OBJECT_PROPERTY_DOUBLE prop_id,const int modifier,double &value) const;
   bool              SetDouble(const ENUM_OBJECT_PROPERTY_DOUBLE prop_id,const int modifier,const double value) const;
   bool              SetDouble(const ENUM_OBJECT_PROPERTY_DOUBLE prop_id,const double value) const;
   string            GetString(const ENUM_OBJECT_PROPERTY_STRING prop_id,const int modifier=-1) const;
   bool              GetString(const ENUM_OBJECT_PROPERTY_STRING prop_id,const int modifier,string &value) const;
   bool              SetString(const ENUM_OBJECT_PROPERTY_STRING prop_id,const int modifier,const string value) const;
   bool              SetString(const ENUM_OBJECT_PROPERTY_STRING prop_id,const string value) const;
   //--- methods of moving
   bool              ShiftObject(const datetime d_time,const double d_price) const;
   bool              ShiftPoint(const int point,const datetime d_time,const double d_price) const;
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObject::CChartObject(void)
  {
//--- initialize protected data
   Detach();
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObject::~CChartObject(void)
  {
   if(m_chart_id!=-1)
      ObjectDelete(m_chart_id,m_name);
  }
//+------------------------------------------------------------------+
//| Changing name of the object                                      |
//+------------------------------------------------------------------+
bool CChartObject::Name(const string name)
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- change
   if(ObjectSetString(m_chart_id,m_name,OBJPROP_NAME,name))
     {
      m_name=name;
      return(true);
     }
//--- failure
   return(false);
  };
//+------------------------------------------------------------------+
//| Attach object                                                    |
//+------------------------------------------------------------------+
bool CChartObject::Attach(long chart_id,const string name,const int window,const int points)
  {
//--- check
   if(ObjectFind(chart_id,name)<0)
      return(false);
//--- attach
   if(chart_id==0)
      chart_id=ChartID();
   m_chart_id  =chart_id;
   m_window    =window;
   m_name      =name;
   m_num_points=points;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Setting new coordinates of anchor point of an object             |
//+------------------------------------------------------------------+
bool CChartObject::SetPoint(const int point,const datetime time,const double price) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(point>=m_num_points)
      return(false);
//--- result
   return(ObjectMove(m_chart_id,m_name,point,time,price));
  }
//+------------------------------------------------------------------+
//| Delete an object                                                 |
//+------------------------------------------------------------------+
bool CChartObject::Delete(void)
  {
//--- checki
   if(m_chart_id==-1)
      return(false);
//--- actions
   bool result=ObjectDelete(m_chart_id,m_name);
   Detach();
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
//| Detach object                                                    |
//+------------------------------------------------------------------+
void CChartObject::Detach(void)
  {
   m_chart_id  =-1;
   m_window    =-1;
   m_name      =NULL;
   m_num_points=0;
  }
//+------------------------------------------------------------------+
//| Get the time coordinate of the specified anchor point of object  |
//+------------------------------------------------------------------+
datetime CChartObject::Time(const int point) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
   if(point>=m_num_points)
      return(0);
//--- result
   return((datetime)ObjectGetInteger(m_chart_id,m_name,OBJPROP_TIME,point));
  }
//+------------------------------------------------------------------+
//| Set the time coordinate of the specified anchor point of object  |
//+------------------------------------------------------------------+
bool CChartObject::Time(const int point,const datetime time) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(point>=m_num_points)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_TIME,point,time));
  }
//+------------------------------------------------------------------+
//| Get the price coordinate of the specified anchor point of object.|
//+------------------------------------------------------------------+
double CChartObject::Price(const int point) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
   if(point>=m_num_points)
      return(EMPTY_VALUE);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_PRICE,point));
  }
//+------------------------------------------------------------------+
//| Set the price coordinate of the specified anchor point of object.|
//+------------------------------------------------------------------+
bool CChartObject::Price(const int point,const double price) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(point>=m_num_points)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_PRICE,point,price));
  }
//+------------------------------------------------------------------+
//| Get object color                                                 |
//+------------------------------------------------------------------+
color CChartObject::Color(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ObjectGetInteger(m_chart_id,m_name,OBJPROP_COLOR));
  }
//+------------------------------------------------------------------+
//| Set object color                                                 |
//+------------------------------------------------------------------+
bool CChartObject::Color(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_COLOR,new_color));
  }
//+------------------------------------------------------------------+
//| Get style of line of object                                      |
//+------------------------------------------------------------------+
ENUM_LINE_STYLE CChartObject::Style(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(WRONG_VALUE);
//--- result
   return((ENUM_LINE_STYLE)ObjectGetInteger(m_chart_id,m_name,OBJPROP_STYLE));
  }
//+------------------------------------------------------------------+
//| Set style of line of object                                      |
//+------------------------------------------------------------------+
bool CChartObject::Style(const ENUM_LINE_STYLE new_style) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_STYLE,new_style));
  }
//+------------------------------------------------------------------+
//| Get width of line of object                                      |
//+------------------------------------------------------------------+
int CChartObject::Width(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(-1);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_WIDTH));
  }
//+------------------------------------------------------------------+
//| Set width of line of object                                      |
//+------------------------------------------------------------------+
bool CChartObject::Width(const int new_width) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_WIDTH,new_width));
  }
//+------------------------------------------------------------------+
//| Get the "Draw object as background" flag                         |
//+------------------------------------------------------------------+
bool CChartObject::Background(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_BACK));
  }
//+------------------------------------------------------------------+
//| Set the "Draw object as background" flag                         |
//+------------------------------------------------------------------+
bool CChartObject::Background(const bool new_back) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_BACK,new_back));
  }
//+------------------------------------------------------------------+
//| Get the "Filling" flag                                           |
//+------------------------------------------------------------------+
bool CChartObject::Fill(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_FILL));
  }
//+------------------------------------------------------------------+
//| Set the "Filling" flag                                           |
//+------------------------------------------------------------------+
bool CChartObject::Fill(const bool new_fill) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_FILL,new_fill));
  }
//+------------------------------------------------------------------+
//| Get the "Z-order" property                                       |
//+------------------------------------------------------------------+
long CChartObject::Z_Order(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return(ObjectGetInteger(m_chart_id,m_name,OBJPROP_ZORDER));
  }
//+------------------------------------------------------------------+
//| Set the "Z-order" property                                       |
//+------------------------------------------------------------------+
bool CChartObject::Z_Order(const long value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_ZORDER,value));
  }
//+------------------------------------------------------------------+
//| Get the "selected" flag                                          |
//+------------------------------------------------------------------+
bool CChartObject::Selected(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_SELECTED));
  }
//+------------------------------------------------------------------+
//| Set the "selected" flag                                          |
//+------------------------------------------------------------------+
bool CChartObject::Selected(const bool new_sel) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_SELECTED,new_sel));
  }
//+------------------------------------------------------------------+
//| Get the "selectable" flag                                        |
//+------------------------------------------------------------------+
bool CChartObject::Selectable(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_SELECTABLE));
  }
//+------------------------------------------------------------------+
//| Set flag the "selectable" flag                                   |
//+------------------------------------------------------------------+
bool CChartObject::Selectable(const bool new_sel) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_SELECTABLE,new_sel));
  }
//+------------------------------------------------------------------+
//| Get comment of object                                            |
//+------------------------------------------------------------------+
string CChartObject::Description(void) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//--- result
   return(ObjectGetString(m_chart_id,m_name,OBJPROP_TEXT));
  }
//+------------------------------------------------------------------+
//| Set comment of object                                            |
//+------------------------------------------------------------------+
bool CChartObject::Description(const string new_text) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- tune
   if(new_text=="")
      return(ObjectSetString(m_chart_id,m_name,OBJPROP_TEXT," "));
//--- result
   return(ObjectSetString(m_chart_id,m_name,OBJPROP_TEXT,new_text));
  }
//+------------------------------------------------------------------+
//| Get tooltip of object                                            |
//+------------------------------------------------------------------+
string CChartObject::Tooltip(void) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//--- result
   return(ObjectGetString(m_chart_id,m_name,OBJPROP_TOOLTIP));
  }
//+------------------------------------------------------------------+
//| Set tooltip of object                                            |
//+------------------------------------------------------------------+
bool CChartObject::Tooltip(const string new_text) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- tune
   if(new_text=="")
      return(ObjectSetString(m_chart_id,m_name,OBJPROP_TOOLTIP," "));
//--- result
   return(ObjectSetString(m_chart_id,m_name,OBJPROP_TOOLTIP,new_text));
  }
//+------------------------------------------------------------------+
//| Get the "Timeframes" (visibility) flag                           |
//+------------------------------------------------------------------+
int CChartObject::Timeframes(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_TIMEFRAMES));
  }
//+------------------------------------------------------------------+
//| Set the "Timeframes" (visibility) flag                           |
//+------------------------------------------------------------------+
bool CChartObject::Timeframes(const int timeframes) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_TIMEFRAMES,timeframes));
  }
//+------------------------------------------------------------------+
//| Get time of object creation                                      |
//+------------------------------------------------------------------+
datetime CChartObject::CreateTime(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((datetime)ObjectGetInteger(m_chart_id,m_name,OBJPROP_CREATETIME));
  }
//+------------------------------------------------------------------+
//| Get number of levels of object                                   |
//+------------------------------------------------------------------+
int CChartObject::LevelsCount(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_LEVELS));
  }
//+------------------------------------------------------------------+
//| Set number of levels of object                                   |
//+------------------------------------------------------------------+
bool CChartObject::LevelsCount(const int new_count) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_LEVELS,new_count));
  }
//+------------------------------------------------------------------+
//| Get color of the specified level of object                       |
//+------------------------------------------------------------------+
color CChartObject::LevelColor(const int level) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
   if(level>=LevelsCount())
      return(CLR_NONE);
//--- result
   return((color)ObjectGetInteger(m_chart_id,m_name,OBJPROP_LEVELCOLOR,level));
  }
//+------------------------------------------------------------------+
//| Set color of the specified level of object                       |
//+------------------------------------------------------------------+
bool CChartObject::LevelColor(const int level,const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(level>=LevelsCount())
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_LEVELCOLOR,level,new_color));
  }
//+------------------------------------------------------------------+
//| Get line style of the specified level of object                  |
//+------------------------------------------------------------------+
ENUM_LINE_STYLE CChartObject::LevelStyle(const int level) const
  {
//--- check
   if(m_chart_id==-1)
      return(WRONG_VALUE);
   if(level>=LevelsCount())
      return(WRONG_VALUE);
//--- result
   return((ENUM_LINE_STYLE)ObjectGetInteger(m_chart_id,m_name,OBJPROP_LEVELSTYLE,level));
  }
//+------------------------------------------------------------------+
//| Set line style of the specified level of object                  |
//+------------------------------------------------------------------+
bool CChartObject::LevelStyle(const int level,const ENUM_LINE_STYLE new_style) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(level>=LevelsCount())
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_LEVELSTYLE,level,new_style));
  }
//+------------------------------------------------------------------+
//| Get line width of the specified level of object                  |
//+------------------------------------------------------------------+
int CChartObject::LevelWidth(const int level) const
  {
//--- check
   if(m_chart_id==-1)
      return(-1);
   if(level>=LevelsCount())
      return(-1);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_LEVELWIDTH,level));
  }
//+------------------------------------------------------------------+
//| Set line width of the specified level of object                  |
//+------------------------------------------------------------------+
bool CChartObject::LevelWidth(const int level,const int new_width) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(level>=LevelsCount())
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_LEVELWIDTH,level,new_width));
  }
//+------------------------------------------------------------------+
//| Get value of the specified level of object                       |
//+------------------------------------------------------------------+
double CChartObject::LevelValue(const int level) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
   if(level>=LevelsCount())
      return(EMPTY_VALUE);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_LEVELVALUE,level));
  }
//+------------------------------------------------------------------+
//| Set value of the specified level of object                       |
//+------------------------------------------------------------------+
bool CChartObject::LevelValue(const int level,const double new_value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(level>=LevelsCount())
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_LEVELVALUE,level,new_value));
  }
//+------------------------------------------------------------------+
//| Get comment of of the specified level of object                  |
//+------------------------------------------------------------------+
string CChartObject::LevelDescription(const int level) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
   if(level>=LevelsCount())
      return("");
//--- result
   return(ObjectGetString(m_chart_id,m_name,OBJPROP_LEVELTEXT,level));
  }
//+------------------------------------------------------------------+
//| Set comment to the specified level of object                     |
//+------------------------------------------------------------------+
bool CChartObject::LevelDescription(const int level,const string new_text) const
  {
//--- checking
   if(m_chart_id==-1)
      return(false);
   if(level>=LevelsCount())
      return(false);
//--- result
   return(ObjectSetString(m_chart_id,m_name,OBJPROP_LEVELTEXT,level,new_text));
  }
//+------------------------------------------------------------------+
//| Access function long ObjectGetInteger(...)                       |
//+------------------------------------------------------------------+
long CChartObject::GetInteger(const ENUM_OBJECT_PROPERTY_INTEGER prop_id,const int modifier) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//---
   if(modifier==-1)
      return(ObjectGetInteger(m_chart_id,m_name,prop_id));
//--- result
   return(ObjectGetInteger(m_chart_id,m_name,prop_id,modifier));
  }
//+------------------------------------------------------------------+
//| Access function bool ObjectGetInteger(...)                       |
//+------------------------------------------------------------------+
bool CChartObject::GetInteger(const ENUM_OBJECT_PROPERTY_INTEGER prop_id,const int modifier,long &value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectGetInteger(m_chart_id,m_name,prop_id,modifier,value));
  }
//+------------------------------------------------------------------+
//| Access function ObjectSetInteger(.,modifier,.)                   |
//+------------------------------------------------------------------+
bool CChartObject::SetInteger(const ENUM_OBJECT_PROPERTY_INTEGER prop_id,const int modifier,const long value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,prop_id,modifier,value));
  }
//+------------------------------------------------------------------+
//| Access function ObjectSetInteger(...)                            |
//+------------------------------------------------------------------+
bool CChartObject::SetInteger(const ENUM_OBJECT_PROPERTY_INTEGER prop_id,const long value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,prop_id,value));
  }
//+------------------------------------------------------------------+
//| Access function double ObjectGetDouble(...)                      |
//+------------------------------------------------------------------+
double CChartObject::GetDouble(const ENUM_OBJECT_PROPERTY_DOUBLE prop_id,const int modifier) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//---
   if(modifier==-1)
      return(ObjectGetDouble(m_chart_id,m_name,prop_id));
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,prop_id,modifier));
  }
//+------------------------------------------------------------------+
//| Access function bool ObjectGetDouble(...)                        |
//+------------------------------------------------------------------+
bool CChartObject::GetDouble(const ENUM_OBJECT_PROPERTY_DOUBLE prop_id,const int modifier,double &value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,prop_id,modifier,value));
  }
//+------------------------------------------------------------------+
//| Access function ObjectSetDouble(.,modifier,.)                    |
//+------------------------------------------------------------------+
bool CChartObject::SetDouble(const ENUM_OBJECT_PROPERTY_DOUBLE prop_id,const int modifier,const double value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,prop_id,modifier,value));
  }
//+------------------------------------------------------------------+
//| Access function ObjectSetDouble(...)                             |
//+------------------------------------------------------------------+
bool CChartObject::SetDouble(const ENUM_OBJECT_PROPERTY_DOUBLE prop_id,const double value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,prop_id,value));
  }
//+------------------------------------------------------------------+
//| Access function string ObjectGetString (...)                     |
//+------------------------------------------------------------------+
string CChartObject::GetString(const ENUM_OBJECT_PROPERTY_STRING prop_id,const int modifier) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//---
   if(modifier==-1)
      return(ObjectGetString(m_chart_id,m_name,prop_id));
//--- result
   return(ObjectGetString(m_chart_id,m_name,prop_id,modifier));
  }
//+------------------------------------------------------------------+
//| Access function bool ObjectGetString(...)                        |
//+------------------------------------------------------------------+
bool CChartObject::GetString(const ENUM_OBJECT_PROPERTY_STRING prop_id,const int modifier,string &value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectGetString(m_chart_id,m_name,prop_id,modifier,value));
  }
//+------------------------------------------------------------------+
//| Access function ObjectSetString(.,modifier,.)                    |
//+------------------------------------------------------------------+
bool CChartObject::SetString(const ENUM_OBJECT_PROPERTY_STRING prop_id,const int modifier,const string value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetString(m_chart_id,m_name,prop_id,modifier,value));
  }
//+------------------------------------------------------------------+
//| Access function ObjectSetString(...)                             |
//+------------------------------------------------------------------+
bool CChartObject::SetString(const ENUM_OBJECT_PROPERTY_STRING prop_id,const string value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetString(m_chart_id,m_name,prop_id,value));
  }
//+------------------------------------------------------------------+
//| Relative movement of object                                      |
//+------------------------------------------------------------------+
bool CChartObject::ShiftObject(const datetime d_time,const double d_price) const
  {
   bool result=true;
   int  i;
//--- check
   if(m_chart_id==-1)
      return(false);
//--- move
   for(i=0;i<m_num_points;i++)
      result&=ShiftPoint(i,d_time,d_price);
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
//| Relative movement of the specified achor point of object         |
//+------------------------------------------------------------------+
bool CChartObject::ShiftPoint(const int point,const datetime d_time,const double d_price) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(point>=m_num_points)
      return(false);
//--- move
   datetime time=(datetime)ObjectGetInteger(m_chart_id,m_name,OBJPROP_TIME,point);
   double   price=ObjectGetDouble(m_chart_id,m_name,OBJPROP_PRICE,point);
//--- result
   return(ObjectMove(m_chart_id,m_name,point,time+d_time,price+d_price));
  }
//+------------------------------------------------------------------+
//| Writing object parameters to file                                |
//+------------------------------------------------------------------+
bool CChartObject::Save(const int file_handle)
  {
   int    i,len;
   int    levels;
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write start marker - 0xFFFFFFFFFFFFFFFF
   if(FileWriteLong(file_handle,-1)!=sizeof(long))
      return(false);
//--- write object type
   if(FileWriteInteger(file_handle,Type(),INT_VALUE)!=INT_VALUE)
      return(false);
//--- write object name
   str=ObjectGetString(m_chart_id,m_name,OBJPROP_NAME);
   len=StringLen(str);
   if(FileWriteInteger(file_handle,len,INT_VALUE)!=INT_VALUE)
      return(false);
   if(len!=0) if(FileWriteString(file_handle,str,len)!=len)
      return(false);
//--- write object color
   if(FileWriteLong(file_handle,ObjectGetInteger(m_chart_id,m_name,OBJPROP_COLOR))!=sizeof(long))
      return(false);
//--- write object line style
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_STYLE))!=sizeof(int))
      return(false);
//--- write object line width
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_WIDTH))!=sizeof(int))
      return(false);
//--- write the property value "Background"
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_BACK),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write the property value "Selectable"
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_SELECTABLE),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write the property value "Timeframes"
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_TIMEFRAMES),INT_VALUE)!=sizeof(int))
      return(false);
//--- write comment
   str=ObjectGetString(m_chart_id,m_name,OBJPROP_TEXT);
   len=StringLen(str);
   if(FileWriteInteger(file_handle,len,INT_VALUE)!=INT_VALUE)
      return(false);
   if(len!=0) if(FileWriteString(file_handle,str,len)!=len)
      return(false);
//--- write number of points
   if(FileWriteInteger(file_handle,m_num_points,INT_VALUE)!=INT_VALUE)
      return(false);
//--- write points
   for(i=0;i<m_num_points;i++)
     {
      if(FileWriteLong(file_handle,ObjectGetInteger(m_chart_id,m_name,OBJPROP_TIME,i))!=sizeof(long))
         return(false);
      if(FileWriteDouble(file_handle,ObjectGetDouble(m_chart_id,m_name,OBJPROP_PRICE,i))!=sizeof(double))
         return(false);
     }
//--- write number of levels
   levels=(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_LEVELS);
   if(FileWriteInteger(file_handle,levels,INT_VALUE)!=INT_VALUE)
      return(false);
//--- write levels
   for(i=0;i<levels;i++)
     {
      //--- level color
      if(FileWriteLong(file_handle,ObjectGetInteger(m_chart_id,m_name,OBJPROP_LEVELCOLOR,i))!=sizeof(long))
         return(false);
      //--- level line style
      if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_LEVELSTYLE,i))!=sizeof(int))
         return(false);
      //--- level line width
      if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_LEVELWIDTH,i))!=sizeof(int))
         return(false);
      //--- level value
      if(FileWriteDouble(file_handle,ObjectGetDouble(m_chart_id,m_name,OBJPROP_LEVELVALUE,i))!=sizeof(double))
         return(false);
      //--- level name
      str=ObjectGetString(m_chart_id,m_name,OBJPROP_LEVELTEXT,i);
      len=StringLen(str);
      if(FileWriteInteger(file_handle,len,INT_VALUE)!=INT_VALUE)
         return(false);
      if(len!=0) if(FileWriteString(file_handle,str,len)!=len)
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading object parameters from file                              |
//+------------------------------------------------------------------+
bool CChartObject::Load(const int file_handle)
  {
   int    i,len,num;
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read and check start marker - 0xFFFFFFFFFFFFFFFF
   if(FileReadLong(file_handle)!=-1)
      return(false);
//--- read and check object type
   if(FileReadInteger(file_handle,INT_VALUE)!=Type())
      return(false);
//--- read object name
   len=FileReadInteger(file_handle,INT_VALUE);
   str=(len!=0) ? FileReadString(file_handle,len) : "";
   if(!ObjectSetString(m_chart_id,m_name,OBJPROP_NAME,str))
      return(false);
//--- read object color
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_COLOR,FileReadLong(file_handle)))
      return(false);
//--- read object line style
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_STYLE,FileReadInteger(file_handle)))
      return(false);
//--- read object line style
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_WIDTH,FileReadInteger(file_handle)))
      return(false);
//--- read the property value "Background"
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_BACK,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read the property value "Selectable"
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_SELECTABLE,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read the property value "Timeframes"
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_TIMEFRAMES,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read comment
   len=FileReadInteger(file_handle,INT_VALUE);
   str=(len!=0) ? FileReadString(file_handle,len) : "";
   if(!ObjectSetString(m_chart_id,m_name,OBJPROP_TEXT,str))
      return(false);
//--- read number of point
   num=FileReadInteger(file_handle,INT_VALUE);
//--- read points
   if(num!=0)
     {
      for(i=0;i<num;i++)
        {
         if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_TIME,i,FileReadLong(file_handle)))
            return(false);
         if(!ObjectSetDouble(m_chart_id,m_name,OBJPROP_PRICE,i,FileReadDouble(file_handle)))
            return(false);
        }
     }
//--- read number of levels
   num=FileReadInteger(file_handle,INT_VALUE);
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_LEVELS,0,num))
      return(false);
//--- read levels
   if(num!=0)
     {
      for(i=0;i<num;i++)
        {
         //--- level color
         if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_LEVELCOLOR,i,FileReadLong(file_handle)))
            return(false);
         //--- levelline style
         if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_LEVELSTYLE,i,FileReadInteger(file_handle)))
            return(false);
         //--- level line width
         if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_LEVELWIDTH,i,FileReadInteger(file_handle)))
            return(false);
         //--- level value
         if(!ObjectSetDouble(m_chart_id,m_name,OBJPROP_LEVELVALUE,i,FileReadDouble(file_handle)))
            return(false);
         //--- level name
         len=FileReadInteger(file_handle,INT_VALUE);
         str=(len!=0) ? FileReadString(file_handle,len) : "";
         if(!ObjectSetString(m_chart_id,m_name,OBJPROP_LEVELTEXT,i,str))
            return(false);
        }
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
