CREATE DATABASE camps1;
USE camps1;
CREATE TABLE `t_student_1` (
                                `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键自增',
                                `name` varchar(32) NOT NULL DEFAULT '' COMMENT '姓名',
                                `create_time` varchar(32) NOT NULL DEFAULT '' COMMENT '创建日期,yyyy-MM-dd HH:mm:ss',
                                `grade_code` varchar(32) NOT NULL DEFAULT '' COMMENT '年级,first,second,third,fourth',
                                PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET = utf8;
CREATE TABLE `t_student_2` (
                                `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键自增',
                                `name` varchar(32) NOT NULL DEFAULT '' COMMENT '姓名',
                                `create_time` varchar(32) NOT NULL DEFAULT '' COMMENT '创建日期,yyyy-MM-dd HH:mm:ss',
                                `grade_code` varchar(32) NOT NULL DEFAULT '' COMMENT '年级,first,second,third,fourth',
                                PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET = utf8;


CREATE DATABASE camps2;
USE camps2;
CREATE TABLE `t_student_1` (
                               `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键自增',
                               `name` varchar(32) NOT NULL DEFAULT '' COMMENT '姓名',
                               `create_time` varchar(32) NOT NULL DEFAULT '' COMMENT '创建日期,yyyy-MM-dd HH:mm:ss',
                               `grade_code` varchar(32) NOT NULL DEFAULT '' COMMENT '年级,first,second,third,fourth',
                               PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET = utf8;
CREATE TABLE `t_student_2` (
                               `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键自增',
                               `name` varchar(32) NOT NULL DEFAULT '' COMMENT '姓名',
                               `create_time` varchar(32) NOT NULL DEFAULT '' COMMENT '创建日期,yyyy-MM-dd HH:mm:ss',
                               `grade_code` varchar(32) NOT NULL DEFAULT '' COMMENT '年级,first,second,third,fourth',
                               PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET = utf8;