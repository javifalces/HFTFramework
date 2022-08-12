//+------------------------------------------------------------------+
//|                                                ArrayFunction.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include "CompareFunction.mqh"
#include <Generic\Interfaces\IComparer.mqh>
//+------------------------------------------------------------------+
//| Searches an entire one-dimensional sorted array for a specific   |
//| element, using the IComparable<T> generic interface implemented  |
//| by each element of the array and by the specified object.        |
//+------------------------------------------------------------------+
template<typename T>
int ArrayBinarySearch(T &array[],const int start_index,const int count, T value,IComparer<T>*comparer)
  {
   int lo=start_index;
   int hi=start_index+count-1;
   int size=ArraySize(array);
//--- check array size
   if(size==0)
      return(-1);
//--- check comaparer
   if(CheckPointer(comparer)==POINTER_INVALID)
      return(-1);
//--- check index
   if(start_index<0 || count<0 || size-start_index<count)
      return(-1);
//--- bianry search value   
   while(lo<=hi)
     {
      int i=lo+((hi-lo)>>1);
      int order=comparer.Compare(array[i],value);
      if(order==0)
        {
         return(i);
        }
      if(order<0)
        {
         lo=i+1;
        }
      else
        {
         hi=i-1;
        }
     }
//--- returns the index of an element nearest in value
   if(lo>0)
      return(lo-1);
   return(lo);
  }
//+------------------------------------------------------------------+
//| Searches for the specified object and returns the index of its   |
//| first occurrence in a one-dimensional array.                     |
//+------------------------------------------------------------------+
template<typename T>
int ArrayIndexOf(T &array[],T value,const int start_index,const int count)
  {
   int size=ArraySize(array);
//--- check array size
   if(size==0)
      return(-1);
//--- check start index and count
   if(start_index<0 || start_index>size || 
      count<0 || count>size-start_index)
      return(-1);
//--- search value
   int end_index=start_index+count;
   for(int i=start_index; i<end_index; i++)
     {
      //--- check the value in array is eqaul to specified value     
      if(::Equals(array[i],value))
        {
         //--- return fist index 
         return(i);
        }
     }
//--- return -1 if value not in array                                     
   return(-1);
  }
//+------------------------------------------------------------------+
//| Returns the index of the last occurrence of a value in a         |
//| one-dimensional array.                                           |
//+------------------------------------------------------------------+
template<typename T>
int ArrayLastIndexOf(T &array[],T value,const int start_index,const int count)
  {
   int size=ArraySize(array);
//--- check array size
   if(size==0)
      return(-1);
//--- check start index and count
   if(start_index<0 || start_index>=size || 
      count<0 || count>start_index+1)
      return(-1);
//--- search value
   int end_index=start_index-count+1;
   for(int i=start_index; i>=end_index; i--)
     {
      //--- check the value in array is eqaul to specified value     
      if(::Equals(array[i],value))
        {
         //--- return fist index from the end         
         return (i);
        }
     }
//--- return -1 if value not in array
   return(-1);
  }
//+------------------------------------------------------------------+
//| Reverses the elements in a range of this array. Following a call |
//| to this method, an element in the range given by index and count |
//| which was previously located at index i will now be located at   |
//| index index + (index + count - i - 1).                           |
//+------------------------------------------------------------------+
template<typename T>
bool ArrayReverse(T &array[],const int start_index,const int count)
  {
   int size=ArraySize(array);
//--- check start index and count
   if(count<0 || size-start_index<count)
      return(false);
//--- reverse elements
   int i = start_index;
   int j = start_index + count - 1;
   while(i<j)
     {
      T temp=array[i];
      array[i] = array[j];
      array[j] = temp;
      i++;
      j--;
     }
   return(true);
  }
//+------------------------------------------------------------------+
