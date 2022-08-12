//+------------------------------------------------------------------+
//|                                           TestSymbolInfoInit.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//---
//+------------------------------------------------------------------+
//| Arrays to initialize graphics objects SymbolInfoSample.          |
//+------------------------------------------------------------------+
//--- for ExtLabel[]
int    init_l_x[]=
  {
   20,220,420,220,420,20,20,220,20,20,
   20,20,220,20,20,20,20,220,420,20,
   220,420,20,220,420,20,20,220,20,20,
   220,420,20,220,20,220,420,20,20,20
  };
int    init_l_y[]=
  {
   1,1,1,2,2,3,4,4,5,6,
   7,8,8,9,10,11,12,12,12,13,
   13,13,14,14,14,15,16,16,17,18,
   18,18,19,19,20,20,20,21,22,23
  };
string init_l_str[]=
  {
   "Volume","VolumeHigh","VolumeLow","VolumeBid","VolumeAsk","Time","Digits","Spread","TicksBookDepth","TradeCalcMode",
   "TradeMode","StopsLevel","FreezeLevel","TradeExecution","SwapMode","SwapRollover3days","Bid","BidHigh","BidLow","Ask",
   "AskHigh","AskLow","Last","LastHigh","LastLow","Point","TickValue","TickSize","ContractSize","VolumeMin",
   "VolumeMax","VolumeStep","SwapLong","SwapShort","CurrencyBase","CurrencyProfit","CurrencyMargin","Bank","Description","Path"
  };
//--- for ExtLabelInfo[]
int    init_li_x[]=
  {
   120,320,520,320,520,120,120,320,120,120,
   120,120,320,120,120,120,120,320,520,120,
   320,520,120,320,520,120,120,320,120,120,
   320,520,120,320,120,320,520,120,120,120
  };
//+------------------------------------------------------------------+
