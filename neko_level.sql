/*
 Navicat Premium Dump SQL

 Source Server         : ubuntu
 Source Server Type    : MariaDB
 Source Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 Source Host           : 10.0.5.2:3306
 Source Schema         : neko_level

 Target Server Type    : MariaDB
 Target Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 File Encoding         : 65001

 Date: 02/11/2025 17:13:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for player_levels
-- ----------------------------
DROP TABLE IF EXISTS `player_levels`;
CREATE TABLE `player_levels`  (
  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `level` int(11) NOT NULL DEFAULT 1,
  `experience` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`uuid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of player_levels
-- ----------------------------
INSERT INTO `player_levels` VALUES ('2df2d00c-a7ab-3920-b3f1-62a699171684', 'l_oveMe', 1, 20);
INSERT INTO `player_levels` VALUES ('4e335955-7f5b-3543-adf8-a46caf2b11d3', 'ChengZhiMeow', 1, 0);
INSERT INTO `player_levels` VALUES ('59db8564-e8d2-3943-b9c7-0f230bc2463d', 'ALK00E', 1, 0);
INSERT INTO `player_levels` VALUES ('63d82913-3793-31ac-be7f-030d58a75872', 'system', 2201, 0);
INSERT INTO `player_levels` VALUES ('b8bf68ab-73c9-315a-9d20-cd9a333368eb', 'LnehC_', 1, 0);
INSERT INTO `player_levels` VALUES ('dbb599a7-c607-3ab9-b5e9-7ec9bf060176', 'MUHUIxingchen', 1, 0);
INSERT INTO `player_levels` VALUES ('e81e6982-cddd-3623-8b4b-fc7f334b754c', 'Cobeti', 1, 0);
INSERT INTO `player_levels` VALUES ('f3034831-7bc6-314d-a4e3-7e07eb7474ab', 'Forever_Millenni', 1, 0);

SET FOREIGN_KEY_CHECKS = 1;
