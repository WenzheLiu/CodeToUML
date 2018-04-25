该工具能够分析源代码，生成UML图。

# 简介

提供2种运行方式：

（1）命令行

（2）桌面GUI

另外正在开发第3种运行方式：Web，基于Angular + Material Design + Spring Boot.

支持多种操作系统，如Windows，Linux等，无需安装，无需管理员权限。

支持2种编程语言：Java，Scala。

支持两种UML图：

（1）类图（命令行和GUI都支持，对被分析的代码无要求，非侵入性，无需要求编译）

（2）时序图（暂不提供GUI，只能用命令行方式，需要侵入被分析代码，需要特殊编译）

UML输出的文件格式：

（1）PlantUML文本，以.uml结尾。

（2）PNG图片

（3）最推荐的是SVG格式，矢量图，可以用Web浏览器打开，支持搜索，任意放大，tooltip。

# 安装

（1）解压graphviz-2.38.zip

（2）设置环境变量：GRAPHVIZ_DOT = C:\Users\weliu\code\sbt\graphviz-2.38\release\bin\dot.exe

（3）要求运行机器安装JRE8或更高版本。

# 运行方式

命令行模式不多介绍，有意使用可以联系我。这里只介绍GUI方式：Windows下双击codetouml.jar即可，命令行可以输入java –jar codetouml.jar

如果想要任意指定文件或者文件夹继续分析生成一个类图，可以创建Standalone Class Diagram。

 ![alt text](https://github.com/WenzheLiu/CodeToUML/blob/master/doc/standalone.png)

如果想要为一个产品，对应每个工程每个包都生成一系列类图，可以创建 Product Class Diagram。

 ![alt text](https://github.com/WenzheLiu/CodeToUML/blob/master/doc/product.png)

可以对生成的类图做多种定制，比如当我们只对类A感兴趣，只想分析以类A相关为中心，距离为1的类图，可以选择Center Class，这样可以只显示相关度高的类.

CodeToUML对我很有帮助，让我能快速理解别人的代码和设计，值得推荐。

# 原理

生成UML类图的原理如下图所示：

 ![alt text](https://github.com/WenzheLiu/CodeToUML/blob/master/doc/class.png)

生成UML时序图的原理如下图所示：

 ![alt text](https://github.com/WenzheLiu/CodeToUML/blob/master/doc/sequence.png)


# 代码设计上的特点

关于CodeToUML的源代码，如果大家有兴趣的话可以阅读源码，这里简单列出了一些特别有意思的地方：

1. 严格贯彻“以不变应万变”的思想，整个程序没有使用过一个变量，一切都是常量，所有的类都是不可变类，所有的函数都是纯函数，全都是线程安全的。（JavaFX GUI部分除外）

2. 宏观（设计）层面：面向接口编程的思想，类间的耦合基本上都是接口，而不是依赖于具体实现类。用到一些面向对象设计模式：如单例模式、组合模式、策略模式、代理模式、工厂模式等。

3. 微观（实现）层面：函数式编程的思想，流式风格，除了大量使用标准库提供的filter、map、reduce、模式匹配等函数式操作符、也自定义了一些操作符，如mapIf、还有支持偏函数的map。用到的一些函数式模式：如filter-map-reduce模式、操作链模式、模式匹配、Lazy模式等。

4. DSL风格，由于采用Scala编写，很多地方都尽量写得像自然语言，如下面的代码： extendedTypes map (_.getNameAsString) filterNot isSkipClass filterNot isUmlClass

5. 面向表达式编程，整个程序几乎没有使用过一个return语句，一切都是表达式，不仅包括代码语句、也包括常量定义、方法定义等。

6. 无Null设计，所有表达式、变量、方法都不会返回null，无需做null check，没有空指针异常问题。

7. JavaFX GUI部分，所有的静态UI组件都采用fxml编写，与Controller代码相分离，采用数据绑定和可观察对象进行数据交互；动态界面则采用DSL风格提供，比如下面代码动态创建一个tab控件并将其text属性与自定义的controller的tabText属性进行单向数据绑定（只要controller的tabText属性的值更新了text的值也会自动更新），设置tab的图片，绑定tooltip，设置内容控件等等。
```
val tab = new Tab {
  text <== controller.tabText      // 这里 <== 符合代表数据绑定的方向是从右到左，而 ==>是从左到右，双向绑定为 <==>，这种操作符远比bind方法直观
  graphic = new sfxi.ImageView {
    image = new sfxi.Image(getClass getResourceAsStream iconPath)
  }
  tooltip = new sfxc.Tooltip {
    text <== controller.tabTooltip
  }
  content = root    // 这里作为Tab内容的root是一个静态UI组件，在fxml文件定义
  userData = controller
}
```
DSL风格比传统JavaFX代码要写一堆tab.setXXX()方法要简洁得多。

# 输出结果展示

这里作为demo，让CodeToUML工具做自我分析，分析自己源代码中关键部分产生的类图，由于整个软件代码量非常多，有上百个类，生成的UML图太过复杂，不便于贴图。因此，我们这里只演示其中的Java语法解析器部分的类图，如下图所示：

 ![alt text](https://github.com/WenzheLiu/CodeToUML/blob/master/doc/self_class.png)

用这个工具分析它自己是如何解析类间的关系（在源码中，调用ClassRelationFactory对象的createRelations方法），这个过程所产生的时序图如下所示：

 ![alt text](https://github.com/WenzheLiu/CodeToUML/blob/master/doc/self_seq.png)

希望大家能喜欢！
