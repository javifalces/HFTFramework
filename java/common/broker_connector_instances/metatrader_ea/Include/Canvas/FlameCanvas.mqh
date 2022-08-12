//+------------------------------------------------------------------+
//|                                                  FlameCanvas.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Canvas.mqh"
#include <Controls\Defines.mqh>
//+------------------------------------------------------------------+
//| Gradient descriptors                                             |
//+------------------------------------------------------------------+
struct GRADIENT_COLOR
  {
   uint              clr;      // color in ARGB format
   uint              pos;      // position of color in percentage of gradient range
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
struct GRADIENT_SIZE
  {
   uint              size;     // width of gradient fill in percentage of base fill
   uint              pos;      // position of color in percentage of gradient length
  };
//+------------------------------------------------------------------+
//| Class CFlameCanvas                                               |
//| Usage: generates flame                                           |
//+------------------------------------------------------------------+
class CFlameCanvas : public CCanvas
  {
private:
   //--- parameters
   uint              m_bar_gap;
   uint              m_bar_width;
   uint              m_chart_scale;
   double            m_chart_price_min;
   double            m_chart_price_max;
   ENUM_TIMEFRAMES   m_timeframe;
   string            m_symbol;
   int               m_future_bars;
   int               m_back_bars;
   int               m_rates_total;
   uint              m_palette[256];            // flame palette
   uchar             m_flame[];                 // buffer for calculation of flame
   uint              m_time_redraw;
   uint              m_delay;
   //   bool              m_resize_flag;
   //   int               m_tick_cnt;
   //--- flame parameters
   datetime          m_tb1;
   double            m_pb1;
   datetime          m_te1;
   double            m_pe1;
   datetime          m_tb2;
   double            m_pb2;
   datetime          m_te2;
   double            m_pe2;
   //--- equation parameters for flame
   int               m_cloud_axis[100];
   double            m_a1;
   double            m_b1;
   double            m_a2;
   double            m_b2;
   int               m_xb1;
   int               m_yb1;
   int               m_xe1;
   int               m_ye1;
   int               m_xb2;
   int               m_yb2;
   int               m_xe2;
   int               m_ye2;

public:
                     CFlameCanvas(void);
                    ~CFlameCanvas(void);
   //--- create
   bool              FlameCreate(const string name,const datetime time,const int future_bars,const int back_bars=0);
   void              RatesTotal(const int value);
   //--- setting
   void              PaletteSet(uint clr=0xFF0000);
   //--- draw
   void              FlameDraw(const double &prices[],const int width,const int lenght);
   void              FlameSet(datetime xb1,double yb1,datetime xe1,double ye1,datetime xb2,double yb2,datetime xe2,double ye2);
   //--- event handler
   void              ChartEventHandler(const int id,const long &lparam,const double &dparam,const string &sparam);

protected:
   bool              Resize(void);
   void              ChartScale(void);
   void              FlameSet(void);
   void              CloudDraw(const double &prices[],const int width,const int lenght,GRADIENT_SIZE &size[],GRADIENT_COLOR &gradient[],const uchar t_level=255,const bool custom_gradient=true);
   void              FlameDraw(const int width,const int lenght,GRADIENT_SIZE &size[],GRADIENT_COLOR &gradient[]);
   void              GradientVertical(const int xb,const int xe,const int yb1,const int ye1,const int yb2,const int ye2,const GRADIENT_COLOR &gradient[]);
   void              GradientVerticalLine(int x,int y1,int y2,const GRADIENT_COLOR &gradient[]);
   void              GradientVerticalLineMonochrome(int x,int y1,int y2,uint clr1,uint clr2);
   void              FlameCreate(void);
   void              FlameCalculate(void);
   void              Delay(const uint value);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CFlameCanvas::CFlameCanvas(void) : m_bar_gap(16),
                                   m_bar_width(8),
                                   m_chart_scale(1),
                                   m_chart_price_min(0.0),
                                   m_chart_price_max(0.0),
                                   m_timeframe(PERIOD_CURRENT),
                                   m_symbol(NULL),
                                   m_future_bars(0),
                                   m_back_bars(0),
                                   m_rates_total(0),
                                   m_time_redraw(0),
                                   m_delay(50),
                                   m_tb1(0),
                                   m_pb1(0),
                                   m_te1(0),
                                   m_pe1(0),
                                   m_tb2(0),
                                   m_pb2(0),
                                   m_te2(0),
                                   m_pe2(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CFlameCanvas::~CFlameCanvas(void)
  {
   Destroy();
  }
//+------------------------------------------------------------------+
//| Creates dynamic resource with object                             |
//+------------------------------------------------------------------+
bool CFlameCanvas::FlameCreate(const string name,const datetime time,const int future_bars,const int back_bars)
  {
//--- get chart parameters
   ChartScale();
//--- create
   int width =(int)m_bar_gap*(future_bars+back_bars);
   int height=(int)ChartGetInteger(0,CHART_HEIGHT_IN_PIXELS);
   if(!CreateBitmap(0,0,name,time-back_bars*PeriodSeconds(),m_chart_price_max,width,height,COLOR_FORMAT_ARGB_NORMALIZE))
      return(false);
   ArrayResize(m_flame,width*height);
//--- save parameters
   m_future_bars=future_bars;
   m_back_bars  =back_bars;
//--- settings
   PaletteSet();
   m_timeframe  =Period();
   m_symbol     =Symbol();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Resize                                                           |
//+------------------------------------------------------------------+
bool CFlameCanvas::Resize(void)
  {
   int x,y;
//--- get limits
   double min=ChartGetDouble(0,CHART_PRICE_MIN);
   double max=ChartGetDouble(0,CHART_PRICE_MAX);
   if(m_chart_price_max!=max)
     {
      //--- move object
      ObjectSetDouble(0,m_objname,OBJPROP_PRICE,0,max);
     }
//--- check
   if(m_chart_price_min==min && m_chart_price_max==max)
      return(false);
   m_chart_price_min=min;
   m_chart_price_max=max;
//--- grt size
   ChartTimePriceToXY(0,0,m_tb1,min,x,y);
   int width =(int)ChartGetInteger(0,CHART_WIDTH_IN_PIXELS)-x;
   int height=(int)ChartGetInteger(0,CHART_HEIGHT_IN_PIXELS);
//--- resize
   if(width<m_width)
      width=m_width;
   if(width<=0)
      return(false);
   CCanvas::Resize(width,height);
//--- resize flame buffer
   ArrayResize(m_flame,width*height);
   ArrayInitialize(m_flame,0);
   ArrayInitialize(m_pixels,0);
//--- restore parameters
   if(m_pb1!=0.0)
      FlameSet(m_tb1,m_pb1,m_te1,m_pe1,m_tb2,m_pb2,m_te2,m_pe2);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void CFlameCanvas::RatesTotal(const int value)
  {
   if(value==0)
      return;
   if(m_rates_total==0)
      m_rates_total=value;
   else
     {
      if(m_rates_total!=value)
        {
         //--- move object
         ObjectSetInteger(0,m_objname,OBJPROP_TIME,0,
                          ObjectGetInteger(0,m_objname,OBJPROP_TIME)+(value-m_rates_total)*PeriodSeconds());
         m_rates_total=value;
        }
     }
  }
//+------------------------------------------------------------------+
//| Adjusts to the chart scale                                       |
//+------------------------------------------------------------------+
void CFlameCanvas::ChartScale(void)
  {
   m_chart_scale=(uint)ChartGetInteger(0,CHART_SCALE);
//--- set params
   switch(m_chart_scale)
     {
      case 0:
         m_bar_gap  =1;
         m_bar_width=1;
         break;
      case 1:
         m_bar_gap  =2;
         m_bar_width=1;
         break;
      case 2:
         m_bar_gap  =4;
         m_bar_width=2;
         break;
      case 3:
         m_bar_gap  =8;
         m_bar_width=4;
         break;
      case 4:
         m_bar_gap  =16;
         m_bar_width=10;
         break;
      case 5:
         m_bar_gap  =32;
         m_bar_width=22;
         break;
      default:
         return;
     }
  }
//+------------------------------------------------------------------+
//| Sets palette                                                     |
//+------------------------------------------------------------------+
void CFlameCanvas::PaletteSet(uint clr)
  {
//--- create palette
   double g=0,b=0,dg=1.45,db=0.63;
//---
   for(uint a,i=0;i<256;i++)
     {
      //--- the first 32 values ??of flame are completely transparent
      a=uchar(i<32?0:i-32);
      //--- generate color for the i value of flame
      m_palette[i]=(a<<24)|(uint(255)<<16)|(uint(g+0.5)<<8)|uint(b+0.5);
      //--- increment the color components
      //--- the red color gets gradient due to transparency
      if(i>80)  g+=dg;
      if(i>160) b+=db;
     }
  }
//+------------------------------------------------------------------+
//| Draws the flame                                                  |
//+------------------------------------------------------------------+
void CFlameCanvas::FlameDraw(const double &prices[],const int width,const int lenght)
  {
   static GRADIENT_SIZE  sword[]={{100,0},{150,70},{0,100}};
   static GRADIENT_COLOR flame[]={{0x00,0},{0x7F7F7F,12},{0xCCCCCC,30},{0xFFFFFF,45},{0xFFFFFF,55},{0xCCCCCC,70},{0x7F7F7F,88},{0x00,100}};
//--- draw
   CloudDraw(prices,width,lenght,sword,flame);
//--- copy flame buffer
   FlameCalculate();
//--- start timer
   EventChartCustom(CONTROLS_SELF_MESSAGE,1302,0,0,NULL);
  }
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
void CFlameCanvas::FlameDraw(const int width,const int lenght,GRADIENT_SIZE &size[],GRADIENT_COLOR &gradient[])
  {
//--- check
   int total=ArraySize(m_cloud_axis);
   if(total<2)
      return;
   if(total>lenght)
      total=lenght;
//--- draw
   int xb,xe;     // coordinates of the segment
   int ybm,yem;   // coordinates of the center line
   int yb1,ye1;   // coordinates of the first line
   int yb2,ye2;   // coordinates of the second line
//--- for implementation of variable width
   int w_total=ArraySize(size);
   if(w_total<2)
      return;
   int    w_i =0;
   int    w_is=(int)size[w_i].pos*total/100;
   int    w_ie=(int)size[w_i+1].pos*total/100;
   double w   =size[w_i].size*width/100;
   double dw  =(size[w_i+1].size*width/100-w)/(w_ie-w_is);
//--- draw from left to right
   xb=0;
   ybm=m_cloud_axis[0];
   yb1=ybm-(int)(w/2);
   yb2=ybm+(int)(w/2);
//--- draw
   for(int i=1;i<total;i++)
     {
      xe=(int)(i*m_bar_gap);
      if(m_cloud_axis[i]==DBL_MAX)
         continue;
      yem=m_cloud_axis[i];
      w+=dw;
      ye1=yem-(int)(w/2);
      ye2=yem+(int)(w/2);
      //--- draw the segment of 'cloud'
      GradientVertical(xb,xe,yb1,ye1,yb2,ye2,gradient);
      xb=xe;
      if(xb>=m_width)
         break;
      yb1=ye1;
      yb2=ye2;
      while(i>=w_ie-1 && i!=total-1)
        {
         w_i++;
         w_is=(int)size[w_i].pos*total/100;
         w_ie=(int)size[w_i+1].pos*total/100;
         w   =size[w_i].size*width/100;
         if(w_ie==w_is)
           {
            //--- for "instant" resize
            dw=size[w_i+1].size*width/100-w;
            w+=dw;
            ye1=yem-(int)(w/2);
            ye2=yem+(int)(w/2);
            //--- draw the segment of 'cloud'
            GradientVertical(xb,xe,yb1,ye1,yb2,ye2,gradient);
            yb1=ye1;
            yb2=ye2;
           }
         else
           {
            dw=(size[w_i+1].size*width/100-w)/(w_ie-w_is);
            break;
           }
        }
     }
//--- copy flame buffer
   FlameCalculate();
  }
//+------------------------------------------------------------------+
//| Sets parameters of the flame and starts to draw                  |
//+------------------------------------------------------------------+
void CFlameCanvas::FlameSet(void)
  {
   m_a1=m_bar_gap*((m_ye1-m_yb1)/((double)m_xe1-m_xb1));
   m_a2=m_bar_gap*((m_ye2-m_yb2)/((double)m_xe2-m_xb2));
  }
//+------------------------------------------------------------------+
//| Sets parameters of the flame and starts to draw                  |
//+------------------------------------------------------------------+
void CFlameCanvas::FlameSet(datetime tb1,double pb1,
                            datetime te1,double pe1,
                            datetime tb2,double pb2,
                            datetime te2,double pe2)
  {
   datetime obj_time =(datetime)ObjectGetInteger(0,m_objname,OBJPROP_TIME);
   double   obj_price=ObjectGetDouble(0,m_objname,OBJPROP_PRICE);
   int      dx,dy;
//--- save parameters
   m_tb1=tb1;
   m_pb1=pb1;
   m_te1=te1;
   m_pe1=pe1;
   m_tb2=tb2;
   m_pb2=pb2;
   m_te2=te2;
   m_pe2=pe2;
//--- resize
   Resize();
//--- convert
   if(ChartTimePriceToXY(0,0,obj_time,obj_price,dx,dy))
     {
      dy=m_yb1;
      if(ChartTimePriceToXY(0,0,tb1,pb1,m_xb1,m_yb1))
         if(ChartTimePriceToXY(0,0,te1,pe1,m_xe1,m_ye1))
            if(ChartTimePriceToXY(0,0,tb2,pb2,m_xb2,m_yb2))
               if(ChartTimePriceToXY(0,0,te2,pe2,m_xe2,m_ye2))
                 {
                  //--- convert to canvas coordinates
                  m_xb1-=dx;
                  m_xe1-=dx;
                  m_xb2-=dx;
                  m_xe2-=dx;
                  //--- 
                  FlameSet();
                 }
     }
//--- start timer
   EventChartCustom(CONTROLS_SELF_MESSAGE,1302,0,0,NULL);
  }
//+------------------------------------------------------------------+
//| Generate array that describes the body of flame                  |
//+------------------------------------------------------------------+
void CFlameCanvas::FlameCreate(void)
  {
   static GRADIENT_SIZE  sword[]={{100,0},{150,70},{0,100}};
   static GRADIENT_COLOR flame[]={{0x00,0},{0x7F7F7F,12},{0xCCCCCC,30},{0xFFFFFF,45},{0xFFFFFF,55},{0xCCCCCC,70},{0x7F7F7F,88},{0x00,100}};
//--- 
   double a=rand();   // parameter of line a*x+b
   double b=rand();   // parameter of line a*x+b
   double c=rand();   // parameter of sine c*Sin(d*x)
   double d=rand();   // parameter of sine c*Sin(d*x)
   int    w=rand();   // width at the base
   int    l=rand();   // length
//--- normalize
   a=fmod(a,(m_a2-m_a1))+m_a1;
   b=(m_yb1+m_yb2)/2;
   c=fmod(c,20);
   d=fmod(d,3*M_PI)+M_PI;
//--- shape
   w%=150;
   if(w<10)
      w=10;                                    // but no less than 10
   sword[1].size=w;
   w=rand();
   l%=50;
   sword[1].pos=l+30;
   l=rand();
//--- sizes
   w=(m_yb2-m_yb1!=0) ? w%(m_yb2-m_yb1) : 10;  // proportional to the starting width
   if(w<10)
      w=10;                                    // but no less than 10
   l=l%((m_xe1-m_xb1)/(int)m_bar_gap-20)+20;   // proportional to length
//--- create
   int total=ArraySize(m_cloud_axis);
   for(int i=0;i<total;i++)
      m_cloud_axis[i]=int(a*i+b+c*sin(i/d));
//--- draw
   FlameDraw(w,l,sword,flame);
  }
//+------------------------------------------------------------------+
//| Calculates and renders frame                                     |
//+------------------------------------------------------------------+
void CFlameCanvas::FlameCalculate(void)
  {
//--- calculate new frame
   int c;
   int idx;
//--- draw body of flame to the right
   for(int x=0,x_tot=m_width-1;x<x_tot;x++)
     {
      //--- separately for y==0
      c=m_flame[x]+m_flame[x+m_width];
      c+=+m_flame[x]+m_flame[x+m_width];
      m_flame[x]=uchar(c/4);
      //---
      for(int y=1,y_tot=m_height-1;y<y_tot;y++)
        {
         idx=y*m_width+x;
         c=m_flame[idx-m_width]+m_flame[idx]+m_flame[idx+m_width];
         idx++;
         c+=m_flame[idx-m_width]+m_flame[idx]+m_flame[idx+m_width];
         m_flame[idx]=uchar(c/6);
        }
      //--- separately for y==m_height-1
      idx=(m_height-1)*m_width+x;
      c=m_flame[idx-m_width]+m_flame[idx];
      idx++;
      c+=m_flame[idx-m_width]+m_flame[idx];
      m_flame[idx]=uchar(c/4);
     }
//--- move flame to the resource buffer
   for(int y=0;y<m_height;y++)
     {
      for(int x=0;x<m_width;x++)
        {
         idx=y*m_width+x;
         m_pixels[idx]=m_palette[m_flame[idx]];
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Draws "cloud"                                                    |
//+------------------------------------------------------------------+
void CFlameCanvas::CloudDraw(const double &prices[],const int width,const int lenght,GRADIENT_SIZE &size[],GRADIENT_COLOR &gradient[],const uchar t_level,const bool custom_gradient)
  {
//--- check
   int total=ArraySize(prices);
   if(total<2)
      return;
   if(total>lenght)
      total=lenght;
//--- draw
   int xb,xe;     // coordinates of the segment
   int ybm,yem;   // coordinates of the center line
   int yb1,ye1;   // coordinates of the first line
   int yb2,ye2;   // coordinates of the second line
   int xx;
//--- for implementation of variable width
   int w_total=ArraySize(size);
   if(w_total<2)
      return;
   int    w_i =0;
   int    w_is=(int)size[w_i].pos*total/100;
   int    w_ie=(int)size[w_i+1].pos*total/100;
   double w   =size[w_i].size*width/100;
   double dw  =(size[w_i+1].size*width/100-w)/(w_ie-w_is);
//--- draw from left to right
   xb=0;
   ChartTimePriceToXY(0,0,0,prices[0],xx,ybm);
   yb1=ybm-(int)(w/2);
   yb2=ybm+(int)(w/2);
//--- draw
   for(int i=1;i<total;i++)
     {
      xe=(int)(i*m_bar_gap);
      if(prices[i]==DBL_MAX)
         continue;
      ChartTimePriceToXY(0,0,0,prices[i],xx,yem);
      w+=dw;
      ye1=yem-(int)(w/2);
      ye2=yem+(int)(w/2);
      //--- draw the segment of 'cloud'
      GradientVertical(xb,xe,yb1,ye1,yb2,ye2,gradient);
      xb=xe;
      if(xb>=m_width)
         break;
      yb1=ye1;
      yb2=ye2;
      while(i>=w_ie-1 && i!=total-1)
        {
         w_i++;
         w_is=(int)size[w_i].pos*total/100;
         w_ie=(int)size[w_i+1].pos*total/100;
         w   =size[w_i].size*width/100;
         if(w_ie==w_is)
           {
            //--- for "instant" resize
            dw=size[w_i+1].size*width/100-w;
            w+=dw;
            ye1=yem-(int)(w/2);
            ye2=yem+(int)(w/2);
            //--- draw the segment of 'cloud'
            GradientVertical(xb,xe,yb1,ye1,yb2,ye2,gradient);
            yb1=ye1;
            yb2=ye2;
           }
         else
           {
            dw=(size[w_i+1].size*width/100-w)/(w_ie-w_is);
            break;
           }
        }
     }
  }
//+------------------------------------------------------------------+
//| Draws area with vertical fill using specified gradient           |
//+------------------------------------------------------------------+
void CFlameCanvas::GradientVertical(const int xb,const int xe,const int yb1,const int ye1,const int yb2,const int ye2,const GRADIENT_COLOR &gradient[])
  {
//--- it is assumed that the colors array has sufficient size and positions are already sorted in ascending order
//--- get length by X and Y
   int x1 =xb;
   int y1 =yb1;
   int x2 =xb;
   int y2 =yb2;
   int dx =(xe>xb)? xe-xb : xb-xe;
   int dy1=(ye1>yb1)? ye1-yb1 : yb1-ye1;
   int dy2=(ye2>yb2)? ye2-yb2 : yb2-ye2;
//--- get direction by X and Y
   int sx =(xb<xe)? 1 : -1;
   int sy1=(yb1<ye1)? 1 : -1;
   int sy2=(yb2<ye2)? 1 : -1;
   int er1=dx-dy1;
   int er2=dx-dy2;
//--- extreme colors
   uint clr_first=gradient[0].clr;
   uint clr_last =gradient[ArraySize(gradient)-1].clr;
//--- draw the first line
   while(x1!=xe || y1!=ye1)
     {
      //--- calculate coordinates of next pixel of the first line
      if((er1<<1)>-dy1)
        {
         //--- try to change X coordinate of the first line
         //--- draw the second line
         while(x2!=xe || y2!=ye2)
           {
            //--- calculate coordinates of next pixel of the second line
            if((er2<<1)>-dy2)
              {
               //--- try to change X coordinate of the second line
               //--- gradient fill
               GradientVerticalLine(x1,y1,y2,gradient);
               er2-=dy2;
               if(x2!=xe)
                  x2+=sx;
              }
            if((er2<<1)<dx)
              {
               er2+=dx;
               if(y2!=ye2)
                  y2+=sy2;
              }
            //--- draw the first line
            if(x1!=x2)
               break;
           }
         er1-=dy1;
         if(x1!=xe)
            x1+=sx;
        }
      if((er1<<1)<dx)
        {
         er1+=dx;
         if(y1!=ye1)
            y1+=sy1;
        }
     }
//--- gradient fill
   GradientVerticalLine(x1,ye1,ye2,gradient);
  }
//+------------------------------------------------------------------+
//| Draws gradient vertical line                                     |
//+------------------------------------------------------------------+
void CFlameCanvas::GradientVerticalLineMonochrome(int x,int y1,int y2,uint clr1,uint clr2)
  {
//---
   double dc;
   int    dd,dy=y2-y1;
//--- check
   if(dy==0)
      return;
//--- extract components from the first color
   uchar clr=(uchar)clr1;
//--- parameters of pixels iteration
   if(dy>0)
     {
      dd=dy;
      dy=1;
     }
   else
     {
      dd=-dy;
      dy=-1;
     }
//--- increments for the color components
   dc=(double)((uchar)clr2-clr)/dd;
//--- draw
   for(int i=0;y1!=y2;i++,y1+=dy)
     {
      int idx=y1*m_width+x;
      //--- check range
      if(idx<0 || idx>=ArraySize(m_flame))
         continue;
      if(x>=0 && x<m_width && y1>=0 && y1<m_height)
         if(m_flame[idx]<(uchar)(clr+dc*i))
            m_flame[idx]=(uchar)(clr+dc*i);
     }
  }
//+------------------------------------------------------------------+
//| Draws vertical line with specified gradient                      |
//+------------------------------------------------------------------+
void CFlameCanvas::GradientVerticalLine(int x,int y1,int y2,const GRADIENT_COLOR &gradient[])
  {
//--- it is assumed that the colors array has sufficient size and positions are already sorted in ascending order
   int total=ArraySize(gradient);
   int dy=y2-y1;
//--- draw segments
   for(int i=0;i<total-1;i++)
      GradientVerticalLineMonochrome(x,y1+dy*gradient[i].pos/100,y1+dy*gradient[i+1].pos/100,gradient[i].clr,gradient[i+1].clr);
  }
//+------------------------------------------------------------------+
//| Event handler                                                    |
//+------------------------------------------------------------------+
void CFlameCanvas::ChartEventHandler(const int id,const long &lparam,const double &dparam,const string &sparam)
  {
//--- events filter
   switch(id)
     {
      case CHARTEVENT_CHART_CHANGE:
         //--- handle only chart modification events
         if(m_chart_scale!=(uint)ChartGetInteger(0,CHART_SCALE))
           {
            Delay(20);
            //--- changed horizontal scale
            ChartScale();
            if(m_pb1!=0.0)
               FlameSet(m_tb1,m_pb1,m_te1,m_pe1,m_tb2,m_pb2,m_te2,m_pe2);
            return;
           }
         if(m_height!=(int)ChartGetInteger(0,CHART_HEIGHT_IN_PIXELS))
           {
            //--- changed vertical size
            Delay(20);
            if(m_pb1!=0.0)
               FlameSet(m_tb1,m_pb1,m_te1,m_pe1,m_tb2,m_pb2,m_te2,m_pe2);
            return;
           }
         if(m_chart_price_min!=ChartGetDouble(0,CHART_PRICE_MIN) ||
            m_chart_price_max!=ChartGetDouble(0,CHART_PRICE_MAX))
           {
            //--- changed vertical scale
            Delay(20);
            if(m_pb1!=0.0)
               FlameSet(m_tb1,m_pb1,m_te1,m_pe1,m_tb2,m_pb2,m_te2,m_pe2);
            return;
           }
         break;
         //--- organize custom timer
      case CHARTEVENT_CUSTOM+1302:
         //--- time to draw the new frame?
         if(GetTickCount()>m_time_redraw)
           {
            //--- add the body of flame
            FlameCreate();
            //--- draw frame
            FlameCalculate();
            Update();
            //--- calculate time for the next frame
            m_time_redraw=GetTickCount()+m_delay;
           }
         //--- generate next event for custom timer
         EventChartCustom(CONTROLS_SELF_MESSAGE,1302,0,0,NULL);
         break;
     }
  }
//+------------------------------------------------------------------+
//| Delay                                                            |
//+------------------------------------------------------------------+
void CFlameCanvas::Delay(const uint value)
  {
//--- too small
   if(value<10)
      return;
//--- start delay
   uint cnt=GetTickCount()+value;
//--- delay
   while(cnt>=GetTickCount());
  }
//+------------------------------------------------------------------+
