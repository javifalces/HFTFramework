//+------------------------------------------------------------------+
//|                                                     SignalAO.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertSignal.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Signals of indicator 'Awesome Oscillator'                  |
//| Type=SignalAdvanced                                              |
//| Name=Awesome Oscillator                                          |
//| ShortName=AO                                                     |
//| Class=CSignalAO                                                  |
//| Page=signal_ao                                                   |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CSignalAO.                                                 |
//| Purpose: Class of generator of trade signals based on            |
//|          the 'Awesome Oscillator' indicator.                     |
//| Is derived from the CExpertSignal class.                         |
//+------------------------------------------------------------------+
class CSignalAO : public CExpertSignal
  {
protected:
   CiAO              m_ao;             // object-indicator
   //--- "weights" of market models (0-100)
   int               m_pattern_0;      // model 0 "first analyzed bar has required color"
   int               m_pattern_1;      // model 1 "the 'saucer' signal"
   int               m_pattern_2;      // model 2 "the 'crossing of the zero line' signal"
   int               m_pattern_3;      // model 2 "the 'divergence' signal"
   //--- variables
   double            m_extr_osc[10];   // array of values of extremums of the oscillator
   double            m_extr_pr[10];    // array of values of the corresponding extremums of price
   int               m_extr_pos[10];   // array of shifts of extremums (in bars)
   uint              m_extr_map;       // resulting bit-map of ratio of extremums of the oscillator and the price

public:
                     CSignalAO(void);
                    ~CSignalAO(void);
   //--- methods of adjusting "weights" of market models
   void              Pattern_0(int value)        { m_pattern_0=value;         }
   void              Pattern_1(int value)        { m_pattern_1=value;         }
   void              Pattern_2(int value)        { m_pattern_2=value;         }
   void              Pattern_3(int value)        { m_pattern_3=value;         }
   //--- method of creating the indicator and timeseries
   virtual bool      InitIndicators(CIndicators *indicators);
   //--- methods of checking if the market models are formed
   virtual int       LongCondition(void);
   virtual int       ShortCondition(void);

protected:
   //--- method of initialization of the indicator
   bool              InitAO(CIndicators *indicators);
   //--- methods of getting data
   double            AO(int ind)                 { return(m_ao.Main(ind));    }
   double            DiffAO(int ind)             { return(AO(ind)-AO(ind+1)); }
   int               StateAO(int ind);
   bool              ExtStateAO(int ind);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSignalAO::CSignalAO(void) : m_pattern_0(30),
                             m_pattern_1(20),
                             m_pattern_2(70),
                             m_pattern_3(90)
  {
//--- initialization of protected data
   m_used_series=USE_SERIES_HIGH+USE_SERIES_LOW;
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSignalAO::~CSignalAO(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicators.                                               |
//+------------------------------------------------------------------+
bool CSignalAO::InitIndicators(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- initialization of indicators and timeseries of additional filters
   if(!CExpertSignal::InitIndicators(indicators))
      return(false);
//--- create and initialize AO indicator
   if(!InitAO(indicators))
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize AO indicators.                                        |
//+------------------------------------------------------------------+
bool CSignalAO::InitAO(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- add object to collection
   if(!indicators.Add(GetPointer(m_ao)))
     {
      printf(__FUNCTION__+": error adding object");
      return(false);
     }
//--- initialize object
   if(!m_ao.Create(m_symbol.Name(),m_period))
     {
      printf(__FUNCTION__+": error initializing object");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Check of the indicator state.                                    |
//+------------------------------------------------------------------+
int CSignalAO::StateAO(int ind)
  {
   int    res=0;
   double var;
//---
   for(int i=ind;;i++)
     {
      if(AO(i+1)==EMPTY_VALUE)
         break;
      var=DiffAO(i);
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
//---
   return(res);
  }
//+------------------------------------------------------------------+
//| Extended check of the oscillator state consists                  |
//| in forming a bit-map according to certain rules,                 |
//| which shows ratios of extremums of the oscillator and price.     |
//+------------------------------------------------------------------+
bool CSignalAO::ExtStateAO(int ind)
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
      off=StateAO(pos);
      if(off>0)
        {
         //--- minimum of the oscillator is detected
         pos+=off;
         m_extr_pos[i]=pos;
         m_extr_osc[i]=AO(pos);
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
         m_extr_osc[i]=AO(pos);
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
//| "Voting" that price will grow.                                   |
//+------------------------------------------------------------------+
int CSignalAO::LongCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- if the first analyzed bar is "red", don't "vote" for buying
   if(DiffAO(idx)<0.0)
      return(result);
//--- first analyzed bar is "green" (the indicator has no objections to buying)
   if(IS_PATTERN_USAGE(0))
      result=m_pattern_0;
   if(AO(idx++)>0.0)
     {
      //--- first analyzed bar is greater than zero, search for the "saucer" and "crosing of the zero line" signals
      if(IS_PATTERN_USAGE(1) && DiffAO(idx)<0.0)
        {
         //--- the "saucer" signal
         //--- there is a condition for buying
         return(m_pattern_1);
        }
      if(IS_PATTERN_USAGE(2) && AO(idx)<0.0)
        {
         //--- the "crossing of the zero line" signal
         //--- there is a condition for buying
         return(m_pattern_2);
        }
     }
   else
     {
      //--- first analyzed bar is less than zero, search for the "divergence" signal
      //--- if the second analyzed bar is "red", the condition for buying may be fulfilled
      if(IS_PATTERN_USAGE(3) && DiffAO(idx)<0.0)
        {
         idx=StartIndex();
         //--- search for the "divergence" signal
         ExtStateAO(idx);
         if((m_extr_map&0xF)==1)
           {
            if(m_extr_osc[0]<0.0 && m_extr_osc[1]<0.0 && m_extr_osc[2]<0.0)
              {
               //--- both valleys are below zero, the peak is between them and it hasn't raised above zero
               //--- we suppose that this is "divergence"
               return(m_pattern_3);
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
int CSignalAO::ShortCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- if the first analyzed bar is "green", don't "vote" for selling
   if(DiffAO(idx)>0.0)
      return(result);
//--- first analyzed bar is "red" (the indicator has no objections to selling)
   if(IS_PATTERN_USAGE(0))
      result=m_pattern_0;
   if(AO(idx++)<0.0)
     {
      //--- first analyzed bar is below zero, search for the "saucer" and "crossing of the zero line" signals
      if(IS_PATTERN_USAGE(1) && DiffAO(idx)>0.0)
        {
         //--- the "saucer" signal
         //--- there is a condition for buying
         return(m_pattern_1);
        }
      if(IS_PATTERN_USAGE(2) && AO(idx)>0.0)
        {
         //--- the "crossing of the zero line" signal
         //--- there is a condition for buying
         return(m_pattern_2);
        }
     }
   else
     {
      //--- first analyzed bar is above zero, search for the "divergence" signal
      //--- if the second analyzed bar is "green", the condition for buying may be fulfilled
      if(IS_PATTERN_USAGE(3) && DiffAO(idx)>0.0)
        {
         idx=StartIndex();
         //--- search for the "divergence" signal
         ExtStateAO(idx);
         if((m_extr_map&0xF)==1)
           {
            if(m_extr_osc[0]>0.0 && m_extr_osc[1]>0.0 && m_extr_osc[2]>0.0)
              {
               //--- both peaks are above zero and the valley between them hasn't fallen below zero
               //--- we suppose that this is "divergence"
               return(m_pattern_3);
              }
           }
        }
     }
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
