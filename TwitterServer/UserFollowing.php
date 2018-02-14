<?php
// 1. Connect to db
require("DBinfo.php");

// 2. Define query
// 'op' == 1 add, 'op' == 2 delete
if ($_GET['op'] == 1) {
    // I. Add Following
    $query = "insert into following(user_id, following_user_id) values ("
        . $_GET['user_id'] . "," . $_GET['following_user_id']. ")";
} else {
    // II. Remove Following
    $query = "delete from following where user_id = " . $_GET['user_id']
        . " and following_user_id = " . $_GET['following_user_id'] ;
}

$result = mysqli_query($connect, $query);
if (!$result) {
    $output = "{'msg':'fail'}";
} else {
    $output = "{'msg':'following is updated'}";
}

print($output);
// 5. Close connection
mysqli_close($connect);
?>