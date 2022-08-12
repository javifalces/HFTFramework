//+------------------------------------------------------------------+
//|                                                    memoryapi.mqh |
//|                        Copyright 2020, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\winnt.mqh>

//---
enum MEMORY_RESOURCE_NOTIFICATION_TYPE
  {
   LowMemoryResourceNotification,
   HighMemoryResourceNotification
  };
//---
enum OFFER_PRIORITY
  {
   VmOfferPriorityVeryLow=1,
   VmOfferPriorityLow,
   VmOfferPriorityBelowNormal,
   VmOfferPriorityNormal
  };
//---
enum WIN32_MEMORY_INFORMATION_CLASS
  {
   MemoryRegionInfo
  };
//---
struct WIN32_MEMORY_RANGE_ENTRY
  {
   PVOID             VirtualAddress;
   ulong             NumberOfBytes;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
int     AllocateUserPhysicalPages(HANDLE hProcess,ulong &NumberOfPages,ulong &PageArray[]);
int     AllocateUserPhysicalPagesNuma(HANDLE hProcess,ulong &NumberOfPages,ulong &PageArray[],uint nndPreferred);
HANDLE  CreateFileMappingFromApp(HANDLE hFile,PVOID SecurityAttributes,uint PageProtection,ulong MaximumSize,const string Name);
HANDLE  CreateFileMappingNumaW(HANDLE hFile,PVOID lpFileMappingAttributes,uint flProtect,uint dwMaximumSizeHigh,uint dwMaximumSizeLow,const string lpName,uint nndPreferred);
HANDLE  CreateFileMappingW(HANDLE hFile,PVOID lpFileMappingAttributes,uint flProtect,uint dwMaximumSizeHigh,uint dwMaximumSizeLow,const string lpName);
HANDLE  CreateMemoryResourceNotification(MEMORY_RESOURCE_NOTIFICATION_TYPE NotificationType);
uint    DiscardVirtualMemory(PVOID VirtualAddress,ulong Size);
int     FlushViewOfFile(const PVOID lpBaseAddress,ulong dwNumberOfBytesToFlush);
int     FreeUserPhysicalPages(HANDLE hProcess,ulong &NumberOfPages,ulong &PageArray[]);
ulong   GetLargePageMinimum(void);
int     GetMemoryErrorHandlingCapabilities(uint &Capabilities);
int     GetProcessWorkingSetSizeEx(HANDLE hProcess,ulong &lpMinimumWorkingSetSize,ulong &lpMaximumWorkingSetSize,uint &Flags);
int     GetSystemFileCacheSize(ulong &lpMinimumFileCacheSize,ulong &lpMaximumFileCacheSize,uint &lpFlags);
uint    GetWriteWatch(uint dwFlags,PVOID lpBaseAddress,ulong dwRegionSize,PVOID &lpAddresses[],uint &lpdwCount,uint &lpdwGranularity);
int     MapUserPhysicalPages(PVOID VirtualAddress,ulong &NumberOfPages,ulong &PageArray[]);
PVOID   MapViewOfFile(HANDLE hFileMappingObject,uint dwDesiredAccess,uint dwFileOffsetHigh,uint dwFileOffsetLow,ulong dwNumberOfBytesToMap);
PVOID   MapViewOfFileEx(HANDLE hFileMappingObject,uint dwDesiredAccess,uint dwFileOffsetHigh,uint dwFileOffsetLow,ulong dwNumberOfBytesToMap,PVOID lpBaseAddress);
PVOID   MapViewOfFileFromApp(HANDLE hFileMappingObject,uint DesiredAccess,ulong FileOffset,ulong NumberOfBytesToMap);
uint    OfferVirtualMemory(PVOID VirtualAddress,ulong Size,OFFER_PRIORITY Priority);
HANDLE  OpenFileMappingW(uint dwDesiredAccess,int bInheritHandle,const string lpName);
int     PrefetchVirtualMemory(HANDLE hProcess,uint &NumberOfEntries,WIN32_MEMORY_RANGE_ENTRY &VirtualAddresses,uint Flags);
int     QueryMemoryResourceNotification(HANDLE ResourceNotificationHandle,int &ResourceState);
int     ReadProcessMemory(HANDLE hProcess,const PVOID lpBaseAddress,PVOID lpBuffer,ulong nSize,ulong &lpNumberOfBytesRead);
uint    ReclaimVirtualMemory(const PVOID VirtualAddress,ulong Size);
PVOID   RegisterBadMemoryNotification(PVOID Callback);
uint    ResetWriteWatch(PVOID lpBaseAddress,ulong dwRegionSize);
int     SetProcessWorkingSetSizeEx(HANDLE hProcess,ulong dwMinimumWorkingSetSize,ulong dwMaximumWorkingSetSize,uint Flags);
int     SetSystemFileCacheSize(ulong MinimumFileCacheSize,ulong MaximumFileCacheSize,uint Flags);
int     UnmapViewOfFile(const PVOID lpBaseAddress);
int     UnmapViewOfFileEx(PVOID BaseAddress,uint UnmapFlags);
int     UnregisterBadMemoryNotification(PVOID RegistrationHandle);
PVOID   VirtualAlloc(PVOID lpAddress,ulong dwSize,uint flAllocationType,uint flProtect);
PVOID   VirtualAllocEx(HANDLE hProcess,PVOID lpAddress,ulong dwSize,uint flAllocationType,uint flProtect);
PVOID   VirtualAllocExNuma(HANDLE hProcess,PVOID lpAddress,ulong dwSize,uint flAllocationType,uint flProtect,uint nndPreferred);
int     VirtualFree(PVOID lpAddress,ulong dwSize,uint dwFreeType);
int     VirtualFreeEx(HANDLE hProcess,PVOID lpAddress,ulong dwSize,uint dwFreeType);
int     VirtualLock(PVOID lpAddress,ulong dwSize);
int     VirtualProtect(PVOID lpAddress,ulong dwSize,uint flNewProtect,uint &lpflOldProtect);
int     VirtualProtectEx(HANDLE hProcess,PVOID lpAddress,ulong dwSize,uint flNewProtect,uint &lpflOldProtect);
ulong   VirtualQuery(const PVOID lpAddress,MEMORY_BASIC_INFORMATION &lpBuffer,ulong dwLength);
ulong   VirtualQueryEx(HANDLE hProcess,const PVOID lpAddress,MEMORY_BASIC_INFORMATION &lpBuffer,ulong dwLength);
int     VirtualUnlock(PVOID lpAddress,ulong dwSize);
int     WriteProcessMemory(HANDLE hProcess,PVOID lpBaseAddress,PVOID lpBuffer,ulong nSize,ulong &lpNumberOfBytesWritten);
int     WriteProcessMemory(HANDLE hProcess,PVOID lpBaseAddress,uchar &lpBuffer[],ulong nSize,ulong &lpNumberOfBytesWritten);
int     WriteProcessMemory(HANDLE hProcess,uchar &lpBaseAddress[],PVOID lpBuffer,ulong nSize,ulong &lpNumberOfBytesWritten);
int     WriteProcessMemory(HANDLE hProcess,uchar &lpBaseAddress[],uchar &lpBuffer[],ulong nSize,ulong &lpNumberOfBytesWritten);
#import
//+------------------------------------------------------------------+
