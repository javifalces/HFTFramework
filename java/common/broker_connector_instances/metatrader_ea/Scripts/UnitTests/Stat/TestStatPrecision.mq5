//+------------------------------------------------------------------+
//|                                            TestStatPrecision.mq5 |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2016-2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"

#include <Math\Stat\Normal.mqh>
#include <Math\Stat\Weibull.mqh>
#include <Math\Stat\Uniform.mqh>
#include <Math\Stat\Logistic.mqh>
#include <Math\Stat\Cauchy.mqh>
#include <Math\Stat\Exponential.mqh>
#include <Math\Stat\Lognormal.mqh>
#include <Math\Stat\Poisson.mqh>
#include <Math\Stat\Gamma.mqh>
#include <Math\Stat\Chisquare.mqh>
#include <Math\Stat\NoncentralChisquare.mqh>
#include <Math\Stat\Binomial.mqh>
#include <Math\Stat\Beta.mqh>
#include <Math\Stat\F.mqh>
#include <Math\Stat\Geometric.mqh>
#include <Math\Stat\T.mqh>
#include <Math\Stat\Hypergeometric.mqh>
#include <Math\Stat\NegativeBinomial.mqh>
#include <Math\Stat\NoncentralBeta.mqh>
#include <Math\Stat\NoncentralF.mqh>
#include <Math\Stat\NoncentralT.mqh>

//--- PDF and CDF values calculated with Wolfram Alpha (30 digits)
//--- http://www.wolframalpha.com/input/?i=N%5BPDF%5BBetaDistribution%5B2,4%5D,0.5%5D,30%5D
const double Wolfram_Beta_PDF                 = 1.25000000000000000000000000000;      // N[PDF[BetaDistribution[2,4],0.5],30]
const double Wolfram_Beta_CDF                 = 0.812500000000000000000000000000;     // N[CDF[BetaDistribution[2,4],0.5],30]
const double Wolfram_Binomial_PDF             = 0.178863050569879740960000000000;     // N[PDF[BinomialDistribution[20,0.3],5],30]
const double Wolfram_Binomial_CDF             = 0.416370829447481383720000000000;     // N[CDF[BinomialDistribution[20,0.3],5],30]
const double Wolfram_Cauchy_PDF               = 0.0783532027529330883785273911988;    // N[PDF[CauchyDistribution[2,1],0.25],30]
const double Wolfram_Cauchy_CDF               = 0.165249340538567909638346176210;     // N[CDF[CauchyDistribution[2,1],0.25],30]
const double Wolfram_ChiSquare_PDF            = 0.389400391535702434122585133489;     // N[PDF[ChiSquareDistribution[2],0.5],30]
const double Wolfram_ChiSquare_CDF            = 0.221199216928595131754829733022;     // N[CDF[ChiSquareDistribution[2],0.5],30]
const double Wolfram_Exponential_PDF          = 0.441248451292297701432446071615;     // N[PDF[ExponentialDistribution[1/2],0.25],30]
const double Wolfram_Exponential_CDF          = 0.117503097415404597135107856771;     // N[CDF[ExponentialDistribution[1/2],0.25],30]
const double Wolfram_F_PDF                    = 0.702331961591220850480109739369;     // N[PDF[FRatioDistribution[2,4],0.25],30]
const double Wolfram_F_CDF                    = 0.209876543209876543209876543210;     // N[CDF[FRatioDistribution[2,4],0.25],30]
const double Wolfram_Gamma_PDF                = 0.606530659712633423603799534991;     // N[PDF[GammaDistribution[1,1],0.5],30] 
const double Wolfram_Gamma_CDF                = 0.393469340287366576396200465009;     // N[CDF[GammaDistribution[1,1],0.5],30]
const double Wolfram_Geometric_PDF            = 0.0504210000000000000000000000000;    // N[PDF[GeometricDistribution[0.3],5],30]
const double Wolfram_Geometric_CDF            = 0.882351000000000000000000000000;     // N[CDF[GeometricDistribution[0.3],5],30]
const double Wolfram_Hypergeometric_PDF       = 0.0366753989045010716837342224339;    // N[PDF[HypergeometricDistribution[9,8,20],6],30]
const double Wolfram_Hypergeometric_CDF       = 0.996784948797332698261490831150;     // N[CDF[HypergeometricDistribution[9,8,20],6],30]
const double Wolfram_Logistic_PDF             = 0.235003712201594489069302695021;     // N[PDF[LogisticDistribution[1,1],0.5],30]
const double Wolfram_Logistic_CDF             = 0.377540668798145435361099434254;     // N[CDF[LogisticDistribution[1,1],0.5],30]
const double Wolfram_Lognormal_PDF            = 2.47498055546993572014793467512E-7;   // N[PDF[LognormalDistribution[10,2],0.5],30]
const double Wolfram_Lognormal_CDF            = 4.48174235017131858935036726113E-8;   // N[CDF[LognormalDistribution[10,2],0.5],30] 
const double Wolfram_NegativeBinomial_PDF     = 0.0468750000000000000000000000000;    // N[PDF[NegativeBinomialDistribution[2,0.5],5],30]
const double Wolfram_NegativeBinomial_CDF     = 0.937500000000000000000000000000;     // N[CDF[NegativeBinomialDistribution[2,0.5],5],30] 
const double Wolfram_NoncentralBeta_PDF       = 1.83531575828435897166952478333;      // N[PDF[NoncentralBetaDistribution[2,4,1],0.25],30]
const double Wolfram_NoncentralBeta_CDF       = 0.279804451879309969773066407543;     // N[CDF[NoncentralBetaDistribution[2,4,1],0.25],30]
const double Wolfram_NoncentralChiSquare_PDF  = 0.266641691212769080163425079921;     // N[PDF[NoncentralChiSquareDistribution[2,1],0.5],30] 
const double Wolfram_NoncentralChiSquare_CDF  = 0.142365913869366361026153686445;     // N[CDF[NoncentralChiSquareDistribution[2,1],0.5],30]
const double Wolfram_NoncentralF_PDF          = 0.354683475208693741397782642610;     // N[PDF[NoncentralFRatioDistribution[2,4,2],0.25],30] 
const double Wolfram_NoncentralF_CDF          = 0.0907943467375269920219944143035;    // N[CDF[NoncentralFRatioDistribution[2,4,2],0.25],30] 
const double Wolfram_Normal_PDF               = 0.0000133655982673381195940786008171; // N[PDF[NormalDistribution[21,5],0.15],30]
const double Wolfram_Normal_CDF               = 0.0000152299819479778795518408747262; // N[CDF[NormalDistribution[21,5],0.15],30]
const double Wolfram_Poisson_PDF              = 2.81323432020839550168137707324E-13;  // N[PDF[PoissonDistribution[1],15],30]
const double Wolfram_Poisson_CDF              = 0.99999999999998132236536831934;      // N[CDF[PoissonDistribution[1],15],30] 
const double Wolfram_Uniform_PDF              = 0.00400000000000000000000000000000;   // N[PDF[UniformDistribution[0,250],0.125],30]
const double Wolfram_Uniform_CDF              = 0.000500000000000000000000000000000;  // N[CDF[UniformDistribution[0,250],0.125],30] 
const double Wolfram_Weibull_PDF              = 0.0195121858238667121530217146408;    // N[PDF[WeibullDistribution[5,1],0.25],30]
const double Wolfram_Weibull_CDF              = 0.000976085818024337765288210389671;  // N[CDF[WeibullDistribution[5,1],0.25],30]
const double Wolfram_T_PDF                    = 0.319904796224811454367412653512;     // N[PDF[TDistribution[4],0.51234567890123456],30]
const double Wolfram_T_CDF                    = 0.6822990443550955053632292600646;    // N[CDF[TDistribution[4],0.51234567890123456],30]
const double Wolfram_NoncentralT_PDF          = 4.06507868645014429884902547978E-14;  // N[PDF[Noncentral T Distribution[10,8],0.25],30]
const double Wolfram_NoncentralT_CDF          = 4.81698E-15;                          // N[CDF[Noncentral T Distribution[10,8],0.25],30]

//+------------------------------------------------------------------+
//| GetCorrectDigits                                                 |
//+------------------------------------------------------------------+
int GetCorrectDigits(const double delta)
  {
   double d=MathAbs(delta);
//--- check if delta to small
   if(d<10E-30)
      return(30);
//--- check if delta is large
   if(d>=1.0)
      return(0);
   int correct_digits=0;
   while(MathAbs(d)<1.0)
     {
      d=d*10;
      correct_digits++;
     }
//---
   return(correct_digits-1);
  }
//+------------------------------------------------------------------+
//| TestPrecision                                                    |
//+------------------------------------------------------------------+
void TestPrecision(const string comment,const double pdf,const double cdf,const double pdf_calculated,const double cdf_calculated)
  {
   double delta_pdf=pdf-pdf_calculated;
   double delta_cdf=cdf-cdf_calculated;
//--- print results
   Print("Testing precision for distribution:",comment);
//---- print results
   PrintFormat("Distribution: %s,  Wolfram PDF=%6.30f,  PDF_calculated=%6.30f,  deltaPDF=%6.30f",comment,pdf,pdf_calculated,delta_pdf);
   PrintFormat("Distribution: %s,  Wolfram CDF=%6.30f,  CDF_calculated=%6.30f,  deltaCDF=%6.30f",comment,cdf,cdf_calculated,delta_cdf);
//--- print correct digits
   PrintFormat("Distribution: %s PDF correct digits=%d",comment,GetCorrectDigits(delta_pdf));
   PrintFormat("Distribution: %s CDF correct digits=%d",comment,GetCorrectDigits(delta_cdf));
   Print("");
   return;
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
   int error_code=0;
//--- Beta distribution parameters
   double a=2.0;
   double b=4.0;
   double x=0.5;
   double d_beta = MathProbabilityDensityBeta(x, a, b,error_code);
   double p_beta = MathCumulativeDistributionBeta(x, a, b,error_code);
   TestPrecision("Beta",Wolfram_Beta_PDF,Wolfram_Beta_CDF,d_beta,p_beta);

//--- Binomial distribution parameters
   x=5;
   double n=20;
   double prob=0.3;
   double d_binomial = MathProbabilityDensityBinomial(x,n,prob,error_code);
   double p_binomial = MathCumulativeDistributionBinomial(x,n,prob,error_code);
   TestPrecision("Binomial",Wolfram_Binomial_PDF,Wolfram_Binomial_CDF,d_binomial,p_binomial);

//--- Cauchy distribution parameters   
   x=0.25;
   a=2.0; //mean
   b=1.0; //scale
   double d_cauchy = MathProbabilityDensityCauchy(x,a,b,error_code);
   double p_cauchy = MathCumulativeDistributionCauchy(x,a,b,error_code);
   TestPrecision("Cauchy",Wolfram_Cauchy_PDF,Wolfram_Cauchy_CDF,d_cauchy,p_cauchy);

//--- ChiSquare distribution parameters   
   double nu=2;
   x=0.5;
   double d_chisquare = MathProbabilityDensityChiSquare(x,nu,error_code);
   double p_chisquare = MathCumulativeDistributionChiSquare(x,nu,error_code);
   TestPrecision("ChiSquare",Wolfram_ChiSquare_PDF,Wolfram_ChiSquare_CDF,d_chisquare,p_chisquare);

//--- Exponential distribution parameters   
   x=0.25;
   double mu=2.0; // scale
   double d_exp = MathProbabilityDensityExponential(x,mu,error_code);
   double p_exp = MathCumulativeDistributionExponential(x,mu,error_code);
   TestPrecision("Exponential",Wolfram_Exponential_PDF,Wolfram_Exponential_CDF,d_exp,p_exp);

//--- F distribution parameters
   x=0.25;
   int nu1=2;
   int nu2=4;
   double d_F = MathProbabilityDensityF(x,nu1,nu2,error_code);
   double p_F = MathCumulativeDistributionF(x,nu1,nu2,error_code);
   TestPrecision("F",Wolfram_F_PDF,Wolfram_F_CDF,d_F,p_F);

//--- Gamma distribution parameters
   x=0.5;
   a=1.0; // shape
   b=1.0; // scale
   double d_gamma = MathProbabilityDensityGamma(x,a,b,error_code);
   double p_gamma = MathCumulativeDistributionGamma(x,a,b,error_code);
   TestPrecision("Gamma",Wolfram_Gamma_PDF,Wolfram_Gamma_CDF,d_gamma,p_gamma);

//--- Geometric distribution parameters
   x=5;
   prob=0.3;
   double d_geometric = MathProbabilityDensityGeometric(x,prob,error_code);
   double p_geometric = MathCumulativeDistributionGeometric(x,prob,error_code);
   TestPrecision("Geometric",Wolfram_Geometric_PDF,Wolfram_Geometric_CDF,d_geometric,p_geometric);

//--- Hypergeometric distribution parameters
   x=6;
   double m=20;
   double k=8;
   n=9;
   double d_hypergeometric = MathProbabilityDensityHypergeometric(x,m,k,n,error_code);
   double p_hypergeometric = MathCumulativeDistributionHypergeometric(x,m,k,n,error_code);
   TestPrecision("Hypergeometric",Wolfram_Hypergeometric_PDF,Wolfram_Hypergeometric_CDF,d_hypergeometric,p_hypergeometric);

//--- Logistic distribution parameters
   x=0.5;
   mu=1.0;    // mean
   double sigma=1.0; // scale
   double d_logistic = MathProbabilityDensityLogistic(x,mu,sigma,error_code);
   double p_logistic = MathCumulativeDistributionLogistic(x,mu,sigma,error_code);
   TestPrecision("Logistic",Wolfram_Logistic_PDF,Wolfram_Logistic_CDF,d_logistic,p_logistic);

//--- Lognormal distribution parameters
   x=0.5;
   mu=10.0;
   sigma=2.0;
   double d_lognormal = MathProbabilityDensityLognormal(x,mu,sigma,error_code);
   double p_lognormal = MathCumulativeDistributionLognormal(x,mu,sigma,error_code);
   TestPrecision("Lognormal",Wolfram_Lognormal_PDF,Wolfram_Lognormal_CDF,d_lognormal,p_lognormal);

//--- Negative Binomial distribution parameters
   x=5.0;
   double r=2.0;
   prob=0.5;
   double d_negbinomial = MathProbabilityDensityNegativeBinomial(x,r,prob,error_code);
   double p_negbinomial = MathCumulativeDistributionNegativeBinomial(x,r,prob,error_code);
   TestPrecision("NegativeBinomial",Wolfram_NegativeBinomial_PDF,Wolfram_NegativeBinomial_CDF,d_negbinomial,p_negbinomial);

//--- Noncentral Beta distribution parameters
   x=0.25;
   a=2.0;
   b=4.0;
   double lambda=1;
   double d_noncentralbeta = MathProbabilityDensityNoncentralBeta(x,a,b,lambda,error_code);
   double p_noncentralbeta = MathCumulativeDistributionNoncentralBeta(x,a,b,lambda,error_code);
   TestPrecision("NoncentralBeta",Wolfram_NoncentralBeta_PDF,Wolfram_NoncentralBeta_CDF,d_noncentralbeta,p_noncentralbeta);

//--- Noncentral ChiSquare distribution parameters
   x=0.5;
   nu=2.0;
   sigma=1.0;
   double d_nchisquare = MathProbabilityDensityNoncentralChiSquare(x,nu,sigma,error_code);
   double p_nchisquare = MathCumulativeDistributionNoncentralChiSquare(x,nu,sigma,error_code);
   TestPrecision("NoncentralChiSquare",Wolfram_NoncentralChiSquare_PDF,Wolfram_NoncentralChiSquare_CDF,d_nchisquare,p_nchisquare);

//--- Noncentral F distribution parameters
   x=0.25;
   nu1=2.0;
   nu2=4.0;
   sigma=2.0;
   double d_noncentralF = MathProbabilityDensityNoncentralF(x,nu1,nu2,sigma,error_code);
   double p_noncentralF = MathCumulativeDistributionNoncentralF(x,nu1,nu2,sigma,error_code);
   TestPrecision("NoncentralF",Wolfram_NoncentralF_PDF,Wolfram_NoncentralF_CDF,d_noncentralF,p_noncentralF);

//--- Normal distribution parameters
   x=0.15;
   mu=21.0;
   sigma=5.0;
   double d_normal = MathProbabilityDensityNormal(x, mu, sigma,error_code);
   double p_normal = MathCumulativeDistributionNormal(x, mu, sigma,error_code);
   TestPrecision("Normal",Wolfram_Normal_PDF,Wolfram_Normal_CDF,d_normal,p_normal);

//--- Poisson distribution parameters
   x=15;
   lambda=1.0;
   double d_poisson = MathProbabilityDensityPoisson(x,lambda,error_code);
   double p_poisson = MathCumulativeDistributionPoisson(x,lambda,error_code);
   TestPrecision("Poisson",Wolfram_Poisson_PDF,Wolfram_Poisson_CDF,d_poisson,p_poisson);

//--- Uniform distribution parameters
   x=0.125;
   a=0.0;   // lower
   b=250.0; // upper
   double d_uniform = MathProbabilityDensityUniform(x,a,b,error_code);
   double p_uniform = MathCumulativeDistributionUniform(x,a,b,error_code);
   TestPrecision("Uniform",Wolfram_Uniform_PDF,Wolfram_Uniform_CDF,d_uniform,p_uniform);

//--- Weibull distribution parameters
   x=0.25;
   a=5.0; // shape
   b=1.0; // scale
   double d_weibull = MathProbabilityDensityWeibull(x,a,b,error_code);
   double p_weibull = MathCumulativeDistributionWeibull(x,a,b,error_code);
   TestPrecision("Weibull",Wolfram_Weibull_PDF,Wolfram_Weibull_CDF,d_weibull,p_weibull);

//--- T distribution parameters
   x=0.51234567890123456;
   nu=4.0;
   double d_T = MathProbabilityDensityT(x,nu,error_code);
   double p_T = MathCumulativeDistributionT(x,nu,error_code);
   TestPrecision("T",Wolfram_T_PDF,Wolfram_T_CDF,d_T,p_T);

//--- Noncentral T distribution parameters
   x=0.25;
   nu=10.0;
   double delta=8.0;
   double d_NT = MathProbabilityDensityNoncentralT(x,nu,delta,error_code);
   double p_NT = MathCumulativeDistributionNoncentralT(x,nu,delta,error_code);
   TestPrecision("NoncentralT",Wolfram_NoncentralT_PDF,Wolfram_NoncentralT_CDF,d_NT,p_NT);
  }
//+------------------------------------------------------------------+
