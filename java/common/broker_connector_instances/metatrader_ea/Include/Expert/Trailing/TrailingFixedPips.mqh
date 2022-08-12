//+------------------------------------------------------------------+
//|                                            TrailingFixedPips.mqh |
//|                   Copyright 2009-2016, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertTrailing.mqh>
// wizard description start
//+----------------------------------------------------------------------+
//| Description of the class                                             |
//| Title=Trailing Stop based on fixed Stop Level                        |
//| Type=Trailing                                                        |
//| Name=FixedPips                                                       |
//| Class=CTrailingFixedPips                                             |
//| Page=                                                                |
//| Parameter=StopLevel,int,30,Stop Loss trailing level (in points)      |
//| Parameter=ProfitLevel,int,50,Take Profit trailing level (in points)  |
//+----------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CTrailingFixedPips.                                        |
//| Purpose: Class of trailing stops with fixed stop level in pips.  |
//|              Derives from class CExpertTrailing.                 |
//+------------------------------------------------------------------+
class CTrailingFixedPips : public CExpertTrailing
  {
protected:
   //--- input parameters
   int               m_stop_level;
   int               m_profit_level;

public:
                     CTrailingFixedPips(void);
                    ~CTrailingFixedPips(void);
   //--- methods of initialization of protected data
   void              StopLevel(int stop_level)     { m_stop_level=stop_level;     }
   void              ProfitLevel(int profit_level) { m_profit_level=profit_level; }
   virtual bool      ValidationSettings(void);
   //---
   virtual bool      CheckTrailingStopLong(CPositionInfo *position,double &sl,double &tp);
   virtual bool      CheckTrailingStopShort(CPositionInfo *position,double &sl,double &tp);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CTrailingFixedPips::CTrailingFixedPips(void) : m_stop_level(30),
                                                    m_profit_level(50)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CTrailingFixedPips::~CTrailingFixedPips(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CTrailingFixedPips::ValidationSettings(void)
  {
   if(!CExpertTrailing::ValidationSettings())
      return(false);
//--- initial data checks
   if(m_profit_level!=0 && m_profit_level*(m_adjusted_point/m_symbol.Point())<m_symbol.StopsLevel())
     {
      printf(__FUNCTION__+": trailing Profit Level must be 0 or greater than %d",m_symbol.StopsLevel());
      return(false);
     }
   if(m_stop_level!=0 && m_stop_level*(m_adjusted_point/m_symbol.Point())<m_symbol.StopsLevel())
     {
      printf(__FUNCTION__+": trailing Stop Level must be 0 or greater than %d",m_symbol.StopsLevel());
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Checking trailing stop and/or profit for long position.          |
//+------------------------------------------------------------------+
bool CTrailingFixedPips::CheckTrailingStopLong(CPositionInfo *position,double &sl,double &tp)
  {
//--- check
   if(position==NULL)
      return(false);
   if(m_stop_level==0)
      return(false);
//---
   double delta;
   double pos_sl=position.StopLoss();
   double base  =(pos_sl==0.0) ? position.PriceOpen() : pos_sl;
   double price =m_symbol.Bid();
//---
   sl=EMPTY_VALUE;
   tp=EMPTY_VALUE;
   delta=m_stop_level*m_adjusted_point;
   if(price-base>delta)
     {
      sl=price-delta;
      if(m_profit_level!=0)
         tp=price+m_profit_level*m_adjusted_point;
     }
//---
   return(sl!=EMPTY_VALUE);
  }
//+------------------------------------------------------------------+
//| Checking trailing stop and/or profit for short position.         |
//+------------------------------------------------------------------+
bool CTrailingFixedPips::CheckTrailingStopShort(CPositionInfo *position,double &sl,double &tp)
  {
//--- check
   if(position==NULL)
      return(false);
   if(m_stop_level==0)
      return(false);
//---
   double delta;
   double pos_sl=position.StopLoss();
   double base  =(pos_sl==0.0) ? position.PriceOpen() : pos_sl;
   double price =m_symbol.Ask();
//---
   sl=EMPTY_VALUE;
   tp=EMPTY_VALUE;
   delta=m_stop_level*m_adjusted_point;
   if(base-price>delta)
     {
      sl=price+delta;
      if(m_profit_level!=0)
         tp=price-m_profit_level*m_adjusted_point;
     }
//---
   return(sl!=EMPTY_VALUE);
  }
//+------------------------------------------------------------------+
