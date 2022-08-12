//+------------------------------------------------------------------+
//|                                                     DealInfo.mqh |
//|                   Copyright 2009-2020, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
//+------------------------------------------------------------------+
//| Class CDealInfo.                                                 |
//| Appointment: Class for access to history deal info.              |
//|              Derives from class CObject.                         |
//+------------------------------------------------------------------+
class CDealInfo : public CObject
  {
protected:
   ulong             m_ticket;             // ticket of history order

public:
                     CDealInfo(void);
                    ~CDealInfo(void);
   //--- methods of access to protected data
   void              Ticket(const ulong ticket)   { m_ticket=ticket;  }
   ulong             Ticket(void)           const { return(m_ticket); }
   //--- fast access methods to the integer position propertyes
   long              Order(void) const;
   datetime          Time(void) const;
   ulong             TimeMsc(void) const;
   ENUM_DEAL_TYPE    DealType(void) const;
   string            TypeDescription(void) const;
   ENUM_DEAL_ENTRY   Entry(void) const;
   string            EntryDescription(void) const;
   long              Magic(void) const;
   long              PositionId(void) const;
   //--- fast access methods to the double position propertyes
   double            Volume(void) const;
   double            Price(void) const;
   double            Commission(void) const;
   double            Swap(void) const;
   double            Profit(void) const;
   //--- fast access methods to the string position propertyes
   string            Symbol(void) const;
   string            Comment(void) const;
   string            ExternalId(void) const;
   //--- access methods to the API MQL5 functions
   bool              InfoInteger(ENUM_DEAL_PROPERTY_INTEGER prop_id,long &var) const;
   bool              InfoDouble(ENUM_DEAL_PROPERTY_DOUBLE prop_id,double &var) const;
   bool              InfoString(ENUM_DEAL_PROPERTY_STRING prop_id,string &var) const;
   //--- info methods
   string            FormatAction(string &str,const uint action) const;
   string            FormatEntry(string &str,const uint entry) const;
   string            FormatDeal(string &str) const;
   //--- method for select deal
   bool              SelectByIndex(const int index);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CDealInfo::CDealInfo(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CDealInfo::~CDealInfo(void)
  {
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_ORDER"                              |
//+------------------------------------------------------------------+
long CDealInfo::Order(void) const
  {
   return(HistoryDealGetInteger(m_ticket,DEAL_ORDER));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_TIME"                               |
//+------------------------------------------------------------------+
datetime CDealInfo::Time(void) const
  {
   return((datetime)HistoryDealGetInteger(m_ticket,DEAL_TIME));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_TIME_MSC"                           |
//+------------------------------------------------------------------+
ulong CDealInfo::TimeMsc(void) const
  {
   return(HistoryDealGetInteger(m_ticket,DEAL_TIME_MSC));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_TYPE"                               |
//+------------------------------------------------------------------+
ENUM_DEAL_TYPE CDealInfo::DealType(void) const
  {
   return((ENUM_DEAL_TYPE)HistoryDealGetInteger(m_ticket,DEAL_TYPE));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_TYPE" as string                     |
//+------------------------------------------------------------------+
string CDealInfo::TypeDescription(void) const
  {
   string str;
//---
   switch(DealType())
     {
      case DEAL_TYPE_BUY:
         str="Buy type";
         break;
      case DEAL_TYPE_SELL:
         str="Sell type";
         break;
      case DEAL_TYPE_BALANCE:
         str="Balance type";
         break;
      case DEAL_TYPE_CREDIT:
         str="Credit type";
         break;
      case DEAL_TYPE_CHARGE:
         str="Charge type";
         break;
      case DEAL_TYPE_CORRECTION:
         str="Correction type";
         break;
      case DEAL_TYPE_BONUS:
         str="Bonus type";
         break;
      case DEAL_TYPE_COMMISSION:
         str="Commission type";
         break;
      case DEAL_TYPE_COMMISSION_DAILY:
         str="Daily Commission type";
         break;
      case DEAL_TYPE_COMMISSION_MONTHLY:
         str="Monthly Commission type";
         break;
      case DEAL_TYPE_COMMISSION_AGENT_DAILY:
         str="Daily Agent Commission type";
         break;
      case DEAL_TYPE_COMMISSION_AGENT_MONTHLY:
         str="Monthly Agent Commission type";
         break;
      case DEAL_TYPE_INTEREST:
         str="Interest Rate type";
         break;
      case DEAL_TYPE_BUY_CANCELED:
         str="Canceled Buy type";
         break;
      case DEAL_TYPE_SELL_CANCELED:
         str="Canceled Sell type";
         break;
      default:
         str="Unknown type";
     }
//---
   return(str);
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_ENTRY"                              |
//+------------------------------------------------------------------+
ENUM_DEAL_ENTRY CDealInfo::Entry(void) const
  {
   return((ENUM_DEAL_ENTRY)HistoryDealGetInteger(m_ticket,DEAL_ENTRY));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_ENTRY" as string                    |
//+------------------------------------------------------------------+
string CDealInfo::EntryDescription(void) const
  {
   string str;
//---
   switch(CDealInfo::Entry())
     {
      case DEAL_ENTRY_IN:
         str="In entry";
         break;
      case DEAL_ENTRY_OUT:
         str="Out entry";
         break;
      case DEAL_ENTRY_INOUT:
         str="InOut entry";
         break;
      case DEAL_ENTRY_STATE:
         str="Status record";
         break;
      case DEAL_ENTRY_OUT_BY:
         str="Out By entry";
         break;
      default:
         str="Unknown entry";
     }
//---
   return(str);
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_MAGIC"                              |
//+------------------------------------------------------------------+
long CDealInfo::Magic(void) const
  {
   return(HistoryDealGetInteger(m_ticket,DEAL_MAGIC));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_POSITION_ID"                        |
//+------------------------------------------------------------------+
long CDealInfo::PositionId(void) const
  {
   return(HistoryDealGetInteger(m_ticket,DEAL_POSITION_ID));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_VOLUME"                             |
//+------------------------------------------------------------------+
double CDealInfo::Volume(void) const
  {
   return(HistoryDealGetDouble(m_ticket,DEAL_VOLUME));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_PRICE_OPEN"                         |
//+------------------------------------------------------------------+
double CDealInfo::Price(void) const
  {
   return(HistoryDealGetDouble(m_ticket,DEAL_PRICE));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_COMMISSION"                         |
//+------------------------------------------------------------------+
double CDealInfo::Commission(void) const
  {
   return(HistoryDealGetDouble(m_ticket,DEAL_COMMISSION));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_SWAP"                               |
//+------------------------------------------------------------------+
double CDealInfo::Swap(void) const
  {
   return(HistoryDealGetDouble(m_ticket,DEAL_SWAP));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_PROFIT"                             |
//+------------------------------------------------------------------+
double CDealInfo::Profit(void) const
  {
   return(HistoryDealGetDouble(m_ticket,DEAL_PROFIT));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_SYMBOL"                             |
//+------------------------------------------------------------------+
string CDealInfo::Symbol(void) const
  {
   return(HistoryDealGetString(m_ticket,DEAL_SYMBOL));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_COMMENT"                            |
//+------------------------------------------------------------------+
string CDealInfo::Comment(void) const
  {
   return(HistoryDealGetString(m_ticket,DEAL_COMMENT));
  }
//+------------------------------------------------------------------+
//| Get the property value "DEAL_EXTERNAL_ID"                        |
//+------------------------------------------------------------------+
string CDealInfo::ExternalId(void) const
  {
   return(HistoryDealGetString(m_ticket,DEAL_EXTERNAL_ID));
  }
//+------------------------------------------------------------------+
//| Access functions HistoryDealGetInteger(...)                      |
//+------------------------------------------------------------------+
bool CDealInfo::InfoInteger(ENUM_DEAL_PROPERTY_INTEGER prop_id,long &var) const
  {
   return(HistoryDealGetInteger(m_ticket,prop_id,var));
  }
//+------------------------------------------------------------------+
//| Access functions HistoryDealGetDouble(...)                       |
//+------------------------------------------------------------------+
bool CDealInfo::InfoDouble(ENUM_DEAL_PROPERTY_DOUBLE prop_id,double &var) const
  {
   return(HistoryDealGetDouble(m_ticket,prop_id,var));
  }
//+------------------------------------------------------------------+
//| Access functions HistoryDealGetString(...)                       |
//+------------------------------------------------------------------+
bool CDealInfo::InfoString(ENUM_DEAL_PROPERTY_STRING prop_id,string &var) const
  {
   return(HistoryDealGetString(m_ticket,prop_id,var));
  }
//+------------------------------------------------------------------+
//| Converths the type of a  deal to text                            |
//+------------------------------------------------------------------+
string CDealInfo::FormatAction(string &str,const uint action) const
  {
//--- see the type
   switch(action)
     {
      case DEAL_TYPE_BUY:
         str="buy";
         break;
      case DEAL_TYPE_SELL:
         str="sell";
         break;
      case DEAL_TYPE_BALANCE:
         str="balance";
         break;
      case DEAL_TYPE_CREDIT:
         str="credit";
         break;
      case DEAL_TYPE_CHARGE:
         str="charge";
         break;
      case DEAL_TYPE_CORRECTION:
         str="correction";
         break;
      case DEAL_TYPE_BONUS:
         str="bonus";
         break;
      case DEAL_TYPE_COMMISSION:
         str="commission";
         break;
      case DEAL_TYPE_COMMISSION_DAILY:
         str="daily commission";
         break;
      case DEAL_TYPE_COMMISSION_MONTHLY:
         str="monthly commission";
         break;
      case DEAL_TYPE_COMMISSION_AGENT_DAILY:
         str="daily agent commission";
         break;
      case DEAL_TYPE_COMMISSION_AGENT_MONTHLY:
         str="monthly agent commission";
         break;
      case DEAL_TYPE_INTEREST:
         str="interest rate";
         break;
      case DEAL_TYPE_BUY_CANCELED:
         str="canceled buy";
         break;
      case DEAL_TYPE_SELL_CANCELED:
         str="canceled sell";
         break;
      default:
         str="unknown deal type "+(string)action;
     }
//--- return the result
   return(str);
  }
//+------------------------------------------------------------------+
//| Converts the deal direction to text                              |
//+------------------------------------------------------------------+
string CDealInfo::FormatEntry(string &str,const uint entry) const
  {
//--- see the type
   switch(entry)
     {
      case DEAL_ENTRY_IN:
         str="in";
         break;
      case DEAL_ENTRY_OUT:
         str="out";
         break;
      case DEAL_ENTRY_INOUT:
         str="in/out";
         break;
      case DEAL_ENTRY_OUT_BY:
         str="out by";
         break;
      default:
         str="unknown deal entry "+(string)entry;
     }
//--- return the result
   return(str);
  }
//+------------------------------------------------------------------+
//| Converts the deal parameters to text                             |
//+------------------------------------------------------------------+
string CDealInfo::FormatDeal(string &str) const
  {
   string type;
   long   tmp_long;
//--- set up
   string symbol_name=this.Symbol();
   int    digits=_Digits;
   if(SymbolInfoInteger(symbol_name,SYMBOL_DIGITS,tmp_long))
      digits=(int)tmp_long;
//--- form the description of the deal
   switch(DealType())
     {
      //--- Buy-Sell
      case DEAL_TYPE_BUY:
      case DEAL_TYPE_SELL:
         str=StringFormat("#%I64u %s %s %s at %s",
                          Ticket(),
                          FormatAction(type,DealType()),
                          DoubleToString(Volume(),2),
                          symbol_name,
                          DoubleToString(Price(),digits));
         break;

      //--- balance operations
      case DEAL_TYPE_BALANCE:
      case DEAL_TYPE_CREDIT:
      case DEAL_TYPE_CHARGE:
      case DEAL_TYPE_CORRECTION:
      case DEAL_TYPE_BONUS:
      case DEAL_TYPE_COMMISSION:
      case DEAL_TYPE_COMMISSION_DAILY:
      case DEAL_TYPE_COMMISSION_MONTHLY:
      case DEAL_TYPE_COMMISSION_AGENT_DAILY:
      case DEAL_TYPE_COMMISSION_AGENT_MONTHLY:
      case DEAL_TYPE_INTEREST:
         str=StringFormat("#%I64u %s %s [%s]",
                          Ticket(),
                          FormatAction(type,DealType()),
                          DoubleToString(Profit(),2),
                          this.Comment());
         break;

      default:
         str="unknown deal type "+(string)DealType();
     }
//--- return the result
   return(str);
  }
//+------------------------------------------------------------------+
//| Select a deal on the index                                       |
//+------------------------------------------------------------------+
bool CDealInfo::SelectByIndex(const int index)
  {
   ulong ticket=HistoryDealGetTicket(index);
   if(ticket==0)
      return(false);
   Ticket(ticket);
//---
   return(true);
  }
//+------------------------------------------------------------------+
