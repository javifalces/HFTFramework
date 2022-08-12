//+------------------------------------------------------------------+
//|                                             SymbolInfoSample.mq5 |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "2009-2017, MetaQuotes Software Corp."
#property link      "http://www.mql5.com"
//---
#property script_show_inputs
//---
input bool InpMarketWatch=true;
//---
#include <Trade\SymbolInfo.mqh>
#include <ChartObjects\ChartObjectsTxtControls.mqh>
//---
#include "SymbolInfoSampleInit.mqh"
//+------------------------------------------------------------------+
//| Script to sample the use of class CSymbolInfo.                   |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Symbol Info Sample script class                                  |
//+------------------------------------------------------------------+
class CSymbolInfoSample
  {
protected:
   CSymbolInfo       m_symbol;
   //--- chart objects
   CChartObjectButton m_buttons[];
   int               m_num_symbols;
   CChartObjectLabel m_label[40];
   CChartObjectLabel m_label_info[40];
   //---
   int               m_symbol_idx;

public:
                     CSymbolInfoSample(void);
                    ~CSymbolInfoSample(void);
   //---
   bool              Init(void);
   void              Deinit(void);
   void              Processing(void);

private:
   void              InfoToChart(void);
  };
//---
CSymbolInfoSample ExtScript;
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSymbolInfoSample::CSymbolInfoSample(void) : m_symbol_idx(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSymbolInfoSample::~CSymbolInfoSample(void)
  {
  }
//+------------------------------------------------------------------+
//| Method Init.                                                     |
//+------------------------------------------------------------------+
bool CSymbolInfoSample::Init(void)
  {
   int   i,sy=10;
   int   dy=16;
   color color_label;
   color color_info;
//--- tuning colors
   color_info =(color)(ChartGetInteger(0,CHART_COLOR_BACKGROUND)^0xFFFFFF);
   color_label=(color)(color_info^0x202020);
//---
   if(ChartGetInteger(0,CHART_SHOW_OHLC))
      sy+=16;
//---
   m_num_symbols=SymbolsTotal(InpMarketWatch);
   if(!m_num_symbols)
      return(false);
   ArrayResize(m_buttons,m_num_symbols);
//--- creation Button[]
   for(i=0;i<m_num_symbols;i++)
     {
      m_buttons[i].Create(0,"Button"+IntegerToString(i),0,10+50*(i%10),sy+20*(i/10),50,20);
      m_buttons[i].Description(SymbolName(i,InpMarketWatch));
      m_buttons[i].Color(Red);
      m_buttons[i].FontSize(8);
     }
   m_symbol_idx=0;
   m_buttons[0].State(true);
   m_symbol.Name(m_buttons[0].Description());
   sy+=20*(1+i/10);
//--- creation Labels[]
   for(i=0;i<40;i++)
     {
      m_label[i].Create(0,"Label"+IntegerToString(i),0,init_l_x[i],sy+dy*init_l_y[i]);
      m_label[i].Description(init_l_str[i]);
      m_label[i].Color(color_label);
      m_label[i].FontSize(8);
      //---
      m_label_info[i].Create(0,"LabelInfo"+IntegerToString(i),0,init_li_x[i],sy+dy*init_l_y[i]);
      m_label_info[i].Description(" ");
      m_label_info[i].Color(color_info);
      m_label_info[i].FontSize(8);
     }
   InfoToChart();
//--- redraw chart
   ChartRedraw();
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Method Deinit.                                                   |
//+------------------------------------------------------------------+
void CSymbolInfoSample::Deinit(void)
  {
  }
//+------------------------------------------------------------------+
//| Method Processing.                                               |
//+------------------------------------------------------------------+
void CSymbolInfoSample::Processing(void)
  {
   int i;
//---
   if(!m_buttons[m_symbol_idx].State())
      m_buttons[m_symbol_idx].State(true);
   for(i=0;i<m_num_symbols;i++)
     {
      if(m_buttons[i].State() && m_symbol_idx!=i)
        {
         m_buttons[m_symbol_idx].State(false);
         m_symbol_idx=i;
         m_symbol.Name(m_buttons[i].Description());
        }
     }
   m_symbol.RefreshRates();
   InfoToChart();
//--- redraw chart (with the processing of events)
   ChartRedraw();
   Sleep(50);
  }
//+------------------------------------------------------------------+
//| Method InfoToChart.                                              |
//+------------------------------------------------------------------+
void CSymbolInfoSample::InfoToChart(void)
  {
   int digits=m_symbol.Digits();
//--- display volumes
   m_label_info[0].Description((string)m_symbol.Volume());
   m_label_info[1].Description((string)m_symbol.VolumeHigh());
   m_label_info[2].Description((string)m_symbol.VolumeLow());
//--- display miscellaneous
   m_label_info[5].Description(TimeToString(m_symbol.Time()));
   m_label_info[6].Description((string)m_symbol.Digits());
   m_label_info[7].Description((string)m_symbol.Spread());
   m_label_info[8].Description((string)m_symbol.TicksBookDepth());
//--- display terms of trade
   m_label_info[9].Description(m_symbol.TradeCalcModeDescription());
   m_label_info[10].Description(m_symbol.TradeModeDescription());
//--- display trade levels
   m_label_info[11].Description((string)m_symbol.StopsLevel());
   m_label_info[12].Description((string)m_symbol.FreezeLevel());
//--- display execution terms of trade
   m_label_info[13].Description(m_symbol.TradeExecutionDescription());
//--- display swap terms of trade
   m_label_info[14].Description(m_symbol.SwapModeDescription());
   m_label_info[15].Description(m_symbol.SwapRollover3daysDescription());
//--- display bid
   m_label_info[16].Description(DoubleToString(m_symbol.Bid(),digits));
   m_label_info[17].Description(DoubleToString(m_symbol.BidHigh(),digits));
   m_label_info[18].Description(DoubleToString(m_symbol.BidLow(),digits));
//--- display ask
   m_label_info[19].Description(DoubleToString(m_symbol.Ask(),digits));
   m_label_info[20].Description(DoubleToString(m_symbol.AskHigh(),digits));
   m_label_info[21].Description(DoubleToString(m_symbol.AskLow(),digits));
//--- display last
   m_label_info[22].Description(DoubleToString(m_symbol.Last(),digits));
   m_label_info[23].Description(DoubleToString(m_symbol.LastHigh(),digits));
   m_label_info[24].Description(DoubleToString(m_symbol.LastLow(),digits));
//--- display tick
   m_label_info[25].Description(DoubleToString(m_symbol.Point(),digits));
   m_label_info[26].Description(DoubleToString(m_symbol.TickValue()));
   m_label_info[27].Description(DoubleToString(m_symbol.TickSize()));
   m_label_info[28].Description(DoubleToString(m_symbol.ContractSize()));
//--- display lots
   m_label_info[29].Description(DoubleToString(m_symbol.LotsMin(),2));
   m_label_info[30].Description(DoubleToString(m_symbol.LotsMax(),2));
   m_label_info[31].Description(DoubleToString(m_symbol.LotsStep(),2));
//--- display swaps
   m_label_info[32].Description(DoubleToString(m_symbol.SwapLong(),2));
   m_label_info[33].Description(DoubleToString(m_symbol.SwapShort(),2));
//--- display currency
   m_label_info[34].Description(m_symbol.CurrencyBase());
   m_label_info[35].Description(m_symbol.CurrencyProfit());
   m_label_info[36].Description(m_symbol.CurrencyMargin());
//--- display another
   m_label_info[37].Description(m_symbol.Bank());
   m_label_info[38].Description(m_symbol.Description());
   m_label_info[39].Description(m_symbol.Path());
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
