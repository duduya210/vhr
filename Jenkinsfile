pipeline {
    // 当前 Jenkins 构建节点运行在 Windows；deploy.sh 仍通过 SSH 在 Linux 虚拟机执行。
    agent any

    // 名称须与 Jenkins > Tools 中配置的一致。
    tools {
        jdk 'jdk21'
        maven 'maven'
    }

    options {
        // 下面的“拉取代码”阶段会主动执行 git，避免 Jenkins 自动检出一次。
        skipDefaultCheckout(true)
        // 防止两次部署同时停止、重建同名容器。
        disableConcurrentBuilds()
    }

    stages {
        stage('拉取代码') {
            steps {
                // 将 fea_1 分支检出到 Jenkins 工作空间；github 是 Jenkins 中的凭证 ID。
                git branch: 'fea_1',
                    credentialsId: 'github',
                    url: 'https://github.com/duduya210/vhr.git'
            }
        }

        stage('前端打包') {
            steps {
                // Windows 构建节点须已安装 Node.js 和 npm；Vite 8 需要兼容的 Node.js 版本。
                dir('vuehr') {
                    bat 'node --version && npm.cmd --version'
                    // 按 package-lock.json 安装依赖，再把 Vue 页面构建到 vuehr/dist。
                    bat 'npm.cmd ci'
                    bat 'npm.cmd run build'
                }
                // 将 dist 里面的内容打包，解压后 index.html 应位于目标目录根部。
                bat '''
                    if not exist "release\\frontend" mkdir "release\\frontend"
                    tar.exe -C "vuehr\\dist" -czf "release\\frontend\\frontend.tar.gz" .
                '''
                archiveArtifacts artifacts: 'release/frontend/frontend.tar.gz', fingerprint: true
            }
        }

        stage('Maven 打包') {
            steps {
                // 先确认构建节点实际使用的 JDK 和 Maven 版本。
                bat 'java -version && mvn.cmd -version'
                // 从多模块父 POM 打包；此处跳过测试编译和执行。
                bat 'mvn.cmd -B -ntp -f vhr/pom.xml -Dmaven.test.skip=true clean package'
            }
            post {
                success {
                    // 保存两个可运行 JAR 为 Jenkins 构建产物；归档本身不会上传到虚拟机。
                    archiveArtifacts artifacts: 'vhr/vhrserver/vhr-web/target/vhr-web-0.0.1-SNAPSHOT.jar', fingerprint: true
                    archiveArtifacts artifacts: 'vhr/mailserver/target/mailserver-0.0.1-SNAPSHOT.jar', fingerprint: true
                }
            }
        }

        stage('准备上传文件') {
            steps {
                // bat 在 Windows 构建节点执行：把 JAR 复制并重命名为 Dockerfile 中 COPY 使用的名字。
                bat '''
                    if not exist "release\\vhrserver" mkdir "release\\vhrserver"
                    if not exist "release\\mailserver" mkdir "release\\mailserver"
                    copy /Y "vhr\\vhrserver\\vhr-web\\target\\vhr-web-0.0.1-SNAPSHOT.jar" "release\\vhrserver\\vhrserver.jar" >NUL || exit /b 1
                    copy /Y "vhr\\mailserver\\target\\mailserver-0.0.1-SNAPSHOT.jar" "release\\mailserver\\mailserver.jar" >NUL || exit /b 1
                '''

                // writeFile 也在 Jenkins 构建节点执行，只写出文件，不执行脚本。
                // 后续通过 SSH 上传 deploy.sh，再由虚拟机上的 sh 执行。
                // 使用三个单引号，让脚本中的 $变量留给远端 Shell 展开。
                writeFile file: 'release/deploy.sh', text: '''
# 从这里开始是虚拟机上执行的 deploy.sh 内容。
# -e：任一命令失败就退出；-u：使用未定义变量时退出。
set -eu

# 第一个参数由后面的 execCommand 传入：vhr 部署主服务，mail 部署邮件服务。
case "$1" in
    vhr)
        directory=/root/vhrserver
        container=vhr
        image=vhrserver:jenkins
        jar=vhrserver.jar
        port=8081
        ;;
    mail)
        directory=/root/mailserver
        container=mail
        image=mailserver:jenkins
        jar=mailserver.jar
        port=8082
        ;;
    *)
        echo '用法: sh deploy.sh vhr|mail' >&2
        exit 2
        ;;
esac

cd "$directory"
# Dockerfile 和 JAR 必须已在对应目录；mail 的 Dockerfile 还需要预先上传的 JDK 压缩包。
# test -f 检查失败时，set -e 会让部署立即停止。
test -f Dockerfile
test -f "$jar"
if [ "$container" = mail ]; then
    test -f jdk-21.tar.gz
fi

# 先构建新镜像，再处理旧容器；构建失败时旧容器继续运行。
docker build -t "$image" .
old_image=
if docker inspect "$container" >/dev/null 2>&1; then
    # 已有容器：记下其镜像 ID（用于回滚）和网络（供新容器沿用）。
    old_image=$(docker inspect -f '{{.Image}}' "$container")
    networks=$(docker inspect -f '{{range $name, $_ := .NetworkSettings.Networks}}{{println $name}}{{end}}' "$container")
    test -n "$networks"
    primary_network=$(echo "$networks" | sed -n '1p')
else
    # 首次部署：没有旧容器，也没有可回滚的旧镜像。
    if [ "$container" = vhr ]; then
        # Nginx 配置通过 vhr:8081 代理；两个容器须加入同一个用户自定义网络。
        docker inspect nginx >/dev/null 2>&1 || { echo '找不到 nginx 容器' >&2; exit 1; }
        if ! docker network inspect vhr-net >/dev/null 2>&1; then
            docker network create vhr-net
        fi
        if ! docker inspect -f '{{range $name, $_ := .NetworkSettings.Networks}}{{println $name}}{{end}}' nginx | grep -qx vhr-net; then
            docker network connect vhr-net nginx
        fi
        primary_network=vhr-net
    else
        # 邮件服务无需与 Nginx 通信，首次部署使用 Docker 默认 bridge 网络。
        primary_network=bridge
    fi
    networks=$primary_network
fi

rollback() {
    # 退出前失败时调用；先关闭退出钩子，避免回滚失败时再次调用自己。
    trap - 0
    docker rm -f "$container" >/dev/null 2>&1 || true
    if [ -n "$old_image" ]; then
        # 更新部署失败：用记录的旧镜像重新创建同名容器，并恢复其网络连接。
        echo "$container 部署失败，尝试恢复旧容器" >&2
        docker run -d --name "$container" --restart unless-stopped --network "$primary_network" -p "$port:$port" "$old_image"
        for network in $networks; do
            if [ "$network" != "$primary_network" ]; then
                docker network connect "$network" "$container"
            fi
        done
        if [ "$container" = vhr ]; then
            docker exec nginx nginx -s reload || true
        fi
    else
        # 首次部署失败：删除失败的新容器，但没有旧容器可恢复。
        echo "$container 首次部署失败，没有旧容器可恢复" >&2
    fi
}

if [ -n "$old_image" ]; then
    # 只有更新部署才需要停止并删除旧容器；首次部署直接跳过。
    docker stop "$container"
    docker rm "$container"
fi
# 后续运行、连网、检查或 Nginx 重载失败时，退出钩子调用 rollback。
trap 'status=$?; if [ "$status" -ne 0 ]; then rollback; fi' 0
# -d 后台运行；--restart unless-stopped 使容器在 Docker 重启后自动恢复。
docker run -d --name "$container" --restart unless-stopped --network "$primary_network" -p "$port:$port" "$image"
# 原容器若连接了多个网络，这里把其余网络也接回去。
for network in $networks; do
    if [ "$network" != "$primary_network" ]; then
        docker network connect "$network" "$container"
    fi
done

# 最多检查 5 次，每次间隔 2 秒；未就绪则输出最近 80 行容器日志并触发回滚。
ready=0
attempt=0
while [ "$attempt" -lt 30 ]; do
    if [ "$container" = vhr ]; then
        # 这里只检查进程在运行且 HTTP 有响应；401/500 仍算“已响应”，不代表接口功能正常。
        response=$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:8081/verifyCode || true)
        if [ "$(docker inspect -f '{{.State.Running}}' vhr)" = true ] &&
           [ -n "$response" ] && [ "$response" != 000 ]; then
            ready=1
            break
        fi
    else
        # 邮件服务没有用于此处的 HTTP 接口，检查容器运行状态和 Spring Boot 启动日志。
        if [ "$(docker inspect -f '{{.State.Running}}' mail)" = true ] &&
           docker logs mail 2>&1 | grep -q 'Started MailserverApplication'; then
            ready=1
            break
        fi
    fi
    attempt=$((attempt + 1))
    sleep 2
done

if [ "$ready" -ne 1 ]; then
    docker logs --tail 80 "$container"
    exit 1
fi

# Nginx 的 proxy_pass 使用容器名 vhr，重建后让它重新解析地址。
if [ "$container" = vhr ]; then
    docker exec nginx nginx -t
    docker exec nginx nginx -s reload
fi
# 部署成功，取消退出时的回滚钩子。
trap - 0
'''

                // 将同一份部署脚本分别放进两个上传目录，和各自的 JAR 一起上传。
                bat '''
                    copy /Y "release\\deploy.sh" "release\\vhrserver\\deploy.sh" >NUL || exit /b 1
                    copy /Y "release\\deploy.sh" "release\\mailserver\\deploy.sh" >NUL || exit /b 1
                '''

                // 这份脚本在虚拟机执行：只向 Nginx 的宿主机挂载目录发布前端静态文件。
                writeFile file: 'release/frontend/deploy-frontend.sh', text: '''
set -eu

archive=/root/frontend/frontend.tar.gz
webroot=/data/nginx/html

test -f "$archive"
test -d "$webroot"
docker inspect nginx >/dev/null 2>&1 || { echo '找不到 nginx 容器' >&2; exit 1; }

# 先确认宿主机目录确实挂载到了 Nginx 的网页目录，避免文件上传成功却访问不到。
mount_source=$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/usr/share/nginx/html"}}{{.Source}}{{end}}{{end}}' nginx)
if [ "$mount_source" != "$webroot" ]; then
    echo "Nginx 网页目录的宿主机挂载路径是 $mount_source，预期是 $webroot；请先检查 docker inspect nginx" >&2
    exit 1
fi

# 先发布 JS、CSS、图片，再替换 index.html，避免页面引用尚未上传的新资源。
# 保留旧的带哈希文件，让已打开网页的浏览器仍能加载原资源。
tar -tzf "$archive" >/dev/null
tar --exclude='./index.html' -xzf "$archive" -C "$webroot"
temporary_index=$(mktemp "$webroot/.index.html.XXXXXX")
trap 'rm -f "$temporary_index"' 0
tar -xOzf "$archive" ./index.html > "$temporary_index"
chmod 644 "$temporary_index"
mv -f "$temporary_index" "$webroot/index.html"
trap - 0
echo '前端静态文件已发布到 /data/nginx/html'
'''
            }
        }

        stage('部署 vhr-web') {
            steps {
                // Publish Over SSH 中 configName 为 root 的远程基础目录应配置为 /root。
                // 上传到 /root/vhrserver，然后在虚拟机执行 deploy.sh vhr。
                sshPublisher(failOnError: true, publishers: [
                    sshPublisherDesc(
                        configName: 'root',
                        transfers: [sshTransfer(
                            sourceFiles: 'release/vhrserver/*',
                            removePrefix: 'release/vhrserver',
                            remoteDirectory: 'vhrserver',
                            cleanRemote: false,
                            execTimeout: 600000,
                            execCommand: 'sh /root/vhrserver/deploy.sh vhr'
                        )]
                    )
                ])
            }
        }

        stage('部署 mailserver') {
            steps {
                // 上传到 /root/mailserver，然后在虚拟机执行 deploy.sh mail。
                sshPublisher(failOnError: true, publishers: [
                    sshPublisherDesc(
                        configName: 'root',
                        transfers: [sshTransfer(
                            sourceFiles: 'release/mailserver/*',
                            removePrefix: 'release/mailserver',
                            remoteDirectory: 'mailserver',
                            cleanRemote: false,
                            execTimeout: 600000,
                            execCommand: 'sh /root/mailserver/deploy.sh mail'
                        )]
                    )
                ])
            }
        }

        stage('部署前端') {
            steps {
                // Publish Over SSH 的远程基础目录同样应为 /root。
                // 上传 tar 包和脚本到 /root/frontend，脚本再将 dist 内容发布到 /data/nginx/html。
                sshPublisher(failOnError: true, publishers: [
                    sshPublisherDesc(
                        configName: 'root',
                        transfers: [sshTransfer(
                            sourceFiles: 'release/frontend/*',
                            removePrefix: 'release/frontend',
                            remoteDirectory: 'frontend',
                            cleanRemote: false,
                            execTimeout: 600000,
                            execCommand: 'sh /root/frontend/deploy-frontend.sh'
                        )]
                    )
                ])
            }
        }
    }

    post {
        success {
            echo 'vhr-web、mailserver 容器和前端静态文件已部署。'
        }
        failure {
            echo '流水线失败，请查看失败阶段的日志。'
        }
    }
}
