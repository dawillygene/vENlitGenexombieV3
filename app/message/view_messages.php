<!DOCTYPE html>
<html>
<head>
    <title>SMS Messages Viewer</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .message { margin: 10px 0; padding: 10px; border-radius: 5px; }
        .sent { background-color: #e3f2fd; border-left: 4px solid #2196f3; }
        .received { background-color: #f3e5f5; border-left: 4px solid #9c27b0; }
        .timestamp { color: #666; font-size: 0.9em; }
        .address { font-weight: bold; color: #333; }
        .type { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 0.8em; color: white; }
        .type.sent { background-color: #2196f3; }
        .type.received { background-color: #9c27b0; }
        h1 { color: #333; }
        .stats { background-color: #f5f5f5; padding: 10px; margin-bottom: 20px; border-radius: 5px; }
    </style>
</head>
<body>
    <h1>SMS Messages</h1>
    
    <?php
    $filePath = 'messages.txt';
    
    if (file_exists($filePath)) {
        $messages = file($filePath, FILE_IGNORE_NEW_LINES);
        $sentCount = 0;
        $receivedCount = 0;
        
        // Count message types
        foreach ($messages as $message) {
            if (strpos($message, 'SENT') !== false) {
                $sentCount++;
            } elseif (strpos($message, 'RECEIVED') !== false) {
                $receivedCount++;
            }
        }
        
        echo "<div class='stats'>";
        echo "<strong>Total Messages:</strong> " . count($messages) . " | ";
        echo "<strong>Sent:</strong> " . $sentCount . " | ";
        echo "<strong>Received:</strong> " . $receivedCount;
        echo "</div>";
        
        // Display messages
        foreach (array_reverse($messages) as $message) {
            if (empty(trim($message))) continue;
            
            // Parse new format: [timestamp] TYPE | address | body | HASH:xxxxx
            if (preg_match('/\[(.*?)\] (SENT|RECEIVED) \| (.*?) \| (.*?) \| HASH:([a-f0-9]+)/', $message, $matches)) {
                $timestamp = $matches[1];
                $type = strtolower($matches[2]);
                $address = $matches[3];
                $body = $matches[4];
                // $hash = $matches[5]; // We capture it but don't display it
                
                echo "<div class='message $type'>";
                echo "<span class='type $type'>$matches[2]</span> ";
                echo "<span class='address'>$address</span> ";
                echo "<span class='timestamp'>$timestamp</span><br>";
                echo "<div style='margin-top: 5px;'>$body</div>";
                echo "</div>";
            } elseif (preg_match('/\[(.*?)\] (SENT|RECEIVED) \| (.*?) \| (.*)/', $message, $matches)) {
                // Fallback for messages without hash (old format)
                $timestamp = $matches[1];
                $type = strtolower($matches[2]);
                $address = $matches[3];
                $body = $matches[4];
                
                echo "<div class='message $type'>";
                echo "<span class='type $type'>$matches[2]</span> ";
                echo "<span class='address'>$address</span> ";
                echo "<span class='timestamp'>$timestamp</span><br>";
                echo "<div style='margin-top: 5px;'>$body</div>";
                echo "</div>";
            } else {
                // Fallback for old format
                echo "<div class='message'>";
                echo htmlspecialchars($message);
                echo "</div>";
            }
        }
    } else {
        echo "<p>No messages found. File does not exist.</p>";
    }
    ?>
    
    <script>
        // Auto-refresh every 30 seconds
        setTimeout(function() {
            location.reload();
        }, 30000);
    </script>
</body>
</html>
