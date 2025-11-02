/*
 Navicat Premium Dump SQL

 Source Server         : ubuntu
 Source Server Type    : MariaDB
 Source Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 Source Host           : 10.0.5.2:3306
 Source Schema         : authme

 Target Server Type    : MariaDB
 Target Server Version : 110803 (11.8.3-MariaDB-ubu2404)
 File Encoding         : 65001

 Date: 02/11/2025 17:12:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for authme
-- ----------------------------
DROP TABLE IF EXISTS `authme`;
CREATE TABLE `authme`  (
  `id` mediumint(8) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_uca1400_ai_ci NOT NULL,
  `realname` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_uca1400_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `ip` varchar(40) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
  `lastlogin` bigint(20) NULL DEFAULT NULL,
  `x` double NOT NULL DEFAULT 0,
  `y` double NOT NULL DEFAULT 0,
  `z` double NOT NULL DEFAULT 0,
  `world` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_uca1400_ai_ci NOT NULL DEFAULT 'world',
  `regdate` bigint(20) NOT NULL DEFAULT 0,
  `regip` varchar(40) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
  `yaw` float NULL DEFAULT NULL,
  `pitch` float NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_uca1400_ai_ci NULL DEFAULT NULL,
  `isLogged` smallint(6) NOT NULL DEFAULT 0,
  `hasSession` smallint(6) NOT NULL DEFAULT 0,
  `totp` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_uca1400_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_uca1400_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of authme
-- ----------------------------
INSERT INTO `authme` VALUES (1, 'l_oveme', 'l_oveMe', '$SHA$8460aae082bf6b46$9ab511cdd4a018906ac1d3fb258b83222a01efac57731664829f110f51f150e3', '2408:8266:e01:49f3:5068:d1e0:e755:bff6', 1762074819128, 0, 0, 0, 'world', 1760887702505, '10.0.5.4', NULL, NULL, 'qianxun114514@gmail.com', 0, 1, NULL);
INSERT INTO `authme` VALUES (2, 'system', 'system', '$SHA$b8b4c9708037428b$1bae8a214292269741853189c259b14cf614f4c0e1981d3cab82d630355410e5', '10.0.5.4', 1761997432227, 0, 0, 0, 'world', 1760948047736, NULL, NULL, NULL, 'support@cnmsb.xin', 0, 1, NULL);
INSERT INTO `authme` VALUES (3, 'forever_millenni', 'Forever_Millenni', '$SHA$013bac1fa1c0b619$6f4c709ade0593461b43a3b723d682c7c1917c2075e78d7d24e92352dc45ad67', '127.0.0.1', 1760987946506, 0, 0, 0, 'world', 1760987946309, '127.0.0.1', NULL, NULL, '485467787@qq.com', 0, 1, NULL);
INSERT INTO `authme` VALUES (4, 'cobeti', 'Cobeti', '$SHA$dd90772b722c39b7$e0a67704dec5d138b0ec9659b0868f24cf7b00da97634ae66b537429938ea9ba', '127.0.0.1', 1761058357781, 0, 0, 0, 'world', 1760989329043, '127.0.0.1', NULL, NULL, '3920967248@qq.com', 0, 1, NULL);
INSERT INTO `authme` VALUES (5, 'muhuixingchen', 'MUHUIxingchen', '$SHA$40667f38ad55675b$2724b68ba7bff8dd8b0c729724a574a43b9aeb0292b0e3ca89537a2bf5af0fff', '2409:8a38:3c11:a8e0:48e8:3d03:c170:975', 1761140271611, 0, 0, 0, 'world', 1761138739453, '127.0.0.1', NULL, NULL, NULL, 0, 1, NULL);
INSERT INTO `authme` VALUES (6, 'lnehc_', 'LnehC_', '$SHA$3abc2053934f6fc2$129bf05e299b2de7e2fdb30638fae0d9b2f41a94119216d52f562e3c19c124e7', '2409:8a55:d4b2:3131:110e:6649:2c67:5400', 1761314324472, 0, 0, 0, 'world', 1761153295458, '2409:8a55:d4b2:3131:2530:387a:f52e:2ca0', NULL, NULL, '2743633653@qq.com', 0, 1, NULL);
INSERT INTO `authme` VALUES (7, 'chengzhimeow', 'ChengZhiMeow', '$SHA$0618f5141538d1da$afe0a5e591c77d91e5af9b94ef02202aa443dafe35edabb14ff8b5065e388e7c', '2409:8a34:824:b920:8dff:4a54:df9a:a223', 1761927960011, 0, 0, 0, 'world', 1761927745089, '2409:8a34:824:b920:8dff:4a54:df9a:a223', NULL, NULL, NULL, 0, 1, NULL);
INSERT INTO `authme` VALUES (8, 'alk00e', 'ALK00E', '$SHA$f36af3e05f2937ed$80dde2961d7698b5c85d84f4517c3c84d6a7a1db56c76227b13405d9cf9f3219', '127.0.0.1', 1762050126228, 0, 0, 0, 'world', 1762050126036, '127.0.0.1', NULL, NULL, NULL, 0, 1, NULL);

SET FOREIGN_KEY_CHECKS = 1;
