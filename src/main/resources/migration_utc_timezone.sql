UPDATE user_expiration 
SET expiry_date = DATE_SUB(expiry_date, INTERVAL 8 HOUR)
WHERE expiry_date IS NOT NULL;