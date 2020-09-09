USE devops_ci_sign;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_SIGN_HISTORY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_SIGN_HISTORY` (
  `RESIGN_ID` varchar(64) NOT NULL,
  `USER_ID` varchar(64) NOT NULL DEFAULT 'system',
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `PIPELINE_ID` varchar(64) DEFAULT NULL,
  `BUILD_ID` varchar(64) DEFAULT NULL,
  `TASK_ID` varchar(64) DEFAULT NULL,
  `TASK_EXECUTE_COUNT` int(11) DEFAULT NULL,
  `ARCHIVE_TYPE` varchar(32) DEFAULT NULL,
  `ARCHIVE_PATH` text,
  `FILE_MD5` varchar(64) DEFAULT NULL,
  `STATUS` varchar(32) DEFAULT NULL,
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `END_TIME` timestamp NULL DEFAULT NULL,
  `RESULT_FILE_MD5` varchar(64) DEFAULT NULL,
  `RESULT_FILE_NAME` varchar(512) DEFAULT NULL,
  `UPLOAD_FINISH_TIME` timestamp NULL DEFAULT NULL,
  `UNZIP_FINISH_TIME` timestamp NULL DEFAULT NULL,
  `RESIGN_FINISH_TIME` timestamp NULL DEFAULT NULL,
  `ZIP_FINISH_TIME` timestamp NULL DEFAULT NULL,
  `ARCHIVE_FINISH_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`RESIGN_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_SIGN_IPA_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_SIGN_IPA_INFO` (
  `RESIGN_ID` varchar(64) NOT NULL,
  `USER_ID` varchar(64) NOT NULL,
  `WILDCARD` bit(1) NOT NULL,
  `CERT_ID` varchar(128) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `PIPELINE_ID` varchar(64) DEFAULT NULL,
  `BUILD_ID` varchar(64) DEFAULT NULL,
  `TASK_ID` varchar(64) DEFAULT NULL,
  `ARCHIVE_TYPE` varchar(32) DEFAULT NULL,
  `ARCHIVE_PATH` text,
  `MOBILE_PROVISION_ID` varchar(128) DEFAULT NULL,
  `UNIVERSAL_LINKS` text,
  `KEYCHAIN_ACCESS_GROUPS` text,
  `REPLACE_BUNDLE` bit(1) DEFAULT NULL,
  `APPEX_SIGN_INFO` text,
  `FILENAME` text,
  `FILE_SIZE` bigint(32) DEFAULT NULL,
  `FILE_MD5` varchar(64) DEFAULT NULL,
  `REQUEST_CONTENT` text NOT NULL,
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`RESIGN_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
