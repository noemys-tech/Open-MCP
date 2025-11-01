package fr.noemys.s3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for MCP S3 Server
 * 
 * @version 1.0.0
 */
@SpringBootApplication
public class S3McpApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(S3McpApplication.class, args);
    }
}

