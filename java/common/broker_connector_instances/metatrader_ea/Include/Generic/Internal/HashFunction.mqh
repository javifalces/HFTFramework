//+------------------------------------------------------------------+
//|                                                 HashFunction.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Unioun BitInterpreter.                                           |
//| Usage: Provides the ability to interpret the same bit sequence in|
//|        different types.                                          |
//+------------------------------------------------------------------+
union  BitInterpreter
  {
   bool              bool_value;
   char              char_value;
   uchar             uchar_value;
   short             short_value;
   ushort            ushort_value;
   color             color_value;
   int               int_value;
   uint              uint_value;
   datetime          datetime_value;
   long              long_value;
   ulong             ulong_value;
   float             float_value;
   double            double_value;
  };
//+------------------------------------------------------------------+
//| Returns a hashcode for boolean.                                  |
//+------------------------------------------------------------------+
int GetHashCode(const bool value)
  {
   return((value)?true:false);
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for character.                                |
//+------------------------------------------------------------------+
int GetHashCode(const char value)
  {
   return((int)value | ((int)value << 16));
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for unsigned character.                       |
//+------------------------------------------------------------------+
int GetHashCode(const uchar value)
  {
   return((int)value | ((int)value << 16));
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for short.                                    |
//+------------------------------------------------------------------+
int GetHashCode(const short value)
  {
   return(((int)((ushort)value) | (((int)value) << 16)));
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for unsigned short.                           |
//+------------------------------------------------------------------+
int GetHashCode(const ushort value)
  {
   return((int)value);
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for color.                                    |
//+------------------------------------------------------------------+
int GetHashCode(const color value)
  {
   return((int)value);
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for integer.                                  |
//+------------------------------------------------------------------+
int GetHashCode(const int value)
  {
   return(value);
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for unsigned integer.                         |
//+------------------------------------------------------------------+ 
int GetHashCode(const uint value)
  {
   return((int)value);
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for datetime.                                 |
//+------------------------------------------------------------------+
int GetHashCode(const datetime value)
  {
   long ticks=(long)value;
   return(((int)ticks) ^ (int)(ticks >> 32));
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for long.                                     |
//+------------------------------------------------------------------+
int GetHashCode(const long value)
  {
   return(((int)((long)value)) ^ (int)(value >> 32));
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for unsigned long.                            |
//+------------------------------------------------------------------+  
int GetHashCode(const ulong value)
  {
   return(((int)value) ^ (int)(value >> 32));
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for float.                                    |
//+------------------------------------------------------------------+
int GetHashCode(const float value)
  {
   if(value==0)
     {
      //--- ensure that 0 and -0 have the same hash code
      return(0);
     }
   BitInterpreter convert;
   convert.float_value=value;
   return(convert.int_value);
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for string.                                   |
//+------------------------------------------------------------------+
int GetHashCode(const double value)
  {
   if(value==0)
     {
      //--- ensure that 0 and -0 have the same hash code
      return(0);
     }
   BitInterpreter convert;
   convert.double_value=value;
   long lvalue=convert.long_value;
   return(((int)lvalue) ^ ((int)(lvalue >> 32)));
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for string.                                   |
//| The hashcode for a string is computed as:                        |
//|                                                                  |
//| s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]                     |
//|                                                                  |
//| using int arithmetic, where s[i] is the ith character of the     |
//| string, n is the length of the string, and ^ indicates           |
//| exponentiation. (The hash value of the empty string is zero.)    |
//+------------------------------------------------------------------+
int GetHashCode(const string value)
  {
   int len=StringLen(value);
   int hash=0;
//--- check length of string
   if(len>0)
     {
      //--- calculate a hash as a fucntion of each char
      for(int i=0; i<len; i++)
         hash=31*hash+value[i];
     }
   return(hash);
  }
//+------------------------------------------------------------------+
//| Returns a hashcode for custom object.                            |
//+------------------------------------------------------------------+
template<typename T>
int GetHashCode(T value)
  {
//--- try to convert to equality comparable object  
   IEqualityComparable<T>*equtable=dynamic_cast<IEqualityComparable<T>*>(value);
   if(equtable)
     {
      //--- calculate hash by specied method   
      return equtable.HashCode();
     }
   else
     {
      //--- calculate hash from name of object
      return GetHashCode(typename(value));
     }
  }
//+------------------------------------------------------------------+
