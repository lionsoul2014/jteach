## 一、关于 Jteach
jteach是使用java开发的一个小巧，跨平台的教学软件

主要功能：

1. 屏幕广播
2. 屏幕监视 + 控制 + 客户机广播
3. 文件传输
4. 远程命令执行(例如，关机命令)
...


## 二、编译

你需要拷贝 javacp 的 ffmpeg 和 opencv 相关jar包到项目的./lib/目录下，然后在使用 ant 编译:
```
ant all
```
编译完成后会得到 jteach-server-{version}.jar 和 jteach-client-{version}.jar


## 二、运行环境要求

安装了 JDK 的主机。并且客户机能够找到服务器机器（如果是局域网那就对了），使用方法如下：

1. 运行服务器端（教师端）：java -jar jteach-server-{version}.jar
2. 运行客户端（学生端）：输入服务器端的IP，点击 connect 即可
