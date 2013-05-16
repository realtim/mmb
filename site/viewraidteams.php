<?php
// +++++++++++ Показ списка команд марш-броска ++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        // Посмотреть профиль пользователя
	function ViewUserInfo(userid)
	{ 
	  document.RaidTeamsForm.UserId.value = userid;
	  document.RaidTeamsForm.action.value = 'UserInfo';
	  document.RaidTeamsForm.submit();
	}



	// Посмотреть профиль команды
	function ViewTeamInfo(teamid)
	{ 
	  document.RaidTeamsForm.TeamId.value = teamid;
	  document.RaidTeamsForm.action.value = "TeamInfo";
	  document.RaidTeamsForm.submit();
	}


        // Смена сортировки
	function OrderTypeChange()
	{ 
            if (this.value=='Num')
            {
                 document.RaidTeamsForm.LevelId.disabled=true;
                 document.RaidTeamsForm.ResultViewMode.disabled=true;
            }else{
                 document.RaidTeamsForm.LevelId.disabled=false;
                 document.RaidTeamsForm.ResultViewMode.disabled=false;
            }
	    document.RaidTeamsForm.action.value = "ViewRaidTeams";          
	    document.RaidTeamsForm.submit();
	  
	}

	// Фильтр по дистанции
	function DistanceIdChange()
	{ 
          document.RaidTeamsForm.action.value = "ViewRaidTeams";          
	  document.RaidTeamsForm.submit();
        } 

	// Фильтр по этапу
	function LevelIdChange()
	{ 
          document.RaidTeamsForm.action.value = "ViewRaidTeams";          
	  document.RaidTeamsForm.submit();
         } 


	// Формат вывода результатов
	function ResultViewModeChange()
	{ 
          document.RaidTeamsForm.action.value = "ViewRaidTeams";          
	  document.RaidTeamsForm.submit();
         } 

        // Выгрузка данных для анализа
	function JsonExport()
	{ 
		document.RaidTeamsForm.action.value = "JsonExport";
    	        document.RaidTeamsForm.submit();
	}




</script>
<!-- Конец вывода javascrpit -->


</script>

<?php



    // функция преобразования вывода данных
    function ConvertTeamLevelPoints2 ($LevelPointNames,$LevelPointPenalties,$TeamLevelPoints,$LevelId)
    {
	
	  $Names = explode(',', $LevelPointNames);
	  $Penalties = explode(',', $LevelPointPenalties);

	  if (count($Names) <> count($Penalties)) 
	  {
           print('Ошибка данных по КП'."\r\n");
	   return;
	  }

	  if (!empty($TeamLevelPoints))
	  {
	    $TeamPoints = explode(',', $TeamLevelPoints);
	  }	

	  if (!empty($TeamLevelPoints) and  count($Names) <> count($TeamPoints))
	  {	
           print('Ошибка данных по КП'."\r\n");
	   return;
	  }

	  $PointString = '';
	  for ($i = 0; $i < count($Names); $i++)
	  {
	    if ($TeamPoints[$i]==1)
	    {
              $PointString = $PointString.' '.$Names[$i];
	    } 
	  } 

          if (trim($PointString) == '')
          {
            $PointString = '&nbsp;';
          }
	  print(trim($PointString));

    return;	
    }
    // Конец функции вывода данных по КП



// функция возвращает для команды невзятые на этапе КП
    function InvertTeamLevelPoints ($LevelPointNames,$TeamLevelPoints,$LevelId)
    {
	
	  if (empty($TeamLevelPoints))
          {
	    return;
	  }

	  $Names = explode(',', $LevelPointNames);
	  $Points = explode(',', $TeamLevelPoints);

	  if (count($Names) <> count($Points)) 
	  {
           print('Ошибка данных по КП'."\r\n");
	   return;
	  }


	  $PointString = '';
	  for ($i = 0; $i < count($Names); $i++)
	  {
	    if ($Points[$i]==0)
	    {
              $PointString = $PointString.' '.$Names[$i];
	    } 
	  } 

          if (trim($PointString) == '')
          {
            $PointString = '&nbsp;';
          }
	  print(trim($PointString));

    return;	
    }
    // Конец функции вывода невзятых КП



        // Проверяем, что передали  идентификатор ММБ
        if ($RaidId <= 0)
	{
		    $statustext = 'Не указан ММБ';
	  	    $alert = 0;
		    return;
	
	}


?>	
         <form  name = "RaidTeamsForm"  action = "<? echo $MyPHPScript; ?>" method = "post">
	 <input type = "hidden" name = "sessionid" value = "<? echo $SessionId; ?>">
         <input type = "hidden" name = "action" value = "ViewRaidTeams">
         <input type = "hidden" name = "TeamId" value = "0">
         <input type = "hidden" name = "UserId" value = "0">
         <input type = "hidden" name = "RaidId" value = "<? echo $RaidId; ?>">

<?

               $TabIndex = 0;
		$DisabledText = '';

                // Разбираемся с сортировкой
                if (isset($_REQUEST['OrderType'])) $OrderType = $_REQUEST['OrderType']; else $OrderType = "";
		if (($OrderType == 'Errors') && !$Administrator && !$Moderator) $OrderType = "";
		$OrderString = '';


		  $sql = "select COALESCE(r.raid_ruleslink, '') as raid_ruleslink,
                                 COALESCE(r.raid_startlink, '') as raid_startlink,
                                 COALESCE(r.raid_kpwptlink, '') as raid_kpwptlink,
                                 COALESCE(r.raid_legendlink, '') as raid_legendlink,
                                 COALESCE(r.raid_ziplink, '') as raid_ziplink,
                                 COALESCE(r.raid_znlink, '') as raid_znlink,
				 raid_registrationenddate,  raid_closedate
			  from  Raids r
			  where r.raid_id = ".$RaidId."
                          "; 
            
		  $Result = MySqlQuery($sql);
		  $Row = mysql_fetch_assoc($Result);
		  mysql_free_result($Result);
                $RaidRulesLink = trim($Row['raid_ruleslink']);
                $RaidStartLink = trim($Row['raid_startlink']);
                $RaidKpWptLink = trim($Row['raid_kpwptlink']);
                $RaidLegendLink = trim($Row['raid_legendlink']);
                $RaidZipLink = trim($Row['raid_ziplink']);
                $RaidZnLink = trim($Row['raid_znlink']);
                $RaidRegisterEndDt = $Row['raid_registrationenddate'];
                $RaidCloseDt = $Row['raid_closedate'];
   

                // если порядок не задан смотрим на соотношение временени публикации и текущего
                if  (empty($OrderType))
                {
                  if ($RaidStage > 3)
		  {
		   // Прповеряем, что внесено хотя бы 30 результатов (может нужна другая проверка)
		  
		    $sql = " select count(*) as raid_teamlevelcount
			    from  TeamLevels tl 
				  inner join Levels l 
				  on tl.level_id = l.level_id 
                                  inner join Teams t
                                  on t.team_id = tl.team_id 
				  inner join  Distances d 
				  on t.distance_id = d.distance_id
			    where tl.teamlevel_hide = 0 
                                 and d.raid_id = ".$RaidId." 
                                 and tl.teamlevel_progress > 0
                                 and t.team_hide = 0
			    ";

		     $Result = MySqlQuery($sql);
		     $Row = mysql_fetch_assoc($Result);
		     mysql_free_result($Result);
                     $RaidTeamLevelCount = (int)$Row['raid_teamlevelcount'];
		  
		     // Смотрим число загруженных результатов
		     if  ($RaidTeamLevelCount > 30)
                     {		  
		        $OrderType = "Place";
		     } else {
	           	$OrderType = "Num";
		     }
		      
		  } else {
		   $OrderType = "Num";
		  }
		  // Конец разбора сортировки по умолчанию
		   
                }

            	print('<div align = "left" style = "font-size: 80%;">'."\r\n");
		print('Сортировать по '."\r\n");
		print('<select name="OrderType" style = "margin-left: 10px; margin-right: 20px;" 
                               onchange = "OrderTypeChange();"  tabindex = "'.(++$TabIndex).'" '.$DisabledText.'>'."\r\n"); 
	        print('<option value = "Num" '.($OrderType == 'Num' ? 'selected' :'').' >убыванию номера'."\r\n");
		if ($RaidStage > 3)
		{
	            print('<option value = "Place" '.($OrderType == 'Place' ? 'selected' :'').' >возрастанию места'."\r\n");
		}
		if ($Administrator || $Moderator)
		{
	            print('<option value = "Errors" '.($OrderType == 'Errors' ? 'selected' :'').' >наличию ошибок'."\r\n");
		}
	        print('</select>'."\r\n");  

		print('Фильтровать: '."\r\n"); 

	        $sql = "select distance_id, distance_name
                        from  Distances where raid_id = ".$RaidId." order by distance_name"; 
		//echo 'sql '.$sql;
		$Result = MySqlQuery($sql);
                
		print('<select name="DistanceId" style = "margin-left: 10px; margin-right: 5px;" 
                               onchange = "DistanceIdChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
                $distanceselected =  (0 == $_REQUEST['DistanceId'] ? 'selected' : '');
		  print('<option value = "0" '.$$distanceselected.' >дистанцию'."\r\n");
		if (!isset($_REQUEST['DistanceId'])) $_REQUEST['DistanceId'] = "";
	        while ($Row = mysql_fetch_assoc($Result))
		{
		  $distanceselected = ($Row['distance_id'] == $_REQUEST['DistanceId']  ? 'selected' : '');
		  print('<option value = "'.$Row['distance_id'].'" '.$distanceselected.' >'.$Row['distance_name']."\r\n");
		}
		print('</select>'."\r\n");  
		mysql_free_result($Result);		

		// Определяем, можно ли показывать пользователю информацию об этапах дистанции
		$LevelDataVisible = CanViewResults($Administrator, $Moderator, $RaidStage);
		if (!isset($_REQUEST['LevelId'])) $_REQUEST['LevelId'] = "";
		if ($LevelDataVisible)
		{
		    $sql = "select level_id, d.distance_name, CONCAT(trim(level_name), ' (', trim(d.distance_name), ')') as level_name
                            from  Levels l
                                inner join Distances d
                                on l.distance_id = d.distance_id
                            where d.raid_id = ".$RaidId;
                    if (!empty($_REQUEST['DistanceId']))
                    {
			$sql = $sql." and d.distance_id = ".$_REQUEST['DistanceId'];
                    }
                    $sql = $sql." order by d.distance_name, l.level_order ";
		    //  echo 'sql '.$sql;
		    $Result = MySqlQuery($sql);

		    print('<select name="LevelId" '.((($OrderType=='Num') || ($OrderType=='Errors')) ? 'disabled' : '').'
                                style = "margin-left: 5px; margin-right: 10px; width: 150px;"
                                onchange = "LevelIdChange();" tabindex = "'.(++$TabIndex).'" >'."\r\n");
	            $levelselected =  ((0 == $_REQUEST['LevelId'] or $OrderType=='Num') ? 'selected' : '');
		    print('<option value = "0" '.$levelselected.' >этап'."\r\n");

		    while ($Row = mysql_fetch_assoc($Result))
		    {
			$levelselected = (($Row['level_id'] == $_REQUEST['LevelId'] and $OrderType<>'Num' )? 'selected' : '');
			print('<option value = "'.$Row['level_id'].'" '.$levelselected.' >'.$Row['level_name']."\r\n");
		    }
		    mysql_free_result($Result);
	            print('</select>'."\r\n");

                    // Режим отображения результатов
                    if (isset($_REQUEST['ResultViewMode'])) $ResultViewMode = $_REQUEST['ResultViewMode']; else $ResultViewMode = "";
		    

                    // Сбрасываем режим отображения, если задан этап
                    if ($_REQUEST['LevelId'] > 0) {
                        $ResultViewMode = '';
                    }


           	     print('Отображать '."\r\n");
		     print('<select name="ResultViewMode" style = "margin-left: 10px; margin-right: 20px;" '.((($OrderType=='Num') || ($OrderType=='Errors') || ($_REQUEST['LevelId'] <> 0)) ? 'disabled' : '').'
                             onchange = "ResultViewModeChange();"  tabindex = "'.(++$TabIndex).'" '.$DisabledText.'>'."\r\n"); 
	   	     print('<option value = "Short" '.($ResultViewMode == 'Short' ? 'selected' :'').' >кратко'."\r\n");
	   	     print('<option value = "WithLevels" '.($ResultViewMode == 'WithLevels' ? 'selected' :'').' >с этапами'."\r\n");
		     print('</select>'."\r\n");  

		}

		print('</div>'."\r\n");
            	print('<div align = "left" style = "margin-top:10px; margin-bottom:10px; font-size: 100%;">'."\r\n");
		print('<a  style = "font-size:80%; margin-right: 15px;" href = "'.$RaidRulesLink.'" target = "_blank">Положение</a> '."\r\n");
		print('<a  style = "font-size:80%; margin-right: 15px;" href = "'.$RaidStartLink.'" target = "_blank">Информация о старте</a> '."\r\n");
                if (trim($RaidKpWptLink) <> '')
		{
			print('<a  style = "font-size:80%; margin-right: 15px;" href = "'.$RaidKpWptLink.'" target = "_blank">Точки КП</a> '."\r\n");
		}
                if (trim($RaidLegendLink) <> '')
		{
			print('<a  style = "font-size:80%; margin-right: 15px;" href = "'.$RaidLegendLink.'" target = "_blank">Легенды</a> '."\r\n");
		}
                if (trim($RaidZipLink) <> '')
		{
			print('<a  style = "font-size:80%; margin-right: 15px;" href = "'.$RaidZipLink.'" target = "_blank">ZIP всех материалов</a> '."\r\n");
		}
                if (trim($RaidZnLink) <> '')
		{
			print('<a  style = "font-size:80%; margin-right: 15px;" href = "'.$RaidZnLink.'" target = "_blank");">Значок</a> '."\r\n");
		}
			print('<a  style = "font-size:80%; margin-right: 15px;" href = "javascript: JsonExport();">Json</a> '."\r\n");
		print('</div>'."\r\n");


                // Информация о дистанции(ях)


		    print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");

		    $sql = "select  d.distance_name, d.distance_data
  		            from Distances d
                            where d.raid_id = ".$RaidId;
				
		    $Result = MySqlQuery($sql);

		    // теперь цикл обработки данных по этапам
		    while ($Row = mysql_fetch_assoc($Result))
		    {
                	print('<tr><td width = "100">'.$Row['distance_name'].'</td>
			           <td width = "300">'.$Row['distance_data'].'</td>'."\r\n");

                        // Если идёт регистрацию время окончания выделяем жирным
                        if ($RaidStage == 1)
			{ 
			    print('<td style = "font-weight: bold;">Регистрация до: '.$RaidRegisterEndDt.'</td>'."\r\n");

			} else {

			    print('<td>Регистрация до: '.$RaidRegisterEndDt.'</td>'."\r\n");
		
			}
				   
			if (!empty($RaidCloseDt))
			{	   
			  print('<td>Протокол закрыт: '.$RaidCloseDt.'</td>'."\r\n");
			}  
			print('</tr>'."\r\n");

		    }
		    
		    // конец цикла по этапам
		    mysql_free_result($Result);

		    print('</table>'."\r\n");
		    

                // Показываем этапы
		if ($LevelDataVisible)
		{

                    // Нужно доработать проыерку старта,т.к. нельзя прямо проверять время старта - на этапах с общим стартом его нет
                    // Добавлен расчет статистики
		    $sql = "select  d.distance_name, l.level_id, l.level_name,
                                    l.level_pointnames, l.level_starttype,
                                    l.level_pointpenalties, l.level_order,
				    DATE_FORMAT(l.level_begtime,    '%d.%m %H:%i') as level_sbegtime,
				    DATE_FORMAT(l.level_maxbegtime, '%d.%m %H:%i') as level_smaxbegtime,
                                    DATE_FORMAT(l.level_minendtime, '%d.%m %H:%i') as level_sminendtime,
                                    DATE_FORMAT(l.level_endtime,    '%d.%m %H:%i') as level_sendtime,
                                    l.level_begtime, l.level_maxbegtime, l.level_minendtime,
                                    l.level_endtime, 
                                    (select count(*) 
                                     from TeamLevels tl
                                          inner join Teams t
                                          on tl.team_id = t.team_id 
                                     where tl.level_id =  l.level_id 
                                           and tl.teamlevel_progress > 0
                                           and tl.teamlevel_hide = 0
                                           and t.team_hide = 0
                                    ) as teamstartcount,
                                    (select count(*) 
                                     from TeamLevels tl
                                          inner join Teams t
                                          on tl.team_id = t.team_id 
                                          inner join TeamUsers tu
                                          on tl.team_id = tu.team_id 
                                          left outer join Levels l2
                                          on tu.level_id = l2.level_id
                                     where tl.level_id =  l.level_id 
                                           and tl.teamlevel_progress > 0
                                           and tl.teamlevel_hide = 0
                                           and tu.teamuser_hide = 0
                                           and t.team_hide = 0
                                           and COALESCE(l2.level_order, l.level_order + 1) > l.level_order 
                                    ) as teamuserstartcount,
				     (select count(*) 
                                     from TeamLevels tl
                                          inner join Teams t
                                          on tl.team_id = t.team_id 
                                     where tl.level_id = l.level_id 
                                           and tl.teamlevel_progress = 2
					    and tl.teamlevel_endtime > 0
					    and tl.teamlevel_hide = 0
                                           and t.team_hide = 0
                                   ) as teamfinishcount,
                                    (select count(*) 
                                     from TeamLevels tl
                                          inner join Teams t
                                          on tl.team_id = t.team_id 
                                          inner join TeamUsers tu
                                          on tl.team_id = tu.team_id 
                                          left outer join Levels l2
                                          on tu.level_id = l2.level_id
                                     where tl.level_id =  l.level_id 
                                           and tl.teamlevel_progress = 2
					    and tl.teamlevel_endtime > 0
                                           and tl.teamlevel_hide = 0
                                           and tu.teamuser_hide = 0
                                           and COALESCE(l2.level_order, l.level_order + 1) > l.level_order 
                                           and t.team_hide = 0
                                    ) as teamuserfinishcount

                            from  Levels l
                                  inner join Distances d
                                  on d.distance_id = l.distance_id
                            where   d.raid_id = ".$RaidId;

                    // $sql = $sql." and l.level_begtime <= now() ";

                    if (!empty($_REQUEST['DistanceId']))
                    {
			$sql = $sql." and d.distance_id = ".$_REQUEST['DistanceId'];
                    }
                    if (!empty($_REQUEST['LevelId']))
                    {
			$sql = $sql." and l.level_id = ".$_REQUEST['LevelId'];
                    }
                    $sql = $sql." order by d.distance_name, l.level_order ";

	            // echo $sql;

		    print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");
		    print('<tr class = "gray">'."\r\n");
		    print('<td width = "100">Дистанция</td>'."\r\n");
		    print('<td width = "300">Этап (по ссылкам - карты)</td>'."\r\n");
		    print('<td width = "400">Тип старта (границы по времени)'."\r\n");
		    print('<td width = "70">Число КП'."\r\n");
		    print('</tr>'."\r\n");
		    $Result = MySqlQuery($sql);

		    // теперь цикл обработки данных по этапам
		    while ($Row = mysql_fetch_assoc($Result))
		    {

			// По этому ключу потом определяем, есть ли уже строчка в teamLevels или её нужно создать

			$TeamLevelId = $Row['level_id'];
			$LevelStartType = $Row['level_starttype'];
			$LevelPointNames =  $Row['level_pointnames'];
			$LevelPointPenalties =  $Row['level_pointpenalties'];

			$LevelStartTeamCount =  $Row['teamstartcount'];
			$LevelFinishTeamCount =  $Row['teamfinishcount'];
			$LevelStartTeamUserCount =  $Row['teamuserstartcount'];
			$LevelFinishTeamUserCount =  $Row['teamuserfinishcount'];

			//   $LevelMapLink = $Row['level_maplink'];

			// Проверяем, что строчка с названиями КП указана
			if (trim($LevelPointNames) <> '')
			{
			    $PointsCount = count(explode(',', $LevelPointNames));
			} else {
			    $PointsCount = 0;
			}
                  

			// Если старт не задан - считаем общим
			if (empty($LevelStartType))
			{
			    $LevelStartType = 2;
			}

			// Делаем оформление в зависимости от типа старта и соотношения  гранчиных дат:
			// Есди даты границ совпадают - выводим только первую
			// Если есть даты старта и фнишиа и они совпадают - выодим только дату старта
			if ($LevelStartType == 1)
			{
			    $LevelStartTypeText = 'Старт по готовности:  ';
			    if (substr(trim($Row['level_sbegtime']), 0, 5) == substr(trim($Row['level_smaxbegtime']), 0, 5))
			    {
				$LevelStartTypeText = $LevelStartTypeText.$Row['level_sbegtime'].' - '.substr(trim($Row['level_smaxbegtime']), 6);
			    } else {
				$LevelStartTypeText = $LevelStartTypeText.$Row['level_sbegtime'].' - '.$Row['level_smaxbegtime'];
			    }
			    $LevelStartTypeText = $LevelStartTypeText.' </br> Финиш: ';

			} elseif ($LevelStartType == 2) {
			    $LevelStartTypeText = 'Старт общий: '.$Row['level_sbegtime'];
			    $LevelStartTypeText = $LevelStartTypeText.' </br> Финиш: ';

			} elseif ($LevelStartType == 3) {
			    $LevelStartTypeText = 'Старт во время финиша предыдущего этапа </br> Финиш: ';

			}


			// Дополняем рамками финиша
			// Проверяем на одинаковые даты
			if (substr(trim($Row['level_sminendtime']), 0, 5) == substr(trim($Row['level_sendtime']), 0, 5))
			{
			    if (substr(trim($Row['level_sbegtime']), 0, 5) == substr(trim($Row['level_sendtime']), 0, 5))
			    {
				$LevelStartTypeText = $LevelStartTypeText.substr(trim($Row['level_sminendtime']), 6).' - '.substr(trim($Row['level_sendtime']), 6);
			    } else {
				$LevelStartTypeText = $LevelStartTypeText.$Row['level_sminendtime'].' - '.substr(trim($Row['level_sendtime']), 6);
			    }
			} else {
			    $LevelStartTypeText = $LevelStartTypeText.$Row['level_sminendtime'].' - '.$Row['level_sendtime'];
			}

			$LevelStartTypeText = $LevelStartTypeText.' ';
       
			// сторим строчку для текущего этапа
			print('<tr><td>'.$Row['distance_name'].'</td>'."\r\n");

			print('<td>'.$Row['level_name']."\r\n");

			$sql = 'select levelmaplink_url, levelmapozilink_url
                                from LevelMapLinks
                                where level_id = '.$Row['level_id'].'
                                order by levelmaplink_id';

			// echo $sql;
			$ResultMap = MySqlQuery($sql);
       
			$MapsCount = 0;
			$MapString = '';
			// теперь цикл обработки данных по этапам
			while ($RowMap = mysql_fetch_assoc($ResultMap))
			{
			    $MapsCount++;
			    $MapString = $MapString.', <a href = "'.trim($RowMap['levelmaplink_url']).'" target = "_blank">'.$MapsCount.'</a>';
			    if (trim($RowMap['levelmapozilink_url']) <> '')
			    {
			      $MapString = $MapString.' <a href = "'.trim($RowMap['levelmapozilink_url']).'" target = "_blank">map</a>';
			    }  
			}
			mysql_free_result($ResultMap);
			if (!empty($MapString))
			{
			    $MapString = '('.trim(substr(trim($MapString), 1)).')';
			    print($MapString."\r\n");
			}

                        // Впечатываем статитстику старт/финиш


			print('</br>'.$LevelStartTeamCount.'/'.$LevelStartTeamUserCount.' - '.$LevelFinishTeamCount.'/'.$LevelFinishTeamUserCount.' '."\r\n");
			print('</td>'."\r\n");

			print('<td>'.$LevelStartTypeText.'</td>
			       <td>'.$PointsCount.'</td>
			       </tr>'."\r\n");

		    }
		    // конец цикла по этапам
		    mysql_free_result($Result);
		    print('</table>'."\r\n");
		}

		// ============ Вывод списка команд ===========================

		// Готовим строчки с описанием схода с этапов
		$DistanceId = 0;
		$DismissNames = array();
		$sql = "select level_name, l.distance_id from Levels l
				inner join Distances d on l.distance_id = d.distance_id
			where d.raid_id = ".$RaidId.'
			order by l.distance_id, level_order';
                $Result = MySqlQuery($sql);
		while ($Row = mysql_fetch_assoc($Result))
		{
			if ($DistanceId <> $Row['distance_id'])
			{
				$DistanceId = $Row['distance_id'];
				$TeamProgress = 0;
			}
			if (!$TeamProgress)
			{
				if ($RaidStage < 4) $DismissNames[$DistanceId][0] = '';
				else $DismissNames[$DistanceId][0] = "\n".'<br><i>Не вышла на старт</i>';
			}
			else $DismissNames[$DistanceId][$TeamProgress] = "\n".'<br><i>Не вышла на этап '.$Row['level_name'].'</i>';
			$DismissNames[$DistanceId][$TeamProgress + 1] = "\n".'<br><i>Сошла с этапа '.$Row['level_name'].'</i>';
			$DismissNames[$DistanceId][$TeamProgress + 2] = '';
			$TeamProgress += 2;
		}
		mysql_free_result($Result);

		// Выводим список команд
		if  ($OrderType == 'Num')
                {

                  // Сортировка по номеру (в обратном порядке)
		  $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, 
		               t.team_mapscount, t.team_progress, d.distance_name, d.distance_id,
                               TIME_FORMAT(t.team_result, '%H:%i') as team_sresult
		        from  Teams t
			     inner join  Distances d 
			     on t.distance_id = d.distance_id
			where t.team_hide = 0 and d.raid_id = ".$RaidId;

		   if (!empty($_REQUEST['DistanceId']))
		   {
                     $sql = $sql." and d.distance_id = ".$_REQUEST['DistanceId']; 
		   }
		   $sql = $sql." order by team_num desc"; 
                    

                } elseif ($OrderType == 'Place') {
                  // Сортировка по месту требует более хитрого запроса


		   if (empty($_REQUEST['LevelId']))
		   {

		    $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,  
		               t.team_mapscount, t.team_progress, d.distance_name, d.distance_id,
                               TIME_FORMAT(t.team_result, '%H:%i') as team_sresult
			  from  Teams t
				inner join  Distances d 
				on t.distance_id = d.distance_id
			  where t.team_hide = 0 and d.raid_id = ".$RaidId;

		      if (!empty($_REQUEST['DistanceId']))
		      {
			$sql = $sql." and d.distance_id = ".$_REQUEST['DistanceId']; 
		      }

		      $sql = $sql." order by distance_name, team_progress desc, team_result asc, team_num asc ";
		    
		     } else {
                          // Если фильтруем по этапу, то другой запрос

		      $sql = " select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,  
				      t.team_mapscount, t.team_progress, d.distance_name, d.distance_id,
				      CASE WHEN COALESCE(tl.teamlevel_duration, 0) > 0 
                                          THEN TIME_FORMAT(SEC_TO_TIME(TIME_TO_SEC(tl.teamlevel_duration) + COALESCE(tl.teamlevel_penalty, 0)*60), '%H:%i') 
                                          ELSE ''
                                     END as  team_sresult,
                                      TIME_FORMAT(SEC_TO_TIME(COALESCE(tl.teamlevel_penalty, 0)*60), '%H:%i') as teamlevel_penalty,
                                      tl.teamlevel_points, tl.teamlevel_comment, l.level_pointnames
			    from  TeamLevels tl 
				  inner join Levels l 
				  on tl.level_id = l.level_id 
                                  inner join Teams t
                                  on t.team_id = tl.team_id 
				  inner join  Distances d 
				  on t.distance_id = d.distance_id
			    where tl.teamlevel_hide = 0 
                                 and tl.level_id = ".$_REQUEST['LevelId']." 
                                 and tl.teamlevel_progress > 0
                                 and t.team_hide = 0
			    order by tl.teamlevel_progress desc, team_sresult asc, t.team_num asc";

                     }

		} elseif ($OrderType == 'Errors') {
		// Ставим первыми те команды, у которых хотя бы на одном из этапов код ошибки или комментарий отличен от нуля
			$sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_mapscount, t.team_progress, d.distance_name, d.distance_id, TIME_FORMAT(t.team_result, '%H:%i') as team_sresult,
					(select count(*) from Teams t2, Distances d2, Levels l, TeamLevels tl
					where t2.team_id = t.team_id and t2.distance_id = d2.distance_id and l.distance_id = d2.distance_id and tl.team_id = t.team_id and tl.level_id = l.level_id
						and (((tl.error_id is not NULL) and (tl.error_id <> 0)) or (tl.teamlevel_comment is not NULL))
					) as n_err
				from Teams t, Distances d
				where t.team_hide = 0 and d.raid_id = ".$RaidId." and t.distance_id = d.distance_id";
			if (!empty($_REQUEST['DistanceId']))
			{
				$sql = $sql." and d.distance_id = ".$_REQUEST['DistanceId'];
			}
			$sql = $sql." order by n_err desc, t.team_progress desc, t.team_result asc, t.team_id asc";
		}

          	//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
	



                $tdstyle = 'padding: 5px 0px 2px 5px;';		
                $tdstyle = '';		
                $thstyle = 'border-color: #000000; border-style: solid; border-width: 1px 1px 1px 1px; padding: 5px 0px 2px 5px;';		
                $thstyle = '';		

                $ColumnWidth = 0;
                if ($ResultViewMode == 'WithLevels') {
                    $ColumnWidth = 175;
                } else {
                    $ColumnWidth = 350;
                }


//		print('<table width = "'.(($_REQUEST['LevelId'] > 0 and  $OrderType == 'Place') ? '1015' : '815').'" border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");  
		print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");  
		print('<tr class = "gray">
		         <td width = "50" style = "'.$thstyle.'">Номер</td>
			 <td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Команда (gps, дистанция, карт)</td>');


		if ($OrderType <> 'Errors') print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Участники</td>');
		else print('<td style = "'.$thstyle.'">Ошибки</td>');
		print('<td width = "50" style = "'.$thstyle.'">Результат</td>'."\r\n");
                if ($OrderType == 'Place')   
                {
                  // дополнительное поле место
		  print('  <td width = "50" style = "'.$thstyle.'">Место</td>'."\r\n");

                  // дополнительные поля в случае вывода  этапа
                  if ($_REQUEST['LevelId'] > 0)
                  {
		    print('<td width = "50" style = "'.$thstyle.'">Штраф</td>
			   <td width = "150" style = "'.$thstyle.'">Комментарий</td>
			   <td width = "250" style = "'.$thstyle.'">Невзятые КП</td>'."\r\n");

                        

		  }


                 if ($ResultViewMode == 'WithLevels') {
		    print('<td width = "500"  style = "'.$thstyle.'">'."\r\n");
                   print('<table border = "0" cellpadding = "0" style = "font-size: 100%"><tr>'."\r\n");
		    print('<td width = "120" style = "'.$thstyle.'">Старт-Финиш</td>'."\r\n");
		    print('<td width = "100" style = "'.$thstyle.'">Время (Штраф)</td>'."\r\n");
		    print('<td width = "50" style = "'.$thstyle.'">Место</td>'."\r\n");
		    print('<td width = "230" style = "'.$thstyle.'">Не взяты КП</td>'."\r\n");
		    print('</tr></table>'."\r\n");

                 } 


                }
		print('</tr>'."\r\n");
	
		$TeamsCount = mysql_num_rows($Result);
                
                // Меняем логику отображения места
                // Было 1111233345  Стало 1111455589  
                $TeamPlace = 0;
                $SamePlaceTeamCount = 1;
                $PredResult = ''; 
		
		while ($Row = mysql_fetch_assoc($Result))
		{

			if ($TeamsCount%2 == 0) {
			  $TrClass = 'yellow';
			} else {
			  $TrClass = 'green';
			} 

			$TeamsCount--;
                      
                       if ($Row['team_greenpeace'] == 1) {
                             $tdgreenstyle = 'background-color:#99ee99;';
                        } else {
                             $tdgreenstyle = '';
                        }

 			print('<tr class = "'.$TrClass.'"><td style = "'.$tdgreenstyle.'"><a name = "'.$Row['team_num'].'"></a>'.$Row['team_num'].'</td><td style = "'.$tdstyle.'"><a href = "javascript:ViewTeamInfo('.$Row['team_id'].');">'.
			          $Row['team_name'].'</a> ('.($Row['team_usegps'] == 1 ? 'gps, ' : '').$Row['distance_name'].', '.$Row['team_mapscount'].')'.
                                  $DismissNames[$Row['distance_id']][$Row['team_progress']].'</td><td style = "'.$tdstyle.'">'."\r\n");

			if ($OrderType <> 'Errors')
			{
			$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear,
                                       tu.level_id, u.user_id, l.level_name 
			        from  TeamUsers tu
				     inner join  Users u
				     on tu.user_id = u.user_id
                                     left outer join Levels l
 				     on tu.level_id = l.level_id
 				where tu.teamuser_hide = 0 and team_id = ".$Row['team_id']; 
			//echo 'sql '.$sql;
			$UserResult = MySqlQuery($sql);

			while ($UserRow = mysql_fetch_assoc($UserResult))
			{
			  print('<div class= "input"><a href = "javascript:ViewUserInfo('.$UserRow['user_id'].');">'.$UserRow['user_name'].'</a> '.$UserRow['user_birthyear']."\r\n");
                          if ($UserRow['level_name'] <> '')
                          {
			      print('<i>Сход: '.$UserRow['level_name'].'</i>'."\r\n");
                          } 
			  print('</div>'."\r\n");
			}  
		        mysql_free_result($UserResult);
			}
			elseif ($Row['n_err'] > 0)
			{
				$sql = 'select l.level_name, teamlevel_comment, tl.error_id, e.error_name from TeamLevels tl
						inner join Levels l on l.level_id = tl.level_id
						inner join Errors e on e.error_id = tl.error_id
					where tl.team_id = '.$Row['team_id'].' and ((tl.error_id <> 0) or (tl.teamlevel_comment is not NULL))';
				$ErrResult = MySqlQuery($sql);
				while ($ErrRow = mysql_fetch_assoc($ErrResult))
				{
					echo $ErrRow['level_name'].', ';
					if ($ErrRow['error_id'] > 0) echo 'ошибка';
					elseif ($ErrRow['error_id'] < 0) echo 'предупреждение';
					else echo 'комментарий';
					if ($ErrRow['error_id']) echo ": <strong>".$ErrRow['error_name']."</strong>";
					else echo ": <em>".$ErrRow['teamlevel_comment']."</em>";
					echo "<br />";
				}
				mysql_free_result($ErrResult);
			}

			print('</td><td>'.$Row['team_sresult']."\r\n");
			print('</td>'."\r\n");
                       // Сортировка "по месту"
			if ($OrderType == 'Place')   
			{
			    print('<td width = "50" style = "'.$thstyle.'">'."\r\n");
                            if ($Row['team_sresult'] == '00:00' or $Row['team_sresult'] == '')
                            {
                               print('&nbsp;');
                            } elseif($Row['team_sresult'] <>  $PredResult) {
                               $TeamPlace = $TeamPlace + $SamePlaceTeamCount;
                               print($TeamPlace);
			        $PredResult = $Row['team_sresult'];
			        $SamePlaceTeamCount = 1;
                            } else {
				$SamePlaceTeamCount++;
                               print($TeamPlace);
			        $PredResult = $Row['team_sresult'];
                            }
			    print('</td>'."\r\n");

                           // Если фильтровали по этапу, то нужныдополнительные колонки 
			    if ($_REQUEST['LevelId'] > 0)
			    {
			      print('<td width = "50" style = "'.$thstyle.'">'.$Row['teamlevel_penalty'].'</td>
				      <td width = "50" style = "'.$thstyle.'">'.$Row['teamlevel_comment'].'</td>
				      <td width = "100" style = "'.$thstyle.'">'."\r\n");
			      InvertTeamLevelPoints($Row['level_pointnames'], $Row['teamlevel_points']); 
//			      ConvertTeamLevelPoints2($LevelPointNames, $LevelPointPenalties, $Row['teamlevel_points'], $_REQUEST['LevelId']); 
			      print('</td>'."\r\n");

			    }

                           // Для вывода "с этапами" 
			    if ($ResultViewMode == 'WithLevels')
                           {
/*
			      print('<td width = "50" style = "'.$thstyle.'">'.$Row['teamlevel_penalty'].'</td>
				      <td width = "50" style = "'.$thstyle.'">'.$Row['teamlevel_comment'].'</td>
				      <td width = "100" style = "'.$thstyle.'">'."\r\n");
			      InvertTeamLevelPoints($Row['level_pointnames'], $Row['teamlevel_points']); 
  */
                            print('<td>'."\r\n");
                               print('<table border = "0" cellpadding = "0" style = "font-size: 100%">'."\r\n");

                            // Запрашиваем все этапы 
			     $sql = "select level_id, level_starttype, 
                                           TIME_FORMAT(level_begtime, '%H:%i') as level_begtime,
                                           level_order, level_pointnames
                                    from  Levels l
                                    where l.distance_id = ".$Row['distance_id']."
                                    order by l.level_order ";
			     //  echo 'sql '.$sql;
			     $LevelResult = MySqlQuery($sql);
  
			      while ($LevelRow = mysql_fetch_assoc($LevelResult))
			      {
                                 // Запрашиваем результаты для этого этапа
				  $sql = "select TIME_FORMAT(SEC_TO_TIME(tl.teamlevel_penalty*60), '%H:%i') as teamlevel_penalty, 
                                                tl.teamlevel_progress,
                                                TIME_FORMAT(tl.teamlevel_duration, '%H:%i') as teamlevel_duration,
                                                TIME_FORMAT(tl.teamlevel_endtime, '%H:%i') as teamlevel_endtime,
                                                TIME_FORMAT(tl.teamlevel_begtime, '%H:%i') as teamlevel_begtime, 
                                                tl.teamlevel_points
					  from  TeamLevels tl
                                    where tl.teamlevel_hide = 0 
                                          and tl.teamlevel_progress > 0
                                          and tl.level_id = ".$LevelRow['level_id']."
                                          and tl.team_id = ".$Row['team_id'];
				    //  echo 'sql '.$sql;
				    $TeamLevelResult = MySqlQuery($sql);
				    while ($TeamLevelRow = mysql_fetch_assoc($TeamLevelResult))
				    {
					print('<tr><td width = "120" align = "left">&nbsp;'."\r\n");

                                               // формируем результирующую строку
                                       if ($LevelRow['level_starttype'] == 1) {

                                         print($TeamLevelRow['teamlevel_begtime']."\r\n"); 

                                       } elseif  ($LevelRow['level_starttype'] == 2) {

                                          print($LevelRow['level_begtime']."\r\n"); 
                                   	} else {
                                          print(''."\r\n"); 
					}

                                       print(' - '.$TeamLevelRow['teamlevel_endtime']."\r\n"); 

                                       if ($LevelRow['level_starttype'] == 3) {

                                      //   print(' ('.$TeamLevelRow['teamlevel_enddate'].')'."\r\n"); 

                                       }

                                       print('</td>'."\r\n");

                                       if ($TeamLevelRow['teamlevel_progress'] > 1) {
					  print('<td width = "100">'."\r\n");
					  print($TeamLevelRow['teamlevel_duration'].' ('.$TeamLevelRow['teamlevel_penalty'].')'."\r\n");
					  print('</td>'."\r\n");

					  print('<td width = "50">'."\r\n");
                                         print(GetTeamLevelPlace($Row['team_id'], $LevelRow['level_id'])."\r\n");
					  print('</td>'."\r\n");


                                       } else {


					  print('<td width = "100">'."\r\n");
					  print('&nbsp;'."\r\n");
					  print('</td>'."\r\n");

					  print('<td width = "50">'."\r\n");
                                         print('&nbsp;'."\r\n");
					  print('</td>'."\r\n");

                                       }

					  print('<td width = "230">'."\r\n");
                                         InvertTeamLevelPoints($LevelRow['level_pointnames'], $TeamLevelRow['teamlevel_points']); 
					  print('</td>'."\r\n");


				    }
				    mysql_free_result($TeamLevelResult);
				    // Конец запроса данных команды по этапу
			       }
			       mysql_free_result($LevelResult);
			       // Конец запроса этапов

                              print('</tr></table>'."\r\n");

                           }
                           // Конец проверки на формат вывода 
             		   print('</td>'."\r\n");
    
			}
			print('</tr>'."\r\n");
		}
		mysql_free_result($Result);
		print('</table>'."\r\n");
	

?>
</form>
</br>



