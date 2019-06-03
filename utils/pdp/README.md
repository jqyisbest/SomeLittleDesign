**描述**

为系统提供缺省变量解析的功能。

目前的实现是解析字符型缺省变量，将字符串中的缺省变量替换为实际值。

字符型缺省变量的格式为：【*identificationString*】【*PredefinedParameterType*】【*partitionSymbol*】【*PredefinedParameter*】【*partitionSymbol*】【附加信息】【*identificationString*】

稍微改改就可以变成任意类型的缺省变量解析。

> 例如应用于规则配置中，每次执行时要获取前一天的日期。

---

**功能**

1. 可以自定义各种想要支持的缺省变量。

---

**缺点**

1. 解析速度和变量所表示的含义复杂度有关。例如解析【昨天的日期】需要0.01秒，解析【距离今天任意偏移量的日期】需要0.1秒左右。

**使用**

1. 编写自定义解析类【A】和解析方法【a】继承*BaseTranslator*。
2. 在枚举*PredefinedParameterType*中添加相应的变量类型和类【A】的全限定名。
3. 在枚举*PredefinedParameter*中添加相应的变量名和方法名【a】。
4. 在配置文件中配置想要的*identificationString*和*partitionSymbol*
5. 在sourceStr中包含所配置格式的缺省变量
6. 调用解析如下

```java
pdpTranslator.translate(sourceStr);
```

**实例**

```java
源字符串：【select * from 'txt$DATE_AND_TIME|ANY_DAY|{d:'[-1,2,1,10,0]',s:'yyyy-MM-dd HH:mm:ss'}$' where date=$DATE_AND_TIME|TODAY|yyyy-MM-dd HH:mm:ss$;】
解析结果：【select * from 'txt2018-08-05 02:14:55' where date=2019-06-03 16:14:55;】
```

