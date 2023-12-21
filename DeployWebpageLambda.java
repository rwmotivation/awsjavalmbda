import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DeployWebpageLambda {
  // Lambda function handler
  AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .build();

  AWSSecretsManager secretsManager = AWSSecretsManagerClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .build();

  AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .build();

  try {
    String secretName = "your-secret-name";
    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
        .withSecretId(secretName);
    GetSecretValueResult getSecretValueResult = secretsManager.getSecretValue(getSecretValueRequest);
    String secretValue = getSecretValueResult.getSecretString();

    JSONObject secretJson = new JSONObject(secretValue);
    // Now you can get values from the JSON object using the keys

    // Upload all files in a directory to your S3 bucket
    String bucketName = "your-bucket-name";
    String directoryPath = "path-to-your-directory";

    Files.walk(Paths.get(directoryPath))
        .filter(Files::isRegularFile)
        .forEach(path -> {
          File file = path.toFile();
          s3Client.putObject(new PutObjectRequest(bucketName, file.getName(), file));
        });

    // Configure the S3 bucket for static website hosting
    try {
      BucketWebsiteConfiguration websiteConfig = new BucketWebsiteConfiguration("index.html");
      s3Client.setBucketWebsiteConfiguration(bucketName, websiteConfig);
    } catch (Exception e) {
      System.out.println("An error occurred when configuring the S3 bucket for static website hosting: " + e.getMessage());
    }

    // Print the S3 URL
    String region = "your-region";  // replace with your AWS region
    String url = "http://" + bucketName + ".s3-website-" + region + ".amazonaws.com";
    System.out.println("Website URL: " + url);
  } catch (Exception e) {
    System.out.println("An error occurred: " + e.getMessage());
  }
}