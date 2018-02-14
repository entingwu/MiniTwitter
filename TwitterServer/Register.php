<?php
// http://localhost/TwitterServer/Register.php?first_name=enting&email=wu.en&password=123&picture_path=test.jpg
// 1. Connect to db
require("DBinfo.php");

// 2. Define query
$query = "insert into login(first_name, email, password, picture_path) values ('"
    . $_GET['first_name'] . "','" . $_GET['email'] . "','"
    . $_GET['password'] . "','" . $_GET['picture_path'] . "')";

// 3. Query
$result = mysqli_query($connect, $query);
if (! $result) {
    $output = "{'msg':'fail'}";
} else {
    $output = "{'msg':'user is added'}";
}
// 4. Print output in json
print($output);
// 5. Close connection
mysqli_close($connect);
?>