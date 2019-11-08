<?php

		ini_set('display_errors',1);
		error_reporting(E_ALL);
	// Content Type JSON
//	header("Content-type: application/json");

	print("start");
	// Database connection
    
    $host = "localhost";
    $dbname = "c5_mmb_test";
    $username = "root";
    $password = "";

    $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);



	// Read data from database
	$essenceId = '';
	$actiontypeId = '';
	$essenceactionId = '';
	$sessionUid = '7021eff3-f89a-11e9-96a3-ed83c44e3933';
	$executionUid = null;

	if (isset($_GET['essenceid'])) {
		$essenceId = $_GET['essenceid'];
	}


	if (isset($_GET['actiontypeid'])) {
		$actiontypeId = $_GET['actiontypeid'];
	}

	if (isset($_GET['essenceactionid'])) {
		$essenceactionId = $_GET['essenceactionid'];
	}

	print " $essenceactionId \n";
	print " $sessionUid \n";

	if (isset($essenceactionId))
	{

        try 
        {
        $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
        
        $getTestUser = "CALL usp_getTestUser(@executionUid, :sessionUid, :userId)";
		//$viewLogs = "CALL usp_viewLogs(:executionUid, :sessionUid)";
        
        $stmtGetTestUser = $pdo->prepare($getTestUser);
			// doesn't work
		//$stmtGetTestUser->bindParam(':executionUid', $executionUid, PDO::PARAM_INT|PDO::PARAM_INPUT_OUTPUT);
        $stmtGetTestUser->bindParam(':sessionUid', $sessionUid, PDO::PARAM_STR, 36);
        $stmtGetTestUser->bindParam(':userId', $user, PDO::PARAM_INT);
        
			$users = array(1,2,3,4);
		
			foreach($users as $user)
			{
				print " $user \n";


				// execute main procedure
				$stmtGetTestUser->execute();
				
				do {

					print "--\n";
					if($result = $stmtGetTestUser->fetchall(PDO::FETCH_ASSOC))
					{
						//print  $result["user_name"]." \n";
						print_r($result);
					}
	
				} while ($stmtGetTestUser->nextRowset());
				
				$stmtGetTestUser->closeCursor();

                // execute the second query to get customer's level
                $row = $pdo->query("SELECT @executionUid AS _executionUid")->fetch(PDO::FETCH_ASSOC);
                if ($row) {
                    print $row["_executionUid"];
                }
                
          
			}

		$stmtGetTestUser->closeCursor();
        $pdo = null;
        //$stmtViewLogs->close();

        } catch (PDOException $e) {
            echo 'Connection failed: ' . $e->getMessage();
        }

	
	}
	else
	{
		//  viewEssenceActions

	}




	// Close database connection
//	$mysqli->close();
	print("finish");
	// print json encoded data
//	echo json_encode($res);
//	die();

?>