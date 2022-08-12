//+------------------------------------------------------------------+
//|                                                    LineChart.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "ChartCanvas.mqh"
#include <Arrays\ArrayObj.mqh>
//+------------------------------------------------------------------+
//| Class CLineChart                                                 |
//| Usage: generates line chart                                      |
//+------------------------------------------------------------------+
class CLineChart : public CChartCanvas
  {
private:
   //--- data
   CArrayObj        *m_values;
   //--- adjusted parameters
   bool              m_filled;

public:
                     CLineChart(void);
                    ~CLineChart(void);
   //--- create
   virtual bool      Create(const string name,const int width,const int height,ENUM_COLOR_FORMAT clrfmt=COLOR_FORMAT_ARGB_NORMALIZE);
   //--- adjusted parameters
   void              Filled(const bool flag=true) { m_filled=flag; }
   //--- set up
   bool              SeriesAdd(const double &value[],const string descr="",const uint clr=0);
   bool              SeriesInsert(const uint pos,const double &value[],const string descr="",const uint clr=0);
   bool              SeriesUpdate(const uint pos,const double &value[],const string descr=NULL,const uint clr=0);
   bool              SeriesDelete(const uint pos);
   bool              ValueUpdate(const uint series,const uint pos,double value);

protected:
   virtual void      DrawChart(void);
   virtual void      DrawData(const uint index=0);

private:
   double            CalcArea(const uint index);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CLineChart::CLineChart(void) : m_filled(false)
  {
   ShowFlags(FLAG_SHOW_LEGEND|FLAGS_SHOW_SCALES|FLAG_SHOW_GRID);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CLineChart::~CLineChart(void)
  {
  }
//+------------------------------------------------------------------+
//| Create dynamic resource                                          |
//+------------------------------------------------------------------+
bool CLineChart::Create(const string name,const int width,const int height,ENUM_COLOR_FORMAT clrfmt)
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
bool CLineChart::SeriesAdd(const double &value[],const string descr,const uint clr)
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
bool CLineChart::SeriesInsert(const uint pos,const double &value[],const string descr,const uint clr)
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
bool CLineChart::SeriesUpdate(const uint pos,const double &value[],const string descr,const uint clr)
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
bool CLineChart::SeriesDelete(const uint pos)
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
bool CLineChart::ValueUpdate(const uint series,const uint pos,double value)
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
//| Redraws data                                                     |
//+------------------------------------------------------------------+
void CLineChart::DrawChart(void)
  {
   if(m_filled)
     {
      //--- calculate areas of filling
      double s[];
      ArrayResize(s,m_data_total);
      ArrayInitialize(s,0);
      for(uint i=0;i<m_data_total;i++)
        {
         CArrayDouble *data=m_values.At(i);
         if(data==NULL)
            continue;
         int total=data.Total();
         if(total<=1)
            continue;
         s[i]=CalcArea(i);
        }
      int index=ArrayMaximum(s);
      while(index!=-1 && s[index]!=0.0)
        {
         //--- draw in area descending order
         DrawData(index);
         s[index]=0.0;
         index=ArrayMaximum(s);
        }
     }
   else
      for(uint i=0;i<m_data_total;i++)
         DrawData(i);
  }
//+------------------------------------------------------------------+
//| Draws lines                                                      |
//+------------------------------------------------------------------+
void CLineChart::DrawData(const uint index)
  {
   double value=0.0;
//--- check
   CArrayDouble *data=m_values.At(index);
   if(data==NULL)
      return;
   int total=data.Total();
   if(total<=1)
      return;
//--- calculate
   int dx=m_data_area.Width()/(total-1);
   int x=m_data_area.left+1;
   int y1=0;
   int y2=(int)(m_y_0-data[0]*m_scale_y);
//--- draw
   for(int i=1;i<total;i++,x+=dx)
     {
      y1=y2;
      double val=data[i];
      if(val==EMPTY_VALUE)
         continue;
      if(m_accumulative)
         value+=val;
      else
         value=val;
      y2=(int)(m_y_0-value*m_scale_y);
      if(m_filled)
        {
         if((y1>m_y_0 && y2<m_y_0) || (y1<m_y_0 && y2>m_y_0))
           {
            //--- draw two triangles
            int x3;
            if(y1>y2)
              {
               x3=x+dx*(y1-m_y_0)/(y1-y2);
               FillTriangle(x,y1,x3,m_y_0,x,m_y_0,(uint)m_colors[index]);
               FillTriangle(x+dx,y2,x3,m_y_0,x+dx,m_y_0,(uint)m_colors[index]);
              }
            else
              {
               x3=x+dx*(m_y_0-y1)/(y2-y1);
               FillTriangle(x,y1,x3,m_y_0,x,m_y_0,(uint)m_colors[index]);
               FillTriangle(x+dx,y2,x3,m_y_0,x+dx,m_y_0,(uint)m_colors[index]);
              }
            continue;
           }
         if(y1<m_y_0 || y2<m_y_0)
           {
            if(y1>y2)
               FillTriangle(x,y1,x+dx,y2,x+dx,y1,(uint)m_colors[index]);
            if(y1<y2)
              {
               FillTriangle(x,y1,x+dx,y2,x,y2,(uint)m_colors[index]);
               y1=y2;
              }
           }
         if(y1>m_y_0 || y2>m_y_0)
           {
            if(y1<y2)
               FillTriangle(x,y1,x+dx,y2,x+dx,y1,(uint)m_colors[index]);
            if(y1>y2)
              {
               FillTriangle(x,y1,x+dx,y2,x,y2,(uint)m_colors[index]);
               y1=y2;
              }
           }
         FillRectangle(x,m_y_0,x+dx,y1,(uint)m_colors[index]);
        }
      else
         LineAA(x,y1,x+dx,y2,(uint)m_colors[index],STYLE_SOLID);
     }
  }
//+------------------------------------------------------------------+
//| Area of filling                                                  |
//+------------------------------------------------------------------+
double CLineChart::CalcArea(const uint index)
  {
   double area =0;
   double value=0;
   int    dx   =100;
//---
   CArrayDouble *data=m_values.At(index);
   if(data==NULL)
      return(0);
   int total=data.Total();
   if(total<=1)
      return(0);
   int y1=0;
   int y2=(int)(m_y_0-data[0]*m_scale_y);
   for(int i=0;i<total;i++)
     {
      y1=y2;
      double val=data[i];
      if(val==EMPTY_VALUE)
         continue;
      if(m_accumulative)
         value+=val;
      else
         value=val;
      y2=(int)(m_y_0-value*m_scale_y);
      if((y1>m_y_0 && y2<m_y_0) || (y1<m_y_0 && y2>m_y_0))
        {
         //--- line of values crosses the Y axis
         int x;
         if(y1>y2)
           {
            //--- from the bottom up
            x=dx*(y1-m_y_0)/(y1-y2);
            //--- add area of lower triangle
            area+=x*(y1-m_y_0)/2;
            //--- add area of upper triangle
            area+=(dx-x)*(m_y_0-y2)/2;
           }
         else
           {
            //--- from top down
            x=dx*(m_y_0-y1)/(y2-y1);
            //--- add area of upper triangle
            area+=x*(m_y_0-y1)/2;
            //--- add area of lower triangle
            area+=(dx-x)*(y2-m_y_0)/2;
           }
         continue;
        }
      if(y1<m_y_0 || y2<m_y_0)
        {
         //--- both values are greater than zero
         if(y1>y2)
           {
            //--- add area of triangle
            area+=dx*(y1-y2)/2;
            //--- add area of rectangle
            area+=dx*(m_y_0-y2);
           }
         if(y1<y2)
           {
            //--- add area of triangle
            area+=dx*(y2-y1)/2;
            //--- add area of rectangle
            area+=dx*(m_y_0-y1);
           }
        }
      if(y1>m_y_0 || y2>m_y_0)
        {
         //--- both values are less than zero
         if(y1<y2)
           {
            //--- add area of triangle
            area+=dx*(y2-y1)/2;
            //--- add area of rectangle
            area+=dx*(y1-m_y_0);
           }
         if(y1>y2)
           {
            //--- add area of triangle
            area+=dx*(y1-y2)/2;
            //--- add area of rectangle
            area+=dx*(y2-m_y_0);
           }
        }
     }
//---
   return(area);
  }
//+------------------------------------------------------------------+
