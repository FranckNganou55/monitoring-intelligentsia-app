-- V7 : Correction du hash BCrypt du compte admin
-- Mot de passe : Admin@1234 (BCrypt cost 12, généré avec BCryptPasswordEncoder)

UPDATE users
SET password_hash = '$2a$12$/Oo9NbYi5kiicW5j7J3uIe63bEaBuyMkK7A8DWomnupZrNDPDcvW2'
WHERE login = 'admin';
