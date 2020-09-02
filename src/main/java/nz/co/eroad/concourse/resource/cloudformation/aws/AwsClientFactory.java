package nz.co.eroad.concourse.resource.cloudformation.aws;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsClientFactory {
  private static final ClientOverrideConfiguration unlimitedRetry = ClientOverrideConfiguration.builder()
      .retryPolicy(RetryPolicy.builder(RetryMode.STANDARD).numRetries(Integer.MAX_VALUE).build())
      .build();


  public static CloudFormationClient cloudFormationClient(Region region, AwsBasicCredentials awsBasicCredentials) {
    return CloudFormationClient.builder()
        .overrideConfiguration(unlimitedRetry)
        .credentialsProvider(awsBasicCredentials == null ? null : StaticCredentialsProvider.create(awsBasicCredentials))
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .region(region)
        .build();
  }

  public static S3Client s3Client(Region region, AwsBasicCredentials awsBasicCredentials) {
    return S3Client.builder()
        .overrideConfiguration(unlimitedRetry)
        .credentialsProvider(awsBasicCredentials == null ? null : StaticCredentialsProvider.create(awsBasicCredentials))
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .region(region)
        .build();
  }
}
