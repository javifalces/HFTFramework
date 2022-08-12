//+------------------------------------------------------------------+
//|                                                        Curve.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
#include <Canvas\Canvas.mqh>
//--- forward declaration
class CGraphic;
//--- fucntion for represent custom plot method
typedef void(*PlotFucntion)(double &x[],double &y[],int size,CGraphic *graphic,CCanvas *canvas,void *cbdata);
//--- function for represent curve
typedef double(*CurveFunction)(double);
//--- drawing type
enum ENUM_CURVE_TYPE
  {
   CURVE_POINTS,
   CURVE_LINES,
   CURVE_POINTS_AND_LINES,
   CURVE_STEPS,
   CURVE_HISTOGRAM,
   CURVE_CUSTOM,
   CURVE_NONE
  };
//--- type for the various point shapes that are available 
enum ENUM_POINT_TYPE
  {
   POINT_CIRCLE,
   POINT_SQUARE,
   POINT_DIAMOND,
   POINT_TRIANGLE,
   POINT_TRIANGLE_DOWN,
   POINT_X_CROSS,
   POINT_PLUS,
   POINT_STAR,
   POINT_HORIZONTAL_DASH,
   POINT_VERTICAL_DASH
  };
//+------------------------------------------------------------------+
//| Structure CPoint2D                                               |
//| Usage: 2d point on graphic in Cartesian coordinates              |
//+------------------------------------------------------------------+
struct CPoint2D
  {
   double            x;
   double            y;
  };
//+------------------------------------------------------------------+
//| Class CCurve                                                     |
//| Usage: class to represent the one-dimensional curve              |
//+------------------------------------------------------------------+
class CCurve : public CObject
  {
private:
   uint              m_clr;
   double            m_x[];
   double            m_y[];
   double            m_xmin;
   double            m_xmax;
   double            m_ymin;
   double            m_ymax;
   int               m_size;
   ENUM_CURVE_TYPE   m_type;
   string            m_name;
   //--- lines
   ENUM_LINE_STYLE   m_lines_style;
   ENUM_LINE_END     m_lines_end_style;
   int               m_lines_width;
   bool              m_lines_smooth;
   double            m_lines_tension;
   double            m_lines_step;
   //--- points
   int               m_points_size;
   ENUM_POINT_TYPE   m_points_type;
   bool              m_points_fill;
   uint              m_points_clr;
   //--- steps
   int               m_steps_dimension;
   //--- histogram
   int               m_hisogram_width;
   //--- custom
   PlotFucntion      m_custom_plot_func;
   void             *m_custom_plot_cbdata;
   //--- general property
   bool              m_visible;
   //--- trend line property
   uint              m_trend_clr;
   bool              m_trend_visible;

protected:
   bool              m_trend_calc;
   double            m_trend_coeff[];

public:
                     CCurve(const double &y[],const uint clr,ENUM_CURVE_TYPE type,const string name);
                     CCurve(const double &x[],const double &y[],const uint clr,ENUM_CURVE_TYPE type,const string name);
                     CCurve(const CPoint2D &points[],const uint clr,ENUM_CURVE_TYPE type,const string name);
                     CCurve(CurveFunction function,const double from,const double to,const double step,const uint clr,ENUM_CURVE_TYPE type,const string name);
                    ~CCurve(void);
   //--- gets the general properties
   void              GetX(double &x[]) const { ArrayCopy(x,m_x); }
   void              GetY(double &y[]) const { ArrayCopy(y,m_y); }
   double            XMax(void)        const { return(m_xmax);   }
   double            XMin(void)        const { return(m_xmin);   }
   double            YMax(void)        const { return(m_ymax);   }
   double            YMin(void)        const { return(m_ymin);   }
   int               Size(void)        const { return(m_size);   }
   //--- update                     
   void              Update(const double &y[]);
   void              Update(const double &x[],const double &y[]);
   void              Update(const CPoint2D &points[]);
   void              Update(CurveFunction function,const double from,const double to,const double step);
   //--- gets or sets general options
   uint              Color(void)                 const { return(m_clr);      }
   int               Type(void)                  const { return(m_type);     }
   string            Name(void)                  const { return(m_name);     }
   bool              Visible(void)               const { return(m_visible);  }
   void              Color(const uint clr)       { m_clr=clr;                    }
   void              Type(const int type)        { m_type=(ENUM_CURVE_TYPE)type; }
   void              Name(const string name)     { m_name=name;                  }
   void              Visible(const bool visible) { m_visible=visible;            }
   //--- gets or sets the lines properties
   ENUM_LINE_STYLE   LinesStyle(void)                         const { return(m_lines_style);     }
   ENUM_LINE_END     LinesEndStyle(void)                      const { return(m_lines_end_style); }
   int               LinesWidth(void)                         const { return(m_lines_width);     }
   bool              LinesSmooth(void)                        const { return(m_lines_smooth);    }
   double            LinesSmoothTension(void)                 const { return(m_lines_tension);   }
   double            LinesSmoothStep(void)                    const { return(m_lines_step);      }
   void              LinesStyle(ENUM_LINE_STYLE style)        { m_lines_style=style;         }
   void              LinesEndStyle(ENUM_LINE_END end_style)   { m_lines_end_style=end_style; }
   void              LinesWidth(const int width)              { m_lines_width=width;         }
   void              LinesSmooth(const bool smooth)           { m_lines_smooth=smooth;       }
   void              LinesSmoothTension(const double tension) { m_lines_tension=tension;     }
   void              LinesSmoothStep(const double step)       { m_lines_step=step;           }
   //--- gets or sets the points properties
   int               PointsSize(void)                 const { return(m_points_size); }
   ENUM_POINT_TYPE   PointsType(void)                 const { return(m_points_type); }
   bool              PointsFill(void)                 const { return(m_points_fill); }
   uint              PointsColor(void)                const { return(m_points_clr);  }
   void              PointsSize(const int size)       { m_points_size=size; }
   void              PointsType(ENUM_POINT_TYPE type) { m_points_type=type; }
   void              PointsFill(const bool fill)      { m_points_fill=fill; }
   void              PointsColor(const uint clr)      { m_points_clr=clr;   }
   //--- gets or sets the steps properties
   int               StepsDimension(void) const { return(m_steps_dimension); }
   void              StepsDimension(const int dimension) { m_steps_dimension=dimension; }
   //--- gets or sets the histogram properties
   int               HistogramWidth(void) const { return(m_hisogram_width); }
   void              HistogramWidth(const int width) { m_hisogram_width=width; }
   //--- gets or sets the custom properties
   PlotFucntion      CustomPlotFunction(void)              const { return(m_custom_plot_func);   }
   void             *CustomPlotCBData(void)                const { return(m_custom_plot_cbdata); }
   void              CustomPlotFunction(PlotFucntion func) { m_custom_plot_func=func;     }
   void              CustomPlotCBData(void *cbdata)        { m_custom_plot_cbdata=cbdata; }
   //--- gets or sets the trend line properties
   bool              TrendLineVisible(void)               const { return(m_trend_visible); }
   uint              TrendLineColor(void)                 const { return(m_trend_clr);     }
   void              TrendLineVisible(const bool visible) { m_trend_visible=visible; }
   void              TrendLineColor(const uint clr)       { m_trend_clr=clr;         }
   void              TrendLineCoefficients(double &coefficients[]);

protected:
   virtual void      CalculateCoefficients(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CCurve::CCurve(const double &y[],const uint clr,ENUM_CURVE_TYPE type,const string name)
   : m_name(name),
     m_clr(clr),
     m_type(type),
     m_visible(false),
     m_lines_style(STYLE_SOLID),
     m_lines_end_style(LINE_END_ROUND),
     m_lines_width(1),
     m_lines_smooth(false),
     m_lines_tension(0.5),
     m_lines_step(1.0),
     m_points_size(6),
     m_points_type(POINT_CIRCLE),
     m_points_fill(false),
     m_points_clr(clr),
     m_steps_dimension(0),
     m_hisogram_width(1),
     m_custom_plot_func(NULL),
     m_custom_plot_cbdata(NULL),
     m_trend_visible(false),
     m_trend_clr(clr),
     m_trend_calc(false)
  {
//--- keep y array
   m_size=ArraySize(y);
   ArrayResize(m_x,m_size);
   ArrayCopy(m_y,y);
   m_xmax = m_size-1;
   m_xmin = 0.0;
   m_ymax = 0.0;
   m_ymin = 0.0;
   bool yvalid=false;
//--- find min and max values 
   for(int i=0; i<m_size; i++)
     {
      m_x[i]=i;
      if(MathIsValidNumber(m_y[i]))
        {
         if(!yvalid)
           {
            m_ymax=m_y[i];
            m_ymin= m_y[i];
            yvalid=true;
           }
         else
           {
            //--- find max and min of y
            if(m_ymax<y[i])
               m_ymax=y[i];
            else
            if(m_ymin>y[i])
               m_ymin=y[i];
           }
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CCurve::CCurve(const double &x[],const double &y[],const uint clr,ENUM_CURVE_TYPE type,const string name)
   : m_name(name),
     m_clr(clr),
     m_type(type),
     m_visible(false),
     m_lines_style(STYLE_SOLID),
     m_lines_end_style(LINE_END_ROUND),
     m_lines_width(1),
     m_lines_smooth(false),
     m_lines_tension(0.5),
     m_lines_step(1.0),
     m_points_size(6),
     m_points_type(POINT_CIRCLE),
     m_points_fill(false),
     m_points_clr(clr),
     m_steps_dimension(0),
     m_hisogram_width(1),
     m_custom_plot_func(NULL),
     m_custom_plot_cbdata(NULL),
     m_trend_visible(false),
     m_trend_clr(clr),
     m_trend_calc(false)
  {
//--- keep x and y array
   ArrayCopy(m_x,x);
   ArrayCopy(m_y,y);
   m_size = ArraySize(x);
   m_xmax = 0.0;
   m_xmin = 0.0;
   m_ymax = 0.0;
   m_ymin = 0.0;
   bool yvalid=false;
   bool xvalid=false;
//--- find min and max values 
   for(int i=0; i<m_size; i++)
     {
      if(MathIsValidNumber(m_x[i]))
        {
         if(!xvalid)
           {
            m_xmax = x[i];
            m_xmin = x[i];
            xvalid=true;
           }
         else
           {
            //--- find max and min of x
            if(m_xmax<x[i])
               m_xmax=x[i];
            else
            if(m_xmin>x[i])
               m_xmin=x[i];
           }
        }
      if(MathIsValidNumber(m_y[i]))
        {
         if(!yvalid)
           {
            m_ymax = y[i];
            m_ymin = y[i];
            yvalid=true;
           }
         else
           {
            //--- find max and min of y
            if(m_ymax<y[i])
               m_ymax=y[i];
            else
            if(m_ymin>y[i])
               m_ymin=y[i];
           }
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CCurve::CCurve(const CPoint2D &points[],const uint clr,ENUM_CURVE_TYPE type,const string name)
   : m_name(name),
     m_clr(clr),
     m_type(type),
     m_visible(false),
     m_lines_style(STYLE_SOLID),
     m_lines_end_style(LINE_END_ROUND),
     m_lines_width(1),
     m_lines_smooth(false),
     m_lines_tension(0.5),
     m_lines_step(1.0),
     m_points_size(6),
     m_points_type(POINT_CIRCLE),
     m_points_fill(false),
     m_points_clr(clr),
     m_steps_dimension(0),
     m_hisogram_width(1),
     m_custom_plot_func(NULL),
     m_custom_plot_cbdata(NULL),
     m_trend_visible(false),
     m_trend_clr(clr),
     m_trend_calc(false)
  {
//--- preliminary calculation
   m_size=ArraySize(points);
   ArrayResize(m_x,m_size);
   ArrayResize(m_y,m_size);
   m_xmax = 0.0;
   m_xmin = 0.0;
   m_ymax = 0.0;
   m_ymin = 0.0;
   bool xvalid=false;
   bool yvalid=false;
//--- keep x and y array
   for(int i=0; i<m_size; i++)
     {
      m_x[i] = points[i].x;
      m_y[i] = points[i].y;
      if(MathIsValidNumber(m_x[i]))
        {
         if(!xvalid)
           {
            m_xmax = m_x[i];
            m_xmin = m_x[i];
            xvalid=true;
           }
         else
           {
            //--- find max and min of x
            if(m_xmax<m_x[i])
               m_xmax=m_x[i];
            else
            if(m_xmin>m_x[i])
               m_xmin=m_x[i];
           }
        }
      if(MathIsValidNumber(m_y[i]))
        {
         if(!yvalid)
           {
            m_ymax = m_y[i];
            m_ymin = m_y[i];
            yvalid=true;
           }
         else
           {
            //--- find max and min of y
            if(m_ymax<m_y[i])
               m_ymax=m_y[i];
            else
            if(m_ymin>m_y[i])
               m_ymin=m_y[i];
           }
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CCurve::CCurve(CurveFunction function,const double from,const double to,const double step,const uint clr,ENUM_CURVE_TYPE type,const string name)
   : m_name(name),
     m_clr(clr),
     m_type(type),
     m_visible(false),
     m_lines_style(STYLE_SOLID),
     m_lines_end_style(LINE_END_ROUND),
     m_lines_width(1),
     m_lines_smooth(false),
     m_lines_tension(0.5),
     m_lines_step(1.0),
     m_points_size(6),
     m_points_type(POINT_CIRCLE),
     m_points_fill(false),
     m_points_clr(clr),
     m_steps_dimension(0),
     m_hisogram_width(1),
     m_custom_plot_func(NULL),
     m_custom_plot_cbdata(NULL),
     m_trend_visible(false),
     m_trend_clr(clr),
     m_trend_calc(false)
  {
//--- preliminary calculation
   m_size=(int)((to-from)/step)+1;
   ArrayResize(m_x,m_size);
   ArrayResize(m_y,m_size);
   m_xmax = to;
   m_xmin = from;
   m_ymax = 0.0;
   m_ymin = 0.0;
   bool yvalid=false;
//--- keep x and y array
   for(int i=0; i<m_size; i++)
     {
      m_x[i]=from+(i*step);
      m_y[i]=function(m_x[i]);
      if(MathIsValidNumber(m_y[i]))
        {
         if(!yvalid)
           {
            m_ymax=m_y[i];
            m_ymin=m_y[i];
            yvalid=true;
           }
         else
           {
            //--- find max and min of y
            if(m_ymax<m_y[i])
               m_ymax=m_y[i];
            else if(m_ymin>m_y[i])
               m_ymin=m_y[i];
           }
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CCurve::~CCurve(void)
  {
  }
//+------------------------------------------------------------------+
//| Update x and y coordinates of curve                              |
//+------------------------------------------------------------------+
void CCurve::Update(const double &y[])
  {
   m_trend_calc=false;
   ArrayFree(m_y);
   int size=ArraySize(y);
//--- keep y array
   if(m_size!=size)
     {
      m_size=size;
      ArrayResize(m_x,m_size);
     }
   ArrayCopy(m_y,y);
   m_xmax = m_size-1;
   m_xmin = 0.0;
   m_ymax = 0.0;
   m_ymin = 0.0;
   bool yvalid=false;
//--- find min and max values 
   for(int i=0; i<m_size; i++)
     {
      m_x[i]=i;
      if(MathIsValidNumber(m_y[i]))
        {
         if(!yvalid)
           {
            m_ymax=m_y[i];
            m_ymin= m_y[i];
            yvalid=true;
           }
         else
           {
            //--- find max and min of y
            if(m_ymax<y[i])
               m_ymax=y[i];
            else
            if(m_ymin>y[i])
               m_ymin=y[i];
           }
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Update x and y coordinates of curve                              |
//+------------------------------------------------------------------+
void CCurve::Update(const double &x[],const double &y[])
  {
   m_trend_calc=false;
   ArrayFree(m_x);
   ArrayFree(m_y);
//--- keep x and y array
   ArrayCopy(m_x,x);
   ArrayCopy(m_y,y);
   m_size = ArraySize(x);
   m_xmax = 0.0;
   m_xmin = 0.0;
   m_ymax = 0.0;
   m_ymin = 0.0;
   bool yvalid=false;
   bool xvalid=false;
//--- find min and max values 
   for(int i=0; i<m_size; i++)
     {
      if(MathIsValidNumber(m_x[i]))
        {
         if(!xvalid)
           {
            m_xmax = x[i];
            m_xmin = x[i];
            xvalid=true;
           }
         else
           {
            //--- find max and min of x
            if(m_xmax<x[i])
               m_xmax=x[i];
            else
            if(m_xmin>x[i])
               m_xmin=x[i];
           }
        }
      if(MathIsValidNumber(m_y[i]))
        {
         if(!yvalid)
           {
            m_ymax = y[i];
            m_ymin = y[i];
            yvalid=true;
           }
         else
           {
            //--- find max and min of y
            if(m_ymax<y[i])
               m_ymax=y[i];
            else
            if(m_ymin>y[i])
               m_ymin=y[i];
           }
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Update x and y coordinates of curve                              |
//+------------------------------------------------------------------+
void CCurve::Update(const CPoint2D &points[])
  {
   m_trend_calc=false;
   int size=ArraySize(points);
//--- preliminary calculation
   if(size!=m_size)
     {
      m_size=size;
      ArrayResize(m_x,m_size);
      ArrayResize(m_y,m_size);
     }
   m_xmax = 0.0;
   m_xmin = 0.0;
   m_ymax = 0.0;
   m_ymin = 0.0;
   bool xvalid=false;
   bool yvalid=false;
//--- keep x and y array
   for(int i=0; i<m_size; i++)
     {
      m_x[i] = points[i].x;
      m_y[i] = points[i].y;
      if(MathIsValidNumber(m_x[i]))
        {
         if(!xvalid)
           {
            m_xmax = m_x[i];
            m_xmin = m_x[i];
            xvalid=true;
           }
         else
           {
            //--- find max and min of x
            if(m_xmax<m_x[i])
               m_xmax=m_x[i];
            else
            if(m_xmin>m_x[i])
               m_xmin=m_x[i];
           }
        }
      if(MathIsValidNumber(m_y[i]))
        {
         if(!yvalid)
           {
            m_ymax = m_y[i];
            m_ymin = m_y[i];
            yvalid=true;
           }
         else
           {
            //--- find max and min of y
            if(m_ymax<m_y[i])
               m_ymax=m_y[i];
            else
            if(m_ymin>m_y[i])
               m_ymin=m_y[i];
           }
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Update x and y coordinates of curve                              |
//+------------------------------------------------------------------+
void CCurve::Update(CurveFunction function,const double from,const double to,const double step)
  {
   m_trend_calc=false;
   int size=(int)((to-from)/step)+1;
//--- preliminary calculation
   if(size!=m_size)
     {
      m_size=size;
      ArrayResize(m_x,m_size);
      ArrayResize(m_y,m_size);
     }
   m_xmax = to;
   m_xmin = from;
   m_ymax = 0.0;
   m_ymin = 0.0;
   bool yvalid=false;
//--- keep x and y array
   for(int i=0; i<m_size; i++)
     {
      m_x[i]=from+(i*step);
      m_y[i]=function(m_x[i]);
      if(MathIsValidNumber(m_y[i]))
        {
         if(!yvalid)
           {
            m_ymax=m_y[i];
            m_ymin=m_y[i];
            yvalid=true;
           }
         else
           {
            //--- find max and min of y
            if(m_ymax<m_y[i])
               m_ymax=m_y[i];
            else if(m_ymin>m_y[i])
               m_ymin=m_y[i];
           }
        }
     }
//---
  }
//+------------------------------------------------------------------+
//| Gets the coefficients for trend line                             |
//+------------------------------------------------------------------+
void CCurve::TrendLineCoefficients(double &coefficients[])
  {
   if(!m_trend_calc)
     {
      CalculateCoefficients();
      m_trend_calc=true;
     }
   ArrayCopy(coefficients,m_trend_coeff);
  }
//+------------------------------------------------------------------+
//| Calculate coefficients                                           |
//+------------------------------------------------------------------+
void CCurve::CalculateCoefficients(void)
  {
//--- simple linear resgression
   ArrayResize(m_trend_coeff,2);
   double xmean=0.0;
   double ymean=0.0;
   double sum_xy=0.0;
   double sum_xx=0.0;
//--- primary calculate
   for(int i=0; i<m_size; i++)
     {
      xmean+=m_x[i];
      ymean+=m_y[i];
      sum_xy+=m_x[i]*m_y[i];
      sum_xx+=m_x[i]*m_x[i];
     }
   xmean/=m_size;
   ymean/=m_size;
//--- calculate intercept
   m_trend_coeff[0]=(sum_xy -(m_size*xmean*ymean))/(sum_xx -(m_size*xmean*xmean));
//--- calculate slope
   m_trend_coeff[1]=ymean-m_trend_coeff[0]*xmean;
  }
//+------------------------------------------------------------------+
