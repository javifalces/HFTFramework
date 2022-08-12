//+------------------------------------------------------------------+
//|                                              securitybaseapi.mqh |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\winnt.mqh>
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "advapi32.dll"
int                       AccessCheck(SECURITY_DESCRIPTOR &security_descriptor,HANDLE client_token,uint desired_access,GENERIC_MAPPING &generic_mapping,PRIVILEGE_SET &privilege_set,uint &privilege_set_length,uint &granted_access,int &access_status);
int                       AccessCheckAndAuditAlarmW(const string subsystem_name,PVOID handle_id,string object_type_name,string object_name,SECURITY_DESCRIPTOR &security_descriptor,uint desired_access,GENERIC_MAPPING &generic_mapping,int object_creation,uint &granted_access,int &access_status,int &generate_on_close);
int                       AccessCheckByType(SECURITY_DESCRIPTOR &security_descriptor,SID &principal_self_sid,HANDLE client_token,uint desired_access,OBJECT_TYPE_LIST &object_type_list,uint object_type_list_length,GENERIC_MAPPING &generic_mapping,PRIVILEGE_SET &privilege_set,uint &privilege_set_length,uint &granted_access,int &access_status);
int                       AccessCheckByTypeResultList(SECURITY_DESCRIPTOR &security_descriptor,SID &principal_self_sid,HANDLE client_token,uint desired_access,OBJECT_TYPE_LIST &object_type_list,uint object_type_list_length,GENERIC_MAPPING &generic_mapping,PRIVILEGE_SET &privilege_set,uint &privilege_set_length,uint &granted_access_list,uint &access_status_list);
int                       AccessCheckByTypeAndAuditAlarmW(const string subsystem_name,PVOID handle_id,const string object_type_name,const string object_name,SECURITY_DESCRIPTOR &security_descriptor,SID &principal_self_sid,uint desired_access,AUDIT_EVENT_TYPE audit_type,uint flags,OBJECT_TYPE_LIST &object_type_list,uint object_type_list_length,GENERIC_MAPPING &generic_mapping,int object_creation,uint &granted_access,int &access_status,int &generate_on_close);
int                       AccessCheckByTypeResultListAndAuditAlarmW(const string subsystem_name,PVOID handle_id,const string object_type_name,const string object_name,SECURITY_DESCRIPTOR &security_descriptor,SID &principal_self_sid,uint desired_access,AUDIT_EVENT_TYPE audit_type,uint flags,OBJECT_TYPE_LIST &object_type_list,uint object_type_list_length,GENERIC_MAPPING &generic_mapping,int object_creation,uint &granted_access_list,uint &access_status_list,int &generate_on_close);
int                       AccessCheckByTypeResultListAndAuditAlarmByHandleW(const string subsystem_name,PVOID handle_id,HANDLE client_token,const string object_type_name,const string object_name,SECURITY_DESCRIPTOR &security_descriptor,SID &principal_self_sid,uint desired_access,AUDIT_EVENT_TYPE audit_type,uint flags,OBJECT_TYPE_LIST &object_type_list,uint object_type_list_length,GENERIC_MAPPING &generic_mapping,int object_creation,uint &granted_access_list,uint &access_status_list,int &generate_on_close);
int                       AddAccessAllowedAce(ACL &acl,uint ace_revision,uint access_mask,SID &sid);
int                       AddAccessAllowedAceEx(ACL &acl,uint ace_revision,uint ace_flags,uint access_mask,SID &sid);
int                       AddAccessAllowedObjectAce(ACL &acl,uint ace_revision,uint ace_flags,uint access_mask,GUID &object_type_guid,GUID &inherited_object_type_guid,SID &sid);
int                       AddAccessDeniedAce(ACL &acl,uint ace_revision,uint access_mask,SID &sid);
int                       AddAccessDeniedAceEx(ACL &acl,uint ace_revision,uint ace_flags,uint access_mask,SID &sid);
int                       AddAccessDeniedObjectAce(ACL &acl,uint ace_revision,uint ace_flags,uint access_mask,GUID &object_type_guid,GUID &inherited_object_type_guid,SID &sid);
int                       AddAce(ACL &acl,uint ace_revision,uint starting_ace_index,PVOID ace_list,uint ace_list_length);
int                       AddAuditAccessAce(ACL &acl,uint ace_revision,uint access_mask,SID &sid,int audit_success,int audit_failure);
int                       AddAuditAccessAceEx(ACL &acl,uint ace_revision,uint ace_flags,uint access_mask,SID &sid,int audit_success,int audit_failure);
int                       AddAuditAccessObjectAce(ACL &acl,uint ace_revision,uint ace_flags,uint access_mask,GUID &object_type_guid,GUID &inherited_object_type_guid,SID &sid,int audit_success,int audit_failure);
int                       AddMandatoryAce(ACL &acl,uint ace_revision,uint ace_flags,uint mandatory_policy,SID &label_sid);
int                       AdjustTokenGroups(HANDLE token_handle,int reset_to_default,TOKEN_GROUPS &new_state,uint buffer_length,TOKEN_GROUPS &previous_state,uint &return_length);
int                       AdjustTokenPrivileges(HANDLE token_handle,int disable_all_privileges,TOKEN_PRIVILEGES &new_state,uint buffer_length,TOKEN_PRIVILEGES &previous_state,uint &return_length);
int                       AllocateAndInitializeSid(SID_IDENTIFIER_AUTHORITY &identifier_authority,uchar sub_authority_count,uint sub_authority0,uint sub_authority1,uint sub_authority2,uint sub_authority3,uint sub_authority4,uint sub_authority5,uint sub_authority6,uint sub_authority7,SID &sid);
int                       AllocateLocallyUniqueId(LUID &luid);
int                       AreAllAccessesGranted(uint granted_access,uint desired_access);
int                       AreAnyAccessesGranted(uint granted_access,uint desired_access);
int                       CheckTokenMembership(HANDLE token_handle,SID &sid_to_check,int &is_member);
int                       ConvertToAutoInheritPrivateObjectSecurity(SECURITY_DESCRIPTOR &parent_descriptor,SECURITY_DESCRIPTOR &current_security_descriptor,SECURITY_DESCRIPTOR  &new_security_descriptor,GUID &object_type,uchar is_directory_object,GENERIC_MAPPING &generic_mapping);
int                       CopySid(uint destination_sid_length,SID &destination_sid,SID &source_sid);
int                       CreatePrivateObjectSecurity(SECURITY_DESCRIPTOR &parent_descriptor,SECURITY_DESCRIPTOR &creator_descriptor,SECURITY_DESCRIPTOR  &new_descriptor,int is_directory_object,HANDLE token,GENERIC_MAPPING &generic_mapping);
int                       CreatePrivateObjectSecurityEx(SECURITY_DESCRIPTOR &parent_descriptor,SECURITY_DESCRIPTOR &creator_descriptor,SECURITY_DESCRIPTOR  &new_descriptor,GUID &object_type,int is_container_object,uint auto_inherit_flags,HANDLE token,GENERIC_MAPPING &generic_mapping);
int                       CreatePrivateObjectSecurityWithMultipleInheritance(SECURITY_DESCRIPTOR &parent_descriptor,SECURITY_DESCRIPTOR &creator_descriptor,SECURITY_DESCRIPTOR  &new_descriptor,GUID &object_types,uint guid_count,int is_container_object,uint auto_inherit_flags,HANDLE token,GENERIC_MAPPING &generic_mapping);
int                       CreateRestrictedToken(HANDLE existing_token_handle,uint flags,uint disable_sid_count,SID_AND_ATTRIBUTES &sids_to_disable,uint delete_privilege_count,LUID_AND_ATTRIBUTES &privileges_to_delete,uint restricted_sid_count,SID_AND_ATTRIBUTES &sids_to_restrict,HANDLE &new_token_handle);
int                       CreateWellKnownSid(WELL_KNOWN_SID_TYPE well_known_sid_type,SID &domain_sid,SID &sid,uint &sid);
int                       EqualDomainSid(SID &sid1,SID &sid2,int &equal);
int                       DeleteAce(ACL &acl,uint ace_index);
int                       DestroyPrivateObjectSecurity(SECURITY_DESCRIPTOR  &object_descriptor);
int                       DuplicateToken(HANDLE existing_token_handle,SECURITY_IMPERSONATION_LEVEL impersonation_level,HANDLE &duplicate_token_handle);
int                       DuplicateTokenEx(HANDLE existing_token,uint desired_access,PVOID token_attributes,SECURITY_IMPERSONATION_LEVEL impersonation_level,TOKEN_TYPE token_type,HANDLE &new_token);
int                       EqualPrefixSid(SID &sid1,SID &sid2);
int                       EqualSid(SID &sid1,SID &sid2);
int                       FindFirstFreeAce(ACL &acl,PVOID &ace);
PVOID                     FreeSid(SID &sid);
int                       GetAce(ACL &acl,uint ace_index,PVOID &ace);
int                       GetAclInformation(ACL &acl,PVOID acl_information,uint acl_information_length,ACL_INFORMATION_CLASS acl_information_class);
int                       GetFileSecurityW(const string file_name,uint requested_information,SECURITY_DESCRIPTOR  &security_descriptor,uint length,uint &length_needed);
int                       GetKernelObjectSecurity(HANDLE handle,uint requested_information,SECURITY_DESCRIPTOR &security_descriptor,uint length,uint &length_needed);
uint                      GetLengthSid(SID &sid);
int                       GetPrivateObjectSecurity(SECURITY_DESCRIPTOR &object_descriptor,uint security_information,SECURITY_DESCRIPTOR  &resultant_descriptor,uint descriptor_length,uint &return_length);
int                       GetSecurityDescriptorControl(SECURITY_DESCRIPTOR &security_descriptor,ushort &control,uint &revision);
int                       GetSecurityDescriptorDacl(SECURITY_DESCRIPTOR &security_descriptor,int &dacl_present,ACL &dacl,int &dacl_defaulted);
int                       GetSecurityDescriptorGroup(SECURITY_DESCRIPTOR &security_descriptor,SID &group,int &group_defaulted);
uint                      GetSecurityDescriptorLength(SECURITY_DESCRIPTOR &security_descriptor);
int                       GetSecurityDescriptorOwner(SECURITY_DESCRIPTOR &security_descriptor,SID &owner,int &owner_defaulted);
uint                      GetSecurityDescriptorRMControl(SECURITY_DESCRIPTOR &security_descriptor,uchar &rm_control);
int                       GetSecurityDescriptorSacl(SECURITY_DESCRIPTOR &security_descriptor,int &sacl_present,ACL &sacl,int &sacl_defaulted);
PVOID                     GetSidIdentifierAuthority(SID &sid);
uint                      GetSidLengthRequired(uchar sub_authority_count);
PVOID                     GetSidSubAuthority(SID &sid,uint sub_authority);
PVOID                     GetSidSubAuthorityCount(SID &sid);
int                       GetTokenInformation(HANDLE token_handle,TOKEN_INFORMATION_CLASS token_information_class,PVOID &token_information,uint token_information_length,uint &return_length);
int                       GetWindowsAccountDomainSid(SID &sid,SID &domain_sid,uint &domain_sid);
int                       ImpersonateAnonymousToken(HANDLE thread_handle);
int                       ImpersonateLoggedOnUser(HANDLE token);
int                       ImpersonateSelf(SECURITY_IMPERSONATION_LEVEL impersonation_level);
int                       InitializeAcl(ACL &acl,uint acl_length,uint acl_revision);
int                       InitializeSecurityDescriptor(SECURITY_DESCRIPTOR &security_descriptor,uint revision);
int                       InitializeSid(SID &sid,SID_IDENTIFIER_AUTHORITY &identifier_authority,uchar sub_authority_count);
int                       IsTokenRestricted(HANDLE token_handle);
int                       IsValidAcl(ACL &acl);
int                       IsValidSecurityDescriptor(SECURITY_DESCRIPTOR &security_descriptor);
int                       IsValidSid(SID &sid);
int                       IsWellKnownSid(SID &sid,WELL_KNOWN_SID_TYPE well_known_sid_type);
int                       MakeAbsoluteSD(SECURITY_DESCRIPTOR &self_relative_security_descriptor,SECURITY_DESCRIPTOR  &absolute_security_descriptor,uint &absolute_security_descriptor_size,ACL &dacl,uint &dacl_size,ACL &sacl,uint &sacl_size,SID &owner,uint &owner_size,SID &primary_group,uint &primary_group_size);
int                       MakeSelfRelativeSD(SECURITY_DESCRIPTOR &absolute_security_descriptor,SECURITY_DESCRIPTOR  &self_relative_security_descriptor,uint &buffer_length);
void                      MapGenericMask(uint &access_mask,GENERIC_MAPPING &generic_mapping);
int                       ObjectCloseAuditAlarmW(const string subsystem_name,PVOID handle_id,int generate_on_close);
int                       ObjectDeleteAuditAlarmW(const string subsystem_name,PVOID handle_id,int generate_on_close);
int                       ObjectOpenAuditAlarmW(const string subsystem_name,PVOID handle_id,string object_type_name,string object_name,SECURITY_DESCRIPTOR &security_descriptor,HANDLE client_token,uint desired_access,uint granted_access,PRIVILEGE_SET &privileges,int object_creation,int access_granted,int &generate_on_close);
int                       ObjectPrivilegeAuditAlarmW(const string subsystem_name,PVOID handle_id,HANDLE client_token,uint desired_access,PRIVILEGE_SET &privileges,int access_granted);
int                       PrivilegeCheck(HANDLE client_token,PRIVILEGE_SET &required_privileges,int &result);
int                       PrivilegedServiceAuditAlarmW(const string subsystem_name,const string service_name,HANDLE client_token,PRIVILEGE_SET &privileges,int access_granted);
void                      QuerySecurityAccessMask(uint security_information,uint &desired_access);
int                       RevertToSelf(void);
int                       SetAclInformation(ACL &acl,PVOID acl_information,uint acl_information_length,ACL_INFORMATION_CLASS acl_information_class);
int                       SetFileSecurityW(const string file_name,uint security_information,SECURITY_DESCRIPTOR &security_descriptor);
int                       SetKernelObjectSecurity(HANDLE handle,uint security_information,SECURITY_DESCRIPTOR &security_descriptor);
int                       SetPrivateObjectSecurity(uint security_information,SECURITY_DESCRIPTOR &modification_descriptor,SECURITY_DESCRIPTOR  &objects_security_descriptor,GENERIC_MAPPING &generic_mapping,HANDLE token);
int                       SetPrivateObjectSecurityEx(uint security_information,SECURITY_DESCRIPTOR &modification_descriptor,SECURITY_DESCRIPTOR  &objects_security_descriptor,uint auto_inherit_flags,GENERIC_MAPPING &generic_mapping,HANDLE token);
void                      SetSecurityAccessMask(uint security_information,uint &desired_access);
int                       SetSecurityDescriptorControl(SECURITY_DESCRIPTOR &security_descriptor,ushort control_bits_of_interest,ushort control_bits_to_set);
int                       SetSecurityDescriptorDacl(SECURITY_DESCRIPTOR &security_descriptor,int dacl_present,ACL &dacl,int dacl_defaulted);
int                       SetSecurityDescriptorGroup(SECURITY_DESCRIPTOR &security_descriptor,SID &group,int group_defaulted);
int                       SetSecurityDescriptorOwner(SECURITY_DESCRIPTOR &security_descriptor,SID &owner,int owner_defaulted);
uint                      SetSecurityDescriptorRMControl(SECURITY_DESCRIPTOR &security_descriptor,uchar &rm_control);
int                       SetSecurityDescriptorSacl(SECURITY_DESCRIPTOR &security_descriptor,int sacl_present,ACL &sacl,int sacl_defaulted);
int                       SetTokenInformation(HANDLE token_handle,TOKEN_INFORMATION_CLASS token_information_class,PVOID token_information,uint token_information_length);
int                       CveEventWrite(const string cve_id,const string additional_details);
#import

#import "kernel32.dll"
int                       AddResourceAttributeAce(ACL &acl,uint ace_revision,uint ace_flags,uint access_mask,SID &sid,CLAIM_SECURITY_ATTRIBUTES_INFORMATION &attribute_info,uint &return_length);
int                       AddScopedPolicyIDAce(ACL &acl,uint ace_revision,uint ace_flags,uint access_mask,SID &sid);
int                       CheckTokenCapability(HANDLE token_handle,SID &capability_sid_to_check,int &has_capability);
int                       GetAppContainerAce(ACL &acl,uint starting_ace_index,PVOID &app_container_ace,uint &app_container_ace_index);
int                       CheckTokenMembershipEx(HANDLE token_handle,SID &sid_to_check,uint flags,int &is_member);
int                       SetCachedSigningLevel(HANDLE &source_files,uint source_file_count,uint flags,HANDLE target_file);
int                       GetCachedSigningLevel(HANDLE file,ulong flags,ulong signing_level,uchar &thumbprint[],ulong thumbprint_size,ulong thumbprint_algorithm);
#import
//+------------------------------------------------------------------+
