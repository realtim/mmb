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
        return $raidStage >= 1 and $raidStage < 7;
    }

    public static function canEditTeam($userId, $raidId, $teamId)
    {
        $teamMemberOrSuper = $teamId == CSql::userTeamId($userId, $raidId) || CSql::userAdmin($userId) || CSql::userModerator($userId, $raidId);
        if (!$teamMemberOrSuper)
            return false;

        $raidStage = CSql::raidStage($raidId);
        $teamOutOfRange = CSql::teamOutOfRange($teamId);

        return (!$teamOutOfRange && $raidStage < 2)
             || ($teamOutOfRange && $raidStage < 7);
    }

    // когда отображать карты в протоколе
    public static function canShowImages($raidId)
    {
        $raidStage = CSql::raidStage($raidId);
        // по идее можно показывать и с 5, но обычно карты загружают позже
        return ($raidStage >= 6);
    }
}

     
