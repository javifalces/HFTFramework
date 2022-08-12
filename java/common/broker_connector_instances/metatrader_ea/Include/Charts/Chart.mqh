//+------------------------------------------------------------------+
//|                                                       Chart.mqh  |
//|                   Copyright 2009-2016, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
//+------------------------------------------------------------------+
//| Class CChart.                                                    |
//| Purpose: Class of the "Chart" object.                            |
//|          Derives from class CObject.                             |
//+------------------------------------------------------------------+
class CChart : public CObject
  {
protected:
   long              m_chart_id;           // chart identifier
public:
                     CChart(void);
                    ~CChart(void);
   //--- methods of access to protected data
   long              ChartId(void) const { return(m_chart_id); }
   //--- method of identifying the object
   virtual int       Type(void) const { return(0x1111); }
   //--- methods of access to properties of the chart
   //--- common properties
   ENUM_CHART_MODE   Mode(void) const;
   bool              Mode(const ENUM_CHART_MODE mode) const;
   bool              Foreground(void) const;
   bool              Foreground(const bool foreground) const;
   bool              Shift(void) const;
   bool              Shift(const bool shift) const;
   double            ShiftSize(void) const;
   bool              ShiftSize(double shift) const;
   bool              AutoScroll(void) const;
   bool              AutoScroll(const bool auto_scroll) const;
   int               Scale(void) const;
   bool              Scale(int scale) const;
   bool              ScaleFix(void) const;
   bool              ScaleFix(const bool scale_fix) const;
   bool              ScaleFix_11(void) const;
   bool              ScaleFix_11(const bool scale_fix_11) const;
   double            FixedMax(void) const;
   bool              FixedMax(const double fixed_max) const;
   double            FixedMin(void) const;
   bool              FixedMin(const double fixed_min) const;
   bool              ScalePPB(void) const;
   bool              ScalePPB(const bool scale_ppb) const;
   double            PointsPerBar(void) const;
   bool              PointsPerBar(const double points_per_bar) const;
   //--- show properties
   bool              ShowOHLC(void) const;
   bool              ShowOHLC(const bool show) const;
   bool              ShowLineBid(void) const;
   bool              ShowLineBid(const bool show) const;
   bool              ShowLineAsk(void) const;
   bool              ShowLineAsk(const bool show) const;
   bool              ShowLastLine(void) const;
   bool              ShowLastLine(const bool show) const;
   bool              ShowPeriodSep(void) const;
   bool              ShowPeriodSep(const bool show) const;
   bool              ShowGrid(void) const;
   bool              ShowGrid(const bool show) const;
   ENUM_CHART_VOLUME_MODE ShowVolumes(void) const;
   bool              ShowVolumes(const ENUM_CHART_VOLUME_MODE show) const;
   bool              ShowObjectDescr(void) const;
   bool              ShowObjectDescr(const bool show) const;
   bool              ShowDateScale(const bool show) const;
   bool              ShowPriceScale(const bool show) const;
   //--- color properties
   color             ColorBackground(void) const;
   bool              ColorBackground(const color new_color) const;
   color             ColorForeground(void) const;
   bool              ColorForeground(const color new_color) const;
   color             ColorGrid(void) const;
   bool              ColorGrid(const color new_color) const;
   color             ColorBarUp(void) const;
   bool              ColorBarUp(const color new_color) const;
   color             ColorBarDown(void) const;
   bool              ColorBarDown(const color new_color) const;
   color             ColorCandleBull(void) const;
   bool              ColorCandleBull(const color new_color) const;
   color             ColorCandleBear(void) const;
   bool              ColorCandleBear(const color new_color) const;
   color             ColorChartLine(void) const;
   bool              ColorChartLine(const color new_color) const;
   color             ColorVolumes(void) const;
   bool              ColorVolumes(const color new_color) const;
   color             ColorLineBid(void) const;
   bool              ColorLineBid(const color new_color) const;
   color             ColorLineAsk(void) const;
   bool              ColorLineAsk(const color new_color) const;
   color             ColorLineLast(void) const;
   bool              ColorLineLast(const color new_color) const;
   color             ColorStopLevels(void) const;
   bool              ColorStopLevels(const color new_color) const;
   //--- other properties
   bool              BringToTop(void) const;
   bool              EventObjectCreate(const bool flag=true) const;
   bool              EventObjectDelete(const bool flag=true) const;
   bool              EventMouseMove(const bool flag=true) const;
   bool              MouseScroll(const bool flag=true) const;
   //--- methods of access to READ ONLY properties of the chart
   int               VisibleBars(void) const;
   int               WindowsTotal(void) const;
   bool              WindowIsVisible(const int num) const;
   int               WindowHandle(void) const;
   int               FirstVisibleBar(void) const;
   int               WidthInBars(void) const;
   int               WidthInPixels(void) const;
   int               HeightInPixels(const int num) const;
   int               SubwindowY(const int num) const;
   double            PriceMin(const int num) const;
   double            PriceMax(const int num) const;
   bool              IsObject(void) const;
   //--- methods of binding chart
   void              Attach(void) { m_chart_id=ChartID(); }
   void              Attach(const long chart) { m_chart_id=chart; }
   void              FirstChart(void) { m_chart_id=ChartFirst(); }
   void              NextChart(void) { m_chart_id=ChartNext(m_chart_id); }
   long              Open(const string symbol_name,const ENUM_TIMEFRAMES timeframe);
   void              Detach(void) { m_chart_id=-1;                    }
   void              Close(void);
   //--- navigation method
   bool              Navigate(const ENUM_CHART_POSITION position,const int shift=0) const;
   //--- methods of access to the API functions of MQL5
   string            Symbol(void) const { return(ChartSymbol(m_chart_id)); }
   ENUM_TIMEFRAMES   Period(void) const { return(ChartPeriod(m_chart_id)); }
   void              Redraw(void) const { ChartRedraw(m_chart_id); }
   long              GetInteger(const ENUM_CHART_PROPERTY_INTEGER prop_id,const int sub_window=0) const;
   bool              GetInteger(const ENUM_CHART_PROPERTY_INTEGER prop_id,const int sub_window,long &value) const;
   bool              SetInteger(const ENUM_CHART_PROPERTY_INTEGER prop_id,const long value) const;
   double            GetDouble(const ENUM_CHART_PROPERTY_DOUBLE prop_id,const int sub_window=0) const;
   bool              GetDouble(const ENUM_CHART_PROPERTY_DOUBLE prop_id,const int sub_window,double &value) const;
   bool              SetDouble(const ENUM_CHART_PROPERTY_DOUBLE prop_id,const double value) const;
   string            GetString(const ENUM_CHART_PROPERTY_STRING prop_id) const;
   bool              GetString(const ENUM_CHART_PROPERTY_STRING prop_id,string &value) const;
   bool              SetString(const ENUM_CHART_PROPERTY_STRING prop_id,const string value) const;
   bool              SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period) const;
   bool              ApplyTemplate(const string filename) const;
   bool              ScreenShot(const string filename,const int width,const int height,
                                const ENUM_ALIGN_MODE align_mode=ALIGN_RIGHT) const;
   int               WindowOnDropped(void) const;
   double            PriceOnDropped(void) const;
   datetime          TimeOnDropped(void) const;
   int               XOnDropped(void) const;
   int               YOnDropped(void) const;
   //--- methods for working with indicators
   bool              IndicatorAdd(const int subwin,const int handle) const;
   bool              IndicatorDelete(const int subwin,const string name) const;
   int               IndicatorsTotal(const int subwin) const;
   string            IndicatorName(const int subwin,const int index) const;
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChart::CChart(void) : m_chart_id(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChart::~CChart(void)
  {
   if(m_chart_id!=-1)
      Close();
  }
//+------------------------------------------------------------------+
//| Opening chart                                                    |
//+------------------------------------------------------------------+
long CChart::Open(const string symbol_name,const ENUM_TIMEFRAMES timeframe)
  {
   m_chart_id=ChartOpen(symbol_name,timeframe);
   if(m_chart_id==0)
      m_chart_id=-1;
   return(m_chart_id);
  }
//+------------------------------------------------------------------+
//| Get the type of representation of chart                          |
//+------------------------------------------------------------------+
ENUM_CHART_MODE CChart::Mode(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(WRONG_VALUE);
//--- result
   return((ENUM_CHART_MODE)ChartGetInteger(m_chart_id,CHART_MODE));
  }
//+------------------------------------------------------------------+
//| Set the type of representation chart                             |
//+------------------------------------------------------------------+
bool CChart::Mode(const ENUM_CHART_MODE mode) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_MODE,mode));
  }
//+------------------------------------------------------------------+
//| Get value of the "Foreground" property                           |
//+------------------------------------------------------------------+
bool CChart::Foreground(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_FOREGROUND));
  }
//+------------------------------------------------------------------+
//| Set value of the "Foreground" property                           |
//+------------------------------------------------------------------+
bool CChart::Foreground(const bool foreground) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_FOREGROUND,foreground));
  }
//+------------------------------------------------------------------+
//| Get value of the "Shift" property                                |
//+------------------------------------------------------------------+
bool CChart::Shift(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SHIFT));
  }
//+------------------------------------------------------------------+
//| Set value of the "Shift"property                                 |
//+------------------------------------------------------------------+
bool CChart::Shift(const bool shift) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHIFT,shift));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShiftSize" property                            |
//+------------------------------------------------------------------+
double CChart::ShiftSize(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(DBL_MAX);
//--- result
   return(ChartGetDouble(m_chart_id,CHART_SHIFT_SIZE));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShiftSize" property                            |
//+------------------------------------------------------------------+
bool CChart::ShiftSize(double shift) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(shift<10)
      shift=10;
   if(shift>50)
      shift=50;
//--- result
   return(ChartSetDouble(m_chart_id,CHART_SHIFT_SIZE,shift));
  }
//+------------------------------------------------------------------+
//| Get value of the "AutoScroll" property                           |
//+------------------------------------------------------------------+
bool CChart::AutoScroll(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_AUTOSCROLL));
  }
//+------------------------------------------------------------------+
//| Set value of the "AutoScroll" property                           |
//+------------------------------------------------------------------+
bool CChart::AutoScroll(const bool auto_scroll) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_AUTOSCROLL,auto_scroll));
  }
//+------------------------------------------------------------------+
//| Get value of the "Scale" property                                |
//+------------------------------------------------------------------+
int CChart::Scale(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_SCALE));
  }
//+------------------------------------------------------------------+
//| Set value of the "Scale" property                                |
//+------------------------------------------------------------------+
bool CChart::Scale(int shift) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
   if(shift<0)
      shift=0;
   if(shift>32)
      shift=32;
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SCALE,shift));
  }
//+------------------------------------------------------------------+
//| Get value of the "ScaleFix" property                             |
//+------------------------------------------------------------------+
bool CChart::ScaleFix(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SCALEFIX));
  }
//+------------------------------------------------------------------+
//| Set value of the "ScaleFix" property                             |
//+------------------------------------------------------------------+
bool CChart::ScaleFix(const bool scale_fix) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SCALEFIX,scale_fix));
  }
//+------------------------------------------------------------------+
//| Get value of the "ScaleFix_11" property                          |
//+------------------------------------------------------------------+
bool CChart::ScaleFix_11(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SCALEFIX_11));
  }
//+------------------------------------------------------------------+
//| Set value of the "ScaleFix_11" property                          |
//+------------------------------------------------------------------+
bool CChart::ScaleFix_11(const bool scale_fix_11) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SCALEFIX_11,scale_fix_11));
  }
//+------------------------------------------------------------------+
//| Get value of the "FixedMax" property                             |
//+------------------------------------------------------------------+
double CChart::FixedMax(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ChartGetDouble(m_chart_id,CHART_FIXED_MAX));
  }
//+------------------------------------------------------------------+
//| Set value of the "FixedMax" property                             |
//+------------------------------------------------------------------+
bool CChart::FixedMax(const double fixed_max) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetDouble(m_chart_id,CHART_FIXED_MAX,fixed_max));
  }
//+------------------------------------------------------------------+
//| Get value of the "FixedMin" property                             |
//+------------------------------------------------------------------+
double CChart::FixedMin(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ChartGetDouble(m_chart_id,CHART_FIXED_MIN));
  }
//+------------------------------------------------------------------+
//| Set value of the "FixedMin" property                             |
//+------------------------------------------------------------------+
bool CChart::FixedMin(const double fixed_min) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetDouble(m_chart_id,CHART_FIXED_MIN,fixed_min));
  }
//+------------------------------------------------------------------+
//| Get value of the "ScalePointsPerBar" property                    |
//+------------------------------------------------------------------+
bool CChart::ScalePPB(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SCALE_PT_PER_BAR));
  }
//+------------------------------------------------------------------+
//| Set value of the "ScalePointsPerBar" property                    |
//+------------------------------------------------------------------+
bool CChart::ScalePPB(const bool scale_ppb) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SCALE_PT_PER_BAR,scale_ppb));
  }
//+------------------------------------------------------------------+
//| Get value of the "PointsPerBar" property                         |
//+------------------------------------------------------------------+
double CChart::PointsPerBar(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ChartGetDouble(m_chart_id,CHART_POINTS_PER_BAR));
  }
//+------------------------------------------------------------------+
//| Set value of the "PointsPerBar" property                         |
//+------------------------------------------------------------------+
bool CChart::PointsPerBar(const double points_per_bar) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetDouble(m_chart_id,CHART_POINTS_PER_BAR,points_per_bar));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShowOHLC" property                             |
//+------------------------------------------------------------------+
bool CChart::ShowOHLC(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SHOW_OHLC));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowOHLC" property                             |
//+------------------------------------------------------------------+
bool CChart::ShowOHLC(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_OHLC,show));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShowLineBid" property                          |
//+------------------------------------------------------------------+
bool CChart::ShowLineBid(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SHOW_BID_LINE));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowLineBid" property                          |
//+------------------------------------------------------------------+
bool CChart::ShowLineBid(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_BID_LINE,show));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShowLineAsk" property                          |
//+------------------------------------------------------------------+
bool CChart::ShowLineAsk(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SHOW_ASK_LINE));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowLineAsk" property                          |
//+------------------------------------------------------------------+
bool CChart::ShowLineAsk(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_ASK_LINE,show));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShowLastLine" property                         |
//+------------------------------------------------------------------+
bool CChart::ShowLastLine(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SHOW_LAST_LINE));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowLastLine" property                         |
//+------------------------------------------------------------------+
bool CChart::ShowLastLine(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_LAST_LINE,show));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShowPeriodSep" property                        |
//+------------------------------------------------------------------+
bool CChart::ShowPeriodSep(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SHOW_PERIOD_SEP));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowPeriodSep" property                        |
//+------------------------------------------------------------------+
bool CChart::ShowPeriodSep(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_PERIOD_SEP,show));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShowGrid" property                             |
//+------------------------------------------------------------------+
bool CChart::ShowGrid(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SHOW_GRID));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowGrid" property                             |
//+------------------------------------------------------------------+
bool CChart::ShowGrid(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_GRID,show));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShowVolumes" property                          |
//+------------------------------------------------------------------+
ENUM_CHART_VOLUME_MODE CChart::ShowVolumes(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(WRONG_VALUE);
//--- result
   return((ENUM_CHART_VOLUME_MODE)ChartGetInteger(m_chart_id,CHART_SHOW_VOLUMES));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowVolumes" property                          |
//+------------------------------------------------------------------+
bool CChart::ShowVolumes(const ENUM_CHART_VOLUME_MODE show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_VOLUMES,show));
  }
//+------------------------------------------------------------------+
//| Get value of the "ShowObjectDescr" property                      |
//+------------------------------------------------------------------+
bool CChart::ShowObjectDescr(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_SHOW_OBJECT_DESCR));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowObjectDescr" property                      |
//+------------------------------------------------------------------+
bool CChart::ShowObjectDescr(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_OBJECT_DESCR,show));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowDateScale" property                        |
//+------------------------------------------------------------------+
bool CChart::ShowDateScale(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_DATE_SCALE,show));
  }
//+------------------------------------------------------------------+
//| Set value of the "ShowPriceScale" property                       |
//+------------------------------------------------------------------+
bool CChart::ShowPriceScale(const bool show) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_SHOW_PRICE_SCALE,show));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Background" property                     |
//+------------------------------------------------------------------+
color CChart::ColorBackground(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_BACKGROUND));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Background" property                     |
//+------------------------------------------------------------------+
bool CChart::ColorBackground(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_BACKGROUND,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Foreground" property                     |
//+------------------------------------------------------------------+
color CChart::ColorForeground(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_FOREGROUND));
  }
//+------------------------------------------------------------------+
//| Set color value for the "Foreground" property                    |
//+------------------------------------------------------------------+
bool CChart::ColorForeground(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_FOREGROUND,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Grid" property                           |
//+------------------------------------------------------------------+
color CChart::ColorGrid(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_GRID));
  }
//+------------------------------------------------------------------+
//| Set color value for the "Grid" property                          |
//+------------------------------------------------------------------+
bool CChart::ColorGrid(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_GRID,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Bar Up" property                         |
//+------------------------------------------------------------------+
color CChart::ColorBarUp(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_CHART_UP));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Bar Up" property                         |
//+------------------------------------------------------------------+
bool CChart::ColorBarUp(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_CHART_UP,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Bar Down" property                       |
//+------------------------------------------------------------------+
color CChart::ColorBarDown(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_CHART_DOWN));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Bar Down" property                       |
//+------------------------------------------------------------------+
bool CChart::ColorBarDown(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_CHART_DOWN,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Candle Bull" property                    |
//+------------------------------------------------------------------+
color CChart::ColorCandleBull(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_CANDLE_BULL));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Candle Bull" property                    |
//+------------------------------------------------------------------+
bool CChart::ColorCandleBull(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_CANDLE_BULL,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Candle Bear" property                    |
//+------------------------------------------------------------------+
color CChart::ColorCandleBear(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_CANDLE_BEAR));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Candle Bear" property                    |
//+------------------------------------------------------------------+
bool CChart::ColorCandleBear(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_CANDLE_BEAR,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Chart Line" property                     |
//+------------------------------------------------------------------+
color CChart::ColorChartLine(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_CHART_LINE));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Chart Line" property                     |
//+------------------------------------------------------------------+
bool CChart::ColorChartLine(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_CHART_LINE,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Volumes" property                        |
//+------------------------------------------------------------------+
color CChart::ColorVolumes(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_VOLUME));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Volumes" property                        |
//+------------------------------------------------------------------+
bool CChart::ColorVolumes(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_VOLUME,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Line Bid" property                       |
//+------------------------------------------------------------------+
color CChart::ColorLineBid(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_BID));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Line Bid" property                       |
//+------------------------------------------------------------------+
bool CChart::ColorLineBid(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_BID,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Line Ask" property                       |
//+------------------------------------------------------------------+
color CChart::ColorLineAsk(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_ASK));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Line Ask" property                       |
//+------------------------------------------------------------------+
bool CChart::ColorLineAsk(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_ASK,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Line Last" property                      |
//+------------------------------------------------------------------+
color CChart::ColorLineLast(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_LAST));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Line Last" property                      |
//+------------------------------------------------------------------+
bool CChart::ColorLineLast(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_LAST,new_color));
  }
//+------------------------------------------------------------------+
//| Get color value of the "Stop Levels" property                    |
//+------------------------------------------------------------------+
color CChart::ColorStopLevels(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(CLR_NONE);
//--- result
   return((color)ChartGetInteger(m_chart_id,CHART_COLOR_STOP_LEVEL));
  }
//+------------------------------------------------------------------+
//| Set color value of the "Stop Levels" property                    |
//+------------------------------------------------------------------+
bool CChart::ColorStopLevels(const color new_color) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_COLOR_STOP_LEVEL,new_color));
  }
//+------------------------------------------------------------------+
//| Shows chart always on top                                        |
//+------------------------------------------------------------------+
bool CChart::BringToTop(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_BRING_TO_TOP,true));
  }
//+------------------------------------------------------------------+
//| Sets flag to generate event of creating objects                  |
//+------------------------------------------------------------------+
bool CChart::EventObjectCreate(const bool flag) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_EVENT_OBJECT_CREATE,flag));
  }
//+------------------------------------------------------------------+
//| Sets flag to generate event of deleting objects                  |
//+------------------------------------------------------------------+
bool CChart::EventObjectDelete(const bool flag) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_EVENT_OBJECT_DELETE,flag));
  }
//+------------------------------------------------------------------+
//| Sets flag to generate event of moving mouse cursor               |
//+------------------------------------------------------------------+
bool CChart::EventMouseMove(const bool flag) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_EVENT_MOUSE_MOVE,flag));
  }
//+------------------------------------------------------------------+
//| Sets flag to mouse scrolling                                     |
//+------------------------------------------------------------------+
bool CChart::MouseScroll(const bool flag) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetInteger(m_chart_id,CHART_MOUSE_SCROLL,flag));
  }
//+------------------------------------------------------------------+
//| Get value of the "VisibleBars" property                          |
//+------------------------------------------------------------------+
int CChart::VisibleBars(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_WIDTH_IN_BARS));
  }
//+------------------------------------------------------------------+
//| Get value of the "WindowsTotal" property                         |
//+------------------------------------------------------------------+
int CChart::WindowsTotal(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_WINDOWS_TOTAL));
  }
//+------------------------------------------------------------------+
//| Get value of the "WindowIsVisible" property                      |
//+------------------------------------------------------------------+
bool CChart::WindowIsVisible(const int num) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_WINDOW_IS_VISIBLE,num));
  }
//+------------------------------------------------------------------+
//| Get value of the "WindowHandle" property                         |
//+------------------------------------------------------------------+
int CChart::WindowHandle(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(INVALID_HANDLE);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_WINDOW_HANDLE));
  }
//+------------------------------------------------------------------+
//| Get value of the "FirstVisibleBar" property                      |
//+------------------------------------------------------------------+
int CChart::FirstVisibleBar(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(-1);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_FIRST_VISIBLE_BAR));
  }
//+------------------------------------------------------------------+
//| Get value of the "WidthInBars" property                          |
//+------------------------------------------------------------------+
int CChart::WidthInBars(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_WIDTH_IN_BARS));
  }
//+------------------------------------------------------------------+
//| Get value of the "WidthInPixels" property                        |
//+------------------------------------------------------------------+
int CChart::WidthInPixels(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_WIDTH_IN_PIXELS));
  }
//+------------------------------------------------------------------+
//| Get value of the "HeightInPixels" property                       |
//+------------------------------------------------------------------+
int CChart::HeightInPixels(const int num) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_HEIGHT_IN_PIXELS,num));
  }
//+------------------------------------------------------------------+
//| Get value of the "WindowYDistance" property                      |
//+------------------------------------------------------------------+
int CChart::SubwindowY(const int num) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ChartGetInteger(m_chart_id,CHART_WINDOW_YDISTANCE,num));
  }
//+------------------------------------------------------------------+
//| Get value of the "PriceMin" property                             |
//+------------------------------------------------------------------+
double CChart::PriceMin(const int num) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ChartGetDouble(m_chart_id,CHART_PRICE_MIN,num));
  }
//+------------------------------------------------------------------+
//| Get value of the "PriceMax" property                             |
//+------------------------------------------------------------------+
double CChart::PriceMax(const int num) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ChartGetDouble(m_chart_id,CHART_PRICE_MAX,num));
  }
//+------------------------------------------------------------------+
//| Get value of the "IsObject" property                             |
//+------------------------------------------------------------------+
bool CChart::IsObject(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ChartGetInteger(m_chart_id,CHART_IS_OBJECT));
  }
//+------------------------------------------------------------------+
//| Chart close                                                      |
//+------------------------------------------------------------------+
void CChart::Close(void)
  {
   if(m_chart_id!=-1)
     {
      ChartClose(m_chart_id);
      m_chart_id=-1;
     }
  }
//+------------------------------------------------------------------+
//| Chart navigation                                                 |
//+------------------------------------------------------------------+
bool CChart::Navigate(const ENUM_CHART_POSITION position,const int shift) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartNavigate(m_chart_id,position,shift));
  }
//+------------------------------------------------------------------+
//| Access functions long ChartGetInteger(...)                       |
//+------------------------------------------------------------------+
long CChart::GetInteger(const ENUM_CHART_PROPERTY_INTEGER prop_id,const int subwindow) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return(ChartGetInteger(m_chart_id,prop_id,subwindow));
  }
//+------------------------------------------------------------------+
//| Access function bool ChartGetInteger(...)                        |
//+------------------------------------------------------------------+
bool CChart::GetInteger(const ENUM_CHART_PROPERTY_INTEGER prop_id,const int subwindow,long &value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartGetInteger(m_chart_id,prop_id,subwindow,value));
  }
//+------------------------------------------------------------------+
//| Access function  ChartSetInteger(...)                            |
//+------------------------------------------------------------------+
bool CChart::SetInteger(const ENUM_CHART_PROPERTY_INTEGER prop_id,const long value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//---
   return(ChartSetInteger(m_chart_id,prop_id,value));
  }
//+------------------------------------------------------------------+
//| Access function double ChartGetDouble(...)                       |
//+------------------------------------------------------------------+
double CChart::GetDouble(const ENUM_CHART_PROPERTY_DOUBLE prop_id,const int subwindow) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ChartGetDouble(m_chart_id,prop_id,subwindow));
  }
//+------------------------------------------------------------------+
//| Access function bool ChartGetDouble(...)                         |
//+------------------------------------------------------------------+
bool CChart::GetDouble(const ENUM_CHART_PROPERTY_DOUBLE prop_id,const int subwindow,double &value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartGetDouble(m_chart_id,prop_id,subwindow,value));
  }
//+------------------------------------------------------------------+
//| Access function ChartSetDouble(...)                              |
//+------------------------------------------------------------------+
bool CChart::SetDouble(const ENUM_CHART_PROPERTY_DOUBLE prop_id,const double value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetDouble(m_chart_id,prop_id,value));
  }
//+------------------------------------------------------------------+
//| Access function string ChartGetString(...)                       |
//+------------------------------------------------------------------+
string CChart::GetString(const ENUM_CHART_PROPERTY_STRING prop_id) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//--- result
   return(ChartGetString(m_chart_id,prop_id));
  }
//+------------------------------------------------------------------+
//| Access functions bool ChartGetString(...)                        |
//+------------------------------------------------------------------+
bool CChart::GetString(const ENUM_CHART_PROPERTY_STRING prop_id,string &value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartGetString(m_chart_id,prop_id,value));
  }
//+------------------------------------------------------------------+
//| Access function ChartSetString(...)                              |
//+------------------------------------------------------------------+
bool CChart::SetString(const ENUM_CHART_PROPERTY_STRING prop_id,const string value) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetString(m_chart_id,prop_id,value));
  }
//+------------------------------------------------------------------+
//| Access function ChartSetSymbolPeriod(...)                        |
//+------------------------------------------------------------------+
bool CChart::SetSymbolPeriod(const string symbol,const ENUM_TIMEFRAMES period) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartSetSymbolPeriod(m_chart_id,symbol,period));
  }
//+------------------------------------------------------------------+
//| Access function ChartApplyTemplate(...)                          |
//+------------------------------------------------------------------+
bool CChart::ApplyTemplate(const string filename) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartApplyTemplate(m_chart_id,filename));
  }
//+------------------------------------------------------------------+
//| Access function ChartScreenShot(...)                             |
//+------------------------------------------------------------------+
bool CChart::ScreenShot(const string filename,const int width,const int height,const ENUM_ALIGN_MODE align_mode) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartScreenShot(m_chart_id,filename,width,height,align_mode));
  }
//+------------------------------------------------------------------+
//| Access function WindowOnDropped()                                |
//+------------------------------------------------------------------+
int CChart::WindowOnDropped(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return(ChartWindowOnDropped());
  }
//+------------------------------------------------------------------+
//| Access function PriceOnDropped()                                 |
//+------------------------------------------------------------------+
double CChart::PriceOnDropped(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(EMPTY_VALUE);
//--- result
   return(ChartPriceOnDropped());
  }
//+------------------------------------------------------------------+
//| Access function TimeOnDropped()                                  |
//+------------------------------------------------------------------+
datetime CChart::TimeOnDropped(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartTimeOnDropped());
  }
//+------------------------------------------------------------------+
//| Access functions XOnDropped()                                    |
//+------------------------------------------------------------------+
int CChart::XOnDropped(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return(ChartXOnDropped());
  }
//+------------------------------------------------------------------+
//| Access functions YOnDropped()                                    |
//+------------------------------------------------------------------+
int CChart::YOnDropped(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return(ChartYOnDropped());
  }
//+------------------------------------------------------------------+
//| Adds indicator to chart                                          |
//+------------------------------------------------------------------+
bool CChart::IndicatorAdd(const int subwin,const int handle) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartIndicatorAdd(m_chart_id,subwin,handle));
  }
//+------------------------------------------------------------------+
//| Deletes indicator from chart                                     |
//+------------------------------------------------------------------+
bool CChart::IndicatorDelete(const int subwin,const string name) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ChartIndicatorDelete(m_chart_id,subwin,name));
  }
//+------------------------------------------------------------------+
//| Gets number of indicators in chart subwindow                     |
//+------------------------------------------------------------------+
int CChart::IndicatorsTotal(const int subwin) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return(ChartIndicatorsTotal(m_chart_id,subwin));
  }
//+------------------------------------------------------------------+
//| Gets short name of indicator                                     |
//+------------------------------------------------------------------+
string CChart::IndicatorName(const int subwin,const int index) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//--- result
   return(ChartIndicatorName(m_chart_id,subwin,index));
  }
//+------------------------------------------------------------------+
//| Writing parameters of chart to file                              |
//+------------------------------------------------------------------+
bool CChart::Save(const int file_handle)
  {
   string work_str;
   int    work_int;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write start marker - 0xFFFFFFFFFFFFFFFF
   if(FileWriteLong(file_handle,-1)!=sizeof(long))
      return(false);
//--- write chart type
   if(FileWriteInteger(file_handle,Type(),INT_VALUE)!=INT_VALUE)
      return(false);
//--- write chart symbol
   work_str=Symbol();
   work_int=StringLen(work_str);
   if(FileWriteInteger(file_handle,work_int,INT_VALUE)!=INT_VALUE)
      return(false);
   if(work_int!=0) if(FileWriteString(file_handle,work_str,work_int)!=work_int)
      return(false);
//--- write period of chart
   if(FileWriteInteger(file_handle,Period(),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Mode" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_MODE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Foreground" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_FOREGROUND),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "Shift" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHIFT),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "ShiftSize" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHIFT),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "AutoScroll" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_AUTOSCROLL),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "Scale" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SCALE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "ScaleFix" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SCALEFIX),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "ScaleFix_11" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SCALEFIX_11),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "FixedMax" property
   if(FileWriteDouble(file_handle,ChartGetDouble(m_chart_id,CHART_FIXED_MAX))!=sizeof(double))
      return(false);
//--- write value of the "FixedMin" property
   if(FileWriteDouble(file_handle,ChartGetDouble(m_chart_id,CHART_FIXED_MIN))!=sizeof(double))
      return(false);
//--- write the "ScalePPB" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SCALE_PT_PER_BAR),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "PointsPerBar" property
   if(FileWriteDouble(file_handle,ChartGetDouble(m_chart_id,CHART_POINTS_PER_BAR))!=sizeof(double))
      return(false);
//--- write value of the "ShowOHLC" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHOW_OHLC),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "ShowLineBid" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHOW_BID_LINE),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "ShowLineAsk" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHOW_ASK_LINE),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "ShowLastLine" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHOW_LAST_LINE),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "ShowPeriodSep" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHOW_PERIOD_SEP),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "ShowGrid" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHOW_GRID),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write value of the "ShowVolumes" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHOW_VOLUMES),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "ShowObjectDescr" property
   if(FileWriteInteger(file_handle,(int)ChartGetInteger(m_chart_id,CHART_SHOW_OBJECT_DESCR),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of chart from file                            |
//+------------------------------------------------------------------+
bool CChart::Load(const int file_handle)
  {
   bool   resutl=true;
   string work_str;
   int    work_int;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read and checking start marker - 0xFFFFFFFFFFFFFFFF
   if(FileReadLong(file_handle)!=-1) return(false);
//--- read and checking chart type
   if(FileReadInteger(file_handle,INT_VALUE)!=Type()) return(false);
//--- read chart symbol
   work_int=FileReadInteger(file_handle);
   if(work_int!=0) work_str=FileReadString(file_handle,work_int);
   else            work_str="";
//--- read chart period
   work_int=FileReadInteger(file_handle);
   SetSymbolPeriod(work_str,(ENUM_TIMEFRAMES)work_int);
//--- read value of the "Mode" property
   if(!ChartSetInteger(m_chart_id,CHART_MODE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "Foreground" property
   if(!ChartSetInteger(m_chart_id,CHART_FOREGROUND,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "Shift" property
   if(!ChartSetInteger(m_chart_id,CHART_SHIFT,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "ShiftSize" property
   if(!ChartSetInteger(m_chart_id,CHART_SHIFT,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "AutoScroll" property
   if(!ChartSetInteger(m_chart_id,CHART_AUTOSCROLL,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "Scale" property
   if(!ChartSetInteger(m_chart_id,CHART_SCALE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "ScaleFix" property
   if(!ChartSetInteger(m_chart_id,CHART_SCALEFIX,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "ScaleFix_11" property
   if(!ChartSetInteger(m_chart_id,CHART_SCALEFIX_11,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "FixedMax" property
   if(!ChartSetDouble(m_chart_id,CHART_FIXED_MAX,FileReadDatetime(file_handle)))
      return(false);
//--- read value of the "FixedMin" property
   if(!ChartSetDouble(m_chart_id,CHART_FIXED_MIN,FileReadDatetime(file_handle)))
      return(false);
//--- read value of the "ScalePPB" property
   if(!ChartSetInteger(m_chart_id,CHART_SCALE_PT_PER_BAR,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "PointsPerBar" property
   if(!ChartSetDouble(m_chart_id,CHART_POINTS_PER_BAR,FileReadDatetime(file_handle)))
      return(false);
//--- read value of the "ShowOHLC" property
   if(!ChartSetInteger(m_chart_id,CHART_SHOW_OHLC,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "ShowLineBid" property
   if(!ChartSetInteger(m_chart_id,CHART_SHOW_BID_LINE,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "ShowLineAsk" property
   if(!ChartSetInteger(m_chart_id,CHART_SHOW_ASK_LINE,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "ShowLastLine" property
   if(!ChartSetInteger(m_chart_id,CHART_SHOW_LAST_LINE,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "ShowPeriodSep" property
   if(!ChartSetInteger(m_chart_id,CHART_SHOW_PERIOD_SEP,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "ShowGrid" property
   if(!ChartSetInteger(m_chart_id,CHART_SHOW_GRID,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read value of the "ShowVolumes" property
   if(!ChartSetInteger(m_chart_id,CHART_SHOW_VOLUMES,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "ShowObjectDescr" property
   if(!ChartSetInteger(m_chart_id,CHART_SHOW_OBJECT_DESCR,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- successful
   return(resutl);
  }
//+------------------------------------------------------------------+
