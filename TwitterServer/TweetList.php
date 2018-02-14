<?php
// 1. Connect to db
require("DBinfo.php");

// 2. Define query
// startFrom, 0-20, 20-40
if ($username = $_GET['op'] == 1) {
    // I. my following
    $query = "select * from user_tweets where user_id in (
        select following_user_id from following where user_id = " . $_GET['user_id']
        . ") or user_id = " . $_GET['user_id'] . " order by tweet_date DESC"
        . " LIMIT 20 OFFSET " . $_GET['startFrom'];
} else if ($username = $_GET['op'] == 2) {
    // II. specific person post
    $query = "select * from user_tweets where user_id = " . $_GET['user_id'] . " order by tweet_date DESC"
        . " LIMIT 20 OFFSET " . $_GET['startFrom'];
} else if ($username = $_GET['op'] == 3) {
    // III. search post of specific topic
    $query = "select * from user_tweets where tweet_text like '%"
        . $_GET['query'] . "%' LIMIT 20 OFFSET " . $_GET['startFrom'];
}

$result = mysqli_query($connect, $query);
if (!$result) {
    die("Error in query");
}

// 3. Get Data from Database
$output = array();
while ($row = mysqli_fetch_assoc($result)) {
    $output[] = $row;
}

if ($output) {// json
    print("{'msg':'has tweet'" . ", 'info':'". json_encode($output) ."'}");
} else {
    print("{'msg':'no tweet'}");
}
print(json_encode($output));

// 4. Clear
mysqli_free_result($result);
// 5. Close connection
mysqli_close($connect);
?>