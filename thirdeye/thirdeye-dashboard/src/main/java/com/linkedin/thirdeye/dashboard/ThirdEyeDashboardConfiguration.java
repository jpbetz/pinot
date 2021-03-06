package com.linkedin.thirdeye.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkedin.thirdeye.anomaly.api.AnomalyDatabaseConfig;

import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ThirdEyeDashboardConfiguration extends Configuration {
  @NotNull
  private String serverUri; // TODO: Support talking to multiple servers

  private String feedbackEmailAddress;

  private String customDashboardRoot; // directory where all {dashboard}.yml files are saved

  private String collectionConfigRoot; // directory where all collection configs are defined

  private AnomalyDatabaseConfig anomalyDatabaseConfig; // information for the anomaly database

  @Valid
  @NotNull
  @JsonProperty
  private HttpClientConfiguration httpClient = new HttpClientConfiguration();

  public HttpClientConfiguration getHttpClient() {
    return httpClient;
  }

  public String getServerUri() {
    return serverUri;
  }

  public String getFeedbackEmailAddress() {
    return feedbackEmailAddress;
  }

  public void setServerUri(String serverUri) {
    this.serverUri = serverUri;
  }

  public String getCustomDashboardRoot() {
    return customDashboardRoot;
  }

  public void setCustomDashboardRoot(String customDashboardRoot) {
    this.customDashboardRoot = customDashboardRoot;
  }

  public String getCollectionConfigRoot() {
    return collectionConfigRoot;
  }

  public void setCollectionConfigRoot(String collectionConfigRoot) {
    this.collectionConfigRoot = collectionConfigRoot;
  }

  public AnomalyDatabaseConfig getAnomalyDatabaseConfig() {
    return anomalyDatabaseConfig;
  }

  public void setAnomalyDatabaseConfig(AnomalyDatabaseConfig anomalyDatabaseConfig) {
    this.anomalyDatabaseConfig = anomalyDatabaseConfig;
  }
}
