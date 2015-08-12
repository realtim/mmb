<?php
// +++++++++++ Показ списка команд марш-броска ++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">
	/*
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
	}*/


        // Смена сортировки
	function OrderTypeChange()
	{ 
/*
            if (this.value=='Num')
            {
                 document.RaidTeamsForm.LevelId.disabled=true;
                 document.RaidTeamsForm.ResultViewMode.disabled=true;
            }else{
                 document.RaidTeamsForm.LevelId.disabled=false;
                 document.RaidTeamsForm.ResultViewMode.disabled=false;
            }
*/
	    document.RaidTeamsForm.action.value = "ViewRaidTeams";          
	    document.RaidTeamsForm.submit();
	  
	}

	// Фильтр по дистанции
	function DistanceIdChange()
	{ 
          document.RaidTeamsForm.action.value = "ViewRaidTeams";          
	  document.RaidTeamsForm.submit();
        } 

	// Фильтр по точке
	function LevelPointIdChange()
	{ 
          document.RaidTeamsForm.action.value = "ViewRaidTeams";          
	  document.RaidTeamsForm.submit();
         } 


	// Фильтр по GPS
	function GPSChange()
	{ 
          document.RaidTeamsForm.action.value = "ViewRaidTeams";          
	  document.RaidTeamsForm.submit();
         } 


	// Формат вывода результатов
	function ResultViewModeChange()
	{ 
          document.RaidTeamsForm.action.value = "ViewRaidTeams";          
          document.RaidTeamsForm.OrderType.value = "Place";          
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
    function InvertTeamLevelPoints ($LevelPointNames,$TeamLevelPoints)
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

    function normalizeSkipped($str)
    {
        if (empty($str))
                return '&nbsp';

        $skipped = explode(',', $str);
        $len = count($skipped);
        if ($len < 2)
                return $str;

	$start = 0;
	$last = $skipped[0];
	$ints = array();
	for ($i = 1; $i < $len; $i++)
	{
		if ($skipped[$i] == $last + 1)
		{
			$last = $skipped[$i];
			continue;
		}



		if ($i > $start + 1)    // полноценный интервал
		{
			$ints[] = "{$skipped[$start]} - $last";
		}
		else
		{
			$ints[] = $skipped[$start];
			if ($last != $skipped[$start])
				$ints[] = $last;

		}

		$start = $i;
		$last = $skipped[$i];
	}

	if ($start < $len - 2)
	{
		$ints[] = "{$skipped[$start]} - $last";
	}
	else
	{
		$ints[] = $skipped[$start];
		if ($start < $len -1)
			$ints[] = $last;
	}

	return implode(", ", $ints);
    }



        // Проверяем, что передали  идентификатор ММБ
        if ($RaidId <= 0)
	{
		    CMmb::setMessage('Не указан ММБ');
		    return;
	
	}


?>	
         <form  name = "RaidTeamsForm"  action = "<? echo $MyPHPScript; ?>" method = "post">
         <input type = "hidden" name = "action" value = "ViewRaidTeams">
         <input type = "hidden" name = "TeamId" value = "0">
         <input type = "hidden" name = "UserId" value = "0">
         <input type = "hidden" name = "RaidId" value = "<? echo $RaidId; ?>">

<?

        $TabIndex = 0;
		$DisabledText = '';

        //Определяем локальные переменные-флаги показа результатов и этапов
        $CanViewResults = CanViewResults($Administrator, $Moderator, $RaidStage);
        $CanViewLevels = CanViewLevels($Administrator, $Moderator, $RaidStage);


        // Разбираемся с сортировкой
        $OrderType = mmb_validate($_REQUEST, 'OrderType', '');
	if (($OrderType == 'Errors') && !$Administrator && !$Moderator) $OrderType = '';


		$sql = "select raid_registrationenddate,  raid_closedate,
				 COALESCE(r.raid_noshowresult, 0) as raid_noshowresult
			  from  Raids r
			  where r.raid_id = $RaidId"; 
            
		$Row = CSql::singleRow($sql);
                $RaidRegisterEndDt = $Row['raid_registrationenddate'];
                $RaidCloseDt = $Row['raid_closedate'];
                $RaidNoShowResult = $Row['raid_noshowresult'];
   
          // 03/05/2014 Исправил порядок сортировки - раньше независисмо от устновленного  $OrderType могло сбрасываться
          // если порядок не задан смотрим на соотношение временени публикации и текущего
          if (empty($OrderType))
	  {
  	      $OrderType = $CanViewResults ? "Place" : "Num";
	  }
	  // Конец разбора сортировки по умолчанию
	   
// region часть над таблицей
          print('<div align="left" style="font-size: 80%;">'."\r\n");

          if ($CanViewResults || $Administrator || $Moderator)    // будет больше 1 пункта
          {
		print("Сортировать по \r\n");
		print('<select name="OrderType" style="margin-left: 10px; margin-right: 20px;"
	                       onchange="OrderTypeChange();"  tabindex="'.(++$TabIndex).'" '."$DisabledText>\r\n");
		print('<option value="Num" '.($OrderType == 'Num' ? 'selected' :'').">убыванию номера</option>\r\n");

	        //Сортировку по месту показыаем только после окончания ММБ, если не стоит флаг "Не показывать результаты"
		// Администраторам и модераторам флаг не мешает
		if ($CanViewResults)
		    print('<option value="Place" '.($OrderType == 'Place' ? 'selected' :'').">возрастанию места</option>\r\n");

		if ($Administrator || $Moderator)
		    print('<option value="Errors" '.($OrderType == 'Errors' ? 'selected' :'').">наличию ошибок</option>\r\n");

		print('</select>'."\r\n");
	 }


	print("Фильтровать: \r\n"); 

	$distanceId = mmb_validate($_REQUEST, 'DistanceId', '');
	$DistanceCondition = empty($distanceId) ? 'true' : "d.distance_id = $distanceId";

        $GpsFilter = (mmb_validateInt($_REQUEST, 'GPSFilter', 0)) == 1 ? 1 : 0;
        $GpsCondition = $GpsFilter ? "t.team_usegps = 0" : "true";

        $sql = "select distance_id, distance_name
                    from  Distances
                    where distance_hide = 0 and raid_id = $RaidId
                    order by distance_name";
		//echo 'sql '.$sql;
	$Result = MySqlQuery($sql);
	if (mysql_num_rows($Result) > 1)
	{
		print('<select name="DistanceId" style = "margin-left: 10px; margin-right: 5px;"
                               onchange="DistanceIdChange();"  tabindex="'.(++$TabIndex).'">'."\r\n");

                $selected  =  (0 == $distanceId) ? ' selected' : '';
		print("<option value=\"0\" $selected>дистанцию</option>\r\n");

	        while ($Row = mysql_fetch_assoc($Result))
		{
		  $selected = ($Row['distance_id'] == $distanceId) ? ' selected' : '';
		  print("<option value=\"{$Row['distance_id']}\" $selected>{$Row['distance_name']}</option>\r\n");
		}
		print("</select>\r\n");
	}
	mysql_free_result($Result);

/*
============================= точки ===============================
*/

        $sql = "select lp.levelpoint_id, lp.levelpoint_name, d.distance_name
                from  LevelPoints lp
                      inner join
				      (
				       select tlp.levelpoint_id
				       from TeamLevelPoints tlp
				            inner join Teams t
				            on tlp.team_id = t.team_id
				      		inner join Distances d
				      		on t.distance_id = d.distance_id
					   where raid_id = $RaidId and $DistanceCondition
					         and  TIME_TO_SEC(COALESCE(tlp.teamlevelpoint_duration, 0)) <> 0
					   group by tlp.levelpoint_id
					  ) a
					  on lp.levelpoint_id = a.levelpoint_id
 		      	 	  inner join Distances d
				      on lp.distance_id = d.distance_id
				order by lp.levelpoint_order ";

	//echo 'sql '.$sql;
	$Result = MySqlQuery($sql);
                
	print('<select name="LevelPointId" style = "margin-left: 10px; margin-right: 5px;" 
                       onchange = "LevelPointIdChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
        $levelpointselected =  (0 == $_REQUEST['LevelPointId'] ? 'selected' : '');
	print("<option value = '0' $levelpointselected>точку (КП)</option>\r\n");

	if (!isset($_REQUEST['LevelPointId'])) $_REQUEST['LevelPointId'] = "";

        while ($Row = mysql_fetch_assoc($Result))
	{
		$levelpointselected = ($Row['levelpoint_id'] == $_REQUEST['LevelPointId']  ? 'selected' : '');
		print("<option value = '{$Row['levelpoint_id']}' $levelpointselected>{$Row['distance_name']} {$Row['levelpoint_name']}</option>\r\n");
	}
	print('</select>'."\r\n");  
	mysql_free_result($Result);		

/*
======================  GPS  ====================
*/
	print('<select name="GPSFilter" style = "margin-left: 10px; margin-right: 5px;"
                       onchange = "GPSChange();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
	print('<option value="0" '. ($GpsFilter == 0 ? 'selected' : '') ." >не фильтровать по GPS</option>\r\n");
	print('<option value="1" '. ($GpsFilter == 1 ? 'selected' : '') ." >без GPS</option>\r\n");
	print('</select>'."\r\n");  

/*
=====================================

*/


//		if (!isset($_REQUEST['LevelId'])) $_REQUEST['LevelId'] = "";
	
	        // Режим отображения результатов
                $ResultViewMode = mmb_validate($_REQUEST, 'ResultViewMode', '');

		// Определяем, можно ли показывать пользователю информацию об этапах дистанции
/*
 	 	if ($CanViewLevels)
		{
		    $sql = "select levelpoint_id, d.distance_name, CONCAT(trim(lp.levelpoint_name), ' (', trim(d.distance_name), ')') as levelpoint_name
                            from  LevelPoints l
                                inner join Distances d
                                on lp.distance_id = d.distance_id
                            where d.raid_id = ".$RaidId;
                    if (!empty($_REQUEST['DistanceId']))
                    {
			$sql = $sql." and d.distance_id = ".$_REQUEST['DistanceId'];
                    }
                    $sql = $sql." order by d.distance_name, lp.levelpoint_order ";
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

         

           	     print('Отображать '."\r\n");
		     print('<select name="ResultViewMode" style = "margin-left: 10px; margin-right: 20px;" '.((($OrderType=='Num') || ($OrderType=='Errors') || ($_REQUEST['LevelId'] <> 0)) ? 'disabled' : '').'
                             onchange = "ResultViewModeChange();"  tabindex = "'.(++$TabIndex).'" '.$DisabledText.'>'."\r\n"); 
	   	     print('<option value = "Short" '.($ResultViewMode == 'Short' ? 'selected' :'').' >кратко'."\r\n");
	   	     print('<option value = "WithLevels" '.($ResultViewMode == 'WithLevels' ? 'selected' :'').' >с этапами'."\r\n");
		     print('</select>'."\r\n");  

		}

*/
		print('</div>'."\r\n");
            	
		print('<div align="left" style="margin-top:10px; margin-bottom:10px; font-size: 100%;">'."\r\n");
		print('<a style="font-size:80%; margin-right: 15px;" href="?files&RaidId='.$RaidId.'" title="Список файлов для выбранного выше ММБ">Файлы</a>'."\r\n");
		print('<a style="font-size:80%; margin-right: 15px;" href="javascript: JsonExport();">Json</a> '."\r\n");
		print('</div>'."\r\n");



// endregion


	print("\r\n</form>\r\n");

                // Информация о дистанции(ях)


		    print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");

		    $sql = "select  d.distance_name, d.distance_data
  		            from Distances d
                            where d.distance_hide = 0 and  d.raid_id = $RaidId";
				
		    $Result = MySqlQuery($sql);

		    // теперь цикл обработки данных по этапам
		    while ($Row = mysql_fetch_assoc($Result))
		    {
                	print('<tr><td width="100">'.$Row['distance_name'].'</td>
			           <td width="300">'.$Row['distance_data']."</td>\r\n");

                        // Если идёт регистрацию время окончания выделяем жирным
                        if ($RaidStage == 1)
			{ 
			    print("<td style=\"font-weight: bold;\">Регистрация до: $RaidRegisterEndDt</td>\r\n");
			} else {
			    print("<td>Регистрация до: $RaidRegisterEndDt</td>\r\n");
			}
				   
			if (!empty($RaidCloseDt))
			{	   
			  print("<td>Протокол закрыт: $RaidCloseDt</td>\r\n");
			}  
			print("</tr>\r\n");

		    }
		    
		    // конец цикла по этапам
		    mysql_free_result($Result);

		    print('</table>'."\r\n");
	/*	    

                // Показываем этапы
		if ($CanViewLevels and  $OrderType == 'Place')  
		{

                    // Нужно доработать проыерку старта,т.к. нельзя прямо проверять время старта - на этапах с общим стартом его нет
                    // Добавлен расчет статистики
		    $sql = "select  d.distance_name, lp.levelpoint_id, lp.levelpoint_name,
                                    pt.pointtype_name, lp.levelpoint_order,
				     lp.levelpoint_penalty,
				    DATE_FORMAT(lp.levelpoint_mindatetime, '%d.%m %H:%i') as levelpoint_smindatetime,
				    DATE_FORMAT(lp.levelpoint_maxdatetime, '%d.%m %H:%i') as levelpoint_smaxdatetime,
                                    (select count(*) 
                                     from TeamLevelPoints tlp
                                          inner join Teams t
                                          on tlp.team_id = t.team_id 
                                     where tlp.levelpoint_id =  lp.levelpoint_id 
                                    ) as teamscount,
                                    (select count(*) 
                                     from TeamLevelPoints tlp
                                          inner join Teams t
                                          on tlp.team_id = t.team_id 
                                          inner join TeamUsers tu
                                          on t.team_id = tu.team_id 
                                          left outer join TeamLevelDismiss tld
                                          on tu.teamuser_id = tld.teamuser_id
					  left outer join LevelPoints lp2
					  on tld.levelpoint_id = lp2.levelpoint_id
                                     where tlp.levelpoint_id =  lp.levelpoint_id 
                                           and t.team_hide = 0
                                           and COALESCE(lp2.levelpoint_order, lp.levelpoint_order + 1) > lp.levelpoint_order 
                                    ) as teamuserscount
                            from  LevelPoints lp
                                  inner join Distances d
                                  on d.distance_id = lp.distance_id
				  inner join PointTypes pt
				  on lp.pointtype_id = pt.pointtype_id
                            where  d.raid_id = ".$RaidId;

                    // $sql = $sql." and l.level_begtime <= now() ";

                    if (!empty($_REQUEST['DistanceId']))
                    {
			$sql = $sql." and d.distance_id = ".$_REQUEST['DistanceId'];
                    }

                    $sql = $sql." order by d.distance_name, lp.levelpoint_order ";

	            // echo $sql;

		    print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");
		    print('<tr class = "gray">'."\r\n");
		    print('<td width = "100">Дистанция</td>'."\r\n");
		    print('<td width = "100">Точка</td>'."\r\n");
		    print('<td width = "100">Тип и границы времени'."\r\n");
		    print('<td width = "100">Время с'."\r\n");
		    print('<td width = "100">по'."\r\n");
		    print('<td width = "100">Штраф за невзятие'."\r\n");
		    print('<td width = "100">Команд'."\r\n");
		    print('<td width = "100">Участников'."\r\n");
		    print('</tr>'."\r\n");
		    $Result = MySqlQuery($sql);

		    // теперь цикл обработки данных по этапам
		    while ($Row = mysql_fetch_assoc($Result))
		    {

			print('<tr><td>'.$Row['distance_name'].'</td>'."\r\n");
			print('<td>'.$Row['levelpoint_name'].'</td>'."\r\n");
			print('<td>'.$Row['pointtype_name'].'</td>'."\r\n");
			print('<td>'.$Row['levelpoint_smindatetime'].'</td>'."\r\n");
			print('<td>'.$Row['levelpoint_smaxdatetime'].'</td>'."\r\n");
			print('<td>'.$Row['levelpoint_penalty'].'</td>'."\r\n");
			print('<td>'.$Row['teamscount'].'</td>'."\r\n");
			print('<td>'.$Row['teamuserscount'].'</td>'."\r\n");
			print('</tr>'."\r\n");

		    }
		    // конец цикла по точкам
		    mysql_free_result($Result);
		    print('</table>'."\r\n");
		}
*/


		// ============ Вывод списка команд ===========================

	
		// Выводим список команд
		if  ($OrderType == 'Num')
		{

                  // Сортировка по номеру (в обратном порядке)
		  $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,  
		               t.team_mapscount, 
			       d.distance_name, d.distance_id,
			       COALESCE(t.team_outofrange, 0) as  team_outofrange 
		        from  Teams t
			     inner join  Distances d 
			     on t.distance_id = d.distance_id
			where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
				and $DistanceCondition and $GpsCondition
			order by team_num desc";


		}
		elseif ($OrderType == 'Place') {
			// Сортировка по месту требует более хитрого запроса

			if (!empty($_REQUEST['LevelPointId']))
			{
			    $LevelCondition = "tlp.levelpoint_id = ".$_REQUEST['LevelPointId'];

			    $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,  
			               t.team_mapscount, lp.levelpoint_order as team_progress, 
			               CASE WHEN COALESCE(t.team_minlevelpointorderwitherror, 0) > lp.levelpoint_order THEN 0 ELSE COALESCE(t.team_minlevelpointorderwitherror, 0) END as team_error,
				       d.distance_name, d.distance_id,
		                       TIME_FORMAT(tlp.teamlevelpoint_result, '%H:%i') as team_sresult,
				       COALESCE(t.team_outofrange, 0) as  team_outofrange,
				       COALESCE(lp.levelpoint_name, '') as levelpoint_name,
				       COALESCE(t.team_comment, '') as team_comment
				  from  Teams t
					inner join  Distances d 
					on t.distance_id = d.distance_id
					inner join  TeamLevelPoints tlp 
					on t.team_id = tlp.team_id
       					inner join LevelPoints lp
					on tlp.levelpoint_id = lp.levelpoint_id
				  where t.team_hide = 0
				  	and $LevelCondition and $GpsCondition
				  order by distance_name, team_outofrange, team_progress desc, team_error asc, tlp.teamlevelpoint_result asc, team_num asc ";

			} else {
			
			    $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,  
			               t.team_mapscount, t.team_maxlevelpointorderdone as team_progress, 
			               COALESCE(t.team_minlevelpointorderwitherror, 0) as team_error,
				       d.distance_name, d.distance_id,
		                       TIME_FORMAT(t.team_result, '%H:%i') as team_sresult,
				       COALESCE(t.team_outofrange, 0) as  team_outofrange,
				       COALESCE(lp.levelpoint_name, '') as levelpoint_name,
				       COALESCE(t.team_comment, '') as team_comment,
				       COALESCE(notlp.notlevelpoint_name, '') as notlevelpoint_name
				  from  Teams t
					inner join  Distances d
					on t.distance_id = d.distance_id
					left outer join LevelPoints lp
					on lp.distance_id = t.distance_id
					   and lp.levelpoint_order = t.team_maxlevelpointorderdone
					left outer join
					(
						select t.team_id, GROUP_CONCAT(lp.levelpoint_name ORDER BY lp.levelpoint_order, ' ') as notlevelpoint_name
						from  Teams t
								inner join  Distances d
								on t.distance_id = d.distance_id
								join LevelPoints lp
 								on t.distance_id = lp.distance_id
									and  COALESCE(t.team_maxlevelpointorderdone, 0) >= lp.levelpoint_order
								left outer join TeamLevelPoints tlp
								on lp.levelpoint_id = tlp.levelpoint_id
									and t.team_id = tlp.team_id
					 	where 	tlp.levelpoint_id is NULL
								and d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
						group by t.team_id
					) as notlp
					on t.team_id = notlp.team_id
				  where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
				  	and $DistanceCondition and $GpsCondition

			          order by distance_name, team_outofrange, team_progress desc, team_error asc, team_result asc, team_num asc ";

			}	    
		
		}
		elseif ($OrderType == 'Errors') {
		
                       // Не знаю, как будет реализовано
	
		    $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, t.team_greenpeace,  
		               t.team_mapscount, COALESCE(t.team_maxlevelpointorderdone, 0) as team_progress, 
		               COALESCE(t.team_minlevelpointorderwitherror, 0) as team_error,
			       d.distance_name, d.distance_id,
                               TIME_FORMAT(t.team_result, '%H:%i') as team_sresult,
			       COALESCE(t.team_outofrange, 0) as  team_outofrange,
			       COALESCE(lp.levelpoint_name, '') as levelpoint_name,
			       COALESCE(t.team_comment, '') as team_comment
			  from  Teams t
				inner join 
				( 
				  select tlp.team_id
				  from TeamLevelPoints tlp
				  where error_id is not null
				  group by tlp.team_id
				) err
				on t.team_id = err.team_id
				inner join  Distances d 
				on t.distance_id = d.distance_id
				left outer join LevelPoints lp
				on lp.distance_id = t.distance_id
				   and lp.levelpoint_order = t.team_maxlevelpointorderdone
			  where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = $RaidId
			  	and $DistanceCondition

		          order by distance_name, team_outofrange, team_progress desc, team_error asc, team_result asc, team_num asc ";
		    
		}

          	//echo 'sql '.$sql;
          	$t1 = microtime(true);
                $Result = MySqlQuery($sql);
                $t2 = microtime(true);
	



                $tdstyle = 'padding: 5px 0px 2px 5px;';		
                $tdstyle = '';		
                $thstyle = 'border-color: #000000; border-style: solid; border-width: 1px 1px 1px 1px; padding: 5px 0px 2px 5px;';		
                $thstyle = '';		



//		print('<table width = "'.(($_REQUEST['LevelId'] > 0 and  $OrderType == 'Place') ? '1015' : '815').'" border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");  
		print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");  
		print('<tr class = "gray">'."\r\n");  

		if ($OrderType == 'Num') {

			$ColumnWidth = 350;
			print('<td width = "50" style = "'.$thstyle.'">Номер</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Команда</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Участники</td>'."\r\n");  
		
		} elseif ($OrderType == 'Place') {
		

            $ColumnWidth = 350;
			print('<td width = "50" style = "'.$thstyle.'">Номер</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Команда</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Участники</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Точка финиша</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Результат</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Место</td>'."\r\n");
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Комментарий</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Не пройдены точки</td>'."\r\n");


		} elseif ($OrderType == 'Errors') {
	
	
            $ColumnWidth = 350;
			print('<td width = "50" style = "'.$thstyle.'">Номер</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Команда</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Участники</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Точка финиша</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Результат</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Место</td>'."\r\n");  
                        print('<td width = "'.$ColumnWidth.'" style = "'.$thstyle.'">Комментарий</td>'."\r\n");  

		
		}
	
		print('</tr>'."\r\n");
	
		$TeamsCount = mysql_num_rows($Result);
                
                // Меняем логику отображения места
                // Было 1111233345  Стало 1111455589  
                $TeamPlace = 0;
                $SamePlaceTeamCount = 1;
                $PredResult = ''; 
		$PredDistanceId = 0;
		
		while ($Row = mysql_fetch_assoc($Result))
		{
			$TrClass = ($TeamsCount%2 == 0) ? 'yellow': 'green';

			$TeamsCount--;

			$useGps = $Row['team_usegps'] == 1 ? 'gps, ' : '';
			$teamGP = $Row['team_greenpeace'] == 1 ? ', <a title="Нет сломанным унитазам!" href="#comment">ну!</a>' : '';
			$outOfRange = $Row['team_outofrange'] == 1 ? ', Вне зачета!' : '';

 			print('<tr class="'.$TrClass.'">
			       <td style="'.$tdstyle.'"><a name="'.$Row['team_num'].'"></a>'.$Row['team_num'].'</td>
			       <td style="'.$tdstyle.'"><a href="?TeamId='.$Row['team_id'].'&RaidId=' . $RaidId .'">'.
			          CMmbUI::toHtml($Row['team_name'])."</a> ($useGps{$Row['distance_name']}, {$Row['team_mapscount']}$teamGP$outOfRange)</td><td style=\"$tdstyle\">\r\n");

                        // Формируем колонку Участники			
				$sql = "select tu.teamuser_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name, u.user_birthyear, u.user_city,
					       u.user_id, 
					       tld.levelpoint_id, lp.levelpoint_name,
					       tu.teamuser_notstartraidid 
				        from  TeamUsers tu
					     inner join  Users u
					     on tu.user_id = u.user_id
		                             left outer join TeamLevelDismiss tld
					     on tu.teamuser_id = tld.teamuser_id
		                             left outer join LevelPoints lp
					     on tld.levelpoint_id = lp.levelpoint_id
					where tu.teamuser_hide = 0 and tu.team_id = {$Row['team_id']}";
				//echo 'sql '.$sql;
				$UserResult = MySqlQuery($sql);

				while ($UserRow = mysql_fetch_assoc($UserResult))
				{
				  print('<div class= "input"><a href="?UserId='.$UserRow['user_id'].'&RaidId=' . $RaidId . '">'.CMmbUI::toHtml($UserRow['user_name']).'</a> '.$UserRow['user_birthyear'].' '.CMmbUI::toHtml($UserRow['user_city'])."\r\n");
 
		                  // Отметка невыходна на старт в предыдущем ММБ                          
		                  if ($UserRow['teamuser_notstartraidid'] > 0) {
				    print(' <a title = "Участник был заявлен, но не вышел на старт в прошлый раз" href = "#comment">(?!)</a> ');
				  } 
        
		                  // Неявку участников показываем, если загружены результаты
				  if ($CanViewResults) 
		                  {
					if ($UserRow['levelpoint_name'] <> '')
					{
					    print("<i>Не явился(-ась) в: {$UserRow['levelpoint_name']}</i>\r\n");
					} 
		                  }
				  print('</div>'."\r\n");
				}  
			        mysql_free_result($UserResult);
			// Конец формирования колонки Участники
			print("</td>\r\n");


                       // Сортировка "по месту"
			if ($OrderType == 'Place')   
			{


			    print("<td>{$Row['levelpoint_name']}</td>\r\n");
			    print("<td>{$Row['team_sresult']}</td>\r\n");

                            // Формируем место    
			    print("<td>\r\n");
                            // Сбрасываем при смене дистанции
			    if ($Row['distance_id'] <> $PredDistanceId)
                            {
			        $TeamPlace = 0;
				$SamePlaceTeamCount = 1;
				$PredResult = '';
				$PredDistanceId = $Row['distance_id'];
                            }
			    
                            if ($Row['team_sresult'] == '00:00' or $Row['team_sresult'] == '' or $Row['team_outofrange'] == 1 or $Row['team_error'] > 0)
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
			    print("</td>\r\n");

                              // Формируем комментарий
			    //print('<td  style = "'.$tdstyle.'">'.GetTeamComment($Row['team_id']).'</td>'."\r\n");
			    print("<td>\r\n");
			    print($Row['team_comment']);
			    print("</td>\r\n");
			    print('<td>' . normalizeSkipped($Row['notlevelpoint_name']) . "</td>\r\n");

			}  elseif ($OrderType == 'Errors') {

			    print("<td>{$Row['levelpoint_name']}</td>\r\n");
			    print("<td>{$Row['team_sresult']}</td>\r\n");

			    print("<td>&nbsp;</td>\r\n");

			    print("<td>\r\n");
			    print($Row['team_comment']);
			    print("</td>\r\n");
			}
			// Конец проверки на вывод с сотрировкой по месту
			print("</tr>\r\n");
		}
		mysql_free_result($Result);
		print("</table>\r\n");
		$t3 = microtime(true);

		print("<div>Total time: '" . ($t3-$t1) . "' query: '" . ($t2-$t1) . "' render: '" . ($t3-$t2). '</div>');
?>
	
<br/>
<div id="comment" align="justify" style="font-size: 80%;">
	Примечания: 1) Команды, взявшие на себя повышенные экологические обязательства,отмечаются знаком <b>ну!</b><br/>
	2) Участники, которые не вышли на старт, и при этом не удалили свою заявку до окончания регистрации, при следующей заявке отмечаются знаком <b>(?!)</b><br/>
</div>


<br/>



