<?php

if (!isset($MyPHPScript)) {
    return;
}

class CRights
{
    public static function canViewLogs($userId)
    {
        global $Administrator;
        return $Administrator || $userId == 2202; // Сергей Титов
    }

    public static function canCreateTeam($userId, $raidId): bool
    {
        // уже есть команда, но не админ / мод
        if (
            CSql::userTeamId($userId, $raidId)
            && !CSql::userAdmin($userId)
            && !CSql::userModerator($userId, $raidId)
        ) {
            return false;
        }

	    //  лотерея проведена
        if (CSql::lotteryStatus($raidId) === "LOT_END") {
            return false;
        }
    	    
        $raidStage = CSql::raidStage($raidId);
        return ($raidStage >= 1 && $raidStage < 2);
    }

    public static function canEditTeam($userId, $raidId, $teamId): bool
    {
        $teamMember = $teamId == CSql::userTeamId($userId, $raidId);
        $Super = CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);

        if (!$teamMember && !$Super) {
            return false;
        }

        $raidStage = CSql::raidStage($raidId);

        return (($teamMember && $raidStage < 2) || ($Super && $raidStage < 7));
    }

    // 21/05/2016 Проверка на число участников
    public static function canAddTeamUser($userId, $raidId, $teamId): bool
    {
        $teamUserCount = CSql::teamUserCount($teamId);
        return (self::canEditTeam($userId, $raidId, $teamId) && $teamUserCount < 10);
    }

    /**
     * Когда отображать карты в протоколе
     */
    public static function canShowImages($raidId): bool
    {
        $raidStage = CSql::raidStage($raidId);
        // по идее можно показывать и с 5, но обычно карты загружают позже
        return ($raidStage >= 6);
    }

    /**
     * Когда отображать фильтр точек в протоколе
     */
    public static function canShowPointsFilter($raidId): bool
    {
        $raidStage = CSql::raidStage($raidId);
        // по идее можно показывать и с 5, но обычно карты загружают позже
        return ($raidStage >= 5);
    }

    /**
     * Возможность вводит и править данные в точке
     */
    public static function canEditPointResult($userId, $raidId, $teamId): bool
    {
        $Super = CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);

        $raidStage = CSql::raidStage($raidId);

        // Администратору или модератору можно всегда до закрытия протокола
        if ($Super) {
            return ($raidStage < 7);
        }

        return (false);
    }

    /**
     * Возможность выдавать приглашения
     */
    public static function canDeliveryInvitation($userId, $raidId, $deliveryTypeId): bool
    {
        $Super = CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);

        if (!$Super) {
            return false;
        }

        $raidStage = CSql::raidStage($raidId);
        // по идее можно показывать и с 5, но обычно карты загружают позже

        // до открытия регистрации (до задания даты окончания регистрации) нельзя выдавать
        if ($raidStage > 2 || $raidStage < 1) {
            return false;
        }

        if (CSql::availableInvitationsCount($raidId) <= 0) {
            return false;
        }

        // здесь можно делать ещё проверки на то, что приглашения уже раздавали и что лотерею уже проводили
        if ($deliveryTypeId == 3) {
            return (true);
        }

        if ($deliveryTypeId == 1 || $deliveryTypeId == 2) {
            return ($raidStage == 1);
        }

        return false;
    }

    /**
     * Проверка на возможность перевода команды вне зачета
     * возвращает идентификатор приглашения
     */
    public static function canInviteTeam($userId, $teamId)
    {
      // проверяем, что команда не в зачете и узнаем ключ ММБ
        $sql = "select t.team_outofrange, d.raid_id, t.team_hide
    			from  Teams t 
    			        inner join Distances d
    			        on t.distance_id = d.distance_id
	    		where  t.team_id = $teamId";

        $Row = CSql::singleRow($sql);
        $outOfRange = $Row['team_outofrange'];
        $hideTeam = $Row['team_hide'];
        $raidId = $Row['raid_id'];

	    if (!$outOfRange || $hideTeam)  {
           return (false);
        }

        $raidStage = CSql::raidStage($raidId);
        // по идее можно показывать и с 5, но обычно карты загружают позже

        // до открытия регистрации (до задания даты окончания регистрации) нельзя выдавать
        if ($raidStage > 2 || $raidStage < 1) {
            return (false);
        }

        // проверяем, что пользователь включен в команду, которая не активирована
        // и пытается активировать другую команду
        // на этом марш-броске
          $sql = "select count(*) selfoutofrange
    			from  Teams t 
    			        inner join TeamUsers tu 
    			        on t.team_id = tu.team_id
    			        inner join Distances d
    			        on t.distance_id = d.distance_id
	    		where  tu.user_id = $userId
	    		    and t.team_id <> $teamId
	    		    and t.team_hide = 0
	    		    and d.raid_id = $raidId
	    		    and tu.teamuser_hide = 0
	    		    and t.team_outofrange = 1
	    		";
	    $selfoutofrange = CSql::singleValue($sql, 'selfoutofrange', false);

	    if ($selfoutofrange)  {
           return (false);
        }

        // проверяем, что у пользователя есть приглашение, оо активно и не активировано
        $sql = "select inv.invitation_id
    			from Invitations inv
	    			inner join InvitationDeliveries idev
    				on inv.invitationdelivery_id = idev.invitationdelivery_id
    				left outer join Teams t
    				on inv.invitation_id = t.invitation_id
	    			   and t.team_hide = 0
		    	where idev.raid_id = $raidId
		    	    and inv.user_id = $userId
			    	and inv.invitation_begindt <= NOW()
    				and inv.invitation_enddt >= NOW()
				    and t.team_id is null
				order by inv.invitation_id asc
				LIMIT 0,1
				";

		$invitationId = CSql::singleValue($sql, 'invitation_id', false);

        return $invitationId;
    }

    /**
     * Возможность видеть приглашения пользователя
     */
    public static function canViewUserInvitations($puserId, $raidId, $userId): bool
    {
        return ($puserId == $userId || CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId));
    }

    public static function canDeleteOutOfRangeTeams($userId, $raidId): bool
    {
        $Super = CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);

        $raidStage = CSql::raidStage($raidId);

        // Администратору или модератору можно всегда после закрытия заявки и до закрытия протокола
        if ($Super) {
            return $raidStage >= 1 && $raidStage < 7;
        }

        return false;
    }

    // Возможность видеть результаты (места, рейтинг, данные по точкам) ММБ
    public static function canViewRaidResult($userId, $raidId): bool
    {
        $Super = CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);

        $raidStage = CSql::raidStage($raidId);

        // Администратору или модератору можно всегда или после снятия флага "не показывать результаты"
        return $Super || $raidStage > 5;
    }
	
   // возможность добавить в волонтёры
    public static function canAddToDevelopers($userId, $raidId, $puserId): bool
    {
        $Super = CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);

        if (!$Super)
        {
            return (false);
        }

	    // проверяем, что не участник
 	   $sql = "select count(*) as teamuser
    			from  TeamUsers tu 
    			        inner join Teams t
    			        on t.team_id = tu.team_id
    			        inner join Distances d
    			        on t.distance_id = d.distance_id
	    		where  tu.user_id = $puserId
	    		       and t.team_hide = 0
	    		       and d.raid_id = $raidId
	    		       and tu.teamuser_hide = 0
	    		    
	    		";
	    $teamuser = CSql::singleValue($sql, 'teamuser', false);

        if ($teamuser) {
            return false;
        }

        // проверяем, что не волонтёр уже
	    $sql = "select count(*) as raiddeveloper
    			from  RaidDevelopers rd 
	    		where  rd.user_id = $puserId
	    		       and rd.raiddeveloper_hide = 0
	    		       and rd.raid_id = $raidId
	    		";
        $raiddeveloper = CSql::singleValue($sql, 'raiddeveloper', false);

        if ($raiddeveloper) {
            return false;
        }

        return true;
    }
}

     
