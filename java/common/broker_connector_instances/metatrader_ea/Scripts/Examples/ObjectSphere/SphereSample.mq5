//+------------------------------------------------------------------+
//|                                                 SphereSample.mq5 |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "2009-2017, MetaQuotes Software Corp."
#property link      "http://www.mql5.com"
//---
#include "Sphere.mqh"
//---
string ArrowChar="*";
int    SleepTime=50;
//---
#define NUM_SPHERES 5
#define VISIBLE     0
#define INVISIBLE   1
//---
//+------------------------------------------------------------------+
//| Script to demonstrate the use of arrays.                         |
//+------------------------------------------------------------------+
CSphere Sphere[NUM_SPHERES];
//--- arrays to initialize spheres
int   arrX[NUM_SPHERES]={100,100,300,500,500};
int   arrY[NUM_SPHERES]={100,500,300,500,350};
int   arrR[NUM_SPHERES]={30,40,100,60,20};
int   arrP[NUM_SPHERES]={10,13,30,20,7};
int   arrM[NUM_SPHERES]={10,13,30,20,7};
color arrC[NUM_SPHERES]={Red,Blue,Yellow,Green,Gray};
//+------------------------------------------------------------------+
//| Script initialization function                                   |
//+------------------------------------------------------------------+
int Init(void)
  {
   int i;
//--- creating objects
   for(i=0;i<NUM_SPHERES;i++)
      if(!Sphere[i].Create(i,arrC[i],arrX[i],arrY[i],arrR[i],arrP[i],arrM[i],ArrowChar))
         break;
   if(i!=NUM_SPHERES)
     {
      printf("Error creating sphere %d",i);
      return(-1);
     }
//--- configuring orbits
   Sphere[0].SetOrbite(GetPointer(Sphere[2]),M_PI/4,-M_PI/8,0,0.1);
   Sphere[1].SetOrbite(GetPointer(Sphere[2]),-M_PI/8,-M_PI/16,M_PI/8,0.02);
   Sphere[3].SetOrbite(GetPointer(Sphere[2]),M_PI/8,M_PI/4,M_PI/8,0.05);
   Sphere[4].SetOrbite(GetPointer(Sphere[3]),M_PI/4,M_PI/8,M_PI/8,0.1);
//---
   return(0);
  }
//+------------------------------------------------------------------+
//| Script deinitialization function                                 |
//+------------------------------------------------------------------+
void Deinit(void)
  {
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
int OnStart(void)
  {
//--- call init function
   if(Init()==0)
     {
      //--- cycle until the script is not halted
      while(!IsStopped())
        {
         //--- цикл по объектам
         for(int i=0;i<NUM_SPHERES;i++)
            Sphere[i].Recalculate();
         ChartRedraw();
         Sleep(SleepTime);
        }
     }
//--- call deinit function
   Deinit();
//---
   return(0);
  }
//+------------------------------------------------------------------+
