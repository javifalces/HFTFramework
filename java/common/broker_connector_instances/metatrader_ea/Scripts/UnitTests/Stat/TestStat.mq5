//+------------------------------------------------------------------+
//|                                                     TestStat.mq5 |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2016-2017, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
#property version   "1.00"

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

//--- precision
double calc_precision_pdf=10E-15;
double calc_precision_cdf=10E-15;
double calc_precision_quantile=10E-14;

//--- test values (x,pdf,cdf) for Binomial distribution
const double binomial_x_values[]={0,1,2,3,4,5,6,7,8,9};

const double binomial_pdf_values[]=
  {
   0.000104857600000000000000000000000,  //N[PDF[BinomialDistribution[10,0.6],0],30]
   0.00157286400000000000000000000000,   //N[PDF[BinomialDistribution[10,0.6],1],30]
   0.0106168320000000000000000000000,    //N[PDF[BinomialDistribution[10,0.6],2],30]
   0.0424673280000000000000000000000,    //N[PDF[BinomialDistribution[10,0.6],3],30]
   0.111476736000000000000000000000,     //N[PDF[BinomialDistribution[10,0.6],4],30]
   0.200658124800000000000000000000,     //N[PDF[BinomialDistribution[10,0.6],5],30]
   0.250822656000000000000000000000,     //N[PDF[BinomialDistribution[10,0.6],6],30]
   0.214990848000000000000000000000,     //N[PDF[BinomialDistribution[10,0.6],7],30]
   0.120932352000000000000000000000,     //N[PDF[BinomialDistribution[10,0.6],8],30]
   0.0403107840000000000000000000000,    //N[PDF[BinomialDistribution[10,0.6],9],30]
   0.00604661760000000000000000000000    //N[PDF[BinomialDistribution[10,0.6],10],30]
  };
const double binomial_cdf_values[]=
  {
   0.000104857600000000000000000000000, //N[CDF[BinomialDistribution[10,0.6],0],30]
   0.00167772160000000000000000000000,  //N[CDF[BinomialDistribution[10,0.6],1],30]
   0.0122945536000000000000000000000,   //N[CDF[BinomialDistribution[10,0.6],2],30]
   0.0547618816000000000000000000000,   //N[CDF[BinomialDistribution[10,0.6],3],30]
   0.166238617600000000000000000000,    //N[CDF[BinomialDistribution[10,0.6],4],30]
   0.366896742400000000000000000000,    //N[CDF[BinomialDistribution[10,0.6],5],30]
   0.617719398400000000000000000000,    //N[CDF[BinomialDistribution[10,0.6],6],30]
   0.832710246400000000000000000000,    //N[CDF[BinomialDistribution[10,0.6],7],30]
   0.953642598400000000000000000000,    //N[CDF[BinomialDistribution[10,0.6],8],30]
   0.993953382400000000000000000000,    //N[CDF[BinomialDistribution[10,0.6],9],30]
   1.00000000000000000000000000000      //N[CDF[BinomialDistribution[10,0.6],10],30]
  };

//--- test values (x,pdf,cdf) for Beta distribution
const double beta_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double beta_pdf_values[]=
  {
   0,     //N[PDF[BetaDistribution[2,4],0.0],30]
   1.458, //N[PDF[BetaDistribution[2,4],0.1],30]
   2.048, //N[PDF[BetaDistribution[2,4],0.2],30]
   2.058, //N[PDF[BetaDistribution[2,4],0.3],30]
   1.728, //N[PDF[BetaDistribution[2,4],0.4],30]
   1.25,  //N[PDF[BetaDistribution[2,4],0.5],30]
   0.768, //N[PDF[BetaDistribution[2,4],0.6],30]
   0.378, //N[PDF[BetaDistribution[2,4],0.7],30]
   0.128, //N[PDF[BetaDistribution[2,4],0.8],30]
   0.018, //N[PDF[BetaDistribution[2,4],0.9],30]
   0      //N[PDF[BetaDistribution[2,4],1.0],30]
  };
const double beta_cdf_values[]=
  {
   0,       //N[PDF[BetaDistribution[2,4],0.0],30]
   0.08146, //N[PDF[BetaDistribution[2,4],0.1],30]
   0.26272, //N[PDF[BetaDistribution[2,4],0.2],30]
   0.47178, //N[PDF[BetaDistribution[2,4],0.3],30]
   0.66304, //N[PDF[BetaDistribution[2,4],0.4],30]
   0.8125,  //N[PDF[BetaDistribution[2,4],0.5],30]
   0.91296, //N[PDF[BetaDistribution[2,4],0.6],30]
   0.96922, //N[PDF[BetaDistribution[2,4],0.7],30]
   0.99328, //N[PDF[BetaDistribution[2,4],0.8],30]
   0.99954, //N[PDF[BetaDistribution[2,4],0.9],30]
   1        //N[PDF[BetaDistribution[2,4],1.0],30]
  };

//--- test values (x,pdf,cdf) for Gamma distribution
const double gamma_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double gamma_pdf_values[]=
  {
   0,                                 //N[PDF[GammaDistribution[1,1],0.0],30] 
   0.904837418035959573164249059446,  //N[PDF[GammaDistribution[1,1],0.1],30] 
   0.818730753077981858669935508619,  //N[PDF[GammaDistribution[1,1],0.2],30] 
   0.740818220681717866066873779318,  //N[PDF[GammaDistribution[1,1],0.3],30] 
   0.670320046035639300744432925148,  //N[PDF[GammaDistribution[1,1],0.4],30] 
   0.606530659712633423603799534991,  //N[PDF[GammaDistribution[1,1],0.5],30] 
   0.548811636094026432628458917233,  //N[PDF[GammaDistribution[1,1],0.6],30] 
   0.496585303791409514704800093398,  //N[PDF[GammaDistribution[1,1],0.7],30] 
   0.449328964117221591430102385016,  //N[PDF[GammaDistribution[1,1],0.8],30] 
   0.406569659740599111883454239646,  //N[PDF[GammaDistribution[1,1],0.9],30] 
   0.367879441171442321595523770161,  //N[PDF[GammaDistribution[1,1],1.0],30] 
  };
const double gamma_cdf_values[]=
  {
   0,                                 //N[CDF[GammaDistribution[1,1],0.0],30] 
   0.0951625819640404268357509405536, //N[CDF[GammaDistribution[1,1],0.1],30] 
   0.181269246922018141330064491381,  //N[CDF[GammaDistribution[1,1],0.2],30] 
   0.259181779318282133933126220682,  //N[CDF[GammaDistribution[1,1],0.3],30] 
   0.329679953964360699255567074852,  //N[CDF[GammaDistribution[1,1],0.4],30] 
   0.393469340287366576396200465009,  //N[CDF[GammaDistribution[1,1],0.5],30] 
   0.451188363905973567371541082767,  //N[CDF[GammaDistribution[1,1],0.6],30] 
   0.503414696208590485295199906602,  //N[CDF[GammaDistribution[1,1],0.7],30] 
   0.550671035882778408569897614984,  //N[CDF[GammaDistribution[1,1],0.8],30] 
   0.593430340259400888116545760354,  //N[CDF[GammaDistribution[1,1],0.9],30] 
   0.632120558828557678404476229839   //N[CDF[GammaDistribution[1,1],1.0],30] 
  };

//--- test values (x,pdf,cdf) for Cauchy distribution
const double cauchy_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double cauchy_pdf_values[]=
  {
   0.0636619772367581343075535053490,  // N[PDF[CauchyDistribution[2,1],0.0],30]
   0.0690476976537506879691469689252,  // N[PDF[CauchyDistribution[2,1],0.1],30]
   0.0750730863641015734758885676285,  // N[PDF[CauchyDistribution[2,1],0.2],30]
   0.0818277342374783217320739143303,  // N[PDF[CauchyDistribution[2,1],0.3],30]
   0.0894128893774692897577998670632,  // N[PDF[CauchyDistribution[2,1],0.4],30]
   0.0979415034411663604731592389985,  // N[PDF[CauchyDistribution[2,1],0.5],30]
   0.107537123710740091735732272549,   // N[PDF[CauchyDistribution[2,1],0.6],30]
   0.118330812707728874177608746002,   // N[PDF[CauchyDistribution[2,1],0.7],30]
   0.130454871386799455548265379814,   // N[PDF[CauchyDistribution[2,1],0.8],30]
   0.144031622707597588931116527939,   // N[PDF[CauchyDistribution[2,1],0.9],30]
   0.159154943091895335768883763373    // N[PDF[CauchyDistribution[2,1],1.0],30]
  };
const double cauchy_cdf_values[]=
  {
   0.147583617650433274175401076225,   // N[CDF[CauchyDistribution[2,1],0.0],30]
   0.154214114450333468865873109512,   // N[CDF[CauchyDistribution[2,1],0.1],30]
   0.161414467217095251124123947093,   // N[CDF[CauchyDistribution[2,1],0.2],30]
   0.169253027330332663755853315960,   // N[CDF[CauchyDistribution[2,1],0.3],30]
   0.177807684489352753115503587502,   // N[CDF[CauchyDistribution[2,1],0.4],30]
   0.187167041810998816186252747565,   // N[CDF[CauchyDistribution[2,1],0.5],30]
   0.197431543288746570049221830324,   // N[CDF[CauchyDistribution[2,1],0.6],30]
   0.208714400160152736606325130038,   // N[CDF[CauchyDistribution[2,1],0.7],30]
   0.221142061623695522257108276774,   // N[CDF[CauchyDistribution[2,1],0.8],30]
   0.234853827811631858353234362662,   // N[CDF[CauchyDistribution[2,1],0.9],30]
   0.250000000000000000000000000000    // N[CDF[CauchyDistribution[2,1],1.0],30]
  };

//--- test values (x,pdf,cdf) for Exponential distribution
const double exponential_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double exponential_pdf_values[]=
  {
   0.500000000000000000000000000000, // N[PDF[ExponentialyDistribution[1/2],0.0],30]
   0.475614712250357004545712659890, // N[PDF[ExponentialyDistribution[1/2],0.1],30]
   0.452418709017979786582124529723, // N[PDF[ExponentialyDistribution[1/2],0.2],30]
   0.430353988212528903614516882272, // N[PDF[ExponentialyDistribution[1/2],0.3],30]
   0.409365376538990929334967754310, // N[PDF[ExponentialyDistribution[1/2],0.4],30]
   0.389400391535702434122585133489, // N[PDF[ExponentialyDistribution[1/2],0.5],30]
   0.370409110340858933033436889659, // N[PDF[ExponentialyDistribution[1/2],0.6],30]
   0.352344044859356717177410349515, // N[PDF[ExponentialyDistribution[1/2],0.7],30]
   0.335160023017819650372216462574, // N[PDF[ExponentialyDistribution[1/2],0.8],30]
   0.318814075810886646571871719156, // N[PDF[ExponentialyDistribution[1/2],0.9],30]
   0.303265329856316711801899767496  // N[PDF[ExponentialyDistribution[1/2],1.0],30]
  };
const double exponential_cdf_values[]=
  {
   0,                                 // N[CDF[ExponentialyDistribution[1/2],0.0],30]
   0.0487705754992859909085746802203, // N[CDF[ExponentialyDistribution[1/2],0.1],30]
   0.0951625819640404268357509405536, // N[CDF[ExponentialyDistribution[1/2],0.2],30]
   0.139292023574942192770966235457,  // N[CDF[ExponentialyDistribution[1/2],0.3],30]
   0.181269246922018141330064491381,  // N[CDF[ExponentialyDistribution[1/2],0.4],30]
   0.221199216928595131754829733022,  // N[CDF[ExponentialyDistribution[1/2],0.5],30]
   0.259181779318282133933126220682,  // N[CDF[ExponentialyDistribution[1/2],0.6],30]
   0.295311910281286565645179300969,  // N[CDF[ExponentialyDistribution[1/2],0.7],30]
   0.329679953964360699255567074852,  // N[CDF[ExponentialyDistribution[1/2],0.8],30]
   0.362371848378226706856256561688,  // N[CDF[ExponentialyDistribution[1/2],0.9],30]
   0.393469340287366576396200465009   // N[CDF[ExponentialyDistribution[1/2],1.0],30]
  };

//--- test values (x,pdf,cdf) for Uniform distribution
const double uniform_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double uniform_pdf_values[]=
  {
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.0],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.1],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.2],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.3],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.4],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.5],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.6],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.7],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.8],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],0.9],30]
   0.100000000000000000000000000000,  //N[PDF[UniformDistribution[0,10],1.0],30]   
  };
const double uniform_cdf_values[]=
  {
   0,                                   //N[CDF[UniformDistribution[0,10],0.0],30]
   0.0100000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.1],30]
   0.0200000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.2],30]
   0.0300000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.3],30]
   0.0400000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.4],30]
   0.0500000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.5],30]
   0.0600000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.6],30]
   0.0700000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.7],30]
   0.0800000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.8],30]
   0.0900000000000000000000000000000,   //N[CDF[UniformDistribution[0,10],0.9],30]
   0.100000000000000000000000000000     //N[CDF[UniformDistribution[0,10],1.0],30]
  };

//--- test values (x,pdf,cdf) for Geometric distribution
const double geometric_x_values[]={0,1,2,3,4,5,6,7,8,9,10};
const double geometric_pdf_values[]=
  {
   0.300000000000000000000000000000,   //N[PDF[GeometricDistribution[0.3],0],30]
   0.210000000000000000000000000000,   //N[PDF[GeometricDistribution[0.3],1],30]
   0.147000000000000000000000000000,   //N[PDF[GeometricDistribution[0.3],2],30]
   0.102900000000000000000000000000,   //N[PDF[GeometricDistribution[0.3],3],30]
   0.0720300000000000000000000000000,  //N[PDF[GeometricDistribution[0.3],4],30]
   0.0504210000000000000000000000000,  //N[PDF[GeometricDistribution[0.3],5],30]
   0.0352947000000000000000000000000,  //N[PDF[GeometricDistribution[0.3],6],30]
   0.0247062900000000000000000000000,  //N[PDF[GeometricDistribution[0.3],7],30]
   0.0172944030000000000000000000000,  //N[PDF[GeometricDistribution[0.3],8],30]
   0.0121060821000000000000000000000,  //N[PDF[GeometricDistribution[0.3],9],30]
   0.00847425747000000000000000000000  //N[PDF[GeometricDistribution[0.3],10],30]
  };
const double geometric_cdf_values[]=
  {
   0.300000000000000000000000000000,   //N[CDF[GeometricDistribution[0.3],0],30]
   0.510000000000000000000000000000,   //N[CDF[GeometricDistribution[0.3],1],30]
   0.657000000000000000000000000000,   //N[CDF[GeometricDistribution[0.3],2],30]
   0.759900000000000000000000000000,   //N[CDF[GeometricDistribution[0.3],3],30]
   0.831930000000000000000000000000,   //N[CDF[GeometricDistribution[0.3],4],30]
   0.882351000000000000000000000000,   //N[CDF[GeometricDistribution[0.3],5],30]
   0.917645700000000000000000000000,   //N[CDF[GeometricDistribution[0.3],6],30]
   0.942351990000000000000000000000,   //N[CDF[GeometricDistribution[0.3],7],30]
   0.959646393000000000000000000000,   //N[CDF[GeometricDistribution[0.3],8],30]
   0.971752475100000000000000000000,   //N[CDF[GeometricDistribution[0.3],9],30]
   0.980226732570000000000000000000,   //N[CDF[GeometricDistribution[0.3],10],30]
  };
//--- test values (x,pdf,cdf) for Hypergeometric distribution
const double hypergeometric_x_values[]={0,1,2,3,4,5,6,7,8,9,10};
const double hypergeometric_pdf_values[]=
  {
   0.000582565859927179267509102591561, //N[PDF[HypergeometricDistribution[12,11,30],0],30]
   0.00961233668879845791390019276076,  //N[PDF[HypergeometricDistribution[12,11,30],1],30]
   0.0587420575426572428071678446491,   //N[PDF[HypergeometricDistribution[12,11,30],2],30]
   0.176226172627971728421503533947,    //N[PDF[HypergeometricDistribution[12,11,30],3],30]
   0.288370100663953737417005782823,    //N[PDF[HypergeometricDistribution[12,11,30],4],30]
   0.269145427286356821589205397301,    //N[PDF[HypergeometricDistribution[12,11,30],5],30]
   0.144924460846499827009572137008,    //N[PDF[HypergeometricDistribution[12,11,30],6],30]
   0.0443646308713774980641547358189,   //N[PDF[HypergeometricDistribution[12,11,30],7],30]
   0.00739410514522958301069245596982,  //N[PDF[HypergeometricDistribution[12,11,30],8],30]
   0.000616175428769131917557704664151, //N[PDF[HypergeometricDistribution[12,11,30],9],30]
   0.0000217473680742046559138013410877 //N[PDF[HypergeometricDistribution[12,11,30],10],30]
  };
const double hypergeometric_cdf_values[]=
  {
   0.000582565859927179267509102591561, //N[CDF[HypergeometricDistribution[12,11,30],0],30]
   0.0101949025487256371814092953523,   //N[CDF[HypergeometricDistribution[12,11,30],1],30]
   0.0689369600913828799885771400014,   //N[CDF[HypergeometricDistribution[12,11,30],2],30]
   0.245163132719354608410080673949,    //N[CDF[HypergeometricDistribution[12,11,30],3],30]
   0.533533233383308345827086456772,    //N[CDF[HypergeometricDistribution[12,11,30],4],30]
   0.802678660669665167416291854073,    //N[CDF[HypergeometricDistribution[12,11,30],5],30]
   0.947603121516164994425863991081,    //N[CDF[HypergeometricDistribution[12,11,30],6],30]
   0.991967752387542492490018726900,    //N[CDF[HypergeometricDistribution[12,11,30],7],30]
   0.999361857532772075500711182870,    //N[CDF[HypergeometricDistribution[12,11,30],8],30]
   0.999978032961541207418268887534,    //N[CDF[HypergeometricDistribution[12,11,30],9],30]
   0.999999780329615412074182688875     //N[CDF[HypergeometricDistribution[12,11,30],10],30]
  };

//--- test values (x,pdf,cdf) for Logistic distribution
const double logistic_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double logistic_pdf_values[]=
  {
   0.117501856100797244534651347511,  //N[PDF[LogisticDistribution[1,2],0.0],30]
   0.118879479955564662317488644914,  //N[PDF[LogisticDistribution[1,2],0.1],30]
   0.120130372870764575214933764554,  //N[PDF[LogisticDistribution[1,2],0.2],30]
   0.121248697511250049746521122520,  //N[PDF[LogisticDistribution[1,2],0.3],30]
   0.122229155845372934532992984692,  //N[PDF[LogisticDistribution[1,2],0.4],30]
   0.123067041368799173792625971279,  //N[PDF[LogisticDistribution[1,2],0.5],30]
   0.123758286355929972293475696239,  //N[PDF[LogisticDistribution[1,2],0.6],30]
   0.124299503338771495086371350191,  //N[PDF[LogisticDistribution[1,2],0.7],30]
   0.124688020096445983910759097800,  //N[PDF[LogisticDistribution[1,2],0.8],30]
   0.124921907540558223470736299443,  //N[PDF[LogisticDistribution[1,2],0.9],30]
   0.125000000000000000000000000000   //N[PDF[LogisticDistribution[1,2],1.0],30]
  };
const double logistic_cdf_values[]=
  {
   0.377540668798145435361099434254,  //N[CDF[LogisticDistribution[1,2],0.0],30]
   0.389360766050778011702230766982,  //N[CDF[LogisticDistribution[1,2],0.1],30]
   0.401312339887547999630921340594,  //N[CDF[LogisticDistribution[1,2],0.2],30]
   0.413382421082669942205822702480,  //N[CDF[LogisticDistribution[1,2],0.3],30]
   0.425557483188341012847928734765,  //N[CDF[LogisticDistribution[1,2],0.4],30]
   0.437823499114201895972676362097,  //N[CDF[LogisticDistribution[1,2],0.5],30]
   0.450166002687522091440847458161,  //N[CDF[LogisticDistribution[1,2],0.6],30]
   0.462570154656250450554984306308,  //N[CDF[LogisticDistribution[1,2],0.7],30]
   0.475020812521060013900806817396,  //N[CDF[LogisticDistribution[1,2],0.8],30]
   0.487502603515789656338498667871,  //N[CDF[LogisticDistribution[1,2],0.9],30]
   0.500000000000000000000000000000   //N[CDF[LogisticDistribution[1,2],1.0],30]
  };

//--- test values (x,pdf,cdf) for Weibull distribution
const double weibull_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double weibull_pdf_values[]=
  {
   0,                                   //N[PDF[WeibullDistribution[5,1],0.0],30]
   0.000499995000024999916666874999583, //N[PDF[WeibullDistribution[5,1],0.1],30]
   0.00799744040955631282836298238325,  //N[PDF[WeibullDistribution[5,1],0.2],30]
   0.0404017044774286886523536633079,   //N[PDF[WeibullDistribution[5,1],0.3],30]
   0.126695968038428483489030385185,    //N[PDF[WeibullDistribution[5,1],0.4],30]
   0.302885385773857525577534122889,    //N[PDF[WeibullDistribution[5,1],0.5],30]
   0.599520816131266665994128475406,    //N[PDF[WeibullDistribution[5,1],0.6],30]
   1.01477624171575290112877265033,     //N[PDF[WeibullDistribution[5,1],0.7],30]
   1.47577563700864634999747886575,     //N[PDF[WeibullDistribution[5,1],0.8],30]
   1.81757982522109020306078908277,     //N[PDF[WeibullDistribution[5,1],0.9],30]
   1.83939720585721160797761885081      //N[PDF[WeibullDistribution[5,1],1.0],30]   
  };
const double weibull_cdf_values[]=
  {
   0,                                   // N[CDF[WeibullDistribution[5,1],0.0],30]
   9.99995000016666625000083333194e-6,  // N[CDF[WeibullDistribution[5,1],0.1],30]
   0.000319948805460896454627202093416, // N[CDF[WeibullDistribution[5,1],0.2],30]
   0.00242704994003237895423053560688,  // N[CDF[WeibullDistribution[5,1],0.3],30]
   0.0101877496997774727419501157385,   // N[CDF[WeibullDistribution[5,1],0.4],30]
   0.0307667655236559181518908067536,   // N[CDF[WeibullDistribution[5,1],0.5],30]
   0.0748135553529835401325177848671,   // N[CDF[WeibullDistribution[5,1],0.6],30]
   0.154705338012700623799439691520,    // N[CDF[WeibullDistribution[5,1],0.7],30]
   0.279406427241871899415293522582,    // N[CDF[WeibullDistribution[5,1],0.8],30]
   0.445944269098890351147450363430,    // N[CDF[WeibullDistribution[5,1],0.9],30]
   0.632120558828557678404476229839,    // N[CDF[WeibullDistribution[5,1],1.0],30]
  };

//--- test values (x,pdf,cdf) for Poisson distribution
const double poisson_x_values[]={0,1,2,3,4,5,6,7,8,9,10};
const double poisson_pdf_values[]=
  {
   0.367879441171442321595523770161,     //N[PDF[PoissonDistribution[1],0],30]
   0.367879441171442321595523770161,     //N[PDF[PoissonDistribution[1],1],30]
   0.183939720585721160797761885081,     //N[PDF[PoissonDistribution[1],2],30]
   0.0613132401952403869325872950269,    //N[PDF[PoissonDistribution[1],3],30]
   0.0153283100488100967331468237567,    //N[PDF[PoissonDistribution[1],4],30]
   0.00306566200976201934662936475135,   //N[PDF[PoissonDistribution[1],5],30]
   0.000510943668293669891104894125224,  //N[PDF[PoissonDistribution[1],6],30]
   0.0000729919526133814130149848750320, //N[PDF[PoissonDistribution[1],7],30]
   9.12399407667267662687310937900e-6,   //N[PDF[PoissonDistribution[1],8],30]
   1.01377711963029740298590104211e-6,   //N[PDF[PoissonDistribution[1],9],30]
   1.01377711963029740298590104211e-7    //N[PDF[PoissonDistribution[1],10],30]
  };
const double poisson_cdf_values[]=
  {
   0.367879441171442321595523770161,  //N[CDF[PoissonDistribution[1],0],30]
   0.735758882342884643191047540323,  //N[CDF[PoissonDistribution[1],1],30]
   0.919698602928605803988809425404,  //N[CDF[PoissonDistribution[1],2],30]
   0.981011843123846190921396720431,  //N[CDF[PoissonDistribution[1],3],30]
   0.996340153172656287654543544187,  //N[CDF[PoissonDistribution[1],4],30]
   0.999405815182418307001172908939,  //N[CDF[PoissonDistribution[1],5],30]
   0.999916758850711976892277803064,  //N[CDF[PoissonDistribution[1],6],30]
   0.999989750803325358305292787939,  //N[CDF[PoissonDistribution[1],7],30]
   0.999998874797402030981919661048,  //N[CDF[PoissonDistribution[1],8],30]
   0.999999888574521661279322646949,  //N[CDF[PoissonDistribution[1],9],30]
   0.999999989952233624309062945539   //N[CDF[PoissonDistribution[1],10],30]
  };

//--- test values (x,pdf,cdf) for F distribution
const double f_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double f_pdf_values[]=
  {
   0,                                 //N[PDF[FRatioDistribution[10,20],0.0],30]
   0.0150468160996581654991613930582, //N[PDF[FRatioDistribution[10,20],0.1],30]
   0.119815720709266406017897451156,  //N[PDF[FRatioDistribution[10,20],0.2],30]
   0.311387742341683605571461609007,  //N[PDF[FRatioDistribution[10,20],0.3],30]
   0.519763015923146756273840230479,  //N[PDF[FRatioDistribution[10,20],0.4],30]
   0.687881962127360000000000000000,  //N[PDF[FRatioDistribution[10,20],0.5],30]
   0.792024864914240532846337384641,  //N[PDF[FRatioDistribution[10,20],0.6],30]
   0.833047435548323908515813891880,  //N[PDF[FRatioDistribution[10,20],0.7],30]
   0.823613604965582842018415096215,  //N[PDF[FRatioDistribution[10,20],0.8],30]
   0.779352806006458219546578697713,  //N[PDF[FRatioDistribution[10,20],0.9],30]
   0.714356849619277621633480515275,  //N[PDF[FRatioDistribution[10,20],1.0],30]
  };
const double f_cdf_values[]=
  {
   0,                                   //N[CDF[FRatioDistribution[10,20],0.0],30]
   0.000341097358913110117497388449419, //N[CDF[FRatioDistribution[10,20],0.1],30]
   0.00616151312342342208319391118936,  //N[CDF[FRatioDistribution[10,20],0.2],30]
   0.0272692959418046279508386753254,   //N[CDF[FRatioDistribution[10,20],0.3],30]
   0.0689751467823785615691843288133,   //N[CDF[FRatioDistribution[10,20],0.4],30]
   0.129839625830400000000000000000,    //N[CDF[FRatioDistribution[10,20],0.5],30]
   0.204391530518198160782502820427,    //N[CDF[FRatioDistribution[10,20],0.6],30]
   0.286128116700867849780999121986,    //N[CDF[FRatioDistribution[10,20],0.7],30]
   0.369316442113680173719430076410,    //N[CDF[FRatioDistribution[10,20],0.8],30]
   0.449692021734750179793277026872,    //N[CDF[FRatioDistribution[10,20],0.9],30]
   0.524499531567108212493118813858,    //N[CDF[FRatioDistribution[10,20],1.0],30]
  };

//--- test values (x,pdf,cdf) for ChiSquare distribution
const double chisquare_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double chisquare_pdf_values[]=
  {
   0,                                //N[PDF[ChiSquareDistribution[2],0.0],30]
   0.475614712250357004545712659890, //N[PDF[ChiSquareDistribution[2],0.1],30]
   0.452418709017979786582124529723, //N[PDF[ChiSquareDistribution[2],0.2],30]
   0.430353988212528903614516882272, //N[PDF[ChiSquareDistribution[2],0.3],30]
   0.409365376538990929334967754310, //N[PDF[ChiSquareDistribution[2],0.4],30]
   0.389400391535702434122585133489, //N[PDF[ChiSquareDistribution[2],0.5],30]
   0.370409110340858933033436889659, //N[PDF[ChiSquareDistribution[2],0.6],30]
   0.352344044859356717177410349515, //N[PDF[ChiSquareDistribution[2],0.7],30]
   0.335160023017819650372216462574, //N[PDF[ChiSquareDistribution[2],0.8],30]
   0.318814075810886646571871719156, //N[PDF[ChiSquareDistribution[2],0.9],30]
   0.303265329856316711801899767496  //N[PDF[ChiSquareDistribution[2],1.0],30]
  };
const double chisquare_cdf_values[]=
  {
   0,                                 //N[CDF[ChiSquareDistribution[2],0.0],30]
   0.0487705754992859909085746802203, //N[CDF[ChiSquareDistribution[2],0.1],30]
   0.0951625819640404268357509405536, //N[CDF[ChiSquareDistribution[2],0.2],30]
   0.139292023574942192770966235457,  //N[CDF[ChiSquareDistribution[2],0.3],30]
   0.181269246922018141330064491381,  //N[CDF[ChiSquareDistribution[2],0.4],30]
   0.221199216928595131754829733022,  //N[CDF[ChiSquareDistribution[2],0.5],30]
   0.259181779318282133933126220682,  //N[CDF[ChiSquareDistribution[2],0.6],30]
   0.295311910281286565645179300969,  //N[CDF[ChiSquareDistribution[2],0.7],30]
   0.329679953964360699255567074852,  //N[CDF[ChiSquareDistribution[2],0.8],30]
   0.362371848378226706856256561688,  //N[CDF[ChiSquareDistribution[2],0.9],30]
   0.393469340287366576396200465009   //N[CDF[ChiSquareDistribution[2],1.0],30]
  };

//--- test values (x,pdf,cdf) for Noncentral ChiSquare distribution
const double noncentral_chisquare_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double noncentral_chisquare_pdf_values[]=
  {
   0,                                //N[PDF[NoncentralChiSquareDistribution[2,1],0.0],30]
   0.295731977425887787437413796316, //N[PDF[NoncentralChiSquareDistribution[2,1],0.1],30]
   0.288298568367079542335907975137, //N[PDF[NoncentralChiSquareDistribution[2,1],0.2],30]
   0.280969741689324922569967363843, //N[PDF[NoncentralChiSquareDistribution[2,1],0.3],30]
   0.273749589012391276633623625898, //N[PDF[NoncentralChiSquareDistribution[2,1],0.4],30]
   0.266641691212769080163425079921, //N[PDF[NoncentralChiSquareDistribution[2,1],0.5],30]
   0.259649153022898257071149284675, //N[PDF[NoncentralChiSquareDistribution[2,1],0.6],30]
   0.252774635687007877097528788462, //N[PDF[NoncentralChiSquareDistribution[2,1],0.7],30]
   0.246020387772423593652235077286, //N[PDF[NoncentralChiSquareDistribution[2,1],0.8],30]
   0.239388274230496750878159083137, //N[PDF[NoncentralChiSquareDistribution[2,1],0.9],30]
   0.232879803796820218250950764782  //N[PDF[NoncentralChiSquareDistribution[2,1],1.0],30]
  };
const double noncentral_chisquare_cdf_values[]=
  {
   0,
   0.0299490533481423931947523237425, //N[CDF[NoncentralChiSquareDistribution[2,1],0.1],30]
   0.0591497275819226474905426294380, //N[CDF[NoncentralChiSquareDistribution[2,1],0.2],30]
   0.0876122537088524075998281348827, //N[CDF[NoncentralChiSquareDistribution[2,1],0.3],30]
   0.115347298953703510108668127844,  //N[CDF[NoncentralChiSquareDistribution[2,1],0.4],30]
   0.142365913869366361026153686445,  //N[CDF[NoncentralChiSquareDistribution[2,1],0.5],30]
   0.168679483009103160200853235778,  //N[CDF[NoncentralChiSquareDistribution[2,1],0.6],30]
   0.194299678960710479382944892634,  //N[CDF[NoncentralChiSquareDistribution[2,1],0.7],30]
   0.219238419553235326130896974995,  //N[CDF[NoncentralChiSquareDistribution[2,1],0.8],30]
   0.243507828056537438718158282551,  //N[CDF[NoncentralChiSquareDistribution[2,1],0.9],30]
   0.267120196203179781749049235218   //N[CDF[NoncentralChiSquareDistribution[2,1],1.0],30]
  };

//--- test values (x,pdf,cdf) for Noncentral F distribution
const double noncentral_f_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double noncentral_f_pdf_values[]=
  {
   0,                                  //N[PDF[NoncentralFRatioDistribution[10,20,2],0.0],30]
   0.00637838395984413629724843643318, //N[PDF[NoncentralFRatioDistribution[10,20,2],0.1],30]
   0.0576689918386590515337738826738,  //N[PDF[NoncentralFRatioDistribution[10,20,2],0.2],30]
   0.168060987055040520728047841242,   //N[PDF[NoncentralFRatioDistribution[10,20,2],0.3],30]
   0.311215969391889284563713778298,   //N[PDF[NoncentralFRatioDistribution[10,20,2],0.4],30]
   0.452740977234892225466655938323,   //N[PDF[NoncentralFRatioDistribution[10,20,2],0.5],30]
   0.568412744474046056099634888126,   //N[PDF[NoncentralFRatioDistribution[10,20,2],0.6],30]
   0.647331638965106980807964878289,   //N[PDF[NoncentralFRatioDistribution[10,20,2],0.7],30]
   0.688682489027566460038593674890,   //N[PDF[NoncentralFRatioDistribution[10,20,2],0.8],30]
   0.697399306416916297988633401289,   //N[PDF[NoncentralFRatioDistribution[10,20,2],0.9],30]
   0.680752937956085914123043272777,   //N[PDF[NoncentralFRatioDistribution[10,20,2],1.0],30]
  };
const double noncentral_f_cdf_values[]=
  {
   0,                                   //N[CDF[NoncentralFRatioDistribution[10,20,2],0.0],30]
   0.000141081926600196286687082764064, //N[CDF[NoncentralFRatioDistribution[10,20,2],0.1],30]
   0.00282513842436593044668501614715,  //N[CDF[NoncentralFRatioDistribution[10,20,2],0.2],30]
   0.0136934496440913860234917981138,   //N[CDF[NoncentralFRatioDistribution[10,20,2],0.3],30]
   0.0375354305391195137901164221206,   //N[CDF[NoncentralFRatioDistribution[10,20,2],0.4],30]
   0.0758689753949193561304449803160,   //N[CDF[NoncentralFRatioDistribution[10,20,2],0.5],30]
   0.127206298388221792570648195287,    //N[CDF[NoncentralFRatioDistribution[10,20,2],0.6],30]
   0.188315146115626248268193389848,    //N[CDF[NoncentralFRatioDistribution[10,20,2],0.7],30]
   0.255414123041890604059805728902,    //N[CDF[NoncentralFRatioDistribution[10,20,2],0.8],30]
   0.324961425880008731771812043346,    //N[CDF[NoncentralFRatioDistribution[10,20,2],0.9],30]
   0.394048455363096839986479820864,    //N[CDF[NoncentralFRatioDistribution[10,20,2],1.0],30]
  };

//--- test values (x,pdf,cdf) for Noncentral Beta distribution
const double noncentral_beta_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double noncentral_beta_pdf_values[]=
  {
   0,                                 //N[PDF[NoncentralBetaDistribution[2,4,1],0.0],30]
   1.02497160055865464633963336051,   //N[PDF[NoncentralBetaDistribution[2,4,1],0.1],30]
   1.66133664626858910409449171342,   //N[PDF[NoncentralBetaDistribution[2,4,1],0.2],30]
   1.91877498001185552818242810746,   //N[PDF[NoncentralBetaDistribution[2,4,1],0.3],30]
   1.84511670851898738523457562051,   //N[PDF[NoncentralBetaDistribution[2,4,1],0.4],30]
   1.52366212772038645304752732774,   //N[PDF[NoncentralBetaDistribution[2,4,1],0.5],30]
   1.06551899551985128188799944192,   //N[PDF[NoncentralBetaDistribution[2,4,1],0.6],30]
   0.595311593728999952808386181057,  //N[PDF[NoncentralBetaDistribution[2,4,1],0.7],30]
   0.228265724218715136455418263844,  //N[PDF[NoncentralBetaDistribution[2,4,1],0.8],30]
   0.0362651708668906692268756554128, //N[PDF[NoncentralBetaDistribution[2,4,1],0.9],30]
   0,                                 //N[PDF[NoncentralBetaDistribution[2,4,1],1.0],30]
  };
const double noncentral_beta_cdf_values[]=
  {
   0,                                 //N[CDF[NoncentralBetaDistribution[2,4,1],0.0],30]
   0.0544271272420940348326261062660, //N[CDF[NoncentralBetaDistribution[2,4,1],0.1],30]
   0.191999109403542743081451188243,  //N[CDF[NoncentralBetaDistribution[2,4,1],0.2],30]
   0.374020701059051706320566724831,  //N[CDF[NoncentralBetaDistribution[2,4,1],0.3],30]
   0.564677487543142735835339607153,  //N[CDF[NoncentralBetaDistribution[2,4,1],0.4],30]
   0.734752398937727590947774975999,  //N[CDF[NoncentralBetaDistribution[2,4,1],0.5],30]
   0.864837477189878037683109886395,  //N[CDF[NoncentralBetaDistribution[2,4,1],0.6],30]
   0.947463049281498670018890201758,  //N[CDF[NoncentralBetaDistribution[2,4,1],0.7],30]
   0.987384842910461036210577979081,  //N[CDF[NoncentralBetaDistribution[2,4,1],0.8],30]
   0.999050083746745712534462538929,  //N[CDF[NoncentralBetaDistribution[2,4,1],0.9],30]
   1,                                 //N[CDF[NoncentralBetaDistribution[2,4,1],1.0],30]
  };

//--- test values (x,pdf,cdf) for Negative Binomial distribution
const double negative_binomial_x_values[]={0,1,2,3,4,5,6,7,8,9,10};
const double negative_binomial_pdf_values[]=
  {
   0.250000000000000000000000000000,   //N[PDF[NegativeBinomialDistribution[2,0.5],0],30]
   0.250000000000000000000000000000,   //N[PDF[NegativeBinomialDistribution[2,0.5],1],30]
   0.187500000000000000000000000000,   //N[PDF[NegativeBinomialDistribution[2,0.5],2],30]
   0.125000000000000000000000000000,   //N[PDF[NegativeBinomialDistribution[2,0.5],3],30]
   0.0781250000000000000000000000000,  //N[PDF[NegativeBinomialDistribution[2,0.5],4],30]
   0.0468750000000000000000000000000,  //N[PDF[NegativeBinomialDistribution[2,0.5],5],30]
   0.0273437500000000000000000000000,  //N[PDF[NegativeBinomialDistribution[2,0.5],6],30]
   0.0156250000000000000000000000000,  //N[PDF[NegativeBinomialDistribution[2,0.5],7],30]
   0.00878906250000000000000000000000, //N[PDF[NegativeBinomialDistribution[2,0.5],8],30]
   0.00488281250000000000000000000000, //N[PDF[NegativeBinomialDistribution[2,0.5],9],30]
   0.00268554687500000000000000000000  //N[PDF[NegativeBinomialDistribution[2,0.5],10],30]
  };
const double negative_binomial_cdf_values[]=
  {
   0.250000000000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],0],30]
   0.500000000000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],1],30]
   0.687500000000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],2],30]
   0.812500000000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],3],30]
   0.890625000000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],4],30]
   0.937500000000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],5],30]
   0.964843750000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],6],30]
   0.980468750000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],7],30]
   0.989257812500000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],8],30]
   0.994140625000000000000000000000,  //N[CDF[NegativeBinomialDistribution[2,0.5],9],30]
   0.996826171875000000000000000000   //N[CDF[NegativeBinomialDistribution[2,0.5],10],30]
  };

//--- test values (x,pdf,cdf) for T distribution
const double t_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double t_pdf_values[]=
  {
   0.386699020961393177406711760526, //N[PDF[TDistribution[8],0.0],30]
   0.384531295953230149911443054954, //N[PDF[TDistribution[8],0.1],30]
   0.378116644001397858209336510749, //N[PDF[TDistribution[8],0.2],30]
   0.367713578039119032335234793984, //N[PDF[TDistribution[8],0.3],30]
   0.353730330292855425371453625491, //N[PDF[TDistribution[8],0.4],30]
   0.336693897928227490108990053105, //N[PDF[TDistribution[8],0.5],30]
   0.317212114954435096838422653364, //N[PDF[TDistribution[8],0.6],30]
   0.295933007571487508535894462780, //N[PDF[TDistribution[8],0.7],30]
   0.273505568477656110583547462302, //N[PDF[TDistribution[8],0.8],30]
   0.250545395739835743338206333915, //N[PDF[TDistribution[8],0.9],30]
   0.227607580145303053396331859981, //N[PDF[TDistribution[8],1.0],30]
  };
const double t_cdf_values[]=
  {
   0.500000000000000000000000000000,  //N[CDF[TDistribution[8],0.0],30]
   0.538597545284701547838380887211,  //N[CDF[TDistribution[8],0.1],30]
   0.576764504299481290107055665260,  //N[CDF[TDistribution[8],0.2],30]
   0.6140877591587506564205123360179, //N[CDF[TDistribution[8],0.3],30]
   0.6501877476197896049046234705905, //N[CDF[TDistribution[8],0.4],30]
   0.6847319622215118291913144672012, //N[CDF[TDistribution[8],0.5],30]
   0.717444971062890912217515266940,  //N[CDF[TDistribution[8],0.6],30]
   0.748114473990727629636386922326,  //N[CDF[TDistribution[8],0.7],30]
   0.7765933329425457124221649477679, //N[CDF[TDistribution[8],0.8],30]
   0.8027978982672077353491118923202, //N[CDF[TDistribution[8],0.9],30]
   0.8267032464563328760859625057156, //N[CDF[TDistribution[8],1.0],30]
  };

//--- test values (x,pdf,cdf) for Noncentral T distribution
const double noncentral_t_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double noncentral_t_pdf_values[]=
  {
   4.92773299108631898092114435293e-15, //N[PDF[Noncentral T Distribution[10,8],0.0],30]
   1.13022616309487993266124014688e-14, //N[PDF[Noncentral T Distribution[10,8],0.1],30]
   2.64161256619123935657695034087e-14, //N[PDF[Noncentral T Distribution[10,8],0.2],30]
   6.28106243570814021661611528450e-14, //N[PDF[Noncentral T Distribution[10,8],0.3],30]
   1.51636983962647220239107618892e-13, //N[PDF[Noncentral T Distribution[10,8],0.4],30]
   3.70864175555732846819012024992e-13, //N[PDF[Noncentral T Distribution[10,8],0.5],30]
   9.16615229573759829315327016195e-13, //N[PDF[Noncentral T Distribution[10,8],0.6],30]
   2.28327725393016095515004668783e-12, //N[PDF[Noncentral T Distribution[10,8],0.7],30]
   5.71609303970754101789636971640e-12, //N[PDF[Noncentral T Distribution[10,8],0.8],30]
   1.43395037240077895541041912481e-11, //N[PDF[Noncentral T Distribution[10,8],0.9],30]
   3.59391445912257844885457911125e-11  //N[PDF[Noncentral T Distribution[10,8],1.0],30]
  };
const double noncentral_t_cdf_values[]=
  {
   6.22096057427178412351599517259e-16, //N[CDF[Noncentral T Distribution[10,8],0.0],30]
   1.38876e-15,                         //N[CDF[Noncentral T Distribution[10,8],0.1],30]
   3.16637e-15,                         //N[CDF[Noncentral T Distribution[10,8],0.2],30]
   7.36264e-15,                         //N[CDF[Noncentral T Distribution[10,8],0.3],30]
   1.74292e-14,                         //N[CDF[Noncentral T Distribution[10,8],0.4],30]
   4.19181e-14,                         //N[CDF[Noncentral T Distribution[10,8],0.5],30]
   1.02187e-13,                         //N[CDF[Noncentral T Distribution[10,8],0.6],30]
   2.5185e-13,                          //N[CDF[Noncentral T Distribution[10,8],0.7],30]
   6.2582e-13,                          //N[CDF[Noncentral T Distribution[10,8],0.8],30]
   1.56338e-12,                         //N[CDF[Noncentral T Distribution[10,8],0.9],30]
   3.91469e-12                          //N[CDF[Noncentral T Distribution[10,8],1.0],30]
  };

//--- test values (x,pdf,cdf) for Normal distribution
const double normal_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double normal_pdf_values[]=
  {
   0.0000117886135513079732292607502817, //N[PDF[NormalDistribution[21,5],0.0],30]
   0.0000128190726454211985066208612721, //N[PDF[NormalDistribution[21,5],0.1],30]
   0.0000139340308738428775166307149964, //N[PDF[NormalDistribution[21,5],0.2],30]
   0.0000151399071060322261068611049960, //N[PDF[NormalDistribution[21,5],0.3],30]
   0.0000164435633072571929616647120839, //N[PDF[NormalDistribution[21,5],0.4],30]
   0.0000178523314354265729452298914839, //N[PDF[NormalDistribution[21,5],0.5],30]
   0.0000193740416797438510221479365609, //N[PDF[NormalDistribution[21,5],0.6],30]
   0.0000210170520860800478589989753631, //N[PDF[NormalDistribution[21,5],0.7],30]
   0.0000227902796137729222332254497820, //N[PDF[NormalDistribution[21,5],0.8],30]
   0.0000247032326682046814399804038565, //N[PDF[NormalDistribution[21,5],0.9],30]
   0.0000267660451529770703548148976881  //N[PDF[NormalDistribution[21,5],1.0],30]
  };
const double normal_cdf_values[]=
  {
   0.0000133457490159063383530921177856,  //N[CDF[NormalDistribution[21,5],0.0],30]
   0.0000145754547908670163565982834186,  //N[CDF[NormalDistribution[21,5],0.1],30]
   0.0000159123797190821776993361592918,  //N[CDF[NormalDistribution[21,5],0.2],30]
   0.0000173652910736040482844247767230,  //N[CDF[NormalDistribution[21,5],0.3],30]
   0.0000189436199505532551675070397105,  //N[CDF[NormalDistribution[21,5],0.4],30]
   0.0000206575069125467387952544594699,  //N[CDF[NormalDistribution[21,5],0.5],30]
   0.0000225178503885254405900409660717,  //N[CDF[NormalDistribution[21,5],0.6],30]
   0.0000245363579664097294832709659110,  //N[CDF[NormalDistribution[21,5],0.7],30]
   0.0000267256007194920873949071274935,  //N[CDF[NormalDistribution[21,5],0.8],30]
   0.0000290990707119309791050371064941,  //N[CDF[NormalDistribution[21,5],0.9],30]
   0.0000316712418331199212537707567222,  //N[CDF[NormalDistribution[21,5],1.0],30]
  };

//--- test values (x,pdf,cdf) for Normal distribution
const double lognormal_x_values[]={0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
const double lognormal_pdf_values[]=
  {
   0,                                   //N[PDF[LognormalDistribution[0.5,0.6],0.0],30]
   0.000121629348383860528983795462471, //N[PDF[LognormalDistribution[0.5,0.6],0.1],30]
   0.00688195025191558633776856014392,  //N[PDF[LognormalDistribution[0.5,0.6],0.2],30]
   0.0392889731874339558688036697379,   //N[PDF[LognormalDistribution[0.5,0.6],0.3],30]
   0.102512981429306814499550041149,    //N[PDF[LognormalDistribution[0.5,0.6],0.4],30]
   0.184116195903499970110082739057,    //N[PDF[LognormalDistribution[0.5,0.6],0.5],30]
   0.268096008671120880492542216599,    //N[PDF[LognormalDistribution[0.5,0.6],0.6],30]
   0.342757405791760554961057548001,    //N[PDF[LognormalDistribution[0.5,0.6],0.7],30]
   0.402013268119797929735215095564,    //N[PDF[LognormalDistribution[0.5,0.6],0.8],30]
   0.444090682986326362528351841435,    //N[PDF[LognormalDistribution[0.5,0.6],0.9],30]
   0.469853125683837587733314480501     //N[PDF[LognormalDistribution[0.5,0.6],1.0],30]
  };
const double lognormal_cdf_values[]=
  {
   0,                                   //N[CDF[LognormalDistribution[0.5,0.6],0.0],30]
   1.49886599868856419859631258646e-6,  //N[CDF[LognormalDistribution[0.5,0.6],0.1],30]
   0.000219273481905296373803502502479, //N[CDF[LognormalDistribution[0.5,0.6],0.2],30]
   0.00225599721794476281427735394792,  //N[CDF[LognormalDistribution[0.5,0.6],0.3],30]
   0.00912553919415598265409187391175,  //N[CDF[LognormalDistribution[0.5,0.6],0.4],30]
   0.0233738658111863175085305417985,   //N[CDF[LognormalDistribution[0.5,0.6],0.5],30]
   0.0460223286277397559159160823805,   //N[CDF[LognormalDistribution[0.5,0.6],0.6],30]
   0.0766759295201034859852110590339,   //N[CDF[LognormalDistribution[0.5,0.6],0.7],30]
   0.114055476665196548777409987330,    //N[CDF[LognormalDistribution[0.5,0.6],0.8],30]
   0.156503097734807967129258608295,    //N[CDF[LognormalDistribution[0.5,0.6],0.9],30]
   0.202328380963643025368533599712     //N[CDF[LognormalDistribution[0.5,0.6],1.0],30]
  };
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
//| Structure for precision information                              |
//+------------------------------------------------------------------+
struct precision_info
  {
   //--- precision info
   double            pdf_max_delta;
   double            cdf_max_delta;
   double            quantile_max_delta;
  };
//+------------------------------------------------------------------+
//| Test function for Binomial distribution                          |
//+------------------------------------------------------------------+
bool TestBinomialDistribution(string test_name,const double n,const double probability,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double binomial_pdf[];
   double binomial_cdf[];
   double binomial_quantile[];
//--- arrays for differences
   double delta_binomial_pdf[];
   double delta_binomial_cdf[];
   double delta_binomial_quantile[];
//--- prepare arrays
   ArrayResize(binomial_pdf,N);
   ArrayResize(binomial_cdf,N);
   ArrayResize(binomial_quantile,N);
   ArrayResize(delta_binomial_pdf,N);
   ArrayResize(delta_binomial_cdf,N);
   ArrayResize(delta_binomial_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      binomial_pdf[i]=MathProbabilityDensityBinomial(x_values[i],n,probability,error_code);
      binomial_cdf[i]=MathCumulativeDistributionBinomial(x_values[i],n,probability,error_code);
      binomial_quantile[i]=MathQuantileBinomial(binomial_cdf[i],n,probability,error_code);
      //--- calculate difference
      delta_binomial_pdf[i]=pdf_values[i]-binomial_pdf[i];
      delta_binomial_cdf[i]=cdf_values[i]-binomial_cdf[i];
      delta_binomial_quantile[i]=x_values[i]-binomial_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_binomial_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_binomial_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_binomial_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityBinomial(x_values,n,probability,binomial_pdf))
      return false;
   if(!MathCumulativeDistributionBinomial(x_values,n,probability,binomial_cdf))
      return false;
   if(!MathQuantileBinomial(binomial_cdf,n,probability,binomial_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_binomial_pdf,N);
   ArrayResize(delta_binomial_cdf,N);
   ArrayResize(delta_binomial_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_binomial_pdf[i]=pdf_values[i]-binomial_pdf[i];
      delta_binomial_cdf[i]=cdf_values[i]-binomial_cdf[i];
      delta_binomial_quantile[i]=x_values[i]-binomial_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_binomial_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_binomial_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_binomial_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_binomial_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_binomial_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_binomial_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_binomial_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_binomial_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_binomial_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityBinomial(x_values,n,probability,log_mode,binomial_pdf))
      return false;
   if(!MathCumulativeDistributionBinomial(x_values,n,probability,tail,log_mode,binomial_cdf))
      return false;
   if(!MathQuantileBinomial(binomial_cdf,n,probability,tail,log_mode,binomial_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_binomial_pdf[i]=pdf_values[i]-MathExp(binomial_pdf[i]);
            delta_binomial_cdf[i]=cdf_values[i]-MathExp(binomial_cdf[i]);
            delta_binomial_quantile[i]=x_values[i]-binomial_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_binomial_pdf[i]=pdf_values[i]-binomial_pdf[i];
         delta_binomial_cdf[i]=cdf_values[i]-binomial_cdf[i];
         delta_binomial_quantile[i]=x_values[i]-binomial_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_binomial_pdf[i]=pdf_values[i]-MathExp(binomial_pdf[i]);
            delta_binomial_cdf[i]=(1.0-cdf_values[i])-MathExp(binomial_cdf[i]);
            delta_binomial_quantile[i]=x_values[i]-binomial_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_binomial_pdf[i]=pdf_values[i]-binomial_pdf[i];
         delta_binomial_cdf[i]=(1.0-cdf_values[i])-binomial_cdf[i];
         delta_binomial_quantile[i]=x_values[i]-binomial_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_binomial_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_binomial_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_binomial_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomBinomial(n,probability,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double binomial_mean=0;
      double binomial_variance=0;
      double binomial_skewness=0;
      double binomial_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsBinomial(n,probability,binomial_mean,binomial_variance,binomial_skewness,binomial_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",binomial_mean,binomial_variance,binomial_skewness,binomial_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-binomial_mean,variance-binomial_variance,skewness-binomial_skewness,kurtosis-binomial_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Beta distribution                              |
//+------------------------------------------------------------------+
bool TestBetaDistribution(string test_name,const double a,const double b,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double beta_pdf[];
   double beta_cdf[];
   double beta_quantile[];
//--- arrays for differences
   double delta_beta_pdf[];
   double delta_beta_cdf[];
   double delta_beta_quantile[];
//--- prepare arrays
   ArrayResize(beta_pdf,N);
   ArrayResize(beta_cdf,N);
   ArrayResize(beta_quantile,N);
   ArrayResize(delta_beta_pdf,N);
   ArrayResize(delta_beta_cdf,N);
   ArrayResize(delta_beta_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      beta_pdf[i]=MathProbabilityDensityBeta(x_values[i],a,b,error_code);
      beta_cdf[i]=MathCumulativeDistributionBeta(x_values[i],a,b,error_code);
      beta_quantile[i]=MathQuantileBeta(beta_cdf[i],a,b,error_code);
      //--- calculate difference
      delta_beta_pdf[i]=pdf_values[i]-beta_pdf[i];
      delta_beta_cdf[i]=cdf_values[i]-beta_cdf[i];
      delta_beta_quantile[i]=x_values[i]-beta_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_beta_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_beta_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_beta_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityBeta(x_values,a,b,beta_pdf))
      return false;
   if(!MathCumulativeDistributionBeta(x_values,a,b,beta_cdf))
      return false;
   if(!MathQuantileBeta(beta_cdf,a,b,beta_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_beta_pdf,N);
   ArrayResize(delta_beta_cdf,N);
   ArrayResize(delta_beta_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_beta_pdf[i]=pdf_values[i]-beta_pdf[i];
      delta_beta_cdf[i]=cdf_values[i]-beta_cdf[i];
      delta_beta_quantile[i]=x_values[i]-beta_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_beta_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_beta_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_beta_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_beta_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_beta_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_beta_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_beta_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_beta_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_beta_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityBeta(x_values,a,b,log_mode,beta_pdf))
      return false;
   if(!MathCumulativeDistributionBeta(x_values,a,b,tail,log_mode,beta_cdf))
      return false;
   if(!MathQuantileBeta(beta_cdf,a,b,tail,log_mode,beta_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_beta_pdf[i]=pdf_values[i]-MathExp(beta_pdf[i]);
            delta_beta_cdf[i]=cdf_values[i]-MathExp(beta_cdf[i]);
            delta_beta_quantile[i]=x_values[i]-beta_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_beta_pdf[i]=pdf_values[i]-beta_pdf[i];
         delta_beta_cdf[i]=cdf_values[i]-beta_cdf[i];
         delta_beta_quantile[i]=x_values[i]-beta_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_beta_pdf[i]=pdf_values[i]-MathExp(beta_pdf[i]);
            delta_beta_cdf[i]=(1.0-cdf_values[i])-MathExp(beta_cdf[i]);
            delta_beta_quantile[i]=x_values[i]-beta_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_beta_pdf[i]=pdf_values[i]-beta_pdf[i];
         delta_beta_cdf[i]=(1.0-cdf_values[i])-beta_cdf[i];
         delta_beta_quantile[i]=x_values[i]-beta_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_beta_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_beta_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_beta_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomBeta(a,b,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double beta_mean=0;
      double beta_variance=0;
      double beta_skewness=0;
      double beta_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsBeta(a,b,beta_mean,beta_variance,beta_skewness,beta_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",beta_mean,beta_variance,beta_skewness,beta_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-beta_mean,variance-beta_variance,skewness-beta_skewness,kurtosis-beta_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Gamma distribution                             |
//+------------------------------------------------------------------+
bool TestGammaDistribution(string test_name,const double a,const double b,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double gamma_pdf[];
   double gamma_cdf[];
   double gamma_quantile[];
//--- arrays for differences
   double delta_gamma_pdf[];
   double delta_gamma_cdf[];
   double delta_gamma_quantile[];
//--- prepare arrays
   ArrayResize(gamma_pdf,N);
   ArrayResize(gamma_cdf,N);
   ArrayResize(gamma_quantile,N);
   ArrayResize(delta_gamma_pdf,N);
   ArrayResize(delta_gamma_cdf,N);
   ArrayResize(delta_gamma_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      gamma_pdf[i]=MathProbabilityDensityGamma(x_values[i],a,b,error_code);
      gamma_cdf[i]=MathCumulativeDistributionGamma(x_values[i],a,b,error_code);
      gamma_quantile[i]=MathQuantileGamma(gamma_cdf[i],a,b,error_code);
      //--- calculate difference
      delta_gamma_pdf[i]=pdf_values[i]-gamma_pdf[i];
      delta_gamma_cdf[i]=cdf_values[i]-gamma_cdf[i];
      delta_gamma_quantile[i]=x_values[i]-gamma_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_gamma_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_gamma_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_gamma_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityGamma(x_values,a,b,gamma_pdf))
      return false;
   if(!MathCumulativeDistributionGamma(x_values,a,b,gamma_cdf))
      return false;
   if(!MathQuantileGamma(gamma_cdf,a,b,gamma_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_gamma_pdf,N);
   ArrayResize(delta_gamma_cdf,N);
   ArrayResize(delta_gamma_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_gamma_pdf[i]=pdf_values[i]-gamma_pdf[i];
      delta_gamma_cdf[i]=cdf_values[i]-gamma_cdf[i];
      delta_gamma_quantile[i]=x_values[i]-gamma_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_gamma_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_gamma_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_gamma_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_gamma_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_gamma_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_gamma_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_gamma_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_gamma_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_gamma_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityGamma(x_values,a,b,log_mode,gamma_pdf))
      return false;
   if(!MathCumulativeDistributionGamma(x_values,a,b,tail,log_mode,gamma_cdf))
      return false;
   if(!MathQuantileGamma(gamma_cdf,a,b,tail,log_mode,gamma_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_gamma_pdf[i]=pdf_values[i]-MathExp(gamma_pdf[i]);
            delta_gamma_cdf[i]=cdf_values[i]-MathExp(gamma_cdf[i]);
            delta_gamma_quantile[i]=x_values[i]-gamma_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_gamma_pdf[i]=pdf_values[i]-gamma_pdf[i];
         delta_gamma_cdf[i]=cdf_values[i]-gamma_cdf[i];
         delta_gamma_quantile[i]=x_values[i]-gamma_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_gamma_pdf[i]=pdf_values[i]-MathExp(gamma_pdf[i]);
            delta_gamma_cdf[i]=(1.0-cdf_values[i])-MathExp(gamma_cdf[i]);
            delta_gamma_quantile[i]=x_values[i]-gamma_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_gamma_pdf[i]=pdf_values[i]-gamma_pdf[i];
         delta_gamma_cdf[i]=(1.0-cdf_values[i])-gamma_cdf[i];
         delta_gamma_quantile[i]=x_values[i]-gamma_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_gamma_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_gamma_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_gamma_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomGamma(a,b,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double gamma_mean=0;
      double gamma_variance=0;
      double gamma_skewness=0;
      double gamma_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsGamma(a,b,gamma_mean,gamma_variance,gamma_skewness,gamma_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",gamma_mean,gamma_variance,gamma_skewness,gamma_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-gamma_mean,variance-gamma_variance,skewness-gamma_skewness,kurtosis-gamma_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Cauchy distribution                            |
//+------------------------------------------------------------------+
bool TestCauchyDistribution(string test_name,const double a,const double b,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double cauchy_pdf[];
   double cauchy_cdf[];
   double cauchy_quantile[];
//--- arrays for differences
   double delta_cauchy_pdf[];
   double delta_cauchy_cdf[];
   double delta_cauchy_quantile[];
//--- prepare arrays
   ArrayResize(cauchy_pdf,N);
   ArrayResize(cauchy_cdf,N);
   ArrayResize(cauchy_quantile,N);
   ArrayResize(delta_cauchy_pdf,N);
   ArrayResize(delta_cauchy_cdf,N);
   ArrayResize(delta_cauchy_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      cauchy_pdf[i]=MathProbabilityDensityCauchy(x_values[i],a,b,error_code);
      cauchy_cdf[i]=MathCumulativeDistributionCauchy(x_values[i],a,b,error_code);
      cauchy_quantile[i]=MathQuantileCauchy(cauchy_cdf[i],a,b,error_code);
      //--- calculate difference
      delta_cauchy_pdf[i]=pdf_values[i]-cauchy_pdf[i];
      delta_cauchy_cdf[i]=cdf_values[i]-cauchy_cdf[i];
      delta_cauchy_quantile[i]=x_values[i]-cauchy_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_cauchy_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_cauchy_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_cauchy_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityCauchy(x_values,a,b,cauchy_pdf))
      return false;
   if(!MathCumulativeDistributionCauchy(x_values,a,b,cauchy_cdf))
      return false;
   if(!MathQuantileCauchy(cauchy_cdf,a,b,cauchy_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_cauchy_pdf,N);
   ArrayResize(delta_cauchy_cdf,N);
   ArrayResize(delta_cauchy_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_cauchy_pdf[i]=pdf_values[i]-cauchy_pdf[i];
      delta_cauchy_cdf[i]=cdf_values[i]-cauchy_cdf[i];
      delta_cauchy_quantile[i]=x_values[i]-cauchy_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_cauchy_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_cauchy_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_cauchy_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_cauchy_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_cauchy_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_cauchy_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_cauchy_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_cauchy_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_cauchy_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityCauchy(x_values,a,b,log_mode,cauchy_pdf))
      return false;
   if(!MathCumulativeDistributionCauchy(x_values,a,b,tail,log_mode,cauchy_cdf))
      return false;
   if(!MathQuantileCauchy(cauchy_cdf,a,b,tail,log_mode,cauchy_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_cauchy_pdf[i]=pdf_values[i]-MathExp(cauchy_pdf[i]);
            delta_cauchy_cdf[i]=cdf_values[i]-MathExp(cauchy_cdf[i]);
            delta_cauchy_quantile[i]=x_values[i]-cauchy_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_cauchy_pdf[i]=pdf_values[i]-cauchy_pdf[i];
         delta_cauchy_cdf[i]=cdf_values[i]-cauchy_cdf[i];
         delta_cauchy_quantile[i]=x_values[i]-cauchy_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_cauchy_pdf[i]=pdf_values[i]-MathExp(cauchy_pdf[i]);
            delta_cauchy_cdf[i]=(1.0-cdf_values[i])-MathExp(cauchy_cdf[i]);
            delta_cauchy_quantile[i]=x_values[i]-cauchy_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_cauchy_pdf[i]=pdf_values[i]-cauchy_pdf[i];
         delta_cauchy_cdf[i]=(1.0-cdf_values[i])-cauchy_cdf[i];
         delta_cauchy_quantile[i]=x_values[i]-cauchy_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_cauchy_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_cauchy_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_cauchy_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomCauchy(a,b,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double cauchy_mean=0;
      double cauchy_variance=0;
      double cauchy_skewness=0;
      double cauchy_kurtosis=0;
      //--- Cauchy distribution has undefined moments, not need to compare
      if(!MathMomentsCauchy(a,b,cauchy_mean,cauchy_variance,cauchy_skewness,cauchy_kurtosis,error_code))
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Exponential distribution                       |
//+------------------------------------------------------------------+
bool TestExponentialDistribution(string test_name,const double mu,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double exponential_pdf[];
   double exponential_cdf[];
   double exponential_quantile[];
//--- arrays for differences
   double delta_exponential_pdf[];
   double delta_exponential_cdf[];
   double delta_exponential_quantile[];
//--- prepare arrays
   ArrayResize(exponential_pdf,N);
   ArrayResize(exponential_cdf,N);
   ArrayResize(exponential_quantile,N);
   ArrayResize(delta_exponential_pdf,N);
   ArrayResize(delta_exponential_cdf,N);
   ArrayResize(delta_exponential_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      exponential_pdf[i]=MathProbabilityDensityExponential(x_values[i],mu,error_code);
      exponential_cdf[i]=MathCumulativeDistributionExponential(x_values[i],mu,error_code);
      exponential_quantile[i]=MathQuantileExponential(exponential_cdf[i],mu,error_code);
      //--- calculate difference
      delta_exponential_pdf[i]=pdf_values[i]-exponential_pdf[i];
      delta_exponential_cdf[i]=cdf_values[i]-exponential_cdf[i];
      delta_exponential_quantile[i]=x_values[i]-exponential_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_exponential_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_exponential_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_exponential_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityExponential(x_values,mu,exponential_pdf))
      return false;
   if(!MathCumulativeDistributionExponential(x_values,mu,exponential_cdf))
      return false;
   if(!MathQuantileExponential(exponential_cdf,mu,exponential_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_exponential_pdf,N);
   ArrayResize(delta_exponential_cdf,N);
   ArrayResize(delta_exponential_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_exponential_pdf[i]=pdf_values[i]-exponential_pdf[i];
      delta_exponential_cdf[i]=cdf_values[i]-exponential_cdf[i];
      delta_exponential_quantile[i]=x_values[i]-exponential_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_exponential_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_exponential_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_exponential_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_exponential_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_exponential_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_exponential_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_exponential_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_exponential_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_exponential_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityExponential(x_values,mu,log_mode,exponential_pdf))
      return false;
   if(!MathCumulativeDistributionExponential(x_values,mu,tail,log_mode,exponential_cdf))
      return false;
   if(!MathQuantileExponential(exponential_cdf,mu,tail,log_mode,exponential_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_exponential_pdf[i]=pdf_values[i]-MathExp(exponential_pdf[i]);
            delta_exponential_cdf[i]=cdf_values[i]-MathExp(exponential_cdf[i]);
            delta_exponential_quantile[i]=x_values[i]-exponential_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_exponential_pdf[i]=pdf_values[i]-exponential_pdf[i];
         delta_exponential_cdf[i]=cdf_values[i]-exponential_cdf[i];
         delta_exponential_quantile[i]=x_values[i]-exponential_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_exponential_pdf[i]=pdf_values[i]-MathExp(exponential_pdf[i]);
            delta_exponential_cdf[i]=(1.0-cdf_values[i])-MathExp(exponential_cdf[i]);
            delta_exponential_quantile[i]=x_values[i]-exponential_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_exponential_pdf[i]=pdf_values[i]-exponential_pdf[i];
         delta_exponential_cdf[i]=(1.0-cdf_values[i])-exponential_cdf[i];
         delta_exponential_quantile[i]=x_values[i]-exponential_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_exponential_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_exponential_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_exponential_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomExponential(mu,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double exponential_mean=0;
      double exponential_variance=0;
      double exponential_skewness=0;
      double exponential_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsExponential(mu,exponential_mean,exponential_variance,exponential_skewness,exponential_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",exponential_mean,exponential_variance,exponential_skewness,exponential_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-exponential_mean,variance-exponential_variance,skewness-exponential_skewness,kurtosis-exponential_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Uniform distribution                           |
//+------------------------------------------------------------------+
bool TestUnifromDistribution(string test_name,const double a,const double b,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double uniform_pdf[];
   double uniform_cdf[];
   double uniform_quantile[];
//--- arrays for differences
   double delta_uniform_pdf[];
   double delta_uniform_cdf[];
   double delta_uniform_quantile[];
//--- prepare arrays
   ArrayResize(uniform_pdf,N);
   ArrayResize(uniform_cdf,N);
   ArrayResize(uniform_quantile,N);
   ArrayResize(delta_uniform_pdf,N);
   ArrayResize(delta_uniform_cdf,N);
   ArrayResize(delta_uniform_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      uniform_pdf[i]=MathProbabilityDensityUniform(x_values[i],a,b,error_code);
      uniform_cdf[i]=MathCumulativeDistributionUniform(x_values[i],a,b,error_code);
      uniform_quantile[i]=MathQuantileUniform(uniform_cdf[i],a,b,error_code);
      //--- calculate difference
      delta_uniform_pdf[i]=pdf_values[i]-uniform_pdf[i];
      delta_uniform_cdf[i]=cdf_values[i]-uniform_cdf[i];
      delta_uniform_quantile[i]=x_values[i]-uniform_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_uniform_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_uniform_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_uniform_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityUniform(x_values,a,b,uniform_pdf))
      return false;
   if(!MathCumulativeDistributionUniform(x_values,a,b,uniform_cdf))
      return false;
   if(!MathQuantileUniform(uniform_cdf,a,b,uniform_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_uniform_pdf,N);
   ArrayResize(delta_uniform_cdf,N);
   ArrayResize(delta_uniform_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_uniform_pdf[i]=pdf_values[i]-uniform_pdf[i];
      delta_uniform_cdf[i]=cdf_values[i]-uniform_cdf[i];
      delta_uniform_quantile[i]=x_values[i]-uniform_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_uniform_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_uniform_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_uniform_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_uniform_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_uniform_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_uniform_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_uniform_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_uniform_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_uniform_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityUniform(x_values,a,b,log_mode,uniform_pdf))
      return false;
   if(!MathCumulativeDistributionUniform(x_values,a,b,tail,log_mode,uniform_cdf))
      return false;
   if(!MathQuantileUniform(uniform_cdf,a,b,tail,log_mode,uniform_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_uniform_pdf[i]=pdf_values[i]-MathExp(uniform_pdf[i]);
            delta_uniform_cdf[i]=cdf_values[i]-MathExp(uniform_cdf[i]);
            delta_uniform_quantile[i]=x_values[i]-uniform_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_uniform_pdf[i]=pdf_values[i]-uniform_pdf[i];
         delta_uniform_cdf[i]=cdf_values[i]-uniform_cdf[i];
         delta_uniform_quantile[i]=x_values[i]-uniform_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_uniform_pdf[i]=pdf_values[i]-MathExp(uniform_pdf[i]);
            delta_uniform_cdf[i]=(1.0-cdf_values[i])-MathExp(uniform_cdf[i]);
            delta_uniform_quantile[i]=x_values[i]-uniform_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_uniform_pdf[i]=pdf_values[i]-uniform_pdf[i];
         delta_uniform_cdf[i]=(1.0-cdf_values[i])-uniform_cdf[i];
         delta_uniform_quantile[i]=x_values[i]-uniform_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_uniform_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_uniform_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_uniform_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomUniform(a,b,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double uniform_mean=0;
      double uniform_variance=0;
      double uniform_skewness=0;
      double uniform_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsUniform(a,b,uniform_mean,uniform_variance,uniform_skewness,uniform_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",uniform_mean,uniform_variance,uniform_skewness,uniform_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-uniform_mean,variance-uniform_variance,skewness-uniform_skewness,kurtosis-uniform_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Geometric distribution                         |
//+------------------------------------------------------------------+
bool TestGeometricDistribution(string test_name,const double p,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double geometric_pdf[];
   double geometric_cdf[];
   double geometric_quantile[];
//--- arrays for differences
   double delta_geometric_pdf[];
   double delta_geometric_cdf[];
   double delta_geometric_quantile[];
//--- prepare arrays
   ArrayResize(geometric_pdf,N);
   ArrayResize(geometric_cdf,N);
   ArrayResize(geometric_quantile,N);
   ArrayResize(delta_geometric_pdf,N);
   ArrayResize(delta_geometric_cdf,N);
   ArrayResize(delta_geometric_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      geometric_pdf[i]=MathProbabilityDensityGeometric(x_values[i],p,error_code);
      geometric_cdf[i]=MathCumulativeDistributionGeometric(x_values[i],p,error_code);
      geometric_quantile[i]=MathQuantileGeometric(geometric_cdf[i],p,error_code);
      //--- calculate difference
      delta_geometric_pdf[i]=pdf_values[i]-geometric_pdf[i];
      delta_geometric_cdf[i]=cdf_values[i]-geometric_cdf[i];
      delta_geometric_quantile[i]=x_values[i]-geometric_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_geometric_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_geometric_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_geometric_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityGeometric(x_values,p,geometric_pdf))
      return false;
   if(!MathCumulativeDistributionGeometric(x_values,p,geometric_cdf))
      return false;
   if(!MathQuantileGeometric(geometric_cdf,p,geometric_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_geometric_pdf,N);
   ArrayResize(delta_geometric_cdf,N);
   ArrayResize(delta_geometric_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_geometric_pdf[i]=pdf_values[i]-geometric_pdf[i];
      delta_geometric_cdf[i]=cdf_values[i]-geometric_cdf[i];
      delta_geometric_quantile[i]=x_values[i]-geometric_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_geometric_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_geometric_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_geometric_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_geometric_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_geometric_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_geometric_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_geometric_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_geometric_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_geometric_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityGeometric(x_values,p,log_mode,geometric_pdf))
      return false;
   if(!MathCumulativeDistributionGeometric(x_values,p,tail,log_mode,geometric_cdf))
      return false;
   if(!MathQuantileGeometric(geometric_cdf,p,tail,log_mode,geometric_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_geometric_pdf[i]=pdf_values[i]-MathExp(geometric_pdf[i]);
            delta_geometric_cdf[i]=cdf_values[i]-MathExp(geometric_cdf[i]);
            delta_geometric_quantile[i]=x_values[i]-geometric_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_geometric_pdf[i]=pdf_values[i]-geometric_pdf[i];
         delta_geometric_cdf[i]=cdf_values[i]-geometric_cdf[i];
         delta_geometric_quantile[i]=x_values[i]-geometric_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_geometric_pdf[i]=pdf_values[i]-MathExp(geometric_pdf[i]);
            delta_geometric_cdf[i]=1.0-cdf_values[i]-MathExp(geometric_cdf[i]);
            delta_geometric_quantile[i]=x_values[i]-geometric_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_geometric_pdf[i]=pdf_values[i]-geometric_pdf[i];
         delta_geometric_cdf[i]=(1.0-cdf_values[i])-geometric_cdf[i];
         delta_geometric_quantile[i]=x_values[i]-geometric_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_geometric_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_geometric_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_geometric_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomGeometric(p,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double geometric_mean=0;
      double geometric_variance=0;
      double geometric_skewness=0;
      double geometric_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsGeometric(p,geometric_mean,geometric_variance,geometric_skewness,geometric_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",geometric_mean,geometric_variance,geometric_skewness,geometric_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-geometric_mean,variance-geometric_variance,skewness-geometric_skewness,kurtosis-geometric_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Hypergeometric distribution                    |
//+------------------------------------------------------------------+
bool TestHypergeometricDistribution(string test_name,const double m,const double k,const double n,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double hypergeometric_pdf[];
   double hypergeometric_cdf[];
   double hypergeometric_quantile[];
//--- arrays for differences
   double delta_hypergeometric_pdf[];
   double delta_hypergeometric_cdf[];
   double delta_hypergeometric_quantile[];
//--- prepare arrays
   ArrayResize(hypergeometric_pdf,N);
   ArrayResize(hypergeometric_cdf,N);
   ArrayResize(hypergeometric_quantile,N);
   ArrayResize(delta_hypergeometric_pdf,N);
   ArrayResize(delta_hypergeometric_cdf,N);
   ArrayResize(delta_hypergeometric_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      hypergeometric_pdf[i]=MathProbabilityDensityHypergeometric(x_values[i],m,k,n,error_code);
      hypergeometric_cdf[i]=MathCumulativeDistributionHypergeometric(x_values[i],m,k,n,error_code);
      hypergeometric_quantile[i]=MathQuantileHypergeometric(hypergeometric_cdf[i],m,k,n,error_code);
      //--- calculate difference
      delta_hypergeometric_pdf[i]=pdf_values[i]-hypergeometric_pdf[i];
      delta_hypergeometric_cdf[i]=cdf_values[i]-hypergeometric_cdf[i];
      delta_hypergeometric_quantile[i]=x_values[i]-hypergeometric_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_hypergeometric_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_hypergeometric_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_hypergeometric_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityHypergeometric(x_values,m,k,n,hypergeometric_pdf))
      return false;
   if(!MathCumulativeDistributionHypergeometric(x_values,m,k,n,hypergeometric_cdf))
      return false;
   if(!MathQuantileHypergeometric(hypergeometric_cdf,m,k,n,hypergeometric_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_hypergeometric_pdf,N);
   ArrayResize(delta_hypergeometric_cdf,N);
   ArrayResize(delta_hypergeometric_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_hypergeometric_pdf[i]=pdf_values[i]-hypergeometric_pdf[i];
      delta_hypergeometric_cdf[i]=cdf_values[i]-hypergeometric_cdf[i];
      delta_hypergeometric_quantile[i]=x_values[i]-hypergeometric_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_hypergeometric_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_hypergeometric_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_hypergeometric_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_hypergeometric_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_hypergeometric_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_hypergeometric_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_hypergeometric_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_hypergeometric_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_hypergeometric_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityHypergeometric(x_values,m,k,n,log_mode,hypergeometric_pdf))
      return false;
   if(!MathCumulativeDistributionHypergeometric(x_values,m,k,n,tail,log_mode,hypergeometric_cdf))
      return false;
   if(!MathQuantileHypergeometric(hypergeometric_cdf,m,k,n,tail,log_mode,hypergeometric_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_hypergeometric_pdf[i]=pdf_values[i]-MathExp(hypergeometric_pdf[i]);
            delta_hypergeometric_cdf[i]=cdf_values[i]-MathExp(hypergeometric_cdf[i]);
            delta_hypergeometric_quantile[i]=x_values[i]-hypergeometric_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_hypergeometric_pdf[i]=pdf_values[i]-hypergeometric_pdf[i];
         delta_hypergeometric_cdf[i]=cdf_values[i]-hypergeometric_cdf[i];
         delta_hypergeometric_quantile[i]=x_values[i]-hypergeometric_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_hypergeometric_pdf[i]=pdf_values[i]-MathExp(hypergeometric_pdf[i]);
            delta_hypergeometric_cdf[i]=1.0-cdf_values[i]-MathExp(hypergeometric_cdf[i]);
            delta_hypergeometric_quantile[i]=x_values[i]-hypergeometric_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_hypergeometric_pdf[i]=pdf_values[i]-hypergeometric_pdf[i];
         delta_hypergeometric_cdf[i]=(1.0-cdf_values[i])-hypergeometric_cdf[i];
         delta_hypergeometric_quantile[i]=x_values[i]-hypergeometric_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_hypergeometric_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_hypergeometric_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_hypergeometric_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomHypergeometric(m,k,n,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double hypergeometric_mean=0;
      double hypergeometric_variance=0;
      double hypergeometric_skewness=0;
      double hypergeometric_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsHypergeometric(m,k,n,hypergeometric_mean,hypergeometric_variance,hypergeometric_skewness,hypergeometric_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",hypergeometric_mean,hypergeometric_variance,hypergeometric_skewness,hypergeometric_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-hypergeometric_mean,variance-hypergeometric_variance,skewness-hypergeometric_skewness,kurtosis-hypergeometric_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Logistic distribution                          |
//+------------------------------------------------------------------+
bool TestLogisticDistribution(string test_name,const double mu,const double sigma,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double logistic_pdf[];
   double logistic_cdf[];
   double logistic_quantile[];
//--- arrays for differences
   double delta_logistic_pdf[];
   double delta_logistic_cdf[];
   double delta_logistic_quantile[];
//--- prepare arrays
   ArrayResize(logistic_pdf,N);
   ArrayResize(logistic_cdf,N);
   ArrayResize(logistic_quantile,N);
   ArrayResize(delta_logistic_pdf,N);
   ArrayResize(delta_logistic_cdf,N);
   ArrayResize(delta_logistic_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      logistic_pdf[i]=MathProbabilityDensityLogistic(x_values[i],mu,sigma,error_code);
      logistic_cdf[i]=MathCumulativeDistributionLogistic(x_values[i],mu,sigma,error_code);
      logistic_quantile[i]=MathQuantileLogistic(logistic_cdf[i],mu,sigma,error_code);
      //--- calculate difference
      delta_logistic_pdf[i]=pdf_values[i]-logistic_pdf[i];
      delta_logistic_cdf[i]=cdf_values[i]-logistic_cdf[i];
      delta_logistic_quantile[i]=x_values[i]-logistic_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_logistic_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_logistic_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_logistic_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityLogistic(x_values,mu,sigma,logistic_pdf))
      return false;
   if(!MathCumulativeDistributionLogistic(x_values,mu,sigma,logistic_cdf))
      return false;
   if(!MathQuantileLogistic(logistic_cdf,mu,sigma,logistic_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_logistic_pdf,N);
   ArrayResize(delta_logistic_cdf,N);
   ArrayResize(delta_logistic_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_logistic_pdf[i]=pdf_values[i]-logistic_pdf[i];
      delta_logistic_cdf[i]=cdf_values[i]-logistic_cdf[i];
      delta_logistic_quantile[i]=x_values[i]-logistic_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_logistic_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_logistic_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_logistic_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_logistic_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_logistic_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_logistic_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_logistic_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_logistic_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_logistic_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityLogistic(x_values,mu,sigma,log_mode,logistic_pdf))
      return false;
   if(!MathCumulativeDistributionLogistic(x_values,mu,sigma,tail,log_mode,logistic_cdf))
      return false;
   if(!MathQuantileLogistic(logistic_cdf,mu,sigma,tail,log_mode,logistic_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_logistic_pdf[i]=pdf_values[i]-MathExp(logistic_pdf[i]);
            delta_logistic_cdf[i]=cdf_values[i]-MathExp(logistic_cdf[i]);
            delta_logistic_quantile[i]=x_values[i]-logistic_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_logistic_pdf[i]=pdf_values[i]-logistic_pdf[i];
         delta_logistic_cdf[i]=cdf_values[i]-logistic_cdf[i];
         delta_logistic_quantile[i]=x_values[i]-logistic_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_logistic_pdf[i]=pdf_values[i]-MathExp(logistic_pdf[i]);
            delta_logistic_cdf[i]=(1.0-cdf_values[i])-MathExp(logistic_cdf[i]);
            delta_logistic_quantile[i]=x_values[i]-logistic_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_logistic_pdf[i]=pdf_values[i]-logistic_pdf[i];
         delta_logistic_cdf[i]=(1.0-cdf_values[i])-logistic_cdf[i];
         delta_logistic_quantile[i]=x_values[i]-logistic_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_logistic_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_logistic_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_logistic_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomLogistic(mu,sigma,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double logistic_mean=0;
      double logistic_variance=0;
      double logistic_skewness=0;
      double logistic_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsLogistic(mu,sigma,logistic_mean,logistic_variance,logistic_skewness,logistic_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",logistic_mean,logistic_variance,logistic_skewness,logistic_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-logistic_mean,variance-logistic_variance,skewness-logistic_skewness,kurtosis-logistic_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Weibull distribution                           |
//+------------------------------------------------------------------+
bool TestWeibullDistribution(string test_name,const double a,const double b,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double weibull_pdf[];
   double weibull_cdf[];
   double weibull_quantile[];
//--- arrays for differences
   double delta_weibull_pdf[];
   double delta_weibull_cdf[];
   double delta_weibull_quantile[];
//--- prepare arrays
   ArrayResize(weibull_pdf,N);
   ArrayResize(weibull_cdf,N);
   ArrayResize(weibull_quantile,N);
   ArrayResize(delta_weibull_pdf,N);
   ArrayResize(delta_weibull_cdf,N);
   ArrayResize(delta_weibull_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      weibull_pdf[i]=MathProbabilityDensityWeibull(x_values[i],a,b,error_code);
      weibull_cdf[i]=MathCumulativeDistributionWeibull(x_values[i],a,b,error_code);
      weibull_quantile[i]=MathQuantileWeibull(weibull_cdf[i],a,b,error_code);
      //--- calculate difference
      delta_weibull_pdf[i]=pdf_values[i]-weibull_pdf[i];
      delta_weibull_cdf[i]=cdf_values[i]-weibull_cdf[i];
      delta_weibull_quantile[i]=x_values[i]-weibull_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_weibull_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_weibull_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_weibull_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityWeibull(x_values,a,b,weibull_pdf))
      return false;
   if(!MathCumulativeDistributionWeibull(x_values,a,b,weibull_cdf))
      return false;
   if(!MathQuantileWeibull(weibull_cdf,a,b,weibull_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_weibull_pdf,N);
   ArrayResize(delta_weibull_cdf,N);
   ArrayResize(delta_weibull_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_weibull_pdf[i]=pdf_values[i]-weibull_pdf[i];
      delta_weibull_cdf[i]=cdf_values[i]-weibull_cdf[i];
      delta_weibull_quantile[i]=x_values[i]-weibull_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_weibull_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_weibull_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_weibull_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_weibull_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_weibull_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_weibull_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_weibull_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_weibull_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_weibull_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityWeibull(x_values,a,b,log_mode,weibull_pdf))
      return false;
   if(!MathCumulativeDistributionWeibull(x_values,a,b,tail,log_mode,weibull_cdf))
      return false;
   if(!MathQuantileWeibull(weibull_cdf,a,b,tail,log_mode,weibull_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_weibull_pdf[i]=pdf_values[i]-MathExp(weibull_pdf[i]);
            delta_weibull_cdf[i]=cdf_values[i]-MathExp(weibull_cdf[i]);
            delta_weibull_quantile[i]=x_values[i]-weibull_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_weibull_pdf[i]=pdf_values[i]-weibull_pdf[i];
         delta_weibull_cdf[i]=cdf_values[i]-weibull_cdf[i];
         delta_weibull_quantile[i]=x_values[i]-weibull_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_weibull_pdf[i]=pdf_values[i]-MathExp(weibull_pdf[i]);
            delta_weibull_cdf[i]=1.0-cdf_values[i]-MathExp(weibull_cdf[i]);
            delta_weibull_quantile[i]=x_values[i]-weibull_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_weibull_pdf[i]=pdf_values[i]-weibull_pdf[i];
         delta_weibull_cdf[i]=(1.0-cdf_values[i])-weibull_cdf[i];
         delta_weibull_quantile[i]=x_values[i]-weibull_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_weibull_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_weibull_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_weibull_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomWeibull(a,b,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double weibull_mean=0;
      double weibull_variance=0;
      double weibull_skewness=0;
      double weibull_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsWeibull(a,b,weibull_mean,weibull_variance,weibull_skewness,weibull_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",weibull_mean,weibull_variance,weibull_skewness,weibull_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-weibull_mean,variance-weibull_variance,skewness-weibull_skewness,kurtosis-weibull_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Poisson distribution                           |
//+------------------------------------------------------------------+
bool TestPoissonDistribution(string test_name,const double lambda,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double poisson_pdf[];
   double poisson_cdf[];
   double poisson_quantile[];
//--- arrays for differences
   double delta_poisson_pdf[];
   double delta_poisson_cdf[];
   double delta_poisson_quantile[];
//--- prepare arrays
   ArrayResize(poisson_pdf,N);
   ArrayResize(poisson_cdf,N);
   ArrayResize(poisson_quantile,N);
   ArrayResize(delta_poisson_pdf,N);
   ArrayResize(delta_poisson_cdf,N);
   ArrayResize(delta_poisson_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      poisson_pdf[i]=MathProbabilityDensityPoisson(x_values[i],lambda,error_code);
      poisson_cdf[i]=MathCumulativeDistributionPoisson(x_values[i],lambda,error_code);
      poisson_quantile[i]=MathQuantilePoisson(poisson_cdf[i],lambda,error_code);
      //--- calculate difference
      delta_poisson_pdf[i]=pdf_values[i]-poisson_pdf[i];
      delta_poisson_cdf[i]=cdf_values[i]-poisson_cdf[i];
      delta_poisson_quantile[i]=x_values[i]-poisson_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_poisson_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_poisson_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_poisson_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityPoisson(x_values,lambda,poisson_pdf))
      return false;
   if(!MathCumulativeDistributionPoisson(x_values,lambda,poisson_cdf))
      return false;
   if(!MathQuantilePoisson(poisson_cdf,lambda,poisson_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_poisson_pdf,N);
   ArrayResize(delta_poisson_cdf,N);
   ArrayResize(delta_poisson_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_poisson_pdf[i]=pdf_values[i]-poisson_pdf[i];
      delta_poisson_cdf[i]=cdf_values[i]-poisson_cdf[i];
      delta_poisson_quantile[i]=x_values[i]-poisson_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_poisson_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_poisson_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_poisson_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_poisson_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_poisson_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_poisson_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_poisson_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_poisson_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_poisson_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityPoisson(x_values,lambda,log_mode,poisson_pdf))
      return false;
   if(!MathCumulativeDistributionPoisson(x_values,lambda,tail,log_mode,poisson_cdf))
      return false;
   if(!MathQuantilePoisson(poisson_cdf,lambda,tail,log_mode,poisson_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_poisson_pdf[i]=pdf_values[i]-MathExp(poisson_pdf[i]);
            delta_poisson_cdf[i]=cdf_values[i]-MathExp(poisson_cdf[i]);
            delta_poisson_quantile[i]=x_values[i]-poisson_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_poisson_pdf[i]=pdf_values[i]-poisson_pdf[i];
         delta_poisson_cdf[i]=cdf_values[i]-poisson_cdf[i];
         delta_poisson_quantile[i]=x_values[i]-poisson_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_poisson_pdf[i]=pdf_values[i]-MathExp(poisson_pdf[i]);
            delta_poisson_cdf[i]=(1.0-cdf_values[i])-MathExp(poisson_cdf[i]);
            delta_poisson_quantile[i]=x_values[i]-poisson_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_poisson_pdf[i]=pdf_values[i]-poisson_pdf[i];
         delta_poisson_cdf[i]=(1.0-cdf_values[i])-poisson_cdf[i];
         delta_poisson_quantile[i]=x_values[i]-poisson_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_poisson_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_poisson_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_poisson_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomPoisson(lambda,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double poisson_mean=0;
      double poisson_variance=0;
      double poisson_skewness=0;
      double poisson_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsPoisson(lambda,poisson_mean,poisson_variance,poisson_skewness,poisson_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",poisson_mean,poisson_variance,poisson_skewness,poisson_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-poisson_mean,variance-poisson_variance,skewness-poisson_skewness,kurtosis-poisson_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for F distribution                                 |
//+------------------------------------------------------------------+
bool TestFDistribution(string test_name,const double nu1,const double nu2,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double f_pdf[];
   double f_cdf[];
   double f_quantile[];
//--- arrays for differences
   double delta_f_pdf[];
   double delta_f_cdf[];
   double delta_f_quantile[];
//--- prepare arrays
   ArrayResize(f_pdf,N);
   ArrayResize(f_cdf,N);
   ArrayResize(f_quantile,N);
   ArrayResize(delta_f_pdf,N);
   ArrayResize(delta_f_cdf,N);
   ArrayResize(delta_f_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      f_pdf[i]=MathProbabilityDensityF(x_values[i],nu1,nu2,error_code);
      f_cdf[i]=MathCumulativeDistributionF(x_values[i],nu1,nu2,error_code);
      f_quantile[i]=MathQuantileF(f_cdf[i],nu1,nu2,error_code);
      //--- calculate difference
      delta_f_pdf[i]=pdf_values[i]-f_pdf[i];
      delta_f_cdf[i]=cdf_values[i]-f_cdf[i];
      delta_f_quantile[i]=x_values[i]-f_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_f_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_f_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_f_quantile[i])>calc_precision_quantile)
         return false;
     }

//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityF(x_values,nu1,nu2,f_pdf))
      return false;
   if(!MathCumulativeDistributionF(x_values,nu1,nu2,f_cdf))
      return false;
   if(!MathQuantileF(f_cdf,nu1,nu2,f_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_f_pdf,N);
   ArrayResize(delta_f_cdf,N);
   ArrayResize(delta_f_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_f_pdf[i]=pdf_values[i]-f_pdf[i];
      delta_f_cdf[i]=cdf_values[i]-f_cdf[i];
      delta_f_quantile[i]=x_values[i]-f_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_f_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_f_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_f_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_f_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_f_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_f_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_f_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_f_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_f_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityF(x_values,nu1,nu2,log_mode,f_pdf))
      return false;
   if(!MathCumulativeDistributionF(x_values,nu1,nu2,tail,log_mode,f_cdf))
      return false;
   if(!MathQuantileF(f_cdf,nu1,nu2,tail,log_mode,f_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_f_pdf[i]=pdf_values[i]-MathExp(f_pdf[i]);
            delta_f_cdf[i]=cdf_values[i]-MathExp(f_cdf[i]);
            delta_f_quantile[i]=x_values[i]-f_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_f_pdf[i]=pdf_values[i]-f_pdf[i];
         delta_f_cdf[i]=cdf_values[i]-f_cdf[i];
         delta_f_quantile[i]=x_values[i]-f_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_f_pdf[i]=pdf_values[i]-MathExp(f_pdf[i]);
            delta_f_cdf[i]=(1.0-cdf_values[i])-MathExp(f_cdf[i]);
            delta_f_quantile[i]=x_values[i]-f_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_f_pdf[i]=pdf_values[i]-f_pdf[i];
         delta_f_cdf[i]=(1.0-cdf_values[i])-f_cdf[i];
         delta_f_quantile[i]=x_values[i]-f_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_f_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_f_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_f_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomF(nu1,nu2,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double f_mean=0;
      double f_variance=0;
      double f_skewness=0;
      double f_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsF(nu1,nu2,f_mean,f_variance,f_skewness,f_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",f_mean,f_variance,f_skewness,f_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-f_mean,variance-f_variance,skewness-f_skewness,kurtosis-f_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for ChiSquare distribution                         |
//+------------------------------------------------------------------+
bool TestChiSquareDistribution(string test_name,const double nu,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double chisquare_pdf[];
   double chisquare_cdf[];
   double chisquare_quantile[];
//--- arrays for differences
   double delta_chisquare_pdf[];
   double delta_chisquare_cdf[];
   double delta_chisquare_quantile[];
//--- prepare arrays
   ArrayResize(chisquare_pdf,N);
   ArrayResize(chisquare_cdf,N);
   ArrayResize(chisquare_quantile,N);
   ArrayResize(delta_chisquare_pdf,N);
   ArrayResize(delta_chisquare_cdf,N);
   ArrayResize(delta_chisquare_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      chisquare_pdf[i]=MathProbabilityDensityChiSquare(x_values[i],nu,error_code);
      chisquare_cdf[i]=MathCumulativeDistributionChiSquare(x_values[i],nu,error_code);
      chisquare_quantile[i]=MathQuantileChiSquare(chisquare_cdf[i],nu,error_code);
      //--- calculate difference
      delta_chisquare_pdf[i]=pdf_values[i]-chisquare_pdf[i];
      delta_chisquare_cdf[i]=cdf_values[i]-chisquare_cdf[i];
      delta_chisquare_quantile[i]=x_values[i]-chisquare_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_chisquare_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_chisquare_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_chisquare_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityChiSquare(x_values,nu,chisquare_pdf))
      return false;
   if(!MathCumulativeDistributionChiSquare(x_values,nu,chisquare_cdf))
      return false;
   if(!MathQuantileChiSquare(chisquare_cdf,nu,chisquare_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_chisquare_pdf,N);
   ArrayResize(delta_chisquare_cdf,N);
   ArrayResize(delta_chisquare_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_chisquare_pdf[i]=pdf_values[i]-chisquare_pdf[i];
      delta_chisquare_cdf[i]=cdf_values[i]-chisquare_cdf[i];
      delta_chisquare_quantile[i]=x_values[i]-chisquare_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_chisquare_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_chisquare_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_chisquare_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_chisquare_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_chisquare_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_chisquare_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_chisquare_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_chisquare_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_chisquare_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityChiSquare(x_values,nu,log_mode,chisquare_pdf))
      return false;
   if(!MathCumulativeDistributionChiSquare(x_values,nu,tail,log_mode,chisquare_cdf))
      return false;
   if(!MathQuantileChiSquare(chisquare_cdf,nu,tail,log_mode,chisquare_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_chisquare_pdf[i]=pdf_values[i]-MathExp(chisquare_pdf[i]);
            delta_chisquare_cdf[i]=cdf_values[i]-MathExp(chisquare_cdf[i]);
            delta_chisquare_quantile[i]=x_values[i]-chisquare_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_chisquare_pdf[i]=pdf_values[i]-chisquare_pdf[i];
         delta_chisquare_cdf[i]=cdf_values[i]-chisquare_cdf[i];
         delta_chisquare_quantile[i]=x_values[i]-chisquare_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_chisquare_pdf[i]=pdf_values[i]-MathExp(chisquare_pdf[i]);
            delta_chisquare_cdf[i]=(1.0-cdf_values[i])-MathExp(chisquare_cdf[i]);
            delta_chisquare_quantile[i]=x_values[i]-chisquare_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_chisquare_pdf[i]=pdf_values[i]-chisquare_pdf[i];
         delta_chisquare_cdf[i]=(1.0-cdf_values[i])-chisquare_cdf[i];
         delta_chisquare_quantile[i]=x_values[i]-chisquare_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_chisquare_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_chisquare_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_chisquare_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomChiSquare(nu,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double chisquare_mean=0;
      double chisquare_variance=0;
      double chisquare_skewness=0;
      double chisquare_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsChiSquare(nu,chisquare_mean,chisquare_variance,chisquare_skewness,chisquare_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",chisquare_mean,chisquare_variance,chisquare_skewness,chisquare_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-chisquare_mean,variance-chisquare_variance,skewness-chisquare_skewness,kurtosis-chisquare_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for NoncentralChiSquare distribution               |
//+------------------------------------------------------------------+
bool TestNoncentralChiSquareDistribution(string test_name,const double nu,const double sigma,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double noncentral_chisquare_pdf[];
   double noncentral_chisquare_cdf[];
   double noncentral_chisquare_quantile[];
//--- arrays for differences
   double delta_noncentral_chisquare_pdf[];
   double delta_noncentral_chisquare_cdf[];
   double delta_noncentral_chisquare_quantile[];
//--- prepare arrays
   ArrayResize(noncentral_chisquare_pdf,N);
   ArrayResize(noncentral_chisquare_cdf,N);
   ArrayResize(noncentral_chisquare_quantile,N);
   ArrayResize(delta_noncentral_chisquare_pdf,N);
   ArrayResize(delta_noncentral_chisquare_cdf,N);
   ArrayResize(delta_noncentral_chisquare_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      noncentral_chisquare_pdf[i]=MathProbabilityDensityNoncentralChiSquare(x_values[i],nu,sigma,error_code);
      noncentral_chisquare_cdf[i]=MathCumulativeDistributionNoncentralChiSquare(x_values[i],nu,sigma,error_code);
      noncentral_chisquare_quantile[i]=MathQuantileNoncentralChiSquare(noncentral_chisquare_cdf[i],nu,sigma,error_code);
      //--- calculate difference
      delta_noncentral_chisquare_pdf[i]=pdf_values[i]-noncentral_chisquare_pdf[i];
      delta_noncentral_chisquare_cdf[i]=cdf_values[i]-noncentral_chisquare_cdf[i];
      delta_noncentral_chisquare_quantile[i]=x_values[i]-noncentral_chisquare_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_chisquare_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_chisquare_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_chisquare_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNoncentralChiSquare(x_values,nu,sigma,noncentral_chisquare_pdf))
      return false;
   if(!MathCumulativeDistributionNoncentralChiSquare(x_values,nu,sigma,noncentral_chisquare_cdf))
      return false;
   if(!MathQuantileNoncentralChiSquare(noncentral_chisquare_cdf,nu,sigma,noncentral_chisquare_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_noncentral_chisquare_pdf,N);
   ArrayResize(delta_noncentral_chisquare_cdf,N);
   ArrayResize(delta_noncentral_chisquare_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_noncentral_chisquare_pdf[i]=pdf_values[i]-noncentral_chisquare_pdf[i];
      delta_noncentral_chisquare_cdf[i]=cdf_values[i]-noncentral_chisquare_cdf[i];
      delta_noncentral_chisquare_quantile[i]=x_values[i]-noncentral_chisquare_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_noncentral_chisquare_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_noncentral_chisquare_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_noncentral_chisquare_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_noncentral_chisquare_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_noncentral_chisquare_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_noncentral_chisquare_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_chisquare_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_chisquare_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_chisquare_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNoncentralChiSquare(x_values,nu,sigma,log_mode,noncentral_chisquare_pdf))
      return false;
   if(!MathCumulativeDistributionNoncentralChiSquare(x_values,nu,sigma,tail,log_mode,noncentral_chisquare_cdf))
      return false;
   if(!MathQuantileNoncentralChiSquare(noncentral_chisquare_cdf,nu,sigma,tail,log_mode,noncentral_chisquare_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_noncentral_chisquare_pdf[i]=pdf_values[i]-MathExp(noncentral_chisquare_pdf[i]);
            delta_noncentral_chisquare_cdf[i]=cdf_values[i]-MathExp(noncentral_chisquare_cdf[i]);
            delta_noncentral_chisquare_quantile[i]=x_values[i]-noncentral_chisquare_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_noncentral_chisquare_pdf[i]=pdf_values[i]-noncentral_chisquare_pdf[i];
         delta_noncentral_chisquare_cdf[i]=cdf_values[i]-noncentral_chisquare_cdf[i];
         delta_noncentral_chisquare_quantile[i]=x_values[i]-noncentral_chisquare_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_noncentral_chisquare_pdf[i]=pdf_values[i]-MathExp(noncentral_chisquare_pdf[i]);
            delta_noncentral_chisquare_cdf[i]=(1.0-cdf_values[i])-MathExp(noncentral_chisquare_cdf[i]);
            delta_noncentral_chisquare_quantile[i]=x_values[i]-noncentral_chisquare_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_noncentral_chisquare_pdf[i]=pdf_values[i]-noncentral_chisquare_pdf[i];
         delta_noncentral_chisquare_cdf[i]=(1.0-cdf_values[i])-noncentral_chisquare_cdf[i];
         delta_noncentral_chisquare_quantile[i]=x_values[i]-noncentral_chisquare_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      //PrintFormat("3 %d, dPDF=%.20e, dCDF=%.20e, dQ=%.20e,",i,delta_noncentral_chisquare_pdf[i],delta_noncentral_chisquare_cdf[i],delta_noncentral_chisquare_quantile[i]);
      if(MathAbs(delta_noncentral_chisquare_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_chisquare_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_chisquare_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomNoncentralChiSquare(nu,sigma,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double noncentral_chisquare_mean=0;
      double noncentral_chisquare_variance=0;
      double noncentral_chisquare_skewness=0;
      double noncentral_chisquare_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsNoncentralChiSquare(nu,sigma,noncentral_chisquare_mean,noncentral_chisquare_variance,noncentral_chisquare_skewness,noncentral_chisquare_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",noncentral_chisquare_mean,noncentral_chisquare_variance,noncentral_chisquare_skewness,noncentral_chisquare_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-noncentral_chisquare_mean,variance-noncentral_chisquare_variance,skewness-noncentral_chisquare_skewness,kurtosis-noncentral_chisquare_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for NoncentralF distribution                       |
//+------------------------------------------------------------------+
bool TestNoncentralFDistribution(string test_name,const double nu1,const double nu2,const double sigma,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double noncentral_f_pdf[];
   double noncentral_f_cdf[];
   double noncentral_f_quantile[];
//--- arrays for differences
   double delta_noncentral_f_pdf[];
   double delta_noncentral_f_cdf[];
   double delta_noncentral_f_quantile[];
//--- prepare arrays
   ArrayResize(noncentral_f_pdf,N);
   ArrayResize(noncentral_f_cdf,N);
   ArrayResize(noncentral_f_quantile,N);
   ArrayResize(delta_noncentral_f_pdf,N);
   ArrayResize(delta_noncentral_f_cdf,N);
   ArrayResize(delta_noncentral_f_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      noncentral_f_pdf[i]=MathProbabilityDensityNoncentralF(x_values[i],nu1,nu2,sigma,error_code);
      noncentral_f_cdf[i]=MathCumulativeDistributionNoncentralF(x_values[i],nu1,nu2,sigma,error_code);
      noncentral_f_quantile[i]=MathQuantileNoncentralF(noncentral_f_cdf[i],nu1,nu2,sigma,error_code);
      //--- calculate difference
      delta_noncentral_f_pdf[i]=pdf_values[i]-noncentral_f_pdf[i];
      delta_noncentral_f_cdf[i]=cdf_values[i]-noncentral_f_cdf[i];
      delta_noncentral_f_quantile[i]=x_values[i]-noncentral_f_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_f_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_f_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_f_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNoncentralF(x_values,nu1,nu2,sigma,noncentral_f_pdf))
      return false;
   if(!MathCumulativeDistributionNoncentralF(x_values,nu1,nu2,sigma,noncentral_f_cdf))
      return false;
   if(!MathQuantileNoncentralF(noncentral_f_cdf,nu1,nu2,sigma,noncentral_f_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_noncentral_f_pdf,N);
   ArrayResize(delta_noncentral_f_cdf,N);
   ArrayResize(delta_noncentral_f_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_noncentral_f_pdf[i]=pdf_values[i]-noncentral_f_pdf[i];
      delta_noncentral_f_cdf[i]=cdf_values[i]-noncentral_f_cdf[i];
      delta_noncentral_f_quantile[i]=x_values[i]-noncentral_f_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_noncentral_f_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_noncentral_f_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_noncentral_f_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_noncentral_f_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_noncentral_f_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_noncentral_f_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_f_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_f_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_f_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNoncentralF(x_values,nu1,nu2,sigma,log_mode,noncentral_f_pdf))
      return false;
   if(!MathCumulativeDistributionNoncentralF(x_values,nu1,nu2,sigma,tail,log_mode,noncentral_f_cdf))
      return false;
   if(!MathQuantileNoncentralF(noncentral_f_cdf,nu1,nu2,sigma,tail,log_mode,noncentral_f_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_noncentral_f_pdf[i]=pdf_values[i]-MathExp(noncentral_f_pdf[i]);
            delta_noncentral_f_cdf[i]=cdf_values[i]-MathExp(noncentral_f_cdf[i]);
            delta_noncentral_f_quantile[i]=x_values[i]-noncentral_f_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_noncentral_f_pdf[i]=pdf_values[i]-noncentral_f_pdf[i];
         delta_noncentral_f_cdf[i]=cdf_values[i]-noncentral_f_cdf[i];
         delta_noncentral_f_quantile[i]=x_values[i]-noncentral_f_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_noncentral_f_pdf[i]=pdf_values[i]-MathExp(noncentral_f_pdf[i]);
            delta_noncentral_f_cdf[i]=(1.0-cdf_values[i])-MathExp(noncentral_f_cdf[i]);
            delta_noncentral_f_quantile[i]=x_values[i]-noncentral_f_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_noncentral_f_pdf[i]=pdf_values[i]-noncentral_f_pdf[i];
         delta_noncentral_f_cdf[i]=(1.0-cdf_values[i])-noncentral_f_cdf[i];
         delta_noncentral_f_quantile[i]=x_values[i]-noncentral_f_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      //PrintFormat("3 %d, dPDF=%.20e, dCDF=%.20e, dQ=%.20e,",i,delta_noncentral_f_pdf[i],delta_noncentral_f_cdf[i],delta_noncentral_f_quantile[i]);
      if(MathAbs(delta_noncentral_f_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_f_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_f_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomNoncentralF(nu1,nu2,sigma,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double noncentral_f_mean=0;
      double noncentral_f_variance=0;
      double noncentral_f_skewness=0;
      double noncentral_f_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsNoncentralF(nu1,nu2,sigma,noncentral_f_mean,noncentral_f_variance,noncentral_f_skewness,noncentral_f_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",noncentral_f_mean,noncentral_f_variance,noncentral_f_skewness,noncentral_f_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-noncentral_f_mean,variance-noncentral_f_variance,skewness-noncentral_f_skewness,kurtosis-noncentral_f_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Noncentral Beta distribution                   |
//+------------------------------------------------------------------+
bool TestNoncentralBetaDistribution(string test_name,const double a,const double b,const double lambda,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double noncentral_beta_pdf[];
   double noncentral_beta_cdf[];
   double noncentral_beta_quantile[];
//--- arrays for differences
   double delta_noncentral_beta_pdf[];
   double delta_noncentral_beta_cdf[];
   double delta_noncentral_beta_quantile[];
//--- prepare arrays
   ArrayResize(noncentral_beta_pdf,N);
   ArrayResize(noncentral_beta_cdf,N);
   ArrayResize(noncentral_beta_quantile,N);
   ArrayResize(delta_noncentral_beta_pdf,N);
   ArrayResize(delta_noncentral_beta_cdf,N);
   ArrayResize(delta_noncentral_beta_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      noncentral_beta_pdf[i]=MathProbabilityDensityNoncentralBeta(x_values[i],a,b,lambda,error_code);
      noncentral_beta_cdf[i]=MathCumulativeDistributionNoncentralBeta(x_values[i],a,b,lambda,error_code);
      noncentral_beta_quantile[i]=MathQuantileNoncentralBeta(noncentral_beta_cdf[i],a,b,lambda,error_code);
      //--- calculate difference
      delta_noncentral_beta_pdf[i]=pdf_values[i]-noncentral_beta_pdf[i];
      delta_noncentral_beta_cdf[i]=cdf_values[i]-noncentral_beta_cdf[i];
      delta_noncentral_beta_quantile[i]=x_values[i]-noncentral_beta_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_beta_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_beta_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_beta_quantile[i])>calc_precision_quantile)
         return false;
     }

//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNoncentralBeta(x_values,a,b,lambda,noncentral_beta_pdf))
      return false;
   if(!MathCumulativeDistributionNoncentralBeta(x_values,a,b,lambda,noncentral_beta_cdf))
      return false;
   if(!MathQuantileNoncentralBeta(noncentral_beta_cdf,a,b,lambda,noncentral_beta_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_noncentral_beta_pdf,N);
   ArrayResize(delta_noncentral_beta_cdf,N);
   ArrayResize(delta_noncentral_beta_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_noncentral_beta_pdf[i]=pdf_values[i]-noncentral_beta_pdf[i];
      delta_noncentral_beta_cdf[i]=cdf_values[i]-noncentral_beta_cdf[i];
      delta_noncentral_beta_quantile[i]=x_values[i]-noncentral_beta_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_noncentral_beta_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_noncentral_beta_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_noncentral_beta_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_noncentral_beta_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_noncentral_beta_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_noncentral_beta_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_beta_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_beta_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_beta_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNoncentralBeta(x_values,a,b,lambda,log_mode,noncentral_beta_pdf))
      return false;
   if(!MathCumulativeDistributionNoncentralBeta(x_values,a,b,lambda,tail,log_mode,noncentral_beta_cdf))
      return false;
   if(!MathQuantileNoncentralBeta(noncentral_beta_cdf,a,b,lambda,tail,log_mode,noncentral_beta_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_noncentral_beta_pdf[i]=pdf_values[i]-MathExp(noncentral_beta_pdf[i]);
            delta_noncentral_beta_cdf[i]=cdf_values[i]-MathExp(noncentral_beta_cdf[i]);
            delta_noncentral_beta_quantile[i]=x_values[i]-noncentral_beta_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_noncentral_beta_pdf[i]=pdf_values[i]-noncentral_beta_pdf[i];
         delta_noncentral_beta_cdf[i]=cdf_values[i]-noncentral_beta_cdf[i];
         delta_noncentral_beta_quantile[i]=x_values[i]-noncentral_beta_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_noncentral_beta_pdf[i]=pdf_values[i]-MathExp(noncentral_beta_pdf[i]);
            delta_noncentral_beta_cdf[i]=(1.0-cdf_values[i])-MathExp(noncentral_beta_cdf[i]);
            delta_noncentral_beta_quantile[i]=x_values[i]-noncentral_beta_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_noncentral_beta_pdf[i]=pdf_values[i]-noncentral_beta_pdf[i];
         delta_noncentral_beta_cdf[i]=(1.0-cdf_values[i])-noncentral_beta_cdf[i];
         delta_noncentral_beta_quantile[i]=x_values[i]-noncentral_beta_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_beta_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_beta_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_beta_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomNoncentralBeta(a,b,lambda,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double noncentral_beta_mean=0;
      double noncentral_beta_variance=0;
      double noncentral_beta_skewness=0;
      double noncentral_beta_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsNoncentralBeta(a,b,lambda,noncentral_beta_mean,noncentral_beta_variance,noncentral_beta_skewness,noncentral_beta_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f skewness=%.10f  kurtosis=%.10f",noncentral_beta_mean,noncentral_beta_variance,noncentral_beta_skewness,noncentral_beta_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-noncentral_beta_mean,variance-noncentral_beta_variance,skewness-noncentral_beta_skewness,kurtosis-noncentral_beta_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Negative Binomial distribution                 |
//+------------------------------------------------------------------+
bool TestNegativeBinomialDistribution(string test_name,const double r,const double p,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double negative_binomial_pdf[];
   double negative_binomial_cdf[];
   double negative_binomial_quantile[];
//--- arrays for differences
   double delta_negative_binomial_pdf[];
   double delta_negative_binomial_cdf[];
   double delta_negative_binomial_quantile[];
//--- prepare arrays
   ArrayResize(negative_binomial_pdf,N);
   ArrayResize(negative_binomial_cdf,N);
   ArrayResize(negative_binomial_quantile,N);
   ArrayResize(delta_negative_binomial_pdf,N);
   ArrayResize(delta_negative_binomial_cdf,N);
   ArrayResize(delta_negative_binomial_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      negative_binomial_pdf[i]=MathProbabilityDensityNegativeBinomial(x_values[i],r,p,error_code);
      negative_binomial_cdf[i]=MathCumulativeDistributionNegativeBinomial(x_values[i],r,p,error_code);
      negative_binomial_quantile[i]=MathQuantileNegativeBinomial(negative_binomial_cdf[i],r,p,error_code);
      //--- calculate difference
      delta_negative_binomial_pdf[i]=pdf_values[i]-negative_binomial_pdf[i];
      delta_negative_binomial_cdf[i]=cdf_values[i]-negative_binomial_cdf[i];
      delta_negative_binomial_quantile[i]=x_values[i]-negative_binomial_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_negative_binomial_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_negative_binomial_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_negative_binomial_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNegativeBinomial(x_values,r,p,negative_binomial_pdf))
      return false;
   if(!MathCumulativeDistributionNegativeBinomial(x_values,r,p,negative_binomial_cdf))
      return false;
   if(!MathQuantileNegativeBinomial(negative_binomial_cdf,r,p,negative_binomial_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_negative_binomial_pdf,N);
   ArrayResize(delta_negative_binomial_cdf,N);
   ArrayResize(delta_negative_binomial_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_negative_binomial_pdf[i]=pdf_values[i]-negative_binomial_pdf[i];
      delta_negative_binomial_cdf[i]=cdf_values[i]-negative_binomial_cdf[i];
      delta_negative_binomial_quantile[i]=x_values[i]-negative_binomial_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_negative_binomial_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_negative_binomial_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_negative_binomial_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_negative_binomial_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_negative_binomial_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_negative_binomial_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_negative_binomial_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_negative_binomial_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_negative_binomial_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNegativeBinomial(x_values,r,p,log_mode,negative_binomial_pdf))
      return false;
   if(!MathCumulativeDistributionNegativeBinomial(x_values,r,p,tail,log_mode,negative_binomial_cdf))
      return false;
   if(!MathQuantileNegativeBinomial(negative_binomial_cdf,r,p,tail,log_mode,negative_binomial_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_negative_binomial_pdf[i]=pdf_values[i]-MathExp(negative_binomial_pdf[i]);
            delta_negative_binomial_cdf[i]=cdf_values[i]-MathExp(negative_binomial_cdf[i]);
            delta_negative_binomial_quantile[i]=x_values[i]-negative_binomial_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_negative_binomial_pdf[i]=pdf_values[i]-negative_binomial_pdf[i];
         delta_negative_binomial_cdf[i]=cdf_values[i]-negative_binomial_cdf[i];
         delta_negative_binomial_quantile[i]=x_values[i]-negative_binomial_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_negative_binomial_pdf[i]=pdf_values[i]-MathExp(negative_binomial_pdf[i]);
            delta_negative_binomial_cdf[i]=(1.0-cdf_values[i])-MathExp(negative_binomial_cdf[i]);
            delta_negative_binomial_quantile[i]=x_values[i]-negative_binomial_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_negative_binomial_pdf[i]=pdf_values[i]-negative_binomial_pdf[i];
         delta_negative_binomial_cdf[i]=(1.0-cdf_values[i])-negative_binomial_cdf[i];
         delta_negative_binomial_quantile[i]=x_values[i]-negative_binomial_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_negative_binomial_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_negative_binomial_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_negative_binomial_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomNegativeBinomial(r,p,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double negative_binomial_mean=0;
      double negative_binomial_variance=0;
      double negative_binomial_skewness=0;
      double negative_binomial_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsNegativeBinomial(r,p,negative_binomial_mean,negative_binomial_variance,negative_binomial_skewness,negative_binomial_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",negative_binomial_mean,negative_binomial_variance,negative_binomial_skewness,negative_binomial_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-negative_binomial_mean,variance-negative_binomial_variance,skewness-negative_binomial_skewness,kurtosis-negative_binomial_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for T distribution                                 |
//+------------------------------------------------------------------+
bool TestTDistribution(string test_name,const double nu,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double t_pdf[];
   double t_cdf[];
   double t_quantile[];
//--- arrays for differences
   double delta_t_pdf[];
   double delta_t_cdf[];
   double delta_t_quantile[];
//--- prepare arrays
   ArrayResize(t_pdf,N);
   ArrayResize(t_cdf,N);
   ArrayResize(t_quantile,N);
   ArrayResize(delta_t_pdf,N);
   ArrayResize(delta_t_cdf,N);
   ArrayResize(delta_t_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      t_pdf[i]=MathProbabilityDensityT(x_values[i],nu,error_code);
      t_cdf[i]=MathCumulativeDistributionT(x_values[i],nu,error_code);
      t_quantile[i]=MathQuantileT(t_cdf[i],nu,error_code);
      //--- calculate difference
      delta_t_pdf[i]=pdf_values[i]-t_pdf[i];
      delta_t_cdf[i]=cdf_values[i]-t_cdf[i];
      delta_t_quantile[i]=x_values[i]-t_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_t_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_t_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_t_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityT(x_values,nu,t_pdf))
      return false;
   if(!MathCumulativeDistributionT(x_values,nu,t_cdf))
      return false;
   if(!MathQuantileT(t_cdf,nu,t_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_t_pdf,N);
   ArrayResize(delta_t_cdf,N);
   ArrayResize(delta_t_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_t_pdf[i]=pdf_values[i]-t_pdf[i];
      delta_t_cdf[i]=cdf_values[i]-t_cdf[i];
      delta_t_quantile[i]=x_values[i]-t_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_t_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_t_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_t_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_t_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_t_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_t_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_t_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_t_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_t_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityT(x_values,nu,log_mode,t_pdf))
      return false;
   if(!MathCumulativeDistributionT(x_values,nu,tail,log_mode,t_cdf))
      return false;
   if(!MathQuantileT(t_cdf,nu,tail,log_mode,t_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_t_pdf[i]=pdf_values[i]-MathExp(t_pdf[i]);
            delta_t_cdf[i]=cdf_values[i]-MathExp(t_cdf[i]);
            delta_t_quantile[i]=x_values[i]-t_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_t_pdf[i]=pdf_values[i]-t_pdf[i];
         delta_t_cdf[i]=cdf_values[i]-t_cdf[i];
         delta_t_quantile[i]=x_values[i]-t_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_t_pdf[i]=pdf_values[i]-MathExp(t_pdf[i]);
            delta_t_cdf[i]=(1.0-cdf_values[i])-MathExp(t_cdf[i]);
            delta_t_quantile[i]=x_values[i]-t_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_t_pdf[i]=pdf_values[i]-t_pdf[i];
         delta_t_cdf[i]=(1.0-cdf_values[i])-t_cdf[i];
         delta_t_quantile[i]=x_values[i]-t_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_t_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_t_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_t_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomT(nu,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double t_mean=0;
      double t_variance=0;
      double t_skewness=0;
      double t_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsT(nu,t_mean,t_variance,t_skewness,t_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",t_mean,t_variance,t_skewness,t_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-t_mean,variance-t_variance,skewness-t_skewness,kurtosis-t_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Noncentral T distribution                      |
//+------------------------------------------------------------------+
bool TestNoncentralTDistribution(string test_name,const double nu,const double delta,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double noncentral_t_pdf[];
   double noncentral_t_cdf[];
   double noncentral_t_quantile[];
//--- arrays for differences
   double delta_noncentral_t_pdf[];
   double delta_noncentral_t_cdf[];
   double delta_noncentral_t_quantile[];
//--- prepare arrays
   ArrayResize(noncentral_t_pdf,N);
   ArrayResize(noncentral_t_cdf,N);
   ArrayResize(noncentral_t_quantile,N);
   ArrayResize(delta_noncentral_t_pdf,N);
   ArrayResize(delta_noncentral_t_cdf,N);
   ArrayResize(delta_noncentral_t_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      noncentral_t_pdf[i]=MathProbabilityDensityNoncentralT(x_values[i],nu,delta,error_code);
      noncentral_t_cdf[i]=MathCumulativeDistributionNoncentralT(x_values[i],nu,delta,error_code);
      noncentral_t_quantile[i]=MathQuantileNoncentralT(noncentral_t_cdf[i],nu,delta,error_code);
      //--- calculate difference
      delta_noncentral_t_pdf[i]=pdf_values[i]-noncentral_t_pdf[i];
      delta_noncentral_t_cdf[i]=cdf_values[i]-noncentral_t_cdf[i];
      delta_noncentral_t_quantile[i]=x_values[i]-noncentral_t_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_t_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_t_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_t_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNoncentralT(x_values,nu,delta,noncentral_t_pdf))
      return false;
   if(!MathCumulativeDistributionNoncentralT(x_values,nu,delta,noncentral_t_cdf))
      return false;
   if(!MathQuantileNoncentralT(noncentral_t_cdf,nu,delta,noncentral_t_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_noncentral_t_pdf,N);
   ArrayResize(delta_noncentral_t_cdf,N);
   ArrayResize(delta_noncentral_t_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_noncentral_t_pdf[i]=pdf_values[i]-noncentral_t_pdf[i];
      delta_noncentral_t_cdf[i]=cdf_values[i]-noncentral_t_cdf[i];
      delta_noncentral_t_quantile[i]=x_values[i]-noncentral_t_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_noncentral_t_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_noncentral_t_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_noncentral_t_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_noncentral_t_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_noncentral_t_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_noncentral_t_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_t_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_t_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_t_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNoncentralT(x_values,nu,delta,log_mode,noncentral_t_pdf))
      return false;
   if(!MathCumulativeDistributionNoncentralT(x_values,nu,delta,tail,log_mode,noncentral_t_cdf))
      return false;
   if(!MathQuantileNoncentralT(noncentral_t_cdf,nu,delta,tail,log_mode,noncentral_t_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_noncentral_t_pdf[i]=pdf_values[i]-MathExp(noncentral_t_pdf[i]);
            delta_noncentral_t_cdf[i]=cdf_values[i]-MathExp(noncentral_t_cdf[i]);
            delta_noncentral_t_quantile[i]=x_values[i]-noncentral_t_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_noncentral_t_pdf[i]=pdf_values[i]-noncentral_t_pdf[i];
         delta_noncentral_t_cdf[i]=cdf_values[i]-noncentral_t_cdf[i];
         delta_noncentral_t_quantile[i]=x_values[i]-noncentral_t_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_noncentral_t_pdf[i]=pdf_values[i]-MathExp(noncentral_t_pdf[i]);
            delta_noncentral_t_cdf[i]=(1.0-cdf_values[i])-MathExp(noncentral_t_cdf[i]);
            delta_noncentral_t_quantile[i]=x_values[i]-noncentral_t_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_noncentral_t_pdf[i]=pdf_values[i]-noncentral_t_pdf[i];
         delta_noncentral_t_cdf[i]=(1.0-cdf_values[i])-noncentral_t_cdf[i];
         delta_noncentral_t_quantile[i]=x_values[i]-noncentral_t_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_noncentral_t_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_noncentral_t_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_noncentral_t_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomNoncentralT(nu,delta,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double noncentral_t_mean=0;
      double noncentral_t_variance=0;
      double noncentral_t_skewness=0;
      double noncentral_t_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsNoncentralT(nu,delta,noncentral_t_mean,noncentral_t_variance,noncentral_t_skewness,noncentral_t_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",noncentral_t_mean,noncentral_t_variance,noncentral_t_skewness,noncentral_t_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-noncentral_t_mean,variance-noncentral_t_variance,skewness-noncentral_t_skewness,kurtosis-noncentral_t_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Normal distribution                            |
//+------------------------------------------------------------------+
bool TestNormalDistribution(string test_name,const double mu,const double sigma,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double normal_pdf[];
   double normal_cdf[];
   double normal_quantile[];
//--- arrays for differences
   double delta_normal_pdf[];
   double delta_normal_cdf[];
   double delta_normal_quantile[];
//--- prepare arrays
   ArrayResize(normal_pdf,N);
   ArrayResize(normal_cdf,N);
   ArrayResize(normal_quantile,N);
   ArrayResize(delta_normal_pdf,N);
   ArrayResize(delta_normal_cdf,N);
   ArrayResize(delta_normal_quantile,N);

//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      normal_pdf[i]=MathProbabilityDensityNormal(x_values[i],mu,sigma,error_code);
      normal_cdf[i]=MathCumulativeDistributionNormal(x_values[i],mu,sigma,error_code);
      normal_quantile[i]=MathQuantileNormal(normal_cdf[i],mu,sigma,error_code);
      //--- calculate difference
      delta_normal_pdf[i]=pdf_values[i]-normal_pdf[i];
      delta_normal_cdf[i]=cdf_values[i]-normal_cdf[i];
      delta_normal_quantile[i]=x_values[i]-normal_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_normal_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_normal_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_normal_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNormal(x_values,mu,sigma,normal_pdf))
      return false;
   if(!MathCumulativeDistributionNormal(x_values,mu,sigma,normal_cdf))
      return false;
   if(!MathQuantileNormal(normal_cdf,mu,sigma,normal_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_normal_pdf,N);
   ArrayResize(delta_normal_cdf,N);
   ArrayResize(delta_normal_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_normal_pdf[i]=pdf_values[i]-normal_pdf[i];
      delta_normal_cdf[i]=cdf_values[i]-normal_cdf[i];
      delta_normal_quantile[i]=x_values[i]-normal_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_normal_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_normal_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_normal_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_normal_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_normal_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_normal_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_normal_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_normal_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_normal_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityNormal(x_values,mu,sigma,log_mode,normal_pdf))
      return false;
   if(!MathCumulativeDistributionNormal(x_values,mu,sigma,tail,log_mode,normal_cdf))
      return false;
   if(!MathQuantileNormal(normal_cdf,mu,sigma,tail,log_mode,normal_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_normal_pdf[i]=pdf_values[i]-MathExp(normal_pdf[i]);
            delta_normal_cdf[i]=cdf_values[i]-MathExp(normal_cdf[i]);
            delta_normal_quantile[i]=x_values[i]-normal_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_normal_pdf[i]=pdf_values[i]-normal_pdf[i];
         delta_normal_cdf[i]=cdf_values[i]-normal_cdf[i];
         delta_normal_quantile[i]=x_values[i]-normal_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_normal_pdf[i]=pdf_values[i]-MathExp(normal_pdf[i]);
            delta_normal_cdf[i]=(1.0-cdf_values[i])-MathExp(normal_cdf[i]);
            delta_normal_quantile[i]=x_values[i]-normal_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_normal_pdf[i]=pdf_values[i]-normal_pdf[i];
         delta_normal_cdf[i]=(1.0-cdf_values[i])-normal_cdf[i];
         delta_normal_quantile[i]=x_values[i]-normal_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_normal_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_normal_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_normal_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomNormal(mu,sigma,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double normal_mean=0;
      double normal_variance=0;
      double normal_skewness=0;
      double normal_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsNormal(mu,sigma,normal_mean,normal_variance,normal_skewness,normal_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",normal_mean,normal_variance,normal_skewness,normal_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-normal_mean,variance-normal_variance,skewness-normal_skewness,kurtosis-normal_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| Test function for Lognormal distribution                         |
//+------------------------------------------------------------------+
bool TestLognormalDistribution(string test_name,const double mu,const double sigma,const double &x_values[],const double &pdf_values[],const double &cdf_values[],precision_info &precision)
  {
   PrintFormat("%s started",test_name);
//---
   int N=ArraySize(x_values);
   int error_code=0;
//--- arrays for calculated values
   double lognormal_pdf[];
   double lognormal_cdf[];
   double lognormal_quantile[];
//--- arrays for differences
   double delta_lognormal_pdf[];
   double delta_lognormal_cdf[];
   double delta_lognormal_quantile[];
//--- prepare arrays
   ArrayResize(lognormal_pdf,N);
   ArrayResize(lognormal_cdf,N);
   ArrayResize(lognormal_quantile,N);
   ArrayResize(delta_lognormal_pdf,N);
   ArrayResize(delta_lognormal_cdf,N);
   ArrayResize(delta_lognormal_quantile,N);
//--- test 1: single values
   PrintFormat("%s: Test 1: Calculations for single values",test_name);
   for(int i=0;i<N;i++)
     {
      //--- calculate values
      lognormal_pdf[i]=MathProbabilityDensityLognormal(x_values[i],mu,sigma,error_code);
      lognormal_cdf[i]=MathCumulativeDistributionLognormal(x_values[i],mu,sigma,error_code);
      lognormal_quantile[i]=MathQuantileLognormal(lognormal_cdf[i],mu,sigma,error_code);
      //--- calculate difference
      delta_lognormal_pdf[i]=pdf_values[i]-lognormal_pdf[i];
      delta_lognormal_cdf[i]=cdf_values[i]-lognormal_cdf[i];
      delta_lognormal_quantile[i]=x_values[i]-lognormal_quantile[i];
     }
//--- check calculated values
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_lognormal_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_lognormal_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_lognormal_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 2: calculations on arrays
   PrintFormat("%s: Test 2: Calculations on arrays",test_name);
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityLognormal(x_values,mu,sigma,lognormal_pdf))
      return false;
   if(!MathCumulativeDistributionLognormal(x_values,mu,sigma,lognormal_cdf))
      return false;
   if(!MathQuantileLognormal(lognormal_cdf,mu,sigma,lognormal_quantile))
      return false;
//--- calculate differences
   ArrayResize(delta_lognormal_pdf,N);
   ArrayResize(delta_lognormal_cdf,N);
   ArrayResize(delta_lognormal_quantile,N);
   for(int i=0;i<N;i++)
     {
      delta_lognormal_pdf[i]=pdf_values[i]-lognormal_pdf[i];
      delta_lognormal_cdf[i]=cdf_values[i]-lognormal_cdf[i];
      delta_lognormal_quantile[i]=x_values[i]-lognormal_quantile[i];
     }
//--- calculate max error
   precision.pdf_max_delta=MathAbs(delta_lognormal_pdf[0]);
   precision.cdf_max_delta=MathAbs(delta_lognormal_cdf[0]);
   precision.quantile_max_delta=MathAbs(delta_lognormal_quantile[0]);
   for(int i=0;i<N;i++)
     {
      precision.pdf_max_delta=MathMax(precision.pdf_max_delta,MathAbs(delta_lognormal_pdf[i]));
      precision.cdf_max_delta=MathMax(precision.cdf_max_delta,MathAbs(delta_lognormal_cdf[i]));
      precision.quantile_max_delta=MathMax(precision.quantile_max_delta,MathAbs(delta_lognormal_quantile[i]));
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_lognormal_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_lognormal_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_lognormal_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 3: calculations on arrays
   PrintFormat("%s: Test 3: R-like overloaded functions",test_name);
//--- parameters for R-like overloaded functions
   bool tail=true;
   bool log_mode=true;
//--- calculate pdf, cdf and quantiles
   if(!MathProbabilityDensityLognormal(x_values,mu,sigma,log_mode,lognormal_pdf))
      return false;
   if(!MathCumulativeDistributionLognormal(x_values,mu,sigma,tail,log_mode,lognormal_cdf))
      return false;
   if(!MathQuantileLognormal(lognormal_cdf,mu,sigma,tail,log_mode,lognormal_quantile))
      return false;
//--- calculate differences
   if(tail==true)
     {
      //--- lower tail mode (cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_lognormal_pdf[i]=pdf_values[i]-MathExp(lognormal_pdf[i]);
            delta_lognormal_cdf[i]=cdf_values[i]-MathExp(lognormal_cdf[i]);
            delta_lognormal_quantile[i]=x_values[i]-lognormal_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_lognormal_pdf[i]=pdf_values[i]-lognormal_pdf[i];
         delta_lognormal_cdf[i]=cdf_values[i]-lognormal_cdf[i];
         delta_lognormal_quantile[i]=x_values[i]-lognormal_quantile[i];
        }
     }
   else
     {
      //--- upper tail mode (1.0-cdf)
      if(log_mode==true)
        {
         //--- logarithmic values
         for(int i=0;i<N;i++)
           {
            delta_lognormal_pdf[i]=pdf_values[i]-MathExp(lognormal_pdf[i]);
            delta_lognormal_cdf[i]=(1.0-cdf_values[i])-MathExp(lognormal_cdf[i]);
            delta_lognormal_quantile[i]=x_values[i]-lognormal_quantile[i];
           }
        }
      else
      for(int i=0;i<N;i++)
        {
         delta_lognormal_pdf[i]=pdf_values[i]-lognormal_pdf[i];
         delta_lognormal_cdf[i]=(1.0-cdf_values[i])-lognormal_cdf[i];
         delta_lognormal_quantile[i]=x_values[i]-lognormal_quantile[i];
        }
     }
//--- check calculations
   for(int i=0;i<N;i++)
     {
      if(MathAbs(delta_lognormal_pdf[i])>calc_precision_pdf)
         return false;
      if(MathAbs(delta_lognormal_cdf[i])>calc_precision_cdf)
         return false;
      if(MathAbs(delta_lognormal_quantile[i])>calc_precision_quantile)
         return false;
     }
//--- test 4: test generators
   PrintFormat("%s: Test 4: Generators of random values",test_name);
//--- generate random values and calculate 4 moments
   int random_data_count=10000;
   double values_rnd[];
   if(MathRandomLognormal(mu,sigma,random_data_count,values_rnd)==true)
     {
      //--- calculate 4 moments of generated values
      double mean=MathMean(values_rnd);
      double variance=MathVariance(values_rnd);
      double skewness=MathSkewness(values_rnd);
      double kurtosis=MathKurtosis(values_rnd);
      //--- inital values
      double lognormal_mean=0;
      double lognormal_variance=0;
      double lognormal_skewness=0;
      double lognormal_kurtosis=0;
      //--- calculate theoretical values and compare with generated
      if(MathMomentsLognormal(mu,sigma,lognormal_mean,lognormal_variance,lognormal_skewness,lognormal_kurtosis,error_code))
        {
         PrintFormat("Mean=%.10f,  variance=%.10f  skewness=%.10f  kurtosis=%.10f",mean,mean,skewness,kurtosis);
         PrintFormat("Theoretical values: mean=%.10f, variance=%.10f  skewness=%.10f  kurtosis=%.10f",lognormal_mean,lognormal_variance,lognormal_skewness,lognormal_kurtosis);
         PrintFormat("delta mean=%.4f, delta variance=%.4f  delta skewness=%.4f  delta kurtosis=%.4f",mean-lognormal_mean,variance-lognormal_variance,skewness-lognormal_skewness,kurtosis-lognormal_kurtosis);
        }
      else
         return false;
     }
   else
      return false;
//--- successful
   PrintFormat("%s passed",test_name);
   return true;
  }
//+------------------------------------------------------------------+
//| ShowPrecisionInfo                                                |
//+------------------------------------------------------------------+
void ShowPrecisionInfo(const string distribution,precision_info &precision)
  {
   PrintFormat("%s: PDF max error          : %5.20e",distribution,precision.pdf_max_delta);
   PrintFormat("%s: CDF max error          : %5.20e",distribution,precision.cdf_max_delta);
   PrintFormat("%s: Quantile max error     : %5.20e",distribution,precision.quantile_max_delta);
   PrintFormat("%s: PDF correct digits     : %d",distribution,GetCorrectDigits(precision.pdf_max_delta));
   PrintFormat("%s: CDF correct digits     : %d",distribution,GetCorrectDigits(precision.cdf_max_delta));
   PrintFormat("%s: Quantile correct digits: %d",distribution,GetCorrectDigits(precision.quantile_max_delta));
  }
//+------------------------------------------------------------------+
//| TestDistributions                                                |
//+------------------------------------------------------------------+
void TestDistributions(int &tests_performed,int &tests_passed)
  {
   precision_info precision;

   string test_name="";
//--- Binomial distribution
   tests_performed++;
   test_name="Binomial distribution test";
   if(TestBinomialDistribution(test_name,10,0.6,binomial_x_values,binomial_pdf_values,binomial_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Beta distribution
   tests_performed++;
   test_name="Beta distribution test";
   if(TestBetaDistribution(test_name,2.0,4.0,beta_x_values,beta_pdf_values,beta_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Gamma distribution
   tests_performed++;
   test_name="Gamma distribution test";
   if(TestGammaDistribution(test_name,1.0,1.0,gamma_x_values,gamma_pdf_values,gamma_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Cauchy distribution
   tests_performed++;
   test_name="Cauchy distribution test";
   if(TestCauchyDistribution(test_name,2.0,1.0,cauchy_x_values,cauchy_pdf_values,cauchy_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Exponential distribution
   tests_performed++;
   test_name="Exponential distribution test";
   if(TestExponentialDistribution(test_name,2.0,exponential_x_values,exponential_pdf_values,exponential_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Uniform distribution
   tests_performed++;
   test_name="Uniform distribution test";
   if(TestUnifromDistribution(test_name,0,10.0,uniform_x_values,uniform_pdf_values,uniform_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Geometric distribution
   tests_performed++;
   test_name="Geometric distribution test";
   if(TestGeometricDistribution(test_name,0.3,geometric_x_values,geometric_pdf_values,geometric_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Hypergeometric distribution
   tests_performed++;
   test_name="Hypergeometric distribution test";
   if(TestHypergeometricDistribution(test_name,30,11,12,hypergeometric_x_values,hypergeometric_pdf_values,hypergeometric_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Logistic distribution
   tests_performed++;
   test_name="Logistic distribution test";
   if(TestLogisticDistribution(test_name,1.0,2.0,logistic_x_values,logistic_pdf_values,logistic_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Weibull distribution
   tests_performed++;
   test_name="Weibull distribution test";
   if(TestWeibullDistribution(test_name,5.0,1.0,weibull_x_values,weibull_pdf_values,weibull_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Poisson distribution
   tests_performed++;
   test_name="Poisson distribution test";
   if(TestPoissonDistribution(test_name,1.0,poisson_x_values,poisson_pdf_values,poisson_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- F distribution
   tests_performed++;
   test_name="F distribution test";
   if(TestFDistribution(test_name,10.0,20.0,f_x_values,f_pdf_values,f_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- ChiSquare distribution
   tests_performed++;
   test_name="ChiSquare distribution test";
   if(TestChiSquareDistribution(test_name,2.0,chisquare_x_values,chisquare_pdf_values,chisquare_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Noncentral ChiSquare distribution
   tests_performed++;
   test_name="Noncentral ChiSquare distribution test";
   if(TestNoncentralChiSquareDistribution(test_name,2.0,1.0,noncentral_chisquare_x_values,noncentral_chisquare_pdf_values,noncentral_chisquare_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Noncentral F distribution
   tests_performed++;
   test_name="Noncentral F distribution test";
   if(TestNoncentralFDistribution(test_name,10.0,20.0,2.0,noncentral_f_x_values,noncentral_f_pdf_values,noncentral_f_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Noncentral Beta Distribution
   tests_performed++;
   test_name="Noncentral Beta distribution test";
   if(TestNoncentralBetaDistribution(test_name,2.0,4.0,1.0,noncentral_beta_x_values,noncentral_beta_pdf_values,noncentral_beta_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Negative Binomial Distribution
   tests_performed++;
   test_name="Negative Binomial distribution test";
   if(TestNegativeBinomialDistribution(test_name,2.0,0.5,negative_binomial_x_values,negative_binomial_pdf_values,negative_binomial_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- T Distribution
   tests_performed++;
   test_name="T distribution test";
   if(TestTDistribution(test_name,8.0,t_x_values,t_pdf_values,t_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Noncentral T Distribution
   tests_performed++;
   test_name="Noncentral T distribution test";
   if(TestNoncentralTDistribution(test_name,10.0,8.0,noncentral_t_x_values,noncentral_t_pdf_values,noncentral_t_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Normal Distribution
   tests_performed++;
   test_name="Normal distribution test";
   if(TestNormalDistribution(test_name,21.0,5.0,normal_x_values,normal_pdf_values,normal_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

//--- Lognormal Distribution
   tests_performed++;
   test_name="Lognormal distribution test";
   if(TestLognormalDistribution(test_name,0.5,0.6,lognormal_x_values,lognormal_pdf_values,lognormal_cdf_values,precision))
     {
      tests_passed++;
      ShowPrecisionInfo(test_name,precision);
     }
   else
      PrintFormat("%s failed",test_name);

   return;
  }
//+------------------------------------------------------------------+
//| UnitTests()                                                      |
//+------------------------------------------------------------------+
void UnitTests(const string package_name)
  {
   PrintFormat("Unit tests for Package %s\n",package_name);
//--- initial values
   int tests_performed=0;
   int tests_passed=0;
//--- test distributions
   TestDistributions(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
   UnitTests("Stat");
  }
//+------------------------------------------------------------------+
