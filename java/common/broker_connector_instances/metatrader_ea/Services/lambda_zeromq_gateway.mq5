//+-------------------------------------------------------------------+
//|     DWX_ZeroMQ_Service_v1.0.0.mq5                                 |
//|     Based on DWX_ZeroMQ_Server_v2.0.1_RC8.mq4                     |
//|     @author: Darwinex Labs (www.darwinex.com)                     |
//|                                                                   |
//|     Last Updated: October 02, 2019                                |
//|                                                                   |
//|     Copyright (c) 2017-2019, Darwinex. All rights reserved.       |
//|                                                                   |
//|     Licensed under the BSD 3-Clause License, you may not use this |
//|     file except in compliance with the License.                   |
//|                                                                   |
//|     You may obtain a copy of the License at:                      |
//|     https://opensource.org/licenses/BSD-3-Clause                  |
//+-------------------------------------------------------------------+
#property service
#property copyright "Copyright 2017-2019, Darwinex Labs."
#property link      "https://www.darwinex.com/"
#property version   "1.0"

// Required: MQL-ZMQ from https://github.com/dingmaotu/mql-zmq
#include <Zmq/Zmq.mqh>
// Transactions helper class From Standard Library
#include <Trade/Trade.mqh>
#include <Generic/ArrayList.mqh>
//+------------------------------------------------------------------+
//|  Enumerations to simplify client-server communication            |
//+------------------------------------------------------------------+
// NOTE: There is not too many actions and all could be replaced
// by one number. No need to send and process
// two strings: TRADE|DATA and ACTION.
enum ENUM_DWX_SERV_ACTION {
   HEARTBEAT=0,
   POS_OPEN=1,
   POS_MODIFY=2,
   POS_CLOSE=3,
   POS_CLOSE_PARTIAL=4,
   POS_CLOSE_MAGIC=5,
   POS_CLOSE_ALL=6,
   ORD_OPEN=7,
   ORD_MODIFY=8,
   ORD_DELETE=9,
   ORD_DELETE_ALL=10,
   GET_POSITIONS=11,
   GET_PENDING_ORDERS=12,
   GET_DATA=13,
   GET_TICK_DATA=14
};

input string PROJECT_NAME="lambda_zeromq_gateway";
input string ZEROMQ_PROTOCOL="tcp";
input string HOSTNAME="127.0.0.1";
input int PUSH_PORT = 32768;
input int PULL_PORT = 32769;
input int PUB_PORT=32770;
// Real max. time resolution of Sleep() and Timer() function
// is usually >= 10-16 milliseconds. This is due to
// hardware limitations in typical OS configuration.
input int REFRESH_INTERVAL=0;

input string t0="--- Trading Parameters ---";
input int MagicNumber=123456;
input int MaximumOrders=25;
input double MaximumLotSize=0.6;
input int MaximumSlippage=3;
input string t1="--- ZeroMQ Configuration ---";
input bool Publish_Tick_MarketData=false;//Publish Tick
input bool Publish_Depth_MarketData=true;//Publish Depth

bool Hedge_to_net = true;//Hedge to net account
input string SuffixDepthSymbol = "";
input bool Close_Hedged_Positions=true;// close trades if are in reversed side

input bool Verbose_market_data=false;
input bool Verbose=true;
input bool Verbose_on_Position=false;

bool Publish_Trades=true;// Publish Trades
int maxDepthLevels=10;
//string Publish_Symbols[]= {"EURUSD"};
//string Publish_Symbols[9]= {"EURGBP","EURUSD","EURAUD","EURNZD","EURJPY","EURCHF","EURCAD","XAUUSD","XAGUSD"};
string Publish_Symbols[]= {"EURUSD","GBPUSD","EURGBP","EURJPY","EURNZD","EURCHF","EURAUD","EURCAD","EURAUD","NZDUSD","AUDUSD","EURSEK","USDNOK","USDSEK","EURNOK","EURMXN","EURTRY"};
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
ulong  Last_MqlTicks_Times_Msc[ArraySize(Publish_Symbols)];
int Publish_Symbols_Digits[ArraySize(Publish_Symbols)];
string lastDepthSend[ArraySize(Publish_Symbols)];//={"","","","","","",""};
string lastTickSend[ArraySize(Publish_Symbols)];

ulong   startTimestamp;
ulong  LastOrdersTicket[];
long  LastOrdersPositionId[];
/*
string Publish_Symbols[28] = {
   "EURUSD","EURGBP","EURAUD","EURNZD","EURJPY","EURCHF","EURCAD",
   "GBPUSD","AUDUSD","NZDUSD","USDJPY","USDCHF","USDCAD","GBPAUD",
   "GBPNZD","GBPJPY","GBPCHF","GBPCAD","AUDJPY","CHFJPY","CADJPY",
   "AUDNZD","AUDCHF","AUDCAD","NZDJPY","NZDCHF","NZDCAD","CADCHF"
};
ulong  Last_MqlTicks_Times_Msc[28];
int Publish_Symbols_Digits[28];
*/
// CREATE ZeroMQ Context
Context context(PROJECT_NAME);

// CREATE ZMQ_PUSH SOCKET
Socket pushSocket(context,ZMQ_PUSH);

// CREATE ZMQ_PULL SOCKET
Socket pullSocket(context,ZMQ_PULL);

// CREATE ZMQ_PUB SOCKET
Socket pubSocket(context,ZMQ_PUB);

// VARIABLES FOR LATER
uchar _data[];
ZmqMsg zmq_request;

CArrayList <string> *SymbolsErrorDepth;

// Object to perform transactions.
// In MQL5 transactions management is much more complicated than
// in MQL4. Using CTrade class will be the best idea in this case.
CTrade tradeHelper;

//+------------------------------------------------------------------+
//| Service program start function                                   |
//+------------------------------------------------------------------+
void OnStart() {
   startTimestamp = TimeCurrent();
   Print("Initializing for "+IntegerToString(ArraySize(Publish_Symbols))+" instruments");
   if(!ServiceInit()) {
      Print("Service initialization failed!");
      //Clean up before leave
      ServiceDeinit();
      return;
   }
   SymbolsErrorDepth = new CArrayList <string>();
   Sleep(5000);



// while(!IsStopped())
   while(CheckServiceStatus()) {
      if(Publish_Tick_MarketData) {
         CollectAndPublishTicks();
      }

      if(Publish_Depth_MarketData) {
         CollectAndPublishDepth();
      }
      if(Publish_Trades) {
         CollectAndPublishTrades();
      }
      if(Close_Hedged_Positions) {
         ClosedHedgedPositions();
      }

      // Code section used to get and respond to commands
      if(CheckServiceStatus()) {
         // Get client's response, but don't block.
         pullSocket.recv(zmq_request,true);

         if(zmq_request.size()>0)
            MessageHandler(zmq_request);
      }

      Sleep(REFRESH_INTERVAL);
   }
   ServiceDeinit();
}
//+------------------------------------------------------------------+
//|  Service initialization function                                 |
//+------------------------------------------------------------------+
bool ServiceInit(void) {
   bool init_result=false;
   context.setBlocky(false);
   /* Set Socket Options */

// Send responses to PULL_PORT that client is listening on.
   pullSocket.setSendHighWaterMark(1);
   pullSocket.setLinger(0);
   Print("[PULL] Binding MT5 Server to Socket on Port "+IntegerToString(PULL_PORT)+"..");
   bool bindPush = pushSocket.bind(StringFormat("%s://%s:%d",ZEROMQ_PROTOCOL,HOSTNAME,PULL_PORT));
   init_result=init_result || bindPush;

// Receive commands from PUSH_PORT that client is sending to.
   pushSocket.setReceiveHighWaterMark(1);
   pushSocket.setLinger(0);
   Print("[PUSH] Binding MT5 Server to Socket on Port "+IntegerToString(PUSH_PORT)+"..");
   bool bindPull = pullSocket.bind(StringFormat("%s://%s:%d",ZEROMQ_PROTOCOL,HOSTNAME,PUSH_PORT));
   init_result=init_result || bindPull;

   if(Publish_Tick_MarketData==true|| Publish_Depth_MarketData==true) {
      // Send new market data to PUB_PORT that client is subscribed to.
      pubSocket.setSendHighWaterMark(1);
      pubSocket.setLinger(0);
      Print("[PUB] Binding MT5 Server to Socket on Port "+IntegerToString(PUB_PORT)+"..");
      bool bindPub = pubSocket.bind(StringFormat("%s://%s:%d",ZEROMQ_PROTOCOL,HOSTNAME,PUB_PORT));
      init_result=init_result || bindPub;

      // Here last ticks timestamps for all 'Publish_Symbols' are collected.
      // See 'CollectAndPublish' function.
      MqlTick last_tick;
      ulong time_current_msc=TimeCurrent()*1000;
      for(int i=0; i<ArraySize(Publish_Symbols); i++) {
         if(SymbolInfoTick(Publish_Symbols[i],last_tick)) {
            Last_MqlTicks_Times_Msc[i]=last_tick.time_msc;
         } else {
            Last_MqlTicks_Times_Msc[i]=time_current_msc;
         }
         // collecting '_Digits' for each symbol
         Publish_Symbols_Digits[i]=(int)SymbolInfoInteger(Publish_Symbols[i],SYMBOL_DIGITS);
      }

      //subscribe to all depth instruments
      if(Publish_Depth_MarketData) {
         for(int i=0; i<ArraySize(Publish_Symbols); i++) {
            MarketBookAdd(Publish_Symbols[i]);
         }
      }

   }


// Setting up few attributes of trading management object.
// By default 'tradeHelper' will work only with trades placed by itself.
   tradeHelper.SetExpertMagicNumber(MagicNumber);
   tradeHelper.SetMarginMode();
   tradeHelper.SetDeviationInPoints(MaximumSlippage);
   tradeHelper.SetAsyncMode(false);

   Print("'"+PROJECT_NAME+"' started.");
   return init_result;
}
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void ServiceDeinit(void) {
   Print("[PUSH] Unbinding MT5 Server from Socket on Port "+IntegerToString(PULL_PORT)+"..");
   pushSocket.unbind(StringFormat("%s://%s:%d", ZEROMQ_PROTOCOL, HOSTNAME, PULL_PORT));
   pushSocket.disconnect(StringFormat("%s://%s:%d", ZEROMQ_PROTOCOL, HOSTNAME, PULL_PORT));

   Print("[PULL] Unbinding MT5 Server from Socket on Port "+IntegerToString(PUSH_PORT)+"..");
   pullSocket.unbind(StringFormat("%s://%s:%d", ZEROMQ_PROTOCOL, HOSTNAME, PUSH_PORT));
   pullSocket.disconnect(StringFormat("%s://%s:%d", ZEROMQ_PROTOCOL, HOSTNAME, PUSH_PORT));

   if(Publish_Tick_MarketData==true ||Publish_Depth_MarketData==true) {
      Print("[PUB] Unbinding MT5 Server from Socket on Port "+IntegerToString(PUB_PORT)+"..");
      pubSocket.unbind(StringFormat("%s://%s:%d", ZEROMQ_PROTOCOL, HOSTNAME, PUB_PORT));
      pubSocket.disconnect(StringFormat("%s://%s:%d", ZEROMQ_PROTOCOL, HOSTNAME, PUB_PORT));
   }
   if(SymbolsErrorDepth!=NULL) {
      SymbolsErrorDepth.Clear();
      SymbolsErrorDepth=NULL;
   }
// Destroy ZeroMQ Context
   context.destroy(0);

   Print("'"+PROJECT_NAME+"' stopped.");
}
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
bool CheckServiceStatus() {
   if(IsStopped()) {
      InformPullClient(pullSocket,"{'_response': 'SERVICE_IS_STOPPED'}");
      return(false);
   }
   return(true);
}



//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void PublishMessage(string message) {
   ZmqMsg pushReply(" " +message);
   pubSocket.send(pushReply,true); // NON-BLOCKING
}
//+------------------------------------------------------------------+
//|  Procedure collecting and publishing new rates data              |
//+------------------------------------------------------------------+
// Unlike using functions 'OnTick' and 'SymbolInfoTick'
// here no any new quote will be missed. Additionally every rate
// will be sent only once.
// Note: If mql4|mql5 application queue already contains NewTick event
// or OnTick|OnCalculate function is been executed no new NewTick event
// is added to the queue. The terminal can simultaneously receive
// few ticks but OnTick procedure will be called only once.
void CollectAndPublishTicks(void) {

   for(int i=0; i<ArraySize(Publish_Symbols); i++) {
      string symbol = Publish_Symbols[i];
      MqlTick tick;
      SymbolInfoTick(symbol,tick);

      string data_str = "{type:TICK,symbol:"+symbol+", Time:"+IntegerToString(TimeGMT())+",";


      // '#' - single tick data delimiter (if more than one new tick)

      // Note: time in milliseconds since 1970.01.01 00:00:00.001. To be formatted on client side.
      // Note 2: date sent in mql string format: 23 bytes, raw miliseconds: 13 bytes
      data_str+="timestamp:"+IntegerToString(tick.time_msc)+
                ",best_bid:"+DoubleToString(tick.bid,Publish_Symbols_Digits[i])+
                ",best_ask:"+DoubleToString(tick.ask,Publish_Symbols_Digits[i])+
                ",last:" +DoubleToString(tick.last,Publish_Symbols_Digits[i])+
                ",last_volume:" +DoubleToString(tick.volume_real,Publish_Symbols_Digits[i])
                ;


      // Data published in format:
      // 'SYMBOL_NAME MILISECONDS;BID_PRICE;ASK_PRICE#MILISECONDS;BID_PRICE;ASK_PRICE....'
      string data_to_pub=data_str+"}";

      if(lastTickSend[i]!=NULL && lastTickSend[i]==data_to_pub) {
         continue;
      }

      if(Verbose_market_data)
         Print("Sending "+data_to_pub+" to PUB Socket "+IntegerToString(PUB_PORT));

      lastTickSend[i]=data_to_pub;
      PublishMessage(data_to_pub);
   }
}
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void CollectAndPublishTrades(void) {
   int     total=PositionsTotal();
   ulong    position_ticket;

   for(int i=0; i<total; i++) {
      //--- return order ticket by its position in the list
      if((position_ticket=PositionGetTicket(i))>0) {
         ulong time = PositionGetInteger(POSITION_TIME_UPDATE);
         if(time<startTimestamp) {
            continue;
         }

         string symbol = PositionGetString(POSITION_SYMBOL);
         long type =PositionGetInteger(POSITION_TYPE);
         if(type!=POSITION_TYPE_BUY && type!=POSITION_TYPE_SELL) {
            continue;
         }

         string comment =PositionGetString(POSITION_COMMENT);
         double fillQty = PositionGetDouble(POSITION_VOLUME);
         long orderId = PositionGetInteger(POSITION_TICKET);
         long positionId = PositionGetInteger(POSITION_IDENTIFIER);

         long magic = PositionGetInteger(POSITION_MAGIC);
         if(magic!= MagicNumber) {
            continue;
         }
         bool isAlreadyInformed =  false;
         for(int orders_informed=0; orders_informed<ArraySize(LastOrdersTicket); orders_informed++) {
            ulong ticket_informed = LastOrdersTicket[orders_informed];
            long positionId_informed = LastOrdersPositionId[orders_informed];
            isAlreadyInformed =  position_ticket==ticket_informed && positionId==positionId_informed;
            if(isAlreadyInformed) {
               break;
            }
         }
         if(isAlreadyInformed) {
            continue;
         }

         //notify trade
         string data_str ="{'_action': 'TRADE'";
         data_str +=", '_magic': '"+IntegerToString(magic)+"'";
         data_str +=", '_ticket': '"+IntegerToString(position_ticket)+"'";
         data_str +=", '_last_qty': '"+DoubleToString(fillQty)+"'";
         data_str+="}";


         InformPullClient(pushSocket,data_str);
         //save las notified status
         ArrayResize(LastOrdersTicket,ArraySize(LastOrdersTicket)+1);
         ArrayResize(LastOrdersPositionId,ArraySize(LastOrdersPositionId)+1);
         LastOrdersTicket[ArraySize(LastOrdersTicket)-1]=position_ticket;
         LastOrdersPositionId[ArraySize(LastOrdersTicket)-1]=positionId;




      }
   }
}

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void CollectAndPublishDepth(void) {
   MqlTick tick_array[];
   for(int i=0; i<ArraySize(Publish_Symbols); i++) {
      string symbol = Publish_Symbols[i];
      if(SymbolsErrorDepth.Contains(symbol)) {
         continue;
      }
      //printf("Depth of Market " + symbol +  " changed");
      MqlBookInfo book[];
      string symbolComplete = symbol+SuffixDepthSymbol;
      MarketBookGet(symbolComplete, book);
      if(ArraySize(book) == 0) {
         int lastErrorReason = GetLastError();
         if(lastErrorReason != 5039) {
            if(!SymbolsErrorDepth.Contains(symbol)) {
               SymbolsErrorDepth.Add(symbol);
               printf("Failed load market book price "+symbol +": "+symbolComplete+" . Reason: " + (string)GetLastError());
            }
         }
         continue;
      }
      string data_str = "{type:DEPTH,symbol:"+symbol+", Time:"+IntegerToString(TimeGMT())+",";
      string ask_side_str="";
      string bid_side_str="";

      string prefix="";
      int askLevel=0;
      int bidLevel=0;
      int other_level=0;
      double askPrice [];
      string askQty [];
      ArrayResize(askPrice,maxDepthLevels);
      ArrayResize(askQty,maxDepthLevels);
      for(int level=0; level<ArraySize(book); level++) {
         //BOOK_TYPE_SELL BOOK_TYPE_BUY BOOK_TYPE_SELL_MARKET BOOK_TYPE_BUY_MARKET
         if(book[level].type==1) {
            if(askLevel+1>ArraySize(askPrice)) {
               ArrayResize(askPrice,ArraySize(askPrice)+1);
               ArrayResize(askQty,ArraySize(askQty)+1);
               maxDepthLevels++;
            }
            askPrice[askLevel] = book[level].price;
            askQty[askLevel] = (string)book[level].volume_real;
            askLevel++;
         }

         else if(book[level].type==2) {
            prefix="BID";
            bid_side_str +=prefix+"_PRICE_"+IntegerToString(bidLevel)+": "+ DoubleToString(book[level].price, Publish_Symbols_Digits[i]) + ",";
            bid_side_str +=prefix+"_QTY_"+IntegerToString(bidLevel)+": " + (string)book[level].volume_real + ",";
            bidLevel++;
         } else {
            other_level++;
            continue;
         }


      }

      //invert the ask size
      prefix="ASK";
      for(int level=0; level<askLevel; level++) {
         double price = askPrice[level];
         string qty=(string)askQty[level];
         ask_side_str +=prefix+"_PRICE_"+IntegerToString(askLevel-level-1)+": "+ DoubleToString(price) + ",";
         ask_side_str +=prefix+"_QTY_"+IntegerToString(askLevel-level-1)+": " + qty + ",";
      }


      data_str+=ask_side_str+bid_side_str;
      data_str=StringSubstr(data_str,0,StringLen(data_str)-1);
      data_str+="}";
      // 'SYMBOL_NAME MILISECONDS;BID_PRICE_0..4;BID_QTY_0...4;ASK_PRICE_0..4;ASK_QTY_0...4'
      string data_to_pub=data_str;
      if(lastDepthSend[i]!=NULL && data_to_pub==lastDepthSend[i]) {
         continue;
      }
      if(data_to_pub== NULL || data_to_pub=="") {
         continue;
      }

      if(Verbose_market_data) {
         Print("Sending "+data_to_pub+" to PUB Socket "+IntegerToString(PUB_PORT));
      }
      lastDepthSend[i]=data_to_pub;
      PublishMessage(data_to_pub);
   }
}

//+------------------------------------------------------------------+
//| BookEvent function                                               |
//+------------------------------------------------------------------+
void OnBookEvent(const string &symbol) {
   ;
}


//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void MessageHandler(ZmqMsg &_request) {
   string components[10];

   if(_request.size()>0) {
      // Get data from request
      ArrayResize(_data,(int)(_request.size()));
      _request.getData(_data);
      string dataStr=CharArrayToString(_data);
      if(dataStr!="11") {
         Print("Received "+dataStr+" to PULL Socket "+IntegerToString(PULL_PORT));
      }
      // Process data
      ParseZmqMessage(dataStr,components);

      // Interpret data
      InterpretZmqMessage(pushSocket,components);
   }
}
//+------------------------------------------------------------------+
//|  Interpret Zmq Message and perform actions                       |
//+------------------------------------------------------------------+
void InterpretZmqMessage(Socket &pSocket,string &compArray[]) {
// IMPORTANT NOTE: In MT5 there are ORDERS (market, pending),
// POSITIONS which are result of one or more DEALS.
// Already closed POSITIONS can be accessed only by corresponding DEALS.
// IMPORTANT NOTE2: size of compArray is 10, not 11 as in original version!
// Message Structures:

// 1) Trading
// ENUM_DWX_SERV_ACTION[from 1 to 10]|TYPE|SYMBOL|PRICE|SL|TP|COMMENT|MAGIC|VOLUME|TICKET
// e.g. POS_OPEN|1|EURUSD|0|50|50|Python-to-MetaTrader5|12345678|0.01

// The 12345678 at the end is the ticket ID, for MODIFY and CLOSE.

// 2) Data Requests: DWX_SERV_ACTION = GET_DATA (OHLC and candle open time)
// or GET_TICK_DATA (tick time in millisecond, bid, ask)
// 2.1) GET_DATA|SYMBOL|TIMEFRAME|START_DATETIME|END_DATETIME
// 2.2) GET_TICK_DATA|SYMBOL|START_DATETIME|END_DATETIME

// NOTE: datetime has format: 'YYYY.MM.DD hh:mm:ss'

   /*
         If compArray[0] = ACTION: one from [POS_OPEN,POS_MODIFY,POS_CLOSE,
            POS_CLOSE_PARTIAL,POS_CLOSE_MAGIC,POS_CLOSE_ALL,ORD_OPEN,ORD_MODIFY,ORD_DELETE,ORD_DELETE_ALL]
            compArray[1] = TYPE: one from [ORDER_TYPE_BUY,ORDER_TYPE_SELL only used when ACTION=POS_OPEN]
            or from [ORDER_TYPE_BUY_LIMIT,ORDER_TYPE_SELL_LIMIT,ORDER_TYPE_BUY_STOP,
            ORDER_TYPE_SELL_STOP only used when ACTION=ORD_OPEN]

            ORDER TYPES:
            https://www.mql5.com/en/docs/constants/tradingconstants/orderproperties#enum_order_type

            ORDER_TYPE_BUY = 0
            ORDER_TYPE_SELL = 1
            ORDER_TYPE_BUY_LIMIT = 2
            ORDER_TYPE_SELL_LIMIT = 3
            ORDER_TYPE_BUY_STOP = 4
            ORDER_TYPE_SELL_STOP = 5

            In this version ORDER_TYPE_BUY_STOP_LIMIT, ORDER_TYPE_SELL_STOP_LIMIT
            and ORDER_TYPE_CLOSE_BY are ignored.

            compArray[2] = Symbol (e.g. EURUSD, etc.)
            compArray[3] = Open/Close Price (ignored if ACTION = POS_MODIFY|ORD_MODIFY)
            compArray[4] = SL
            compArray[5] = TP
            compArray[6] = Trade Comment
            compArray[7] = Lots
            compArray[8] = Magic Number
            compArray[9] = Ticket Number (all type of modify|close|delete)
      */

// Only simple number to process
   ENUM_DWX_SERV_ACTION switch_action=(ENUM_DWX_SERV_ACTION)StringToInteger(compArray[0]);

   /* Setup processing variables */
   string zmq_ret="";
   string ret = "";
   int ticket = -1;
   bool ans=false;

   StringReplace(compArray[3],",",".");
   StringReplace(compArray[7],",",".");
   double new_price = StringToDouble(compArray[3]);
   double new_qty = StringToDouble(compArray[7]);
   long ticketOrig=0;
   string lambdaMagicNumber = compArray[8];
   /****************************
       * PERFORM SOME CHECKS HERE *
       ****************************/
   if(CheckOpsStatus(pSocket,(int)switch_action)==true) {
      switch(switch_action) {
      case HEARTBEAT:

         InformPullClient(pSocket,"{'_action': 'heartbeat', '_response': 'loud and clear!'}");
         break;

      case POS_OPEN: {
         zmq_ret="{";
         string symbol = compArray[2];
         double size = StringToDouble(compArray[7]);
         size = round(size*100.0)/100.0;
         double price = StringToDouble(compArray[3]);
         int typeOrder = (int)StringToInteger(compArray[1]);
         double SL = StringToDouble(compArray[4]);
         double TP = StringToDouble(compArray[5]);
         string commentOrder = compArray[6];

         ticket=DWX_PositionOpen(symbol,typeOrder,size,price,SL,TP,
                                 commentOrder,lambdaMagicNumber,zmq_ret,pSocket);
         InformPullClient(pSocket,zmq_ret+"}");
      }
      break;

      case POS_MODIFY:

         zmq_ret="{'_action': 'POSITION_MODIFY'";
         ans=DWX_PositionModify((int)StringToInteger(compArray[9]),StringToDouble(compArray[4]),StringToDouble(compArray[5]),zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");
         break;

      case POS_CLOSE_PARTIAL: {
         zmq_ret="{";
         zmq_ret+="'_action': 'CLOSE'";
         zmq_ret+=",'_ticket': "+compArray[9];
      }
      case POS_CLOSE: {
         string symbol = compArray[2];
         PositionSelect(symbol);
         double sizeClose = StringToDouble(compArray[7]);
         sizeClose = round(sizeClose*100.0)/100.0;

         if (symbol=="") {
            DWX_PositionsClose_All(zmq_ret);
         } else {
            ans=DWX_CloseAtMarket(sizeClose,zmq_ret,symbol);
         }
         InformPullClient(pSocket,zmq_ret+"}");

      }
      break;
      case POS_CLOSE_MAGIC:

         zmq_ret="{";
         DWX_PositionClose_Magic(MagicNumber,zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      case POS_CLOSE_ALL:

         zmq_ret="{";
         DWX_PositionsClose_All(zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      case ORD_OPEN:

         zmq_ret="{";
         ticket=DWX_OrderOpen(compArray[2],(int)StringToInteger(compArray[1]),StringToDouble(compArray[7]),
                              StringToDouble(compArray[3]),(int)StringToInteger(compArray[4]),(int)StringToInteger(compArray[5]),
                              compArray[6],lambdaMagicNumber,zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      case ORD_MODIFY:
         zmq_ret="{'_action': 'ORDER_MODIFY'";
         ticketOrig = StringToInteger(compArray[9]);

         ans=DWX_OrderModify(ticketOrig,StringToDouble(compArray[4]),StringToDouble(compArray[5]),new_price,new_qty,compArray[8],zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      case ORD_DELETE:
         ticketOrig = StringToInteger(compArray[9]);
         zmq_ret="{";
         DWX_PendingOrderDelete_Ticket(ticketOrig,lambdaMagicNumber,zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      case ORD_DELETE_ALL:

         zmq_ret="{";
         DWX_PendingOrderDelete_All(zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      case GET_POSITIONS:

         zmq_ret="{";
         DWX_GetOpenPositions(zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}",Verbose_on_Position);

         break;
      case GET_PENDING_ORDERS:

         zmq_ret="{";
         DWX_GetPendingOrders(zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      case GET_DATA:

         zmq_ret="{";
         DWX_GetData(compArray,zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      case GET_TICK_DATA:

         zmq_ret="{";
         DWX_GetTickData(compArray,zmq_ret);
         InformPullClient(pSocket,zmq_ret+"}");

         break;
      default:
         break;
      }
   }
}
//+------------------------------------------------------------------+
//|  Check if operations are permitted                               |
//+------------------------------------------------------------------+
bool CheckOpsStatus(Socket &pSocket,int sFlag) {
   if(sFlag<=10 && sFlag>0) {
      if(!MQLInfoInteger(MQL_TRADE_ALLOWED)) {
         InformPullClient(pSocket,"{'_response': 'TRADING_IS_NOT_ALLOWED__ABORTED_COMMAND'}");
         return(false);
      } else if(!AccountInfoInteger(ACCOUNT_TRADE_EXPERT)) {
         InformPullClient(pSocket,"{'_response': 'EA_IS_DISABLED__ABORTED_COMMAND'}");
         return(false);
      } else if(!TerminalInfoInteger(TERMINAL_DLLS_ALLOWED)) {
         InformPullClient(pSocket,"{'_response': 'DLLS_DISABLED__ABORTED_COMMAND'}");
         return(false);
      } else if(!MQLInfoInteger(MQL_DLLS_ALLOWED)) {
         InformPullClient(pSocket,"{'_response': 'LIBS_DISABLED__ABORTED_COMMAND'}");
         return(false);
      } else if(!TerminalInfoInteger(TERMINAL_CONNECTED)) {
         InformPullClient(pSocket,"{'_response': 'NO_BROKER_CONNECTION__ABORTED_COMMAND'}");
         return(false);
      }
   }
   return(true);
}
//+------------------------------------------------------------------+
//|  Parse Zmq Message                                               |
//+------------------------------------------------------------------+
void ParseZmqMessage(string &message,string &retArray[]) {
   string sep="|";
   ushort u_sep=StringGetCharacter(sep,0);
   int splits=StringSplit(message,u_sep,retArray);
}
//+------------------------------------------------------------------+
//|  Get data for request datetime range                             |
//+------------------------------------------------------------------+
void DWX_GetData(string &compArray[],string &zmq_ret) {
// GET_DATA == 13
// Format: 13|SYMBOL|TIMEFRAME|START_DATETIME|END_DATETIME

   double open_array[],high_array[],low_array[],close_array[];
   datetime time_array[];

// Get open prices
   int open_count=CopyOpen(compArray[1],
                           (ENUM_TIMEFRAMES)StringToInteger(compArray[2]),StringToTime(compArray[3]),
                           StringToTime(compArray[4]),open_array);
// Get close prices
   int close_count=CopyClose(compArray[1],
                             (ENUM_TIMEFRAMES)StringToInteger(compArray[2]),StringToTime(compArray[3]),
                             StringToTime(compArray[4]),close_array);
// Get high prices
   int high_count=CopyHigh(compArray[1],
                           (ENUM_TIMEFRAMES)StringToInteger(compArray[2]),StringToTime(compArray[3]),
                           StringToTime(compArray[4]),high_array);
// Get low prices
   int low_count=CopyLow(compArray[1],
                         (ENUM_TIMEFRAMES)StringToInteger(compArray[2]),StringToTime(compArray[3]),
                         StringToTime(compArray[4]),low_array);
// Get open time
   int time_count=CopyTime(compArray[1],
                           (ENUM_TIMEFRAMES)StringToInteger(compArray[2]),StringToTime(compArray[3]),
                           StringToTime(compArray[4]),time_array);

   zmq_ret+="'_action': 'GET_DATA'";

   if(time_count>0 && (time_count==open_count && time_count==close_count
                       && time_count==high_count && time_count==low_count)) {
      zmq_ret+=", '_ohlc_data': {";

      for(int i=0; i<time_count; i++) {
         if(i>0)
            zmq_ret+=", ";

         zmq_ret+="'"+TimeToString(time_array[i])+"': ["+DoubleToString(open_array[i])
                  +", "+DoubleToString(high_array[i])+", "+DoubleToString(low_array[i])
                  +", "+DoubleToString(close_array[i])+"]";
      }
      zmq_ret+="}";
   } else {
      zmq_ret+=", "+"'_response': 'NOT_AVAILABLE'";
   }
}
//+------------------------------------------------------------------+
//|  Using this function for the first time for given symbol and time|
//|  period may halt the service for up to 45 seconds. During this   |
//|  time synchronization between local symbol's database and        |
//|  server's database (broker) should be completed.                 |
//+------------------------------------------------------------------+
void DWX_GetTickData(string &compArray[],string &zmq_ret) {
// GET_TICK_DATA == 14
// Format: 14|SYMBOL|START_DATETIME|END_DATETIME

   datetime d0 = StringToTime(compArray[2]);
   datetime d1 = StringToTime(compArray[3]);
   zmq_ret+="'_action': 'GET_TICK_DATA'";
   if(d0>0) {
      MqlTick tck_array[];
      int copied=-1;
      ResetLastError();
      // COPY_TICKS_ALL - fastest
      // COPY_TICKS_INFO – ticks with Bid and/or Ask changes
      // COPY_TICKS_TRADE – ticks with changes in Last and Volume
      if(d1>0) {
         copied=CopyTicksRange(compArray[1],tck_array,COPY_TICKS_ALL,(ulong)(d0*1000),(ulong)(d1*1000));
      } else {
         copied=CopyTicks(compArray[1],tck_array,COPY_TICKS_ALL,(ulong)(d0*1000));
      }
      // Error while using 'Copy' function
      if(copied==-1) {
         Add_Error_Description(GetLastError(),zmq_ret);
      } else if(copied==0) {
         zmq_ret+=", "+"'_response': 'NO_TICKS_AVAILABLE'";
      } else {
         zmq_ret+=", '_data': {";

         for(int i=0; i<copied; i++) {
            if(i>0)
               zmq_ret+=", ";

            zmq_ret+="'"+TimeToString((long)(tck_array[i].time_msc/1000.0),TIME_DATE|TIME_SECONDS)
                     +"."+IntegerToString((long)fmod(tck_array[i].time_msc,1000),3,'0')
                     +"': ["+DoubleToString(tck_array[i].bid)+", "+DoubleToString(tck_array[i].ask)+"]";
         }
         zmq_ret+="}";
      }
   } else {
      zmq_ret+=", "+"'_response': 'INCORRECT_DATE_FORMAT'";
   }
}
//+------------------------------------------------------------------+
//|  Inform Client                                                   |
//+------------------------------------------------------------------+
void InformPullClient(Socket &pSocket,string message,bool verbose=true) {
// Do not use StringFormat here, as it cannot handle string arguments
// longer than sizeof(short) - problems for sending longer data, etc.
   ZmqMsg pushReply(message);
   if(verbose)
      Print("PUSH "+message+" to PUSH Socket "+IntegerToString(PUSH_PORT));
   pSocket.send(pushReply,true); // NON-BLOCKING
}
//+------------------------------------------------------------------+
//|  OPEN NEW POSITION                                               |
//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
int DWX_PositionOpen(string _symbol,int _type,double _lots,double _price,double _SL,double _TP,string _comment,string _magic,string &zmq_ret,Socket &pSocket) {
   int ticket,error;
   zmq_ret+="'_action': 'EXECUTION'";
   zmq_ret+=", '_magic': "+_magic;

// Market order valid types: ORDER_TYPE_BUY(0) | ORDER_TYPE_SELL(1)
   if(_type>1) {
      zmq_ret+=", "+"'_response': 'ACTION_TYPE_ERROR', 'response_value': 'INVALID_POSITION_OPEN_TYPE'";
      return(-1);
   }

   if(_lots>MaximumLotSize) {
      zmq_ret+=", "+"'_response': 'LOT_SIZE_ERROR', 'response_value': 'MAX_LOT_SIZE_EXCEEDED'";
      return(-1);
   }

   string valid_symbol=(_symbol=="NULL")?Symbol():_symbol;

   double vpoint  = SymbolInfoDouble(valid_symbol, SYMBOL_POINT);
   int    vdigits = (int)SymbolInfoInteger(valid_symbol, SYMBOL_DIGITS);

   _lots = round(_lots*100.0)/100.0;

   double sl = 0.0;
   double tp = 0.0;


   double symbol_bid=SymbolInfoDouble(valid_symbol,SYMBOL_BID);
   double symbol_ask=SymbolInfoDouble(valid_symbol,SYMBOL_ASK);
   double overrideLotsOriginal=0.0;
   if(Hedge_to_net && PositionSelect(valid_symbol)) {
      double currentPositionSymbol = GetOpenPositionSymbol(valid_symbol);

      bool isBuyToClose = (ENUM_ORDER_TYPE)_type==ORDER_TYPE_BUY && currentPositionSymbol<0;
      bool isSellToClose = (ENUM_ORDER_TYPE)_type==ORDER_TYPE_SELL && currentPositionSymbol>0;
      if(isBuyToClose || isSellToClose) {
         if(MathAbs(_lots)<=MathAbs(currentPositionSymbol)) {
            //Close partial position
            bool closeAllPosition = MathAbs(_lots) == MathAbs(currentPositionSymbol);
            if(closeAllPosition) {
               Print("Hedge_to_net Complete DWX_PositionsSymbolClose_All "+valid_symbol+" position:" + DoubleToString(currentPositionSymbol,2)+" close:"+ DoubleToString(_lots,2));
               zmq_ret="{";
               //zmq_ret+=", '_magic': "+_magic;

               DWX_PositionsSymbolClose_All(valid_symbol,zmq_ret);
               //bool output= DWX_CloseAtMarket(_lots,zmq_ret);
               return true;
            }
            Print("Hedge_to_net partial DWX_PositionClosePartial "+valid_symbol+" position:" + DoubleToString(currentPositionSymbol,2)+" close:"+ DoubleToString(_lots,2));
            string originalZmq_ret=zmq_ret;
            if(DWX_PositionClosePartial(_lots,zmq_ret,0,valid_symbol)) {
               return true;
            } else {
               zmq_ret = originalZmq_ret;
               double positionOut = GetOpenPositionSymbol(valid_symbol);
               double changePosition = MathAbs(currentPositionSymbol-positionOut);
               double rest_lots = _lots-changePosition;
               if(rest_lots>0) {
                  printf("Hedge_to_net Failed DWX_PositionClosePartial "+valid_symbol+" currentPositionSymbol:" + DoubleToString(currentPositionSymbol,2)+" positionOut:" + DoubleToString(positionOut,2)+" changePosition:" + DoubleToString(changePosition,2)+" continue opening new trade of _lots "+ DoubleToString(rest_lots,2));

                  InformPullClient(pSocket,zmq_ret+"}");//notify new trade that close some positions and continue opening more silently
                  bool output=true;
                  overrideLotsOriginal=_lots;//this is what we are going to notify
                  _lots=rest_lots;//this is the rest we are going to close silently
                  zmq_ret=originalZmq_ret;

               } else {
                  printf("Hedge_to_net Failed DWX_PositionClosePartial "+valid_symbol+" currentPositionSymbol:" + DoubleToString(currentPositionSymbol,2)+" positionOut:" + DoubleToString(positionOut,2)+" but resulted _lots "+ DoubleToString(rest_lots,2)+" is < 0.01");
                  return false;
               }
            }
         } else {
            double lots_to_continue=_lots-MathAbs(currentPositionSymbol);
            Print("Hedge_to_net Complete DWX_PositionsSymbolClose_All "+valid_symbol+" currentPositionSymbol:" + DoubleToString(currentPositionSymbol,2)+" close:"+ DoubleToString(_lots,2)+" and continue with the rest _lots "+DoubleToString(lots_to_continue,2));

            string originalZmq_ret=zmq_ret;
            zmq_ret="{";
            DWX_PositionsSymbolClose_All(valid_symbol,zmq_ret);
            InformPullClient(pSocket,zmq_ret+"}");//notify close all the rest is not going to be notified!!

            bool output=true;
            overrideLotsOriginal=_lots;//this is what we are going to notify
            _lots = lots_to_continue;//this is the rest we are going to close silently
            zmq_ret=originalZmq_ret;

         }

      }
   }




// IMPORTANT NOTE: Single-Step stops placing for market orders: no more 2-step procedure for STP|ECN|DMA
// available int MT5 from build 821 ('Market' and 'Exchange' execution types)
   if((ENUM_ORDER_TYPE)_type==ORDER_TYPE_BUY) {
      if(_SL!=0.0)
         sl=NormalizeDouble(symbol_bid-_SL*vpoint,vdigits);
      if(_TP!=0.0)
         tp=NormalizeDouble(symbol_bid+_TP*vpoint,vdigits);
   } else {
      if(_SL!=0.0)
         sl=NormalizeDouble(symbol_ask+_SL*vpoint,vdigits);
      if(_TP!=0.0)
         tp=NormalizeDouble(symbol_ask-_TP*vpoint,vdigits);
   }

// Using helper object to perform transaction.
   if(!tradeHelper.PositionOpen(valid_symbol,(ENUM_ORDER_TYPE)_type,_lots,_price,sl,tp,_comment)) {
      error=(int)tradeHelper.ResultRetcode();
      Add_Error_Description(error,zmq_ret);
      return(-1*error);
   }

   if(overrideLotsOriginal!=0.0) {
      string lots_original="'_lots': "+DoubleToString(_lots,2);
      string lots_replace="'_lots': "+DoubleToString(overrideLotsOriginal,2);
      Print("Hedge_to_net "+valid_symbol+" replace lots_original "+lots_original+" with lots_replace "+lots_replace);
      int replaced=StringReplace(zmq_ret,lots_original,lots_replace);
   }

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
   uint ret_code=tradeHelper.ResultRetcode();
   if(ret_code==TRADE_RETCODE_DONE) {
      // To get already opened position's id we need to find corresponding deal.
      ulong deal_id=tradeHelper.ResultDeal();
      if(HistoryDealSelect(deal_id)) {
         ENUM_DEAL_ENTRY entry_type=(ENUM_DEAL_ENTRY)HistoryDealGetInteger(deal_id,DEAL_ENTRY);
         long position_ticket=HistoryDealGetInteger(deal_id,DEAL_POSITION_ID);
         // For security reason we perform checking deal entry type.
         if(PositionSelectByTicket(position_ticket) && entry_type==DEAL_ENTRY_IN) {
            zmq_ret+=", '_comment': "+_comment
                     +", '_ticket': "+IntegerToString(position_ticket)
                     +", '_open_time': '"+TimeToString((datetime)PositionGetInteger(POSITION_TIME),TIME_DATE|TIME_SECONDS)
                     +"', '_open_price': "+DoubleToString(PositionGetDouble(POSITION_PRICE_OPEN));

            ticket=(int)position_ticket;
         } else {
            // Immediately closed...?
            zmq_ret+=", "+"'_response': 'Position opened, but cannot be selected'";
            return(-1);
         }
      } else {
         zmq_ret+=", "+"'_response': 'Position opened, but corresponding deal cannot be selected'";
         return(-1);
      }
   } else {
      Add_Error_Description(ret_code,zmq_ret);
      return(-1*(int)ret_code);
   }

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
   return(ticket);
}
//+------------------------------------------------------------------+
//| PLACE NEW PENDING ORDER                                          |
//+------------------------------------------------------------------+
int DWX_OrderOpen(string _symbol,int _type,double _lots,double _price,double _SL,double _TP,string _comment,string _magic,string &zmq_ret) {
   int ticket,error;

   zmq_ret+="'_action': 'EXECUTION'";
   zmq_ret+=", '_magic': "+_magic;

// Pending order valid types: ORDER_TYPE_BUY_LIMIT(2) | ORDER_TYPE_SELL_LIMIT(3) | ORDER_TYPE_BUY_STOP(4) | ORDER_TYPE_SELL_STOP(5).
// Also: ORDER_TYPE_BUY_STOP_LIMIT and ORDER_TYPE_SELL_STOP_LIMIT, but we leave it out int this Service version.
   if(_type<2 || _type>5) {
      zmq_ret+=", "+"'_response': 'ACTION_TYPE_ERROR', 'response_value': 'INVALID_PENDING_ORDER_TYPE'";
      return(-1);
   }

   if(_lots>MaximumLotSize) {
      zmq_ret+=", "+"'_response': 'LOT_SIZE_ERROR', 'response_value': 'MAX_LOT_SIZE_EXCEEDED'";
      return(-1);
   }

   string valid_symbol=(_symbol=="NULL")?Symbol():_symbol;
   _lots = round(_lots*100.0)/100.0;

   double vpoint=SymbolInfoDouble(valid_symbol,SYMBOL_POINT);
   int    vdigits=(int)SymbolInfoInteger(valid_symbol,SYMBOL_DIGITS);

   double sl = 0.0;
   double tp = 0.0;




   if((ENUM_ORDER_TYPE)_type==ORDER_TYPE_BUY_LIMIT || (ENUM_ORDER_TYPE)_type==ORDER_TYPE_BUY_STOP) {
      if(_SL!=0.0)
         sl=NormalizeDouble(_price-_SL*vpoint,vdigits);
      if(_TP!=0.0)
         tp=NormalizeDouble(_price+_TP*vpoint,vdigits);
   } else {
      if(_SL!=0.0)
         sl=NormalizeDouble(_price+_SL*vpoint,vdigits);
      if(_TP!=0.0)
         tp=NormalizeDouble(_price-_TP*vpoint,vdigits);
   }

   if(!tradeHelper.OrderOpen(valid_symbol,(ENUM_ORDER_TYPE)_type,_lots,0,_price,sl,tp,0,0,_comment)) {
      error=(int)tradeHelper.ResultRetcode();
      Add_Error_Description(error,zmq_ret);
      return(-1*error);
   }

   uint ret_code=tradeHelper.ResultRetcode();
   if(ret_code==TRADE_RETCODE_DONE) {
      ulong order_ticket=tradeHelper.ResultOrder();
      if(OrderSelect(order_ticket)) {
         zmq_ret+=//", "+"'_magic': "+IntegerToString(OrderGetInteger(ORDER_MAGIC))
            //", "+"'_magic': "+_magic
            ", '_comment': "+_comment
            +", '_ticket': "+IntegerToString(order_ticket)
            +", '_setup_time': '"+TimeToString((datetime)OrderGetInteger(ORDER_TIME_SETUP),TIME_DATE|TIME_SECONDS)
            +"', '_open_price': "+DoubleToString(OrderGetDouble(ORDER_PRICE_OPEN));

         ticket=(int)order_ticket;
      } else {
         zmq_ret+=", "+"'_response': 'Pending order placed, but cannot be selected'";
         return(-1);
      }
   } else {
      Add_Error_Description(ret_code,zmq_ret);
      return(-1*(int)ret_code);
   }

   return(ticket);
}
//+------------------------------------------------------------------+
//|  UPDATE POSITION SL/TP (SET|RESET|UPDATE)                        |
//+------------------------------------------------------------------+
bool DWX_PositionModify(int ticket,double _SL,double _TP,string &zmq_ret) {
   if(PositionSelectByTicket(ticket)) {
      int dir_flag=-1;

      ENUM_POSITION_TYPE ord_type=(ENUM_POSITION_TYPE)PositionGetInteger(POSITION_TYPE);
      if(ord_type==POSITION_TYPE_BUY)
         dir_flag=1;

      double vpoint  = SymbolInfoDouble(PositionGetString(POSITION_SYMBOL), SYMBOL_POINT);
      int    vdigits = (int)SymbolInfoInteger(PositionGetString(POSITION_SYMBOL), SYMBOL_DIGITS);

      //If it is necessary we can remove stops.
      double sl = 0.0;
      double tp = 0.0;
      //To update|set stops
      if(_SL!=0.0)
         sl=NormalizeDouble(PositionGetDouble(POSITION_PRICE_OPEN)-_SL*dir_flag*vpoint,vdigits);
      if(_TP!=0.0)
         tp=NormalizeDouble(PositionGetDouble(POSITION_PRICE_OPEN)+_TP*dir_flag*vpoint,vdigits);

      if(!tradeHelper.PositionModify(ticket,sl,tp)) {
         int error=(int)tradeHelper.ResultRetcode();
         Add_Error_Description(error,zmq_ret);
         zmq_ret+=", '_sl_attempted': "+DoubleToString(sl)+", '_tp_attempted': "+DoubleToString(tp);
         return(false);
      }
      uint ret_code=tradeHelper.ResultRetcode();
      if(ret_code==TRADE_RETCODE_DONE) {
         zmq_ret+=", '_sl': "+DoubleToString(sl)+", '_tp': "+DoubleToString(tp);
         return(true);
      } else {
         Add_Error_Description(ret_code,zmq_ret);
      }
   } else {
      zmq_ret+=", '_response': 'NOT_FOUND'";
   }

   return(false);
}
//+------------------------------------------------------------------+
//|  UPDATE PENDING ORDER SL/TP (SET|RESET|UPDATE)                   |
//+------------------------------------------------------------------+
bool DWX_OrderModify(long ticket,double _SL,double _TP,double _price,double _quantity,string _magic,string &zmq_ret) {


//printf("DWX_OrderModify  ticket "+IntegerToString(ticket));

   zmq_ret+=", '_magic': "+_magic;
   if(OrderSelect(ticket)) {

      int dir_flag=-1;

      ENUM_ORDER_TYPE ord_type=(ENUM_ORDER_TYPE)OrderGetInteger(ORDER_TYPE);
      if(ord_type==ORDER_TYPE_BUY_LIMIT || ord_type==ORDER_TYPE_BUY_STOP)
         dir_flag=1;

      double vpoint  = SymbolInfoDouble(OrderGetString(ORDER_SYMBOL), SYMBOL_POINT);
      int    vdigits = (int)SymbolInfoInteger(OrderGetString(ORDER_SYMBOL), SYMBOL_DIGITS);

      // To remove stops if necessary.
      double sl = 0.0;
      double tp = 0.0;

      double price = NormalizeDouble(_price,vdigits);
      // To update|set stops
      if(_SL!=0.0)
         sl=NormalizeDouble(OrderGetDouble(ORDER_PRICE_OPEN)-_SL*dir_flag*vpoint,vdigits);
      if(_TP!=0.0)
         tp=NormalizeDouble(OrderGetDouble(ORDER_PRICE_OPEN)+_TP*dir_flag*vpoint,vdigits);

      //if(!tradeHelper.OrderModify(ticket,OrderGetDouble(ORDER_PRICE_OPEN),sl,tp,0,0)) {
      if(!tradeHelper.OrderModify(ticket,price,sl,tp,0,0)) {
         int error=(int)tradeHelper.ResultRetcode();
         Add_Error_Description(error,zmq_ret);
         zmq_ret+=", '_sl_attempted': "+DoubleToString(sl)+", '_tp_attempted': "+DoubleToString(tp)+", '_price_attempted': "+DoubleToString(price);
         zmq_ret+=", '_ticket': "+IntegerToString(ticket);
         return(false);
      }
      uint ret_code=tradeHelper.ResultRetcode();
      if(ret_code==TRADE_RETCODE_DONE) {
         zmq_ret+=", '_sl': "+DoubleToString(sl)+", '_tp': "+DoubleToString(tp);
         zmq_ret+=", '_ticket': "+IntegerToString(ticket);
         zmq_ret+=", '_price': "+DoubleToString(price);
         return(true);
      } else {
         Add_Error_Description(ret_code,zmq_ret);
      }
   } else {
      zmq_ret+=", '_ticket': "+IntegerToString(ticket);
      zmq_ret+=", '_response': 'NOT_FOUND'";
   }

   return(false);
}
//+------------------------------------------------------------------+
//|  CLOSE AT MARKET                                                 |
//+------------------------------------------------------------------+
bool DWX_CloseAtMarket(double size,string &zmq_ret,string symbol) {
   string originalZmq_ret=zmq_ret;

   int retries=15;
   while(true) {
      retries--;
      if(retries < 0) {
         return(false);
      } else {
         zmq_ret=originalZmq_ret;
      }

      if(DWX_IsTradeAllowed(30,zmq_ret)==1) {
         if(DWX_PositionClosePartial(size,zmq_ret,0,symbol)) {
            //zmq_ret = zmq_ret+"}";
            // trade successfuly closed
            return(true);
         }

         Sleep(500);
      }
   }
   return(false);
}
//+------------------------------------------------------------------+
//|  POSITION CLOSE PARTIAL                                          |
//+------------------------------------------------------------------+
bool DWX_PositionClosePartial(double size,string &zmq_ret,int ticket=0,string symbol="") {
   int error;
   bool close_ret=false;
   bool closePartial =false;
   size =  round(size*100.0)/100.0;


   if(ticket!=0) {
      zmq_ret += "'_action': 'CLOSE', '_ticket': " + IntegerToString(ticket);
      zmq_ret += ", '_response': 'CLOSE_PARTIAL'";
      PositionSelectByTicket(ticket);
      long _ticket=PositionGetInteger(POSITION_TICKET);
      closePartial = tradeHelper.PositionClosePartial(_ticket,size);
   } else if (symbol!="") {
      PositionSelect(symbol);
      double initialPosition = GetOpenPositionSymbol(symbol);
      double signTargetSize = size;
      if (initialPosition<0) {
         signTargetSize=-1*signTargetSize;
      }
      double expectedPosition = initialPosition-signTargetSize;
      expectedPosition = round(expectedPosition*100.0)/100.0;
      double minvol=SymbolInfoDouble(symbol,SYMBOL_VOLUME_MIN);
      double distanceToExpected =expectedPosition;
      do {
         double targetSize =  MathAbs(expectedPosition-GetOpenPositionSymbol(symbol));
         closePartial = tradeHelper.PositionClosePartial(symbol,targetSize);
         if(!closePartial) {
            error=(int)tradeHelper.ResultRetcode();
            if(error==10009) {
               continue;
            } else {
               printf("retcode  on PositionClosePartial error "+IntegerToString(error)+":"+GetErrorDescription(error));
            }
            break;
         }
         double currentPosition = GetOpenPositionSymbol(symbol);
         distanceToExpected = MathAbs(currentPosition-expectedPosition);
         distanceToExpected = round(distanceToExpected*100.0)/100.0;
      } while(distanceToExpected>=minvol);

   } else {
      printf("ERROR: on PositionClosePartial without ticket or symbol ");
      return false;
   }

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
   uint ret_code=tradeHelper.ResultRetcode();
   if(!closePartial) {
      error=(int)tradeHelper.ResultRetcode();
      string errorDescr = GetErrorDescription(error);
      if(error==10009) {
         //its okey but not found the price/size
         printf("retcode 10009 on PositionClosePartial return close_ret "+IntegerToString(close_ret));
         ret_code =TRADE_RETCODE_DONE;
         //zmq_ret+=", '_response': '("+IntegerToString(error)+") "+errorDescr+" ','_response_value': 'SUCCESS'";
         //return true;
      } else {
         printf("retcode on PositionClosePartial error "+IntegerToString(error));
         zmq_ret+=", '_response': '("+IntegerToString(error)+") "+errorDescr+" ', '_response_value': 'ERROR'";
         return close_ret;
      }
   }
   if(ret_code==TRADE_RETCODE_DONE) {
      double sizeTraded = tradeHelper.ResultVolume();
      double restVolume = size-sizeTraded;
      if (restVolume>0) {
         //WARNING
         printf("WARNING: on PositionClosePartial rest som volume to close: "+DoubleToString(restVolume));
      }

      ulong deal_id=tradeHelper.ResultDeal();
      if(HistoryDealSelect(deal_id)) {
         ENUM_DEAL_ENTRY entry_type=(ENUM_DEAL_ENTRY)HistoryDealGetInteger(deal_id,DEAL_ENTRY);
         if(entry_type==DEAL_ENTRY_OUT) {
            zmq_ret+=", '_close_price': "+DoubleToString(HistoryDealGetDouble(deal_id,DEAL_PRICE))
                     +", '_close_lots': "+DoubleToString(HistoryDealGetDouble(deal_id,DEAL_VOLUME));

            close_ret=true;
         } else {
            zmq_ret+=", "+"'_response': 'Position partially closed 1, but corresponding deal cannot be selected "+DoubleToString(deal_id)+"'";
            close_ret=false;
         }
      } else {
         zmq_ret+=", "+"'_response': 'Position partially closed 2, but corresponding deal cannot be selected "+DoubleToString(deal_id)+"'";
         close_ret=false;
      }
   } else {
      Add_Error_Description(ret_code,zmq_ret);
   }
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
   return(close_ret);
}
//+------------------------------------------------------------------+
//|  CLOSE POSITION (by Magic Number)                                |
//+------------------------------------------------------------------+
void DWX_PositionClose_Magic(int _magic,string &zmq_ret) {
   bool found=false;

   zmq_ret += "'_action': 'CLOSE_ALL_MAGIC'";
   zmq_ret += ", '_magic': " + IntegerToString(_magic);

   zmq_ret+=", '_responses': {";

   for(int i=PositionsTotal()-1; i>=0; i--) {
      if(PositionGetTicket(i)>0 && (int)PositionGetInteger(POSITION_MAGIC)==_magic) {
         found=true;

         zmq_ret+=IntegerToString(PositionGetInteger(POSITION_TICKET))+": {'_symbol':'"+PositionGetString(POSITION_SYMBOL)+"'";

         DWX_CloseAtMarket(-1,zmq_ret,PositionGetString(POSITION_SYMBOL));
         zmq_ret+=", '_response': 'CLOSE_MARKET'";

         if(i!=0)
            zmq_ret+="}, ";
         else
            zmq_ret+="}";
      }
   }

   zmq_ret+="}";
   if(found==false) {
      zmq_ret+=", '_response': 'NOT_FOUND'";
   } else {
      zmq_ret+=", '_response_value': 'SUCCESS'";
   }
}
//+------------------------------------------------------------------+
//|  CLOSE POSITION (by Ticket)                                      |
//+------------------------------------------------------------------+
void DWX_PositionClose_Ticket(long _ticket,string &zmq_ret) {
   zmq_ret+="'_action': 'CLOSE', '_ticket': "+IntegerToString(_ticket);

   if(PositionSelectByTicket(_ticket)) {
      bool output=DWX_CloseAtMarket(-1,zmq_ret,PositionGetString(POSITION_SYMBOL));
      if(output) {
         zmq_ret+=", '_response_value': 'SUCCESS'";
      } else {
         zmq_ret+=", '_response_value': 'ERROR'";
      }
      zmq_ret+=", '_response': 'CLOSE_MARKET'";
   } else
      zmq_ret+=", '_response': 'NOT_FOUND'";
}
//+------------------------------------------------------------------+
//|  DELETE PENDING ORDER (by Ticket)                                |
//+------------------------------------------------------------------+
void DWX_PendingOrderDelete_Ticket(long _ticket,string _magic,string &zmq_ret) {
//printf("DWX_PendingOrderDelete_Ticket  ticket "+IntegerToString(_ticket));

   zmq_ret+="'_action': 'DELETE', '_ticket': "+IntegerToString(_ticket);
   zmq_ret+=", '_magic':"+ _magic;
   if(OrderSelect(_ticket)) {
      if(tradeHelper.OrderDelete(_ticket)) {
         if(tradeHelper.ResultRetcode()==TRADE_RETCODE_DONE) {
            zmq_ret+=", '_response': 'CLOSE_PENDING'";
            zmq_ret+=", '_response_value': 'SUCCESS'";
         } else {
            Add_Error_Description(tradeHelper.ResultRetcode(),zmq_ret);
         }
      } else {
         Add_Error_Description(tradeHelper.ResultRetcode(),zmq_ret);
      }
   } else {
      zmq_ret+=", '_response': 'NOT_FOUND'";
   }
}
//+------------------------------------------------------------------+
//|  DELETE ALL PENDING ORDERS                                       |
//+------------------------------------------------------------------+
void DWX_PendingOrderDelete_All(string &zmq_ret) {
   bool found=false;

   zmq_ret+="'_action': 'DELETE_ALL'";
   zmq_ret+=", '_responses': {";
   int total_pending=OrdersTotal();
   for(int i=total_pending-1; i>=0; i--) {
      if(OrderGetTicket(i)>0) {
         found=true;
         zmq_ret+=IntegerToString(OrderGetInteger(ORDER_TICKET))+": {'_symbol':'"+OrderGetString(ORDER_SYMBOL)
                  +"', '_magic': "+IntegerToString(OrderGetInteger(ORDER_MAGIC));

         DWX_PendingOrderDelete_Ticket((int)OrderGetInteger(ORDER_TICKET),"",zmq_ret);
         zmq_ret+=", '_response': 'CLOSE_PENDING'";

         if(i!=0)
            zmq_ret+="}, ";
         else
            zmq_ret+="}";
      }
   }
   zmq_ret+="}";
   if(found==false) {
      zmq_ret+=", '_response': 'NOT_FOUND'";
   } else {
      zmq_ret+=", '_response_value': 'SUCCESS'";
   }
}
//+------------------------------------------------------------------+
//|  CLOSE ALL POSITIONS                                             |
//+------------------------------------------------------------------+
void DWX_PositionsClose_All(string &zmq_ret) {
   bool found=false;
   zmq_ret+="'_action': 'CLOSE_ALL'";
   zmq_ret+=", '_responses': {";

   for(int i=PositionsTotal()-1; i>=0; i--) {
      if(PositionGetTicket(i)>0) {
         found=true;

         zmq_ret+=IntegerToString(PositionGetInteger(POSITION_TICKET))+": {'_symbol':'"+PositionGetString(POSITION_SYMBOL)
                  +"', '_magic': "+IntegerToString(PositionGetInteger(POSITION_MAGIC));

         DWX_CloseAtMarket(-1,zmq_ret,PositionGetString(POSITION_SYMBOL));
         zmq_ret+=", '_response': 'CLOSE_MARKET'";

         if(i!=0)
            zmq_ret+="}, ";
         else
            zmq_ret+="}";
      }
   }
   zmq_ret+="}";

   if(found==false) {
      zmq_ret+=", '_response': 'NOT_FOUND'";
   } else {
      zmq_ret+=", '_response_value': 'SUCCESS'";
   }
}
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void DWX_PositionsSymbolClose_All(string symbol,string &zmq_ret) {
   {
      bool found=false;
      zmq_ret+="'_action': 'CLOSE_ALL'";
      zmq_ret+=", '_comment':'"+symbol+"'";
      zmq_ret+=", '_responses': {";

      double minvol=SymbolInfoDouble(symbol,SYMBOL_VOLUME_MIN);
      int retries=0;
      while(MathAbs(GetOpenPositionSymbol(symbol))>=minvol) {

         if(retries>15) {
            printf("DWX_PositionsSymbolClose_All error position cant be zero at "+symbol+"after 15 retries");
            found=false;
            break;
         }
         if(retries>0) {
            Sleep(1000);
         }

         for(int i=PositionsTotal()-1; i>=0; i--) {
            if(PositionGetTicket(i)>0) {
               string symbolIter = PositionGetString(POSITION_SYMBOL);
               if(symbol==symbolIter) {
                  found=true;

                  zmq_ret+=IntegerToString(PositionGetInteger(POSITION_TICKET))+": {'_symbol':'"+PositionGetString(POSITION_SYMBOL)
                           +"', '_magic': "+IntegerToString(PositionGetInteger(POSITION_MAGIC));
                  double size = PositionGetDouble(POSITION_VOLUME);
                  DWX_CloseAtMarket(size,zmq_ret,symbol);
                  zmq_ret+=", '_response': 'CLOSE_MARKET'";


                  zmq_ret+="}, ";
               }
            }
         }
         retries++;
      }


      zmq_ret=StringSubstr(zmq_ret,0,StringLen(zmq_ret)-2);//remove last comma
      zmq_ret+="}";

      if(found==false) {
         printf("DWX_PositionsSymbolClose_All not positions detected for "+symbol);
         zmq_ret+=", '_response': 'NOT_FOUND'";
      } else {
         zmq_ret+=", '_response_value': 'SUCCESS'";
      }
   }
}
//+------------------------------------------------------------------+
//|  GET LIST OF WORKING POSITIONS                                   |
//+------------------------------------------------------------------+
void DWX_GetOpenPositions(string &zmq_ret) {
   bool found=false;

   zmq_ret +="'_action': 'OPEN_POSITIONS'";
   zmq_ret +=", '_positions': {";

   for(int i=PositionsTotal()-1; i>=0; i--) {
      found=true;

      // Check existence and select.
      if(PositionGetTicket(i)>0) {
         zmq_ret+=IntegerToString(PositionGetInteger(POSITION_TICKET))+": {";

         zmq_ret+="'_magic': "+IntegerToString(PositionGetInteger(POSITION_MAGIC))+", '_symbol': '"+PositionGetString(POSITION_SYMBOL)
                  +"', '_lots': "+DoubleToString(PositionGetDouble(POSITION_VOLUME))+", '_type': "+IntegerToString(PositionGetInteger(POSITION_TYPE))
                  +", '_open_price': "+DoubleToString(PositionGetDouble(POSITION_PRICE_OPEN))+", '_open_time': '"+TimeToString(PositionGetInteger(POSITION_TIME),TIME_DATE|TIME_SECONDS)
                  +"', '_SL': "+DoubleToString(PositionGetDouble(POSITION_SL))+", '_TP': "+DoubleToString(PositionGetDouble(POSITION_TP))
                  +", '_pnl': "+DoubleToString(PositionGetDouble(POSITION_PROFIT))+", '_comment': '"+PositionGetString(POSITION_COMMENT)+"'";

         if(i!=0)
            zmq_ret+="}, ";
         else
            zmq_ret+="}";
      }
   }
   zmq_ret+="}";
}

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
double GetOpenPositionSymbol(string symbol) {
   double position=0.0;
   for(int i=PositionsTotal()-1; i>=0; i--) {
      if(PositionGetTicket(i)>0) {
         if(PositionGetString(POSITION_SYMBOL)==symbol) {
            double currPosition = PositionGetDouble(POSITION_VOLUME);
            long positionType = PositionGetInteger(POSITION_TYPE);
            if(positionType==POSITION_TYPE_SELL) {
               currPosition = -1*currPosition;
            }
            position+=currPosition;



         }

      }

   }
   position =  round(position*100.0)/100.0;
   return position;

}

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
int GetTradesSymbol(string symbol) {
   int counter=0;
   for(int i=PositionsTotal()-1; i>=0; i--) {
      if(PositionGetTicket(i)>0) {
         if(PositionGetString(POSITION_SYMBOL)==symbol) {
            counter++;
         }
      }
   }
   return counter;

}

//+------------------------------------------------------------------+
//|  GET LIST OF PENDING ORDERS                                      |
//+------------------------------------------------------------------+
void DWX_GetPendingOrders(string &zmq_ret) {
   bool found=false;
   zmq_ret += "'_action': 'PENDING_ORDERS'";
   zmq_ret += ", '_orders': {";

   for(int i=OrdersTotal()-1; i>=0; i--) {
      found=true;

      if(OrderGetTicket(i)>0) {
         zmq_ret+=IntegerToString(OrderGetInteger(ORDER_TICKET))+": {";

         zmq_ret+="'_magic': "+IntegerToString(OrderGetInteger(ORDER_MAGIC))+", '_symbol': '"+OrderGetString(ORDER_SYMBOL)
                  +"', '_lots': "+DoubleToString(OrderGetDouble(ORDER_VOLUME_CURRENT))+", '_type': "+IntegerToString(OrderGetInteger(ORDER_TYPE))
                  +", '_open_price': "+DoubleToString(OrderGetDouble(ORDER_PRICE_OPEN))+", '_SL': "+DoubleToString(OrderGetDouble(ORDER_SL))
                  +", '_TP': "+DoubleToString(OrderGetDouble(ORDER_TP))+", '_comment': '"+OrderGetString(ORDER_COMMENT)+"'";

         if(i!=0)
            zmq_ret+="}, ";
         else
            zmq_ret+="}";
      }
   }
   zmq_ret+="}";
}
//+------------------------------------------------------------------+
//|  CHECK IF TRADE IS ALLOWED                                       |
//+------------------------------------------------------------------+
int DWX_IsTradeAllowed(int MaxWaiting_sec,string &zmq_ret) {
   if(!MQLInfoInteger(MQL_TRADE_ALLOWED)) {
      int StartWaitingTime=(int)GetTickCount();
      zmq_ret+=", "+"'_response': 'TRADE_CONTEXT_BUSY'";

      while(true) {
         if(IsStopped()) {
            zmq_ret+=", "+"'_response_value': 'EA_STOPPED_BY_USER'";
            return(-1);
         }

         int diff=(int)(GetTickCount()-StartWaitingTime);
         if(diff>MaxWaiting_sec*1000) {
            zmq_ret+=", '_response': 'WAIT_LIMIT_EXCEEDED', '_response_value': "+IntegerToString(MaxWaiting_sec);
            return(-2);
         }
         // if the trade context has become free,
         if(MQLInfoInteger(MQL_TRADE_ALLOWED)) {
            zmq_ret+=", '_response': 'TRADE_CONTEXT_NOW_FREE'";
            return(1);
         }
      }
   } else {
      return(1);
   }
}
//+------------------------------------------------------------------+
//|  Function adding error description to zmq string message         |
//+------------------------------------------------------------------+
void Add_Error_Description(uint error_code,string &out_string) {
// Use 'GetErrorDescription' function already imported from https://github.com/dingmaotu/mql-zmq
   out_string+=", "+"'_response': '"+IntegerToString(error_code)+"', 'response_value': '"+GetErrorDescription(error_code)+"'";
}
//+------------------------------------------------------------------+

//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void ClosedHedgedPositions(void) {
   int     total=PositionsTotal();
   for(int i=0; i<total; i++) {
      if(PositionGetTicket(i)<=0) {
         continue;
      }
      string symbol = PositionGetString(POSITION_SYMBOL);
      double actPosition = GetOpenPositionSymbol(symbol);
      int numberOrders=GetTradesSymbol(symbol);
      if(actPosition==0.0) {
         printf("ClosedHedgedPositions detected for "+symbol +" with no position and "+IntegerToString(numberOrders)+" orders");
         string zmq_ret="{";
         DWX_PositionsSymbolClose_All(symbol,zmq_ret);
      }
   }

   CloseHedgedPositionsIndividual();
}


//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void CloseHedgedPositionsIndividual(void) {
//search matching orders
   int numberOrders=PositionsTotal();
   for(int j=0; j<numberOrders; j++) {
      if(PositionGetTicket(j)<=0) {
         continue;
      }

      double size1 = PositionGetDouble(POSITION_VOLUME);
      long ticket1 = PositionGetInteger(POSITION_TICKET);
      string symbol1 = PositionGetString(POSITION_SYMBOL);
      ENUM_POSITION_TYPE type1=(ENUM_POSITION_TYPE)PositionGetInteger(POSITION_TYPE);
      for(int k=0; k<numberOrders; k++) {
         if(j==k) {
            continue;
         }
         if(PositionGetTicket(k)<=0) {
            continue;
         }

         double size2 = PositionGetDouble(POSITION_VOLUME);
         long ticket2 = PositionGetInteger(POSITION_TICKET);
         string symbol2 = PositionGetString(POSITION_SYMBOL);

         ENUM_POSITION_TYPE type2=(ENUM_POSITION_TYPE)PositionGetInteger(POSITION_TYPE);
         if(size1==size2 && symbol2==symbol1 && type1!=type2) {
            printf("ClosedHedgedPositions detected for "+symbol1 +" with two opossite positions  "+IntegerToString(ticket1)+" and "+IntegerToString(ticket2));
            bool ret1=tradeHelper.PositionClose(ticket1);
            bool ret2=tradeHelper.PositionClose(ticket2);

         }

      }
   }

}
//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
