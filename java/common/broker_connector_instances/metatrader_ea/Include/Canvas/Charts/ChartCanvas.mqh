//+------------------------------------------------------------------+
//|                                                  ChartCanvas.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "..\Canvas.mqh"
#include <Arrays\ArrayInt.mqh>
#include <Arrays\ArrayDouble.mqh>
#include <Arrays\ArrayString.mqh>
//--- enumerations
enum ENUM_SHOW_FLAGS
  {
   FLAG_SHOW_NONE        =0,
   FLAG_SHOW_LEGEND      =1,
   FLAG_SHOW_SCALE_LEFT  =2,
   FLAG_SHOW_SCALE_RIGHT =4,
   FLAG_SHOW_SCALE_TOP   =8,
   FLAG_SHOW_SCALE_BOTTOM=16,
   FLAG_SHOW_GRID        =32,
   FLAG_SHOW_DESCRIPTORS =64,
   FLAG_SHOW_VALUE       =128,
   FLAG_SHOW_PERCENT     =256,
   FLAGS_SHOW_SCALES     =(FLAG_SHOW_SCALE_LEFT+FLAG_SHOW_SCALE_RIGHT+
                           FLAG_SHOW_SCALE_TOP+FLAG_SHOW_SCALE_BOTTOM),
   FLAGS_SHOW_ALL        =(FLAG_SHOW_LEGEND+FLAGS_SHOW_SCALES+FLAG_SHOW_GRID+
                           FLAG_SHOW_DESCRIPTORS+FLAG_SHOW_VALUE+FLAG_SHOW_PERCENT)
  };
enum ENUM_ALIGNMENT
  {
   ALIGNMENT_LEFT        = 1,   // align by left border
   ALIGNMENT_TOP         = 2,   // align by top border
   ALIGNMENT_RIGHT       = 4,   // align by right border
   ALIGNMENT_BOTTOM      = 8    // align by bottom border
  };
//--- macro
#define IS_SHOW_LEGEND          ((m_show_flags&FLAG_SHOW_LEGEND)      !=0)
#define IS_SHOW_SCALES          ((m_show_flags&FLAGS_SHOW_SCALES)     !=0)
#define IS_SHOW_SCALE_LEFT      ((m_show_flags&FLAG_SHOW_SCALE_LEFT)  !=0)
#define IS_SHOW_SCALE_RIGHT     ((m_show_flags&FLAG_SHOW_SCALE_RIGHT) !=0)
#define IS_SHOW_SCALE_TOP       ((m_show_flags&FLAG_SHOW_SCALE_TOP)   !=0)
#define IS_SHOW_SCALE_BOTTOM    ((m_show_flags&FLAG_SHOW_SCALE_BOTTOM)!=0)
#define IS_SHOW_GRID            ((m_show_flags&FLAG_SHOW_GRID)        !=0)
#define IS_SHOW_DESCRIPTORS     ((m_show_flags&FLAG_SHOW_DESCRIPTORS) !=0)
#define IS_SHOW_VALUE           ((m_show_flags&FLAG_SHOW_VALUE)       !=0)
#define IS_SHOW_PERCENT         ((m_show_flags&FLAG_SHOW_PERCENT)     !=0)
//+------------------------------------------------------------------+
//| Class CChartCanvas                                               |
//| Usage: base class for graphical charts                           |
//+------------------------------------------------------------------+
class CChartCanvas : public CCanvas
  {
protected:
   //--- colors
   uint              m_color_background;
   uint              m_color_border;
   uint              m_color_text;
   uint              m_color_grid;
   //--- adjusted parameters
   uint              m_max_data;
   uint              m_max_descr_len;
   uint              m_allowed_show_flags;
   uint              m_show_flags;
   ENUM_ALIGNMENT    m_legend_alignment;
   uint              m_threshold_drawing;
   bool              m_accumulative;
   //--- parameters for scales and grid
   double            m_v_scale_min;
   double            m_v_scale_max;
   uint              m_num_grid;
   int               m_scale_digits;
   //--- data
   int               m_data_offset;
   uint              m_data_total;
   CArray           *m_data;
   CArrayInt         m_colors;
   CArrayString      m_descriptors;
   //---
   CArrayInt         m_index;
   uint              m_index_size;
   double            m_sum;
   double            m_others;
   uint              m_max_descr_width;
   uint              m_max_value_width;
   //--- variables
   CRect             m_data_area;
   //--- variables for scaling and scales
   double            m_scale_x;
   int               m_x_min;
   int               m_x_0;
   int               m_x_max;
   int               m_dx_grid;
   double            m_scale_y;
   int               m_y_min;
   int               m_y_0;
   int               m_y_max;
   int               m_dy_grid;
   string            m_scale_text[];

public:
                     CChartCanvas(void);
                    ~CChartCanvas(void);
   //--- create
   virtual bool      Create(const string name,const int width,const int height,ENUM_COLOR_FORMAT clrfmt=COLOR_FORMAT_XRGB_NOALPHA);
   //--- colors
   uint              ColorBackground(void) const { return(m_color_background); }
   void              ColorBackground(const uint value);
   uint              ColorBorder(void) const { return(m_color_border); }
   void              ColorBorder(const uint value);
   uint              ColorText(void) const { return(m_color_text); }
   void              ColorText(const uint value);
   uint              ColorGrid(void) const { return(m_color_grid); }
   void              ColorGrid(const uint value) { m_color_grid=value; }
   //--- adjusted parameters
   uint              MaxData(void) const { return(m_max_data); }
   void              MaxData(const uint value);
   uint              MaxDescrLen(void) const { return(m_max_descr_len); }
   void              MaxDescrLen(const uint value);
   //--- show flags
   void              AllowedShowFlags(const uint flags);
   uint              ShowFlags(void) const { return(m_show_flags); }
   void              ShowFlags(const uint flags);
   bool              IsShowLegend(void)          const { return(IS_SHOW_LEGEND);       }
   bool              IsShowScaleLeft(void)       const { return(IS_SHOW_SCALE_LEFT);   }
   bool              IsShowScaleRight(void)      const { return(IS_SHOW_SCALE_RIGHT);  }
   bool              IsShowScaleTop(void)        const { return(IS_SHOW_SCALE_TOP);    }
   bool              IsShowScaleBottom(void)     const { return(IS_SHOW_SCALE_BOTTOM); }
   bool              IsShowGrid(void)            const { return(IS_SHOW_GRID);         }
   bool              IsShowDescriptors(void)     const { return(IS_SHOW_DESCRIPTORS);  }
   bool              IsShowPercent(void)         const { return(IS_SHOW_PERCENT);      }
   void              ShowLegend(const bool flag=true);
   void              ShowScaleLeft(const bool flag=true);
   void              ShowScaleRight(const bool flag=true);
   void              ShowScaleTop(const bool flag=true);
   void              ShowScaleBottom(const bool flag=true);
   void              ShowGrid(const bool flag=true);
   void              ShowDescriptors(const bool flag=true);
   void              ShowValue(const bool flag=true);
   void              ShowPercent(const bool flag=true);
   void              LegendAlignment(const ENUM_ALIGNMENT value);
   void              Accumulative(const bool flag=true);
   //--- for scales and grid
   double            VScaleMin(void) const { return(m_v_scale_min); }
   void              VScaleMin(const double value);
   double            VScaleMax(void) const { return(m_v_scale_max); }
   void              VScaleMax(const double value);
   uint              NumGrid(void) const { return(m_num_grid); }
   void              NumGrid(const uint value);
   void              VScaleParams(const double max,const double min,const uint grid);
   //--- state
   int               DataOffset(void) const { return(m_data_offset); }
   void              DataOffset(const int value);
   //--- data
   uint              DataTotal(void) const { return(m_data_total); }
   bool              DescriptorUpdate(const uint pos,const string descr);
   bool              ColorUpdate(const uint pos,const uint clr);

protected:
   virtual void      ValuesCheck(void);
   virtual void      Redraw(void);
   virtual void      DrawBackground(void);
   virtual void      DrawLegend(void);
   int               DrawLegendVertical(const int w,const int h);
   int               DrawLegendHorizontal(const int w,const int h);
   virtual void      CalcScales(void);
   virtual void      DrawScales(void);
   virtual int       DrawScaleLeft(const bool draw=true);
   virtual int       DrawScaleRight(const bool draw=true);
   virtual int       DrawScaleTop(const bool draw=true);
   virtual int       DrawScaleBottom(const bool draw=true);
   virtual void      DrawGrid(void);
   virtual void      DrawDescriptors(void)       {}
   virtual void      DrawChart(void);
   virtual void      DrawData(const uint idx=0)  {}
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartCanvas::CChartCanvas(void) : m_color_background(XRGB(0xFF,0xFF,0xFF)),
                                   m_color_border(XRGB(0x9F,0x9F,0x9F)),
                                   m_color_text(XRGB(0x3F,0x3F,0x3F)),
                                   m_color_grid(XRGB(0xCF,0xCF,0xCF)),
                                   m_max_data(10),
                                   m_max_descr_len(10),
                                   m_allowed_show_flags(FLAGS_SHOW_ALL),
                                   m_show_flags(FLAG_SHOW_NONE),
                                   m_legend_alignment(ALIGNMENT_BOTTOM),
                                   m_threshold_drawing(2),
                                   m_accumulative(false),
                                   m_data_offset(0),
                                   m_data_total(0),
                                   m_data(NULL),
                                   m_v_scale_min(0.0),
                                   m_v_scale_max(10.0),
                                   m_num_grid(5),
                                   m_scale_digits(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartCanvas::~CChartCanvas(void)
  {
   if(m_data!=NULL)
      delete m_data;
  }
//+------------------------------------------------------------------+
//| Create dynamic resource                                          |
//+------------------------------------------------------------------+
bool CChartCanvas::Create(const string name,const int width,const int height,ENUM_COLOR_FORMAT clrfmt)
  {
//--- call method of parent class
   if(!CCanvas::Create(name,width,height,clrfmt))
      return(false);
//--- set font
   FontSet("Tahoma",-100);
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Sets background color                                            |
//+------------------------------------------------------------------+
void CChartCanvas::ColorBackground(const uint value)
  {
   m_color_background=value;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets border color                                                |
//+------------------------------------------------------------------+
void CChartCanvas::ColorBorder(const uint value)
  {
   m_color_border=value;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets text color                                                  |
//+------------------------------------------------------------------+
void CChartCanvas::ColorText(const uint value)
  {
   m_color_text=value;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets maximum amount of data                                      |
//+------------------------------------------------------------------+
void CChartCanvas::MaxData(const uint value)
  {
//--- check
   if((value==0) || (m_data_total==value))
    return;
//--- save
   m_max_data=value;
   if(m_data_total>m_max_data)
     {
      m_data_total=value;
      m_colors.Resize(value);
      m_descriptors.Resize(value);
     }
  }
//+------------------------------------------------------------------+
//| Sets maximum length of descriptor                                |
//+------------------------------------------------------------------+
void CChartCanvas::MaxDescrLen(const uint value)
  {
   m_max_descr_len=value;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets allowed visibility flags                                    |
//+------------------------------------------------------------------+
void CChartCanvas::AllowedShowFlags(const uint flags)
  {
   m_allowed_show_flags=flags;
   m_show_flags&=m_allowed_show_flags;
  }
//+------------------------------------------------------------------+
//| Sets visibility flags                                            |
//+------------------------------------------------------------------+
void CChartCanvas::ShowFlags(const uint flags)
  {
   m_show_flags=flags&m_allowed_show_flags;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for "legend"                                |
//+------------------------------------------------------------------+
void CChartCanvas::ShowLegend(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_LEGEND)!=0)
     {
      if(flag)
         m_show_flags|=FLAG_SHOW_LEGEND;
      else
         m_show_flags&=~FLAG_SHOW_LEGEND;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for left scale                              |
//+------------------------------------------------------------------+
void CChartCanvas::ShowScaleLeft(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_SCALE_LEFT)!=0)
     {
      if(flag)
         m_show_flags|=FLAG_SHOW_SCALE_LEFT;
      else
         m_show_flags&=~FLAG_SHOW_SCALE_LEFT;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for right scale                             |
//+------------------------------------------------------------------+
void CChartCanvas::ShowScaleRight(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_SCALE_RIGHT)!=0)
     {
      if(flag)
         m_show_flags|=FLAG_SHOW_SCALE_RIGHT;
      else
         m_show_flags&=~FLAG_SHOW_SCALE_RIGHT;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for top scale                               |
//+------------------------------------------------------------------+
void CChartCanvas::ShowScaleTop(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_SCALE_TOP)!=0)
     {
      if(flag)
         m_show_flags|=FLAG_SHOW_SCALE_TOP;
      else
         m_show_flags&=~FLAG_SHOW_SCALE_TOP;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for bottom scale                            |
//+------------------------------------------------------------------+
void CChartCanvas::ShowScaleBottom(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_SCALE_BOTTOM)!=0)
     {
      if(flag)
         m_show_flags|=FLAG_SHOW_SCALE_BOTTOM;
      else
         m_show_flags&=~FLAG_SHOW_SCALE_BOTTOM;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for grid                                    |
//+------------------------------------------------------------------+
void CChartCanvas::ShowGrid(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_GRID)!=0)
     {
      if(flag)
         m_show_flags|=FLAG_SHOW_GRID;
      else
         m_show_flags&=~FLAG_SHOW_GRID;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for descriptors                             |
//+------------------------------------------------------------------+
void CChartCanvas::ShowDescriptors(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_DESCRIPTORS)!=0)
     {
      if(flag)
         m_show_flags|=FLAG_SHOW_DESCRIPTORS;
      else
         m_show_flags&=~FLAG_SHOW_DESCRIPTORS;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for value                                   |
//+------------------------------------------------------------------+
void CChartCanvas::ShowValue(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_VALUE)!=0)
     {
      if(flag)
        {
         m_show_flags|=FLAG_SHOW_VALUE;
         m_show_flags&=~FLAG_SHOW_PERCENT;
        }
      else
         m_show_flags&=~FLAG_SHOW_VALUE;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets visibility flag for percentage                              |
//+------------------------------------------------------------------+
void CChartCanvas::ShowPercent(const bool flag)
  {
   if((m_allowed_show_flags&FLAG_SHOW_PERCENT)!=0)
     {
      if(flag)
        {
         m_show_flags|=FLAG_SHOW_PERCENT;
         m_show_flags&=~FLAG_SHOW_VALUE;
        }
      else
         m_show_flags&=~FLAG_SHOW_PERCENT;
      //--- redraw
      if(m_data_total>0)
         Redraw();
     }
  }
//+------------------------------------------------------------------+
//| Sets legend alignment                                            |
//+------------------------------------------------------------------+
void CChartCanvas::LegendAlignment(const ENUM_ALIGNMENT value)
  {
   m_legend_alignment=value;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets accumulative flag                                           |
//+------------------------------------------------------------------+
void CChartCanvas::Accumulative(const bool flag=true)
  {
   m_accumulative=flag;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets lower limit for vertical scale                              |
//+------------------------------------------------------------------+
void CChartCanvas::VScaleMin(const double value)
  {
//--- check
   if(value==m_v_scale_max)
      return;
//--- save
   m_v_scale_min=value;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets upper limit for vertical scale                              |
//+------------------------------------------------------------------+
void CChartCanvas::VScaleMax(const double value)
  {
   if(value==m_v_scale_min)
      return;
//--- save
   m_v_scale_max=value;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets number of vertical scale divisions                          |
//+------------------------------------------------------------------+
void CChartCanvas::NumGrid(const uint value)
  {
//--- check
   if(value==0)
    return;
//--- save
   m_num_grid=value;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets parameters for vertical scale                               |
//+------------------------------------------------------------------+
void CChartCanvas::VScaleParams(const double max,const double min,const uint grid)
  {
//--- check
   if(grid==0)
      return;
   if(max<=min)
      return;
//--- save
   m_v_scale_max=max;
   m_v_scale_min=min;
   m_num_grid   =grid;
//--- redraw
   if(m_data_total>0)
      Redraw();
  }
//+------------------------------------------------------------------+
//| Sets data offset                                                 |
//+------------------------------------------------------------------+
void CChartCanvas::DataOffset(const int value)
  {
   m_data_offset=value;
//--- redraw
   Redraw();
  }
//+------------------------------------------------------------------+
//| Updates parameter descriptor only (in specified position)        |
//+------------------------------------------------------------------+
bool CChartCanvas::DescriptorUpdate(const uint pos,const string descr)
  {
//--- update
   if(descr!=NULL && !m_descriptors.Update(pos,descr))
      return(false);
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Updates parameter color only (in specified position)             |
//+------------------------------------------------------------------+
bool CChartCanvas::ColorUpdate(const uint pos,const uint clr)
  {
//--- update
   if(clr!=0 && !m_colors.Update(pos,clr))
      return(false);
//--- redraw
   Redraw();
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Checks values for insignificance                                 |
//+------------------------------------------------------------------+
void CChartCanvas::ValuesCheck(void)
  {
   string text;
   uint   w,h;
//--- clear
   m_max_value_width=0;
   m_sum            =0;
   m_others         =0;
   m_index_size     =0;
   m_index.Clear();
//--- check
   if(m_data==NULL)
      return;
   if(m_data.Type()==TYPE_DOUBLE)
     {
      //--- single-series chart
      //--- calculate sum of all values
      for(uint i=0;i<m_data_total;i++)
         m_sum+=((CArrayDouble*)m_data)[i];
      //--- find insignificant values
      for(uint i=0;i<m_data_total;i++)
        {
         double value=((CArrayDouble*)m_data)[i];
         //--- insignificant value is less than drawing threshold value
         if(value/m_sum<0.01*m_threshold_drawing)
           {
            //--- move insignificant value to 'others' group
            m_others+=value;
           }
         else
           {
            //--- populate index for all significant values
            m_index.Add(i);
            text=DoubleToString(value,2);
            TextSize(text,w,h);
            if(m_max_value_width<w)
               m_max_value_width=w;
           }
        }
      text=DoubleToString(m_others,2);
      TextSize(text,w,h);
      if(m_max_value_width<w)
         m_max_value_width=w;
      m_index_size=m_index.Total();
     }
   else
     {
      //--- multi-series chart
      //--- populate index for uniformity
      for(uint i=0;i<m_data_total;i++)
         m_index.Add(i);
      m_index_size=m_data_total;
     }
  }
//+------------------------------------------------------------------+
//| Redraws chart                                                    |
//+------------------------------------------------------------------+
void CChartCanvas::Redraw(void)
  {
   int gap=m_fontsize/(-10);
   m_data_area.SetBound(gap,gap,m_width-gap,m_height-gap);
//--- values check
   ValuesCheck();
//--- draw background
   DrawBackground();
//--- draw auxiliary elements
   if(IS_SHOW_LEGEND)
      DrawLegend();
   if(IS_SHOW_SCALES)
      DrawScales();
   if(IS_SHOW_GRID)
      DrawGrid();
   if(IS_SHOW_DESCRIPTORS)
      DrawDescriptors();
//--- draw data
   DrawChart();
//--- fix changes
   Update();
  }
//+------------------------------------------------------------------+
//| Redraws background                                               |
//+------------------------------------------------------------------+
void CChartCanvas::DrawBackground(void)
  {
   Erase(m_color_background);
   Rectangle(0,0,m_width-1,m_height-1,m_color_border);
  }
//+------------------------------------------------------------------+
//| Redraws "legend"                                                 |
//+------------------------------------------------------------------+
void CChartCanvas::DrawLegend(void)
  {
//--- check
   if(m_data_total==0)
      return;
//--- variables
   string text="A";
   int    max_len=0;
   int    w,h;
//--- set font
   FontAngleSet(0);
//--- calculate
   if(m_data_total==m_index_size)
     {
      for(uint i=0;i<m_data_total;i++)
        {
         w=StringLen(m_descriptors[i]);
         if(max_len<w)
           {
            max_len=w;
            text=m_descriptors[i];
           }
        }
     }
   else
     {
      for(uint i=0;i<m_index_size;i++)
        {
         int index=m_index[i];
         w=StringLen(m_descriptors[index]);
         if(max_len<w)
           {
            max_len=w;
            text=m_descriptors[index];
           }
        }
      w=StringLen("Others");
      if(max_len<w)
        {
         max_len=w;
         text="Others";
        }
     }
   if(max_len==0)
    return;
   TextSize(" - "+text,m_max_descr_width,h);
   w=(int)m_max_descr_width+3*h;
//--- check flag
   if(!IS_SHOW_LEGEND)
      return;
//--- draw
   switch(m_legend_alignment)
     {
      case ALIGNMENT_LEFT:
         m_data_area.left+=DrawLegendVertical(w,h);
         break;
      case ALIGNMENT_RIGHT:
         m_data_area.right-=DrawLegendVertical(w,h);
         break;
      case ALIGNMENT_TOP:
         m_data_area.top+=DrawLegendHorizontal(w,h);
         break;
      case ALIGNMENT_BOTTOM:
         m_data_area.bottom-=DrawLegendHorizontal(w,h);
         break;
     }
  }
//+------------------------------------------------------------------+
//| Draw vertical "legend"                                           |
//+------------------------------------------------------------------+
int CChartCanvas::DrawLegendVertical(const int w,const int h)
  {
   int width =m_data_area.Width();
   int height=m_data_area.Height();
   int rows  =(int)m_index_size;
   int cols  =1;
   int dy    =(int)(1.5*h);
//--- calculate
   while(dy*rows>m_height)
     {
      cols++;
      rows=(int)m_index_size/cols;
      if((int)m_index_size%cols!=0)
         rows++;
     }
//--- draw
   int x0=(m_legend_alignment==ALIGNMENT_RIGHT) ? width-w*cols+h : h;
   int x=0;
   int y =-h/2;
   int i;
   if(m_data_total==m_index_size)
     {
      for(i=0;i<(int)m_data_total;i++,x+=w)
        {
         if(i%cols==0)
           {
            x=x0;
            y+=dy;
           }
         FillRectangle(x,y,x+h,y+h,(uint)m_colors[i]);
         TextOut(x+h,y," - "+m_descriptors[i],m_color_text);
        }
     }
   else
     {
      for(i=0;i<(int)m_index_size;i++,x+=w)
        {
         int index=m_index[i];
         if(i%cols==0)
           {
            x=x0;
            y+=dy;
           }
         FillRectangle(x,y,x+h,y+h,(uint)m_colors[index]);
         TextOut(x+h,y," - "+m_descriptors[index],m_color_text);
        }
      if(i%cols==0)
        {
         x=x0;
         y+=dy;
        }
      FillRectangle(x,y,x+h,y+h,COLOR2RGB(clrBlack));
      TextOut(x+h,y," - Others",m_color_text);
     }
//--- width
   return(w*cols);
  }
//+------------------------------------------------------------------+
//| Draw horizontal "legend"                                         |
//+------------------------------------------------------------------+
int CChartCanvas::DrawLegendHorizontal(const int w,const int h)
  {
   int width =m_data_area.Width();
   int height=m_data_area.Height();
   int rows  =1;
   int cols  =(int)m_index_size;
//--- calculate
   while(w*cols>m_width)
     {
      rows++;
      cols=(int)m_index_size/rows;
      if((int)m_index_size%rows!=0)
         cols++;
     }
//--- draw
   int dx=width/(cols+1);
   int x =dx-w/2+h;
   int dy=(int)(1.5*h);
   int y =(m_legend_alignment==ALIGNMENT_BOTTOM) ? height-dy*(rows+1) : -h/2;
   int i;
   if(m_data_total==m_index_size)
     {
      for(i=0;i<(int)m_data_total;i++,x+=dx)
        {
         if(i%cols==0)
           {
            x=dx-w/2+h;
            y+=dy;
           }
         FillRectangle(x,y,x+h,y+h,(uint)m_colors[i]);
         TextOut(x+h,y," - "+m_descriptors[i],m_color_text);
        }
      }
    else
      {
      for(i=0;i<(int)m_index_size;i++,x+=dx)
        {
         int index=m_index[i];
         if(i%cols==0)
           {
            x=dx-w/2+h;
            y+=dy;
           }
         FillRectangle(x,y,x+h,y+h,(uint)m_colors[index]);
         TextOut(x+h,y," - "+m_descriptors[index],m_color_text);
        }
      if(i%cols==0)
        {
         x=dx-w/2+h;
         y+=dy;
        }
      FillRectangle(x,y,x+h,y+h,COLOR2RGB(clrBlack));
      TextOut(x+h,y," - Others",m_color_text);
      }
//--- height
   return(dy*(rows+1));
  }
//+------------------------------------------------------------------+
//| Calculates coordinates of scales                                 |
//+------------------------------------------------------------------+
void CChartCanvas::CalcScales(void)
  {
   int width =m_data_area.Width();
   int height=m_data_area.Height();
//--- limits
   m_y_max=m_data_area.top+DrawScaleTop(false);
   m_y_min=m_data_area.bottom-DrawScaleBottom(false);
//--- additional
   m_dy_grid=(int)((m_y_min-m_y_max)/m_num_grid);
   m_y_max+=(int)(((m_y_min-m_y_max)-m_dy_grid*m_num_grid)/2);
   m_y_min=(int)(m_y_max+m_dy_grid*m_num_grid);
//--- normalize
   if(m_v_scale_min>=0.0)
      m_y_0=m_y_min;
   else
     {
      if(m_v_scale_max<=0.0)
         m_y_0=m_y_max;
      else
         m_y_0=(int)(m_y_max+(m_y_min-m_y_max)*m_v_scale_max/(m_v_scale_max-m_v_scale_min));
     }
//--- scale
   m_scale_y=(m_v_scale_max!=m_v_scale_min) ? (m_y_min-m_y_max)/(m_v_scale_max-m_v_scale_min) : 1;
//--- labels on scale
   if(ArraySize(m_scale_text)!=m_num_grid+1 && ArrayResize(m_scale_text,m_num_grid+1)==-1)
      return;
   double val=m_v_scale_min;
   double dval=(m_v_scale_max-m_v_scale_min)/m_num_grid;
   for(uint i=0;i<=m_num_grid;i++,val+=dval)
      m_scale_text[i]=DoubleToString(val,m_scale_digits);
  }
//+------------------------------------------------------------------+
//| Redraws scales                                                   |
//+------------------------------------------------------------------+
void CChartCanvas::DrawScales(void)
  {
//--- recalculate
   CalcScales();
//--- redraw scales
   if(IS_SHOW_SCALE_LEFT)
      DrawScaleLeft();
   if(IS_SHOW_SCALE_RIGHT)
      DrawScaleRight();
   if(IS_SHOW_SCALE_TOP)
      DrawScaleTop();
   if(IS_SHOW_SCALE_BOTTOM)
      DrawScaleBottom();
  }
//+------------------------------------------------------------------+
//| Redraws left scale                                               |
//+------------------------------------------------------------------+
int CChartCanvas::DrawScaleLeft(const bool draw)
  {
//--- check flag
   if(!IS_SHOW_SCALE_LEFT)
      return(0);
//--- variables
   int x1=m_data_area.left;
   int x2;
   int y=m_y_min;
//--- calculate scale width
   int size=0;
   for(uint i=0;i<=m_num_grid;i++)
     {
      if(size<TextWidth(m_scale_text[i]))
         size=TextWidth(m_scale_text[i]);
     }
//--- add indent and graduation mark (for now 5 pixels)
   size+=5+5;
//--- draw
   if(draw)
     {
      x2=x1+size;
      x1=x2-5;
      //--- draw line
      Line(x2,y,x2,m_y_max,m_color_text);
      //--- set font
      FontAngleSet(0);
      //--- draw text
      for(uint i=0;i<=m_num_grid;i++,y-=m_dy_grid)
        {
         Line(x1,y,x2,y,m_color_text);
         if((int)i<ArraySize(m_scale_text))
            TextOut(x1-5,y,m_scale_text[i],m_color_text,TA_RIGHT|TA_VCENTER);
        }
      //--- adjust data area
      m_data_area.left+=size;
     }
//--- return width
   return(size);
  }
//+------------------------------------------------------------------+
//| Redraws right scale                                              |
//+------------------------------------------------------------------+
int CChartCanvas::DrawScaleRight(const bool draw)
  {
//--- check flag
   if(!IS_SHOW_SCALE_RIGHT)
      return(0);
//--- variables
   int x1;
   int x2=m_data_area.right;
   int y =m_y_min;
//--- calculate scale width
   int size=0;
   for(uint i=0;i<=m_num_grid;i++)
     {
      if(size<TextWidth(m_scale_text[i]))
         size=TextWidth(m_scale_text[i]);
     }
//--- add indent and graduation mark (for now 5 pixels)
   size+=5+5;
//--- draw
   if(draw)
     {
      x1=x2-size;
      x2=x1+5;
      //--- draw line
      Line(x1,y,x1,m_y_max,m_color_text);
      //--- set font
      FontAngleSet(0);
      //--- draw text
      for(uint i=0;i<=m_num_grid;i++,y-=m_dy_grid)
        {
         Line(x1,y,x2,y,m_color_text);
         if((int)i<ArraySize(m_scale_text))
            TextOut(x2+5,y,m_scale_text[i],m_color_text,TA_LEFT|TA_VCENTER);
        }
      //--- adjust data area
      m_data_area.right-=size;
     }
//--- return widht
   return(size);
  }
//+------------------------------------------------------------------+
//| Redraws top scale                                                |
//+------------------------------------------------------------------+
int CChartCanvas::DrawScaleTop(const bool draw)
  {
   int size=0;
//--- check flag
   if(!IS_SHOW_SCALE_TOP)
      return(0);
//--- draw
   if(draw)
     {
      //--- draw line
      Line(m_data_area.left,m_y_max,m_data_area.right,m_y_max,m_color_text);
     }
//--- return height
   return(size);
  }
//+------------------------------------------------------------------+
//| Redraws bottom scale                                             |
//+------------------------------------------------------------------+
int CChartCanvas::DrawScaleBottom(const bool draw)
  {
   int size=0;
//--- check flag
   if(!IS_SHOW_SCALE_BOTTOM)
      return(0);
//--- draw
   if(draw)
     {
      //--- draw line
      Line(m_data_area.left,m_y_min,m_data_area.right,m_y_min,m_color_text);
     }
//--- return height
   return(size);
  }
//+------------------------------------------------------------------+
//| Redraws grid                                                     |
//+------------------------------------------------------------------+
void CChartCanvas::DrawGrid(void)
  {
//--- check flag
   if(!IS_SHOW_GRID)
      return;
//--- variables
   int x1=m_data_area.left;
   int x2=m_data_area.right;
   int y =m_y_min;
//--- draw
   uint j=m_num_grid-((IS_SHOW_SCALE_TOP) ? 1 : 0);
   if(IS_SHOW_SCALE_BOTTOM)
     {
      y-=m_dy_grid;
      j--;
     }
   for(uint i=0;i<=j;i++,y-=m_dy_grid)
      LineAA(x1,y,x2,y,m_color_grid,STYLE_DASH);
  }
//+------------------------------------------------------------------+
//| Redraws data                                                     |
//+------------------------------------------------------------------+
void CChartCanvas::DrawChart(void)
  {
   for(uint i=0;i<m_data_total;i++)
      DrawData(i);
  }
//+------------------------------------------------------------------+
