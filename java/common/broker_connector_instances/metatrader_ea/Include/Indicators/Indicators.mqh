//+------------------------------------------------------------------+
//|                                                   Indicators.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Trend.mqh"
#include "Oscilators.mqh"
#include "Volumes.mqh"
#include "BillWilliams.mqh"
#include "Custom.mqh"
#include "TimeSeries.mqh"
//+------------------------------------------------------------------+
//| Class CIndicators.                                               |
//| Purpose: Class for creation of collection of instances of        |
//|          technical indicators.                                   |
//+------------------------------------------------------------------+
class CIndicators : public CArrayObj
  {
protected:
   MqlDateTime       m_prev_time;

public:
                     CIndicators(void);
                    ~CIndicators(void);
   //--- method for creation
   CIndicator       *Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const ENUM_INDICATOR type,const int count,const MqlParam &params[]);
   bool              BufferResize(const int size);
   //--- method of refreshing of the data of all indicators in the collection
   int               Refresh(void);
protected:
   //--- method of formation of flags timeframes
   int               TimeframesFlags(const MqlDateTime &time);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CIndicators::CIndicators(void)
  {
   m_prev_time.min=-1;
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CIndicators::~CIndicators(void)
  {
  }
//+------------------------------------------------------------------+
//| Indicator creation                                               |
//+------------------------------------------------------------------+
CIndicator *CIndicators::Create(const string symbol,const ENUM_TIMEFRAMES period,
                                const ENUM_INDICATOR type,const int count,const MqlParam &params[])
  {
   CIndicator *result=NULL;
//---
   switch(type)
     {
      case IND_AC:
         //--- Identifier of "Accelerator Oscillator"
         if(count==0)
            result=new CiAC;
         break;
      case IND_AD:
         //--- Identifier of "Accumulation/Distribution"
         if(count==1)
            result=new CiAD;
         break;
      case IND_ALLIGATOR:
         //--- Identifier of "Alligator"
         if(count==8)
            result=new CiAlligator;
         break;
      case IND_ADX:
         //--- Identifier of "Average Directional Index"
         if(count==1)
            result=new CiADX;
         break;
      case IND_ADXW:
         //--- Identifier of "Average Directional Index by Welles Wilder"
         if(count==1)
            result=new CiADXWilder;
         break;
      case IND_ATR:
         //--- Identifier of "Average True Range"
         if(count==1)
            result=new CiATR;
         break;
      case IND_AO:
         //--- Identifier of "Awesome Oscillator"
         if(count==0)
            result=new CiAO;
         break;
      case IND_BEARS:
         //--- Identifier of "Bears Power"
         if(count==1)
            result=new CiBearsPower;
         break;
      case IND_BANDS:
         //--- Identifier of "Bollinger Bands"
         if(count==4)
            result=new CiBands;
         break;
      case IND_BULLS:
         //--- Identifier of "Bulls Power"
         if(count==1)
            result=new CiBullsPower;
         break;
      case IND_CCI:
         //--- Identifier of "Commodity Channel Index"
         if(count==2)
            result=new CiCCI;
         break;
      case IND_CHAIKIN:
         //--- Identifier of "Chaikin Oscillator"
         if(count==4)
            result=new CiChaikin;
         break;
      case IND_DEMARKER:
         //--- Identifier of "DeMarker"
         if(count==1)
            result=new CiDeMarker;
         break;
      case IND_ENVELOPES:
         //--- Identifier of "Envelopes"
         if(count==5)
            result=new CiEnvelopes;
         break;
      case IND_FORCE:
         //--- Identifier of "Force Index"
         if(count==3)
            result=new CiForce;
         break;
      case IND_FRACTALS:
         //--- Identifier of "Fractals"
         if(count==0)
            result=new CiFractals;
         break;
      case IND_GATOR:
         //--- Identifier of "Gator oscillator"
         if(count==8)
            result=new CiGator;
         break;
      case IND_ICHIMOKU:
         //--- Identifier of "Ichimoku Kinko Hyo"
         if(count==3)
            result=new CiIchimoku;
         break;
      case IND_MACD:
         //--- Identifier of "Moving Averages Convergence-Divergence"
         if(count==4)
            result=new CiMACD;
         break;
      case IND_BWMFI:
         //--- Identifier of "Market Facilitation Index by Bill Williams"
         if(count==1)
            result=new CiBWMFI;
         break;
      case IND_MOMENTUM:
         //--- Identifier of "Momentum"
         if(count==2)
            result=new CiMomentum;
         break;
      case IND_MFI:
         //--- Identifier of "Money Flow Index"
         if(count==2)
            result=new CiMFI;
         break;
      case IND_MA:
         //--- Identifier of "Moving Average"
         if(count==4)
            result=new CiMA;
         break;
      case IND_OSMA:
         //--- Identifier of "Moving Average of Oscillator (MACD histogram)"
         if(count==4)
            result=new CiOsMA;
         break;
      case IND_OBV:
         //--- Identifier of "On Balance Volume"
         if(count==1)
            result=new CiOBV;
         break;
      case IND_SAR:
         //--- Identifier of "Parabolic Stop And Reverse System"
         if(count==2)
            result=new CiSAR;
         break;
      case IND_RSI:
         //--- Identifier of "Relative Strength Index"
         if(count==2)
            result=new CiRSI;
         break;
      case IND_RVI:
         //--- Identifier of "Relative Vigor Index"
         if(count==1)
            result=new CiRVI;
         break;
      case IND_STDDEV:
         //--- Identifier of "Standard Deviation"
         if(count==4)
            result=new CiStdDev;
         break;
      case IND_STOCHASTIC:
         //--- Identifier of "Stochastic Oscillator"
         if(count==5)
            result=new CiStochastic;
         break;
      case IND_WPR:
         //--- Identifier of "Williams' Percent Range"
         if(count==1)
            result=new CiWPR;
         break;
      case IND_DEMA:
         //--- Identifier of "Double Exponential Moving Average"
         if(count==3)
            result=new CiDEMA;
         break;
      case IND_TEMA:
         //--- Identifier of "Triple Exponential Moving Average"
         if(count==3)
            result=new CiTEMA;
         break;
      case IND_TRIX:
         //--- Identifier of "Triple Exponential Moving Averages Oscillator"
         if(count==2)
            result=new CiTriX;
         break;
      case IND_FRAMA:
         //--- Identifier of "Fractal Adaptive Moving Average"
         if(count==3)
            result=new CiFrAMA;
         break;
      case IND_AMA:
         //--- Identifier of "Adaptive Moving Average"
         if(count==5)
            result=new CiAMA;
         break;
      case IND_VIDYA:
         //--- Identifier of "Variable Index DYnamic Average"
         if(count==4)
            result=new CiVIDyA;
         break;
      case IND_VOLUMES:
         //--- Identifier of "Volumes"
         if(count==1)
            result=new CiVolumes;
         break;
         //--- Identifier of "Custom"
      case IND_CUSTOM:
         if(count>0)
         result=new CiCustom;
         break;
     }
   if(result!=NULL)
     {
      if(result.Create(symbol,period,type,count,params))
         Add(result);
      else
        {
         delete result;
         result=NULL;
        }
     }
//---
   return(result);
  }
//+------------------------------------------------------------------+
//| Set size of buffers of all indicators in the collection          |
//+------------------------------------------------------------------+
bool CIndicators::BufferResize(const int size)
  {
   int total=Total();
   for(int i=0;i<total;i++)
     {
      CSeries *series=At(i);
      //--- check pointer
      if(series==NULL)
         return(false);
      if(!series.BufferResize(size))
         return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Refreshing of the data of all indicators in the collection       |
//+------------------------------------------------------------------+
int CIndicators::Refresh(void)
  {
   MqlDateTime time;
   TimeCurrent(time);
//---
   int flags=TimeframesFlags(time);
   int total=Total();
//---
   for(int i=0;i<total;i++)
     {
      CSeries *indicator=At(i);
      if(indicator!=NULL)
         indicator.Refresh(flags);
     }
//---
   m_prev_time=time;
//---
   return(flags);
  }
//+------------------------------------------------------------------+
//| Formation of timeframe flags                                     |
//+------------------------------------------------------------------+
int CIndicators::TimeframesFlags(const MqlDateTime &time)
  {
//--- set flags for all timeframes
   int   result=OBJ_ALL_PERIODS;
//--- if first check, then setting flags all timeframes
   if(m_prev_time.min==-1)
      return(result);
//--- check change time
   if(time.min==m_prev_time.min && 
      time.hour==m_prev_time.hour && 
      time.day==m_prev_time.day &&
      time.mon==m_prev_time.mon)
      return(OBJ_NO_PERIODS);
//--- new month?
   if(time.mon!=m_prev_time.mon)
      return(result);
//--- reset the "new month" flag
   result^=OBJ_PERIOD_MN1;
//--- new day?
   if(time.day!=m_prev_time.day)
      return(result);
//--- reset the "new day" and "new week" flags
   result^=OBJ_PERIOD_D1+OBJ_PERIOD_W1;
//--- temporary variables to speed up working with structures
   int curr,delta;
//--- new hour?
   curr=time.hour;
   delta=curr-m_prev_time.hour;
   if(delta!=0)
     {
      if(curr%2>=delta)
         result^=OBJ_PERIOD_H2;
      if(curr%3>=delta)
         result^=OBJ_PERIOD_H3;
      if(curr%4>=delta)
         result^=OBJ_PERIOD_H4;
      if(curr%6>=delta)
         result^=OBJ_PERIOD_H6;
      if(curr%8>=delta)
         result^=OBJ_PERIOD_H8;
      if(curr%12>=delta)
         result^=OBJ_PERIOD_H12;
      return(result);
     }
//--- reset all flags for hour timeframes
   result^=OBJ_PERIOD_H1+OBJ_PERIOD_H2+OBJ_PERIOD_H3+OBJ_PERIOD_H4+OBJ_PERIOD_H6+OBJ_PERIOD_H8+OBJ_PERIOD_H12;
//--- new minute?
   curr=time.min;
   delta=curr-m_prev_time.min;
   if(delta!=0)
     {
      if(curr%2>=delta)
         result^=OBJ_PERIOD_M2;
      if(curr%3>=delta)
         result^=OBJ_PERIOD_M3;
      if(curr%4>=delta)
         result^=OBJ_PERIOD_M4;
      if(curr%5>=delta)
         result^=OBJ_PERIOD_M5;
      if(curr%6>=delta)
         result^=OBJ_PERIOD_M6;
      if(curr%10>=delta)
         result^=OBJ_PERIOD_M10;
      if(curr%12>=delta)
         result^=OBJ_PERIOD_M12;
      if(curr%15>=delta)
         result^=OBJ_PERIOD_M15;
      if(curr%20>=delta)
         result^=OBJ_PERIOD_M20;
      if(curr%30>=delta)
         result^=OBJ_PERIOD_M30;
     }
//---
   return(result);
  }
//+------------------------------------------------------------------+
