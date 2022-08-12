//+------------------------------------------------------------------+
//|                                                    Indicator.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Series.mqh"
//+------------------------------------------------------------------+
//| Class CIndicatorBuffer.                                          |
//| Purpose: Class for access to data of buffers of                  |
//|          technical indicators.                                   |
//|          Derives from class CDoubleBuffer.                       |
//+------------------------------------------------------------------+
class CIndicatorBuffer : public CDoubleBuffer
  {
protected:
   int               m_offset;           // shift along the time axis (in bars)
   string            m_name;             // name of buffer

public:
                     CIndicatorBuffer(void);
                    ~CIndicatorBuffer(void);
   //--- methods of access to protected data
   int               Offset(void)        const { return(m_offset); }
   void              Offset(const int offset)  { m_offset=offset;  }
   string            Name(void)          const { return(m_name);   }
   void              Name(const string name)   { m_name=name;      }
   //--- methods of access to data
   double            At(const int index) const;
   //--- method of refreshing of data in buffer
   bool              Refresh(const int handle,const int num);
   bool              RefreshCurrent(const int handle,const int num);
   
private:
   virtual bool      Refresh(void) { return(CDoubleBuffer::Refresh()); }
   virtual bool      RefreshCurrent(void) { return(CDoubleBuffer::RefreshCurrent()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CIndicatorBuffer::CIndicatorBuffer(void) : m_offset(0),
                                           m_name("")
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CIndicatorBuffer::~CIndicatorBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Access to data in a specified position                           |
//+------------------------------------------------------------------+
double CIndicatorBuffer::At(const int index) const
  {
   return(CDoubleBuffer::At(index+m_offset));
  }
//+------------------------------------------------------------------+
//| Refreshing of data in buffer                                     |
//+------------------------------------------------------------------+
bool CIndicatorBuffer::Refresh(const int handle,const int num)
  {
//--- check
   if(handle==INVALID_HANDLE)
     {
      SetUserError(ERR_USER_INVALID_HANDLE);
      return(false);
     }
//---
   m_data_total=CopyBuffer(handle,num,-m_offset,m_size,m_data);
//---
   return(m_data_total>0);
  }
//+------------------------------------------------------------------+
//| Refreshing of the data in buffer                                 |
//+------------------------------------------------------------------+
bool CIndicatorBuffer::RefreshCurrent(const int handle,const int num)
  {
   double array[1];
//--- check
   if(handle==INVALID_HANDLE)
     {
      SetUserError(ERR_USER_INVALID_HANDLE);
      return(false);
     }
//---
   if(CopyBuffer(handle,num,-m_offset,1,array)>0 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Class CIndicator.                                                |
//| Purpose: Base class of technical indicators.                     |
//|          Derives from class CSeries.                             |
//+------------------------------------------------------------------+
class CIndicator : public CSeries
  {
protected:
   int               m_handle;           // indicator handle
   string            m_status;           // status of creation
   bool              m_full_release;     // flag
   bool              m_redrawer;         // flag

public:
                     CIndicator(void);
                    ~CIndicator(void);
   //--- methods of access to protected data
   int               Handle(void)                const { return(m_handle);    }
   string            Status(void)                const { return(m_status);    }
   void              FullRelease(const bool flag=true) { m_full_release=flag; }
   void              Redrawer(const bool flag=true)    { m_redrawer=flag;     }
   //--- method for creating
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const ENUM_INDICATOR type,const int num_params,const MqlParam &params[]);
   virtual bool      BufferResize(const int size);
   //--- methods of access to data
   int               BarsCalculated(void) const;
   double            GetData(const int buffer_num,const int index) const;
   int               GetData(const int start_pos,const int count,const int buffer_num,double &buffer[]) const;
   int               GetData(const datetime start_time,const int count,const int buffer_num,double &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,const int buffer_num,double &buffer[]) const;
   //--- methods for find extremum
   int               Minimum(const int buffer_num,const int start,const int count) const;
   double            MinValue(const int buffer_num,const int start,const int count,int &index) const;
   int               Maximum(const int buffer_num,const int start,const int count) const;
   double            MaxValue(const int buffer_num,const int start,const int count,int &index) const;
   //--- method of "freshening" of the data
   virtual void      Refresh(const int flags=OBJ_ALL_PERIODS);
   //--- methods for working with chart
   bool              AddToChart(const long chart,const int subwin);
   bool              DeleteFromChart(const long chart,const int subwin);
   //--- methods of conversion of constants to strings
   string            MethodDescription(const int val) const;
   string            PriceDescription(const int val) const;
   string            VolumeDescription(const int val) const;

protected:
   //--- methods of tuning
   bool              CreateBuffers(const string symbol,const ENUM_TIMEFRAMES period,const int buffers);
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int num_params,const MqlParam &params[]) {return(false);}
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CIndicator::CIndicator(void) : m_handle(INVALID_HANDLE),
                                    m_status(""),
                                    m_full_release(false),
                                    m_redrawer(false)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
void CIndicator::~CIndicator(void)
  {
//--- indicator handle release
   if(m_full_release && m_handle!=INVALID_HANDLE)
     {
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
     }
  }
//+------------------------------------------------------------------+
//| Creation of the indicator with universal parameters              |
//+------------------------------------------------------------------+
bool CIndicator::Create(const string symbol,const ENUM_TIMEFRAMES period,
                        const ENUM_INDICATOR type,const int num_params,const MqlParam &params[])
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=IndicatorCreate(symbol,period,type,num_params,params);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- idicator successfully created
   if(!Initialize(symbol,period,num_params,params))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Returns the amount of calculated indicator data                  |
//+------------------------------------------------------------------+
int CIndicator::BarsCalculated(void) const
  {
   if(m_handle==INVALID_HANDLE)
      return(-1);
//---
   return(::BarsCalculated(m_handle));
  }
//+------------------------------------------------------------------+
//| API access method "Copying an element of indicator buffer        |
//| by specifying number of buffer and position of element"          |        
//+------------------------------------------------------------------+
double CIndicator::GetData(const int buffer_num,const int index) const
  {
   CIndicatorBuffer *buffer=At(buffer_num);
//--- check
   if(buffer==NULL)
     {
      Print(__FUNCTION__,": invalid buffer");
      return(EMPTY_VALUE);
     }
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the buffer of indicator by specifying |
//| a start position and number of elements"                         |
//+------------------------------------------------------------------+
int CIndicator::GetData(const int start_pos,const int count,const int buffer_num,double &buffer[]) const
  {
//--- check
   if(m_handle==INVALID_HANDLE)
     {
      SetUserError(ERR_USER_INVALID_HANDLE);
      return(-1);
     }
   if(buffer_num>=m_buffers_total)
     {
      SetUserError(ERR_USER_INVALID_BUFF_NUM);
      return(-1);
     }
//---
   return(CopyBuffer(m_handle,buffer_num,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the buffer of indicator by specifying |
//| start time and number of elements"                               |
//+------------------------------------------------------------------+
int CIndicator::GetData(const datetime start_time,const int count,const int buffer_num,double &buffer[]) const
  {
//--- check
   if(m_handle==INVALID_HANDLE)
     {
      SetUserError(ERR_USER_INVALID_HANDLE);
      return(-1);
     }
   if(buffer_num>=m_buffers_total)
     {
      SetUserError(ERR_USER_INVALID_BUFF_NUM);
      return(-1);
     }
//---
   return(CopyBuffer(m_handle,buffer_num,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the buffer of indicator by specifying |
//| start and final time                                             |
//+------------------------------------------------------------------+
int CIndicator::GetData(const datetime start_time,const datetime stop_time,const int buffer_num,double &buffer[]) const
  {
//--- check
   if(m_handle==INVALID_HANDLE)
     {
      SetUserError(ERR_USER_INVALID_HANDLE);
      return(-1);
     }
   if(buffer_num>=m_buffers_total)
     {
      SetUserError(ERR_USER_INVALID_BUFF_NUM);
      return(-1);
     }
//---
   return(CopyBuffer(m_handle,buffer_num,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Find minimum of a specified buffer                               |
//+------------------------------------------------------------------+
int CIndicator::Minimum(const int buffer_num,const int start,const int count) const
  {
//--- check
   if(m_handle==INVALID_HANDLE)
     {
      SetUserError(ERR_USER_INVALID_HANDLE);
      return(-1);
     }
   if(buffer_num>=m_buffers_total)
     {
      SetUserError(ERR_USER_INVALID_BUFF_NUM);
      return(-1);
     }
//---
   CIndicatorBuffer *buffer=At(buffer_num);
   if(buffer==NULL)
      return(-1);
//---
   return(buffer.Minimum(start,count));
  }
//+------------------------------------------------------------------+
//| Find minimum of a specified buffer                               |
//+------------------------------------------------------------------+
double CIndicator::MinValue(const int buffer_num,const int start,const int count,int &index) const
  {
   int    idx=Minimum(buffer_num,start,count);
   double res=EMPTY_VALUE;
//--- check
   if(idx!=-1)
     {
      CIndicatorBuffer *buffer=At(buffer_num);
      res=buffer.At(idx);
      index=idx;
     }
//---
   return(res);
  }
//+------------------------------------------------------------------+
//| Find maximum of a specified buffer                               |
//+------------------------------------------------------------------+
int CIndicator::Maximum(const int buffer_num,const int start,const int count) const
  {
//--- check
   if(m_handle==INVALID_HANDLE)
     {
      SetUserError(ERR_USER_INVALID_HANDLE);
      return(-1);
     }
   if(buffer_num>=m_buffers_total)
     {
      SetUserError(ERR_USER_INVALID_BUFF_NUM);
      return(-1);
     }
//---
   CIndicatorBuffer *buffer=At(buffer_num);
   if(buffer==NULL)
      return(-1);
//---
   return(buffer.Maximum(start,count));
  }
//+------------------------------------------------------------------+
//| Find maximum of specified buffer                                 |
//+------------------------------------------------------------------+
double CIndicator::MaxValue(const int buffer_num,const int start,const int count,int &index) const
  {
   int    idx=Maximum(buffer_num,start,count);
   double res=EMPTY_VALUE;
//--- check
   if(idx!=-1)
     {
      CIndicatorBuffer *buffer=At(buffer_num);
      res=buffer.At(idx);
      index=idx;
     }
//---
   return(res);
  }
//+------------------------------------------------------------------+
//| Creating data buffers of indicator                               |
//+------------------------------------------------------------------+
bool CIndicator::CreateBuffers(const string symbol,const ENUM_TIMEFRAMES period,const int buffers)
  {
   bool result=true;
//--- check
   if(m_handle==INVALID_HANDLE)
     {
      SetUserError(ERR_USER_INVALID_HANDLE);
      return(false);
     }
   if(buffers==0)
      return(false);
   if(!Reserve(buffers))
      return(false);
//---
   for(int i=0;i<buffers;i++)
      result&=Add(new CIndicatorBuffer);
//---
   if(result)
      m_buffers_total=buffers;
//---
   return(result);
  }
//+------------------------------------------------------------------+
//| Set size of buffers of indicator                                 |
//+------------------------------------------------------------------+
bool CIndicator::BufferResize(const int size)
  {
   if(size>m_buffer_size && !CSeries::BufferResize(size))
      return(false);
//-- history is avalible
   int total=Total();
   for(int i=0;i<total;i++)
     {
      CIndicatorBuffer *buff=At(i);
      //--- check pointer
      if(buff==NULL)
         return(false);
      buff.Size(size);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Refreshing data of indicator                                     |
//+------------------------------------------------------------------+
void CIndicator::Refresh(const int flags)
  {
   int               i;
   CIndicatorBuffer *buff;
//--- refreshing buffers
   for(i=0;i<Total();i++)
     {
      buff=At(i);
      if(m_redrawer)
        {
         buff.Refresh(m_handle,i);
         continue;
        }
      if(!(flags&m_timeframe_flags))
        {
         if(m_refresh_current)
            buff.RefreshCurrent(m_handle,i);
        }
      else
         buff.Refresh(m_handle,i);
     }
  }
//+------------------------------------------------------------------+
//| Adds indicator to chart                                          |
//+------------------------------------------------------------------+
bool CIndicator::AddToChart(const long chart,const int subwin)
  {
   if(ChartIndicatorAdd(chart,subwin,m_handle))
     {
      m_name=ChartIndicatorName(chart,subwin,ChartIndicatorsTotal(chart,subwin)-1);
      return(true);
     }
//--- failed
   return(false);
  }
//+------------------------------------------------------------------+
//| Deletes indicator from chart                                     |
//+------------------------------------------------------------------+
bool CIndicator::DeleteFromChart(const long chart,const int subwin)
  {
   return(ChartIndicatorDelete(chart,subwin,m_name));
  }
//+------------------------------------------------------------------+
//| Converting value of ENUM_MA_METHOD into string                   |
//+------------------------------------------------------------------+
string CIndicator::MethodDescription(const int val) const
  {
   string str;
//--- array for conversion of ENUM_MA_METHOD to string
   static string _m_str[]={"SMA","EMA","SMMA","LWMA"};
//--- check
   if(val<0)
      return("ERROR");
//---
   if(val<4)
      str=_m_str[val];
   else
   if(val<10)
      str="METHOD_UNKNOWN="+IntegerToString(val);
//---
   return(str);
  }
//+------------------------------------------------------------------+
//| Converting value of ENUM_APPLIED_PRICE into string               |
//+------------------------------------------------------------------+
string CIndicator::PriceDescription(const int val) const
  {
   string str;
//--- array for conversion of ENUM_APPLIED_PRICE to string
   static string _a_str[]={"Close","Open","High","Low","Median","Typical","Weighted"};
//--- check
   if(val<0)
      return("Unknown");
//---
   if(val<7)
      str=_a_str[val];
   else
     {
      if(val<10)
         str="PriceUnknown="+IntegerToString(val);
      else
         str="AppliedHandle="+IntegerToString(val);
     }
//---
   return(str);
  }
//+------------------------------------------------------------------+
//| Converting value of ENUM_APPLIED_VOLUME into string              |
//+------------------------------------------------------------------+
string CIndicator::VolumeDescription(const int val) const
  {
   string str;
//--- array for conversion of ENUM_APPLIED_VOLUME to string
   static string _v_str[]={"None","Tick","Real"};
//--- check
   if(val<0)
      return("Unknown");
//---
   if(val<3)
      str=_v_str[val];
   else
     {
      if(val<10)
         str="VolumeUnknown="+IntegerToString(val);
      else
         str="AppliedHandle="+IntegerToString(val);
     }
//---
   return(str);
  }
//+------------------------------------------------------------------+
