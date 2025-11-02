/*
 Navicat Premium Dump SQL

 Source Server         : ubuntu
 Source Server Type    : MariaDB
 Source Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 Source Host           : 10.0.5.2:3306
 Source Schema         : thepit

 Target Server Type    : MariaDB
 Target Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 File Encoding         : 65001

 Date: 02/11/2025 16:24:42
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ThePitChatOptions
-- ----------------------------
DROP TABLE IF EXISTS `ThePitChatOptions`;
CREATE TABLE `ThePitChatOptions`  (
  `Player` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT NULL,
  `UUID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `BOUNTIES` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'true',
  `STREAKS` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'true',
  `PRESTIGE` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'true',
  `MINOR_EVENTS` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'true',
  `KILL_FEED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'true',
  `PLAYER_CHAT` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'true',
  `MISCELLANEOUS` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'true',
  PRIMARY KEY (`UUID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ThePitChatOptions
-- ----------------------------
INSERT INTO `ThePitChatOptions` VALUES ('l_oveMe', '2df2d00c-a7ab-3920-b3f1-62a699171684', 'true', 'true', 'true', 'true', 'true', 'true', 'true');
INSERT INTO `ThePitChatOptions` VALUES ('system', '63d82913-3793-31ac-be7f-030d58a75872', 'true', 'true', 'true', 'true', 'true', 'true', 'true');

-- ----------------------------
-- Table structure for ThePitPerks
-- ----------------------------
DROP TABLE IF EXISTS `ThePitPerks`;
CREATE TABLE `ThePitPerks`  (
  `Player` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT NULL,
  `UUID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `GOLDEN_HEADS_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `GOLDEN_HEADS_SLOT` int(1) NULL DEFAULT 0,
  `FISHING_ROD_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `FISHING_ROD_SLOT` int(1) NULL DEFAULT 0,
  `LAVA_BUCKET_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `LAVA_BUCKET_SLOT` int(1) NULL DEFAULT 0,
  `STRENGTH_CHAINING_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `STRENGTH_CHAINING_SLOT` int(1) NULL DEFAULT 0,
  `ENDLESS_QUIVER_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `ENDLESS_QUIVER_SLOT` int(1) NULL DEFAULT 0,
  `MINEMAN_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `MINEMAN_SLOT` int(1) NULL DEFAULT 0,
  `SAFETY_FIRST_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `SAFETY_FIRST_SLOT` int(1) NULL DEFAULT 0,
  `TRICKLE_DOWN_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `TRICKLE_DOWN_SLOT` int(1) NULL DEFAULT 0,
  `LUCKY_DIAMOND_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `LUCKY_DIAMOND_SLOT` int(1) NULL DEFAULT 0,
  `SPAMMER_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `SPAMMER_SLOT` int(1) NULL DEFAULT 0,
  `BOUNTY_HUNTER_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `BOUNTY_HUNTER_SLOT` int(1) NULL DEFAULT 0,
  `STREAKER_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `STREAKER_SLOT` int(1) NULL DEFAULT 0,
  `GLADIATOR_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `GLADIATOR_SLOT` int(1) NULL DEFAULT 0,
  `VAMPIRE_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `VAMPIRE_SLOT` int(1) NULL DEFAULT 0,
  `OVERHEAL_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `OVERHEAL_SLOT` int(1) NULL DEFAULT 0,
  `BARBARIAN_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `BARBARIAN_SLOT` int(1) NULL DEFAULT 0,
  `DIRTY_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `DIRTY_SLOT` int(1) NULL DEFAULT 0,
  `RAMBO_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `RAMBO_SLOT` int(1) NULL DEFAULT 0,
  `OLYMPUS_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `OLYMPUS_SLOT` int(1) NULL DEFAULT 0,
  `FIRST_STRIKE_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `FIRST_STRIKE_SLOT` int(1) NULL DEFAULT 0,
  `ASSIST_STREAKER_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `ASSIST_STREAKER_SLOT` int(1) NULL DEFAULT 0,
  `MARATHON_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `MARATHON_SLOT` int(1) NULL DEFAULT 0,
  `SOUP_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `SOUP_SLOT` int(1) NULL DEFAULT 0,
  `RECON_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `RECON_SLOT` int(1) NULL DEFAULT 0,
  `CONGLOMERATE_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `CONGLOMERATE_SLOT` int(1) NULL DEFAULT 0,
  `KUNG_FU_KNOWLEDGE_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `KUNG_FU_KNOWLEDGE_SLOT` int(1) NULL DEFAULT 0,
  `CO_OP_CAT_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `CO_OP_CAT_SLOT` int(1) NULL DEFAULT 0,
  `THICK_OWNED` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT 'false',
  `THICK_SLOT` int(1) NULL DEFAULT 0,
  PRIMARY KEY (`UUID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ThePitPerks
-- ----------------------------
INSERT INTO `ThePitPerks` VALUES ('l_oveMe', '2df2d00c-a7ab-3920-b3f1-62a699171684', 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0);
INSERT INTO `ThePitPerks` VALUES ('system', '63d82913-3793-31ac-be7f-030d58a75872', 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0, 'false', 0);

-- ----------------------------
-- Table structure for ThePitPrestige
-- ----------------------------
DROP TABLE IF EXISTS `ThePitPrestige`;
CREATE TABLE `ThePitPrestige`  (
  `Player` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `UUID` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `Prestige` int(11) NULL DEFAULT 0,
  `Renown` int(11) NULL DEFAULT 0,
  `Grinded` int(11) NULL DEFAULT 0,
  PRIMARY KEY (`UUID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ThePitPrestige
-- ----------------------------
INSERT INTO `ThePitPrestige` VALUES ('l_oveMe', '2df2d00c-a7ab-3920-b3f1-62a699171684', 0, 0, 18);
INSERT INTO `ThePitPrestige` VALUES ('system', '63d82913-3793-31ac-be7f-030d58a75872', 0, 0, 0);

-- ----------------------------
-- Table structure for ThePitProfiles
-- ----------------------------
DROP TABLE IF EXISTS `ThePitProfiles`;
CREATE TABLE `ThePitProfiles`  (
  `Player` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `UUID` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `Balance` int(11) NULL DEFAULT 0,
  `Bounty` int(11) NULL DEFAULT 0,
  `Level` int(3) NULL DEFAULT 1,
  PRIMARY KEY (`UUID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ThePitProfiles
-- ----------------------------
INSERT INTO `ThePitProfiles` VALUES ('l_oveMe', '2df2d00c-a7ab-3920-b3f1-62a699171684', 28, 0, 120);
INSERT INTO `ThePitProfiles` VALUES ('system', '63d82913-3793-31ac-be7f-030d58a75872', 50, 0, 1);

-- ----------------------------
-- Table structure for ThePitStats
-- ----------------------------
DROP TABLE IF EXISTS `ThePitStats`;
CREATE TABLE `ThePitStats`  (
  `Player` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NULL DEFAULT NULL,
  `UUID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci NOT NULL,
  `BOW_DAMAGE_TAKEN` int(11) NULL DEFAULT 0,
  `DAMAGE_TAKEN` int(11) NULL DEFAULT 0,
  `DEATHS` int(11) NULL DEFAULT 0,
  `MELEE_DAMAGE_TAKEN` int(11) NULL DEFAULT 0,
  `BLOCKS_BROKEN` int(11) NULL DEFAULT 0,
  `BLOCKS_PLACED` int(11) NULL DEFAULT 0,
  `CHAT_MESSAGES` int(11) NULL DEFAULT 0,
  `FISHING_RODS_LAUNCHED` int(11) NULL DEFAULT 0,
  `GOLDEN_APPLES_EATEN` int(11) NULL DEFAULT 0,
  `JUMPS_INTO_PIT` int(11) NULL DEFAULT 0,
  `LAVA_BUCKETS_EMPTIED` int(11) NULL DEFAULT 0,
  `LEFT_CLICKS` int(11) NULL DEFAULT 0,
  `ARROW_HITS` int(11) NULL DEFAULT 0,
  `ARROWS_SHOT` int(11) NULL DEFAULT 0,
  `ASSISTS` int(11) NULL DEFAULT 0,
  `BOW_DAMAGE_DEALT` int(11) NULL DEFAULT 0,
  `DIAMOND_ITEMS_PURCHASED` int(11) NULL DEFAULT 0,
  `LAUNCHES` int(11) NULL DEFAULT 0,
  `DAMAGE_DEALT` int(11) NULL DEFAULT 0,
  `HIGHEST_STREAK` int(11) NULL DEFAULT 0,
  `KILLS` int(11) NULL DEFAULT 0,
  `MELEE_DAMAGE_DEALT` int(11) NULL DEFAULT 0,
  `SWORD_HITS` int(11) NULL DEFAULT 0,
  `GOLD_EARNED` int(11) NULL DEFAULT 0,
  `GOLDEN_HEADS_EATEN` int(11) NULL DEFAULT 0,
  PRIMARY KEY (`UUID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ThePitStats
-- ----------------------------
INSERT INTO `ThePitStats` VALUES ('l_oveMe', '2df2d00c-a7ab-3920-b3f1-62a699171684', 0, 0, 0, 0, 0, 8, 6, 0, 1, 10, 0, 118, 1, 87, 0, 2, 0, 0, 0, 1, 1, 19, 8, 18, 0);
INSERT INTO `ThePitStats` VALUES ('system', '63d82913-3793-31ac-be7f-030d58a75872', 2, 0, 1, 19, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

SET FOREIGN_KEY_CHECKS = 1;
