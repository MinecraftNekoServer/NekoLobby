/*
 Navicat Premium Dump SQL

 Source Server         : ubuntu
 Source Server Type    : MariaDB
 Source Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 Source Host           : 10.0.5.2:3306
 Source Schema         : nekobedwars

 Target Server Type    : MariaDB
 Target Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 File Encoding         : 65001

 Date: 02/11/2025 17:13:38
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bw_stats_players
-- ----------------------------
DROP TABLE IF EXISTS `bw_stats_players`;
CREATE TABLE `bw_stats_players`  (
  `kills` int(11) NOT NULL DEFAULT 0,
  `wins` int(11) NOT NULL DEFAULT 0,
  `score` int(11) NOT NULL DEFAULT 0,
  `loses` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `destroyedBeds` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `deaths` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`uuid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bw_stats_players
-- ----------------------------
INSERT INTO `bw_stats_players` VALUES (1, 8, 435, 0, 'l_oveMe', 1, '2df2d00c-a7ab-3920-b3f1-62a699171684', 0);
INSERT INTO `bw_stats_players` VALUES (0, 0, 0, 1, 'system', 0, '63d82913-3793-31ac-be7f-030d58a75872', 1);

SET FOREIGN_KEY_CHECKS = 1;
