USE telco;

delete from userinfo where timestamp < DATE_SUB(NOW() , INTERVAL 1 DAY)
