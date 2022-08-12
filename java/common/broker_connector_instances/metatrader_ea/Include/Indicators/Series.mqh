//+------------------------------------------------------------------+
//|                                                       Series.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Arrays\ArrayObj.mqh>
#include <Arrays\ArrayDouble.mqh>
//+------------------------------------------------------------------+
//| defines                                                          |
//+------------------------------------------------------------------+
#define DEFAULT_BUFFER_SIZE 1024
//+------------------------------------------------------------------+
//| Class CSeries.                                                   |
//| Purpose: Base class for access to timeseries.                    |
//|          Derives from class CArrayObj.                           |
//+------------------------------------------------------------------+
class CSeries : public CArrayObj
  {
protected:
   string            m_name;             // name of series
   int               m_buffers_total;    // number of buffers
   int               m_buffer_size;      // buffer size
   int               m_timeframe_flags;  // flags of timeframes (similar to "flags of visibility of objects")
   string            m_symbol;           // symbol
   ENUM_TIMEFRAMES   m_period;           // period
   bool              m_refresh_current;  // flag
   //--- 
   datetime          m_first_date;

public:
                     CSeries(void);
                    ~CSeries(void);
   //--- methods of access to protected data
   string            Name(void)                const { return(m_name);            }
   int               BuffersTotal(void)        const { return(m_buffers_total);   }
   int               BufferSize(void)          const { return(m_buffer_size);     }
   int               Timeframe(void)           const { return(m_timeframe_flags); }
   string            Symbol(void)              const { return(m_symbol);          }
   ENUM_TIMEFRAMES   Period(void)              const { return(m_period);          }
   string            PeriodDescription(const int val=0);
   void              RefreshCurrent(const bool flag) { m_refresh_current=flag;    }
   //--- method of tuning
   virtual bool      BufferResize(const int size);
   //--- method of refreshing" of the data
   virtual void      Refresh(const int flags) { }

protected:
   //--- methods of tuning
   bool              SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period);
   void              PeriodToTimeframeFlag(const ENUM_TIMEFRAMES period);
   //---
   bool              CheckLoadHistory(const int size);
   bool              CheckTerminalHistory(const int size);
   bool              CheckServerHistory(const int size);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CSeries::CSeries(void) : m_name(""),
                              m_timeframe_flags(0),
                              m_buffers_total(0),
                              m_buffer_size(0),
                              m_symbol(""),
                              m_period(WRONG_VALUE),
                              m_refresh_current(true)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSeries::~CSeries(void)
  {
  }
//+------------------------------------------------------------------+
//| Set buffer size                                                  |
//+------------------------------------------------------------------+
bool CSeries::BufferResize(const int size)
  {
//--- check history
   if(!CheckLoadHistory(size))
     {
      printf("failed to get %d bars for %s,%s",size,m_symbol,EnumToString(m_period));
      return(false);
     }
//--- history is available
   m_buffer_size=size;
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Set symbol and period                                            |
//+------------------------------------------------------------------+
bool CSeries::SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period)
  {
   m_symbol=(symbol==NULL) ? ChartSymbol() : symbol;
   m_period=(period==0)    ? ChartPeriod() : period;
   PeriodToTimeframeFlag(m_period);
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Convert period to timeframe flag (similar to visibility flags)   |
//+------------------------------------------------------------------+
void CSeries::PeriodToTimeframeFlag(const ENUM_TIMEFRAMES period)
  {
   static ENUM_TIMEFRAMES _p_int[]=
     {
      PERIOD_M1,PERIOD_M2,PERIOD_M3,PERIOD_M4,PERIOD_M5,PERIOD_M6,
      PERIOD_M10,PERIOD_M12,PERIOD_M15,PERIOD_M20,PERIOD_M30,
      PERIOD_H1,PERIOD_H2,PERIOD_H3,PERIOD_H4,PERIOD_H6,PERIOD_H8,PERIOD_H12,
      PERIOD_D1,PERIOD_W1,PERIOD_MN1
     };
//--- cycle for all timeframes
   for(int i=0;i<ArraySize(_p_int);i++)
      if(period==_p_int[i])
        {
         //--- at the same time generate the flag of the working timeframe
         m_timeframe_flags=((int)1)<<i;
         return;
        }
  }
//+------------------------------------------------------------------+
//| Converting value of ENUM_TIMEFRAMES to string                    |
//+------------------------------------------------------------------+
string CSeries::PeriodDescription(const int val)
  {
   int i,frame;
//--- arrays for conversion of ENUM_TIMEFRAMES to string
   static string _p_str[]=
     {
      "M1","M2","M3","M4","M5","M6","M10","M12","M15","M20","M30",
      "H1","H2","H3","H4","H6","H8","H12","D1","W1","MN","UNKNOWN"
     };
   static ENUM_TIMEFRAMES _p_int[]=
     {
      PERIOD_M1,PERIOD_M2,PERIOD_M3,PERIOD_M4,PERIOD_M5,PERIOD_M6,
      PERIOD_M10,PERIOD_M12,PERIOD_M15,PERIOD_M20,PERIOD_M30,
      PERIOD_H1,PERIOD_H2,PERIOD_H3,PERIOD_H4,PERIOD_H6,PERIOD_H8,PERIOD_H12,
      PERIOD_D1,PERIOD_W1,PERIOD_MN1
     };
//--- check
   frame=(val==0)?m_period:val;
   if(frame==WRONG_VALUE)
      return("WRONG_VALUE");
//--- cycle for all timeframes
   for(i=0;i<ArraySize(_p_int);i++)
      if(frame==_p_int[i])
         break;
//---
   return(_p_str[i]);
  }
//+------------------------------------------------------------------+
//| Checks data by specified symbol's timeframe and                  |
//| downloads it from server, if necessary                           |
//+------------------------------------------------------------------+
bool CSeries::CheckLoadHistory(const int size)
  {
//--- don't ask for load of its own data if it is an indicator
   if(MQL5InfoInteger(MQL5_PROGRAM_TYPE)==PROGRAM_INDICATOR && Period()==m_period && Symbol()==m_symbol)
      return(true);
   if(size>TerminalInfoInteger(TERMINAL_MAXBARS))
     {
      //--- Definitely won't have such amount of data
      printf(__FUNCTION__+": requested too much data (%d)",size);
      return(false);
     }
   m_first_date=0;
   if(CheckTerminalHistory(size))
      return(true);
   if(CheckServerHistory(size))
      return(true);
//--- failed
   return(false);
  }
//+------------------------------------------------------------------+
//| Checks data in terminal                                          |
//+------------------------------------------------------------------+
bool CSeries::CheckTerminalHistory(const int size)
  {
   datetime times[1];
   long     bars=0;
//--- Enough data in timeseries?
   if(Bars(m_symbol,m_period)>=size)
      return(true);
//--- second attempt
   if(SeriesInfoInteger(m_symbol,PERIOD_M1,SERIES_BARS_COUNT,bars))
     {
      //--- there is loaded data to build timeseries
      if(bars>size*PeriodSeconds(m_period)/60)
        {
         //--- force timeseries build
         CopyTime(m_symbol,m_period,size-1,1,times);
         //--- check date
         if(SeriesInfoInteger(m_symbol,m_period,SERIES_BARS_COUNT,bars))
            //--- Timeseries generated using data from terminal
            if(bars>size)
               return(true);
        }
     }
//--- failed
   return(false);
  }
//+------------------------------------------------------------------+
//| Downloads missing data from server                               |
//+------------------------------------------------------------------+
bool CSeries::CheckServerHistory(const int size)
  {
//--- load symbol history info
   datetime first_server_date=0;
   while(!SeriesInfoInteger(m_symbol,PERIOD_M1,SERIES_SERVER_FIRSTDATE,first_server_date) && !IsStopped())
      Sleep(5);
//--- Enough data on server?
   if(first_server_date>TimeCurrent()-size*PeriodSeconds(m_period))
      return(false);
//--- load data step by step
   int      fail_cnt=0;
   datetime times[1];
   while(!IsStopped())
     {
      //--- wait for timeseries build
      while(!SeriesInfoInteger(m_symbol,m_period,SERIES_SYNCHRONIZED) && !IsStopped())
         Sleep(5);
      //--- ask for built bars
      int bars=Bars(m_symbol,m_period);
      if(bars>size)
         return(true);
      //--- copying of next part forces data loading
      if(CopyTime(m_symbol,m_period,size-1,1,times)==1)
         return(true);
      //--- no more than 100 failed attempts
      if(++fail_cnt>=100)
         return(false);
      Sleep(10);
     }
//--- failed
   return(false);
  }
//+------------------------------------------------------------------+
//| Class CDoubleBuffer.                                             |
//| Purpose: Base class of buffer of data of the double type.        |
//|          Derives from class CArrayDouble.                        |
//+------------------------------------------------------------------+
class CDoubleBuffer : public CArrayDouble
  {
protected:
   string            m_symbol;           // symbol
   ENUM_TIMEFRAMES   m_period;           // period
   int               m_size;             // size of used history

public:
                     CDoubleBuffer(void);
                    ~CDoubleBuffer(void);
   //--- methods of access to protected data
   void              Size(const int size) { m_size=size; }
   //--- methods of access to data
   double            At(const int index) const;
   //--- method of refreshing of the data buffer
   virtual bool      Refresh(void) { return(true); }
   virtual bool      RefreshCurrent(void) { return(true); }
   //--- methods of tuning
   void              SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CDoubleBuffer::CDoubleBuffer(void) : m_symbol(""),
                                     m_period(WRONG_VALUE),
                                     m_size(DEFAULT_BUFFER_SIZE)
  {
   ArraySetAsSeries(m_data,true);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CDoubleBuffer::~CDoubleBuffer(void)
  {
  }
//+------------------------------------------------------------------+
//| Access to data in a specified position                           |
//+------------------------------------------------------------------+
double CDoubleBuffer::At(const int index) const
  {
//--- check
   if(index>=m_data_total)
      return(EMPTY_VALUE);
//---
   double d=CArrayDouble::At(index);
//---
   return(d);
  }
//+------------------------------------------------------------------+
//| Set symbol and period                                            |
//+------------------------------------------------------------------+
void CDoubleBuffer::SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period)
  {
   m_symbol=(symbol==NULL) ? ChartSymbol() : symbol;
   m_period=(period==0)    ? ChartPeriod() : period;
  }
//+------------------------------------------------------------------+
