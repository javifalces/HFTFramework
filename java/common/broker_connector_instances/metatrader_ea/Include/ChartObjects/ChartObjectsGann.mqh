//+------------------------------------------------------------------+
//|                                             ChartObjectsGann.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//| All Gann tools.                                                  |
//+------------------------------------------------------------------+
#include "ChartObjectsLines.mqh"
//+------------------------------------------------------------------+
//| Class CChartObjectGannLine.                                      |
//| Purpose: Class of the "Gann Line" object of chart.               |
//|          Derives from class CChartObjectTrendByAngle.            |
//+------------------------------------------------------------------+
class CChartObjectGannLine : public CChartObjectTrendByAngle
  {
public:
                     CChartObjectGannLine(void);
                    ~CChartObjectGannLine(void);
   //--- methods of access to properties of the object
   double            PipsPerBar(void) const;
   bool              PipsPerBar(const double ppb) const;
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double ppb);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_GANNLINE); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectGannLine::CChartObjectGannLine(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectGannLine::~CChartObjectGannLine(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Gann Line"                                        |
//+------------------------------------------------------------------+
bool CChartObjectGannLine::Create(long chart_id,const string name,const int window,
                                  const datetime time1,const double price1,
                                  const datetime time2,const double ppb)
  {
   if(!ObjectCreate(chart_id,name,OBJ_GANNLINE,window,time1,price1,time2,0.0))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
   if(!PipsPerBar(ppb))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get value of the "PipsPerBar" property                           |
//+------------------------------------------------------------------+
double CChartObjectGannLine::PipsPerBar(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_SCALE));
  }
//+------------------------------------------------------------------+
//| Set value for the "PipsPerBar" property                          |
//+------------------------------------------------------------------+
bool CChartObjectGannLine::PipsPerBar(const double ppb) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_SCALE,ppb));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectGannLine::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObjectTrend::Save(file_handle))
      return(false);
//--- write value of the "PipsPerBar"
   if(FileWriteDouble(file_handle,ObjectGetDouble(m_chart_id,m_name,OBJPROP_SCALE))!=sizeof(double))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectGannLine::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObjectTrend::Load(file_handle))
      return(false);
//--- read value of the "PipsPerBar" property
   if(!ObjectSetDouble(m_chart_id,m_name,OBJPROP_SCALE,FileReadDouble(file_handle)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectGannFan.                                       |
//| Purpose: Class of the "Gann Fan" object of chart.                |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectGannFan : public CChartObjectTrend
  {
public:
                     CChartObjectGannFan(void);
                    ~CChartObjectGannFan(void);
   //--- methods of access to properties of the object
   double            PipsPerBar(void) const;
   bool              PipsPerBar(const double ppb) const;
   bool              Downtrend(void) const;
   bool              Downtrend(const bool downtrend) const;
   //--- method of create the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double ppb);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_GANNFAN); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectGannFan::CChartObjectGannFan(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectGannFan::~CChartObjectGannFan(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Gann Fan"                                         |
//+------------------------------------------------------------------+
bool CChartObjectGannFan::Create(long chart_id,const string name,const int window,
                                 const datetime time1,const double price1,
                                 const datetime time2,const double ppb)
  {
   if(!ObjectCreate(chart_id,name,OBJ_GANNFAN,window,time1,price1,time2,0.0))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
   if(!PipsPerBar(ppb))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get value of the "PipsPerBar" property                           |
//+------------------------------------------------------------------+
double CChartObjectGannFan::PipsPerBar(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_SCALE));
  }
//+------------------------------------------------------------------+
//| Set value for the "PipsPerBar" property                          |
//+------------------------------------------------------------------+
bool CChartObjectGannFan::PipsPerBar(const double ppb) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_SCALE,ppb));
  }
//+------------------------------------------------------------------+
//| Get value of the "Downtrend" property                            |
//+------------------------------------------------------------------+
bool CChartObjectGannFan::Downtrend(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_DIRECTION));
  }
//+------------------------------------------------------------------+
//| Set value for the "Downtrend" property                           |
//+------------------------------------------------------------------+
bool CChartObjectGannFan::Downtrend(const bool downtrend) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_DIRECTION,downtrend));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectGannFan::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObjectTrend::Save(file_handle))
      return(false);
//--- write value of the "PipsPerBar" property
   if(FileWriteDouble(file_handle,ObjectGetDouble(m_chart_id,m_name,OBJPROP_SCALE))!=sizeof(double))
      return(false);
//--- write value of the "Downtrend" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_DIRECTION),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading object parameters from file                              |
//+------------------------------------------------------------------+
bool CChartObjectGannFan::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObjectTrend::Load(file_handle))
      return(false);
//--- read value of the "PipsPerBar"  property
   if(!ObjectSetDouble(m_chart_id,m_name,OBJPROP_SCALE,FileReadDouble(file_handle)))
      return(false);
//--- read value of the "Downtrend"  property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_DIRECTION,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectGannGrid.                                      |
//| Purpose: Class of the "Gann Grid" object of chart.               |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectGannGrid : public CChartObjectTrend
  {
public:
                     CChartObjectGannGrid(void);
                    ~CChartObjectGannGrid(void);
   //--- methods of access to properties of the object
   double            PipsPerBar(void) const;
   bool              PipsPerBar(const double ppb) const;
   bool              Downtrend(void) const;
   bool              Downtrend(const bool downtrend) const;
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double ppb);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_GANNGRID); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectGannGrid::CChartObjectGannGrid(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectGannGrid::~CChartObjectGannGrid(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Gann Grid"                                        |
//+------------------------------------------------------------------+
bool CChartObjectGannGrid::Create(long chart_id,const string name,const int window,
                                  const datetime time1,const double price1,
                                  const datetime time2,const double ppb)
  {
   if(!ObjectCreate(chart_id,name,OBJ_GANNGRID,window,time1,price1,time2,0.0))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
   if(!PipsPerBar(ppb))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get value of the"PipsPerBar" property                            |
//+------------------------------------------------------------------+
double CChartObjectGannGrid::PipsPerBar(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_SCALE));
  }
//+------------------------------------------------------------------+
//| Set value for the "PipsPerBar" property                          |
//+------------------------------------------------------------------+
bool CChartObjectGannGrid::PipsPerBar(const double ppb) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_SCALE,ppb));
  }
//+------------------------------------------------------------------+
//| Get the property value "Downtrend"                               |
//+------------------------------------------------------------------+
bool CChartObjectGannGrid::Downtrend(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_DIRECTION));
  }
//+------------------------------------------------------------------+
//| Set the property value "Downtrend"                               |
//+------------------------------------------------------------------+
bool CChartObjectGannGrid::Downtrend(const bool downtrend) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_DIRECTION,downtrend));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectGannGrid::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObjectTrend::Save(file_handle))
      return(false);
//--- write value of the "PipsPerBar" property
   if(FileWriteDouble(file_handle,ObjectGetDouble(m_chart_id,m_name,OBJPROP_SCALE))!=sizeof(double))
      return(false);
//--- write value of the "Downtrend" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_DIRECTION),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading paprameters of object from file                          |
//+------------------------------------------------------------------+
bool CChartObjectGannGrid::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObjectTrend::Load(file_handle))
      return(false);
//--- read value of the "PipsPerBar" property
   if(!ObjectSetDouble(m_chart_id,m_name,OBJPROP_SCALE,FileReadDouble(file_handle)))
      return(false);
//--- read value of the "Downtrend" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_DIRECTION,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
