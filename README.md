静态扫描+动态监测，识别代码性能瓶颈

启动服务：
java -jar codeperf-demo/target/codeperf-demo.jar

静态扫描：
java -jar codeperf-cli/target/codeperf-cli.jar  scan  --target-package com.codeperf.demo --classes-dir codeperf-demo/target/classes  --output perf-static.json  --report perf-static.html

动态采集：
java -jar codeperf-cli/target/codeperf-cli.jar attach --target-package com.codeperf.demo  --entry "POST /api/orders/report"  --output ./perf-data.raw  --report ./perf-report.html  --wait 120 --pid 9768  

触发请求：
curl.exe -X POST http://localhost:8080/api/orders/report

合并报告：
java -jar codeperf-cli/target/codeperf-cli.jar report --input perf-data.raw  --static perf-static.json  --output perf-report.html  --fail-on warn
  echo "report 退出码=$?"
