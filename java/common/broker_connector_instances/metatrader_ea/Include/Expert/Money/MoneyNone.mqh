//+------------------------------------------------------------------+
//|                                                    MoneyNone.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertMoney.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Trading with minimal allowed trade volume                  |
//| Type=Money                                                       |
//| Name=MinLot                                                      |
//| Class=CMoneyNone                                                 |
//| Page=                                                            |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CMoneyNone.                                                |
//| Appointment: Class no money managment.                           |
//|              Derives from class CExpertMoney.                    |
//+------------------------------------------------------------------+
class CMoneyNone : public CExpertMoney
  {
public:
                     CMoneyNone(void);
                    ~CMoneyNone(void);
   //---
   virtual bool      ValidationSettings(void);
   //---
   virtual double    CheckOpenLong(double price,double sl);
   virtual double    CheckOpenShort(double price,double sl);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CMoneyNone::CMoneyNone(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
void CMoneyNone::~CMoneyNone(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CMoneyNone::ValidationSettings(void)
  {
   Percent(100.0);
//--- initial data checks
   if(!CExpertMoney::ValidationSettings())
      return(false);
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Getting lot size for open long position.                         |
//+------------------------------------------------------------------+
double CMoneyNone::CheckOpenLong(double price,double sl)
  {
   return(m_symbol.LotsMin());
  }
//+------------------------------------------------------------------+
//| Getting lot size for open short position.                        |
//+------------------------------------------------------------------+
double CMoneyNone::CheckOpenShort(double price,double sl)
  {
   return(m_symbol.LotsMin());
  }
//+------------------------------------------------------------------+
