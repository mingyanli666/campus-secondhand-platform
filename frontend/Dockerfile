# ============================================================
# 前端镜像：多阶段构建
#
# 构建阶段用 Node 打包出静态文件，运行阶段只需要 nginx 托管，
# 最终镜像里没有 node_modules、没有源码、没有构建工具。
# ============================================================

# ---------- 阶段一：构建 ----------
FROM node:20-alpine AS build
WORKDIR /build

# 同样先只拷依赖清单：改业务代码不会触发重新 npm ci
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- 阶段二：运行 ----------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80
