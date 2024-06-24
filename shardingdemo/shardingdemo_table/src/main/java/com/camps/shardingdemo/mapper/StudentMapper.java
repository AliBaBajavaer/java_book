package com.camps.shardingdemo.mapper;

import org.apache.ibatis.annotations.*;
import com.camps.shardingdemo.model.Student;

import java.util.List;

@Mapper
public interface StudentMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO t_student (`name`,`create_time`,`grade_code`) VALUES (#{student.name},#{student.createTime},#{student.gradeCode})")
    int insert(@Param("student") Student student);

    @Select("SELECT * FROM t_student WHERE id=#{id}")
    Student selectById(@Param("id") long id);

    @Select("SELECT * FROM t_student WHERE create_time >= #{start} AND create_time < #{end}")
    List<Student> queryByTime(@Param("start") String start, @Param("end") String end);
}
