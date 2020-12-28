package org.kie.cekit.cacher.objects;

public class PlainArtifact {

    private String fileName;
    private String checksum;
    private String timestamp;
    private String buildDate;
    private String version;
    private String branch;

    public PlainArtifact() {
    }

    public PlainArtifact(String fileName, String checksum, String timestamp, String buildDate, String version, String branch) {
        this.fileName = fileName;
        this.checksum = checksum;
        this.timestamp = timestamp;
        this.buildDate = buildDate;
        this.version = version;
        this.branch = branch;
    }

    public PlainArtifact(String fileName, String checksum, String timestamp) {
        this.fileName = fileName;
        this.checksum = checksum;
        this.timestamp = timestamp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getChecksum() {
        if (null == checksum) {
            checksum = "";
        }
        return checksum.replace("tmp", "Downloading...");
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getBuildDate() {
        return buildDate;
    }

    public void setBuildDate(String buildDate) {
        this.buildDate = buildDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    @Override
    public String toString() {
        return "PlainArtifact{" +
                "fileName='" + fileName + '\'' +
                ", checksum='" + checksum + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", buildDate='" + buildDate + '\'' +
                ", version='" + version + '\'' +
                ", branch='" + branch + '\'' +
                '}';
    }
}
