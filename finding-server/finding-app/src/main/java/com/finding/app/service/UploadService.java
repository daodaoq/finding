package com.finding.app.service;

public interface UploadService {

    String uploadImage(byte[] data, String originalFilename, String contentType);

    String uploadImage(byte[] data, String originalFilename, String contentType, String scene);

    String uploadVideo(byte[] data, String originalFilename, String contentType);
}
