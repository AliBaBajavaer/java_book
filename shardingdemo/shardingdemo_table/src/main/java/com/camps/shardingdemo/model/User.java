package com.camps.shardingdemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@NoArgsConstructor
@AllArgsConstructor
@With
public class User {
    private long id;
    private String name;
    private int age;
    private String createTime;
}