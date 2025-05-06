
    <?php
    header('Content-Type: application/json');
    
    $file = 'messages.txt';
    $messages = [];
    
    if (file_exists($file)) {
        $content = file_get_contents($file);
        $lines = explode("\n", $content);
        
        foreach ($lines as $line) {
            if (trim($line) === '') continue;
            
            if (preg_match('/Sender: (.*?), Timestamp: (.*?), Message: (.*)/', $line, $matches)) {
                $sender = trim($matches[1]);
                $timestamp = trim($matches[2]);
                $message = trim($matches[3]);
                
                $messages[] = [
                    'sender' => $sender,
                    'timestamp' => $timestamp,
                    'message' => $message
                ];
            }
        }
    }
    
    echo json_encode($messages);
    ?>
