//+------------------------------------------------------------------+
//|                                            ArrayDoubleSample.mq5 |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "2009-2017, MetaQuotes Software Corp."
#property link      "http://www.mql5.com"
//---
#include <Arrays\ArrayDouble.mqh>
#include <Files\FileBin.mqh>
//---
const int ExtArraySize=10000;
const int ExtArrayAdd=100;
string    ExtFileName="ArrayDoubleSample.bin";
//+------------------------------------------------------------------+
//| Example class CArrayDouble                                       |
//+------------------------------------------------------------------+
int OnStart(void)
  {
   int           i,pos;
   double        key;
   CFileBin      File;
   CArrayDouble  ArrayDouble;
//---  
   printf("Start sample %s.",__FILE__);
//--- fill an array of background information
//--- open file for reading
   if(File.Open(ExtFileName,FILE_READ)!=INVALID_HANDLE)
     {
      //--- read array from file
      if(!ArrayDouble.Load(File.Handle()))
        {
         //--- error reading from file
         printf("%s (%4d): error %d",__FILE__,__LINE__,GetLastError());
        }
      //--- do not forget close file
      File.Close();
     }
//--- check whether enough information in the array
   if(ArrayDouble.Total()<ExtArraySize)
     {
      //--- information in the file is not enough, or it is not at all
      //--- reserve position in the array for the missing information
      if(!ArrayDouble.Reserve(ExtArraySize-ArrayDouble.Total()))
        {
         //--- displaying the log error information
         printf("%s (%4d): reserve error",__FILE__,__LINE__);
         //--- remove a previously created array
         return(__LINE__);
        }
      //--- additional fill an array of "random" values 
      for(i=ArrayDouble.Total();i<ExtArraySize;i++)
         ArrayDouble.Add(MathRand()*MathPow(10,MathRand()%100));
     }
//--- sort array
   ArrayDouble.Sort();
//--- inserts the additional data without violating sorting (ExtArrayAdd items)
   for(i=0;i<ExtArrayAdd;i++)
      ArrayDouble.InsertSort(MathRand()*MathPow(10,MathRand()%100));
//--- set tolerance "fuzzy" comparison for the search
   ArrayDouble.Delta(0.1);
//--- produce some of the search in sorted array
   key=MathRand()*MathPow(10,MathRand()%100);
   if((pos=ArrayDouble.SearchGreat(key))==-1)
      printf("Search for items greater than %f, not found",key);
   else
     {
      printf("Search for items greater than %f, found %f in the position %d",key,ArrayDouble.At(pos),pos);
      //--- your actions have found the element
      //--- ...
      //---
     }
   key=MathRand()*MathPow(10,MathRand()%100);
   if((pos=ArrayDouble.SearchLess(key))==-1)
      printf("Search for items less than %f, not found",key);
   else
     {
      printf("Search for items less than %f, found %f in the position %d",key,ArrayDouble.At(pos),pos);
      //--- your actions have found the element
      //--- ...
      //---
     }
//--- Remove from the array of extra data (ExtArrayAdd/2 largest and ExtArrayAdd/2 lowest)
   ArrayDouble.DeleteRange(ArrayDouble.Total()-ExtArrayAdd/2-1,ArrayDouble.Total());
   ArrayDouble.DeleteRange(0,ExtArraySize/2);
//--- save the modified array of file
//--- open file for writing
   if(File.Open(ExtFileName,FILE_WRITE)!=INVALID_HANDLE)
      if(ArrayDouble.Save(File.Handle()))
        {
         //--- normal completion
         //--- because when you call the destructor, an open file is closed automatically 
         //--- and explicitly close the file is not necessary but desirable
         printf("End of sample %s. OK!",__FILE__);
         return(0);
        }
//--- error with file
//--- displaying the log error information
   printf("%s (%4d): error %d",__FILE__,__LINE__,GetLastError());
   return(__LINE__);
  }
//+------------------------------------------------------------------+
