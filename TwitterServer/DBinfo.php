<?php
// 1. Connect to db
$host = "127.0.0.1";
$user = "root";
$password = "123";
$database = "facebook";
$connect = mysqli_connect($host, $user, $password, $database);
if(mysqli_connect_errno())
{
    die("Cannot connect to database field:". mysqli_connect_error());
}
?>
