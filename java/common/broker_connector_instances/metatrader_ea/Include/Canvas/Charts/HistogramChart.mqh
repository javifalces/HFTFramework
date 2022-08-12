//+------------------------------------------------------------------+
//|                                               HistogramChart.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "ChartCanvas.mqh"
#include <Arrays\ArrayObj.mqh>
//+------------------------------------------------------------------+
//| Class CHistogramChart                                            |
//| Usage: generates histogram chart                                 |
//+------------------------------------------------------------------+
class CHistogramChart : public CChartCanvas
  {
private:
   //--- colors
   uint              m_fill_brush[];
   //--- adjusted parameters
   bool              m_gradient;
   uint              m_bar_gap;
   uint              m_bar_min_size;
   uint              m_bar_border;
   //--- data
   CArrayObj        *m_values;

public:
                     CHistogramChart(void);
                    ~CHistogramChart(void);
   //--- create
   virtual bool      Create(const string name,const int width,const int height,ENUM_COLOR_FORMAT clrfmt=COLOR_FORMAT_ARGB_NORMALIZE);
   //--- adjusted parameters
   void              Gradient(const bool flag=true) { m_gradient=flag;      }
   void              BarGap(const uint value)       { m_bar_gap=value;      }
   void              BarMinSize(const uint value)   { m_bar_min_size=value; }
   void              BarBorder(const uint value)    { m_bar_border=value;   }
   //--- data
   bool              SeriesAdd(const double &value[],const string descr="",const uint clr=0);
   bool              SeriesInsert(const uint pos,const double &value[],const string descr="",const uint clr=0);
   bool              SeriesUpdate(const uint pos,const double &value[],const string descr=NULL,const uint clr=0);
   bool              SeriesDelete(const uint pos);
   bool              ValueUpdate(const uint series,const uint pos,double value);

protected:
   virtual void      DrawData(const uint idx);
   void              DrawBar(const int x,const int y,const int w,const int h,const uint clr);
   void              GradientBrush(const int size,const uint fill_clr);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CHistogramChart::CHistogramChart(void) : m_gradient(true),
                                           m_bar_gap(3),
                                           m_bar_min_size(5),
                                           m_bar_border(0)
  {
   ShowFlags(FLAG_SHOW_LEGEND|FLAGS_SHOW_SCALES|FLAG_SHOW_GRID);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CHistogramChart::~CHistogramChart(void)
  {
   if(ArraySize(m_fill_brush)!=0)
      ArrayFree(m_fill_brush);
  }
//+------------------------------------------------------------------+
//| Create dynamic resource                                          |
//+------------------------------------------------------------------+
bool CHistogramChart::Create(const string name,const int width,const int height,ENUM_COLOR_FORMAT clrfmt)
  {
//--- create object to store data
   if((m_values=new CArrayObj)==NULL)
      return(false);
//--- pass responsibility for its destruction to the parent class
   m_data=m_values;
//--- call method of parent class
   if(!CChartCanvas::Create(name,width,height,clrfmt))
      return(false);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Adds data series                                                 |
//+------------------------------------------------------------------+
bool CHistogramChart::SeriesAdd(const double &value[],const string descr,const uint clr)
  {
//--- check
   if(m_data_total==m_max_data)
      return(false);
//--- add
   CArrayDouble *arr=new CArrayDouble;
   if(!m_values.Add(arr))
      return(false);
   if(!arr.AssignArray(value))
      return(false);
   if(!m_colors.Add((clr==0) ? GetDefaultColor(m_data_total) : clr))
      return(false);
   if(!m_descriptors.Add(descr))
      return(false);
   m_data_total++;
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Inserts data series                                              |
//+------------------------------------------------------------------+
bool CHistogramChart::SeriesInsert(const uint pos,const double &value[],const string descr,const uint clr)
  {
//--- check
   if(m_data_total==m_max_data)
      return(false);
   if(pos>=m_data_total)
      return(false);
//--- insert
   CArrayDouble *arr=new CArrayDouble;
   if(!m_values.Insert(arr,pos))
      return(false);
   if(!arr.AssignArray(value))
      return(false);
   if(!m_colors.Insert((clr==0) ? GetDefaultColor(m_data_total) : clr,pos))
      return(false);
   if(!m_descriptors.Insert(descr,pos))
      return(false);
   m_data_total++;
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Updates data series                                              |
//+------------------------------------------------------------------+
bool CHistogramChart::SeriesUpdate(const uint pos,const double &value[],const string descr,const uint clr)
  {
//--- check
   if(pos>=m_data_total)
      return(false);
   CArrayDouble *data=m_values.At(pos);
   if(data==NULL)
      return(false);
//--- update
   if(!data.AssignArray(value))
      return(false);
   if(clr!=0 && !m_colors.Update(pos,clr))
      return(false);
   if(descr!=NULL && !m_descriptors.Update(pos,descr))
      return(false);
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Deletes data series                                              |
//+------------------------------------------------------------------+
bool CHistogramChart::SeriesDelete(const uint pos)
  {
//--- check
   if(pos>=m_data_total && m_data_total!=0)
      return(false);
//--- delete
   if(!m_values.Delete(pos))
      return(false);
   m_data_total--;
   if(!m_colors.Delete(pos))
      return(false);
   if(!m_descriptors.Delete(pos))
      return(false);
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Updates element in data series                                   |
//+------------------------------------------------------------------+
bool CHistogramChart::ValueUpdate(const uint series,const uint pos,double value)
  {
   CArrayDouble *data=m_values.At(series);
//--- check
   if(data==NULL)
      return(false);
//--- update
   if(!data.Update(pos,value))
      return(false);
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Draws histogram                                                  |
//+------------------------------------------------------------------+
void CHistogramChart::DrawData(const uint idx)
  {
   double value=0.0;
//--- check
   CArrayDouble *data=m_values.At(idx);
   if(data==NULL)
      return;
   int total=data.Total();
   if(total==0 || (int)idx>=total)
      return;
//--- calculate
   int  x1=m_data_area.left;
   int  x2=m_data_area.right;
   int  dx=(x2-x1)/total;
   uint clr=m_colors[idx];
   uint w=dx/m_data_total;
   if(w<m_bar_min_size)
      w=m_bar_min_size;
   int x=x1+(int)(m_bar_gap+w*idx);
//--- set font
   string fontname;
   int    fontsize=0;
   uint   fontflags=0;
   uint   fontangle=0;
   if(IS_SHOW_VALUE)
     {
      FontGet(fontname,fontsize,fontflags,fontangle);
      FontSet(fontname,-10*(w-3),fontflags,900);
     }
//--- prepare gradient fill
   GradientBrush(w,clr);
//--- draw
   for(int i=0;i<total;i++,x+=dx)
     {
      int y,h;
      double val=data[i];
      if(val==EMPTY_VALUE)
         continue;
      if(m_accumulative)
         value+=val;
      else
         value=val;
//      int val=(int)(value*m_scale_y);
      if(value>0)
        {
         y=(m_y_0-(int)(value*m_scale_y));
         h=m_y_0-y;
        }
      else
        {
         y=m_y_0;
         h=-(int)(value*m_scale_y);
        }
      DrawBar(x,y,w,h,clr);
      //--- draw text of value
      if(IS_SHOW_VALUE)
        {
         string text =DoubleToString(value,2);
         int    width=(int)(TextWidth(text)+w);
         if(value>0)
           {
            if(width>y-m_y_max)
               TextOut(x+w/2,y+w,text,m_color_text,TA_RIGHT|TA_VCENTER);
            else
               TextOut(x+w/2,y-w,text,m_color_text,TA_LEFT|TA_VCENTER);
           }
         else
           {
            if(width>m_y_min-y-h)
               TextOut(x+w/2,y+h-w,text,m_color_text,TA_LEFT|TA_VCENTER);
            else
               TextOut(x+w/2,y+h+w,text,m_color_text,TA_RIGHT|TA_VCENTER);
           }
        }
     }
   if(IS_SHOW_VALUE)
      FontSet(fontname,fontsize,fontflags,fontangle);
  }
//+------------------------------------------------------------------+
//| Draws bar                                                        |
//+------------------------------------------------------------------+
void CHistogramChart::DrawBar(const int x,const int y,const int w,const int h,const uint clr)
  {
//--- draw bar
   if(!m_gradient || ArraySize(m_fill_brush)<w)
      FillRectangle(x+1,y+1,w-x-2,h-y-2,clr);
   else
     {
      for(int i=1;i<h;i++)
         ArrayCopy(m_pixels,m_fill_brush,(y+i)*m_width+x+1,0,w);
     }
//--- draw bar border
   if(m_bar_border!=0)
      Rectangle(x,y,x+w-1,y+h-1,m_color_border);
  }
//+------------------------------------------------------------------+
//| Creates brush for gradient fill                                  |
//+------------------------------------------------------------------+
void CHistogramChart::GradientBrush(const int size,const uint fill_clr)
  {
//--- to prepare gradient fill, we will use Bresenham's circle algorithm
//--- X coordinate - size of the fill,
//--- Y coordinate - color brightness
   if(m_gradient)
     {
      //--- adjust gradient radius if necessary
      int r=size;
      //--- check
      if(r<1)
         return;
      if(r!=ArrayResize(m_fill_brush,r))
         return;
      //--- initialize brush
      ArrayInitialize(m_fill_brush,m_color_background);
      //--- variables
      int  f   =1-r;
      int  dd_x=1;
      int  dd_y=-2*r;
      int  dx  =0;
      int  dy  =r;
      int  i1,i2;
      uint clr,dclr;
      //---
      i1=i2=r>>1;
      if((r&1)==0)
         i1--;
      //--- calculate
      while(dy>=dx)
        {
         clr=fill_clr;
         dclr=GETRGB(XRGB((r-dy)*GETRGBR(clr)/r,(r-dy)*GETRGBG(clr)/r,(r-dy)*GETRGBB(clr)/r));
         clr-=dclr;
         m_fill_brush[i1]=clr;
         m_fill_brush[i2]=clr;
         //---
         if(f>=0)
           {
            dy--;
            dd_y+=2;
            f+=dd_y;
           }
         dx++;
         if(--i1<0)
            break;
         i2++;
         dd_x+=2;
         f+=dd_x;
        }
     }
   else
      ArrayFree(m_fill_brush);
  }
//+------------------------------------------------------------------+
