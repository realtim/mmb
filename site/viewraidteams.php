

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

</script>


<?



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

        // Проверяем, что передали  идентификатор ММБ
        if (empty($RaidId)) 
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
                $OrderType = $_POST['OrderType'];
		$OrderString = '';


		  $sql = "select CASE WHEN  now() <= COALESCE(r.raid_resultpublicationdate, now()) 
                                      THEN 'Num' 
                                      ELSE 'Place' 
                                  END as ordertype,
                                  r.raid_resultpublicationdate,
                                  COALESCE(r.raid_ruleslink, '') as raid_ruleslink,
                                  COALESCE(r.raid_startlink, '') as raid_startlink
			  from  Raids r
			  where r.raid_id = ".$RaidId."
                          "; 
            
		  $Result = MySqlQuery($sql);
		  $Row = mysql_fetch_assoc($Result);
		  mysql_free_result($UserResult);
                // Скорее всего нужно использоать только в сравнении с текущим временем
                $RsultPublicated =  $Row['raid_resultpublicationdate'];
                $RaidRulesLink = trim($Row['raid_ruleslink']);
                $RaidStartLink = trim($Row['raid_startlink']);

                // если порядок не задан смотрим на соотношение временени публикации и текущего
                if  (empty($OrderType))
                {
                  $OrderType = trim($Row['ordertype']);
                }

            	print('<div align = "left" style = "font-size: 80%;">'."\r\n");
		print('Сортировать по '."\r\n");
		print('<select name="OrderType" style = "margin-left: 10px; margin-right: 20px;"  onchange = "if (this.value==\'Num\'){document.RaidTeamsForm.LevelId.disabled=true;}else{document.RaidTeamsForm.LevelId.disabled=false;}document.RaidTeamsForm.submit();"  tabindex = "'.(++$TabIndex).'" '.$DisabledText.'>'."\r\n"); 
	        print('<option value = "Num" '.($OrderType == 'Num' ? 'selected' :'').' >убыванию номера'."\r\n");
	        print('<option value = "Place" '.($OrderType == 'Place' ? 'selected' :'').' >возрастанию места'."\r\n");
	        print('</select>'."\r\n");  

		print('Фильтровать: '."\r\n"); 

	        $sql = "select distance_id, distance_name
                        from  Distances where raid_id = ".$RaidId." order by distance_name"; 
		//echo 'sql '.$sql;
		$Result = MySqlQuery($sql);
                
		print('<select name="DistanceId" style = "margin-left: 10px; margin-right: 5px;" onchange = "document.RaidTeamsForm.submit();"  tabindex = "'.(++$TabIndex).'">'."\r\n"); 
                $distanceselected =  (0 == $_POST['DistanceId'] ? 'selected' : '');
		  print('<option value = "0" '.$$distanceselected.' >дистанцию'."\r\n");
	        while ($Row = mysql_fetch_assoc($Result))
		{
		  $distanceselected = ($Row['distance_id'] == $_POST['DistanceId'] ? 'selected' : '');
		  print('<option value = "'.$Row['distance_id'].'" '.$distanceselected.' >'.$Row['distance_name']."\r\n");
		}
		print('</select>'."\r\n");  
		mysql_free_result($Result);		

		$sql = "select level_id, d.distance_name, CONCAT(trim(level_name), ' (', trim(d.distance_name), ')') as level_name
                          from  Levels l 
                                inner join Distances d 
                                on l.distance_id = d.distance_id  
                           where d.raid_id = ".$RaidId;
                if (!empty($_POST['DistanceId']))
                {
                     $sql = $sql." and d.distance_id = ".$_POST['DistanceId']; 
                }
                $sql = $sql." order by d.distance_name, l.level_order "; 
		//  echo 'sql '.$sql;
		$Result = MySqlQuery($sql);

		print('<select name="LevelId" '.(($OrderType=='Num') ? 'disabled' : '').'  style = "margin-left: 5px; margin-right: 10px;"  onchange = "document.RaidTeamsForm.submit();" tabindex = "'.(++$TabIndex).'" >'."\r\n"); 
	          $levelselected =  (0 == $_POST['LevelId'] ? 'selected' : '');
		print('<option value = "0" '.$levelselected.' >этап'."\r\n");

		while ($Row = mysql_fetch_assoc($Result))
		{
		    $levelselected = ($Row['level_id'] == $_POST['LevelId'] ? 'selected' : '');
		    print('<option value = "'.$Row['level_id'].'" '.$levelselected.' >'.$Row['level_name']."\r\n");
		}
		mysql_free_result($Result);
	        print('</select>'."\r\n");  
		print('</div>'."\r\n");
            	print('<div align = "left" style = "margin-top:10px; margin-bottom:10px; font-size: 100%;">'."\r\n");
		print('<a  style = "font-size:80%; margin-right: 15px;" href = "'.$RaidRulesLink.'" target = "_blank">Положение</a> '."\r\n");
		print('<a  style = "font-size:80%; margin-right: 15px;" href = "'.$RaidStartLink.'" target = "_blank">Информация о старте</a> '."\r\n");
		print('<a  style = "font-size:80%;" href = "'.trim(str_replace('index.php','printraidteams.php?RaidId=',$MyPHPScript)).$RaidId.'" target = "_blank">Список для печати</a>'."\r\n");
		print('</div>'."\r\n");

                // Показываем этапы
   	        $sql = "select  d.distance_name, l.level_id, l.level_name, 
                                l.level_pointnames, l.level_starttype,
                                l.level_pointpenalties, l.level_order, 
				  DATE_FORMAT(l.level_begtime,    '%d.%m %H:%i') as level_sbegtime,
				  DATE_FORMAT(l.level_maxbegtime, '%d.%m %H:%i') as level_smaxbegtime,
                                  DATE_FORMAT(l.level_minendtime, '%d.%m %H:%i') as level_sminendtime,  
                                  DATE_FORMAT(l.level_endtime,    '%d.%m %H:%i') as level_sendtime,  
                                l.level_begtime, l.level_maxbegtime, l.level_minendtime, 
                                l.level_endtime
                        from  Levels l  
                              inner join Distances d                              
                              on d.distance_id = l.distance_id 
                        where   d.raid_id = ".$RaidId;

              //  $sql = $sql." and l.level_begtime <= now() "; 

                if (!empty($_POST['DistanceId']))
                {
                     $sql = $sql." and d.distance_id = ".$_POST['DistanceId']; 
                }
                if (!empty($_POST['LevelId']))
                {
                     $sql = $sql." and l.level_id = ".$_POST['LevelId']; 
                }
                $sql = $sql." order by d.distance_name, l.level_order "; 

	   //     echo $sql;

		print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");  
		print('<tr class = "gray">'."\r\n");  
		print('<td width = "70">Дистанция</td>'."\r\n");  
		print('<td width = "200">Этап (по ссылкам - карты)</td>'."\r\n");  
		print('<td width = "400">Тип старта (границы по времени)'."\r\n");  
		print('<td width = "70">Число КП'."\r\n");  
		print('</tr>'."\r\n");  
		$Result = MySqlQuery($sql);  
       
		// теперь цикл обработки данных по этапам 
		while ($Row = mysql_fetch_assoc($Result))
		{

		  // По этому ключу потом определяем, есть ли уже строчка в teamLevels или её нужно создать 

		  $TeamLevelId = $Row['teamlevel_id'];
		  $LevelStartType = $Row['level_starttype'];
		  $LevelPointNames =  $Row['level_pointnames'];
		  $LevelPointPenalties =  $Row['level_pointpenalties'];
               //   $LevelMapLink = $Row['level_maplink'];
                 
                  $PointsCount = count(explode(',', $LevelPointNames));

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
		      $LevelStartTypeText = 'По готовности (';
		      if (substr(trim($Row['level_sbegtime']), 0, 5) == substr(trim($Row['level_smaxbegtime']), 0, 5))
		      { 
			$LevelStartTypeText = $LevelStartTypeText.$Row['level_sbegtime'].' - '.substr(trim($Row['level_smaxbegtime']), 6);
		      } else {
			  $LevelStartTypeText = $LevelStartTypeText.$Row['level_sbegtime'].' - '.$Row['level_smaxbegtime'];
		      }
		      $LevelStartTypeText = $LevelStartTypeText.')/('; 

		  } elseif ($LevelStartType == 2) {
		    $LevelStartTypeText = 'Общий ('.$Row['level_sbegtime'];
		    $LevelStartTypeText = $LevelStartTypeText.')/('; 

		  } elseif ($LevelStartType == 3) {
		    $LevelStartTypeText = 'Во время финиша (';

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

		  $LevelStartTypeText = $LevelStartTypeText.')'; 
       
		  // сторим строчку для текущего этапа  
		  print('<tr><td>'.$Row['distance_name'].'</td>'."\r\n"); 

                  print('<td>'.$Row['level_name']."\r\n"); 

                  $sql = 'select levelmaplink_url 
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
		  }
                  mysql_free_result($ResultMap);
                  if (!empty($MapString))   
                  {
		    $MapString = '('.trim(substr(trim($MapString), 1)).')';
		    print($MapString."\r\n"); 
		  }
		  print('</td>'."\r\n"); 

		  print('<td>'.$LevelStartTypeText.'</td>
                         <td>'.$PointsCount.'</td>
                         </tr>'."\r\n"); 

		}
		// конец цикла по этапам
		mysql_free_result($Result);
		print('</table>'."\r\n");

		if  ($OrderType == 'Num')
                {

                  // Сортировка по номеру (в обратном порядке)
		  $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, 
		               t.team_mapscount, d.distance_name, d.distance_id,
                               TIME_FORMAT(t.team_result, '%H:%i') as team_sresult,
			       COALESCE(l.level_name, '') as level_name
		        from  Teams t
			     inner join  Distances d 
			     on t.distance_id = d.distance_id
                             left outer join Levels l
                             on t.level_id = l.level_id 
			where t.team_hide = 0 and d.raid_id = ".$RaidId;

		   if (!empty($_POST['DistanceId']))
		   {
                     $sql = $sql." and d.distance_id = ".$_POST['DistanceId']; 
		   }
		   $sql = $sql." order by team_num desc"; 
                    

                } elseif ($OrderType == 'Place') {
                  // Сортировка по месту требует более хитрого запроса


		   if (empty($_POST['LevelId']))
		   {

		    $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, 
		               t.team_mapscount, d.distance_name, d.distance_id,
                               TIME_FORMAT(t.team_result, '%H:%i') as team_sresult,
			       COALESCE(l.level_name, '') as level_name,
                               CASE WHEN l.level_id is NULL and t.team_result > 0 THEN 0
                                    WHEN l.level_id is not NULL and t.team_result > 0 THEN ROUND(1.00/l.level_order, 4)
                                    ELSE 2
                               END as placegroup  
			  from  Teams t
				inner join  Distances d 
				on t.distance_id = d.distance_id
				left outer join Levels l
				on t.level_id = l.level_id 
			  where t.team_hide = 0 and d.raid_id = ".$RaidId;

		      if (!empty($_POST['DistanceId']))
		      {
			$sql = $sql." and d.distance_id = ".$_POST['DistanceId']; 
		      }

		      $sql = $sql." order by distance_name, placegroup asc, team_sresult desc "; 
		    
		     } else {
                          // Если фильтруем пожтапу, то другой запрос

		      $sql = " select t.team_num, t.team_id, t.team_usegps, t.team_name, 
				      t.team_mapscount, d.distance_name, d.distance_id,
				      COALESCE(lout.level_name, '') as level_name,
				      TIME_FORMAT(timediff(tl.teamlevel_endtime, 
					CASE l.level_starttype 
					    WHEN 1 THEN tl.teamlevel_begtime 
					    WHEN 2 THEN l.level_begtime 
					    WHEN 3 THEN (select MAX(tl2.teamlevel_endtime) 
							 from TeamLevels tl2
							      inner join Levels l2 
							      on tl2.level_id = l2.level_id
							 where tl2.team_id = tl.team_id 
							       and l2.level_order < l.level_order
							) 
					    ELSE NULL 
					END
				      ) + COALESCE(tl.teamlevel_penalty, 0)*60, '%H:%i') as  team_sresult,
                                      tl.teamlevel_penalty, tl.teamlevel_points, tl.teamlevel_comment
			    from  TeamLevels tl 
				  inner join Levels l 
				  on tl.level_id = l.level_id 
                                  inner join Teams t
                                  on t.team_id = tl.team_id 
				  inner join  Distances d 
				  on t.distance_id = d.distance_id
				  left outer join Levels lout
				  on t.level_id = lout.level_id 
			    where tl.teamlevel_hide = 0 and tl.level_id = ".$_POST['LevelId']." 
				  and timediff(tl.teamlevel_endtime, 
					CASE l.level_starttype 
					    WHEN 1 THEN tl.teamlevel_begtime 
					    WHEN 2 THEN l.level_begtime 
					    WHEN 3 THEN (select MAX(tl2.teamlevel_endtime) 
							 from TeamLevels tl2
							      inner join Levels l2 
							      on tl2.level_id = l2.level_id
							 where tl2.team_id = tl.team_id 
							       and l2.level_order < l.level_order
							) 
					    ELSE NULL 
					END
				      ) > 0
			    order by  team_sresult desc";

                     }

		} 


          	//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
	



                $tdstyle = 'padding: 5px 0px 2px 5px;';		
                $tdstyle = '';		
                $thstyle = 'border-color: #000000; border-style: solid; border-width: 1px 1px 1px 1px; padding: 5px 0px 2px 5px;';		
                $thstyle = '';		

//		print('<table width = "'.(($_POST['LevelId'] > 0 and  $OrderType == 'Place') ? '1015' : '815').'" border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");  
		print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");  
		print('<tr class = "gray">
		         <td width = "50" style = "'.$thstyle.'">Номер</td>
			 <td width = "350" style = "'.$thstyle.'">Команда (gps, дистанция, карт)</td>
			 <td width = "350" style = "'.$thstyle.'">Участники</td>
			 <td width = "50" style = "'.$thstyle.'">Результат</td>'."\r\n");
                if ($OrderType == 'Place')   
                {
                  // дополнительное поле место
		  print('  <td width = "50" style = "'.$thstyle.'">Место</td>'."\r\n");

                  // дополнительные поля в случае вывода  этапа
                  if ($_POST['LevelId'] > 0)
                  {
		    print('<td width = "50" style = "'.$thstyle.'">Штраф</td>
			   <td width = "150" style = "'.$thstyle.'">Комментарий</td>
			   <td width = "250" style = "'.$thstyle.'">Невзятые КП</td>'."\r\n");

                        

		  }
                }
		print('</tr>'."\r\n");
	
		$TeamsCount = mysql_num_rows($Result);
                 
                $TeamPlace = 0;
                $PredResult = ''; 
		
		while ($Row = mysql_fetch_assoc($Result))
		{

			if ($TeamsCount%2 == 0) {
			  $TrClass = 'yellow';
			} else {
			  $TrClass = 'green';
			} 

			$TeamsCount--;

 			print('<tr class = "'.$TrClass.'"><td style = "'.$tdstyle.'">'.$Row['team_num'].'</td><td style = "'.$tdstyle.'"><a href = "javascript:ViewTeamInfo('.$Row['team_id'].');">'.
			          $Row['team_name'].'</a> ('.($Row['team_usegps'] == 1 ? 'gps, ' : '').$Row['distance_name'].', '.$Row['team_mapscount'].')
                                   '.($Row['level_name'] == '' ? '' : '</br><i>Не вышла на этап: '.$Row['level_name'].'</i>').'</td><td style = "'.$tdstyle.'">'."\r\n");
		
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

			print('</td><td>'.$Row['team_sresult']."\r\n");
			print('</td>'."\r\n");
			if ($OrderType == 'Place')   
			{
			    print('<td width = "50" style = "'.$thstyle.'">'."\r\n");
                            if ($Row['team_sresult'] == '00:00')
                            {
                               print('&nbsp;');
                            } elseif($Row['team_sresult'] <>  $PredResult) {
                               print(++$TeamPlace);
			       $PredResult = $Row['team_sresult'];
                            } else {
                               print($TeamPlace);
			       $PredResult = $Row['team_sresult'];
                            }
			    print('</td>'."\r\n");

			    if ($_POST['LevelId'] > 0)
			    {
			      print('<td width = "50" style = "'.$thstyle.'">'.$Row['teamlevel_penalty'].'</td>
				      <td width = "50" style = "'.$thstyle.'">'.$Row['teamlevel_comment'].'</td>
				      <td width = "100" style = "'.$thstyle.'">'."\r\n");
			      ConvertTeamLevelPoints2($LevelPointNames, $LevelPointPenalties, $Row['teamlevel_points'], $_POST['LevelId']); 
			      print('</td>'."\r\n");



			    }
     
			}
			print('</tr>'."\r\n");
		}
		mysql_free_result($Result);
		print('</table>'."\r\n");
	

?>



		</br>



