//+------------------------------------------------------------------+
//|                                                    handleapi.mqh |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
int  CloseHandle(HANDLE object);
int  DuplicateHandle(HANDLE source_process_handle,HANDLE source_handle,HANDLE target_process_handle,HANDLE &target_handle,uint desired_access,int inherit_handle,uint options);
int  GetHandleInformation(HANDLE object,uint& flags);
int  SetHandleInformation(HANDLE object,uint mask,uint flags);
#import

#import "kernelbase.dll"
int  CompareObjectHandles(HANDLE first_object_handle, HANDLE second_object_handle); 
#import 
//+------------------------------------------------------------------+