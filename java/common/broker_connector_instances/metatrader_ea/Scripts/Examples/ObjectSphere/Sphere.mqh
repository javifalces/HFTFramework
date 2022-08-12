//+------------------------------------------------------------------+
//|                                                       Sphere.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Arrays\ArrayObj.mqh>
#include <ChartObjects\ChartObjectsTxtControls.mqh>
//+------------------------------------------------------------------+
//| Class CSphere.                                                   |
//| Appointment: Class of the graphical object "Sphere".             |
//+------------------------------------------------------------------+
class CSphere : public CArrayObj
  {
private:
   int               m_id;              // sphere idintifier
   color             m_color;           // sphere color
   int               m_num_parallel;    // number of parallels
   int               m_num_meridian;    // number of meridians
   int               m_radius;          // sphere radius in pixels
   int               m_center_X;        // coordinate of the center X
   int               m_center_Y;        // coordinate of the center Y
   CSphere          *m_orbite_center;   // "sun"
   double            m_orbite_radius;   // orbital radius
   double            m_orbite_fi_X;     // angle of inclination to the axis X
   double            m_orbite_fi_Y;     // angle of inclination to the axis Y
   double            m_orbite_fi_Z;     // angle of inclination to the axis Z
   double            m_d_fi_orb;        // angular velocity of the orbit
   //--- working variables
   double            m_fi_orb;
   double            m_fi_x;
   double            m_fi_y;
   double            m_fi_z;

public:
                     CSphere(void);
                    ~CSphere(void);
   //--- methods of access to protected data
   int               CenterX(void) const { return(m_center_X); }
   int               CenterY(void) const { return(m_center_Y); }
   //---
   bool              Create(const int id,const color c,const int x,const int y,const int r,const int p,const int m,const string str);
   void              SetOrbite(CSphere *sun,const double fi_x,const double fi_y,const double fi_z,const double d_fi_orb);
   void              Recalculate(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSphere::CSphere(void) : m_id(0),
                         m_color(0),
                         m_num_parallel(0),
                         m_num_meridian(0),
                         m_radius(0),
                         m_center_X(0),
                         m_center_Y(0),
                         m_orbite_center(NULL),
                         m_orbite_radius(0.0),
                         m_orbite_fi_X(0.0),
                         m_orbite_fi_Y(0.0),
                         m_orbite_fi_Z(0.0),
                         m_d_fi_orb(0.0),
                         m_fi_orb(0.0),
                         m_fi_x(0.0),
                         m_fi_y(0.0),
                         m_fi_z(0.0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSphere::~CSphere(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object                                                    |
//+------------------------------------------------------------------+
bool CSphere::Create(const int id,const color c,const int x,const int y,const int r,const int p,const int m,const string str)
  {
   CArrayObj *arr;
   CChartObjectLabel *label;
//---
   m_id=id;
   m_color       =c;
   m_num_parallel=(p>r/3) ? r/3 : p;
   m_num_meridian=(m>r/3) ? r/3 : m;
   m_radius      =r;
   m_center_X    =x;
   m_center_Y    =y;
   if(!Reserve(p)) return(false);
//--- loop parallels
   for(int i=0;i<p;i++)
     {
      arr=new CArrayObj;
      if(arr==NULL)
         return(false);
      if(!arr.Reserve(m))
         return(false);
      //--- loop meridians
      for(int j=0;j<m;j++)
        {
         label=new CChartObjectLabel;
         if(label==NULL)
            return(false);
         label.Create(0,"ar"+string(id)+"_"+string(j)+"_"+string(i),0,0,0);
         label.Color(m_color);
         label.Description(str);
         arr.Add(label);
        }
      Add(arr);
     }
   return(true);
  }
//+------------------------------------------------------------------+
//| Setting                                                          |
//+------------------------------------------------------------------+
void CSphere::SetOrbite(CSphere *sun,const double fi_x,const double fi_y,const double fi_z,const double d_fi_orb)
  {
   m_orbite_center=sun;
   m_orbite_fi_X=fi_x;
   m_orbite_fi_Y=fi_y;
   m_d_fi_orb=d_fi_orb;
   m_orbite_fi_Z=fi_z;
   m_orbite_radius=MathSqrt(MathPow(m_center_X-m_orbite_center.CenterX(),2)+MathPow(m_center_Y-m_orbite_center.CenterY(),2))/2;
  }
//+------------------------------------------------------------------+
//| Recalculation of the sphere.                                     |
//+------------------------------------------------------------------+
void CSphere::Recalculate(void)
  {
   CArrayObj         *arr;
   CChartObjectLabel *label;
//--- 
   double     d_fi_m,d_fi_p;
   double     x,y,z;
   int        i,q;
   double     idx3=0;
   double     idx4=0;
//---
   d_fi_m=2*M_PI/m_num_meridian;
   d_fi_p=M_PI/m_num_parallel;
//---
   idx3=idx4;
   if(m_orbite_center!=NULL)
     {
      //--- calculation of the coordinates of the dynamic center of the orbit
      m_fi_orb+=m_d_fi_orb;
      m_center_X=(int)(m_orbite_center.CenterX()+m_orbite_radius*(MathSin(m_fi_orb)+MathCos(m_orbite_fi_X)*MathSin(m_orbite_fi_Y)));
      m_center_Y=(int)(m_orbite_center.CenterY()+m_orbite_radius*(MathCos(m_fi_orb)+MathSin(m_orbite_fi_X)*MathCos(m_orbite_fi_Y)));
     }
   q=-m_num_parallel/2-1;
//--- loop parallels
   for(int j=0;j<m_num_parallel;j++)
     {
      q++;
      arr=At(j);
      //--- loop meridians
      for(i=0;i<m_num_meridian;i++)
        {
         //--- calculation of the coordinates of the point of excluding traffic
         x=m_radius*MathSin(d_fi_m*i)*MathCos(d_fi_p*q);
         y=m_radius*MathSin(d_fi_p*q);
         z=m_radius*MathCos(d_fi_m*i)*MathCos(d_fi_p*q);
         //--- recalculation of the coordinates of the point of view of traffic
         label=arr.At(i);
         label.X_Distance(m_center_X+x2d_(x,y,z,m_fi_x,m_fi_y,m_fi_z));
         label.Y_Distance(m_center_Y+y2d_(x,y,z,m_fi_x,m_fi_y,m_fi_z));
        }
      idx3=idx3+0.5*m_id;
     }
//---
   m_fi_z=m_fi_z+MathSin(idx3*0.08)/32+MathCos(idx3*0.16)*0.01;
   m_fi_x=m_fi_x+MathCos(idx3*0.08)*0.016+MathCos(idx3*0.12)*0.008;
  }
//+------------------------------------------------------------------+
//| auxiliary function                                               |
//+------------------------------------------------------------------+
int x2d_(double x_,double y_,double z_,double fi_x_,double fi_y_,double fi_z_)
  {
   return((int)(x_*MathCos(fi_z_)+y_*MathSin(fi_z_)));
  }
//+------------------------------------------------------------------+
//| auxiliary function                                               |
//+------------------------------------------------------------------+
int y2d_(double x_,double y_,double z_,double fi_x_,double fi_y_,double fi_z_)
  {
   return((int)((-x_*MathSin(fi_z_)+y_*MathCos(fi_z_))*MathCos(fi_x_)+z_*MathSin(fi_x_)));
  }
//+------------------------------------------------------------------+
