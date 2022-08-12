//+------------------------------------------------------------------+
//|                                               errhandlingapi.mqh |
//|                        Copyright 2020, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\winnt.mqh>
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
void                         RaiseException(uint exception_code,uint exception_flags,uint number_of_arguments,const ulong &arguments[]);
int                          UnhandledExceptionFilter(EXCEPTION_POINTERS &exception_info);
PVOID                        SetUnhandledExceptionFilter(PVOID top_level_exception_filter);
uint                         GetLastError(void);
void                         SetLastError(uint err_code);
uint                         GetErrorMode(void);
uint                         SetErrorMode(uint mode);
PVOID                        AddVectoredExceptionHandler(uint first,PVOID handler);
uint                         RemoveVectoredExceptionHandler(PVOID handle);
PVOID                        AddVectoredContinueHandler(uint first,PVOID handler);
uint                         RemoveVectoredContinueHandler(PVOID handle);
void                         RestoreLastError(uint err_code);
void                         RaiseFailFastException(EXCEPTION_RECORD &exception_record,CONTEXT &context_record,uint flags);
void                         FatalAppExitW(uint action,const string message_text);
uint                         GetThreadErrorMode(void);
int                          SetThreadErrorMode(uint new_mode,uint& old_mode);
#import
