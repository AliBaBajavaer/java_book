package com.camps.shardingdemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.camps.shardingdemo.mapper.StudentMapper;
import com.camps.shardingdemo.model.Student;

@RestController
@RequestMapping("/camps")
public class StudentController {

    @Autowired
    StudentMapper studentMapper;

    @RequestMapping("/addStudent")
    public String add(@RequestParam("name") String name, @RequestParam("createTime") String createTime, @RequestParam("gradeCode") String gradeCode) {
        Student student = new Student()
                .withName(name)
                .withCreateTime(createTime)
                .withGradeCode(gradeCode);
        studentMapper.insert(student);
        return "success";
    }
}
