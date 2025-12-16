-- 1. Auth 모듈
CREATE DATABASE IF NOT EXISTS ziply_auth;
CREATE USER IF NOT EXISTS 'ziply_auth'@'%' IDENTIFIED BY 'auth_pw';
GRANT ALL PRIVILEGES ON ziply_auth.* TO 'ziply_auth'@'%';

-- 2. User 모듈
CREATE DATABASE IF NOT EXISTS ziply_user;
CREATE USER IF NOT EXISTS 'ziply_user'@'%' IDENTIFIED BY 'user_pw';
GRANT ALL PRIVILEGES ON ziply_user.* TO 'ziply_user'@'%';

-- 3. Review 모듈
CREATE DATABASE IF NOT EXISTS ziply_review;
CREATE USER IF NOT EXISTS 'ziply_review'@'%' IDENTIFIED BY 'review_pw';
GRANT ALL PRIVILEGES ON ziply_review.* TO 'ziply_review'@'%';

-- 4. Analysis 모듈
CREATE DATABASE IF NOT EXISTS ziply_analysis;
CREATE USER IF NOT EXISTS 'ziply_analysis'@'%' IDENTIFIED BY 'analysis_pw';
GRANT ALL PRIVILEGES ON ziply_analysis.* TO 'ziply_analysis'@'%';

FLUSH PRIVILEGES;