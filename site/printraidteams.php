<?

    // говорим браузеру, чтобы не кешировал
	include("nocash.php");
        // Общие настройки
	include("settings.php");
	// Библиотека функций
	include("functions.php");

        // Проверяем, что передали  идентификатор ММБ
        if (empty($RaidId)) 
	{
		    echo 'Не указан ММБ';
		    return;
	
	}


		print('Дистанция;Номера карточек</br>'."\r\n");

		$sql = "select t.team_num, d.distance_name
		        from  Teams t
			     inner join  Distances d 
			     on t.distance_id = d.distance_id
			where t.team_hide = 0 and d.raid_id = ".$RaidId."
			order by d.distance_name, team_num asc"; 
		//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
		
		$PredDistance = '';
		$CardsArr = '';
		while ($Row = mysql_fetch_assoc($Result))
		{
                    if ($Row['distance_name'] <> $PredDistance)
		    {
	                if ($PredDistance <> '')
			{
			      // записываем накопленное
	                      print($CardsArr.'</br>'."\r\n");
		        }
                       $PredDistance  =  $Row['distance_name'];
     		       $CardsArr = $PredDistance.';'.$Row['team_num'];

		      
		    } else {
		      // копим
		       $CardsArr = $CardsArr.','.$Row['team_num'];
		    }
                }

	        // записываем накопленное
                print($CardsArr.'</br>'."\r\n");

                print('====</br>'."\r\n");
		
		print('Дистанция;Номер;GPS;Название;Участники;Карты</br>'."\r\n");

		$sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name, 
		               t.team_mapscount, d.distance_name, d.distance_id
		        from  Teams t
			     inner join  Distances d 
			     on t.distance_id = d.distance_id
			where t.team_hide = 0 and d.raid_id = ".$RaidId."
			order by d.distance_name, team_num asc"; 
		//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
		
		
//		print('<table  border = "0" cellpadding = "0" cellspacing = "0">'."\r\n");  
//		print('<tr><td>Дистанция;Номер;GPS;Название;Участники;Карты</td><td>'."\r\n");



		
		while ($Row = mysql_fetch_assoc($Result))
		{
			$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, tu.level_id, u.user_id 
			        from  TeamUsers tu
				     inner join  Users u
				     on tu.user_id = u.user_id
				where tu.teamuser_hide = 0 and team_id = ".$Row['team_id']."
				order by tu.teamuser_id asc"; 
			//echo 'sql '.$sql;
			$UserResult = MySqlQuery($sql);

                        $First = 1;
			while ($UserRow = mysql_fetch_assoc($UserResult))
			{
                          if ($First == 1) 
			  {
			     print($Row['distance_name'].';'.$Row['team_num'].';'.($Row['team_usegps'] == 1 ? '+' : '').';'.$Row['team_name'].';'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';'.$Row['team_mapscount'].'</br>'."\r\n");
  
		//	     print('<tr><td>'.$Row['distance_name'].';'.$Row['team_num'].';'.($Row['team_usegps'] == 1 ? '+' : '').';'.$Row['team_name'].';'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';'.$Row['team_mapscount'].'</td><td>'."\r\n");
                             $First = 0;			  

			  } else { 

				print(';;;;'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';</br>'."\r\n");
		//		print('<tr><td>;;;;'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';'.'</td><td>'."\r\n");

			  }
                  	}  
		}

//		print('</table>'."\r\n");
	

?>



		</br>



