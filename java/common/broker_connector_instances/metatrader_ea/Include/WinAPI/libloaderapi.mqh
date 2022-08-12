//+------------------------------------------------------------------+
//|                                                 libloaderapi.mqh |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>

//---
struct ENUMUILANG
  {
   uint              NumOfEnumUILang;
   uint              SizeOfEnumUIBuffer;
   PVOID             EnumUIBuffer;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
int                  DisableThreadLibraryCalls(HANDLE lib_module);
HANDLE               FindResourceExW(HANDLE module,const string type,const string name,ushort language);
int                  FindStringOrdinal(uint find_string_ordinal_flags,const string string_source,int source,const string string_value,int value,int ignore_case);
int                  FreeLibrary(HANDLE lib_module);
void                 FreeLibraryAndExitThread(HANDLE lib_module,uint exit_code);
int                  FreeResource(HANDLE res_data);
uint                 GetModuleFileNameW(HANDLE module,ushort &filename[],uint size);
HANDLE               GetModuleHandleW(const string module_name);
int                  GetModuleHandleExW(uint flags,const string module_name,HANDLE &module);
PVOID                GetProcAddress(HANDLE module,uchar &proc_name[]);
HANDLE               LoadLibraryExW(const string lib_file_name,HANDLE file,uint flags);
HANDLE               LoadResource(HANDLE module,HANDLE res_info);
PVOID                LockResource(HANDLE res_data);
uint                 SizeofResource(HANDLE module,HANDLE res_info);
PVOID                AddDllDirectory(const string new_directory);
int                  RemoveDllDirectory(PVOID cookie);
int                  SetDefaultDllDirectories(uint directory_flags);
int                  EnumResourceLanguagesExW(HANDLE module,const string type,const string name,PVOID enum_func,long param,uint flags,ushort lang_id);
int                  EnumResourceNamesExW(HANDLE module,const string type,PVOID enum_func,long param,uint flags,ushort lang_id);
int                  EnumResourceTypesExW(HANDLE module,PVOID enum_func,long param,uint flags,ushort lang_id);
HANDLE               FindResourceW(HANDLE module,const string name,const string type);
HANDLE               LoadLibraryW(const string lib_file_name);
int                  EnumResourceNamesW(HANDLE module,const string type,PVOID enum_func,long param);
#import

#import "user32.dll"
int                  LoadStringW(HANDLE instance,uint id,string buffer,int buffer_max);
#import
//+------------------------------------------------------------------+
