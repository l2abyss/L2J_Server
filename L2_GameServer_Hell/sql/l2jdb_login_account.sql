-- ---------------------------
-- Table structure for accounts
-- ---------------------------
CREATE TABLE `accounts` (
  `login` VARCHAR(45) NOT NULL default '',
  `password` VARCHAR(45) ,
  `lastactive` DECIMAL(20),
  `accessLevel` TINYINT,
  `lastIP` CHAR(15),
  PRIMARY KEY (`login`)
);