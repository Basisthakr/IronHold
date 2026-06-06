package com.Basisttha.IronHold.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

//S3StorageService only talks to S3. Anything else that is anything other than S3 Talk, like checking if file<=qouta IS NOT S3 Talk. 
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    @Value("${aws.s3.bucket}")
    private String bucketname;

    public String initiateUpload(String s3ObjectKey) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                                                         .bucket(bucketname)
                                                         .key(s3ObjectKey)
                                                         .build();
        PutObjectPresignRequest objectPresignRequest = PutObjectPresignRequest.builder()
                                                                              .signatureDuration(Duration.ofMinutes(15))
                                                                              .putObjectRequest(objectRequest)
                                                                              .build();

        return s3Presigner.presignPutObject(objectPresignRequest).url().toString();
    }

    public String initiateDownload(String s3ObjectKey, String originalFilename) {
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename;
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                         .bucket(bucketname)
                                                         .key(s3ObjectKey)
                                                         .responseContentDisposition(disposition)
                                                         .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                                                                        .signatureDuration(Duration.ofMinutes(15))
                                                                        .getObjectRequest(getObjectRequest)
                                                                        .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public boolean objectExists(String s3ObjectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketname)
                    .key(s3ObjectKey)
                    .build());
            return true;//if no exception is thrown, the object is there
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public void deleteObject(String s3ObjectKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketname)
                .key(s3ObjectKey)
                .build();
        s3Client.deleteObject(deleteRequest);
    }
}
