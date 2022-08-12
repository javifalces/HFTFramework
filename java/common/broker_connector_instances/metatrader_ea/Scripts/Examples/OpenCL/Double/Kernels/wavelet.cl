//--- by default some GPU doesn't support doubles
//--- cl_khr_fp64 directive is used to enable work with doubles
#pragma OPENCL EXTENSION cl_khr_fp64 : enable
//+------------------------------------------------------------------+  
//| Morlet wavelet function                                          |  
//+------------------------------------------------------------------+  
double Morlet(const double t)
  {
   return exp(-t*t*0.5)*cos(M_2_PI*t);
  }
//+------------------------------------------------------------------+  
//| OpenCL kernel function                                           |  
//+------------------------------------------------------------------+  
__kernel void Wavelet_GPU(__global double *data,int datacount,int x_size,int y_size,__global double *result)
  {
   size_t i = get_global_id(0);
   size_t j = get_global_id(1);
   double a1=(double)10e-10;
   double a2=(double)15.0;
   double da=(a2-a1)/(double)y_size;
   double db=((double)datacount-(double)0.0)/x_size;
   double a=a1+j*da;
   double b=0+i*db;
   uint norm=1;
   double B=(double)1.0; //Morlet                                         
   double B_inv=(double)1.0/B;
   double a_inv=(double)1.0/a;
   double dt=(double)1.0;
   double coef=(double)0.0;
   if(norm==0)
      coef=sqrt(a_inv);
   else
     {
      for(int k=0; k<datacount; k++)
        {
         double arg=(dt*k-b)*a_inv;
         arg=-B_inv*arg*arg;
         coef=coef+exp(arg);
        }
     }
   double sum=(float)0.0;
   for(int k=0; k<datacount; k++)
     {
      double arg=(dt*k-b)*a_inv;
      sum+=data[k]*Morlet(arg);
     }
   sum=sum/coef;
   uint pos=(int)(j*x_size+i);
   result[pos]=sum;
  };
//+------------------------------------------------------------------+
