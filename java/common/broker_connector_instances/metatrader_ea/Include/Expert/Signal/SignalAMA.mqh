//+------------------------------------------------------------------+
//|                                                    SignalAMA.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertSignal.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Signals of indicator 'Adaptive Moving Average'             |
//| Type=SignalAdvanced                                              |
//| Name=Adaptive Moving Average                                     |
//| ShortName=AMA                                                    |
//| Class=CSignalAMA                                                 |
//| Page=signal_ama                                                  |
//| Parameter=PeriodMA,int,10,Period of averaging                    |
//| Parameter=PeriodFast,int,2,Period of fast EMA                    |
//| Parameter=PeriodSlow,int,30,Period of slow EMA                   |
//| Parameter=Shift,int,0,Time shift                                 |
//| Parameter=Applied,ENUM_APPLIED_PRICE,PRICE_CLOSE,Prices series   |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CSignalAMA.                                                |
//| Purpose: Class of generator of trade signals based on            |
//|          the 'Adaptive Moving Average' indicator.                |
//| Is derived from the CExpertSignal class.                         |
//+------------------------------------------------------------------+
class CSignalAMA : public CExpertSignal
  {
protected:
   CiAMA             m_ma;             // object-indicator
   //--- adjusted parameters
   int               m_ma_period;      // the "period of averaging" parameter of the indicator
   int               m_period_fast;    // the "period of fast EMA" parameter of the indicator
   int               m_period_slow;    // the "period of slow EMA" parameter of the indicator
   int               m_ma_shift;       // the "time shift" parameter of the indicator
   ENUM_APPLIED_PRICE m_ma_applied;    // the "object of averaging" parameter" of the indicator
   //--- "weights" of market models (0-100)
   int               m_pattern_0;      // model 0 "price is on the necessary side from the indicator"
   int               m_pattern_1;      // model 1 "price crossed the indicator with opposite direction"
   int               m_pattern_2;      // model 2 "price crossed the indicator with the same direction"
   int               m_pattern_3;      // model 3 "piercing"

public:
                     CSignalAMA(void);
                    ~CSignalAMA(void);
   //--- methods of setting adjustable parameters
   void              PeriodMA(int value)                 { m_ma_period=value;          }
   void              PeriodFast(int value)               { m_period_fast=value;        }
   void              PeriodSlow(int value)               { m_period_slow=value;        }
   void              Shift(int value)                    { m_ma_shift=value;           }
   void              Applied(ENUM_APPLIED_PRICE value)   { m_ma_applied=value;         }
   //--- methods of adjusting "weights" of market models
   void              Pattern_0(int value)                { m_pattern_0=value;          }
   void              Pattern_1(int value)                { m_pattern_1=value;          }
   void              Pattern_2(int value)                { m_pattern_2=value;          }
   void              Pattern_3(int value)                { m_pattern_3=value;          }
   //--- method of verification of settings
   virtual bool      ValidationSettings(void);
   //--- method of creating the indicator and timeseries
   virtual bool      InitIndicators(CIndicators *indicators);
   //--- methods of checking if the market models are formed
   virtual int       LongCondition(void);
   virtual int       ShortCondition(void);

protected:
   //--- method of initialization of the indicator
   bool              InitMA(CIndicators *indicators);
   //--- methods of getting data
   double            MA(int ind)                         { return(m_ma.Main(ind));     }
   double            DiffMA(int ind)                     { return(MA(ind)-MA(ind+1));  }
   double            DiffOpenMA(int ind)                 { return(Open(ind)-MA(ind));  }
   double            DiffHighMA(int ind)                 { return(High(ind)-MA(ind));  }
   double            DiffLowMA(int ind)                  { return(Low(ind)-MA(ind));   }
   double            DiffCloseMA(int ind)                { return(Close(ind)-MA(ind)); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSignalAMA::CSignalAMA(void) : m_ma_period(10),
                               m_ma_shift(0),
                               m_period_fast(2),
                               m_period_slow(30),
                               m_ma_applied(PRICE_CLOSE),
                               m_pattern_0(10),
                               m_pattern_1(70),
                               m_pattern_2(100),
                               m_pattern_3(60)
  {
//--- initialization of protected data
   m_used_series=USE_SERIES_OPEN+USE_SERIES_HIGH+USE_SERIES_LOW+USE_SERIES_CLOSE;
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSignalAMA::~CSignalAMA(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CSignalAMA::ValidationSettings(void)
  {
//--- call of the method of the parent class
   if(!CExpertSignal::ValidationSettings())
      return(false);
//--- initial data checks
   if(m_ma_period<=0)
     {
      printf(__FUNCTION__+": period MA must be greater than 0");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Create indicators.                                               |
//+------------------------------------------------------------------+
bool CSignalAMA::InitIndicators(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- initialization of indicators and timeseries of additional filters
   if(!CExpertSignal::InitIndicators(indicators))
      return(false);
//--- create and initialize AMA indicator
   if(!InitMA(indicators))
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Create MA indicators.                                            |
//+------------------------------------------------------------------+
bool CSignalAMA::InitMA(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- add object to collection
   if(!indicators.Add(GetPointer(m_ma)))
     {
      printf(__FUNCTION__+": error adding object");
      return(false);
     }
//--- initialize object
   if(!m_ma.Create(m_symbol.Name(),m_period,m_ma_period,m_period_fast,m_period_slow,m_ma_shift,m_ma_applied))
     {
      printf(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| "Voting" that price will grow.                                   |
//+------------------------------------------------------------------+
int CSignalAMA::LongCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- analyze positional relationship of the close price and the indicator at the first analyzed bar
   if(DiffCloseMA(idx)<0.0)
     {
      //--- the close price is below the indicator
      if(IS_PATTERN_USAGE(1) && DiffOpenMA(idx)>0.0 && DiffMA(idx)>0.0)
        {
         //--- the open price is above the indicator (i.e. there was an intersection), but the indicator is directed upwards
         result=m_pattern_1;
         //--- consider that this is an unformed "piercing" and suggest to enter the market at the current price
         m_base_price=0.0;
        }
     }
   else
     {
      //--- the close price is above the indicator (the indicator has no objections to buying)
      if(IS_PATTERN_USAGE(0))
         result=m_pattern_0;
      //--- if the indicator is directed upwards
      if(DiffMA(idx)>0.0)
        {
         if(DiffOpenMA(idx)<0.0)
           {
            //--- if the model 2 is used
            if(IS_PATTERN_USAGE(2))
              {
               //--- the open price is below the indicator (i.e. there was an intersection)
               result=m_pattern_2;
               //--- suggest to enter the market at the "roll back"
               m_base_price=m_symbol.NormalizePrice(MA(idx));
              }
           }
         else
           {
            //--- if the model 3 is used and the open price is above the indicator
            if(IS_PATTERN_USAGE(3) && DiffLowMA(idx)<0.0)
              {
               //--- the low price is below the indicator
               result=m_pattern_3;
               //--- consider that this is a formed "piercing" and suggest to enter the market at the current price
               m_base_price=0.0;
              }
           }
        }
     }
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
//| "Voting" that price will fall.                                   |
//+------------------------------------------------------------------+
int CSignalAMA::ShortCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- analyze positional relationship of the close price and the indicator at the first analyzed bar
   if(DiffCloseMA(idx)>0.0)
     {
      //--- the close price is above the indicator
      if(IS_PATTERN_USAGE(1) && DiffOpenMA(idx)<0.0 && DiffMA(idx)<0.0)
        {
         //--- the open price is below the indicator (i.e. there was an intersection), but the indicator is directed downwards
         result=m_pattern_1;
         //--- consider that this is an unformed "piercing" and suggest to enter the market at the current price
         m_base_price=0.0;
        }
     }
   else
     {
      //--- the close price is below the indicator (the indicator has no objections to buying)
      if(IS_PATTERN_USAGE(0))
         result=m_pattern_0;
      //--- the indicator is directed downwards
      if(DiffMA(idx)<0.0)
        {
         if(DiffOpenMA(idx)>0.0)
           {
            //--- if the model 2 is used
            if(IS_PATTERN_USAGE(2))
              {
               //--- the open price is above the indicator (i.e. there was an intersection)
               result=m_pattern_2;
               //--- suggest to enter the market at the "roll back"
               m_base_price=m_symbol.NormalizePrice(MA(idx));
              }
           }
         else
           {
            //--- if the model 3 is used and the open price is below the indicator
            if(IS_PATTERN_USAGE(3) && DiffHighMA(idx)>0.0)
              {
               //--- the high price is above the indicator
               result=m_pattern_3;
               //--- consider that this is a formed "piercing" and suggest to enter the market at the current price
               m_base_price=0.0;
              }
           }
        }
     }
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
