//+------------------------------------------------------------------+
//|                                               PrimeGenerator.mqh |
//|                        Copyright 2016, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Class CPrimeGenrator.                                            |
//| Usage: Used to generate prime numbers.                           |
//+------------------------------------------------------------------+
class CPrimeGenerator
  {
private:
   const static int  s_primes[];       // table of prime numbers
   const static int  s_hash_prime;

public:
   static bool       IsPrime(const int candidate);
   static int        GetPrime(const int min);
   static int        ExpandPrime(const int old_size);
  };
const static int  CPrimeGenerator::s_primes[]=
  {
   3,7,11,17,23,29,37,47,59,71,89,107,131,163,197,239,293,353,431,521,631,761,919,
   1103,1327,1597,1931,2333,2801,3371,4049,4861,5839,7013,8419,10103,12143,14591,
   17519,21023,25229,30293,36353,43627,52361,62851,75431,90523,108631,130363,156437,
   187751,225307,270371,324449,389357,467237,560689,672827,807403,968897,1162687,1395263,
   1674319,2009191,2411033,2893249,3471899,4166287,4999559,5999471,7199369
  };
const static int CPrimeGenerator::s_hash_prime=101;
//+------------------------------------------------------------------+
//| Determines whether a value is prime.                             |
//+------------------------------------------------------------------+
bool CPrimeGenerator::IsPrime(const int candidate)
  {
   if((candidate  &1)!=0)
     {
      int limit=(int)MathSqrt(candidate);
      //--- check value is prime
      for(int divisor=3; divisor<=limit; divisor+=2)
         if((candidate%divisor)==0)
            return(false);
      return(true);
     }
   return(candidate == 2);
  }
//+------------------------------------------------------------------+
//| Fast generator of parime value.                                  |
//+------------------------------------------------------------------+
int CPrimeGenerator::GetPrime(const int min)
  {
//--- a typical resize algorithm would pick the smallest prime number in this array
//--- that is larger than twice the previous capacity. 
//--- get next prime value from table
   for(int i=0; i<ArraySize(s_primes); i++)
     {
      int prime=s_primes[i];
      if(prime>=min)
         return(prime);
     }
//--- outside of our predefined table
   for(int i=(min|1); i<=INT_MAX;i+=2)
     {
      if(IsPrime(i) && ((i-1)%s_hash_prime!=0))
         return(i);
     }
   return(min);
  }
//+------------------------------------------------------------------+
//| Generate a new prime value greater than old_size.                |
//+------------------------------------------------------------------+
int CPrimeGenerator::ExpandPrime(const int old_size)
  {
   int new_size=2*old_size;
   if((uint)new_size>INT_MAX && INT_MAX>old_size)
      return INT_MAX;
   else
      return GetPrime(new_size);
  }
//+------------------------------------------------------------------+
