<?php

// функция преобразования вывода данных
  function ConvertTeamLevelPointsToHTML ($LevelPointNames,$LevelPointPenalties,$TeamLevelPoints,$LevelId, $DisabledResultText) {
	
	  
	  
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
	else $TeamPoints = array();

//      echo count($Names);
  //  echo count($Penalties);
    //echo count($TeamPoints);

        if (!empty($TeamLevelPoints) and  count($Names) <> count($TeamPoints))
        {	
           print('Ошибка данных по КП'."\r\n");
	   return;
        }

        print('<table style = "text-align: center; font-size: 100%; border-style: solid; border-width: 1px; border-color: #000000;">'."\r\n");
	print('<tr>'."\r\n");
	print('<td align = "left">Взяты: &nbsp; </td>'."\r\n");
	print('<td style = "border-left-style: solid; border-left-width: 1px; border-left-color: #000000;">'."\r\n");

        // Проверяем, что не отмечены все checkbox
        // ===  а не == - так написано в инструкции к функции
        //if (strpos($TeamLevelPoints, '0') === false)
        if (!strstr($TeamLevelPoints, '0') && !empty($TeamLevelPoints))
        {
              $AllChecked = 'checked';
        } else {
              $AllChecked = '';
        }
        // Прописываем javascript, который ставит или сбрасывает все checkbox-ы
        print('Все</br><input type = "checkbox" name = "chkall" '.$AllChecked.' '.$DisabledResultText.' OnClick = "javascript:'."\r\n");
		
        for ($i = 0; $i < count($Names); $i++)
        {
          print('document.TeamResultDataForm.Level'.$LevelId.'_chk'.$i.'.checked = this.checked;'."\r\n");
        } 
        print('">'."\r\n");
        print('</br>&nbsp;'."\r\n");
	print('</td>'."\r\n");
	
	for ($i = 0; $i < count($Names); $i++) 
        {
		//print('<td>'.$Names[$i].'</br>('.$Penalties[$i].')'."\r\n");
                print('<td style = "border-left-style: solid; border-left-width: 1px; border-left-color: #000000;">'.$Names[$i]."\r\n");
                $Checked = (isset($TeamPoints[$i]) && ($TeamPoints[$i] == 1)) ? 'checked' : '';
                print('</br><input type = "checkbox" name = "Level'.$LevelId.'_chk'.$i.'"  '. $Checked.'  '.$DisabledResultText.' OnClick = "">'."\r\n");
                print('</br>'.$Penalties[$i]."\r\n");
		print('</td>'."\r\n");
	}

  
	print('</tr>'."\r\n");
	print('</table>'."\r\n");


  return;	
  }
  // Конец функции вывода данных по КП

	print('<div style = "margin-top: 15px;">&nbsp;</div>'."\r\n");

   // считаем, что все переменные уже определны. если нет - выходим
         if (empty($TeamId))
	 {
		return;
	 } 

	 // Текущий пользователь
//	 $NowUserId = GetSession($SessionId);

         // результаты не могут отображаться, если команда только вводится 
         // Или ещё не закончена регистрация
         if ($viewmode == 'Add')
	 {
            return;
         }
 

         // Правильнее возможность подтверждать результаты открывать не после окончания регистрации, а после 
         // старта первого этапа (а м..б. и позже)
         // М.б. разделить: для модераторов сразу после окончания регистрации,
          // для остальных - после даты публикации результатов
           // Не, ещё круче: модераторам сразу после открытия старата первого этапа,
           // остальным - сразу после закрытия финиша последнего!
	      $sql = "select r.raid_resultpublicationdate, r.raid_registrationenddate, 
                        CASE WHEN r.raid_registrationenddate is not null and YEAR(r.raid_registrationenddate) <= 2011 
                             THEN 1 
                             ELSE 0 
                        END as oldmmb,
			CASE WHEN r.raid_registrationenddate is not null and r.raid_registrationenddate <= NOW() 
                             THEN 1 
                             ELSE 0 
                        END as showresultfield,
                        r.raid_id,
                        t.team_moderatorconfirmresult,
                        t.team_confirmresult
		 from   Raids r
                        inner join Distances  d
                        on r.raid_id = d.raid_id
                        inner join Teams t
                        on d.distance_id = t.distance_id
		 where t.team_id = ".$TeamId; 
		//echo 'sql '.$sql;

                $Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
                mysql_free_result($Result);
		$RaidPublicationResultDate = $Row['raid_resultpublicationdate'];
		$RaidId = $Row['raid_id'];
                $OldMmb = $Row['oldmmb'];
		$ModeratorConfirmResult = $Row['team_moderatorconfirmresult'];
		$TeamConfirmResult = $Row['team_confirmresult'];
		$RaidShowResultField = $Row['showresultfield'];

            // Тут надо проверить глобальный запрет на правку данных по ММБ

         if ($RaidShowResultField <> 1)
	 {
            return;
         }


            // нужна, возможно, доп проверка на время,чтобы не показывать пустые результаты с данными этапа

	    if (CheckModerator($SessionId, $RaidId))
	    {
	      $Moderator = 1;
	    } else {
	      $Moderator = 0;
	    }
      
	    if  (CheckTeamUser($SessionId, $TeamId))
	    {
		  $TeamUser = 1;
	    } else {
		  $TeamUser = 0;
	    }


       // Общая проверка возможности редактирования
        // 24,01,2012 Добавил ограничение что править результаты участнику команды нельзя, 
         // если в карточке команды стоит, что они подтверждены (тогда сначала нужно снятья галку оттуда)
          // 05.02.2012 Убрал для старых ММБ возможности править кому угодно

	    if ($Moderator or ($TeamUser and !$TeamModeratorConfirmResult and !$TeamConfirmResult))
	    {
	      $AllowEditResult = 1;
              $NextResultActionName = 'ChangeTeamResult';
	      $DisabledResultText = '';
	      $OnSubmitResultFunction = 'return ValidateTeamResultDataForm();';
              

	    } else { 
	      $AllowEditResult = 0;
              $NextResultActionName = '';
	      //$DisabledResultText = 'disabled';
             $DisabledResultText = 'readonly';
	      $OnSubmitResultFunction = 'return false;';
	    }
 

?>

<script language = "JavaScript">

        // Функция проверки правильности заполнения формы
	function ValidateTeamResultDataForm()
	{ 
	        document.TeamResultDataForm.action.value = "<? echo $NextResultActionName; ?>";
		return true;
	}
        // Конец проверки правильности заполнения формы



	// Функция отмены изменения
	function CancelResult()
	{ 
		document.TeamResultDataForm.action.value = "CancelChangeTeamResultData";
		document.TeamResultDataForm.submit();
	}
	
        // Функция выделения
	function SelectEntry(loElement)
	{ 
                 loElement.select();
	}

        // Выделеить все checkbox-ы 
	function CheckAll(oForm, chkName, checked)
	{
	  for (var i=0; i < oForm[chkName].length; i++) oForm[chkName][i].checked = checked;
	}


</script>


<?

  print('<form  name = "TeamResultDataForm"  action = "'.$MyPHPScript.'" method = "post" onSubmit = "'.$OnSubmitResultFunction.'">'."\r\n");
  print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\r\n");
  print('<input type = "hidden" name = "action" value = "">'."\r\n");
  print('<input type = "hidden" name = "view" value = "'.(($viewmode == "Add") ? 'ViewRaidTeams' : 'ViewTeamData').'">'."\r\n");
  print('<input type = "hidden" name = "TeamId" value = "'.$TeamId.'">'."\r\n");
  print('<input type = "hidden" name = "RaidId" value = "'.$RaidId.'">'."\r\n");
  
        

     print('<table border = "0" cellpadding = "10" style = "font-size: 80%">'."\r\n");  
		print('<tr class = "gray">'."\r\n");  
		print('<td width = "200">Этап</td>'."\r\n");  
		print('<td width = "350">Параметры старта/финиша</td>'."\r\n");  
		print('<td width = "100">Старт</td>'."\r\n");  
		print('<td width = "100">Финиш</td>'."\r\n");  
		print('<td width = "60">Штраф</td>'."\r\n");  
	//	print('<td width = "200">Невзятые КП</td>'."\r\n");  
		print('<td width = "150">Комментарий</td>'."\r\n");  
	

  //$TabIndex = 0;

           //  выводим данные только, когда минимальное время начала этапа меньше или равно текущему
               // Довольнг своеорбазно определяем год, чтобы не вводить его каждый раз
	   	   $sql = "select l.level_id, l.level_name, l.level_pointnames, l.level_starttype,
                                  l.level_pointpenalties, l.level_order, 
				  DATE_FORMAT(l.level_begtime,    '%d.%m %H:%i') as level_sbegtime,
				  DATE_FORMAT(l.level_maxbegtime, '%d.%m %H:%i') as level_smaxbegtime,
                                  DATE_FORMAT(l.level_minendtime, '%d.%m %H:%i') as level_sminendtime,  
                                  DATE_FORMAT(l.level_endtime,    '%d.%m %H:%i') as level_sendtime,  
                                  CASE WHEN COALESCE(YEAR(l.level_minendtime), YEAR(NOW())) = 
                                            COALESCE(YEAR(l.level_endtime), YEAR(NOW()))  
                                       THEN COALESCE(YEAR(l.level_minendtime), YEAR(NOW()))
                                       ELSE YEAR(NOW()) 
                                  END as level_sendyear , 
                                  CASE WHEN COALESCE(YEAR(l.level_maxbegtime), YEAR(NOW())) = 
                                            COALESCE(YEAR(l.level_begtime), YEAR(NOW()))  
                                       THEN COALESCE(YEAR(l.level_maxbegtime), YEAR(NOW()))
                                       ELSE YEAR(NOW()) 
                                  END as level_sbegyear , 
                                  l.level_begtime, l.level_maxbegtime, l.level_minendtime, l.level_endtime,
                                  tl.teamlevel_begtime, tl.teamlevel_endtime,
                                  DATE_FORMAT(tl.teamlevel_begtime, '%Y') as teamlevel_sbegyear,
                                  DATE_FORMAT(tl.teamlevel_begtime, '%d%m') as teamlevel_sbegdate,
                                  DATE_FORMAT(tl.teamlevel_begtime, '%H%i') as teamlevel_sbegtime,
                                  DATE_FORMAT(tl.teamlevel_endtime, '%Y') as teamlevel_sendyear,
                                  DATE_FORMAT(tl.teamlevel_endtime, '%d%m') as teamlevel_senddate,
                                  DATE_FORMAT(tl.teamlevel_endtime, '%H%i') as teamlevel_sendtime,
                                  tl.teamlevel_points, tl.teamlevel_penalty, 
                                  tl.teamlevel_id, tl.teamlevel_comment, t.level_id as teamnotonlevelid
       		           from  Teams t 
                                 inner join Distances d 
                                 on t.distance_id = d.distance_id 
                                 inner join Levels l  
                                 on d.distance_id = l.distance_id 
                                 left outer join Levels l1
                                 on t.level_id = l1.level_id
                                 left outer join TeamLevels tl
                                 on l.level_id =  tl.level_id
                                    and t.team_id = tl.team_id   
                                    and tl.teamlevel_hide = 0
                           where l.level_order < COALESCE(l1.level_order, l.level_order + 1) and  t.team_id = ".$TeamId;

//.
// Пока убрал ограничение по выдаче этапов от времени просмотра
         // $sql =   $sql." and l.level_begtime <= now() ";
          $sql =   $sql." order by  l.level_order ";

           //echo $sql;
	   $Result = MySqlQuery($sql);  
       
           // теперь цикл обработки данных по этапам 
          while ($Row = mysql_fetch_assoc($Result))
	   {

              // По этому ключу потом определяем, есть ли уже строчка в teamLevels или её нужно создать 

              $TeamLevelId = $Row['teamlevel_id'];
              $LevelStartType = $Row['level_starttype'];
              $LevelPointNames =  $Row['level_pointnames'];
              $LevelPointPenalties =  $Row['level_pointpenalties'];
              $TeamLevelPoints = ($TeamLevelId > 0) ?  $Row['teamlevel_points'] : '&nbsp;';
              $TeamLevelComment = $Row['teamlevel_comment'];

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
       
              //$LevelStartTypeText  = $LevelStartTypeText.' / '.$Row['level_sminendtime'].' - '.$Row['level_sendtime'];

              // сторим строчку для текущего этапа  
	      print('<tr><td>'.$Row['level_name'].'</td>
                          <td>'.$LevelStartTypeText.'</td>
                          <td>'."\r\n"); 

              if ($LevelStartType == 1)
              {
                // год записываем из данных этапа (макс и мин время старта/финишв
		print('<input type="hidden" maxlength = "4" name="Level'.$Row['level_id'].'_begyear" size="3"
                                        value="'.$Row['level_sbegyear'].'" >'."\r\n"); 


		// Если даты совпадают - отключаем поле даты с помощью readonly
		// Нельзя просто ставить disabled, т.к. в этом случае параметр не передается  
                if (substr(trim($Row['level_sbegtime']), 0, 5) == substr(trim($Row['level_smaxbegtime']), 0, 5))
                {
                  $TeamLevelBegDate = substr(trim($Row['level_sbegtime']), 0, 2).substr(trim($Row['level_sbegtime']), 3, 2);
                  $BegDateReadOnly = 'readonly';

		} else {
                  $TeamLevelBegDate = $Row['teamlevel_sbegdate'];
                  $BegDateReadOnly = '';
		}

                print('<input type="Text" maxlength = "4" name="Level'.$Row['level_id'].'_begdate" size="3"
                                        value="'.$TeamLevelBegDate.'" 
                                        tabindex = "'.(++$TabIndex).'" '.$DisabledResultText.' '.$BegDateReadOnly.' 
                                        title = "ддмм - день месяц без разделителя"
                                        onclick = "this.select();"
					onkeydown = "if (event.keyCode == 13 && this.value.length == 4) {document.TeamResultDataForm.Level'.$Row['level_id'].'_begtime.focus();}">'."\r\n");


		print('<input type="Text" maxlength = "4"  name="Level'.$Row['level_id'].'_begtime" size="3"
                                        value="'.$Row['teamlevel_sbegtime'].'" 
                                        tabindex = "'.(++$TabIndex).'" '.$DisabledResultText.' 
                                        onclick = "this.select();"
                                        title = "ччмм - часы минуты без разделителя">'."\r\n"); 
              } else {
                 print('-'."\r\n"); 
              }
              print('</td><td>'."\r\n");


         
	      print('<input type="hidden" maxlength = "4" name="Level'.$Row['level_id'].'_endyear" size="3"
                                        value="'.$Row['level_sendyear'].'" >'."\r\n"); 

              // Если даты совпадают - отключаем поле даты с помощью readonly
              // Нельзя просто ставить disabled, т.к. в этом случае параметр не передается  
              if (substr(trim($Row['level_sendtime']), 0, 5) == substr(trim($Row['level_sminendtime']), 0, 5))
              {
                  $TeamLevelEndDate = substr(trim($Row['level_sendtime']), 0, 2).substr(trim($Row['level_sendtime']), 3, 2);
                  $EndDateReadOnly = 'readonly';

              } else {
                  $TeamLevelEndDate = $Row['teamlevel_senddate'];
                  $EndDateReadOnly = '';
              }

	      print('<input type="Text" maxlength = "4" name="Level'.$Row['level_id'].'_enddate" size="3"
                                        value="'.$TeamLevelEndDate.'" 
                                        tabindex = "'.(++$TabIndex).'" '.$DisabledResultText.' '.$EndDateReadOnly.' 
                                        title = "ддмм - день месяц без разделителя"
                                        onclick = "this.select();"
					onkeydown = "if (event.keyCode == 13 && this.value.length == 4) {document.TeamResultDataForm.Level'.$Row['level_id'].'_endtime.focus();}">'."\r\n");
              
              print('<input type="Text" maxlength = "4"  name="Level'.$Row['level_id'].'_endtime" size="3"
                                        value="'.$Row['teamlevel_sendtime'].'" 
                                        tabindex = "'.(++$TabIndex).'" '.$DisabledResultText.' 
                                        onclick = "this.select();"
                                        title = "ччмм - часы минуты без разделителя">'."\r\n");

              print('</td><td>&nbsp;'."\r\n");
	 //     ConvertTeamLevelPointsToHTML($Row['level_pointnames'], $Row['level_pointpenalties'], $Row['teamlevel_points'], $Row['level_id']);
          //    print('</td><td>'."\r\n");
              print('<input type="Text" maxlength = "4"  name="Level'.$Row['level_id'].'_penalty" size="3"
                                        value="'.(int)$Row['teamlevel_penalty'].'" 
                                        readonly
                                        tabindex = "'.(++$TabIndex).'" '.$DisabledResultText.' 
                                        title = "">'."\r\n");
              print('</td><td>'."\r\n");
              print('<input type="text" name="Level'.$Row['level_id'].'_comment" size="15"
                                        value="'.$TeamLevelComment.'" 
                                        tabindex = "'.(++$TabIndex).'" '.$DisabledResultText.' 
                                        onclick = "this.select();"
                                        title = "">'."\r\n");
              print('</td></tr>'."\r\n"); 
            
              // Следующая  строка - невзятые КП
             print('<tr><td colspan = "6"  style = "padding-top: 0px; border-bottom-style: dotted; border-bottom-width: 1px; border-bottom-color: #000000;">'."\r\n");
	     ConvertTeamLevelPointsToHTML($Row['level_pointnames'], $Row['level_pointpenalties'], $Row['teamlevel_points'], $Row['level_id'], $DisabledResultText);
             print('</td></tr>'."\r\n"); 

              //$TabIndex = $TabIndex + 5

	   }
           // Конец цикла по этапам
	   mysql_free_result($Result);      

           // закрываем  таблицу
	   print('</table>'."\r\n"); 


	   if ($AllowEdit == 1) 
           {
	    // Выводим кнопки

            print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0">'."\r\n");

	    print('<tr><td class = "input"  style =  "padding-top: 10px;">'."\r\n");
	    $TabIndex++;
	    print('<input type="button" onClick = "javascript: if (ValidateTeamResultDataForm()) submit();"
                   name="SaveChangeResultButton" value="Сохранить результаты" tabindex = "'.$TabIndex.'">'."\r\n");
	    $TabIndex++;
	    print('<select name="CaseView" onChange = "javascript:document.TeamResultDataForm.view.value = document.TeamResultDataForm.CaseView.value;"  
                    class = "leftmargin" tabindex = "'.$TabIndex.'">'."\r\n"); 
	    print('<option value = "ViewTeamData"  selected >и остаться на этой странице'."\r\n"); 
	    print('<option value = "ViewRaidTeams" >и перейти к списку команд'."\r\n"); 
	    print('</select>'."\r\n"); 
	    $TabIndex++;
	    print('<input type="button" onClick = "javascript: CancelResult();"  name="CancelButton" value="Отмена"
                     tabindex = "'.$TabIndex.'">'."\r\n"); 

	    print('</td></tr>'."\r\n"); 
            print('</table>'."\r\n"); 

           }

        // закрываем  форму
        print('</form>'."\r\n"); 

?>
