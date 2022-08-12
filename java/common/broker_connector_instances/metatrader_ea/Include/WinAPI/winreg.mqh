//+------------------------------------------------------------------+
//|                                                       winreg.mqh |
//|                        Copyright 2020, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\winnt.mqh>

//---
struct VALENTW
  {
   PVOID             ve_valuename;
   uint              ve_valuelen;
   uchar             offset1[4];
   PVOID             ve_valueptr;
   uint              ve_type;
   uchar             offset2[4];
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "advapi32.dll"
int     AbortSystemShutdownW(string machine_name);
uint    CheckForHiberboot(uchar &hiberboot,uchar clear_flag);
uint    InitiateShutdownW(string machine_name,string message,uint grace_period,uint shutdown_flags,uint reason);
int     InitiateSystemShutdownExW(string machine_name,string message,uint timeout,int force_apps_closed,int reboot_after_shutdown,uint reason);
int     InitiateSystemShutdownW(string machine_name,string message,uint timeout,int force_apps_closed,int reboot_after_shutdown);
int     RegCloseKey(HANDLE key);
int     RegConnectRegistryExW(const string machine_name,HANDLE key,uint Flags,HANDLE &result);
int     RegConnectRegistryW(const string machine_name,HANDLE key,HANDLE &result);
int     RegCopyTreeW(HANDLE key_src,const string sub_key,HANDLE key_dest);
int     RegCreateKeyExW(HANDLE key,const string sub_key,PVOID reserved,string class_name,uint options,uint desired,PVOID security_attributes,HANDLE &result,uint &disposition);
int     RegCreateKeyTransactedW(HANDLE key,const string sub_key,PVOID reserved,string class_name,uint options,uint desired,PVOID security_attributes,HANDLE &result,uint &disposition,HANDLE transaction,PVOID extended_parameter);
int     RegCreateKeyW(HANDLE key,const string sub_key,HANDLE &result);
int     RegDeleteKeyExW(HANDLE key,const string sub_key,uint desired,PVOID reserved);
int     RegDeleteKeyTransactedW(HANDLE key,const string sub_key,uint desired,PVOID reserved,HANDLE transaction,PVOID extended_parameter);
int     RegDeleteKeyValueW(HANDLE key,const string sub_key,const string value_name);
int     RegDeleteKeyW(HANDLE key,const string sub_key);
int     RegDeleteTreeW(HANDLE key,const string sub_key);
int     RegDeleteValueW(HANDLE key,const string value_name);
int     RegDisablePredefinedCache(void);
int     RegDisablePredefinedCacheEx(void);
int     RegDisableReflectionKey(HANDLE base);
int     RegEnableReflectionKey(HANDLE base);
int     RegEnumKeyExW(HANDLE key,uint index,ushort &name[],uint &name_size,PVOID reserved,ushort &class_name[],uint &class_size,FILETIME &last_write_time);
int     RegEnumKeyW(HANDLE key,uint index,ushort &name[],uint &name_size);
int     RegEnumValueW(HANDLE key,uint index,ushort &value_name[],uint &value_name_size,PVOID reserved,uint &type,uchar &data[],uint &data_size);
int     RegFlushKey(HANDLE key);
int     RegGetKeySecurity(HANDLE key,uint SecurityInformation,SECURITY_DESCRIPTOR &security_descriptor,uint &security_descriptor_size);
int     RegGetValueW(HANDLE key,const string sub_key,const string value,uint flags,uint &type,uchar &data[],uint &data_size);
int     RegLoadAppKeyW(const string file,HANDLE &result,uint desired,uint options,PVOID reserved);
int     RegLoadKeyW(HANDLE key,const string sub_key,const string file);
int     RegLoadMUIStringW(HANDLE key,const string value,ushort &out_buf[],uint &out_buf_size,uint &data,uint flags,const string directory);
int     RegNotifyChangeKeyValue(HANDLE key,int watch_subtree,uint notify_filter,HANDLE event,int asynchronous);
int     RegOpenCurrentUser(uint desired,HANDLE &result);
int     RegOpenKeyExW(HANDLE key,const string sub_key,uint options,uint desired,HANDLE &result);
int     RegOpenKeyTransactedW(HANDLE key,const string sub_key,uint options,uint desired,HANDLE &result,HANDLE transaction,PVOID extended_paremeter);
int     RegOpenKeyW(HANDLE key,const string sub_key,HANDLE &result);
int     RegOpenUserClassesRoot(HANDLE token,uint options,uint desired,HANDLE &result);
int     RegOverridePredefKey(HANDLE key,HANDLE new_key);
int     RegQueryInfoKeyW(HANDLE key,string class_name,uint &class_size,PVOID reserved,uint &sub_keys,uint &max_sub_key_len,uint &max_class_len,uint &values,uint &max_value_name_len,uint &max_value_len,uint &security_descriptor,FILETIME &last_write_time);
int     RegQueryMultipleValuesW(HANDLE key,VALENTW &val_list[],uint num_vals,ushort &value_buf[],uint &totsize);
int     RegQueryReflectionKey(HANDLE base,int &is_reflection_disabled);
int     RegQueryValueExW(HANDLE key,const string value_name,PVOID reserved,uint &type,uchar &data[],uint &data_size);
int     RegQueryValueW(HANDLE key,const string sub_key,uchar &data[],uint &data_size);
int     RegRenameKey(HANDLE key,const string sub_key_name,const string new_key_name);
int     RegReplaceKeyW(HANDLE key,const string sub_key,const string new_file,const string old_file);
int     RegRestoreKeyW(HANDLE key,const string file,uint flags);
int     RegSaveKeyExW(HANDLE key,const string file,PVOID security_attributes,uint flags);
int     RegSaveKeyW(HANDLE key,const string file,PVOID security_attributes);
int     RegSetKeySecurity(HANDLE key,uint SecurityInformation,SECURITY_DESCRIPTOR &security_descriptor);
int     RegSetKeyValueW(HANDLE key,const string sub_key,const string value_name,uint type,const uchar &data[],uint data);
int     RegSetValueExW(HANDLE key,const string value_name,PVOID reserved,uint type,const uchar &data[],uint data_size);
int     RegSetValueW(HANDLE key,const string sub_key,uint type,const ushort &data[],uint data_size);
int     RegUnLoadKeyW(HANDLE key,const string sub_key);
#import
//+------------------------------------------------------------------+