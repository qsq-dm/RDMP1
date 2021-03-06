use tmailcontest;
CREATE TABLE IF NOT EXISTS `TEMP_tmail_firstseason` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `brand_id` bigint(20) NOT NULL,
  `type` int(11) NOT NULL,
  `visit_datetime` date NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=182881 DEFAULT CHARSET=utf8 COMMENT='第一赛季的预测结果格式为：\r\n\r\n用户ID   商品ID\r\n6295680	24773,11923,15559\r\n9060360	21078,1173,9399\r\n4930440	3079,24909\r\n5905320	19736,24773,5477,9605,17606\r\n1954920	9738,15239,3051,2392\r\n763560	11456,18305,1173\r\n8462560	17606,24773,15239';

CREATE TABLE IF NOT EXISTS `WRK_tmail_firstseason` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `brand_id` bigint(20) NOT NULL,
  `type` int(11) NOT NULL,
  `visit_datetime` date NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=182881 DEFAULT CHARSET=utf8 COMMENT='第一赛季的预测结果格式为：\r\n\r\n用户ID   商品ID\r\n6295680	24773,11923,15559\r\n9060360	21078,1173,9399\r\n4930440	3079,24909\r\n5905320	19736,24773,5477,9605,17606\r\n1954920	9738,15239,3051,2392\r\n763560	11456,18305,1173\r\n8462560	17606,24773,15239';

CREATE TABLE IF NOT EXISTS `EXP_Table` (
  `system` char(20),
  `app` char(20),
  `key1` char(20),
  `key2` char(20),
  `time` datetime,
  `error_code` char(20),
  `error_desc` char(100)
);

insert into EXP_Table 
( system,
  app,
  key1,
  key2,
  time,
  error_code,
  error_desc
)
values
('Taobao','Transaction','1191','','2014-04-23 21:53:28','ERROR_CODE_001','Visit time is not illegal.'),
('Taobao','Transaction','2491','','2014-04-23 21:53:28','ERROR_CODE_001','Visit time is not illegal.'),
('Taobao','Transaction','3561','','2014-04-23 21:53:28','ERROR_CODE_001','Visit time is not illegal.'),
('Taobao','Transaction','31','','2014-04-23 21:53:28','ERROR_CODE_002','Type is not illegal.'),
('Taobao','Transaction','91191','','2014-04-23 21:53:28','ERROR_CODE_002','Type time is not illegal.'),
('Taobao','Transaction','891','','2014-04-23 21:53:28','ERROR_CODE_002','Visit time is not illegal.'),
('Taobao','Transaction','51','','2014-04-23 21:53:28','ERROR_CODE_001','Visit time is not illegal.');
