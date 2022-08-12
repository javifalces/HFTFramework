//+------------------------------------------------------------------+
//|                                            TestStatBenchmark.mq5 |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2016-2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"

#include <Math\Stat\Math.mqh>
#include <Math\Stat\Binomial.mqh>
#include <Math\Stat\Beta.mqh>
#include <Math\Stat\Gamma.mqh>
#include <Math\Stat\Cauchy.mqh>
#include <Math\Stat\Exponential.mqh>
#include <Math\Stat\Uniform.mqh>
#include <Math\Stat\Geometric.mqh>
#include <Math\Stat\Hypergeometric.mqh>
#include <Math\Stat\Logistic.mqh>
#include <Math\Stat\Weibull.mqh>
#include <Math\Stat\Poisson.mqh>
#include <Math\Stat\F.mqh>
#include <Math\Stat\ChiSquare.mqh>
#include <Math\Stat\NoncentralChiSquare.mqh>
#include <Math\Stat\NoncentralF.mqh>
#include <Math\Stat\NoncentralBeta.mqh>
#include <Math\Stat\NegativeBinomial.mqh>
#include <Math\Stat\T.mqh>
#include <Math\Stat\NoncentralT.mqh>
#include <Math\Stat\Normal.mqh>
#include <Math\Stat\Lognormal.mqh>
//+------------------------------------------------------------------+
//| Structure for benchmark information                              |
//+------------------------------------------------------------------+
struct benchmark_info
  {
   //--- propability density calculation times
   double            pdf_time_min;
   double            pdf_time_max;
   double            pdf_time_mean;
   double            pdf_time_median;
   double            pdf_time_stddev;
   double            pdf_time_avgdev;
   //--- cumulative distribution calculation times
   double            cdf_time_min;
   double            cdf_time_max;
   double            cdf_time_mean;
   double            cdf_time_median;
   double            cdf_time_stddev;
   double            cdf_time_avgdev;
   //--- quantile calculation times
   double            quantile_time_min;
   double            quantile_time_max;
   double            quantile_time_mean;
   double            quantile_time_median;
   double            quantile_time_stddev;
   double            quantile_time_avgdev;
   //--- random variable calculation times
   double            random_time_min;
   double            random_time_max;
   double            random_time_mean;
   double            random_time_median;
   double            random_time_stddev;
   double            random_time_avgdev;
  };

#define AverageCount 1000
//+------------------------------------------------------------------+
//| ShowBenchmarkInfo                                                |
//+------------------------------------------------------------------+
void ShowBenchmarkInfo(const string test_info,benchmark_info &benchmark)
  {
   PrintFormat("%s time (microseconds):      pdf_mean=%4.2f,      pdf_median=%4.2f,      pdf_min=%4.2f,      pdf_max=%4.2f,       pdf_stddev=%4.2f,       pdf_avgdev=%4.2f",
               test_info,benchmark.pdf_time_mean,benchmark.pdf_time_median,benchmark.pdf_time_min,benchmark.pdf_time_max,benchmark.pdf_time_stddev,benchmark.pdf_time_avgdev);
   PrintFormat("%s time (microseconds):      cdf_mean=%4.2f,      cdf_median=%4.2f,      cdf_min=%4.2f,      cdf_max=%4.2f,       cdf_stddev=%4.2f,       cdf_avgdev=%4.2f",
               test_info,benchmark.cdf_time_mean,benchmark.cdf_time_median,benchmark.cdf_time_min,benchmark.cdf_time_max,benchmark.cdf_time_stddev,benchmark.cdf_time_avgdev);
   PrintFormat("%s time (microseconds): quantile_mean=%4.2f, quantile_median=%4.2f, quantile_min=%4.2f, quantile_max=%4.2f,  quantile_stddev=%4.2f,  quantile_avgdev=%4.2f",
               test_info,benchmark.quantile_time_mean,benchmark.quantile_time_median,benchmark.quantile_time_min,benchmark.quantile_time_max,benchmark.quantile_time_stddev,benchmark.quantile_time_avgdev);
   PrintFormat("%s time (microseconds):   random_mean=%4.2f,   random_median=%4.2f,   random_min=%4.2f,   random_max=%4.2f,    random_stddev=%4.2f,    random_avgdev=%4.2f",
               test_info,benchmark.random_time_mean,benchmark.random_time_median,benchmark.random_time_min,benchmark.random_time_max,benchmark.random_time_stddev,benchmark.random_time_avgdev);
  }
//+------------------------------------------------------------------+
//| CalculateStatisticalProperties                                   |
//+------------------------------------------------------------------+
void CalculateStatisticalProperties(benchmark_info &benchmark,double &pdf_times[],double &cdf_times[],double &quantile_times[],double &random_times[])
  {
//--- calculate statistical properties for calculation time: min,max,mean,median,standard and average deviations
//--- probability density
   benchmark.pdf_time_min=MathMin(pdf_times);
   benchmark.pdf_time_max=MathMax(pdf_times);
   benchmark.pdf_time_mean=MathMean(pdf_times);
   benchmark.pdf_time_median=MathMedian(pdf_times);
   benchmark.pdf_time_stddev=MathStandardDeviation(pdf_times);
   benchmark.pdf_time_avgdev=MathAverageDeviation(pdf_times);
//--- cumulative distribution
   benchmark.cdf_time_min=MathMin(cdf_times);
   benchmark.cdf_time_max=MathMax(cdf_times);
   benchmark.cdf_time_mean=MathMean(cdf_times);
   benchmark.cdf_time_median=MathMedian(cdf_times);
   benchmark.cdf_time_stddev=MathStandardDeviation(cdf_times);
   benchmark.cdf_time_avgdev=MathAverageDeviation(cdf_times);
//--- quantile
   benchmark.quantile_time_min=MathMin(quantile_times);
   benchmark.quantile_time_max=MathMax(quantile_times);
   benchmark.quantile_time_mean=MathMean(quantile_times);
   benchmark.quantile_time_median=MathMedian(quantile_times);
   benchmark.quantile_time_stddev=MathStandardDeviation(quantile_times);
   benchmark.quantile_time_avgdev=MathAverageDeviation(quantile_times);
//--- random values
   benchmark.random_time_min=MathMin(random_times);
   benchmark.random_time_max=MathMax(random_times);
   benchmark.random_time_mean=MathMean(random_times);
   benchmark.random_time_median=MathMedian(random_times);
   benchmark.random_time_stddev=MathStandardDeviation(random_times);
   benchmark.random_time_avgdev=MathAverageDeviation(random_times);
   return;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkBinomial                                       |
//+------------------------------------------------------------------+
bool CalculateBenchmarkBinomial(benchmark_info &benchmark_binomial,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Binomial distribution parameters
   const int N=50;
   const double probability=0.1;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0;

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityBinomial(x_values,N,probability,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionBinomial(x_values,N,probability,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileBinomial(cdf_values,N,probability,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomBinomial(N,probability,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_binomial,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkBeta                                           |
//+------------------------------------------------------------------+
bool CalculateBenchmarkBeta(benchmark_info &benchmark_beta,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Beta distribution parameters
   const double a=2.0;
   const double b=4.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityBeta(x_values,a,b,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionBeta(x_values,a,b,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileBeta(cdf_values,a,b,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomBeta(a,b,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_beta,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkGamma                                          |
//+------------------------------------------------------------------+
bool CalculateBenchmarkGamma(benchmark_info &benchmark_gamma,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Gamma distribution parameters
   const double a=1.0;
   const double b=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityGamma(x_values,a,b,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionGamma(x_values,a,b,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileGamma(cdf_values,a,b,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomGamma(a,b,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_gamma,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkCauchy                                         |
//+------------------------------------------------------------------+
bool CalculateBenchmarkCauchy(benchmark_info &benchmark_cauchy,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Cauchy distribution parameters
   const double a=2.0;
   const double b=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityCauchy(x_values,a,b,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionCauchy(x_values,a,b,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileCauchy(cdf_values,a,b,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomCauchy(a,b,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_cauchy,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkExponential                                    |
//+------------------------------------------------------------------+
bool CalculateBenchmarkExponential(benchmark_info &benchmark_exponential,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Exponential distribution parameters
   const double mu=2.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityExponential(x_values,mu,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionExponential(x_values,mu,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileExponential(cdf_values,mu,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomExponential(mu,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_exponential,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkUniform                                        |
//+------------------------------------------------------------------+
bool CalculateBenchmarkUniform(benchmark_info &benchmark_uniform,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Uniform distribution parameters
   const double a=0.0;
   const double b=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityUniform(x_values,a,b,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionUniform(x_values,a,b,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileUniform(cdf_values,a,b,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomUniform(a,b,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_uniform,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkGeometric                                      |
//+------------------------------------------------------------------+
bool CalculateBenchmarkGeometric(benchmark_info &benchmark_geometric,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Geometric distribution parameters
   const double prob=0.3;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0;

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityGeometric(x_values,prob,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionGeometric(x_values,prob,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileGeometric(cdf_values,prob,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomGeometric(prob,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_geometric,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkHypergeometric                                 |
//+------------------------------------------------------------------+
bool CalculateBenchmarkHypergeometric(benchmark_info &benchmark_hypergeometric,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Hypergeometric distribution parameters
   const double m=50;
   const double k=11;
   const double n=12;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0;

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityHypergeometric(x_values,m,k,n,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionHypergeometric(x_values,m,k,n,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileHypergeometric(cdf_values,m,k,n,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomHypergeometric(m,k,n,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_hypergeometric,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkLogistic                                       |
//+------------------------------------------------------------------+
bool CalculateBenchmarkLogistic(benchmark_info &benchmark_logistic,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Logistic distribution parameters
   const double mu=1.0;
   const double sigma=2.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityLogistic(x_values,mu,sigma,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionLogistic(x_values,mu,sigma,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileLogistic(cdf_values,mu,sigma,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomLogistic(mu,sigma,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_logistic,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkWeibull                                        |
//+------------------------------------------------------------------+
bool CalculateBenchmarkWeibull(benchmark_info &benchmark_weibull,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Weibull distribution parameters
   const double a=5.0;
   const double b=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityWeibull(x_values,a,b,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionWeibull(x_values,a,b,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileWeibull(cdf_values,a,b,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomWeibull(a,b,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_weibull,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkPoisson                                        |
//+------------------------------------------------------------------+
bool CalculateBenchmarkPoisson(benchmark_info &benchmark_poisson,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Poisson distribution parameters
   const double lambda=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0;

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityPoisson(x_values,lambda,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionPoisson(x_values,lambda,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantilePoisson(cdf_values,lambda,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomPoisson(lambda,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_poisson,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkF                                              |
//+------------------------------------------------------------------+
bool CalculateBenchmarkF(benchmark_info &benchmark_f,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- F distribution parameters
   const double nu1=10.0;
   const double nu2=20.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityF(x_values,nu1,nu2,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionF(x_values,nu1,nu2,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileF(cdf_values,nu1,nu2,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomF(nu1,nu2,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_f,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkChiSquare                                      |
//+------------------------------------------------------------------+
bool CalculateBenchmarkChiSquare(benchmark_info &benchmark_chisquare,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- ChiSquare distribution parameters
   const double nu=2.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityChiSquare(x_values,nu,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionChiSquare(x_values,nu,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileChiSquare(cdf_values,nu,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomChiSquare(nu,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_chisquare,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkNoncentralChiSquare                                      |
//+------------------------------------------------------------------+
bool CalculateBenchmarkNoncentralChiSquare(benchmark_info &benchmark_noncentral_chisquare,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Noncentral ChiSquare distribution parameters
   const double nu=2.0;
   const double sigma=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityNoncentralChiSquare(x_values,nu,sigma,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionNoncentralChiSquare(x_values,nu,sigma,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileNoncentralChiSquare(cdf_values,nu,sigma,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomNoncentralChiSquare(nu,sigma,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_noncentral_chisquare,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkNoncentralF                                    |
//+------------------------------------------------------------------+
bool CalculateBenchmarkNoncentralF(benchmark_info &benchmark_noncentral_f,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Noncentral F distribution parameters
   const double nu1=10.0;
   const double nu2=20.0;
   const double sigma=2.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityNoncentralF(x_values,nu1,nu2,sigma,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionNoncentralF(x_values,nu1,nu2,sigma,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileNoncentralF(cdf_values,nu1,nu2,sigma,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomNoncentralF(nu1,nu2,sigma,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_noncentral_f,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkNoncentralBeta                                 |
//+------------------------------------------------------------------+
bool CalculateBenchmarkNoncentralBeta(benchmark_info &benchmark_noncentral_beta,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Noncentral Beta distribution parameters
   const double a=2.0;
   const double b=4.0;
   const double lambda=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityNoncentralBeta(x_values,a,b,lambda,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionNoncentralBeta(x_values,a,b,lambda,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileNoncentralBeta(cdf_values,a,b,lambda,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomNoncentralBeta(a,b,lambda,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_noncentral_beta,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkNegativeBinomial                               |
//+------------------------------------------------------------------+
bool CalculateBenchmarkNegativeBinomial(benchmark_info &benchmark_negative_binomial,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Negatibe Binomial distribution parameters
   const double r=2.0;
   const double p=0.5;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0;

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityNegativeBinomial(x_values,r,p,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionNegativeBinomial(x_values,r,p,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileNegativeBinomial(cdf_values,r,p,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomNegativeBinomial(r,p,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_negative_binomial,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkT                                              |
//+------------------------------------------------------------------+
bool CalculateBenchmarkT(benchmark_info &benchmark_t,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- T distribution parameters
   const double nu=8.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityT(x_values,nu,pdf_values))
         return false;
      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionT(x_values,nu,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileT(cdf_values,nu,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomT(nu,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_t,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkNoncentralT                                    |
//+------------------------------------------------------------------+
bool CalculateBenchmarkNoncentralT(benchmark_info &benchmark_noncentral_t,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Noncentral T distribution parameters
   const double nu=10.0;
   const double delta=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityNoncentralT(x_values,nu,delta,pdf_values))
         return false;

      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionNoncentralT(x_values,nu,delta,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileNoncentralT(cdf_values,nu,delta,quantile_values))
         return false;
      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomNoncentralT(nu,delta,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_noncentral_t,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkNormal                                         |
//+------------------------------------------------------------------+
bool CalculateBenchmarkNormal(benchmark_info &benchmark_normal,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Normal distribution parameters
   const double mu=1.0;
   const double sigma=1.0;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityNormal(x_values,mu,sigma,pdf_values))
         return false;

      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionNormal(x_values,mu,sigma,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileNormal(cdf_values,mu,sigma,quantile_values))
         return false;

      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomNormal(mu,sigma,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_normal,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarkLognormal                                      |
//+------------------------------------------------------------------+
bool CalculateBenchmarkLognormal(benchmark_info &benchmark_lognormal,int &tests_performed,int &tests_passed)
  {
   tests_performed++;
//--- Lognormal distribution parameters
   const double mu=0.5;
   const double sigma=0.6;

   const int N=50;
//--- prepare x values
   double x_values[];
   ArrayResize(x_values,N);
   for(int i=0; i<N; i++)
      x_values[i]=i*1.0/(N-1);

//--- arrays for calculated values
   double pdf_values[];
   double cdf_values[];
   double quantile_values[];
   double random_values[];
//--- arrays for time
   double pdf_times[];
   double cdf_times[];
   double quantile_times[];
   double random_times[];
   int random_data_count=10000;

   ulong msc=0;
   int average_count=AverageCount;
//---
   ArrayResize(pdf_times,average_count);
   ArrayResize(cdf_times,average_count);
   ArrayResize(quantile_times,average_count);
   ArrayResize(random_times,average_count);
//--- calculate benchmark
   for(int i=0; i<average_count; i++)
     {
      msc=GetMicrosecondCount();
      if(!MathProbabilityDensityLognormal(x_values,mu,sigma,pdf_values))
         return false;

      pdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathCumulativeDistributionLognormal(x_values,mu,sigma,cdf_values))
         return false;
      cdf_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathQuantileLognormal(cdf_values,mu,sigma,quantile_values))
         return false;

      quantile_times[i]=(double)GetMicrosecondCount()-msc;

      msc=GetMicrosecondCount();
      if(!MathRandomLognormal(mu,sigma,random_data_count,random_values))
         return false;
      random_times[i]=(double)GetMicrosecondCount()-msc;
     }
//--- calculate statistical properties min,max,mean,median,standard and average deviations
   CalculateStatisticalProperties(benchmark_lognormal,pdf_times,cdf_times,quantile_times,random_times);
//--- successful
   tests_passed++;
   return true;
  }
//+------------------------------------------------------------------+
//| CalculateBenchmarks                                              |
//+------------------------------------------------------------------+
void CalculateBenchmarks(string package_name)
  {
   PrintFormat("Unit tests for Package %s\n",package_name);
//--- initial values
   int tests_performed=0;
   int tests_passed=0;

//--- benchmark data for distirbutions
   benchmark_info benchmark_binomial;
   benchmark_info benchmark_beta;
   benchmark_info benchmark_gamma;
   benchmark_info benchmark_cauchy;
   benchmark_info benchmark_exponential;
   benchmark_info benchmark_uniform;
   benchmark_info benchmark_geometric;
   benchmark_info benchmark_hypergeometric;
   benchmark_info benchmark_logistic;
   benchmark_info benchmark_weibull;
   benchmark_info benchmark_poisson;
   benchmark_info benchmark_f;
   benchmark_info benchmark_chisquare;
   benchmark_info benchmark_noncentral_chisquare;
   benchmark_info benchmark_noncentral_f;
   benchmark_info benchmark_noncentral_beta;
   benchmark_info benchmark_negative_binomial;
   benchmark_info benchmark_t;
   benchmark_info benchmark_noncentral_t;
   benchmark_info benchmark_normal;
   benchmark_info benchmark_lognormal;
//---
   if(CalculateBenchmarkBinomial(benchmark_binomial,tests_performed,tests_passed))
      ShowBenchmarkInfo("Binomial",benchmark_binomial);

   if(CalculateBenchmarkBeta(benchmark_beta,tests_performed,tests_passed))
      ShowBenchmarkInfo("Beta",benchmark_beta);

   if(CalculateBenchmarkGamma(benchmark_gamma,tests_performed,tests_passed))
      ShowBenchmarkInfo("Gamma",benchmark_gamma);

   if(CalculateBenchmarkCauchy(benchmark_cauchy,tests_performed,tests_passed))
      ShowBenchmarkInfo("Cauchy",benchmark_cauchy);

   if(CalculateBenchmarkExponential(benchmark_exponential,tests_performed,tests_passed))
      ShowBenchmarkInfo("Exponential",benchmark_exponential);

   if(CalculateBenchmarkUniform(benchmark_uniform,tests_performed,tests_passed))
      ShowBenchmarkInfo("Uniform",benchmark_uniform);

   if(CalculateBenchmarkGeometric(benchmark_geometric,tests_performed,tests_passed))
      ShowBenchmarkInfo("Geometric",benchmark_geometric);

   if(CalculateBenchmarkHypergeometric(benchmark_hypergeometric,tests_performed,tests_passed))
      ShowBenchmarkInfo("Hypergeometric",benchmark_hypergeometric);

   if(CalculateBenchmarkLogistic(benchmark_logistic,tests_performed,tests_passed))
      ShowBenchmarkInfo("Logistic",benchmark_logistic);

   if(CalculateBenchmarkWeibull(benchmark_weibull,tests_performed,tests_passed))
      ShowBenchmarkInfo("Weibull",benchmark_weibull);

   if(CalculateBenchmarkPoisson(benchmark_poisson,tests_performed,tests_passed))
      ShowBenchmarkInfo("Poisson",benchmark_poisson);

   if(CalculateBenchmarkF(benchmark_f,tests_performed,tests_passed))
      ShowBenchmarkInfo("F",benchmark_f);

   if(CalculateBenchmarkChiSquare(benchmark_chisquare,tests_performed,tests_passed))
      ShowBenchmarkInfo("ChiSquare",benchmark_chisquare);

   if(CalculateBenchmarkNoncentralChiSquare(benchmark_noncentral_chisquare,tests_performed,tests_passed))
      ShowBenchmarkInfo("Noncentral ChiSquare",benchmark_noncentral_chisquare);

   if(CalculateBenchmarkNoncentralF(benchmark_noncentral_f,tests_performed,tests_passed))
      ShowBenchmarkInfo("Noncentral F",benchmark_noncentral_f);

   if(CalculateBenchmarkNoncentralBeta(benchmark_noncentral_beta,tests_performed,tests_passed))
      ShowBenchmarkInfo("Noncentral Beta",benchmark_noncentral_beta);

   if(CalculateBenchmarkNegativeBinomial(benchmark_negative_binomial,tests_performed,tests_passed))
      ShowBenchmarkInfo("Negative Binomial",benchmark_negative_binomial);

   if(CalculateBenchmarkNormal(benchmark_normal,tests_performed,tests_passed))
      ShowBenchmarkInfo("Normal",benchmark_normal);

   if(CalculateBenchmarkLognormal(benchmark_lognormal,tests_performed,tests_passed))
      ShowBenchmarkInfo("Lognormal",benchmark_lognormal);

   if(CalculateBenchmarkT(benchmark_t,tests_performed,tests_passed))
      ShowBenchmarkInfo("T",benchmark_t);

   if(CalculateBenchmarkNoncentralT(benchmark_noncentral_t,tests_performed,tests_passed))
      ShowBenchmarkInfo("Noncentral T",benchmark_noncentral_t);

//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
   CalculateBenchmarks("Statistic Benchmark");
  }
//+------------------------------------------------------------------+
