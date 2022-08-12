//+------------------------------------------------------------------+  
//| Morlet wavelet function                                          |  
//+------------------------------------------------------------------+  
float Morlet(const float t)
  {
   return exp(-t*t*0.5)*cos(M_2_PI*t);
  }
//+------------------------------------------------------------------+  
//| OpenCL kernel function                                           |  
//+------------------------------------------------------------------+  
__kernel void Wavelet_GPU(__global float *data,int datacount,int x_size,int y_size,__global float *result)
  {
   size_t i = get_global_id(0);
   size_t j = get_global_id(1);
   float a1=(float)10e-10;
   float a2=(float)15.0;
   float da=(a2-a1)/(float)y_size;
   float db=((float)datacount-(float)0.0)/x_size;
   float a=a1+j*da;
   float b=0+i*db;
   uint norm=1;
   float B=(float)1.0; //Morlet                                         
   float B_inv=(float)1.0/B;
   float a_inv=(float)1.0/a;
   float dt=(float)1.0;
   float coef=(float)0.0;
   if(norm==0)
      coef=sqrt(a_inv);
   else
     {
      for(int k=0; k<datacount; k++)
        {
         float arg=(dt*k-b)*a_inv;
         arg=-B_inv*arg*arg;
         coef=coef+exp(arg);
        }
     }
   float sum=(float)0.0;
   for(int k=0; k<datacount; k++)
     {
      float arg=(dt*k-b)*a_inv;
      sum+=data[k]*Morlet(arg);
     }
   sum=sum/coef;
   uint pos=(int)(j*x_size+i);
   result[pos]=sum;
  };
//+------------------------------------------------------------------+
