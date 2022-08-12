//+------------------------------------------------------------------+
//|                                                       windef.mqh |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#define HANDLE                         long
#define PVOID                          long
//---
#define ANYSIZE_ARRAY                  1
#define MAX_BREAKPOINTS                8
#define MAX_WATCHPOINTS                2
#define MAX_HW_COUNTERS                16
#define MAX_PATH                       260
#define EXCEPTION_MAXIMUM_PARAMETERS   15

//---
enum LATENCY_TIME
  {
   LT_DONT_CARE,
   LT_LOWEST_LATENCY
  };
//---
enum GET_FILEEX_INFO_LEVELS
  {
   GetFileExInfoStandard,
   GetFileExMaxInfoLevel
  };
//---
enum FINDEX_INFO_LEVELS
  {
   FindExInfoStandard,
   FindExInfoBasic,
   FindExInfoMaxInfoLevel
  };
//---
enum FINDEX_SEARCH_OPS
  {
   FindExSearchNameMatch,
   FindExSearchLimitToDirectories,
   FindExSearchLimitToDevices,
   FindExSearchMaxSearchOp
  };
//---
enum DPI_AWARENESS
  {
   DPI_AWARENESS_INVALID=-1,
   DPI_AWARENESS_UNAWARE=0,
   DPI_AWARENESS_SYSTEM_AWARE=1,
   DPI_AWARENESS_PER_MONITOR_AWARE=2
  };
//---
enum DPI_HOSTING_BEHAVIOR
  {
   DPI_HOSTING_BEHAVIOR_INVALID=-1,
   DPI_HOSTING_BEHAVIOR_DEFAULT=0,
   DPI_HOSTING_BEHAVIOR_MIXED=1
  };
//---
enum FILE_INFO_BY_HANDLE_CLASS
  {
   FileBasicInfo=0,
   FileStandardInfo=1,
   FileNameInfo=2,
   FileRenameInfo=3,
   FileDispositionInfo= 4,
   FileAllocationInfo = 5,
   FileEndOfFileInfo=6,
   FileStreamInfo=7,
   FileCompressionInfo=8,
   FileAttributeTagInfo=9,
   FileIdBothDirectoryInfo=10,
   FileIdBothDirectoryRestartInfo=11,
   FileIoPriorityHintInfo = 12,
   FileRemoteProtocolInfo = 13,
   FileFullDirectoryInfo=14,
   FileFullDirectoryRestartInfo=15,
   FileStorageInfo=16,
   FileAlignmentInfo=17,
   FileIdInfo=18,
   FileIdExtdDirectoryInfo=19,
   FileIdExtdDirectoryRestartInfo=20,
   MaximumFileInfoByHandlesClass
  };
//---
enum READ_DIRECTORY_NOTIFY_INFORMATION_CLASS
  {
   ReadDirectoryNotifyInformation=1,
   ReadDirectoryNotifyExtendedInformation
  };
//---
enum WELL_KNOWN_SID_TYPE
  {
   WinNullSid=0,
   WinWorldSid= 1,
   WinLocalSid= 2,
   WinCreatorOwnerSid= 3,
   WinCreatorGroupSid= 4,
   WinCreatorOwnerServerSid=5,
   WinCreatorGroupServerSid= 6,
   WinNtAuthoritySid=7,
   WinDialupSid=8,
   WinNetworkSid=9,
   WinBatchSid=10,
   WinInteractiveSid=11,
   WinServiceSid=12,
   WinAnonymousSid=13,
   WinProxySid=14,
   WinEnterpriseControllersSid=15,
   WinSelfSid=16,
   WinAuthenticatedUserSid=17,
   WinRestrictedCodeSid= 18,
   WinTerminalServerSid= 19,
   WinRemoteLogonIdSid=20,
   WinLogonIdsSid=21,
   WinLocalSystemSid=22,
   WinLocalServiceSid=23,
   WinNetworkServiceSid=24,
   WinBuiltinDomainSid=25,
   WinBuiltinAdministratorsSid=26,
   WinBuiltinUsersSid=27,
   WinBuiltinGuestsSid=28,
   WinBuiltinPowerUsersSid=29,
   WinBuiltinAccountOperatorsSid=30,
   WinBuiltinSystemOperatorsSid=31,
   WinBuiltinPrintOperatorsSid=32,
   WinBuiltinBackupOperatorsSid=33,
   WinBuiltinReplicatorSid=34,
   WinBuiltinPreWindows2000CompatibleAccessSid=35,
   WinBuiltinRemoteDesktopUsersSid=36,
   WinBuiltinNetworkConfigurationOperatorsSid=37,
   WinAccountAdministratorSid=38,
   WinAccountGuestSid=39,
   WinAccountKrbtgtSid=40,
   WinAccountDomainAdminsSid=41,
   WinAccountDomainUsersSid=42,
   WinAccountDomainGuestsSid=43,
   WinAccountComputersSid=44,
   WinAccountControllersSid=45,
   WinAccountCertAdminsSid=46,
   WinAccountSchemaAdminsSid=47,
   WinAccountEnterpriseAdminsSid=48,
   WinAccountPolicyAdminsSid=49,
   WinAccountRasAndIasServersSid=50,
   WinNTLMAuthenticationSid=51,
   WinDigestAuthenticationSid=52,
   WinSChannelAuthenticationSid=53,
   WinThisOrganizationSid=54,
   WinOtherOrganizationSid=55,
   WinBuiltinIncomingForestTrustBuildersSid=56,
   WinBuiltinPerfMonitoringUsersSid=57,
   WinBuiltinPerfLoggingUsersSid=58,
   WinBuiltinAuthorizationAccessSid=59,
   WinBuiltinTerminalServerLicenseServersSid=60,
   WinBuiltinDCOMUsersSid=61,
   WinBuiltinIUsersSid=62,
   WinIUserSid=63,
   WinBuiltinCryptoOperatorsSid=64,
   WinUntrustedLabelSid=65,
   WinLowLabelSid=66,
   WinMediumLabelSid=67,
   WinHighLabelSid=68,
   WinSystemLabelSid=69,
   WinWriteRestrictedCodeSid=70,
   WinCreatorOwnerRightsSid=71,
   WinCacheablePrincipalsGroupSid=72,
   WinNonCacheablePrincipalsGroupSid=73,
   WinEnterpriseReadonlyControllersSid=74,
   WinAccountReadonlyControllersSid=75,
   WinBuiltinEventLogReadersGroup=76,
   WinNewEnterpriseReadonlyControllersSid=77,
   WinBuiltinCertSvcDComAccessGroup=78,
   WinMediumPlusLabelSid=79,
   WinLocalLogonSid=80,
   WinConsoleLogonSid=81,
   WinThisOrganizationCertificateSid= 82,
   WinApplicationPackageAuthoritySid= 83,
   WinBuiltinAnyPackageSid=84,
   WinCapabilityInternetClientSid=85,
   WinCapabilityInternetClientServerSid=86,
   WinCapabilityPrivateNetworkClientServerSid=87,
   WinCapabilityPicturesLibrarySid=88,
   WinCapabilityVideosLibrarySid=89,
   WinCapabilityMusicLibrarySid=90,
   WinCapabilityDocumentsLibrarySid=91,
   WinCapabilitySharedUserCertificatesSid=92,
   WinCapabilityEnterpriseAuthenticationSid=93,
   WinCapabilityRemovableStorageSid=94,
   WinBuiltinRDSRemoteAccessServersSid=95,
   WinBuiltinRDSEndpointServersSid=96,
   WinBuiltinRDSManagementServersSid=97,
   WinUserModeDriversSid=98,
   WinBuiltinHyperVAdminsSid=99,
   WinAccountCloneableControllersSid=100,
   WinBuiltinAccessControlAssistanceOperatorsSid=101,
   WinBuiltinRemoteManagementUsersSid=102,
   WinAuthenticationAuthorityAssertedSid=103,
   WinAuthenticationServiceAssertedSid=104,
   WinLocalAccountSid=105,
   WinLocalAccountAndAdministratorSid=106,
   WinAccountProtectedUsersSid=107,
   WinCapabilityAppointmentsSid=108,
   WinCapabilityContactsSid=109,
   WinAccountDefaultSystemManagedSid=110,
   WinBuiltinDefaultSystemManagedGroupSid=111,
   WinBuiltinStorageReplicaAdminsSid=112,
   WinAccountKeyAdminsSid=113,
   WinAccountEnterpriseKeyAdminsSid=114,
   WinAuthenticationKeyTrustSid=115,
   WinAuthenticationKeyPropertyMFASid=116,
   WinAuthenticationKeyPropertyAttestationSid=117,
   WinAuthenticationFreshKeyAuthSid=118,
   WinBuiltinDeviceOwnersSid=119
  };
//---
union FILE_SEGMENT_ELEMENT
  {
   PVOID             Buffer;
   ulong             Alignment;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
//---
struct REASON_CONTEXT
  {
   uint              Version;
   uint              Flags;
   PVOID             Reason;
  };
//---
struct OVERLAPPED
  {
   PVOID             Internal;
   PVOID             InternalHigh;
   uint              Offset;
   uint              OffsetHigh;
   HANDLE            hEvent;
  };
//---
struct LDT_ENTRY
  {
   ushort            LimitLow;
   ushort            BaseLow;
   uchar             BaseMid;
   uchar             Flags1;
   uchar             Flags2;
   uchar             BaseHi;
  };
//---
struct GUID
  {
   ulong             Data1;
   ushort            Data2;
   ushort            Data3;
   uchar             Data4[8];
  };
//---
struct FILETIME
  {
   uint              dwLowDateTime;
   uint              dwHighDateTime;
  };
//---
struct POINT
  {
   int               x;
   int               y;
  };
//---
struct POINTL
  {
   int               x;
   int               y;
  };
//---
struct POINTS
  {
   short             x;
   short             y;
  };
//---
struct RECT
  {
   int               left;
   int               top;
   int               right;
   int               bottom;
  };
//---
struct RECTL
  {
   int               left;
   int               top;
   int               right;
   int               bottom;
  };
//---
struct SIZE
  {
   int               cx;
   int               cy;
  };
//---
struct FILE_INFO
  {
  };
//---
struct CLAIM_SECURITY_ATTRIBUTE_V1
  {
   PVOID             Name;
   ushort            ValueType;
   ushort            Reserved;
   uint              Flags;
   uint              ValueCount;
   PVOID             Values;
  };
//---
struct CLAIM_SECURITY_ATTRIBUTES_INFORMATION
  {
   ushort            Version;
   ushort            Reserved;
   uint              AttributeCount;
   PVOID             Attribute;
  };
//+------------------------------------------------------------------+
