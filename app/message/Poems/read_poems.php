<?php
// Database connection
$servername = "";
$username = "";
$password = "";
$dbname = "";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Fetch poems
$sql = "SELECT id, title, content, author, created_at FROM poems";
$result = $conn->query($sql);

$poems = array();
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $poems[] = $row;
    }
}

// Close connection
$conn->close();

// Return poems in JSON format
header('Content-Type: application/json');
echo json_encode(array("poems" => $poems));
?>