//+------------------------------------------------------------------+
//|                                                   TimeSeries.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Series.mqh"
#include <Arrays\ArrayInt.mqh>
#include <Arrays\ArrayLong.mqh>
//+------------------------------------------------------------------+
//| Class CPriceSeries.                                              |
//| Purpose: Base class of price series.                             |
//|          Derives from class CSeries.                             |
//+------------------------------------------------------------------+
class CPriceSeries : public CSeries
  {
public:
                     CPriceSeries(void);
                    ~CPriceSeries(void);
   //--- method of creation
   virtual bool      BufferResize(const int size);
   //--- methods for searching extremum
   virtual int       MinIndex(const int start,const int count) const;
   virtual double    MinValue(const int start,const int count,int &index) const;
   virtual int       MaxIndex(const int start,const int count) const;
   virtual double    MaxValue(const int start,const int count,int &index) const;
   //--- methods of access to data
   double            GetData(const int index) const;
   //--- method of refreshing of the data
   virtual void      Refresh(const int flags=OBJ_ALL_PERIODS);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CPriceSeries::CPriceSeries(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CPriceSeries::~CPriceSeries(void)
  {
  }
//+------------------------------------------------------------------+
//| Set size of buffer                                               |
//+------------------------------------------------------------------+
bool CPriceSeries::BufferResize(const int size)
  {
   if(size>m_buffer_size && !CSeries::BufferResize(size))
      return(false);
//-- history is avalible
   CDoubleBuffer *buff=At(0);
//--- check pointer
   if(buff==NULL)
      return(false);
//--
   buff.Size(size);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Find minimum of specified buffer                                 |
//+------------------------------------------------------------------+
int CPriceSeries::MinIndex(const int start,const int count) const
  {
   CDoubleBuffer *buff=At(0);
//--- check
   if(buff==NULL)
      return(-1);
//---
   return(buff.Minimum(start,count));
  }
//+------------------------------------------------------------------+
//| Find minimum of specified buffer                                 |
//+------------------------------------------------------------------+
double CPriceSeries::MinValue(const int start,const int count,int &index) const
  {
   int    idx=MinIndex(start,count);
   double res=EMPTY_VALUE;
//--- check
   if(idx!=-1)
     {
      CDoubleBuffer *buff=At(0);
      res=buff.At(idx);
      index=idx;
     }
//---
   return(res);
  }
//+------------------------------------------------------------------+
//| Find maximum of specified buffer                                 |
//+------------------------------------------------------------------+
int CPriceSeries::MaxIndex(const int start,const int count) const
  {
   CDoubleBuffer *buff=At(0);
//--- check
   if(buff==NULL)
      return(-1);
//---
   return(buff.Maximum(start,count));
  }
//+------------------------------------------------------------------+
//| Find maximum of specified buffer                                 |
//+------------------------------------------------------------------+
double CPriceSeries::MaxValue(const int start,const int count,int &index) const
  {
   int    idx=MaxIndex(start,count);
   double res=EMPTY_VALUE;
//--- check
   if(idx!=-1)
     {
      CDoubleBuffer *buff=At(0);
      res=buff.At(idx);
      index=idx;
     }
//---
   return(res);
  }
//+------------------------------------------------------------------+
//| Method to access data                                            |
//+------------------------------------------------------------------+
double CPriceSeries::GetData(const int index) const
  {
   CDoubleBuffer *buff=At(0);
//--- check
   if(buff==NULL)
     {
      Print(__FUNCTION__,": invalid buffer");
      return(EMPTY_VALUE);
     }
//---
   return(buff.At(index));
  }
//+------------------------------------------------------------------+
//| Refreshing of the data                                           |
//+------------------------------------------------------------------+
void CPriceSeries::Refresh(const int flags)
  {
   CDoubleBuffer *buff=At(0);
//--- check
   if(buff==NULL)
      return;
//--- refresh of buffers
   if(!(flags&m_timeframe_flags))
     {
      if(m_refresh_current)
         buff.RefreshCurrent();
     }
   else
      buff.Refresh();
  }
//+------------------------------------------------------------------+
//| Class COpenBuffer.                                               |
//| Purpose: Class of buffer of open price series.                   |
//|          Derives from class CDoubleBuffer.                       |
//+------------------------------------------------------------------+
class COpenBuffer : public CDoubleBuffer
  {
public:
                     COpenBuffer(void);
                    ~COpenBuffer(void);
   //--- method of refreshing of the data buffer
   virtual bool      Refresh(void);
   virtual bool      RefreshCurrent(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
COpenBuffer::COpenBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
COpenBuffer::~COpenBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Refreshing of the data buffer                                    |
//+------------------------------------------------------------------+
bool COpenBuffer::Refresh(void)
  {
   m_data_total=CopyOpen(m_symbol,m_period,0,m_size,m_data);
//---
   return(m_data_total>0);
  }
//+------------------------------------------------------------------+
//| Refreshing of the data buffer                                    |
//+------------------------------------------------------------------+
bool COpenBuffer::RefreshCurrent(void)
  {
   double array[1];
//---
   if(CopyOpen(m_symbol,m_period,0,1,array)==1 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Class CiOpen.                                                    |
//| Purpose: Class of open series.                                   |
//|          Derives from class CPriceSeries.                        |
//+------------------------------------------------------------------+
class CiOpen : public CPriceSeries
  {
public:
                     CiOpen(void);
                    ~CiOpen(void);
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period);
   //--- methods of access to data
   double            GetData(const int index) const { return(CPriceSeries::GetData(index)); }
   int               GetData(const int start_pos,const int count,double &buffer[]) const;
   int               GetData(const datetime start_time,const int count,double &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,double &buffer[]) const;   
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiOpen::CiOpen(void)
  {
   m_name="Open";
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiOpen::~CiOpen(void)
  {
  }
//+------------------------------------------------------------------+
//| Creation of open series                                          |
//+------------------------------------------------------------------+
bool CiOpen::Create(const string symbol,const ENUM_TIMEFRAMES period)
  {
   CDoubleBuffer *buff;
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   if((buff=new COpenBuffer)==NULL)
      return(false);
//--- add
   if(!Add(buff))
     {
      delete buff;
      return(false);
     }
//--- tune
   buff.SetSymbolPeriod(m_symbol,m_period);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| API access method "Copying the open buffer by specifying         |
//| start position and number of elements"                           |
//+------------------------------------------------------------------+
int CiOpen::GetData(const int start_pos,const int count,double &buffer[]) const
  {
   return(CopyOpen(m_symbol,m_period,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the open buffer by specifying         |
//| start time and number of elements"                               |
//+------------------------------------------------------------------+
int CiOpen::GetData(const datetime start_time,const int count,double &buffer[]) const
  {
   return(CopyOpen(m_symbol,m_period,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the open buffer by specifying         |
//| start and end time"                                              |
//+------------------------------------------------------------------+
int CiOpen::GetData(const datetime start_time,const datetime stop_time,double &buffer[]) const
  {
   return(CopyOpen(m_symbol,m_period,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Class CHighBuffer.                                               |
//| Purpose: Class of buffer of high price series.                   |
//|          Derives from class CDoubleBuffer.                       |
//+------------------------------------------------------------------+
class CHighBuffer : public CDoubleBuffer
  {
public:
                     CHighBuffer(void);
                    ~CHighBuffer(void);
   //--- method of refreshing of the data buffer
   virtual bool      Refresh(void);
   virtual bool      RefreshCurrent(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CHighBuffer::CHighBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CHighBuffer::~CHighBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CHighBuffer::Refresh(void)
  {
   m_data_total=CopyHigh(m_symbol,m_period,0,m_size,m_data);
//---
   return(m_data_total>0);
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CHighBuffer::RefreshCurrent(void)
  {
   double array[1];
//---
   if(CopyHigh(m_symbol,m_period,0,1,array)>0 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Class CiHigh.                                                    |
//| Purpose: Class of high series.                                   |
//|          Derives from class CPriceSeries.                        |
//+------------------------------------------------------------------+
class CiHigh : public CPriceSeries
  {
public:
                     CiHigh(void);
                    ~CiHigh(void);
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period);
   //--- methods of access to data
   double            GetData(const int index) const { return(CPriceSeries::GetData(index)); }
   int               GetData(const int start_pos,const int count,double &buffer[]) const;
   int               GetData(const datetime start_time,const int count,double &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,double &buffer[]) const;
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiHigh::CiHigh(void)
  {
   m_name="High";
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiHigh::~CiHigh(void)
  {
  }
//+------------------------------------------------------------------+
//| Creation of high series                                          |
//+------------------------------------------------------------------+
bool CiHigh::Create(const string symbol,const ENUM_TIMEFRAMES period)
  {
   CDoubleBuffer *buff;
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   if((buff=new CHighBuffer)==NULL)
      return(false);
//--- add
   if(!Add(buff))
     {
      delete buff;
      return(false);
     }
//--- tune
   buff.SetSymbolPeriod(m_symbol,m_period);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| API access method "Copying the high buffer by specifying         |
//| start position and number of elements"                           |
//+------------------------------------------------------------------+
int CiHigh::GetData(const int start_pos,const int count,double &buffer[]) const
  {
   return(CopyHigh(m_symbol,m_period,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the high buffer for the initial       |
//| time and the number of elements"                                 |
//+------------------------------------------------------------------+
int CiHigh::GetData(const datetime start_time,const int count,double &buffer[]) const
  {
   return(CopyHigh(m_symbol,m_period,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the high buffer by specifying         |
//| start and end time"                                              |
//+------------------------------------------------------------------+
int CiHigh::GetData(const datetime start_time,const datetime stop_time,double &buffer[]) const
  {
   return(CopyHigh(m_symbol,m_period,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Class CLowBuffer.                                                |
//| Purpose: Class of buffer of low price series.                    |
//|          Derives from class CPriceBuffer.                        |
//+------------------------------------------------------------------+
class CLowBuffer : public CDoubleBuffer
  {
public:
                     CLowBuffer(void);
                    ~CLowBuffer(void);
   //--- method of refreshing of the data buffer
   virtual bool      Refresh(void);
   virtual bool      RefreshCurrent(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CLowBuffer::CLowBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CLowBuffer::~CLowBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CLowBuffer::Refresh(void)
  {
   m_data_total=CopyLow(m_symbol,m_period,0,m_size,m_data);
//---
   return(m_data_total>0);
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CLowBuffer::RefreshCurrent(void)
  {
   double array[1];
//---
   if(CopyLow(m_symbol,m_period,0,1,array)>0 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Class CiLow.                                                     |
//| Purpose: Class of low series.                                    |
//|          Derives from class CPriceSeries.                        |
//+------------------------------------------------------------------+
class CiLow : public CPriceSeries
  {
public:
                     CiLow(void);
                    ~CiLow(void);
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period);
   //--- methods of access to data
   double            GetData(const int index) const { return(CPriceSeries::GetData(index)); }
   int               GetData(const int start_pos,const int count,double &buffer[]) const;
   int               GetData(const datetime start_time,const int count,double &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,double &buffer[]) const;
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiLow::CiLow(void)
  {
   m_name="Low";
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiLow::~CiLow(void)
  {
  }
//+------------------------------------------------------------------+
//| Creation of low series                                           |
//+------------------------------------------------------------------+
bool CiLow::Create(const string symbol,const ENUM_TIMEFRAMES period)
  {
   CDoubleBuffer *buff;
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   if((buff=new CLowBuffer)==NULL)
      return(false);
//--- add
   if(!Add(buff))
     {
      delete buff;
      return(false);
     }
//--- tune
   buff.SetSymbolPeriod(m_symbol,m_period);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| API access method "Copying the low buffer by specifying          |
//| start position and number of elements"                           |
//+------------------------------------------------------------------+
int CiLow::GetData(const int start_pos,const int count,double &buffer[]) const
  {
   return(CopyLow(m_symbol,m_period,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the low buffer for the initial        |
//| time and the number of elements"                                 |
//+------------------------------------------------------------------+
int CiLow::GetData(const datetime start_time,const int count,double &buffer[]) const
  {
   return(CopyLow(m_symbol,m_period,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the low buffer for the initial        |
//| and final time"                                                  |
//+------------------------------------------------------------------+
int CiLow::GetData(const datetime start_time,const datetime stop_time,double &buffer[]) const
  {
   return(CopyLow(m_symbol,m_period,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Class CCloseBuffer.                                              |
//| Purpose: Class of buffer of low price series.                    |
//|          Derives from class CPriceBuffer.                        |
//+------------------------------------------------------------------+
class CCloseBuffer : public CDoubleBuffer
  {
public:
                     CCloseBuffer(void);
                    ~CCloseBuffer(void);
   //--- method of refreshing of data buffer
   virtual bool      Refresh(void);
   virtual bool      RefreshCurrent(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CCloseBuffer::CCloseBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CCloseBuffer::~CCloseBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CCloseBuffer::Refresh(void)
  {
   m_data_total=CopyClose(m_symbol,m_period,0,m_size,m_data);
//---
   return(m_data_total>0);
  }
//+------------------------------------------------------------------+
//| Refreshing of the data buffer                                    |
//+------------------------------------------------------------------+
bool CCloseBuffer::RefreshCurrent(void)
  {
   double array[1];
//---
   if(CopyClose(m_symbol,m_period,0,1,array)>0 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Class CiClose.                                                   |
//| Purpose: Class of close series.                                  |
//|          Derives from class CPriceSeries.                        |
//+------------------------------------------------------------------+
class CiClose : public CPriceSeries
  {
public:
                     CiClose(void);
                    ~CiClose(void);
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period);
   //--- methods of access to data
   double            GetData(const int index) const { return(CPriceSeries::GetData(index)); }
   int               GetData(const int start_pos,const int count,double &buffer[]) const;
   int               GetData(const datetime start_time,const int count,double &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,double &buffer[]) const;
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiClose::CiClose(void)
  {
   m_name="Close";
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiClose::~CiClose(void)
  {
  }
//+------------------------------------------------------------------+
//| Creation of the close series                                     |
//+------------------------------------------------------------------+
bool CiClose::Create(const string symbol,const ENUM_TIMEFRAMES period)
  {
   CDoubleBuffer *buff;
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   if((buff=new CCloseBuffer)==NULL)
      return(false);
//--- add
   if(!Add(buff))
     {
      delete buff;
      return(false);
     }
//--- tune
   buff.SetSymbolPeriod(m_symbol,m_period);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| API access method "Copying the close buffer by specifying        |
//| start position and number of elements"                           |
//+------------------------------------------------------------------+
int CiClose::GetData(const int start_pos,const int count,double &buffer[]) const
  {
   return(CopyClose(m_symbol,m_period,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the close buffer by specifying        |
//| start time and number of elements"                               |
//+------------------------------------------------------------------+
int CiClose::GetData(const datetime start_time,const int count,double &buffer[]) const
  {
   return(CopyClose(m_symbol,m_period,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the close buffer by specifying        |
//| start and end time"                                              |
//+------------------------------------------------------------------+
int CiClose::GetData(const datetime start_time,const datetime stop_time,double &buffer[]) const
  {
   return(CopyClose(m_symbol,m_period,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Class CSpreadBuffer.                                             |
//| Purpose: Class of buffer of spread series.                       |
//|          Derives from class CArrayInt.                           |
//+------------------------------------------------------------------+
class CSpreadBuffer : public CArrayInt
  {
protected:
   string            m_symbol;           // symbol
   ENUM_TIMEFRAMES   m_period;           // period
   int               m_freshed_data;     // number of refreshed data
   int               m_size;             // size of used history

public:
                     CSpreadBuffer(void);
                    ~CSpreadBuffer(void);
   //--- methods of access to protected data
   void              Size(const int size) { m_size=size; }
   //--- methods of access to data
   int               At(const int index) const;
   //--- method of refreshing of the data buffer
   virtual bool      Refresh(void);
   virtual bool      RefreshCurrent(void);
   //--- methods of tuning
   void              SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSpreadBuffer::CSpreadBuffer(void) : m_symbol(""),
                                     m_period(WRONG_VALUE),
                                     m_freshed_data(0),
                                     m_size(DEFAULT_BUFFER_SIZE)
  {
   ArraySetAsSeries(m_data,true);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSpreadBuffer::~CSpreadBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Access to data on the position                                   |
//+------------------------------------------------------------------+
int CSpreadBuffer::At(const int index) const
  {
//--- check
   if(index>=m_data_total)
      return(0);
//---
   return(CArrayInt::At(index));
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CSpreadBuffer::Refresh(void)
  {
   m_freshed_data=CopySpread(m_symbol,m_period,0,m_size,m_data);
//---
   if(m_freshed_data>0)
     {
      m_data_total=ArraySize(m_data);
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Refreshing of the data buffer                                    |
//+------------------------------------------------------------------+
bool CSpreadBuffer::RefreshCurrent(void)
  {
   int array[1];
//---
   if(CopySpread(m_symbol,m_period,0,1,array)==1 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Set symbol and period                                            |
//+------------------------------------------------------------------+
void CSpreadBuffer::SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period)
  {
   m_symbol=(symbol==NULL) ? ChartSymbol() : symbol;
   m_period=(period==0)    ? ChartPeriod() : period;
  }
//+------------------------------------------------------------------+
//| Class CiSpread.                                                  |
//| Purpose: Class of spread series.                                 |
//|          Derives from class CSeries.                             |
//+------------------------------------------------------------------+
class CiSpread : public CSeries
  {
public:
                     CiSpread(void);
                    ~CiSpread(void);
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period);
   virtual bool      BufferResize(const int size);
   //--- methods of access to data
   int               GetData(const int index) const;
   int               GetData(const int start_pos,const int count,int &buffer[]) const;
   int               GetData(const datetime start_time,const int count,int &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,int &buffer[]) const;
   //--- method of refreshing of the data
   virtual void      Refresh(const int flags=OBJ_ALL_PERIODS);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiSpread::CiSpread(void)
  {
   m_name="Spread";
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiSpread::~CiSpread(void)
  {
  }
//+------------------------------------------------------------------+
//| Creating of the spread series                                    |
//+------------------------------------------------------------------+
bool CiSpread::Create(const string symbol,const ENUM_TIMEFRAMES period)
  {
   CSpreadBuffer *buff;
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   if((buff=new CSpreadBuffer)==NULL)
      return(false);
//--- add
   if(!Add(buff))
     {
      delete buff;
      return(false);
     }
//--- tune
   buff.SetSymbolPeriod(m_symbol,m_period);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Method to access data                                            |
//+------------------------------------------------------------------+
int CiSpread::GetData(const int index) const
  {
   CSpreadBuffer *buff=At(0);
//--- check
   if(buff==NULL)
     {
      Print(__FUNCTION__,": invalid buffer");
      return(0);
     }
//---
   return(buff.At(index));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the spread buffer by specifying       |
//| start position and number of elements"                           |
//+------------------------------------------------------------------+
int CiSpread::GetData(const int start_pos,const int count,int &buffer[]) const
  {
   return(CopySpread(m_symbol,m_period,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the spread buffer by specifying       |
//| start time and number of elements"                               |
//+------------------------------------------------------------------+
int CiSpread::GetData(const datetime start_time,const int count,int &buffer[]) const
  {
   return(CopySpread(m_symbol,m_period,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the spread buffer by specifying       |
//| start and end time"                                              |
//+------------------------------------------------------------------+
int CiSpread::GetData(const datetime start_time,const datetime stop_time,int &buffer[]) const
  {
   return(CopySpread(m_symbol,m_period,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Set size buffer                                                  |
//+------------------------------------------------------------------+
bool CiSpread::BufferResize(const int size)
  {
   if(size>m_buffer_size && !CSeries::BufferResize(size))
      return(false);
//-- history is avalible
   CSpreadBuffer *buff=At(0);
//--- check pointer
   if(buff==NULL)
      return(false);
//---
   buff.Size(size);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Refreshing of data                                               |
//+------------------------------------------------------------------+
void CiSpread::Refresh(const int flags)
  {
   CSpreadBuffer *buff=At(0);
//--- check
   if(buff==NULL)
      return;
//--- refresh buffer
   if(!(flags&m_timeframe_flags))
     {
      if(m_refresh_current)
         buff.RefreshCurrent();
     }
   else
      buff.Refresh();
  }
//+------------------------------------------------------------------+
//| Class CTimeBuffer.                                               |
//| Purpose: Class of buffer of time series.                         |
//|          Derives from class CArrayLong.                          |
//+------------------------------------------------------------------+
class CTimeBuffer : public CArrayLong
  {
protected:
   string            m_symbol;           // symbol
   ENUM_TIMEFRAMES   m_period;           // period
   int               m_freshed_data;     // number of refreshed data
   int               m_size;             // size of used history

public:
                     CTimeBuffer(void);
                    ~CTimeBuffer(void);
   //--- methods of access to protected data
   void              Size(const int size) { m_size=size; }
   //--- methods of access to data
   long              At(const int index) const;
   //--- method of refreshing of the data buffer
   virtual bool      Refresh(void);
   virtual bool      RefreshCurrent(void);
   //--- methods of tuning
   void              SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CTimeBuffer::CTimeBuffer(void) : m_symbol(""),
                                 m_period(WRONG_VALUE),
                                 m_freshed_data(0),
                                 m_size(DEFAULT_BUFFER_SIZE)
  {
   ArraySetAsSeries(m_data,true);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CTimeBuffer::~CTimeBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Access to data in a position                                     |
//+------------------------------------------------------------------+
long CTimeBuffer::At(const int index) const
  {
//--- check
   if(index>=m_data_total)
      return(0);
//---
   return((datetime)CArrayLong::At(index));
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CTimeBuffer::Refresh(void)
  {
   m_freshed_data=CopyTime(m_symbol,m_period,0,m_size,m_data);
//---
   if(m_freshed_data>0)
     {
      m_data_total=ArraySize(m_data);
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CTimeBuffer::RefreshCurrent(void)
  {
   long array[1];
//---
   if(CopyTime(m_symbol,m_period,0,1,array)==1 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Set symbol and period                                            |
//+------------------------------------------------------------------+
void CTimeBuffer::SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period)
  {
   m_symbol=(symbol==NULL) ? ChartSymbol() : symbol;
   m_period=(period==0)    ? ChartPeriod() : period;
  }
//+------------------------------------------------------------------+
//| Class CiTime.                                                    |
//| Purpose: Class of time series.                                   |
//|          Derives from class CSeries.                             |
//+------------------------------------------------------------------+
class CiTime : public CSeries
  {
public:
                     CiTime(void);
                    ~CiTime(void);
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period);
   virtual bool      BufferResize(const int size);
   //--- methods of access to data
   datetime          GetData(const int index) const;
   int               GetData(const int start_pos,const int count,datetime &buffer[]) const;
   int               GetData(const datetime start_time,const int count,datetime &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,datetime &buffer[]) const;
   //--- method of refreshing of the data
   virtual void      Refresh(const int flags=OBJ_ALL_PERIODS);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiTime::CiTime(void)
  {
   m_name="Time";
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiTime::~CiTime(void)
  {
  }
//+------------------------------------------------------------------+
//| Creating of the time series                                      |
//+------------------------------------------------------------------+
bool CiTime::Create(const string symbol,const ENUM_TIMEFRAMES period)
  {
   CTimeBuffer *buff;
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   if((buff=new CTimeBuffer)==NULL)
      return(false);
//--- add
   if(!Add(buff))
     {
      delete buff;
      return(false);
     }
//--- tune
   buff.SetSymbolPeriod(m_symbol,m_period);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Method to access data                                            |
//+------------------------------------------------------------------+
datetime CiTime::GetData(const int index) const
  {
   CTimeBuffer *buff=At(0);
//--- check
   if(buff==NULL)
     {
      Print(__FUNCTION__,": invalid buffer");
      return(0);
     }
//---
   return((datetime)buff.At(index));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the time buffer by specifying         |
//| start position and number of elements"                           |
//+------------------------------------------------------------------+
int CiTime::GetData(const int start_pos,const int count,datetime &buffer[]) const
  {
   return(CopyTime(m_symbol,m_period,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the time buffer by specifying         |
//| start time and number of elements"                               |
//+------------------------------------------------------------------+
int CiTime::GetData(const datetime start_time,const int count,datetime &buffer[]) const
  {
   return(CopyTime(m_symbol,m_period,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the time buffer by specifying         |
//| start and end time"                                              |
//+------------------------------------------------------------------+
int CiTime::GetData(const datetime start_time,const datetime stop_time,datetime &buffer[]) const
  {
   return(CopyTime(m_symbol,m_period,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Set size buffer                                                  |
//+------------------------------------------------------------------+
bool CiTime::BufferResize(const int size)
  {
   if(size>m_buffer_size && !CSeries::BufferResize(size))
      return(false);
//-- history is avalible
   CTimeBuffer *buff=At(0);
//--- check pointer
   if(buff==NULL)
      return(false);
//---
   buff.Size(size);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Refreshing of data                                               |
//+------------------------------------------------------------------+
void CiTime::Refresh(const int flags)
  {
   CTimeBuffer *buff=At(0);
//--- check
   if(buff==NULL)
      return;
//--- refresh buffers
   if(!(flags&m_timeframe_flags))
     {
      if(m_refresh_current)
         buff.RefreshCurrent();
     }
   else
      buff.Refresh();
  }
//+------------------------------------------------------------------+
//| Class CTickVolumeBuffer.                                         |
//| Purpose: Class of buffer of tick volume series.                  |
//|          Derives from class CArrayLong.                          |
//+------------------------------------------------------------------+
class CTickVolumeBuffer : public CArrayLong
  {
protected:
   string            m_symbol;           // symbol
   ENUM_TIMEFRAMES   m_period;           // period
   int               m_freshed_data;     // number of refreshed data
   int               m_size;             // size of used history

public:
                     CTickVolumeBuffer(void);
                    ~CTickVolumeBuffer(void);
   //--- methods of access to protected data
   void              Size(const int size) { m_size=size; }
   //--- methods of access to data
   long              At(const int index) const;
   //--- method of refreshing of the data buffer
   virtual bool      Refresh(void);
   virtual bool      RefreshCurrent(void);
   //--- methods of tuning
   void              SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CTickVolumeBuffer::CTickVolumeBuffer(void) : m_symbol(""),
                                             m_period(WRONG_VALUE),
                                             m_freshed_data(0),
                                             m_size(DEFAULT_BUFFER_SIZE)
  {
   ArraySetAsSeries(m_data,true);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CTickVolumeBuffer::~CTickVolumeBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Access to data in a position                                     |
//+------------------------------------------------------------------+
long CTickVolumeBuffer::At(const int index) const
  {
//--- check
   if(index>=m_data_total)
      return(0);
//---
   return((datetime)CArrayLong::At(index));
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CTickVolumeBuffer::Refresh(void)
  {
   m_freshed_data=CopyTickVolume(m_symbol,m_period,0,m_size,m_data);
//---
   if(m_freshed_data>0)
     {
      m_data_total=ArraySize(m_data);
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CTickVolumeBuffer::RefreshCurrent(void)
  {
   long array[1];
//---
   if(CopyTickVolume(m_symbol,m_period,0,1,array)==1 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Set symbol and period                                            |
//+------------------------------------------------------------------+
void CTickVolumeBuffer::SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period)
  {
   m_symbol=(symbol==NULL) ? ChartSymbol() : symbol;
   m_period=(period==0)    ? ChartPeriod() : period;
  }
//+------------------------------------------------------------------+
//| Class CiTickVolume.                                              |
//| Purpose: Class of tick volume series.                            |
//|          Derives from class CSeries.                             |
//+------------------------------------------------------------------+
class CiTickVolume : public CSeries
  {
public:
                     CiTickVolume(void);
                    ~CiTickVolume(void);
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period);
   virtual bool      BufferResize(const int size);
   //--- methods of access to data
   long              GetData(const int index) const;
   int               GetData(const int start_pos,const int count,long &buffer[]) const;
   int               GetData(const datetime start_time,const int count,long &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,long &buffer[]) const;
   //--- method of refreshing of the data
   virtual void      Refresh(const int flags=OBJ_ALL_PERIODS);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiTickVolume::CiTickVolume(void)
  {
   m_name="Volume";
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiTickVolume::~CiTickVolume(void)
  {
  }
//+------------------------------------------------------------------+
//| Creation of the tick volume series                               |
//+------------------------------------------------------------------+
bool CiTickVolume::Create(const string symbol,const ENUM_TIMEFRAMES period)
  {
   CTickVolumeBuffer *buff;
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   if((buff=new CTickVolumeBuffer)==NULL)
      return(false);
//--- add
   if(!Add(buff))
     {
      delete buff;
      return(false);
     }
//--- tune
   buff.SetSymbolPeriod(m_symbol,m_period);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Method to access data                                            |
//+------------------------------------------------------------------+
long CiTickVolume::GetData(const int index) const
  {
   CTickVolumeBuffer *buff=At(0);
//--- check
   if(buff==NULL)
     {
      Print(__FUNCTION__,": invalid buffer");
      return(0);
     }
//---
   return(buff.At(index));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the tick volume buffer by specifying  |
//| start position and number of elements"                           |
//+------------------------------------------------------------------+
int CiTickVolume::GetData(const int start_pos,const int count,long &buffer[]) const
  {
   return(CopyTickVolume(m_symbol,m_period,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the tick volume buffer by specifying  |
//| start time and number of elements"                               |
//+------------------------------------------------------------------+
int CiTickVolume::GetData(const datetime start_time,const int count,long &buffer[]) const
  {
   return(CopyTickVolume(m_symbol,m_period,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the tick volume buffer by specifying  |
//| start and end time"                                              |
//+------------------------------------------------------------------+
int CiTickVolume::GetData(const datetime start_time,const datetime stop_time,long &buffer[]) const
  {
   return(CopyTickVolume(m_symbol,m_period,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Set size buffer                                                  |
//+------------------------------------------------------------------+
bool CiTickVolume::BufferResize(const int size)
  {
   if(size>m_buffer_size && !CSeries::BufferResize(size))
      return(false);
//-- history is avalible
   CTickVolumeBuffer *buff=At(0);
//--- check pointer
   if(buff==NULL)
      return(false);
//--
   buff.Size(size);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Refreshing of data                                               |
//+------------------------------------------------------------------+
void CiTickVolume::Refresh(const int flags)
  {
   CTickVolumeBuffer *buff=At(0);
//--- check
   if(buff==NULL)
      return;
//--- refresh buffers
   if(!(flags&m_timeframe_flags))
     {
      if(m_refresh_current)
         buff.RefreshCurrent();
     }
   else
      buff.Refresh();
  }
//+------------------------------------------------------------------+
//| Class CRealVolumeBuffer.                                         |
//| Purpose: Class of buffer of real volume series.                  |
//|          Derives from class CArrayLong.                          |
//+------------------------------------------------------------------+
class CRealVolumeBuffer : public CArrayLong
  {
protected:
   string            m_symbol;           // symbol
   ENUM_TIMEFRAMES   m_period;           // period
   int               m_freshed_data;     // number of refreshed data
   int               m_size;             // size of used history

public:
                     CRealVolumeBuffer(void);
                    ~CRealVolumeBuffer(void);
   //--- methods of access to protected data
   void              Size(const int size) { m_size=size; }
   //--- methods of access to data
   long              At(const int index) const;
   //--- method of refreshing of the data buffer
   virtual bool      Refresh(void);
   virtual bool      RefreshCurrent(void);
   //--- methods of tuning
   void              SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CRealVolumeBuffer::CRealVolumeBuffer(void) : m_symbol(""),
                                             m_period(WRONG_VALUE),
                                             m_freshed_data(0),
                                             m_size(DEFAULT_BUFFER_SIZE)
  {
   ArraySetAsSeries(m_data,true);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CRealVolumeBuffer::~CRealVolumeBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Access to data in a position                                     |
//+------------------------------------------------------------------+
long CRealVolumeBuffer::At(const int index) const
  {
//--- check
   if(index>=m_data_total)
      return(0);
//---
   return((datetime)CArrayLong::At(index));
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CRealVolumeBuffer::Refresh(void)
  {
   m_freshed_data=CopyRealVolume(m_symbol,m_period,0,m_size,m_data);
//---
   if(m_freshed_data>0)
     {
      m_data_total=ArraySize(m_data);
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Refreshing of data buffer                                        |
//+------------------------------------------------------------------+
bool CRealVolumeBuffer::RefreshCurrent(void)
  {
   long array[1];
//---
   if(CopyRealVolume(m_symbol,m_period,0,1,array)==1 && m_data_total>0)
     {
      m_data[0]=array[0];
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Set symbol and period                                            |
//+------------------------------------------------------------------+
void CRealVolumeBuffer::SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period)
  {
   m_symbol=(symbol==NULL) ? ChartSymbol() : symbol;
   m_period=(period==0)    ? ChartPeriod() : period;
  }
//+------------------------------------------------------------------+
//| Class CiRealVolume.                                              |
//| Purpose: Class of real volume series.                            |
//|          Derives from class CSeries.                             |
//+------------------------------------------------------------------+
class CiRealVolume : public CSeries
  {
public:
                     CiRealVolume(void);
                    ~CiRealVolume(void);
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period);
   virtual bool      BufferResize(const int size);
   //--- methods of access to data
   long              GetData(const int index) const;
   int               GetData(const int start_pos,const int count,long &buffer[]) const;
   int               GetData(const datetime start_time,const int count,long &buffer[]) const;
   int               GetData(const datetime start_time,const datetime stop_time,long &buffer[]) const;
   //--- method of refreshing of the data
   virtual void      Refresh(const int flags=OBJ_ALL_PERIODS);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiRealVolume::CiRealVolume(void)
  {
   m_name="RealVolume";
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiRealVolume::~CiRealVolume(void)
  {
  }
//+------------------------------------------------------------------+
//| Creation of the real volume series                               |
//+------------------------------------------------------------------+
bool CiRealVolume::Create(const string symbol,const ENUM_TIMEFRAMES period)
  {
   CRealVolumeBuffer *buff;
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   if((buff=new CRealVolumeBuffer)==NULL)
      return(false);
//--- add
   if(!Add(buff))
     {
      delete buff;
      return(false);
     }
//--- tune
   buff.SetSymbolPeriod(m_symbol,m_period);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Method to access data                                            |
//+------------------------------------------------------------------+
long CiRealVolume::GetData(const int index) const
  {
   CRealVolumeBuffer *buff=At(0);
//--- check
   if(buff==NULL)
     {
      Print(__FUNCTION__,": invalid buffer");
      return(0);
     }
//---
   return(buff.At(index));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the real volume buffer by specifying  |
//| start position and number of elements"                           |
//+------------------------------------------------------------------+
int CiRealVolume::GetData(const int start_pos,const int count,long &buffer[]) const
  {
   return(CopyRealVolume(m_symbol,m_period,start_pos,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the real volume buffer by specifying  |
//| start time and number of elements"                               |
//+------------------------------------------------------------------+
int CiRealVolume::GetData(const datetime start_time,const int count,long &buffer[]) const
  {
   return(CopyRealVolume(m_symbol,m_period,start_time,count,buffer));
  }
//+------------------------------------------------------------------+
//| API access method "Copying the real volume buffer by specifying  |
//| start and end time"                                              |
//+------------------------------------------------------------------+
int CiRealVolume::GetData(const datetime start_time,const datetime stop_time,long &buffer[]) const
  {
   return(CopyRealVolume(m_symbol,m_period,start_time,stop_time,buffer));
  }
//+------------------------------------------------------------------+
//| Set size buffer                                                  |
//+------------------------------------------------------------------+
bool CiRealVolume::BufferResize(const int size)
  {
   if(size>m_buffer_size && !CSeries::BufferResize(size))
      return(false);
//-- history is avalible
   CRealVolumeBuffer *buff=At(0);
//--- check pointer
   if(buff==NULL)
      return(false);
//--
   buff.Size(size);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Refreshing of data                                               |
//+------------------------------------------------------------------+
void CiRealVolume::Refresh(const int flags)
  {
   CRealVolumeBuffer *buff=At(0);
//--- check
   if(buff==NULL)
      return;
//--- refresh buffers
   if(!(flags&m_timeframe_flags))
     {
      if(m_refresh_current)
         buff.RefreshCurrent();
     }
   else
      buff.Refresh();
  }
//+------------------------------------------------------------------+
