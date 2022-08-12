//+------------------------------------------------------------------+
//|                                              TestAccountInfo.mq5 |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "2009-2017, MetaQuotes Software Corp."
#property link      "http://www.mql5.com"
//---
#include <Trade\AccountInfo.mqh>
#include <ChartObjects\ChartObjectsTxtControls.mqh>
//---
#include "AccountInfoSampleInit.mqh"
//+------------------------------------------------------------------+
//| Script to testing the use of class CAccountInfo.                 |
//+------------------------------------------------------------------+
//---
//+------------------------------------------------------------------+
//| Account Info Sample script class                                 |
//+------------------------------------------------------------------+
class CAccountInfoSample
  {
protected:
   CAccountInfo      m_account;
   //--- chart objects
   CChartObjectLabel m_label[19];
   CChartObjectLabel m_label_info[19];

public:
                     CAccountInfoSample(void);
                    ~CAccountInfoSample(void);
   //---
   bool              Init(void);
   void              Deinit(void);
   void              Processing(void);

private:
   void              AccountInfoToChart(void);
  };
//---
CAccountInfoSample ExtScript;
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CAccountInfoSample::CAccountInfoSample(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CAccountInfoSample::~CAccountInfoSample(void)
  {
  }
//+------------------------------------------------------------------+
//| Method Init.                                                     |
//+------------------------------------------------------------------+
bool CAccountInfoSample::Init(void)
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
//--- creation Labels[]
   for(i=0;i<19;i++)
     {
      m_label[i].Create(0,"Label"+IntegerToString(i),0,20,sy+dy*i);
      m_label[i].Description(init_str[i]);
      m_label[i].Color(color_label);
      m_label[i].FontSize(8);
      //---
      m_label_info[i].Create(0,"LabelInfo"+IntegerToString(i),0,120,sy+dy*i);
      m_label_info[i].Description(" ");
      m_label_info[i].Color(color_info);
      m_label_info[i].FontSize(8);
     }
   AccountInfoToChart();
//--- redraw chart
   ChartRedraw();
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Method Deinit.                                                   |
//+------------------------------------------------------------------+
void CAccountInfoSample::Deinit(void)
  {
  }
//+------------------------------------------------------------------+
//| Method Processing.                                               |
//+------------------------------------------------------------------+
void CAccountInfoSample::Processing(void)
  {
   AccountInfoToChart();
//--- redraw chart
   ChartRedraw();
   Sleep(50);
  }
//+------------------------------------------------------------------+
//| Method InfoToChart.                                              |
//+------------------------------------------------------------------+
void CAccountInfoSample::AccountInfoToChart(void)
  {
   m_label_info[0].Description((string)m_account.Login());
   m_label_info[1].Description(m_account.TradeModeDescription());
   m_label_info[2].Description((string)m_account.Leverage());
   m_label_info[3].Description(m_account.MarginModeDescription());
   m_label_info[4].Description((string)m_account.TradeAllowed());
   m_label_info[5].Description((string)m_account.TradeExpert());
   m_label_info[6].Description(DoubleToString(m_account.Balance(),2));
   m_label_info[7].Description(DoubleToString(m_account.Credit(),2));
   m_label_info[8].Description(DoubleToString(m_account.Profit(),2));
   m_label_info[9].Description(DoubleToString(m_account.Equity(),2));
   m_label_info[10].Description(DoubleToString(m_account.Margin(),2));
   m_label_info[11].Description(DoubleToString(m_account.FreeMargin(),2));
   m_label_info[12].Description(DoubleToString(m_account.MarginLevel(),2));
   m_label_info[13].Description(DoubleToString(m_account.MarginCall(),2));
   m_label_info[14].Description(DoubleToString(m_account.MarginStopOut(),2));
   m_label_info[15].Description(m_account.Name());
   m_label_info[16].Description(m_account.Server());
   m_label_info[17].Description(m_account.Currency());
   m_label_info[18].Description(m_account.Company());
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart(void)
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
  }
//+------------------------------------------------------------------+
