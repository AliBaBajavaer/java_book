1.精确查询：
  term ：精确匹配
  range:范围查询
  term不会在做分词
                  GET /hotel/_search
                {
                  "query":
                    {
                      "term":
                      {
                        "city":
                        {
                          "value":"上海"
                        }
                      }
                    }
                }
                range是针对数值型，日期型的范围查询
                
                GET /hotel/_search
                {
                  "query":
                  {
                    "range":
                    {
                      "price":
                      {
                        "gte": 100,
                        "lte":1000
                      }
                    }
                  }
                }
2.全文检索
  match match_all，multi_match三个
                #批量查询    
              GET /hotel/_search
              {
                "query":{
                  "match_all": {}
                }
              }
              
              #根据单字段查询
              GET /hotel/_search
              {
                "query": {
                  "match": {
                    "all": "https"
                  }
                }
              }
              #根据多字段查
              GET /hotel/_search
              {
                "query": {
                  "multi_match": {
                    "query": "连锁",
                    "fields": ["brand","name","business"]
                  }
                }
              }
  3.地理查询
    查询geo_point在某个矩形范围内的点
              GET /hotel/_search
                {
                  "query":
                  {
                    "geo_bounding_box":
                    {
                      "location":
                      {
                        "top_left":
                        {
                          "lat":31.1,
                          "lon":121.5
                        },
                        "bottom_right":
                        {
                          "lat": 30.9,
                          "lon":121.7
                        }
                      }
                    }
                  }
                }
        geo_distance:圆形查询
        
              GET /hotel/_search
                  {
                    "query":
                    {
                      "geo_distance":
                      {
                        "distance": "30km",
                        "location":"31.21, 121.5"
                      }
                    }
                  }

    4.复合查询
          相关性算法：TF-IDF
           1. score =各词的 （词条频率*词条的逆文档频率） 之和
            每篇文章的每隔词条频率等于        词条出现次数/文档中词条总数
            每个词的逆文档频率等于            log(文档总数/包含词条的文档总数)
            2. BM25算法 相比于TF-IDF比较稳定，原理类似
      5.修改score:function score query

      GET /hotel/_search

{
  "query":
  {
    "function_score": {
      "query": {"match":{"all":"外滩"}},    得到初始数据
      "functions": [
        {
          "filter": {"term":{"brand":"如家"}}, 进行过滤
          "weight": 10                    进行加分  field_value_factor, random_score, script_score
        }
      ],
      "boost_mode": "multiply"            与原始得分相乘   replace替代   sum avg,max min 
    }
  }
}

4.boolean query 与funcation score相比，不会算分，而是进行进一步过滤
  must（与） must_not（非） should（陪陪一个即可）  filter（必须匹配）

  
GET /hotel/_search
{
  "query":
  {
    "bool":
    {
      "must":
      [
          {"term":{"city":"上海"}}
      ],
      "should":
      [
      {"term":{"brand": "皇冠假日"}},
      {"term":{"brand":"华美达"}}
      ],
      "must_not":
      [
      {"range":{"price":{"lte": 500}}}
      ],
      "filter":
      [
      {"range":
      {
        "score":
        {
          "gte":"45"
        }
      }}  
      ]
    }
  }
}

GET /hotel/_search
{
  "query":
  {
    "bool":
    {
      "must":
      [
          {"term":{"city":"上海"}}
      ],
      "should":
      [
      {"term":{"brand": "皇冠假日"}},
      {"term":{"brand":"华美达"}}
      ],
      "must_not":
      [
      {"range":{"price":{"lte": 500}}}
      ],
      "filter":
      [
      {"geo_distance":
      {
        "distance":"100km",
        "location": {
          
          "lat": 30.9,
          "lon":121.7
        }
        
      }}  
      ]
    }
  }
}

分页，排序，高亮
GET /hotel/_search
{
  "query":
  {
    "bool":
    {
      "must":
      [
          {"term":{"city":"上海"}}
      ],
      "should":
      [
      {"term":{"brand": "皇冠假日"}},
      {"term":{"brand":"华美达"}}
      ],
      "must_not":
      [
      {"range":{"price":{"lte": 500}}}
      ],
      "filter":
      [
      {"geo_distance":
      {
        "distance":"100km",
        "location": {
          
          "lat": 30.9,
          "lon":121.7
        }
        
      }}  
      ]
    }
  },
  "sort":
  [
    {"score":"desc"},
    {"price":"asc"}
  ],
  "from":1,
  "size":10
  
  }

  深分页： search_after(分页时排序，从上一次排序开始，查询下一页数据) scroll

  高亮
  GET /hotel/_search
{
  "query":
  {
    "bool":
    {
      "must":
      [
          {"term":{"city":"上海"}}
      ],
      "should":
      [
      {"term":{"brand": "皇冠假日"}},
      {"term":{"brand":"华美达"}}
      ],
      "must_not":
      [
      {"range":{"price":{"lte": 500}}}
      ],
      "filter":
      [
      {"geo_distance":
      {
        "distance":"100km",
        "location": {
          
          "lat": 30.9,
          "lon":121.7
        }
        
      }}  
      ]
    }
  },
  "sort":
  [
    {"score":"desc"},
    {"price":"asc"}
  ],
  "from":1,
  "size":10
  , "highlight": {
    "fields":
    {
      "name":
      {
        "pre_tags": "<em>",
        "post_tags": "<em>"
        , "require_field_match": "false"
      }
    }
  }
  }

桶查询,  根据品牌进行分组,terms里填写分组的字段，分组的数据大小，排序依据，分别对应field,size order:{}
外面包裹aggs:{"xxxAgg":{"terms:{}}}
GET /hotel/_search
{1
  "aggs":
  {
    "brandAgg":
    {
      "terms":
      {
        "field":"brand",
        "size": 20
      }
    }
  },
  "size": 0      这个意思是不返回数据
}

bool查询

GET /hotel/_search
{
  "query":
  {
    "bool":
    {
      "must":
      [
       { "range":
        {
          "price":
          {
            "lte": 2000,
            "gte": 20
          }
        }
       }
      ],
      "should" : 
      [
        {
         "geo_bounding_box" :
          {
            "location":
            {
              "top_left":
              {
                "lat": 31.1,
                "lon": 121.5
              },
              "bottom_right":
              {
                "lat":30.9,
                "lon":121.7
              }
            }
          }
          
        }
      ]
    }
  }



管道聚合，再聚合的基础上进行子聚合  
GET /hotel/_search

{
  "size":0,
  "aggs":
  {
    "brandAgg":
    {
      "terms":
      {
        "field":"brand",
        "size": 20,
        "order":
        {
          "statsAgg.avg":"desc"
        }
      },
      "aggs":
      {
        "statsAgg":
        {
          "stats":
          {
            "field":"price"
          }
        }
      }
    }
  }
}

自定义分词器；
PUT /test
{
  "settings":
  {
    "analysis": 
    {
      "analyzer": 
      {
        "my_analyzer":
        {
          "tokenizer":"ik_max_word",
          "filter":"py"
        }
      },
      "filter":
      {
        "py":
        {
          "type":"pinyin",
          "keep_full_pinyin":false,
          "keep_joined_full_pinyin":true,
          "keep_original":true,
          "limit_first_letter_length": 16,
          "remove_duplicated_term":true,
          "none_chinese_pinyin_tokenize":false
        }
      }
    }
    
  },
  
  "mappings":
  {
    "properties": 
    {
     
      "name":
      {
        "type":"text",
        "analyzer": "my_analyzer",
        "search_analyzer": "ik_max_word"
      }
    
     
    }
  }
  
  
}
补全查询，开头就是suggest

//其实就是手动更新,kafka拿到消息后进行更新。

#补全查询
PUT /test2
{
  "mappings":
  {
    "properties":
    {
      "title":
      {
        "type":"completion"
      }
    }
  }
}

POST /test2/_doc
{
  "title": ["Sony","WH-1000XM3"]
}


POST /test2/_doc
{
  "title": ["SK-II","PITERA"]
}

POST /test2/_doc
{
  "title": ["Nintendo","switch"]
}
#text是文本 field只补全的字段，skip直是否跳过重复，size指返回的词条数
GET /test2/_search
{
  "suggest":
  {
    "title_suggest":
    {
      "text":"N",   
      "completion":
      {
        "field":"title",
        "skip_duplicates":true,
        "size":10
      }
    }
  }
}

