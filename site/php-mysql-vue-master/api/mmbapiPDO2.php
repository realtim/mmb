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
        $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
        
        $getTestUser = "CALL usp_getTestOutputUser(?, ?, ?)";
		//$viewLogs = "CALL usp_viewLogs(:executionUid, :sessionUid)";
        
        $stmtGetTestUser = $pdo->prepare($getTestUser);
      //  $stmtGetTestUser->bindParam(1, $executionUid, PDO::PARAM_STR|PDO::PARAM_INPUT_OUTPUT, 36);
        $stmtGetTestUser->bindParam(2, $sessionUid, PDO::PARAM_STR, 36);
        $stmtGetTestUser->bindParam(3, $user, PDO::PARAM_INT);
        
			$users = array(1,2,3,4,19);
		
			foreach($users as $user)
			{
				print " $user \n";


				// execute main procedure
				$stmtGetTestUser->execute(array($executionUid));
                $stmtGetTestUser->closeCursor();

                print " executionUid:  $executionUid \n";

                // execute the second query to get customer's level
               /*  $row = $pdo->query("SELECT @executionUid AS _executionUid")->fetch(PDO::FETCH_ASSOC);
                if ($row) {
                    print $row["_executionUid"];
                } */
                
                

				// read the procedure output (recordsets) 	
			/* 	do {
					if ($result = $stmtGetTestUser->get_result()) {
						printf("---\n");
						while ($row = $result->fetch_array(MYSQLI_ASSOC))
						{
							printf ("%s (%s)\n", $row["user_name"], $row["user_email"]);
						} 
						printf("-\n");
						//var_dump(mysqli_fetch_all($res));
						
					} else {
						if ($stmtGetTestUser->errno) {
							echo "Store failed: (" . $stmtGetTestUser->errno . ") " . $stmtGetTestUser->error;
						}
					}
				} while ($stmtGetTestUser->more_results() && $stmtGetTestUser->next_result());
			 */
				//print "  output $executionUid ";
				
		/* 		print " start log reading ";

				// setup viewLog procedure
				$stmtViewLogs->bind_param("ss", $executionUid, $sessionUid);
				$stmtViewLogs->execute();

				// read logs	
				do {
					if ($resultLog = $stmtViewLogs->get_result()) {
						printf("---\n");
						while ($row = $resultLog->fetch_array(MYSQLI_ASSOC))
						{
							printf ("%s (%s)\n", $row["logs_message"], $row["logs_dt"]);
						} 
						printf("-\n");
						//var_dump(mysqli_fetch_all($res));
						
					} else {
						if ($stmtViewLogs->errno) {
							echo "Store failed: (" . $stmtViewLogs->errno . ") " . $stmtGetTestUser->error;
						}
					}
				} while ($stmtViewLogs->more_results() && $stmtViewLogs->next_result());
			
				$stmtViewLogs->free_result();
				print " finish log reading \n"; */
			}

		
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