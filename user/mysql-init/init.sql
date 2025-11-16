-- DB 분리
CREATE DATABASE IF NOT EXISTS ziply_auth;
CREATE DATABASE IF NOT EXISTS ziply_user;

-- 유저 생성
CREATE USER IF NOT EXISTS 'ziply_auth'@'%' IDENTIFIED BY 'auth_pw';
CREATE USER IF NOT EXISTS 'ziply_user'@'%' IDENTIFIED BY 'user_pw';

-- 권한 부여 (각각 자기 DB만)
GRANT ALL PRIVILEGES ON ziply_auth.* TO 'ziply_auth'@'%';
GRANT ALL PRIVILEGES ON ziply_user.* TO 'ziply_user'@'%';

FLUSH PRIVILEGES;
