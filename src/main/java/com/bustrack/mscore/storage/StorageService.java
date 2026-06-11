package com.bustrack.mscore.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

@Service
public class StorageService {

    @Value("${app.mock.external-services:true}")
    private boolean mockEnabled;

    private static final String BASE_PATH = System.getProperty("java.io.tmpdir") + "/bustrack-uploads";
    private static final String BASE_URL = "http://localhost:3000/mock-files";

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    @Value("${s3.buckets.boletos:bustrack-boletos}")
    private String boletosBucket;

    @Value("${s3.buckets.facturas:bustrack-facturas}")
    private String facturasBucket;

    @Value("${s3.buckets.licencias:bustrack-licencias}")
    private String licenciasBucket;

    @Value("${s3.buckets.evidencias:bustrack-evidencias}")
    private String evidenciasBucket;

    @Value("${s3.buckets.buses:bustrack-buses}")
    private String busesBucket;

    @Value("${s3.url-expiration-seconds:600}")
    private long urlExpirationSeconds;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        if (mockEnabled) {
            new File(BASE_PATH).mkdirs();
        } else {
            var creds = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
            var r = Region.of(region);
            s3Client = S3Client.builder().region(r).credentialsProvider(creds).build();
            s3Presigner = S3Presigner.builder().region(r).credentialsProvider(creds).build();
        }
    }

    public String generateUploadUrl(String s3Key) {
        if (mockEnabled) return BASE_URL + "/upload/" + s3Key;
        var req = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(urlExpirationSeconds))
                .putObjectRequest(b -> b.bucket(bucketFromKey(s3Key)).key(s3Key))
                .build();
        return s3Presigner.presignPutObject(req).url().toString();
    }

    public String getDownloadUrl(String s3Key) {
        if (mockEnabled) return BASE_URL + "/" + s3Key;
        var req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(urlExpirationSeconds))
                .getObjectRequest(b -> b.bucket(bucketFromKey(s3Key)).key(s3Key))
                .build();
        return s3Presigner.presignGetObject(req).url().toString();
    }

    public String uploadFile(String s3Key, byte[] content) throws IOException {
        if (mockEnabled) {
            var file = new File(BASE_PATH, s3Key);
            file.getParentFile().mkdirs();
            try (var out = new FileOutputStream(file)) { out.write(content); }
            return s3Key;
        }
        s3Client.putObject(
                b -> b.bucket(bucketFromKey(s3Key)).key(s3Key).contentLength((long) content.length),
                RequestBody.fromBytes(content));
        return s3Key;
    }

    public byte[] downloadFile(String s3Key) throws IOException {
        if (mockEnabled) return Files.readAllBytes(Paths.get(BASE_PATH, s3Key));
        return s3Client.getObjectAsBytes(b -> b.bucket(bucketFromKey(s3Key)).key(s3Key)).asByteArray();
    }

    private String bucketFromKey(String s3Key) {
        return switch (s3Key.split("/")[0]) {
            case "boletos" -> boletosBucket;
            case "facturas" -> facturasBucket;
            case "licencia", "foto_chofer", "foto_facial" -> licenciasBucket;
            case "foto_bus" -> busesBucket;
            case "evidencias" -> evidenciasBucket;
            default -> boletosBucket;
        };
    }

    public String getBucketName(String type) {
        return switch (type) {
            case "boletos" -> boletosBucket;
            case "facturas" -> facturasBucket;
            case "licencias" -> licenciasBucket;
            case "evidencias" -> evidenciasBucket;
            case "buses" -> busesBucket;
            default -> boletosBucket;
        };
    }
}
