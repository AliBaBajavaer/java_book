package com.camps.shardingdemo.mapper;

import com.camps.shardingdemo.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO `t_user` (`name`,`create_time`,`age`) VALUES (#{user.name},#{user.createTime},#{user.age})")
    int insert(@Param("user") User user);
}
