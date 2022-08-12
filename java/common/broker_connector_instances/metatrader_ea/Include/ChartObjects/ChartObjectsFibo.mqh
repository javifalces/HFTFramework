//+------------------------------------------------------------------+
//|                                             ChartObjectsFibo.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//| All Fibonacci tools.                                             |
//+------------------------------------------------------------------+
#include "ChartObjectsLines.mqh"
//+------------------------------------------------------------------+
//| Class CChartObjectFibo.                                          |
//| Purpose: Class of the "Fibonacci Lines" object.                  |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectFibo : public CChartObjectTrend
  {
public:
                     CChartObjectFibo(void);
                    ~CChartObjectFibo(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_FIBO); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectFibo::CChartObjectFibo(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectFibo::~CChartObjectFibo(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Fibonacci Lines"                                  |
//+------------------------------------------------------------------+
bool CChartObjectFibo::Create(long chart_id,const string name,const int window,
                              const datetime time1,const double price1,
                              const datetime time2,const double price2)
  {
   if(!ObjectCreate(chart_id,name,OBJ_FIBO,window,time1,price1,time2,price2))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectFiboTimes.                                     |
//| Purpose: Class of the "Fibonacci Time Zones" object of chart     |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectFiboTimes : public CChartObject
  {
public:
                     CChartObjectFiboTimes(void);
                    ~CChartObjectFiboTimes(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_FIBOTIMES); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectFiboTimes::CChartObjectFiboTimes(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectFiboTimes::~CChartObjectFiboTimes(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Fibonacci Time Zones"                             |
//+------------------------------------------------------------------+
bool CChartObjectFiboTimes::Create(long chart_id,const string name,const int window,
                                   const datetime time1,const double price1,
                                   const datetime time2,const double price2)
  {
   if(!ObjectCreate(chart_id,name,OBJ_FIBOTIMES,window,time1,price1,time2,price2))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectFiboFan.                                       |
//| Purpose: Class of the "Fibonacci Fan" object of chart.           |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectFiboFan : public CChartObject
  {
public:
                     CChartObjectFiboFan(void);
                    ~CChartObjectFiboFan(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_FIBOFAN); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectFiboFan::CChartObjectFiboFan(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectFiboFan::~CChartObjectFiboFan(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Fibonacci Fan"                                    |
//+------------------------------------------------------------------+
bool CChartObjectFiboFan::Create(long chart_id,const string name,const int window,
                                 const datetime time1,const double price1,
                                 const datetime time2,const double price2)
  {
   if(!ObjectCreate(chart_id,name,OBJ_FIBOFAN,window,time1,price1,time2,price2))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectFiboArc.                                       |
//| Purpose: Class of the "Fibonacci Arcs" object of chart.          |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectFiboArc : public CChartObject
  {
public:
                     CChartObjectFiboArc(void);
                    ~CChartObjectFiboArc(void);
   //--- methods of access to properties of the object
   double            Scale(void) const;
   bool              Scale(const double scale) const;
   bool              Ellipse(void) const;
   bool              Ellipse(const bool ellipse) const;
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2,const double scale);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_FIBOARC); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectFiboArc::CChartObjectFiboArc(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectFiboArc::~CChartObjectFiboArc(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Fibonacci Arcs"                                   |
//+------------------------------------------------------------------+
bool CChartObjectFiboArc::Create(long chart_id,const string name,const int window,
                                 const datetime time1,const double price1,
                                 const datetime time2,const double price2,const double scale)
  {
   if(!ObjectCreate(chart_id,name,OBJ_FIBOARC,window,time1,price1,time2,price2))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
   if(!Scale(scale))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get value of the "Scale" property                                |
//+------------------------------------------------------------------+
double CChartObjectFiboArc::Scale(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_SCALE));
  }
//+------------------------------------------------------------------+
//| Set value for the "Scale" property                               |
//+------------------------------------------------------------------+
bool CChartObjectFiboArc::Scale(const double scale) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_SCALE,scale));
  }
//+------------------------------------------------------------------+
//| Get value of the "Ellipse" property                              |
//+------------------------------------------------------------------+
bool CChartObjectFiboArc::Ellipse(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ELLIPSE));
  }
//+------------------------------------------------------------------+
//| Set value for the "Ellipse" property                             |
//+------------------------------------------------------------------+
bool CChartObjectFiboArc::Ellipse(const bool ellipse) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_ELLIPSE,ellipse));
  }
//+------------------------------------------------------------------+
//| Writing parameter of object to file                              |
//+------------------------------------------------------------------+
bool CChartObjectFiboArc::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObject::Save(file_handle))
      return(false);
//--- write value of the "Scale" property
   if(FileWriteDouble(file_handle,ObjectGetDouble(m_chart_id,m_name,OBJPROP_SCALE))!=sizeof(double))
      return(false);
//--- write value of the "Ellipse" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ELLIPSE),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectFiboArc::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObject::Load(file_handle))
      return(false);
//--- read value of the "Scale" property
   if(!ObjectSetDouble(m_chart_id,m_name,OBJPROP_SCALE,FileReadDouble(file_handle)))
      return(false);
//--- read value of the "Ellipse" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_ELLIPSE,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectFiboChannel.                                   |
//| Purpose: Class of the "Fibonacci Channel" object of chart.       |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectFiboChannel : public CChartObjectTrend
  {
public:
                     CChartObjectFiboChannel(void);
                    ~CChartObjectFiboChannel(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2,
                            const datetime time3,const double price3);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_FIBOCHANNEL); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectFiboChannel::CChartObjectFiboChannel(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectFiboChannel::~CChartObjectFiboChannel(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Fibonacci Channel"                                |
//+------------------------------------------------------------------+
bool CChartObjectFiboChannel::Create(long chart_id,const string name,const int window,
                                     const datetime time1,const double price1,
                                     const datetime time2,const double price2,
                                     const datetime time3,const double price3)
  {
   if(!ObjectCreate(chart_id,name,OBJ_FIBOCHANNEL,window,time1,price1,time2,price2,time3,price3))
      return(false);
   if(!Attach(chart_id,name,window,3))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectFiboExpansion.                                 |
//| Purpose: Class of the "Fibonacci Expansion" object.              |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectFiboExpansion : public CChartObjectTrend
  {
public:
                     CChartObjectFiboExpansion(void);
                    ~CChartObjectFiboExpansion(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2,
                            const datetime time3,const double price3);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_EXPANSION); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectFiboExpansion::CChartObjectFiboExpansion(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectFiboExpansion::~CChartObjectFiboExpansion(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Fibonacci Expansion"                              |
//+------------------------------------------------------------------+
bool CChartObjectFiboExpansion::Create(long chart_id,const string name,const int window,
                                       const datetime time1,const double price1,
                                       const datetime time2,const double price2,
                                       const datetime time3,const double price3)
  {
   if(!ObjectCreate(chart_id,name,OBJ_EXPANSION,window,time1,price1,time2,price2,time3,price3))
      return(false);
   if(!Attach(chart_id,name,window,3))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
