package com.camps.shardingdemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@AllArgsConstructor
@NoArgsConstructor
@With
public class Student {
    private long id;
    private String name;
    private String createTime;
    private String gradeCode;
}
