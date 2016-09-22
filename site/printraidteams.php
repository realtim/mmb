<?php
return;

// Общие настройки
include("settings.php");
// Библиотека функций
include("functions.php");

// Проверяем, что передали идентификатор ММБ
$RaidId = (int) mmb_validateInt($_REQUEST, 'RaidId', -1);
if ($RaidId < 0)
{
	echo 'Не указан ММБ';
	return;
}

print('Дистанция;Номера карточек<br />'."\n");
$sql = "select t.team_num, d.distance_name
	from Teams t
		inner join Distances d on t.distance_id = d.distance_id
	where t.team_hide = 0 and d.raid_id = $RaidId
	order by d.distance_name, team_num asc";
$Result = MySqlQuery($sql);

$PredDistance = "";
$CardsArr = "";
while ($Row = mysql_fetch_assoc($Result))
{
	if ($Row['distance_name'] <> $PredDistance)
	{
		if ($PredDistance <> "")
		// записываем накопленное
		{
			print($CardsArr.'<br />'."\n");
		}
		$PredDistance = $Row['distance_name'];
		$CardsArr = $PredDistance.';'.$Row['team_num'];
	}
	else
	// копим
	{
		$CardsArr = $CardsArr.','.$Row['team_num'];
	}
}
// записываем накопленное
print($CardsArr.'<br />'."\n");
print('====<br />'."\n");

print('Дистанция;Номер;GPS;Название;Участники;Карты<br />'."\n");
$sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name,
	t.team_mapscount, d.distance_name, d.distance_id
	from Teams t
		inner join Distances d on t.distance_id = d.distance_id
	where t.team_hide = 0 and d.raid_id = $RaidId
	order by d.distance_name, team_num asc";
$Result = MySqlQuery($sql);

while ($Row = mysql_fetch_assoc($Result))
{
	$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, u.user_id
		from TeamUsers tu
			inner join Users u on tu.user_id = u.user_id
		where tu.teamuser_hide = 0 and team_id = {$Row['team_id']}
		order by tu.teamuser_id asc";
	$UserResult = MySqlQuery($sql);

	$First = 1;
	while ($UserRow = mysql_fetch_assoc($UserResult))
	{
		if ($First == 1)
		{
			print($Row['distance_name'].';'.$Row['team_num'].';'.($Row['team_usegps'] == 1 ? '+' : '').';'.$Row['team_name'].';'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';'.$Row['team_mapscount'].'<br />'."\n");
			$First = 0;
		}
		else
		{
			print(';;;;'.$UserRow['user_name'].' '.$UserRow['user_birthyear'].';<br />'."\n");
		}
	}
}
?>
<br />
