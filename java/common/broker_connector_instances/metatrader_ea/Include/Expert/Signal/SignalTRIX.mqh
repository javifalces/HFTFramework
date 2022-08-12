//+------------------------------------------------------------------+
//|                                                   SignalTRIX.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertSignal.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Signals of oscillator 'Triple Exponential Average'         |
//| Type=SignalAdvanced                                              |
//| Name=Triple Exponential Average                                  |
//| ShortName=TriX                                                   |
//| Class=CSignalTriX                                                |
//| Page=signal_trix                                                 |
//| Parameter=PeriodTriX,int,14,Period of calculation                |
//| Parameter=Applied,ENUM_APPLIED_PRICE,PRICE_CLOSE,Prices series   |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CSignalTriX.                                               |
//| Purpose: Class of generator of trade signals based on            |
//|          the 'Triple Exponential Average' oscillator.            |
//| Is derived from the CExpertSignal class.                         |
//+------------------------------------------------------------------+
class CSignalTriX : public CExpertSignal
  {
protected:
   CiTriX            m_trix;           // object-oscillator
   //--- adjusted parameters
   int               m_period_trix;    // the "period of calculation" parameter of the oscillator
   ENUM_APPLIED_PRICE m_applied;       // the "price series" parameter of the oscillator
   //--- "weights" of market models (0-100)
   int               m_pattern_0;      // model 0 "the oscillator has required direction"
   int               m_pattern_1;      // model 1 "reverse of the oscillator to required direction"
   int               m_pattern_2;      // model 2 "crossing of main line an the zero level"
   int               m_pattern_3;      // model 3 "divergence of the oscillator and price"
   //--- variables
   double            m_extr_osc[10];   // array of values of extremums of the oscillator
   double            m_extr_pr[10];    // array of values of the corresponding extremums of price
   int               m_extr_pos[10];   // array of shifts of extremums (in bars)
   uint              m_extr_map;       // resulting bit-map of ratio of extremums of the oscillator and the price

public:
                     CSignalTriX(void);
                    ~CSignalTriX(void);
   //--- methods of setting adjustable parameters
   void              PeriodTriX(int value)             { m_period_trix=value;           }
   void              Applied(ENUM_APPLIED_PRICE value) { m_applied=value;               }
   //--- methods of adjusting "weights" of market models
   void              Pattern_0(int value)              { m_pattern_0=value;             }
   void              Pattern_1(int value)              { m_pattern_1=value;             }
   void              Pattern_2(int value)              { m_pattern_2=value;             }
   void              Pattern_3(int value)              { m_pattern_3=value;             }
   //--- method of verification of settings
   virtual bool      ValidationSettings(void);
   //--- method of creating the indicator and timeseries
   virtual bool      InitIndicators(CIndicators *indicators);
   //--- methods of checking if the market models are formed
   virtual int       LongCondition(void);
   virtual int       ShortCondition(void);

protected:
   //--- method of initialization of the oscillator
   bool              InitTriX(CIndicators *indicators);
   //--- methods of getting data
   double            TriX(int ind)                     { return(m_trix.Main(ind));      }
   double            DiffTriX(int ind)                 { return(TriX(ind)-TriX(ind+1)); }
   int               State(int ind);
   bool              ExtState(int ind);
   bool              CompareMaps(int map,int count,bool minimax=false,int start=0);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSignalTriX::CSignalTriX(void) : m_period_trix(12),
                                 m_applied(PRICE_CLOSE),
                                 m_pattern_0(20),
                                 m_pattern_1(80),
                                 m_pattern_2(100),
                                 m_pattern_3(70)
  {
//--- initialization of protected data
   m_used_series=USE_SERIES_HIGH+USE_SERIES_LOW;
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSignalTriX::~CSignalTriX(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CSignalTriX::ValidationSettings(void)
  {
   if(!CExpertSignal::ValidationSettings())
      return(false);
//---
   if(m_period_trix<=0)
     {
      printf(__FUNCTION__+": period must be greater than 0");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Create indicators.                                               |
//+------------------------------------------------------------------+
bool CSignalTriX::InitIndicators(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- initialization of indicators and timeseries of additional filters
   if(!CExpertSignal::InitIndicators(indicators))
      return(false);
//--- create and initialize TriX oscilator
   if(!InitTriX(indicators))
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize TriX oscillators.                                     |
//+------------------------------------------------------------------+
bool CSignalTriX::InitTriX(CIndicators *indicators)
  {
//--- check pointer
   if(indicators==NULL)
      return(false);
//--- add object to collection
   if(!indicators.Add(GetPointer(m_trix)))
     {
      printf(__FUNCTION__+": error adding object");
      return(false);
     }
//--- initialize object
   if(!m_trix.Create(m_symbol.Name(),m_period,m_period_trix,m_applied))
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
int CSignalTriX::State(int ind)
  {
   int    res=0;
   double var;
//---
   for(int i=ind;;i++)
     {
      if(TriX(i+1)==EMPTY_VALUE)
         break;
      var=DiffTriX(i);
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
bool CSignalTriX::ExtState(int ind)
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
      off=State(pos);
      if(off>0)
        {
         //--- minimum of the oscillator is detected
         pos+=off;
         m_extr_pos[i]=pos;
         m_extr_osc[i]=TriX(pos);
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
         m_extr_osc[i]=TriX(pos);
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
//| Comparing the bit-map of extremums with pattern.                 |
//+------------------------------------------------------------------+
bool CSignalTriX::CompareMaps(int map,int count,bool minimax,int start)
  {
   int step =(minimax)?4:8;
   int total=step*(start+count);
//--- check input parameters for a possible going out of range of the bit-map
   if(total>32)
      return(false);
//--- bit-map of the patter is an "array" of 4-bit fields
//--- each "element of the array" definitely describes the desired ratio
//--- of current extremums of the oscillator and the price with previous ones
//--- purpose of bits of an elements of the pattern of the bit-map pattern
//--- bit 3 - is equal to if the ratio of extremums of the oscillator is insignificant for us
//---         is equal to 0 if we want to "find" the ratio of extremums of the oscillator determined by the value of bit 2
//--- bit 2 - is equal to 1 if we want to "discover" the situation when the current extremum of the "oscillator" is "more extreme" than the previous one
//---         (current peak is higher or current valley is deeper)
//---         is equal to 0 if we want to "discover" the situation when the current extremum of the oscillator is "less extreme" than the previous one
//---         (current peak is lower or current valley is less deep)
//--- bit 1 - is equal to 1 if the ratio of extremums is insignificant for us
//---         it is equal to 0 if we want to "find" the ratio of price extremums determined by the value of bit 0
//--- bit 0 - is equal to 1 if we want to "discover" the situation when the current price extremum is "more extreme" than the previous one
//---         (current peak is higher or current valley is deeper)
//---         it is equal to 0 if we want to "discover" the situation when the current price extremum is "less extreme" than the previous one
//---         (current peak is lower or current valley is less deep)
   uint inp_map,check_map;
   int  i,j;
//--- loop by extremums (4 minimums and 4 maximums)
//--- price and the oscillator are checked separately (thus, there are 16 checks)
   for(i=step*start,j=0;i<total;i+=step,j+=4)
     {
      //--- "take" two bits - patter of the corresponding extremum of the price
      inp_map=(map>>j)&3;
      //--- if the higher-order bit=1, then any ratio is suitable for us
      if(inp_map<2)
        {
         //--- "take" two bits of the corresponding extremum of the price (higher-order bit is always 0)
         check_map=(m_extr_map>>i)&3;
         if(inp_map!=check_map)
            return(false);
        }
      //--- "take" two bits - pattern of the corresponding oscillator extremum
      inp_map=(map>>(j+2))&3;
      //--- if the higher-order bit=1, then any ratio is suitable for us
      if(inp_map>=2)
         continue;
      //--- "take" two bits of the corresponding oscillator extremum (higher-order bit is always 0)
      check_map=(m_extr_map>>(i+2))&3;
      if(inp_map!=check_map)
         return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| "Voting" that price will grow.                                   |
//+------------------------------------------------------------------+
int CSignalTriX::LongCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- check direction of the oscillator
   if(DiffTriX(idx)>0.0)
     {
      //--- the oscillator is directed upwards confirming the possibility of price growth
      if(IS_PATTERN_USAGE(0))
         result=m_pattern_0;       // "confirming" signal number 0
      //--- if the model 1 is used, search for a reverse of the oscillator
      if(IS_PATTERN_USAGE(1) && DiffTriX(idx)>0.0 && DiffTriX(idx+1)<0.0)
         result=m_pattern_1;       // signal number 1
      //--- if the model 2 is used, search for an intersection of the oscillator line and the zero level
      if(IS_PATTERN_USAGE(2) && TriX(idx)>0.0 && TriX(idx+1)<0.0)
         result=m_pattern_2;       // signal number 2
      //--- if the model 3 is used, and the oscillator turned up below the zero level, search for the divergence
      if(IS_PATTERN_USAGE(3) && TriX(idx)<0.0)
        {
         //--- perform the extended analysis of the oscillator state
         ExtState(idx);
         //--- search for the "divergence" signal
         if(CompareMaps(1,1)) // 0000 0001b
           {
            if(m_extr_osc[0]<0.0 && m_extr_osc[1]<0.0 && m_extr_osc[2]<0.0)
              {
               //--- both valleys of the oscillator are below zero and the peak between them hasn't raised above zero
               result=m_pattern_3; // signal number 3
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
int CSignalTriX::ShortCondition(void)
  {
   int result=0;
   int idx   =StartIndex();
//--- check direction of the main line
   if(DiffTriX(idx)<0.0)
     {
      //--- main line is directed downwards, confirming a possibility of falling of price
      if(IS_PATTERN_USAGE(0))
         result=m_pattern_0;       // "confirming" signal number 0
      //--- if the model 1 is used, search for a reverse of the main line
      if(IS_PATTERN_USAGE(1) && DiffTriX(idx)<0.0 && DiffTriX(idx+1)>0.0)
         result=m_pattern_1;       // signal number 1
      //--- if the model 2 is used, search for an intersection of the main line and the zero level
      if(IS_PATTERN_USAGE(2) && TriX(idx)<0.0 && TriX(idx+1)>0.0)
         result=m_pattern_2;       // signal number 2
      //--- if the model 3 is used and the main line turned down above the zero level, search for the divergence
      if(IS_PATTERN_USAGE(3) && TriX(idx)>0.0)
        {
         //--- perform the extended analysis of the oscillator state
         ExtState(idx);
         //--- search for the "divergence" signal
         if(CompareMaps(1,1)) // 0000 0001b
           {
            if(m_extr_osc[0]>0.0 && m_extr_osc[1]>0.0 && m_extr_osc[2]>0.0)
              {
               //--- both peaks of the oscillator are above zero and the valley between them hasn't fallen below zero
               result=m_pattern_3; // signal number 3
              }
           }
        }
     }
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
