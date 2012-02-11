SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- База данных: `mmb`
--


--
-- Структура таблицы `Distances`
--

DROP TABLE IF EXISTS `Distances`;
CREATE TABLE IF NOT EXISTS `Distances` (
  `distance_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ дистанции',
  `distance_name` varchar(50) NOT NULL COMMENT 'Название дистанции',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ марш-броска',
  `distance_data` varchar(50) NOT NULL COMMENT 'Данные о дистанции',
  `distance_resultlink` varchar(50) NOT NULL COMMENT 'Ссылка на результаты дистанции',
  PRIMARY KEY (`distance_id`),
  KEY `raid_id` (`raid_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Таблица дистанций марш-бросков' AUTO_INCREMENT=27 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Errors`
--

DROP TABLE IF EXISTS `Errors`;
CREATE TABLE IF NOT EXISTS `Errors` (
  `error_id` int(11) NOT NULL COMMENT 'Ключ ошибки',
  `error_name` varchar(50) NOT NULL COMMENT 'Название ошибки',
  PRIMARY KEY (`error_id`)
) ENGINE=MyISAM DEFAULT CHARSET=cp1251 COMMENT='Таблица ошибок в результатах';

-- --------------------------------------------------------

--
-- Структура таблицы `LevelFiles`
--

DROP TABLE IF EXISTS `LevelFiles`;
CREATE TABLE IF NOT EXISTS `LevelFiles` (
  `levelfile_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ файла',
  `levelfile_name` varchar(50) NOT NULL COMMENT 'Имя файла',
  `level_id` int(11) NOT NULL COMMENT 'Клюс этапа',
  `levelfile_type` int(11) NOT NULL COMMENT 'Тип файла (1 - начало этапа, 2 - конец этапа, 3 -? )',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя, загружавшего файл',
  `levelfile_loaddate` datetime NOT NULL COMMENT 'Время загрузки',
  PRIMARY KEY (`levelfile_id`),
  KEY `user_id` (`user_id`),
  KEY `level_id` (`level_id`)
) ENGINE=MyISAM DEFAULT CHARSET=cp1251 COMMENT='Таблица csv фйалов' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `LevelFilesData`
--

DROP TABLE IF EXISTS `LevelFilesData`;
CREATE TABLE IF NOT EXISTS `LevelFilesData` (
  `levelfiledata_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ записи в файле',
  `levelfile_id` int(11) NOT NULL COMMENT 'Ключ файла',
  `levelfile_rownum` int(11) NOT NULL COMMENT 'Номер строки в файле',
  `levelfile_rowsource` varchar(200) NOT NULL COMMENT 'Строка источник',
  `levelfile_timemark` datetime DEFAULT NULL COMMENT 'Время',
  `levelfile_teamnum` int(11) DEFAULT NULL COMMENT 'Номер команды',
  `levelfile_teamname` varchar(50) DEFAULT NULL COMMENT 'Название команды',
  `levelfile_points` varchar(50) DEFAULT NULL COMMENT 'Невзятые КП',
  `levelfile_comments` varchar(150) DEFAULT NULL COMMENT 'Комментарий',
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды',
  `error_id` int(11) NOT NULL COMMENT 'Ключ ошибки',
  PRIMARY KEY (`levelfiledata_id`),
  KEY `levelfile_id` (`levelfile_id`),
  KEY `team_id` (`team_id`)
) ENGINE=MyISAM DEFAULT CHARSET=cp1251 COMMENT='Таблица данных файлов' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Levels`
--

DROP TABLE IF EXISTS `Levels`;
CREATE TABLE IF NOT EXISTS `Levels` (
  `level_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ этапа',
  `level_name` varchar(50) DEFAULT NULL COMMENT 'Название этапа',
  `level_order` int(11) NOT NULL COMMENT 'Порядок этапов внутри дистанции',
  `distance_id` int(11) NOT NULL COMMENT 'Ключ дистанции',
  `level_begtime` datetime NOT NULL COMMENT 'Минимальное время старта на этап',
  `level_maxbegtime` datetime NOT NULL COMMENT 'Максимально возможное время старта',
  `level_minendtime` datetime NOT NULL COMMENT 'Минимально возможное время финиша',
  `level_endtime` datetime NOT NULL COMMENT 'Время закрытия этапа',
  `level_starttype` tinyint(1) NOT NULL COMMENT 'Тип старта (1 - по готовности, 2 - общий, 3 - )в момент фигиша на предыдущем этапе)',
  `level_pointnames` varchar(150) NOT NULL COMMENT 'Спсиок названий КП этапа',
  `level_pointpenalties` varchar(150) NOT NULL COMMENT 'Список штрафов в минутах за невзятие КП  (в том же порядке, что и список КП)',
  `level_maplink` varchar(100) DEFAULT NULL COMMENT 'Ссылка на карту этапа',
  PRIMARY KEY (`level_id`),
  KEY `distance_id` (`distance_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Таблица этапов' AUTO_INCREMENT=8 ;

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
  PRIMARY KEY (`raidmoderator_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Таблица модераторов' AUTO_INCREMENT=6 ;

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
  `raid_registrationbegdate` date DEFAULT NULL COMMENT 'Дата начала регистрации',
  `raid_registrationenddate` date DEFAULT NULL COMMENT 'Дата окончания регистрации',
  `raid_resultpublicationdate` date DEFAULT NULL COMMENT 'Дата публикации результатов',
  `raid_closedate` datetime DEFAULT NULL COMMENT 'Дата, с котрой марш-бросок  закрывается  на правку пользователями (е модератороами)',
  PRIMARY KEY (`raid_id`),
  UNIQUE KEY `raid_name` (`raid_name`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Таблица марш-бросков' AUTO_INCREMENT=20 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Roles`
--

DROP TABLE IF EXISTS `Roles`;
CREATE TABLE IF NOT EXISTS `Roles` (
  `role_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ роли',
  `role_name` varchar(50) NOT NULL COMMENT 'Название роли',
  PRIMARY KEY (`role_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Таблица ролей' AUTO_INCREMENT=5 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Sessions`
--

DROP TABLE IF EXISTS `Sessions`;
CREATE TABLE IF NOT EXISTS `Sessions` (
  `session_id` varchar(50) NOT NULL COMMENT 'Идентификатор сессии',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя',
  `connection_id` int(11) NOT NULL COMMENT 'Ключ соединения',
  `session_status` int(11) NOT NULL COMMENT 'Статус сессии',
  `session_starttime` datetime NOT NULL COMMENT 'Время старта сесии',
  `session_updatetime` datetime NOT NULL COMMENT 'Время обновления (последнего обращения к сесии',
  PRIMARY KEY (`session_id`)
) ENGINE=MyISAM DEFAULT CHARSET=cp1251 COMMENT='Таблица сессий';

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
  `teamlevel_points` varchar(150) DEFAULT NULL COMMENT 'Список невзятых КП',
  `teamlevel_comment` varchar(150) DEFAULT NULL COMMENT 'Комментарий к результату команды',
  `teamlevel_penalty` int(11) DEFAULT NULL COMMENT 'Итоговый штраф (в минутах) команды на этапе',
  `error_id` int(11) DEFAULT NULL COMMENT 'Ключ  ошибки',
  `teamlevel_hide` tinyint(1) NOT NULL COMMENT 'Признак, что резултат команды по этапу удален',
  PRIMARY KEY (`teamlevel_id`),
  KEY `level_id` (`level_id`),
  KEY `error_id` (`error_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Таблица результатов этапа для команды' AUTO_INCREMENT=30 ;

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
  `team_result` time NOT NULL COMMENT 'Результат команды с  учетом штрафа',
  PRIMARY KEY (`team_id`),
  KEY `distance_id` (`distance_id`),
  KEY `team_result` (`team_result`),
  KEY `team_num` (`team_num`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Таблица команд ММБ' AUTO_INCREMENT=67 ;

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
  PRIMARY KEY (`teamuser_id`),
  KEY `team_id` (`team_id`),
  KEY `user_id` (`user_id`),
  KEY `level_id` (`level_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Участники команды' AUTO_INCREMENT=110 ;

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
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_email` (`user_email`),
  UNIQUE KEY `user_name` (`user_name`,`user_birthyear`)
) ENGINE=MyISAM  DEFAULT CHARSET=cp1251 COMMENT='Таблица участника ММБ' AUTO_INCREMENT=51 ;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
