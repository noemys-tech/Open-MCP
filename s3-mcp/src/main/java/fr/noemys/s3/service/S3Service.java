package fr.noemys.s3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S3 Service for managing S3 operations
 * 
 * @version 1.0.0
 */
@Service
public class S3Service {
    
    private static final Logger log = LoggerFactory.getLogger(S3Service.class);
    
    /**
     * Create S3 client from credentials
     */
    private S3Client createS3Client(String endpoint, String token, String userToken) {
        // Use token and userToken as S3 credentials (access key and secret key)
        AwsBasicCredentials credentials = AwsBasicCredentials.create(token, userToken);
        
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // Default region, can be parameterized
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
    
    /**
     * List all buckets
     */
    public Map<String, Object> listBuckets(String endpoint, String token, String userToken) {
        log.info("Listing all buckets from endpoint: {}", endpoint);
        
        try (S3Client s3Client = createS3Client(endpoint, token, userToken)) {
            ListBucketsResponse response = s3Client.listBuckets();
            
            List<Map<String, Object>> buckets = new ArrayList<>();
            for (Bucket bucket : response.buckets()) {
                Map<String, Object> bucketInfo = new HashMap<>();
                bucketInfo.put("name", bucket.name());
                bucketInfo.put("creationDate", bucket.creationDate().toString());
                buckets.add(bucketInfo);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("buckets", buckets);
            result.put("count", buckets.size());
            
            log.info("Found {} bucket(s)", buckets.size());
            return result;
        } catch (Exception e) {
            log.error("Error listing buckets", e);
            throw new RuntimeException("Failed to list buckets: " + e.getMessage(), e);
        }
    }
    
    /**
     * List objects in a bucket
     */
    public Map<String, Object> listObjects(String endpoint, String token, String userToken, 
                                           String bucketName, String prefix) {
        log.info("Listing objects in bucket: {} with prefix: {}", bucketName, prefix);
        
        try (S3Client s3Client = createS3Client(endpoint, token, userToken)) {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);
            
            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }
            
            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            
            List<Map<String, Object>> objects = new ArrayList<>();
            for (S3Object s3Object : response.contents()) {
                Map<String, Object> objectInfo = new HashMap<>();
                objectInfo.put("key", s3Object.key());
                objectInfo.put("size", s3Object.size());
                objectInfo.put("lastModified", s3Object.lastModified().toString());
                objectInfo.put("storageClass", s3Object.storageClassAsString());
                objects.add(objectInfo);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("objects", objects);
            result.put("count", objects.size());
            result.put("prefix", prefix != null ? prefix : "");
            
            log.info("Found {} object(s) in bucket {}", objects.size(), bucketName);
            return result;
        } catch (Exception e) {
            log.error("Error listing objects", e);
            throw new RuntimeException("Failed to list objects: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download an object from a bucket
     */
    public Map<String, Object> downloadObject(String endpoint, String token, String userToken,
                                              String bucketName, String objectKey) {
        log.info("Downloading object: {} from bucket: {}", objectKey, bucketName);
        
        try (S3Client s3Client = createS3Client(endpoint, token, userToken)) {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            
            // Read the content
            byte[] content = response.readAllBytes();
            String contentString = new String(content);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", contentString);
            result.put("size", content.length);
            result.put("contentType", response.response().contentType());
            
            log.info("Downloaded object {} ({} bytes)", objectKey, content.length);
            return result;
        } catch (Exception e) {
            log.error("Error downloading object", e);
            throw new RuntimeException("Failed to download object: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get object metadata
     */
    public Map<String, Object> getObjectMetadata(String endpoint, String token, String userToken,
                                                 String bucketName, String objectKey) {
        log.info("Getting metadata for object: {} in bucket: {}", objectKey, bucketName);
        
        try (S3Client s3Client = createS3Client(endpoint, token, userToken)) {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            
            HeadObjectResponse response = s3Client.headObject(request);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key", objectKey);
            metadata.put("size", response.contentLength());
            metadata.put("contentType", response.contentType());
            metadata.put("lastModified", response.lastModified().toString());
            metadata.put("eTag", response.eTag());
            metadata.put("storageClass", response.storageClassAsString());
            
            // Add custom metadata if present
            if (response.metadata() != null && !response.metadata().isEmpty()) {
                metadata.put("customMetadata", response.metadata());
            }
            
            log.info("Retrieved metadata for object {}", objectKey);
            return metadata;
        } catch (Exception e) {
            log.error("Error getting object metadata", e);
            throw new RuntimeException("Failed to get object metadata: " + e.getMessage(), e);
        }
    }
}

