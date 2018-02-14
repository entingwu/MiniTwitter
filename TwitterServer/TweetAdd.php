<?php
// 1. Connect to db
require("DBinfo.php");

// 2. Define query
$query = "insert into tweets(user_id, tweet_text, tweet_picture) values (" . $_GET['user_id']
    . ",'" . $_GET['tweet_text'] . "','" . $_GET['tweet_picture'] . "')";
$result = mysqli_query($connect, $query);

if (!$result)
{
    $output = "{'msg':'fail'}";
} else {
    $output = "{'msg':'tweet is added'}";
}

print($output);

mysqli_close($connect);
?>