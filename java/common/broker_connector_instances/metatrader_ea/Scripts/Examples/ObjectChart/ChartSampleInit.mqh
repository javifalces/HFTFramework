//+------------------------------------------------------------------+
//|                                              ChartSampleInit.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Arrays to initialize graphics objects ObjChartSample.            |
//+------------------------------------------------------------------+
#define NUM_PANELS    8
#define NUM_LABELS   40
#define NUM_EDITS    38
#define NUM_BUTTONS  28
//--- for Pabel[]
string p_str[NUM_PANELS]=
  {
   "Modes","Anothers","Scales","Shows","Timeframes","Symbols","Colors",
   "Read only parameters"
  };
//--- for Label[]
int    l_x[NUM_LABELS]=
  {
   20,20,20,20,20,20,20,
   20,20,20,20,20,20,
   80,140,200,260,20,20,20,20,
   20,80,110,160,260,290,
   20,20,20,20,20,20,20,20,20,20,20,20,20
  };
int    l_y[NUM_LABELS]=
  {
   14,49,84,151,218,269,346,
   1,21,41,61,81,101,
   121,121,121,121,141,161,181,201,
   241,221,221,221,221,221,
   21,41,61,81,101,121,141,161,181,201,221,241,261
  };
int    l_pan[NUM_LABELS]=
  {
   14,49,84,151,218,269,346,
   7,7,7,7,7,7,
   7,7,7,7,7,7,7,7,
   7,7,7,7,7,7,
   6,6,6,6,6,6,6,6,6,6,6,6,6
  };
string l_str[]=
  {
   "Modes","Anothers","Scales","Shows","Timeframes","Symbols","Read only parameters",
   "Handle","Visible bars","First bar","Width (bars)","Width (pix)","Win total",
   "Win 0","Win 1","Win 2","Win 3","Visible","Height (pix)","Price min","Price max",
   "OnDropped","Win","Price","Time","X","Y",
   "Background","Foreground","Grid","BarUp","BarDown","CandleBull","CandleBear",
   "ChartLine","Volumes","LineBid","LineAsk","LineLast","StopLevels"
  };
//--- for Edit[]
int    e_x[NUM_EDITS]=
  {
   220,220,320,320,95,245,120,
   80,80,80,140,200,260,320,
   80,80,80,80,
   80,140,200,260,320,
   80,140,200,260,320,
   80,140,200,260,320,
   80,100,160,260,290
  };
int    e_y[NUM_EDITS]=
  {
   0,0,0,0,16,16,32,
   20,100,140,140,140,140,140,
   0,40,60,80,
   160,160,160,160,160,
   180,180,180,180,180,
   200,200,200,200,200,
   240,240,240,240,240
  };
int    e_sizeX[NUM_EDITS]=
  {
   80,80,0,0,75,75,100,
   60,60,60,60,60,60,0,
   60,60,60,60,
   60,60,60,60,0,
   60,60,60,60,0,
   60,60,60,60,0,
   20,60,100,30,30
  };
int    e_pan[NUM_EDITS]=
  {
   1,2,2,2,2,2,2,
   7,7,7,7,7,7,7,
   7,7,7,7,
   7,7,7,7,7,
   7,7,7,7,7,
   7,7,7,7,7,
   7,7,7,7,7
  };
//--- for Button[]
int    b_x[NUM_BUTTONS]=
  {
   20,120,220,
   95,170,20,300,300,
   20,120,300,300,20,170,20,
   20,95,170,245,20,120,220,
   20,120,220,
   20,120,220
  };
int    b_y[NUM_BUTTONS]=
  {
   0,0,0,
   0,0,0,0,8,
   0,0,0,8,16,16,32,
   0,0,0,0,16,16,16,
   32,32,32,
   0,0,0
  };
int    b_sizeX[NUM_BUTTONS]=
  {
   100,100,100,
   75,50,75,20,20,
   100,100,20,20,75,75,100,
   75,75,75,75,100,100,100,
   100,100,100,
   100,100,100
  };
int    b_sizeY[NUM_BUTTONS]=
  {
   16,16,16,
   16,16,16,8,8,
   16,16,8,8,16,16,16,
   16,16,16,16,16,16,16,
   16,16,16,
   16,16,16
  };
string b_str[NUM_BUTTONS]=
  {
   "Bars","Candles","Line",
   "AutoScroll","Shift","Foreground"," "," ",
   "Scale fix","Scale fix 1/1"," "," ",
   "Fixed Max","Fixed Min",
   "Scale PixPerBar",
   "Show OHLC","Show Bid","Show Ask","Show Last",
   "Show Separator","Show Grid","Show ObjDescr",
   "Not Volumes","Tick Volumes","Real Volumes",
   "Yellow on Black","Green on Black","Black on White"
  };
int    b_pan[NUM_BUTTONS]=
  {
   0,0,0,
   1,1,1,1,1,
   2,2,2,2,2,2,2,
   3,3,3,3,3,3,3,
   3,3,3,
   6,6,6
  };
//--- for ButtonTF[]
string tf_str[]=
  {
   "M1","M2","M3","M4","M5","M6","M10","M12","M15","M20","M30",
   "H1","H2","H3","H4","H6","H12","D1","W1","MN"
  };
int    tf_int[]=
  {
   1,2,3,4,5,6,10,12,15,20,30,
   0x4001,0x4002,0x4003,0x4004,0x4006,0x400c,0x4018,0x8001,0xc001
  };
//+------------------------------------------------------------------+
