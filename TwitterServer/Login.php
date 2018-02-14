<?php
// http://localhost/TwitterServer/Login.php?email=wu.en&password=123-
// 1. Connect to db
require("DBinfo.php");

// 2. Define query
$query = "select * from login where email = '" . $_GET['email']
    . "' and password = '" . $_GET['password'] . "'";

$result = mysqli_query($connect, $query);
if (!$result) {
    die("Error in query");
}
// 3. Get Data from Database
$output = array();
while ($row = mysqli_fetch_assoc($result))
{
    $output[] = $row;
    break;
}

if ($output) {
    print("{'msg':'Pass Login'" . ",'info':'" . json_encode($output) ."'}");
} else {
    print("{'msg':'Cannot login'}");
}
?>