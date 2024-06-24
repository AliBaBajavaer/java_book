package com.camps.shardingdemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.camps.shardingdemo.mapper.StudentMapper;
import com.camps.shardingdemo.mapper.UserMapper;
import com.camps.shardingdemo.model.Student;
import com.camps.shardingdemo.model.User;

@RestController
@RequestMapping("/camps")
public class StudentController {

    @Autowired
    StudentMapper studentMapper;

    @Autowired
    UserMapper userMapper;

    @RequestMapping("/addStudent")
    public String add(@RequestParam("name") String name, @RequestParam("createTime") String createTime, @RequestParam("gradeCode") String gradeCode) {
        Student student = new Student()
                .withName(name)
                .withCreateTime(createTime)
                .withGradeCode(gradeCode);
        studentMapper.insert(student);
        return "success";
    }

    @RequestMapping("/queryStudentById")
    public Student queryStudentByTime(@RequestParam("id") long id) {
        Student student = studentMapper.selectById(id);
        return student;
    }

    @RequestMapping("/addUser")
    public String addUser(@RequestParam("name") String name, @RequestParam("createTime") String createTime, @RequestParam("age") int age) {
        User user = new User()
                .withName(name)
                .withCreateTime(createTime)
                .withAge(age);
        userMapper.insert(user);
        return "success";
    }

}
