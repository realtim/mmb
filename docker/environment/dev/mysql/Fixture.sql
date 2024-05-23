INSERT INTO Users (user_id, user_name, user_email, user_phone, user_password, user_birthyear, user_prohibitadd, user_sessionfornewpassword, user_sendnewpasswordrequestdt, user_admin, user_hide, user_registerdt, user_lastauthorizationdt, user_sendnewpassworddt, user_allowsendchangeinfo, user_allowsendorgmessages, user_city, user_importkey, user_importattempt, userunionlog_id, user_noshow, user_rank, user_minraidid, user_maxraidid, user_r6, user_noinvitation, user_maxnotstartraidid, user_r6old, user_sex, user_amateur) VALUES (1, 'Админ', 'admin@example.com', '', '827ccb0eea8a706c4c34a16891f84e7b', 1983, 0, null, null, 1, 0, '2024-05-22 14:11:54', '2024-05-22 14:15:51', '2024-05-22 14:15:51', 1, 0, '', null, null, null, 0, null, null, null, null, null, null, null, 2, null);
INSERT INTO Users (user_id, user_name, user_email, user_phone, user_password, user_birthyear, user_prohibitadd, user_sessionfornewpassword, user_sendnewpasswordrequestdt, user_admin, user_hide, user_registerdt, user_lastauthorizationdt, user_sendnewpassworddt, user_allowsendchangeinfo, user_allowsendorgmessages, user_city, user_importkey, user_importattempt, userunionlog_id, user_noshow, user_rank, user_minraidid, user_maxraidid, user_r6, user_noinvitation, user_maxnotstartraidid, user_r6old, user_sex, user_amateur) VALUES (2, 'Иван Иванов', 'ivan@example.com', '', '827ccb0eea8a706c4c34a16891f84e7b', 1983, 0, null, null, 0, 0, '2024-05-22 14:11:54', '2024-05-22 14:15:51', '2024-05-22 14:15:51', 1, 0, '', null, null, null, 0, null, null, null, null, null, null, null, 2, null);

INSERT INTO FileTypes (filetype_id, filetype_name) VALUES (1, 'Положение');
INSERT INTO FileTypes (filetype_id, filetype_name) VALUES (2, 'Логотип');
INSERT INTO FileTypes (filetype_id, filetype_name) VALUES (10, 'Информация о старте');

INSERT INTO PointTypes (pointtype_id, pointtype_name) VALUES (1, 'Старт');
INSERT INTO PointTypes (pointtype_id, pointtype_name) VALUES (2, 'Финиш');
INSERT INTO PointTypes (pointtype_id, pointtype_name) VALUES (3, 'Обязательный КП');
INSERT INTO PointTypes (pointtype_id, pointtype_name) VALUES (4, 'Смена карт');
INSERT INTO PointTypes (pointtype_id, pointtype_name) VALUES (5, 'КП');

INSERT INTO LinkTypes (linktype_id, linktype_name, linktype_hide, linktype_textonly, linktype_order) VALUES (1, 'Текст', 0, 1, 1);
INSERT INTO InvitationDeliveryTypes (invitationdeliverytype_id, invitationdeliverytype_name) VALUES (1, 'Приглашение');
INSERT INTO InvitationDeliveryTypes (invitationdeliverytype_id, invitationdeliverytype_name) VALUES (2, 'Лотерея');
