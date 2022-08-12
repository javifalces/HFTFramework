//+------------------------------------------------------------------+
//|                                                    SignalSAR.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertSignal.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Signals of indicator 'Parabolic SAR'                       |
//| Type=SignalAdvanced                                              |
//| Name=Parabolic SAR                                               |
//| ShortName=SAR                                                    |
//| Class=CSignalSAR                                                 |
//| Page=signal_sar                                                  |
//| Parameter=Step,double,0.02,Speed increment                       |
//| Parameter=Maximum,double,0.2,Maximum rate                        |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CSignalSAR.                                                |
//| Purpose: Class of generator of trade signals based on            |
//|          the 'Parabolic SAR' indicator.                          |
//| Is derived from the CExpertSignal class.                         |
//+------------------------------------------------------------------+
class CSignalSAR : public CExpertSignal
  {
protected:
   CiSAR             m_sar;            // object-indicator
   //--- adjusted parameters
   double            m_step;           // the "speed increment" parameter of the indicator
   double            m_maximum;        // the "maximum rate" parameter of the indicator
   //--- "weights" of market models (0-100)
   int               m_pattern_0;      // model 0 "the parabolic is on the necessary side from the price"
   int               m_pattern_1;      // model 1 "the parabolic has 'switched'"

public:
                     CSignalSAR(void);
                    ~CSignalSAR(void);
   //--- methods of setting adjustable parameters
   void              Step(double value)          { m_step=value;                 }
   void              Maximum(double value)       { m_maximum=value;              }
   //--- methods of adjusting "weights" of market models
   void              Pattern_0(int value)        { m_pattern_0=value;            }
   void              Pattern_1(int value)        { m_pattern_1=value;            }
   //--- method of verification of settings
   virtual bool      ValidationSettings(void);
   //--- method of creating the indicator and timeseries
   virtual bool      InitIndicators(CIndicators *indicators);
   //--- methods of checking if the market models are formed
   virtual int       LongCondition(void);
   virtual int       ShortCondition(void);

protected:
   //--- method of initialization of the indicator
   bool              InitSAR(CIndicators *indicators);
   //--- methods of getting data
   double            SAR(int ind)                { return(m_sar.Main(ind));      }
   double            Close(int ind)              { return(m_close.GetData(ind)); }
   double            DiffClose(int ind)          { return(Close(ind)-SAR(ind));  }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSignalSAR::CSignalSAR(void) : m_step(0.02),
                               m_maximum(0.2),
                               m_pattern_0(40),
                               m_pattern_1(90)
  {
//--- initialization of protected data
   m_used_series=USE_SERIES_CLOSE;
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSignalSAR::~CSignalSAR(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CSignalSAR::ValidationSettings(void)
  {
//--- call of the method of the parent class
   if(!CExpertSignal::ValidationSettings())
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Create indicators.                                               |
//+------------------------------------------------------------------+
bool CSignalSAR::InitIndicators(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- initialization of indicators and timeseries of additional filters
   if(!CExpertSignal::InitIndicators(indicators))
      return(false);
//--- create and initialize SAR indicator
   if(!InitSAR(indicators))
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Create SAR indicators.                                           |
//+------------------------------------------------------------------+
bool CSignalSAR::InitSAR(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- add object to collection
   if(!indicators.Add(GetPointer(m_sar)))
     {
      printf(__FUNCTION__+": error adding object");
      return(false);
     }
//--- initialize object
   if(!m_sar.Create(m_symbol.Name(),m_period,m_step,m_maximum))
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
int CSignalSAR::LongCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- if the indicator is above the price at the first analyzed bar, don't 'vote' buying
   if(DiffClose(idx++)<0.0)
      return(result);
//--- the indicator is below the price at the first analyzed bar (the indicator has no objections to buying)
   if(IS_PATTERN_USAGE(0))
      result=m_pattern_0;
//--- if the indicator is above the price at the second analyzed bar, then there is a condition for buying
   if(IS_PATTERN_USAGE(1) && DiffClose(idx)<0.0)
      return(m_pattern_1);
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
//| "Voting" that price will fall.                                   |
//+------------------------------------------------------------------+
int CSignalSAR::ShortCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- if the indicator is below the price at the first analyzed bar, don't "vote" for selling
   if(DiffClose(idx++)>0.0)
      return(result);
//--- the indicator is above the price at the first analyzed bar (the indicator has no objections to selling)
   if(IS_PATTERN_USAGE(0))
      result=m_pattern_0;
//--- if the indicator is below the price at the second analyzed bar, then there is a condition for selling
   if(IS_PATTERN_USAGE(1) && DiffClose(idx)>0.0)
      return(m_pattern_1);
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
