//+------------------------------------------------------------------+
//|                                                  ChartSample.mq5 |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "2009-2017, MetaQuotes Software Corp."
#property link      "http://www.mql5.com"
//---
#include <Charts\Chart.mqh>
#include <ChartObjects\ChartObjectsTxtControls.mqh>
#include <ChartObjects\ChartObjectPanel.mqh>
//---
#include "ChartSampleInit.mqh"
//+------------------------------------------------------------------+
//| Script to demonstrate the use of class CChart.                   |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Chart Sample script class                                        |
//+------------------------------------------------------------------+
class CChartSample
  {
protected:
   CChart            m_chart;                     // instance of the class to access properties chart
   CChartObjectButton *m_button[NUM_BUTTONS];       // array of pointers other buttons
   CChartObjectButton *m_button_tf[20];             // array of pointers period buttons
   CChartObjectButton *m_button_sym[];              // array of pointers symbol buttons
   int               m_num_symbols;               // number of symbol
   CChartObjectEdit *m_edit[NUM_EDITS];           // array of pointers other edits
   CChartObjectEdit *m_edit_color[13];            // array of pointers to colors show
   CChartObjectEdit *m_edit_rgb[13][3];           // array of pointers to RGB show
   CChartObjectLabel *m_label[NUM_LABELS];         // array of pointers to labels
   CChartObjectPanel m_panel[NUM_PANELS];         // array of panels

public:
                     CChartSample(void);
                    ~CChartSample(void);
   //--- initialization
   bool              Init(void);
   void              Deinit(void);
   //--- processing
   void              Processing(void);

private:
   void              CheckPanelModes(void);
   void              CheckPanelAnothers(void);
   void              CheckPanelScales(void);
   void              CheckPanelShows(void);
   void              CheckPanelTimeframes(void);
   void              CheckPanelSymbols(void);
   void              CheckPanelColors(void);
   void              CheckPanelReadOnly(void);
  };
//---
CChartSample ExtScript;
//+------------------------------------------------------------------+
//| Constructor.                                                     |
//+------------------------------------------------------------------+
CChartSample::CChartSample(void) : m_num_symbols(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor.                                                      |
//+------------------------------------------------------------------+
CChartSample::~CChartSample(void)
  {
//--- does not perform any action
//--- all dynamic objects created in the method Init(),
//--- will be deleted when deleting panels,
//--- to which they were added
  }
//+------------------------------------------------------------------+
//| Method CheckPanelModes.                                          |
//+------------------------------------------------------------------+
void CChartSample::CheckPanelModes(void)
  {
   if(m_button[0].State() && m_chart.Mode()!=CHART_BARS)
     {
      //--- Set Bars Mode
      m_button[1].State(false);
      m_button[2].State(false);
      m_chart.Mode(CHART_BARS);
     }
   if(m_button[1].State() && m_chart.Mode()!=CHART_CANDLES)
     {
      //--- Set Candles Mode
      m_button[0].State(false);
      m_button[2].State(false);
      m_chart.Mode(CHART_CANDLES);
     }
   if(m_button[2].State() && m_chart.Mode()!=CHART_LINE)
     {
      //--- Set Line Mode
      m_button[0].State(false);
      m_button[1].State(false);
      m_chart.Mode(CHART_LINE);
     }
  }
//+------------------------------------------------------------------+
//| Method CheckPanelAnothers.                                       |
//+------------------------------------------------------------------+
void CChartSample::CheckPanelAnothers(void)
  {
   int i,j;
//--- Set Autoscroll
   if(m_button[3].State())
     {
      if(!m_chart.AutoScroll())
         m_chart.AutoScroll(true);
     }
   else
     {
      if(m_chart.AutoScroll())
         m_chart.AutoScroll(false);
     }
//--- Set Shift
   if(m_button[4].State())
     {
      if(m_edit[0].Description()=="")
         m_edit[0].Description(DoubleToString(m_chart.ShiftSize()));
      else
        {
         i=(int)StringToInteger(m_edit[0].Description());
         j=i;
         if(i>50)
            i=50;
         if(i<10)
            i=10;
         if(j!=i)
            m_edit[0].Description(IntegerToString(i));
         if(i!=m_chart.ShiftSize())
            m_chart.ShiftSize(i);
        }
      if(!m_chart.Shift())
         m_chart.Shift(true);
     }
   else
     {
      m_edit[0].Description("");
      if(m_chart.Shift())
         m_chart.Shift(false);
     }
//--- Set Shift Size
   if(m_button[6].State())
     {
      if(m_button[4].State())
        {
         //--- Set Shift Size Up
         i=(int)StringToInteger(m_edit[0].Description());
         if(i<50)
            m_chart.ShiftSize(++i);
         m_edit[0].Description(IntegerToString(i));
        }
      m_button[6].State(false);
     }
   if(m_button[7].State())
     {
      if(m_button[4].State())
        {
         //--- Set Shift Size Down
         i=(int)StringToInteger(m_edit[0].Description());
         if(i>10)
            m_chart.ShiftSize(--i);
         m_edit[0].Description(IntegerToString(i));
        }
      m_button[7].State(false);
     }
   m_edit[2].Description(DoubleToString(m_chart.ShiftSize()));
//--- Set Foreground
   if(m_button[5].State())
     {
      if(!m_chart.Foreground())
         m_chart.Foreground(true);
     }
   else
     {
      if(m_chart.Foreground())
         m_chart.Foreground(false);
     }
  }
//+------------------------------------------------------------------+
//| Method CheckPanelScales.                                         |
//+------------------------------------------------------------------+
void CChartSample::CheckPanelScales(void)
  {
   int    i;
   double d;
//--- Set Scale fix
   if(m_button[8].State())
     {
      if(m_edit[4].Description()=="")
        {
         m_edit[4].Description(DoubleToString(m_chart.PriceMax(0),4));
         m_edit[5].Description(DoubleToString(m_chart.PriceMin(0),4));
        }
      if(!m_chart.ScaleFix())
         m_chart.ScaleFix(true);
     }
   else
     {
      if(m_edit[4].Description()!="")
        {
         m_edit[4].Description("");
         m_edit[5].Description("");
        }
      if(m_chart.ScaleFix())
         m_chart.ScaleFix(false);
     }
//--- Set Scale fix 1 to 1
   if(m_button[9].State())
     {
      if(!m_chart.ScaleFix_11())
         m_chart.ScaleFix_11(true);
     }
   else
     {
      if(m_chart.ScaleFix_11())
         m_chart.ScaleFix_11(false);
     }
//--- Set Scale
   if(m_button[10].State())
     {
      //--- Set Scale Up
      i=(int)StringToInteger(m_edit[1].Description());
      if(i<5)
        {
         i++;
         m_chart.Scale(i);
         m_edit[1].Description(IntegerToString(i));
        }
      m_button[10].State(false);
     }
   if(m_button[11].State())
     {
      //--- Set Scale Down
      i=(int)StringToInteger(m_edit[1].Description());
      if(i>0)
        {
         i--;
         m_chart.Scale(i);
         m_edit[1].Description(IntegerToString(i));
        }
      m_button[11].State(false);
     }
   m_edit[3].Description(IntegerToString(m_chart.Scale()));
//--- Set Fixed Max
   if(m_button[12].State())
     {
      if(m_chart.ScaleFix())
        {
         d=StringToDouble(m_edit[4].Description());
         if(m_chart.FixedMax()!=d)
            m_chart.FixedMax(d);
         m_edit[4].Description(DoubleToString(d,4));
        }
      else
         m_edit[4].Description("");
      m_button[12].State(false);
     }
//--- Set Fixed Min
   if(m_button[13].State())
     {
      if(m_chart.ScaleFix())
        {
         d=StringToDouble(m_edit[5].Description());
         if(m_chart.FixedMin()!=d)
            m_chart.FixedMin(d);
         m_edit[5].Description(DoubleToString(d,4));
        }
      else
         m_edit[5].Description("");
      m_button[13].State(false);
     }
//--- Set Scale PPB
   if(m_button[14].State())
     {
      if(m_edit[6].Description()=="")
        {
         d=m_chart.PointsPerBar();
         if(d==0.0)
           {
            d=1.0;
            m_chart.PointsPerBar(d);
           }
         m_edit[6].Description(DoubleToString(d,4));
        }
      if(!m_chart.ScalePPB())
         m_chart.ScalePPB(true);
      d=StringToDouble(m_edit[6].Description());
      if(m_chart.PointsPerBar()!=d)
         m_chart.PointsPerBar(d);
     }
   else
     {
      m_edit[6].Description("");
      if(m_chart.ScalePPB())
         m_chart.ScalePPB(false);
     }
  }
//+------------------------------------------------------------------+
//| Method CheckPanelShows.                                          |
//+------------------------------------------------------------------+
void CChartSample::CheckPanelShows(void)
  {
//--- Set Show OHLC
   if(m_button[15].State())
     {
      if(!m_chart.ShowOHLC())
         m_chart.ShowOHLC(true);
     }
   else
     {
      if(m_chart.ShowOHLC())
         m_chart.ShowOHLC(false);
     }
//--- Set Show Bid
   if(m_button[16].State())
     {
      if(!m_chart.ShowLineBid())
         m_chart.ShowLineBid(true);
     }
   else
     {
      if(m_chart.ShowLineBid())
         m_chart.ShowLineBid(false);
     }
//--- Set Show Ask
   if(m_button[17].State())
     {
      if(!m_chart.ShowLineAsk())
         m_chart.ShowLineAsk(true);
     }
   else
     {
      if(m_chart.ShowLineAsk())
         m_chart.ShowLineAsk(false);
     }
//--- Set Show Last
   if(m_button[18].State())
     {
      if(!m_chart.ShowLastLine())
         m_chart.ShowLastLine(true);
     }
   else
     {
      if(m_chart.ShowLastLine())
         m_chart.ShowLastLine(false);
     }
//--- Set Show Separator
   if(m_button[19].State())
     {
      if(!m_chart.ShowPeriodSep())
         m_chart.ShowPeriodSep(true);
     }
   else
     {
      if(m_chart.ShowPeriodSep())
         m_chart.ShowPeriodSep(false);
     }
//--- Set Show Grid
   if(m_button[20].State())
     {
      if(!m_chart.ShowGrid())
         m_chart.ShowGrid(true);
     }
   else
     {
      if(m_chart.ShowGrid())
         m_chart.ShowGrid(false);
     }
//--- Set Show Objects Descriptor
   if(m_button[21].State())
     {
      if(!m_chart.ShowObjectDescr())
         m_chart.ShowObjectDescr(true);
     }
   else
     {
      if(m_chart.ShowObjectDescr())
         m_chart.ShowObjectDescr(false);
     }
//--- Set Show Not Volumes
   if(m_button[22].State())
     {
      m_chart.ShowVolumes((ENUM_CHART_VOLUME_MODE)0);
      m_button[22].State(false);
     }
//--- Set Show Tick Volumes
   if(m_button[23].State())
     {
      m_chart.ShowVolumes((ENUM_CHART_VOLUME_MODE)1);
      m_button[23].State(false);
     }
//--- Set Show Real Volumes
   if(m_button[24].State())
     {
      m_chart.ShowVolumes((ENUM_CHART_VOLUME_MODE)2);
      m_button[24].State(false);
     }
  }
//+------------------------------------------------------------------+
//| Method CheckPanelTimeframes.                                     |
//+------------------------------------------------------------------+
void CChartSample::CheckPanelTimeframes(void)
  {
   int i,j;
//--- No Set Period PERIOD_MN
   if(m_button_tf[19].State())
      m_button_tf[19].State(false);
//--- Set Period
   for(i=0;i<20;i++)
      if(m_button_tf[i].State())
        {
         if(m_chart.Period()!=tf_int[i])
            m_chart.SetSymbolPeriod(m_chart.Symbol(),(ENUM_TIMEFRAMES)tf_int[i]);
         else
            continue;
         for(j=0;j<20;j++)
            if(i!=j)
               m_button_tf[j].State(false);
        }
  }
//+------------------------------------------------------------------+
//| Method CheckPanelSymbols.                                        |
//+------------------------------------------------------------------+
void CChartSample::CheckPanelSymbols(void)
  {
   int i,j;
//--- Set Symbol
   for(i=0;i<m_num_symbols;i++)
      if(m_button_sym[i].State())
        {
         if(m_chart.Symbol()!=SymbolName(i,true))
           {
            m_chart.SetSymbolPeriod(SymbolName(i,true),m_chart.Period());
            //--- by changing the symbol switch OFF ScaleFix if it is ON
            m_button[8].State(false);
           }
         else
            continue;
         for(j=0;j<m_num_symbols;j++)
            if(i!=j)
               m_button_sym[j].State(false);
        }
  }
//+------------------------------------------------------------------+
//| Method CheckPanelColors.                                         |
//+------------------------------------------------------------------+
void CChartSample::CheckPanelColors(void)
  {
   int   i;
   color c;
   static color yellow_on_black[]={Black,White,LightSlateGray,Yellow,Yellow,Black,White,Yellow,LimeGreen,LightSlateGray,Red,C'0,192,0',Red};
   static color green_on_black[] ={Black,White,LightSlateGray,Lime,Lime,Black,White,Lime,LimeGreen,LightSlateGray,Red,C'0,192,0',Red};
   static color black_on_white[] ={White,Black,Silver,Black,Black,White,Black,Black,Green,Silver,Silver,Silver,OrangeRed};
   static int   color_id[]=
     {
      CHART_COLOR_BACKGROUND,CHART_COLOR_FOREGROUND,CHART_COLOR_GRID,CHART_COLOR_CHART_UP,
      CHART_COLOR_CHART_DOWN,CHART_COLOR_CANDLE_BULL,CHART_COLOR_CANDLE_BEAR,CHART_COLOR_CHART_LINE,
      CHART_COLOR_VOLUME,CHART_COLOR_BID,CHART_COLOR_ASK,CHART_COLOR_LAST,CHART_COLOR_STOP_LEVEL
     };
//---
   if(m_button[25].State())
     {
      //--- Set "Yellow on Black"
      m_button[25].State(false);
      for(i=0;i<13;i++)
         m_chart.SetInteger((ENUM_CHART_PROPERTY_INTEGER)color_id[i],yellow_on_black[i]);
     }
   if(m_button[26].State())
     {
      //--- Set "Green on Black"
      m_button[26].State(false);
      for(i=0;i<13;i++)
         m_chart.SetInteger((ENUM_CHART_PROPERTY_INTEGER)color_id[i],green_on_black[i]);
     }
   if(m_button[27].State())
     {
      //--- Set "Black on White" palette
      m_button[27].State(false);
      for(i=0;i<13;i++)
         m_chart.SetInteger((ENUM_CHART_PROPERTY_INTEGER)color_id[i],black_on_white[i]);
     }
//--- tuning colors
   color color_label=(color)(m_chart.ColorBackground()^0xFFFFFF);
   for(i=7;i<NUM_LABELS;i++)
      m_label[i].Color(color_label);
   for(i=0;i<13;i++)
     {
      c=(color)m_chart.GetInteger((ENUM_CHART_PROPERTY_INTEGER)color_id[i]);
      m_edit_color[i].BackColor(c);
      m_edit_rgb[i][0].Description((string)((c&0xFF0000)>>16));
      m_edit_rgb[i][1].Description((string)((c&0xFF00)>>8));
      m_edit_rgb[i][2].Description((string)(c&0xFF));
     }
  }
//+------------------------------------------------------------------+
//| Method CheckPanelReadOnly.                                       |
//+------------------------------------------------------------------+
void CChartSample::CheckPanelReadOnly(void)
  {
   int i,j;
//--- Get VisibleBars
   m_edit[7].Description((string)m_chart.VisibleBars());
//--- Get WindowsTotal
   m_edit[8].Description((string)(j=m_chart.WindowsTotal()));
   j%=6;
//--- Get WindowIsVisible[i]
   for(i=0;i<j;i++)
      m_edit[9+i].Description((string)m_chart.WindowIsVisible(i));
//--- Get WindowHandle
   m_edit[14].Description((string)m_chart.WindowHandle());
//--- Get FirstVisibleBar
   m_edit[15].Description((string)m_chart.FirstVisibleBar());
//--- Get WidthInBars
   m_edit[16].Description((string)m_chart.WidthInBars());
//--- Get WidthInPixels
   m_edit[17].Description((string)m_chart.WidthInPixels());
//--- Get HeightInPixels[i]
   for(i=0;i<j;i++)
      m_edit[18+i].Description((string)m_chart.HeightInPixels(i));
//--- Get PriceMin[i]
   for(i=0;i<j;i++)
      m_edit[23+i].Description(DoubleToString(m_chart.PriceMin(i),4));
//--- Get PriceMax[i]
   for(i=0;i<j;i++)
      m_edit[28+i].Description(DoubleToString(m_chart.PriceMax(i),4));
//--- Get WindowOnDropped
   m_edit[33].Description((string)m_chart.WindowOnDropped());
//--- Get PriceOnDropped
   m_edit[34].Description(DoubleToString(m_chart.PriceOnDropped(),4));
//--- Get TimeOnDropped
   m_edit[35].Description(TimeToString(m_chart.TimeOnDropped()));
//--- Get XOnDropped
   m_edit[36].Description((string)m_chart.XOnDropped());
//--- YOnDropped
   m_edit[37].Description((string)m_chart.YOnDropped());
  }
//+------------------------------------------------------------------+
//| Method Init.                                                     |
//+------------------------------------------------------------------+
bool CChartSample::Init(void)
  {
   int   i,j,x;
   int   sx,sy=16;
   color color_label;
//---
   if((m_num_symbols=SymbolsTotal(true))==0)
      return(false);
//---
   if(m_chart.Open(SymbolName(0,true),PERIOD_M1)==0)
     {
      printf("Chart not created");
      return(false);
     }
//--- tuning colors
   color_label=(color)(m_chart.ColorBackground()^0xFFFFFF);
//--- create m_panel[]
   for(i=0;i<NUM_PANELS;i++)
     {
      m_panel[i].Create(m_chart.ChartId(),"Panel"+IntegerToString(i),0,10,sy,150,16);
      m_panel[i].Description(p_str[i]);
      m_panel[i].Color(Black);
      m_panel[i].FontSize(8);
      m_panel[i].State(true);
      sy+=m_panel[i].Y_Size();
     }
   sy=4;
//--- creation m_label[]
   for(i=7;i<NUM_LABELS;i++)
     {
      if((m_label[i]=new CChartObjectLabel)==NULL)
         return(false);
      m_label[i].Create(m_chart.ChartId(),"Label"+IntegerToString(i),0,l_x[i],sy+l_y[i]);
      m_label[i].Description(l_str[i]);
      m_label[i].Color(color_label);
      if(i>=7)
         m_label[i].FontSize(8);
      if(l_pan[i]<NUM_PANELS)
         m_panel[l_pan[i]].Attach(m_label[i]);
     }
//--- creation m_button[]
   for(i=0;i<NUM_BUTTONS;i++)
     {
      if((m_button[i]=new CChartObjectButton)==NULL)
         return(false);
      m_button[i].Create(m_chart.ChartId(),"Button"+IntegerToString(i),0,b_x[i],sy+b_y[i],b_sizeX[i],b_sizeY[i]);
      m_button[i].Description(b_str[i]);
      m_button[i].Color(Black);
      m_button[i].FontSize(8);
      if(b_pan[i]<NUM_PANELS)
         m_panel[b_pan[i]].Attach(m_button[i]);
     }
//--- creation m_edit[]
   for(i=0;i<NUM_EDITS;i++)
     {
      if((m_edit[i]=new CChartObjectEdit)==NULL)
         return(false);
      m_edit[i].Create(m_chart.ChartId(),"Edit"+IntegerToString(i),0,e_x[i],sy+e_y[i],e_sizeX[i],16);
      m_edit[i].FontSize(8);
      if(e_pan[i]<NUM_PANELS)
         m_panel[e_pan[i]].Attach(m_edit[i]);
     }
//--- creation m_edit_color[] and m_edit_rgb[][]
   for(i=0;i<13;i++)
     {
      if((m_edit_color[i]=new CChartObjectEdit)==NULL)
         return(false);
      m_edit_color[i].Create(m_chart.ChartId(),"EditColor"+IntegerToString(i),0,80,20+20*i,16,16);
      m_edit_color[i].FontSize(8);
      m_panel[6].Attach(m_edit_color[i]);
      for(j=0;j<3;j++)
        {
         if((m_edit_rgb[i][j]=new CChartObjectEdit)==NULL)
            return(false);
         m_edit_rgb[i][j].Create(m_chart.ChartId(),"EditRGB"+IntegerToString(i)+"_"+IntegerToString(j),0,100+80*j,20+20*i,50,16);
         m_edit_rgb[i][j].FontSize(8);
         m_panel[6].Attach(m_edit_rgb[i][j]);
        }
     }
//--- creation m_button_tf[]
   for(i=0;i<20;i++)
     {
      x=28*(i%11);
      if(i%11<4)
        {
         sx=26;
         x-=2*(i%11);
        }
      else
        {
         x-=8;
         sx=28;
        }
      if(i>16)
         x+=28;
      if(i>17)
         x+=28;
      if((m_button_tf[i]=new CChartObjectButton)==NULL)
         return(false);
      m_button_tf[i].Create(m_chart.ChartId(),"ButtonTF"+IntegerToString(i),0,20+x,16*(i/11),sx,16);
      if(m_chart.Period()==tf_int[i])
         m_button_tf[i].State(true);
      m_button_tf[i].Description(tf_str[i]);
      m_button_tf[i].Color(Blue);
      m_button_tf[i].FontSize(8);
      m_panel[4].Attach(m_button_tf[i]);
     }
//--- creation m_button_sym[]
   ArrayResize(m_button_sym,m_num_symbols);
   for(i=0;i<m_num_symbols;i++)
     {
      if((m_button_sym[i]=new CChartObjectButton)==NULL)
         return(false);
      m_button_sym[i].Create(m_chart.ChartId(),"ButtonS"+IntegerToString(i),0,20+60*(i%5),16*(i/5),60,16);
      m_button_sym[i].Description(SymbolName(i,true));
      if(m_chart.Symbol()==SymbolName(i,true))
         m_button_sym[i].State(true);
      m_button_sym[i].Color(Green);
      m_button_sym[i].FontSize(8);
      m_panel[5].Attach(m_button_sym[i]);
     }
//--- initial installation of the objects
   m_button[0].State(true);
   m_button[1].State(false);
   m_button[2].State(false);
   m_button[3].State(m_chart.AutoScroll());
   m_button[4].State(m_chart.Shift());
   m_button[5].State(m_chart.Foreground());
   m_button[6].State(false);
   m_button[7].State(false);
   m_button[8].State(m_chart.ScaleFix());
   m_button[9].State(m_chart.ScaleFix_11());
   m_button[10].State(false);
   m_button[11].State(false);
   m_button[12].State(false);
   m_button[13].State(false);
   m_button[14].State(m_chart.ScalePPB());
   m_button[15].State(m_chart.ShowOHLC());
   m_button[16].State(m_chart.ShowLineBid());
   m_button[17].State(m_chart.ShowLineAsk());
   m_button[18].State(m_chart.ShowLastLine());
   m_button[19].State(m_chart.ShowPeriodSep());
   m_button[20].State(m_chart.ShowGrid());
   m_button[21].State(m_chart.ShowObjectDescr());
   m_button[22].State(false);
//--- initial installation of the chart
   if(m_chart.Shift())
      m_edit[0].Description(DoubleToString(m_chart.ShiftSize()));
   else
      m_edit[0].Description("");
   m_edit[1].Description(IntegerToString(m_chart.Scale()));
   m_edit[2].Description(m_edit[0].Description());
   m_edit[3].Description(m_edit[1].Description());
   if(m_chart.ScaleFix())
     {
      m_edit[4].Description(DoubleToString(m_chart.PriceMax(0),4));
      m_edit[5].Description(DoubleToString(m_chart.PriceMin(0),4));
     }
   else
     {
      m_edit[4].Description("");
      m_edit[5].Description("");
     }
   if(m_chart.ScalePPB())
      m_edit[6].Description(DoubleToString(m_chart.PointsPerBar(),4));
   else
      m_edit[6].Description("");
//--- tune m_panel[]
   sy=16;
   for(i=0;i<NUM_PANELS;i++)
     {
      m_panel[i].Y_Distance(sy);
      sy+=m_panel[i].Y_Size();
     }
//--- redraw chart
   m_chart.Redraw();
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Method Deinit.                                                   |
//+------------------------------------------------------------------+
void CChartSample::Deinit(void)
  {
  }
//+------------------------------------------------------------------+
//| Method Processing.                                               |
//+------------------------------------------------------------------+
void CChartSample::Processing(void)
  {
   int i;
   int sy=0;
//---
   for(i=0;i<NUM_PANELS;i++)
      if(m_panel[i].CheckState())
        {
         sy=m_panel[i].Y_Distance()+m_panel[i].Y_Size();
         i++;
         break;
        }
   for(;i<NUM_PANELS;i++)
     {
      m_panel[i].Y_Distance(sy);
      sy+=m_panel[i].Y_Size();
     }
   CheckPanelModes();
   CheckPanelAnothers();
   CheckPanelScales();
   CheckPanelShows();
   CheckPanelTimeframes();
   CheckPanelSymbols();
   CheckPanelColors();
   CheckPanelReadOnly();
//--- chart redrawn (with the processing of events)
   m_chart.Redraw();
   Sleep(50);
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
int OnStart(void)
  {
//--- call init function
   if(ExtScript.Init())
     {
      //--- cycle until the script is not halted
      while(!IsStopped())
         ExtScript.Processing();
     }
//--- call deinit function
   ExtScript.Deinit();
//---
   return(0);
  }
//+------------------------------------------------------------------+
