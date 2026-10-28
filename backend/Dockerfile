# ============================================================
# 后端镜像：多阶段构建
#
# 为什么分两个阶段？
# 构建阶段要 Maven + JDK + 全部依赖源码（~700MB），但这些在运行阶段
# 一点用都没有。分成两个阶段后，最终镜像里只有 JRE + 一个 jar（~180MB）。
#
# 为什么先 COPY pom.xml 再 COPY src？
# Docker 按层缓存，某一层的输入没变就直接复用。依赖很少变、代码经常变，
# 分开之后改代码只会触发最后两层重建，不用每次重新下载依赖。
# ============================================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:17-jre-alpine

# curl 留给 healthcheck 用；tzdata 让容器里的时间正确，
# 否则日志时间戳和定时任务（浏览量回写）会差 8 小时
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 不用 root 跑应用：容器被拿到 shell 也不能随意改文件
RUN addgroup -S app \
    && adduser -S app -G app \
    && mkdir -p /app/uploads /app/logs \
    && chown -R app:app /app

COPY --from=build --chown=app:app /build/target/*.jar app.jar

USER app

EXPOSE 8080

# MaxRAMPercentage：容器内存有限，让 JVM 按容器可用内存算堆大小，
# 而不是按宿主机内存的 1/4（那会直接把容器撑爆）
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
