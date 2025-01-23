INSERT INTO users (name,
                   login_id,
                   password, role) VALUES
                                       ('Lisa', 'user1', 'i0nt(FAreJFXDmGD', 'ROLE_USER'),
                                       ('Virginia', 'user2', 'SbqLq0XYW?laFfSA', 'ROLE_USER'),
                                       ('Lea', 'user3', '9iTf>O7V8gpxXVTq3', 'ROLE_USER'),
                                       ('Deborah', 'user4', 'wa5stuDff3NPw{zQ', 'ROLE_USER'),
                                       ('Jennifer', 'user5', '2YDGaAOn1JJLUkx', 'ROLE_USER'),
                                       ('Julie', 'user6', 'Jb7y5ur4>Snmv1uJ', 'ROLE_USER'),
                                       ('Garrett', 'user7', '}Xo45jub9BjM8vu3', 'ROLE_USER'),
                                       ('Melissa', 'user8', '15zLe>o0PKEan1s', 'ROLE_USER'),
                                       ('James', 'user9', 'AOT69mWb_ufZIurJ', 'ROLE_USER'),
                                       ('Stephen', 'user10', 'viBsTxrRiLdI2f)', 'ROLE_USER');

INSERT INTO account (user_id,
                     password,
                     account_number,
                     money,
                     account_name,
                     created_at,
                     currency) VALUES
                                   (1, 'i0nt(FAreJFXDmGD', '8591138-891036980', 10000000, '급여통장', NOW(), 'KRW'),
                                   (2, 'SbqLq0XYW?laFfSA', '1423039-881703740', 500000, '저축통장', NOW(), 'USD'),
                                   (3, '9iTf>O7V8gpxXVTq3', '0550776-465073841', 2000000, '관리자통장', NOW(), 'KRW'),
                                   (4, 'wa5stuDff3NPw{zQ', '5372862-170154457', 80000, '급여통장', NOW(), 'EUR'),
                                   (5, '2YDGaAOn1JJLUkx', '9276951-449251744', 45000000, '관리자통장', NOW(), 'KRW'),
                                   (6, 'Jb7y5ur4>Snmv1uJ', '3400334-889646753', 34000000, '관리자통장', NOW(), 'KRW'),
                                   (7, '}Xo45jub9BjM8vu3', '4340069-064146812', 10000, '저축통장', NOW(), 'USD'),
                                   (8, '15zLe>o0PKEan1s', '9553044-843629147', 2300000, '급여통장', NOW(), 'KRW'),
                                   (9, 'AOT69mWb_ufZIurJ', '4906421-433228703', 100000, '관리자통장', NOW(), 'EUR'),
                                   (10, 'viBsTxrRiLdI2f)', '6749750-841603518', 7200000, '급여통장', NOW(), 'KRW');

