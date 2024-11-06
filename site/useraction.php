<?php

/**
 * +++++++++++ Обработчик действий, связанных с пользователем +++++++++++++++++
 */

function UACanEdit($pUserId)
{
    global $UserId, $Administrator;

    return (($pUserId == $UserId) || $Administrator) ? (1) : (0);
}

function UACanLinkEdit($pUserId, $raidId, $userId)
{
    $Admin = CSql::userAdmin($userId);
    $RaidModerator = CSql::userModerator($userId, $raidId);

    return (($pUserId == $userId) || $Admin || $RaidModerator) ? (1) : (0);
}

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) {
    return;
}

// 03/04/2014 Добавил значения по умолчанию, чтобы подсказки в полях были не только при добавлении,
// но и при правке, если не были заполнены поля при добавлении
$UserCityPlaceHolder = 'Город';
$UserPhonePlaceHolder = 'Телефон';

if ($action == "") {
    // Действие не указано
    $view = "MainPage";
    // $statustext = "Сотрудник: ".$employeename.", табельный номер: ".$tabnum ;
} elseif ($action === "UserLogin") {
    // обработка регистрации
    if ($ReadOnly == 1) {
        CMmb::setErrorMessage('В связи техническими работами сайт доступен только в режиме чтения.');
        return;
    }

    // первичная проверка данных
    $Login = trim(mmb_validate($_POST, 'Login'));
    $Password = trim(mmb_validate($_POST, 'Password'));
    if ($Login === '') {
        CMmb::setErrorMessage('Не указан e-mail.');
        return;
    }

    if ($Password === '') {
        CMmb::setErrorMessage('Не указан пароль.');
        return;
    }
    // конец первичной проверки входных данных

    $UserId = CMmbAuth::getUserId($Login, $Password);
    CMmbLogger::i('Login', "'$Login';" . $_SERVER['REMOTE_ADDR'] . ";$UserId");
    if ($UserId <= 0) {
        //.$login." не найден!";
        //		CSql::closeConnection();
        CMmb::setErrorMessage('Неверный email или пароль.');
        return;
    }

    //Конец проверки пользователя и пароля
    $SessionId = StartSession($UserId);

    // Если есть открытый марш-бросок, то открываем список команд, а не список всех ММБ
    if (isset($RaidId) and CSql::raidStage($RaidId) < 7 and CSql::raidStage($RaidId) > 0) {
        CMmb::setViews('ViewRaidTeams', '');
    } else {
        CMmb::setViews('MainPage', '');
    }
    //$statustext = "ua Пользователь: ".$UserId.", сессия: ".$SessionId;


} elseif ($action === "UserInfo") {
    // Действие вызывается ссылкой под имененм пользователя
    CMmb::setViews('ViewUserData', '');
} elseif ($action === "ViewNewUserForm") {
    // Действие вызывается ссылкой Новый пользователь
    if ($ReadOnly === 1) {
        CMmb::setErrorMessage('В связи техническими работами сайт доступен только в режиме чтения.');
        return;
    }

    CMmb::setViews('ViewUserData', 'Add');
} elseif ($action === "UserChangeData" || $action === "AddUser") {
    // Действие вызывается либо при регистрации нового пользователя либо при сменен данных старого
    $view = "ViewUserData";

    $pUserEmail = trim(mmb_validate($_POST, 'UserEmail'));
    $pUserName = trim(mmb_validate($_POST, 'UserName'));
    $pUserCity = trim(mmb_validate($_POST, 'UserCity'));
    $pUserPhone = trim(mmb_validate($_POST, 'UserPhone'));
    $pUserBirthYear = mmb_validateInt($_POST, 'UserBirthYear');
    $pUserProhibitAdd = mmb_isOn($_POST, 'UserProhibitAdd');
    $pUserId = mmb_validateInt($_POST, 'UserId', -1);
    $pUserSex = mmb_validateInt($_POST, 'UserSex', 0);


    // флаги разрешения получать письма передаем только при правке (см. ниже)


    $pUserNewPassword = trim(mmb_validate($_POST, 'UserNewPassword', ''));
    $pUserConfirmNewPassword = trim(mmb_validate($_POST, 'UserConfirmNewPassword', ''));

    if ($pUserCity == $UserCityPlaceHolder) {
        $pUserCity = '';
    }
    if ($pUserPhone == $UserPhonePlaceHolder) {
        $pUserPhone = '';
    }

    // 03/07/2014  Скрываем ФИО
    $pUserNoShow = mmb_isOn($_POST, 'UserNoShow');

    if ($action === 'AddUser') {
        // Новый пользователь
        $pUserId = 0;
        $viewmode = "Add";
    } else {
        $viewmode = "";
    }

    if ($pUserEmail == '') {
        CMmb::setErrorSm('Не указан e-mail.');
        return;
    }

    if ($pUserName == '' || trim($pUserName) === 'Фамилия Имя' || (trim($pUserName) == $Anonimus && !$pUserNoShow)) {
        CMmb::setErrorSm('Не указано ФИО.');
        return;
    }

    if ($pUserBirthYear < 1930 || $pUserBirthYear > date("Y")) {
        CMmb::setErrorSm('Год не указан или указан некорректный.');
        return;
    }


    if (($pUserNewPassword <> '' || $pUserConfirmNewPassword <> '') and $pUserNewPassword <> $pUserConfirmNewPassword) {
        CMmb::setErrorSm('Не совпадает новый пароль и его подтверждение.');
        return;
    }


    if ($pUserNewPassword <> '') {
        $err = CMmbAuth::isValidPassword($pUserNewPassword);
        if ($err !== true) {
            CMmb::setErrorSm($err);
            return;
        }
    }

    // Проверяем, что нет активной учетной записи с таким e-mail
    $sql = sprintf(
        "select count(*) as resultcount from  Users where COALESCE(user_password, '') <> '' and trim(user_email) = '%s' and user_id <> %s",
        $pUserEmail,
        $pUserId

    );

    if (CSql::singleValue($sql, 'resultcount') > 0) {
        CMmb::setErrorSm('Уже есть пользователь с таким email.');
        return;
    }

    $sql = "select count(*) as resultcount
               from  Users 
           where  trim(user_name) = '$pUserName'
                  and user_birthyear = $pUserBirthYear
              and user_id <> $pUserId
              and userunionlog_id is null";

    if (CSql::singleValue($sql, 'resultcount') > 0) {
        CMmb::setErrorSm('Уже есть пользователь с таким именем и годом рождения.');
        return;
    }

    // Если есть неактивная учетная запись - высылаем на почту ссылку с активацией
    $sql = "select user_id from  Users where COALESCE(user_password, '') = '' and trim(user_email) = '$pUserEmail' and user_id <> $pUserId";

    $Row = CSql::singleRow($sql);
    if ($Row['user_id'] > 0) {
        if ($action === 'AddUser') {
            $ChangePasswordSessionId = uniqid('', true);
            $sql = "update  Users set  user_sessionfornewpassword = '$ChangePasswordSessionId', user_sendnewpasswordrequestdt = now()
                 where user_id = {$Row['user_id']}";

            MySqlQuery($sql);

            // Решил не писать здесь имя - м.б. и в адресе не надо
            $Msg = "Здравствуйте!\r\n\r\n";
            $Msg = $Msg . "Кто-то (возможно, это были Вы) пытается зарегистрировать учетную запись на сайте ММБ, связанную с этим адресом e-mail.\r\n";
            $Msg = $Msg . "Запись помечена, как неактивная, поэтому повторно высылается ссылка для активации:\r\n";
            $Msg = $Msg . "Для активации пользователя и получения пароля необходимо перейти по ссылке:\r\n";
            $Msg = $Msg . $MyHttpLink . $MyLocation . "?changepasswordsessionid=$ChangePasswordSessionId\r\n\r\n";
            $Msg = $Msg . "Учетные записи без активации могут быть удалены.\r\n";
            //$Msg =  $Msg."P.S. Если Вас зарегистрировали без Вашего желания - просто проигнорируйте письмо - приносим извинения за доставленные неудобства."."\r\n";

            // Отправляем письмо
            SendMail($pUserEmail, $Msg, $pUserName);

            CMmb::setShortResult(
                'Повторная ссылка для активации пользователя и получения пароля выслана на указанный адрес.
                          Если письмо не пришло - проверьте спам. Учетные записи без активации могут быть удалены.',
                'MainPage'
            );
            return;
        } else {
            CMmb::setErrorSm('Уже есть пользователь с таким email.');
            return;
        }
    }

    if ($action == 'AddUser') {
        // Новый пользователь

        // Для более жёсткой проверки можно делать дополнительную активацию:
        // не генерировать сразу пароль, а высылать на почту запрос
        // Потребуется, видимо, ещё дополнительное поле (например, время отправки запроса), чтобы удалять неактивные записи

        // Создаём пароь
        // $NewPassword = GeneratePassword(6);

        // Пароль выводим на экран, высылаем по почте и пишем его хэш в базу

        $ChangePasswordSessionId = uniqid();

        // записываем нового пользователя
        // пароль пустой, сессия для смены пароля и время отправки запроса

        $sql = "insert into  Users (user_email, user_name, user_birthyear, user_password, user_registerdt,
                                     user_sessionfornewpassword, user_sendnewpasswordrequestdt, 
                         user_prohibitadd, user_city, user_phone, user_noshow, user_sex)
                             values ('$pUserEmail', '$pUserName', $pUserBirthYear, '', now(),
                             '$ChangePasswordSessionId', now(),
                          $pUserProhibitAdd, '$pUserCity', '$pUserPhone', $pUserNoShow, $pUserSex)";

        // При insert должен вернуться последний id - это реализовано в  MySqlQuery
        $newUserId = MySqlQuery($sql);

        if ($newUserId <= 0) {
            CMmb::setErrorSm('Ошибка записи нового пользователя.');
            return;
        } else {
            // Решил не писать здесь имя - м.б. и в адресе не надо
            $Msg = "Здравствуйте!\r\n\r\n";
            $Msg = $Msg . "Кто-то (возможно, это были Вы) зарегистрировал учетную запись на сайте ММБ, связанную с этим адресом e-mail.\r\n";
            $Msg = $Msg . "Для активации пользователя и получения пароля необходимо перейти по ссылке:\r\n";
            $Msg = $Msg . $MyHttpLink . $MyLocation . "?changepasswordsessionid=$ChangePasswordSessionId\r\n\r\n";
            $Msg = $Msg . "Учетные записи без активации могут быть удалены.\r\n";
            $Msg = $Msg . "P.S. Если Вас зарегистрировали без Вашего желания - просто проигнорируйте письмо - приносим извинения за доставленные неудобства.\r\n";

            // Отправляем письмо
            SendMail($pUserEmail, $Msg, $pUserName);

            CMmb::setShortResult(
                'Ссылка для активации пользователя и получения пароля выслана на указанный адрес.
                          Если письмо не пришло - проверьте спам. Учетные записи без активации могут быть удалены.',
                'MainPage'
            );
        }
        // Конец обработки нового пользователя

    } elseif ($action === 'UserChangeData') {
        // Правка текущего пользователя

        // флаги подписки передаем только при правке
        // пока убрал обработку флага ChangeInf

        // $pUserAllowChangeInfo = mmb_isOn($_POST, 'UserAllowChangeInfo');
        $pUserAllowOrgMessages = mmb_isOn($_POST, 'UserAllowOrgMessages');

        // Если вызвали с таким действием, должны быть определены оба пользователя
        if ($pUserId <= 0 || $UserId <= 0) {
            return;
        }

        // Права на редактирование
        if (!UACanEdit($pUserId)) {
            return;
        }              // выходим

        // 03/07/2014  Добавляем признак анонимности (скрывать ФИО)
        // user_allowsendchangeinfo = $pUserAllowChangeInfo,

        $sql = "update  Users set   user_email = '$pUserEmail',
                             user_name = '$pUserName',
                             user_city = '$pUserCity',
                             user_phone = '$pUserPhone',
                             user_prohibitadd = $pUserProhibitAdd,
                             user_allowsendorgmessages = $pUserAllowOrgMessages,
                             user_noshow = $pUserNoShow,
                             user_sex = $pUserSex,
                             user_birthyear = $pUserBirthYear
                where user_id = $pUserId";
        $rs = MySqlQuery($sql);

        // Обновление пароля делаем только, когда просят
        if ($pUserNewPassword <> '' and $pUserConfirmNewPassword <> '' and $pUserNewPassword == $pUserConfirmNewPassword) {
            $err = CMmbAuth::setPassword($pUserId, $pUserNewPassword);
            if ($err !== true) {
                CMmb::setErrorSm($err);
                return;
            }

            $statustext = 'Сохранён новый пароль.';
        }

        // Формируем сообщение
        $ChangeDataUserName = CSql::userName($UserId);

        $Msg = "Уважаемый пользователь $pUserName!\r\n\r\n";
        $Msg .= "В Вашей учетной записи произошли изменения - их можно увидеть в карточке пользователя.\r\n";
        $Msg .= "Автор изменений: $ChangeDataUserName\r\n\r\n";
        $Msg .= "P.S. Изменения можете вносить Вы, а также администратор сайта ММБ.";

        // Отправляем письмо
        SendMail($pUserEmail, $Msg, $pUserName);
        // Конец сохранений изменений текущего пользователя
    } else {
        // других вариантов не должно быть
        return;
    }
    // Конец добавления нового или сохранений изменений текущего пользователя


} elseif ($action === "SendEmailWithNewPassword") {
    // Действие вызывается ссылкой из формы просмотра данных пользователя

    $view = "ViewUserData";

    $pUserId = mmb_validateInt($_POST, 'UserId');

    // Если вызвали с таким действием, должны быть определены оба пользователя
    if ($pUserId <= 0 || $UserId <= 0) {
        return;
    }

    // Права на редактирование
    if (!UACanEdit($pUserId)) {
        return;
    }              // выходим

    $row = CSql::fullUser($pUserId);
    $UserEmail = $row['user_email'];
    $UserName = $row['user_name'];

    $NewPassword = CMmbAuth::setAutoPassword($pUserId);

    CMmb::setShortResult("Пароль $NewPassword выслан.", '');

    $ChangeDataUserName = CSql::userName($UserId);

    $Msg = "Уважаемый пользователь $UserName!\r\n\r\n";
    $Msg .= "У Вашей учетной записи изменён пароль: $NewPassword\r\n";
    $Msg .= "Автор изменений: $ChangeDataUserName.\r\n\r\n";
    $Msg .= "P.S. Изменения можете вносить Вы, а также администратор сайта ММБ.";

    // Отправляем письмо
    SendMail($UserEmail, $Msg, $UserName);
} elseif ($action === "RestorePasswordRequest") {
    // Действие вызывается ссылкой "Забыли пароль"

    if ($ReadOnly == 1) {
        CMmb::setErrorMessage('В связи техническими работами сайт доступен только в режиме чтения.');
        return;
    }

    $view = "";

    $pUserEmail = trim(mmb_validate($_POST, 'Login'));

    if ($pUserEmail == '' || $pUserEmail === 'E-mail') {
        CMmb::setErrorMessage('Не указан e-mail.');
        return;
    }

    $sql = "select user_id, user_name
                from  Users 
                where user_hide = 0 and user_email = '$pUserEmail'";

    $pUserId = CSql::singleValue($sql, 'user_id', false);
    if ($pUserId <= 0) {
        CMmb::setErrorMessage("Пользователь с  e-mail $pUserEmail не найден");
        return;
    }
    $pUserName = CSql::singleValue($sql, 'user_name');

    $ChangePasswordSessionId = uniqid();

    // пишем в базу сессию для восстановления пароля
    $sql = "update   Users  set user_sessionfornewpassword = '$ChangePasswordSessionId',
                                   user_sendnewpasswordrequestdt = now()
               where user_id = $pUserId";

    CMmbLogger::d('User token', $sql);

    $rs = MySqlQuery($sql);

    $Msg = "Здравствуйте!\r\n\r\n";
    $Msg .= "Кто-то (возможно, это были Вы) запросил восстановление пароля на сайте ММБ для этого адреса e-mail.\r\n";
    $Msg .= "Для получения нового пароля необходимо перейти по ссылке:\r\n";
    $Msg .= $MyHttpLink . $MyLocation . "?changepasswordsessionid=$ChangePasswordSessionId \r\n\r\n";
    $Msg .= "P.S. Если Вы не запрашивали восстановление пароля, то просто проигнорируйте это письмо.\r\n";

    SendMail($pUserEmail, $Msg, $pUserName);
    $statustext = 'Ссылка для получения нового пароля выслана на указанный адрес. Если письмо не пришло - проверьте спам.';
} elseif ($action === "sendpasswordafterrequest") {
    // Действие вызывается из письма переходом по ссылке
    $view = "";

    $changepasswordsessionid = mmb_validate($_REQUEST, 'changepasswordsessionid', '');
    if (empty($changepasswordsessionid)) {
        CMmb::setShortResult("Пустой идентификатор запроса пароля.", 'MainPage');

        $action = "";
        return;
    }

    $sql = "select user_id, user_email, user_name from  Users where user_sessionfornewpassword = '$changepasswordsessionid'";

    CMmbLogger::d('Find user by token', $sql);

    $Row = CSql::singleRow($sql);
    $UserId = $Row['user_id'];
    $UserEmail = $Row['user_email'];
    $UserName = $Row['user_name'];

    // Если идентификаторы совпали - меняем пароль
    // Возможно здесь стоит сразу стартовать сессию...
    if ($UserId > 0) {
        $NewPassword = CMmbAuth::setAutoPassword($UserId);

        $Msg = "Уважаемый пользователь $UserName!\r\n\r\n";
        $Msg .= "Согласно подтверждённому запросу с Вашего адреса e-mail,\r\n";
        $Msg .= "для Вашей учетной записи на сайте ММБ создан пароль: $NewPassword\r\n";

        // Отправляем письмо
        SendMail($UserEmail, $Msg, $UserName);

        CMmb::setShortResult("Пароль выслан.", 'MainPage');

        // и вот тут м.б. стоит активировать сессию, чтобы автоматом войти на сайт
        $SessionId = StartSession($UserId);
    } else {
        CMmb::setShortResult("Не найден пользователь для идентификатора $changepasswordsessionid, возможно вы запросили пароль дважды и испльзуете ссылку не из последнего письма.", 'MainPage');
    }

    $changepasswordsessionid = "";
    $action = "";
} elseif ($action == "UserLogout") {
    // Выход

    CloseSession($SessionId, 3);
    $SessionId = "";
    $action = "";
    $view = "MainPage";
} elseif ($action == "CancelChangeUserData") {
    // Действие вызывается ссылкой Отмена

    CMmb::setViews('ViewUserData', '');
} elseif ($action === "FindUser") {
    // Действие вызывается поиском участника

    $FindString = trim(mmb_validate($_POST, 'FindString', ''));
    if ($FindString == '' || $FindString == 'Часть ФИО') {
        CMmb::setShortResult('Не указан критерий поиска.', '');
        return;
    }

    if ($FindString === 'все-все' || $FindString === 'все-все-все') {
        $sqlFindString = '';
    } else {
        $connectionId = CSql::getConnection();
        $sqlFindString = mysqli_real_escape_string($connectionId, $FindString);
    }

    $sql = "select count(*) as FindUsersCount
                from  Users u
            where ltrim(COALESCE(u.user_password, '')) <> '' 
                              and u.user_hide = 0 
                  and COALESCE(u.user_noshow, 0) = 0
                              and user_name like '%$sqlFindString%'";

    if (CSql::singleValue($sql, 'FindUsersCount') > 0) {
        $view = "ViewUsers";
    } else {
        CMmb::setShortResult('Не найдено пользователей, чьи ФИО содержат ' . $FindString, '');
    }
} elseif ($action === "MakeModerator") {
    // Действие вызывается нажатием кнопки "Сделать модератором"

    $pUserId = mmb_validateInt($_POST, 'UserId');

    // Если вызвали с таким действием, должны быть определны оба пользователя
    if ($pUserId <= 0 || $UserId <= 0) {
        return;
    }

    // Права на редактирование
    if (!$Administrator) {
        return;
    }

    $Sql = "select raidmoderator_id,  raidmoderator_hide
                        from RaidModerators 
                    where raid_id = $RaidId
                          and user_id = $pUserId
                    LIMIT 0,1 ";

    $Row = CSql::singleRow($Sql);
    $RaidModeratorId = $Row['raidmoderator_id'];
    $RaidModeratorHide = $Row['raidmoderator_hide'];

    $ModeratorAdd = 0;

    if (empty($RaidModeratorId)) {
        $Sql = "insert into RaidModerators (raid_id, user_id, raidmoderator_hide) values ($RaidId, $pUserId, 0)";
        MySqlQuery($Sql);
        $ModeratorAdd = 1;
    } else {
        if ($RaidModeratorHide == 0) {
            $ModeratorAdd = 0;
        } else {
            // Есть и модератор скрыт -  обновляем
            $Sql = "update RaidModerators set raidmoderator_hide = 0 where raidmoderator_id = $RaidModeratorId";
            MySqlQuery($Sql);
            $ModeratorAdd = 1;
        }
        // Конец проверки существующей записи

    }
    // Конец разбора ситуации с модераторами

    if ($ModeratorAdd) {
        $ChangeDataUserName = CSql::userName($UserId);

        $Row = CSql::fullUser($pUserId);
        $pUserName = $Row['user_name'];
        $pUserEmail = $Row['user_email'];

        $Sql = "select raid_name from  Raids where raid_id = $RaidId";
        $RaidName = CSql::singleValue($Sql, 'raid_name');

        $Msg = "Уважаемый пользователь $pUserName!\r\n\r\n";
        $Msg .= "Вы получили статус модератора марш-броска $RaidName\r\n";
        $Msg .= "Автор изменений: $ChangeDataUserName.\r\n\r\n";

        // Отправляем письмо
        SendMail($pUserEmail, $Msg, $pUserName);

        CMmb::setResult('Добавлен модератор', 'ViewAdminModeratorsPage');
    } else {
        CMmb::setResult('Пользователь уже имеет статус модератора!', 'ViewUserData');
    }
} elseif ($action === "HideModerator") {
    // Действие вызывается нажатием кнопки "Удалить" на странице со списком модераторов

    $RaidModeratorId = mmb_validateInt($_POST, 'RaidModeratorId', -1);
    $pUserId = mmb_validateInt($_POST, 'UserId', -1);

    // Если вызвали с таким действием, должны быть определны оба пользователя
    if ($RaidModeratorId <= 0 || !$Administrator) {
        return;
    }

    $Sql = "update RaidModerators set raidmoderator_hide = 1 where raidmoderator_id = $RaidModeratorId";
    MySqlQuery($Sql);

    $ChangeDataUserName = CSql::userName($UserId);
    $Row = CSql::fullUser($pUserId);
    $pUserName = $Row['user_name'];
    $pUserEmail = $Row['user_email'];

    $Sql = "select raid_name from  Raids where raid_id = $RaidId";
    $RaidName = CSql::singleValue($Sql, 'raid_name');

    $Msg = "Уважаемый пользователь $pUserName!\r\n\r\n";
    $Msg .= "Вы потеряли статус модератора марш-броска $RaidName.\r\n";
    $Msg .= "Автор изменений: $ChangeDataUserName.\r\n\r\n";

    // Отправляем письмо
    SendMail($pUserEmail, $Msg, $pUserName);

    // Остаемся на той же странице
    CMmb::setResult('Удален модератор', 'ViewAdminModeratorsPage');
} elseif ($action === "MakeDeveloper") {
    // Действие вызывается нажатием кнопки "Сделать волонтёром"

    $pUserId = mmb_validateInt($_POST, 'UserId');

    // Если вызвали с таким действием, должны быть определны оба пользователя
    if ($pUserId <= 0 || $UserId <= 0) {
        return;
    }

    // Права на редактирование
    if (!$Administrator) {
        return;
    }

    $Sql = "select tu.teamuser_id
                        from TeamUsers tu 
                                inner join Teams t
                                on tu.team_id = t.team_id
                                inner join Distances d
                                on t.distance_id = d.distance_id
                    where d.raid_id = $RaidId
                            and t.team_hide = 0
                            and tu.teamuser_hide = 0
                            and tu.user_id = $pUserId
                    LIMIT 0,1 ";

    $Row = CSql::singleRow($Sql);
    $teamuserId = $Row['teamuser_id'];

    if ($teamuserId > 0) {
        CMmb::setErrorMessage('Пользователь уже является участником');
        return;
    }

    $Sql = "select raiddeveloper_id,  raiddeveloper_hide
                        from RaidDevelopers 
                    where raid_id = $RaidId
                          and user_id = $pUserId
                    LIMIT 0,1 ";

    $Row = CSql::singleRow($Sql);
    $RaidDeveloperId = $Row['raiddeveloper_id'];
    $RaidDeveloperHide = $Row['raiddeveloper_hide'];

    $DeveloperAdd = 0;

    if (empty($RaidDeveloperId)) {
        $Sql = "insert into RaidDevelopers (raid_id, user_id, raiddeveloper_hide) values ($RaidId, $pUserId, 0)";
        MySqlQuery($Sql);
        $DeveloperAdd = 1;
    } else {
        if ($RaidDeveloperHide == 0) {
            $DeveloperAdd = 0;
        } else {
            // Есть и модератор скрыт -  обновляем
            $Sql = "update RaidDevelopers set raiddeveloper_hide = 0 where raiddeveloper_id = $RaidDeveloperId";
            MySqlQuery($Sql);
            $DeveloperAdd = 1;
        }
        // Конец проверки существующей записи

    }
    // Конец разбора ситуации с модераторами

    if ($DeveloperAdd) {
        $ChangeDataUserName = CSql::userName($UserId);

        $Row = CSql::fullUser($pUserId);
        $pUserName = $Row['user_name'];
        $pUserEmail = $Row['user_email'];


        $Sql = "select raid_name from  Raids where raid_id = $RaidId";
        $RaidName = CSql::singleValue($Sql, 'raid_name');

        $Msg = "Уважаемый пользователь $pUserName!\r\n\r\n";
        $Msg .= "Вы добавлены в волонтёры марш-броска $RaidName\r\n";
        $Msg .= "Автор изменений: $ChangeDataUserName.\r\n\r\n";


        // Отправляем письмо
        SendMail($pUserEmail, $Msg, $pUserName);

        CMmb::setResult('Добавлен волонтёр', 'ViewRaidDevelopersPage');
    } else {
        CMmb::setResult('Пользователь уже включен в волонтёры!', 'ViewUserData');
    }
} elseif ($action === "HideDeveloper") {
    // Действие вызывается нажатием кнопки "Удалить" на странице со списком волонтёров

    $RaidDeveloperId = mmb_validateInt($_POST, 'RaidDeveloperId', -1);
    $pUserId = mmb_validateInt($_POST, 'UserId', -1);

    // Если вызвали с таким действием, должны быть определены оба пользователя
    if ($RaidDeveloperId <= 0 || !$Administrator) {
        return;
    }

    $Sql = "update RaidDevelopers set raiddeveloper_hide = 1 where raiddeveloper_id = $RaidDeveloperId";
    MySqlQuery($Sql);

    $ChangeDataUserName = CSql::userName($UserId);
    $Row = CSql::fullUser($pUserId);
    $pUserName = $Row['user_name'];
    $pUserEmail = $Row['user_email'];

    $Sql = "select raid_name from  Raids where raid_id = $RaidId";
    $RaidName = CSql::singleValue($Sql, 'raid_name');

    $Msg = "Уважаемый пользователь $pUserName!\r\n\r\n";
    $Msg .= "Вы исключены из списка волонтёров марш-броска $RaidName.\r\n";
    $Msg .= "Автор изменений: $ChangeDataUserName.\r\n\r\n";

    // Отправляем письмо
    SendMail($pUserEmail, $Msg, $pUserName);

    // Остаемся на той же странице
    CMmb::setResult('Удален волонтёр', 'ViewRaidDevelopersPage');
} // ============ Обратимое удаление пользователя ====================================
elseif ($action == 'HideUser') {
    $pUserId = mmb_validateInt($_POST, 'UserId', -1);

    if ($pUserId <= 0) {
        CMmb::setErrorMessage('Пользователь не найден');
        return;
    }

    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    // Проверка возможности удалить пользователя
    if (!$Administrator) {
        CMmb::setErrorMessage('Удаление пользователя запрещено');
        return;
    }

    /*
            if ($pUserId == $UserId) {
                CMmb::setErrorMessage('Нельзя удалить самого себя');
                return;
            }
    */

    // Проверяем, что пользователя нет ни в одной команде
    $sql = " select tu.teamuser_id
             from TeamUsers tu 
             inner join Teams t
             on tu.team_id = t.team_id     
             where tu.teamuser_hide = 0 
                   and t.team_hide = 0
                       and tu.user_id = $pUserId";

    if (CSql::rowCount($sql) > 0) {
        CMmb::setErrorMessage('Пользователь уже является участником по крайней мере одной команды');
        return;
    }

    $pUserName = CSql::userName($pUserId);

    $sql = "update Users set user_hide = 1 where user_id = $pUserId";
    $rs = MySqlQuery($sql);

    // Права на редактирование
    if (!UACanEdit($pUserId)) {
        return;
    }              // выходим

    CMmb::setShortResult("Пользователь $pUserName ключ $pUserId удален ", 'ViewRaidTeams');
} // ============ Добавление устройства ====================================
elseif ($action == 'AddDevice') {
    $pUserId = mmb_validateInt($_POST, 'UserId', -1);

    if ($pUserId <= 0) {
        CMmb::setErrorMessage('Пользователь не найден');
        return;
    }

    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    // Права на редактирование
    if (!UACanEdit($pUserId)) {
        return;
    }              // выходим

    $pNewDeviceName = trim(mmb_validate($_POST, 'NewDeviceName'));
    if (empty($pNewDeviceName) || $pNewDeviceName === 'Название нового устройства') {
        $alert = 1;
        CMmb::setResult('Не указано название устройства', 'ViewUserData');
        return;
    }

    // Прверяем, что нет устройства с таким именем
    $sql = "select count(*) as resultcount from  Devices where trim(device_name) = '$pNewDeviceName'";

    if (CSql::singleValue($sql, 'resultcount') > 0) {
        CMmb::setErrorSm('Уже есть устройство с таким именем.');
        return;
    }

    $Sql = "insert into Devices (device_name, user_id) values ('$pNewDeviceName', $pUserId)";
    MySqlQuery($Sql);

    CMmb::setResult('Добавлено устройство', 'ViewUserData');
} elseif ($action === 'GetDeviceId') {
    // ============ Получение конфигурации ====================================
    $pUserId = (int)mmb_validateInt($_POST, 'UserId', -1);
    $pDeviceId = (int)mmb_validateInt($_POST, 'DeviceId', -1);

    if ($pUserId <= 0) {
        CMmb::setErrorMessage('Пользователь не найден');
        return;
    }

    if ($pDeviceId <= 0) {
        CMmb::setErrorMessage('Устройство не найден');
        return;
    }

    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    // Права на редактирование
    if (!UACanEdit($pUserId)) {
        return;
    }              // выходим

    // Проверяем, что есть устройство для пользователя
    $sql = "select count(*) as resultcount from  Devices where user_id  = $pUserId and device_id = $pDeviceId";

    if (CSql::singleValue($sql, 'resultcount') <> 1) {
        CMmb::setErrorSm('Нет устройства.');
        return;
    }

    // Сбор данных для конфигурации
    $data = [];

    // Raids: raid_id, raid_name, raid_registrationenddate
    $Sql = "select d.device_id, d.device_name, d.user_id, u.user_name, u.user_password from Devices d inner join Users u on d.user_id = u.user_id  where d.device_id = $pDeviceId";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Devices"][] = $Row;
    }
    mysqli_free_result($Result);

    // Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
    header("Content-Type: application/octet-stream");
    header("Content-Disposition: attachment; filename=\"device.json\"");

    // Вывод json
    print json_encode($data);

    // Можно не прерывать, но тогда нужно написать обработчик в index, чтобы не выводить дальше ничего
    die();
    return;
} // ============ Отправка сообщения другому пользователю ====================================
elseif ($action == "SendMessage") {
    // 

    CMmb::setViews('ViewUserData', '');

    $pUserId = mmb_validateInt($_POST, 'UserId', -1);
    $pText = $_POST['MessageText'];
    $pSendMessageCopyToAuthor = mmb_isOn($_POST, 'SendMessageCopyToAuthor');

    if (empty($pText) || trim($pText) == 'Текст сообщения') {
        CMmb::setError('Укажите текст сообщения.', $view, '');
        return;
    }

    // Если вызвали с таким действием, должны быть определены оба пользователя
    if ($pUserId <= 0 || $UserId <= 0) {
        return;
    }

    $row = CSql::fullUser($pUserId);
    $UserEmail = $row['user_email'];
    $UserName = $row['user_name'];

    if ($pSendMessageCopyToAuthor == 1) {
        $row = CSql::fullUser($UserId);
        $AuthorUserEmail = $row['user_email'];
        $AuthorUserName = $row['user_name'];
    }

    CMmb::setShortResult('Сообщение выслано.', '');

    $SendMessageUserName = CSql::userName($UserId);

    $pTextArr = explode('\r\n', $pText);

    $Msg = "Уважаемый пользователь $UserName!\r\n"
        . "Через сайт ММБ пользователь $SendMessageUserName отправил Вам следующее сообщение:\r\n\r\n";

    foreach ($pTextArr as $NowString) {
        $Msg .= $NowString . "\r\n";
    }

    $Msg .= "\r\nДля ответа необходимо авторизоваться и открыть карточку пользователя $SendMessageUserName\r\n"
        . $MyHttpLink . $MyLocation . "?UserId=$UserId\r\n";

    // Отправляем письмо
    SendMail($UserEmail, $Msg, $UserName);

    $LogMsg = "Usermessage was sent to $pUserId";

    $Sql = "insert into Logs (logs_level, user_id, logs_message) values ('info', $UserId, '$LogMsg')";

    MySqlQuery($Sql);


    // Отправляем копию
    if (!empty($AuthorUserEmail)) {
        $Msg = "Копия письма, которое Вами было отправлено\r\n ================ \r\n" . $Msg;
        SendMail($AuthorUserEmail, $Msg, $UserName);
    }
} // ============ Добавить пользователя в слияние ====================================

elseif ($action == "AddUserInUnion") {
    // Действие вызывается нажатием кнопки "Запросить слияние"

    if ($UserId <= 0) {
        CMmb::setErrorMessage('Пользователь не найден');
        return;
    }
    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    $pUserId = mmb_validateInt($_POST, 'UserId', -1);

    if ($UserId == $pUserId) {
        CMmb::setErrorMessage('Нельзя сделать слияние с самим собой');
        return;
    }


    // Проверяем, что пользователя нет в слиянии
    $sql = " select userunionlog_id
                 from UserUnionLogs 
             where union_status <> 0
                   and union_status <> 3
                   and user_parentid = $pUserId";

    if (CSql::rowCount($sql) > 0) {
        CMmb::setResult('Пользователь уже есть в слиянии', 'ViewUserUnionPage', '');
        $viewsubmode = "ReturnAfterError";

        return;
    }


    // Проверяем, что пользователь не скрыт
    // здесь можно ещё проверить, что пользователь импортирован или любое другое условие
    $sql = " select user_id 
             from Users 
         where user_hide = 0 
               and user_id = $pUserId";

    if (CSql::rowCount($sql) <= 0) {
        CMmb::setResult('Пользователь скрыт', 'ViewAdminUnionPage');
        $viewsubmode = "ReturnAfterError";

        return;
    }

    $UnionRequestId = 0;
    $Sql = "insert into UserUnionLogs (user_id, userunionlog_dt, 
                 user_parentid, union_status)
              values ($UserId, now(), $pUserId,  1)";
    $UnionRequestId = MySqlQuery($Sql);

    if ($UnionRequestId) {
        $Row = CSql::fullUser($pUserId);
        $pUserName = $Row['user_name'];
        $pUserEmail = $Row['user_email'];
        $Import = $Row['user_importattempt'];

        // Проверяем, что пользовтельский email не является автогенерированным
        if (substr(trim($pUserEmail), -7) <> '@mmb.ru' && !empty($pUserName)) {
            $pRequestUserName = CSql::userName($UserId);

            $Msg = "Уважаемый пользователь $pUserName!\r\n\r\n"
                . "Сделан запрос на слияние Вас с пользователем $pRequestUserName\r\n"
                . "После подтверждения запроса администраторм сервиса, все ваши участия в командах буду перенесены на пользователя, который запросил слияние, а Ваша учетная запись скрыта" . "\r\n"
                . "Если Вы считаете это неправильным, необходимо авторизоваться на сервисе ММБ, перейти на страницу 'Запросы на слияние' и отклонить запрос." . "\r\n\r\n";

            // Отправляем письмо
            SendMail($pUserEmail, $Msg, $pUserName);
        }
        // Конец проверки, что пользователь не импортирован
    }

    // Конец проверки на успешное добавление запроса
    CMmb::setResult('Создан запрос на слияние пользователей', 'ViewUserUnionPage', '');
} elseif ($action == "RejectUnion") {
    // Действие вызывается нажатием кнопки "Отклонить"

    $UserUnionLogId = mmb_validateInt($_POST, 'UserUnionLogId', -1);

    if (!CanRejectUserUnion($Administrator, $UserUnionLogId, $UserId)) {
        CMmb::setErrorMessage('Нет прав на отклонение запроса');
        return;
    }

    // Просто ставим статус в журнале - ничего больше делать не надол
    $sql = " update UserUnionLogs set union_status = 0 
             where userunionlog_id = $UserUnionLogId";

    MySqlQuery($sql);

    CMmb::setViews('ViewUserUnionPage', '');
} elseif ($action === "ApproveUnion") {
    $UserUnionLogId = mmb_validateInt($_POST, 'UserUnionLogId', -1);
    if (!CanApproveUserUnion($Administrator, $UserUnionLogId, $UserId)) {
        CMmb::setErrorMessage('Нет прав на подтверждение запроса');
        return;
    }


    $Sql = "select user_id, user_parentid  from  UserUnionLogs where userunionlog_id = $UserUnionLogId";
    $Row = CSql::singleRow($Sql);
    $pUserId = $Row['user_id'];
    $pUserParentId = $Row['user_parentid'];

    // Перебрасываем ссылки, ставим признак скрытия пользователя

    // Скрываем старого пользователя
    // Ключ журнала нужен исключительно для возможности потом переименовать пользователя - сделан уникальный ключ, который не допускает одинаковое ФИО и год, но теперь я туда добавил ещё поле userunionlog_id
    // Тонкость в том, что при отмене объединения надо проверять, что пользователь не совпадает, иначе будет ошибка ключа
    $sql = " update Users set user_hide = 1, userunionlog_id = $UserUnionLogId
         where user_id = $pUserParentId";

    MySqlQuery($sql);

    // Меняем ссылку в командах
    $sql = " update TeamUsers set user_id = $pUserId, userunionlog_id = $UserUnionLogId
         where user_id = $pUserParentId";

    MySqlQuery($sql);

    // Меняем статус в журнале
    $sql = " update UserUnionLogs set union_status = 2 
             where userunionlog_id = $UserUnionLogId";

    MySqlQuery($sql);

    CMmb::setViews('ViewUserUnionPage', '');
} elseif ($action === "RollBackUnion") {
    $UserUnionLogId = mmb_validateInt($_POST, 'UserUnionLogId', -1);

    if (!CanRollBackUserUnion($Administrator, $UserUnionLogId, $UserId)) {
        CMmb::setErrorMessage('Нет прав на отмену слияния');
        return;
    }

    $Sql = "select user_id, user_parentid  from  UserUnionLogs where userunionlog_id = $UserUnionLogId";
    $Row = CSql::singleRow($Sql);
    $pUserId = $Row['user_id'];
    $pUserParentId = $Row['user_parentid'];


    // Проверяем что новый пользователь не успел переименоваться в старого
    $UserName = CSql::userName($pUserId);
    $ParentUserName = CSql::userName($pUserParentId);

    // если успел - нового переименовываем
    if (trim($UserName) == trim($ParentUserName)) {
        $sql = " update Users set user_name =  '" . trim($UserName) . "_$UserUnionLogId'
                 where user_id = $pUserId";

        MySqlQuery($sql);
    }

    // Перебрасываем ссылки, ставим признак скрытия пользователя

    // Скрываем старого пользователя
    // Ключ журнала нужен исключительно для возможности потом переименовать пользователя - сделан уникальный ключ, который не допускает одинаковое ФИО и год, но теперь я туда добавил ещё поле userunionlog_id
    // Тонкость в том, что при отмене объединения надо проверять, что пользователь не совпадает, иначе будет ошибка ключа
    $sql = " update Users set user_hide = 0, userunionlog_id = NULL 
            where userunionlog_id = $UserUnionLogId";

    MySqlQuery($sql);

    // Меняем ссылку в командах
    $sql = " update TeamUsers set user_id = $pUserParentId, userunionlog_id = NULL
         where userunionlog_id = $UserUnionLogId";

    MySqlQuery($sql);

    // Меняем статус в журнале
    $sql = " update UserUnionLogs set union_status = 3 
             where userunionlog_id = $UserUnionLogId";

    MySqlQuery($sql);
    CMmb::setViews('ViewUserUnionPage', '');
} elseif ($action === 'AddLink') {
    // ============ Добавление впечатления ====================================
    $pUserId = mmb_validateInt($_POST, 'UserId');
    if ($pUserId <= 0) {
        CMmb::setErrorMessage('Пользователь не найден');
        return;
    }

    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    $pLinkName = trim(mmb_validate($_POST, 'NewLinkName'));
    $pLinkUrl = trim(mmb_validate($_POST, 'NewLinkUrl'));
    $pLinkTypeId = mmb_validateInt($_POST, 'LinkTypeId');
    $pLinkRaidId = mmb_validateInt($_POST, 'LinkRaidId');

    if ($pLinkRaidId <= 0) {
        CMmb::setErrorMessage('ММБ не найден');
        return;
    }

    if (empty($pLinkUrl) || $pLinkUrl === 'Адрес ссылки на впечатление') {
        $alert = 1;
        CMmb::setResult('Не указан адрес ссылки', 'ViewUserData');
        return;
    }


    if (empty($pLinkTypeId)) {
        $alert = 1;
        CMmb::setResult('Не указан тип ссылки', 'ViewUserData');
        return;
    }

    if ($pLinkName === 'Название (можно не заполнять)') {
        $pLinkName = '';
    }

    $userId = CSql::userId($SessionId);

    // Права на редактирование
    if (!UACanLinkEdit($pUserId, $pLinkRaidId, $userId)) {
        return;
    }              // выходим

    // Проверяем, что нет ссылки с таким адресом
    $sql = "select count(*) as resultcount 
               from  UserLinks 
               where trim(userlink_url) = '$pLinkUrl'
                 and linktype_id = $pLinkTypeId
                 and raid_id = $pLinkRaidId
                 and user_id = $pUserId
                 and userlink_hide = 0 ";

    if (CSql::singleValue($sql, 'resultcount') > 0) {
        CMmb::setErrorSm('Уже есть впечатление с таким адресом.');
        return;
    }

    // Проверяем, что не более трёх ссылок на ММБ
    $sql = "select count(*) as resultcount 
               from  UserLinks 
           where raid_id = $pLinkRaidId
             and userlink_hide = 0
             and user_id = $pUserId";

    if (CSql::singleValue($sql, 'resultcount') >= 4) {
        CMmb::setErrorSm('Уже есть 4 впечатления на этот ММБ.');
        return;
    }

    $Sql = "insert into UserLinks (userlink_name, userlink_url, linktype_id, userlink_hide, raid_id, user_id) 
                 values ('$pLinkName', '$pLinkUrl', $pLinkTypeId, 0, $pLinkRaidId, $pUserId)";

    MySqlQuery($Sql);

    CMmb::setResult('Добавлено новое впечатление', 'ViewUserData', "");
} elseif ($action === 'DelLink') {
    // ============ Удаление впечатления ====================================
    $pUserId = mmb_validateInt($_POST, 'UserId');

    if ($pUserId <= 0) {
        CMmb::setErrorMessage('Пользователь не найден');
        return;
    }

    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    $pUserLinkId = mmb_validateInt($_POST, 'UserLinkId');
    if ($pUserLinkId <= 0) {
        CMmb::setErrorMessage('Ссылка не найдена');
        return;
    }

    $sql = "select raid_id 
               from  UserLinks 
               where userlink_id = $pUserLinkId";

    $raidId = CSql::singleValue($sql, 'raid_id');
    $userId = CSql::userId($SessionId);

    // Права на редактирование
    if (!UACanLinkEdit($pUserId, $raidId, $userId)) {
        return;
    }              // выходим

    $Sql = "update  UserLinks set userlink_hide = 1 where userlink_id = $pUserLinkId";

    MySqlQuery($Sql);

    CMmb::setResult('Впечатление удалено', "ViewUserData", "");
} elseif ($action === "SendInvitation") {
    // Действие вызывается нажатием кнопки "Выдать приглашение"

    $pUserId = mmb_validateInt($_POST, 'UserId');

    if ($pUserId <= 0 || $UserId <= 0 || !CRights::canDeliveryInvitation($UserId, $RaidId, 1)) {
        CMmb::setErrorMessage('Не хватает прав или нет доступных приглашений');
        return;
    }

    // вставляем запись о раздаче

    // Находим дату окончания регистрации
    $sql = "select ADDTIME(r.raid_registrationenddate, '23:59:59') as invenddt
                from Raids r
                where  r.raid_id = $RaidId
                    and r.raid_registrationenddate is not null
            ";
    $invEndDt = CSql::singleValue($sql, 'invenddt', false);

    if (empty($invEndDt)) {
        CMmb::setErrorSm('Не определена дата окончания действия приглашения.');
        return;
    }

    // смотрим максимальную дату приглашений по рейтингу
    // если нашли, то ставим её, а не дату окончания ММБ
    $sql = "select MAX(inv.invitation_enddt) as maxinvdt
                from InvitationDeliveries invd
                    inner join Invitations inv
                    on invd.invitationdelivery_id = inv.invitationdelivery_id
                where  invd.raid_id = $RaidId
                    and inv.invitation_enddt > NOW()
                    and invd.invitationdelivery_type = 1 
            ";
    $maxinvdt = CSql::singleValue($sql, 'maxinvdt', false);

    if (!empty($maxinvdt)) {
        $invEndDt = $maxinvdt;
    }

    $sql = "insert into InvitationDeliveries (raid_id, invitationdelivery_type, invitationdelivery_dt, user_id, invitationdelivery_amount)
                    VALUES ($RaidId, 3, NOW(), $UserId, 1)";

    $newInvDeliveryId = MySqlQuery($sql);

    if ($newInvDeliveryId <= 0) {
        CMmb::setErrorSm('Ошибка записи раздачи приглашения.');
        return;
    }

    if ($pUserId <= 0 || $UserId <= 0 || !CRights::canDeliveryInvitation($UserId, $RaidId, 1)) {
        CMmb::setErrorMessage('Не хватает прав или нет доступных приглашений');
        return;
    }

    $sql = "insert into Invitations (user_id, invitation_begindt, invitation_enddt, invitationdelivery_id)
                VALUES ($pUserId, NOW(), '$invEndDt', $newInvDeliveryId)";

    $newInvId = MySqlQuery($sql);
    if ($newInvId <= 0) {
        CMmb::setErrorSm('Ошибка записи приглашения.');
        return;
    }

    CMmb::setResult('Приглашение выдано', "ViewUserData", "");
}
