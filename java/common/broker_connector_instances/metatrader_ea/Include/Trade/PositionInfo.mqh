//+------------------------------------------------------------------+
//|                                                 PositionInfo.mqh |
//|                   Copyright 2009-2020, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
//+------------------------------------------------------------------+
//| Class CPositionInfo.                                             |
//| Appointment: Class for access to position info.                  |
//|              Derives from class CObject.                         |
//+------------------------------------------------------------------+
class CPositionInfo : public CObject
  {
protected:
   ENUM_POSITION_TYPE m_type;
   double            m_volume;
   double            m_price;
   double            m_stop_loss;
   double            m_take_profit;

public:
                     CPositionInfo(void);
                    ~CPositionInfo(void);
   //--- fast access methods to the integer position propertyes
   ulong             Ticket(void) const;
   datetime          Time(void) const;
   ulong             TimeMsc(void) const;
   datetime          TimeUpdate(void) const;
   ulong             TimeUpdateMsc(void) const;
   ENUM_POSITION_TYPE PositionType(void) const;
   string            TypeDescription(void) const;
   long              Magic(void) const;
   long              Identifier(void) const;
   //--- fast access methods to the double position propertyes
   double            Volume(void) const;
   double            PriceOpen(void) const;
   double            StopLoss(void) const;
   double            TakeProfit(void) const;
   double            PriceCurrent(void) const;
   double            Commission(void) const;
   double            Swap(void) const;
   double            Profit(void) const;
   //--- fast access methods to the string position propertyes
   string            Symbol(void) const;
   string            Comment(void) const;
   //--- access methods to the API MQL5 functions
   bool              InfoInteger(const ENUM_POSITION_PROPERTY_INTEGER prop_id,long &var) const;
   bool              InfoDouble(const ENUM_POSITION_PROPERTY_DOUBLE prop_id,double &var) const;
   bool              InfoString(const ENUM_POSITION_PROPERTY_STRING prop_id,string &var) const;
   //--- info methods
   string            FormatType(string &str,const uint type) const;
   string            FormatPosition(string &str) const;
   //--- methods for select position
   bool              Select(const string symbol);
   bool              SelectByMagic(const string symbol,const ulong magic);
   bool              SelectByTicket(const ulong ticket);
   bool              SelectByIndex(const int index);
   //---
   void              StoreState(void);
   bool              CheckState(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CPositionInfo::CPositionInfo(void) : m_type(WRONG_VALUE),
                                     m_volume(0.0),
                                     m_price(0.0),
                                     m_stop_loss(0.0),
                                     m_take_profit(0.0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CPositionInfo::~CPositionInfo(void)
  {
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_TICKET"                         |
//+------------------------------------------------------------------+
ulong CPositionInfo::Ticket(void) const
  {
   return((ulong)PositionGetInteger(POSITION_TICKET));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_TIME"                           |
//+------------------------------------------------------------------+
datetime CPositionInfo::Time(void) const
  {
   return((datetime)PositionGetInteger(POSITION_TIME));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_TIME_MSC"                       |
//+------------------------------------------------------------------+
ulong CPositionInfo::TimeMsc(void) const
  {
   return((ulong)PositionGetInteger(POSITION_TIME_MSC));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_TIME_UPDATE"                    |
//+------------------------------------------------------------------+
datetime CPositionInfo::TimeUpdate(void) const
  {
   return((datetime)PositionGetInteger(POSITION_TIME_UPDATE));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_TIME_UPDATE_MSC"                |
//+------------------------------------------------------------------+
ulong CPositionInfo::TimeUpdateMsc(void) const
  {
   return((ulong)PositionGetInteger(POSITION_TIME_UPDATE_MSC));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_TYPE"                           |
//+------------------------------------------------------------------+
ENUM_POSITION_TYPE CPositionInfo::PositionType(void) const
  {
   return((ENUM_POSITION_TYPE)PositionGetInteger(POSITION_TYPE));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_TYPE" as string                 |
//+------------------------------------------------------------------+
string CPositionInfo::TypeDescription(void) const
  {
   string str;
//---
   return(FormatType(str,PositionType()));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_MAGIC"                          |
//+------------------------------------------------------------------+
long CPositionInfo::Magic(void) const
  {
   return(PositionGetInteger(POSITION_MAGIC));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_IDENTIFIER"                     |
//+------------------------------------------------------------------+
long CPositionInfo::Identifier(void) const
  {
   return(PositionGetInteger(POSITION_IDENTIFIER));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_VOLUME"                         |
//+------------------------------------------------------------------+
double CPositionInfo::Volume(void) const
  {
   return(PositionGetDouble(POSITION_VOLUME));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_PRICE_OPEN"                     |
//+------------------------------------------------------------------+
double CPositionInfo::PriceOpen(void) const
  {
   return(PositionGetDouble(POSITION_PRICE_OPEN));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_SL"                             |
//+------------------------------------------------------------------+
double CPositionInfo::StopLoss(void) const
  {
   return(PositionGetDouble(POSITION_SL));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_TP"                             |
//+------------------------------------------------------------------+
double CPositionInfo::TakeProfit(void) const
  {
   return(PositionGetDouble(POSITION_TP));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_PRICE_CURRENT"                  |
//+------------------------------------------------------------------+
double CPositionInfo::PriceCurrent(void) const
  {
   return(PositionGetDouble(POSITION_PRICE_CURRENT));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_COMMISSION"                     |
//+------------------------------------------------------------------+
double CPositionInfo::Commission(void) const
  {
   return(PositionGetDouble(POSITION_COMMISSION));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_SWAP"                           |
//+------------------------------------------------------------------+
double CPositionInfo::Swap(void) const
  {
   return(PositionGetDouble(POSITION_SWAP));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_PROFIT"                         |
//+------------------------------------------------------------------+
double CPositionInfo::Profit(void) const
  {
   return(PositionGetDouble(POSITION_PROFIT));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_SYMBOL"                         |
//+------------------------------------------------------------------+
string CPositionInfo::Symbol(void) const
  {
   return(PositionGetString(POSITION_SYMBOL));
  }
//+------------------------------------------------------------------+
//| Get the property value "POSITION_COMMENT"                        |
//+------------------------------------------------------------------+
string CPositionInfo::Comment(void) const
  {
   return(PositionGetString(POSITION_COMMENT));
  }
//+------------------------------------------------------------------+
//| Access functions PositionGetInteger(...)                         |
//+------------------------------------------------------------------+
bool CPositionInfo::InfoInteger(const ENUM_POSITION_PROPERTY_INTEGER prop_id,long &var) const
  {
   return(PositionGetInteger(prop_id,var));
  }
//+------------------------------------------------------------------+
//| Access functions PositionGetDouble(...)                          |
//+------------------------------------------------------------------+
bool CPositionInfo::InfoDouble(const ENUM_POSITION_PROPERTY_DOUBLE prop_id,double &var) const
  {
   return(PositionGetDouble(prop_id,var));
  }
//+------------------------------------------------------------------+
//| Access functions PositionGetString(...)                          |
//+------------------------------------------------------------------+
bool CPositionInfo::InfoString(const ENUM_POSITION_PROPERTY_STRING prop_id,string &var) const
  {
   return(PositionGetString(prop_id,var));
  }
//+------------------------------------------------------------------+
//| Converts the position type to text                               |
//+------------------------------------------------------------------+
string CPositionInfo::FormatType(string &str,const uint type) const
  {
//--- see the type
   switch(type)
     {
      case POSITION_TYPE_BUY:
         str="buy";
         break;
      case POSITION_TYPE_SELL:
         str="sell";
         break;
      default:
         str="unknown position type "+(string)type;
     }
//--- return the result
   return(str);
  }
//+------------------------------------------------------------------+
//| Converts the position parameters to text                         |
//+------------------------------------------------------------------+
string CPositionInfo::FormatPosition(string &str) const
  {
   string tmp,type;
   long   tmp_long;
   ENUM_ACCOUNT_MARGIN_MODE margin_mode=(ENUM_ACCOUNT_MARGIN_MODE)AccountInfoInteger(ACCOUNT_MARGIN_MODE);
//--- set up
   string symbol_name=this.Symbol();
   int    digits=_Digits;
   if(SymbolInfoInteger(symbol_name,SYMBOL_DIGITS,tmp_long))
      digits=(int)tmp_long;
//--- form the position description
   if(margin_mode==ACCOUNT_MARGIN_MODE_RETAIL_HEDGING)
      str=StringFormat("#%I64u %s %s %s %s",
                       Ticket(),
                       FormatType(type,PositionType()),
                       DoubleToString(Volume(),2),
                       symbol_name,
                       DoubleToString(PriceOpen(),digits+3));
   else
      str=StringFormat("%s %s %s %s",
                       FormatType(type,PositionType()),
                       DoubleToString(Volume(),2),
                       symbol_name,
                       DoubleToString(PriceOpen(),digits+3));
//--- add stops if there are any
   double sl=StopLoss();
   double tp=TakeProfit();
   if(sl!=0.0)
     {
      tmp=StringFormat(" sl: %s",DoubleToString(sl,digits));
      str+=tmp;
     }
   if(tp!=0.0)
     {
      tmp=StringFormat(" tp: %s",DoubleToString(tp,digits));
      str+=tmp;
     }
//--- return the result
   return(str);
  }
//+------------------------------------------------------------------+
//| Access functions PositionSelect(...)                             |
//+------------------------------------------------------------------+
bool CPositionInfo::Select(const string symbol)
  {
   return(PositionSelect(symbol));
  }
//+------------------------------------------------------------------+
//| Access functions PositionSelect(...)                             |
//+------------------------------------------------------------------+
bool CPositionInfo::SelectByMagic(const string symbol,const ulong magic)
  {
   bool res=false;
   uint total=PositionsTotal();
//---
   for(uint i=0; i<total; i++)
     {
      string position_symbol=PositionGetSymbol(i);
      if(position_symbol==symbol && magic==PositionGetInteger(POSITION_MAGIC))
        {
         res=true;
         break;
        }
     }
//---
   return(res);
  }
//+------------------------------------------------------------------+
//| Access functions PositionSelectByTicket(...)                     |
//+------------------------------------------------------------------+
bool CPositionInfo::SelectByTicket(const ulong ticket)
  {
   return(PositionSelectByTicket(ticket));
  }
//+------------------------------------------------------------------+
//| Select a position on the index                                   |
//+------------------------------------------------------------------+
bool CPositionInfo::SelectByIndex(const int index)
  {
   ulong ticket=PositionGetTicket(index);
   return(ticket>0);
  }
//+------------------------------------------------------------------+
//| Stored position's current state                                  |
//+------------------------------------------------------------------+
void CPositionInfo::StoreState(void)
  {
   m_type       =PositionType();
   m_volume     =Volume();
   m_price      =PriceOpen();
   m_stop_loss  =StopLoss();
   m_take_profit=TakeProfit();
  }
//+------------------------------------------------------------------+
//| Check position change                                            |
//+------------------------------------------------------------------+
bool CPositionInfo::CheckState(void)
  {
   if(m_type==PositionType()  &&
      m_volume==Volume()      &&
      m_price==PriceOpen()    &&
      m_stop_loss==StopLoss() &&
      m_take_profit==TakeProfit())
      return(false);
//---
   return(true);
  }
//+------------------------------------------------------------------+
