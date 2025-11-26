CREATE DATABASE IF NOT EXISTS ziply_auth;
CREATE DATABASE IF NOT EXISTS ziply_user;
CREATE DATABASE IF NOT EXISTS ziply_review;

CREATE USER IF NOT EXISTS 'ziply_auth'@'%' IDENTIFIED BY 'auth_pw';
CREATE USER IF NOT EXISTS 'ziply_user'@'%' IDENTIFIED BY 'user_pw';
CREATE USER IF NOT EXISTS 'ziply_review'@'%' IDENTIFIED BY 'review_pw';

GRANT ALL PRIVILEGES ON ziply_auth.* TO 'ziply_auth'@'%';
GRANT ALL PRIVILEGES ON ziply_user.* TO 'ziply_user'@'%';
GRANT ALL PRIVILEGES ON ziply_review.* TO 'ziply_review'@'%';

FLUSH PRIVILEGES;