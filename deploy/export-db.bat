@echo off
chcp 65001 >nul
REM ============================================================
REM Finding - Windows 本机数据导出脚本
REM 将本地 MySQL 数据库导出为待部署的 SQL 文件
REM ============================================================

echo.
echo ╔══════════════════════════════════════════╗
echo ║   Finding 数据库导出工具                  ║
echo ╚══════════════════════════════════════════╝
echo.

REM 配置 MySQL 连接信息（修改为你的实际配置）
set MYSQL_USER=root
set MYSQL_PASSWORD=123456
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_DB=finding

echo 数据库: %MYSQL_DB%@%MYSQL_HOST%:%MYSQL_PORT%
echo 用户: %MYSQL_USER%
echo.

REM 检查 mysqldump
where mysqldump >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 mysqldump 命令
    echo 请安装 MySQL 或添加 MySQL bin 目录到 PATH
    echo 例如: set PATH=^%PATH^%;C:\Program Files\MySQL\MySQL Server 8.0\bin
    pause
    exit /b 1
)

REM 1. 仅结构（不含数据）→ init.sql（已有则跳过）
echo [1/2] 导出表结构...
mysqldump -u%MYSQL_USER% -p%MYSQL_PASSWORD% -h%MYSQL_HOST% -P%MYSQL_PORT% --no-data --routines --triggers %MYSQL_DB% > init-structure.sql 2>nul
if %errorlevel% neq 0 (
    echo [错误] 导出失败，请检查 MySQL 密码和连接
    pause
    exit /b 1
)
echo        → init-structure.sql (仅结构，不含数据)

REM 2. 完整数据导出（用于迁移）
echo [2/2] 导出完整数据...
mysqldump -u%MYSQL_USER% -p%MYSQL_PASSWORD% -h%MYSQL_HOST% -P%MYSQL_PORT% --complete-insert --skip-extended-insert --routines --triggers %MYSQL_DB% > init-data-full.sql 2>nul
if %errorlevel% neq 0 (
    echo [错误] 导出失败
    pause
    exit /b 1
)
echo        → init-data-full.sql (完整数据，可用于迁移)

echo.
echo ╔══════════════════════════════════════════╗
echo ║   导出完成！                              ║
echo ╠══════════════════════════════════════════╣
echo ║  init-structure.sql  - 仅表结构           ║
echo ║  init-data-full.sql  - 完整数据+结构      ║
echo ║                                          ║
echo ║  部署时用完整数据文件覆盖 init.sql 即可    ║
echo ╚══════════════════════════════════════════╝
echo.
echo 下一步:
echo   1. 将 init-data-full.sql 重命名为 init.sql
echo   2. 或者部署后在服务器上执行:
echo      mysql -uroot -p finding ^< init-data-full.sql
echo.
pause
