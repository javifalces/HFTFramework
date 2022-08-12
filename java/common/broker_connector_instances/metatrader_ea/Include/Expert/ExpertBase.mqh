//+------------------------------------------------------------------+
//|                                                   ExpertBase.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Trade\SymbolInfo.mqh>
#include <Trade\AccountInfo.mqh>
#include <Trade\PositionInfo.mqh>
#include <Trade\OrderInfo.mqh>
#include <Trade\DealInfo.mqh>
#include <Trade\HistoryOrderInfo.mqh>
#include <Indicators\Indicators.mqh>
//+------------------------------------------------------------------+
//| enumerations                                                     |
//+------------------------------------------------------------------+
//--- constants of identification of trend
enum ENUM_TYPE_TREND
  {
   TYPE_TREND_HARD_DOWN  =0,           // strong down trend
   TYPE_TREND_DOWN       =1,           // down trend
   TYPE_TREND_SOFT_DOWN  =2,           // weak down trend
   TYPE_TREND_FLAT       =3,           // no trend
   TYPE_TREND_SOFT_UP    =4,           // weak up trend
   TYPE_TREND_UP         =5,           // up trend
   TYPE_TREND_HARD_UP    =6            // strong up trend
  };
//--- flags of used timeseries
enum ENUM_USED_SERIES
  {
   USE_SERIES_OPEN       =0x1,
   USE_SERIES_HIGH       =0x2,
   USE_SERIES_LOW        =0x4,
   USE_SERIES_CLOSE      =0x8,
   USE_SERIES_SPREAD     =0x10,
   USE_SERIES_TIME       =0x20,
   USE_SERIES_TICK_VOLUME=0x40,
   USE_SERIES_REAL_VOLUME=0x80
  };
//--- phases of initialization of an object
enum ENUM_INIT_PHASE
  {
   INIT_PHASE_FIRST      =0,           // start phase (only Init(...) can be called)
   INIT_PHASE_TUNING     =1,           // phase of tuning (set in Init(...))
   INIT_PHASE_VALIDATION =2,           // phase of checking of parameters(set in ValidationSettings(...))
   INIT_PHASE_COMPLETE   =3            // end phase (set in InitIndicators(...))
  };
//+------------------------------------------------------------------+
//| Macro definitions.                                               |
//+------------------------------------------------------------------+
//--- check the use of timeseries
#define IS_OPEN_SERIES_USAGE         ((m_used_series&USE_SERIES_OPEN)!=0)
#define IS_HIGH_SERIES_USAGE         ((m_used_series&USE_SERIES_HIGH)!=0)
#define IS_LOW_SERIES_USAGE          ((m_used_series&USE_SERIES_LOW)!=0)
#define IS_CLOSE_SERIES_USAGE        ((m_used_series&USE_SERIES_CLOSE)!=0)
#define IS_SPREAD_SERIES_USAGE       ((m_used_series&USE_SERIES_SPREAD)!=0)
#define IS_TIME_SERIES_USAGE         ((m_used_series&USE_SERIES_TIME)!=0)
#define IS_TICK_VOLUME_SERIES_USAGE  ((m_used_series&USE_SERIES_TICK_VOLUME)!=0)
#define IS_REAL_VOLUME_SERIES_USAGE  ((m_used_series&USE_SERIES_REAL_VOLUME)!=0)
//+------------------------------------------------------------------+
//| Class CExpertBase.                                               |
//| Purpose: Base class of component of Expert Advisor.              |
//| Derives from class CObject.                                      |
//+------------------------------------------------------------------+
class CExpertBase : public CObject
  {
protected:
   //--- variables
   ulong             m_magic;          // expert magic number
   ENUM_INIT_PHASE   m_init_phase;     // the phase (stage) of initialization of object
   bool              m_other_symbol;   // flag of a custom work symbols (different from one of the Expert Advisor)
   CSymbolInfo      *m_symbol;         // pointer to the object-symbol
   bool              m_other_period;   // flag of a custom timeframe (different from one of the Expert Advisor)
   ENUM_TIMEFRAMES   m_period;         // work timeframe
   double            m_adjusted_point; // "weight" 2/4 of a point
   CAccountInfo      m_account;        // object-deposit
   ENUM_ACCOUNT_MARGIN_MODE m_margin_mode; // netting or hedging
   ENUM_TYPE_TREND   m_trend_type;     // identifier of trend
   bool              m_every_tick;     // flag of starting the analysis from current (incomplete) bar
   //--- timeseries
   int               m_used_series;    // flags of using of series
   CiOpen           *m_open;           // pointer to the object for access to open prices of bars
   CiHigh           *m_high;           // pointer to the object for access to high prices of bars
   CiLow            *m_low;            // pointer to the object for access to low prices of bars
   CiClose          *m_close;          // pointer to the object for access to close prices of bars
   CiSpread         *m_spread;         // pointer to the object for access to spreads
   CiTime           *m_time;           // pointer to the object for access to time of closing of bars
   CiTickVolume     *m_tick_volume;    // pointer to the object for access to tick volumes of bars
   CiRealVolume     *m_real_volume;    // pointer to the object for access to real volumes of bars

public:
                     CExpertBase(void);
                    ~CExpertBase(void);
   //--- methods of access to protected data
   ENUM_INIT_PHASE   InitPhase(void)            const { return(m_init_phase); }
   void              TrendType(ENUM_TYPE_TREND value) { m_trend_type=value;   }
   int               UsedSeries(void) const;
   void              EveryTick(bool value)            { m_every_tick=value;   }
   //--- methods of access to protected data
   double            Open(int ind) const;
   double            High(int ind) const;
   double            Low(int ind) const;
   double            Close(int ind) const;
   int               Spread(int ind) const;
   datetime          Time(int ind) const;
   long              TickVolume(int ind) const;
   long              RealVolume(int ind) const;
   //--- methods of initialization of the object
   virtual bool      Init(CSymbolInfo *symbol,ENUM_TIMEFRAMES period,double point);
   bool              Symbol(string name);
   bool              Period(ENUM_TIMEFRAMES value);
   void              Magic(ulong value) { m_magic=value; }
   void              SetMarginMode(void) { m_margin_mode=(ENUM_ACCOUNT_MARGIN_MODE)AccountInfoInteger(ACCOUNT_MARGIN_MODE); }
   //--- method of verification of settings
   virtual bool      ValidationSettings();
   //--- methods of creating the indicator and timeseries
   virtual bool      SetPriceSeries(CiOpen *open,CiHigh *high,CiLow *low,CiClose *close);
   virtual bool      SetOtherSeries(CiSpread *spread,CiTime *time,CiTickVolume *tick_volume,CiRealVolume *real_volume);
   virtual bool      InitIndicators(CIndicators *indicators=NULL);

protected:
   //--- methods initialization of timeseries
   bool              InitOpen(CIndicators *indicators);
   bool              InitHigh(CIndicators *indicators);
   bool              InitLow(CIndicators *indicators);
   bool              InitClose(CIndicators *indicators);
   bool              InitSpread(CIndicators *indicators);
   bool              InitTime(CIndicators *indicators);
   bool              InitTickVolume(CIndicators *indicators);
   bool              InitRealVolume(CIndicators *indicators);
   //--- method of getting the measure units of price levels
   virtual double    PriceLevelUnit(void)      { return(m_adjusted_point); }
   //--- method of getting index of bar the analysis starts with
   virtual int       StartIndex(void) { return((m_every_tick?0:1)); }
   virtual bool      CompareMagic(ulong magic) { return(m_magic==magic);   }
   bool              IsHedging(void) const { return(m_margin_mode==ACCOUNT_MARGIN_MODE_RETAIL_HEDGING); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CExpertBase::CExpertBase(void) : m_magic(0),
                                      m_margin_mode(ACCOUNT_MARGIN_MODE_RETAIL_NETTING),
                                      m_init_phase(INIT_PHASE_FIRST),
                                      m_other_symbol(false),
                                      m_symbol(NULL),
                                      m_other_period(false),
                                      m_period(PERIOD_CURRENT),
                                      m_adjusted_point(1.0),
                                      m_trend_type(TYPE_TREND_FLAT),
                                      m_every_tick(false),
                                      m_used_series(0),
                                      m_open(NULL),
                                      m_high(NULL),
                                      m_low(NULL),
                                      m_close(NULL),
                                      m_spread(NULL),
                                      m_time(NULL),
                                      m_tick_volume(NULL),
                                      m_real_volume(NULL)

  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
void CExpertBase::~CExpertBase(void)
  {
//--- if the symbol is "custom", delete it
   if(m_other_symbol && m_symbol!=NULL)
      delete m_symbol;
//--- release of "custom" timeseries
   if(m_other_symbol || m_other_period)
     {
      if(IS_OPEN_SERIES_USAGE && CheckPointer(m_open)==POINTER_DYNAMIC)
         delete m_open;
      if(IS_HIGH_SERIES_USAGE && CheckPointer(m_high)==POINTER_DYNAMIC)
         delete m_high;
      if(IS_LOW_SERIES_USAGE && CheckPointer(m_low)==POINTER_DYNAMIC)
         delete m_low;
      if(IS_CLOSE_SERIES_USAGE && CheckPointer(m_close)==POINTER_DYNAMIC)
         delete m_close;
      if(IS_SPREAD_SERIES_USAGE && CheckPointer(m_spread)==POINTER_DYNAMIC)
         delete m_spread;
      if(IS_TIME_SERIES_USAGE && CheckPointer(m_time)==POINTER_DYNAMIC)
         delete m_time;
      if(IS_TICK_VOLUME_SERIES_USAGE && CheckPointer(m_tick_volume)==POINTER_DYNAMIC)
         delete m_tick_volume;
      if(IS_REAL_VOLUME_SERIES_USAGE && CheckPointer(m_real_volume)==POINTER_DYNAMIC)
         delete m_real_volume;
     }
  }
//+------------------------------------------------------------------+
//| Get flags of used timeseries                                     |
//+------------------------------------------------------------------+
int CExpertBase::UsedSeries(void) const
  {
   if(m_other_symbol || m_other_period)
      return(0);
//---
   return(m_used_series);
  }
//+------------------------------------------------------------------+
//| Initialization of object.                                        |
//+------------------------------------------------------------------+
bool CExpertBase::Init(CSymbolInfo *symbol,ENUM_TIMEFRAMES period,double point)
  {
//--- check the initialization phase
   if(m_init_phase!=INIT_PHASE_FIRST)
     {
      Print(__FUNCTION__+": attempt of re-initialization");
      return(false);
     }
//--- check of pointer
   if(symbol==NULL)
     {
      Print(__FUNCTION__+": error initialization");
      return(false);
     }
//--- initialization
   m_symbol        =symbol;
   m_period        =period;
   m_adjusted_point=point;
   m_other_symbol  =false;
   m_other_period  =false;
   SetMarginMode();
//--- primary initialization is successful, pass to the phase of tuning
   m_init_phase=INIT_PHASE_TUNING;
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Changing work symbol.                                            |
//+------------------------------------------------------------------+
bool CExpertBase::Symbol(string name)
  {
//--- check the initialization phase
   if(m_init_phase!=INIT_PHASE_TUNING)
     {
      Print(__FUNCTION__+": changing of symbol is forbidden");
      return(false);
     }
   if(m_symbol!=NULL)
     {
      //--- symbol has been already set
      if(m_symbol.Name()==name)
         return(true);
      //--- symbol is not the one required, but is already "custom"
      if(m_other_symbol)
        {
         if(!m_symbol.Name(name))
           {
            //--- failed to initialize the symbol
            delete m_symbol;
            return(false);
           }
         return(true);
        }
     }
   m_symbol=new CSymbolInfo;
//--- check of pointer
   if(m_symbol==NULL)
     {
      Print(__FUNCTION__+": error of changing of symbol");
      return(false);
     }
   if(!m_symbol.Name(name))
     {
      //--- failed to initialize the symbol
      delete m_symbol;
      return(false);
     }
   m_other_symbol=true;
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Changing work timeframe.                                         |
//+------------------------------------------------------------------+
bool CExpertBase::Period(ENUM_TIMEFRAMES value)
  {
//--- check the initialization phase
   if(m_init_phase!=INIT_PHASE_TUNING)
     {
      Print(__FUNCTION__+": changing of timeframe is forbidden");
      return(false);
     }
   if(m_period==value)
      return(true);
//--- change work timeframe
   m_period=value;
   m_other_period=true;
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Checking adjustable parameters                                   |
//+------------------------------------------------------------------+
bool CExpertBase::ValidationSettings()
  {
//--- rechecking parameters
   if(m_init_phase==INIT_PHASE_VALIDATION)
      return(true);
//--- check the initialization phase
   if(m_init_phase!=INIT_PHASE_TUNING)
     {
      Print(__FUNCTION__+": not the right time to check parameters");
      return(false);
     }
//--- initial check of parameters is successful, phase of tuning is over
   m_init_phase=INIT_PHASE_VALIDATION;
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Setting pointers of price timeseries.                            |
//+------------------------------------------------------------------+
bool CExpertBase::SetPriceSeries(CiOpen *open,CiHigh *high,CiLow *low,CiClose *close)
  {
//--- check the initialization phase
   if(m_init_phase!=INIT_PHASE_VALIDATION)
     {
      Print(__FUNCTION__+": changing of timeseries is forbidden");
      return(false);
     }
//--- check pointers
   if((IS_OPEN_SERIES_USAGE  && open==NULL) ||
      (IS_HIGH_SERIES_USAGE  && high==NULL) ||
      (IS_LOW_SERIES_USAGE   && low==NULL)  || 
      (IS_CLOSE_SERIES_USAGE && close==NULL))
     {
      Print(__FUNCTION__+": NULL pointer");
      return(false);
     }
   m_open =open;
   m_high =high;
   m_low  =low;
   m_close=close;
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Setting pointers of other timeseries.                            |
//+------------------------------------------------------------------+
bool CExpertBase::SetOtherSeries(CiSpread *spread,CiTime *time,CiTickVolume *tick_volume,CiRealVolume *real_volume)
  {
//--- check the initialization phase
   if(m_init_phase!=INIT_PHASE_VALIDATION)
     {
      Print(__FUNCTION__+": changing of timeseries is forbidden");
      return(false);
     }
//--- check pointers
   if((IS_SPREAD_SERIES_USAGE && spread==NULL) || 
      (IS_TIME_SERIES_USAGE && time==NULL) || 
      (IS_TICK_VOLUME_SERIES_USAGE && tick_volume==NULL) ||
      (IS_REAL_VOLUME_SERIES_USAGE && real_volume==NULL))
     {
      Print(__FUNCTION__+": NULL pointer");
      return(false);
     }
   m_spread     =spread;
   m_time       =time;
   m_tick_volume=tick_volume;
   m_real_volume=real_volume;
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialization of indicators and timeseries.                     |
//+------------------------------------------------------------------+
bool CExpertBase::InitIndicators(CIndicators *indicators)
  {
//--- this call is for compatibility with the previous version
   if(!ValidationSettings())
      return(false);
//--- check the initialization phase
   if(m_init_phase!=INIT_PHASE_VALIDATION)
     {
      Print(__FUNCTION__+": parameters of setting are not checked");
      return(false);
     }
   if(!m_other_symbol && !m_other_period)
      return(true);
//--- check pointers
   if(m_symbol==NULL)
      return(false);
   if(indicators==NULL)
      return(false);
//--- initialization of required timeseries
   if(IS_OPEN_SERIES_USAGE && !InitOpen(indicators))
      return(false);
   if(IS_HIGH_SERIES_USAGE && !InitHigh(indicators))
      return(false);
   if(IS_LOW_SERIES_USAGE && !InitLow(indicators))
      return(false);
   if(IS_CLOSE_SERIES_USAGE && !InitClose(indicators))
      return(false);
   if(IS_SPREAD_SERIES_USAGE && !InitSpread(indicators))
      return(false);
   if(IS_TIME_SERIES_USAGE && !InitTime(indicators))
      return(false);
   if(IS_TICK_VOLUME_SERIES_USAGE && !InitTickVolume(indicators))
      return(false);
   if(IS_REAL_VOLUME_SERIES_USAGE && !InitRealVolume(indicators))
      return(false);
//--- initialization of object (from the point of view of the base class) has been performed successfully
//--- now it's impossible to change anything in the settings
   m_init_phase=INIT_PHASE_COMPLETE;
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Access to data of the Open timeseries.                           |
//+------------------------------------------------------------------+
double CExpertBase::Open(int ind) const
  {
//--- check pointer
   if(m_open==NULL)
      return(EMPTY_VALUE);
//--- return the result
   return(m_open.GetData(ind));
  }
//+------------------------------------------------------------------+
//| Access to data of the High timeseries.                           |
//+------------------------------------------------------------------+
double CExpertBase::High(int ind) const
  {
//--- check pointer
   if(m_high==NULL)
      return(EMPTY_VALUE);
//--- return the result
   return(m_high.GetData(ind));
  }
//+------------------------------------------------------------------+
//| Access to data of the Low timeseries.                            |
//+------------------------------------------------------------------+
double CExpertBase::Low(int ind) const
  {
//--- check pointer
   if(m_low==NULL)
      return(EMPTY_VALUE);
//--- return the result
   return(m_low.GetData(ind));
  }
//+------------------------------------------------------------------+
//| Access to data of the Close timeseries.                          |
//+------------------------------------------------------------------+
double CExpertBase::Close(int ind) const
  {
//--- check pointer
   if(m_close==NULL)
      return(EMPTY_VALUE);
//--- return the result
   return(m_close.GetData(ind));
  }
//+------------------------------------------------------------------+
//| Access to data of the Spread timeseries.                         |
//+------------------------------------------------------------------+
int CExpertBase::Spread(int ind) const
  {
//--- check pointer
   if(m_spread==NULL)
      return(INT_MAX);
//--- return the result
   return(m_spread.GetData(ind));
  }
//+------------------------------------------------------------------+
//| Access to data of the Time timeseries.                           |
//+------------------------------------------------------------------+
datetime CExpertBase::Time(int ind) const
  {
//--- check pointer
   if(m_time==NULL)
      return(0);
//--- return the result
   return(m_time.GetData(ind));
  }
//+------------------------------------------------------------------+
//| Access to data of the TickVolume timeseries.                     |
//+------------------------------------------------------------------+
long CExpertBase::TickVolume(int ind) const
  {
//--- check pointer
   if(m_tick_volume==NULL)
      return(0);
//--- return the result
   return(m_tick_volume.GetData(ind));
  }
//+------------------------------------------------------------------+
//| Access to data of the RealVolume timeseries.                     |
//+------------------------------------------------------------------+
long CExpertBase::RealVolume(int ind) const
  {
//--- check pointer
   if(m_real_volume==NULL)
      return(0);
//--- return the result
   return(m_real_volume.GetData(ind));
  }
//+------------------------------------------------------------------+
//| Initialization of the Open timeseries.                           |
//+------------------------------------------------------------------+
bool CExpertBase::InitOpen(CIndicators *indicators)
  {
//--- create object
   if((m_open=new CiOpen)==NULL)
     {
      Print(__FUNCTION__+": error creating object");
      return(false);
     }
//--- add object to collection
   if(!indicators.Add(m_open))
     {
      Print(__FUNCTION__+": error adding object");
      delete m_open;
      return(false);
     }
//--- initialize object
   if(!m_open.Create(m_symbol.Name(),m_period))
     {
      Print(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialization of the High timeseries.                           |
//+------------------------------------------------------------------+
bool CExpertBase::InitHigh(CIndicators *indicators)
  {
//--- create object
   if((m_high=new CiHigh)==NULL)
     {
      Print(__FUNCTION__+": error creating object");
      return(false);
     }
//--- add object to collection
   if(!indicators.Add(m_high))
     {
      Print(__FUNCTION__+": error adding object");
      delete m_high;
      return(false);
     }
//--- initialize object
   if(!m_high.Create(m_symbol.Name(),m_period))
     {
      Print(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialization of the Low timeseries.                            |
//+------------------------------------------------------------------+
bool CExpertBase::InitLow(CIndicators *indicators)
  {
//--- create object
   if((m_low=new CiLow)==NULL)
     {
      Print(__FUNCTION__+": error creating object");
      return(false);
     }
//--- add object to collection
   if(!indicators.Add(m_low))
     {
      Print(__FUNCTION__+": error adding object");
      delete m_low;
      return(false);
     }
//--- initialize object
   if(!m_low.Create(m_symbol.Name(),m_period))
     {
      Print(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialization of the Close timeseries.                          |
//+------------------------------------------------------------------+
bool CExpertBase::InitClose(CIndicators *indicators)
  {
//--- create object
   if((m_close=new CiClose)==NULL)
     {
      Print(__FUNCTION__+": error creating object");
      return(false);
     }
//--- add object to collection
   if(!indicators.Add(m_close))
     {
      Print(__FUNCTION__+": error adding object");
      delete m_close;
      return(false);
     }
//--- initialize object
   if(!m_close.Create(m_symbol.Name(),m_period))
     {
      Print(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialization of the Spread timeseries.                         |
//+------------------------------------------------------------------+
bool CExpertBase::InitSpread(CIndicators *indicators)
  {
//--- create object
   if((m_spread=new CiSpread)==NULL)
     {
      Print(__FUNCTION__+": error creating object");
      return(false);
     }
//--- add object to collection
   if(!indicators.Add(m_spread))
     {
      Print(__FUNCTION__+": error adding object");
      delete m_spread;
      return(false);
     }
//--- initialize object
   if(!m_spread.Create(m_symbol.Name(),m_period))
     {
      Print(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialization of the Time timeseries.                           |
//+------------------------------------------------------------------+
bool CExpertBase::InitTime(CIndicators *indicators)
  {
//--- create object
   if((m_time=new CiTime)==NULL)
     {
      Print(__FUNCTION__+": error creating object");
      return(false);
     }
//--- add object to collection
   if(!indicators.Add(m_time))
     {
      Print(__FUNCTION__+": error adding object");
      delete m_time;
      return(false);
     }
//--- initialize object
   if(!m_time.Create(m_symbol.Name(),m_period))
     {
      Print(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialization of the TickVolume timeseries.                     |
//+------------------------------------------------------------------+
bool CExpertBase::InitTickVolume(CIndicators *indicators)
  {
//--- create object
   if((m_tick_volume=new CiTickVolume)==NULL)
     {
      Print(__FUNCTION__+": error creating object");
      return(false);
     }
//--- add object to collection
   if(!indicators.Add(m_tick_volume))
     {
      Print(__FUNCTION__+": error adding object");
      delete m_tick_volume;
      return(false);
     }
//--- initialize object
   if(!m_tick_volume.Create(m_symbol.Name(),m_period))
     {
      Print(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialization of the RealVolume timeseries.                     |
//+------------------------------------------------------------------+
bool CExpertBase::InitRealVolume(CIndicators *indicators)
  {
//--- create object
   if((m_real_volume=new CiRealVolume)==NULL)
     {
      Print(__FUNCTION__+": error creating object");
      return(false);
     }
//--- add object to collection
   if(!indicators.Add(m_real_volume))
     {
      Print(__FUNCTION__+": error adding object");
      delete m_real_volume;
      return(false);
     }
//--- initialize object
   if(!m_real_volume.Create(m_symbol.Name(),m_period))
     {
      Print(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
