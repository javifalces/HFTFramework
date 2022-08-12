//+------------------------------------------------------------------+
//|                                               MovingAverages.mqh |
//|                   Copyright 2009-2020, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+

//+------------------------------------------------------------------+
//| Simple Moving Average                                            |
//+------------------------------------------------------------------+
double SimpleMA(const int position,const int period,const double &price[])
  {
   double result=0.0;
//--- check period
   if(period>0 && period<=(position+1))
     {
      for(int i=0; i<period; i++)
         result+=price[position-i];

      result/=period;
     }

   return(result);
  }
//+------------------------------------------------------------------+
//| Exponential Moving Average                                       |
//+------------------------------------------------------------------+
double ExponentialMA(const int position,const int period,const double prev_value,const double &price[])
  {
   double result=0.0;
//--- check period
   if(period>0)
     {
      double pr=2.0/(period+1.0);
      result=price[position]*pr+prev_value*(1-pr);
     }

   return(result);
  }
//+------------------------------------------------------------------+
//| Smoothed Moving Average                                          |
//+------------------------------------------------------------------+
double SmoothedMA(const int position,const int period,const double prev_value,const double &price[])
  {
   double result=0.0;
//--- check period
   if(period>0 && period<=(position+1))
     {
      if(position==period-1)
        {
         for(int i=0; i<period; i++)
            result+=price[position-i];

         result/=period;
        }

      result=(prev_value*(period-1)+price[position])/period;
     }

   return(result);
  }
//+------------------------------------------------------------------+
//| Linear Weighted Moving Average                                   |
//+------------------------------------------------------------------+
double LinearWeightedMA(const int position,const int period,const double &price[])
  {
   double result=0.0;
//--- check period
   if(period>0 && period<=(position+1))
     {
      double sum =0.0;
      int    wsum=0;

      for(int i=period; i>0; i--)
        {
         wsum+=i;
         sum +=price[position-i+1]*(period-i+1);
        }

      result=sum/wsum;
     }

   return(result);
  }
//+------------------------------------------------------------------+
//| Simple moving average on price array                             |
//+------------------------------------------------------------------+
int SimpleMAOnBuffer(const int rates_total,const int prev_calculated,const int begin,const int period,const double& price[],double& buffer[])
  {
//--- check period
   if(period<=1 || period>(rates_total-begin))
      return(0);
//--- save as_series flags
   bool as_series_price=ArrayGetAsSeries(price);
   bool as_series_buffer=ArrayGetAsSeries(buffer);

   ArraySetAsSeries(price,false);
   ArraySetAsSeries(buffer,false);
//--- calculate start position
   int start_position;

   if(prev_calculated==0)  // first calculation or number of bars was changed
     {
      //--- set empty value for first bars
      start_position=period+begin;

      for(int i=0; i<start_position-1; i++)
         buffer[i]=0.0;
      //--- calculate first visible value
      double first_value=0;

      for(int i=begin; i<start_position; i++)
         first_value+=price[i];

      buffer[start_position-1]=first_value/period;
     }
   else
      start_position=prev_calculated-1;
//--- main loop
   for(int i=start_position; i<rates_total; i++)
      buffer[i]=buffer[i-1]+(price[i]-price[i-period])/period;
//--- restore as_series flags
   ArraySetAsSeries(price,as_series_price);
   ArraySetAsSeries(buffer,as_series_buffer);
//---
   return(rates_total);
  }
//+------------------------------------------------------------------+
//|  Exponential moving average on price array                       |
//+------------------------------------------------------------------+
int ExponentialMAOnBuffer(const int rates_total,const int prev_calculated,const int begin,const int period,const double& price[],double& buffer[])
  {
//--- check period
   if(period<=1 || period>(rates_total-begin))
      return(0);
//--- save and clear 'as_series' flags
   bool as_series_price=ArrayGetAsSeries(price);
   bool as_series_buffer=ArrayGetAsSeries(buffer);

   ArraySetAsSeries(price,false);
   ArraySetAsSeries(buffer,false);
//--- calculate start position
   int    start_position;
   double smooth_factor=2.0/(1.0+period);

   if(prev_calculated==0)  // first calculation or number of bars was changed
     {
      //--- set empty value for first bars
      for(int i=0; i<begin; i++)
         buffer[i]=0.0;
      //--- calculate first visible value
      start_position=period+begin;
      buffer[begin] =price[begin];

      for(int i=begin+1; i<start_position; i++)
         buffer[i]=price[i]*smooth_factor+buffer[i-1]*(1.0-smooth_factor);
     }
   else
      start_position=prev_calculated-1;
//--- main loop
   for(int i=start_position; i<rates_total; i++)
      buffer[i]=price[i]*smooth_factor+buffer[i-1]*(1.0-smooth_factor);
//--- restore as_series flags
   ArraySetAsSeries(price,as_series_price);
   ArraySetAsSeries(buffer,as_series_buffer);
//---
   return(rates_total);
  }
//+------------------------------------------------------------------+
//|  Linear weighted moving average on price array classic           |
//+------------------------------------------------------------------+
int LinearWeightedMAOnBuffer(const int rates_total,const int prev_calculated,const int begin,const int period,const double& price[],double& buffer[])
  {
//--- check period
   if(period<=1 || period>(rates_total-begin))
      return(0);
//--- save as_series flags
   bool as_series_price=ArrayGetAsSeries(price);
   bool as_series_buffer=ArrayGetAsSeries(buffer);

   ArraySetAsSeries(price,false);
   ArraySetAsSeries(buffer,false);
//--- calculate start position
   int i,start_position;

   if(prev_calculated<=period+begin+2)  // first calculation or number of bars was changed
     {
      //--- set empty value for first bars
      start_position=period+begin;

      for(i=0; i<start_position; i++)
         buffer[i]=0.0;
     }
   else
      start_position=prev_calculated-2;
//--- calculate first visible value
   double sum=0.0,lsum=0.0;
   int    l,weight=0;

   for(i=start_position-period,l=1; i<start_position; i++,l++)
     {
      sum   +=price[i]*l;
      lsum  +=price[i];
      weight+=l;
     }
   buffer[start_position-1]=sum/weight;
//--- main loop
   for(i=start_position; i<rates_total; i++)
     {
      sum      =sum-lsum+price[i]*period;
      lsum     =lsum-price[i-period]+price[i];
      buffer[i]=sum/weight;
     }
//--- restore as_series flags
   ArraySetAsSeries(price,as_series_price);
   ArraySetAsSeries(buffer,as_series_buffer);
//---
   return(rates_total);
  }
//+------------------------------------------------------------------+
//|  Linear weighted moving average on price array fast              |
//+------------------------------------------------------------------+
int LinearWeightedMAOnBuffer(const int rates_total,const int prev_calculated,const int begin,const int period,const double& price[],double& buffer[],int &weight_sum)
  {
//--- check period
   if(period<=1 || period>(rates_total-begin))
      return(0);
//--- save as_series flags
   bool as_series_price=ArrayGetAsSeries(price);
   bool as_series_buffer=ArrayGetAsSeries(buffer);

   ArraySetAsSeries(price,false);
   ArraySetAsSeries(buffer,false);
//--- calculate start position
   int start_position;

   if(prev_calculated==0)  // first calculation or number of bars was changed
     {
      //--- set empty value for first bars
      start_position=period+begin;

      for(int i=0; i<start_position; i++)
         buffer[i]=0.0;
      //--- calculate first visible value
      double first_value=0;
      int    wsum       =0;

      for(int i=begin,k=1; i<start_position; i++,k++)
        {
         first_value+=k*price[i];
         wsum       +=k;
        }

      buffer[start_position-1]=first_value/wsum;
      weight_sum=wsum;
     }
   else
      start_position=prev_calculated-1;
//--- main loop
   for(int i=start_position; i<rates_total; i++)
     {
      double sum=0;

      for(int j=0; j<period; j++)
         sum+=(period-j)*price[i-j];

      buffer[i]=sum/weight_sum;
     }
//--- restore as_series flags
   ArraySetAsSeries(price,as_series_price);
   ArraySetAsSeries(buffer,as_series_buffer);
//---
   return(rates_total);
  }
//+------------------------------------------------------------------+
//|  Smoothed moving average on price array                          |
//+------------------------------------------------------------------+
int SmoothedMAOnBuffer(const int rates_total,const int prev_calculated,const int begin,const int period,const double& price[],double& buffer[])
  {
//--- check period
   if(period<=1 || period>(rates_total-begin))
      return(0);
//--- save as_series flags
   bool as_series_price=ArrayGetAsSeries(price);
   bool as_series_buffer=ArrayGetAsSeries(buffer);

   ArraySetAsSeries(price,false);
   ArraySetAsSeries(buffer,false);
//--- calculate start position
   int start_position;

   if(prev_calculated==0)  // first calculation or number of bars was changed
     {
      //--- set empty value for first bars
      start_position=period+begin;

      for(int i=0; i<start_position-1; i++)
         buffer[i]=0.0;
      //--- calculate first visible value
      double first_value=0;

      for(int i=begin; i<start_position; i++)
         first_value+=price[i];

      buffer[start_position-1]=first_value/period;
     }
   else
      start_position=prev_calculated-1;
//--- main loop
   for(int i=start_position; i<rates_total; i++)
      buffer[i]=(buffer[i-1]*(period-1)+price[i])/period;
//--- restore as_series flags
   ArraySetAsSeries(price,as_series_price);
   ArraySetAsSeries(buffer,as_series_buffer);
//---
   return(rates_total);
  }
//+------------------------------------------------------------------+
