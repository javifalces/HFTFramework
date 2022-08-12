//+------------------------------------------------------------------+
//|                                          ChartObjectsElliott.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//| All Elliott tools.                                               |
//+------------------------------------------------------------------+
#include "ChartObject.mqh"
//+------------------------------------------------------------------+
//| Class CChartObjectElliottWave3.                                  |
//| Purpose: Class of the "ElliottCorrectiveWave" object of chart.   |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectElliottWave3 : public CChartObject
  {
public:
                     CChartObjectElliottWave3(void);
                    ~CChartObjectElliottWave3(void);
   //--- methods of access to properties of the object
   ENUM_ELLIOT_WAVE_DEGREE Degree(void) const;
   bool              Degree(const ENUM_ELLIOT_WAVE_DEGREE degree) const;
   bool              Lines(void) const;
   bool              Lines(const bool lines) const;
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2,
                            const datetime time3,const double price3);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_ELLIOTWAVE3); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectElliottWave3::CChartObjectElliottWave3(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectElliottWave3::~CChartObjectElliottWave3(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "ElliottCorrectiveWave"                            |
//+------------------------------------------------------------------+
bool CChartObjectElliottWave3::Create(long chart_id,const string name,const int window,
                                      const datetime time1,const double price1,
                                      const datetime time2,const double price2,
                                      const datetime time3,const double price3)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ELLIOTWAVE3,window,time1,price1,time2,price2,time3,price3))
      return(false);
   if(!Attach(chart_id,name,window,3))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get value of the "Degree" property                               |
//+------------------------------------------------------------------+
ENUM_ELLIOT_WAVE_DEGREE CChartObjectElliottWave3::Degree(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(WRONG_VALUE);
//--- result
   return((ENUM_ELLIOT_WAVE_DEGREE)ObjectGetInteger(m_chart_id,m_name,OBJPROP_DEGREE));
  }
//+------------------------------------------------------------------+
//| Set value for the "Degree" property                              |
//+------------------------------------------------------------------+
bool CChartObjectElliottWave3::Degree(const ENUM_ELLIOT_WAVE_DEGREE degree) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_DEGREE,degree));
  }
//+------------------------------------------------------------------+
//| Get value of the "Lines" property                                |
//+------------------------------------------------------------------+
bool CChartObjectElliottWave3::Lines(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_DRAWLINES));
  }
//+------------------------------------------------------------------+
//| Set value for the "Lines" property                               |
//+------------------------------------------------------------------+
bool CChartObjectElliottWave3::Lines(const bool lines) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_DRAWLINES,lines));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectElliottWave3::Save(const int file_handle)
  {
   bool result;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   result=CChartObject::Save(file_handle);
   if(result)
     {
      //--- write value of the "Degree" property
      if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_DEGREE),INT_VALUE)!=sizeof(int))
         return(false);
      //--- write value of the "Lines" property
      if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_DRAWLINES),INT_VALUE)!=sizeof(int))
         return(false);
     }
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectElliottWave3::Load(const int file_handle)
  {
   bool result;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   result=CChartObject::Load(file_handle);
   if(result)
     {
      //--- read value of the "Degree" property
      if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_DEGREE,FileReadInteger(file_handle,INT_VALUE)))
         return(false);
      //--- read value of the "Lines" property
      if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_DRAWLINES,FileReadInteger(file_handle,INT_VALUE)))
         return(false);
     }
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectElliottWave5.                                  |
//| Purpose: Class of the "ElliottMotiveWave" object of chart.       |
//|          Derives from class CChartObjectElliottWave3.            |
//+------------------------------------------------------------------+
class CChartObjectElliottWave5 : public CChartObjectElliottWave3
  {
public:
                     CChartObjectElliottWave5(void);
                    ~CChartObjectElliottWave5(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,
                            const datetime time1,const double price1,
                            const datetime time2,const double price2,
                            const datetime time3,const double price3,
                            const datetime time4,const double price4,
                            const datetime time5,const double price5);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_ELLIOTWAVE5); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectElliottWave5::CChartObjectElliottWave5(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectElliottWave5::~CChartObjectElliottWave5(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "ElliottMotiveWave"                                |
//+------------------------------------------------------------------+
bool CChartObjectElliottWave5::Create(long chart_id,const string name,const int window,
                                      const datetime time1,const double price1,
                                      const datetime time2,const double price2,
                                      const datetime time3,const double price3,
                                      const datetime time4,const double price4,
                                      const datetime time5,const double price5)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ELLIOTWAVE5,window,time1,price1,time2,price2,time3,price3,time4,price4,time5,price5))
      return(false);
   if(!Attach(chart_id,name,window,5))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
