CREATE DATABASE datatooltk;

GRANT DELETE,INSERT,SELECT,UPDATE,CREATE,DROP ON datatooltk.* TO 'datatool'@'localhost' IDENTIFIED BY 'datatool-test';

USE datatooltk;

CREATE TABLE `testsqldata` (
  `temp` float DEFAULT NULL,
  `time` int(11) DEFAULT NULL,
  `t2g` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

LOCK TABLES `testsqldata` WRITE;
INSERT INTO `testsqldata` VALUES (40,120,40),(40,90,60),(35,180,20),(55,190,40);
UNLOCK TABLES;
