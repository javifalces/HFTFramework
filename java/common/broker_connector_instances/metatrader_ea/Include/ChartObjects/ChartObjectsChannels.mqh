//+------------------------------------------------------------------+
//|                                         ChartObjectsChannels.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//| All channels.                                                    |
//+------------------------------------------------------------------+
#include "ChartObjectsLines.mqh"
//+------------------------------------------------------------------+
//| Class CChartObjectChannel.                                       |
//| Purpose: Class of the "Equidistant channel" object of chart.     |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectChannel : public CChartObjectTrend
  {
public:
                     CChartObjectChannel(void);
                    ~CChartObjectChannel(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2,
                            const datetime time3,const double price3);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_CHANNEL); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectChannel::CChartObjectChannel(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectChannel::~CChartObjectChannel(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Equidistant channel"                              |
//+------------------------------------------------------------------+
bool CChartObjectChannel::Create(long chart_id,const string name,const int window,
                                 const datetime time1,const double price1,
                                 const datetime time2,const double price2,
                                 const datetime time3,const double price3)
  {
   if(!ObjectCreate(chart_id,name,OBJ_CHANNEL,window,time1,price1,time2,price2,time3,price3))
      return(false);
   if(!Attach(chart_id,name,window,3))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectStdDevChannel.                                 |
//| Purpose: Class of the "Standrad deviation channel"               |
//|          object of chart.                                        |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectStdDevChannel : public CChartObjectTrend
  {
public:
                     CChartObjectStdDevChannel(void);
                    ~CChartObjectStdDevChannel(void);
   //--- methods of access to properties of the object
   double            Deviations(void) const;
   bool              Deviations(const double deviation) const;
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const datetime time2,const double deviation);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_STDDEVCHANNEL); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectStdDevChannel::CChartObjectStdDevChannel(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectStdDevChannel::~CChartObjectStdDevChannel(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Standard deviation channel"                       |
//+------------------------------------------------------------------+
bool CChartObjectStdDevChannel::Create(long chart_id,const string name,const int window,
                                       const datetime time1,const datetime time2,const double deviation)
  {
   if(!ObjectCreate(chart_id,name,OBJ_STDDEVCHANNEL,window,time1,0.0,time2,0.0))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
   if(!Deviations(deviation))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get value of the "Deviations" property                           |
//+------------------------------------------------------------------+
double CChartObjectStdDevChannel::Deviations(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ObjectGetDouble(m_chart_id,m_name,OBJPROP_DEVIATION));
  }
//+------------------------------------------------------------------+
//| Set value for the "Deviations" property                          |
//+------------------------------------------------------------------+
bool CChartObjectStdDevChannel::Deviations(const double deviation) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetDouble(m_chart_id,m_name,OBJPROP_DEVIATION,deviation));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectStdDevChannel::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObjectTrend::Save(file_handle))
      return(false);
//--- write value of the "Deviations" property
   if(FileWriteDouble(file_handle,ObjectGetDouble(m_chart_id,m_name,OBJPROP_DEVIATION))!=sizeof(double))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectStdDevChannel::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObjectTrend::Load(file_handle))
      return(false);
//--- read value of the "Deviations" property
   if(!ObjectSetDouble(m_chart_id,m_name,OBJPROP_DEVIATION,FileReadDouble(file_handle)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectRegression.                                    |
//| Purpose: Class of the "Regression channel" object of chart.      |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectRegression : public CChartObjectTrend
  {
public:
                     CChartObjectRegression(void);
                    ~CChartObjectRegression(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const datetime time2);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_REGRESSION); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectRegression::CChartObjectRegression(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectRegression::~CChartObjectRegression(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Regression channel"                               |
//+------------------------------------------------------------------+
bool CChartObjectRegression::Create(long chart_id,const string name,const int window,
                                    const datetime time1,const datetime time2)
  {
   if(!ObjectCreate(chart_id,name,OBJ_REGRESSION,window,time1,0.0,time2,0.0))
      return(false);
   if(!Attach(chart_id,name,window,2))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectPitchfork.                                     |
//| Purpose: Class of the "Andrews pitchfork" object of chart        |
//|          Derives from class CChartObjectTrend.                   |
//+------------------------------------------------------------------+
class CChartObjectPitchfork : public CChartObjectTrend
  {
public:
                     CChartObjectPitchfork(void);
                    ~CChartObjectPitchfork(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2,
                            const datetime time3,const double price3);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_CHANNEL); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectPitchfork::CChartObjectPitchfork(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectPitchfork::~CChartObjectPitchfork(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Andrews pitchfork"                                |
//+------------------------------------------------------------------+
bool CChartObjectPitchfork::Create(long chart_id,const string name,const int window,
                                   const datetime time1,const double price1,
                                   const datetime time2,const double price2,
                                   const datetime time3,const double price3)
  {
   if(!ObjectCreate(chart_id,name,OBJ_PITCHFORK,window,time1,price1,time2,price2,time3,price3))
      return(false);
   if(!Attach(chart_id,name,window,3))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
