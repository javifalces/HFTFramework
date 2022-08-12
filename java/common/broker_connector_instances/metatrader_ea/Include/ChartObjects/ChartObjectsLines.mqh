//+------------------------------------------------------------------+
//|                                            ChartObjectsLines.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//| All lines.                                                       |
//+------------------------------------------------------------------+
#include "ChartObject.mqh"
//+------------------------------------------------------------------+
//| Class CChartObjectVLine.                                         |
//| Purpose: Class of the "Vertical line" object of chart.           |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectVLine : public CChartObject
  {
public:
                     CChartObjectVLine(void);
                    ~CChartObjectVLine(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_VLINE); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectVLine::CChartObjectVLine(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectVLine::~CChartObjectVLine(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Vertical line"                                    |
//+------------------------------------------------------------------+
bool CChartObjectVLine::Create(long chart_id,const string name,const int window,const datetime time)
  {
   if(!ObjectCreate(chart_id,name,OBJ_VLINE,window,time,0.0))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectHLine.                                         |
//| Purpose: Class of the "Horizontal line" object of chart.         |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectHLine : public CChartObject
  {
public:
                     CChartObjectHLine(void);
                    ~CChartObjectHLine(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_HLINE); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectHLine::CChartObjectHLine(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectHLine::~CChartObjectHLine(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Horizontal line"                                  |
//+------------------------------------------------------------------+
bool CChartObjectHLine::Create(long chart_id,const string name,const int window,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_HLINE,window,0,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectTrend.                                         |
//| Purpose: Class of the "Trendline" object of chart.               |
//|          Derives from class CChartObject.                        |
//| It is the parent class for all objects that have properties      |
//| RAY_LEFT and RAY_RIGHT.                                          |
//+------------------------------------------------------------------+
class CChartObjectTrend : public CChartObject
  {
public:
                     CChartObjectTrend(void);
                    ~CChartObjectTrend(void);
   //--- methods of access to properties of the object
   bool              RayLeft(void) const;
   bool              RayLeft(const bool new_sel) const;
   bool              RayRight(void) const;
   bool              RayRight(const bool new_sel) const;
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_TREND); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectTrend::CChartObjectTrend(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectTrend::~CChartObjectTrend(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Trendline"                                        |
//+------------------------------------------------------------------+
bool CChartObjectTrend::Create(long chart_id,const string name,const int window,
                               const datetime time1,const double price1,
                               const datetime time2,const double price2)
  {
   if(!ObjectCreate(chart_id,name,OBJ_TREND,window,time1,price1,time2,price2))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get the "Ray left" flag                                          |
//+------------------------------------------------------------------+
bool CChartObjectTrend::RayLeft(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_RAY_LEFT));
  }
//+------------------------------------------------------------------+
//| Set the "Ray left" flag                                          |
//+------------------------------------------------------------------+
bool CChartObjectTrend::RayLeft(const bool new_ray) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_RAY_LEFT,new_ray));
  }
//+------------------------------------------------------------------+
//| Get the "Ray right" flag                                         |
//+------------------------------------------------------------------+
bool CChartObjectTrend::RayRight(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_RAY_RIGHT));
  }
//+------------------------------------------------------------------+
//| Set the "Ray right" flag                                         |
//+------------------------------------------------------------------+
bool CChartObjectTrend::RayRight(const bool new_ray) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_RAY_RIGHT,new_ray));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectTrend::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObject::Save(file_handle))
      return(false);
//--- write value of the "Ray left" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_RAY_LEFT),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "Ray right" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_RAY_RIGHT),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectTrend::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObject::Load(file_handle))
      return(false);
//--- read value of the "Ray left" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_RAY_LEFT,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "Ray right" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_RAY_RIGHT,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectTrendByAngle.                                  |
//| Puprose: Class of the "Trendline by angle" object of chart.      |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectTrendByAngle : public CChartObjectTrend
  {
public:
                     CChartObjectTrendByAngle(void);
                    ~CChartObjectTrendByAngle(void);
   //--- methods of access to properties of the object
   double            Angle(void) const;
   bool              Angle(const double angle) const;
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_TRENDBYANGLE); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectTrendByAngle::CChartObjectTrendByAngle(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectTrendByAngle::~CChartObjectTrendByAngle(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Trendline by angle"                               |
//+------------------------------------------------------------------+
bool CChartObjectTrendByAngle::Create(long chart_id,const string name,const int window,
                                      const datetime time1,const double price1,
                                      const datetime time2,const double price2)
  {
   if(!ObjectCreate(chart_id,name,OBJ_TRENDBYANGLE,window,time1,price1,time2,price2))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get the "Angle" property                                         |
//+------------------------------------------------------------------+
double CChartObjectTrendByAngle::Angle(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//---
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_ANGLE));
  }
//+------------------------------------------------------------------+
//| Set the "Angle" property                                         |
//+------------------------------------------------------------------+
bool CChartObjectTrendByAngle::Angle(const double angle) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_ANGLE,angle));
  }
//+------------------------------------------------------------------+
//| Class CChartObjectCycles.                                        |
//| Purpose: Class of the "Cycle lines" object of chart.             |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectCycles : public CChartObject
  {
public:
                     CChartObjectCycles(void);
                    ~CChartObjectCycles(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_CYCLES); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectCycles::CChartObjectCycles(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectCycles::~CChartObjectCycles(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Cycle lines"                                      |
//+------------------------------------------------------------------+
bool CChartObjectCycles::Create(long chart_id,const string name,const int window,
                                const datetime time1,const double price1,
                                const datetime time2,const double price2)
  {
   if(!ObjectCreate(chart_id,name,OBJ_CYCLES,window,time1,price1,time2,price2))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
