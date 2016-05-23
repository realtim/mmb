
SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";



--
-- Структура таблицы `Devices`
--

DROP TABLE IF EXISTS `Devices`;
CREATE TABLE IF NOT EXISTS `Devices` (
  `device_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ устройства',
  `device_name` varchar(100) NOT NULL COMMENT 'Название устройства',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя -владельца',
  PRIMARY KEY (`device_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица устройств' AUTO_INCREMENT=150 ;

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
  `distance_length` int(11) DEFAULT NULL COMMENT 'Дина дистнации в километрах (используется для определения коэффициента в рейтинге)',
  `distance_rankcoeff` float DEFAULT NULL COMMENT 'Коэффициент дистанции в рейтинге (отношение длины текущей к максимальной)',
  PRIMARY KEY (`distance_id`),
  KEY `raid_id` (`raid_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица дистанций марш-бросков' AUTO_INCREMENT=35 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Errors`
--

DROP TABLE IF EXISTS `Errors`;
CREATE TABLE IF NOT EXISTS `Errors` (
  `error_id` int(11) NOT NULL COMMENT 'Ключ ошибки',
  `error_name` varchar(100) NOT NULL COMMENT 'Название ошибки',
  `error_manual` int(11) NOT NULL COMMENT 'ПРизнак, что ошибка ставится оператором',
  PRIMARY KEY (`error_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Таблица ошибок в результатах';

-- --------------------------------------------------------

--
-- Структура таблицы `FileTypes`
--

DROP TABLE IF EXISTS `FileTypes`;
CREATE TABLE IF NOT EXISTS `FileTypes` (
  `filetype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа',
  `filetype_name` varchar(100) NOT NULL COMMENT 'Название типа',
  PRIMARY KEY (`filetype_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Типы файлов' AUTO_INCREMENT=12 ;

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
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица ссылок на карты этапов' AUTO_INCREMENT=32 ;

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
  `levelpoint_id` int(11) NOT NULL COMMENT 'Ключ точки, в которой будет производится учёт этого облака',
  PRIMARY KEY (`levelpointdiscount_id`),
  UNIQUE KEY `levelpoint_id` (`levelpoint_id`),
  KEY `distance_id` (`distance_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='Таблица групп (интервалов) КП с амнистией' AUTO_INCREMENT=13 ;

-- --------------------------------------------------------

--
-- Структура таблицы `LevelPoints`
--

DROP TABLE IF EXISTS `LevelPoints`;
CREATE TABLE IF NOT EXISTS `LevelPoints` (
  `levelpoint_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ контрольной точки',
  `levelpoint_name` varchar(50) NOT NULL COMMENT 'Название контрольной точки, на которой ведется протокол',
  `pointtype_id` int(11) NOT NULL COMMENT 'Ключ типа точки',
  `levelpoint_order` int(11) NOT NULL COMMENT 'Порядковый номер контрольной точки на всей дистанции',
  `levelpoint_hide` int(1) NOT NULL DEFAULT '0' COMMENT 'Признак, что точка скрыта',
  `distance_id` int(11) DEFAULT NULL COMMENT 'Ключ дистанции',
  `levelpoint_penalty` int(11) DEFAULT NULL COMMENT 'Штраф за невзятие КП в минутах',
  `levelpoint_mindatetime` datetime DEFAULT NULL COMMENT 'Минимальное время, которое может быть у команды на точке',
  `levelpoint_maxdatetime` datetime DEFAULT NULL COMMENT 'Максимальное время, которое может быть у команды на точке',
  `scanpoint_id` int(11) NOT NULL COMMENT 'Ключ точки сканирования',
  `level_id` int(11) NOT NULL,
  PRIMARY KEY (`levelpoint_id`),
  KEY `pointtype_id` (`pointtype_id`),
  KEY `levelpoint_order` (`levelpoint_order`),
  KEY `distance_id` (`distance_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Контрольные точки на этапах' AUTO_INCREMENT=1632 ;

-- --------------------------------------------------------

--
-- Структура таблицы `LinkTypes`
--

DROP TABLE IF EXISTS `LinkTypes`;
CREATE TABLE IF NOT EXISTS `LinkTypes` (
  `linktype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа ссылки',
  `linktype_name` varchar(50) NOT NULL COMMENT 'Название типа ссылки',
  `linktype_hide` int(11) NOT NULL COMMENT 'Тип скрыт',
  `linktype_textonly` int(11) NOT NULL COMMENT 'Признак, что впечателния содержат только текст',
  `linktype_order` int(11) NOT NULL COMMENT 'Порядок вывода на странице впечатлений',
  PRIMARY KEY (`linktype_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Справочник типов ссылок пользователей (отзывы, впечатления)' AUTO_INCREMENT=7 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Logs`
--

DROP TABLE IF EXISTS `Logs`;
CREATE TABLE IF NOT EXISTS `Logs` (
  `logs_id` int(11) NOT NULL AUTO_INCREMENT,
  `logs_dt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `logs_level` enum('critical','error','warning','info','debug','trace') NOT NULL DEFAULT 'critical',
  `user_id` int(11) DEFAULT NULL,
  `logs_duration` int(11) DEFAULT NULL,
  `logs_operation` tinytext,
  `logs_message` text,
  PRIMARY KEY (`logs_id`),
  KEY `user_id` (`user_id`),
  KEY `logs_level` (`logs_level`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=4971 ;

-- --------------------------------------------------------

--
-- Структура таблицы `PointTypes`
--

DROP TABLE IF EXISTS `PointTypes`;
CREATE TABLE IF NOT EXISTS `PointTypes` (
  `pointtype_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ типа точки',
  `pointtype_name` varchar(50) NOT NULL COMMENT 'Название типа точки',
  PRIMARY KEY (`pointtype_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Типы контрольных точек' AUTO_INCREMENT=6 ;

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
  PRIMARY KEY (`raidfile_id`),
  KEY `raid_id` (`raid_id`),
  KEY `filetype_id` (`filetype_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='таблица файлов для ММБ (положения, логотипы, карты)' AUTO_INCREMENT=558 ;

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
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица модераторов' AUTO_INCREMENT=34 ;

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
  `raid_notstartfee` int(11) DEFAULT NULL COMMENT 'Штраф за неявку на старт в предыдущий марш-бросок',
  `raid_nostartprice` int(11) NOT NULL COMMENT 'Стоимость неявки на старт (при условии, тчо участник не был удален)',
  `raid_teamslimit` int(11) NOT NULL COMMENT 'Лимит команд на марш-броске',
  PRIMARY KEY (`raid_id`),
  UNIQUE KEY `raid_name` (`raid_name`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица марш-бросков' AUTO_INCREMENT=28 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Roles`
--

DROP TABLE IF EXISTS `Roles`;
CREATE TABLE IF NOT EXISTS `Roles` (
  `role_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ роли',
  `role_name` varchar(50) NOT NULL COMMENT 'Название роли',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица ролей' AUTO_INCREMENT=5 ;

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
  PRIMARY KEY (`scanpoint_id`),
  KEY `raid_id` (`raid_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица мест сканирования для объединения физически одинаковых точек разных дист' AUTO_INCREMENT=29 ;

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
  `teamuser_id` int(11) NOT NULL COMMENT 'Ключ участника в этой команде',
  PRIMARY KEY (`teamleveldismiss_id`),
  KEY `device_id` (`device_id`),
  KEY `levelpoint_id` (`levelpoint_id`),
  KEY `teamuser_id` (`teamuser_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица схода участников перед контрольными точками' AUTO_INCREMENT=1801 ;

-- --------------------------------------------------------

--
-- Структура таблицы `TeamLevelPoints`
--

DROP TABLE IF EXISTS `TeamLevelPoints`;
CREATE TABLE IF NOT EXISTS `TeamLevelPoints` (
  `teamlevelpoint_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ данных по точке',
  `teamlevelpoint_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Время создания записи',
  `user_id` int(11) NOT NULL DEFAULT '0' COMMENT 'Ключ пользователя, который ввел запись',
  `device_id` int(11) NOT NULL COMMENT 'Ключ устройства',
  `levelpoint_id` int(11) NOT NULL COMMENT 'Ключ контрольной точки',
  `team_id` int(11) NOT NULL COMMENT 'Ключ команды',
  `teamlevelpoint_datetime` datetime DEFAULT NULL COMMENT 'Время прхождения контрольной точки',
  `teamlevelpoint_comment` varchar(100) DEFAULT NULL COMMENT 'Комментарий к прохождению контрольной точки',
  `teamlevelpoint_duration` time DEFAULT NULL COMMENT 'Время нахождения (без штрафа)  команды между точками  ',
  `teamlevelpoint_penalty` int(11) DEFAULT NULL COMMENT 'Штраф  команды на точке (по предыдущим точкам)',
  `error_id` int(11) DEFAULT NULL COMMENT 'Ключ ошибки',
  `teamlevelpoint_result` time DEFAULT NULL COMMENT 'Результат команды в точке',
  PRIMARY KEY (`teamlevelpoint_id`),
  KEY `levelpoint_id` (`levelpoint_id`),
  KEY `team_id` (`team_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица прохождения командой контрольных точек' AUTO_INCREMENT=1046175 ;

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
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица результатов этапа для команды' AUTO_INCREMENT=15970 ;

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
  `team_maxlevelpointorderdone` int(11) DEFAULT NULL COMMENT 'Максимальный порядковый номер взятой точки',
  `team_result` time DEFAULT NULL COMMENT 'Результат команды с  учетом штрафа',
  `team_parentid` int(11) DEFAULT NULL COMMENT 'Ключ команды, в которую объединили текущую',
  `team_outofrange` tinyint(1) DEFAULT NULL COMMENT 'Признак, что команда вне зачета',
  `team_importkey` int(11) DEFAULT NULL COMMENT 'Ключ команды при импорте',
  `team_importattempt` int(11) DEFAULT NULL COMMENT 'Попытка импорта',
  `team_minlevelpointorderwitherror` int(11) DEFAULT NULL COMMENT 'Минимальный порядковый номер точки с ошибкой',
  `team_comment` varchar(150) DEFAULT NULL COMMENT 'Комментарий (рассчитывается, как конкатенция комментариев по всем точкам',
  `team_skippedlevelpoint` varchar(250) DEFAULT NULL COMMENT 'Список невзятых КП',
  `team_waitdt` datetime DEFAULT NULL COMMENT 'Команда попала в список ожидания',
  `team_donelevelpoint` varchar(150) DEFAULT NULL COMMENT 'Время взятия точек',
  PRIMARY KEY (`team_id`),
  KEY `distance_id` (`distance_id`),
  KEY `team_result` (`team_result`),
  KEY `team_num` (`team_num`),
  KEY `team_progress` (`team_maxlevelpointorderdone`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица команд ММБ' AUTO_INCREMENT=28950 ;

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
  PRIMARY KEY (`teamunionlog_id`),
  KEY `user_id` (`user_id`),
  KEY `team_id` (`team_id`),
  KEY `team_parentid` (`team_parentid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица истории объединения команд' AUTO_INCREMENT=397 ;

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
  `teamuser_importkey` int(11) DEFAULT NULL COMMENT 'Ключ участника команды при  импорте',
  `teamuser_importattempt` int(11) DEFAULT NULL COMMENT 'Попытка импорта',
  `userunionlog_id` int(11) DEFAULT NULL COMMENT 'Ключ журнала объединения пользоватлей',
  `teamuser_rank` decimal(6,5) DEFAULT NULL COMMENT 'Рейтинг участника',
  `teamuser_notstartraidid` int(11) DEFAULT NULL COMMENT 'Ключ марш-броска, на котороый пользователь заявился, но не пришёл',
  `teamuser_changedt` datetime DEFAULT NULL COMMENT 'Время изменения данных',
  PRIMARY KEY (`teamuser_id`),
  KEY `team_id` (`team_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Участники команды' AUTO_INCREMENT=30391 ;

-- --------------------------------------------------------

--
-- Структура таблицы `UserLinks`
--

DROP TABLE IF EXISTS `UserLinks`;
CREATE TABLE IF NOT EXISTS `UserLinks` (
  `userlink_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ ссылки от пользователя',
  `linktype_id` int(11) NOT NULL COMMENT 'ключ типа ссылки',
  `userlink_hide` tinyint(1) NOT NULL COMMENT 'Ссылка скрыта',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя',
  `raid_id` int(11) NOT NULL COMMENT 'Ключ ММБ',
  `userlink_url` varchar(100) NOT NULL COMMENT 'URL ссылки ',
  `userlink_name` varchar(100) NOT NULL COMMENT 'Название ссылки',
  PRIMARY KEY (`userlink_id`),
  KEY `user_id` (`user_id`),
  KEY `raid_id` (`raid_id`),
  KEY `linktype_id` (`linktype_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица ссылок на впечатления  пользхователей' AUTO_INCREMENT=1330 ;

-- --------------------------------------------------------

--
-- Структура таблицы `Users`
--

DROP TABLE IF EXISTS `Users`;
CREATE TABLE IF NOT EXISTS `Users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ  участника',
  `user_name` varchar(100) NOT NULL COMMENT 'ФИО участника',
  `user_birthyear` int(11) NOT NULL COMMENT 'Год рождения',
  `user_hide` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Признак, что пользователь удален',
  `user_registerdt` datetime NOT NULL COMMENT 'Время регистрации (добавления записи',
  `user_city` varchar(50) DEFAULT NULL COMMENT 'Город пользователя',
  `userunionlog_id` int(11) DEFAULT NULL COMMENT 'Ключ записи об объединении с другим пользователем',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_name` (`user_name`,`user_birthyear`,`userunionlog_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица участника ММБ' AUTO_INCREMENT=11117 ;

-- --------------------------------------------------------

--
-- Структура таблицы `UserUnionLogs`
--

DROP TABLE IF EXISTS `UserUnionLogs`;
CREATE TABLE IF NOT EXISTS `UserUnionLogs` (
  `userunionlog_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Ключ журнала объединения пользователей',
  `user_id` int(11) NOT NULL COMMENT 'Ключ пользователя (нового)',
  `user_parentid` int(11) NOT NULL COMMENT 'Ключ пользователя (старого)',
  `union_status` int(11) NOT NULL COMMENT 'Статус объединения (0 - отклонено, 1 - запрос, 2 - объединены, 3 - отмена объединения ',
  `userunionlog_dt` datetime NOT NULL COMMENT 'Время запроса на объединение',
  `userunionlog_comment` varchar(100) DEFAULT NULL COMMENT 'Комментарий',
  PRIMARY KEY (`userunionlog_id`),
  KEY `user_id` (`user_id`),
  KEY `user_parentid` (`user_parentid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Таблица объединений пользователей' AUTO_INCREMENT=474 ;

--
-- Ограничения внешнего ключа сохраненных таблиц
--

--
-- Ограничения внешнего ключа таблицы `Devices`
--
ALTER TABLE `Devices`
  ADD CONSTRAINT `Devices_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Ограничения внешнего ключа таблицы `Distances`
--
ALTER TABLE `Distances`
  ADD CONSTRAINT `Distances_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`);

--
-- Ограничения внешнего ключа таблицы `LevelPointDiscounts`
--
ALTER TABLE `LevelPointDiscounts`
  ADD CONSTRAINT `LevelPointDiscounts_ibfk_1` FOREIGN KEY (`distance_id`) REFERENCES `Distances` (`distance_id`),
  ADD CONSTRAINT `LevelPointDiscounts_ibfk_2` FOREIGN KEY (`levelpoint_id`) REFERENCES `LevelPoints` (`levelpoint_id`);

--
-- Ограничения внешнего ключа таблицы `LevelPoints`
--
ALTER TABLE `LevelPoints`
  ADD CONSTRAINT `LevelPoints_ibfk_1` FOREIGN KEY (`pointtype_id`) REFERENCES `PointTypes` (`pointtype_id`),
  ADD CONSTRAINT `LevelPoints_ibfk_2` FOREIGN KEY (`distance_id`) REFERENCES `Distances` (`distance_id`);

--
-- Ограничения внешнего ключа таблицы `Logs`
--
ALTER TABLE `Logs`
  ADD CONSTRAINT `Logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Ограничения внешнего ключа таблицы `RaidFiles`
--
ALTER TABLE `RaidFiles`
  ADD CONSTRAINT `RaidFiles_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`),
  ADD CONSTRAINT `RaidFiles_ibfk_2` FOREIGN KEY (`filetype_id`) REFERENCES `FileTypes` (`filetype_id`);

--
-- Ограничения внешнего ключа таблицы `RaidModerators`
--
ALTER TABLE `RaidModerators`
  ADD CONSTRAINT `RaidModerators_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`),
  ADD CONSTRAINT `RaidModerators_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Ограничения внешнего ключа таблицы `ScanPoints`
--
ALTER TABLE `ScanPoints`
  ADD CONSTRAINT `ScanPoints_ibfk_1` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`);

--
-- Ограничения внешнего ключа таблицы `TeamLevelDismiss`
--
ALTER TABLE `TeamLevelDismiss`
  ADD CONSTRAINT `TeamLevelDismiss_ibfk_1` FOREIGN KEY (`device_id`) REFERENCES `Devices` (`device_id`),
  ADD CONSTRAINT `TeamLevelDismiss_ibfk_2` FOREIGN KEY (`levelpoint_id`) REFERENCES `LevelPoints` (`levelpoint_id`),
  ADD CONSTRAINT `TeamLevelDismiss_ibfk_3` FOREIGN KEY (`teamuser_id`) REFERENCES `TeamUsers` (`teamuser_id`);

--
-- Ограничения внешнего ключа таблицы `TeamLevelPoints`
--
ALTER TABLE `TeamLevelPoints`
  ADD CONSTRAINT `TeamLevelPoints_ibfk_1` FOREIGN KEY (`levelpoint_id`) REFERENCES `LevelPoints` (`levelpoint_id`),
  ADD CONSTRAINT `TeamLevelPoints_ibfk_2` FOREIGN KEY (`team_id`) REFERENCES `Teams` (`team_id`);

--
-- Ограничения внешнего ключа таблицы `Teams`
--
ALTER TABLE `Teams`
  ADD CONSTRAINT `Teams_ibfk_1` FOREIGN KEY (`distance_id`) REFERENCES `Distances` (`distance_id`);

--
-- Ограничения внешнего ключа таблицы `TeamUnionLogs`
--
ALTER TABLE `TeamUnionLogs`
  ADD CONSTRAINT `TeamUnionLogs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`),
  ADD CONSTRAINT `TeamUnionLogs_ibfk_2` FOREIGN KEY (`team_id`) REFERENCES `Teams` (`team_id`);

--
-- Ограничения внешнего ключа таблицы `TeamUsers`
--
ALTER TABLE `TeamUsers`
  ADD CONSTRAINT `TeamUsers_ibfk_1` FOREIGN KEY (`team_id`) REFERENCES `Teams` (`team_id`),
  ADD CONSTRAINT `TeamUsers_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

--
-- Ограничения внешнего ключа таблицы `UserLinks`
--
ALTER TABLE `UserLinks`
  ADD CONSTRAINT `UserLinks_ibfk_1` FOREIGN KEY (`linktype_id`) REFERENCES `LinkTypes` (`linktype_id`),
  ADD CONSTRAINT `UserLinks_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`),
  ADD CONSTRAINT `UserLinks_ibfk_3` FOREIGN KEY (`raid_id`) REFERENCES `Raids` (`raid_id`);

--
-- Ограничения внешнего ключа таблицы `UserUnionLogs`
--
ALTER TABLE `UserUnionLogs`
  ADD CONSTRAINT `UserUnionLogs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
