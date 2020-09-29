# Pub-Query README

这是一个帮助Flutter/Dart开发者在pubspec.yaml上快速依赖所需要库的Android Studio的插件

## 预览

![](https://github.com/franticn/pub-query/blob/master/previews/preview2.gif?raw=true)

## 使用说明

引入插件后，在`pubspec.yaml`中的`dependencies`和`dev_dependencies`的代码块当中输入要搜索的插件名，并且三个字符以上，会进行搜索，选择对应的包名之后会出现改包对应的所有版本号；

## 已知问题

- 目前暂不支持我们的私有pub,因为其api与公有库不一样
- 目前暂不支持自定义pub地址，默认搜索地址为`https://pub.flutter-io.cn/`

## 后续

- 改造私有pub，支持同时检索私有和公有pub
- 版本号提供`lastStableVersion`标识用于明确当前最新版本