//+------------------------------------------------------------------+
//|                                                    SignalRVI.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertSignal.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Signals of oscillator 'Relative Vigor Index'               |
//| Type=SignalAdvanced                                              |
//| Name=Relative Vigor Index                                        |
//| ShortName=RVI                                                    |
//| Class=CSignalRVI                                                 |
//| Page=signal_rvi                                                  |
//| Parameter=PeriodRVI,int,10,Period of calculation                 |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CSignalRVI.                                                |
//| Purpose: Class of generator of trade signals based on            |
//|          the 'Relative Vigor Index' oscillator.                  |
//| Is derived from the CExpertSignal class.                         |
//+------------------------------------------------------------------+
class CSignalRVI : public CExpertSignal
  {
protected:
   CiRVI             m_rvi;            // object-oscillator
   //--- adjusted parameters
   int               m_periodRVI;      // the "period of calculation" parameter of the oscillator
   //--- "weights" of market models (0-100)
   int               m_pattern_0;      // model 0 "the oscillator has required direction"
   int               m_pattern_1;      // model 1 "crossing of main and signal line"

public:
                     CSignalRVI(void);
                    ~CSignalRVI(void);
   //--- methods of setting adjustable parameters
   void              PeriodRVI(int value)            { m_periodRVI=value;                 }
   //--- methods of adjusting "weights" of market models
   void              Pattern_0(int value)            { m_pattern_0=value;                 }
   void              Pattern_1(int value)            { m_pattern_1=value;                 }
   //--- method of verification of settings
   virtual bool      ValidationSettings(void);
   //--- method of creating the indicator and timeseries
   virtual bool      InitIndicators(CIndicators *indicators);
   //--- methods of checking if the market models are formed
   virtual int       LongCondition(void);
   virtual int       ShortCondition(void);

protected:
   //--- method of initialization of the oscillator
   bool              InitRVI(CIndicators *indicators);
   //--- methods of getting data
   double            Main(int ind)                   { return(m_rvi.Main(ind));           }
   double            DiffMain(int ind)               { return(Main(ind)-Main(ind+1));     }
   double            Signal(int ind)                 { return(m_rvi.Signal(ind));         }
   double            DiffSignal(int ind)             { return(Signal(ind)-Signal(ind+1)); }
   double            DiffMainSignal(int ind)         { return(Main(ind)-Signal(ind));     }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSignalRVI::CSignalRVI(void) : m_periodRVI(10),
                               m_pattern_0(60),
                               m_pattern_1(100)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSignalRVI::~CSignalRVI(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CSignalRVI::ValidationSettings(void)
  {
//--- validation settings of additional filters
   if(!CExpertSignal::ValidationSettings())
      return(false);
//--- initial data checks
   if(m_periodRVI<=0)
     {
      printf(__FUNCTION__+": the period of calculation of the RVI oscillator must be greater than 0");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Create indicators.                                               |
//+------------------------------------------------------------------+
bool CSignalRVI::InitIndicators(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- initialization of indicators and timeseries of additional filters
   if(!CExpertSignal::InitIndicators(indicators))
      return(false);
//--- create and initialize RVI oscillator
   if(!InitRVI(indicators))
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize RVI oscillators.                                      |
//+------------------------------------------------------------------+
bool CSignalRVI::InitRVI(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- add object to collection
   if(!indicators.Add(GetPointer(m_rvi)))
     {
      printf(__FUNCTION__+": error adding object");
      return(false);
     }
//--- initialize object
   if(!m_rvi.Create(m_symbol.Name(),m_period,m_periodRVI))
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
int CSignalRVI::LongCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//---
   if(DiffMain(idx)>0.0)
     {
      //--- the main line of the oscillator is directed upwards confirming the possibility of price growth
      if(IS_PATTERN_USAGE(0))
         result=m_pattern_0;      // "confirming" signal
      //--- if the main line crosses the signal line upwards, this is a signal for buying
      if(DiffMainSignal(idx)>0 && DiffMainSignal(idx+1)<0)
        {
         //--- the main line of the oscillator has crossed the signal line upwards (signal for buying)
         if(IS_PATTERN_USAGE(1))
            result=m_pattern_1;   // signal number 1
        }
     }
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
//| "Voting" that price will fall.                                   |
//+------------------------------------------------------------------+
int CSignalRVI::ShortCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//---
   if(DiffMain(idx)<0.0)
     {
      //--- the main line of the oscillator is directed downwards confirming the possibility of falling of price
      if(IS_PATTERN_USAGE(0))
         result=m_pattern_0;      // "confirming" signal
      //--- if the main line crosses the signal line from top downwards, this is a signal for selling
      if(DiffMainSignal(idx)<0 && DiffMainSignal(idx+1)>0)
        {
         //--- the main line of the oscillator has crossed the signal line from top downwards (signal for selling)
         if(IS_PATTERN_USAGE(1))
            result=m_pattern_1;   // signal number 1
        }
     }
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
