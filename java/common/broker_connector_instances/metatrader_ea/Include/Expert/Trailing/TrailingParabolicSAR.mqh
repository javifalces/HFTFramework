//+------------------------------------------------------------------+
//|                                         TrailingParabolicSAR.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertTrailing.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Trailing Stop based on Parabolic SAR                       |
//| Type=Trailing                                                    |
//| Name=ParabolicSAR                                                |
//| Class=CTrailingPSAR                                              |
//| Page=                                                            |
//| Parameter=Step,double,0.02,Speed increment                       |
//| Parameter=Maximum,double,0.2,Maximum rate                        |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CTrailingPSAR.                                             |
//| Appointment: Class traling stops with Parabolic SAR.             |
//| Derives from class CExpertTrailing.                              |
//+------------------------------------------------------------------+
class CTrailingPSAR : public CExpertTrailing
  {
protected:
   CiSAR             m_sar;            // object-indicator
   //--- adjusted parameters
   double            m_step;           // the "speed increment" parameter of the indicator
   double            m_maximum;        // the "maximum rate" parameter of the indicator

public:
                     CTrailingPSAR(void);
                    ~CTrailingPSAR(void);
   //--- methods of setting adjustable parameters
   void              Step(double step)       { m_step=step;       }
   void              Maximum(double maximum) { m_maximum=maximum; }
   //--- method of creating the indicator and timeseries
   virtual bool      InitIndicators(CIndicators *indicators);
   //---
   virtual bool      CheckTrailingStopLong(CPositionInfo *position,double &sl,double &tp);
   virtual bool      CheckTrailingStopShort(CPositionInfo *position,double &sl,double &tp);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CTrailingPSAR::CTrailingPSAR(void) : m_step(0.02),
                                          m_maximum(0.2)

  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
void CTrailingPSAR::~CTrailingPSAR(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicators.                                               |
//+------------------------------------------------------------------+
bool CTrailingPSAR::InitIndicators(CIndicators *indicators)
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
//| Checking trailing stop and/or profit for long position.          |
//+------------------------------------------------------------------+
bool CTrailingPSAR::CheckTrailingStopLong(CPositionInfo *position,double &sl,double &tp)
  {
//--- check
   if(position==NULL)
      return(false);
//---
   double level =NormalizeDouble(m_symbol.Bid()-m_symbol.StopsLevel()*m_symbol.Point(),m_symbol.Digits());
   double new_sl=NormalizeDouble(m_sar.Main(1),m_symbol.Digits());
   double pos_sl=position.StopLoss();
   double base  =(pos_sl==0.0) ? position.PriceOpen() : pos_sl;
//---
   sl=EMPTY_VALUE;
   tp=EMPTY_VALUE;
   if(new_sl>base && new_sl<level)
      sl=new_sl;
//---
   return(sl!=EMPTY_VALUE);
  }
//+------------------------------------------------------------------+
//| Checking trailing stop and/or profit for short position.         |
//+------------------------------------------------------------------+
bool CTrailingPSAR::CheckTrailingStopShort(CPositionInfo *position,double &sl,double &tp)
  {
//--- check
   if(position==NULL)
      return(false);
//---
   double level =NormalizeDouble(m_symbol.Ask()+m_symbol.StopsLevel()*m_symbol.Point(),m_symbol.Digits());
   double new_sl=NormalizeDouble(m_sar.Main(1)+m_symbol.Spread()*m_symbol.Point(),m_symbol.Digits());
   double pos_sl=position.StopLoss();
   double base  =(pos_sl==0.0) ? position.PriceOpen() : pos_sl;
//---
   sl=EMPTY_VALUE;
   tp=EMPTY_VALUE;
   if(new_sl<base && new_sl>level)
      sl=new_sl;
//---
   return(sl!=EMPTY_VALUE);
  }
//+------------------------------------------------------------------+
