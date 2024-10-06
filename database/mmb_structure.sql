-- phpMyAdmin SQL Dump
-- version 4.9.2
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Erstellungszeit: 22. Mai 2024 um 12:15
-- Server-Version: 10.3.28-MariaDB
-- PHP-Version: 7.2.24

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Datenbank: `mmb`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `ACRA`
--

CREATE TABLE `ACRA` (
  `date` timestamp NOT NULL DEFAULT current_timestamp(),
  `REPORT_ID` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `APP_VERSION_CODE` smallint(5) UNSIGNED DEFAULT NULL,
  `APP_VERSION_NAME` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PACKAGE_NAME` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FILE_PATH` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PHONE_MODEL` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BRAND` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PRODUCT` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ANDROID_VERSION` char(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BUILD` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TOTAL_MEM_SIZE` bigint(20) DEFAULT NULL,
  `AVAILABLE_MEM_SIZE` bigint(20) DEFAULT NULL,
  `CUSTOM_DATA` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STACK_TRACE` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `INITIAL_CONFIGURATION` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CRASH_CONFIGURATION` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DISPLAY` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `USER_APP_START_DATE` datetime DEFAULT NULL,
  `USER_CRASH_DATE` datetime DEFAULT NULL,
  `LOGCAT` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EVENTSLOG` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RADIOLOG` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `INSTALLATION_ID` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEVICE_FEATURES` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ENVIRONMENT` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SETTINGS_SYSTEM` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SETTINGS_SECURE` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SETTINGS_GLOBAL` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `THREAD_DETAILS` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BUILD_CONFIG` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Devices`
--

CREATE TABLE `Devices` (
  `device_id` int(11) NOT NULL COMMENT 'Ключ устройства',
  `device_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название устройства',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя -владельца'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица устройств';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Distances`
--

CREATE TABLE `Distances` (
  `distance_id` int(11) NOT NULL COMMENT 'Ключ дистанции',
  `distance_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название дистанции',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `distance_data` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Данные о дистанции',
  `distance_resultlink` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ссылка на результаты дистанции',
  `distance_hide` int(1) NOT NULL DEFAULT 0 COMMENT 'Признак, что дистанция скрыта',
  `distance_length` int(11) DEFAULT NULL COMMENT 'Дина дистнации в километрах (используется для определения коэффициента в рейтинге)',
  `distance_rankcoeff` float DEFAULT NULL COMMENT 'Коэффициент дистанции в рейтинге (отношение длины текущей к максимальной)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица дистанций марш-бросков';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Errors`
--

CREATE TABLE `Errors` (
  `error_id` int(11) NOT NULL COMMENT 'Ключ ошибки',
  `error_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название ошибки',
  `error_manual` int(11) NOT NULL COMMENT 'ПРизнак, что ошибка ставится оператором'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица ошибок в результатах';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `FileTypes`
--

CREATE TABLE `FileTypes` (
  `filetype_id` int(11) NOT NULL COMMENT 'Ключ типа',
  `filetype_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название типа'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Типы файлов';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `InvitationDeliveries`
--

CREATE TABLE `InvitationDeliveries` (
  `invitationdelivery_id` int(11) NOT NULL,
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `invitationdelivery_type` int(11) NOT NULL,
  `invitationdelivery_dt` datetime NOT NULL,
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя, который этот розыгрыш проводил',
  `invitationdelivery_amount` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица выдач приглашений';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `InvitationDeliveryTypes`
--

CREATE TABLE `InvitationDeliveryTypes` (
  `invitationdeliverytype_id` int(11) NOT NULL COMMENT 'Ключ типа раздачи приглашений',
  `invitationdeliverytype_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название типа раздачи приглашений'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Справочник типов раздачи приглашений';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Invitations`
--

CREATE TABLE `Invitations` (
  `invitation_id` int(11) NOT NULL COMMENT 'Ключ приглашения',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя, которому выдано приглашение',
  `invitation_begindt` datetime NOT NULL COMMENT 'Время начала действия приглашения',
  `invitation_enddt` datetime NOT NULL COMMENT 'Время конца действия приглашения',
  `invitationdelivery_id` int(11) NOT NULL COMMENT 'Ключ выдачи приглашений'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица приглашений';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `LevelPointDiscounts`
--

CREATE TABLE `LevelPointDiscounts` (
  `levelpointdiscount_id` int(11) NOT NULL COMMENT 'Ключ группы с амнистией',
  `levelpointdiscount_hide` int(1) NOT NULL COMMENT 'Признак, что группа с амнистией скрыта',
  `distance_id` int(11) NOT NULL COMMENT 'Ключ дистанции',
  `levelpointdiscount_value` int(11) NOT NULL COMMENT 'Величина амнистии для группы в минутах',
  `levelpointdiscount_start` int(11) NOT NULL COMMENT 'Порядковый номер (levelpoint_order) первого КП текущей группы',
  `levelpointdiscount_finish` int(11) NOT NULL COMMENT 'Порядковый номер (levelpoint_order) последнего КП текущей группы',
  `levelpoint_id` int(11) NOT NULL COMMENT 'Ключ точки, в которой будет производится учёт этого облака'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица групп (интервалов) КП с амнистией' ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `LevelPoints`
--

CREATE TABLE `LevelPoints` (
  `levelpoint_id` int(11) NOT NULL COMMENT 'Ключ контрольной точки',
  `levelpoint_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название контрольной точки, на которой ведется протокол',
  `pointtype_id` int(11) NOT NULL COMMENT 'Ключ типа точки',
  `levelpoint_order` int(11) NOT NULL COMMENT 'Порядковый номер контрольной точки на всей дистанции',
  `levelpoint_hide` int(1) NOT NULL DEFAULT 0 COMMENT 'Признак, что точка скрыта',
  `distance_id` int(11) DEFAULT NULL COMMENT 'Ключ дистанции',
  `levelpoint_penalty` int(11) DEFAULT NULL COMMENT 'Штраф за невзятие КП в минутах',
  `levelpoint_mindatetime` datetime DEFAULT NULL COMMENT 'Минимальное время, которое может быть у команды на точке',
  `levelpoint_maxdatetime` datetime DEFAULT NULL COMMENT 'Максимальное время, которое может быть у команды на точке',
  `scanpoint_id` int(11) DEFAULT NULL COMMENT 'Ключ точки сканирования',
  `level_id` int(11) DEFAULT NULL,
  `levelpoint_maxintervaltoprevious` int(11) DEFAULT NULL COMMENT 'Максимальная длительность интервала в минутах по отношению к предыдущей точке'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Контрольные точки на этапах';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `LinkTypes`
--

CREATE TABLE `LinkTypes` (
  `linktype_id` int(11) NOT NULL COMMENT 'Ключ типа ссылки',
  `linktype_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название типа ссылки',
  `linktype_hide` int(11) NOT NULL COMMENT 'Тип скрыт',
  `linktype_textonly` int(11) NOT NULL COMMENT 'Признак, что впечателния содержат только текст',
  `linktype_order` int(11) NOT NULL COMMENT 'Порядок вывода на странице впечатлений'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Справочник типов ссылок пользователей (отзывы, впечатления)';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Logs`
--

CREATE TABLE `Logs` (
  `logs_id` int(11) NOT NULL,
  `logs_dt` timestamp NOT NULL DEFAULT current_timestamp(),
  `logs_level` enum('critical','error','warning','info','debug','trace') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'critical',
  `user_id` int(11) DEFAULT NULL,
  `logs_duration` int(11) DEFAULT NULL,
  `logs_operation` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logs_message` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Журнал ошибок и предупреждений';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `PointTypes`
--

CREATE TABLE `PointTypes` (
  `pointtype_id` int(11) NOT NULL COMMENT 'Ключ типа точки',
  `pointtype_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название типа точки'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Типы контрольных точек';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `RaidDevelopers`
--

CREATE TABLE `RaidDevelopers` (
  `raiddeveloper_id` int(11) NOT NULL COMMENT 'Ключ записи о волонтёрах',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя',
  `raiddeveloper_hide` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак удаления записи о волонтёре'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица волонтёров';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `RaidFiles`
--

CREATE TABLE `RaidFiles` (
  `raidfile_id` int(11) NOT NULL COMMENT 'Ключ файла',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `raidfile_mimetype` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'mime тип файла',
  `filetype_id` int(11) NOT NULL COMMENT 'Ключ типа файла',
  `raidfile_binarydata` blob DEFAULT NULL COMMENT 'Содержимое файла (бинарное)',
  `raidfile_textdata` mediumtext COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Содержимое файла (текст)',
  `raidfile_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Имя  файлa после загрузки ',
  `raidfile_uploaddt` datetime NOT NULL COMMENT 'Время загрузки файла',
  `raidfile_comment` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Описание файла',
  `raidfile_hide` int(1) NOT NULL COMMENT 'Файл скрыт'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='таблица файлов для ММБ (положения, логотипы, карты)';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `RaidModerators`
--

CREATE TABLE `RaidModerators` (
  `raidmoderator_id` int(11) NOT NULL COMMENT 'Ключ записи о модераторе',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя',
  `raidmoderator_hide` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак удаления записи о модераторе'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица модераторов';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Raids`
--

CREATE TABLE `Raids` (
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `raid_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название марш-броска',
  `raid_period` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Сроки проведения',
  `raid_startpoint` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Район старта марш-броска',
  `raid_finishpoint` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Район финиша марш броска',
  `raid_ruleslink` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ссылка на положение',
  `raid_startlink` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ссылка на информацию о старте',
  `raid_folder` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Каталог на сервере',
  `raid_registrationenddate` date DEFAULT NULL COMMENT 'Дата окончания регистрации - крайний срок заявки до 23:59 этой даты.  Наличие даты является идикатором, что  ММБ нужно показать в списке для пользователей - до этого видит только администратор',
  `raid_closedate` date DEFAULT NULL COMMENT 'Дата, с котрой марш-бросок  закрывается  на правку пользователями (е модератороами)',
  `raid_znlink` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ссылка на значок',
  `raid_kpwptlink` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ссылка на файл с точками КП',
  `raid_legendlink` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ссылка на легенду',
  `raid_ziplink` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ссылка на zip файл с данными ММБ',
  `raid_noshowresult` int(1) DEFAULT NULL COMMENT 'Признак, что не надо показывать результаты',
  `raid_fileprefix` char(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Префикс файлов, который автоматически добавляется, если файл не начинается с него',
  `raid_readonlyhoursbeforestart` int(11) DEFAULT NULL COMMENT 'За сколько часов до старта закрывается редактирование для участников',
  `raid_mapprice` int(11) DEFAULT NULL COMMENT 'Стоимость одного комплекта карт в рублях',
  `raid_notstartfee` int(11) DEFAULT NULL COMMENT 'Штраф за неявку на старт в предыдущий марш-бросок',
  `raid_nostartprice` int(11) DEFAULT NULL COMMENT 'Стоимость неявки на старт (при условии, тчо участник не был удален)',
  `raid_teamslimit` int(11) NOT NULL DEFAULT 0 COMMENT 'Лимит команд на марш-броске',
  `raid_btpin` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Sportiduino Bluetooth default PIN',
  `raid_excludefromrank` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак исключения марш-броска из расчета рейтинга'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица марш-бросков';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `RaidStatuses`
--

CREATE TABLE `RaidStatuses` (
  `status_id` int(11) NOT NULL COMMENT 'ключ Статуса',
  `status_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название статуса',
  `status_description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Описание статуса',
  `status_order` int(11) NOT NULL COMMENT 'Порядковый номер',
  `status_oldnum` int(11) NOT NULL COMMENT 'Номер статуса (для обратной совместимости)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Справочник статусов';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `ScanPoints`
--

CREATE TABLE `ScanPoints` (
  `scanpoint_id` int(11) NOT NULL,
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `scanpoint_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название места сканирования',
  `scanpoint_order` int(11) NOT NULL COMMENT 'Порядковый номер ',
  `scanpoint_hide` int(1) NOT NULL COMMENT 'ПРизнак, что место сканирования скрыто'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица мест сканирования для объединения физически одинаковых точек разных дист';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Sessions`
--

CREATE TABLE `Sessions` (
  `session_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Идентификатор сессии',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя',
  `connection_id` int(11) NOT NULL DEFAULT 0 COMMENT 'Ключ соединения',
  `session_status` int(11) NOT NULL COMMENT 'Статус сессии (1 - превышение времени, 3 - пользователь вышел, 0 - активная сессия))',
  `session_starttime` datetime NOT NULL COMMENT 'Время старта сесии',
  `session_updatetime` datetime NOT NULL COMMENT 'Время обновления (последнего обращения к сесии'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица сессий';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `SportiduinoRecords`
--

CREATE TABLE `SportiduinoRecords` (
  `sportiduinorecord_id` int(11) NOT NULL COMMENT 'Используется только как primary key',
  `user_id` int(11) NOT NULL COMMENT 'Пользователь, который скачал базу на планшет. Нужно для идентификации планшета, откуда поступили данные.',
  `sportiduino_dbdate` datetime NOT NULL COMMENT 'Время скачивания базы на планшет. Нужно для идентификации локальной базы на планшете, откуда поступили данные.',
  `sportiduino_stationmac` bigint(20) UNSIGNED NOT NULL COMMENT 'MAC-адрес Bluetooth-адаптера судейской станции. Нужно для идентификации станции, откуда поступили данные.',
  `sportiduino_stationtime` datetime NOT NULL COMMENT 'Время на станции в тот момент, когда с нее поступили данные. Нужно для интеллектуальной коррекции результатов, если часы на станции ушли вперед/назад.',
  `sportiduino_stationdrift` int(11) NOT NULL COMMENT 'Отклонение в секундах времени на станции от времени на планшете. Нужно для обнаружения отклонения часов станции от истинного времени. Примечание: время на планшете тоже может быть неправильным.',
  `sportiduino_stationnumber` smallint(5) UNSIGNED NOT NULL COMMENT 'Номер, который имеет станция в момент отправки данных. Нужно, чтобы определить источник данных. Если это поле не равно levelpoint_order, то информация о посещении КП с номером levelpoint_order получена косвенно из чипа команды.',
  `sportiduino_stationmode` tinyint(3) UNSIGNED NOT NULL COMMENT 'Режим, в котором сейчас находится станция. Нужно, чтобы отличать записи об инициализации чипов от записей с посещениями КП на дистанции.',
  `sportiduino_inittime` datetime NOT NULL COMMENT 'Время выдачи чипа. Вместе с team_num нужно для идентификации конкретного чипа. В случае потери чипа у команды будет новый чип с новым временем выдачи.',
  `team_num` int(11) NOT NULL COMMENT 'Номер команды на данном марш-броске. Нужно для определения team_id, по которому мы можем однозначно идентифицировать команду.',
  `sportiduino_teammask` smallint(6) UNSIGNED NOT NULL COMMENT 'Битовая маска присутствия участников команды на точке. Список участников отсортирован по user_id. Нужно для записи схода участников в TeamLevelDismiss.',
  `levelpoint_order` int(11) NOT NULL COMMENT 'Порядковый номер точки на дистанции. Нужно для определения levelpoint_id. Точка выдачи чипов на старте имеет номер 0.',
  `teamlevelpoint_datetime` datetime NOT NULL COMMENT 'Время команды на точке. Нужно для записи результатов в TeamLevelPoints.'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `SportiduinoResults`
--

CREATE TABLE `SportiduinoResults` (
  `sportiduinoresults_id` int(11) NOT NULL COMMENT 'Используется только как primary key',
  `sportiduinoresults_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Время создания/изменения записи',
  `team_num` int(11) NOT NULL COMMENT 'Номер команды на данном марш-броске. Нужно для определения team_id, по которому мы можем однозначно идентифицировать команду.',
  `sportiduino_teammask` smallint(6) UNSIGNED NOT NULL COMMENT 'Битовая маска присутствия участников команды на точке. Список участников отсортирован по user_id. Нужно для записи схода участников в TeamLevelDismiss.',
  `levelpoint_order` int(11) NOT NULL COMMENT 'Порядковый номер точки на дистанции. Нужно для определения levelpoint_id. Точка выдачи чипов на старте имеет номер 0.',
  `teamlevelpoint_datetime` datetime NOT NULL COMMENT 'Время команды на точке. Нужно для записи результатов в TeamLevelPoints.'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `TeamLevelDismiss`
--

CREATE TABLE `TeamLevelDismiss` (
  `teamleveldismiss_id` int(11) NOT NULL COMMENT 'Ключ схода участника в точке',
  `teamleveldismiss_date` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Время создания записи',
  `user_id` int(11) NOT NULL DEFAULT 0 COMMENT 'Ключ пользователя, который ввел запись',
  `device_id` int(11) NOT NULL DEFAULT 1 COMMENT 'Ключ устройства, на котором была введена запись',
  `levelpoint_id` int(11) NOT NULL COMMENT 'Ключ контрольной точки, на которую не пришел сошедший участник',
  `teamuser_id` int(11) NOT NULL COMMENT 'Ключ участника в этой команде'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица схода участников перед контрольными точками';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `TeamLevelPoints`
--

CREATE TABLE `TeamLevelPoints` (
  `teamlevelpoint_id` int(11) NOT NULL COMMENT 'Ключ данных по точке',
  `teamlevelpoint_date` timestamp NOT NULL DEFAULT current_timestamp() COMMENT 'Время создания записи',
  `user_id` int(11) NOT NULL DEFAULT 0 COMMENT 'Ключ пользователя, который ввел запись',
  `device_id` int(11) NOT NULL DEFAULT 1 COMMENT 'Ключ устройства',
  `levelpoint_id` int(11) NOT NULL COMMENT 'Ключ контрольной точки',
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды',
  `teamlevelpoint_datetime` datetime DEFAULT NULL COMMENT 'Время прхождения контрольной точки',
  `teamlevelpoint_comment` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Комментарий к прохождению контрольной точки',
  `teamlevelpoint_duration` time DEFAULT NULL COMMENT 'Время нахождения (без штрафа)  команды между точками  ',
  `teamlevelpoint_penalty` int(11) DEFAULT NULL COMMENT 'Штраф  команды на точке (по предыдущим точкам)',
  `error_id` int(11) DEFAULT NULL COMMENT 'Ключ ошибки',
  `teamlevelpoint_result` time DEFAULT NULL COMMENT 'Результат команды в точке',
  `teamlevelpoint_datetimeaftercorrection` datetime DEFAULT NULL,
  `teamlevelpoint_result_old` time DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица прохождения командой контрольных точек';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Teams`
--

CREATE TABLE `Teams` (
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды',
  `team_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название команды',
  `distance_id` int(11) NOT NULL COMMENT 'Ключ дистанции',
  `team_num` int(11) NOT NULL COMMENT 'Номер команды внутри дистанции',
  `team_usegps` tinyint(1) NOT NULL COMMENT 'Признак, что команда использует GPS',
  `team_mapscount` int(11) NOT NULL COMMENT 'Необходимое число карт',
  `team_registerdt` datetime NOT NULL COMMENT 'Время регистрации команды (для отслеживания команд, которые зарегистрировались позже, чем  закрывалась регистрация',
  `team_confirmresult` int(1) NOT NULL DEFAULT 0 COMMENT 'Команда подтвердила правильность ввода данных',
  `team_hide` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак, что команда удалена',
  `team_greenpeace` int(11) NOT NULL COMMENT 'Отношение к политике "Нет сломанным унитазам!"',
  `team_moderatorconfirmresult` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак, что модератор подтвердил результаты команды',
  `level_id` int(11) DEFAULT NULL COMMENT 'Ключ этапа, на который команда не стартовала',
  `team_maxlevelpointorderdone` int(11) DEFAULT NULL COMMENT 'Максимальный порядковый номер взятой точки',
  `team_result` time DEFAULT NULL COMMENT 'Результат команды с  учетом штрафа',
  `team_parentid` int(11) DEFAULT NULL COMMENT 'Ключ команды, в которую объединили текущую',
  `team_outofrange` tinyint(1) DEFAULT NULL COMMENT 'Признак, что команда вне зачета',
  `team_importkey` int(11) DEFAULT NULL COMMENT 'Ключ команды при импорте',
  `team_importattempt` int(11) DEFAULT NULL COMMENT 'Попытка импорта',
  `team_minlevelpointorderwitherror` int(11) DEFAULT NULL COMMENT 'Минимальный порядковый номер точки с ошибкой',
  `team_comment` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Комментарий (рассчитывается, как конкатенция комментариев по всем точкам',
  `team_skippedlevelpoint` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Список невзятых КП',
  `team_waitdt` datetime DEFAULT NULL COMMENT 'Команда попала в список ожидания',
  `team_donelevelpoint` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Время взятия точек',
  `invitation_id` int(11) DEFAULT NULL COMMENT 'Ключ приглашения, которое перевело команду в зачет',
  `invitation_usedt` datetime DEFAULT NULL COMMENT 'Время активации команды (использования приглашения)',
  `team_dismiss` int(11) DEFAULT NULL COMMENT 'Признак, что команда не пришла на ммб ставится по отсутствию точек в базе',
  `team_minsex` int(11) DEFAULT NULL COMMENT 'минимальный пол участника',
  `team_maxsex` int(11) DEFAULT NULL COMMENT 'максимальный пол участника',
  `team_minage` int(11) DEFAULT NULL COMMENT 'минимальный возраст участников команды',
  `team_maxage` int(11) DEFAULT NULL COMMENT 'максимальный возраст участников команды',
  `team_userscount` int(11) DEFAULT NULL COMMENT 'число участников'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица команд ММБ';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `TeamUnionLogs`
--

CREATE TABLE `TeamUnionLogs` (
  `teamunionlog_id` int(11) NOT NULL COMMENT 'Ключ элемента объединения',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя, проводившего объединение',
  `teamunionlog_dt` datetime NOT NULL COMMENT 'Время изменений',
  `teamunionlog_hide` tinyint(1) NOT NULL COMMENT 'Признак удаления команды из объединения',
  `team_id` int(11) NOT NULL COMMENT 'Ключ старой команды',
  `team_parentid` int(11) DEFAULT NULL COMMENT 'Ключ новой команды',
  `union_status` int(11) NOT NULL COMMENT 'Статус объединенеия'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица истории объединения команд';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `TeamUsers`
--

CREATE TABLE `TeamUsers` (
  `teamuser_id` int(11) NOT NULL COMMENT 'Ключ участника команды',
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды',
  `user_id` int(11) NOT NULL COMMENT 'Ключ участника',
  `teamuser_hide` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак, что  участник удалён из команды (чтобы не делать физическое удаление)',
  `userunionlog_id` int(11) DEFAULT NULL COMMENT 'Ключ журнала объединения пользоватлей',
  `teamuser_rank` decimal(6,5) DEFAULT NULL COMMENT 'Рейтинг участника',
  `teamuser_notstartraidid` int(11) DEFAULT NULL COMMENT 'Ключ марш-броска, на котороый пользователь заявился, но не пришёл',
  `teamuser_changedt` datetime DEFAULT NULL COMMENT 'Время изменения данных',
  `invitationdelivery_id` int(11) DEFAULT NULL COMMENT 'Ключ выдачи приглашений',
  `teamuser_new` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Признак, что пользователь новый'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Участники команды';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `UserLinks`
--

CREATE TABLE `UserLinks` (
  `userlink_id` int(11) NOT NULL COMMENT 'Ключ ссылки от пользователя',
  `linktype_id` int(11) NOT NULL COMMENT 'ключ типа ссылки',
  `userlink_hide` tinyint(1) NOT NULL COMMENT 'Ссылка скрыта',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ ММБ',
  `userlink_url` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'URL ссылки ',
  `userlink_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Название ссылки'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица ссылок на впечатления  пользхователей';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `Users`
--

CREATE TABLE `Users` (
  `user_id` int(11) NOT NULL COMMENT 'Ключ  участника',
  `user_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ФИО участника',
  `user_email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Адрес электронной почты',
  `user_phone` varchar(25) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Телефон',
  `user_password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Пароль участника',
  `user_birthyear` int(11) NOT NULL COMMENT 'Год рождения',
  `user_prohibitadd` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак запрета добавлять в команду, которую создаёт другой пользователь',
  `user_sessionfornewpassword` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Поле с последним идентификаторм временной сессии для смены пароля (отправки пиьсма с запросом о смене)',
  `user_sendnewpasswordrequestdt` datetime DEFAULT NULL COMMENT 'Время последней отправки письма с запросом на смену пароля',
  `user_admin` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак, что пользователь является администратором',
  `user_hide` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Признак, что пользователь удален',
  `user_registerdt` datetime NOT NULL COMMENT 'Время регистрации (добавления записи',
  `user_lastauthorizationdt` datetime DEFAULT NULL COMMENT 'Время последней успешной авторизации',
  `user_sendnewpassworddt` datetime DEFAULT NULL COMMENT 'Время  поледней отправки письма с паролем',
  `user_allowsendchangeinfo` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Флаг разрешения высылать  письма об изменениях в команде',
  `user_allowsendorgmessages` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Флаг разрешения высылать информационные письма от организаторов',
  `user_city` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Город пользователя',
  `user_importkey` int(11) DEFAULT NULL COMMENT 'Ключ внешний при импорте данных',
  `user_importattempt` int(11) DEFAULT NULL COMMENT 'Попытка импорта',
  `userunionlog_id` int(11) DEFAULT NULL COMMENT 'Ключ записи об объединении с другим пользователем',
  `user_noshow` tinyint(1) DEFAULT NULL COMMENT 'Признак,  что ФИО пользователя не выводится  в результатах и рейтингах',
  `user_rank` decimal(28,5) DEFAULT NULL COMMENT 'Рейтинг пользователя без уценки',
  `user_minraidid` int(11) DEFAULT NULL COMMENT 'Минимальный ключ  ММБ, в котором пользователь принимал участие',
  `user_maxraidid` int(11) DEFAULT NULL COMMENT 'Максимальный ключ ММБ, в котором пользовтаель принимал участие',
  `user_r6` decimal(28,5) DEFAULT NULL COMMENT 'R6 пользователя ',
  `user_noinvitation` int(11) DEFAULT NULL COMMENT 'Признак, чтопользователь не получает приглашения  ',
  `user_maxnotstartraidid` int(11) DEFAULT NULL COMMENT 'Максимальный ключ ММБ, в котором пользовтаель не вышел на старт, но зарегистрировался и не удалил себя',
  `user_r6old` decimal(28,5) DEFAULT NULL COMMENT 'вариант без учета волонтёров',
  `user_sex` int(11) DEFAULT NULL COMMENT 'Пол',
  `user_amateur` tinyint(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица участника ММБ';

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `UserUnionLogs`
--

CREATE TABLE `UserUnionLogs` (
  `userunionlog_id` int(11) NOT NULL COMMENT 'Ключ журнала объединения пользователей',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя (нового)',
  `user_parentid` int(11) NOT NULL COMMENT 'Ключ пользователя (старого)',
  `union_status` int(11) NOT NULL COMMENT 'Статус объединения (0 - отклонено, 1 - запрос, 2 - объединены, 3 - отмена объединения ',
  `userunionlog_dt` datetime NOT NULL COMMENT 'Время запроса на объединение',
  `userunionlog_comment` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Комментарий'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Таблица объединений пользователей';

--
-- Indizes der exportierten Tabellen
--

--
-- Indizes für die Tabelle `ACRA`
--
ALTER TABLE `ACRA`
  ADD PRIMARY KEY (`REPORT_ID`);

--
-- Indizes für die Tabelle `Devices`
--
ALTER TABLE `Devices`
  ADD PRIMARY KEY (`device_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indizes für die Tabelle `Distances`
--
ALTER TABLE `Distances`
  ADD PRIMARY KEY (`distance_id`),
  ADD KEY `raid_id` (`raid_id`);

--
-- Indizes für die Tabelle `Errors`
--
ALTER TABLE `Errors`
  ADD PRIMARY KEY (`error_id`);

--
-- Indizes für die Tabelle `FileTypes`
--
ALTER TABLE `FileTypes`
  ADD PRIMARY KEY (`filetype_id`);

--
-- Indizes für die Tabelle `InvitationDeliveries`
--
ALTER TABLE `InvitationDeliveries`
  ADD PRIMARY KEY (`invitationdelivery_id`),
  ADD KEY `invitationdelivery_type` (`invitationdelivery_type`),
  ADD KEY `raid_id` (`raid_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indizes für die Tabelle `InvitationDeliveryTypes`
--
ALTER TABLE `InvitationDeliveryTypes`
  ADD PRIMARY KEY (`invitationdeliverytype_id`);

--
-- Indizes für die Tabelle `Invitations`
--
ALTER TABLE `Invitations`
  ADD PRIMARY KEY (`invitation_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `invitationdelivery_id` (`invitationdelivery_id`);

--
-- Indizes für die Tabelle `LevelPointDiscounts`
--
ALTER TABLE `LevelPointDiscounts`
  ADD PRIMARY KEY (`levelpointdiscount_id`),
  ADD UNIQUE KEY `levelpoint_id` (`levelpoint_id`),
  ADD KEY `distance_id` (`distance_id`);

--
-- Indizes für die Tabelle `LevelPoints`
--
ALTER TABLE `LevelPoints`
  ADD PRIMARY KEY (`levelpoint_id`),
  ADD KEY `pointtype_id` (`pointtype_id`),
  ADD KEY `levelpoint_order` (`levelpoint_order`),
  ADD KEY `distance_id` (`distance_id`);

--
-- Indizes für die Tabelle `LinkTypes`
--
ALTER TABLE `LinkTypes`
  ADD PRIMARY KEY (`linktype_id`);

--
-- Indizes für die Tabelle `Logs`
--
ALTER TABLE `Logs`
  ADD PRIMARY KEY (`logs_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `logs_level` (`logs_level`);

--
-- Indizes für die Tabelle `PointTypes`
--
ALTER TABLE `PointTypes`
  ADD PRIMARY KEY (`pointtype_id`);

--
-- Indizes für die Tabelle `RaidDevelopers`
--
ALTER TABLE `RaidDevelopers`
  ADD PRIMARY KEY (`raiddeveloper_id`),
  ADD KEY `raid_id` (`raid_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indizes für die Tabelle `RaidFiles`
--
ALTER TABLE `RaidFiles`
  ADD PRIMARY KEY (`raidfile_id`),
  ADD KEY `raid_id` (`raid_id`),
  ADD KEY `filetype_id` (`filetype_id`);

--
-- Indizes für die Tabelle `RaidModerators`
--
ALTER TABLE `RaidModerators`
  ADD PRIMARY KEY (`raidmoderator_id`),
  ADD KEY `raid_id` (`raid_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indizes für die Tabelle `Raids`
--
ALTER TABLE `Raids`
  ADD PRIMARY KEY (`raid_id`),
  ADD UNIQUE KEY `raid_name` (`raid_name`);

--
-- Indizes für die Tabelle `RaidStatuses`
--
ALTER TABLE `RaidStatuses`
  ADD PRIMARY KEY (`status_id`);

--
-- Indizes für die Tabelle `ScanPoints`
--
ALTER TABLE `ScanPoints`
  ADD PRIMARY KEY (`scanpoint_id`),
  ADD KEY `raid_id` (`raid_id`);

--
-- Indizes für die Tabelle `Sessions`
--
ALTER TABLE `Sessions`
  ADD PRIMARY KEY (`session_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indizes für die Tabelle `SportiduinoRecords`
--
ALTER TABLE `SportiduinoRecords`
  ADD PRIMARY KEY (`sportiduinorecord_id`),
  ADD UNIQUE KEY `chip_event` (`user_id`,`sportiduino_dbdate`,`sportiduino_stationmac`,`sportiduino_stationtime`,`sportiduino_stationdrift`,`sportiduino_stationnumber`,`sportiduino_stationmode`,`sportiduino_inittime`,`team_num`,`sportiduino_teammask`,`levelpoint_order`,`teamlevelpoint_datetime`);

--
-- Indizes für die Tabelle `SportiduinoResults`
--
ALTER TABLE `SportiduinoResults`
  ADD PRIMARY KEY (`sportiduinoresults_id`),
  ADD UNIQUE KEY `result` (`team_num`,`sportiduino_teammask`,`levelpoint_order`,`teamlevelpoint_datetime`);

--
-- Indizes für die Tabelle `TeamLevelDismiss`
--
ALTER TABLE `TeamLevelDismiss`
  ADD PRIMARY KEY (`teamleveldismiss_id`),
  ADD KEY `device_id` (`device_id`),
  ADD KEY `levelpoint_id` (`levelpoint_id`),
  ADD KEY `teamuser_id` (`teamuser_id`);

--
-- Indizes für die Tabelle `TeamLevelPoints`
--
ALTER TABLE `TeamLevelPoints`
  ADD PRIMARY KEY (`teamlevelpoint_id`),
  ADD KEY `levelpoint_id` (`levelpoint_id`),
  ADD KEY `team_id` (`team_id`);

--
-- Indizes für die Tabelle `Teams`
--
ALTER TABLE `Teams`
  ADD PRIMARY KEY (`team_id`),
  ADD KEY `distance_id` (`distance_id`),
  ADD KEY `team_result` (`team_result`),
  ADD KEY `team_num` (`team_num`),
  ADD KEY `team_progress` (`team_maxlevelpointorderdone`),
  ADD KEY `invitation_id` (`invitation_id`);

--
-- Indizes für die Tabelle `TeamUnionLogs`
--
ALTER TABLE `TeamUnionLogs`
  ADD PRIMARY KEY (`teamunionlog_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `team_id` (`team_id`),
  ADD KEY `team_parentid` (`team_parentid`);

--
-- Indizes für die Tabelle `TeamUsers`
--
ALTER TABLE `TeamUsers`
  ADD PRIMARY KEY (`teamuser_id`),
  ADD KEY `team_id` (`team_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `invitationdelivery_id` (`invitationdelivery_id`);

--
-- Indizes für die Tabelle `UserLinks`
--
ALTER TABLE `UserLinks`
  ADD PRIMARY KEY (`userlink_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `raid_id` (`raid_id`),
  ADD KEY `linktype_id` (`linktype_id`);

--
-- Indizes für die Tabelle `Users`
--
ALTER TABLE `Users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `user_email` (`user_email`),
  ADD UNIQUE KEY `user_name` (`user_name`,`user_birthyear`,`userunionlog_id`);

--
-- Indizes für die Tabelle `UserUnionLogs`
--
ALTER TABLE `UserUnionLogs`
  ADD PRIMARY KEY (`userunionlog_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `user_parentid` (`user_parentid`);

--
-- AUTO_INCREMENT für exportierte Tabellen
--

--
-- AUTO_INCREMENT für Tabelle `Devices`
--
ALTER TABLE `Devices`
  MODIFY `device_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ устройства';

--
-- AUTO_INCREMENT für Tabelle `Distances`
--
ALTER TABLE `Distances`
  MODIFY `distance_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ дистанции';

--
-- AUTO_INCREMENT für Tabelle `FileTypes`
--
ALTER TABLE `FileTypes`
  MODIFY `filetype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа';

--
-- AUTO_INCREMENT für Tabelle `InvitationDeliveries`
--
ALTER TABLE `InvitationDeliveries`
  MODIFY `invitationdelivery_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `InvitationDeliveryTypes`
--
ALTER TABLE `InvitationDeliveryTypes`
  MODIFY `invitationdeliverytype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа раздачи приглашений';

--
-- AUTO_INCREMENT für Tabelle `Invitations`
--
ALTER TABLE `Invitations`
  MODIFY `invitation_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ приглашения';

--
-- AUTO_INCREMENT für Tabelle `LevelPointDiscounts`
--
ALTER TABLE `LevelPointDiscounts`
  MODIFY `levelpointdiscount_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ группы с амнистией';

--
-- AUTO_INCREMENT für Tabelle `LevelPoints`
--
ALTER TABLE `LevelPoints`
  MODIFY `levelpoint_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ контрольной точки';

--
-- AUTO_INCREMENT für Tabelle `LinkTypes`
--
ALTER TABLE `LinkTypes`
  MODIFY `linktype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа ссылки';

--
-- AUTO_INCREMENT für Tabelle `Logs`
--
ALTER TABLE `Logs`
  MODIFY `logs_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `PointTypes`
--
ALTER TABLE `PointTypes`
  MODIFY `pointtype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа точки';

--
-- AUTO_INCREMENT für Tabelle `RaidDevelopers`
--
ALTER TABLE `RaidDevelopers`
  MODIFY `raiddeveloper_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ записи о волонтёрах';

--
-- AUTO_INCREMENT für Tabelle `RaidFiles`
--
ALTER TABLE `RaidFiles`
  MODIFY `raidfile_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ файла';

--
-- AUTO_INCREMENT für Tabelle `RaidModerators`
--
ALTER TABLE `RaidModerators`
  MODIFY `raidmoderator_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ записи о модераторе';

--
-- AUTO_INCREMENT für Tabelle `Raids`
--
ALTER TABLE `Raids`
  MODIFY `raid_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ марш-броска';

--
-- AUTO_INCREMENT für Tabelle `RaidStatuses`
--
ALTER TABLE `RaidStatuses`
  MODIFY `status_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ключ Статуса';

--
-- AUTO_INCREMENT für Tabelle `ScanPoints`
--
ALTER TABLE `ScanPoints`
  MODIFY `scanpoint_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `SportiduinoRecords`
--
ALTER TABLE `SportiduinoRecords`
  MODIFY `sportiduinorecord_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Используется только как primary key';

--
-- AUTO_INCREMENT für Tabelle `SportiduinoResults`
--
ALTER TABLE `SportiduinoResults`
  MODIFY `sportiduinoresults_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Используется только как primary key';

--
-- AUTO_INCREMENT für Tabelle `TeamLevelDismiss`
--
ALTER TABLE `TeamLevelDismiss`
  MODIFY `teamleveldismiss_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ схода участника в точке';

--
-- AUTO_INCREMENT für Tabelle `TeamLevelPoints`
--
ALTER TABLE `TeamLevelPoints`
  MODIFY `teamlevelpoint_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ данных по точке';

--
-- AUTO_INCREMENT für Tabelle `Teams`
--
ALTER TABLE `Teams`
  MODIFY `team_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ команды';

--
-- AUTO_INCREMENT für Tabelle `TeamUnionLogs`
--
ALTER TABLE `TeamUnionLogs`
  MODIFY `teamunionlog_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ элемента объединения';

--
-- AUTO_INCREMENT für Tabelle `TeamUsers`
--
ALTER TABLE `TeamUsers`
  MODIFY `teamuser_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ участника команды';

--
-- AUTO_INCREMENT für Tabelle `UserLinks`
--
ALTER TABLE `UserLinks`
  MODIFY `userlink_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ ссылки от пользователя';

--
-- AUTO_INCREMENT für Tabelle `Users`
--
ALTER TABLE `Users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ  участника';

--
-- AUTO_INCREMENT für Tabelle `UserUnionLogs`
--
ALTER TABLE `UserUnionLogs`
  MODIFY `userunionlog_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ журнала объединения пользователей';

--
-- Constraints der exportierten Tabellen
--

--
-- Constraints der Tabelle `Devices`
--
ALTER TABLE `Devices`
  ADD CONSTRAINT `Devices_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Constraints der Tabelle `Distances`
--
ALTER TABLE `Distances`
  ADD CONSTRAINT `Distances_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`);

--
-- Constraints der Tabelle `InvitationDeliveries`
--
ALTER TABLE `InvitationDeliveries`
  ADD CONSTRAINT `InvitationDeliveries_ibfk_1` FOREIGN KEY (`invitationdelivery_type`) REFERENCES `InvitationDeliveryTypes` (`invitationdeliverytype_id`),
  ADD CONSTRAINT `InvitationDeliveries_ibfk_2` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`),
  ADD CONSTRAINT `InvitationDeliveries_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Constraints der Tabelle `Invitations`
--
ALTER TABLE `Invitations`
  ADD CONSTRAINT `Invitations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`),
  ADD CONSTRAINT `Invitations_ibfk_4` FOREIGN KEY (`invitationdelivery_id`) REFERENCES `InvitationDeliveries` (`invitationdelivery_id`);

--
-- Constraints der Tabelle `LevelPointDiscounts`
--
ALTER TABLE `LevelPointDiscounts`
  ADD CONSTRAINT `LevelPointDiscounts_ibfk_1` FOREIGN KEY (`distance_id`) REFERENCES `Distances` (`distance_id`),
  ADD CONSTRAINT `LevelPointDiscounts_ibfk_2` FOREIGN KEY (`levelpoint_id`) REFERENCES `LevelPoints` (`levelpoint_id`);

--
-- Constraints der Tabelle `LevelPoints`
--
ALTER TABLE `LevelPoints`
  ADD CONSTRAINT `LevelPoints_ibfk_1` FOREIGN KEY (`pointtype_id`) REFERENCES `PointTypes` (`pointtype_id`),
  ADD CONSTRAINT `LevelPoints_ibfk_2` FOREIGN KEY (`distance_id`) REFERENCES `Distances` (`distance_id`);

--
-- Constraints der Tabelle `Logs`
--
ALTER TABLE `Logs`
  ADD CONSTRAINT `Logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Constraints der Tabelle `RaidDevelopers`
--
ALTER TABLE `RaidDevelopers`
  ADD CONSTRAINT `RaidDevelopers_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`),
  ADD CONSTRAINT `RaidDevelopers_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Constraints der Tabelle `RaidFiles`
--
ALTER TABLE `RaidFiles`
  ADD CONSTRAINT `RaidFiles_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`),
  ADD CONSTRAINT `RaidFiles_ibfk_2` FOREIGN KEY (`filetype_id`) REFERENCES `FileTypes` (`filetype_id`);

--
-- Constraints der Tabelle `RaidModerators`
--
ALTER TABLE `RaidModerators`
  ADD CONSTRAINT `RaidModerators_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`),
  ADD CONSTRAINT `RaidModerators_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Constraints der Tabelle `ScanPoints`
--
ALTER TABLE `ScanPoints`
  ADD CONSTRAINT `ScanPoints_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`);

--
-- Constraints der Tabelle `TeamLevelDismiss`
--
ALTER TABLE `TeamLevelDismiss`
  ADD CONSTRAINT `TeamLevelDismiss_ibfk_1` FOREIGN KEY (`device_id`) REFERENCES `Devices` (`device_id`),
  ADD CONSTRAINT `TeamLevelDismiss_ibfk_2` FOREIGN KEY (`levelpoint_id`) REFERENCES `LevelPoints` (`levelpoint_id`),
  ADD CONSTRAINT `TeamLevelDismiss_ibfk_3` FOREIGN KEY (`teamuser_id`) REFERENCES `TeamUsers` (`teamuser_id`);

--
-- Constraints der Tabelle `TeamLevelPoints`
--
ALTER TABLE `TeamLevelPoints`
  ADD CONSTRAINT `TeamLevelPoints_ibfk_1` FOREIGN KEY (`levelpoint_id`) REFERENCES `LevelPoints` (`levelpoint_id`),
  ADD CONSTRAINT `TeamLevelPoints_ibfk_2` FOREIGN KEY (`team_id`) REFERENCES `Teams` (`team_id`);

--
-- Constraints der Tabelle `Teams`
--
ALTER TABLE `Teams`
  ADD CONSTRAINT `Teams_ibfk_1` FOREIGN KEY (`distance_id`) REFERENCES `Distances` (`distance_id`);

--
-- Constraints der Tabelle `TeamUnionLogs`
--
ALTER TABLE `TeamUnionLogs`
  ADD CONSTRAINT `TeamUnionLogs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`),
  ADD CONSTRAINT `TeamUnionLogs_ibfk_2` FOREIGN KEY (`team_id`) REFERENCES `Teams` (`team_id`);

--
-- Constraints der Tabelle `TeamUsers`
--
ALTER TABLE `TeamUsers`
  ADD CONSTRAINT `TeamUsers_ibfk_1` FOREIGN KEY (`team_id`) REFERENCES `Teams` (`team_id`),
  ADD CONSTRAINT `TeamUsers_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Constraints der Tabelle `UserLinks`
--
ALTER TABLE `UserLinks`
  ADD CONSTRAINT `UserLinks_ibfk_1` FOREIGN KEY (`linktype_id`) REFERENCES `LinkTypes` (`linktype_id`),
  ADD CONSTRAINT `UserLinks_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`),
  ADD CONSTRAINT `UserLinks_ibfk_3` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`);

--
-- Constraints der Tabelle `UserUnionLogs`
--
ALTER TABLE `UserUnionLogs`
  ADD CONSTRAINT `UserUnionLogs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
