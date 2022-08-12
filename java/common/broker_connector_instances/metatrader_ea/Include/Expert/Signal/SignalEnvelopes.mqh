//+------------------------------------------------------------------+
//|                                              SignalEnvelopes.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertSignal.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Signals of indicator 'Envelopes'                           |
//| Type=SignalAdvanced                                              |
//| Name=Envelopes                                                   |
//| ShortName=Envelopes                                              |
//| Class=CSignalEnvelopes                                           |
//| Page=signal_envelopes                                            |
//| Parameter=PeriodMA,int,45,Period of averaging                    |
//| Parameter=Shift,int,0,Time shift                                 |
//| Parameter=Method,ENUM_MA_METHOD,MODE_SMA,Method of averaging     |
//| Parameter=Applied,ENUM_APPLIED_PRICE,PRICE_CLOSE,Prices series   |
//| Parameter=Deviation,double,0.15,Deviation                        |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CSignalEnvelopes.                                          |
//| Purpose: Class of generator of trade signals based on            |
//|          the 'Envelopes' indicator.                              |
//| Is derived from the CExpertSignal class.                         |
//+------------------------------------------------------------------+
class CSignalEnvelopes : public CExpertSignal
  {
protected:
   CiEnvelopes       m_env;            // object-indicator
   //--- adjusted parameters
   int               m_ma_period;      // the "period of averaging" parameter of the indicator
   int               m_ma_shift;       // the "time shift" parameter of the indicator
   ENUM_MA_METHOD    m_ma_method;      // the "method of averaging" parameter of the indicator
   ENUM_APPLIED_PRICE m_ma_applied;    // the "object of averaging" parameter of the indicator
   double            m_deviation;      // the "deviation" parameter of the indicator
   double            m_limit_in;       // threshold sensitivity of the 'rollback zone'
   double            m_limit_out;      // threshold sensitivity of the 'break through zone'
   //--- "weights" of market models (0-100)
   int               m_pattern_0;      // model 0 "price is near the necessary border of the envelope"
   int               m_pattern_1;      // model 1 "price crossed a border of the envelope"

public:
                     CSignalEnvelopes(void);
                    ~CSignalEnvelopes(void);
   //--- methods of setting adjustable parameters
   void              PeriodMA(int value)                 { m_ma_period=value;        }
   void              Shift(int value)                    { m_ma_shift=value;         }
   void              Method(ENUM_MA_METHOD value)        { m_ma_method=value;        }
   void              Applied(ENUM_APPLIED_PRICE value)   { m_ma_applied=value;       }
   void              Deviation(double value)             { m_deviation=value;        }
   void              LimitIn(double value)               { m_limit_in=value;         }
   void              LimitOut(double value)              { m_limit_out=value;        }
   //--- methods of adjusting "weights" of market models
   void              Pattern_0(int value)                { m_pattern_0=value;        }
   void              Pattern_1(int value)                { m_pattern_1=value;        }
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
   double            Upper(int ind)                      { return(m_env.Upper(ind)); }
   double            Lower(int ind)                      { return(m_env.Lower(ind)); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSignalEnvelopes::CSignalEnvelopes(void) : m_ma_period(45),
                                           m_ma_shift(0),
                                           m_ma_method(MODE_SMA),
                                           m_ma_applied(PRICE_CLOSE),
                                           m_deviation(0.15),
                                           m_limit_in(0.2),
                                           m_limit_out(0.2),
                                           m_pattern_0(90),
                                           m_pattern_1(70)
  {
//--- initialization of protected data
   m_used_series=USE_SERIES_OPEN+USE_SERIES_HIGH+USE_SERIES_LOW+USE_SERIES_CLOSE;
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSignalEnvelopes::~CSignalEnvelopes(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CSignalEnvelopes::ValidationSettings(void)
  {
//--- validation settings of additional filters
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
bool CSignalEnvelopes::InitIndicators(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- initialization of indicators and timeseries of additional filters
   if(!CExpertSignal::InitIndicators(indicators))
      return(false);
//--- create and initialize MA indicator
   if(!InitMA(indicators))
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize MA indicators.                                        |
//+------------------------------------------------------------------+
bool CSignalEnvelopes::InitMA(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- add object to collection
   if(!indicators.Add(GetPointer(m_env)))
     {
      printf(__FUNCTION__+": error adding object");
      return(false);
     }
//--- initialize object
   if(!m_env.Create(m_symbol.Name(),m_period,m_ma_period,m_ma_shift,m_ma_method,m_ma_applied,m_deviation))
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
int CSignalEnvelopes::LongCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
   double close=Close(idx);
   double upper=Upper(idx);
   double lower=Lower(idx);
   double width=upper-lower;
//--- if the model 0 is used and price is in the rollback zone, then there is a condition for buying
   if(IS_PATTERN_USAGE(0) && close<lower+m_limit_in*width && close>lower-m_limit_out*width)
      result=m_pattern_0;
//--- if the model 1 is used and price is above the rollback zone, then there is a condition for buying
   if(IS_PATTERN_USAGE(1) && close>upper+m_limit_out*width)
      result=m_pattern_1;
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
//| "Voting" that price will fall.                                   |
//+------------------------------------------------------------------+
int CSignalEnvelopes::ShortCondition(void)
  {
   int result  =0;
   int idx     =StartIndex();
   double close=Close(idx);
   double upper=Upper(idx);
   double lower=Lower(idx);
   double width=upper-lower;
//--- if the model 0 is used and price is in the rollback zone, then there is a condition for selling
   if(IS_PATTERN_USAGE(0) && close>upper-m_limit_in*width && close<upper+m_limit_out*width)
      result=m_pattern_0;
//--- if the model 1 is used and price is above the rollback zone, then there is a condition for selling
   if(IS_PATTERN_USAGE(1) && close<lower-m_limit_out*width)
      result=m_pattern_1;
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
