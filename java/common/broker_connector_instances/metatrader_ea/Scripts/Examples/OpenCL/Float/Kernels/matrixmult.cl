//+-----------------------------------------------------------+
//| OpenCL kernel for matrix multiplication                   |
//| using global work groups                                  |
//+-----------------------------------------------------------+
//| http://gpgpu-computing4.blogspot.ru/2009/09/              |
//|      /matrix-multiplication-2-opencl.html                 |
//+-----------------------------------------------------------+
__kernel void MatrixMult_GPU1(__global float *matrix_a,
                              __global float *matrix_b,
                              __global float *matrix_c,
                              int rows_a,int cols_a,int cols_b)
  {
   int i=get_global_id(0);
   int j=get_global_id(1);
   float sum=0.0;
   for(int k=0; k<cols_a; k++)
     {
      sum+=matrix_a[cols_a*i+k]*matrix_b[cols_b*k+j];
     }
   matrix_c[cols_b*i+j]=sum;
  }
#define BLOCK_SIZE 10                                          
//+-----------------------------------------------------------+
//| OpenCL kernel for matrix multiplication                   |
//| using local groups with common local memory               |
//+-----------------------------------------------------------+
//| http://gpgpu-computing4.blogspot.ru/2009/10/              |
//|      /matrix-multiplication-3-opencl.html                 |
//+-----------------------------------------------------------+
__kernel void MatrixMult_GPU2(__global float *matrix_a,
                              __global float *matrix_b,
                              __global float *matrix_c,
                              int rows_a,int cols_a,int cols_b)
  {
   int group_i=get_group_id(0);
   int group_j=get_group_id(1);

   int i=get_local_id(0);
   int j=get_local_id(1);

   int offset_b=BLOCK_SIZE*group_i;
   int offset_a_start=cols_a*BLOCK_SIZE*group_j;
   float sum=(float)0.0;

   for(int offset_a=offset_a_start;
       offset_a<offset_a_start+cols_a;
       offset_a+=BLOCK_SIZE,
       offset_b+=BLOCK_SIZE*cols_b)
     {
      __local float submatrix_a[BLOCK_SIZE][BLOCK_SIZE];
      __local float submatrix_b[BLOCK_SIZE][BLOCK_SIZE];

      submatrix_a[i][j]=matrix_a[offset_a+cols_a*i+j];
      submatrix_b[i][j]=matrix_b[offset_b+cols_b*i+j];

      barrier(CLK_LOCAL_MEM_FENCE);

      for(int k=0; k<BLOCK_SIZE; k++)
         sum+=submatrix_a[i][k]*submatrix_b[k][j];

      barrier(CLK_LOCAL_MEM_FENCE);
     }
   int offset_c=BLOCK_SIZE*(cols_b*group_j+group_i);
   matrix_c[offset_c+cols_b*i+j]=sum;
  };
//+------------------------------------------------------------------+
