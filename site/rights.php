<?php
/**
 * Created by PhpStorm.
 * User: Serge Titov
 * Date: 14.04.2016
 */

if (!isset($MyPHPScript)) return;

class CRights
{
    public static function canViewLogs($userId)
    {
        global $Administrator;
        return $Administrator || $userId == 2202; // Сергей Титов
    }

    public static function canCreateTeam($userId, $raidId)
    {
        // уже есть команда, но не админ / мод
        if (CSql::userTeamId($userId, $raidId) and !CSql::userAdmin($userId) and !CSql::userModerator($userId, $raidId))
            return false;

        $raidStage = CSql::raidStage($raidId);
        return ($raidStage >= 1 and $raidStage < 2);
    }

    public static function canEditTeam($userId, $raidId, $teamId)
    {
        $teamMemberOrSuper = $teamId == CSql::userTeamId($userId, $raidId) || CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);
        if (!$teamMemberOrSuper)
            return false;

        $raidStage = CSql::raidStage($raidId);
        //$teamOutOfRange = CSql::teamOutOfRange($teamId);

        return ($raidStage < 2);
    }

    // 21/05/2016 Проверка на число участников
    public static function canAddTeamUser($userId, $raidId, $teamId)
    {
        $teamUserCount = CSql::teamUserCount($teamId);
        return (self::canEditTeam($userId, $raidId, $teamId) && $teamUserCount < 10);
    }




    // когда отображать карты в протоколе
    public static function canShowImages($raidId)
    {
        $raidStage = CSql::raidStage($raidId);
        // по идее можно показывать и с 5, но обычно карты загружают позже
        return ($raidStage >= 6);
    }
    
    // когда отображать фильтр точек в протоколе
    public static function canShowPointsFilter($raidId)
    {
        $raidStage = CSql::raidStage($raidId);
        // по идее можно показывать и с 5, но обычно карты загружают позже
        return ($raidStage >= 5);
    }
    
    // ВОзможность вводит и править данные в точке 
    public static function canEditPointResult($userId, $raidId, $teamId)
    {
        $Super = CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);
        $raidStage = CSql::raidStage($raidId);

        // Администратору или модератору можно всегда до закрытия протокола
        if ($Super)
        {
            return ($raidStage < 7);
        }

        return (false);
    }

    
}

     
