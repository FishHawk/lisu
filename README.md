# 栗鼠

>深挖洞，广积粮，做一只快乐的栗鼠。

栗鼠是一个免费的漫画库管理系统。你可以浏览各种来源里的漫画，将感兴趣的添加到库中。栗鼠会自动把漫画内容同步到你的磁盘上。

这个仓库包含栗鼠后端服务器的代码。

- Server：https://github.com/FishHawk/lisu（你在这）
- Android：https://github.com/FishHawk/lisu-android

## 快速开始

你可以从[发布页](https://github.com/fishhawk/lisu/releases)下载可执行 jar 文件，默认的库目录为当前文件夹。要求 java 版本至少为 java11。

你也可以使用 docker 运行：

```shell
docker build https://github.com/FishHawk/lisu.git#main -t lisu:latest
docker image prune -f
docker run -d -p 8080:8080 -v <your-library-path>:/data --name=lisu --restart=always lisu:latest
```

启动后使用浏览器访问 http://localhost:8080/ ，如果显示 `Yes, the server is running!`，说明服务器正常运行。

## 特征

- 基于系统目录管理漫画，你可以在文件管理器中直接编辑漫画库。
- 支持三种漫画文件结构：
  - 三级目录：Manga -> Collection -> Chapter
  - 二级目录：Collection -> Chapter
  - 一级目录：Chapter
- 提供其他漫画源，支持在线阅读和订阅。漫画源列表：
  - [x] 哔哩哔哩漫画
  - [x] 动漫之家
  - [x] 漫画人
  - [ ] e-hentai
  - [ ] exhentai
