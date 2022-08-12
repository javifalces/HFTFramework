//+------------------------------------------------------------------+
//|                                           MoneySizeOptimized.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertMoney.mqh>
#include <Trade\DealInfo.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Trading with optimized trade volume                        |
//| Type=Money                                                       |
//| Name=SizeOptimized                                               |
//| Class=CMoneySizeOptimized                                        |
//| Page=                                                            |
//| Parameter=DecreaseFactor,double,3.0,Decrease factor              |
//| Parameter=Percent,double,10.0,Percent                            |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CMoneySizeOptimized.                                       |
//| Purpose: Class of money management with size optimized.          |
//|              Derives from class CExpertMoney.                    |
//+------------------------------------------------------------------+
class CMoneySizeOptimized : public CExpertMoney
  {
protected:
   double            m_decrease_factor;

public:
                     CMoneySizeOptimized(void);
                    ~CMoneySizeOptimized(void);
   //---
   void              DecreaseFactor(double decrease_factor) { m_decrease_factor=decrease_factor; }
   virtual bool      ValidationSettings(void);
   //---
   virtual double    CheckOpenLong(double price,double sl);
   virtual double    CheckOpenShort(double price,double sl);

protected:
   double            Optimize(double lots);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
void CMoneySizeOptimized::CMoneySizeOptimized(void) : m_decrease_factor(3.0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
void CMoneySizeOptimized::~CMoneySizeOptimized(void)
  {
  }
//+------------------------------------------------------------------+
//| Validation settings protected data.                              |
//+------------------------------------------------------------------+
bool CMoneySizeOptimized::ValidationSettings(void)
  {
   if(!CExpertMoney::ValidationSettings())
      return(false);
//--- initial data checks
   if(m_decrease_factor<=0.0)
     {
      printf(__FUNCTION__+": decrease factor must be greater then 0");
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Getting lot size for open long position.                         |
//+------------------------------------------------------------------+
double CMoneySizeOptimized::CheckOpenLong(double price,double sl)
  {
   if(m_symbol==NULL)
      return(0.0);
//--- select lot size
   double lot;
   if(price==0.0)
      lot=m_account.MaxLotCheck(m_symbol.Name(),ORDER_TYPE_BUY,m_symbol.Ask(),m_percent);
   else
      lot=m_account.MaxLotCheck(m_symbol.Name(),ORDER_TYPE_BUY,price,m_percent);
//--- return trading volume
   return(Optimize(lot));
  }
//+------------------------------------------------------------------+
//| Getting lot size for open short position.                        |
//+------------------------------------------------------------------+
double CMoneySizeOptimized::CheckOpenShort(double price,double sl)
  {
   if(m_symbol==NULL)
      return(0.0);
//--- select lot size
   double lot;
   if(price==0.0)
      lot=m_account.MaxLotCheck(m_symbol.Name(),ORDER_TYPE_SELL,m_symbol.Bid(),m_percent);
   else
      lot=m_account.MaxLotCheck(m_symbol.Name(),ORDER_TYPE_SELL,price,m_percent);
//--- return trading volume
   return(Optimize(lot));
  }
//+------------------------------------------------------------------+
//| Optimizing lot size for open.                                    |
//+------------------------------------------------------------------+
double CMoneySizeOptimized::Optimize(double lots)
  {
   double lot=lots;
//--- calculate number of losses orders without a break
   if(m_decrease_factor>0)
     {
      //--- select history for access
      HistorySelect(0,TimeCurrent());
      //---
      int       orders=HistoryDealsTotal();  // total history deals
      int       losses=0;                    // number of consequent losing orders
      CDealInfo deal;
      //---
      for(int i=orders-1;i>=0;i--)
        {
         deal.Ticket(HistoryDealGetTicket(i));
         if(deal.Ticket()==0)
           {
            Print("CMoneySizeOptimized::Optimize: HistoryDealGetTicket failed, no trade history");
            break;
           }
         //--- check symbol
         if(deal.Symbol()!=m_symbol.Name())
            continue;
         //--- check profit
         double profit=deal.Profit();
         if(profit>0.0)
            break;
         if(profit<0.0)
            losses++;
        }
      //---
      if(losses>1)
         lot=NormalizeDouble(lot-lot*losses/m_decrease_factor,2);
     }
//--- normalize and check limits
   double stepvol=m_symbol.LotsStep();
   lot=stepvol*NormalizeDouble(lot/stepvol,0);
//---
   double minvol=m_symbol.LotsMin();
   if(lot<minvol)
      lot=minvol;
//---
   double maxvol=m_symbol.LotsMax();
   if(lot>maxvol)
      lot=maxvol;
//---
   return(lot);
  }
//+------------------------------------------------------------------+
