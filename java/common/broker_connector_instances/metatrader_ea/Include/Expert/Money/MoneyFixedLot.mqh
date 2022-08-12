//+------------------------------------------------------------------+
//|                                                MoneyFixedLot.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertMoney.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Trading with fixed trade volume                            |
//| Type=Money                                                       |
//| Name=FixLot                                                      |
//| Class=CMoneyFixedLot                                             |
//| Page=                                                            |
//| Parameter=Percent,double,10.0,Percent                            |
//| Parameter=Lots,double,0.1,Fixed volume                           |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CMoneyFixedLot.                                            |
//| Purpose: Class of money management with fixed lot.               |
//|              Derives from class CExpertMoney.                    |
//+------------------------------------------------------------------+
class CMoneyFixedLot : public CExpertMoney
  {
protected:
   //--- input parameters
   double            m_lots;

public:
                     CMoneyFixedLot(void);
                    ~CMoneyFixedLot(void);
   //---
   void              Lots(double lots)                      { m_lots=lots; }
   virtual bool      ValidationSettings(void);
   //---
   virtual double    CheckOpenLong(double price,double sl)  { return(m_lots); }
   virtual double    CheckOpenShort(double price,double sl) { return(m_lots); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CMoneyFixedLot::CMoneyFixedLot(void) : m_lots(0.1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
void CMoneyFixedLot::~CMoneyFixedLot(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CMoneyFixedLot::ValidationSettings(void)
  {
   if(!CExpertMoney::ValidationSettings())
      return(false);
//--- initial data checks
   if(m_lots<m_symbol.LotsMin() || m_lots>m_symbol.LotsMax())
     {
      printf(__FUNCTION__+": lots amount must be in the range from %f to %f",m_symbol.LotsMin(),m_symbol.LotsMax());
      return(false);
     }
   if(MathAbs(m_lots/m_symbol.LotsStep()-MathRound(m_lots/m_symbol.LotsStep()))>1.0E-10)
     {
      printf(__FUNCTION__+": lots amount is not corresponding with lot step %f",m_symbol.LotsStep());
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
