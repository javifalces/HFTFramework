//+------------------------------------------------------------------+
//|                                                     PieChart.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "ChartCanvas.mqh"
//+------------------------------------------------------------------+
//| Class CPieChart                                                  |
//| Usage: generates pie chart                                       |
//+------------------------------------------------------------------+
class CPieChart : public CChartCanvas
  {
private:
   //--- data
   CArrayDouble     *m_values;
   //--- for draw
   int               m_x0;
   int               m_y0;
   int               m_r;

public:
                     CPieChart(void);
                    ~CPieChart(void);
   //--- create
   virtual bool      Create(const string name,const int width,const int height,ENUM_COLOR_FORMAT clrfmt=COLOR_FORMAT_XRGB_NOALPHA);
   //--- data
   bool              SeriesSet(const double &value[],const string &text[],const uint &clr[]);
   bool              ValueAdd(const double value,const string descr="",const uint clr=0);
   bool              ValueInsert(const uint pos,const double value,const string descr="",const uint clr=0);
   bool              ValueUpdate(const uint pos,const double value,const string descr=NULL,const uint clr=0);
   bool              ValueDelete(const uint pos);

protected:
   virtual void      DrawChart(void);
   void              DrawPie(double fi3,double fi4,int idx,CPoint &p[],const uint clr);
   string            LabelMake(const string text,const double value,const bool to_left);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CPieChart::CPieChart(void)
  {
   uint flags=FLAG_SHOW_LEGEND|FLAG_SHOW_DESCRIPTORS|FLAG_SHOW_VALUE|FLAG_SHOW_PERCENT;
   AllowedShowFlags(flags);
   ShowFlags(flags);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CPieChart::~CPieChart(void)
  {
  }
//+------------------------------------------------------------------+
//| Create dynamic resource                                          |
//+------------------------------------------------------------------+
bool CPieChart::Create(const string name,const int width,const int height,ENUM_COLOR_FORMAT clrfmt)
  {
//--- create object to store data
   if((m_values=new CArrayDouble)==NULL)
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
//| Sets displayed parameters                                        |
//+------------------------------------------------------------------+
bool CPieChart::SeriesSet(const double &value[],const string &text[],const uint &clr[])
  {
//--- !!! user is responsible for correct filling of arrays !!!
//--- check
   if(m_values==NULL)
      return(false);
//--- set
   if(!m_values.AssignArray(value))
      return(false);
   if(!m_descriptors.AssignArray(text))
      return(false);
   if(!m_colors.AssignArray(clr))
      return(false);
   m_data_total=m_values.Total();
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Adds displayed parameter (to the end)                            |
//+------------------------------------------------------------------+
bool CPieChart::ValueAdd(const double value,const string descr,const uint clr)
  {
//--- check
   if((value<=0))
      return(false);
//--- add
   if(!m_values.Add(value))
      return(false);
   if(!m_descriptors.Add(descr))
      return(false);
   if(!m_colors.Add((clr==0) ? GetDefaultColor(m_data_total) : clr))
      return(false);
   m_data_total++;
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Inserts displayed parameter (to specified position)              |
//+------------------------------------------------------------------+
bool CPieChart::ValueInsert(const uint pos,const double value,const string descr,const uint clr)
  {
//--- check
   if((value<=0))
      return(false);
//--- insert
   if(!m_values.Insert(value,pos))
      return(false);
   if(!m_descriptors.Insert(descr,pos))
      return(false);
   if(!m_colors.Insert((clr==0) ? GetDefaultColor(m_data_total) : clr,pos))
      return(false);
   m_data_total++;
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Updates displayed parameter (in specified position)              |
//+------------------------------------------------------------------+
bool CPieChart::ValueUpdate(const uint pos,const double value,const string descr,const uint clr)
  {
//--- check
   if((value<=0))
      return(false);
//--- update
   if(!m_values.Update(pos,value))
      return(false);
   if(descr!=NULL && !m_descriptors.Update(pos,descr))
      return(false);
   if(clr!=0 && !m_colors.Update(pos,clr))
      return(false);
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Deletes displayed parameter (from specified position)            |
//+------------------------------------------------------------------+
bool CPieChart::ValueDelete(const uint pos)
  {
//--- delete
   if(!m_values.Delete(pos))
      return(false);
   m_data_total--;
   if(!m_descriptors.Delete(pos))
      return(false);
   if(!m_colors.Delete(pos))
      return(false);
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Draws                                                            |
//+------------------------------------------------------------------+
void CPieChart::DrawChart(void)
  {
//--- check
   if(m_data_total==0)
      return;
//--- variables
   string text="";
   double angle=M_PI*(m_data_offset%360)/180;
   int    width,height;
   int    dw=0;
   int    dh=0;
   int    index;
   CPoint p0[];
   CPoint p1[];
//--- calculate geometry
   width =(m_data_area.Width()<<3)/10;
   height=(m_data_area.Height()<<3)/10;
   if(IS_SHOW_LEGEND || !IS_SHOW_DESCRIPTORS)
     {
      if(IS_SHOW_VALUE)
         dw=(int)m_max_value_width;
      else
        {
         if(IS_SHOW_PERCENT)
            dw=TextWidth("100.00%");
        }
     }
   else
     {
      if(IS_SHOW_DESCRIPTORS)
        {
         if(IS_SHOW_VALUE)
            dw=(int)m_max_value_width+TextWidth(" ()");
         else
           {
            if(IS_SHOW_PERCENT)
               dw=TextWidth(" (100.00%)");
           }
         dw+=(int)m_max_descr_width;
        }
     }
//--- pie chart will always be round
   width -=2*dw+10;
   height-=20;
   m_x0=m_data_area.left+(m_data_area.Width()>>1);
   m_y0=m_data_area.top+(m_data_area.Height()>>1);
   m_r =(((width>height) ? height : width)>>1);
//--- draw pie chart
   if(ArrayResize(p0,m_data_total+1)==-1)
      return;
   if(m_data_total==1)
     {
      FillCircle(m_x0,m_y0,m_r,m_colors[0]);
      Circle(m_x0,m_y0,m_r,m_color_border);
     }
   else
     {
      Circle(m_x0,m_y0,m_r,m_color_border);
      for(uint i=0;i<m_index_size;i++)
        {
         index=m_index[i];
         double val=m_values[index];
         double d_a=2*M_PI*val/m_sum;
         DrawPie(angle,angle+d_a,i,p0,m_colors[index]);
         angle+=d_a;
         angle=MathMod(angle,2*M_PI);
        }
      if(m_data_total!=m_index_size)
         DrawPie(angle,angle+2*M_PI*m_others/m_sum,m_index_size,p0,COLOR2RGB(clrBlack));
      for(uint i=0;i<=m_index_size;i++)
         Line(m_x0,m_y0,p0[i].x,p0[i].y,m_color_border);
      Circle(m_x0,m_y0,m_r,m_color_border);
     }
//--- draw descriptors
   angle=M_PI*(m_data_offset%360)/180;
   int r1=(int)round(1.1*m_r);
   if(ArrayResize(p1,m_data_total)==-1)
      return;
   //--- calculations
   for(uint i=0;i<m_index_size;i++)
     {
      index=m_index[i];
      angle+=M_PI*m_values[index]/m_sum;
      //--- lines
      p0[i].x=m_x0+(int)round(m_r*cos(angle));
      p0[i].y=m_y0-(int)round(m_r*sin(angle));
      p1[i].x=m_x0+(int)round(r1*cos(angle));
      p1[i].y=m_y0-(int)round(r1*sin(angle));
      angle+=M_PI*m_values[index]/m_sum;
     }
   if(m_data_total!=m_index_size)
     {
      index=(int)m_data_total-1;
      angle+=M_PI*m_others/m_sum;
      p0[index].x=m_x0+(int)round(m_r*cos(angle));
      p0[index].y=m_y0-(int)round(m_r*sin(angle));
      p1[index].x=m_x0+(int)round(r1*cos(angle));
      p1[index].y=m_y0-(int)round(r1*sin(angle));
     }
   int x,y;
   //--- draw descriptors to the left
   for(uint i=0;i<m_index_size;i++)
      if(p0[i].x<m_x0)
        {
         x=p1[i].x-10;
         y=p1[i].y;
         index=m_index[i];
         text=LabelMake(m_descriptors[index],m_values[index],true);
         if(text!="")
           {
            Line(p0[i].x,p0[i].y,p1[i].x,p1[i].y,m_color_border);
            Line(p1[i].x,p1[i].y,x,y,m_color_border);
            TextOut(x-5,y,text,m_color_text,TA_RIGHT|TA_VCENTER);
           }
        }
   //--- draw descriptors to the right
   for(uint i=0;i<m_index_size;i++)
      if(p0[i].x>=m_x0)
        {
         x=p1[i].x+10;
         y=p1[i].y;
         index=m_index[i];
         text=LabelMake(m_descriptors[index],m_values[index],false);
         if(text!="")
           {
            Line(p0[i].x,p0[i].y,p1[i].x,p1[i].y,m_color_border);
            Line(p1[i].x,p1[i].y,x,y,m_color_border);
            TextOut(x+5,y,text,m_color_text,TA_LEFT|TA_VCENTER);
           }
        }
   if(m_data_total!=m_index_size)
     {
      index=(int)m_data_total-1;
      if(p0[index].x>=m_x0)
        {
         x=p1[index].x+10;
         y=p1[index].y;
         text=LabelMake("Others",m_others,true);
         TextOut(x+5,y,text,m_color_text,TA_LEFT|TA_VCENTER);
        }
      else
        {
         x=p1[index].x-10;
         y=p1[index].y;
         text=LabelMake("Others",m_others,false);
         TextOut(x-5,y,text,m_color_text,TA_RIGHT|TA_VCENTER);
        }
      if(text!="")
        {
         Line(p0[index].x,p0[index].y,p1[index].x,p1[index].y,m_color_border);
         Line(p1[index].x,p1[index].y,x,y,m_color_border);
        }
     }
   ArrayFree(p1);
   ArrayFree(p0);
  }
//+------------------------------------------------------------------+
//| Draw pie                                                         |
//+------------------------------------------------------------------+
void CPieChart::DrawPie(double fi3,double fi4,int idx,CPoint &p[],const uint clr)
  {
//--- draw arc
   Arc(m_x0,m_y0,m_r,m_r,fi3,fi4,p[idx].x,p[idx].y,p[idx+1].x,p[idx+1].y,clr);
//--- variables
   int x3=p[idx].x;
   int y3=p[idx].y;
   int x4=p[idx+1].x;
   int y4=p[idx+1].y;
//--- draw radii
   if(idx==0)
      Line(m_x0,m_y0,x3,y3,clr);
   if(idx!=m_data_total-1)
      Line(m_x0,m_y0,x4,y4,clr);
//--- fill
   double fi=(fi3+fi4)/2;
   int xf=m_x0+(int)(0.99*m_r*cos(fi));
   int yf=m_y0-(int)(0.99*m_r*sin(fi));
   Fill(xf,yf,clr);
//--- for small pie
   if(fi4-fi3<=M_PI_4)
      Line(m_x0,m_y0,xf,yf,clr);
  }
//+------------------------------------------------------------------+
//| Make label for pie                                               |
//+------------------------------------------------------------------+
string CPieChart::LabelMake(const string text,const double value,const bool to_left)
  {
   string label="";
//---
   if(to_left)
     {
      if(IS_SHOW_LEGEND || !IS_SHOW_DESCRIPTORS)
        {
         if(IS_SHOW_VALUE)
            label=DoubleToString(value,2);
         else
           {
            if(IS_SHOW_PERCENT)
               label=DoubleToString(100*value/m_sum,2)+"%";
           }
        }
      else
        {
         label=text;
         if(IS_SHOW_VALUE)
            label+=" ("+DoubleToString(value,2)+")";
         else
           {
            if(IS_SHOW_PERCENT)
               label+=" ("+DoubleToString(100*value/m_sum,2)+"%)";
           }
        }
     }
   else
     {
      if(IS_SHOW_LEGEND || !IS_SHOW_DESCRIPTORS)
        {
         if(IS_SHOW_VALUE)
            label=DoubleToString(value,2);
         else
           {
            if(IS_SHOW_PERCENT)
               label=DoubleToString(100*value/m_sum,2)+"%";
           }
        }
      else
        {
         if(IS_SHOW_VALUE)
            label="("+DoubleToString(value,2)+") ";
         else
           {
            if(IS_SHOW_PERCENT)
               label="("+DoubleToString(100*value/m_sum,2)+"%) ";
           }
         label+=text;
        }
     }
//---
   return(label);
  }
//+------------------------------------------------------------------+
