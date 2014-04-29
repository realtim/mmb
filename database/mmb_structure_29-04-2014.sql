-- phpMyAdmin SQL Dump
-- version 3.4.10.1deb1
-- http://www.phpmyadmin.net
--
-- Хост: localhost
-- Время создания: Апр 29 2014 г., 23:51
-- Версия сервера: 5.5.32
-- Версия PHP: 5.3.10-1ubuntu3.8

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- База данных: `c5_mmb`
--

-- --------------------------------------------------------

--
-- Структура таблицы `Devices`
--

DROP TABLE IF EXISTS `Devices`;
CREATE TABLE IF NOT EXISTS `Devices` (
  `device_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ устройства',
  `device_name` varchar(100) NOT NULL COMMENT 'Название устройства',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя -владельца',
  PRIMARY KEY (`device_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица устройств' AUTO_INCREMENT=62 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Distances`
--

DROP TABLE IF EXISTS `Distances`;
CREATE TABLE IF NOT EXISTS `Distances` (
  `distance_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ дистанции',
  `distance_name` varchar(50) NOT NULL COMMENT 'Название дистанции',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `distance_data` varchar(50) NOT NULL COMMENT 'Данные о дистанции',
  `distance_resultlink` varchar(150) NOT NULL COMMENT 'Ссылка на результаты дистанции',
  `distance_hide` int(1) NOT NULL DEFAULT '0' COMMENT 'Признак, что дистанция скрыта',
  PRIMARY KEY (`distance_id`),
  KEY `raid_id` (`raid_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица дистанций марш-бросков' AUTO_INCREMENT=31 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Errors`
--

DROP TABLE IF EXISTS `Errors`;
CREATE TABLE IF NOT EXISTS `Errors` (
  `error_id` int(11) NOT NULL COMMENT 'Ключ ошибки',
  `error_name` varchar(100) NOT NULL COMMENT 'Название ошибки',
  PRIMARY KEY (`error_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='Таблица ошибок в результатах';

-- --------------------------------------------------------

--
-- Структура таблицы `FileTypes`
--

DROP TABLE IF EXISTS `FileTypes`;
CREATE TABLE IF NOT EXISTS `FileTypes` (
  `filetype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа',
  `filetype_name` varchar(100) NOT NULL COMMENT 'Название типа',
  PRIMARY KEY (`filetype_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Типы файлов' AUTO_INCREMENT=12 ;

-- --------------------------------------------------------

--
-- Структура таблицы `LevelMapLinks`
--

DROP TABLE IF EXISTS `LevelMapLinks`;
CREATE TABLE IF NOT EXISTS `LevelMapLinks` (
  `levelmaplink_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ ссылки на карту этапа',
  `level_id` int(11) NOT NULL COMMENT 'Ключ этапа',
  `levelmaplink_url` varchar(100) NOT NULL COMMENT 'Адрес сылки на карту',
  `levelmapozilink_url` varchar(100) NOT NULL COMMENT 'Ссылка на файл привязки',
  PRIMARY KEY (`levelmaplink_id`),
  KEY `level_id` (`level_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица ссылок на карты этапов' AUTO_INCREMENT=32 ;

-- --------------------------------------------------------

--
-- Структура таблицы `LevelPointDiscounts`
--

DROP TABLE IF EXISTS `LevelPointDiscounts`;
CREATE TABLE IF NOT EXISTS `LevelPointDiscounts` (
  `levelpointdiscount_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ группы с амнистией',
  `levelpointdiscount_hide` int(1) NOT NULL COMMENT 'Признак, что группа с амнистией скрыта',
  `distance_id` int(11) NOT NULL COMMENT 'Ключ дистанции',
  `levelpointdiscount_value` int(11) NOT NULL COMMENT 'Величина амнистии для группы в минутах',
  `levelpointdiscount_start` int(11) NOT NULL COMMENT 'Порядковый номер (levelpoint_order) первого КП текущей группы',
  `levelpointdiscount_finish` int(11) NOT NULL COMMENT 'Порядковый номер (levelpoint_order) последнего КП текущей группы',
  PRIMARY KEY (`levelpointdiscount_id`),
  KEY `distance_id` (`distance_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='Таблица групп (интервалов) КП с амнистией' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `LevelPoints`
--

DROP TABLE IF EXISTS `LevelPoints`;
CREATE TABLE IF NOT EXISTS `LevelPoints` (
  `levelpoint_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ контрольной точки',
  `levelpoint_name` varchar(50) NOT NULL COMMENT 'Название контрольной точки, на которой ведется протокол',
  `level_id` int(11) DEFAULT NULL COMMENT 'Ключ этапа',
  `pointtype_id` int(11) NOT NULL COMMENT 'Ключ типа точки',
  `levelpoint_order` int(11) NOT NULL COMMENT 'Порядковый номер контрольной точки на всей дистанции',
  `levelpoint_hide` int(1) NOT NULL DEFAULT '0' COMMENT 'Признак, что точка скрыта',
  `distance_id` int(11) DEFAULT NULL COMMENT 'Ключ дистанции',
  `levelpoint_penalty` int(11) DEFAULT NULL COMMENT 'Штраф за невзятие КП в минутах',
  `levelpoint_mindatetime` datetime DEFAULT NULL COMMENT 'Минимальное время, которое может быть у команды на точке',
  `levelpoint_maxdatetime` datetime DEFAULT NULL COMMENT 'Максимальное время, которое может быть у команды на точке',
  `scanpoint_id` int(11) NOT NULL COMMENT 'Ключ точки сканирования',
  PRIMARY KEY (`levelpoint_id`),
  KEY `level_id` (`level_id`),
  KEY `pointtype_id` (`pointtype_id`),
  KEY `levelpoint_order` (`levelpoint_order`),
  KEY `distance_id` (`distance_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Контрольные точки на этапах' AUTO_INCREMENT=140 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Levels`
--

DROP TABLE IF EXISTS `Levels`;
CREATE TABLE IF NOT EXISTS `Levels` (
  `level_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ этапа',
  `level_name` varchar(50) NOT NULL COMMENT 'Название этапа',
  `level_order` int(11) NOT NULL COMMENT 'Порядок этапов внутри дистанции',
  `distance_id` int(11) NOT NULL COMMENT 'Ключ дистанции',
  `level_begtime` datetime NOT NULL COMMENT 'Минимальное время старта на этап',
  `level_maxbegtime` datetime NOT NULL COMMENT 'Максимально возможное время старта',
  `level_minendtime` datetime NOT NULL COMMENT 'Минимально возможное время финиша',
  `level_endtime` datetime NOT NULL COMMENT 'Время закрытия этапа',
  `level_starttype` tinyint(1) NOT NULL COMMENT 'Тип старта (1 - по готовности, 2 - общий, 3 - в момент финиша на предыдущем этапе)',
  `level_pointnames` varchar(150) NOT NULL COMMENT 'Спсиок названий КП этапа',
  `level_pointpenalties` varchar(150) NOT NULL COMMENT 'Список штрафов в минутах за невзятие КП  (в том же порядке, что и список КП)',
  `level_discountpoints` varchar(150) NOT NULL COMMENT 'Идикаторы КП, на которых действует скидка (0 или 1)',
  `level_discount` int(11) NOT NULL COMMENT 'Размер скидки в минутах',
  `level_hide` int(1) NOT NULL DEFAULT '0' COMMENT 'Признак, что уровень скрыт',
  PRIMARY KEY (`level_id`),
  KEY `distance_id` (`distance_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица этапов' AUTO_INCREMENT=87 ;

-- --------------------------------------------------------

--
-- Структура таблицы `PointTypes`
--

DROP TABLE IF EXISTS `PointTypes`;
CREATE TABLE IF NOT EXISTS `PointTypes` (
  `pointtype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа точки',
  `pointtype_name` varchar(50) NOT NULL COMMENT 'Название типа точки',
  PRIMARY KEY (`pointtype_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Типы контрольных точек' AUTO_INCREMENT=6 ;

-- --------------------------------------------------------

--
-- Структура таблицы `RaidFiles`
--

DROP TABLE IF EXISTS `RaidFiles`;
CREATE TABLE IF NOT EXISTS `RaidFiles` (
  `raidfile_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ файла',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `raidfile_mimetype` varchar(50) NOT NULL COMMENT 'mime тип файла',
  `filetype_id` int(11) NOT NULL COMMENT 'Ключ типа файла',
  `raidfile_binarydata` blob COMMENT 'Содержимое файла (бинарное)',
  `raidfile_textdata` text COMMENT 'Содержимое файла (текст)',
  `raidfile_name` varchar(50) NOT NULL COMMENT 'Имя  файлa после загрузки ',
  `raidfile_uploaddt` datetime NOT NULL COMMENT 'Время загрузки файла',
  `raidfile_comment` varchar(50) NOT NULL COMMENT 'Описание файла',
  `raidfile_hide` int(1) NOT NULL COMMENT 'Файл скрыт',
  PRIMARY KEY (`raidfile_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='таблица файлов для ММБ (положения, логотипы, карты)' AUTO_INCREMENT=26 ;

-- --------------------------------------------------------

--
-- Структура таблицы `RaidModerators`
--

DROP TABLE IF EXISTS `RaidModerators`;
CREATE TABLE IF NOT EXISTS `RaidModerators` (
  `raidmoderator_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ записи о модераторе',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя',
  `raidmoderator_hide` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Признак удаления записи о модераторе',
  PRIMARY KEY (`raidmoderator_id`),
  KEY `raid_id` (`raid_id`),
  KEY `user_id` (`user_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица модераторов' AUTO_INCREMENT=26 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Raids`
--

DROP TABLE IF EXISTS `Raids`;
CREATE TABLE IF NOT EXISTS `Raids` (
  `raid_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ марш-броска',
  `raid_name` varchar(50) NOT NULL COMMENT 'Название марш-броска',
  `raid_period` varchar(50) NOT NULL COMMENT 'Сроки проведения',
  `raid_startpoint` varchar(50) DEFAULT NULL COMMENT 'Район старта марш-броска',
  `raid_finishpoint` varchar(50) DEFAULT NULL COMMENT 'Район финиша марш броска',
  `raid_ruleslink` varchar(50) DEFAULT NULL COMMENT 'Ссылка на положение',
  `raid_startlink` varchar(100) DEFAULT NULL COMMENT 'Ссылка на информацию о старте',
  `raid_folder` varchar(50) DEFAULT NULL COMMENT 'Каталог на сервере',
  `raid_registrationenddate` date DEFAULT NULL COMMENT 'Дата окончания регистрации - крайний срок заявки до 23:59 этой даты.  Наличие даты является идикатором, что  ММБ нужно показать в списке для пользователей - до этого видит только администратор',
  `raid_closedate` date DEFAULT NULL COMMENT 'Дата, с котрой марш-бросок  закрывается  на правку пользователями (е модератороами)',
  `raid_logolink` varchar(100) NOT NULL COMMENT 'Ссылка на логотип',
  `raid_znlink` varchar(100) NOT NULL COMMENT 'Ссылка на значок',
  `raid_kpwptlink` varchar(100) NOT NULL COMMENT 'Ссылка на файл с точками КП',
  `raid_legendlink` varchar(100) NOT NULL COMMENT 'Ссылка на легенду',
  `raid_ziplink` varchar(100) NOT NULL COMMENT 'Ссылка на zip файл с данными ММБ',
  `raid_noshowresult` int(1) DEFAULT NULL COMMENT 'Признак, что не надо показывать результаты',
  `raid_fileprefix` char(10) DEFAULT NULL COMMENT 'Префикс файлов, который автоматически добавляется, если файл не начинается с него',
  `raid_readonlyhoursbeforestart` int(11) DEFAULT NULL COMMENT 'За сколько часов до старта закрывается редактирование для участников',
  `raid_mapprice` int(11) NOT NULL COMMENT 'Стоимость одного комплекта карт в рублях',
  PRIMARY KEY (`raid_id`),
  UNIQUE KEY `raid_name` (`raid_name`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица марш-бросков' AUTO_INCREMENT=24 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Roles`
--

DROP TABLE IF EXISTS `Roles`;
CREATE TABLE IF NOT EXISTS `Roles` (
  `role_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ роли',
  `role_name` varchar(50) NOT NULL COMMENT 'Название роли',
  PRIMARY KEY (`role_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица ролей' AUTO_INCREMENT=5 ;

-- --------------------------------------------------------

--
-- Структура таблицы `ScanPoints`
--

DROP TABLE IF EXISTS `ScanPoints`;
CREATE TABLE IF NOT EXISTS `ScanPoints` (
  `scanpoint_id` int(11) NOT NULL AUTO_INCREMENT,
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `scanpoint_name` varchar(50) NOT NULL COMMENT 'Название места сканирования',
  `scanpoint_order` int(11) NOT NULL COMMENT 'Порядковый номер ',
  `scanpoint_hide` int(1) NOT NULL COMMENT 'ПРизнак, что место сканирования скрыто',
  PRIMARY KEY (`scanpoint_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Таблица мест сканирования для объединения физически одинаковых точек разных дист' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Sessions`
--

DROP TABLE IF EXISTS `Sessions`;
CREATE TABLE IF NOT EXISTS `Sessions` (
  `session_id` varchar(50) NOT NULL COMMENT 'Идентификатор сессии',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя',
  `connection_id` int(11) NOT NULL COMMENT 'Ключ соединения',
  `session_status` int(11) NOT NULL COMMENT 'Статус сессии (1 - превышение времени, 3 - пользователь вышел, 0 - активная сессия))',
  `session_starttime` datetime NOT NULL COMMENT 'Время старта сесии',
  `session_updatetime` datetime NOT NULL COMMENT 'Время обновления (последнего обращения к сесии',
  PRIMARY KEY (`session_id`),
  KEY `user_id` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='Таблица сессий';

-- --------------------------------------------------------

--
-- Структура таблицы `TeamLevelDismiss`
--

DROP TABLE IF EXISTS `TeamLevelDismiss`;
CREATE TABLE IF NOT EXISTS `TeamLevelDismiss` (
  `teamleveldismiss_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ схода участника в точке',
  `teamleveldismiss_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Время создания записи',
  `user_id` int(11) NOT NULL DEFAULT '0' COMMENT 'Ключ пользователя, который ввел запись',
  `device_id` int(11) NOT NULL DEFAULT '1' COMMENT 'Ключ устройства, на котором была введена запись',
  `levelpoint_id` int(11) NOT NULL COMMENT 'Ключ контрольной точки, на которую не пришел сошедший участник',
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды, в которой сошел участник',
  `teamuser_id` int(11) NOT NULL COMMENT 'Ключ участника в этой команде',
  PRIMARY KEY (`teamleveldismiss_id`),
  UNIQUE KEY `userlevelpointteamteamuser` (`user_id`,`levelpoint_id`,`team_id`,`teamuser_id`),
  KEY `user_id` (`user_id`),
  KEY `device_id` (`device_id`),
  KEY `levelpoint_id` (`levelpoint_id`),
  KEY `team_id` (`team_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица схода участников перед контрольными точками' AUTO_INCREMENT=474 ;

-- --------------------------------------------------------

--
-- Структура таблицы `TeamLevelPoints`
--

DROP TABLE IF EXISTS `TeamLevelPoints`;
CREATE TABLE IF NOT EXISTS `TeamLevelPoints` (
  `teamlevelpoint_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ данных по точке',
  `teamlevelpoint_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Время создания записи',
  `user_id` int(11) NOT NULL DEFAULT '0' COMMENT 'Ключ пользователя, который ввел запись',
  `device_id` int(11) NOT NULL COMMENT 'Ключ устройства',
  `levelpoint_id` int(11) NOT NULL COMMENT 'Ключ контрольной точки',
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды',
  `teamlevelpoint_datetime` datetime DEFAULT NULL COMMENT 'Время прхождения контрольной точки',
  `teamlevelpoint_points` varchar(150) DEFAULT NULL COMMENT 'Список взятых КП',
  `teamlevelpoint_comment` varchar(100) DEFAULT NULL COMMENT 'Комментарий к прохождению контрольной точки',
  `teamlevelpoint_duration` time DEFAULT NULL COMMENT 'Время нахождения (без штрафа)  команды между точками  ',
  `teamlevelpoint_penalty` int(11) DEFAULT NULL COMMENT 'Штраф  команды на точке (по предыдущим точкам)',
  PRIMARY KEY (`teamlevelpoint_id`),
  UNIQUE KEY `userlevelpointteam` (`user_id`,`levelpoint_id`,`team_id`),
  KEY `user_id` (`user_id`),
  KEY `device_id` (`device_id`),
  KEY `levelpoint_id` (`levelpoint_id`),
  KEY `team_id` (`team_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица прохождения командой контрольных точек' AUTO_INCREMENT=9324 ;

-- --------------------------------------------------------

--
-- Структура таблицы `TeamLevels`
--

DROP TABLE IF EXISTS `TeamLevels`;
CREATE TABLE IF NOT EXISTS `TeamLevels` (
  `teamlevel_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ результатов этапа для команды',
  `level_id` int(11) NOT NULL COMMENT 'Ключ этапа',
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды',
  `teamlevel_begtime` datetime DEFAULT NULL COMMENT 'Время старта команды на этапе',
  `teamlevel_endtime` datetime DEFAULT NULL COMMENT 'Время финиша команды на этапе',
  `teamlevel_points` varchar(150) DEFAULT NULL COMMENT 'Список взятых КП',
  `teamlevel_comment` varchar(150) DEFAULT NULL COMMENT 'Комментарий к результату команды',
  `teamlevel_progress` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0 - не вышли на этап, 1 - сошли с этапа, 2 - дошли до конца этапа',
  `teamlevel_penalty` int(11) DEFAULT NULL COMMENT 'Итоговый штраф (в минутах) команды на этапе',
  `error_id` int(11) DEFAULT NULL COMMENT 'Ключ  ошибки',
  `teamlevel_hide` tinyint(1) NOT NULL COMMENT 'Признак, что резултат команды по этапу удален',
  `teamlevel_duration` time DEFAULT NULL COMMENT 'время нахождения команды на этапе без учета штрафа',
  PRIMARY KEY (`teamlevel_id`),
  KEY `level_id` (`level_id`),
  KEY `error_id` (`error_id`),
  KEY `team_id` (`team_id`),
  KEY `teamlevel_progress` (`teamlevel_progress`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица результатов этапа для команды' AUTO_INCREMENT=9172 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Teams`
--

DROP TABLE IF EXISTS `Teams`;
CREATE TABLE IF NOT EXISTS `Teams` (
  `team_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ команды',
  `team_name` varchar(50) NOT NULL COMMENT 'Название команды',
  `distance_id` int(11) NOT NULL COMMENT 'Ключ дистанции',
  `team_num` int(11) NOT NULL COMMENT 'Номер команды внутри дистанции',
  `team_usegps` tinyint(1) NOT NULL COMMENT 'Признак, что команда использует GPS',
  `team_mapscount` int(11) NOT NULL COMMENT 'Необходимое число карт',
  `team_registerdt` datetime NOT NULL COMMENT 'Время регистрации команды (для отслеживания команд, которые зарегистрировались позже, чем  закрывалась регистрация',
  `team_confirmresult` int(1) NOT NULL COMMENT 'Команда подтвердила правильность ввода данных',
  `team_hide` tinyint(1) NOT NULL COMMENT 'Признак, что команда удалена',
  `team_greenpeace` int(11) NOT NULL COMMENT 'Отношение к политике "Нет сломанным унитазам!"',
  `team_moderatorconfirmresult` tinyint(1) NOT NULL COMMENT 'Признак, что модератор подтвердил результаты команды',
  `level_id` int(11) DEFAULT NULL COMMENT 'Ключ этапа, на который команда не стартовала',
  `team_progress` int(11) NOT NULL DEFAULT '0' COMMENT 'Сумма teamlevel_progress по всем этапам, чем она больше, тем больше этапов успешно пройдено',
  `team_result` time DEFAULT NULL COMMENT 'Результат команды с  учетом штрафа',
  `team_parentid` int(11) NOT NULL COMMENT 'Ключ команды, в которую объединили текущую',
  `team_outofrange` tinyint(1) DEFAULT NULL COMMENT 'Признак, что команда вне зачета',
  PRIMARY KEY (`team_id`),
  KEY `distance_id` (`distance_id`),
  KEY `team_result` (`team_result`),
  KEY `team_num` (`team_num`),
  KEY `level_id` (`level_id`),
  KEY `team_progress` (`team_progress`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица команд ММБ' AUTO_INCREMENT=5455 ;

-- --------------------------------------------------------

--
-- Структура таблицы `TeamUnionLogs`
--

DROP TABLE IF EXISTS `TeamUnionLogs`;
CREATE TABLE IF NOT EXISTS `TeamUnionLogs` (
  `teamunionlog_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ элемента объединения',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя, проводившего объединение',
  `teamunionlog_dt` datetime NOT NULL COMMENT 'Время изменений',
  `teamunionlog_hide` tinyint(1) NOT NULL COMMENT 'Признак удаления команды из объединения',
  `team_id` int(11) NOT NULL COMMENT 'Ключ старой команды',
  `team_parentid` int(11) DEFAULT NULL COMMENT 'Ключ новой команды',
  `union_status` int(11) NOT NULL COMMENT 'Статус объединенеия',
  PRIMARY KEY (`teamunionlog_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица истории объединения команд' AUTO_INCREMENT=78 ;

-- --------------------------------------------------------

--
-- Структура таблицы `TeamUsers`
--

DROP TABLE IF EXISTS `TeamUsers`;
CREATE TABLE IF NOT EXISTS `TeamUsers` (
  `teamuser_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ участника команды',
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды',
  `user_id` int(11) NOT NULL COMMENT 'Ключ участника',
  `teamuser_hide` tinyint(1) NOT NULL COMMENT 'Признак, что  участник удалён из команды (чтобы не делать физическое удаление)',
  `level_id` int(11) DEFAULT NULL COMMENT 'Ключ этапа на котором сошёл участник',
  `levelpoint_id` int(11) DEFAULT NULL COMMENT 'Ключ точки, в которую не явился участник',
  PRIMARY KEY (`teamuser_id`),
  KEY `team_id` (`team_id`),
  KEY `user_id` (`user_id`),
  KEY `level_id` (`level_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Участники команды' AUTO_INCREMENT=8003 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Users`
--

DROP TABLE IF EXISTS `Users`;
CREATE TABLE IF NOT EXISTS `Users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ  участника',
  `user_name` varchar(100) NOT NULL COMMENT 'ФИО участника',
  `user_email` varchar(100) NOT NULL COMMENT 'Адрес электронной почты',
  `user_password` varchar(50) NOT NULL COMMENT 'Пароль участника',
  `user_birthyear` int(11) NOT NULL COMMENT 'Год рождения',
  `user_prohibitadd` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Признак запрета добавлять в команду, которую создаёт другой пользователь',
  `user_sessionfornewpassword` varchar(50) DEFAULT NULL COMMENT 'Поле с последним идентификаторм временной сессии для смены пароля (отправки пиьсма с запросом о смене)',
  `user_sendnewpasswordrequestdt` datetime DEFAULT NULL COMMENT 'Время последней отправки письма с запросом на смену пароля',
  `user_admin` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Признак, что пользователь является администратором',
  `user_hide` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Признак, что пользователь удален',
  `user_registerdt` datetime NOT NULL COMMENT 'Время регистрации (добавления записи',
  `user_lastauthorizationdt` datetime DEFAULT NULL COMMENT 'Время последней успешной авторизации',
  `user_sendnewpassworddt` datetime DEFAULT NULL COMMENT 'Время  поледней отправки письма с паролем',
  `user_prohibitsendteamchangeinfo` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Флаг запрета высылать  письма об изменениях в команде',
  `user_prohibitsendorgmessages` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Флаг запрета высылать информационные письма от организаторов',
  `user_city` varchar(50) DEFAULT NULL COMMENT 'Город пользователя',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_email` (`user_email`),
  UNIQUE KEY `user_name` (`user_name`,`user_birthyear`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Таблица участника ММБ' AUTO_INCREMENT=4189 ;

-- --------------------------------------------------------

--
-- Структура таблицы `zeropasswordusers`
--

DROP TABLE IF EXISTS `zeropasswordusers`;
CREATE TABLE IF NOT EXISTS `zeropasswordusers` (
  `user_id` int(11) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='таблица пользователей, занесённых на старте ММБ 2012 весна';

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
