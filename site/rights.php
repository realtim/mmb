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
        return true;    // todo удалить в основной ветке!

        global $Administrator;
        return $Administrator || $userId == 2202; // Сергей Титов
    }

    public static function canCreateTeam($userId, $raidId)
    {
        // уже есть команда, но не админ / мод
        if (CSql::userTeamId($userId, $raidId) and CSql::userAdmin($userId) and !CSql::userModerator($userId, $raidId))
            return false;

        $raidStage = CSql::raidStage($raidId);
        return $raidStage >= 1 and $raidStage < 7;
    }
}

     