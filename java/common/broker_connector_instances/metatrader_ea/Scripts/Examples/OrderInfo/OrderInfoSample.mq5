//+------------------------------------------------------------------+
//|                                              OrderInfoSample.mq5 |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "2009-2017, MetaQuotes Software Corp."
#property link      "http://www.mql5.com"
//---
#include <Trade\OrderInfo.mqh>
#include <ChartObjects\ChartObjectsTxtControls.mqh>
//---
#include "OrderInfoSampleInit.mqh"
//+------------------------------------------------------------------+
//| Script to testing the use of class COrderInfo.                   |
//+------------------------------------------------------------------+
//---
//+------------------------------------------------------------------+
//| Order Info Sample script class                                   |
//+------------------------------------------------------------------+
class COrderInfoSample
  {
protected:
   COrderInfo        m_order;
   //--- chart objects
   CChartObjectButton m_button_prev;
   CChartObjectButton m_button_next;
   CChartObjectLabel m_label[20];
   CChartObjectLabel m_label_info[20];
   //---
   int               m_curr_ord;
   int               m_total_ord;

public:
                     COrderInfoSample(void);
                    ~COrderInfoSample(void);
   //---
   bool              Init(void);
   void              Deinit(void);
   void              Processing(void);

private:
   void              InfoToChart(void);
  };
//---
COrderInfoSample ExtScript;
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
COrderInfoSample::COrderInfoSample(void) : m_curr_ord(-1),
                                           m_total_ord(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
COrderInfoSample::~COrderInfoSample(void)
  {
  }
//+------------------------------------------------------------------+
//| Method Init.                                                     |
//+------------------------------------------------------------------+
bool COrderInfoSample::Init(void)
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
//--- creation Buttons
   m_button_prev.Create(0,"ButtonPrev",0,10,sy,100,20);
   m_button_prev.Description("Prev Order");
   m_button_prev.Color(Red);
   m_button_prev.FontSize(8);
//---
   m_button_next.Create(0,"ButtonNext",0,110,sy,100,20);
   m_button_next.Description("Next Order");
   m_button_next.Color(Red);
   m_button_next.FontSize(8);
//---
   sy+=20;
//--- creation Labels[]
   for(i=0;i<20;i++)
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
   InfoToChart();
//--- redraw chart
   ChartRedraw();
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Method Deinit.                                                   |
//+------------------------------------------------------------------+
void COrderInfoSample::Deinit(void)
  {
  }
//+------------------------------------------------------------------+
//| Method Processing.                                               |
//+------------------------------------------------------------------+
void COrderInfoSample::Processing(void)
  {
   ulong ticket;
//---
   if(m_total_ord!=OrdersTotal())
     {
      m_total_ord=OrdersTotal();
      if(m_total_ord==0)
        {
         m_label_info[0].Description("0");
         m_label_info[1].Description("");
         m_curr_ord=-1;
         return;
        }
      else
        {
         m_label_info[0].Description(IntegerToString(m_total_ord));
         if(m_curr_ord==-1)
            m_curr_ord=0;
         if(m_curr_ord>=m_total_ord)
            m_curr_ord=m_total_ord-1;
         m_label_info[1].Description(IntegerToString(m_curr_ord));
        }
     }
   if(m_button_prev.State())
     {
      m_button_prev.State(false);
      if(m_curr_ord>=0)
         m_label_info[1].Description(IntegerToString(--m_curr_ord));
     }
   if(m_button_next.State())
     {
      m_button_next.State(false);
      if(m_curr_ord<m_total_ord-1)
         m_label_info[1].Description(IntegerToString(++m_curr_ord));
     }
   ticket=OrderGetTicket(m_curr_ord);
   if(OrderSelect(ticket))
     {
      m_label_info[2].Description(IntegerToString(ticket));
      InfoToChart();
     }
//--- redraw chart
   ChartRedraw();
   Sleep(250);
  }
//+------------------------------------------------------------------+
//| Method InfoToChart.                                              |
//+------------------------------------------------------------------+
void COrderInfoSample::InfoToChart(void)
  {
   m_label_info[3].Description(m_order.Symbol());
   m_label_info[4].Description(TimeToString(m_order.TimeSetup()));
   m_label_info[5].Description(m_order.TypeDescription());
   m_label_info[6].Description(m_order.StateDescription());
   m_label_info[7].Description(TimeToString(m_order.TimeExpiration()));
   m_label_info[8].Description(TimeToString(m_order.TimeDone()));
   m_label_info[9].Description(m_order.TypeFillingDescription());
   m_label_info[10].Description(m_order.TypeTimeDescription());
   m_label_info[11].Description(IntegerToString(m_order.Magic()));
   m_label_info[12].Description(DoubleToString(m_order.VolumeInitial()));
   m_label_info[13].Description(DoubleToString(m_order.VolumeCurrent()));
   m_label_info[14].Description(DoubleToString(m_order.PriceOpen()));
   m_label_info[15].Description(DoubleToString(m_order.StopLoss()));
   m_label_info[16].Description(DoubleToString(m_order.TakeProfit()));
   m_label_info[17].Description(DoubleToString(m_order.PriceCurrent()));
   m_label_info[18].Description(DoubleToString(m_order.PriceStopLimit()));
   m_label_info[19].Description(m_order.Comment());
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
