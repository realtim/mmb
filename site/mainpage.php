<?php
// +++++++++++ Показ списка всех маршбросков ++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

print('<div align = "center" class = "HeadText">Московский Марш-бросок<br>
cоревнования по туристскому ориентированию</div>'."\r\n");

print('<p align = "left" style = "text-align: justify;">Основная идея: полностью автономное прохождение лесного маршрута с
отметкой на контрольных пунктах. Общая идея соревнований и многие
детали взяты из традиционного <a href = "http://www.kandid.ru">питерского Марш-броска</a> </p>'."\r\n");

print('<p align = "left" style = "text-align: justify;">Московский Марш-бросок проводится дважды в год: в
середине мая и в конце октября. Обычно, ММБ состоит из двух этапов ориентирования с ночевкой (обязательным
свободным временем) между ними. Организаторы стараются включить в
соревнования ночное ориентирование и различные неожиданности на дистанции.</p>'."\r\n");


//print(''."\r\n");

print('<table cellpadding = "10" border = "0" width = "100%">'."\r\n");
print('<tr class="gray">'."\r\n");
print('<td>Марш-бросок</td>'."\r\n");
print('<td colspan="2">Время и место</td>'."\r\n");
print('<td>Результаты и карты, параметры дистанции (<a href = "#help">?</a>)</td>'."\r\n");
print("</tr>\r\n");

// Вытаскиваем информацию о марш-бросках
$resultRaids = MySqlQuery('SELECT * FROM Raids r WHERE raid_registrationenddate is not null ORDER BY raid_id  DESC');

$RaidsCount = mysql_num_rows($resultRaids);
while ($rowRaids = mysql_fetch_assoc($resultRaids)) {

        
	$nextRaidId = $rowRaids['raid_id'];
	$RaidName = trim($rowRaids['raid_name']);
	$RaidPeriod = trim($rowRaids['raid_period']);
	$RaidStartPoint = $rowRaids['raid_startpoint'];
	$RaidFinishPoint = $rowRaids['raid_finishpoint'];


	$RaidRulesLink = CSql::raidFileLink($nextRaidId, 1, true);	// Cсылка на положение в загруженных файлах
	$RaidStartLink = CSql::raidFileLink($nextRaidId, 10, true); // Информация о старте

	//echo $i;
	$TrClass = $RaidsCount%2 == 0 ? 'yellow' : 'green';
	print("<tr class=\"$TrClass\">\r\n");

	$RaidsCount--;

 
	//class = "yellow"
	print('<td><a href="?RaidId='.$nextRaidId.'">'.$RaidName.'</a></td>'."\r\n");

	print('<td><a href="'.$RaidRulesLink.'" target="_blank">'.$RaidPeriod.'</a></td>'."\r\n");
        
	if (empty($RaidStartLink))
	{
		print("<td>$RaidStartPoint - $RaidFinishPoint</td>\r\n");
	} else {
		print('<td><a href="'.$RaidStartLink.'" target="_blank">'.$RaidStartPoint."</a> - $RaidFinishPoint</td>\r\n");
	}
        
	print("<td>\r\n");
	 
	// Вытаскиваем информацию о дистанциях
	$resultDistance = MySqlQuery("SELECT d.*,
					(
					 select count(team_id) 
					 from  Teams t 
					 where t.distance_id = d.distance_id
					            and t.team_hide = 0
					) as teamscount,
					(
					 select count(team_id) 
					 from  Teams t 
					 where t.distance_id = d.distance_id
					            and t.team_hide = 0
						    and t.team_greenpeace = 1
					) as teamsgreencount,
					COALESCE((
					 select sum(COALESCE(team_mapscount, 0)) 
					 from  Teams t 
					 where t.distance_id = d.distance_id
					            and t.team_hide = 0
					), 0) as teammapscount,
					(
					 select count(tu.teamuser_id) 
					 from  Teams t 
					          inner join  TeamUsers tu
					          on t.team_id = tu.team_id
					 where t.distance_id = d.distance_id
					            and t.team_hide = 0
					            and tu.teamuser_hide = 0
					) as teamuserscount
			            FROM  Distances d 
				    WHERE d.distance_hide = 0 and raid_id = $nextRaidId  ORDER BY distance_name ASC");
	
        $DistancesCount = 0;
         
	while ($rowDistance = mysql_fetch_assoc($resultDistance)) {
	
	        $DistancesCount++;
		
		if ($DistancesCount > 1) {
			print("<br>\r\n");
		}
	
		$DistanceName = $rowDistance['distance_name'];
		$DistanceLink = $rowDistance['distance_resultlink'];
		$DistanceData = $rowDistance['distance_data'];
		$DistanceCounters = '('.$rowDistance['teamscount'].'/<span style = "color:#007700;">'.$rowDistance['teamsgreencount'].'</span> / '.$rowDistance['teammapscount'].' / '.$rowDistance['teamuserscount'].')';
	
	        // Если ссылка пустая - заменяем на сгенерированную
	        if (trim($DistanceLink) == '')
		{
		  //$DistanceLink = '?action=ViewRaidTeams&RaidId='.$nextRaidId;
		  $DistanceLink = "?RaidId=$nextRaidId";
		}
	
	        print('<a href="'.$DistanceLink.'">'."$DistanceName</a>: $DistanceCounters $DistanceData\r\n");
	
	}
	mysql_free_result($resultDistance);
        print("</td>\r\n");
	print("</tr>\r\n");

}
mysql_free_result($resultRaids);
print("</table>\r\n");
//print('</body></html>'."\r\n");

print('<hr>'."\r\n");
print('<p align="left" style="text-align: justify;">'."\r\n");
print('<a name="help">Обычно за месяц до проведения ММБ в сеть выкладывается положение, за неделю - раскрывается место старта.<br/>
 После названия дистанции в скобках через слэш указывается число: команд (зелёным - "<a href="http://community.livejournal.com/_mmb_/2010/09/24/">нет сломанным унитазам</a>"), заказанных карт и участников.
 Дальше идут параметры дистанции: километраж - по прямой между вершинами КП, в скобках - количество КП, через слэш - этапы. 
 После снятия дистанции на месте КП вместо компостера может висеть кусочек светоотражалки, если об этом позаботился снимающий, если не сгрызли мыши и т.п.
 Карты дистанции выкладываются в разделе "Файлы", обновленные карты местности (без дистанции), а также векторные карты - смотрите в разделе "<a href="http://slazav.mccme.ru/maps/">Карты</a>"
'."\r\n");
print("</p>\r\n");

?>
