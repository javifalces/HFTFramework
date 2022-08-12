//--- by default some GPU doesn't support doubles
//--- cl_khr_fp64 directive is used to enable work with doubles
#pragma OPENCL EXTENSION cl_khr_fp64 : enable
//+-----------------------------------------------------------+
//| OpenCL kernel                                             |
//| The bitonic sort kernel does an ascending sort.           |
//+-----------------------------------------------------------+
//| R. Banger,K. Bhattacharyya, OpenCL Programming by Example:|
//| A comprehensive guide on OpenCL programming with examples |
//| PACKT Publishing, 2013.                                   |
//+-----------------------------------------------------------+
__kernel void BitonicSort_GPU(__global double *data,const uint stage,const uint pass)
  {
   uint id=get_global_id(0);
   uint distance = 1<<(stage-pass);
   uint left_id  =(id &(distance-1));
   left_id+=(id>>(stage-pass))*(distance<<1);
   uint right_id=left_id+distance;
   double left_value=data[left_id];
   double right_value=data[right_id];
   uint same_direction=(id>>stage)&0x1;
   uint temp = same_direction?right_id:temp;
   right_id  = same_direction?left_id:right_id;
   left_id   = same_direction?temp:left_id;
   int compare_res=(left_value<right_value);
   double greater = compare_res?right_value:left_value;
   double lesser  = compare_res?left_value:right_value;
   data[left_id] = lesser;
   data[right_id]= greater;
  };
//+------------------------------------------------------------------+
