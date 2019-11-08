<?php

		ini_set('display_errors',1);
		error_reporting(E_ALL);
	// Content Type JSON
//	header("Content-type: application/json");

	print("start");
	// Database connection
	$mysqli = new mysqli("localhost", "root", "", "c5_mmb_test");
	if($mysqli->connect_error)
	{
    	die("Database connection failed! $mysqli->connect_errno: $mysqli->connect_error");
	}
	//$res = array('error' => false);


	// Read data from database
	$essenceId = '';
	$actiontypeId = '';
	$essenceactionId = '';
	$sessionUid = '7021eff3-f89a-11e9-96a3-ed83c44e3933';
	$executionUid = '';

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
		//  call getEssenceActionHandler
		//$query = "SELECT user_name, user_email FROM Users WHERE user_id = ?";
		$getTestUser = "CALL usp_getTestUser(@executionUid, ?, ?)";
		$viewLogs = "CALL usp_viewLogs(?, ?)";
		//$query = "call usp_getEssenceActionHandeler(@executionUid, ?, ?)";
		$stmtGetTestUser = $mysqli->stmt_init();
		$stmtViewLogs = $mysqli->stmt_init();
		if(!$stmtGetTestUser->prepare($getTestUser) || !$stmtViewLogs->prepare($viewLogs))
		{
			print "Failed to prepare statement\n";
		}
		else
		{
			
			$stmtGetTestUser->bind_param("si", $sessionUid, $user);
			//$stmt->bind_param("si", $sessionUid, $essenceactionId);
			$users = array(1,2,3,4,19);
		
			foreach($users as $user)
			{
				print " $user \n";

				// setup empty @executionUid
				if (!$mysqli->query("set @executionUid = null")) {
					echo "Fetch output failed: (" . $mysqli->errno . ") " . $mysqli->error;
				}

				// execute main procedure
				$stmtGetTestUser->execute();

				// read the procedure output (recordsets) 	
				do {
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
			
				$stmtGetTestUser->free_result();

				if (!($resultOutputParameters = $mysqli->query("select @executionUid as _executionUid"))) {
					echo "Fetch output failed: (" . $mysqli->errno . ") " . $mysqli->error;
				}
				
				$outputParameters = $resultOutputParameters->fetch_assoc();
				$executionUid = $outputParameters['_executionUid'];
				print "  output $executionUid ";
				
				print " start log reading ";

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
				print " finish log reading \n";
			}
		}
		



		$stmtGetTestUser->close();
		$stmtViewLogs->close();


		/* $select = $conn->query('SELECT @executionUid');
		$result = $select->fetch_assoc();
		$executionUid = $result['@executionUid'];

		$callViewLog = $conn->prepare('call usp_viewLog(?, ?)');
		$callViewLog->bind_param('ss', $executionUid, $sessionId);
		$callViewLog->execute();
		$result = $callViewLog->get_result();
		$row = $result->fetch_array(MYSQLI_ASSOC);

		$logs  = array();
		while ($row = $result->fetch_array(MYSQLI_ASSOC)) 
		{
			array_push($logs, $row);
		}
		$res['logs'] = $logs; */
	}
	else
	{
		//  viewEssenceActions

	}



	/* if ($action == 'read') {
		$result = $conn->query("SELECT * FROM `users`");
		$users  = array();

		while ($row = $result->fetch_assoc()) {
			array_push($users, $row);
		}
		$res['users'] = $users;
		//$res['error']   = true;
		//$res['message'] = "Users load failed!";
		$res['error']   = false;
		$res['message'] = "Users load ok!";
	}


	// Insert data into database
	if ($action == 'create') {
		$username = $_POST['username'];
		$email    = $_POST['email'];
		$mobile   = $_POST['mobile'];

		$result = $conn->query("INSERT INTO `users` (`username`, `email`, `mobile`) VALUES('$username', '$email', '$mobile')");

		if ($result) {
			$res['message'] = "User added successfully";
		} else {
			$res['error']   = true;
			$res['message'] = "User insert failed!";
		}
	}


	// Update data

	if ($action == 'update') {
		$id       = $_POST['id'];
		$username = $_POST['username'];
		$email    = $_POST['email'];
		$mobile   = $_POST['mobile'];


		$result = $conn->query("UPDATE `users` SET `username`='$username', `email`='$email', `mobile`='$mobile' WHERE `id`='$id'");

		if ($result) {
			$res['message'] = "User updated successfully";
		} else {
			$res['error']   = true;
			$res['message'] = "User update failed!";
 		}
	}


	// Delete data

	if ($action == 'delete') {
		$id       = $_POST['id'];
		$username = $_POST['username'];
		$email    = $_POST['email'];
		$mobile   = $_POST['mobile'];

		$result = $conn->query("DELETE FROM `users` WHERE `id`='$id'");

		if ($result) {
			$res['message'] = "User delete success";
		} else {
			$res['error']   = true;
			$res['message'] = "User delete failed!";
		}
	} */


	// Close database connection
	$mysqli->close();
	print("finish");
	// print json encoded data
//	echo json_encode($res);
//	die();

?>