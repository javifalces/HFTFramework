//+------------------------------------------------------------------+
//| fft_init OpenCL kernel for Fast Fourier Transfrom                |
//+------------------------------------------------------------------+
//| Matthew Scarpino, "OpenCL in Action: How to accelerate graphics  |
//| and computations", Manning, 2012, Chapter 14.                    |
//+------------------------------------------------------------------+
__kernel void fft_init(__global float2 *in_data,
                       __global float2 *out_data,
                       __local float2 *l_data,
                       uint points_per_group,uint size,int dir)
  {
   uint4 br,index;
   uint points_per_item,g_addr,l_addr,i,fft_index,stage,N2;
   float2 x1,x2,x3,x4,sum12,diff12,sum34,diff34;

   points_per_item=points_per_group/get_local_size(0);
   l_addr = get_local_id(0)*points_per_item;
   g_addr = get_group_id(0)*points_per_group + l_addr;
//--- load data from bit-reversed addresses and perform 4-point FFTs
   for(i=0; i<points_per_item; i+=4)
     {
      index=(uint4)(g_addr,g_addr+1,g_addr+2,g_addr+3);
      fft_index=size/2;
      stage=1;
      N2 =(uint)log2((float)size)-1;
      br =(index<< N2) & fft_index;
      br|=(index>> N2) & stage;
      //--- bit-reverse addresses
      while(N2>1)
        {
         N2-=2;
         fft_index>>=1;
         stage<<=1;
         br |= (index << N2) & fft_index;
         br |= (index >> N2) & stage;
        }
      //--- load global data
      x1 = in_data[br.s0];
      x2 = in_data[br.s1];
      x3 = in_data[br.s2];
      x4 = in_data[br.s3];

      sum12=x1+x2;
      diff12= x1-x2;
      sum34 = x3+x4;
      diff34=(float2)(x3.s1-x4.s1,x4.s0-x3.s0)*dir;
      l_data[l_addr]=sum12+sum34;
      l_data[l_addr+1] = diff12 + diff34;
      l_data[l_addr+2] = sum12 - sum34;
      l_data[l_addr+3] = diff12 - diff34;
      l_addr += 4;
      g_addr += 4;
     }
//--- perform initial stages of the FFT - each of length N2*2
   for(N2=4; N2<points_per_item; N2<<=1)
     {
      l_addr=get_local_id(0)*points_per_item;
      for(fft_index=0; fft_index<points_per_item; fft_index+=2*N2)
        {
         x1=l_data[l_addr];
         l_data[l_addr]+=l_data[l_addr+N2];
         l_data[l_addr+N2]=x1-l_data[l_addr+N2];
         for(i=1; i<N2; i++)
           {
            x3.s0=cos(M_PI_F*i/N2);
            x3.s1=dir*sin(M_PI_F*i/N2);
            x2=(float2)(l_data[l_addr+N2+i].s0*x3.s0+l_data[l_addr+N2+i].s1*x3.s1,l_data[l_addr+N2+i].s1*x3.s0-l_data[l_addr+N2+i].s0*x3.s1);
            l_data[l_addr+N2+i]=l_data[l_addr+i]-x2;
            l_data[l_addr+i]+=x2;
           }
         l_addr+=2*N2;
        }
     }
   barrier(CLK_LOCAL_MEM_FENCE);
//--- perform FFT with other items in group - each of length N2*2
   stage=2;
   for(N2=points_per_item; N2<points_per_group; N2<<=1)
     {
      br.s0=(get_local_id(0)+(get_local_id(0)/stage)*stage) *(points_per_item/2);
      size = br.s0 % (N2*2);
      for(i=br.s0; i<br.s0+points_per_item/2; i++)
        {
         x3.s0=cos(M_PI_F*size/N2);
         x3.s1=dir*sin(M_PI_F*size/N2);
         x2=(float2)(l_data[N2+i].s0*x3.s0+l_data[N2+i].s1*x3.s1,l_data[N2+i].s1*x3.s0-l_data[N2+i].s0*x3.s1);
         l_data[N2+i]=l_data[i]-x2;
         l_data[i]+=x2;
         size++;
        }
      stage<<=1;
      barrier(CLK_LOCAL_MEM_FENCE);
     }
//--- store results in global memory
   l_addr = get_local_id(0)*points_per_item;
   g_addr = get_group_id(0)*points_per_group + l_addr;
   for(i=0; i<points_per_item; i+=4)
     {
      out_data[g_addr]=l_data[l_addr];
      out_data[g_addr+1] = l_data[l_addr+1];
      out_data[g_addr+2] = l_data[l_addr+2];
      out_data[g_addr+3] = l_data[l_addr+3];
      g_addr += 4;
      l_addr += 4;
     }
  }
//+------------------------------------------------------------------+
//| fft_stage OpenCL kernel for Fast Fourier Transfrom               |
//+------------------------------------------------------------------+
//| Matthew Scarpino, "OpenCL in Action: How to accelerate graphics  |
//| and computations", Manning, 2012, Chapter 14.                    |
//+------------------------------------------------------------------+
__kernel void fft_stage(__global float2 *g_data,uint stage,uint points_per_group,int dir)
  {
   uint points_per_item,addr,N,ang,i;
   float c,s;
   float2 input1,input2,w;

   points_per_item=points_per_group/get_local_size(0);
   addr=(get_group_id(0)+(get_group_id(0)/stage)*stage)*(points_per_group/2)+get_local_id(0)*(points_per_item/2);
   N=points_per_group*(stage/2);
   ang=addr%(N*2);

   for(i=addr; i<addr+points_per_item/2; i++)
     {
      c = cos(M_PI_F*ang/N);
      s = dir*sin(M_PI_F*ang/N);
      input1 = g_data[i];
      input2 = g_data[i+N];
      w=(float2)(input2.s0*c+input2.s1*s,input2.s1*c-input2.s0*s);
      g_data[i]=input1+w;
      g_data[i+N]=input1-w;
      ang++;
     }
  }
//+------------------------------------------------------------------+
//| fft_scale OpenCL kernel for Fast Fourier Transfrom               |
//+------------------------------------------------------------------+
//| Matthew Scarpino, "OpenCL in Action: How to accelerate graphics  |
//| and computations", Manning, 2012, Chapter 14.                    |
//+------------------------------------------------------------------+
__kernel void fft_scale(__global float2 *g_data,uint points_per_group,uint scale)
  {
   uint points_per_item,addr,i;

   points_per_item=points_per_group/get_local_size(0);
   addr=get_group_id(0)*points_per_group+get_local_id(0)*points_per_item;

   for(i=addr; i<addr+points_per_item; i++)
     {
      g_data[i]/=scale;
     }
  }
//+------------------------------------------------------------------+
