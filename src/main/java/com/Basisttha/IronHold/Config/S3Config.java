package com.Basisttha.IronHold.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.secretAccessKey}")
    private String secretAccessKey;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private AwsCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim())
        );
    }

    private S3Configuration serviceConfiguration() {
        return S3Configuration.builder()
                .checksumValidationEnabled(false)   // prevents checksum signed headers in presigned URLs
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region.trim()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(serviceConfiguration())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region.trim()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(serviceConfiguration())
                .build();
    }

    @Bean
    public ApplicationRunner s3CredentialValidator(S3Client s3Client) {
        return args -> {
            try {
                s3Client.headBucket(b -> b.bucket(bucket.trim()));
                System.out.println("S3 bucket '" + bucket + "' is accessible - credentials OK");
            } catch (AwsServiceException | SdkClientException e) {
                System.out.println("S3 bucket access check FAILED - check credentials/region/permissions: " + e.getMessage());
            }
        };
    }
}