#!/bin/bash
# 启动开发环境服务

# 设置环境变量
export SPRING_PROFILES_ACTIVE=dev
export JAVA_HOME=P:/my-new-project/JDK21
export PATH=$JAVA_HOME/bin:$PATH

# 启动应用
java -jar target/food-traceability-1.0.0.jar