/*
 Navicat Premium Dump SQL

 Source Server         : ubuntu
 Source Server Type    : MariaDB
 Source Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 Source Host           : 10.0.5.2:3306
 Source Schema         : GadgetsMenu

 Target Server Type    : MariaDB
 Target Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 File Encoding         : 65001

 Date: 02/11/2025 17:13:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for GadgetsMenu_Data
-- ----------------------------
DROP TABLE IF EXISTS `GadgetsMenu_Data`;
CREATE TABLE `GadgetsMenu_Data`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `UUID` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `Name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `Mystery_Dust` int(11) NOT NULL DEFAULT 0,
  `Mystery_Gift_Packs` int(11) NOT NULL DEFAULT 0,
  `Mystery_Gift_Sent` int(11) NOT NULL DEFAULT 0,
  `Mystery_Gift_Received` int(11) NOT NULL DEFAULT 0,
  `Pet_Name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'Pet',
  `Self_Morph_View` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'true',
  `Bypass_Cooldown` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'false',
  `Mystery_Vault_Animation` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'Normal',
  `Recent_Loots_Found` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `Selected_Hat` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Animated_Hat` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Particle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Suit_Helmet` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Suit_Chestplate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Suit_Leggings` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Suit_Boots` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Gadget` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Pet` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Morph` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Banner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Emote` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  `Selected_Cloak` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL DEFAULT 'none',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `playeruuid`(`UUID`) USING HASH
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of GadgetsMenu_Data
-- ----------------------------
INSERT INTO `GadgetsMenu_Data` VALUES (1, '2df2d00c-a7ab-3920-b3f1-62a699171684', 'l_oveMe', 0, 0, 0, 0, '§bl_oveMe\'s pet', 'true', 'false', 'Summer', '', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none');
INSERT INTO `GadgetsMenu_Data` VALUES (2, 'dbb599a7-c607-3ab9-b5e9-7ec9bf060176', 'MUHUIxingchen', 0, 0, 0, 0, '§bMUHUIxingchen的宠物', 'true', 'false', 'Normal', '', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none');
INSERT INTO `GadgetsMenu_Data` VALUES (3, 'b8bf68ab-73c9-315a-9d20-cd9a333368eb', 'LnehC_', 0, 0, 0, 0, '§bLnehC_的宠物', 'true', 'false', 'Normal', '', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none');
INSERT INTO `GadgetsMenu_Data` VALUES (4, '63d82913-3793-31ac-be7f-030d58a75872', 'system', 0, 0, 0, 0, '§bsystem的宠物', 'true', 'false', 'Normal', '', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none');
INSERT INTO `GadgetsMenu_Data` VALUES (5, '4e335955-7f5b-3543-adf8-a46caf2b11d3', 'ChengZhiMeow', 0, 0, 0, 0, '§bChengZhiMeow的宠物', 'true', 'false', 'Normal', '', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none');
INSERT INTO `GadgetsMenu_Data` VALUES (6, '59db8564-e8d2-3943-b9c7-0f230bc2463d', 'ALK00E', 0, 0, 0, 0, '§bALK00E的宠物', 'true', 'false', 'Normal', '', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none', 'none');

-- ----------------------------
-- Table structure for GadgetsMenu_Mystery_Boxes
-- ----------------------------
DROP TABLE IF EXISTS `GadgetsMenu_Mystery_Boxes`;
CREATE TABLE `GadgetsMenu_Mystery_Boxes`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `UUID` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `UID` int(11) NOT NULL,
  `Loots` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `playeruuid`(`UUID`) USING HASH
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of GadgetsMenu_Mystery_Boxes
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
