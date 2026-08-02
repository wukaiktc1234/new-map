#!/bin/bash
# ============================================================
# 食品溯源系统 - 自动备份脚本
# 功能: 备份数据库、日志、上传文件到异地存储
# 调度: cron - 0 2 * * * /opt/scripts/backup-daily.sh
# ============================================================

set -euo pipefail

# ==================== 配置区 ====================

# 数据库配置
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="food_traceability"
DB_USER="postgres"
DB_PASS="${DATABASE_BACKUP_PASSWORD:-}"

# 备份目录
BACKUP_BASE_DIR="/backup/food-traceability"
BACKUP_DB_DIR="$BACKUP_BASE_DIR/database"
BACKUP_LOG_DIR="$BACKUP_BASE_DIR/logs"
BACKUP_FILE_DIR="$BACKUP_BASE_DIR/uploads"

# 保留天数
RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-180}

# 异地存储配置（S3/OSS）
REMOTE_ENABLED=${REMOTE_BACKUP_ENABLED:-false}
REMOTE_BUCKET="${REMOTE_BUCKET_NAME:-}"
REMOTE_ENDPOINT="${REMOTE_S3_ENDPOINT:-}"
REMOTE_ACCESS_KEY="${REMOTE_ACCESS_KEY:-}"
REMOTE_SECRET_KEY="${REMOTE_SECRET_KEY:-}"

# 通知配置
NOTIFY_EMAIL="${ALERT_EMAIL:-ops@yourdomain.com}"
SLACK_WEBHOOK="${SLACK_WEBHOOK_URL:-}"

# 日志文件
LOG_FILE="/var/log/backup.log"

# ==================== 函数定义 ====================

log() {
    local level=$1
    shift
    local message=$*
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] [$level] $message" | tee -a "$LOG_FILE"
}

notify() {
    local status=$1
    local message=$2
    
    if [ -n "$SLACK_WEBHOOK" ]; then
        curl -s -X POST "$SLACK_WEBHOOK" \
            -H 'Content-type: application/json' \
            -d "{\"text\": \"*备份通知* [$status] $message\"}" > /dev/null 2>&1 || true
    fi
    
    if command -v mail &> /dev/null && [ -n "$NOTIFY_EMAIL" ]; then
        echo "$message" | mail -s "[备份$status] 食品溯源系统" "$NOTIFY_EMAIL" > /dev/null 2>&1 || true
    fi
}

cleanup_old_backups() {
    local dir=$1
    local days=$2
    
    log "INFO" "清理超过 $days 天的旧备份..."
    find "$dir" -type f -mtime +$days -delete 2>/dev/null || true
    log "INFO" "清理完成"
}

upload_to_remote() {
    local file=$1
    local remote_path=$2
    
    if [ "$REMOTE_ENABLED" != "true" ] || [ -z "$REMOTE_BUCKET" ]; then
        return
    fi
    
    log "INFO" "上传到异地存储: $(basename $file)"
    
    if command -v aws &> /dev/null; then
        AWS_ACCESS_KEY_ID="$REMOTE_ACCESS_KEY" \
        AWS_SECRET_ACCESS_KEY="$REMOTE_SECRET_KEY" \
        aws --endpoint-url "$REMOTE_ENDPOINT" s3 cp "$file" "s3://$REMOTE_BUCKET/$remote_path/" || true
    elif command -v ossutil64 &> /dev/null; then
        ossutil64 cp "$file" "oss://$REMOTE_BUCKET/$remote_path/" -f || true
    else
        log "WARN" "未找到AWS CLI或OSS工具，跳过异地上传"
    fi
}

backup_database() {
    local date_str=$(date +%Y%m%d_%H%M%S)
    local backup_file="$BACKUP_DB_DIR/db_$date_str.dump"
    local backup_checksum_file="$BACKUP_DB_DIR/db_$date_str.sha256"
    
    log "INFO" "开始备份数据库..."
    
    mkdir -p "$BACKUP_DB_DIR"
    
    PGPASSWORD="$DB_PASS" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -F c \
        -f "$backup_file" \
        --verbose 2>> "$LOG_FILE"
    
    if [ $? -eq 0 ]; then
        sha256sum "$backup_file" > "$backup_checksum_file"
        local size=$(du -h "$backup_file" | cut -f1)
        log "INFO" "数据库备份成功: $(basename $backup_file), 大小: $size"
        
        upload_to_remote "$backup_file" "database/daily"
        return 0
    else
        log "ERROR" "数据库备份失败!"
        return 1
    fi
}

backup_logs() {
    local date_str=$(date +%Y%m%d_%H%M%S)
    local log_source="/var/log/food-traceability"
    local backup_file="$BACKUP_LOG_DIR/logs_$date_str.tar.gz"
    
    log "INFO" "开始备份日志..."
    
    mkdir -p "$BACKUP_LOG_DIR"
    
    if [ -d "$log_source" ]; then
        tar -czf "$backup_file" -C "$(dirname $log_source)" "$(basename $log_source)" 2>/dev/null || true
        
        if [ -f "$backup_file" ]; then
            local size=$(du -h "$backup_file" | cut -f1)
            log "INFO" "日志备份成功: $(basename $backup_file), 大小: $size"
            
            upload_to_remote "$backup_file" "logs/daily"
        fi
    else
        log "WARN" "日志源目录不存在: $log_source"
    fi
}

backup_uploads() {
    local date_str=$(date +%Y%m%d_%H%M%S)
    local upload_source="/data/uploads"
    local backup_file="$BACKUP_FILE_DIR/uploads_$date_str.tar.gz"
    
    log "INFO" "开始备份上传文件..."
    
    mkdir -p "$BACKUP_FILE_DIR"
    
    if [ -d "$upload_source" ]; then
        tar -czf "$backup_file" -C "$(dirname $upload_source)" "$(basename $upload_source)" 2>/dev/null || true
        
        if [ -f "$backup_file" ]; then
            local size=$(du -h "$backup_file" | cut -f1)
            log "INFO" "上传文件备份成功: $(basename $backup_file), 大小: $size"
            
            upload_to_remote "$backup_file" "uploads/daily"
        fi
    else
        log "WARN" "上传文件源目录不存在: $upload_source"
    fi
}

verify_backup() {
    local file=$1
    
    if [ ! -f "$file" ]; then
        return 1
    fi
    
    local checksum_file="${file}.sha256"
    if [ -f "$checksum_file" ]; then
        cd "$(dirname $file)" && sha256sum -c "$(basename $checksum_file)" > /dev/null 2>&1
        return $?
    fi
    
    return 0
}

# ==================== 主流程 ====================

main() {
    log "INFO" "========== 开始执行备份任务 =========="
    
    START_TIME=$(date +%s)
    
    # 创建备份目录
    mkdir -p "$BACKUP_DB_DIR" "$BACKUP_LOG_DIR" "$BACKUP_FILE_DIR"
    
    # 执行各项备份
    DB_RESULT=0
    backup_database || DB_RESULT=1
    
    backup_logs
    backup_uploads
    
    # 清理旧备份
    cleanup_old_backups "$BACKUP_DB_DIR" $RETENTION_DAYS
    cleanup_old_backups "$BACKUP_LOG_DIR" $RETENTION_DAYS
    cleanup_old_backups "$BACKUP_FILE_DIR" $RETENTION_DAYS
    
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    log "INFO" "========== 备份任务完成，耗时 ${DURATION}s =========="
    
    # 发送结果通知
    if [ $DB_RESULT -eq 0 ]; then
        notify "成功" "每日备份已完成，耗时${DURATION}秒"
    else
        notify "失败" "数据库备份失败，请检查日志: $LOG_FILE"
        exit 1
    fi
}

main "$@"
