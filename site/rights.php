<?php
/**
 * Created by PhpStorm.
 * User: Serge Titov
 * Date: 14.04.2016
 */

if (!isset($MyPHPScript)) return;

class CRights {
    public static function canViewLogs($userId)
    {
        return true;    // todo удалить в основной ветке!

        global $Administrator;
        return $Administrator || $userId == 2202; // Сергей Титов
    }
}