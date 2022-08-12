//+------------------------------------------------------------------+
//|                                             SignalBullsPower.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertSignal.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Signals of oscillator 'Bulls Power'                        |
//| Type=SignalAdvanced                                              |
//| Name=Bulls Power                                                 |
//| ShortName=BullsPower                                             |
//| Class=CSignalBullsPower                                          |
//| Page=signal_bulls                                                |
//| Parameter=PeriodBulls,int,13,Period of calculation               |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CSignalBullsPower.                                         |
//| Purpose: Class of generator of trade signals based on            |
//|          the 'Bulls Power' oscillator.                           |
//| Is derived from the CExpertSignal class.                         |
//+------------------------------------------------------------------+
class CSignalBullsPower : public CExpertSignal
  {
protected:
   CiBullsPower      m_bulls;          // object-oscillator
   //--- adjusted parameters
   int               m_period_bulls;   // the "period of calculation" parameter of the oscillator
   //--- "weights" of market models (0-100)
   int               m_pattern_0;      // model 0 "reverse of the oscillator to required direction"
   int               m_pattern_1;      // model 1 "divergence of the oscillator and price"
   //--- variables
   double            m_extr_osc[10];   // array of values of extremums of the oscillator
   double            m_extr_pr[10];    // array of values of the corresponding extremums of price
   int               m_extr_pos[10];   // array of shifts of extremums (in bars)
   uint              m_extr_map;       // resulting bit-map of ratio of extremums of the oscillator and the price

public:
                     CSignalBullsPower(void);
                    ~CSignalBullsPower(void);
   //--- methods of setting adjustable parameters
   void              PeriodBulls(int value)      { m_period_bulls=value;             }
   //--- methods of adjusting "weights" of market models
   void              Pattern_0(int value)        { m_pattern_0=value;                }
   void              Pattern_1(int value)        { m_pattern_1=value;                }
   //--- method of verification of settings
   virtual bool      ValidationSettings(void);
   //--- method of creating the indicator and timeseries
   virtual bool      InitIndicators(CIndicators *indicators);
   //--- methods of checking if the market models are formed
   virtual int       ShortCondition(void);
   //--- the oscillator doesn't identify conditions for buying

protected:
   //--- method of initialization of the oscillator
   bool              InitBears(CIndicators *indicators);
   //--- methods of getting data
   double            Bulls(int ind)               { return(m_bulls.Main(ind));       }
   double            DiffBulls(int ind)           { return(Bulls(ind)-Bulls(ind+1)); }
   int               StateBulls(int ind);
   bool              ExtStateBulls(int ind);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSignalBullsPower::CSignalBullsPower(void) : m_period_bulls(13),
                                             m_pattern_0(20),
                                             m_pattern_1(80)
  {
//--- initialization of protected data
   m_used_series=USE_SERIES_HIGH+USE_SERIES_LOW;
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSignalBullsPower::~CSignalBullsPower(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CSignalBullsPower::ValidationSettings(void)
  {
//--- validation settings of additional filters
   if(!CExpertSignal::ValidationSettings())
      return(false);
//--- initial data checks
   if(m_period_bulls<=0)
     {
      printf(__FUNCTION__+": period Bulls must be greater than 0");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Create indicators.                                               |
//+------------------------------------------------------------------+
bool CSignalBullsPower::InitIndicators(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- initialization of indicators and timeseries of additional filters
   if(!CExpertSignal::InitIndicators(indicators))
      return(false);
//--- create and initialize BullsPower oscillator
   if(!InitBears(indicators))
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize BearsPower oscillators.                               |
//+------------------------------------------------------------------+
bool CSignalBullsPower::InitBears(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- add object to collection
   if(!indicators.Add(GetPointer(m_bulls)))
     {
      printf(__FUNCTION__+": error adding object");
      return(false);
     }
//--- initialize object
   if(!m_bulls.Create(m_symbol.Name(),m_period,m_period_bulls))
     {
      printf(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Check of the oscillator state.                                   |
//+------------------------------------------------------------------+
int CSignalBullsPower::StateBulls(int ind)
  {
   int    res=0;
   double var;
//---
   for(int i=ind;;i++)
     {
      if(Bulls(i+1)==EMPTY_VALUE)
         break;
      var=DiffBulls(i);
      if(res>0)
        {
         if(var<0)
            break;
         res++;
         continue;
        }
      if(res<0)
        {
         if(var>0)
            break;
         res--;
         continue;
        }
      if(var>0)
         res++;
      if(var<0)
         res--;
     }
//--- return the result
   return(res);
  }
//+------------------------------------------------------------------+
//| Extended check of the oscillator state consists                  |
//| in forming a bit-map according to certain rules,                 |
//| which shows ratios of extremums of the oscillator and price.     |
//+------------------------------------------------------------------+
bool CSignalBullsPower::ExtStateBulls(int ind)
  {
//--- operation of this method results in a bit-map of extremums
//--- practically, the bit-map of extremums is an "array" of 4-bit fields
//--- each "element of the array" definitely describes the ratio
//--- of current extremums of the oscillator and the price with previous ones
//--- purpose of bits of an element of the analyzed bit-map
//--- bit 3 - not used (always 0)
//--- bit 2 - is equal to 1 if the current extremum of the oscillator is "more extreme" than the previous one
//---         (a higher peak or a deeper valley), otherwise - 0
//--- bit 1 - not used (always 0)
//--- bit 0 - is equal to 1 if the current extremum of price is "more extreme" than the previous one
//---         (a higher peak or a deeper valley), otherwise - 0
//--- in addition to them, the following is formed:
//--- array of values of extremums of the oscillator,
//--- array of values of price extremums and
//--- array of "distances" between extremums of the oscillator (in bars)
//--- it should be noted that when using the results of the extended check of state,
//--- you should consider, which extremum of the oscillator (peak or valley)
//--- is the "reference point" (i.e. was detected first during the analysis)
//--- if a peak is detected first then even elements of all arrays
//--- will contain information about peaks, and odd elements will contain information about valleys
//--- if a valley is detected first, then respectively in reverse
   int    pos=ind,off,index;
   uint   map;                 // intermediate bit-map for one extremum
//---
   m_extr_map=0;
   for(int i=0;i<10;i++)
     {
      off=StateBulls(pos);
      if(off>0)
        {
         //--- minimum of the oscillator is detected
         pos+=off;
         m_extr_pos[i]=pos;
         m_extr_osc[i]=Bulls(pos);
         if(i>1)
           {
            m_extr_pr[i]=m_low.MinValue(pos-2,5,index);
            //--- form the intermediate bit-map
            map=0;
            if(m_extr_pr[i-2]<m_extr_pr[i])
               map+=1;  // set bit 0
            if(m_extr_osc[i-2]<m_extr_osc[i])
               map+=4;  // set bit 2
            //--- add the result
            m_extr_map+=map<<(4*(i-2));
           }
         else
            m_extr_pr[i]=m_low.MinValue(pos-1,4,index);
        }
      else
        {
         //--- maximum of the oscillator is detected
         pos-=off;
         m_extr_pos[i]=pos;
         m_extr_osc[i]=Bulls(pos);
         if(i>1)
           {
            m_extr_pr[i]=m_high.MaxValue(pos-2,5,index);
            //--- form the intermediate bit-map
            map=0;
            if(m_extr_pr[i-2]>m_extr_pr[i])
               map+=1;  // set bit 0
            if(m_extr_osc[i-2]>m_extr_osc[i])
               map+=4;  // set bit 2
            //--- add the result
            m_extr_map+=map<<(4*(i-2));
           }
         else
            m_extr_pr[i]=m_high.MaxValue(pos-1,4,index);
        }
     }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| "Voting" that price will fall.                                   |
//+------------------------------------------------------------------+
int CSignalBullsPower::ShortCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- if the oscillator is below zero, don't "vote" for selling
   if(Bulls(idx)<0.0)
      return(result);
//--- the oscillator is above zero
   if(StateBulls(idx)<0)
     {
      //--- the oscillator has turned downwards at a previous bar
      //--- there us a condition for selling
      if(IS_PATTERN_USAGE(0))
         result=m_pattern_0;
      //--- if the model 1 is used, search for the "divergence" signal
      if(IS_PATTERN_USAGE(1))
        {
         ExtStateBulls(idx);
         if((m_extr_map&0xF)==1)
           {
            if(m_extr_osc[0]>0.0 && m_extr_osc[2]>0.0)
              {
               //--- both peaks are above zero
               //--- we suppose that this is "divergence"
               result=m_pattern_1;
              }
           }
        }
     }
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
